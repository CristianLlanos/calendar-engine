package com.calendarengine.modules.event.actions

import com.calendarengine.dto.EventOccurrence
import com.calendarengine.models.Events
import com.calendarengine.models.RecurrenceExceptions
import com.calendarengine.models.RecurrenceRules
import com.calendarengine.modules.event.RruleExpander
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId

class ExpandOccurrencesAction(
    private val rruleExpander: RruleExpander,
) {

    fun execute(
        calendarId: Int,
        tenantId: Int,
        rangeStart: LocalDateTime,
        rangeEnd: LocalDateTime,
    ): List<EventOccurrence> = transaction {
        val events = Events.selectAll().where {
            (Events.calendarId eq calendarId) and
                (Events.tenantId eq tenantId) and
                (
                    // Non-recurring events that overlap the range
                    ((Events.recurrenceRuleId.isNull()) and
                        (Events.startTime lessEq rangeEnd) and
                        (Events.endTime greaterEq rangeStart)) or
                        // Recurring events (need expansion to determine overlap)
                        (Events.recurrenceRuleId.isNotNull())
                    )
        }.toList()

        val occurrences = mutableListOf<EventOccurrence>()

        for (event in events) {
            val recurrenceRuleId = event[Events.recurrenceRuleId]

            if (recurrenceRuleId == null) {
                // Single event — include directly
                occurrences.add(event.toOccurrence(isRecurring = false))
            } else {
                // Recurring event — expand RRULE
                val rule = RecurrenceRules.selectAll()
                    .where { RecurrenceRules.id eq recurrenceRuleId }
                    .firstOrNull() ?: continue

                val exceptions = RecurrenceExceptions.selectAll()
                    .where { RecurrenceExceptions.eventId eq event[Events.id] }
                    .toList()

                val excludedDates = exceptions
                    .filter { it[RecurrenceExceptions.isExcluded] }
                    .map { it[RecurrenceExceptions.originalDate] }
                    .toSet()

                val overrides = exceptions
                    .filter { !it[RecurrenceExceptions.isExcluded] }
                    .associateBy { it[RecurrenceExceptions.originalDate] }

                val rrule = rule[RecurrenceRules.rrule]
                val dtstart = rule[RecurrenceRules.dtstart]
                val timezone = ZoneId.of(rule[RecurrenceRules.timezone])
                val eventDuration = Duration.between(event[Events.startTime], event[Events.endTime])

                val expandedDates = rruleExpander.expand(
                    rrule = rrule,
                    dtstart = dtstart,
                    timezone = timezone,
                    rangeStart = rangeStart,
                    rangeEnd = rangeEnd,
                )

                for (occurrenceStart in expandedDates) {
                    if (occurrenceStart in excludedDates) continue

                    val override = overrides[occurrenceStart]
                    if (override != null) {
                        occurrences.add(
                            EventOccurrence(
                                eventId = event[Events.id],
                                calendarId = event[Events.calendarId],
                                title = override[RecurrenceExceptions.overrideTitle] ?: event[Events.title],
                                description = override[RecurrenceExceptions.overrideDescription] ?: event[Events.description],
                                location = override[RecurrenceExceptions.overrideLocation] ?: event[Events.location],
                                startTime = (override[RecurrenceExceptions.overrideStartTime] ?: occurrenceStart).toString(),
                                endTime = (override[RecurrenceExceptions.overrideEndTime] ?: occurrenceStart.plus(eventDuration)).toString(),
                                allDay = event[Events.allDay],
                                status = event[Events.status],
                                isRecurring = true,
                                isException = true,
                                originalDate = occurrenceStart.toString(),
                            )
                        )
                    } else {
                        val occurrenceEnd = occurrenceStart.plus(eventDuration)
                        occurrences.add(
                            EventOccurrence(
                                eventId = event[Events.id],
                                calendarId = event[Events.calendarId],
                                title = event[Events.title],
                                description = event[Events.description],
                                location = event[Events.location],
                                startTime = occurrenceStart.toString(),
                                endTime = occurrenceEnd.toString(),
                                allDay = event[Events.allDay],
                                status = event[Events.status],
                                isRecurring = true,
                                isException = false,
                                originalDate = occurrenceStart.toString(),
                            )
                        )
                    }
                }
            }
        }

        occurrences.sortBy { it.startTime }
        occurrences
    }

    private fun ResultRow.toOccurrence(isRecurring: Boolean) = EventOccurrence(
        eventId = this[Events.id],
        calendarId = this[Events.calendarId],
        title = this[Events.title],
        description = this[Events.description],
        location = this[Events.location],
        startTime = this[Events.startTime].toString(),
        endTime = this[Events.endTime].toString(),
        allDay = this[Events.allDay],
        status = this[Events.status],
        isRecurring = isRecurring,
    )
}

package com.cristianllanos.calendarengine.modules.event

import com.cristianllanos.calendarengine.dto.*
import com.cristianllanos.calendarengine.models.Events
import com.cristianllanos.calendarengine.models.RecurrenceRules
import com.cristianllanos.calendarengine.modules.event.actions.*
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

class EventService(
    private val createEventAction: CreateEventAction,
    private val updateEventAction: UpdateEventAction,
    private val deleteEventAction: DeleteEventAction,
    private val expandOccurrencesAction: ExpandOccurrencesAction,
    private val updateOccurrenceAction: UpdateOccurrenceAction,
    private val iCalExporter: ICalExporter,
) {

    fun listOccurrences(
        calendarId: Int,
        tenantId: Int,
        start: String,
        end: String,
    ): List<EventOccurrence> {
        val rangeStart = LocalDateTime.parse(start)
        val rangeEnd = LocalDateTime.parse(end)
        return expandOccurrencesAction.execute(calendarId, tenantId, rangeStart, rangeEnd)
    }

    fun create(calendarId: Int, tenantId: Int, request: CreateEventRequest): EventResponse {
        return createEventAction.execute(calendarId, tenantId, request)
    }

    fun getById(eventId: Int, calendarId: Int, tenantId: Int): EventResponse = transaction {
        val row = Events.selectAll()
            .where { (Events.id eq eventId) and (Events.calendarId eq calendarId) and (Events.tenantId eq tenantId) }
            .firstOrNull() ?: throw NoSuchElementException("Event not found")

        val recurrenceRule = row[Events.recurrenceRuleId]?.let { ruleId ->
            RecurrenceRules.selectAll().where { RecurrenceRules.id eq ruleId }.firstOrNull()?.let { rule ->
                RecurrenceRuleResponse(
                    id = rule[RecurrenceRules.id],
                    rrule = rule[RecurrenceRules.rrule],
                    dtstart = rule[RecurrenceRules.dtstart].toString(),
                    timezone = rule[RecurrenceRules.timezone],
                )
            }
        }

        row.toEventResponse(recurrenceRule)
    }

    fun update(eventId: Int, calendarId: Int, tenantId: Int, request: UpdateEventRequest): EventResponse {
        return updateEventAction.execute(eventId, calendarId, tenantId, request)
    }

    fun updateOccurrence(
        eventId: Int,
        calendarId: Int,
        tenantId: Int,
        occurrenceDate: String,
        request: UpdateOccurrenceRequest,
    ): EventOccurrence {
        return updateOccurrenceAction.execute(eventId, calendarId, tenantId, occurrenceDate, request)
    }

    fun delete(eventId: Int, calendarId: Int, tenantId: Int, scope: DeleteScope, occurrenceDate: String?) {
        deleteEventAction.execute(eventId, calendarId, tenantId, scope, occurrenceDate)
    }

    fun exportICal(calendarId: Int, tenantId: Int): String {
        return iCalExporter.export(calendarId, tenantId)
    }
}

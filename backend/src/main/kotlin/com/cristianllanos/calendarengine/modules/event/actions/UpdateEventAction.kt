package com.cristianllanos.calendarengine.modules.event.actions

import com.cristianllanos.calendarengine.dto.EventResponse
import com.cristianllanos.calendarengine.dto.RecurrenceRuleResponse
import com.cristianllanos.calendarengine.dto.UpdateEventRequest
import com.cristianllanos.calendarengine.models.Events
import com.cristianllanos.calendarengine.models.RecurrenceRules
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

/** Partially updates an existing calendar event's properties. */
class UpdateEventAction {

    /** Applies the non-null fields from the request to the event and returns the updated result. */
    fun execute(eventId: Int, calendarId: Int, tenantId: Int, request: UpdateEventRequest): EventResponse = transaction {
        Events.selectAll()
            .where { (Events.id eq eventId) and (Events.calendarId eq calendarId) and (Events.tenantId eq tenantId) }
            .firstOrNull() ?: throw NoSuchElementException("Event not found")

        val now = LocalDateTime.now()

        Events.update({ (Events.id eq eventId) and (Events.tenantId eq tenantId) }) {
            request.title?.let { title -> it[Events.title] = title }
            request.description?.let { desc -> it[Events.description] = desc }
            request.location?.let { loc -> it[Events.location] = loc }
            request.startTime?.let { st -> it[Events.startTime] = LocalDateTime.parse(st) }
            request.endTime?.let { et -> it[Events.endTime] = LocalDateTime.parse(et) }
            request.allDay?.let { ad -> it[Events.allDay] = ad }
            request.status?.let { status -> it[Events.status] = status }
            it[Events.updatedAt] = now
        }

        val updated = Events.selectAll()
            .where { Events.id eq eventId }
            .first()

        val recurrenceRule = updated[Events.recurrenceRuleId]?.let { ruleId ->
            RecurrenceRules.selectAll().where { RecurrenceRules.id eq ruleId }.firstOrNull()?.let { row ->
                RecurrenceRuleResponse(
                    id = row[RecurrenceRules.id],
                    rrule = row[RecurrenceRules.rrule],
                    dtstart = row[RecurrenceRules.dtstart].toString(),
                    timezone = row[RecurrenceRules.timezone],
                )
            }
        }

        updated.toEventResponse(recurrenceRule)
    }
}

internal fun ResultRow.toEventResponse(recurrenceRule: RecurrenceRuleResponse? = null) = EventResponse(
    id = this[Events.id],
    calendarId = this[Events.calendarId],
    title = this[Events.title],
    description = this[Events.description],
    location = this[Events.location],
    startTime = this[Events.startTime].toString(),
    endTime = this[Events.endTime].toString(),
    allDay = this[Events.allDay],
    status = this[Events.status],
    isRecurring = this[Events.recurrenceRuleId] != null,
    recurrenceRule = recurrenceRule,
    createdBy = this[Events.createdBy],
    createdAt = this[Events.createdAt].toString(),
    updatedAt = this[Events.updatedAt].toString(),
)

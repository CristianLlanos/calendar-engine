package com.cristianllanos.calendarengine.modules.event.actions

import com.cristianllanos.calendarengine.dto.CreateEventRequest
import com.cristianllanos.calendarengine.dto.EventResponse
import com.cristianllanos.calendarengine.dto.RecurrenceRuleResponse
import com.cristianllanos.calendarengine.models.Calendars
import com.cristianllanos.calendarengine.models.Events
import com.cristianllanos.calendarengine.models.RecurrenceRules
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

class CreateEventAction {

    fun execute(calendarId: Int, tenantId: Int, request: CreateEventRequest): EventResponse = transaction {
        // Verify calendar belongs to tenant
        val calendar = Calendars.selectAll()
            .where { (Calendars.id eq calendarId) and (Calendars.tenantId eq tenantId) }
            .firstOrNull() ?: throw NoSuchElementException("Calendar not found")

        val now = LocalDateTime.now()
        val startTime = LocalDateTime.parse(request.startTime)
        val endTime = LocalDateTime.parse(request.endTime)

        if (endTime.isBefore(startTime) || endTime.isEqual(startTime)) {
            throw IllegalArgumentException("End time must be after start time")
        }

        // Create recurrence rule if provided
        var recurrenceRuleId: Int? = null
        var recurrenceRuleResponse: RecurrenceRuleResponse? = null

        request.recurrence?.let { recurrence ->
            val tz = recurrence.timezone ?: calendar[Calendars.timezone]
            val ruleId = RecurrenceRules.insert {
                it[rrule] = recurrence.rrule
                it[dtstart] = startTime
                it[timezone] = tz
            } get RecurrenceRules.id

            recurrenceRuleId = ruleId
            recurrenceRuleResponse = RecurrenceRuleResponse(
                id = ruleId,
                rrule = recurrence.rrule,
                dtstart = startTime.toString(),
                timezone = tz,
            )
        }

        val eventId = Events.insert {
            it[Events.tenantId] = tenantId
            it[Events.calendarId] = calendarId
            it[title] = request.title
            it[description] = request.description
            it[location] = request.location
            it[Events.startTime] = startTime
            it[Events.endTime] = endTime
            it[allDay] = request.allDay
            it[status] = request.status
            it[Events.recurrenceRuleId] = recurrenceRuleId
            it[createdAt] = now
            it[updatedAt] = now
        } get Events.id

        EventResponse(
            id = eventId,
            calendarId = calendarId,
            title = request.title,
            description = request.description,
            location = request.location,
            startTime = startTime.toString(),
            endTime = endTime.toString(),
            allDay = request.allDay,
            status = request.status,
            isRecurring = recurrenceRuleId != null,
            recurrenceRule = recurrenceRuleResponse,
            createdAt = now.toString(),
            updatedAt = now.toString(),
        )
    }
}

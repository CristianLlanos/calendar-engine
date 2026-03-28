package com.calendarengine.modules.event.actions

import com.calendarengine.dto.EventOccurrence
import com.calendarengine.dto.UpdateOccurrenceRequest
import com.calendarengine.models.Events
import com.calendarengine.models.RecurrenceExceptions
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Duration
import java.time.LocalDateTime

class UpdateOccurrenceAction {

    fun execute(
        eventId: Int,
        calendarId: Int,
        tenantId: Int,
        occurrenceDate: String,
        request: UpdateOccurrenceRequest,
    ): EventOccurrence = transaction {
        val event = Events.selectAll()
            .where { (Events.id eq eventId) and (Events.calendarId eq calendarId) and (Events.tenantId eq tenantId) }
            .firstOrNull() ?: throw NoSuchElementException("Event not found")

        if (event[Events.recurrenceRuleId] == null) {
            throw IllegalArgumentException("Cannot update occurrence of a non-recurring event")
        }

        val originalDate = LocalDateTime.parse(occurrenceDate)
        val eventDuration = Duration.between(event[Events.startTime], event[Events.endTime])

        // Check if an exception already exists for this date
        val existing = RecurrenceExceptions.selectAll()
            .where { (RecurrenceExceptions.eventId eq eventId) and (RecurrenceExceptions.originalDate eq originalDate) }
            .firstOrNull()

        if (existing != null) {
            // Update the existing exception
            RecurrenceExceptions.update({
                (RecurrenceExceptions.eventId eq eventId) and (RecurrenceExceptions.originalDate eq originalDate)
            }) {
                it[isExcluded] = false
                request.title?.let { title -> it[overrideTitle] = title }
                request.description?.let { desc -> it[overrideDescription] = desc }
                request.location?.let { loc -> it[overrideLocation] = loc }
                request.startTime?.let { st -> it[overrideStartTime] = LocalDateTime.parse(st) }
                request.endTime?.let { et -> it[overrideEndTime] = LocalDateTime.parse(et) }
            }
        } else {
            // Create a new exception
            RecurrenceExceptions.insert {
                it[RecurrenceExceptions.eventId] = eventId
                it[RecurrenceExceptions.originalDate] = originalDate
                it[isExcluded] = false
                it[overrideTitle] = request.title
                it[overrideDescription] = request.description
                it[overrideLocation] = request.location
                it[overrideStartTime] = request.startTime?.let { st -> LocalDateTime.parse(st) }
                it[overrideEndTime] = request.endTime?.let { et -> LocalDateTime.parse(et) }
            }
        }

        val overrideStartTime = request.startTime?.let { LocalDateTime.parse(it) } ?: originalDate
        val overrideEndTime = request.endTime?.let { LocalDateTime.parse(it) } ?: overrideStartTime.plus(eventDuration)

        EventOccurrence(
            eventId = eventId,
            calendarId = calendarId,
            title = request.title ?: event[Events.title],
            description = request.description ?: event[Events.description],
            location = request.location ?: event[Events.location],
            startTime = overrideStartTime.toString(),
            endTime = overrideEndTime.toString(),
            allDay = event[Events.allDay],
            status = event[Events.status],
            isRecurring = true,
            isException = true,
            originalDate = originalDate.toString(),
        )
    }
}

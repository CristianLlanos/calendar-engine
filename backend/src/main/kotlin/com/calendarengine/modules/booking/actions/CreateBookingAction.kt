package com.calendarengine.modules.booking.actions

import com.calendarengine.dto.*
import com.calendarengine.models.Bookings
import com.calendarengine.models.BookingUrls
import com.calendarengine.models.Events
import com.calendarengine.models.enums.BookingStatus
import com.cristianllanos.events.Emitter
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate
import java.time.LocalDateTime

class CreateBookingAction(
    private val calculateAvailabilityAction: CalculateAvailabilityAction,
    private val emitter: Emitter,
) {

    fun execute(request: CreateBookingRequest): BookingResponse = transaction {
        // Load booking URL
        val urlRow = BookingUrls.selectAll()
            .where { BookingUrls.id eq request.bookingUrlId }
            .firstOrNull() ?: throw NoSuchElementException("Booking URL not found")

        val tenantId = urlRow[BookingUrls.tenantId]
        val calendarId = urlRow[BookingUrls.calendarId]
        val autoConfirm = urlRow[BookingUrls.autoConfirm]

        // Parse times
        val startTime = LocalDateTime.parse(request.startTime)
        val endTime = startTime.plusMinutes(request.durationMinutes.toLong())

        // Validate the slot is actually available
        val dayAvailability = calculateAvailabilityAction.forDate(request.bookingUrlId, startTime.toLocalDate())
        val slotAvailable = dayAvailability.slots.any { slot ->
            slot.startTime == startTime.toString() && request.durationMinutes in slot.availableDurations
        }

        if (!slotAvailable) {
            throw IllegalArgumentException("The requested time slot is not available")
        }

        val now = LocalDateTime.now()
        val status = if (autoConfirm) BookingStatus.CONFIRMED.name else BookingStatus.PENDING_APPROVAL.name

        // Auto-create a calendar event for this booking
        val eventId = Events.insert {
            it[Events.tenantId] = tenantId
            it[Events.calendarId] = calendarId
            it[title] = "Booking: ${request.bookerName}"
            it[description] = request.notes
            it[Events.startTime] = startTime
            it[Events.endTime] = endTime
            it[allDay] = false
            it[Events.status] = "CONFIRMED"
            it[createdAt] = now
            it[updatedAt] = now
        } get Events.id

        // Create the booking
        val bookingId = Bookings.insert {
            it[Bookings.tenantId] = tenantId
            it[bookingUrlId] = request.bookingUrlId
            it[Bookings.calendarId] = calendarId
            it[Bookings.eventId] = eventId
            it[Bookings.status] = status
            it[Bookings.startTime] = startTime
            it[Bookings.endTime] = endTime
            it[bookerName] = request.bookerName
            it[bookerEmail] = request.bookerEmail
            it[bookerPhone] = request.bookerPhone
            it[bookerUserId] = request.bookerUserId
            it[notes] = request.notes
            it[createdAt] = now
        } get Bookings.id

        val response = BookingResponse(
            id = bookingId,
            tenantId = tenantId,
            bookingUrlId = request.bookingUrlId,
            calendarId = calendarId,
            eventId = eventId,
            status = status,
            startTime = startTime.toString(),
            endTime = endTime.toString(),
            bookerName = request.bookerName,
            bookerEmail = request.bookerEmail,
            bookerPhone = request.bookerPhone,
            bookerUserId = request.bookerUserId,
            notes = request.notes,
            createdAt = now.toString(),
        )

        emitter.emit(BookingCreatedEvent(tenantId, response))

        response
    }
}

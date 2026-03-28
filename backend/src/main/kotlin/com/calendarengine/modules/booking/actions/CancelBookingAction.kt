package com.calendarengine.modules.booking.actions

import com.calendarengine.dto.BookingResponse
import com.calendarengine.dto.CalendarSystemEvent
import com.calendarengine.models.Bookings
import com.calendarengine.models.Events
import com.calendarengine.models.enums.BookingStatus
import com.calendarengine.modules.notification.SystemEventEmitter
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

class CancelBookingAction(
    private val systemEventEmitter: SystemEventEmitter,
) {

    fun execute(bookingId: Int, tenantId: Int, reason: String?): BookingResponse = transaction {
        val booking = Bookings.selectAll()
            .where { (Bookings.id eq bookingId) and (Bookings.tenantId eq tenantId) }
            .firstOrNull() ?: throw NoSuchElementException("Booking not found")

        if (booking[Bookings.status] == BookingStatus.CANCELLED.name) {
            throw IllegalArgumentException("Booking is already cancelled")
        }

        val now = LocalDateTime.now()

        Bookings.update({ Bookings.id eq bookingId }) {
            it[status] = BookingStatus.CANCELLED.name
            it[cancelledAt] = now
            it[cancellationReason] = reason
        }

        // Remove the auto-created calendar event
        booking[Bookings.eventId]?.let { eventId ->
            Events.deleteWhere { Events.id eq eventId }
        }

        val response = BookingResponse(
            id = bookingId,
            tenantId = tenantId,
            bookingUrlId = booking[Bookings.bookingUrlId],
            calendarId = booking[Bookings.calendarId],
            eventId = null,
            status = BookingStatus.CANCELLED.name,
            startTime = booking[Bookings.startTime].toString(),
            endTime = booking[Bookings.endTime].toString(),
            bookerName = booking[Bookings.bookerName],
            bookerEmail = booking[Bookings.bookerEmail],
            bookerPhone = booking[Bookings.bookerPhone],
            bookerUserId = booking[Bookings.bookerUserId],
            notes = booking[Bookings.notes],
            cancelledAt = now.toString(),
            cancellationReason = reason,
            createdAt = booking[Bookings.createdAt].toString(),
        )

        systemEventEmitter.emit(tenantId, CalendarSystemEvent.BookingCancelled(response))

        response
    }
}

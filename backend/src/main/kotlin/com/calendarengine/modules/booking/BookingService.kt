package com.calendarengine.modules.booking

import com.calendarengine.dto.*
import com.calendarengine.models.Bookings
import com.calendarengine.modules.booking.actions.CancelBookingAction
import com.calendarengine.modules.booking.actions.CreateBookingAction
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

class BookingService(
    private val createBookingAction: CreateBookingAction,
    private val cancelBookingAction: CancelBookingAction,
) {

    companion object {
        private val ALLOWED_SORT_COLUMNS: Map<String, Expression<*>> = mapOf(
            "startTime" to Bookings.startTime,
            "createdAt" to Bookings.createdAt,
            "bookerName" to Bookings.bookerName,
        )
    }

    fun getAll(tenantId: Int, params: PaginationParams): PaginatedResponse<BookingResponse> = transaction {
        val condition = buildSearchCondition(
            Bookings.tenantId, tenantId, params.search,
            listOf(Bookings.bookerName, Bookings.bookerEmail),
        )
        val total = Bookings.selectAll().where { condition }.count()

        paginate(
            query = Bookings.selectAll().where { condition },
            total = total,
            params = params,
            allowedSortColumns = ALLOWED_SORT_COLUMNS,
            defaultSortColumn = Bookings.createdAt,
        ) { it.toBookingResponse() }
    }

    fun getById(bookingId: Int, tenantId: Int): BookingResponse = transaction {
        Bookings.selectAll()
            .where { (Bookings.id eq bookingId) and (Bookings.tenantId eq tenantId) }
            .firstOrNull()?.toBookingResponse()
            ?: throw NoSuchElementException("Booking not found")
    }

    fun create(request: CreateBookingRequest): BookingResponse {
        return createBookingAction.execute(request)
    }

    fun cancel(bookingId: Int, tenantId: Int, reason: String?): BookingResponse {
        return cancelBookingAction.execute(bookingId, tenantId, reason)
    }

    private fun ResultRow.toBookingResponse() = BookingResponse(
        id = this[Bookings.id],
        tenantId = this[Bookings.tenantId],
        bookingUrlId = this[Bookings.bookingUrlId],
        calendarId = this[Bookings.calendarId],
        eventId = this[Bookings.eventId],
        status = this[Bookings.status],
        startTime = this[Bookings.startTime].toString(),
        endTime = this[Bookings.endTime].toString(),
        bookerName = this[Bookings.bookerName],
        bookerEmail = this[Bookings.bookerEmail],
        bookerPhone = this[Bookings.bookerPhone],
        bookerUserId = this[Bookings.bookerUserId],
        notes = this[Bookings.notes],
        cancelledAt = this[Bookings.cancelledAt]?.toString(),
        cancellationReason = this[Bookings.cancellationReason],
        createdAt = this[Bookings.createdAt].toString(),
    )
}

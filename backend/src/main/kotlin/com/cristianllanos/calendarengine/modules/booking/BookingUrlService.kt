package com.cristianllanos.calendarengine.modules.booking

import com.cristianllanos.calendarengine.dto.*
import com.cristianllanos.calendarengine.models.Bookings
import com.cristianllanos.calendarengine.models.BookingUrlAvailabilityWindows
import com.cristianllanos.calendarengine.models.BookingUrls
import com.cristianllanos.calendarengine.models.Calendars
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.time.LocalTime

/** CRUD operations for booking URL configuration, including availability windows. */
class BookingUrlService {

    companion object {
        private val ALLOWED_SORT_COLUMNS: Map<String, Expression<*>> = mapOf(
            "name" to BookingUrls.name,
            "createdAt" to BookingUrls.createdAt,
        )
    }

    /** Returns a paginated list of booking URLs with their availability windows. */
    fun getAll(tenantId: Int, params: PaginationParams): PaginatedResponse<BookingUrlResponse> = transaction {
        val condition = buildSearchCondition(
            BookingUrls.tenantId, tenantId, params.search,
            listOf(BookingUrls.name, BookingUrls.slug),
        )
        val total = BookingUrls.selectAll().where { condition }.count()

        val response = paginate(
            query = BookingUrls.selectAll().where { condition },
            total = total,
            params = params,
            allowedSortColumns = ALLOWED_SORT_COLUMNS,
            defaultSortColumn = BookingUrls.id,
        ) { it.toBookingUrlResponse() }

        // Load availability windows for all booking URLs in one query
        val urlIds = response.items.map { it.id }
        val windowsByUrl = loadWindowsForUrls(urlIds)

        response.copy(
            items = response.items.map { url ->
                url.copy(availabilityWindows = windowsByUrl[url.id] ?: emptyList())
            }
        )
    }

    /** Retrieves a single booking URL by ID, including its availability windows. */
    fun getById(id: Int, tenantId: Int): BookingUrlResponse = transaction {
        val row = BookingUrls.selectAll()
            .where { (BookingUrls.id eq id) and (BookingUrls.tenantId eq tenantId) }
            .firstOrNull() ?: throw NoSuchElementException("Booking URL not found")

        val windows = loadWindowsForUrls(listOf(id))
        row.toBookingUrlResponse().copy(availabilityWindows = windows[id] ?: emptyList())
    }

    /** Creates a new booking URL with availability windows, enforcing unique slug per tenant. */
    fun create(tenantId: Int, request: CreateBookingUrlRequest): BookingUrlResponse = transaction {
        // Verify calendar belongs to tenant
        Calendars.selectAll()
            .where { (Calendars.id eq request.calendarId) and (Calendars.tenantId eq tenantId) }
            .firstOrNull() ?: throw NoSuchElementException("Calendar not found")

        // Check slug uniqueness within tenant
        val existing = BookingUrls.selectAll()
            .where { (BookingUrls.tenantId eq tenantId) and (BookingUrls.slug eq request.slug) }
            .firstOrNull()
        if (existing != null) throw IllegalArgumentException("Booking URL slug already exists for this tenant")

        val now = LocalDateTime.now()
        val id = BookingUrls.insert {
            it[BookingUrls.tenantId] = tenantId
            it[calendarId] = request.calendarId
            it[slug] = request.slug
            it[name] = request.name
            it[description] = request.description
            it[durationMinutes] = request.durationMinutes
            it[durationOptions] = request.durationOptions
            it[bufferBeforeMinutes] = request.bufferBeforeMinutes
            it[bufferAfterMinutes] = request.bufferAfterMinutes
            it[minLeadTimeHours] = request.minLeadTimeHours
            it[maxLeadTimeDays] = request.maxLeadTimeDays
            it[maxBookingsPerDay] = request.maxBookingsPerDay
            it[maxBookingsPerWeek] = request.maxBookingsPerWeek
            it[autoConfirm] = request.autoConfirm
            it[createdAt] = now
        } get BookingUrls.id

        // Create availability windows
        val windows = insertWindows(id, request.availabilityWindows)

        BookingUrls.selectAll().where { BookingUrls.id eq id }.first()
            .toBookingUrlResponse()
            .copy(availabilityWindows = windows)
    }

    /** Partially updates a booking URL; replaces availability windows if provided. */
    fun update(id: Int, tenantId: Int, request: UpdateBookingUrlRequest): BookingUrlResponse = transaction {
        // Check slug uniqueness if changing
        request.slug?.let { newSlug ->
            val existing = BookingUrls.selectAll()
                .where { (BookingUrls.tenantId eq tenantId) and (BookingUrls.slug eq newSlug) and (BookingUrls.id neq id) }
                .firstOrNull()
            if (existing != null) throw IllegalArgumentException("Booking URL slug already exists for this tenant")
        }

        BookingUrls.update({ (BookingUrls.id eq id) and (BookingUrls.tenantId eq tenantId) }) {
            request.slug?.let { slug -> it[BookingUrls.slug] = slug }
            request.name?.let { name -> it[BookingUrls.name] = name }
            request.description?.let { desc -> it[BookingUrls.description] = desc }
            request.status?.let { status -> it[BookingUrls.status] = status }
            request.durationMinutes?.let { dur -> it[BookingUrls.durationMinutes] = dur }
            request.durationOptions?.let { opts -> it[BookingUrls.durationOptions] = opts }
            request.bufferBeforeMinutes?.let { buf -> it[BookingUrls.bufferBeforeMinutes] = buf }
            request.bufferAfterMinutes?.let { buf -> it[BookingUrls.bufferAfterMinutes] = buf }
            request.minLeadTimeHours?.let { lead -> it[BookingUrls.minLeadTimeHours] = lead }
            request.maxLeadTimeDays?.let { lead -> it[BookingUrls.maxLeadTimeDays] = lead }
            request.maxBookingsPerDay?.let { max -> it[BookingUrls.maxBookingsPerDay] = max }
            request.maxBookingsPerWeek?.let { max -> it[BookingUrls.maxBookingsPerWeek] = max }
            request.autoConfirm?.let { ac -> it[BookingUrls.autoConfirm] = ac }
        }

        // Replace availability windows if provided
        request.availabilityWindows?.let { newWindows ->
            BookingUrlAvailabilityWindows.deleteWhere { bookingUrlId eq id }
            insertWindows(id, newWindows)
        }

        val row = BookingUrls.selectAll()
            .where { (BookingUrls.id eq id) and (BookingUrls.tenantId eq tenantId) }
            .firstOrNull() ?: throw NoSuchElementException("Booking URL not found")

        val windows = loadWindowsForUrls(listOf(id))
        row.toBookingUrlResponse().copy(availabilityWindows = windows[id] ?: emptyList())
    }

    /** Deletes a booking URL, failing if it has existing bookings. */
    fun delete(id: Int, tenantId: Int) = transaction {
        deleteWithConflictChecks(
            table = BookingUrls, idColumn = BookingUrls.id, id = id,
            tenantIdColumn = BookingUrls.tenantId, tenantId = tenantId,
            notFoundMessage = "Booking URL not found",
            conflictChecks = listOf(
                DeleteConflictCheck(Bookings, Bookings.bookingUrlId, "booking URL has {count} booking(s)"),
            ),
        )
    }

    private fun insertWindows(bookingUrlId: Int, windows: List<CreateAvailabilityWindowRequest>): List<AvailabilityWindowResponse> {
        return windows.map { window ->
            val windowId = BookingUrlAvailabilityWindows.insert {
                it[BookingUrlAvailabilityWindows.bookingUrlId] = bookingUrlId
                it[dayOfWeek] = window.dayOfWeek
                it[startTime] = LocalTime.parse(window.startTime)
                it[endTime] = LocalTime.parse(window.endTime)
            } get BookingUrlAvailabilityWindows.id

            AvailabilityWindowResponse(
                id = windowId,
                dayOfWeek = window.dayOfWeek,
                startTime = window.startTime,
                endTime = window.endTime,
            )
        }
    }

    private fun loadWindowsForUrls(urlIds: List<Int>): Map<Int, List<AvailabilityWindowResponse>> {
        if (urlIds.isEmpty()) return emptyMap()

        return BookingUrlAvailabilityWindows.selectAll()
            .where { BookingUrlAvailabilityWindows.bookingUrlId inList urlIds }
            .groupBy { it[BookingUrlAvailabilityWindows.bookingUrlId] }
            .mapValues { (_, rows) ->
                rows.map { row ->
                    AvailabilityWindowResponse(
                        id = row[BookingUrlAvailabilityWindows.id],
                        dayOfWeek = row[BookingUrlAvailabilityWindows.dayOfWeek],
                        startTime = row[BookingUrlAvailabilityWindows.startTime].toString(),
                        endTime = row[BookingUrlAvailabilityWindows.endTime].toString(),
                    )
                }
            }
    }

    private fun ResultRow.toBookingUrlResponse() = BookingUrlResponse(
        id = this[BookingUrls.id],
        tenantId = this[BookingUrls.tenantId],
        calendarId = this[BookingUrls.calendarId],
        slug = this[BookingUrls.slug],
        name = this[BookingUrls.name],
        description = this[BookingUrls.description],
        status = this[BookingUrls.status],
        durationMinutes = this[BookingUrls.durationMinutes],
        durationOptions = this[BookingUrls.durationOptions],
        bufferBeforeMinutes = this[BookingUrls.bufferBeforeMinutes],
        bufferAfterMinutes = this[BookingUrls.bufferAfterMinutes],
        minLeadTimeHours = this[BookingUrls.minLeadTimeHours],
        maxLeadTimeDays = this[BookingUrls.maxLeadTimeDays],
        maxBookingsPerDay = this[BookingUrls.maxBookingsPerDay],
        maxBookingsPerWeek = this[BookingUrls.maxBookingsPerWeek],
        autoConfirm = this[BookingUrls.autoConfirm],
        createdAt = this[BookingUrls.createdAt].toString(),
    )
}

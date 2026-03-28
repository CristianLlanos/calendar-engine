package com.calendarengine.modules.booking.actions

import com.calendarengine.dto.AvailableSlot
import com.calendarengine.dto.DayAvailability
import com.calendarengine.models.*
import com.calendarengine.models.enums.BookingStatus
import com.calendarengine.modules.event.RruleExpander
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.*

class CalculateAvailabilityAction(
    private val rruleExpander: RruleExpander,
) {

    fun forDate(bookingUrlId: Int, date: LocalDate): DayAvailability {
        val slots = calculateSlots(bookingUrlId, date)
        return DayAvailability(date = date.toString(), slots = slots)
    }

    fun forRange(bookingUrlId: Int, startDate: LocalDate, endDate: LocalDate): List<DayAvailability> {
        val days = mutableListOf<DayAvailability>()
        var current = startDate
        while (!current.isAfter(endDate)) {
            days.add(forDate(bookingUrlId, current))
            current = current.plusDays(1)
        }
        return days
    }

    private fun calculateSlots(bookingUrlId: Int, date: LocalDate): List<AvailableSlot> = transaction {
        // 1. Load booking URL config
        val urlRow = BookingUrls.selectAll()
            .where { BookingUrls.id eq bookingUrlId }
            .firstOrNull() ?: throw NoSuchElementException("Booking URL not found")

        val calendarId = urlRow[BookingUrls.calendarId]
        val tenantId = urlRow[BookingUrls.tenantId]
        val defaultDuration = urlRow[BookingUrls.durationMinutes]
        val durationOptionsStr = urlRow[BookingUrls.durationOptions]
        val bufferBefore = urlRow[BookingUrls.bufferBeforeMinutes].toLong()
        val bufferAfter = urlRow[BookingUrls.bufferAfterMinutes].toLong()
        val minLeadTimeHours = urlRow[BookingUrls.minLeadTimeHours].toLong()
        val maxLeadTimeDays = urlRow[BookingUrls.maxLeadTimeDays].toLong()
        val maxPerDay = urlRow[BookingUrls.maxBookingsPerDay]
        val maxPerWeek = urlRow[BookingUrls.maxBookingsPerWeek]

        val durations = parseDurationOptions(durationOptionsStr, defaultDuration)
        val maxDuration = durations.max()

        // 2. Load availability windows for this day of week
        val dayOfWeek = date.dayOfWeek.value // 1=Monday..7=Sunday
        val windows = BookingUrlAvailabilityWindows.selectAll()
            .where { (BookingUrlAvailabilityWindows.bookingUrlId eq bookingUrlId) and (BookingUrlAvailabilityWindows.dayOfWeek eq dayOfWeek) }
            .map { row ->
                TimeWindow(
                    start = row[BookingUrlAvailabilityWindows.startTime],
                    end = row[BookingUrlAvailabilityWindows.endTime],
                )
            }

        if (windows.isEmpty()) return@transaction emptyList()

        // 3. Generate candidate slots
        val candidates = mutableListOf<CandidateSlot>()
        for (window in windows) {
            var slotStart = window.start
            while (true) {
                val slotEnd = slotStart.plusMinutes(maxDuration.toLong())
                if (slotEnd.isAfter(window.end)) break

                candidates.add(CandidateSlot(
                    start = date.atTime(slotStart),
                    end = date.atTime(slotEnd),
                ))

                // Advance by smallest duration + buffers
                slotStart = slotStart.plusMinutes(durations.min().toLong() + bufferBefore + bufferAfter)
            }
        }

        if (candidates.isEmpty()) return@transaction emptyList()

        // 4. Load blocking calendar events for the day
        val dayStart = date.atStartOfDay()
        val dayEnd = date.plusDays(1).atStartOfDay()
        val blockingEvents = loadBlockingEvents(calendarId, tenantId, dayStart, dayEnd)

        // 5. Load external busy blocks
        val busyBlocks = loadExternalBusyBlocks(calendarId, dayStart, dayEnd)

        // 6. Load existing bookings
        val existingBookings = loadExistingBookings(bookingUrlId, dayStart, dayEnd)

        // 7. Combine all blocked intervals
        val blockedIntervals = mutableListOf<TimeInterval>()
        blockingEvents.forEach { blockedIntervals.add(it) }
        busyBlocks.forEach { blockedIntervals.add(it) }
        existingBookings.forEach { (start, end) ->
            blockedIntervals.add(TimeInterval(
                start = start.minusMinutes(bufferBefore),
                end = end.plusMinutes(bufferAfter),
            ))
        }

        // 8. Filter by lead time constraints
        val now = LocalDateTime.now()
        val earliestAllowed = now.plusHours(minLeadTimeHours)
        val latestAllowed = now.plusDays(maxLeadTimeDays)

        // 9. Check rate limits
        if (maxPerDay != null) {
            val dayBookingCount = existingBookings.size
            if (dayBookingCount >= maxPerDay) return@transaction emptyList()
        }

        if (maxPerWeek != null) {
            val weekStart = date.with(java.time.DayOfWeek.MONDAY).atStartOfDay()
            val weekEnd = weekStart.plusDays(7)
            val weekBookingCount = loadExistingBookings(bookingUrlId, weekStart, weekEnd).size
            if (weekBookingCount >= maxPerWeek) return@transaction emptyList()
        }

        // 10. Filter candidates and build available slots
        candidates.mapNotNull { candidate ->
            if (candidate.start.isBefore(earliestAllowed)) return@mapNotNull null
            if (candidate.start.isAfter(latestAllowed)) return@mapNotNull null

            // Check which durations fit without overlapping blocked intervals
            val availableDurations = durations.filter { duration ->
                val slotEnd = candidate.start.plusMinutes(duration.toLong())
                val slotWithBuffers = TimeInterval(
                    start = candidate.start.minusMinutes(bufferBefore),
                    end = slotEnd.plusMinutes(bufferAfter),
                )
                blockedIntervals.none { it.overlaps(slotWithBuffers) }
            }

            if (availableDurations.isEmpty()) return@mapNotNull null

            AvailableSlot(
                startTime = candidate.start.toString(),
                endTime = candidate.start.plusMinutes(availableDurations.first().toLong()).toString(),
                availableDurations = availableDurations,
            )
        }
    }

    private fun loadBlockingEvents(
        calendarId: Int,
        tenantId: Int,
        dayStart: LocalDateTime,
        dayEnd: LocalDateTime,
    ): List<TimeInterval> {
        val intervals = mutableListOf<TimeInterval>()

        val events = Events.selectAll().where {
            (Events.calendarId eq calendarId) and (Events.tenantId eq tenantId) and (
                // Non-recurring events in range
                ((Events.recurrenceRuleId.isNull()) and
                    (Events.startTime less dayEnd) and
                    (Events.endTime greater dayStart)) or
                    // Recurring events need expansion
                    (Events.recurrenceRuleId.isNotNull())
                )
        }.toList()

        for (event in events) {
            val recurrenceRuleId = event[Events.recurrenceRuleId]
            if (recurrenceRuleId == null) {
                intervals.add(TimeInterval(event[Events.startTime], event[Events.endTime]))
            } else {
                val rule = RecurrenceRules.selectAll()
                    .where { RecurrenceRules.id eq recurrenceRuleId }
                    .firstOrNull() ?: continue

                val eventDuration = Duration.between(event[Events.startTime], event[Events.endTime])
                val expanded = rruleExpander.expand(
                    rrule = rule[RecurrenceRules.rrule],
                    dtstart = rule[RecurrenceRules.dtstart],
                    timezone = ZoneId.of(rule[RecurrenceRules.timezone]),
                    rangeStart = dayStart,
                    rangeEnd = dayEnd,
                )

                // Check for exclusions
                val exclusions = RecurrenceExceptions.selectAll()
                    .where { (RecurrenceExceptions.eventId eq event[Events.id]) and (RecurrenceExceptions.isExcluded eq true) }
                    .map { it[RecurrenceExceptions.originalDate] }
                    .toSet()

                for (occStart in expanded) {
                    if (occStart in exclusions) continue
                    intervals.add(TimeInterval(occStart, occStart.plus(eventDuration)))
                }
            }
        }
        return intervals
    }

    private fun loadExternalBusyBlocks(
        calendarId: Int,
        dayStart: LocalDateTime,
        dayEnd: LocalDateTime,
    ): List<TimeInterval> {
        return ExternalBusyBlocks.selectAll().where {
            (ExternalBusyBlocks.calendarId eq calendarId) and
                (ExternalBusyBlocks.startTime less dayEnd) and
                (ExternalBusyBlocks.endTime greater dayStart)
        }.map { TimeInterval(it[ExternalBusyBlocks.startTime], it[ExternalBusyBlocks.endTime]) }
    }

    private fun loadExistingBookings(
        bookingUrlId: Int,
        dayStart: LocalDateTime,
        dayEnd: LocalDateTime,
    ): List<Pair<LocalDateTime, LocalDateTime>> {
        return Bookings.selectAll().where {
            (Bookings.bookingUrlId eq bookingUrlId) and
                (Bookings.status neq BookingStatus.CANCELLED.name) and
                (Bookings.startTime less dayEnd) and
                (Bookings.endTime greater dayStart)
        }.map { Pair(it[Bookings.startTime], it[Bookings.endTime]) }
    }

    private fun parseDurationOptions(optionsStr: String?, defaultDuration: Int): List<Int> {
        if (optionsStr.isNullOrBlank()) return listOf(defaultDuration)
        return try {
            optionsStr.trim('[', ']').split(",").map { it.trim().toInt() }.ifEmpty { listOf(defaultDuration) }
        } catch (_: Exception) {
            listOf(defaultDuration)
        }
    }
}

private data class TimeWindow(val start: LocalTime, val end: LocalTime)

private data class CandidateSlot(val start: LocalDateTime, val end: LocalDateTime)

data class TimeInterval(val start: LocalDateTime, val end: LocalDateTime) {
    fun overlaps(other: TimeInterval): Boolean {
        return start.isBefore(other.end) && end.isAfter(other.start)
    }
}

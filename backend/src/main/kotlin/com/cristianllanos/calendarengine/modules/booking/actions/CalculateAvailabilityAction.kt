package com.cristianllanos.calendarengine.modules.booking.actions

import com.cristianllanos.calendarengine.dto.AvailableSlot
import com.cristianllanos.calendarengine.dto.DayAvailability
import com.cristianllanos.calendarengine.models.*
import com.cristianllanos.calendarengine.models.enums.BookingStatus
import com.cristianllanos.calendarengine.modules.event.RruleExpander
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.*

/**
 * Computes available booking slots for a given booking URL and date range.
 *
 * The algorithm works as follows:
 * 1. Loads the booking URL's availability windows for the requested day-of-week.
 * 2. Generates candidate time slots by stepping through each window at the minimum duration interval (plus buffers).
 * 3. Loads all blocking intervals: calendar events (including recurrence expansion), external busy blocks, and existing bookings (with buffer padding).
 * 4. Applies lead-time constraints (min/max) and per-day/per-week booking caps.
 * 5. For each candidate, filters out durations that would overlap any blocked interval, returning only slots with at least one viable duration.
 */
class CalculateAvailabilityAction(
    private val rruleExpander: RruleExpander,
) {

    /** Returns available slots for a single date. */
    fun forDate(bookingUrlId: Int, date: LocalDate): DayAvailability {
        val slots = calculateSlots(bookingUrlId, date)
        return DayAvailability(date = date.toString(), slots = slots)
    }

    /** Returns available slots for each day in the inclusive date range. */
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

        val candidates = mutableListOf<CandidateSlot>()
        for (window in windows) {
            var slotStart = window.start
            while (true) {
                val slotEnd = slotStart.plusMinutes(maxDuration.toLong())
                if (slotEnd.isAfter(window.end)) break

                candidates.add(CandidateSlot(start = date.atTime(slotStart)))

                slotStart = slotStart.plusMinutes(durations.min().toLong() + bufferBefore + bufferAfter)
            }
        }

        if (candidates.isEmpty()) return@transaction emptyList()

        val dayStart = date.atStartOfDay()
        val dayEnd = date.plusDays(1).atStartOfDay()
        val blockingEvents = loadBlockingEvents(calendarId, tenantId, dayStart, dayEnd)

        val busyBlocks = loadExternalBusyBlocks(calendarId, dayStart, dayEnd)

        val existingBookings = loadExistingBookings(bookingUrlId, dayStart, dayEnd)

        val blockedIntervals = mutableListOf<TimeInterval>()
        blockingEvents.forEach { blockedIntervals.add(it) }
        busyBlocks.forEach { blockedIntervals.add(it) }
        existingBookings.forEach { (start, end) ->
            blockedIntervals.add(TimeInterval(
                start = start.minusMinutes(bufferBefore),
                end = end.plusMinutes(bufferAfter),
            ))
        }

        val now = LocalDateTime.now()
        val earliestAllowed = now.plusHours(minLeadTimeHours)
        val latestAllowed = now.plusDays(maxLeadTimeDays)

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
                ((Events.recurrenceRuleId.isNull()) and
                    (Events.startTime less dayEnd) and
                    (Events.endTime greater dayStart)) or
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

private data class CandidateSlot(val start: LocalDateTime)

/** A half-open time interval used for overlap detection in availability calculations. */
data class TimeInterval(val start: LocalDateTime, val end: LocalDateTime) {
    /** Returns true if this interval overlaps with [other]. */
    fun overlaps(other: TimeInterval): Boolean {
        return start.isBefore(other.end) && end.isAfter(other.start)
    }
}

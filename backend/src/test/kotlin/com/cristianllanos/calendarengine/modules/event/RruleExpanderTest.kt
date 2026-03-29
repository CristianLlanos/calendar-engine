package com.cristianllanos.calendarengine.modules.event

import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RruleExpanderTest {

    private val expander = RruleExpander()
    private val utc = ZoneId.of("UTC")
    private val newYork = ZoneId.of("America/New_York")

    @Test
    fun `weekly recurrence on MWF`() {
        val results = expander.expand(
            rrule = "FREQ=WEEKLY;BYDAY=MO,WE,FR",
            dtstart = LocalDateTime.of(2026, 1, 5, 9, 0),  // Monday
            timezone = utc,
            rangeStart = LocalDateTime.of(2026, 1, 5, 0, 0),
            rangeEnd = LocalDateTime.of(2026, 1, 12, 0, 0),
        )

        assertEquals(3, results.size)
        assertEquals(LocalDateTime.of(2026, 1, 5, 9, 0), results[0])   // Mon
        assertEquals(LocalDateTime.of(2026, 1, 7, 9, 0), results[1])   // Wed
        assertEquals(LocalDateTime.of(2026, 1, 9, 9, 0), results[2])   // Fri
    }

    @Test
    fun `daily recurrence with COUNT`() {
        val results = expander.expand(
            rrule = "FREQ=DAILY;COUNT=5",
            dtstart = LocalDateTime.of(2026, 3, 1, 10, 0),
            timezone = utc,
            rangeStart = LocalDateTime.of(2026, 3, 1, 0, 0),
            rangeEnd = LocalDateTime.of(2026, 3, 31, 0, 0),
        )

        assertEquals(5, results.size)
        assertEquals(LocalDateTime.of(2026, 3, 1, 10, 0), results[0])
        assertEquals(LocalDateTime.of(2026, 3, 5, 10, 0), results[4])
    }

    @Test
    fun `monthly recurrence on 15th`() {
        val results = expander.expand(
            rrule = "FREQ=MONTHLY;BYMONTHDAY=15",
            dtstart = LocalDateTime.of(2026, 1, 15, 14, 0),
            timezone = utc,
            rangeStart = LocalDateTime.of(2026, 1, 1, 0, 0),
            rangeEnd = LocalDateTime.of(2026, 7, 1, 0, 0),
        )

        assertEquals(6, results.size)
        // Jan 15, Feb 15, Mar 15, Apr 15, May 15, Jun 15
        assertEquals(15, results[0].dayOfMonth)
        assertEquals(1, results[0].monthValue)
        assertEquals(15, results[5].dayOfMonth)
        assertEquals(6, results[5].monthValue)
    }

    @Test
    fun `weekly with UNTIL`() {
        val results = expander.expand(
            rrule = "FREQ=WEEKLY;BYDAY=TU;UNTIL=20260131T235959Z",
            dtstart = LocalDateTime.of(2026, 1, 6, 11, 0),  // Tuesday
            timezone = utc,
            rangeStart = LocalDateTime.of(2026, 1, 1, 0, 0),
            rangeEnd = LocalDateTime.of(2026, 12, 31, 0, 0),
        )

        // Tuesdays in Jan 2026: 6, 13, 20, 27
        assertEquals(4, results.size)
        assertTrue(results.all { it.dayOfWeek.value == 2 })  // Tuesday
        assertTrue(results.all { it.monthValue == 1 })
    }

    @Test
    fun `range filtering only returns occurrences in range`() {
        val results = expander.expand(
            rrule = "FREQ=DAILY",
            dtstart = LocalDateTime.of(2026, 1, 1, 9, 0),
            timezone = utc,
            rangeStart = LocalDateTime.of(2026, 3, 1, 0, 0),
            rangeEnd = LocalDateTime.of(2026, 3, 4, 0, 0),
        )

        assertEquals(3, results.size)
        assertEquals(LocalDateTime.of(2026, 3, 1, 9, 0), results[0])
        assertEquals(LocalDateTime.of(2026, 3, 2, 9, 0), results[1])
        assertEquals(LocalDateTime.of(2026, 3, 3, 9, 0), results[2])
    }

    @Test
    fun `max occurrences limit is respected`() {
        val results = expander.expand(
            rrule = "FREQ=DAILY",
            dtstart = LocalDateTime.of(2026, 1, 1, 9, 0),
            timezone = utc,
            rangeStart = LocalDateTime.of(2026, 1, 1, 0, 0),
            rangeEnd = LocalDateTime.of(2028, 1, 1, 0, 0),
            maxOccurrences = 10,
        )

        assertEquals(10, results.size)
    }

    @Test
    fun `timezone-aware expansion`() {
        val results = expander.expand(
            rrule = "FREQ=WEEKLY;BYDAY=MO;COUNT=2",
            dtstart = LocalDateTime.of(2026, 1, 5, 9, 0),
            timezone = newYork,
            rangeStart = LocalDateTime.of(2026, 1, 1, 0, 0),
            rangeEnd = LocalDateTime.of(2026, 2, 1, 0, 0),
        )

        assertEquals(2, results.size)
        assertEquals(LocalDateTime.of(2026, 1, 5, 9, 0), results[0])
        assertEquals(LocalDateTime.of(2026, 1, 12, 9, 0), results[1])
    }

    @Test
    fun `every other week (INTERVAL=2)`() {
        val results = expander.expand(
            rrule = "FREQ=WEEKLY;INTERVAL=2;BYDAY=MO",
            dtstart = LocalDateTime.of(2026, 1, 5, 9, 0),
            timezone = utc,
            rangeStart = LocalDateTime.of(2026, 1, 1, 0, 0),
            rangeEnd = LocalDateTime.of(2026, 2, 28, 0, 0),
        )

        // Jan 5, Jan 19, Feb 2, Feb 16
        assertEquals(4, results.size)
        assertEquals(5, results[0].dayOfMonth)
        assertEquals(19, results[1].dayOfMonth)
        assertEquals(2, results[2].dayOfMonth)
        assertEquals(16, results[3].dayOfMonth)
    }

    @Test
    fun `yearly recurrence`() {
        val results = expander.expand(
            rrule = "FREQ=YEARLY;BYMONTH=3;BYMONTHDAY=15",
            dtstart = LocalDateTime.of(2026, 3, 15, 10, 0),
            timezone = utc,
            rangeStart = LocalDateTime.of(2026, 1, 1, 0, 0),
            rangeEnd = LocalDateTime.of(2030, 1, 1, 0, 0),
        )

        assertEquals(4, results.size)
        assertTrue(results.all { it.monthValue == 3 && it.dayOfMonth == 15 })
    }

    @Test
    fun `empty result when range is before dtstart`() {
        val results = expander.expand(
            rrule = "FREQ=DAILY;COUNT=5",
            dtstart = LocalDateTime.of(2026, 6, 1, 9, 0),
            timezone = utc,
            rangeStart = LocalDateTime.of(2026, 1, 1, 0, 0),
            rangeEnd = LocalDateTime.of(2026, 2, 1, 0, 0),
        )

        assertTrue(results.isEmpty())
    }
}

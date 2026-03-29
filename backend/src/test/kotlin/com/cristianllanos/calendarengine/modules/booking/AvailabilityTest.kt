package com.cristianllanos.calendarengine.modules.booking

import com.cristianllanos.calendarengine.modules.booking.actions.TimeInterval
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AvailabilityTest {

    @Test
    fun `overlapping intervals detected`() {
        val a = TimeInterval(
            start = LocalDateTime.of(2026, 3, 15, 9, 0),
            end = LocalDateTime.of(2026, 3, 15, 10, 0),
        )
        val b = TimeInterval(
            start = LocalDateTime.of(2026, 3, 15, 9, 30),
            end = LocalDateTime.of(2026, 3, 15, 10, 30),
        )
        assertTrue(a.overlaps(b))
        assertTrue(b.overlaps(a))
    }

    @Test
    fun `adjacent intervals do not overlap`() {
        val a = TimeInterval(
            start = LocalDateTime.of(2026, 3, 15, 9, 0),
            end = LocalDateTime.of(2026, 3, 15, 10, 0),
        )
        val b = TimeInterval(
            start = LocalDateTime.of(2026, 3, 15, 10, 0),
            end = LocalDateTime.of(2026, 3, 15, 11, 0),
        )
        assertFalse(a.overlaps(b))
        assertFalse(b.overlaps(a))
    }

    @Test
    fun `non-overlapping intervals`() {
        val a = TimeInterval(
            start = LocalDateTime.of(2026, 3, 15, 9, 0),
            end = LocalDateTime.of(2026, 3, 15, 10, 0),
        )
        val b = TimeInterval(
            start = LocalDateTime.of(2026, 3, 15, 14, 0),
            end = LocalDateTime.of(2026, 3, 15, 15, 0),
        )
        assertFalse(a.overlaps(b))
    }

    @Test
    fun `interval contains another`() {
        val outer = TimeInterval(
            start = LocalDateTime.of(2026, 3, 15, 8, 0),
            end = LocalDateTime.of(2026, 3, 15, 17, 0),
        )
        val inner = TimeInterval(
            start = LocalDateTime.of(2026, 3, 15, 10, 0),
            end = LocalDateTime.of(2026, 3, 15, 11, 0),
        )
        assertTrue(outer.overlaps(inner))
        assertTrue(inner.overlaps(outer))
    }

    @Test
    fun `same interval overlaps`() {
        val a = TimeInterval(
            start = LocalDateTime.of(2026, 3, 15, 9, 0),
            end = LocalDateTime.of(2026, 3, 15, 10, 0),
        )
        assertTrue(a.overlaps(a))
    }

    @Test
    fun `one minute overlap detected`() {
        val a = TimeInterval(
            start = LocalDateTime.of(2026, 3, 15, 9, 0),
            end = LocalDateTime.of(2026, 3, 15, 10, 0),
        )
        val b = TimeInterval(
            start = LocalDateTime.of(2026, 3, 15, 9, 59),
            end = LocalDateTime.of(2026, 3, 15, 11, 0),
        )
        assertTrue(a.overlaps(b))
    }
}

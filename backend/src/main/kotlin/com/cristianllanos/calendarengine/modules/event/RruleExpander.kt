package com.cristianllanos.calendarengine.modules.event

import org.dmfs.rfc5545.DateTime
import org.dmfs.rfc5545.recur.RecurrenceRule
import org.dmfs.rfc5545.recurrenceset.OfRuleAndFirst
import org.dmfs.rfc5545.recurrenceset.Within
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.TimeZone

/**
 * Expands RFC 5545 recurrence rules (RRULE) into concrete occurrence dates within a given range.
 *
 * Uses the lib-recur library to parse the RRULE string and generate occurrence timestamps,
 * converting between Java time types and the library's RFC 5545 DateTime representation.
 */
class RruleExpander {

    /**
     * Expands the given RRULE from [dtstart] and returns occurrence start times that fall within [[rangeStart], [rangeEnd]].
     * @param maxOccurrences safety cap to prevent unbounded expansion
     */
    fun expand(
        rrule: String,
        dtstart: LocalDateTime,
        timezone: ZoneId,
        rangeStart: LocalDateTime,
        rangeEnd: LocalDateTime,
        maxOccurrences: Int = 366,
    ): List<LocalDateTime> {
        val rule = RecurrenceRule(rrule)
        val tz = TimeZone.getTimeZone(timezone)

        val start = dtstart.toRfc5545DateTime(tz)
        val rStart = rangeStart.toRfc5545DateTime(tz)
        val rEnd = rangeEnd.toRfc5545DateTime(tz)

        val occurrences = Within(rStart, rEnd, OfRuleAndFirst(rule, start))

        return occurrences
            .take(maxOccurrences)
            .map { it.toLocalDateTime() }
    }

    private fun LocalDateTime.toRfc5545DateTime(tz: TimeZone): DateTime {
        return DateTime(
            tz,
            year,
            monthValue - 1, // lib-recur uses 0-based months
            dayOfMonth,
            hour,
            minute,
            second,
        )
    }

    private fun DateTime.toLocalDateTime(): LocalDateTime {
        return LocalDateTime.of(
            year,
            month + 1, // convert back from 0-based
            dayOfMonth,
            hours,
            minutes,
            seconds,
        )
    }
}

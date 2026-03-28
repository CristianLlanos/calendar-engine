package com.calendarengine.modules.event

import org.dmfs.rfc5545.DateTime
import org.dmfs.rfc5545.recur.RecurrenceRule
import org.dmfs.rfc5545.recurrenceset.OfRuleAndFirst
import org.dmfs.rfc5545.recurrenceset.Within
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.TimeZone

class RruleExpander {

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

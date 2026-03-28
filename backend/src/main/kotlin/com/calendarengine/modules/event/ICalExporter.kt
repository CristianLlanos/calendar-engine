package com.calendarengine.modules.event

import com.calendarengine.models.Calendars
import com.calendarengine.models.Events
import com.calendarengine.models.RecurrenceRules
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ICalExporter {

    private val icalDateFormat = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")

    fun export(calendarId: Int, tenantId: Int): String = transaction {
        val calendar = Calendars.selectAll()
            .where { (Calendars.id eq calendarId) and (Calendars.tenantId eq tenantId) }
            .firstOrNull() ?: throw NoSuchElementException("Calendar not found")

        val calendarName = calendar[Calendars.name]
        val timezone = calendar[Calendars.timezone]

        val events = Events.selectAll()
            .where { (Events.calendarId eq calendarId) and (Events.tenantId eq tenantId) }
            .toList()

        buildString {
            appendLine("BEGIN:VCALENDAR")
            appendLine("VERSION:2.0")
            appendLine("PRODID:-//CalendarEngine//EN")
            appendLine("CALSCALE:GREGORIAN")
            appendLine("X-WR-CALNAME:$calendarName")
            appendLine("X-WR-TIMEZONE:$timezone")

            for (event in events) {
                appendLine("BEGIN:VEVENT")
                appendLine("UID:event-${event[Events.id]}@calendarengine")
                appendLine("DTSTART:${event[Events.startTime].format(icalDateFormat)}")
                appendLine("DTEND:${event[Events.endTime].format(icalDateFormat)}")
                appendLine("SUMMARY:${escapeICalText(event[Events.title])}")

                event[Events.description]?.let {
                    appendLine("DESCRIPTION:${escapeICalText(it)}")
                }
                event[Events.location]?.let {
                    appendLine("LOCATION:${escapeICalText(it)}")
                }

                appendLine("STATUS:${event[Events.status]}")
                appendLine("CREATED:${event[Events.createdAt].format(icalDateFormat)}")
                appendLine("LAST-MODIFIED:${event[Events.updatedAt].format(icalDateFormat)}")

                // Add RRULE if recurring
                event[Events.recurrenceRuleId]?.let { ruleId ->
                    val rule = RecurrenceRules.selectAll()
                        .where { RecurrenceRules.id eq ruleId }
                        .firstOrNull()
                    rule?.let {
                        appendLine("RRULE:${it[RecurrenceRules.rrule]}")
                    }
                }

                appendLine("END:VEVENT")
            }

            appendLine("END:VCALENDAR")
        }
    }

    private fun escapeICalText(text: String): String {
        return text
            .replace("\\", "\\\\")
            .replace(",", "\\,")
            .replace(";", "\\;")
            .replace("\n", "\\n")
    }
}

package com.cristianllanos.calendarengine.modules.event.actions

import com.cristianllanos.calendarengine.models.Events
import com.cristianllanos.calendarengine.models.RecurrenceExceptions
import com.cristianllanos.calendarengine.models.RecurrenceRules
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

class DeleteEventAction {

    fun execute(eventId: Int, calendarId: Int, tenantId: Int, scope: DeleteScope, occurrenceDate: String? = null) = transaction {
        val event = Events.selectAll()
            .where { (Events.id eq eventId) and (Events.calendarId eq calendarId) and (Events.tenantId eq tenantId) }
            .firstOrNull() ?: throw NoSuchElementException("Event not found")

        val recurrenceRuleId = event[Events.recurrenceRuleId]

        when (scope) {
            DeleteScope.ALL -> {
                // Delete all exceptions, then the event, then the recurrence rule
                RecurrenceExceptions.deleteWhere { RecurrenceExceptions.eventId eq eventId }
                Events.deleteWhere { Events.id eq eventId }
                recurrenceRuleId?.let { ruleId ->
                    RecurrenceRules.deleteWhere { RecurrenceRules.id eq ruleId }
                }
            }

            DeleteScope.THIS -> {
                if (recurrenceRuleId == null) {
                    throw IllegalArgumentException("Cannot delete single occurrence of a non-recurring event")
                }
                requireNotNull(occurrenceDate) { "Occurrence date is required for THIS scope" }

                // Add an exclusion for this occurrence
                RecurrenceExceptions.insert {
                    it[RecurrenceExceptions.eventId] = eventId
                    it[originalDate] = LocalDateTime.parse(occurrenceDate)
                    it[isExcluded] = true
                }
            }

            DeleteScope.FOLLOWING -> {
                if (recurrenceRuleId == null) {
                    throw IllegalArgumentException("Cannot delete following occurrences of a non-recurring event")
                }
                requireNotNull(occurrenceDate) { "Occurrence date is required for FOLLOWING scope" }

                val cutoffDate = LocalDateTime.parse(occurrenceDate)

                // Update the RRULE to end before this occurrence
                val rule = RecurrenceRules.selectAll()
                    .where { RecurrenceRules.id eq recurrenceRuleId }
                    .first()
                val existingRrule = rule[RecurrenceRules.rrule]

                // Add/replace UNTIL in the RRULE
                val untilStr = cutoffDate.minusSeconds(1).toRruleUntil()
                val newRrule = replaceOrAddUntil(existingRrule, untilStr)

                RecurrenceRules.update({ RecurrenceRules.id eq recurrenceRuleId }) {
                    it[rrule] = newRrule
                }

                // Remove any exceptions at or after the cutoff
                RecurrenceExceptions.deleteWhere {
                    (RecurrenceExceptions.eventId eq eventId) and
                        (RecurrenceExceptions.originalDate.greaterEq(cutoffDate))
                }
            }
        }
    }

    private fun LocalDateTime.toRruleUntil(): String {
        return "%04d%02d%02dT%02d%02d%02dZ".format(year, monthValue, dayOfMonth, hour, minute, second)
    }

    private fun replaceOrAddUntil(rrule: String, untilValue: String): String {
        val parts = rrule.split(";").toMutableList()
        val untilIndex = parts.indexOfFirst { it.startsWith("UNTIL=") }
        val countIndex = parts.indexOfFirst { it.startsWith("COUNT=") }

        // Remove COUNT if present (UNTIL takes precedence)
        if (countIndex >= 0) parts.removeAt(countIndex)

        if (untilIndex >= 0) {
            parts[untilIndex] = "UNTIL=$untilValue"
        } else {
            parts.add("UNTIL=$untilValue")
        }
        return parts.joinToString(";")
    }
}

enum class DeleteScope {
    ALL,
    THIS,
    FOLLOWING,
}

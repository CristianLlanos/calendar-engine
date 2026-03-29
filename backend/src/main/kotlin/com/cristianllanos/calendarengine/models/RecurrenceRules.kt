package com.cristianllanos.calendarengine.models

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

/** Exposed table for iCalendar RRULE-based recurrence definitions. */
object RecurrenceRules : Table("recurrence_rules") {
    val id = integer("id").autoIncrement()
    val rrule = varchar("rrule", 1000)
    val dtstart = datetime("dtstart")
    val timezone = varchar("timezone", 100)

    override val primaryKey = PrimaryKey(id)
}

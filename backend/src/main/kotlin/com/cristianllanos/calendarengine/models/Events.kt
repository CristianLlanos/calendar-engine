package com.cristianllanos.calendarengine.models

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object Events : Table("events") {
    val id = integer("id").autoIncrement()
    val tenantId = integer("tenant_id").references(Tenants.id)
    val calendarId = integer("calendar_id").references(Calendars.id)
    val title = varchar("title", 500)
    val description = text("description").nullable()
    val location = varchar("location", 500).nullable()
    val startTime = datetime("start_time")
    val endTime = datetime("end_time")
    val allDay = bool("all_day").default(false)
    val status = varchar("status", 50).default("CONFIRMED")
    val recurrenceRuleId = integer("recurrence_rule_id").references(RecurrenceRules.id).nullable()
    val createdBy = integer("created_by").nullable()
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")

    override val primaryKey = PrimaryKey(id)

    init {
        index(false, calendarId)
        index(false, tenantId)
        index(false, startTime, endTime)
        index(false, recurrenceRuleId)
    }
}

package com.cristianllanos.calendarengine.models

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object ExternalBusyBlocks : Table("external_busy_blocks") {
    val id = integer("id").autoIncrement()
    val connectionId = integer("connection_id").references(ExternalCalendarConnections.id, onDelete = ReferenceOption.CASCADE)
    val tenantId = integer("tenant_id").references(Tenants.id)
    val calendarId = integer("calendar_id").references(Calendars.id)
    val startTime = datetime("start_time")
    val endTime = datetime("end_time")
    val externalEventId = varchar("external_event_id", 500).nullable()

    override val primaryKey = PrimaryKey(id)

    init {
        index(false, connectionId)
        index(false, calendarId)
        index(false, startTime, endTime)
    }
}

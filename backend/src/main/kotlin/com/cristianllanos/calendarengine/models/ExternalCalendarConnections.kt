package com.cristianllanos.calendarengine.models

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object ExternalCalendarConnections : Table("external_calendar_connections") {
    val id = integer("id").autoIncrement()
    val tenantId = integer("tenant_id").references(Tenants.id)
    val calendarId = integer("calendar_id").references(Calendars.id)
    val provider = varchar("provider", 50)
    val externalCalendarId = varchar("external_calendar_id", 500)
    val syncMode = varchar("sync_mode", 50).default("POLLING")
    val lastSyncedAt = datetime("last_synced_at").nullable()
    val syncToken = varchar("sync_token", 500).nullable()
    val enabled = bool("enabled").default(true)
    val createdAt = datetime("created_at")

    override val primaryKey = PrimaryKey(id)

    init {
        index(false, tenantId)
        index(false, calendarId)
    }
}

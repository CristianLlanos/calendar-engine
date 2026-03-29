package com.cristianllanos.calendarengine.models

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

/** Exposed table for internal system events used for async processing. */
object SystemEvents : Table("system_events") {
    val id = integer("id").autoIncrement()
    val tenantId = integer("tenant_id").references(Tenants.id)
    val eventType = varchar("event_type", 100)
    val payload = text("payload")
    val processed = bool("processed").default(false)
    val createdAt = datetime("created_at")

    override val primaryKey = PrimaryKey(id)

    init {
        index(false, tenantId)
        index(false, processed)
    }
}

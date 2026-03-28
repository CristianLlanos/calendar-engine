package com.calendarengine.models

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object Calendars : Table("calendars") {
    val id = integer("id").autoIncrement()
    val tenantId = integer("tenant_id").references(Tenants.id)
    val name = varchar("name", 255)
    val description = text("description").nullable()
    val timezone = varchar("timezone", 100).default("UTC")
    val color = varchar("color", 20).nullable()
    val visibility = varchar("visibility", 20).default("PRIVATE")
    val createdAt = datetime("created_at")

    override val primaryKey = PrimaryKey(id)

    init {
        index(false, tenantId)
    }
}

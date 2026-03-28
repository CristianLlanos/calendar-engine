package com.calendarengine.models

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object Tenants : Table("tenants") {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 255)
    val slug = varchar("slug", 255).uniqueIndex()
    val config = text("config").nullable()
    val createdAt = datetime("created_at")

    override val primaryKey = PrimaryKey(id)
}

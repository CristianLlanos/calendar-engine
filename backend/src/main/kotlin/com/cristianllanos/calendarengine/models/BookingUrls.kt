package com.cristianllanos.calendarengine.models

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

/** Exposed table for booking URL configurations with availability and scheduling constraints. */
object BookingUrls : Table("booking_urls") {
    val id = integer("id").autoIncrement()
    val tenantId = integer("tenant_id").references(Tenants.id)
    val calendarId = integer("calendar_id").references(Calendars.id)
    val slug = varchar("slug", 255)
    val name = varchar("name", 255)
    val description = text("description").nullable()
    val status = varchar("status", 20).default("ACTIVE")
    val durationMinutes = integer("duration_minutes")
    val durationOptions = varchar("duration_options", 500).nullable()
    val bufferBeforeMinutes = integer("buffer_before_minutes").default(0)
    val bufferAfterMinutes = integer("buffer_after_minutes").default(0)
    val minLeadTimeHours = integer("min_lead_time_hours").default(1)
    val maxLeadTimeDays = integer("max_lead_time_days").default(60)
    val maxBookingsPerDay = integer("max_bookings_per_day").nullable()
    val maxBookingsPerWeek = integer("max_bookings_per_week").nullable()
    val autoConfirm = bool("auto_confirm").default(true)
    val createdAt = datetime("created_at")

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex(tenantId, slug)
        index(false, tenantId)
        index(false, calendarId)
    }
}

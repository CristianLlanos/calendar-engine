package com.cristianllanos.calendarengine.models

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object Bookings : Table("bookings") {
    val id = integer("id").autoIncrement()
    val tenantId = integer("tenant_id").references(Tenants.id)
    val bookingUrlId = integer("booking_url_id").references(BookingUrls.id)
    val calendarId = integer("calendar_id").references(Calendars.id)
    val eventId = integer("event_id").references(Events.id).nullable()
    val status = varchar("status", 50).default("CONFIRMED")
    val startTime = datetime("start_time")
    val endTime = datetime("end_time")
    val bookerName = varchar("booker_name", 255)
    val bookerEmail = varchar("booker_email", 255)
    val bookerPhone = varchar("booker_phone", 50).nullable()
    val bookerUserId = integer("booker_user_id").nullable()
    val notes = text("notes").nullable()
    val cancelledAt = datetime("cancelled_at").nullable()
    val cancellationReason = text("cancellation_reason").nullable()
    val createdAt = datetime("created_at")

    override val primaryKey = PrimaryKey(id)

    init {
        index(false, tenantId)
        index(false, calendarId)
        index(false, bookingUrlId)
        index(false, startTime, endTime)
        index(false, bookerEmail)
    }
}

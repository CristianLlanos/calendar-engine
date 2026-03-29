package com.cristianllanos.calendarengine.models

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.time

object BookingUrlAvailabilityWindows : Table("booking_url_availability_windows") {
    val id = integer("id").autoIncrement()
    val bookingUrlId = integer("booking_url_id").references(BookingUrls.id, onDelete = ReferenceOption.CASCADE)
    val dayOfWeek = integer("day_of_week")
    val startTime = time("start_time")
    val endTime = time("end_time")

    override val primaryKey = PrimaryKey(id)

    init {
        index(false, bookingUrlId)
    }
}

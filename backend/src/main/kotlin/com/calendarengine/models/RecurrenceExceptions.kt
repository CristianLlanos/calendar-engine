package com.calendarengine.models

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object RecurrenceExceptions : Table("recurrence_exceptions") {
    val id = integer("id").autoIncrement()
    val eventId = integer("event_id").references(Events.id, onDelete = ReferenceOption.CASCADE)
    val originalDate = datetime("original_date")
    val isExcluded = bool("is_excluded").default(true)
    val overrideTitle = varchar("override_title", 500).nullable()
    val overrideStartTime = datetime("override_start_time").nullable()
    val overrideEndTime = datetime("override_end_time").nullable()
    val overrideLocation = varchar("override_location", 500).nullable()
    val overrideDescription = text("override_description").nullable()

    override val primaryKey = PrimaryKey(id)

    init {
        index(false, eventId)
    }
}

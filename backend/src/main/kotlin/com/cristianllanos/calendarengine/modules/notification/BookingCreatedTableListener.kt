package com.cristianllanos.calendarengine.modules.notification

import com.cristianllanos.calendarengine.dto.BookingCreatedEvent
import com.cristianllanos.calendarengine.models.SystemEvents
import com.cristianllanos.events.Listener
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

class BookingCreatedTableListener : Listener<BookingCreatedEvent> {

    private val json = Json { encodeDefaults = true }

    override fun handle(event: BookingCreatedEvent) {
        transaction {
            SystemEvents.insert {
                it[tenantId] = event.tenantId
                it[eventType] = "BOOKING_CREATED"
                it[payload] = json.encodeToString(event.booking)
                it[processed] = false
                it[createdAt] = LocalDateTime.now()
            }
        }
    }
}

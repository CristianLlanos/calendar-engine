package com.cristianllanos.calendarengine.modules.notification

import com.cristianllanos.calendarengine.dto.BookingCancelledEvent
import com.cristianllanos.calendarengine.models.SystemEvents
import com.cristianllanos.events.Listener
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

class BookingCancelledTableListener : Listener<BookingCancelledEvent> {

    private val json = Json { encodeDefaults = true }

    override fun handle(event: BookingCancelledEvent) {
        transaction {
            SystemEvents.insert {
                it[tenantId] = event.tenantId
                it[eventType] = "BOOKING_CANCELLED"
                it[payload] = json.encodeToString(event.booking)
                it[processed] = false
                it[createdAt] = LocalDateTime.now()
            }
        }
    }
}

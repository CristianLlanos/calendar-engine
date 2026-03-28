package com.calendarengine.modules.notification

import com.calendarengine.dto.CalendarSystemEvent
import com.calendarengine.models.SystemEvents
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

class TableSystemEventEmitter : SystemEventEmitter {

    private val json = Json { encodeDefaults = true }

    override fun emit(tenantId: Int, event: CalendarSystemEvent) {
        transaction {
            SystemEvents.insert {
                it[SystemEvents.tenantId] = tenantId
                it[eventType] = event.type
                it[payload] = json.encodeToString(event)
                it[processed] = false
                it[createdAt] = LocalDateTime.now()
            }
        }
    }
}

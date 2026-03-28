package com.calendarengine.modules.sync

import com.calendarengine.models.Calendars
import com.calendarengine.models.ExternalBusyBlocks
import com.calendarengine.models.ExternalCalendarConnections
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

data class ConnectionResponse(
    val id: Int,
    val tenantId: Int,
    val calendarId: Int,
    val provider: String,
    val externalCalendarId: String,
    val syncMode: String,
    val lastSyncedAt: String?,
    val enabled: Boolean,
    val createdAt: String,
)

data class CreateConnectionRequest(
    val calendarId: Int,
    val provider: String,
    val externalCalendarId: String,
    val syncMode: String = "ON_DEMAND",
)

class ConnectionService {

    fun list(tenantId: Int): List<ConnectionResponse> = transaction {
        ExternalCalendarConnections.selectAll()
            .where { ExternalCalendarConnections.tenantId eq tenantId }
            .map { it.toResponse() }
    }

    fun create(tenantId: Int, request: CreateConnectionRequest): ConnectionResponse = transaction {
        // Verify calendar belongs to tenant
        Calendars.selectAll()
            .where { (Calendars.id eq request.calendarId) and (Calendars.tenantId eq tenantId) }
            .firstOrNull() ?: throw NoSuchElementException("Calendar not found")

        val now = LocalDateTime.now()
        val id = ExternalCalendarConnections.insert {
            it[ExternalCalendarConnections.tenantId] = tenantId
            it[calendarId] = request.calendarId
            it[provider] = request.provider
            it[externalCalendarId] = request.externalCalendarId
            it[syncMode] = request.syncMode
            it[enabled] = true
            it[createdAt] = now
        } get ExternalCalendarConnections.id

        ExternalCalendarConnections.selectAll()
            .where { ExternalCalendarConnections.id eq id }
            .first()
            .toResponse()
    }

    fun delete(connectionId: Int, tenantId: Int) = transaction {
        // Busy blocks cascade on delete via FK
        val deleted = ExternalCalendarConnections.deleteWhere {
            (ExternalCalendarConnections.id eq connectionId) and (ExternalCalendarConnections.tenantId eq tenantId)
        }
        if (deleted == 0) throw NoSuchElementException("Connection not found")
    }

    private fun ResultRow.toResponse() = ConnectionResponse(
        id = this[ExternalCalendarConnections.id],
        tenantId = this[ExternalCalendarConnections.tenantId],
        calendarId = this[ExternalCalendarConnections.calendarId],
        provider = this[ExternalCalendarConnections.provider],
        externalCalendarId = this[ExternalCalendarConnections.externalCalendarId],
        syncMode = this[ExternalCalendarConnections.syncMode],
        lastSyncedAt = this[ExternalCalendarConnections.lastSyncedAt]?.toString(),
        enabled = this[ExternalCalendarConnections.enabled],
        createdAt = this[ExternalCalendarConnections.createdAt].toString(),
    )
}

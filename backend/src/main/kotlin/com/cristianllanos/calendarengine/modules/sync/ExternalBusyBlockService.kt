package com.cristianllanos.calendarengine.modules.sync

import com.cristianllanos.calendarengine.models.ExternalBusyBlocks
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

/** Represents a busy time range from an external calendar. */
data class BusyBlock(
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val externalEventId: String?,
)

/** Manages external busy block records used to prevent booking conflicts with external calendars. */
class ExternalBusyBlockService {

    /** Atomically replaces all busy blocks for a connection (used during full sync). */
    fun replaceBlocksForConnection(
        connectionId: Int,
        tenantId: Int,
        calendarId: Int,
        blocks: List<BusyBlock>,
    ) = transaction {
        ExternalBusyBlocks.deleteWhere { ExternalBusyBlocks.connectionId eq connectionId }

        for (block in blocks) {
            ExternalBusyBlocks.insert {
                it[ExternalBusyBlocks.connectionId] = connectionId
                it[ExternalBusyBlocks.tenantId] = tenantId
                it[ExternalBusyBlocks.calendarId] = calendarId
                it[startTime] = block.startTime
                it[endTime] = block.endTime
                it[externalEventId] = block.externalEventId
            }
        }
    }

    /** Inserts or updates a single busy block, matching by external event ID if available (used during incremental sync). */
    fun upsertBlock(
        connectionId: Int,
        tenantId: Int,
        calendarId: Int,
        block: BusyBlock,
    ) = transaction {
        val externalId = block.externalEventId

        if (externalId != null) {
            // Check if exists by external event ID
            val existing = ExternalBusyBlocks.selectAll()
                .where {
                    (ExternalBusyBlocks.connectionId eq connectionId) and
                        (ExternalBusyBlocks.externalEventId eq externalId)
                }
                .firstOrNull()

            if (existing != null) {
                ExternalBusyBlocks.update({
                    (ExternalBusyBlocks.connectionId eq connectionId) and
                        (ExternalBusyBlocks.externalEventId eq externalId)
                }) {
                    it[startTime] = block.startTime
                    it[endTime] = block.endTime
                }
                return@transaction
            }
        }

        ExternalBusyBlocks.insert {
            it[ExternalBusyBlocks.connectionId] = connectionId
            it[ExternalBusyBlocks.tenantId] = tenantId
            it[ExternalBusyBlocks.calendarId] = calendarId
            it[startTime] = block.startTime
            it[endTime] = block.endTime
            it[ExternalBusyBlocks.externalEventId] = externalId
        }
    }

    /** Removes a busy block identified by its external event ID (used when an external event is cancelled). */
    fun removeByExternalEventId(connectionId: Int, externalEventId: String) = transaction {
        ExternalBusyBlocks.deleteWhere {
            (ExternalBusyBlocks.connectionId eq connectionId) and
                (ExternalBusyBlocks.externalEventId eq externalEventId)
        }
    }
}

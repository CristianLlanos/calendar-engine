package com.cristianllanos.calendarengine.modules.sync

import com.cristianllanos.calendarengine.models.ExternalBusyBlocks
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

data class BusyBlock(
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val externalEventId: String?,
)

class ExternalBusyBlockService {

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

    fun removeByExternalEventId(connectionId: Int, externalEventId: String) = transaction {
        ExternalBusyBlocks.deleteWhere {
            (ExternalBusyBlocks.connectionId eq connectionId) and
                (ExternalBusyBlocks.externalEventId eq externalEventId)
        }
    }
}

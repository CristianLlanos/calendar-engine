package com.calendarengine.modules.sync

import com.calendarengine.config.SyncConfig
import com.calendarengine.models.ExternalCalendarConnections
import kotlinx.coroutines.*
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

class SyncPollingScheduler(
    private val googleSyncService: GoogleCalendarSyncService,
    private val appleSyncService: AppleCalDavSyncService,
) {

    private val logger = LoggerFactory.getLogger(SyncPollingScheduler::class.java)
    private var job: Job? = null

    fun start(scope: CoroutineScope) {
        if (SyncConfig.config.mode != "POLLING") {
            logger.info("Sync polling disabled (mode=${SyncConfig.config.mode})")
            return
        }

        val intervalMinutes = SyncConfig.config.pollingIntervalMinutes.toLong()
        logger.info("Starting sync polling scheduler (interval=${intervalMinutes}m)")

        job = scope.launch {
            while (isActive) {
                delay(intervalMinutes * 60 * 1000)
                pollAllConnections()
            }
        }
    }

    fun stop() {
        job?.cancel()
    }

    private suspend fun pollAllConnections() {
        val connections = transaction {
            ExternalCalendarConnections.selectAll()
                .where { ExternalCalendarConnections.enabled eq true }
                .toList()
        }

        for (connection in connections) {
            val id = connection[ExternalCalendarConnections.id]
            val provider = connection[ExternalCalendarConnections.provider]

            try {
                when (provider) {
                    "GOOGLE" -> googleSyncService.syncConnection(id)
                    "APPLE" -> appleSyncService.syncConnection(id)
                    else -> logger.warn("Unknown sync provider: $provider for connection $id")
                }
            } catch (e: Exception) {
                logger.error("Polling sync failed for connection $id ($provider): ${e.message}")
            }
        }
    }
}

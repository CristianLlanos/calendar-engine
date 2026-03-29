package com.cristianllanos.calendarengine.modules.sync

import com.cristianllanos.calendarengine.config.SyncConfig
import com.cristianllanos.calendarengine.models.ExternalCalendarConnections
import com.cristianllanos.calendarengine.models.enums.SyncMode
import com.cristianllanos.calendarengine.models.enums.SyncProvider
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
        if (SyncConfig.config.mode != SyncMode.POLLING.name) {
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

    private suspend fun pollAllConnections() = coroutineScope {
        val connections = transaction {
            ExternalCalendarConnections.selectAll()
                .where { ExternalCalendarConnections.enabled eq true }
                .toList()
        }

        connections.map { connection ->
            val id = connection[ExternalCalendarConnections.id]
            val provider = connection[ExternalCalendarConnections.provider]

            launch {
                try {
                    when (provider) {
                        SyncProvider.GOOGLE.name -> googleSyncService.syncConnection(id)
                        SyncProvider.APPLE.name -> appleSyncService.syncConnection(id)
                        else -> logger.warn("Unknown sync provider: $provider for connection $id")
                    }
                } catch (e: Exception) {
                    logger.error("Polling sync failed for connection $id ($provider): ${e.message}")
                }
            }
        }
    }
}

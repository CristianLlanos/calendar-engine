package com.calendarengine.modules.sync

import com.calendarengine.models.ExternalCalendarConnections
import com.calendarengine.modules.sync.actions.GoogleOAuthAction
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

class GoogleCalendarSyncService(
    private val httpClient: HttpClient,
    private val oauthAction: GoogleOAuthAction,
    private val busyBlockService: ExternalBusyBlockService,
) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun syncConnection(connectionId: Int) {
        val connection = transaction {
            ExternalCalendarConnections.selectAll()
                .where { ExternalCalendarConnections.id eq connectionId }
                .firstOrNull()
        } ?: throw NoSuchElementException("Connection not found")

        val tenantId = connection[ExternalCalendarConnections.tenantId]
        val calendarId = connection[ExternalCalendarConnections.calendarId]
        val externalCalendarId = connection[ExternalCalendarConnections.externalCalendarId]
        val syncToken = connection[ExternalCalendarConnections.syncToken]

        val accessToken = oauthAction.getValidAccessToken(tenantId)

        if (syncToken != null) {
            incrementalSync(connectionId, tenantId, calendarId, externalCalendarId, accessToken, syncToken)
        } else {
            fullSync(connectionId, tenantId, calendarId, externalCalendarId, accessToken)
        }
    }

    private suspend fun fullSync(
        connectionId: Int,
        tenantId: Int,
        calendarId: Int,
        externalCalendarId: String,
        accessToken: String,
    ) {
        val blocks = mutableListOf<BusyBlock>()
        var pageToken: String? = null
        var newSyncToken: String? = null

        do {
            val url = buildString {
                append("https://www.googleapis.com/calendar/v3/calendars/")
                append(externalCalendarId.encodeURLParameter())
                append("/events?singleEvents=true&maxResults=250")
                append("&fields=items(id,start,end,status),nextPageToken,nextSyncToken")
                pageToken?.let { append("&pageToken=${it.encodeURLParameter()}") }
            }

            val response = httpClient.get(url) {
                header("Authorization", "Bearer $accessToken")
            }

            val body = response.body<String>()
            val result = json.decodeFromString<GoogleEventsResponse>(body)

            for (item in result.items) {
                if (item.status == "cancelled") continue
                val start = parseGoogleDateTime(item.start) ?: continue
                val end = parseGoogleDateTime(item.end) ?: continue
                blocks.add(BusyBlock(start, end, item.id))
            }

            pageToken = result.nextPageToken
            newSyncToken = result.nextSyncToken
        } while (pageToken != null)

        // Replace all blocks for this connection
        busyBlockService.replaceBlocksForConnection(connectionId, tenantId, calendarId, blocks)

        // Store sync token
        updateSyncState(connectionId, newSyncToken)
    }

    private suspend fun incrementalSync(
        connectionId: Int,
        tenantId: Int,
        calendarId: Int,
        externalCalendarId: String,
        accessToken: String,
        syncToken: String,
    ) {
        var pageToken: String? = null
        var newSyncToken: String? = null

        do {
            val url = buildString {
                append("https://www.googleapis.com/calendar/v3/calendars/")
                append(externalCalendarId.encodeURLParameter())
                append("/events?syncToken=${syncToken.encodeURLParameter()}")
                append("&fields=items(id,start,end,status),nextPageToken,nextSyncToken")
                pageToken?.let { append("&pageToken=${it.encodeURLParameter()}") }
            }

            val response = httpClient.get(url) {
                header("Authorization", "Bearer $accessToken")
            }

            // If sync token is invalid (410 Gone), fall back to full sync
            if (response.status == HttpStatusCode.Gone) {
                clearSyncToken(connectionId)
                fullSync(connectionId, tenantId, calendarId, externalCalendarId, accessToken)
                return
            }

            val body = response.body<String>()
            val result = json.decodeFromString<GoogleEventsResponse>(body)

            for (item in result.items) {
                if (item.status == "cancelled") {
                    item.id?.let { busyBlockService.removeByExternalEventId(connectionId, it) }
                } else {
                    val start = parseGoogleDateTime(item.start) ?: continue
                    val end = parseGoogleDateTime(item.end) ?: continue
                    busyBlockService.upsertBlock(
                        connectionId, tenantId, calendarId,
                        BusyBlock(start, end, item.id),
                    )
                }
            }

            pageToken = result.nextPageToken
            newSyncToken = result.nextSyncToken
        } while (pageToken != null)

        updateSyncState(connectionId, newSyncToken)
    }

    private fun parseGoogleDateTime(dt: GoogleDateTime?): LocalDateTime? {
        if (dt == null) return null
        return when {
            dt.dateTime != null -> {
                val instant = Instant.parse(dt.dateTime)
                LocalDateTime.ofInstant(instant, ZoneOffset.UTC)
            }
            dt.date != null -> {
                // All-day event
                LocalDateTime.parse("${dt.date}T00:00:00")
            }
            else -> null
        }
    }

    private fun updateSyncState(connectionId: Int, syncToken: String?) = transaction {
        ExternalCalendarConnections.update({ ExternalCalendarConnections.id eq connectionId }) {
            it[lastSyncedAt] = LocalDateTime.now()
            if (syncToken != null) {
                it[ExternalCalendarConnections.syncToken] = syncToken
            }
        }
    }

    private fun clearSyncToken(connectionId: Int) = transaction {
        ExternalCalendarConnections.update({ ExternalCalendarConnections.id eq connectionId }) {
            it[syncToken] = null
        }
    }
}

@Serializable
private data class GoogleEventsResponse(
    val items: List<GoogleEventItem> = emptyList(),
    val nextPageToken: String? = null,
    val nextSyncToken: String? = null,
)

@Serializable
private data class GoogleEventItem(
    val id: String? = null,
    val start: GoogleDateTime? = null,
    val end: GoogleDateTime? = null,
    val status: String? = null,
)

@Serializable
private data class GoogleDateTime(
    val dateTime: String? = null,
    val date: String? = null,
    val timeZone: String? = null,
)

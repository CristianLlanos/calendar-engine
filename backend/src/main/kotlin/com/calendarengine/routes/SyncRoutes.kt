package com.calendarengine.routes

import com.calendarengine.auth.pathParam
import com.calendarengine.auth.tenantPrincipal
import com.calendarengine.config.SyncConfig
import com.calendarengine.modules.sync.ConnectionService
import com.calendarengine.modules.sync.CreateConnectionRequest
import com.calendarengine.modules.sync.GoogleCalendarSyncService
import com.calendarengine.modules.sync.actions.GoogleOAuthAction
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

fun Route.syncRoutes(
    googleOAuthAction: GoogleOAuthAction,
    connectionService: ConnectionService,
    googleSyncService: GoogleCalendarSyncService,
) {
    // Google OAuth flow
    route("/api/sync/google") {
        get("/auth") {
            if (!SyncConfig.config.google.enabled) {
                throw IllegalArgumentException("Google Calendar sync is not enabled")
            }

            val principal = call.tenantPrincipal()
            val calendarId = call.request.queryParameters["calendarId"]?.toIntOrNull()
                ?: throw IllegalArgumentException("Missing calendarId")

            val redirectUri = "${call.request.local.scheme}://${call.request.local.serverHost}:${call.request.local.serverPort}/api/sync/google/callback"
            val authUrl = googleOAuthAction.buildAuthUrl(calendarId, principal.tenantId, redirectUri)
            call.respondRedirect(authUrl)
        }

        get("/callback") {
            val code = call.request.queryParameters["code"]
                ?: throw IllegalArgumentException("Missing authorization code")
            val state = call.request.queryParameters["state"] ?: ""
            val parts = state.split(":")
            val tenantId = parts.getOrNull(0)?.toIntOrNull()
                ?: throw IllegalArgumentException("Invalid state parameter")

            val redirectUri = "${call.request.local.scheme}://${call.request.local.serverHost}:${call.request.local.serverPort}/api/sync/google/callback"
            val result = googleOAuthAction.exchangeCode(code, tenantId, redirectUri)
            call.respond(mapOf("message" to "Google Calendar connected", "expiresAt" to result.expiresAt))
        }
    }

    // Connection management
    route("/api/sync/connections") {
        get {
            val principal = call.tenantPrincipal()
            call.respond(connectionService.list(principal.tenantId))
        }

        post {
            val principal = call.tenantPrincipal()
            val request = call.receive<CreateConnectionApiRequest>()
            call.respond(
                HttpStatusCode.Created,
                connectionService.create(
                    principal.tenantId,
                    CreateConnectionRequest(
                        calendarId = request.calendarId,
                        provider = request.provider,
                        externalCalendarId = request.externalCalendarId,
                        syncMode = request.syncMode ?: "ON_DEMAND",
                    ),
                ),
            )
        }

        delete("/{id}") {
            val principal = call.tenantPrincipal()
            val id = call.pathParam("id")
            connectionService.delete(id, principal.tenantId)
            call.respond(HttpStatusCode.NoContent)
        }

        post("/{id}/sync") {
            call.tenantPrincipal() // verify tenant access
            val id = call.pathParam("id")
            googleSyncService.syncConnection(id)
            call.respond(mapOf("message" to "Sync completed"))
        }
    }

    // Webhook endpoint for Google push notifications
    post("/api/sync/webhooks/google") {
        val channelId = call.request.headers["X-Goog-Channel-ID"]

        if (channelId != null) {
            // Parse connectionId from channel ID (format: "ce-{connectionId}")
            val connectionId = channelId.removePrefix("ce-").toIntOrNull()
            if (connectionId != null) {
                googleSyncService.syncConnection(connectionId)
            }
        }

        call.respond(HttpStatusCode.OK)
    }
}

@Serializable
private data class CreateConnectionApiRequest(
    val calendarId: Int,
    val provider: String,
    val externalCalendarId: String,
    val syncMode: String? = null,
)

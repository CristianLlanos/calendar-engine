package com.cristianllanos.calendarengine.routes

import com.cristianllanos.calendarengine.auth.pathParam
import com.cristianllanos.calendarengine.auth.tenantPrincipal
import com.cristianllanos.calendarengine.config.SyncConfig
import com.cristianllanos.calendarengine.modules.sync.ConnectionService
import com.cristianllanos.calendarengine.modules.sync.CreateConnectionRequest
import com.cristianllanos.calendarengine.modules.sync.GoogleCalendarSyncService
import com.cristianllanos.calendarengine.modules.sync.actions.GoogleOAuthAction
import com.cristianllanos.calendarengine.models.enums.SyncMode
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

/** Registers sync routes: Google OAuth flow, connection management, manual sync trigger, and webhook endpoint. */
fun Route.syncRoutes(
    googleOAuthAction: GoogleOAuthAction,
    connectionService: ConnectionService,
    googleSyncService: GoogleCalendarSyncService,
) {
    // Google OAuth flow
    route("/sync/google") {
        get("/auth") {
            if (!SyncConfig.config.google.enabled) {
                throw IllegalArgumentException("Google Calendar sync is not enabled")
            }

            val principal = call.tenantPrincipal()
            val calendarId = call.request.queryParameters["calendarId"]?.toIntOrNull()
                ?: throw IllegalArgumentException("Missing calendarId")

            val redirectUri = call.googleOAuthRedirectUri()
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

            val redirectUri = call.googleOAuthRedirectUri()
            val result = googleOAuthAction.exchangeCode(code, tenantId, redirectUri)
            call.respond(mapOf("message" to "Google Calendar connected", "expiresAt" to result.expiresAt))
        }
    }

    // Connection management
    route("/sync/connections") {
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
                        syncMode = request.syncMode ?: SyncMode.ON_DEMAND.name,
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
    post("/sync/webhooks/google") {
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

private fun ApplicationCall.googleOAuthRedirectUri(): String {
    // Derive the callback URI from the current request path (sibling to /auth)
    val uri = request.local.uri
    val basePath = uri.substringBeforeLast("/sync/google/auth")
    return "${request.local.scheme}://${request.local.serverHost}:${request.local.serverPort}$basePath/sync/google/callback"
}

@Serializable
private data class CreateConnectionApiRequest(
    val calendarId: Int,
    val provider: String,
    val externalCalendarId: String,
    val syncMode: String? = null,
)

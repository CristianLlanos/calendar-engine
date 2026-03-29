package com.cristianllanos.calendarengine.modules.sync.actions

import com.cristianllanos.calendarengine.config.SyncConfig
import com.cristianllanos.calendarengine.models.OAuthTokens
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import com.cristianllanos.calendarengine.models.enums.SyncProvider
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

/** Handles Google OAuth 2.0 authorization, token exchange, and token refresh for calendar sync. */
class GoogleOAuthAction(
    private val httpClient: HttpClient,
) {

    private val json = Json { ignoreUnknownKeys = true }

    /** Builds the Google OAuth consent URL with calendar read-only scope. */
    fun buildAuthUrl(calendarId: Int, tenantId: Int, redirectUri: String): String {
        val config = SyncConfig.config.google
        val state = "$tenantId:$calendarId"

        return "https://accounts.google.com/o/oauth2/v2/auth?" +
            "client_id=${config.clientId}" +
            "&redirect_uri=${redirectUri.encodeURLParameter()}" +
            "&response_type=code" +
            "&scope=${SCOPES.encodeURLParameter()}" +
            "&access_type=offline" +
            "&prompt=consent" +
            "&state=${state.encodeURLParameter()}"
    }

    /** Exchanges an authorization code for access and refresh tokens, persisting them in the database. */
    suspend fun exchangeCode(code: String, tenantId: Int, redirectUri: String): OAuthTokenResult {
        val config = SyncConfig.config.google

        val response = httpClient.submitForm(
            url = "https://oauth2.googleapis.com/token",
            formParameters = parameters {
                append("code", code)
                append("client_id", config.clientId)
                append("client_secret", config.clientSecret)
                append("redirect_uri", redirectUri)
                append("grant_type", "authorization_code")
            }
        )

        val body = response.body<String>()
        val tokenResponse = json.decodeFromString<GoogleTokenResponse>(body)

        val now = LocalDateTime.now()
        val expiresAt = now.plusSeconds(tokenResponse.expiresIn.toLong())

        transaction {
            OAuthTokens.deleteWhere {
                (OAuthTokens.tenantId eq tenantId) and (OAuthTokens.provider eq SyncProvider.GOOGLE.name)
            }

            OAuthTokens.insert {
                it[OAuthTokens.tenantId] = tenantId
                it[provider] = SyncProvider.GOOGLE.name
                it[accessToken] = tokenResponse.accessToken
                it[refreshToken] = tokenResponse.refreshToken ?: ""
                it[OAuthTokens.expiresAt] = expiresAt
                it[scopes] = SCOPES
                it[createdAt] = now
                it[updatedAt] = now
            }
        }

        return OAuthTokenResult(
            accessToken = tokenResponse.accessToken,
            expiresAt = expiresAt.toString(),
        )
    }

    /** Returns a valid access token for the tenant, refreshing it automatically if expired. */
    suspend fun getValidAccessToken(tenantId: Int): String {
        val token = transaction {
            OAuthTokens.selectAll()
                .where { (OAuthTokens.tenantId eq tenantId) and (OAuthTokens.provider eq SyncProvider.GOOGLE.name) }
                .firstOrNull()
        } ?: throw NoSuchElementException("No Google OAuth token found for tenant")

        val expiresAt = token[OAuthTokens.expiresAt]
        val now = LocalDateTime.now()

        // If token is still valid (with 5 min buffer), return it
        if (expiresAt.isAfter(now.plusMinutes(5))) {
            return token[OAuthTokens.accessToken]
        }

        // Refresh the token
        val refreshToken = token[OAuthTokens.refreshToken]
        if (refreshToken.isBlank()) {
            throw IllegalStateException("No refresh token available, re-authorization required")
        }

        return refreshAccessToken(tenantId, refreshToken)
    }

    private suspend fun refreshAccessToken(tenantId: Int, refreshToken: String): String {
        val config = SyncConfig.config.google

        val response = httpClient.submitForm(
            url = "https://oauth2.googleapis.com/token",
            formParameters = parameters {
                append("refresh_token", refreshToken)
                append("client_id", config.clientId)
                append("client_secret", config.clientSecret)
                append("grant_type", "refresh_token")
            }
        )

        val body = response.body<String>()
        val tokenResponse = json.decodeFromString<GoogleTokenResponse>(body)

        val now = LocalDateTime.now()
        val expiresAt = now.plusSeconds(tokenResponse.expiresIn.toLong())

        transaction {
            OAuthTokens.update({
                (OAuthTokens.tenantId eq tenantId) and (OAuthTokens.provider eq SyncProvider.GOOGLE.name)
            }) {
                it[accessToken] = tokenResponse.accessToken
                it[OAuthTokens.expiresAt] = expiresAt
                it[updatedAt] = now
            }
        }

        return tokenResponse.accessToken
    }

    companion object {
        private const val SCOPES = "https://www.googleapis.com/auth/calendar.readonly"
    }
}

@Serializable
private data class GoogleTokenResponse(
    val access_token: String,
    val expires_in: Int,
    val refresh_token: String? = null,
    val scope: String? = null,
    val token_type: String? = null,
) {
    val accessToken get() = access_token
    val expiresIn get() = expires_in
    val refreshToken get() = refresh_token
}

/** Result of an OAuth token exchange or refresh. */
data class OAuthTokenResult(
    val accessToken: String,
    val expiresAt: String,
)

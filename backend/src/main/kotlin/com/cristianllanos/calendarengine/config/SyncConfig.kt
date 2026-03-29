package com.cristianllanos.calendarengine.config

import io.ktor.server.application.*

/** Holds Google OAuth credentials and webhook settings for calendar sync. */
data class GoogleOAuthConfig(
    val enabled: Boolean = false,
    val clientId: String = "",
    val clientSecret: String = "",
    val webhookBaseUrl: String = "",
)

/** Configuration data for external calendar synchronization (providers, mode, and polling interval). */
data class SyncConfiguration(
    val google: GoogleOAuthConfig = GoogleOAuthConfig(),
    val appleEnabled: Boolean = false,
    val mode: String = "ON_DEMAND",
    val pollingIntervalMinutes: Int = 15,
)

/** Global singleton holding the active sync configuration, populated from application.conf or the plugin DSL. */
object SyncConfig {
    var config: SyncConfiguration = SyncConfiguration()

    /** Initializes sync configuration from the Ktor [ApplicationEnvironment] properties. */
    fun init(environment: ApplicationEnvironment) {
        config = SyncConfiguration(
            google = GoogleOAuthConfig(
                enabled = environment.config.property("sync.google.enabled").getString().toBoolean(),
                clientId = environment.config.property("sync.google.clientId").getString(),
                clientSecret = environment.config.property("sync.google.clientSecret").getString(),
                webhookBaseUrl = environment.config.property("sync.google.webhookBaseUrl").getString(),
            ),
            appleEnabled = environment.config.property("sync.apple.enabled").getString().toBoolean(),
            mode = environment.config.property("sync.mode").getString(),
            pollingIntervalMinutes = environment.config.property("sync.pollingIntervalMinutes").getString().toInt(),
        )
    }
}

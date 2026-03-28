package com.calendarengine.config

import io.ktor.server.application.*

data class GoogleOAuthConfig(
    val enabled: Boolean,
    val clientId: String,
    val clientSecret: String,
    val webhookBaseUrl: String,
)

data class SyncConfiguration(
    val google: GoogleOAuthConfig,
    val appleEnabled: Boolean,
    val mode: String,
    val pollingIntervalMinutes: Int,
)

object SyncConfig {
    lateinit var config: SyncConfiguration
        private set

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

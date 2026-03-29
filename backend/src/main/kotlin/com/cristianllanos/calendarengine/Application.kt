package com.cristianllanos.calendarengine

import com.cristianllanos.calendarengine.config.DatabaseConfig
import com.cristianllanos.calendarengine.config.EventConfig
import com.cristianllanos.calendarengine.config.SyncConfig
import com.cristianllanos.calendarengine.plugins.*
import io.ktor.server.application.*
import io.ktor.server.netty.*

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module() {
    // Standalone mode: connect to DB from application.conf
    DatabaseConfig.init(environment)

    // Plugins (host app provides these in plugin mode)
    configureCallLogging()
    configureSerialization()
    configureCORS()
    configureStatusPages()

    // Read config from application.conf and install the calendar engine
    SyncConfig.init(environment)
    EventConfig.init(environment)

    install(CalendarEngine) {
        createTables = false // already created by DatabaseConfig.init above

        sync {
            google {
                enabled = SyncConfig.config.google.enabled
                clientId = SyncConfig.config.google.clientId
                clientSecret = SyncConfig.config.google.clientSecret
                webhookBaseUrl = SyncConfig.config.google.webhookBaseUrl
            }
            apple = SyncConfig.config.appleEnabled
            mode = SyncConfig.config.mode
            pollingIntervalMinutes = SyncConfig.config.pollingIntervalMinutes
        }

        events {
            emitter = EventConfig.emitterType
            webhookUrl = EventConfig.webhookTarget
        }
    }
}

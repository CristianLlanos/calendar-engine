package com.cristianllanos.calendarengine

import com.cristianllanos.calendarengine.config.SyncConfig
import com.cristianllanos.calendarengine.config.SyncConfiguration
import com.cristianllanos.calendarengine.config.GoogleOAuthConfig
import com.cristianllanos.calendarengine.config.EventConfig
import com.cristianllanos.calendarengine.config.DatabaseConfig
import com.cristianllanos.calendarengine.container.AppServiceProvider
import com.cristianllanos.calendarengine.modules.sync.SyncPollingScheduler
import com.cristianllanos.calendarengine.routes.*
import com.cristianllanos.container.Container
import com.cristianllanos.container.resolve
import io.ktor.server.application.*
import io.ktor.server.routing.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

val CalendarEngine = createApplicationPlugin(
    name = "CalendarEngine",
    createConfiguration = ::CalendarEngineConfig,
) {
    val config = pluginConfig

    // Apply config to global singletons (for services that read from them)
    SyncConfig.config = SyncConfiguration(
        google = GoogleOAuthConfig(
            enabled = config.syncConfig.googleConfig.enabled,
            clientId = config.syncConfig.googleConfig.clientId,
            clientSecret = config.syncConfig.googleConfig.clientSecret,
            webhookBaseUrl = config.syncConfig.googleConfig.webhookBaseUrl,
        ),
        appleEnabled = config.syncConfig.appleEnabled,
        mode = config.syncConfig.mode,
        pollingIntervalMinutes = config.syncConfig.pollingIntervalMinutes,
    )

    EventConfig.emitterType = config.eventsConfig.emitter
    EventConfig.webhookTarget = config.eventsConfig.webhookUrl

    // Create tables if requested (host app must have an active Exposed DB connection)
    if (config.createTables) {
        DatabaseConfig.createTables()
    }

    // Set up container
    val container = (config.container ?: Container()).apply {
        register(AppServiceProvider())
    }

    // Install routes under the configured prefix
    val prefix = config.routePrefix
    application.routing {
        route(prefix) {
            container.call(this::tenantRoutes)
            container.call(this::calendarRoutes)
            container.call(this::eventRoutes)
            container.call(this::iCalRoutes)
            container.call(this::bookingUrlRoutes)
            container.call(this::availabilityRoutes)
            container.call(this::bookingRoutes)
            container.call(this::syncRoutes)
        }
    }

    // Start polling scheduler
    val scheduler = container.resolve<SyncPollingScheduler>()
    scheduler.start(CoroutineScope(Dispatchers.IO))
}

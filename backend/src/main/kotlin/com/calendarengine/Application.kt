package com.calendarengine

import com.calendarengine.config.DatabaseConfig
import com.calendarengine.config.EventConfig
import com.calendarengine.config.SyncConfig
import com.calendarengine.container.AppServiceProvider
import com.cristianllanos.container.Container
import com.cristianllanos.container.resolve
import com.calendarengine.modules.sync.SyncPollingScheduler
import com.calendarengine.plugins.*
import com.calendarengine.routes.*
import io.ktor.server.application.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module() {
    // Config
    DatabaseConfig.init(environment)
    SyncConfig.init(environment)
    EventConfig.init(environment)

    // Plugins
    configureCallLogging()
    configureSerialization()
    configureCORS()
    configureStatusPages()

    // Container
    val container = Container().apply {
        register(AppServiceProvider())
    }

    // Routes
    routing {
        tenantRoutes(container.resolve())
        calendarRoutes(container.resolve())
        eventRoutes(container.resolve())
        iCalRoutes(container.resolve())
        bookingUrlRoutes(container.resolve())
        availabilityRoutes(container.resolve())
        bookingRoutes(container.resolve())
        syncRoutes(container.resolve(), container.resolve(), container.resolve())
    }

    // Start polling scheduler if configured
    val scheduler = container.resolve<SyncPollingScheduler>()
    scheduler.start(CoroutineScope(Dispatchers.IO))
}

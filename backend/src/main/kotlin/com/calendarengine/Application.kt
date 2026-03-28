package com.calendarengine

import com.calendarengine.config.DatabaseConfig
import com.calendarengine.container.AppServiceProvider
import com.calendarengine.container.Dependencies
import com.calendarengine.container.resolve
import com.calendarengine.plugins.*
import com.calendarengine.routes.*
import io.ktor.server.application.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module() {
    // Config
    DatabaseConfig.init(environment)

    // Plugins
    configureCallLogging()
    configureSerialization()
    configureCORS()
    configureStatusPages()

    // Container
    val container = Dependencies.make().apply {
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
    }
}

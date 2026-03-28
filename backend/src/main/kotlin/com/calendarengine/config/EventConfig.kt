package com.calendarengine.config

import io.ktor.server.application.*

object EventConfig {
    var emitter: String = "TABLE"
        private set
    var webhookUrl: String = ""
        private set

    fun init(environment: ApplicationEnvironment) {
        emitter = environment.config.property("events.emitter").getString()
        webhookUrl = environment.config.property("events.webhookUrl").getString()
    }
}

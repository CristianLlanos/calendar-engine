package com.cristianllanos.calendarengine.config

import io.ktor.server.application.*

object EventConfig {
    var emitterType: String = "TABLE"
    var webhookTarget: String = ""

    fun init(environment: ApplicationEnvironment) {
        emitterType = environment.config.property("events.emitter").getString()
        webhookTarget = environment.config.property("events.webhookUrl").getString()
    }
}

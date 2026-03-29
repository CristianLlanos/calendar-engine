package com.cristianllanos.calendarengine.config

import io.ktor.server.application.*

/** Global singleton holding event notification configuration (emitter type and webhook target). */
object EventConfig {
    var emitterType: String = "TABLE"
    var webhookTarget: String = ""

    /** Initializes event notification settings from the Ktor [ApplicationEnvironment] properties. */
    fun init(environment: ApplicationEnvironment) {
        emitterType = environment.config.property("events.emitter").getString()
        webhookTarget = environment.config.property("events.webhookUrl").getString()
    }
}

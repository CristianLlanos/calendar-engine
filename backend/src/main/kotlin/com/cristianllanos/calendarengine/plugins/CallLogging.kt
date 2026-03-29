package com.cristianllanos.calendarengine.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.callloging.*
import org.slf4j.event.Level

/** Installs the CallLogging plugin with INFO-level logging. */
fun Application.configureCallLogging() {
    install(CallLogging) {
        level = Level.INFO
        disableDefaultColors()
    }
}

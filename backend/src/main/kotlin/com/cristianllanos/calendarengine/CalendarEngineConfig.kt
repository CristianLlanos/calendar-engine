package com.cristianllanos.calendarengine

import com.cristianllanos.container.Container

class CalendarEngineConfig {
    var routePrefix: String = "/api/calendar"
    var container: Container? = null
    var createTables: Boolean = true

    internal var syncConfig = SyncDsl()
    internal var eventsConfig = EventsDsl()

    fun sync(block: SyncDsl.() -> Unit) {
        syncConfig.apply(block)
    }

    fun events(block: EventsDsl.() -> Unit) {
        eventsConfig.apply(block)
    }
}

class SyncDsl {
    var mode: String = "ON_DEMAND"
    var pollingIntervalMinutes: Int = 15

    internal var googleConfig = GoogleDsl()
    internal var appleEnabled: Boolean = false

    fun google(block: GoogleDsl.() -> Unit) {
        googleConfig.apply(block)
    }

    var apple: Boolean
        get() = appleEnabled
        set(value) { appleEnabled = value }
}

class GoogleDsl {
    var enabled: Boolean = false
    var clientId: String = ""
    var clientSecret: String = ""
    var webhookBaseUrl: String = ""
}

class EventsDsl {
    var emitter: String = "TABLE"
    var webhookUrl: String = ""
}

package com.cristianllanos.calendarengine

import com.cristianllanos.container.Container

/**
 * DSL configuration for the CalendarEngine Ktor plugin.
 *
 * Example usage:
 * ```kotlin
 * install(CalendarEngine) {
 *     routePrefix = "/api/calendar"
 *     createTables = true
 *     sync {
 *         google {
 *             enabled = true
 *             clientId = "your-client-id"
 *             clientSecret = "your-secret"
 *         }
 *         mode = "POLLING"
 *     }
 *     events {
 *         emitter = "WEBHOOK"
 *         webhookUrl = "https://example.com/hook"
 *     }
 * }
 * ```
 */
class CalendarEngineConfig {
    var routePrefix: String = "/api/calendar"
    var container: Container? = null
    var createTables: Boolean = true

    internal var syncConfig = SyncDsl()
    internal var eventsConfig = EventsDsl()

    /** Configures calendar sync settings (Google, Apple, polling). */
    fun sync(block: SyncDsl.() -> Unit) {
        syncConfig.apply(block)
    }

    /** Configures event notification settings (emitter type, webhook URL). */
    fun events(block: EventsDsl.() -> Unit) {
        eventsConfig.apply(block)
    }
}

/** DSL block for configuring external calendar sync providers and polling behavior. */
class SyncDsl {
    var mode: String = "ON_DEMAND"
    var pollingIntervalMinutes: Int = 15

    internal var googleConfig = GoogleDsl()
    internal var appleEnabled: Boolean = false

    /** Configures Google Calendar OAuth credentials and webhook settings. */
    fun google(block: GoogleDsl.() -> Unit) {
        googleConfig.apply(block)
    }

    var apple: Boolean
        get() = appleEnabled
        set(value) { appleEnabled = value }
}

/** DSL block for Google Calendar OAuth and webhook configuration. */
class GoogleDsl {
    var enabled: Boolean = false
    var clientId: String = ""
    var clientSecret: String = ""
    var webhookBaseUrl: String = ""
}

/** DSL block for configuring the event notification emitter (table-based or webhook). */
class EventsDsl {
    var emitter: String = "TABLE"
    var webhookUrl: String = ""
}

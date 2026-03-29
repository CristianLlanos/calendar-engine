# Module calendar-engine

Headless multi-tenant calendar engine with bookings, RFC 5545 recurrence, and external calendar sync.

# Package com.cristianllanos.calendarengine

Root package containing the Ktor application entry point, the `CalendarEngine` plugin, and its DSL configuration.

Install the plugin with:

```kotlin
install(CalendarEngine) {
    routePrefix = "/api/calendar"
    sync {
        google { enabled = true; clientId = "..."; clientSecret = "..." }
    }
}
```

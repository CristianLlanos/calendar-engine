# Calendar Engine

Headless multi-tenant calendar engine with bookings, RFC 5545 recurrence, external calendar sync, and a Ktor plugin architecture.

## Maven coordinates

```kotlin
implementation("com.cristianllanos:calendar-engine:0.0.1")
```

## Core concepts

- **Multi-tenancy**: Every request is scoped to a tenant via the `X-Tenant-Id` header. The `AuthMiddleware` extracts this into a `TenantPrincipal`.
- **Ktor plugin**: `CalendarEngine` is a Ktor `ApplicationPlugin` that can run standalone or be embedded into an existing Ktor app via `install(CalendarEngine) { ... }`.
- **DI container**: Uses `com.cristianllanos:container` for dependency injection. Services are registered in `AppServiceProvider` and resolved via `container.resolve<T>()`.
- **Event bus**: Uses `com.cristianllanos:events` for domain events (e.g., `BookingCreatedEvent`, `BookingCancelledEvent`). Emitter type is configurable (`TABLE` or `WEBHOOK`).
- **Exposed ORM**: All database models use JetBrains Exposed with table objects in the `models` package.

## Architecture

```
calendarengine/
  Application.kt           -- Standalone entry point
  CalendarEnginePlugin.kt   -- Ktor plugin (embeddable)
  CalendarEngineConfig.kt   -- DSL configuration
  auth/                     -- Tenant auth middleware
  config/                   -- Database, sync, event config singletons
  container/                -- DI service provider
  dto/                      -- Request/response data classes and helpers
  models/                   -- Exposed table definitions and enums
  modules/
    tenant/                 -- Tenant CRUD service
    calendar/               -- Calendar CRUD service
    event/                  -- Event CRUD, recurrence, iCal export
      actions/              -- CreateEvent, UpdateEvent, DeleteEvent, ExpandOccurrences, UpdateOccurrence
    booking/                -- Booking and BookingUrl services
      actions/              -- CreateBooking, CancelBooking, CalculateAvailability
    sync/                   -- Google Calendar & Apple CalDAV sync
      actions/              -- GoogleOAuth
    notification/           -- Event listeners (table-based storage)
  plugins/                  -- Ktor plugins (serialization, CORS, logging, status pages)
  routes/                   -- Route registration functions
```

## Usage patterns

### Standalone mode

Run directly with `./gradlew run`. Reads config from `application.conf`.

### Plugin mode (embed in existing app)

```kotlin
install(CalendarEngine) {
    routePrefix = "/api/calendar"
    createTables = true
    container = existingContainer  // optional: share a DI container

    sync {
        google {
            enabled = true
            clientId = "..."
            clientSecret = "..."
        }
        apple = true
        mode = "POLLING"
        pollingIntervalMinutes = 10
    }

    events {
        emitter = "WEBHOOK"
        webhookUrl = "https://example.com/hooks"
    }
}
```

### CRUD pattern

Services follow a consistent CRUD pattern. Routes use `crudRoutes<T>()` for standard list/get/create/update/delete with pagination, search, and tenant scoping.

### Recurrence

Events support RFC 5545 RRULE recurrence. `RruleExpander` generates occurrences from rules. `ExpandOccurrencesAction` materializes them for a date range. Individual occurrences can be modified or cancelled via `RecurrenceExceptions`.

### Availability and bookings

`CalculateAvailabilityAction` computes free/busy slots by combining calendar events, existing bookings, and external busy blocks. Supports configurable buffers, lead times, and booking limits per `BookingUrl`.

## API reference

All routes are under the configured `routePrefix` (default: `/api/calendar`):

| Resource         | Path                                        |
|------------------|---------------------------------------------|
| Tenants          | `/tenants`                                  |
| Calendars        | `/calendars`                                |
| Events           | `/events`                                   |
| iCal export      | `/calendars/{id}/ical`                      |
| Booking URLs     | `/booking-urls`                             |
| Availability     | `/booking-urls/{id}/availability`           |
| Bookings         | `/bookings`                                 |
| Sync             | `/sync/google/oauth`, `/sync/google/webhook`|

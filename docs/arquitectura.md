# Arquitectura

## Vision general

Calendar Engine es un sistema de tres capas:

```
Widget (React)  →  SDK (TypeScript)  →  Backend API (Kotlin/Ktor)  →  MySQL
```

- El **backend** expone una API REST multi-tenant
- El **SDK** encapsula las llamadas HTTP con tipos
- El **widget** consume el SDK via hooks y renderiza componentes

## Multi-tenancy

Cada peticion autenticada incluye el header `X-Tenant-Id`. El `AuthMiddleware` extrae un `TenantPrincipal` con el `tenantId`. Todas las consultas a la base de datos filtran por `tenant_id`.

Endpoints publicos (sin autenticacion de tenant):
- `POST /api/calendar/bookings` — crear reserva
- `GET /api/calendar/booking-urls/{id}/availability` — consultar disponibilidad
- `GET /api/calendar/calendars/public` — listar calendarios publicos

## Modulos del backend

```
backend/
  auth/          → AuthMiddleware (extraccion de tenantId)
  config/        → DatabaseConfig, SyncConfig, EventConfig
  container/     → AppServiceProvider (registro de dependencias)
  dto/           → Objetos de transferencia (request/response)
  models/        → Tablas de Exposed ORM (12 tablas)
  modules/
    tenant/      → TenantService
    calendar/    → CalendarService
    event/       → EventService, RruleExpander, ICalExporter, 5 Actions
    booking/     → BookingService, BookingUrlService, 3 Actions
    sync/        → GoogleCalendarSyncService, AppleCalDavSyncService, ConnectionService
    notification/→ Listeners del event bus (tabla system_events)
  plugins/       → Serialization, CORS, CallLogging, StatusPages
  routes/        → 8 archivos de rutas (~30 endpoints)
```

## Inyeccion de dependencias

Usa `com.cristianllanos:container:0.3.0` con resolucion automatica:

```kotlin
// AppServiceProvider.kt
container.singleton<EventService>()          // auto-resuelve constructor
container.singleton<BookingService>()
container.singleton<HttpClient> {            // factory explicita cuando se necesita configuracion
    HttpClient(CIO) { install(ContentNegotiation) { json() } }
}
```

El `Container()` usa `ReflectionAutoResolver` para inspeccionar constructores y resolver parametros automaticamente.

## Event bus

Usa `com.cristianllanos:events:0.2.0`:

```kotlin
// Definir evento
data class BookingCreatedEvent(val tenantId: Int, val booking: BookingResponse) : Event()

// Definir listener
class BookingCreatedTableListener : Listener<BookingCreatedEvent> {
    override fun handle(event: BookingCreatedEvent) { /* persistir en system_events */ }
}

// Suscribir
subscriber.subscribe<BookingCreatedEvent, BookingCreatedTableListener>()

// Emitir (desde CreateBookingAction)
emitter.emit(BookingCreatedEvent(tenantId, response))
```

## Flujo: creacion de reserva

```
Cliente → POST /api/calendar/bookings
  → CreateBookingAction
    → CalculateAvailabilityAction.forDate() (validar slot disponible)
      → Cargar ventanas de disponibilidad
      → Generar slots candidatos
      → Cargar eventos del calendario (expansion RRULE)
      → Cargar busy blocks externos
      → Cargar reservas existentes
      → Filtrar por lead time y rate limits
    → Insertar evento en calendario
    → Insertar booking
    → emitter.emit(BookingCreatedEvent)
      → BookingCreatedTableListener → system_events
  → Retornar BookingResponse
```

## Flujo: sincronizacion externa

```
1. Usuario inicia OAuth → GET /api/calendar/sync/google/auth
2. Google redirige → GET /api/calendar/sync/google/callback
3. GoogleOAuthAction almacena tokens en oauth_tokens
4. Usuario crea conexion → POST /api/calendar/sync/connections
5. Sync (manual, polling, o webhook):
   → GoogleCalendarSyncService.syncConnection()
     → Obtener access token (refresh si expirado)
     → GET Google Calendar API /events (con sync_token si incremental)
     → Convertir a BusyBlock (solo start/end, sin detalles)
     → ExternalBusyBlockService.replaceBlocks() o upsertBlock()
     → Actualizar last_synced_at y sync_token
```

## Flujo: expansion de recurrencia

```
GET /api/calendar/calendars/{id}/events?start=&end=
  → ExpandOccurrencesAction
    → Consultar eventos del calendario
    → Para cada evento recurrente:
      → RecurrenceRules → obtener RRULE y dtstart
      → RruleExpander.expand(rrule, dtstart, timezone, rangeStart, rangeEnd)
        → lib-recur: Within(rangeStart, rangeEnd, OfRuleAndFirst(rule, start))
      → RecurrenceExceptions → filtrar exclusiones, aplicar overrides
    → Combinar con eventos normales
    → Ordenar por startTime
    → Retornar List<EventOccurrence>
```

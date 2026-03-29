# Backend

## Stack tecnologico

| Dependencia | Version | Uso |
|-------------|---------|-----|
| Ktor | 2.3.12 | Framework HTTP |
| Exposed ORM | 0.52.0 | Acceso a base de datos |
| MySQL | 8.0 | Base de datos |
| HikariCP | 5.1.0 | Pool de conexiones (10 max, REPEATABLE_READ) |
| lib-recur | 0.17.1 | Expansion de RRULE (RFC 5545) |
| container | 0.3.0 | Inyeccion de dependencias |
| events | 0.2.0 | Event bus con listeners |
| Ktor Client (CIO) | 2.3.12 | Llamadas HTTP (Google API, CalDAV) |
| kotlinx.serialization | - | Serializacion JSON |

## Estructura del codigo

| Paquete | Contenido |
|---------|-----------|
| `auth/` | `AuthMiddleware` — extraccion de `TenantPrincipal` del header `X-Tenant-Id` |
| `config/` | `DatabaseConfig`, `SyncConfig`, `EventConfig` |
| `container/` | `AppServiceProvider` — registro de todos los servicios |
| `dto/` | Objetos request/response (10 archivos) |
| `models/` | Tablas de Exposed (12) + enums (6) |
| `modules/` | Logica de negocio organizada por dominio |
| `plugins/` | Configuracion de Ktor (Serialization, CORS, CallLogging, StatusPages) |
| `routes/` | Definicion de endpoints (8 archivos) |

## Configuracion

El archivo `application.conf` usa formato HOCON con sobreescritura via variables de entorno:

```hocon
ktor {
    deployment { port = 7730 }
}
database {
    url = "jdbc:mysql://localhost:7706/calendar_engine"
    driver = "com.mysql.cj.jdbc.Driver"
    user = "calendar_engine"
    password = "calendar_engine"
}
sync {
    google { enabled = false, clientId = "", clientSecret = "" }
    apple { enabled = false }
    mode = "ON_DEMAND"            # ON_DEMAND | POLLING | WEBHOOK
    pollingIntervalMinutes = 15
}
events {
    emitter = "TABLE"             # TABLE | WEBHOOK
    webhookUrl = ""
}
```

Todas las propiedades son sobreescribibles con `${?VARIABLE_NAME}`.

## Plugins de Ktor

- **Serialization**: JSON con `prettyPrint`, `ignoreUnknownKeys`, `encodeDefaults`
- **CORS**: todos los metodos y headers, `anyHost()`
- **CallLogging**: nivel INFO
- **StatusPages**: mapeo de excepciones a codigos HTTP:

| Excepcion | Codigo HTTP |
|-----------|-------------|
| `UnauthorizedException` | 401 |
| `ForbiddenException` | 403 |
| `ConflictException` | 409 |
| `NoSuchElementException` | 404 |
| `IllegalArgumentException` | 400 |
| `Throwable` (generico) | 500 |

## Autenticacion

`AuthMiddleware` extrae el `tenantId` del header `X-Tenant-Id`:

```kotlin
fun ApplicationCall.tenantPrincipal(): TenantPrincipal
fun ApplicationCall.pathParam(name: String): Int
```

No hay JWT ni roles por ahora — la autenticacion se delega a la aplicacion consumidora.

## Modulos

### Tenant

`TenantService`: create, getById, update. Campos: `name`, `slug` (unico), `config` (JSON libre).

### Calendar

`CalendarService`: CRUD completo + `getPublicCalendars()`. Cada calendario tiene `timezone` y `visibility` (PRIVATE/PUBLIC). Validacion de conflictos al eliminar (verifica eventos y booking URLs asociados).

### Event

Modulo complejo con patron Action:

| Componente | Responsabilidad |
|------------|----------------|
| `EventService` | Orquestador: delega a las acciones |
| `CreateEventAction` | Crea evento + recurrence_rule si es recurrente |
| `UpdateEventAction` | Actualiza campos del evento |
| `DeleteEventAction` | Elimina con scope: `ALL`, `THIS` (excluir ocurrencia), `FOLLOWING` (truncar RRULE) |
| `UpdateOccurrenceAction` | Modifica una ocurrencia via recurrence_exceptions |
| `ExpandOccurrencesAction` | Expande RRULE en rango, aplica excepciones |
| `RruleExpander` | Wrapper de lib-recur para expansion de RRULE |
| `ICalExporter` | Genera contenido VCALENDAR/VEVENT |

### Booking

| Componente | Responsabilidad |
|------------|----------------|
| `BookingUrlService` | CRUD de URLs de reserva + ventanas de disponibilidad |
| `BookingService` | Orquestador: list, getById, create, cancel |
| `CalculateAvailabilityAction` | Algoritmo de 10 pasos para calcular slots disponibles |
| `CreateBookingAction` | Valida disponibilidad, crea booking + evento, emite evento |
| `CancelBookingAction` | Cancela booking, elimina evento, emite evento |

Ver [Sistema de reservas](sistema-de-reservas.md) para detalle del algoritmo.

### Sync

| Componente | Responsabilidad |
|------------|----------------|
| `GoogleOAuthAction` | Flujo OAuth: auth URL, code exchange, token refresh |
| `GoogleCalendarSyncService` | Sync completa e incremental via sync tokens |
| `AppleCalDavSyncService` | Sync via REPORT CalDAV |
| `ConnectionService` | CRUD de conexiones externas |
| `ExternalBusyBlockService` | Persistencia de bloques ocupados (opaco, sin detalles) |
| `SyncPollingScheduler` | Coroutine periodica para modo POLLING |

Ver [Sincronizacion externa](sincronizacion-externa.md) para detalle.

### Notification

Listeners del event bus que persisten eventos en `system_events`:

- `BookingCreatedTableListener` — maneja `BookingCreatedEvent`
- `BookingCancelledTableListener` — maneja `BookingCancelledEvent`

Ver [Sistema de eventos](sistema-de-eventos.md) para detalle.

## Ejecucion de tests

```bash
cd backend
./gradlew test
```

Tests incluidos:
- `RruleExpanderTest` — 10 tests de expansion RRULE
- `AvailabilityTest` — 6 tests de solapamiento de intervalos

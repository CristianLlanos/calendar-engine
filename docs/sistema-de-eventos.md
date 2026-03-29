# Sistema de eventos

## Arquitectura

Calendar Engine usa `com.cristianllanos:events:0.2.0` como event bus interno. El patron es:

1. **Event** — clase de datos que extiende `Event()` y representa algo que ocurrio
2. **Listener** — interfaz con `handle(event)` que reacciona a un tipo de evento
3. **Subscriber** — registra listeners para tipos de evento
4. **Emitter** — emite eventos hacia los listeners suscritos

```kotlin
// El event bus se resuelve desde el container
val emitter = container.resolve<Emitter>()
val subscriber = container.resolve<Subscriber>()
```

## Eventos del sistema

### BookingCreatedEvent

Se emite cuando se crea una reserva exitosamente.

```kotlin
data class BookingCreatedEvent(
    val tenantId: Int,
    val booking: BookingResponse,  // datos completos de la reserva
) : Event()
```

### BookingCancelledEvent

Se emite cuando se cancela una reserva.

```kotlin
data class BookingCancelledEvent(
    val tenantId: Int,
    val booking: BookingResponse,  // datos completos incluyendo razon de cancelacion
) : Event()
```

## Listeners implementados

### BookingCreatedTableListener

Persiste el evento en la tabla `system_events`:

```kotlin
class BookingCreatedTableListener : Listener<BookingCreatedEvent> {
    override fun handle(event: BookingCreatedEvent) {
        // INSERT INTO system_events (tenant_id, event_type, payload, processed, created_at)
        // VALUES (event.tenantId, "BOOKING_CREATED", json(event.booking), false, now())
    }
}
```

### BookingCancelledTableListener

Mismo patron, con `event_type = "BOOKING_CANCELLED"`.

## Suscripcion

Los listeners se suscriben en `AppServiceProvider`:

```kotlin
container.singleton<BookingCreatedTableListener>()
container.singleton<BookingCancelledTableListener>()

val subscriber = container.resolve<Subscriber>()
subscriber.subscribe<BookingCreatedEvent, BookingCreatedTableListener>()
subscriber.subscribe<BookingCancelledEvent, BookingCancelledTableListener>()
```

## Modos de entrega

Configurado via `EVENT_EMITTER` en `application.conf`:

### TABLE (default)

Los listeners almacenan eventos en `system_events`. Un consumidor externo los lee por polling:

```sql
SELECT * FROM system_events
WHERE processed = false
ORDER BY created_at ASC;
```

Despues de procesar, marcar como procesado:

```sql
UPDATE system_events SET processed = true WHERE id = ?;
```

### WEBHOOK

Configurar:
```
EVENT_EMITTER=WEBHOOK
EVENT_WEBHOOK_URL=https://tu-servidor/webhook
```

En modo webhook, se pueden reemplazar los listeners de tabla por listeners de webhook que envian HTTP POST a la URL configurada con:
- Header `X-Tenant-Id`
- Header `X-Event-Type` (BOOKING_CREATED, BOOKING_CANCELLED)
- Body: JSON con el payload del evento

## Agregar un nuevo evento

### 1. Definir el evento

```kotlin
// dto/SystemEvent.kt
data class EventCancelledEvent(
    val tenantId: Int,
    val eventId: Int,
    val calendarId: Int,
) : Event()
```

### 2. Crear el listener

```kotlin
// modules/notification/EventCancelledTableListener.kt
class EventCancelledTableListener : Listener<EventCancelledEvent> {
    override fun handle(event: EventCancelledEvent) {
        transaction {
            SystemEvents.insert {
                it[tenantId] = event.tenantId
                it[eventType] = "EVENT_CANCELLED"
                it[payload] = Json.encodeToString(mapOf(
                    "eventId" to event.eventId,
                    "calendarId" to event.calendarId,
                ))
                it[processed] = false
                it[createdAt] = LocalDateTime.now()
            }
        }
    }
}
```

### 3. Registrar y suscribir

```kotlin
// container/AppServiceProvider.kt
container.singleton<EventCancelledTableListener>()

val subscriber = container.resolve<Subscriber>()
subscriber.subscribe<EventCancelledEvent, EventCancelledTableListener>()
```

### 4. Emitir desde la logica de negocio

```kotlin
// modules/event/actions/DeleteEventAction.kt
emitter.emit(EventCancelledEvent(tenantId, eventId, calendarId))
```

Se pueden suscribir multiples listeners al mismo evento (ej: un listener para tabla y otro para webhook).

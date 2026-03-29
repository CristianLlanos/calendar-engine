# Referencia API

## Informacion general

- **URL base**: `http://localhost:7730`
- **Content-Type**: `application/json`
- **Autenticacion**: header `X-Tenant-Id` (obligatorio salvo endpoints publicos)
- **Paginacion**: query params `limit` (default 50, max 100), `offset`, `sortBy`, `sortDir` (asc/desc), `search`

---

## Tenants

### Crear tenant

```
POST /api/tenants
```

```json
{
  "name": "Mi Empresa",
  "slug": "mi-empresa",
  "config": "{\"timezone\": \"America/Mexico_City\"}"
}
```

Respuesta `201`:
```json
{
  "id": 1,
  "name": "Mi Empresa",
  "slug": "mi-empresa",
  "config": "{\"timezone\": \"America/Mexico_City\"}",
  "createdAt": "2026-03-28T12:00:00"
}
```

### Obtener tenant

```
GET /api/tenants/{id}
```

### Actualizar tenant

```
PUT /api/tenants/{id}
```

Body parcial (solo los campos a modificar).

---

## Calendarios

Requiere `X-Tenant-Id`.

### Listar calendarios

```
GET /api/calendars
```

Respuesta paginada: `{ items, total, limit, offset, hasMore }`.

### Crear calendario

```
POST /api/calendars
```

```json
{
  "name": "Consultas",
  "description": "Calendario de consultas",
  "timezone": "America/Mexico_City",
  "color": "#6366f1",
  "visibility": "PUBLIC"
}
```

### Obtener calendario

```
GET /api/calendars/{id}
```

### Actualizar calendario

```
PUT /api/calendars/{id}
```

### Eliminar calendario

```
DELETE /api/calendars/{id}
```

Retorna `204`. Falla con `409` si tiene eventos o booking URLs asociados.

### Listar calendarios publicos

```
GET /api/calendars/public
```

No requiere `X-Tenant-Id`. Retorna calendarios con `visibility = PUBLIC` de cualquier tenant.

---

## Eventos

Requiere `X-Tenant-Id`.

### Listar ocurrencias

```
GET /api/calendars/{calendarId}/events?start=2026-04-01T00:00:00&end=2026-04-30T23:59:59
```

Retorna `EventOccurrence[]` con eventos normales y ocurrencias expandidas de eventos recurrentes.

```json
[
  {
    "eventId": 1,
    "calendarId": 1,
    "title": "Reunion semanal",
    "startTime": "2026-04-01T09:00:00",
    "endTime": "2026-04-01T10:00:00",
    "allDay": false,
    "status": "CONFIRMED",
    "isRecurring": true,
    "isException": false,
    "originalDate": "2026-04-01T09:00:00"
  }
]
```

### Crear evento

```
POST /api/calendars/{calendarId}/events
```

Evento simple:
```json
{
  "title": "Cita",
  "startTime": "2026-04-15T14:00:00",
  "endTime": "2026-04-15T15:00:00"
}
```

Evento recurrente:
```json
{
  "title": "Reunion semanal",
  "startTime": "2026-04-01T09:00:00",
  "endTime": "2026-04-01T10:00:00",
  "recurrence": {
    "rrule": "FREQ=WEEKLY;BYDAY=MO,WE,FR",
    "timezone": "America/Mexico_City"
  }
}
```

### Obtener evento

```
GET /api/calendars/{calendarId}/events/{id}
```

### Actualizar evento

```
PUT /api/calendars/{calendarId}/events/{id}
```

### Actualizar una ocurrencia

```
PUT /api/calendars/{calendarId}/events/{id}/occurrence/{date}
```

```json
{
  "title": "Reunion especial",
  "startTime": "2026-04-08T10:00:00",
  "endTime": "2026-04-08T11:00:00"
}
```

### Eliminar evento

```
DELETE /api/calendars/{calendarId}/events/{id}?scope=ALL
```

Scopes:
- `ALL` — elimina el evento completo
- `THIS` — excluye una ocurrencia: `?scope=THIS&occurrenceDate=2026-04-08T09:00:00`
- `FOLLOWING` — trunca la recurrencia desde una fecha: `?scope=FOLLOWING&occurrenceDate=2026-04-15T09:00:00`

### Exportar iCal

```
GET /api/calendars/{calendarId}/export.ics
```

Retorna `Content-Type: text/calendar` con formato VCALENDAR.

---

## URLs de reserva

Requiere `X-Tenant-Id`.

### Listar

```
GET /api/booking-urls
```

### Crear

```
POST /api/booking-urls
```

```json
{
  "calendarId": 1,
  "slug": "consulta-30min",
  "name": "Consulta 30 minutos",
  "durationMinutes": 30,
  "durationOptions": "[15,30,60]",
  "bufferBeforeMinutes": 5,
  "bufferAfterMinutes": 10,
  "minLeadTimeHours": 2,
  "maxLeadTimeDays": 30,
  "maxBookingsPerDay": 8,
  "autoConfirm": true,
  "availabilityWindows": [
    { "dayOfWeek": 1, "startTime": "09:00", "endTime": "13:00" },
    { "dayOfWeek": 1, "startTime": "15:00", "endTime": "18:00" },
    { "dayOfWeek": 2, "startTime": "09:00", "endTime": "18:00" },
    { "dayOfWeek": 3, "startTime": "09:00", "endTime": "18:00" },
    { "dayOfWeek": 4, "startTime": "09:00", "endTime": "18:00" },
    { "dayOfWeek": 5, "startTime": "09:00", "endTime": "13:00" }
  ]
}
```

`dayOfWeek`: 1=lunes, 2=martes, ..., 7=domingo.

### Obtener / Actualizar / Eliminar

```
GET /api/booking-urls/{id}
PUT /api/booking-urls/{id}
DELETE /api/booking-urls/{id}
```

---

## Disponibilidad (publico)

No requiere autenticacion.

### Disponibilidad de un dia

```
GET /api/booking-urls/{id}/availability?date=2026-04-15
```

```json
{
  "date": "2026-04-15",
  "slots": [
    {
      "startTime": "2026-04-15T09:00",
      "endTime": "2026-04-15T09:30",
      "availableDurations": [15, 30]
    },
    {
      "startTime": "2026-04-15T09:45",
      "endTime": "2026-04-15T10:15",
      "availableDurations": [15, 30, 60]
    }
  ]
}
```

### Disponibilidad de un rango

```
GET /api/booking-urls/{id}/availability/range?start=2026-04-15&end=2026-04-20
```

Retorna `DayAvailability[]`. Maximo 60 dias por consulta.

---

## Reservas

### Crear reserva (publico)

```
POST /api/bookings
```

```json
{
  "bookingUrlId": 1,
  "startTime": "2026-04-15T09:00:00",
  "durationMinutes": 30,
  "bookerName": "Juan Perez",
  "bookerEmail": "juan@ejemplo.com",
  "bookerPhone": "+52 555 123 4567",
  "notes": "Primera consulta"
}
```

Respuesta `201`:
```json
{
  "id": 1,
  "tenantId": 1,
  "bookingUrlId": 1,
  "calendarId": 1,
  "eventId": 5,
  "status": "CONFIRMED",
  "startTime": "2026-04-15T09:00",
  "endTime": "2026-04-15T09:30",
  "bookerName": "Juan Perez",
  "bookerEmail": "juan@ejemplo.com",
  "bookerPhone": "+52 555 123 4567",
  "notes": "Primera consulta",
  "createdAt": "2026-03-28T12:00:00"
}
```

### Listar reservas

```
GET /api/bookings
```

Requiere `X-Tenant-Id`. Respuesta paginada.

### Obtener reserva

```
GET /api/bookings/{id}
```

### Cancelar reserva

```
DELETE /api/bookings/{id}
```

Body opcional:
```json
{ "reason": "El cliente cancelo" }
```

---

## Sincronizacion

Requiere `X-Tenant-Id` (excepto webhook).

### Iniciar OAuth de Google

```
GET /api/sync/google/auth?calendarId=1
```

Redirige al consent screen de Google.

### Callback de OAuth

```
GET /api/sync/google/callback?code={code}&state={tenantId}:{calendarId}
```

### Listar conexiones

```
GET /api/sync/connections
```

### Crear conexion

```
POST /api/sync/connections
```

```json
{
  "calendarId": 1,
  "provider": "GOOGLE",
  "externalCalendarId": "usuario@gmail.com",
  "syncMode": "ON_DEMAND"
}
```

### Eliminar conexion

```
DELETE /api/sync/connections/{id}
```

### Forzar sincronizacion

```
POST /api/sync/connections/{id}/sync
```

### Webhook de Google

```
POST /api/sync/webhooks/google
```

Headers: `X-Goog-Channel-ID: ce-{connectionId}`

---

## Codigos de error

| Codigo | Significado |
|--------|-------------|
| 400 | Parametro invalido o faltante |
| 401 | Header `X-Tenant-Id` faltante o invalido |
| 404 | Recurso no encontrado |
| 409 | Conflicto (ej: eliminar calendario con eventos) |
| 500 | Error interno del servidor |

# Sistema de reservas

## Conceptos

- **BookingUrl**: configuracion de un "tipo de cita" vinculado a un calendario. Define duracion, restricciones y ventanas de disponibilidad.
- **Slot**: franja horaria disponible para reservar, calculada por el algoritmo de disponibilidad.
- **Booking**: reserva confirmada que crea un evento en el calendario automaticamente.

## Configuracion de una URL de reserva

| Campo | Tipo | Default | Descripcion |
|-------|------|---------|-------------|
| `calendarId` | int | - | Calendario asociado |
| `slug` | string | - | Identificador unico por tenant (para URLs amigables) |
| `name` | string | - | Nombre descriptivo |
| `durationMinutes` | int | - | Duracion por defecto de la cita |
| `durationOptions` | string | null | JSON array de duraciones permitidas, ej: `"[15,30,60]"` |
| `bufferBeforeMinutes` | int | 0 | Minutos de espacio antes de cada cita |
| `bufferAfterMinutes` | int | 0 | Minutos de espacio despues de cada cita |
| `minLeadTimeHours` | int | 1 | Minimo de horas de antelacion para reservar |
| `maxLeadTimeDays` | int | 60 | Maximo de dias en el futuro para reservar |
| `maxBookingsPerDay` | int | null | Limite de reservas por dia (null = sin limite) |
| `maxBookingsPerWeek` | int | null | Limite de reservas por semana |
| `autoConfirm` | bool | true | Si las reservas se confirman automaticamente |

### Ventanas de disponibilidad

Cada BookingUrl tiene ventanas horarias por dia de la semana:

```json
{
  "availabilityWindows": [
    { "dayOfWeek": 1, "startTime": "09:00", "endTime": "13:00" },
    { "dayOfWeek": 1, "startTime": "15:00", "endTime": "18:00" },
    { "dayOfWeek": 2, "startTime": "09:00", "endTime": "18:00" }
  ]
}
```

`dayOfWeek`: 1=lunes, 2=martes, ..., 7=domingo.

Un dia sin ventanas no tiene disponibilidad.

## Algoritmo de disponibilidad

`CalculateAvailabilityAction` calcula los slots disponibles para un dia en 10 pasos:

### Paso 1: Cargar configuracion

Leer del BookingUrl: duraciones, buffers, lead times, limites.

### Paso 2: Cargar ventanas

Obtener las `booking_url_availability_windows` para el dia de la semana solicitado. Si no hay ventanas, retornar vacio.

### Paso 3: Generar slots candidatos

Para cada ventana, generar slots avanzando por `duracion_minima + buffer_before + buffer_after`:

```
Ventana: 09:00 - 13:00, duracion 30min, buffer before 5min, buffer after 10min
Slots: 09:00, 09:45, 10:30, 11:15, 12:00 (el siguiente seria 12:45 + 30 = 13:15 > 13:00)
```

### Paso 4: Cargar eventos bloqueantes

Consultar todos los eventos del calendario para ese dia, incluyendo:
- Eventos simples que se solapan con el dia
- Eventos recurrentes expandidos via `RruleExpander` (excluyendo ocurrencias eliminadas)

### Paso 5: Cargar busy blocks externos

Consultar `external_busy_blocks` del calendario para ese dia. Estos provienen de la sincronizacion con Google Calendar o Apple CalDAV.

### Paso 6: Cargar reservas existentes

Consultar `bookings` del BookingUrl con status distinto a CANCELLED.

### Paso 7: Combinar intervalos bloqueados

Unificar todos los intervalos:
- Eventos del calendario
- Busy blocks externos
- Reservas existentes (con buffers: `start - bufferBefore`, `end + bufferAfter`)

### Paso 8: Filtrar por lead time

Eliminar slots donde:
- `startTime < ahora + minLeadTimeHours`
- `startTime > ahora + maxLeadTimeDays`

### Paso 9: Verificar limites de tasa

- Si `maxBookingsPerDay` esta definido y ya se alcanzo el limite, retornar vacio
- Si `maxBookingsPerWeek` esta definido y ya se alcanzo el limite semanal (lunes a domingo), retornar vacio

### Paso 10: Evaluar duraciones por slot

Para cada slot candidato, verificar cuales de las `durationOptions` caben sin solaparse con ningun intervalo bloqueado. Si ninguna duracion cabe, descartar el slot.

Resultado: `DayAvailability` con lista de `AvailableSlot`:

```json
{
  "date": "2026-04-15",
  "slots": [
    { "startTime": "2026-04-15T09:00", "endTime": "2026-04-15T09:30", "availableDurations": [15, 30] },
    { "startTime": "2026-04-15T10:30", "endTime": "2026-04-15T11:00", "availableDurations": [15, 30, 60] }
  ]
}
```

## Flujo de reserva

1. El cliente consulta disponibilidad (`GET /api/calendar/booking-urls/{id}/availability?date=...`)
2. El cliente selecciona un slot y envia `POST /api/calendar/bookings` con:
   - `bookingUrlId`, `startTime`, `durationMinutes`
   - `bookerName`, `bookerEmail`, `bookerPhone` (opcional)
3. El backend valida que el slot sigue disponible
4. Crea un evento en el calendario ("Booking: {nombre}")
5. Crea el registro de booking vinculado al evento
6. Emite `BookingCreatedEvent` via el event bus
7. Retorna `BookingResponse` con status CONFIRMED o PENDING_APPROVAL

## Cancelacion

```
DELETE /api/calendar/bookings/{id}
Body: { "reason": "motivo" }  // opcional
```

1. Marca el booking como CANCELLED con `cancelled_at` y `cancellation_reason`
2. Elimina el evento auto-creado del calendario
3. Emite `BookingCancelledEvent`

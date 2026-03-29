# SDK TypeScript

## Instalacion

```bash
npm install @calendarengine/sdk
```

## Configuracion

```typescript
import { createClient } from "@calendarengine/sdk";

const client = createClient({
  baseUrl: "http://localhost:7730",
  getTenantId: () => 1,
  getToken: () => localStorage.getItem("token"),  // opcional
  onUnauthorized: () => { window.location.href = "/login"; },  // opcional
});
```

| Opcion | Tipo | Descripcion |
|--------|------|-------------|
| `baseUrl` | `string` | URL del backend (obligatorio) |
| `getToken` | `() => string \| null` | Funcion que retorna token Bearer |
| `getTenantId` | `() => number \| string` | Funcion que retorna tenant ID |
| `onUnauthorized` | `() => void` | Callback cuando el servidor retorna 401 |

## Manejo de errores

```typescript
import { ApiError } from "@calendarengine/sdk";

try {
  await client.calendars.create({ name: "Test" });
} catch (err) {
  if (err instanceof ApiError) {
    console.log(err.status);  // 400, 404, 409, etc.
    console.log(err.message); // mensaje del servidor
    console.log(err.body);    // body completo de la respuesta
  }
}
```

---

## Modulo calendars

```typescript
// Listar (paginado)
const result = await client.calendars.list({ limit: 20, search: "consulta" });
// result: { items: CalendarResponse[], total, limit, offset, hasMore }

// Obtener uno
const calendar = await client.calendars.get(1);

// Listar publicos (sin tenant)
const publicCals = await client.calendars.listPublic();

// Crear
const created = await client.calendars.create({
  name: "Consultas",
  timezone: "America/Mexico_City",
  visibility: "PUBLIC",
});

// Actualizar
await client.calendars.update(1, { name: "Nuevo nombre" });

// Eliminar
await client.calendars.delete(1);
```

## Modulo events

```typescript
// Listar ocurrencias en rango
const events = await client.events.list(1, "2026-04-01T00:00:00", "2026-04-30T23:59:59");
// events: EventOccurrence[]

// Crear evento simple
const event = await client.events.create(1, {
  title: "Cita",
  startTime: "2026-04-15T14:00:00",
  endTime: "2026-04-15T15:00:00",
});

// Crear evento recurrente
const recurring = await client.events.create(1, {
  title: "Reunion semanal",
  startTime: "2026-04-01T09:00:00",
  endTime: "2026-04-01T10:00:00",
  recurrence: { rrule: "FREQ=WEEKLY;BYDAY=MO,WE,FR" },
});

// Actualizar
await client.events.update(1, eventId, { title: "Nuevo titulo" });

// Modificar una ocurrencia
await client.events.updateOccurrence(1, eventId, "2026-04-08T09:00:00", {
  title: "Reunion especial",
  startTime: "2026-04-08T10:00:00",
  endTime: "2026-04-08T11:00:00",
});

// Eliminar
await client.events.delete(1, eventId);                           // ALL
await client.events.delete(1, eventId, "THIS", "2026-04-08T09:00:00");   // una ocurrencia
await client.events.delete(1, eventId, "FOLLOWING", "2026-04-15T09:00:00"); // siguientes

// Exportar iCal
const ical = await client.events.exportIcal(1);
```

## Modulo bookingUrls

```typescript
// CRUD
const urls = await client.bookingUrls.list();
const url = await client.bookingUrls.get(1);
const created = await client.bookingUrls.create({
  calendarId: 1,
  slug: "consulta-30min",
  name: "Consulta 30 minutos",
  durationMinutes: 30,
  availabilityWindows: [
    { dayOfWeek: 1, startTime: "09:00", endTime: "18:00" },
    { dayOfWeek: 2, startTime: "09:00", endTime: "18:00" },
  ],
});
await client.bookingUrls.update(1, { maxBookingsPerDay: 10 });
await client.bookingUrls.delete(1);

// Disponibilidad de un dia
const day = await client.bookingUrls.getAvailability(1, "2026-04-15");
// day: { date: "2026-04-15", slots: [{ startTime, endTime, availableDurations }] }

// Disponibilidad de un rango
const range = await client.bookingUrls.getAvailabilityRange(1, "2026-04-15", "2026-04-20");
// range: DayAvailability[]
```

## Modulo bookings

```typescript
// Crear reserva
const booking = await client.bookings.create({
  bookingUrlId: 1,
  startTime: "2026-04-15T09:00:00",
  durationMinutes: 30,
  bookerName: "Juan Perez",
  bookerEmail: "juan@ejemplo.com",
  bookerPhone: "+52 555 123 4567",
});

// Listar
const bookings = await client.bookings.list({ sortBy: "startTime", sortDir: "asc" });

// Obtener
const one = await client.bookings.get(1);

// Cancelar
await client.bookings.cancel(1, "El cliente cancelo");
```

## Modulo sync

```typescript
// URL de autorizacion de Google (para redirigir al usuario)
const authUrl = client.sync.initiateGoogleAuth(calendarId);

// Listar conexiones
const connections = await client.sync.listConnections();

// Crear conexion
const conn = await client.sync.createConnection({
  calendarId: 1,
  provider: "GOOGLE",
  externalCalendarId: "usuario@gmail.com",
  syncMode: "ON_DEMAND",
});

// Eliminar conexion
await client.sync.removeConnection(1);

// Forzar sincronizacion
await client.sync.triggerSync(1);
```

## Tipos exportados

| Tipo | Archivo |
|------|---------|
| `PaginatedResponse<T>`, `PaginationParams` | `types/common.ts` |
| `CalendarResponse`, `CreateCalendarRequest`, `UpdateCalendarRequest` | `types/calendar.ts` |
| `EventResponse`, `EventOccurrence`, `CreateEventRequest`, `UpdateEventRequest`, `UpdateOccurrenceRequest`, `DeleteScope` | `types/event.ts` |
| `BookingUrlResponse`, `CreateBookingUrlRequest`, `UpdateBookingUrlRequest`, `AvailabilityWindowResponse` | `types/booking-url.ts` |
| `BookingResponse`, `CreateBookingRequest`, `CancelBookingRequest` | `types/booking.ts` |
| `AvailableSlot`, `DayAvailability` | `types/availability.ts` |
| `ExternalCalendarConnection`, `CreateConnectionRequest` | `types/sync.ts` |

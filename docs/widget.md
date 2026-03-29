# Widget React

## Instalacion

```bash
npm install @calendarengine/widget @calendarengine/sdk
```

Compatible con React 18 y 19.

## CalendarEngineProvider

Componente raiz obligatorio que provee la configuracion del SDK y el tema:

```tsx
import { CalendarEngineProvider } from "@calendarengine/widget";
import "@calendarengine/widget/styles.css";

<CalendarEngineProvider
  baseUrl="http://localhost:7730"
  tenantId={1}
  token="jwt-token"      // opcional
  role="owner"            // owner | member | viewer (default: viewer)
  theme={{ "--ce-primary": "#e11d48" }}  // opcional, tema parcial
>
  {children}
</CalendarEngineProvider>
```

| Prop | Tipo | Default | Descripcion |
|------|------|---------|-------------|
| `baseUrl` | `string` | - | URL del backend (obligatorio) |
| `tenantId` | `number` | - | ID del tenant |
| `token` | `string` | - | Token Bearer para autenticacion |
| `role` | `CalendarRole` | `"viewer"` | Rol del usuario |
| `theme` | `Partial<CalendarTheme>` | - | Sobreescritura parcial del tema |

**Roles:**
- `owner` — puede crear y editar eventos (EventForm visible)
- `member` — puede ver todo y reservar
- `viewer` — solo lectura y reservas

---

## Componentes de vista

### MonthView

Vista mensual con grilla de 6 semanas:

```tsx
<MonthView
  calendarId={1}
  onEventClick={(event) => console.log(event)}
  onDateClick={(date) => console.log(date)}
/>
```

### WeekView

Vista semanal con grilla horaria (7:00 - 22:00):

```tsx
<WeekView calendarId={1} onEventClick={(event) => {}} />
```

### DayView

Vista diaria con grilla horaria:

```tsx
<DayView calendarId={1} onEventClick={(event) => {}} />
```

### MergedCalendarView

Combina eventos de multiples calendarios con codigos de color:

```tsx
<MergedCalendarView
  calendarIds={[1, 2, 3]}
  start="2026-04-01T00:00:00"
  end="2026-04-30T23:59:59"
  onEventClick={(event) => {}}
/>
```

---

## Componentes de reserva

### BookingSlotPicker

Flujo completo de reserva: seleccion de dia, seleccion de slot, formulario de datos y confirmacion.

```tsx
<BookingSlotPicker bookingUrlId={1} initialDate={new Date()} />
```

Internamente usa `useAvailabilitySlots` y `useBookingForm`. Incluye:
1. Navegacion por dia (anterior/siguiente)
2. Lista de slots disponibles (`SlotButton`)
3. Formulario de confirmacion (`BookingConfirmation`)
4. Pantalla de exito con opcion de reservar otra vez

### SlotButton

Boton individual para cada slot disponible:

```tsx
<SlotButton slot={slot} selected={false} onClick={(s) => {}} />
```

---

## Componentes de eventos

### EventForm

Formulario de creacion de eventos. Solo visible cuando `role="owner"`:

```tsx
<EventForm
  calendarId={1}
  initialDate="2026-04-15T09:00"
  onCreated={(event) => console.log("Creado:", event)}
  onCancel={() => {}}
/>
```

Incluye campos para titulo, fecha/hora inicio y fin, descripcion, ubicacion, todo el dia, y opcion de hacer el evento recurrente (activa `RecurrenceEditor`).

### RecurrenceEditor

Editor visual de reglas RRULE:

```tsx
<RecurrenceEditor value={rrule} onChange={(newRrule) => setRrule(newRrule)} />
```

Permite configurar:
- Frecuencia (diaria, semanal, mensual, anual)
- Intervalo (cada N periodos)
- Dias de la semana (para frecuencia semanal)
- Fin: nunca, despues de N ocurrencias, o en una fecha

### EventList

Lista cronologica de eventos agrupada por dia:

```tsx
<EventList
  calendarId={1}
  start="2026-04-01T00:00:00"
  end="2026-04-30T23:59:59"
  onEventClick={(event) => {}}
/>
```

### EventChip

Chip compacto que muestra un evento (hora + titulo):

```tsx
<EventChip event={occurrence} onClick={(event) => {}} />
```

### CalendarHeader

Barra de navegacion con botones prev/today/next y selector de vista:

```tsx
<CalendarHeader
  currentDate={date}
  view="month"
  onViewChange={(view) => {}}
  onPrev={() => {}}
  onNext={() => {}}
  onToday={() => {}}
/>
```

---

## Hooks

### useCalendarEngine

Acceso al contexto del provider:

```typescript
const { client, role } = useCalendarEngine();
```

### useCalendarEvents

Obtiene eventos de un calendario en un rango:

```typescript
const { events, loading, error, reload } = useCalendarEvents(calendarId, start, end);
```

### useCalendarNavigation

Estado de navegacion del calendario:

```typescript
const { currentDate, view, setView, goToToday, goNext, goPrev, goToDate } = useCalendarNavigation();
```

`view`: `"month"` | `"week"` | `"day"`

### useAvailabilitySlots

Obtiene slots disponibles para un dia:

```typescript
const { availability, loading, error, reload } = useAvailabilitySlots(bookingUrlId, "2026-04-15");
// availability: { date, slots: [{ startTime, endTime, availableDurations }] }
```

### useBookingForm

Estado completo del flujo de reserva:

```typescript
const {
  selectedSlot,       // AvailableSlot | null
  selectedDuration,   // number | null
  form,               // { bookerName, bookerEmail, bookerPhone, notes }
  submitting,         // boolean
  error,              // Error | null
  booking,            // BookingResponse | null (resultado exitoso)
  updateField,        // (field, value) => void
  selectSlot,         // (slot, duration?) => void
  setSelectedDuration,// (duration) => void
  submit,             // () => Promise<void>
  reset,              // () => void
} = useBookingForm(bookingUrlId);
```

### useMergedCalendars

Combina eventos de multiples calendarios:

```typescript
const { events, loading, error, reload } = useMergedCalendars([1, 2, 3], start, end);
```

---

## Tematizacion

El widget usa CSS variables inyectadas en el elemento `.ce-root`. Sobreescribir via la prop `theme`:

```tsx
<CalendarEngineProvider
  baseUrl="..."
  tenantId={1}
  theme={{
    "--ce-primary": "#e11d48",
    "--ce-primary-hover": "#be123c",
    "--ce-bg": "#0f172a",
    "--ce-bg-secondary": "#1e293b",
    "--ce-text": "#f1f5f9",
    "--ce-text-muted": "#94a3b8",
    "--ce-border": "#334155",
  }}
>
```

### Variables disponibles

| Variable | Default | Descripcion |
|----------|---------|-------------|
| `--ce-bg` | `#ffffff` | Fondo principal |
| `--ce-bg-secondary` | `#f8f9fa` | Fondo secundario (headers, formularios) |
| `--ce-text` | `#1a1a2e` | Color de texto principal |
| `--ce-text-muted` | `#6b7280` | Color de texto secundario |
| `--ce-primary` | `#6366f1` | Color primario (botones, seleccion) |
| `--ce-primary-hover` | `#4f46e5` | Hover del color primario |
| `--ce-primary-text` | `#ffffff` | Texto sobre color primario |
| `--ce-border` | `#e5e7eb` | Color de bordes |
| `--ce-border-hover` | `#d1d5db` | Hover de bordes |
| `--ce-slot-available` | `#ecfdf5` | Fondo de slot disponible |
| `--ce-slot-hover` | `#d1fae5` | Hover de slot |
| `--ce-slot-selected` | `#6366f1` | Slot seleccionado |
| `--ce-event-bg` | `#eef2ff` | Fondo de evento normal |
| `--ce-event-text` | `#3730a3` | Texto de evento |
| `--ce-event-recurring-bg` | `#fef3c7` | Fondo de evento recurrente |
| `--ce-booking-bg` | `#dbeafe` | Fondo de reserva |
| `--ce-today-bg` | `#eff6ff` | Fondo del dia actual |
| `--ce-radius` | `8px` | Radio de borde base |
| `--ce-radius-sm` | `4px` | Radio pequeño |
| `--ce-radius-lg` | `12px` | Radio grande |
| `--ce-font-family` | `system-ui, -apple-system, sans-serif` | Familia tipografica |
| `--ce-font-size` | `14px` | Tamaño de fuente base |
| `--ce-font-size-sm` | `12px` | Tamaño pequeño |
| `--ce-font-size-lg` | `16px` | Tamaño grande |
| `--ce-shadow` | `0 1px 3px rgba(0,0,0,0.1)` | Sombra |
| `--ce-transition` | `150ms ease` | Transicion de animaciones |

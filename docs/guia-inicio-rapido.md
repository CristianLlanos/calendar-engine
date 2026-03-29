# Guia de inicio rapido

## Requisitos previos

- JDK 17+
- MySQL 8.0
- Node.js 18+ (para SDK y widget)

## 1. Configurar la base de datos

Crear la base de datos y el usuario:

```sql
CREATE DATABASE calendar_engine;
CREATE USER 'calendar_engine'@'%' IDENTIFIED BY 'calendar_engine';
GRANT ALL PRIVILEGES ON calendar_engine.* TO 'calendar_engine'@'%';
FLUSH PRIVILEGES;
```

Las tablas se crean automaticamente al iniciar el backend (`SchemaUtils.create`).

## 2. Variables de entorno

Todas las variables tienen valores por defecto en `application.conf`:

| Variable | Default | Descripcion |
|----------|---------|-------------|
| `PORT` | `7730` | Puerto del servidor |
| `DB_URL` | `jdbc:mysql://localhost:7706/calendar_engine` | URL de conexion MySQL |
| `DB_USER` | `calendar_engine` | Usuario de BD |
| `DB_PASSWORD` | `calendar_engine` | Contraseña de BD |
| `SYNC_GOOGLE_ENABLED` | `false` | Habilitar sincronizacion Google |
| `GOOGLE_CLIENT_ID` | - | Client ID de Google OAuth |
| `GOOGLE_CLIENT_SECRET` | - | Client Secret de Google OAuth |
| `GOOGLE_WEBHOOK_BASE_URL` | - | URL base para webhooks de Google |
| `SYNC_APPLE_ENABLED` | `false` | Habilitar sincronizacion Apple |
| `SYNC_MODE` | `ON_DEMAND` | Modo de sync: `ON_DEMAND`, `POLLING`, `WEBHOOK` |
| `SYNC_POLLING_INTERVAL` | `15` | Intervalo de polling en minutos |
| `EVENT_EMITTER` | `TABLE` | Emisor de eventos: `TABLE` o `WEBHOOK` |
| `EVENT_WEBHOOK_URL` | - | URL destino para webhook de eventos |

## 3. Iniciar el backend

```bash
cd backend
./gradlew run
```

El servidor inicia en `http://localhost:7730`.

## 4. Crear un tenant

```bash
curl -X POST http://localhost:7730/api/calendar/tenants \
  -H "Content-Type: application/json" \
  -d '{"name": "Mi Empresa", "slug": "mi-empresa"}'
```

Respuesta:
```json
{
  "id": 1,
  "name": "Mi Empresa",
  "slug": "mi-empresa",
  "config": null,
  "createdAt": "2026-03-28T12:00:00"
}
```

## 5. Crear un calendario

```bash
curl -X POST http://localhost:7730/api/calendar/calendars \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: 1" \
  -d '{"name": "Consultas", "timezone": "America/Mexico_City", "visibility": "PUBLIC"}'
```

## 6. Crear un evento

```bash
curl -X POST http://localhost:7730/api/calendar/calendars/1/events \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: 1" \
  -d '{
    "title": "Reunion semanal",
    "startTime": "2026-04-01T09:00:00",
    "endTime": "2026-04-01T10:00:00",
    "recurrence": {
      "rrule": "FREQ=WEEKLY;BYDAY=MO,WE,FR"
    }
  }'
```

## 7. Instalar el SDK

```bash
npm install @calendarengine/sdk
```

```typescript
import { createClient } from "@calendarengine/sdk";

const client = createClient({
  baseUrl: "http://localhost:7730",
  getTenantId: () => 1,
});

const calendars = await client.calendars.list();
const slots = await client.bookingUrls.getAvailability(1, "2026-04-01");
```

## 8. Montar el widget

```bash
npm install @calendarengine/widget
```

```tsx
import { CalendarEngineProvider, MonthView, BookingSlotPicker } from "@calendarengine/widget";
import "@calendarengine/widget/styles.css";

function App() {
  return (
    <CalendarEngineProvider baseUrl="http://localhost:7730" tenantId={1} role="owner">
      <MonthView calendarId={1} />
      <BookingSlotPicker bookingUrlId={1} />
    </CalendarEngineProvider>
  );
}
```

## Siguientes pasos

- [Configurar URLs de reserva](sistema-de-reservas.md)
- [Crear eventos recurrentes](recurrencia.md)
- [Sincronizar con Google Calendar](sincronizacion-externa.md)
- [Personalizar el tema del widget](widget.md#tematizacion)

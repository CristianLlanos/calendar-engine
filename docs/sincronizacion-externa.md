# Sincronizacion externa

## Conceptos

- **Conexion externa** (`ExternalCalendarConnection`): vincula un calendario interno con un calendario externo (Google, Apple)
- **Sync token**: token incremental de Google Calendar para obtener solo cambios desde la ultima sincronizacion
- **Busy blocks**: los eventos externos se almacenan como bloques opacos (solo inicio y fin, sin titulo ni detalles) por privacidad
- Los busy blocks se usan en el paso 5 del [algoritmo de disponibilidad](sistema-de-reservas.md) para evitar sobrecargar la agenda del usuario

## Modos de sincronizacion

Configurables via variable de entorno `SYNC_MODE`:

| Modo | Descripcion |
|------|-------------|
| `ON_DEMAND` | Sincronizacion manual via `POST /api/calendar/sync/connections/{id}/sync` |
| `POLLING` | El `SyncPollingScheduler` ejecuta sincronizacion automatica cada N minutos |
| `WEBHOOK` | Google envia notificaciones push al endpoint `/api/calendar/sync/webhooks/google` |

El intervalo de polling se configura con `SYNC_POLLING_INTERVAL` (default 15 minutos).

## Google Calendar

### Requisitos previos

1. Crear un proyecto en [Google Cloud Console](https://console.cloud.google.com)
2. Habilitar la Google Calendar API
3. Crear credenciales OAuth 2.0 (tipo "Web application")
4. Agregar el redirect URI: `http://tu-servidor:7730/api/calendar/sync/google/callback`

### Configuracion

```
SYNC_GOOGLE_ENABLED=true
GOOGLE_CLIENT_ID=tu-client-id.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=tu-client-secret
GOOGLE_WEBHOOK_BASE_URL=https://tu-dominio-publico  # solo para modo WEBHOOK
```

### Flujo OAuth

1. El usuario accede a `GET /api/calendar/sync/google/auth?calendarId=1` (con header `X-Tenant-Id`)
2. El backend redirige al consent screen de Google con scope `calendar.readonly`
3. Google redirige a `/api/calendar/sync/google/callback` con un `code` y `state`
4. `GoogleOAuthAction.exchangeCode()` obtiene access token y refresh token
5. Los tokens se almacenan en `oauth_tokens` (se reemplazan tokens previos del mismo tenant/provider)

### Sincronizacion incremental

1. **Primera sync**: obtiene todos los eventos del calendario externo via Google Calendar API
   - Parametros: `singleEvents=true`, `maxResults=250`
   - Paginacion automatica via `nextPageToken`
   - Guarda el `nextSyncToken` para futuras sincronizaciones
2. **Syncs posteriores**: usa el `syncToken` para obtener solo cambios (creaciones, modificaciones, eliminaciones)
3. **Token invalido (410 Gone)**: limpia el sync token y ejecuta una sync completa

Los eventos se convierten a `BusyBlock` (solo `startTime`, `endTime`, `externalEventId`) y se almacenan en `external_busy_blocks`.

### Refresh de tokens

`GoogleOAuthAction.getValidAccessToken()` verifica si el token esta por expirar (5 minutos de margen) y lo refresca automaticamente usando el refresh token.

### Webhooks (Push Notifications)

Cuando `SYNC_MODE=WEBHOOK`:
- Al crear una conexion, se puede registrar un canal de push notifications via Google Calendar API
- Google envia POST a `/api/calendar/sync/webhooks/google` con header `X-Goog-Channel-ID: ce-{connectionId}`
- El backend ejecuta `syncConnection()` automaticamente

## Apple CalDAV

### Configuracion

```
SYNC_APPLE_ENABLED=true
```

### Funcionamiento

`AppleCalDavSyncService` realiza una peticion CalDAV REPORT al servidor CalDAV del usuario:

1. Envia un XML `calendar-query` con filtro de rango de fechas (3 meses)
2. Parsea la respuesta para extraer DTSTART, DTEND y UID de cada VEVENT
3. Convierte a `BusyBlock` y almacena via `ExternalBusyBlockService.replaceBlocksForConnection()`

La URL CalDAV del usuario se almacena en `external_calendar_id` de la conexion.

## Administrar conexiones

### Crear conexion

```bash
curl -X POST http://localhost:7730/api/calendar/sync/connections \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: 1" \
  -d '{
    "calendarId": 1,
    "provider": "GOOGLE",
    "externalCalendarId": "usuario@gmail.com",
    "syncMode": "ON_DEMAND"
  }'
```

### Listar conexiones

```
GET /api/calendar/sync/connections
```

### Eliminar conexion

```
DELETE /api/calendar/sync/connections/{id}
```

Los busy blocks asociados se eliminan automaticamente (CASCADE).

### Forzar sincronizacion

```
POST /api/calendar/sync/connections/{id}/sync
```

## Impacto en disponibilidad

Los busy blocks se integran automaticamente en el calculo de disponibilidad:

1. Cuando un usuario consulta slots disponibles, el paso 5 del algoritmo carga los `external_busy_blocks` del calendario
2. Estos intervalos se tratan igual que los eventos del calendario interno
3. Los slots que se solapan con busy blocks son eliminados

Esto permite que la agenda personal del usuario (Google Calendar, iCloud) sea respetada sin exponer detalles de sus eventos externos.

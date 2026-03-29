# Esquema de base de datos

## Diagrama de relaciones

```
tenants
  ├── calendars
  │     ├── events → recurrence_rules
  │     │     └── recurrence_exceptions
  │     ├── booking_urls
  │     │     └── booking_url_availability_windows
  │     ├── bookings → events
  │     ├── external_calendar_connections
  │     │     └── external_busy_blocks
  │     └── external_busy_blocks
  ├── bookings
  ├── booking_urls
  ├── external_calendar_connections
  ├── oauth_tokens
  └── system_events
```

---

## Tablas

### tenants

| Columna | Tipo | Notas |
|---------|------|-------|
| id | INT PK AUTO_INCREMENT | |
| name | VARCHAR(255) | |
| slug | VARCHAR(255) | UNIQUE |
| config | TEXT | Nullable, JSON libre |
| created_at | DATETIME | |

### calendars

| Columna | Tipo | Notas |
|---------|------|-------|
| id | INT PK AUTO_INCREMENT | |
| tenant_id | INT FK → tenants | |
| name | VARCHAR(255) | |
| description | TEXT | Nullable |
| timezone | VARCHAR(100) | Default `UTC` |
| color | VARCHAR(20) | Nullable, hex |
| visibility | VARCHAR(20) | Default `PRIVATE` |
| created_at | DATETIME | |

Indice: `tenant_id`

### events

| Columna | Tipo | Notas |
|---------|------|-------|
| id | INT PK AUTO_INCREMENT | |
| tenant_id | INT FK → tenants | |
| calendar_id | INT FK → calendars | |
| title | VARCHAR(500) | |
| description | TEXT | Nullable |
| location | VARCHAR(500) | Nullable |
| start_time | DATETIME | UTC |
| end_time | DATETIME | UTC |
| all_day | BOOLEAN | Default `false` |
| status | VARCHAR(50) | Default `CONFIRMED` |
| recurrence_rule_id | INT FK → recurrence_rules | Nullable (null = evento simple) |
| created_by | INT | Nullable |
| created_at | DATETIME | |
| updated_at | DATETIME | |

Indices: `calendar_id`, `tenant_id`, `(start_time, end_time)`, `recurrence_rule_id`

### recurrence_rules

| Columna | Tipo | Notas |
|---------|------|-------|
| id | INT PK AUTO_INCREMENT | |
| rrule | VARCHAR(1000) | Ej: `FREQ=WEEKLY;BYDAY=MO,WE` |
| dtstart | DATETIME | Ancla de recurrencia |
| timezone | VARCHAR(100) | |

### recurrence_exceptions

| Columna | Tipo | Notas |
|---------|------|-------|
| id | INT PK AUTO_INCREMENT | |
| event_id | INT FK → events | CASCADE on delete |
| original_date | DATETIME | Ocurrencia siendo modificada |
| is_excluded | BOOLEAN | Default `true` (true=eliminada, false=modificada) |
| override_title | VARCHAR(500) | Nullable |
| override_start_time | DATETIME | Nullable |
| override_end_time | DATETIME | Nullable |
| override_location | VARCHAR(500) | Nullable |
| override_description | TEXT | Nullable |

Indice: `event_id`

### booking_urls

| Columna | Tipo | Notas |
|---------|------|-------|
| id | INT PK AUTO_INCREMENT | |
| tenant_id | INT FK → tenants | |
| calendar_id | INT FK → calendars | |
| slug | VARCHAR(255) | Unico por tenant |
| name | VARCHAR(255) | |
| description | TEXT | Nullable |
| status | VARCHAR(20) | Default `ACTIVE` |
| duration_minutes | INT | |
| duration_options | VARCHAR(500) | Nullable, JSON array `[15,30,60]` |
| buffer_before_minutes | INT | Default `0` |
| buffer_after_minutes | INT | Default `0` |
| min_lead_time_hours | INT | Default `1` |
| max_lead_time_days | INT | Default `60` |
| max_bookings_per_day | INT | Nullable (null = sin limite) |
| max_bookings_per_week | INT | Nullable (null = sin limite) |
| auto_confirm | BOOLEAN | Default `true` |
| created_at | DATETIME | |

Indices: `(tenant_id, slug)` UNIQUE, `tenant_id`, `calendar_id`

### booking_url_availability_windows

| Columna | Tipo | Notas |
|---------|------|-------|
| id | INT PK AUTO_INCREMENT | |
| booking_url_id | INT FK → booking_urls | CASCADE on delete |
| day_of_week | INT | 1=lunes, 7=domingo |
| start_time | TIME | |
| end_time | TIME | |

Indice: `booking_url_id`

### bookings

| Columna | Tipo | Notas |
|---------|------|-------|
| id | INT PK AUTO_INCREMENT | |
| tenant_id | INT FK → tenants | |
| booking_url_id | INT FK → booking_urls | |
| calendar_id | INT FK → calendars | |
| event_id | INT FK → events | Nullable, evento auto-creado |
| status | VARCHAR(50) | Default `CONFIRMED` |
| start_time | DATETIME | |
| end_time | DATETIME | |
| booker_name | VARCHAR(255) | |
| booker_email | VARCHAR(255) | |
| booker_phone | VARCHAR(50) | Nullable |
| booker_user_id | INT | Nullable (si es miembro del tenant) |
| notes | TEXT | Nullable |
| cancelled_at | DATETIME | Nullable |
| cancellation_reason | TEXT | Nullable |
| created_at | DATETIME | |

Indices: `tenant_id`, `calendar_id`, `booking_url_id`, `(start_time, end_time)`, `booker_email`

### external_calendar_connections

| Columna | Tipo | Notas |
|---------|------|-------|
| id | INT PK AUTO_INCREMENT | |
| tenant_id | INT FK → tenants | |
| calendar_id | INT FK → calendars | |
| provider | VARCHAR(50) | GOOGLE, APPLE |
| external_calendar_id | VARCHAR(500) | |
| sync_mode | VARCHAR(50) | Default `POLLING` |
| last_synced_at | DATETIME | Nullable |
| sync_token | VARCHAR(500) | Nullable, Google sync token / CalDAV ctag |
| enabled | BOOLEAN | Default `true` |
| created_at | DATETIME | |

Indices: `tenant_id`, `calendar_id`

### external_busy_blocks

| Columna | Tipo | Notas |
|---------|------|-------|
| id | INT PK AUTO_INCREMENT | |
| connection_id | INT FK → external_calendar_connections | CASCADE on delete |
| tenant_id | INT FK → tenants | |
| calendar_id | INT FK → calendars | |
| start_time | DATETIME | |
| end_time | DATETIME | |
| external_event_id | VARCHAR(500) | Nullable, para deduplicacion |

Indices: `connection_id`, `calendar_id`, `(start_time, end_time)`

### oauth_tokens

| Columna | Tipo | Notas |
|---------|------|-------|
| id | INT PK AUTO_INCREMENT | |
| tenant_id | INT FK → tenants | |
| provider | VARCHAR(50) | GOOGLE |
| access_token | TEXT | |
| refresh_token | TEXT | |
| expires_at | DATETIME | |
| scopes | VARCHAR(1000) | Nullable |
| created_at | DATETIME | |
| updated_at | DATETIME | |

Indice: `tenant_id`

### system_events

| Columna | Tipo | Notas |
|---------|------|-------|
| id | INT PK AUTO_INCREMENT | |
| tenant_id | INT FK → tenants | |
| event_type | VARCHAR(100) | BOOKING_CREATED, BOOKING_CANCELLED |
| payload | TEXT | JSON serializado |
| processed | BOOLEAN | Default `false` |
| created_at | DATETIME | |

Indices: `tenant_id`, `processed`

---

## Enums

| Enum | Valores |
|------|---------|
| EventStatus | CONFIRMED, TENTATIVE, CANCELLED |
| BookingStatus | CONFIRMED, PENDING_APPROVAL, CANCELLED |
| BookingUrlStatus | ACTIVE, PAUSED, ARCHIVED |
| CalendarVisibility | PRIVATE, PUBLIC |
| SyncProvider | GOOGLE, APPLE |
| SyncMode | WEBHOOK, POLLING, ON_DEMAND |

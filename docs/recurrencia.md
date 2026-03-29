# Recurrencia (RRULE)

## Introduccion

Calendar Engine soporta eventos recurrentes usando el estandar RFC 5545 (RRULE). Las reglas de recurrencia se almacenan como strings y se expanden en tiempo de consulta usando la libreria `lib-recur`.

## Formato RRULE

Una regla de recurrencia se compone de pares clave-valor separados por `;`:

```
FREQ=WEEKLY;BYDAY=MO,WE,FR;UNTIL=20261231T235959Z
```

### Parametros soportados

| Parametro | Descripcion | Ejemplo |
|-----------|-------------|---------|
| `FREQ` | Frecuencia (obligatorio) | `DAILY`, `WEEKLY`, `MONTHLY`, `YEARLY` |
| `INTERVAL` | Cada N periodos | `INTERVAL=2` (cada 2 semanas) |
| `BYDAY` | Dias de la semana (con FREQ=WEEKLY) | `BYDAY=MO,WE,FR` |
| `BYMONTHDAY` | Dia del mes (con FREQ=MONTHLY) | `BYMONTHDAY=15` |
| `BYMONTH` | Mes del año (con FREQ=YEARLY) | `BYMONTH=3` |
| `COUNT` | Numero total de ocurrencias | `COUNT=10` |
| `UNTIL` | Fecha limite | `UNTIL=20261231T235959Z` |

`COUNT` y `UNTIL` son mutuamente excluyentes. Si no se especifica ninguno, la recurrencia es infinita (se limita a 366 ocurrencias por expansion).

## Ejemplos

| Patron | RRULE |
|--------|-------|
| Todos los dias | `FREQ=DAILY` |
| Cada 2 dias, 10 veces | `FREQ=DAILY;INTERVAL=2;COUNT=10` |
| Lunes, miercoles y viernes | `FREQ=WEEKLY;BYDAY=MO,WE,FR` |
| Cada 2 semanas los lunes | `FREQ=WEEKLY;INTERVAL=2;BYDAY=MO` |
| El 15 de cada mes | `FREQ=MONTHLY;BYMONTHDAY=15` |
| Cada 3 meses hasta fin de año | `FREQ=MONTHLY;INTERVAL=3;UNTIL=20261231T235959Z` |
| Cada año el 15 de marzo | `FREQ=YEARLY;BYMONTH=3;BYMONTHDAY=15` |
| Cada martes, 4 veces | `FREQ=WEEKLY;BYDAY=TU;COUNT=4` |

## Crear un evento recurrente

```bash
curl -X POST http://localhost:7730/api/calendar/calendars/1/events \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: 1" \
  -d '{
    "title": "Standup diario",
    "startTime": "2026-04-01T09:00:00",
    "endTime": "2026-04-01T09:15:00",
    "recurrence": {
      "rrule": "FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR",
      "timezone": "America/Mexico_City"
    }
  }'
```

Esto crea:
- Un registro en `events` con `recurrence_rule_id` apuntando a:
- Un registro en `recurrence_rules` con el RRULE, dtstart y timezone

## Expansion de ocurrencias

Al consultar eventos con rango (`GET /api/calendar/calendars/{id}/events?start=&end=`), el backend expande los RRULE en ocurrencias virtuales.

### Proceso

1. Consultar eventos del calendario (simples + recurrentes)
2. Para cada evento recurrente, `RruleExpander` usa `lib-recur` para generar fechas de ocurrencia dentro del rango
3. La duracion de cada ocurrencia se calcula a partir de la diferencia entre `start_time` y `end_time` del evento original
4. Se consultan `recurrence_exceptions` para el evento:
   - `is_excluded = true`: la ocurrencia se omite
   - `is_excluded = false`: se aplican los campos override (titulo, hora, ubicacion)
5. Las ocurrencias se retornan como `EventOccurrence` con `isRecurring = true`

### Limites

- Maximo 366 ocurrencias por expansion (configurable en `RruleExpander`)
- La expansion se hace en memoria por cada consulta, sin tabla materializada

## Excepciones de ocurrencia

### Excluir una ocurrencia

Eliminar una sola ocurrencia de un evento recurrente:

```
DELETE /api/calendar/calendars/1/events/5?scope=THIS&occurrenceDate=2026-04-08T09:00:00
```

Crea un registro en `recurrence_exceptions` con `is_excluded = true`.

### Modificar una ocurrencia

Cambiar titulo, hora o ubicacion de una sola ocurrencia:

```bash
curl -X PUT http://localhost:7730/api/calendar/calendars/1/events/5/occurrence/2026-04-08T09:00:00 \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: 1" \
  -d '{
    "title": "Standup especial",
    "startTime": "2026-04-08T10:00:00",
    "endTime": "2026-04-08T10:30:00"
  }'
```

Crea un registro en `recurrence_exceptions` con `is_excluded = false` y los campos override.

### Eliminar ocurrencias siguientes

Truncar la recurrencia desde una fecha:

```
DELETE /api/calendar/calendars/1/events/5?scope=FOLLOWING&occurrenceDate=2026-04-15T09:00:00
```

Modifica el RRULE añadiendo o reemplazando `UNTIL` con la fecha anterior a la especificada. Elimina excepciones posteriores a esa fecha.

### Eliminar todo

```
DELETE /api/calendar/calendars/1/events/5?scope=ALL
```

Elimina el evento, sus excepciones y la regla de recurrencia.

## RecurrenceEditor (widget)

El componente `RecurrenceEditor` permite construir RRULE visualmente:

```tsx
<RecurrenceEditor value={rrule} onChange={(newRrule) => setRrule(newRrule)} />
```

Ofrece controles para:
- Seleccionar frecuencia (diaria, semanal, mensual, anual)
- Definir intervalo
- Seleccionar dias de la semana (para WEEKLY)
- Elegir tipo de fin (nunca, N ocurrencias, fecha)
- Muestra el RRULE generado en tiempo real

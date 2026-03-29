# Calendar Engine

Motor de calendario headless multi-tenant con sistema de reservas, recurrencia RFC 5545, sincronizacion con calendarios externos (Google Calendar, Apple CalDAV), SDK TypeScript y widget React con temas personalizables via variables CSS.

## Caracteristicas

- **Multi-tenancy** via header `X-Tenant-Id`
- **Eventos recurrentes** con soporte completo de RRULE (RFC 5545)
- **Sistema de reservas** con algoritmo de disponibilidad configurable (buffers, lead times, limites)
- **Sincronizacion externa** con Google Calendar (OAuth + sync incremental) y Apple CalDAV
- **SDK TypeScript** con cliente tipado para todas las operaciones
- **Widget React** con vistas de mes/semana/dia, selector de reservas, editor de recurrencia y temas CSS
- **Exportacion iCal** compatible con cualquier cliente de calendario
- **Event bus** con listeners para notificaciones (almacenamiento en BD o webhook)

## Estructura del proyecto

| Directorio | Descripcion | Stack |
|------------|-------------|-------|
| `backend/` | API REST | Kotlin, Ktor 2.3.12, Exposed ORM, MySQL 8.0 |
| `sdk/` | Cliente TypeScript | TypeScript 5 |
| `widget/` | Componentes React | React 18/19, Vite, CSS Variables |

## Inicio rapido

```bash
# 1. Configurar base de datos MySQL
mysql -u root -e "CREATE DATABASE calendar_engine; CREATE USER 'calendar_engine'@'%' IDENTIFIED BY 'calendar_engine'; GRANT ALL ON calendar_engine.* TO 'calendar_engine'@'%';"

# 2. Iniciar el backend (puerto 7730)
cd backend && ./gradlew run

# 3. Crear un tenant
curl -X POST http://localhost:7730/api/tenants \
  -H "Content-Type: application/json" \
  -d '{"name": "Mi Empresa", "slug": "mi-empresa"}'
```

## Documentacion

- [Guia de inicio rapido](docs/guia-inicio-rapido.md)
- [Arquitectura](docs/arquitectura.md)
- [Backend](docs/backend.md)
- [Referencia API](docs/referencia-api.md)
- [Esquema de base de datos](docs/esquema-base-de-datos.md)
- [SDK TypeScript](docs/sdk.md)
- [Widget React](docs/widget.md)
- [Sistema de reservas](docs/sistema-de-reservas.md)
- [Recurrencia (RRULE)](docs/recurrencia.md)
- [Sincronizacion externa](docs/sincronizacion-externa.md)
- [Sistema de eventos](docs/sistema-de-eventos.md)

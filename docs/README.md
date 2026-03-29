# Documentacion

## Primeros pasos

| Documento | Descripcion |
|-----------|-------------|
| [Guia de inicio rapido](guia-inicio-rapido.md) | Configurar BD, iniciar backend, crear tenant, instalar SDK y widget |
| [Arquitectura](arquitectura.md) | Vision general del sistema, modulos, flujos de datos, DI y event bus |

## Backend

| Documento | Descripcion |
|-----------|-------------|
| [Backend](backend.md) | Stack, estructura de codigo, configuracion, plugins, modulos |
| [Referencia API](referencia-api.md) | ~30 endpoints con metodos, rutas, request/response y ejemplos curl |
| [Esquema de base de datos](esquema-base-de-datos.md) | 12 tablas con columnas, tipos, relaciones, indices y enums |

## Integraciones

| Documento | Descripcion |
|-----------|-------------|
| [SDK TypeScript](sdk.md) | Instalacion, configuracion, 5 modulos API con ejemplos y tipos |
| [Widget React](widget.md) | 13 componentes, 5 hooks, roles, tematizacion con 26 variables CSS |

## Guias de funcionalidades

| Documento | Descripcion |
|-----------|-------------|
| [Sistema de reservas](sistema-de-reservas.md) | Booking URLs, ventanas de disponibilidad, algoritmo de 10 pasos, buffers, lead times, limites |
| [Recurrencia (RRULE)](recurrencia.md) | Formato RFC 5545, patrones soportados, excepciones, expansion de ocurrencias |
| [Sincronizacion externa](sincronizacion-externa.md) | Google Calendar (OAuth + sync incremental), Apple CalDAV, modos de sync |
| [Sistema de eventos](sistema-de-eventos.md) | Event bus, eventos y listeners, entrega via tabla o webhook, agregar nuevos eventos |

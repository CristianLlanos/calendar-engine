# Modo plugin: integrar en un servidor Ktor existente

Esta guia explica como agregar Calendar Engine como dependencia en tu aplicacion Ktor, sin ejecutar el servidor standalone.

## 1. Agregar la dependencia

```kotlin
// build.gradle.kts
repositories {
    mavenCentral()
}

dependencies {
    implementation("com.cristianllanos:calendar-engine:0.0.1")
}
```

## 2. Preparar la base de datos

Calendar Engine usa MySQL 8.0 con Exposed ORM. Tu aplicacion debe tener una conexion Exposed activa antes de instalar el plugin.

```sql
CREATE DATABASE calendar_engine;
CREATE USER 'calendar_engine'@'%' IDENTIFIED BY 'calendar_engine';
GRANT ALL PRIVILEGES ON calendar_engine.* TO 'calendar_engine'@'%';
```

Si ya tienes una conexion Exposed configurada (por ejemplo con HikariCP), Calendar Engine la reutiliza automaticamente. Solo asegurate de que `Database.connect(...)` se ejecute antes de `install(CalendarEngine)`.

## 3. Instalar el plugin

```kotlin
import com.cristianllanos.calendarengine.CalendarEngine

fun Application.module() {
    // Tu configuracion de base de datos existente
    // Database.connect(...)

    // Tus plugins existentes (serialization, CORS, etc.)

    install(CalendarEngine) {
        routePrefix = "/api/calendar"   // default: "/api/calendar"
        createTables = true             // crea las tablas si no existen
    }
}
```

Esto registra todas las rutas de Calendar Engine bajo el prefijo configurado.

## 4. Configuracion completa

```kotlin
install(CalendarEngine) {
    // Prefijo para todas las rutas (default: "/api/calendar")
    routePrefix = "/api/calendar"

    // Crear tablas automaticamente (default: true)
    // Poner en false si manejas migraciones por separado
    createTables = true

    // Compartir un container de DI existente (opcional)
    // Calendar Engine registra sus propios servicios en el
    container = miContainerExistente

    // Configuracion de sincronizacion con calendarios externos
    sync {
        // Google Calendar
        google {
            enabled = true
            clientId = "tu-client-id"
            clientSecret = "tu-client-secret"
            webhookBaseUrl = "https://tu-dominio.com"
        }

        // Apple CalDAV
        apple = true

        // Modo de sincronizacion: "ON_DEMAND" | "POLLING" | "WEBHOOK"
        mode = "POLLING"
        pollingIntervalMinutes = 10
    }

    // Configuracion de notificaciones de eventos
    events {
        // Emisor: "TABLE" (persiste en BD) o "WEBHOOK" (envia a URL)
        emitter = "TABLE"
        webhookUrl = "https://tu-dominio.com/webhooks/calendar"
    }
}
```

## 5. Compartir un container de DI

Si tu aplicacion ya usa `com.cristianllanos:container`, puedes pasar tu instancia existente. Calendar Engine registra sus servicios en el sin sobreescribir los tuyos:

```kotlin
val miContainer = Container().apply {
    register(MiServiceProvider())
}

install(CalendarEngine) {
    container = miContainer
}

// Despues de instalar, el container tiene tanto tus servicios como los de Calendar Engine
val bookingService = miContainer.resolve<BookingService>()
```

## 6. Sobre plugins de Ktor

Calendar Engine **no instala** plugins de Ktor (Serialization, CORS, StatusPages, CallLogging). En modo plugin, tu aplicacion es responsable de configurarlos.

Como minimo necesitas:

```kotlin
// Serialization con kotlinx.json (requerido)
install(ContentNegotiation) {
    json(Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    })
}

// StatusPages para mapeo de excepciones (recomendado)
install(StatusPages) {
    exception<NoSuchElementException> { call, _ ->
        call.respond(HttpStatusCode.NotFound, ErrorResponse("Not found"))
    }
    exception<IllegalArgumentException> { call, cause ->
        call.respond(HttpStatusCode.BadRequest, ErrorResponse(cause.message ?: "Bad request"))
    }
}
```

Calendar Engine lanza estas excepciones que deberias mapear:

| Excepcion | Codigo sugerido | Cuando |
|-----------|----------------|--------|
| `NoSuchElementException` | 404 | Recurso no encontrado |
| `IllegalArgumentException` | 400 | Parametros invalidos |
| `ConflictException` | 409 | Conflicto al eliminar (referencias existentes) |
| `UnauthorizedException` | 401 | Falta header `X-Tenant-Id` |
| `ForbiddenException` | 403 | Tenant no tiene acceso al recurso |

## 7. Autenticacion

Calendar Engine usa el header `X-Tenant-Id` para identificar al tenant en cada request. No incluye JWT ni autenticacion de usuarios — eso queda a cargo de tu aplicacion.

Un patron comun es agregar un middleware que valide el JWT de tu usuario y luego inyecte el `X-Tenant-Id` correspondiente:

```kotlin
install(Authentication) {
    jwt("auth") {
        // tu configuracion JWT
    }
}

routing {
    authenticate("auth") {
        // Middleware que agrega X-Tenant-Id basado en el usuario autenticado
        intercept(ApplicationCallPipeline.Call) {
            val tenantId = obtenerTenantIdDelUsuario(call)
            call.request.headers.append("X-Tenant-Id", tenantId.toString())
        }
    }
}
```

## 8. Rutas disponibles

Todas las rutas se registran bajo `routePrefix` (default `/api/calendar`):

| Recurso | Metodos | Path |
|---------|---------|------|
| Tenants | CRUD | `/tenants` |
| Calendars | CRUD | `/calendars` |
| Events | CRUD + occurrences | `/calendars/{id}/events` |
| iCal export | GET | `/calendars/{id}/ical` |
| Booking URLs | CRUD | `/booking-urls` |
| Availability | GET | `/booking-urls/{id}/availability` |
| Bookings | list, get, create, cancel | `/bookings` |
| Sync (Google) | OAuth + webhook | `/sync/google/*` |

Todas las rutas (excepto tenants) requieren el header `X-Tenant-Id`.

## 9. Ejemplo minimo completo

```kotlin
fun Application.module() {
    // 1. Base de datos
    Database.connect(
        url = "jdbc:mysql://localhost:3306/mi_app",
        driver = "com.mysql.cj.jdbc.Driver",
        user = "root",
        password = "root"
    )

    // 2. Plugins de Ktor
    install(ContentNegotiation) { json() }
    install(StatusPages) {
        exception<NoSuchElementException> { call, _ ->
            call.respond(HttpStatusCode.NotFound)
        }
    }

    // 3. Calendar Engine
    install(CalendarEngine)

    // 4. Tus propias rutas
    routing {
        get("/health") { call.respondText("OK") }
    }
}
```

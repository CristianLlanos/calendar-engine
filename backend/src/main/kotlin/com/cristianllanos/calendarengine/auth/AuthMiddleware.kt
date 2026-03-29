package com.cristianllanos.calendarengine.auth

import io.ktor.server.application.*

const val TENANT_HEADER = "X-Tenant-Id"

data class TenantPrincipal(
    val tenantId: Int,
)

fun ApplicationCall.tenantPrincipal(): TenantPrincipal {
    val tenantId = request.headers[TENANT_HEADER]?.toIntOrNull()
        ?: throw UnauthorizedException("Missing or invalid $TENANT_HEADER header")
    return TenantPrincipal(tenantId = tenantId)
}

fun ApplicationCall.pathParam(name: String): Int {
    return parameters[name]?.toIntOrNull()
        ?: throw IllegalArgumentException("Missing or invalid path parameter: $name")
}

class ForbiddenException(message: String) : RuntimeException(message)
class UnauthorizedException(message: String) : RuntimeException(message)

package com.cristianllanos.calendarengine.auth

import io.ktor.server.application.*

/** HTTP header name used to identify the current tenant. */
const val TENANT_HEADER = "X-Tenant-Id"

/** Holds the authenticated tenant identity extracted from the request. */
data class TenantPrincipal(
    val tenantId: Int,
)

/** Extracts the tenant ID from the [TENANT_HEADER] header and returns a [TenantPrincipal]. Throws [UnauthorizedException] if missing or invalid. */
fun ApplicationCall.tenantPrincipal(): TenantPrincipal {
    val tenantId = request.headers[TENANT_HEADER]?.toIntOrNull()
        ?: throw UnauthorizedException("Missing or invalid $TENANT_HEADER header")
    return TenantPrincipal(tenantId = tenantId)
}

/** Extracts a required integer path parameter by [name]. Returns the parsed [Int] or throws [IllegalArgumentException]. */
fun ApplicationCall.pathParam(name: String): Int {
    return parameters[name]?.toIntOrNull()
        ?: throw IllegalArgumentException("Missing or invalid path parameter: $name")
}

/** Thrown when the authenticated tenant is not allowed to access a resource. */
class ForbiddenException(message: String) : RuntimeException(message)

/** Thrown when the tenant identity header is missing or invalid. */
class UnauthorizedException(message: String) : RuntimeException(message)

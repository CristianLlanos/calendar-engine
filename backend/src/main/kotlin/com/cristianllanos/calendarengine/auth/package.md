# Package com.cristianllanos.calendarengine.auth

Authentication and tenant resolution middleware.

Extracts the `X-Tenant-Id` header into a [TenantPrincipal] for multi-tenant request scoping.
Provides helper functions for accessing the authenticated tenant and path parameters from route handlers.

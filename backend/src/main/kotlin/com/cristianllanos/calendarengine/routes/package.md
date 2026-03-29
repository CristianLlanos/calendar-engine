# Package com.cristianllanos.calendarengine.routes

Ktor route registration functions for all API endpoints.

Each function registers routes for a resource (tenants, calendars, events, bookings, etc.) under
the configured route prefix. Uses [crudRoutes] for standardized CRUD with pagination and search.

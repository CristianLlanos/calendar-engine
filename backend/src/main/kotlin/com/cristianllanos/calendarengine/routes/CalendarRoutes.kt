package com.cristianllanos.calendarengine.routes

import com.cristianllanos.calendarengine.auth.tenantPrincipal
import com.cristianllanos.calendarengine.auth.pathParam
import com.cristianllanos.calendarengine.dto.*
import com.cristianllanos.calendarengine.modules.calendar.CalendarService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/** Registers calendar CRUD routes plus get-by-ID and public calendar listing. */
fun Route.calendarRoutes(calendarService: CalendarService) {
    crudRoutes<CalendarResponse, CreateCalendarRequest, UpdateCalendarRequest>(
        path = "calendars",
        getAll = { tenantId, params -> calendarService.getAll(tenantId, params) },
        create = { tenantId, request -> calendarService.create(tenantId, request) },
        update = { id, tenantId, request -> calendarService.update(id, tenantId, request) },
        delete = { id, tenantId -> calendarService.delete(id, tenantId) },
    )

    route("/calendars") {
        get("/{id}") {
            val principal = call.tenantPrincipal()
            val id = call.pathParam("id")
            call.respond(calendarService.getById(id, principal.tenantId))
        }

        get("/public") {
            val params = call.paginationParams()
            call.respond(calendarService.getPublicCalendars(params))
        }
    }
}

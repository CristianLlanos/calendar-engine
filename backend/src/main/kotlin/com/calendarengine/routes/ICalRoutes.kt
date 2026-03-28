package com.calendarengine.routes

import com.calendarengine.auth.pathParam
import com.calendarengine.auth.tenantPrincipal
import com.calendarengine.modules.event.EventService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.iCalRoutes(eventService: EventService) {
    get("/api/calendars/{calendarId}/export.ics") {
        val principal = call.tenantPrincipal()
        val calendarId = call.pathParam("calendarId")
        val ical = eventService.exportICal(calendarId, principal.tenantId)
        call.respondText(ical, ContentType("text", "calendar"))
    }
}

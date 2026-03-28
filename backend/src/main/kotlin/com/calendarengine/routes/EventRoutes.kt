package com.calendarengine.routes

import com.calendarengine.auth.pathParam
import com.calendarengine.auth.tenantPrincipal
import com.calendarengine.dto.CreateEventRequest
import com.calendarengine.dto.UpdateEventRequest
import com.calendarengine.dto.UpdateOccurrenceRequest
import com.calendarengine.modules.event.EventService
import com.calendarengine.modules.event.actions.DeleteScope
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.eventRoutes(eventService: EventService) {
    route("/api/calendars/{calendarId}/events") {
        get {
            val principal = call.tenantPrincipal()
            val calendarId = call.pathParam("calendarId")
            val start = call.request.queryParameters["start"]
                ?: throw IllegalArgumentException("Missing 'start' query parameter")
            val end = call.request.queryParameters["end"]
                ?: throw IllegalArgumentException("Missing 'end' query parameter")
            call.respond(eventService.listOccurrences(calendarId, principal.tenantId, start, end))
        }

        post {
            val principal = call.tenantPrincipal()
            val calendarId = call.pathParam("calendarId")
            val request = call.receive<CreateEventRequest>()
            call.respond(HttpStatusCode.Created, eventService.create(calendarId, principal.tenantId, request))
        }

        get("/{id}") {
            val principal = call.tenantPrincipal()
            val calendarId = call.pathParam("calendarId")
            val id = call.pathParam("id")
            call.respond(eventService.getById(id, calendarId, principal.tenantId))
        }

        put("/{id}") {
            val principal = call.tenantPrincipal()
            val calendarId = call.pathParam("calendarId")
            val id = call.pathParam("id")
            val request = call.receive<UpdateEventRequest>()
            call.respond(eventService.update(id, calendarId, principal.tenantId, request))
        }

        put("/{id}/occurrence/{date}") {
            val principal = call.tenantPrincipal()
            val calendarId = call.pathParam("calendarId")
            val id = call.pathParam("id")
            val date = call.parameters["date"]
                ?: throw IllegalArgumentException("Missing occurrence date")
            val request = call.receive<UpdateOccurrenceRequest>()
            call.respond(eventService.updateOccurrence(id, calendarId, principal.tenantId, date, request))
        }

        delete("/{id}") {
            val principal = call.tenantPrincipal()
            val calendarId = call.pathParam("calendarId")
            val id = call.pathParam("id")
            val scope = call.request.queryParameters["scope"]?.uppercase()?.let {
                try { DeleteScope.valueOf(it) } catch (_: Exception) { DeleteScope.ALL }
            } ?: DeleteScope.ALL
            val occurrenceDate = call.request.queryParameters["occurrenceDate"]
            eventService.delete(id, calendarId, principal.tenantId, scope, occurrenceDate)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}

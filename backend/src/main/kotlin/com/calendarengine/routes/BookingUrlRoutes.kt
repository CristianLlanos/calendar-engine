package com.calendarengine.routes

import com.calendarengine.auth.pathParam
import com.calendarengine.auth.tenantPrincipal
import com.calendarengine.dto.*
import com.calendarengine.modules.booking.BookingUrlService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.bookingUrlRoutes(bookingUrlService: BookingUrlService) {
    route("/api/booking-urls") {
        get {
            val principal = call.tenantPrincipal()
            val params = call.paginationParams()
            call.respond(bookingUrlService.getAll(principal.tenantId, params))
        }

        post {
            val principal = call.tenantPrincipal()
            val request = call.receive<CreateBookingUrlRequest>()
            call.respond(HttpStatusCode.Created, bookingUrlService.create(principal.tenantId, request))
        }

        get("/{id}") {
            val principal = call.tenantPrincipal()
            val id = call.pathParam("id")
            call.respond(bookingUrlService.getById(id, principal.tenantId))
        }

        put("/{id}") {
            val principal = call.tenantPrincipal()
            val id = call.pathParam("id")
            val request = call.receive<UpdateBookingUrlRequest>()
            call.respond(bookingUrlService.update(id, principal.tenantId, request))
        }

        delete("/{id}") {
            val principal = call.tenantPrincipal()
            val id = call.pathParam("id")
            bookingUrlService.delete(id, principal.tenantId)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}

package com.calendarengine.routes

import com.calendarengine.auth.pathParam
import com.calendarengine.auth.tenantPrincipal
import com.calendarengine.dto.CancelBookingRequest
import com.calendarengine.dto.CreateBookingRequest
import com.calendarengine.dto.paginationParams
import com.calendarengine.modules.booking.BookingService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.bookingRoutes(bookingService: BookingService) {
    route("/api/bookings") {
        // Public — anyone with email+phone can book
        post {
            val request = call.receive<CreateBookingRequest>()
            call.respond(HttpStatusCode.Created, bookingService.create(request))
        }

        // Tenant-scoped management
        get {
            val principal = call.tenantPrincipal()
            val params = call.paginationParams()
            call.respond(bookingService.getAll(principal.tenantId, params))
        }

        get("/{id}") {
            val principal = call.tenantPrincipal()
            val id = call.pathParam("id")
            call.respond(bookingService.getById(id, principal.tenantId))
        }

        delete("/{id}") {
            val principal = call.tenantPrincipal()
            val id = call.pathParam("id")
            val request = try {
                call.receive<CancelBookingRequest>()
            } catch (_: Exception) {
                CancelBookingRequest()
            }
            call.respond(bookingService.cancel(id, principal.tenantId, request.reason))
        }
    }
}

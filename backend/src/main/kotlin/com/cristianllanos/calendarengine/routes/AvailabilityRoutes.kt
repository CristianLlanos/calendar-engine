package com.cristianllanos.calendarengine.routes

import com.cristianllanos.calendarengine.auth.pathParam
import com.cristianllanos.calendarengine.modules.booking.actions.CalculateAvailabilityAction
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.time.LocalDate

/** Registers public availability query routes for a booking URL (single date and date range). */
fun Route.availabilityRoutes(calculateAvailabilityAction: CalculateAvailabilityAction) {
    route("/booking-urls/{id}/availability") {
        // Public endpoint — no tenant auth required
        get {
            val id = call.pathParam("id")
            val dateStr = call.request.queryParameters["date"]
                ?: throw IllegalArgumentException("Missing 'date' query parameter")
            val date = LocalDate.parse(dateStr)
            call.respond(calculateAvailabilityAction.forDate(id, date))
        }

        get("/range") {
            val id = call.pathParam("id")
            val startStr = call.request.queryParameters["start"]
                ?: throw IllegalArgumentException("Missing 'start' query parameter")
            val endStr = call.request.queryParameters["end"]
                ?: throw IllegalArgumentException("Missing 'end' query parameter")
            val startDate = LocalDate.parse(startStr)
            val endDate = LocalDate.parse(endStr)

            if (endDate.isBefore(startDate)) {
                throw IllegalArgumentException("End date must be after start date")
            }
            // Limit range to 60 days to prevent abuse
            if (startDate.plusDays(60).isBefore(endDate)) {
                throw IllegalArgumentException("Date range cannot exceed 60 days")
            }

            call.respond(calculateAvailabilityAction.forRange(id, startDate, endDate))
        }
    }
}

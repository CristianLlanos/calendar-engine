package com.calendarengine.routes

import com.calendarengine.auth.pathParam
import com.calendarengine.dto.CreateTenantRequest
import com.calendarengine.dto.UpdateTenantRequest
import com.calendarengine.modules.tenant.TenantService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.tenantRoutes(tenantService: TenantService) {
    route("/api/tenants") {
        post {
            val request = call.receive<CreateTenantRequest>()
            call.respond(HttpStatusCode.Created, tenantService.create(request))
        }

        get("/{id}") {
            val id = call.pathParam("id")
            call.respond(tenantService.getById(id))
        }

        put("/{id}") {
            val id = call.pathParam("id")
            val request = call.receive<UpdateTenantRequest>()
            call.respond(tenantService.update(id, request))
        }
    }
}

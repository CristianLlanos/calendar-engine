package com.calendarengine.routes

import com.calendarengine.auth.pathParam
import com.calendarengine.auth.tenantPrincipal
import com.calendarengine.dto.PaginatedResponse
import com.calendarengine.dto.PaginationParams
import com.calendarengine.dto.paginationParams
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

inline fun <reified TResponse : Any, reified TCreate : Any, reified TUpdate : Any> Route.crudRoutes(
    path: String,
    crossinline getAll: suspend (tenantId: Int, params: PaginationParams) -> PaginatedResponse<TResponse>,
    crossinline create: suspend (tenantId: Int, request: TCreate) -> TResponse,
    crossinline update: suspend (id: Int, tenantId: Int, request: TUpdate) -> TResponse,
    crossinline delete: suspend (id: Int, tenantId: Int) -> Unit,
) {
    route("/api/$path") {
        get {
            val principal = call.tenantPrincipal()
            val params = call.paginationParams()
            call.respond(getAll(principal.tenantId, params))
        }

        post {
            val principal = call.tenantPrincipal()
            val request = call.receive<TCreate>()
            call.respond(HttpStatusCode.Created, create(principal.tenantId, request))
        }

        put("/{id}") {
            val principal = call.tenantPrincipal()
            val id = call.pathParam("id")
            val request = call.receive<TUpdate>()
            call.respond(update(id, principal.tenantId, request))
        }

        delete("/{id}") {
            val principal = call.tenantPrincipal()
            val id = call.pathParam("id")
            delete(id, principal.tenantId)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}

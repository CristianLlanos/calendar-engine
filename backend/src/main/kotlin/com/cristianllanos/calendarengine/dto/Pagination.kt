package com.cristianllanos.calendarengine.dto

import io.ktor.server.application.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*

data class PaginationParams(
    val limit: Int,
    val offset: Int,
    val sortBy: String?,
    val sortDir: String?,
    val search: String? = null
)

fun ApplicationCall.paginationParams(defaultLimit: Int = 50, maxLimit: Int = 100): PaginationParams {
    val limit = (request.queryParameters["limit"]?.toIntOrNull() ?: defaultLimit).coerceIn(1, maxLimit)
    val offset = (request.queryParameters["offset"]?.toIntOrNull() ?: 0).coerceAtLeast(0)
    val sortBy = request.queryParameters["sortBy"]
    val sortDir = request.queryParameters["sortDir"]?.lowercase()?.let {
        if (it == "asc" || it == "desc") it else null
    }
    val search = request.queryParameters["search"]?.takeIf { it.isNotBlank() }
    return PaginationParams(limit, offset, sortBy, sortDir, search)
}

@Serializable
data class PaginatedResponse<T>(
    val items: List<T>,
    val total: Long,
    val limit: Int,
    val offset: Int,
    val hasMore: Boolean
)

fun <T> paginate(
    query: Query,
    total: Long,
    params: PaginationParams,
    allowedSortColumns: Map<String, Expression<*>>,
    defaultSortColumn: Expression<*>,
    defaultSortOrder: SortOrder = SortOrder.DESC,
    mapper: (ResultRow) -> T,
): PaginatedResponse<T> {
    val sortColumn = allowedSortColumns[params.sortBy] ?: defaultSortColumn
    val sortOrder = when (params.sortDir) {
        "asc" -> SortOrder.ASC
        "desc" -> SortOrder.DESC
        else -> defaultSortOrder
    }

    val items = query
        .orderBy(sortColumn to sortOrder)
        .limit(params.limit, params.offset.toLong())
        .map(mapper)

    return PaginatedResponse(
        items = items,
        total = total,
        limit = params.limit,
        offset = params.offset,
        hasMore = params.offset + items.size < total
    )
}

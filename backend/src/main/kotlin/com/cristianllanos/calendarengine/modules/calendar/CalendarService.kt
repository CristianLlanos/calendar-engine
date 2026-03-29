package com.cristianllanos.calendarengine.modules.calendar

import com.cristianllanos.calendarengine.dto.*
import com.cristianllanos.calendarengine.models.BookingUrls
import com.cristianllanos.calendarengine.models.Calendars
import com.cristianllanos.calendarengine.models.Events
import com.cristianllanos.calendarengine.models.enums.CalendarVisibility
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

class CalendarService {

    companion object {
        private val ALLOWED_SORT_COLUMNS: Map<String, Expression<*>> = mapOf(
            "name" to Calendars.name,
            "createdAt" to Calendars.createdAt,
        )
    }

    fun getAll(tenantId: Int, params: PaginationParams): PaginatedResponse<CalendarResponse> = transaction {
        val condition = buildSearchCondition(
            Calendars.tenantId, tenantId, params.search,
            listOf(Calendars.name),
        )
        val total = Calendars.selectAll().where { condition }.count()

        paginate(
            query = Calendars.selectAll().where { condition },
            total = total,
            params = params,
            allowedSortColumns = ALLOWED_SORT_COLUMNS,
            defaultSortColumn = Calendars.id,
        ) { it.toCalendarResponse() }
    }

    fun getById(id: Int, tenantId: Int): CalendarResponse = transaction {
        Calendars.selectAll().where { (Calendars.id eq id) and (Calendars.tenantId eq tenantId) }
            .firstOrNull()?.toCalendarResponse()
            ?: throw NoSuchElementException("Calendar not found")
    }

    fun getPublicCalendars(params: PaginationParams): PaginatedResponse<CalendarResponse> = transaction {
        val condition = Op.build { Calendars.visibility eq CalendarVisibility.PUBLIC.name }
        val total = Calendars.selectAll().where { condition }.count()

        paginate(
            query = Calendars.selectAll().where { condition },
            total = total,
            params = params,
            allowedSortColumns = ALLOWED_SORT_COLUMNS,
            defaultSortColumn = Calendars.id,
        ) { it.toCalendarResponse() }
    }

    fun create(tenantId: Int, request: CreateCalendarRequest): CalendarResponse = transaction {
        val now = LocalDateTime.now()
        val id = Calendars.insert {
            it[Calendars.tenantId] = tenantId
            it[name] = request.name
            it[description] = request.description
            it[timezone] = request.timezone
            it[color] = request.color
            it[visibility] = request.visibility
            it[createdAt] = now
        } get Calendars.id

        Calendars.selectAll().where { Calendars.id eq id }.first().toCalendarResponse()
    }

    fun update(id: Int, tenantId: Int, request: UpdateCalendarRequest): CalendarResponse = transaction {
        Calendars.update({ (Calendars.id eq id) and (Calendars.tenantId eq tenantId) }) {
            request.name?.let { name -> it[Calendars.name] = name }
            request.description?.let { desc -> it[Calendars.description] = desc }
            request.timezone?.let { tz -> it[Calendars.timezone] = tz }
            request.color?.let { color -> it[Calendars.color] = color }
            request.visibility?.let { vis -> it[Calendars.visibility] = vis }
        }
        Calendars.selectAll().where { (Calendars.id eq id) and (Calendars.tenantId eq tenantId) }
            .firstOrNull()?.toCalendarResponse()
            ?: throw NoSuchElementException("Calendar not found")
    }

    fun delete(id: Int, tenantId: Int) = transaction {
        deleteWithConflictChecks(
            table = Calendars, idColumn = Calendars.id, id = id,
            tenantIdColumn = Calendars.tenantId, tenantId = tenantId,
            notFoundMessage = "Calendar not found",
            conflictChecks = listOf(
                DeleteConflictCheck(Events, Events.calendarId, "calendar has {count} event(s)"),
                DeleteConflictCheck(BookingUrls, BookingUrls.calendarId, "calendar has {count} booking URL(s)"),
            ),
        )
    }

    private fun ResultRow.toCalendarResponse() = CalendarResponse(
        id = this[Calendars.id],
        tenantId = this[Calendars.tenantId],
        name = this[Calendars.name],
        description = this[Calendars.description],
        timezone = this[Calendars.timezone],
        color = this[Calendars.color],
        visibility = this[Calendars.visibility],
        createdAt = this[Calendars.createdAt].toString(),
    )
}

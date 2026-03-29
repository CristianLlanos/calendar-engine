package com.cristianllanos.calendarengine.dto

import com.cristianllanos.calendarengine.plugins.ConflictException
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

/** Describes a foreign-key relationship to check before deleting a record. */
data class DeleteConflictCheck(
    val table: Table,
    val foreignKey: Expression<*>,
    val label: String,
)

/**
 * Deletes a tenant-scoped row after verifying no foreign-key conflicts exist.
 *
 * ```kotlin
 * deleteWithConflictChecks(
 *     table = Calendars, idColumn = Calendars.id, id = 1,
 *     tenantIdColumn = Calendars.tenantId, tenantId = 42,
 *     notFoundMessage = "Calendar not found",
 *     conflictChecks = listOf(
 *         DeleteConflictCheck(Events, Events.calendarId, "{count} events reference this calendar")
 *     ),
 * )
 * ```
 *
 * @throws ConflictException if any dependent rows are found.
 * @throws NoSuchElementException if the target row does not exist.
 */
fun deleteWithConflictChecks(
    table: Table,
    idColumn: Column<Int>,
    id: Int,
    tenantIdColumn: Column<Int>,
    tenantId: Int,
    notFoundMessage: String,
    conflictChecks: List<DeleteConflictCheck>,
) {
    for (check in conflictChecks) {
        @Suppress("UNCHECKED_CAST")
        val fk = check.foreignKey as Column<Int?>
        val count = check.table.selectAll()
            .where { fk eq id }
            .count()
        if (count > 0) {
            throw ConflictException("Cannot delete: ${check.label.replace("{count}", count.toString())}")
        }
    }

    val deleted = table.deleteWhere { (idColumn eq id) and (tenantIdColumn eq tenantId) }
    if (deleted == 0) throw NoSuchElementException(notFoundMessage)
}

/**
 * Builds an Exposed [Op] that filters by tenant and optionally applies a LIKE search across multiple columns.
 *
 * @param searchColumns columns to match against when [searchTerm] is non-null.
 * @return a combined WHERE condition.
 */
fun buildSearchCondition(
    tenantIdColumn: Column<Int>,
    tenantId: Int,
    searchTerm: String?,
    searchColumns: List<Expression<*>>,
): Op<Boolean> {
    var condition = Op.build { tenantIdColumn eq tenantId }
    searchTerm?.let { term ->
        val pattern = "%$term%"
        @Suppress("UNCHECKED_CAST")
        val searchOp = searchColumns
            .map { col -> Op.build { (col as ExpressionWithColumnType<String>) like pattern } }
            .reduce { acc, op -> acc or op }
        condition = condition and searchOp
    }
    return condition
}

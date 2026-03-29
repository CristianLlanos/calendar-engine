package com.cristianllanos.calendarengine.dto

import com.cristianllanos.calendarengine.plugins.ConflictException
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

data class DeleteConflictCheck(
    val table: Table,
    val foreignKey: Expression<*>,
    val label: String,
)

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

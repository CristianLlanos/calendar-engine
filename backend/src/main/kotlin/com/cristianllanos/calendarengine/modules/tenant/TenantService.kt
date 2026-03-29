package com.cristianllanos.calendarengine.modules.tenant

import com.cristianllanos.calendarengine.dto.*
import com.cristianllanos.calendarengine.models.Tenants
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

/** CRUD operations for tenant management. */
class TenantService {

    /** Retrieves a tenant by its ID. */
    fun getById(id: Int): TenantResponse = transaction {
        Tenants.selectAll().where { Tenants.id eq id }
            .firstOrNull()?.toTenantResponse()
            ?: throw NoSuchElementException("Tenant not found")
    }

    /** Creates a new tenant, enforcing unique slug. */
    fun create(request: CreateTenantRequest): TenantResponse = transaction {
        val existing = Tenants.selectAll().where { Tenants.slug eq request.slug }.firstOrNull()
        if (existing != null) throw IllegalArgumentException("Tenant slug already exists")

        val id = Tenants.insert {
            it[name] = request.name
            it[slug] = request.slug
            it[config] = request.config
            it[createdAt] = LocalDateTime.now()
        } get Tenants.id

        Tenants.selectAll().where { Tenants.id eq id }.first().toTenantResponse()
    }

    /** Partially updates a tenant, enforcing unique slug if changed. */
    fun update(id: Int, request: UpdateTenantRequest): TenantResponse = transaction {
        request.slug?.let { newSlug ->
            val existing = Tenants.selectAll()
                .where { (Tenants.slug eq newSlug) and (Tenants.id neq id) }
                .firstOrNull()
            if (existing != null) throw IllegalArgumentException("Tenant slug already exists")
        }

        Tenants.update({ Tenants.id eq id }) {
            request.name?.let { name -> it[Tenants.name] = name }
            request.slug?.let { slug -> it[Tenants.slug] = slug }
            request.config?.let { config -> it[Tenants.config] = config }
        }

        Tenants.selectAll().where { Tenants.id eq id }
            .firstOrNull()?.toTenantResponse()
            ?: throw NoSuchElementException("Tenant not found")
    }

    private fun ResultRow.toTenantResponse() = TenantResponse(
        id = this[Tenants.id],
        name = this[Tenants.name],
        slug = this[Tenants.slug],
        config = this[Tenants.config],
        createdAt = this[Tenants.createdAt].toString(),
    )
}

package com.calendarengine.models

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object OAuthTokens : Table("oauth_tokens") {
    val id = integer("id").autoIncrement()
    val tenantId = integer("tenant_id").references(Tenants.id)
    val provider = varchar("provider", 50)
    val accessToken = text("access_token")
    val refreshToken = text("refresh_token")
    val expiresAt = datetime("expires_at")
    val scopes = varchar("scopes", 1000).nullable()
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")

    override val primaryKey = PrimaryKey(id)

    init {
        index(false, tenantId)
    }
}

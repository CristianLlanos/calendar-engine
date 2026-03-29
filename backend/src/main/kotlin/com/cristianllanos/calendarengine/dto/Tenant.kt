package com.cristianllanos.calendarengine.dto

import kotlinx.serialization.Serializable

/** Response DTO for a tenant. */
@Serializable
data class TenantResponse(
    val id: Int,
    val name: String,
    val slug: String,
    val config: String? = null,
    val createdAt: String,
)

/** Request DTO for creating a new tenant. */
@Serializable
data class CreateTenantRequest(
    val name: String,
    val slug: String,
    val config: String? = null,
)

/** Request DTO for partially updating a tenant. All fields are optional. */
@Serializable
data class UpdateTenantRequest(
    val name: String? = null,
    val slug: String? = null,
    val config: String? = null,
)

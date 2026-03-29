package com.cristianllanos.calendarengine.dto

import kotlinx.serialization.Serializable

@Serializable
data class TenantResponse(
    val id: Int,
    val name: String,
    val slug: String,
    val config: String? = null,
    val createdAt: String,
)

@Serializable
data class CreateTenantRequest(
    val name: String,
    val slug: String,
    val config: String? = null,
)

@Serializable
data class UpdateTenantRequest(
    val name: String? = null,
    val slug: String? = null,
    val config: String? = null,
)

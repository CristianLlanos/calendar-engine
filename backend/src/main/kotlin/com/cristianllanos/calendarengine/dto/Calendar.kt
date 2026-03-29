package com.cristianllanos.calendarengine.dto

import kotlinx.serialization.Serializable

@Serializable
data class CalendarResponse(
    val id: Int,
    val tenantId: Int,
    val name: String,
    val description: String? = null,
    val timezone: String,
    val color: String? = null,
    val visibility: String,
    val createdAt: String,
)

@Serializable
data class CreateCalendarRequest(
    val name: String,
    val description: String? = null,
    val timezone: String = "UTC",
    val color: String? = null,
    val visibility: String = "PRIVATE",
)

@Serializable
data class UpdateCalendarRequest(
    val name: String? = null,
    val description: String? = null,
    val timezone: String? = null,
    val color: String? = null,
    val visibility: String? = null,
)

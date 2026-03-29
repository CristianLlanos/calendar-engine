package com.cristianllanos.calendarengine.dto

import kotlinx.serialization.Serializable

@Serializable
data class BookingResponse(
    val id: Int,
    val tenantId: Int,
    val bookingUrlId: Int,
    val calendarId: Int,
    val eventId: Int? = null,
    val status: String,
    val startTime: String,
    val endTime: String,
    val bookerName: String,
    val bookerEmail: String,
    val bookerPhone: String? = null,
    val bookerUserId: Int? = null,
    val notes: String? = null,
    val cancelledAt: String? = null,
    val cancellationReason: String? = null,
    val createdAt: String,
)

@Serializable
data class CreateBookingRequest(
    val bookingUrlId: Int,
    val startTime: String,
    val durationMinutes: Int,
    val bookerName: String,
    val bookerEmail: String,
    val bookerPhone: String? = null,
    val bookerUserId: Int? = null,
    val notes: String? = null,
)

@Serializable
data class CancelBookingRequest(
    val reason: String? = null,
)

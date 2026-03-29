package com.cristianllanos.calendarengine.dto

import kotlinx.serialization.Serializable

/** Response DTO for a booking URL with its availability windows. */
@Serializable
data class BookingUrlResponse(
    val id: Int,
    val tenantId: Int,
    val calendarId: Int,
    val slug: String,
    val name: String,
    val description: String? = null,
    val status: String,
    val durationMinutes: Int,
    val durationOptions: String? = null,
    val bufferBeforeMinutes: Int,
    val bufferAfterMinutes: Int,
    val minLeadTimeHours: Int,
    val maxLeadTimeDays: Int,
    val maxBookingsPerDay: Int? = null,
    val maxBookingsPerWeek: Int? = null,
    val autoConfirm: Boolean,
    val availabilityWindows: List<AvailabilityWindowResponse> = emptyList(),
    val createdAt: String,
)

/** Response DTO for a single availability window within a booking URL. */
@Serializable
data class AvailabilityWindowResponse(
    val id: Int,
    val dayOfWeek: Int,
    val startTime: String,
    val endTime: String,
)

/** Request DTO for creating a new booking URL. */
@Serializable
data class CreateBookingUrlRequest(
    val calendarId: Int,
    val slug: String,
    val name: String,
    val description: String? = null,
    val durationMinutes: Int,
    val durationOptions: String? = null,
    val bufferBeforeMinutes: Int = 0,
    val bufferAfterMinutes: Int = 0,
    val minLeadTimeHours: Int = 1,
    val maxLeadTimeDays: Int = 60,
    val maxBookingsPerDay: Int? = null,
    val maxBookingsPerWeek: Int? = null,
    val autoConfirm: Boolean = true,
    val availabilityWindows: List<CreateAvailabilityWindowRequest> = emptyList(),
)

/** Request DTO for creating an availability window within a booking URL. */
@Serializable
data class CreateAvailabilityWindowRequest(
    val dayOfWeek: Int,
    val startTime: String,
    val endTime: String,
)

/** Request DTO for partially updating a booking URL. All fields are optional. */
@Serializable
data class UpdateBookingUrlRequest(
    val slug: String? = null,
    val name: String? = null,
    val description: String? = null,
    val status: String? = null,
    val durationMinutes: Int? = null,
    val durationOptions: String? = null,
    val bufferBeforeMinutes: Int? = null,
    val bufferAfterMinutes: Int? = null,
    val minLeadTimeHours: Int? = null,
    val maxLeadTimeDays: Int? = null,
    val maxBookingsPerDay: Int? = null,
    val maxBookingsPerWeek: Int? = null,
    val autoConfirm: Boolean? = null,
    val availabilityWindows: List<CreateAvailabilityWindowRequest>? = null,
)

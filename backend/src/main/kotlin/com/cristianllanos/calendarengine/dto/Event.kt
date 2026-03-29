package com.cristianllanos.calendarengine.dto

import kotlinx.serialization.Serializable

/** Response DTO for a calendar event, optionally including its recurrence rule. */
@Serializable
data class EventResponse(
    val id: Int,
    val calendarId: Int,
    val title: String,
    val description: String? = null,
    val location: String? = null,
    val startTime: String,
    val endTime: String,
    val allDay: Boolean,
    val status: String,
    val isRecurring: Boolean,
    val recurrenceRule: RecurrenceRuleResponse? = null,
    val createdBy: Int? = null,
    val createdAt: String,
    val updatedAt: String,
)

/** Response DTO for an RFC 5545 recurrence rule attached to an event. */
@Serializable
data class RecurrenceRuleResponse(
    val id: Int,
    val rrule: String,
    val dtstart: String,
    val timezone: String,
)

/** A single occurrence of an event, which may be a recurring instance or an exception. */
@Serializable
data class EventOccurrence(
    val eventId: Int,
    val calendarId: Int,
    val title: String,
    val description: String? = null,
    val location: String? = null,
    val startTime: String,
    val endTime: String,
    val allDay: Boolean,
    val status: String,
    val isRecurring: Boolean,
    val isException: Boolean = false,
    val originalDate: String? = null,
)

/** Request DTO for creating a new event, optionally with a recurrence rule. */
@Serializable
data class CreateEventRequest(
    val title: String,
    val description: String? = null,
    val location: String? = null,
    val startTime: String,
    val endTime: String,
    val allDay: Boolean = false,
    val status: String = "CONFIRMED",
    val recurrence: CreateRecurrenceRequest? = null,
)

/** Request DTO for attaching a recurrence rule (RRULE) to an event. */
@Serializable
data class CreateRecurrenceRequest(
    val rrule: String,
    val timezone: String? = null,
)

/** Request DTO for partially updating an event. All fields are optional. */
@Serializable
data class UpdateEventRequest(
    val title: String? = null,
    val description: String? = null,
    val location: String? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val allDay: Boolean? = null,
    val status: String? = null,
)

/** Request DTO for updating a single occurrence of a recurring event. */
@Serializable
data class UpdateOccurrenceRequest(
    val title: String? = null,
    val description: String? = null,
    val location: String? = null,
    val startTime: String? = null,
    val endTime: String? = null,
)

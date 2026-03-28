package com.calendarengine.dto

import kotlinx.serialization.Serializable

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

@Serializable
data class RecurrenceRuleResponse(
    val id: Int,
    val rrule: String,
    val dtstart: String,
    val timezone: String,
)

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

@Serializable
data class CreateRecurrenceRequest(
    val rrule: String,
    val timezone: String? = null,
)

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

@Serializable
data class UpdateOccurrenceRequest(
    val title: String? = null,
    val description: String? = null,
    val location: String? = null,
    val startTime: String? = null,
    val endTime: String? = null,
)

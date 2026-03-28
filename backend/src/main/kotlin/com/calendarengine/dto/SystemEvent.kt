package com.calendarengine.dto

import kotlinx.serialization.Serializable

@Serializable
sealed class CalendarSystemEvent {
    abstract val type: String

    @Serializable
    data class BookingCreated(val booking: BookingResponse) : CalendarSystemEvent() {
        override val type = "BOOKING_CREATED"
    }

    @Serializable
    data class BookingCancelled(val booking: BookingResponse) : CalendarSystemEvent() {
        override val type = "BOOKING_CANCELLED"
    }

    @Serializable
    data class EventCreated(val event: EventResponse) : CalendarSystemEvent() {
        override val type = "EVENT_CREATED"
    }

    @Serializable
    data class EventUpdated(val event: EventResponse) : CalendarSystemEvent() {
        override val type = "EVENT_UPDATED"
    }

    @Serializable
    data class EventDeleted(val eventId: Int, val calendarId: Int) : CalendarSystemEvent() {
        override val type = "EVENT_DELETED"
    }
}

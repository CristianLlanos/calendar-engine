package com.cristianllanos.calendarengine.dto

import com.cristianllanos.events.Event

/** System event emitted when a new booking is created. */
data class BookingCreatedEvent(
    val tenantId: Int,
    val booking: BookingResponse,
) : Event()

/** System event emitted when a booking is cancelled. */
data class BookingCancelledEvent(
    val tenantId: Int,
    val booking: BookingResponse,
) : Event()

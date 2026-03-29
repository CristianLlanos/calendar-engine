package com.calendarengine.dto

import com.cristianllanos.events.Event

data class BookingCreatedEvent(
    val tenantId: Int,
    val booking: BookingResponse,
) : Event()

data class BookingCancelledEvent(
    val tenantId: Int,
    val booking: BookingResponse,
) : Event()

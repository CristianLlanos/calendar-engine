package com.cristianllanos.calendarengine.dto

import kotlinx.serialization.Serializable

/** A bookable time slot with its start/end times and selectable duration options. */
@Serializable
data class AvailableSlot(
    val startTime: String,
    val endTime: String,
    val availableDurations: List<Int>,
)

/** Availability for a single calendar date, containing all available slots. */
@Serializable
data class DayAvailability(
    val date: String,
    val slots: List<AvailableSlot>,
)

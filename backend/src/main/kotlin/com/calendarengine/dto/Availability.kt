package com.calendarengine.dto

import kotlinx.serialization.Serializable

@Serializable
data class AvailableSlot(
    val startTime: String,
    val endTime: String,
    val availableDurations: List<Int>,
)

@Serializable
data class DayAvailability(
    val date: String,
    val slots: List<AvailableSlot>,
)

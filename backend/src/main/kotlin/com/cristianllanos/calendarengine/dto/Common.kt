package com.cristianllanos.calendarengine.dto

import kotlinx.serialization.Serializable

/** Generic success message response. */
@Serializable
data class MessageResponse(val message: String)

/** Generic error response containing an error description. */
@Serializable
data class ErrorResponse(val error: String)

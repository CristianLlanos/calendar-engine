package com.cristianllanos.calendarengine.models.enums

/** Strategy used to synchronize an external calendar connection. */
enum class SyncMode {
    WEBHOOK,
    POLLING,
    ON_DEMAND,
}

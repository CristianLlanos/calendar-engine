package com.calendarengine.modules.notification

import com.calendarengine.dto.CalendarSystemEvent

interface SystemEventEmitter {
    fun emit(tenantId: Int, event: CalendarSystemEvent)
}

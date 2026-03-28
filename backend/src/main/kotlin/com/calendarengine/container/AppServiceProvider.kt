package com.calendarengine.container

import com.calendarengine.modules.booking.BookingService
import com.calendarengine.modules.booking.BookingUrlService
import com.calendarengine.modules.booking.actions.CalculateAvailabilityAction
import com.calendarengine.modules.booking.actions.CancelBookingAction
import com.calendarengine.modules.booking.actions.CreateBookingAction
import com.calendarengine.modules.calendar.CalendarService
import com.calendarengine.modules.notification.SystemEventEmitter
import com.calendarengine.modules.notification.TableSystemEventEmitter
import com.calendarengine.modules.event.EventService
import com.calendarengine.modules.event.ICalExporter
import com.calendarengine.modules.event.RruleExpander
import com.calendarengine.modules.event.actions.*
import com.calendarengine.modules.tenant.TenantService

class AppServiceProvider : ServiceProvider {
    override fun register(container: Container) {
        container.singleton<TenantService> { TenantService() }
        container.singleton<CalendarService> { CalendarService() }

        // Event module
        container.singleton<RruleExpander> { RruleExpander() }
        container.singleton<ICalExporter> { ICalExporter() }
        container.singleton<CreateEventAction> { CreateEventAction() }
        container.singleton<UpdateEventAction> { UpdateEventAction() }
        container.singleton<DeleteEventAction> { DeleteEventAction() }
        container.singleton<UpdateOccurrenceAction> { UpdateOccurrenceAction() }
        container.singleton<ExpandOccurrencesAction> { ExpandOccurrencesAction(resolve()) }
        container.singleton<EventService> { EventService(resolve(), resolve(), resolve(), resolve(), resolve(), resolve()) }

        // Notification module
        container.singleton<SystemEventEmitter> { TableSystemEventEmitter() }

        // Booking module
        container.singleton<BookingUrlService> { BookingUrlService() }
        container.singleton<CalculateAvailabilityAction> { CalculateAvailabilityAction(resolve()) }
        container.singleton<CreateBookingAction> { CreateBookingAction(resolve(), resolve()) }
        container.singleton<CancelBookingAction> { CancelBookingAction(resolve()) }
        container.singleton<BookingService> { BookingService(resolve(), resolve()) }
    }
}

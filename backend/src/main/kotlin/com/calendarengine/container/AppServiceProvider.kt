package com.calendarengine.container

import com.calendarengine.dto.BookingCancelledEvent
import com.calendarengine.dto.BookingCreatedEvent
import com.calendarengine.modules.booking.BookingService
import com.calendarengine.modules.booking.BookingUrlService
import com.calendarengine.modules.booking.actions.CalculateAvailabilityAction
import com.calendarengine.modules.booking.actions.CancelBookingAction
import com.calendarengine.modules.booking.actions.CreateBookingAction
import com.calendarengine.modules.calendar.CalendarService
import com.calendarengine.modules.event.EventService
import com.calendarengine.modules.event.ICalExporter
import com.calendarengine.modules.event.RruleExpander
import com.calendarengine.modules.event.actions.*
import com.calendarengine.modules.notification.BookingCancelledTableListener
import com.calendarengine.modules.notification.BookingCreatedTableListener
import com.calendarengine.modules.sync.ConnectionService
import com.calendarengine.modules.sync.ExternalBusyBlockService
import com.calendarengine.modules.sync.AppleCalDavSyncService
import com.calendarengine.modules.sync.GoogleCalendarSyncService
import com.calendarengine.modules.sync.SyncPollingScheduler
import com.calendarengine.modules.sync.actions.GoogleOAuthAction
import com.calendarengine.modules.tenant.TenantService
import com.cristianllanos.container.Container
import com.cristianllanos.container.ServiceProvider
import com.cristianllanos.container.resolve
import com.cristianllanos.container.singleton
import com.cristianllanos.events.EventServiceProvider
import com.cristianllanos.events.Subscriber
import com.cristianllanos.events.subscribe
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*

class AppServiceProvider : ServiceProvider {
    override fun register(container: Container) {
        // Event bus
        container.register(EventServiceProvider())

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

        // Notification listeners
        container.singleton<BookingCreatedTableListener> { BookingCreatedTableListener() }
        container.singleton<BookingCancelledTableListener> { BookingCancelledTableListener() }

        // Booking module
        container.singleton<BookingUrlService> { BookingUrlService() }
        container.singleton<CalculateAvailabilityAction> { CalculateAvailabilityAction(resolve()) }
        container.singleton<CreateBookingAction> { CreateBookingAction(resolve(), resolve()) }
        container.singleton<CancelBookingAction> { CancelBookingAction(resolve()) }
        container.singleton<BookingService> { BookingService(resolve(), resolve()) }

        // Sync module
        container.singleton<HttpClient> {
            HttpClient(CIO) {
                install(ContentNegotiation) { json() }
            }
        }
        container.singleton<ExternalBusyBlockService> { ExternalBusyBlockService() }
        container.singleton<GoogleOAuthAction> { GoogleOAuthAction(resolve()) }
        container.singleton<ConnectionService> { ConnectionService() }
        container.singleton<GoogleCalendarSyncService> { GoogleCalendarSyncService(resolve(), resolve(), resolve()) }
        container.singleton<AppleCalDavSyncService> { AppleCalDavSyncService(resolve(), resolve()) }
        container.singleton<SyncPollingScheduler> { SyncPollingScheduler(resolve(), resolve()) }

        // Subscribe listeners to events
        val subscriber = container.resolve<Subscriber>()
        subscriber.subscribe<BookingCreatedEvent, BookingCreatedTableListener>()
        subscriber.subscribe<BookingCancelledEvent, BookingCancelledTableListener>()
    }
}

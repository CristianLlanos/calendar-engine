package com.cristianllanos.calendarengine.container

import com.cristianllanos.calendarengine.dto.BookingCancelledEvent
import com.cristianllanos.calendarengine.dto.BookingCreatedEvent
import com.cristianllanos.calendarengine.modules.booking.BookingService
import com.cristianllanos.calendarengine.modules.booking.BookingUrlService
import com.cristianllanos.calendarengine.modules.booking.actions.CalculateAvailabilityAction
import com.cristianllanos.calendarengine.modules.booking.actions.CancelBookingAction
import com.cristianllanos.calendarengine.modules.booking.actions.CreateBookingAction
import com.cristianllanos.calendarengine.modules.calendar.CalendarService
import com.cristianllanos.calendarengine.modules.event.EventService
import com.cristianllanos.calendarengine.modules.event.ICalExporter
import com.cristianllanos.calendarengine.modules.event.RruleExpander
import com.cristianllanos.calendarengine.modules.event.actions.*
import com.cristianllanos.calendarengine.modules.notification.BookingCancelledTableListener
import com.cristianllanos.calendarengine.modules.notification.BookingCreatedTableListener
import com.cristianllanos.calendarengine.modules.sync.ConnectionService
import com.cristianllanos.calendarengine.modules.sync.ExternalBusyBlockService
import com.cristianllanos.calendarengine.modules.sync.AppleCalDavSyncService
import com.cristianllanos.calendarengine.modules.sync.GoogleCalendarSyncService
import com.cristianllanos.calendarengine.modules.sync.SyncPollingScheduler
import com.cristianllanos.calendarengine.modules.sync.actions.GoogleOAuthAction
import com.cristianllanos.calendarengine.modules.tenant.TenantService
import com.cristianllanos.container.Container
import com.cristianllanos.container.resolve
import com.cristianllanos.container.singleton
import com.cristianllanos.events.EventServiceProvider
import com.cristianllanos.events.Subscriber
import com.cristianllanos.events.subscribe
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*

/** Registers all calendar engine services, actions, and event subscribers into the DI container. */
class AppServiceProvider {
    /** Binds all service singletons and wires event subscribers into the given [container]. */
    fun register(container: Container) {
        container.register(EventServiceProvider())

        container.singleton<TenantService>()
        container.singleton<CalendarService>()
        container.singleton<RruleExpander>()
        container.singleton<ICalExporter>()
        container.singleton<BookingUrlService>()
        container.singleton<ExternalBusyBlockService>()
        container.singleton<ConnectionService>()
        container.singleton<BookingCreatedTableListener>()
        container.singleton<BookingCancelledTableListener>()

        container.singleton<CreateEventAction>()
        container.singleton<UpdateEventAction>()
        container.singleton<DeleteEventAction>()
        container.singleton<UpdateOccurrenceAction>()
        container.singleton<ExpandOccurrencesAction>()
        container.singleton<EventService>()

        container.singleton<CalculateAvailabilityAction>()
        container.singleton<CreateBookingAction>()
        container.singleton<CancelBookingAction>()
        container.singleton<BookingService>()

        container.singleton<HttpClient> {
            HttpClient(CIO) {
                install(ContentNegotiation) { json() }
            }
        }
        container.singleton<GoogleOAuthAction>()
        container.singleton<GoogleCalendarSyncService>()
        container.singleton<AppleCalDavSyncService>()
        container.singleton<SyncPollingScheduler>()

        val subscriber = container.resolve<Subscriber>()
        subscriber.subscribe<BookingCreatedEvent, BookingCreatedTableListener>()
        subscriber.subscribe<BookingCancelledEvent, BookingCancelledTableListener>()
    }
}

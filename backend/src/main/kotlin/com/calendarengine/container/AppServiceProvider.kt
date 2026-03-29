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

        // No-arg services (auto-resolved)
        container.singleton<TenantService> { resolve() }
        container.singleton<CalendarService> { resolve() }
        container.singleton<RruleExpander> { resolve() }
        container.singleton<ICalExporter> { resolve() }
        container.singleton<BookingUrlService> { resolve() }
        container.singleton<ExternalBusyBlockService> { resolve() }
        container.singleton<ConnectionService> { resolve() }
        container.singleton<BookingCreatedTableListener> { resolve() }
        container.singleton<BookingCancelledTableListener> { resolve() }

        // Event actions (auto-resolved via constructor injection)
        container.singleton<CreateEventAction> { resolve() }
        container.singleton<UpdateEventAction> { resolve() }
        container.singleton<DeleteEventAction> { resolve() }
        container.singleton<UpdateOccurrenceAction> { resolve() }
        container.singleton<ExpandOccurrencesAction> { resolve() }
        container.singleton<EventService> { resolve() }

        // Booking actions (auto-resolved via constructor injection)
        container.singleton<CalculateAvailabilityAction> { resolve() }
        container.singleton<CreateBookingAction> { resolve() }
        container.singleton<CancelBookingAction> { resolve() }
        container.singleton<BookingService> { resolve() }

        // Sync module
        container.singleton<HttpClient> {
            HttpClient(CIO) {
                install(ContentNegotiation) { json() }
            }
        }
        container.singleton<GoogleOAuthAction> { resolve() }
        container.singleton<GoogleCalendarSyncService> { resolve() }
        container.singleton<AppleCalDavSyncService> { resolve() }
        container.singleton<SyncPollingScheduler> { resolve() }

        // Subscribe listeners to events
        val subscriber = container.resolve<Subscriber>()
        subscriber.subscribe<BookingCreatedEvent, BookingCreatedTableListener>()
        subscriber.subscribe<BookingCancelledEvent, BookingCancelledTableListener>()
    }
}

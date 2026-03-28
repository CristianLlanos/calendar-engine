import {
  createRequest,
  createCalendarsApi,
  createEventsApi,
  createBookingUrlsApi,
  createBookingsApi,
  createSyncApi,
} from "./api";
import type { CalendarEngineConfig } from "./api";

export function createClient(config: CalendarEngineConfig) {
  const request = createRequest(config);

  return {
    calendars: createCalendarsApi(request),
    events: createEventsApi(request),
    bookingUrls: createBookingUrlsApi(request),
    bookings: createBookingsApi(request),
    sync: createSyncApi(request),
  };
}

export type CalendarEngineClient = ReturnType<typeof createClient>;

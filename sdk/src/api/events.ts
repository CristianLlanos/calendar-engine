import type {
  EventResponse,
  EventOccurrence,
  CreateEventRequest,
  UpdateEventRequest,
  UpdateOccurrenceRequest,
  DeleteScope,
} from "../types";
import { createRequest } from "./request";

export function createEventsApi(request: ReturnType<typeof createRequest>) {
  return {
    async list(calendarId: number, start: string, end: string): Promise<EventOccurrence[]> {
      return request(
        `/api/calendars/${calendarId}/events?start=${encodeURIComponent(start)}&end=${encodeURIComponent(end)}`,
      );
    },

    async get(calendarId: number, eventId: number): Promise<EventResponse> {
      return request(`/api/calendars/${calendarId}/events/${eventId}`);
    },

    async create(calendarId: number, data: CreateEventRequest): Promise<EventResponse> {
      return request(`/api/calendars/${calendarId}/events`, {
        method: "POST",
        body: JSON.stringify(data),
      });
    },

    async update(
      calendarId: number,
      eventId: number,
      data: UpdateEventRequest,
    ): Promise<EventResponse> {
      return request(`/api/calendars/${calendarId}/events/${eventId}`, {
        method: "PUT",
        body: JSON.stringify(data),
      });
    },

    async updateOccurrence(
      calendarId: number,
      eventId: number,
      occurrenceDate: string,
      data: UpdateOccurrenceRequest,
    ): Promise<EventOccurrence> {
      return request(
        `/api/calendars/${calendarId}/events/${eventId}/occurrence/${encodeURIComponent(occurrenceDate)}`,
        {
          method: "PUT",
          body: JSON.stringify(data),
        },
      );
    },

    async delete(
      calendarId: number,
      eventId: number,
      scope: DeleteScope = "ALL",
      occurrenceDate?: string,
    ): Promise<void> {
      const params = [`scope=${scope}`];
      if (occurrenceDate) params.push(`occurrenceDate=${encodeURIComponent(occurrenceDate)}`);
      return request(`/api/calendars/${calendarId}/events/${eventId}?${params.join("&")}`, {
        method: "DELETE",
      });
    },

    async exportIcal(calendarId: number): Promise<string> {
      return request(`/api/calendars/${calendarId}/export.ics`);
    },
  };
}

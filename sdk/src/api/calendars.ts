import type {
  CalendarResponse,
  CreateCalendarRequest,
  UpdateCalendarRequest,
  PaginatedResponse,
  PaginationParams,
} from "../types";
import { buildPaginationQuery, createRequest, type CalendarEngineConfig } from "./request";

export function createCalendarsApi(request: ReturnType<typeof createRequest>) {
  return {
    async list(params?: PaginationParams): Promise<PaginatedResponse<CalendarResponse>> {
      return request(`/api/calendars${buildPaginationQuery(params)}`);
    },

    async get(id: number): Promise<CalendarResponse> {
      return request(`/api/calendars/${id}`);
    },

    async listPublic(params?: PaginationParams): Promise<PaginatedResponse<CalendarResponse>> {
      return request(`/api/calendars/public${buildPaginationQuery(params)}`);
    },

    async create(data: CreateCalendarRequest): Promise<CalendarResponse> {
      return request("/api/calendars", {
        method: "POST",
        body: JSON.stringify(data),
      });
    },

    async update(id: number, data: UpdateCalendarRequest): Promise<CalendarResponse> {
      return request(`/api/calendars/${id}`, {
        method: "PUT",
        body: JSON.stringify(data),
      });
    },

    async delete(id: number): Promise<void> {
      return request(`/api/calendars/${id}`, { method: "DELETE" });
    },
  };
}

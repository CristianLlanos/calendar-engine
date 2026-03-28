import type {
  BookingUrlResponse,
  CreateBookingUrlRequest,
  UpdateBookingUrlRequest,
  PaginatedResponse,
  PaginationParams,
  DayAvailability,
} from "../types";
import { buildPaginationQuery, createRequest } from "./request";

export function createBookingUrlsApi(request: ReturnType<typeof createRequest>) {
  return {
    async list(params?: PaginationParams): Promise<PaginatedResponse<BookingUrlResponse>> {
      return request(`/api/booking-urls${buildPaginationQuery(params)}`);
    },

    async get(id: number): Promise<BookingUrlResponse> {
      return request(`/api/booking-urls/${id}`);
    },

    async create(data: CreateBookingUrlRequest): Promise<BookingUrlResponse> {
      return request("/api/booking-urls", {
        method: "POST",
        body: JSON.stringify(data),
      });
    },

    async update(id: number, data: UpdateBookingUrlRequest): Promise<BookingUrlResponse> {
      return request(`/api/booking-urls/${id}`, {
        method: "PUT",
        body: JSON.stringify(data),
      });
    },

    async delete(id: number): Promise<void> {
      return request(`/api/booking-urls/${id}`, { method: "DELETE" });
    },

    async getAvailability(id: number, date: string): Promise<DayAvailability> {
      return request(`/api/booking-urls/${id}/availability?date=${encodeURIComponent(date)}`);
    },

    async getAvailabilityRange(
      id: number,
      start: string,
      end: string,
    ): Promise<DayAvailability[]> {
      return request(
        `/api/booking-urls/${id}/availability/range?start=${encodeURIComponent(start)}&end=${encodeURIComponent(end)}`,
      );
    },
  };
}

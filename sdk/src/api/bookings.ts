import type {
  BookingResponse,
  CreateBookingRequest,
  PaginatedResponse,
  PaginationParams,
} from "../types";
import { buildPaginationQuery, createRequest } from "./request";

export function createBookingsApi(request: ReturnType<typeof createRequest>) {
  return {
    async list(params?: PaginationParams): Promise<PaginatedResponse<BookingResponse>> {
      return request(`/api/bookings${buildPaginationQuery(params)}`);
    },

    async get(id: number): Promise<BookingResponse> {
      return request(`/api/bookings/${id}`);
    },

    async create(data: CreateBookingRequest): Promise<BookingResponse> {
      return request("/api/bookings", {
        method: "POST",
        body: JSON.stringify(data),
      });
    },

    async cancel(id: number, reason?: string): Promise<BookingResponse> {
      return request(`/api/bookings/${id}`, {
        method: "DELETE",
        body: reason ? JSON.stringify({ reason }) : undefined,
      });
    },
  };
}

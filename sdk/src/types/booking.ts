export interface BookingResponse {
  id: number;
  tenantId: number;
  bookingUrlId: number;
  calendarId: number;
  eventId?: number | null;
  status: string;
  startTime: string;
  endTime: string;
  bookerName: string;
  bookerEmail: string;
  bookerPhone?: string | null;
  bookerUserId?: number | null;
  notes?: string | null;
  cancelledAt?: string | null;
  cancellationReason?: string | null;
  createdAt: string;
}

export interface CreateBookingRequest {
  bookingUrlId: number;
  startTime: string;
  durationMinutes: number;
  bookerName: string;
  bookerEmail: string;
  bookerPhone?: string;
  bookerUserId?: number;
  notes?: string;
}

export interface CancelBookingRequest {
  reason?: string;
}

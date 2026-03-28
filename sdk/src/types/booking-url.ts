export interface AvailabilityWindowResponse {
  id: number;
  dayOfWeek: number;
  startTime: string;
  endTime: string;
}

export interface BookingUrlResponse {
  id: number;
  tenantId: number;
  calendarId: number;
  slug: string;
  name: string;
  description?: string | null;
  status: string;
  durationMinutes: number;
  durationOptions?: string | null;
  bufferBeforeMinutes: number;
  bufferAfterMinutes: number;
  minLeadTimeHours: number;
  maxLeadTimeDays: number;
  maxBookingsPerDay?: number | null;
  maxBookingsPerWeek?: number | null;
  autoConfirm: boolean;
  availabilityWindows: AvailabilityWindowResponse[];
  createdAt: string;
}

export interface CreateAvailabilityWindowRequest {
  dayOfWeek: number;
  startTime: string;
  endTime: string;
}

export interface CreateBookingUrlRequest {
  calendarId: number;
  slug: string;
  name: string;
  description?: string;
  durationMinutes: number;
  durationOptions?: string;
  bufferBeforeMinutes?: number;
  bufferAfterMinutes?: number;
  minLeadTimeHours?: number;
  maxLeadTimeDays?: number;
  maxBookingsPerDay?: number;
  maxBookingsPerWeek?: number;
  autoConfirm?: boolean;
  availabilityWindows?: CreateAvailabilityWindowRequest[];
}

export interface UpdateBookingUrlRequest {
  slug?: string;
  name?: string;
  description?: string;
  status?: string;
  durationMinutes?: number;
  durationOptions?: string;
  bufferBeforeMinutes?: number;
  bufferAfterMinutes?: number;
  minLeadTimeHours?: number;
  maxLeadTimeDays?: number;
  maxBookingsPerDay?: number;
  maxBookingsPerWeek?: number;
  autoConfirm?: boolean;
  availabilityWindows?: CreateAvailabilityWindowRequest[];
}

export interface CalendarResponse {
  id: number;
  tenantId: number;
  name: string;
  description?: string | null;
  timezone: string;
  color?: string | null;
  visibility: string;
  createdAt: string;
}

export interface CreateCalendarRequest {
  name: string;
  description?: string;
  timezone?: string;
  color?: string;
  visibility?: string;
}

export interface UpdateCalendarRequest {
  name?: string;
  description?: string;
  timezone?: string;
  color?: string;
  visibility?: string;
}

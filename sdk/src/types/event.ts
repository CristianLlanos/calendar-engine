export interface RecurrenceRuleResponse {
  id: number;
  rrule: string;
  dtstart: string;
  timezone: string;
}

export interface EventResponse {
  id: number;
  calendarId: number;
  title: string;
  description?: string | null;
  location?: string | null;
  startTime: string;
  endTime: string;
  allDay: boolean;
  status: string;
  isRecurring: boolean;
  recurrenceRule?: RecurrenceRuleResponse | null;
  createdBy?: number | null;
  createdAt: string;
  updatedAt: string;
}

export interface EventOccurrence {
  eventId: number;
  calendarId: number;
  title: string;
  description?: string | null;
  location?: string | null;
  startTime: string;
  endTime: string;
  allDay: boolean;
  status: string;
  isRecurring: boolean;
  isException?: boolean;
  originalDate?: string | null;
}

export interface CreateRecurrenceRequest {
  rrule: string;
  timezone?: string;
}

export interface CreateEventRequest {
  title: string;
  description?: string;
  location?: string;
  startTime: string;
  endTime: string;
  allDay?: boolean;
  status?: string;
  recurrence?: CreateRecurrenceRequest;
}

export interface UpdateEventRequest {
  title?: string;
  description?: string;
  location?: string;
  startTime?: string;
  endTime?: string;
  allDay?: boolean;
  status?: string;
}

export interface UpdateOccurrenceRequest {
  title?: string;
  description?: string;
  location?: string;
  startTime?: string;
  endTime?: string;
}

export type DeleteScope = "ALL" | "THIS" | "FOLLOWING";

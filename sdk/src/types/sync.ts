export interface ExternalCalendarConnection {
  id: number;
  tenantId: number;
  calendarId: number;
  provider: string;
  externalCalendarId: string;
  syncMode: string;
  lastSyncedAt?: string | null;
  enabled: boolean;
  createdAt: string;
}

export interface CreateConnectionRequest {
  calendarId: number;
  provider: string;
  externalCalendarId: string;
  syncMode?: string;
}

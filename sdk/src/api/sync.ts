import type { ExternalCalendarConnection, CreateConnectionRequest } from "../types";
import { createRequest } from "./request";

export function createSyncApi(request: ReturnType<typeof createRequest>) {
  return {
    initiateGoogleAuth(calendarId: number): string {
      return `/api/sync/google/auth?calendarId=${calendarId}`;
    },

    async listConnections(): Promise<ExternalCalendarConnection[]> {
      return request("/api/sync/connections");
    },

    async createConnection(data: CreateConnectionRequest): Promise<ExternalCalendarConnection> {
      return request("/api/sync/connections", {
        method: "POST",
        body: JSON.stringify(data),
      });
    },

    async removeConnection(id: number): Promise<void> {
      return request(`/api/sync/connections/${id}`, { method: "DELETE" });
    },

    async triggerSync(connectionId: number): Promise<void> {
      return request(`/api/sync/connections/${connectionId}/sync`, { method: "POST" });
    },
  };
}

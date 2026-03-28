import { useCallback, useEffect, useState } from "react";
import type { EventOccurrence } from "@calendarengine/sdk";
import { useCalendarEngine } from "../CalendarEngineProvider";

export function useCalendarEvents(calendarId: number, start: string, end: string) {
  const { client } = useCalendarEngine();
  const [events, setEvents] = useState<EventOccurrence[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await client.events.list(calendarId, start, end);
      setEvents(data);
    } catch (err) {
      setError(err instanceof Error ? err : new Error("Failed to load events"));
    } finally {
      setLoading(false);
    }
  }, [client, calendarId, start, end]);

  useEffect(() => {
    load();
  }, [load]);

  return { events, loading, error, reload: load };
}

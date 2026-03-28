import { useCallback, useEffect, useState } from "react";
import type { EventOccurrence } from "@calendarengine/sdk";
import { useCalendarEngine } from "../CalendarEngineProvider";

export function useMergedCalendars(calendarIds: number[], start: string, end: string) {
  const { client } = useCalendarEngine();
  const [events, setEvents] = useState<EventOccurrence[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const results = await Promise.all(
        calendarIds.map((id) => client.events.list(id, start, end)),
      );
      const merged = results.flat().sort((a, b) => a.startTime.localeCompare(b.startTime));
      setEvents(merged);
    } catch (err) {
      setError(err instanceof Error ? err : new Error("Failed to load events"));
    } finally {
      setLoading(false);
    }
  }, [client, calendarIds.join(","), start, end]);

  useEffect(() => {
    load();
  }, [load]);

  return { events, loading, error, reload: load };
}

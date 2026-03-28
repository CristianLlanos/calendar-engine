import { useCallback, useEffect, useState } from "react";
import type { DayAvailability } from "@calendarengine/sdk";
import { useCalendarEngine } from "../CalendarEngineProvider";

export function useAvailabilitySlots(bookingUrlId: number, date: string) {
  const { client } = useCalendarEngine();
  const [availability, setAvailability] = useState<DayAvailability | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await client.bookingUrls.getAvailability(bookingUrlId, date);
      setAvailability(data);
    } catch (err) {
      setError(err instanceof Error ? err : new Error("Failed to load availability"));
    } finally {
      setLoading(false);
    }
  }, [client, bookingUrlId, date]);

  useEffect(() => {
    load();
  }, [load]);

  return { availability, loading, error, reload: load };
}

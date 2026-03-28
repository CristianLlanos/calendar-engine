import { useCallback, useState } from "react";

export type CalendarView = "month" | "week" | "day";

export function useCalendarNavigation(initialDate?: Date, initialView?: CalendarView) {
  const [currentDate, setCurrentDate] = useState(initialDate ?? new Date());
  const [view, setView] = useState<CalendarView>(initialView ?? "month");

  const goToToday = useCallback(() => setCurrentDate(new Date()), []);

  const goNext = useCallback(() => {
    setCurrentDate((prev) => {
      const next = new Date(prev);
      if (view === "month") next.setMonth(next.getMonth() + 1);
      else if (view === "week") next.setDate(next.getDate() + 7);
      else next.setDate(next.getDate() + 1);
      return next;
    });
  }, [view]);

  const goPrev = useCallback(() => {
    setCurrentDate((prev) => {
      const next = new Date(prev);
      if (view === "month") next.setMonth(next.getMonth() - 1);
      else if (view === "week") next.setDate(next.getDate() - 7);
      else next.setDate(next.getDate() - 1);
      return next;
    });
  }, [view]);

  const goToDate = useCallback((date: Date) => setCurrentDate(date), []);

  return {
    currentDate,
    view,
    setView,
    goToToday,
    goNext,
    goPrev,
    goToDate,
  };
}

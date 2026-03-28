import type { EventOccurrence } from "@calendarengine/sdk";
import { useCalendarEvents } from "../hooks/use-calendar-events";
import { useCalendarNavigation } from "../hooks/use-calendar-navigation";
import { CalendarHeader } from "./CalendarHeader";
import { DayCell } from "./DayCell";
import { DAY_NAMES, getMonthCalendarDates, toDateTimeRange } from "./utils";

interface MonthViewProps {
  calendarId: number;
  onEventClick?: (event: EventOccurrence) => void;
  onDateClick?: (date: Date) => void;
}

export function MonthView({ calendarId, onEventClick, onDateClick }: MonthViewProps) {
  const nav = useCalendarNavigation();
  const { start, end } = toDateTimeRange(nav.currentDate, "month");
  const { events, loading } = useCalendarEvents(calendarId, start, end);

  const dates = getMonthCalendarDates(nav.currentDate.getFullYear(), nav.currentDate.getMonth());

  return (
    <div className="ce-month-view" style={{ fontFamily: "var(--ce-font-family)" }}>
      <CalendarHeader
        currentDate={nav.currentDate}
        view={nav.view}
        onViewChange={nav.setView}
        onPrev={nav.goPrev}
        onNext={nav.goNext}
        onToday={nav.goToToday}
      />

      {loading && (
        <div style={{ textAlign: "center", padding: "20px", color: "var(--ce-text-muted)" }}>Loading...</div>
      )}

      <div
        style={{
          display: "grid",
          gridTemplateColumns: "repeat(7, 1fr)",
          border: "1px solid var(--ce-border)",
          borderRadius: "var(--ce-radius)",
          overflow: "hidden",
        }}
      >
        {DAY_NAMES.map((day) => (
          <div
            key={day}
            style={{
              padding: "8px 4px",
              textAlign: "center",
              fontSize: "var(--ce-font-size-sm)",
              fontWeight: 600,
              color: "var(--ce-text-muted)",
              backgroundColor: "var(--ce-bg-secondary)",
              borderRight: "1px solid var(--ce-border)",
              borderBottom: "1px solid var(--ce-border)",
            }}
          >
            {day}
          </div>
        ))}

        {dates.map((date, i) => (
          <DayCell
            key={i}
            date={date}
            currentMonth={nav.currentDate.getMonth()}
            events={events}
            onEventClick={onEventClick}
            onDateClick={onDateClick}
          />
        ))}
      </div>
    </div>
  );
}

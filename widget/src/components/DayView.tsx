import type { EventOccurrence } from "@calendarengine/sdk";
import { useCalendarEvents } from "../hooks/use-calendar-events";
import { useCalendarNavigation } from "../hooks/use-calendar-navigation";
import { CalendarHeader } from "./CalendarHeader";
import { EventChip } from "./EventChip";
import { isSameDay, toDateTimeRange } from "./utils";

interface DayViewProps {
  calendarId: number;
  onEventClick?: (event: EventOccurrence) => void;
}

export function DayView({ calendarId, onEventClick }: DayViewProps) {
  const nav = useCalendarNavigation(undefined, "day");
  const { start, end } = toDateTimeRange(nav.currentDate, "day");
  const { events, loading } = useCalendarEvents(calendarId, start, end);

  const hours = Array.from({ length: 16 }, (_, i) => i + 7); // 7:00 - 22:00
  const dayEvents = events.filter((e) => isSameDay(new Date(e.startTime), nav.currentDate));

  return (
    <div className="ce-day-view" style={{ fontFamily: "var(--ce-font-family)" }}>
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
          border: "1px solid var(--ce-border)",
          borderRadius: "var(--ce-radius)",
          overflow: "hidden",
        }}
      >
        {hours.map((hour) => {
          const hourEvents = dayEvents.filter((e) => new Date(e.startTime).getHours() === hour);

          return (
            <div
              key={hour}
              style={{
                display: "grid",
                gridTemplateColumns: "60px 1fr",
                borderBottom: "1px solid var(--ce-border)",
                minHeight: "48px",
              }}
            >
              <div
                style={{
                  padding: "4px 8px",
                  fontSize: "var(--ce-font-size-sm)",
                  color: "var(--ce-text-muted)",
                  textAlign: "right",
                  borderRight: "1px solid var(--ce-border)",
                }}
              >
                {String(hour).padStart(2, "0")}:00
              </div>
              <div style={{ padding: "2px 4px" }}>
                {hourEvents.map((event, i) => (
                  <EventChip key={`${event.eventId}-${i}`} event={event} onClick={onEventClick} />
                ))}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}

import type { EventOccurrence } from "@calendarengine/sdk";
import { useCalendarEvents } from "../hooks/use-calendar-events";
import { useCalendarNavigation } from "../hooks/use-calendar-navigation";
import { CalendarHeader } from "./CalendarHeader";
import { EventChip } from "./EventChip";
import { DAY_NAMES, formatDateISO, getWeekDates, isSameDay, isToday, toDateTimeRange } from "./utils";

interface WeekViewProps {
  calendarId: number;
  onEventClick?: (event: EventOccurrence) => void;
}

export function WeekView({ calendarId, onEventClick }: WeekViewProps) {
  const nav = useCalendarNavigation(undefined, "week");
  const { start, end } = toDateTimeRange(nav.currentDate, "week");
  const { events, loading } = useCalendarEvents(calendarId, start, end);

  const weekDates = getWeekDates(nav.currentDate);
  const hours = Array.from({ length: 16 }, (_, i) => i + 7); // 7:00 - 22:00

  return (
    <div className="ce-week-view" style={{ fontFamily: "var(--ce-font-family)" }}>
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
          gridTemplateColumns: "60px repeat(7, 1fr)",
          border: "1px solid var(--ce-border)",
          borderRadius: "var(--ce-radius)",
          overflow: "hidden",
        }}
      >
        {/* Header row */}
        <div style={{ backgroundColor: "var(--ce-bg-secondary)", borderBottom: "1px solid var(--ce-border)" }} />
        {weekDates.map((date, i) => (
          <div
            key={i}
            style={{
              padding: "8px 4px",
              textAlign: "center",
              fontSize: "var(--ce-font-size-sm)",
              fontWeight: 600,
              color: isToday(date) ? "var(--ce-primary)" : "var(--ce-text-muted)",
              backgroundColor: isToday(date) ? "var(--ce-today-bg)" : "var(--ce-bg-secondary)",
              borderRight: "1px solid var(--ce-border)",
              borderBottom: "1px solid var(--ce-border)",
            }}
          >
            {DAY_NAMES[i]}
            <div style={{ fontSize: "var(--ce-font-size-lg)", fontWeight: isToday(date) ? 700 : 400 }}>
              {date.getDate()}
            </div>
          </div>
        ))}

        {/* Time grid */}
        {hours.map((hour) => (
          <>
            <div
              key={`time-${hour}`}
              style={{
                padding: "4px 8px",
                fontSize: "var(--ce-font-size-sm)",
                color: "var(--ce-text-muted)",
                textAlign: "right",
                borderRight: "1px solid var(--ce-border)",
                borderBottom: "1px solid var(--ce-border)",
                height: "48px",
              }}
            >
              {String(hour).padStart(2, "0")}:00
            </div>
            {weekDates.map((date, di) => {
              const cellEvents = events.filter((e) => {
                const eventDate = new Date(e.startTime);
                return isSameDay(eventDate, date) && eventDate.getHours() === hour;
              });

              return (
                <div
                  key={`cell-${hour}-${di}`}
                  style={{
                    borderRight: "1px solid var(--ce-border)",
                    borderBottom: "1px solid var(--ce-border)",
                    padding: "2px",
                    height: "48px",
                    overflow: "hidden",
                  }}
                >
                  {cellEvents.map((event, ei) => (
                    <EventChip key={`${event.eventId}-${ei}`} event={event} onClick={onEventClick} />
                  ))}
                </div>
              );
            })}
          </>
        ))}
      </div>
    </div>
  );
}

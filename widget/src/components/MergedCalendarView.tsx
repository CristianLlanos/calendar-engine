import type { EventOccurrence } from "@calendarengine/sdk";
import { useMergedCalendars } from "../hooks/use-merged-calendars";
import { formatTime } from "./utils";

interface MergedCalendarViewProps {
  calendarIds: number[];
  start: string;
  end: string;
  onEventClick?: (event: EventOccurrence) => void;
}

export function MergedCalendarView({ calendarIds, start, end, onEventClick }: MergedCalendarViewProps) {
  const { events, loading, error } = useMergedCalendars(calendarIds, start, end);

  if (loading) {
    return <div style={{ padding: "20px", color: "var(--ce-text-muted)", fontFamily: "var(--ce-font-family)" }}>Loading...</div>;
  }

  if (error) {
    return <div style={{ padding: "20px", color: "#ef4444", fontFamily: "var(--ce-font-family)" }}>Failed to load events</div>;
  }

  if (events.length === 0) {
    return <div style={{ padding: "20px", color: "var(--ce-text-muted)", fontFamily: "var(--ce-font-family)" }}>No events</div>;
  }

  const grouped = new Map<string, EventOccurrence[]>();
  for (const event of events) {
    const dateKey = event.startTime.split("T")[0];
    const arr = grouped.get(dateKey) ?? [];
    arr.push(event);
    grouped.set(dateKey, arr);
  }

  const CALENDAR_COLORS = ["#6366f1", "#8b5cf6", "#ec4899", "#f59e0b", "#10b981", "#3b82f6"];

  return (
    <div className="ce-merged-calendar" style={{ fontFamily: "var(--ce-font-family)" }}>
      {Array.from(grouped.entries()).map(([dateKey, dayEvents]) => (
        <div key={dateKey} style={{ marginBottom: "16px" }}>
          <div style={{
            fontSize: "var(--ce-font-size-sm)",
            fontWeight: 600,
            color: "var(--ce-text-muted)",
            padding: "4px 0",
            borderBottom: "1px solid var(--ce-border)",
            marginBottom: "6px",
          }}>
            {new Date(dateKey + "T12:00:00").toLocaleDateString(undefined, {
              weekday: "long", month: "long", day: "numeric",
            })}
          </div>

          {dayEvents.map((event, i) => {
            const colorIndex = event.calendarId % CALENDAR_COLORS.length;
            return (
              <div
                key={`${event.eventId}-${event.calendarId}-${i}`}
                onClick={() => onEventClick?.(event)}
                style={{
                  display: "flex",
                  gap: "12px",
                  padding: "8px",
                  borderRadius: "var(--ce-radius-sm)",
                  cursor: onEventClick ? "pointer" : "default",
                }}
              >
                <div style={{
                  width: "4px",
                  borderRadius: "2px",
                  backgroundColor: CALENDAR_COLORS[colorIndex],
                  flexShrink: 0,
                }} />
                <div>
                  <div style={{ fontWeight: 500, color: "var(--ce-text)" }}>{event.title}</div>
                  <div style={{ fontSize: "var(--ce-font-size-sm)", color: "var(--ce-text-muted)" }}>
                    {event.allDay ? "All day" : `${formatTime(event.startTime)} - ${formatTime(event.endTime)}`}
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      ))}
    </div>
  );
}

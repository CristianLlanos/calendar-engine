import type { EventOccurrence } from "@calendarengine/sdk";
import { useCalendarEvents } from "../hooks/use-calendar-events";
import { formatTime } from "./utils";

interface EventListProps {
  calendarId: number;
  start: string;
  end: string;
  onEventClick?: (event: EventOccurrence) => void;
}

export function EventList({ calendarId, start, end, onEventClick }: EventListProps) {
  const { events, loading, error } = useCalendarEvents(calendarId, start, end);

  if (loading) {
    return <div style={{ padding: "20px", color: "var(--ce-text-muted)", fontFamily: "var(--ce-font-family)" }}>Loading...</div>;
  }

  if (error) {
    return <div style={{ padding: "20px", color: "#ef4444", fontFamily: "var(--ce-font-family)" }}>Failed to load events</div>;
  }

  if (events.length === 0) {
    return <div style={{ padding: "20px", color: "var(--ce-text-muted)", fontFamily: "var(--ce-font-family)" }}>No events</div>;
  }

  // Group by date
  const grouped = new Map<string, EventOccurrence[]>();
  for (const event of events) {
    const dateKey = event.startTime.split("T")[0];
    const arr = grouped.get(dateKey) ?? [];
    arr.push(event);
    grouped.set(dateKey, arr);
  }

  return (
    <div className="ce-event-list" style={{ fontFamily: "var(--ce-font-family)" }}>
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

          {dayEvents.map((event, i) => (
            <div
              key={`${event.eventId}-${i}`}
              className="ce-event-list-item"
              onClick={() => onEventClick?.(event)}
              style={{
                display: "flex",
                gap: "12px",
                padding: "8px",
                borderRadius: "var(--ce-radius-sm)",
                cursor: onEventClick ? "pointer" : "default",
                transition: "background var(--ce-transition)",
              }}
            >
              <div style={{
                width: "4px",
                borderRadius: "2px",
                backgroundColor: event.isRecurring ? "var(--ce-event-recurring-bg)" : "var(--ce-primary)",
                flexShrink: 0,
              }} />
              <div>
                <div style={{ fontWeight: 500, color: "var(--ce-text)" }}>{event.title}</div>
                <div style={{ fontSize: "var(--ce-font-size-sm)", color: "var(--ce-text-muted)" }}>
                  {event.allDay ? "All day" : `${formatTime(event.startTime)} - ${formatTime(event.endTime)}`}
                </div>
                {event.location && (
                  <div style={{ fontSize: "var(--ce-font-size-sm)", color: "var(--ce-text-muted)" }}>
                    {event.location}
                  </div>
                )}
              </div>
            </div>
          ))}
        </div>
      ))}
    </div>
  );
}

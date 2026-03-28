import type { EventOccurrence } from "@calendarengine/sdk";
import { EventChip } from "./EventChip";
import { isSameDay, isToday } from "./utils";

interface DayCellProps {
  date: Date;
  currentMonth: number;
  events: EventOccurrence[];
  onEventClick?: (event: EventOccurrence) => void;
  onDateClick?: (date: Date) => void;
}

export function DayCell({ date, currentMonth, events, onEventClick, onDateClick }: DayCellProps) {
  const isCurrentMonth = date.getMonth() === currentMonth;
  const today = isToday(date);
  const dayEvents = events.filter((e) => {
    const eventDate = new Date(e.startTime);
    return isSameDay(eventDate, date);
  });

  return (
    <div
      className="ce-day-cell"
      style={{
        minHeight: "80px",
        padding: "4px",
        borderRight: "1px solid var(--ce-border)",
        borderBottom: "1px solid var(--ce-border)",
        backgroundColor: today ? "var(--ce-today-bg)" : "var(--ce-bg)",
        opacity: isCurrentMonth ? 1 : 0.4,
        cursor: onDateClick ? "pointer" : "default",
      }}
      onClick={() => onDateClick?.(date)}
    >
      <div
        style={{
          fontSize: "var(--ce-font-size-sm)",
          fontWeight: today ? 700 : 400,
          color: today ? "var(--ce-primary)" : "var(--ce-text)",
          marginBottom: "2px",
        }}
      >
        {date.getDate()}
      </div>
      {dayEvents.slice(0, 3).map((event, i) => (
        <EventChip key={`${event.eventId}-${i}`} event={event} onClick={onEventClick} />
      ))}
      {dayEvents.length > 3 && (
        <div style={{ fontSize: "var(--ce-font-size-sm)", color: "var(--ce-text-muted)", padding: "0 4px" }}>
          +{dayEvents.length - 3} more
        </div>
      )}
    </div>
  );
}

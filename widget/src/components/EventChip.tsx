import type { EventOccurrence } from "@calendarengine/sdk";
import { formatTime } from "./utils";

interface EventChipProps {
  event: EventOccurrence;
  onClick?: (event: EventOccurrence) => void;
}

export function EventChip({ event, onClick }: EventChipProps) {
  const bgVar = event.isRecurring ? "var(--ce-event-recurring-bg)" : "var(--ce-event-bg)";

  return (
    <button
      className="ce-event-chip"
      style={{
        display: "block",
        width: "100%",
        textAlign: "left",
        padding: "2px 4px",
        marginBottom: "1px",
        borderRadius: "var(--ce-radius-sm)",
        backgroundColor: bgVar,
        color: "var(--ce-event-text)",
        fontSize: "var(--ce-font-size-sm)",
        border: "none",
        cursor: onClick ? "pointer" : "default",
        overflow: "hidden",
        textOverflow: "ellipsis",
        whiteSpace: "nowrap",
        transition: "opacity var(--ce-transition)",
      }}
      onClick={() => onClick?.(event)}
      type="button"
    >
      {!event.allDay && <span>{formatTime(event.startTime)} </span>}
      {event.title}
    </button>
  );
}

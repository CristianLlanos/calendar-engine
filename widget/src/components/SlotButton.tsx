import type { AvailableSlot } from "@calendarengine/sdk";
import { formatTime } from "./utils";

interface SlotButtonProps {
  slot: AvailableSlot;
  selected: boolean;
  onClick: (slot: AvailableSlot) => void;
}

export function SlotButton({ slot, selected, onClick }: SlotButtonProps) {
  return (
    <button
      className="ce-slot-btn"
      type="button"
      onClick={() => onClick(slot)}
      style={{
        display: "block",
        width: "100%",
        padding: "10px 12px",
        marginBottom: "6px",
        border: selected ? "2px solid var(--ce-slot-selected)" : "1px solid var(--ce-border)",
        borderRadius: "var(--ce-radius)",
        backgroundColor: selected ? "var(--ce-slot-selected)" : "var(--ce-slot-available)",
        color: selected ? "var(--ce-primary-text)" : "var(--ce-text)",
        fontSize: "var(--ce-font-size)",
        fontFamily: "var(--ce-font-family)",
        cursor: "pointer",
        textAlign: "center",
        transition: "all var(--ce-transition)",
      }}
    >
      {formatTime(slot.startTime)}
      {slot.availableDurations.length > 1 && (
        <span style={{ fontSize: "var(--ce-font-size-sm)", color: selected ? "var(--ce-primary-text)" : "var(--ce-text-muted)", marginLeft: "8px" }}>
          ({slot.availableDurations.join(" / ")} min)
        </span>
      )}
    </button>
  );
}

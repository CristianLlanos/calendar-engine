import { useState } from "react";
import type { CreateEventRequest, EventResponse } from "@calendarengine/sdk";
import { useCalendarEngine } from "../CalendarEngineProvider";
import { RecurrenceEditor } from "./RecurrenceEditor";

interface EventFormProps {
  calendarId: number;
  initialDate?: string;
  onCreated?: (event: EventResponse) => void;
  onCancel?: () => void;
}

export function EventForm({ calendarId, initialDate, onCreated, onCancel }: EventFormProps) {
  const { client, role } = useCalendarEngine();

  if (role !== "owner") return null;

  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [location, setLocation] = useState("");
  const [startTime, setStartTime] = useState(initialDate ?? "");
  const [endTime, setEndTime] = useState("");
  const [allDay, setAllDay] = useState(false);
  const [isRecurring, setIsRecurring] = useState(false);
  const [rrule, setRrule] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    setError(null);

    try {
      const request: CreateEventRequest = {
        title,
        description: description || undefined,
        location: location || undefined,
        startTime,
        endTime,
        allDay,
      };

      if (isRecurring && rrule) {
        request.recurrence = { rrule };
      }

      const event = await client.events.create(calendarId, request);
      onCreated?.(event);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to create event");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <form className="ce-event-form" onSubmit={handleSubmit} style={{ fontFamily: "var(--ce-font-family)" }}>
      <div style={{ marginBottom: "12px" }}>
        <label style={labelStyle}>Title *</label>
        <input type="text" required value={title} onChange={(e) => setTitle(e.target.value)} style={inputStyle} />
      </div>

      <div style={{ marginBottom: "12px" }}>
        <label style={labelStyle}>Start Time *</label>
        <input type="datetime-local" required value={startTime} onChange={(e) => setStartTime(e.target.value)} style={inputStyle} />
      </div>

      <div style={{ marginBottom: "12px" }}>
        <label style={labelStyle}>End Time *</label>
        <input type="datetime-local" required value={endTime} onChange={(e) => setEndTime(e.target.value)} style={inputStyle} />
      </div>

      <div style={{ marginBottom: "12px" }}>
        <label style={{ ...labelStyle, display: "flex", alignItems: "center", gap: "8px" }}>
          <input type="checkbox" checked={allDay} onChange={(e) => setAllDay(e.target.checked)} />
          All Day
        </label>
      </div>

      <div style={{ marginBottom: "12px" }}>
        <label style={labelStyle}>Description</label>
        <textarea value={description} onChange={(e) => setDescription(e.target.value)} rows={2} style={{ ...inputStyle, resize: "vertical" }} />
      </div>

      <div style={{ marginBottom: "12px" }}>
        <label style={labelStyle}>Location</label>
        <input type="text" value={location} onChange={(e) => setLocation(e.target.value)} style={inputStyle} />
      </div>

      <div style={{ marginBottom: "12px" }}>
        <label style={{ ...labelStyle, display: "flex", alignItems: "center", gap: "8px" }}>
          <input type="checkbox" checked={isRecurring} onChange={(e) => setIsRecurring(e.target.checked)} />
          Recurring Event
        </label>
      </div>

      {isRecurring && <RecurrenceEditor value={rrule} onChange={setRrule} />}

      {error && (
        <div style={{ color: "#ef4444", marginBottom: "12px", fontSize: "var(--ce-font-size-sm)" }}>{error}</div>
      )}

      <div style={{ display: "flex", gap: "8px" }}>
        <button type="submit" disabled={submitting} style={submitBtnStyle}>
          {submitting ? "Creating..." : "Create Event"}
        </button>
        {onCancel && (
          <button type="button" onClick={onCancel} style={cancelBtnStyle}>
            Cancel
          </button>
        )}
      </div>
    </form>
  );
}

const labelStyle: React.CSSProperties = {
  display: "block",
  marginBottom: "4px",
  fontSize: "var(--ce-font-size-sm)",
  fontWeight: 500,
  color: "var(--ce-text)",
};

const inputStyle: React.CSSProperties = {
  width: "100%",
  padding: "8px 12px",
  border: "1px solid var(--ce-border)",
  borderRadius: "var(--ce-radius-sm)",
  fontSize: "var(--ce-font-size)",
  fontFamily: "var(--ce-font-family)",
  backgroundColor: "var(--ce-bg)",
  color: "var(--ce-text)",
  boxSizing: "border-box",
};

const submitBtnStyle: React.CSSProperties = {
  flex: 1,
  padding: "10px",
  backgroundColor: "var(--ce-primary)",
  color: "var(--ce-primary-text)",
  border: "none",
  borderRadius: "var(--ce-radius)",
  cursor: "pointer",
  fontSize: "var(--ce-font-size)",
  fontWeight: 600,
};

const cancelBtnStyle: React.CSSProperties = {
  padding: "10px 20px",
  backgroundColor: "var(--ce-bg)",
  color: "var(--ce-text)",
  border: "1px solid var(--ce-border)",
  borderRadius: "var(--ce-radius)",
  cursor: "pointer",
  fontSize: "var(--ce-font-size)",
};

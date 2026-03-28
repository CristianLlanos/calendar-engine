import type { AvailableSlot } from "@calendarengine/sdk";
import { formatTime } from "./utils";

interface BookingConfirmationProps {
  slot: AvailableSlot;
  selectedDuration: number;
  form: {
    bookerName: string;
    bookerEmail: string;
    bookerPhone: string;
    notes: string;
  };
  submitting: boolean;
  error: Error | null;
  onUpdateField: (field: "bookerName" | "bookerEmail" | "bookerPhone" | "notes", value: string) => void;
  onSubmit: () => void;
  onBack: () => void;
}

export function BookingConfirmation({
  slot,
  selectedDuration,
  form,
  submitting,
  error,
  onUpdateField,
  onSubmit,
  onBack,
}: BookingConfirmationProps) {
  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSubmit();
  };

  return (
    <div className="ce-booking-confirmation" style={{ fontFamily: "var(--ce-font-family)" }}>
      <button type="button" onClick={onBack} style={backBtnStyle}>
        &#8249; Back
      </button>

      <div style={{ padding: "12px", backgroundColor: "var(--ce-bg-secondary)", borderRadius: "var(--ce-radius)", margin: "12px 0" }}>
        <div style={{ fontWeight: 600, color: "var(--ce-text)" }}>
          {formatTime(slot.startTime)} &mdash; {selectedDuration} min
        </div>
      </div>

      <form onSubmit={handleSubmit}>
        <div style={{ marginBottom: "12px" }}>
          <label style={labelStyle}>Name *</label>
          <input
            type="text"
            required
            value={form.bookerName}
            onChange={(e) => onUpdateField("bookerName", e.target.value)}
            style={inputStyle}
          />
        </div>

        <div style={{ marginBottom: "12px" }}>
          <label style={labelStyle}>Email *</label>
          <input
            type="email"
            required
            value={form.bookerEmail}
            onChange={(e) => onUpdateField("bookerEmail", e.target.value)}
            style={inputStyle}
          />
        </div>

        <div style={{ marginBottom: "12px" }}>
          <label style={labelStyle}>Phone</label>
          <input
            type="tel"
            value={form.bookerPhone}
            onChange={(e) => onUpdateField("bookerPhone", e.target.value)}
            style={inputStyle}
          />
        </div>

        <div style={{ marginBottom: "16px" }}>
          <label style={labelStyle}>Notes</label>
          <textarea
            value={form.notes}
            onChange={(e) => onUpdateField("notes", e.target.value)}
            rows={3}
            style={{ ...inputStyle, resize: "vertical" }}
          />
        </div>

        {error && (
          <div style={{ color: "#ef4444", marginBottom: "12px", fontSize: "var(--ce-font-size-sm)" }}>
            {error.message}
          </div>
        )}

        <button type="submit" disabled={submitting} style={submitBtnStyle}>
          {submitting ? "Booking..." : "Confirm Booking"}
        </button>
      </form>
    </div>
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

const backBtnStyle: React.CSSProperties = {
  border: "none",
  background: "none",
  color: "var(--ce-primary)",
  cursor: "pointer",
  fontSize: "var(--ce-font-size)",
  fontFamily: "var(--ce-font-family)",
  padding: "4px 0",
};

const submitBtnStyle: React.CSSProperties = {
  width: "100%",
  padding: "10px",
  backgroundColor: "var(--ce-primary)",
  color: "var(--ce-primary-text)",
  border: "none",
  borderRadius: "var(--ce-radius)",
  cursor: "pointer",
  fontSize: "var(--ce-font-size)",
  fontFamily: "var(--ce-font-family)",
  fontWeight: 600,
  transition: "background var(--ce-transition)",
};

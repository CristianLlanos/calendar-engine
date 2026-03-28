import { useCallback, useState } from "react";

interface RecurrenceEditorProps {
  value: string;
  onChange: (rrule: string) => void;
}

type Frequency = "DAILY" | "WEEKLY" | "MONTHLY" | "YEARLY";

const DAYS = [
  { label: "Mon", value: "MO" },
  { label: "Tue", value: "TU" },
  { label: "Wed", value: "WE" },
  { label: "Thu", value: "TH" },
  { label: "Fri", value: "FR" },
  { label: "Sat", value: "SA" },
  { label: "Sun", value: "SU" },
];

export function RecurrenceEditor({ value, onChange }: RecurrenceEditorProps) {
  const [frequency, setFrequency] = useState<Frequency>("WEEKLY");
  const [interval, setInterval] = useState(1);
  const [selectedDays, setSelectedDays] = useState<string[]>(["MO"]);
  const [endType, setEndType] = useState<"never" | "count" | "until">("never");
  const [count, setCount] = useState(10);
  const [until, setUntil] = useState("");

  const buildRrule = useCallback(() => {
    const parts = [`FREQ=${frequency}`];
    if (interval > 1) parts.push(`INTERVAL=${interval}`);
    if (frequency === "WEEKLY" && selectedDays.length > 0) {
      parts.push(`BYDAY=${selectedDays.join(",")}`);
    }
    if (endType === "count") parts.push(`COUNT=${count}`);
    if (endType === "until" && until) {
      parts.push(`UNTIL=${until.replace(/-/g, "")}T235959Z`);
    }
    const rrule = parts.join(";");
    onChange(rrule);
    return rrule;
  }, [frequency, interval, selectedDays, endType, count, until, onChange]);

  const handleChange = () => {
    // Defer to next tick so state is updated
    setTimeout(buildRrule, 0);
  };

  const toggleDay = (day: string) => {
    setSelectedDays((prev) => {
      const next = prev.includes(day) ? prev.filter((d) => d !== day) : [...prev, day];
      return next;
    });
    handleChange();
  };

  return (
    <div className="ce-recurrence-editor" style={{ padding: "12px", backgroundColor: "var(--ce-bg-secondary)", borderRadius: "var(--ce-radius)", marginBottom: "12px" }}>
      <div style={{ marginBottom: "10px" }}>
        <label style={labelStyle}>Frequency</label>
        <select
          value={frequency}
          onChange={(e) => { setFrequency(e.target.value as Frequency); handleChange(); }}
          style={selectStyle}
        >
          <option value="DAILY">Daily</option>
          <option value="WEEKLY">Weekly</option>
          <option value="MONTHLY">Monthly</option>
          <option value="YEARLY">Yearly</option>
        </select>
      </div>

      <div style={{ marginBottom: "10px" }}>
        <label style={labelStyle}>Every</label>
        <input
          type="number"
          min={1}
          max={99}
          value={interval}
          onChange={(e) => { setInterval(parseInt(e.target.value) || 1); handleChange(); }}
          style={{ ...selectStyle, width: "60px" }}
        />
        <span style={{ marginLeft: "8px", color: "var(--ce-text-muted)", fontSize: "var(--ce-font-size-sm)" }}>
          {frequency === "DAILY" ? "day(s)" : frequency === "WEEKLY" ? "week(s)" : frequency === "MONTHLY" ? "month(s)" : "year(s)"}
        </span>
      </div>

      {frequency === "WEEKLY" && (
        <div style={{ marginBottom: "10px" }}>
          <label style={labelStyle}>On days</label>
          <div style={{ display: "flex", gap: "4px" }}>
            {DAYS.map((day) => (
              <button
                key={day.value}
                type="button"
                onClick={() => toggleDay(day.value)}
                style={{
                  padding: "4px 8px",
                  borderRadius: "var(--ce-radius-sm)",
                  border: "1px solid var(--ce-border)",
                  backgroundColor: selectedDays.includes(day.value) ? "var(--ce-primary)" : "var(--ce-bg)",
                  color: selectedDays.includes(day.value) ? "var(--ce-primary-text)" : "var(--ce-text)",
                  fontSize: "var(--ce-font-size-sm)",
                  cursor: "pointer",
                }}
              >
                {day.label}
              </button>
            ))}
          </div>
        </div>
      )}

      <div style={{ marginBottom: "10px" }}>
        <label style={labelStyle}>Ends</label>
        <select value={endType} onChange={(e) => { setEndType(e.target.value as any); handleChange(); }} style={selectStyle}>
          <option value="never">Never</option>
          <option value="count">After N occurrences</option>
          <option value="until">On date</option>
        </select>
      </div>

      {endType === "count" && (
        <div style={{ marginBottom: "10px" }}>
          <input
            type="number"
            min={1}
            value={count}
            onChange={(e) => { setCount(parseInt(e.target.value) || 1); handleChange(); }}
            style={{ ...selectStyle, width: "80px" }}
          />
          <span style={{ marginLeft: "8px", color: "var(--ce-text-muted)", fontSize: "var(--ce-font-size-sm)" }}>occurrences</span>
        </div>
      )}

      {endType === "until" && (
        <div style={{ marginBottom: "10px" }}>
          <input
            type="date"
            value={until}
            onChange={(e) => { setUntil(e.target.value); handleChange(); }}
            style={selectStyle}
          />
        </div>
      )}

      <button type="button" onClick={buildRrule} style={applyBtnStyle}>
        Apply Rule
      </button>

      {value && (
        <div style={{ marginTop: "8px", fontSize: "var(--ce-font-size-sm)", color: "var(--ce-text-muted)", wordBreak: "break-all" }}>
          RRULE: {value}
        </div>
      )}
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

const selectStyle: React.CSSProperties = {
  padding: "6px 10px",
  border: "1px solid var(--ce-border)",
  borderRadius: "var(--ce-radius-sm)",
  fontSize: "var(--ce-font-size-sm)",
  fontFamily: "var(--ce-font-family)",
  backgroundColor: "var(--ce-bg)",
  color: "var(--ce-text)",
};

const applyBtnStyle: React.CSSProperties = {
  padding: "6px 16px",
  backgroundColor: "var(--ce-primary)",
  color: "var(--ce-primary-text)",
  border: "none",
  borderRadius: "var(--ce-radius-sm)",
  cursor: "pointer",
  fontSize: "var(--ce-font-size-sm)",
};

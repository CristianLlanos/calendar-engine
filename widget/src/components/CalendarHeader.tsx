import type { CalendarView } from "../hooks";
import { MONTH_NAMES } from "./utils";

interface CalendarHeaderProps {
  currentDate: Date;
  view: CalendarView;
  onViewChange: (view: CalendarView) => void;
  onPrev: () => void;
  onNext: () => void;
  onToday: () => void;
}

export function CalendarHeader({
  currentDate,
  view,
  onViewChange,
  onPrev,
  onNext,
  onToday,
}: CalendarHeaderProps) {
  const title =
    view === "day"
      ? currentDate.toLocaleDateString(undefined, { weekday: "long", month: "long", day: "numeric", year: "numeric" })
      : `${MONTH_NAMES[currentDate.getMonth()]} ${currentDate.getFullYear()}`;

  return (
    <div
      className="ce-calendar-header"
      style={{
        display: "flex",
        alignItems: "center",
        justifyContent: "space-between",
        padding: "12px 0",
        fontFamily: "var(--ce-font-family)",
      }}
    >
      <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
        <button className="ce-btn" onClick={onPrev} style={navBtnStyle} type="button">
          &#8249;
        </button>
        <button className="ce-btn" onClick={onToday} style={todayBtnStyle} type="button">
          Today
        </button>
        <button className="ce-btn" onClick={onNext} style={navBtnStyle} type="button">
          &#8250;
        </button>
      </div>

      <h2 style={{ margin: 0, fontSize: "var(--ce-font-size-lg)", fontWeight: 600, color: "var(--ce-text)" }}>
        {title}
      </h2>

      <div style={{ display: "flex", gap: "4px" }}>
        {(["month", "week", "day"] as const).map((v) => (
          <button
            key={v}
            className="ce-btn"
            onClick={() => onViewChange(v)}
            style={{
              ...viewBtnStyle,
              backgroundColor: view === v ? "var(--ce-primary)" : "transparent",
              color: view === v ? "var(--ce-primary-text)" : "var(--ce-text)",
            }}
            type="button"
          >
            {v.charAt(0).toUpperCase() + v.slice(1)}
          </button>
        ))}
      </div>
    </div>
  );
}

const navBtnStyle: React.CSSProperties = {
  border: "1px solid var(--ce-border)",
  borderRadius: "var(--ce-radius-sm)",
  background: "var(--ce-bg)",
  color: "var(--ce-text)",
  padding: "4px 10px",
  cursor: "pointer",
  fontSize: "18px",
  lineHeight: 1,
};

const todayBtnStyle: React.CSSProperties = {
  border: "1px solid var(--ce-border)",
  borderRadius: "var(--ce-radius-sm)",
  background: "var(--ce-bg)",
  color: "var(--ce-text)",
  padding: "4px 12px",
  cursor: "pointer",
  fontSize: "var(--ce-font-size-sm)",
};

const viewBtnStyle: React.CSSProperties = {
  border: "1px solid var(--ce-border)",
  borderRadius: "var(--ce-radius-sm)",
  padding: "4px 12px",
  cursor: "pointer",
  fontSize: "var(--ce-font-size-sm)",
  transition: "all var(--ce-transition)",
};

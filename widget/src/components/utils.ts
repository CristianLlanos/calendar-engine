export function getMonthCalendarDates(year: number, month: number): Date[] {
  const firstDay = new Date(year, month, 1);
  const lastDay = new Date(year, month + 1, 0);

  // Start from Monday
  const startOffset = (firstDay.getDay() + 6) % 7;
  const start = new Date(firstDay);
  start.setDate(start.getDate() - startOffset);

  const dates: Date[] = [];
  const current = new Date(start);

  // Always show 6 rows (42 cells)
  for (let i = 0; i < 42; i++) {
    dates.push(new Date(current));
    current.setDate(current.getDate() + 1);
  }

  return dates;
}

export function getWeekDates(date: Date): Date[] {
  const start = new Date(date);
  const day = (start.getDay() + 6) % 7; // Monday = 0
  start.setDate(start.getDate() - day);

  const dates: Date[] = [];
  for (let i = 0; i < 7; i++) {
    const d = new Date(start);
    d.setDate(d.getDate() + i);
    dates.push(d);
  }
  return dates;
}

export function isSameDay(a: Date, b: Date): boolean {
  return a.getFullYear() === b.getFullYear() &&
    a.getMonth() === b.getMonth() &&
    a.getDate() === b.getDate();
}

export function isToday(date: Date): boolean {
  return isSameDay(date, new Date());
}

export function formatTime(dateStr: string): string {
  const date = new Date(dateStr);
  return date.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
}

export function formatDateISO(date: Date): string {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, "0");
  const d = String(date.getDate()).padStart(2, "0");
  return `${y}-${m}-${d}`;
}

export function formatDateTimeISO(date: Date): string {
  return `${formatDateISO(date)}T00:00:00`;
}

export function toDateTimeRange(date: Date, view: "month" | "week" | "day") {
  if (view === "month") {
    const start = new Date(date.getFullYear(), date.getMonth(), 1);
    start.setDate(start.getDate() - 7); // include overflow
    const end = new Date(date.getFullYear(), date.getMonth() + 1, 0);
    end.setDate(end.getDate() + 7);
    return { start: formatDateTimeISO(start), end: formatDateTimeISO(end) };
  }
  if (view === "week") {
    const dates = getWeekDates(date);
    return { start: formatDateTimeISO(dates[0]), end: formatDateTimeISO(new Date(dates[6].getTime() + 86400000)) };
  }
  // day
  const next = new Date(date);
  next.setDate(next.getDate() + 1);
  return { start: formatDateTimeISO(date), end: formatDateTimeISO(next) };
}

const DAY_NAMES = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"];
const MONTH_NAMES = [
  "January", "February", "March", "April", "May", "June",
  "July", "August", "September", "October", "November", "December",
];

export { DAY_NAMES, MONTH_NAMES };

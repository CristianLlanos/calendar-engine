import "./styles.css";

export { CalendarEngineProvider, useCalendarEngine } from "./CalendarEngineProvider";
export type { CalendarEngineProviderProps, CalendarRole } from "./CalendarEngineProvider";

export {
  MonthView,
  WeekView,
  DayView,
  EventList,
  EventForm,
  RecurrenceEditor,
  BookingSlotPicker,
  BookingConfirmation,
  MergedCalendarView,
  CalendarHeader,
  EventChip,
  SlotButton,
} from "./components";

export {
  useCalendarNavigation,
  useCalendarEvents,
  useAvailabilitySlots,
  useBookingForm,
  useMergedCalendars,
} from "./hooks";
export type { CalendarView } from "./hooks";

export { defaultTheme } from "./themes";
export type { CalendarTheme } from "./themes";

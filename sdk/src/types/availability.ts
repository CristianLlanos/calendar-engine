export interface AvailableSlot {
  startTime: string;
  endTime: string;
  availableDurations: number[];
}

export interface DayAvailability {
  date: string;
  slots: AvailableSlot[];
}

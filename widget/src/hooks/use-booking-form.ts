import { useCallback, useState } from "react";
import type { AvailableSlot, BookingResponse } from "@calendarengine/sdk";
import { useCalendarEngine } from "../CalendarEngineProvider";

interface BookingFormState {
  bookerName: string;
  bookerEmail: string;
  bookerPhone: string;
  notes: string;
}

const initialFormState: BookingFormState = {
  bookerName: "",
  bookerEmail: "",
  bookerPhone: "",
  notes: "",
};

export function useBookingForm(bookingUrlId: number) {
  const { client } = useCalendarEngine();
  const [selectedSlot, setSelectedSlot] = useState<AvailableSlot | null>(null);
  const [selectedDuration, setSelectedDuration] = useState<number | null>(null);
  const [form, setForm] = useState<BookingFormState>(initialFormState);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<Error | null>(null);
  const [booking, setBooking] = useState<BookingResponse | null>(null);

  const updateField = useCallback(
    <K extends keyof BookingFormState>(field: K, value: BookingFormState[K]) => {
      setForm((prev) => ({ ...prev, [field]: value }));
    },
    [],
  );

  const selectSlot = useCallback((slot: AvailableSlot, duration?: number) => {
    setSelectedSlot(slot);
    setSelectedDuration(duration ?? slot.availableDurations[0]);
    setBooking(null);
    setError(null);
  }, []);

  const submit = useCallback(async () => {
    if (!selectedSlot || !selectedDuration) return;

    setSubmitting(true);
    setError(null);
    try {
      const result = await client.bookings.create({
        bookingUrlId,
        startTime: selectedSlot.startTime,
        durationMinutes: selectedDuration,
        bookerName: form.bookerName,
        bookerEmail: form.bookerEmail,
        bookerPhone: form.bookerPhone || undefined,
        notes: form.notes || undefined,
      });
      setBooking(result);
    } catch (err) {
      setError(err instanceof Error ? err : new Error("Failed to create booking"));
    } finally {
      setSubmitting(false);
    }
  }, [client, bookingUrlId, selectedSlot, selectedDuration, form]);

  const reset = useCallback(() => {
    setSelectedSlot(null);
    setSelectedDuration(null);
    setForm(initialFormState);
    setBooking(null);
    setError(null);
  }, []);

  return {
    selectedSlot,
    selectedDuration,
    form,
    submitting,
    error,
    booking,
    updateField,
    selectSlot,
    setSelectedDuration,
    submit,
    reset,
  };
}

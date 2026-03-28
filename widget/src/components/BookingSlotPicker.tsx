import { useState } from "react";
import { useAvailabilitySlots } from "../hooks/use-availability-slots";
import { useBookingForm } from "../hooks/use-booking-form";
import { BookingConfirmation } from "./BookingConfirmation";
import { SlotButton } from "./SlotButton";
import { formatDateISO } from "./utils";

interface BookingSlotPickerProps {
  bookingUrlId: number;
  initialDate?: Date;
}

export function BookingSlotPicker({ bookingUrlId, initialDate }: BookingSlotPickerProps) {
  const [selectedDate, setSelectedDate] = useState(initialDate ?? new Date());
  const dateStr = formatDateISO(selectedDate);
  const { availability, loading, error } = useAvailabilitySlots(bookingUrlId, dateStr);
  const bookingForm = useBookingForm(bookingUrlId);

  const goToPrevDay = () => {
    const d = new Date(selectedDate);
    d.setDate(d.getDate() - 1);
    setSelectedDate(d);
  };

  const goToNextDay = () => {
    const d = new Date(selectedDate);
    d.setDate(d.getDate() + 1);
    setSelectedDate(d);
  };

  if (bookingForm.booking) {
    return (
      <div className="ce-booking-slot-picker" style={{ fontFamily: "var(--ce-font-family)" }}>
        <div style={{ textAlign: "center", padding: "24px" }}>
          <div style={{ fontSize: "var(--ce-font-size-lg)", fontWeight: 600, color: "var(--ce-text)", marginBottom: "8px" }}>
            Booking Confirmed!
          </div>
          <div style={{ color: "var(--ce-text-muted)", marginBottom: "16px" }}>
            Your booking has been created successfully.
          </div>
          <button
            type="button"
            onClick={bookingForm.reset}
            style={primaryBtnStyle}
          >
            Book Another
          </button>
        </div>
      </div>
    );
  }

  if (bookingForm.selectedSlot) {
    return (
      <BookingConfirmation
        slot={bookingForm.selectedSlot}
        selectedDuration={bookingForm.selectedDuration!}
        form={bookingForm.form}
        submitting={bookingForm.submitting}
        error={bookingForm.error}
        onUpdateField={bookingForm.updateField}
        onSubmit={bookingForm.submit}
        onBack={() => bookingForm.selectSlot(null as any)}
      />
    );
  }

  return (
    <div className="ce-booking-slot-picker" style={{ fontFamily: "var(--ce-font-family)" }}>
      {/* Date navigation */}
      <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", padding: "12px 0" }}>
        <button type="button" onClick={goToPrevDay} style={navBtnStyle}>&#8249;</button>
        <span style={{ fontWeight: 600, color: "var(--ce-text)" }}>
          {selectedDate.toLocaleDateString(undefined, { weekday: "long", month: "long", day: "numeric" })}
        </span>
        <button type="button" onClick={goToNextDay} style={navBtnStyle}>&#8250;</button>
      </div>

      {/* Slots */}
      {loading && (
        <div style={{ textAlign: "center", padding: "20px", color: "var(--ce-text-muted)" }}>Loading...</div>
      )}

      {error && (
        <div style={{ textAlign: "center", padding: "20px", color: "#ef4444" }}>Failed to load availability</div>
      )}

      {!loading && !error && availability && (
        <>
          {availability.slots.length === 0 ? (
            <div style={{ textAlign: "center", padding: "20px", color: "var(--ce-text-muted)" }}>
              No available slots for this day
            </div>
          ) : (
            <div style={{ maxHeight: "400px", overflowY: "auto" }}>
              {availability.slots.map((slot, i) => (
                <SlotButton
                  key={i}
                  slot={slot}
                  selected={false}
                  onClick={(s) => bookingForm.selectSlot(s)}
                />
              ))}
            </div>
          )}
        </>
      )}
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

const primaryBtnStyle: React.CSSProperties = {
  padding: "8px 24px",
  backgroundColor: "var(--ce-primary)",
  color: "var(--ce-primary-text)",
  border: "none",
  borderRadius: "var(--ce-radius)",
  cursor: "pointer",
  fontSize: "var(--ce-font-size)",
  fontFamily: "var(--ce-font-family)",
};

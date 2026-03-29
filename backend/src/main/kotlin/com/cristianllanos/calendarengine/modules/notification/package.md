# Package com.cristianllanos.calendarengine.modules.notification

Event bus listeners that persist domain events to the database.

Listens for [BookingCreatedEvent] and [BookingCancelledEvent] and stores them in the `SystemEvents` table
for audit logging and downstream processing.

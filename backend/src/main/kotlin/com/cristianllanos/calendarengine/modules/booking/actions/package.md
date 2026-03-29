# Package com.cristianllanos.calendarengine.modules.booking.actions

Domain actions for bookings: creating, cancelling, and calculating availability.

[CalculateAvailabilityAction] computes free slots by combining calendar events, existing bookings,
and external busy blocks, respecting buffer times, lead times, and booking limits.

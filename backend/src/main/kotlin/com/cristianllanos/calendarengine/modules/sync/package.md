# Package com.cristianllanos.calendarengine.modules.sync

External calendar synchronization with Google Calendar and Apple CalDAV.

Supports on-demand and polling sync modes. [GoogleCalendarSyncService] uses incremental sync tokens
for efficient updates. [AppleCalDavSyncService] syncs via CalDAV protocol. [SyncPollingScheduler]
runs periodic sync jobs in the background.

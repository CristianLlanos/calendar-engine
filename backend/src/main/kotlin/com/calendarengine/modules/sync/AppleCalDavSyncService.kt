package com.calendarengine.modules.sync

import com.calendarengine.models.ExternalCalendarConnections
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class AppleCalDavSyncService(
    private val httpClient: HttpClient,
    private val busyBlockService: ExternalBusyBlockService,
) {

    private val logger = LoggerFactory.getLogger(AppleCalDavSyncService::class.java)

    suspend fun syncConnection(connectionId: Int) {
        val connection = transaction {
            ExternalCalendarConnections.selectAll()
                .where { ExternalCalendarConnections.id eq connectionId }
                .firstOrNull()
        } ?: throw NoSuchElementException("Connection not found")

        val tenantId = connection[ExternalCalendarConnections.tenantId]
        val calendarId = connection[ExternalCalendarConnections.calendarId]
        val calDavUrl = connection[ExternalCalendarConnections.externalCalendarId]

        try {
            val busyBlocks = fetchFreeBusy(calDavUrl)
            busyBlockService.replaceBlocksForConnection(connectionId, tenantId, calendarId, busyBlocks)

            transaction {
                ExternalCalendarConnections.update({ ExternalCalendarConnections.id eq connectionId }) {
                    it[lastSyncedAt] = LocalDateTime.now()
                }
            }
        } catch (e: Exception) {
            logger.error("Apple CalDAV sync failed for connection $connectionId: ${e.message}")
            throw e
        }
    }

    private suspend fun fetchFreeBusy(calDavUrl: String): List<BusyBlock> {
        // CalDAV REPORT request for free/busy information
        // The calendar URL should be the user's CalDAV endpoint
        // e.g., https://caldav.icloud.com/{dsid}/calendars/{calendar-id}/

        val now = LocalDateTime.now()
        val rangeEnd = now.plusMonths(3) // Sync 3 months ahead

        val reportBody = buildCalDavReportBody(now, rangeEnd)

        val response = httpClient.request(calDavUrl) {
            method = HttpMethod("REPORT")
            header("Depth", "1")
            contentType(ContentType("application", "xml"))
            setBody(reportBody)
        }

        val responseBody = response.body<String>()
        return parseCalDavResponse(responseBody)
    }

    private fun buildCalDavReportBody(start: LocalDateTime, end: LocalDateTime): String {
        val dtStart = start.format(CALDAV_DATE_FORMAT)
        val dtEnd = end.format(CALDAV_DATE_FORMAT)

        return """<?xml version="1.0" encoding="UTF-8"?>
            |<C:calendar-query xmlns:D="DAV:" xmlns:C="urn:ietf:params:xml:ns:caldav">
            |  <D:prop>
            |    <D:getetag/>
            |    <C:calendar-data>
            |      <C:comp name="VCALENDAR">
            |        <C:comp name="VEVENT">
            |          <C:prop name="DTSTART"/>
            |          <C:prop name="DTEND"/>
            |          <C:prop name="UID"/>
            |        </C:comp>
            |      </C:comp>
            |    </C:calendar-data>
            |  </D:prop>
            |  <C:filter>
            |    <C:comp-filter name="VCALENDAR">
            |      <C:comp-filter name="VEVENT">
            |        <C:time-range start="${dtStart}Z" end="${dtEnd}Z"/>
            |      </C:comp-filter>
            |    </C:comp-filter>
            |  </C:filter>
            |</C:calendar-query>""".trimMargin()
    }

    private fun parseCalDavResponse(xml: String): List<BusyBlock> {
        // Simple regex-based parser for VEVENT DTSTART/DTEND
        // A production implementation would use a proper iCal/XML parser
        val blocks = mutableListOf<BusyBlock>()
        val eventPattern = Regex("""DTSTART[^:]*:(\d{8}T\d{6}Z?).*?DTEND[^:]*:(\d{8}T\d{6}Z?).*?UID:([^\r\n]+)""", RegexOption.DOT_MATCHES_ALL)

        for (match in eventPattern.findAll(xml)) {
            val startStr = match.groupValues[1]
            val endStr = match.groupValues[2]
            val uid = match.groupValues[3].trim()

            val start = parseICalDateTime(startStr) ?: continue
            val end = parseICalDateTime(endStr) ?: continue

            blocks.add(BusyBlock(start, end, uid))
        }

        return blocks
    }

    private fun parseICalDateTime(str: String): LocalDateTime? {
        return try {
            // Format: 20260315T090000Z or 20260315T090000
            val clean = str.removeSuffix("Z")
            LocalDateTime.parse(clean, ICAL_DATE_FORMAT)
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private val CALDAV_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
        private val ICAL_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
    }
}

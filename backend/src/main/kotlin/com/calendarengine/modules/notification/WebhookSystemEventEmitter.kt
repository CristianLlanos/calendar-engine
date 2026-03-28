package com.calendarengine.modules.notification

import com.calendarengine.dto.CalendarSystemEvent
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

class WebhookSystemEventEmitter(
    private val httpClient: HttpClient,
    private val webhookUrl: String,
) : SystemEventEmitter {

    private val logger = LoggerFactory.getLogger(WebhookSystemEventEmitter::class.java)
    private val json = Json { encodeDefaults = true }

    override fun emit(tenantId: Int, event: CalendarSystemEvent) {
        runBlocking {
            try {
                httpClient.post(webhookUrl) {
                    contentType(ContentType.Application.Json)
                    header("X-Tenant-Id", tenantId.toString())
                    header("X-Event-Type", event.type)
                    setBody(json.encodeToString(event))
                }
            } catch (e: Exception) {
                logger.error("Failed to deliver webhook event ${event.type} to $webhookUrl: ${e.message}")
            }
        }
    }
}

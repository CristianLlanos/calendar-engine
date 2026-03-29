package com.cristianllanos.calendarengine.config

import com.cristianllanos.calendarengine.models.*
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseConfig {
    /**
     * Initialize database connection from application.conf and create tables.
     * Used in standalone mode.
     */
    fun init(environment: ApplicationEnvironment) {
        val url = environment.config.property("database.url").getString()
        val driver = environment.config.property("database.driver").getString()
        val user = environment.config.property("database.user").getString()
        val password = environment.config.property("database.password").getString()

        val hikariConfig = HikariConfig().apply {
            jdbcUrl = url
            driverClassName = driver
            username = user
            this.password = password
            maximumPoolSize = 10
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }

        Database.connect(HikariDataSource(hikariConfig))
        createTables()
    }

    /**
     * Create all calendar engine tables.
     * Requires an active Exposed database connection.
     * Used in plugin mode where the host app provides the DB connection.
     */
    fun createTables() {
        transaction {
            SchemaUtils.create(
                Tenants,
                Calendars,
                Events,
                RecurrenceRules,
                RecurrenceExceptions,
                BookingUrls,
                BookingUrlAvailabilityWindows,
                Bookings,
                ExternalCalendarConnections,
                ExternalBusyBlocks,
                OAuthTokens,
                SystemEvents,
            )
        }
    }
}

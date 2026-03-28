package com.calendarengine.config

import com.calendarengine.models.*
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseConfig {
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

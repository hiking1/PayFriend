package com.payfriend.config

import com.payfriend.repository.ExposedTransactionRepository
import com.payfriend.repository.InMemoryTransactionRepository
import com.payfriend.repository.TransactionRepository
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.config.ApplicationConfig
import org.jetbrains.exposed.sql.Database
import org.slf4j.LoggerFactory

object DatabaseConfig {
    private val log = LoggerFactory.getLogger(DatabaseConfig::class.java)

    /** PostgreSQL when `payfriend.db.url` (env `DB_URL`) is set, otherwise an in-memory repository. */
    fun createRepository(config: ApplicationConfig): TransactionRepository {
        val url = config.propertyOrNull("payfriend.db.url")?.getString().orEmpty()
        if (url.isBlank()) {
            log.warn("DB_URL is not set: using the in-memory repository. Data is lost on restart.")
            return InMemoryTransactionRepository()
        }
        val hikari = HikariConfig().apply {
            jdbcUrl = url
            username = config.propertyOrNull("payfriend.db.user")?.getString().orEmpty()
            password = config.propertyOrNull("payfriend.db.password")?.getString().orEmpty()
            driverClassName = "org.postgresql.Driver"
            maximumPoolSize = 5
        }
        log.info("Connecting to PostgreSQL")
        return ExposedTransactionRepository(Database.connect(HikariDataSource(hikari)))
    }
}

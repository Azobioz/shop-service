package com.example.migrations

import com.example.config.DatabaseConfig
import org.flywaydb.core.Flyway
import org.slf4j.LoggerFactory

object FlywayMigration {
    private val logger = LoggerFactory.getLogger(FlywayMigration::class.java)

    fun migrate(config: DatabaseConfig) {
        logger.info("Running Flyway migrations on ${config.url}")
        val flyway = Flyway.configure()
            .dataSource(config.url, config.user, config.password)
            .locations("classpath:db/migration")
            .load()
        flyway.migrate()
    }
}
package com.example.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database

fun Application.configureDatabase() {
    val config = environment.config

    // Берем настройки из system properties для тестов, затем env или конфига
    val dbDriver = System.getProperty("DB_DRIVER") ?: System.getenv("DB_DRIVER") ?: config.propertyOrNull("database.driver")?.getString() ?: "org.postgresql.Driver"
    val dbUrl = System.getProperty("DB_URL") ?: System.getenv("DB_URL") ?: config.propertyOrNull("database.url")?.getString() ?: "jdbc:postgresql://localhost:5433/shop_db"
    val dbUser = System.getProperty("DB_USER") ?: System.getenv("DB_USER") ?: config.propertyOrNull("database.user")?.getString() ?: "postgres"
    val dbPassword = System.getProperty("DB_PASSWORD") ?: System.getenv("DB_PASSWORD") ?: config.propertyOrNull("database.password")?.getString() ?: "postgres"
    val maxPoolSize = System.getProperty("DB_MAX_POOL_SIZE")?.toIntOrNull() ?: System.getenv("DB_MAX_POOL_SIZE")?.toIntOrNull() ?: config.propertyOrNull("database.maxPoolSize")?.getString()?.toIntOrNull() ?: 10

    log.info("Database configuration: driver=$dbDriver, url=$dbUrl, user=$dbUser, maxPoolSize=$maxPoolSize")

    val dbConfig = HikariConfig().apply {
        driverClassName = dbDriver
        jdbcUrl = dbUrl
        username = dbUser
        password = dbPassword
        maximumPoolSize = maxPoolSize
        isAutoCommit = false
        transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        validate()
    }

    val dataSource = HikariDataSource(dbConfig)
    Database.connect(dataSource)

    // Применяем миграции
    log.info("Running Flyway migrations...")
    val flyway = Flyway.configure()
        .dataSource(dataSource)
        .locations("classpath:db/migration")
        .baselineOnMigrate(true)
        .validateOnMigrate(false)
        .sqlMigrationPrefix("V")
        .sqlMigrationSeparator("__")
        .sqlMigrationSuffixes(".sql")
        .cleanDisabled(false)
        .load()

    try {
        val migrationsApplied = flyway.migrate()
        log.info("Flyway migrations completed successfully. Applied ${migrationsApplied.migrationsExecuted} migration(s)")
    } catch (e: Exception) {
        log.error("Flyway migration failed: ${e.message}", e)
        throw e
    }
}
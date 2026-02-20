package com.example.config

import io.ktor.server.application.*
import com.example.data.repository.UserRepository
import com.example.module.UserRole
import org.jetbrains.exposed.sql.transactions.transaction

// Создает дефолтного админа при первом запуске
fun Application.initializeData() {
    val config = environment.config

    // Читаем настройки админа
    val adminEmail = System.getenv("ADMIN_EMAIL")
        ?: config.propertyOrNull("admin.email")?.getString()
        ?: "admin@shop.com"

    val adminPassword = System.getenv("ADMIN_PASSWORD")
        ?: config.propertyOrNull("admin.password")?.getString()
        ?: "admin123"

    val userRepository = UserRepository()

    transaction {
        // Проверяем есть ли уже админ
        val existingAdmin = userRepository.findByEmail(adminEmail)

        if (existingAdmin == null) {
            // Создаем админа
            val admin = userRepository.create(
                email = adminEmail,
                password = adminPassword,
                role = UserRole.ADMIN
            )

            if (admin != null) {
                log.info("Default admin user created successfully:")
                log.info("Email: $adminEmail")
                log.info("Password: $adminPassword")
            } else {
                log.error("Failed to create default admin user")
            }
        } else {
            log.info("Admin user already exists (${existingAdmin.email})")
        }
    }
}
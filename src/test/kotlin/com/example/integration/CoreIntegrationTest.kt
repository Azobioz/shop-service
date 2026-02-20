package com.example.integration

import org.junit.jupiter.api.BeforeAll

// Базовый класс для интеграционных тестов
abstract class CoreIntegrationTest {

    companion object {
        @JvmStatic
        @BeforeAll
        fun configureTestcontainers() {
            // Настройки для Docker Desktop на винде
            System.setProperty("testcontainers.reuse.enable", "false")
            System.setProperty("testcontainers.ryuk.disabled", "false")

            println("Docker environment configured for TestContainers")
        }
    }
}
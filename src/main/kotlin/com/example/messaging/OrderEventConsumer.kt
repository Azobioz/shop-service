package com.example.messaging

import com.example.config.KafkaConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.errors.WakeupException
import org.slf4j.LoggerFactory
import java.time.Duration

object OrderEventConsumer {
    private val logger = LoggerFactory.getLogger(OrderEventConsumer::class.java)
    private val json = Json

    suspend fun consume(consumer: KafkaConsumer<String, String>) {
        consumer.subscribe(listOf(KafkaConfig.fromEnvironment().topic))
        try {
            while (true) {
                val records: ConsumerRecords<String, String> = consumer.poll(Duration.ofMillis(1000))
                records.forEach { record ->
                    processMessage(record.value())
                }
                delay(100)
            }
        } catch (e: WakeupException) {
        } finally {
            consumer.close()
        }
    }

    private fun processMessage(value: String) {
        try {
            val event = json.decodeFromString<OrderCreatedEvent>(value)
            logger.info("Processing order event: orderId=${event.orderId}, userId=${event.userId}, total=${event.totalAmount}")
            sendFakeEmail(event)
        } catch (e: Exception) {
            logger.error("Failed to process message: $value", e)
        }
    }

    private fun sendFakeEmail(event: OrderCreatedEvent) {
        logger.info("Sending fake email to user ${event.userId} about order ${event.orderId}")
    }
}
package com.example.kafka


import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*

class KafkaConsumer(
    bootstrapServers: String,
    private val topic: String,
    private val groupId: String = "order-consumer-group"
) {

    private val logger = LoggerFactory.getLogger(KafkaConsumer::class.java)
    private val consumer: KafkaConsumer<String, String>

    init {
        val props = Properties().apply {
            put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
            put(ConsumerConfig.GROUP_ID_CONFIG, groupId)
            put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java.name)
            put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java.name)
            put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
        }
        consumer = KafkaConsumer(props)
        consumer.subscribe(listOf(topic))
    }

    suspend fun startConsuming() = withContext(Dispatchers.IO) {
        logger.info("Starting Kafka consumer for topic: $topic")

        while (isActive) {
            try {
                val records = consumer.poll(Duration.ofMillis(1000))

                for (record in records) {
                    try {
                        val event = Json.decodeFromString<OrderEvent>(record.value())
                        processEvent(event)
                    } catch (e: Exception) {
                        logger.error("Error processing message: ${e.message}", e)
                    }
                }
            } catch (e: Exception) {
                logger.error("Error polling messages: ${e.message}", e)
                delay(5000) // Ждем перед повторной попыткой
            }
        }
    }

    private fun processEvent(event: OrderEvent) {
        logger.info("Processing order event: ${event.eventType} for order ${event.orderId}")

        when (event.eventType) {
            "ORDER_CREATED" -> {
                logger.info("Order ${event.orderId} created by user ${event.userId}")
                // Отправка email-заглушки
                sendEmailNotification(event.userId, "Order Created", event.details)
            }
            "ORDER_CANCELLED" -> {
                logger.info("Order ${event.orderId} cancelled by user ${event.userId}")
                sendEmailNotification(event.userId, "Order Cancelled", event.details)
            }
            else -> {
                logger.warn("Unknown event type: ${event.eventType}")
            }
        }
    }

    private fun sendEmailNotification(userId: Long, subject: String, body: String) {
        logger.info("Sending email to user $userId: Subject='$subject', Body='$body'")
    }

    fun close() {
        consumer.close()
        logger.info("Kafka consumer closed")
    }
}
package com.example.messaging

import com.example.config.KafkaConfig
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.slf4j.LoggerFactory
import java.math.BigDecimal

@Serializable
data class OrderCreatedEvent(
    val orderId: Int,
    val userId: Int,
    @Contextual val totalAmount: BigDecimal,
    val timestamp: Long = System.currentTimeMillis()
)

class OrderEventProducer(
    private val producer: KafkaProducer<String, String>,
    private val config: KafkaConfig
) {
    private val logger = LoggerFactory.getLogger(OrderEventProducer::class.java)
    private val json = Json

    fun sendOrderCreated(orderId: Int, userId: Int, totalAmount: BigDecimal) {
        val event = OrderCreatedEvent(orderId, userId, totalAmount)
        val value = json.encodeToString(event)
        val record = ProducerRecord(config.topic, "order-$orderId", value)
        producer.send(record) { metadata: RecordMetadata?, exception: Exception? ->
            if (exception != null) {
                logger.error("Failed to send order created event for orderId=$orderId", exception)
            }
            else {
                logger.info("Order created event sent successfully for orderId=$orderId, topic=${metadata?.topic()}, partition=${metadata?.partition()}, offset=${metadata?.offset()}")
            }
        }
    }
}
package com.example.kafka

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import java.util.*

@Serializable
data class OrderEvent(
    val orderId: Long,
    val userId: Long,
    val eventType: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)

class KafkaProducer(bootstrapServers: String, private val topic: String) {

    private val producer: KafkaProducer<String, String>

    init {
        val props = Properties().apply {
            put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
            put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
            put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
            put(ProducerConfig.ACKS_CONFIG, "all")
        }
        producer = KafkaProducer(props)
    }

    fun sendOrderEvent(event: OrderEvent) {
        val json = Json.encodeToString(event)
        val record = ProducerRecord(topic, event.orderId.toString(), json)
        producer.send(record)
    }

    fun close() {
        producer.close()
    }
}
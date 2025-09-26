package com.virtualcode.fraud.infrastructure.kafka.config

import com.virtualcode.fraud.domain.model.transaction.ProcessedTransaction
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.support.serializer.JsonDeserializer

@Configuration
class KafkaConsumerConfig(private val objectMapper: ObjectMapper) {

    @Bean
    fun processedTransactionListenerContainerFactory(
        consumerFactory: ConsumerFactory<String, Any>
    ): ConcurrentKafkaListenerContainerFactory<String, ProcessedTransaction> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, ProcessedTransaction>()

        // Create new consumer properties from the original but use our custom deserializer
        val props = HashMap(consumerFactory.configurationProperties)

        // Configure the deserializer via properties only (not mixing with setters)
        props[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = JsonDeserializer::class.java
        props[JsonDeserializer.TRUSTED_PACKAGES] = "*"
        props[JsonDeserializer.TYPE_MAPPINGS] = "processedTransaction:com.bloomteq.fraud.domain.model.transaction.ProcessedTransaction"
        props[JsonDeserializer.VALUE_DEFAULT_TYPE] = "com.bloomteq.fraud.domain.model.transaction.ProcessedTransaction"

        // Create a custom consumer factory with the properties
        val kafkaConsumerFactory = DefaultKafkaConsumerFactory<String, ProcessedTransaction>(
            props,
            StringDeserializer(),
            JsonDeserializer<ProcessedTransaction>(objectMapper)
        )

        factory.consumerFactory = kafkaConsumerFactory
        return factory
    }
}
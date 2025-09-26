package com.bloomteq.fraud.infrastructure.kafka.config

import com.bloomteq.fraud.domain.model.transaction.ProcessedTransaction
import com.bloomteq.fraud.infrastructure.kafka.serialization.JsonSerde
import com.bloomteq.fraud.infrastructure.kafka.serialization.TransactionSerde
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsConfig
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafkaStreams
import org.springframework.kafka.config.KafkaStreamsConfiguration
import org.springframework.kafka.config.TopicBuilder
import org.springframework.kafka.core.KafkaAdmin
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.producer.ProducerConfig
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.serializer.JsonSerializer

@Configuration
@EnableKafkaStreams
class KafkaStreamsConfig {

    @Value("\${spring.kafka.bootstrap-servers:localhost:9092}")
    private lateinit var bootstrapServers: String

    @Value("\${spring.kafka.streams.application-id:fraud-detector}")
    private lateinit var applicationId: String

    @Bean(name = ["defaultKafkaStreamsConfig"])
    fun kafkaStreamsConfiguration(): KafkaStreamsConfiguration {
        val props = mapOf(
            StreamsConfig.APPLICATION_ID_CONFIG to applicationId,
            StreamsConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG to Serdes.String()::class.java,
            StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG to Serdes.String()::class.java,
            StreamsConfig.COMMIT_INTERVAL_MS_CONFIG to 1000,
            StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG to 0
        )
        return KafkaStreamsConfiguration(props)
    }

    @Bean
    fun kafkaProcessedTransactionTemplate(
        kafkaProducerFactory: DefaultKafkaProducerFactory<String, Any>,
        objectMapper: ObjectMapper
    ): KafkaTemplate<String, ProcessedTransaction> {
        val producerProps = HashMap(kafkaProducerFactory.configurationProperties)
        producerProps[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = JsonSerializer::class.java

        val processedTransactionFactory = DefaultKafkaProducerFactory<String, ProcessedTransaction>(producerProps)
        processedTransactionFactory.setValueSerializer(JsonSerializer<ProcessedTransaction>(objectMapper))

        return KafkaTemplate(processedTransactionFactory)
    }

    @Bean
    fun incomingTransactionsTopic(): NewTopic {
        return TopicBuilder.name(KafkaTopics.INCOMING_TRANSACTIONS)
            .partitions(1)  // Start with 1 partition for local development
            .replicas(1)    // Single replica for local development
            .build()
    }

    @Bean
    fun processedTransactionsTopic(): NewTopic {
        return TopicBuilder.name(KafkaTopics.PROCESSED_TRANSACTIONS)
            .partitions(1)
            .replicas(1)
            .build()
    }

    @Bean
    fun fraudAlertsTopic(): NewTopic {
        return TopicBuilder.name(KafkaTopics.FRAUD_ALERTS)
            .partitions(1)
            .replicas(1)
            .build()
    }

    @Bean
    fun transactionHistoryTopic(): NewTopic {
        return TopicBuilder.name(KafkaTopics.TRANSACTION_HISTORY)
            .partitions(1)
            .replicas(1)
            .build()
    }
}
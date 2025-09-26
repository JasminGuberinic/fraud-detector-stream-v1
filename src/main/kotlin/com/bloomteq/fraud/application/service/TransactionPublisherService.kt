package com.bloomteq.fraud.application.service

import com.bloomteq.fraud.domain.model.transaction.Transaction
import com.bloomteq.fraud.infrastructure.kafka.config.KafkaTopics
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.stereotype.Service
import java.util.UUID
import java.util.concurrent.CompletableFuture
import org.slf4j.LoggerFactory

@Service
class TransactionPublisherService(
    private val kafkaTemplate: KafkaTemplate<String, Any>
) {
    private val logger = LoggerFactory.getLogger(TransactionPublisherService::class.java)

    fun publishTransaction(transaction: Transaction): CompletableFuture<SendResult<String, Any>> {
        // Use non-null ID or generate one
        val key = transaction.id

        logger.info("Publishing transaction with ID: $key for user: ${transaction.userId}")

        return kafkaTemplate.send(KafkaTopics.INCOMING_TRANSACTIONS, key, transaction)
            .whenComplete { result, ex ->
                if (ex == null && result != null) {
                    logger.info("Successfully published transaction $key " +
                            "to topic ${result.recordMetadata.topic()}, " +
                            "partition ${result.recordMetadata.partition()}, " +
                            "offset ${result.recordMetadata.offset()}")
                } else {
                    logger.error("Failed to publish transaction $key", ex)
                }
            }
    }

    fun publishBatch(transactions: List<Transaction>): List<CompletableFuture<SendResult<String, Any>>> {
        logger.info("Publishing batch of ${transactions.size} transactions")
        return transactions.map { publishTransaction(it) }
    }
}
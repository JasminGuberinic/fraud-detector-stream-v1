package com.virtualcode.fraud.infrastructure.kafka.consumer

import com.virtualcode.fraud.domain.model.transaction.ProcessedTransaction
import com.virtualcode.fraud.infrastructure.kafka.config.KafkaTopics
import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.elasticsearch.core.IndexRequest
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class FraudAlertElasticsearchConsumer(
    private val elasticsearchClient: ElasticsearchClient
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val INDEX_NAME = "fraud-alerts"
    private var running = true

    @KafkaListener(
        topics = [KafkaTopics.FRAUD_ALERTS],
        groupId = "elasticsearch-fraud-alert-consumer", // Use a distinct consumer group
        containerFactory = "processedTransactionListenerContainerFactory"
    )
    fun consumeFraudAlert(processedTransaction: ProcessedTransaction) {
        logger.info("Indexing fraud alert for transaction ${processedTransaction.transaction.id} to Elasticsearch")
        try {
            val request = IndexRequest.Builder<Map<String, Any>>()
                .index(INDEX_NAME)
                .id(processedTransaction.transaction.id)
                .document(processedTransactionToMap(processedTransaction))
                .build()

            elasticsearchClient.index(request)
            logger.info("Successfully indexed fraud alert for transaction ${processedTransaction.transaction.id}")
        } catch (e: Exception) {
            logger.error("Failed to index fraud alert for transaction ${processedTransaction.transaction.id}", e)
        }
    }

    @PreDestroy
    fun shutdown() {
        logger.info("Shutting down Elasticsearch fraud alert consumer")
        running = false
        logger.info("Elasticsearch fraud alert consumer shutdown complete")
    }

    private fun processedTransactionToMap(processedTransaction: ProcessedTransaction): Map<String, Any> {
        // Convert ProcessedTransaction to a Map that Elasticsearch can handle
        val map = mutableMapOf<String, Any>()

        // Add top-level fields
        map["riskScore"] = processedTransaction.riskScore
        map["isFraudulent"] = processedTransaction.isFraudulent
        map["processedAt"] = processedTransaction.processedAt.toString()

        // Add rule results
        map["ruleResults"] = processedTransaction.ruleResults.map { rule ->
            mapOf(
                "ruleName" to rule.ruleName,
                "score" to rule.score,
                "triggered" to rule.triggered,
                "reason" to rule.reason
            )
        }

        // Add transaction details
        val transaction = processedTransaction.transaction
        val transactionMap = mutableMapOf<String, Any>()

        transactionMap["id"] = transaction.id
        transactionMap["userId"] = transaction.userId
        transactionMap["amount"] = transaction.amount
        transactionMap["currency"] = transaction.currency
        transactionMap["merchantId"] = transaction.merchantId
        transactionMap["timestamp"] = transaction.timestamp.toString()
        transactionMap["cardNumber"] = transaction.cardNumber
        transactionMap["transactionType"] = transaction.transactionType.name

        // Handle location
        transactionMap["location"] = mapOf(
            "country" to transaction.location.country,
            "city" to transaction.location.city,
            "latitude" to (transaction.location.latitude ?: 0.0),
            "longitude" to (transaction.location.longitude ?: 0.0),
            "timezone" to (transaction.location.timezone ?: "")
        )

        // Add optional fields if present
        transaction.ipAddress?.let { transactionMap["ipAddress"] = it }
        transaction.userAgent?.let { transactionMap["userAgent"] = it }
        transaction.sessionId?.let { transactionMap["sessionId"] = it }

        // Handle device info if present
        transaction.deviceInfo?.let { deviceInfo ->
            transactionMap["deviceInfo"] = mapOf(
                "deviceId" to deviceInfo.deviceId,
                "deviceType" to deviceInfo.deviceType.name,
                "operatingSystem" to (deviceInfo.operatingSystem ?: ""),
                "browserInfo" to (deviceInfo.browserInfo ?: ""),
                "screenResolution" to (deviceInfo.screenResolution ?: ""),
                "isMobile" to deviceInfo.isMobile
            )
        }

        map["transaction"] = transactionMap

        return map
    }
}
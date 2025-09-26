package com.bloomteq.fraud.infrastructure.kafka.consumer

import com.bloomteq.fraud.domain.model.transaction.ProcessedTransaction
import com.bloomteq.fraud.infrastructure.kafka.config.KafkaTopics
import com.bloomteq.fraud.infrastructure.service.FraudAlertService
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class FraudAlertConsumer(
    private val fraudAlertService: FraudAlertService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = [KafkaTopics.FRAUD_ALERTS],
        groupId = "fraud-alert-ui-consumer",
        containerFactory = "processedTransactionListenerContainerFactory"
    )
    fun consumeFraudAlert(processedTransaction: ProcessedTransaction) {
        logger.warn("⚠️ FRAUD ALERT: Transaction ${processedTransaction.transaction.id} for user ${processedTransaction.transaction.userId}")
        logger.info("Risk score: ${processedTransaction.riskScore}, Rules triggered: ${processedTransaction.ruleResults.filter { it.triggered }.map { it.ruleName }}")

        // Store the fraud alert in the service
        fraudAlertService.addFraudAlert(processedTransaction)
    }
}
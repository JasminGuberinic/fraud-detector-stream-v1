package com.virtualcode.fraud.infrastructure.kafka.config

object KafkaTopics {
    const val INCOMING_TRANSACTIONS = "incoming-transactions"
    const val PROCESSED_TRANSACTIONS = "processed-transactions"
    const val FRAUD_ALERTS = "fraud-alerts"
    const val TRANSACTION_HISTORY = "transaction-history"
}
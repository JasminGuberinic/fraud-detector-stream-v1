package com.virtualcode.fraud.infrastructure.persistence.entity

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "processed_transactions")
data class ProcessedTransactionEntity(
    @Id
    @Column(name = "transaction_id")
    val transactionId: String,

    @Column(name = "risk_score")
    val riskScore: Double,

    @Column(name = "is_fraudulent")
    val isFraudulent: Boolean,

    @Column(name = "rule_results", columnDefinition = "TEXT")
    val ruleResults: String, // JSON string

    @Column(name = "processed_at")
    val processedAt: Instant = Instant.now()
)
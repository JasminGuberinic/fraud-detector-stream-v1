package com.bloomteq.fraud.domain.model.transaction

import java.time.Instant

data class ProcessedTransaction(
    val transaction: Transaction,
    val riskScore: Double,
    val isFraudulent: Boolean,
    val ruleResults: List<RuleResult>,
    val processedAt: Instant = Instant.now(),
    val mlRiskScore: Double? = null
)

data class RuleResult(
    val ruleName: String,
    val score: Double,
    val triggered: Boolean,
    val reason: String
)
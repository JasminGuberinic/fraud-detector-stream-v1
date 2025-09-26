package com.bloomteq.fraud.infrastructure.rules.impl

import com.bloomteq.fraud.domain.model.transaction.RuleResult
import com.bloomteq.fraud.domain.model.transaction.Transaction
import com.bloomteq.fraud.domain.service.rules.FraudRule
import org.springframework.stereotype.Component
import java.time.temporal.ChronoUnit

@Component
class LocationRule : FraudRule {
    override val name = "LOCATION_RULE"
    override val weight = 0.3

    override fun evaluate(transaction: Transaction, userHistory: List<Transaction>): RuleResult {
        val recentLocations = userHistory.filter {
            it.timestamp.isAfter(transaction.timestamp.minus(2, ChronoUnit.HOURS))
        }.map { it.location.country }.distinct()

        val commonCountries = userHistory.map { it.location.country }.distinct()

        return when {
            !commonCountries.contains(transaction.location.country) && commonCountries.isNotEmpty() -> RuleResult(
                ruleName = name,
                score = 0.7,
                triggered = true,
                reason = "Transaction from new country: ${transaction.location.country}"
            )
            recentLocations.isNotEmpty() && !recentLocations.contains(transaction.location.country) -> RuleResult(
                ruleName = name,
                score = 0.9,
                triggered = true,
                reason = "Transaction from different country within 2 hours"
            )
            else -> RuleResult(
                ruleName = name,
                score = 0.1,
                triggered = false,
                reason = "Normal location"
            )
        }
    }
}
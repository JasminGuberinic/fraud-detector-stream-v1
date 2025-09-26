package com.virtualcode.fraud.infrastructure.rules.impl

import com.virtualcode.fraud.domain.model.profile.UserBehaviorProfile
import com.virtualcode.fraud.domain.model.transaction.RuleResult
import com.virtualcode.fraud.domain.model.transaction.Transaction
import com.virtualcode.fraud.domain.service.rules.BehaviorRule
import org.springframework.stereotype.Component
import java.math.BigDecimal
import kotlin.math.abs

@Component
class UnusualAmountRule : BehaviorRule {
    override val name = "UNUSUAL_AMOUNT_RULE"
    override val weight = 0.35

    override fun evaluate(transaction: Transaction, behaviorProfile: UserBehaviorProfile): RuleResult {
        val averageAmount = behaviorProfile.averageTransactionAmount
        val typicalAmounts = behaviorProfile.typicalTransactionAmounts

        if (averageAmount == BigDecimal.ZERO || typicalAmounts.isEmpty()) {
            return RuleResult(name, 0.2, false, "Insufficient transaction history for amount analysis")
        }

        val deviation = calculateDeviation(transaction.amount, typicalAmounts)
        val ratioToAverage = transaction.amount.divide(averageAmount, 2, BigDecimal.ROUND_HALF_UP).toDouble()

        return when {
            deviation > 5.0 && ratioToAverage > 10 -> RuleResult(
                ruleName = name,
                score = 0.9,
                triggered = true,
                reason = "Extremely unusual amount: ${transaction.amount} (${ratioToAverage}x average)"
            )
            deviation > 3.0 && ratioToAverage > 5 -> RuleResult(
                ruleName = name,
                score = 0.6,
                triggered = true,
                reason = "Unusual amount detected: ${transaction.amount}"
            )
            ratioToAverage > 3 -> RuleResult(
                ruleName = name,
                score = 0.4,
                triggered = false,
                reason = "Higher than typical amount"
            )
            else -> RuleResult(
                ruleName = name,
                score = 0.1,
                triggered = false,
                reason = "Amount within normal range"
            )
        }
    }

    private fun calculateDeviation(amount: BigDecimal, typicalAmounts: List<BigDecimal>): Double {
        val average = typicalAmounts.map { it.toDouble() }.average()
        val variance = typicalAmounts.map { (it.toDouble() - average) * (it.toDouble() - average) }.average()
        val standardDeviation = kotlin.math.sqrt(variance)
        return abs(amount.toDouble() - average) / standardDeviation
    }
}
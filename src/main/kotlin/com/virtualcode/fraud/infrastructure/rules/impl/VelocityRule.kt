package com.virtualcode.fraud.infrastructure.rules.impl

import com.virtualcode.fraud.domain.model.profile.UserVelocityProfile
import com.virtualcode.fraud.domain.model.transaction.RuleResult
import com.virtualcode.fraud.domain.model.transaction.Transaction
import com.virtualcode.fraud.domain.service.rules.VelocityRule
import org.springframework.stereotype.Component

@Component
class VelocityRules : VelocityRule {
    override val name = "VELOCITY_RULE"
    override val weight = 0.3

    override fun evaluate(transaction: Transaction, velocityProfile: UserVelocityProfile): RuleResult {
        // Use transactionCount from the profile instead of recentTransactions
        val recentTransactionsCount = velocityProfile.transactionCount

        return when {
            recentTransactionsCount > 5 -> RuleResult(
                ruleName = name,
                score = 0.9,
                triggered = true,
                reason = "Too many transactions: $recentTransactionsCount"
            )
            recentTransactionsCount > 3 -> RuleResult(
                ruleName = name,
                score = 0.5,
                triggered = false,
                reason = "Moderate transaction velocity"
            )
            else -> RuleResult(
                ruleName = name,
                score = 0.1,
                triggered = false,
                reason = "Normal velocity"
            )
        }
    }
}
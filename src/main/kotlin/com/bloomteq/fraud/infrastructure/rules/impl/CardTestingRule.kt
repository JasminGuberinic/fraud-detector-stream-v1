package com.bloomteq.fraud.infrastructure.rules.impl

import com.bloomteq.fraud.domain.model.profile.UserVelocityProfile
import com.bloomteq.fraud.domain.model.transaction.RuleResult
import com.bloomteq.fraud.domain.model.transaction.Transaction
import com.bloomteq.fraud.domain.service.rules.FraudRule
import com.bloomteq.fraud.domain.service.rules.VelocityRule
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class CardTestingRule : VelocityRule {
    override val name = "CARD_TESTING_RULE"
    override val weight = 0.4

    override fun evaluate(transaction: Transaction, velocityProfile: UserVelocityProfile): RuleResult {
        // Card testing pattern: many small transactions at different merchants
        val smallAmountThreshold = transaction.amount.multiply(java.math.BigDecimal("0.1")) // 10% of current amount

        // Use uniqueMerchants from the profile instead of recentMerchants
        val uniqueMerchants = velocityProfile.uniqueMerchants.size

        // Look at transaction count and merchant variety instead of detailed transaction info
        val highVelocity = velocityProfile.transactionCount > 5
        val manyMerchants = uniqueMerchants >= 3
        val smallAverageAmount = velocityProfile.transactionCount > 0 &&
                velocityProfile.totalAmount.divide(
                    BigDecimal(velocityProfile.transactionCount),
                    java.math.RoundingMode.HALF_UP) <= smallAmountThreshold

        return when {
            highVelocity && manyMerchants && smallAverageAmount -> RuleResult(
                ruleName = name,
                score = 0.9,
                triggered = true,
                reason = "Card testing detected: ${velocityProfile.transactionCount} transactions across $uniqueMerchants merchants"
            )
            manyMerchants && velocityProfile.transactionCount > 3 -> RuleResult(
                ruleName = name,
                score = 0.6,
                triggered = true,
                reason = "Potential card testing: ${velocityProfile.transactionCount} transactions across $uniqueMerchants merchants"
            )
            else -> RuleResult(
                ruleName = name,
                score = 0.1,
                triggered = false,
                reason = "Normal merchant pattern"
            )
        }
    }
}
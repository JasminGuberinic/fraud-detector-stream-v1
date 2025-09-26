package com.virtualcode.fraud.infrastructure.rules.impl

import com.virtualcode.fraud.domain.model.profile.UserVelocityProfile
import com.virtualcode.fraud.domain.model.transaction.RuleResult
import com.virtualcode.fraud.domain.model.transaction.Transaction
import com.virtualcode.fraud.domain.service.rules.VelocityRule
import org.springframework.stereotype.Component

@Component
class RoboticPatternRule : VelocityRule {
    override val name = "ROBOTIC_PATTERN_RULE"
    override val weight = 0.35

    override fun evaluate(transaction: Transaction, velocityProfile: UserVelocityProfile): RuleResult {
        // Check if we have enough transactions to detect a pattern
        if (velocityProfile.transactionCount < 3) {
            return RuleResult(name, 0.1, false, "Insufficient data for pattern analysis")
        }

        // Use averageTimeBetween for consistency analysis
        val avgTimeBetween = velocityProfile.averageTimeBetween

        // If the average time between transactions is very consistent (low standard deviation),
        // that's a robotic pattern
        val isVeryConsistent = avgTimeBetween != null &&
                avgTimeBetween.toMinutes() > 0 &&
                avgTimeBetween.toMinutes() < 5 &&
                velocityProfile.transactionCount >= 4

        // Calculate merchant variety ratio (lower = more suspicious)
        val merchantVarietyRatio = if (velocityProfile.transactionCount > 0) {
            velocityProfile.uniqueMerchants.size.toDouble() / velocityProfile.transactionCount.toDouble()
        } else 1.0

        return when {
            isVeryConsistent && merchantVarietyRatio < 0.3 -> RuleResult(
                ruleName = name,
                score = 0.8,
                triggered = true,
                reason = "Highly regular timing pattern detected with repeated merchants"
            )
            isVeryConsistent -> RuleResult(
                ruleName = name,
                score = 0.7,
                triggered = true,
                reason = "Robotic pattern: highly regular timing"
            )
            velocityProfile.transactionCount > 5 && merchantVarietyRatio < 0.3 -> RuleResult(
                ruleName = name,
                score = 0.5,
                triggered = false,
                reason = "Potential bot behavior: repeated merchants"
            )
            else -> RuleResult(
                ruleName = name,
                score = 0.1,
                triggered = false,
                reason = "Human-like irregular pattern"
            )
        }
    }
}
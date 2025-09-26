package com.virtualcode.fraud.infrastructure.rules.impl

import com.virtualcode.fraud.domain.model.profile.UserBehaviorProfile
import com.virtualcode.fraud.domain.model.transaction.RuleResult
import com.virtualcode.fraud.domain.model.transaction.Transaction
import com.virtualcode.fraud.domain.service.rules.BehaviorRule
import org.springframework.stereotype.Component
import java.time.LocalTime
import java.time.ZoneOffset

@Component
class UnusualTimeRule : BehaviorRule {
    override val name = "UNUSUAL_TIME_RULE"
    override val weight = 0.4

    override fun evaluate(transaction: Transaction, behaviorProfile: UserBehaviorProfile): RuleResult {
        val transactionTime = transaction.timestamp.atZone(ZoneOffset.UTC).toLocalTime()
        val isUnusualTime = isUnusualTime(transactionTime, behaviorProfile.preferredTimeRanges)

        return when {
            isUnusualTime && behaviorProfile.preferredTimeRanges.isNotEmpty() -> RuleResult(
                ruleName = name,
                score = 0.7,
                triggered = true,
                reason = "Transaction at unusual time: $transactionTime (outside normal patterns)"
            )
            behaviorProfile.preferredTimeRanges.isEmpty() -> RuleResult(
                ruleName = name,
                score = 0.2,
                triggered = false,
                reason = "No historical time pattern available"
            )
            else -> RuleResult(
                ruleName = name,
                score = 0.1,
                triggered = false,
                reason = "Transaction within normal time pattern"
            )
        }
    }

    private fun isUnusualTime(transactionTime: LocalTime, timeRanges: Set<UserBehaviorProfile.TimeRange>): Boolean {
        return timeRanges.none { range ->
            transactionTime.isAfter(range.startTime) && transactionTime.isBefore(range.endTime)
        }
    }
}
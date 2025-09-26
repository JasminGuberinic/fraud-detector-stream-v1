package com.virtualcode.fraud.infrastructure.rules.impl

import com.virtualcode.fraud.domain.model.profile.UserDeviceProfile
import com.virtualcode.fraud.domain.model.transaction.RuleResult
import com.virtualcode.fraud.domain.model.transaction.Transaction
import com.virtualcode.fraud.domain.service.rules.DeviceRule
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class DeviceSwitchingRule : DeviceRule {
    override val name = "DEVICE_SWITCHING_RULE"
    override val weight = 0.3

    override fun evaluate(transaction: Transaction, deviceProfile: UserDeviceProfile): RuleResult {
        val currentDeviceId = extractDeviceId(transaction)
        val lastDevice = deviceProfile.lastDeviceUsed
        val switchCount = deviceProfile.deviceSwitchCount

        return when {
            lastDevice != null && lastDevice.deviceId != currentDeviceId &&
                    Duration.between(lastDevice.lastSeen, transaction.timestamp).toMinutes() < 30 -> RuleResult(
                ruleName = name,
                score = 0.8,
                triggered = true,
                reason = "Rapid device switching: from ${lastDevice.deviceId} to $currentDeviceId within 30 minutes"
            )
            switchCount > 10 && Duration.between(deviceProfile.lastDeviceUpdate, transaction.timestamp).toHours() < 24 -> RuleResult(
                ruleName = name,
                score = 0.6,
                triggered = true,
                reason = "Excessive device switching: $switchCount switches in 24 hours"
            )
            switchCount > 5 -> RuleResult(
                ruleName = name,
                score = 0.4,
                triggered = false,
                reason = "Moderate device switching pattern"
            )
            else -> RuleResult(
                ruleName = name,
                score = 0.1,
                triggered = false,
                reason = "Normal device usage pattern"
            )
        }
    }

    private fun extractDeviceId(transaction: Transaction): String {
        return "${transaction.cardNumber.takeLast(4)}_${transaction.location.country}"
    }
}
package com.virtualcode.fraud.infrastructure.rules.impl

import com.virtualcode.fraud.domain.model.profile.UserDeviceProfile
import com.virtualcode.fraud.domain.model.transaction.RuleResult
import com.virtualcode.fraud.domain.model.transaction.Transaction
import com.virtualcode.fraud.domain.service.rules.DeviceRule
import org.springframework.stereotype.Component

@Component
class NewDeviceRule : DeviceRule {
    override val name = "NEW_DEVICE_RULE"
    override val weight = 0.4

    override fun evaluate(transaction: Transaction, deviceProfile: UserDeviceProfile): RuleResult {
        val transactionDeviceId = extractDeviceId(transaction)
        val isKnownDevice = deviceProfile.knownDevices.any { it.deviceId == transactionDeviceId }
        val isSuspiciousDevice = deviceProfile.suspiciousDevices.any { it.deviceId == transactionDeviceId }

        return when {
            isSuspiciousDevice -> RuleResult(
                ruleName = name,
                score = 0.9,
                triggered = true,
                reason = "Transaction from previously flagged suspicious device"
            )
            !isKnownDevice && deviceProfile.knownDevices.isNotEmpty() -> RuleResult(
                ruleName = name,
                score = 0.7,
                triggered = true,
                reason = "Transaction from new/unknown device: $transactionDeviceId"
            )
            !isKnownDevice -> RuleResult(
                ruleName = name,
                score = 0.3,
                triggered = false,
                reason = "First device for user"
            )
            else -> RuleResult(
                ruleName = name,
                score = 0.1,
                triggered = false,
                reason = "Transaction from known device"
            )
        }
    }

    private fun extractDeviceId(transaction: Transaction): String {
        // Simplified device ID extraction - in reality would be more complex
        return "${transaction.cardNumber.takeLast(4)}_${transaction.location.country}"
    }
}
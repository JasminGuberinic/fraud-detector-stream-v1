package com.bloomteq.fraud.infrastructure.rules.impl

import com.bloomteq.fraud.domain.model.profile.UserDeviceProfile
import com.bloomteq.fraud.domain.model.transaction.DeviceType
import com.bloomteq.fraud.domain.model.transaction.RuleResult
import com.bloomteq.fraud.domain.model.transaction.Transaction
import com.bloomteq.fraud.domain.service.rules.DeviceRule
import org.springframework.stereotype.Component

@Component
class SuspiciousDeviceRule : DeviceRule {
    override val name = "SUSPICIOUS_DEVICE_RULE"
    override val weight = 0.4

    override fun evaluate(transaction: Transaction, deviceProfile: UserDeviceProfile): RuleResult {
        val deviceInfo = transaction.deviceInfo
        val userPreferences = deviceProfile.preferredDeviceTypes

        return when {
            deviceInfo == null -> RuleResult(
                ruleName = name,
                score = 0.3,
                triggered = false,
                reason = "No device information available"
            )

            deviceInfo.deviceType == DeviceType.UNKNOWN && deviceProfile.knownDevices.isNotEmpty() -> RuleResult(
                ruleName = name,
                score = 0.6,
                triggered = true,
                reason = "Transaction from unknown device type"
            )

            isUnusualDeviceType(deviceInfo.deviceType, userPreferences) -> RuleResult(
                ruleName = name,
                score = 0.5,
                triggered = false,
                reason = "Transaction from unusual device type: ${deviceInfo.deviceType}"
            )

            else -> RuleResult(
                ruleName = name,
                score = 0.1,
                triggered = false,
                reason = "Normal device usage"
            )
        }
    }

    private fun isUnusualDeviceType(deviceType: DeviceType?, preferences: Map<DeviceType, Int>): Boolean {
        if (deviceType == null || preferences.isEmpty()) return false

        val totalTransactions = preferences.values.sum()
        val deviceTypeUsage = preferences[deviceType] ?: 0
        val usagePercentage = deviceTypeUsage.toDouble() / totalTransactions

        return usagePercentage < 0.1 // Less than 10% usage
    }
}
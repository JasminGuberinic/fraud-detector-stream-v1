package com.virtualcode.fraud.domain.model.profile

import com.virtualcode.fraud.domain.model.transaction.DeviceType
import java.time.Instant

data class UserDeviceProfile(
    val knownDevices: Set<DeviceInfo> = emptySet(),
    val suspiciousDevices: Set<DeviceInfo> = emptySet(),
    val deviceSwitchCount: Int = 0,
    val lastDeviceUsed: DeviceInfo? = null,
    val lastDeviceUpdate: Instant? = null,
    val preferredDeviceTypes: Map<DeviceType, Int> = emptyMap()
) {
    data class DeviceInfo(
        val deviceId: String,
        val userAgent: String? = null,
        val ipAddress: String? = null,
        val firstSeen: Instant,
        val lastSeen: Instant,
        val transactionCount: Int = 1,
        val deviceType: DeviceType? = null,
        val operatingSystem: String? = null,
        val isMobile: Boolean = false
    )
}
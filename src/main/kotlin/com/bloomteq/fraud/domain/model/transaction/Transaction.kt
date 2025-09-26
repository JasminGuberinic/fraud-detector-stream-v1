package com.bloomteq.fraud.domain.model.transaction

import java.math.BigDecimal
import java.time.Instant

data class Transaction(
    val id: String,
    val userId: String,
    val amount: BigDecimal,
    val currency: String,
    val merchantId: String,
    val location: Location,
    val timestamp: Instant,
    val cardNumber: String,
    val transactionType: TransactionType = TransactionType.PURCHASE,
    // NOVI PODACI ZA GEO I DEVICE ANALYSIS
    val deviceInfo: DeviceInfo? = null,
    val ipAddress: String? = null,
    val userAgent: String? = null,
    val sessionId: String? = null
) {
    fun isWithinTimeWindow(other: Transaction, windowMinutes: Long): Boolean {
        return timestamp.isAfter(other.timestamp.minusSeconds(windowMinutes * 60))
    }

    fun hasSameLocation(other: Transaction): Boolean {
        return location.country == other.location.country && location.city == other.location.city
    }
}

data class Location(
    val country: String,
    val city: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val timezone: String? = null
)

data class DeviceInfo(
    val deviceId: String,
    val deviceType: DeviceType = DeviceType.UNKNOWN,
    val operatingSystem: String? = null,
    val browserInfo: String? = null,
    val screenResolution: String? = null,
    val isMobile: Boolean = false
)

enum class DeviceType {
    MOBILE, DESKTOP, TABLET, ATM, POS_TERMINAL, UNKNOWN
}

enum class TransactionType {
    PURCHASE, WITHDRAWAL, TRANSFER, REFUND
}
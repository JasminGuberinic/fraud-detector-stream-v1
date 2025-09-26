package com.virtualcode.fraud.web.dto

import com.virtualcode.fraud.domain.model.transaction.DeviceInfo
import com.virtualcode.fraud.domain.model.transaction.DeviceType
import com.virtualcode.fraud.domain.model.transaction.Location
import com.virtualcode.fraud.domain.model.transaction.Transaction
import com.virtualcode.fraud.domain.model.transaction.TransactionType
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class TransactionRequest(
    val userId: String,
    val amount: BigDecimal,
    val currency: String,
    val merchantId: String,
    // Location fields as individual properties
    val country: String,
    val city: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val timezone: String? = null,
    val cardNumber: String,
    val transactionType: String = "PURCHASE",
    val deviceId: String? = null,
    val deviceType: String? = null,
    val operatingSystem: String? = null,
    val browserInfo: String? = null,
    val screenResolution: String? = null,
    val isMobile: Boolean? = null,
    val ipAddress: String? = null,
    val userAgent: String? = null,
    val sessionId: String? = null
) {
    fun toTransaction(): Transaction {
        val id = UUID.randomUUID().toString()
        return Transaction(
            id = id,
            userId = userId,
            amount = amount,
            currency = currency,
            merchantId = merchantId,
            location = Location(
                country = country,
                city = city,
                latitude = latitude,
                longitude = longitude,
                timezone = timezone
            ),
            timestamp = Instant.now(),
            cardNumber = cardNumber,
            transactionType = TransactionType.valueOf(transactionType),
            deviceInfo = if (deviceId != null) {
                DeviceInfo(
                    deviceId = deviceId,
                    deviceType = deviceType?.let { DeviceType.valueOf(it) } ?: DeviceType.UNKNOWN,
                    operatingSystem = operatingSystem,
                    browserInfo = browserInfo,
                    screenResolution = screenResolution,
                    isMobile = isMobile ?: false
                )
            } else null,
            ipAddress = ipAddress,
            userAgent = userAgent,
            sessionId = sessionId
        )
    }
}
package com.bloomteq.fraud.infrastructure.persistence.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant

@Entity
@Table(name = "transactions")
data class TransactionEntity(
    @Id
    val id: String,

    @Column(name = "user_id")
    val userId: String,

    @Column(precision = 19, scale = 4)
    val amount: BigDecimal,

    val currency: String,

    @Column(name = "merchant_id")
    val merchantId: String,

    val country: String,
    val city: String,

    val timestamp: Instant,

    @Column(name = "card_number")
    val cardNumber: String,

    @Column(name = "transaction_type")
    val transactionType: String,

    @Column(name = "device_id")
    val deviceId: String? = null,

    @Column(name = "ip_address")
    val ipAddress: String? = null
)
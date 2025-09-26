package com.virtualcode.fraud.domain.model.profile

import java.math.BigDecimal
import java.time.Duration
import java.time.Instant

data class UserVelocityProfile(
    // CORE VELOCITY FIELDS
    val transactionCount: Int = 0,
    val totalAmount: BigDecimal = BigDecimal.ZERO,
    val lastTransactionTime: Instant? = null,

    // VELOCITY-SPECIFIC PATTERNS
    val uniqueMerchants: Set<String> = emptySet(),      // Card testing - isti user, različiti merchantovi
    val failedAttempts: Int = 0,                        // Brzu neuspešni pokušaju = bot
    val averageTimeBetween: Duration? = null,           // Robotsko ponašanje ima pattern

    // REMOVE THESE - ne pripadaju velocity
    // val uniqueCountries: Set<String> = emptySet(),   // Ovo je GEO rule!
    // val uniqueCities: Set<String> = emptySet()       // Ovo je GEO rule!
)
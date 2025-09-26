package com.virtualcode.fraud.domain.model.profile

import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime

data class UserBehaviorProfile(
    val preferredTimeRanges: Set<TimeRange> = emptySet(),
    val frequentDaysOfWeek: Map<DayOfWeek, Int> = emptyMap(),
    val typicalTransactionAmounts: List<BigDecimal> = emptyList(),
    val merchantCategories: Map<String, Int> = emptyMap(),
    val averageTransactionAmount: BigDecimal = BigDecimal.ZERO,
    val deviceFingerprints: Set<String> = emptySet(),
    val lastBehaviorUpdate: Instant? = null
) {
    data class TimeRange(
        val startTime: LocalTime,
        val endTime: LocalTime
    ) {
        fun contains(time: LocalTime): Boolean {
            return time.isAfter(startTime) && time.isBefore(endTime)
        }
    }
}
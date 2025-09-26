package com.virtualcode.fraud.infrastructure.rules.impl

import com.virtualcode.fraud.domain.model.profile.UserGeoProfile
import com.virtualcode.fraud.domain.model.transaction.RuleResult
import com.virtualcode.fraud.domain.model.transaction.Transaction
import com.virtualcode.fraud.domain.service.rules.GeoRule
import org.springframework.stereotype.Component
import java.time.Duration
import kotlin.math.*

@Component
class ImpossibleTravelRule: GeoRule {
    override val name = "IMPOSSIBLE_TRAVEL_RULE"
    override val weight = 0.4

    override fun evaluate(transaction: Transaction, geoProfile: UserGeoProfile): RuleResult {
        val lastLocation = geoProfile.lastLocation
            ?: return RuleResult("IMPOSSIBLE_TRAVEL_RULE", 0.1, false, "No previous location data")

        if (lastLocation.latitude == null || lastLocation.longitude == null ||
            transaction.location.latitude == null || transaction.location.longitude == null) {
            return RuleResult("IMPOSSIBLE_TRAVEL_RULE", 0.2, false, "Missing GPS coordinates")
        }

        val distance = calculateDistance(
            lastLocation.latitude, lastLocation.longitude,
            transaction.location.latitude, transaction.location.longitude
        )

        val timeDiff = Duration.between(lastLocation.lastSeen, transaction.timestamp)
        val hoursDiff = timeDiff.toMinutes() / 60.0

        if (hoursDiff <= 0) {
            return RuleResult("IMPOSSIBLE_TRAVEL_RULE", 0.1, false, "Invalid time sequence")
        }

        val speed = distance / hoursDiff // km/h

        return when {
            speed > 900 -> RuleResult(
                ruleName = "IMPOSSIBLE_TRAVEL_RULE",
                score = 0.95,
                triggered = true,
                reason = "Impossible travel: ${distance.toInt()}km in ${hoursDiff.toInt()}h (${speed.toInt()}km/h)"
            )
            speed > 500 -> RuleResult(
                ruleName = "IMPOSSIBLE_TRAVEL_RULE",
                score = 0.7,
                triggered = true,
                reason = "Very high speed travel: ${speed.toInt()}km/h"
            )
            speed > 200 -> RuleResult(
                ruleName = "IMPOSSIBLE_TRAVEL_RULE",
                score = 0.4,
                triggered = false,
                reason = "Fast travel detected: ${speed.toInt()}km/h"
            )
            else -> RuleResult(
                ruleName = "IMPOSSIBLE_TRAVEL_RULE",
                score = 0.1,
                triggered = false,
                reason = "Normal travel speed"
            )
        }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371.0 // Earth radius in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c
    }
}
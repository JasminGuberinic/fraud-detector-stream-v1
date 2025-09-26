package com.bloomteq.fraud.infrastructure.rules.impl

import com.bloomteq.fraud.domain.model.profile.UserGeoProfile
import com.bloomteq.fraud.domain.model.transaction.RuleResult
import com.bloomteq.fraud.domain.model.transaction.Transaction
import com.bloomteq.fraud.domain.service.rules.FraudRule
import com.bloomteq.fraud.domain.service.rules.GeoRule
import org.springframework.stereotype.Component

@Component
class NewLocationRule : GeoRule  {
    override val name = "NEW_LOCATION_RULE"
    override val weight = 0.3

    override fun evaluate(transaction: Transaction, geoProfile: UserGeoProfile): RuleResult {
        val currentCountry = transaction.location.country
        val frequentCountries = geoProfile.travelPattern.keys

        return when {
            frequentCountries.isEmpty() -> RuleResult(
                ruleName = "NEW_LOCATION_RULE",
                score = 0.3,
                triggered = false,
                reason = "First transaction - no location history"
            )
            !frequentCountries.contains(currentCountry) -> RuleResult(
                ruleName = "NEW_LOCATION_RULE",
                score = 0.6,
                triggered = true,
                reason = "Transaction from new country: $currentCountry"
            )
            !geoProfile.knownLocations.any { it.country == currentCountry } -> RuleResult(
                ruleName = "NEW_LOCATION_RULE",
                score = 0.4,
                triggered = false,
                reason = "Return to known country after absence"
            )
            !geoProfile.knownLocations.any { it.country == currentCountry && it.lastSeen.isAfter(
                transaction.timestamp.minusMillis(300000)) } -> RuleResult(
                ruleName = "NEW_LOCATION_RULE",
                score = 0.4,
                triggered = false,
                reason = "Return to known country after absence"
            )
            else -> RuleResult(
                ruleName = "NEW_LOCATION_RULE",
                score = 0.1,
                triggered = false,
                reason = "Transaction from familiar location"
            )
        }
    }
}
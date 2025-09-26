package com.virtualcode.fraud.infrastructure.rules.impl

import com.virtualcode.fraud.domain.model.profile.UserGeoProfile
import com.virtualcode.fraud.domain.model.transaction.RuleResult
import com.virtualcode.fraud.domain.model.transaction.Transaction
import com.virtualcode.fraud.domain.service.rules.GeoRule
import org.springframework.stereotype.Component

@Component
class HighRiskCountryRule: GeoRule {
    override val name = "HIGH_RISK_COUNTRY_RULE"
    override val weight = 0.35

    private val highRiskCountries = setOf(
        "AF", "BD", "BF", "BI", "CF", "TD", "KM", "CD", "ER", "ET",
        "GN", "GW", "HT", "LR", "LY", "MG", "ML", "MR", "NE", "KP",
        "SO", "SS", "SD", "SY", "TJ", "UZ", "VE", "YE", "ZW"
    )

    override fun evaluate(transaction: Transaction, geoProfile: UserGeoProfile): RuleResult {
        val country = transaction.location.country
        val isHighRisk = highRiskCountries.contains(country)
        val userFrequentCountries = geoProfile.travelPattern.keys

        return when {
            isHighRisk && !userFrequentCountries.contains(country) -> RuleResult(
                ruleName = "HIGH_RISK_COUNTRY_RULE",
                score = 0.8,
                triggered = true,
                reason = "Transaction from high-risk country: $country (new for user)"
            )
            isHighRisk && userFrequentCountries.contains(country) -> RuleResult(
                ruleName = "HIGH_RISK_COUNTRY_RULE",
                score = 0.5,
                triggered = false,
                reason = "Transaction from high-risk country: $country (user's frequent country)"
            )
            else -> RuleResult(
                ruleName = "HIGH_RISK_COUNTRY_RULE",
                score = 0.1,
                triggered = false,
                reason = "Transaction from normal risk country: $country"
            )
        }
    }
}
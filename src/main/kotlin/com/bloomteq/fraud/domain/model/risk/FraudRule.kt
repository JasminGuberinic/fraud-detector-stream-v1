package com.bloomteq.fraud.domain.service.rules

import com.bloomteq.fraud.domain.model.profile.UserBehaviorProfile
import com.bloomteq.fraud.domain.model.profile.UserDeviceProfile
import com.bloomteq.fraud.domain.model.profile.UserGeoProfile
import com.bloomteq.fraud.domain.model.profile.UserVelocityProfile
import com.bloomteq.fraud.domain.model.transaction.RuleResult
import com.bloomteq.fraud.domain.model.transaction.Transaction

interface FraudRule {
    val name: String
    val weight: Double
    fun evaluate(transaction: Transaction, userHistory: List<Transaction>): RuleResult
}

interface GeoRule {
    val name: String
    val weight: Double
    fun evaluate(transaction: Transaction, geoProfile: UserGeoProfile): RuleResult
}

interface BehaviorRule {
    val name: String
    val weight: Double
    fun evaluate(transaction: Transaction, behaviorProfile: UserBehaviorProfile): RuleResult
}

interface DeviceRule {
    val name: String
    val weight: Double
    fun evaluate(transaction: Transaction, deviceProfile: UserDeviceProfile): RuleResult
}

interface VelocityRule {
    val name: String
    val weight: Double
    fun evaluate(transaction: Transaction, velocityProfile: UserVelocityProfile): RuleResult
}
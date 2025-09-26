package com.bloomteq.fraud.infrastructure.kafka.processor

import com.bloomteq.fraud.domain.model.profile.UserBehaviorProfile
import com.bloomteq.fraud.domain.model.transaction.ProcessedTransaction
import com.bloomteq.fraud.domain.model.transaction.Transaction
import com.bloomteq.fraud.infrastructure.kafka.serialization.JsonSerde
import com.bloomteq.fraud.infrastructure.rules.impl.UnusualTimeRule
import com.bloomteq.fraud.infrastructure.rules.impl.UnusualAmountRule
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.utils.Bytes
import org.apache.kafka.streams.StoreQueryParameters
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.KTable
import org.apache.kafka.streams.kstream.Materialized
import org.apache.kafka.streams.state.KeyValueStore
import org.apache.kafka.streams.state.QueryableStoreTypes
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore
import org.springframework.kafka.config.StreamsBuilderFactoryBean
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.Duration
import java.time.LocalTime
import java.time.ZoneOffset

@Component
class BehaviorStream(
    private val streamsBuilderFactoryBean: StreamsBuilderFactoryBean,
    private val objectMapper: ObjectMapper
) {

    fun buildBehaviorProfile(transactionStream: KStream<String, Transaction>): KTable<String, UserBehaviorProfile> {
        val behaviorProfileSerde = JsonSerde(objectMapper, UserBehaviorProfile::class.java)

        return transactionStream
            .groupByKey()
            .aggregate(
                {
                    logger.debug("Initializing empty UserBehaviorProfile")
                    UserBehaviorProfile()
                },
                { userId, transaction, profile ->
                    logger.debug("Updating behavior profile for user: {}", userId)
                    updateBehaviorProfile(profile, transaction)
                },
                Materialized.`as`<String, UserBehaviorProfile, KeyValueStore<Bytes, ByteArray>>("behavior-store")
                    .withKeySerde(Serdes.String())
                    .withValueSerde(behaviorProfileSerde)
                    .withRetention(Duration.ofDays(30))
            )
    }

    fun processRules(transaction: Transaction): ProcessedTransaction? {
        val behaviorProfile = getBehaviorProfile(transaction.userId) ?: return null

        val behaviorRules = listOf(
            UnusualTimeRule(),
            UnusualAmountRule()
        )

        val ruleResults = behaviorRules.map { rule ->
            rule.evaluate(transaction, behaviorProfile)
        }

        val totalScore = ruleResults.sumOf { it.score }
        val isFraudulent = ruleResults.any { it.triggered }

        return ProcessedTransaction(
            transaction = transaction,
            riskScore = totalScore,
            isFraudulent = isFraudulent,
            ruleResults = ruleResults,
        )
    }

    private fun updateBehaviorProfile(profile: UserBehaviorProfile, transaction: Transaction): UserBehaviorProfile {
        val transactionTime = LocalTime.ofInstant(transaction.timestamp, ZoneOffset.UTC)
        val dayOfWeek = transaction.timestamp.atZone(ZoneOffset.UTC).dayOfWeek

        val updatedTimeRanges = updateTimeRanges(profile.preferredTimeRanges, transactionTime)
        val updatedDaysOfWeek = profile.frequentDaysOfWeek.toMutableMap().apply {
            this[dayOfWeek] = (this[dayOfWeek] ?: 0) + 1
        }
        val updatedAmounts = (profile.typicalTransactionAmounts + transaction.amount).takeLast(100)
        val updatedMerchants = profile.merchantCategories.toMutableMap().apply {
            val category = getMerchantCategory(transaction.merchantId)
            this[category] = (this[category] ?: 0) + 1
        }

        val newAverage = if (profile.averageTransactionAmount == BigDecimal.ZERO) {
            transaction.amount
        } else {
            (profile.averageTransactionAmount + transaction.amount).divide(BigDecimal(2))
        }

        return profile.copy(
            preferredTimeRanges = updatedTimeRanges,
            frequentDaysOfWeek = updatedDaysOfWeek,
            typicalTransactionAmounts = updatedAmounts,
            merchantCategories = updatedMerchants,
            averageTransactionAmount = newAverage,
            lastBehaviorUpdate = transaction.timestamp
        )
    }

    private fun updateTimeRanges(
        currentRanges: Set<UserBehaviorProfile.TimeRange>,
        transactionTime: LocalTime
    ): Set<UserBehaviorProfile.TimeRange> {
        val hourRange = UserBehaviorProfile.TimeRange(
            startTime = transactionTime.withMinute(0).withSecond(0),
            endTime = transactionTime.withMinute(59).withSecond(59)
        )

        return if (currentRanges.any { it.contains(transactionTime) }) {
            currentRanges
        } else {
            currentRanges + hourRange
        }
    }

    private fun getMerchantCategory(merchantId: String): String {
        return when {
            merchantId.contains("grocery", ignoreCase = true) -> "GROCERY"
            merchantId.contains("gas", ignoreCase = true) -> "GAS"
            merchantId.contains("restaurant", ignoreCase = true) -> "RESTAURANT"
            merchantId.contains("retail", ignoreCase = true) -> "RETAIL"
            else -> "OTHER"
        }
    }

    private fun getBehaviorProfile(userId: String): UserBehaviorProfile? {
        return try {
            val kafkaStreams = streamsBuilderFactoryBean.kafkaStreams ?: return null
            val store: ReadOnlyKeyValueStore<String, UserBehaviorProfile> = kafkaStreams.store(
                StoreQueryParameters.fromNameAndType("behavior-store", QueryableStoreTypes.keyValueStore())
            )
            store.get(userId)
        } catch (e: Exception) {
            null
        }
    }
}
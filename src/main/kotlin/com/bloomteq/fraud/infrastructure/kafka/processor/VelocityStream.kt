package com.bloomteq.fraud.infrastructure.kafka.processor

import com.bloomteq.fraud.domain.model.profile.UserVelocityProfile
import com.bloomteq.fraud.domain.model.transaction.ProcessedTransaction
import com.bloomteq.fraud.domain.model.transaction.Transaction
import com.bloomteq.fraud.infrastructure.kafka.serialization.JsonSerde
import com.bloomteq.fraud.infrastructure.rules.impl.CardTestingRule
import com.bloomteq.fraud.infrastructure.rules.impl.RoboticPatternRule
import com.bloomteq.fraud.infrastructure.rules.impl.VelocityRules
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
import java.time.Duration
import java.time.temporal.ChronoUnit

@Component
class VelocityStream(
    private val streamsBuilderFactoryBean: StreamsBuilderFactoryBean,
    private val objectMapper: ObjectMapper
) {

    // Prima stream umesto da ga kreira
    fun buildVelocityProfile(transactionStream: KStream<String, Transaction>): KTable<String, UserVelocityProfile> {
        val velocityProfileSerde = JsonSerde(objectMapper, UserVelocityProfile::class.java)

        return transactionStream
            .groupByKey()
            .aggregate(
                { UserVelocityProfile() },
                { userId, transaction, profile -> updateVelocityProfile(profile, transaction) },
                Materialized.`as`<String, UserVelocityProfile, KeyValueStore<Bytes, ByteArray>>("velocity-store")
                    .withKeySerde(Serdes.String())
                    .withValueSerde(velocityProfileSerde)
                    .withRetention(Duration.ofDays(1))
            )
    }

    fun processRules(transaction: Transaction): ProcessedTransaction? {
        val velocityProfile = getVelocityProfile(transaction.userId) ?: return null

        val velocityRules = listOf(
            VelocityRules(),
            CardTestingRule(),
            RoboticPatternRule()
        )

        val ruleResults = velocityRules.map { rule ->
            rule.evaluate(transaction, velocityProfile)
        }

        val totalScore = ruleResults.sumOf { it.score }
        val isFraudulent = ruleResults.any { it.triggered }

        return ProcessedTransaction(
            transaction = transaction,
            riskScore = totalScore,
            isFraudulent = isFraudulent,
            ruleResults = ruleResults
        )
    }

    private fun updateVelocityProfile(profile: UserVelocityProfile, transaction: Transaction): UserVelocityProfile {
        val cutoff = transaction.timestamp.minus(10, ChronoUnit.MINUTES)

        return if (profile.lastTransactionTime?.isBefore(cutoff) == true) {
            UserVelocityProfile(
                transactionCount = 1,
                totalAmount = transaction.amount,
                lastTransactionTime = transaction.timestamp,
                uniqueMerchants = setOf(transaction.merchantId)
            )
        } else {
            profile.copy(
                transactionCount = profile.transactionCount + 1,
                totalAmount = profile.totalAmount + transaction.amount,
                lastTransactionTime = transaction.timestamp,
                uniqueMerchants = profile.uniqueMerchants + transaction.merchantId,
                averageTimeBetween = calculateAverageTime(profile, transaction)
            )
        }
    }

    private fun getVelocityProfile(userId: String): UserVelocityProfile? {
        return try {
            val kafkaStreams = streamsBuilderFactoryBean.kafkaStreams ?: return null
            val store: ReadOnlyKeyValueStore<String, UserVelocityProfile> = kafkaStreams.store(
                StoreQueryParameters.fromNameAndType("velocity-store", QueryableStoreTypes.keyValueStore())
            )
            store.get(userId)
        } catch (e: Exception) {
            null
        }
    }

    private fun calculateAverageTime(profile: UserVelocityProfile, transaction: Transaction): Duration? {
        return if (profile.lastTransactionTime != null) {
            val timeDiff = Duration.between(profile.lastTransactionTime, transaction.timestamp)
            if (profile.averageTimeBetween == null) {
                timeDiff
            } else {
                val avgMillis = (profile.averageTimeBetween.toMillis() + timeDiff.toMillis()) / 2
                Duration.ofMillis(avgMillis)
            }
        } else {
            null
        }
    }
}
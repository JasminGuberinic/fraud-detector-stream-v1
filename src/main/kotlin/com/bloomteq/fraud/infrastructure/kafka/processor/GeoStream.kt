package com.bloomteq.fraud.infrastructure.kafka.processor

import com.bloomteq.fraud.domain.model.profile.UserGeoProfile
import com.bloomteq.fraud.domain.model.transaction.ProcessedTransaction
import com.bloomteq.fraud.domain.model.transaction.Transaction
import com.bloomteq.fraud.infrastructure.kafka.serialization.JsonSerde
import com.bloomteq.fraud.infrastructure.rules.impl.ImpossibleTravelRule
import com.bloomteq.fraud.infrastructure.rules.impl.NewLocationRule
import com.bloomteq.fraud.infrastructure.rules.impl.HighRiskCountryRule
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

@Component
class GeoStream(
    private val streamsBuilderFactoryBean: StreamsBuilderFactoryBean,
    private val objectMapper: ObjectMapper,
) {

    fun buildGeoProfile(transactionStream: KStream<String, Transaction>): KTable<String, UserGeoProfile> {
        val geoProfileSerde = JsonSerde(objectMapper, UserGeoProfile::class.java)

        return transactionStream
            .groupByKey()
            .aggregate(
                { UserGeoProfile() },
                { userId, transaction, profile -> updateGeoProfile(profile, transaction) },
                Materialized.`as`<String, UserGeoProfile, KeyValueStore<Bytes, ByteArray>>("geo-store")
                    .withKeySerde(Serdes.String())
                    .withValueSerde(geoProfileSerde)
                    .withRetention(Duration.ofDays(7))
            )
    }

    fun processRules(transaction: Transaction): ProcessedTransaction? {
        val geoProfile = getGeoProfile(transaction.userId) ?: return null

        val geoRules = listOf(
            ImpossibleTravelRule(),
            NewLocationRule(),
            HighRiskCountryRule()
        )

        val ruleResults = geoRules.map { rule ->
            rule.evaluate(transaction, geoProfile)
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

    private fun updateGeoProfile(profile: UserGeoProfile, transaction: Transaction): UserGeoProfile {
        val newLocation = UserGeoProfile.LocationInfo(
            country = transaction.location.country,
            city = transaction.location.city,
            latitude = transaction.location.latitude,
            longitude = transaction.location.longitude,
            firstSeen = transaction.timestamp,
            lastSeen = transaction.timestamp,
            transactionCount = 1
        )

        val existingLocation = profile.knownLocations.find {
            it.country == transaction.location.country && it.city == transaction.location.city
        }

        val updatedLocations = if (existingLocation != null) {
            profile.knownLocations - existingLocation + existingLocation.copy(
                lastSeen = transaction.timestamp,
                transactionCount = existingLocation.transactionCount + 1
            )
        } else {
            profile.knownLocations + newLocation
        }

        val homeCountry = profile.homeCountry ?: transaction.location.country

        return profile.copy(
            knownLocations = updatedLocations,
            lastLocation = newLocation,
            lastLocationUpdate = transaction.timestamp,
            homeCountry = homeCountry
        )
    }

    private fun getGeoProfile(userId: String): UserGeoProfile? {
        return try {
            val kafkaStreams = streamsBuilderFactoryBean.kafkaStreams ?: return null
            val store: ReadOnlyKeyValueStore<String, UserGeoProfile> = kafkaStreams.store(
                StoreQueryParameters.fromNameAndType("geo-store", QueryableStoreTypes.keyValueStore())
            )
            store.get(userId)
        } catch (e: Exception) {
            null
        }
    }
}
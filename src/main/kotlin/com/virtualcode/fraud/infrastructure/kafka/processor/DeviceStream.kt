package com.virtualcode.fraud.infrastructure.kafka.processor

import com.virtualcode.fraud.domain.model.profile.UserDeviceProfile
import com.virtualcode.fraud.domain.model.transaction.ProcessedTransaction
import com.virtualcode.fraud.domain.model.transaction.Transaction
import com.virtualcode.fraud.infrastructure.kafka.serialization.JsonSerde
import com.virtualcode.fraud.infrastructure.rules.impl.NewDeviceRule
import com.virtualcode.fraud.infrastructure.rules.impl.DeviceSwitchingRule
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
import java.time.Instant

@Component
class DeviceStream(
    private val streamsBuilderFactoryBean: StreamsBuilderFactoryBean,
    private val objectMapper: ObjectMapper
) {

    fun buildDeviceProfile(transactionStream: KStream<String, Transaction>): KTable<String, UserDeviceProfile> {
        val deviceProfileSerde = JsonSerde(objectMapper, UserDeviceProfile::class.java)

        return transactionStream
            .groupByKey()
            .aggregate(
                { UserDeviceProfile() },
                { userId, transaction, profile -> updateDeviceProfile(profile, transaction) },
                Materialized.`as`<String, UserDeviceProfile, KeyValueStore<Bytes, ByteArray>>("device-store")
                    .withKeySerde(Serdes.String())
                    .withValueSerde(deviceProfileSerde)
                    .withRetention(Duration.ofDays(90))
            )
    }

    fun processRules(transaction: Transaction): ProcessedTransaction? {
        val deviceProfile = getDeviceProfile(transaction.userId) ?: return null

        val deviceRules = listOf(
            NewDeviceRule(),
            DeviceSwitchingRule()
        )

        val ruleResults = deviceRules.map { rule ->
            rule.evaluate(transaction, deviceProfile)
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

    private fun updateDeviceProfile(profile: UserDeviceProfile, transaction: Transaction): UserDeviceProfile {
        val currentDeviceId = extractDeviceId(transaction)
        val currentDeviceInfo = UserDeviceProfile.DeviceInfo(
            deviceId = currentDeviceId,
            userAgent = extractUserAgent(transaction),
            ipAddress = extractIpAddress(transaction),
            firstSeen = Instant.now(),
            lastSeen = transaction.timestamp,
            transactionCount = 1,
            deviceType = transaction.deviceInfo?.deviceType,
            operatingSystem = transaction.deviceInfo?.operatingSystem,
            isMobile = transaction.deviceInfo?.isMobile ?: false
        )

        val existingDevice = profile.knownDevices.find { it.deviceId == currentDeviceId }
        val updatedKnownDevices = if (existingDevice != null) {
            profile.knownDevices - existingDevice + existingDevice.copy(
                lastSeen = transaction.timestamp,
                transactionCount = existingDevice.transactionCount + 1
            )
        } else {
            profile.knownDevices + currentDeviceInfo
        }

        val deviceSwitchCount = if (profile.lastDeviceUsed != null &&
            profile.lastDeviceUsed.deviceId != currentDeviceId) {
            profile.deviceSwitchCount + 1
        } else {
            profile.deviceSwitchCount
        }

        return profile.copy(
            knownDevices = updatedKnownDevices,
            deviceSwitchCount = deviceSwitchCount,
            lastDeviceUsed = currentDeviceInfo,
            lastDeviceUpdate = transaction.timestamp
        )
    }

    private fun getDeviceProfile(userId: String): UserDeviceProfile? {
        return try {
            val kafkaStreams = streamsBuilderFactoryBean.kafkaStreams ?: return null
            val store: ReadOnlyKeyValueStore<String, UserDeviceProfile> = kafkaStreams.store(
                StoreQueryParameters.fromNameAndType("device-store", QueryableStoreTypes.keyValueStore())
            )
            store.get(userId)
        } catch (e: Exception) {
            null
        }
    }

    private fun extractDeviceId(transaction: Transaction): String {
        return transaction.deviceInfo?.deviceId
            ?: "${transaction.cardNumber.takeLast(4)}_${transaction.location.country}_fallback"
    }

    private fun extractUserAgent(transaction: Transaction): String? {
        return transaction.userAgent ?: transaction.deviceInfo?.browserInfo
    }

    private fun extractIpAddress(transaction: Transaction): String? {
        return transaction.ipAddress
    }
}
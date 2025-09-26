package com.bloomteq.fraud.infrastructure.kafka.processor

import com.bloomteq.fraud.application.service.FraudMlService
import com.bloomteq.fraud.application.service.TransactionPublisherService
import com.bloomteq.fraud.domain.model.transaction.ProcessedTransaction
import com.bloomteq.fraud.domain.model.transaction.Transaction
import com.bloomteq.fraud.infrastructure.kafka.config.KafkaTopics
import com.bloomteq.fraud.infrastructure.kafka.serialization.TransactionSerde
import com.bloomteq.fraud.infrastructure.persistence.entity.ProcessedTransactionEntity
import com.bloomteq.fraud.infrastructure.persistence.entity.TransactionEntity
import com.bloomteq.fraud.infrastructure.persistence.repository.ProcessedTransactionRepository
import com.bloomteq.fraud.infrastructure.persistence.repository.TransactionRepository
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.annotation.PostConstruct
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.KStream
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.time.ZoneOffset

val logger = LoggerFactory.getLogger(MainFraudStream::class.java)

@Component
class MainFraudStream(
    private val kafkaTemplate: KafkaTemplate<String, Any>,
    private val velocityStream: VelocityStream,
    private val geoStream: GeoStream,
    private val behaviorStream: BehaviorStream,
    private val deviceStream: DeviceStream,
    private val transactionRepository: TransactionRepository,
    private val processedTransactionRepository: ProcessedTransactionRepository,
    private val objectMapper: ObjectMapper,
    private val processedTransactionTemplate: KafkaTemplate<String, ProcessedTransaction>,
    private val fraudMlService: FraudMlService // Inject FraudMlService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Autowired
    private lateinit var streamsBuilder: StreamsBuilder

    @PostConstruct
    fun buildMainFraudPipeline() {
        logger.info("Building main fraud pipeline")

        // Create transaction stream with proper serde
        val transactionSerde = TransactionSerde()

        // Define the base transaction stream
        val transactionStream: KStream<String, Transaction> = streamsBuilder
            .stream(
                KafkaTopics.INCOMING_TRANSACTIONS,
                Consumed.with(Serdes.String(), transactionSerde)
            )
            .peek { key, transaction ->
                logger.info("Received transaction: ${transaction.id} for user ${transaction.userId}")
            }
            .selectKey { _, transaction -> transaction.userId }

        // Material point for transaction processing - ensures the stream is built
        transactionStream
            .peek { _, transaction ->
                // Process rules here to ensure every transaction is processed
                processAllRules(transaction)
            }
            .to(KafkaTopics.TRANSACTION_HISTORY)

        // Build all profile tables
        logger.info("Building profile stores")
        velocityStream.buildVelocityProfile(transactionStream)
        geoStream.buildGeoProfile(transactionStream)
        behaviorStream.buildBehaviorProfile(transactionStream)
        deviceStream.buildDeviceProfile(transactionStream)
        logger.info("Profile stores built successfully")
    }

    private fun processAllRules(transaction: Transaction) {
        // 1. Save original transaction
        saveTransactionToDatabase(transaction)
        logger.debug("Transaction ${transaction.id} saved to database")

        val allRuleResults = mutableListOf<ProcessedTransaction>()

        // 2. Process with all rule engines
        velocityStream.processRules(transaction)?.let { allRuleResults.add(it) }
        geoStream.processRules(transaction)?.let { allRuleResults.add(it) }
        behaviorStream.processRules(transaction)?.let { allRuleResults.add(it) }
        deviceStream.processRules(transaction)?.let { allRuleResults.add(it) }

        val mlScore = predictFraudProbability(transaction)


        // 3. Combine and process results
        if (allRuleResults.isNotEmpty()) {
            val combinedResult = combineResults(transaction, allRuleResults, mlScore)
            logger.info("Transaction ${transaction.id} processed with risk score: ${combinedResult.riskScore}, fraudulent: ${combinedResult.isFraudulent}")

            // 4. Save processed result
            saveProcessedTransactionToDatabase(combinedResult)

            // 5. Send to appropriate Kafka topics
            kafkaTemplate.send(KafkaTopics.PROCESSED_TRANSACTIONS, transaction.id, combinedResult)
            if (combinedResult.isFraudulent) {
                processedTransactionTemplate.send(KafkaTopics.FRAUD_ALERTS, transaction.id, combinedResult)
                logger.warn("FRAUD ALERT: Transaction ${transaction.id} for user ${transaction.userId}")
            }
        }
    }

    private fun saveTransactionToDatabase(transaction: Transaction) {
        try {
            val entity = TransactionEntity(
                id = transaction.id,
                userId = transaction.userId,
                amount = transaction.amount,
                currency = transaction.currency,
                merchantId = transaction.merchantId,
                country = transaction.location.country,
                city = transaction.location.city,
                timestamp = transaction.timestamp,
                cardNumber = transaction.cardNumber,
                transactionType = transaction.transactionType.name,
                deviceId = transaction.deviceInfo?.deviceId,
                ipAddress = transaction.ipAddress
            )
            transactionRepository.save(entity)
        } catch (e: Exception) {
            // Log error but don't fail processing
            println("Failed to save transaction ${transaction.id}: ${e.message}")
        }
    }

    private fun saveProcessedTransactionToDatabase(processedTransaction: ProcessedTransaction) {
        try {
            val ruleResultsJson = objectMapper.writeValueAsString(processedTransaction.ruleResults)

            val entity = ProcessedTransactionEntity(
                transactionId = processedTransaction.transaction.id,
                riskScore = processedTransaction.riskScore,
                isFraudulent = processedTransaction.isFraudulent,
                ruleResults = ruleResultsJson,
                processedAt = processedTransaction.processedAt
            )
            processedTransactionRepository.save(entity)
        } catch (e: Exception) {
            // Log error but don't fail processing
            println("Failed to save processed transaction ${processedTransaction.transaction.id}: ${e.message}")
        }
    }

    private fun combineResults(transaction: Transaction, results: List<ProcessedTransaction>, mlScore: Double): ProcessedTransaction {
        val allRuleResults = results.flatMap { it.ruleResults }
        val weights = mapOf(
            "VELOCITY" to 0.25,
            "GEO" to 0.35,
            "BEHAVIOR" to 0.25,
            "DEVICE" to 0.15
        )

        val finalScore = allRuleResults.sumOf { result ->
            val weight = weights.entries.find { result.ruleName.contains(it.key) }?.value ?: 0.1
            result.score * weight
        }

        val isFraudulent = allRuleResults.any { it.triggered } || finalScore > 0.7

        return ProcessedTransaction(
            transaction = transaction,
            riskScore = finalScore,
            isFraudulent = isFraudulent,
            ruleResults = allRuleResults,
            mlRiskScore = mlScore
        )
    }

    private fun predictFraudProbability(transaction: Transaction): Double {
        return fraudMlService.predict(transaction)
    }

    /**
     * Extracts features from a transaction in the order expected by the ML model
     */

    /**
     * Helper method to identify high-risk countries
     */
    private fun isHighRiskCountry(country: String): Boolean {
        val highRiskCountries = setOf("XY", "ZZ", "AA", "BB") // Example high-risk countries
        return highRiskCountries.contains(country)
    }
}
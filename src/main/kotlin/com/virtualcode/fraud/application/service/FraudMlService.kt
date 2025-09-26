package com.virtualcode.fraud.application.service

import com.virtualcode.fraud.domain.model.transaction.Transaction
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException

@Service
class FraudMlService(
    private val restTemplate: RestTemplate
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Predicts fraud probability by calling a Flask Python service.
     * @param request The transaction request to evaluate.
     * @return Score between 0 and 1, where higher values indicate higher fraud probability.
     */
    fun predict(request: Transaction): Double {
        val aiServiceUrl = "http://localhost:5100/predict" // Replace with your Flask service URL

        return try {
            val response = restTemplate.postForObject(
                aiServiceUrl,
                request.toRequestPayload(),
                FraudPredictionResponse::class.java
            )

            if (response != null && response.fraud == 1) {
                logger.info("Fraud detected for transaction with score ${response.score}")
                response.score
            } else {
                logger.info("Transaction is not fraudulent")
                0.0
            }
        } catch (e: HttpClientErrorException) {
            logger.error("Client error while calling service: ${e.message}")
            0.0
        } catch (e: HttpServerErrorException) {
            logger.error("Server error while calling service: ${e.message}")
            0.0
        } catch (e: Exception) {
            logger.error("Unexpected error while calling service: ${e.message}")
            0.0
        }
    }
}

private fun Transaction.toRequestPayload(): Map<String, Any> {
    return mapOf(
        "id" to id,
        "userId" to userId,
        "amount" to amount.toPlainString(),
        "currency" to currency,
        "merchantId" to merchantId,
        "location" to mapOf(
            "country" to location.country,
            "city" to (location.city ?: "").toString(),
            "latitude" to (location.latitude ?: 0.0).toString(),
            "longitude" to (location.longitude ?: 0.0).toString(),
            "timezone" to (location.timezone ?: "").toString()
        ),
        "timestamp" to timestamp.toString(),
        "cardNumber" to cardNumber,
        "transactionType" to transactionType.name,
        "ipAddress" to (ipAddress ?: "").toString(),
        "userAgent" to (userAgent ?: "").toString()
    )
}

data class FraudPredictionResponse(
    val fraud: Int,
    val score: Double,
    val ml_score: Double,
    val rule_score: Double,
    val reasons: List<String>
)
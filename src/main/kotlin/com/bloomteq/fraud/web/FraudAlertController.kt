package com.bloomteq.fraud.infrastructure.web.controller

import com.bloomteq.fraud.infrastructure.service.FraudAlertService
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ResponseBody

@Controller
class FraudAlertController(
    private val fraudAlertService: FraudAlertService
) {
    @GetMapping("/dashboard")
    fun dashboard(model: Model): String {
        model.addAttribute("fraudAlerts", fraudAlertService.getAllFraudAlerts())
        return "fraud-dashboard"
    }

    @GetMapping("/api/fraud-alerts")
    @ResponseBody
    fun getFraudAlerts(): List<FraudAlertDto> {
        return fraudAlertService.getAllFraudAlerts().map { alert ->
            FraudAlertDto(
                transactionId = alert.transaction.id,
                userId = alert.transaction.userId,
                amount = alert.transaction.amount,
                currency = alert.transaction.currency,
                riskScore = alert.riskScore,
                triggeredRules = alert.ruleResults.filter { it.triggered }.map { it.ruleName },
                timestamp = alert.transaction.timestamp
            )
        }
    }

    data class FraudAlertDto(
        val transactionId: String,
        val userId: String,
        val amount: java.math.BigDecimal,
        val currency: String,
        val riskScore: Double,
        val triggeredRules: List<String>,
        val timestamp: java.time.Instant
    )
}
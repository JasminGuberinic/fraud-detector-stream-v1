package com.virtualcode.fraud.web

import com.virtualcode.fraud.infrastructure.service.ElasticsearchAnalyticsService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/analytics")
class AnalyticsController(private val elasticsearchAnalyticsService: ElasticsearchAnalyticsService) {

    @GetMapping("/summary")
    fun getFraudSummary(): ResponseEntity<Map<String, Any>> {
        val summary = elasticsearchAnalyticsService.getFraudSummary()
        return ResponseEntity.ok(summary)
    }

    @GetMapping("/transactions/riskiest")
    fun getTopRiskiestTransactions(
        @RequestParam(defaultValue = "10") limit: Int
    ): ResponseEntity<List<Map<String, Any>>> {
        val transactions = elasticsearchAnalyticsService.getTopRiskiestTransactions(limit)
        return ResponseEntity.ok(transactions)
    }

    @GetMapping("/rules/distribution")
    fun getTriggeredRulesDistribution(): ResponseEntity<Map<String, Any>> {
        val distribution = elasticsearchAnalyticsService.getTriggeredRulesDistribution()
        return ResponseEntity.ok(distribution)
    }
}
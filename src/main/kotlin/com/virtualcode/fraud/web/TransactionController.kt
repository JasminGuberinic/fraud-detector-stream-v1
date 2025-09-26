package com.virtualcode.fraud.web

import com.virtualcode.fraud.application.service.TransactionPublisherService
import com.virtualcode.fraud.web.dto.TransactionRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/transactions")
class TransactionController(private val transactionPublisherService: TransactionPublisherService) {

    @PostMapping
    fun submitTransaction(@RequestBody request: TransactionRequest): ResponseEntity<Map<String, Any>> {
        val transaction = request.toTransaction()
        transactionPublisherService.publishTransaction(transaction)

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(
            mapOf(
                "transactionId" to transaction.id,
                "status" to "PROCESSING",
                "message" to "Transaction submitted for fraud analysis"
            )
        )
    }
}
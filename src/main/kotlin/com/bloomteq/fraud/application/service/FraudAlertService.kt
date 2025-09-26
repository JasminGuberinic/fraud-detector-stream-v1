package com.bloomteq.fraud.infrastructure.service

import com.bloomteq.fraud.domain.model.transaction.ProcessedTransaction
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentLinkedQueue

@Service
class FraudAlertService {
    // Store the last 100 fraud alerts
    private val fraudAlerts = ConcurrentLinkedQueue<ProcessedTransaction>()
    private val maxAlerts = 100

    fun addFraudAlert(fraudAlert: ProcessedTransaction) {
        fraudAlerts.add(fraudAlert)

        // Keep only the latest alerts
        while (fraudAlerts.size > maxAlerts) {
            fraudAlerts.poll()
        }
    }

    fun getAllFraudAlerts(): List<ProcessedTransaction> {
        return fraudAlerts.toList()
    }
}
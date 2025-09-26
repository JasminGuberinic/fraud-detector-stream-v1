package com.virtualcode.fraud.web

import com.virtualcode.fraud.infrastructure.service.FraudAlertService
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/dashboard")
class DashboardController(
    private val fraudAlertService: FraudAlertService
) {

    @GetMapping("/fraud")
    fun showFraudDashboard(model: Model): String {
        val fraudAlerts = fraudAlertService.getAllFraudAlerts()
        model.addAttribute("fraudAlerts", fraudAlerts)
        return "fraud-dashboard"
    }
}
package com.bloomteq.fraud

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class FraudDetectorApplication

fun main(args: Array<String>) {
    runApplication<FraudDetectorApplication>(*args)
}

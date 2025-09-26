package com.virtualcode.fraud.infrastructure.kafka.serialization

import com.virtualcode.fraud.domain.model.transaction.Transaction
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serializer

class TransactionSerde : Serde<Transaction> {
    private val objectMapper = ObjectMapper().apply {
        registerModule(KotlinModule.Builder().build())
        registerModule(JavaTimeModule())
    }

    override fun serializer(): Serializer<Transaction> = TransactionSerializer(objectMapper)
    override fun deserializer(): Deserializer<Transaction> = TransactionDeserializer(objectMapper)
}

class TransactionSerializer(private val objectMapper: ObjectMapper) : Serializer<Transaction> {
    override fun serialize(topic: String?, data: Transaction?): ByteArray? {
        return data?.let { objectMapper.writeValueAsBytes(it) }
    }
}

class TransactionDeserializer(private val objectMapper: ObjectMapper) : Deserializer<Transaction> {
    override fun deserialize(topic: String?, data: ByteArray?): Transaction? {
        return data?.let { objectMapper.readValue(it, Transaction::class.java) }
    }
}
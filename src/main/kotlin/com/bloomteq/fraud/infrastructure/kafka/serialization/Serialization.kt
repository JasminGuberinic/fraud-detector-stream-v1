package com.bloomteq.fraud.infrastructure.kafka.serialization

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serializer

class JsonSerde<T>(private val objectMapper: ObjectMapper, private val type: Class<T>) : Serde<T> {
    override fun serializer(): Serializer<T> = JsonSerializer(objectMapper)
    override fun deserializer(): Deserializer<T> = JsonDeserializer(objectMapper, type)
}

class JsonSerializer<T>(private val objectMapper: ObjectMapper) : Serializer<T> {
    override fun serialize(topic: String?, data: T?): ByteArray? {
        return data?.let { objectMapper.writeValueAsBytes(it) }
    }
}

class JsonDeserializer<T>(private val objectMapper: ObjectMapper, private val type: Class<T>) : Deserializer<T> {
    override fun deserialize(topic: String?, data: ByteArray?): T? {
        return data?.let { objectMapper.readValue(it, type) }
    }
}
package com.virtualcode.fraud.infrastructure.config

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.json.jackson.JacksonJsonpMapper
import co.elastic.clients.transport.ElasticsearchTransport
import co.elastic.clients.transport.rest_client.RestClientTransport
import org.apache.http.HttpHost
import org.elasticsearch.client.RestClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ElasticsearchConfig {
    @Value("\${spring.elasticsearch.uris:http://localhost:9200}")
    private lateinit var elasticsearchUri: String

    @Bean
    fun elasticsearchClient(): ElasticsearchClient {
        val uri = java.net.URI(elasticsearchUri)
        val restClient = RestClient.builder(
            HttpHost(uri.host, uri.port, uri.scheme)
        ).build()

        val transport: ElasticsearchTransport = RestClientTransport(
            restClient, JacksonJsonpMapper()
        )

        return ElasticsearchClient(transport)
    }
}
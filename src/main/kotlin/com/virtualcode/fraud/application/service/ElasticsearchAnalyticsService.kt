package com.virtualcode.fraud.infrastructure.service

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.elasticsearch._types.SortOrder
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation
import co.elastic.clients.elasticsearch.core.SearchResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.HashMap

@Service
class ElasticsearchAnalyticsService(
    private val elasticsearchClient: ElasticsearchClient
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val INDEX_NAME = "fraud-alerts"

    fun getFraudSummary(): Map<String, Any> {
        try {
            val response = elasticsearchClient.search({ s ->
                s.index(INDEX_NAME)
                    .size(0)
                    .aggregations("total_count", Aggregation.of { a ->
                        a.valueCount { vc -> vc.field("riskScore") }
                    })
                    .aggregations("avg_score", Aggregation.of { a ->
                        a.avg { avg -> avg.field("riskScore") }
                    })
                    .aggregations("countries", Aggregation.of { a ->
                        a.terms { t -> t.field("transaction.location.country").size(10) }
                    })
            }, HashMap::class.java) as SearchResponse<HashMap<String, Any>>

            val result = HashMap<String, Any>()
            result["total_alerts"] = response.aggregations()["total_count"]?.valueCount()?.value() ?: 0
            result["avg_risk_score"] = response.aggregations()["avg_score"]?.avg()?.value() ?: 0.0

            val countries = ArrayList<Map<String, Any>>()
            response.aggregations()["countries"]?.sterms()?.buckets()?.array()?.forEach { bucket ->
                countries.add(mapOf(
                    "country" to (bucket.key() as String),
                    "count" to bucket.docCount()
                ))
            }
            result["countries"] = countries

            return result
        } catch (e: Exception) {
            logger.error("Error getting fraud summary", e)
            return emptyMap()
        }
    }

    fun getTopRiskiestTransactions(limit: Int = 10): List<Map<String, Any>> {
        try {
            val response = elasticsearchClient.search({ s ->
                s.index(INDEX_NAME)
                    .size(limit)
                    .sort { sort ->
                        sort.field { f ->
                            f.field("riskScore").order(SortOrder.Desc)
                        }
                    }
            }, HashMap::class.java) as SearchResponse<HashMap<String, Any>>

            return response.hits().hits().map { hit ->
                val source = hit.source() ?: emptyMap<String, Any>()
                val transactionData = source["transaction"] as? Map<String, Any> ?: emptyMap()

                mapOf(
                    "id" to (transactionData["id"] as? String ?: ""),
                    "userId" to (transactionData["userId"] as? String ?: ""),
                    "amount" to ((transactionData["amount"] as? Number)?.toDouble() ?: 0.0),
                    "riskScore" to ((source["riskScore"] as? Number)?.toDouble() ?: 0.0),
                    "timestamp" to (transactionData["timestamp"] as? String ?: ""),
                    "country" to ((transactionData["location"] as? Map<String, Any>)?.get("country") as? String ?: "")
                )
            }
        } catch (e: Exception) {
            logger.error("Error getting top riskiest transactions", e)
            return emptyList()
        }
    }

    fun getTriggeredRulesDistribution(): Map<String, Any> {
        try {
            val response = elasticsearchClient.search({ s ->
                s.index(INDEX_NAME)
                    .size(0)
                    .aggregations("rules", Aggregation.of { a ->
                        a.terms { t ->
                            t.field("ruleResults.ruleName")
                                .size(20)
                        }
                    })
            }, HashMap::class.java) as SearchResponse<HashMap<String, Any>>

            val result = HashMap<String, Any>()
            val rulesData = HashMap<String, Long>()

            response.aggregations()["rules"]?.sterms()?.buckets()?.array()?.forEach { bucket ->
                val ruleName = bucket.key() as String
                rulesData[ruleName] = bucket.docCount()
            }

            result["rules"] = rulesData
            return result
        } catch (e: Exception) {
            logger.error("Error getting rules distribution", e)
            return emptyMap()
        }
    }

}
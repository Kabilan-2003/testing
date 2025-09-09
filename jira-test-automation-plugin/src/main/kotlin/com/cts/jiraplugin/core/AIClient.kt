package com.cts.jiraplugin.core

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.intellij.openapi.diagnostic.Logger
import java.net.HttpURLConnection
import java.net.URL

data class AIAnalysisResult(
    val embedding: List<Double>,
    val severity: String,
    val probableCause: String,
    val summary: String,
    val isDuplicate: Boolean,
    val similarTicket: SimilarTicket?
)

data class SimilarTicket(
    val testName: String,
    val jiraKey: String,
    val jiraUrl: String
)

// Represents a group of similar test failures
data class FailureCluster(
    val representativeName: String,
    val failures: List<Map<String, Any>>,
    val count: Int
)

object AIClient {
    private val logger = Logger.getInstance(AIClient::class.java)
    private const val AI_SERVICE_URL = "http://localhost:8000"  // Your Python service
    private val mapper = jacksonObjectMapper()

    fun analyzeFailure(testName: String, errorMessage: String, stackTrace: String): AIAnalysisResult? {
        return try {
            val url = URL("$AI_SERVICE_URL/analyze-failure")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            val requestBody = mapOf(
                "test_name" to testName,
                "error_message" to errorMessage,
                "stack_trace" to stackTrace
            )

            connection.outputStream.use { os ->
                val input = mapper.writeValueAsString(requestBody).toByteArray(Charsets.UTF_8)
                os.write(input, 0, input.size)
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                    val response = reader.readText()
                    mapper.readValue(response, AIAnalysisResult::class.java)
                }
            } else {
                logger.warn("AI service returned error: ${connection.responseCode}")
                null
            }
        } catch (e: Exception) {
            logger.error("Error calling AI service: ${e.message}")
            null
        }
    }

    fun clusterFailures(failures: List<Map<String, Any>>): List<FailureCluster> {
        if (failures.isEmpty()) {
            return emptyList()
        }

        return try {
            val url = URL("$AI_SERVICE_URL/cluster-failures")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            val requestBody = mapOf("failures" to failures)

            connection.outputStream.use { os ->
                val input = mapper.writeValueAsString(requestBody).toByteArray(Charsets.UTF_8)
                os.write(input, 0, input.size)
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                    val response = reader.readText()
                    mapper.readValue(response, mapper.typeFactory.constructCollectionType(List::class.java, FailureCluster::class.java))
                }
            } else {
                logger.warn("Clustering service returned error: ${connection.responseCode}")
                emptyList()
            }
        } catch (e: Exception) {
            logger.error("Error calling clustering service: ${e.message}")
            emptyList()
        }
    }

    fun checkForDuplicates(embedding: List<Double>, previousFailures: List<Map<String, Any>>): Boolean {
        // This logic will now be handled by the clustering endpoint
        return false
    }
}
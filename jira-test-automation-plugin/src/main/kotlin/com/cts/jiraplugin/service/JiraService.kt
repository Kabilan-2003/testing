package com.cts.jiraplugin.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.cts.jiraplugin.model.JiraIssue
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class JiraService(
    private val baseUrl: String,
    private val email: String,
    private val apiToken: String
) {
    private val client = OkHttpClient()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val objectMapper = ObjectMapper()

    private val auth = Credentials.basic(email, apiToken)

    fun createIssue(issue: JiraIssue): Result<String> {
        val payload = mapOf(
            "fields" to mapOf(
                "project" to mapOf(
                    "key" to issue.projectKey
                ),
                "summary" to issue.summary,
                "description" to issue.description,
                "issuetype" to mapOf(
                    "name" to issue.issueType
                ),
                "priority" to mapOf(
                    "name" to issue.priority
                ),
                "labels" to issue.labels
            )
        )

        val requestBody = objectMapper.writeValueAsString(payload)
            .toRequestBody(jsonMediaType)

        val request = Request.Builder()
            .url("$baseUrl/rest/api/3/issue")
            .header("Authorization", auth)
            .header("Accept", "application/json")
            .post(requestBody)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return Result.failure(IOException("Unexpected code $response: ${response.body?.string()}"))
                }

                val responseBody = response.body?.string() ?: ""
                val jsonResponse = objectMapper.readValue<Map<String, Any>>(responseBody)
                val issueKey = jsonResponse["key"] as? String
                    ?: return Result.failure(IOException("No issue key in response"))

                Result.success(issueKey)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun testConnection(): Boolean {
        val request = Request.Builder()
            .url("$baseUrl/rest/api/3/myself")
            .header("Authorization", auth)
            .header("Accept", "application/json")
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            false
        }
    }
}

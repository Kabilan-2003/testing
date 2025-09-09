package com.cts.jiraplugin.core

import com.cts.jiraplugin.PluginSettings
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.intellij.openapi.diagnostic.Logger
import java.net.HttpURLConnection
import java.net.URL

object JiraClient {
    private val logger = Logger.getInstance(JiraClient::class.java)
    private val mapper = jacksonObjectMapper()

    private fun getAccessToken(): String? {
        val settings = PluginSettings.getInstance()
        val now = System.currentTimeMillis()
        val expiry = settings.tokenExpiry

        return if (settings.accessToken != null && now < expiry) {
            settings.accessToken
        } else {
            // Try refresh
            val success = OAuthManager.refreshAccessToken()
            if (success) settings.accessToken else null
        }
    }


    private fun authorizedConnection(endpoint: String, method: String): HttpURLConnection {
        val settings = PluginSettings.getInstance()
        val url = URL("${settings.jiraUrl}/rest/api/3/$endpoint")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = method
        connection.setRequestProperty("Content-Type", "application/json")

        val token = getAccessToken()
        if (token != null) {
            connection.setRequestProperty("Authorization", "Bearer $token")
        } else {
            logger.error("⚠️ No access token found. Please login via Atlassian.")
        }

        return connection
    }

    fun createIssue(summary: String, description: String, issueType: String = "Bug"): String? {
        return try {
            val settings = PluginSettings.getInstance()
            val projectKey = settings.projectKey

            val payload = mapOf(
                "fields" to mapOf(
                    "project" to mapOf("key" to projectKey),
                    "summary" to summary,
                    "description" to description,
                    "issuetype" to mapOf("name" to issueType)
                )
            )

            val connection = authorizedConnection("issue", "POST")
            connection.doOutput = true

            connection.outputStream.use { os ->
                val input = mapper.writeValueAsString(payload).toByteArray(Charsets.UTF_8)
                os.write(input, 0, input.size)
            }

            if (connection.responseCode == HttpURLConnection.HTTP_CREATED) {
                connection.inputStream.bufferedReader().use { reader ->
                    val response = reader.readText()
                    val json = mapper.readTree(response)
                    json["key"].asText() // return Jira issue key (e.g. "PROJ-123")
                }
            } else {
                logger.error("❌ Failed to create Jira issue: ${connection.responseCode}")
                null
            }
        } catch (e: Exception) {
            logger.error("Error creating Jira issue: ${e.message}")
            null
        }
    }

    fun getMyself(): String? {
        return try {
            val connection = authorizedConnection("myself", "GET")

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader().use { reader ->
                    val response = reader.readText()
                    val json = mapper.readTree(response)
                    json["displayName"].asText()
                }
            } else {
                logger.error("❌ Failed to fetch user profile: ${connection.responseCode}")
                null
            }
        } catch (e: Exception) {
            logger.error("Error fetching Jira user: ${e.message}")
            null
        }
    }

    fun searchIssues(jql: String): List<String> {
        return try {
            val connection = authorizedConnection("search?jql=${jql}", "GET")

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader().use { reader ->
                    val response = reader.readText()
                    val json = mapper.readTree(response)
                    json["issues"].map { it["key"].asText() }
                }
            } else {
                logger.error("❌ Jira search failed: ${connection.responseCode}")
                emptyList()
            }
        } catch (e: Exception) {
            logger.error("Error searching Jira issues: ${e.message}")
            emptyList()
        }
    }
}

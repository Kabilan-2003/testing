package com.jira.plugin.services

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.intellij.openapi.diagnostic.thisLogger
import com.jira.plugin.models.TestFailure
import com.jira.plugin.settings.JiraSettings
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Service for interacting with Jira REST API
 */
class JiraApiService {
    
    private val logger = thisLogger()
    private val settings = JiraSettings.getInstance()
    private val gson = Gson()
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
    
    /**
     * Create a Jira ticket from a test failure
     */
    fun createTicket(testFailure: TestFailure): Result<JiraTicketResponse> {
        if (!settings.isJiraConfigured()) {
            return Result.failure(IllegalStateException("Jira is not properly configured"))
        }
        
        val requestBody = buildCreateTicketRequest(testFailure)
        val request = buildCreateTicketRequest(requestBody)
        
        return try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            
            if (response.isSuccessful && responseBody != null) {
                val ticketResponse = gson.fromJson(responseBody, JiraTicketResponse::class.java)
                logger.info("Successfully created Jira ticket: ${ticketResponse.key}")
                Result.success(ticketResponse)
            } else {
                val errorMessage = "Failed to create Jira ticket. Status: ${response.code}, Body: $responseBody"
                logger.error(errorMessage)
                Result.failure(RuntimeException(errorMessage))
            }
        } catch (e: IOException) {
            logger.error("Network error while creating Jira ticket", e)
            Result.failure(e)
        } catch (e: Exception) {
            logger.error("Unexpected error while creating Jira ticket", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get ticket details by key
     */
    fun getTicket(ticketKey: String): Result<JiraTicketResponse> {
        if (!settings.isJiraConfigured()) {
            return Result.failure(IllegalStateException("Jira is not properly configured"))
        }
        
        val url = "${settings.getJiraBaseUrl()}/rest/api/3/issue/$ticketKey"
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", getAuthHeader())
            .addHeader("Accept", "application/json")
            .build()
        
        return try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            
            if (response.isSuccessful && responseBody != null) {
                val ticketResponse = gson.fromJson(responseBody, JiraTicketResponse::class.java)
                Result.success(ticketResponse)
            } else {
                val errorMessage = "Failed to get Jira ticket. Status: ${response.code}, Body: $responseBody"
                logger.error(errorMessage)
                Result.failure(RuntimeException(errorMessage))
            }
        } catch (e: IOException) {
            logger.error("Network error while getting Jira ticket", e)
            Result.failure(e)
        } catch (e: Exception) {
            logger.error("Unexpected error while getting Jira ticket", e)
            Result.failure(e)
        }
    }
    
    /**
     * Test Jira connection
     */
    fun testConnection(): Result<Boolean> {
        if (!settings.isJiraConfigured()) {
            return Result.failure(IllegalStateException("Jira is not properly configured"))
        }
        
        val url = "${settings.getJiraBaseUrl()}/rest/api/3/myself"
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", getAuthHeader())
            .addHeader("Accept", "application/json")
            .build()
        
        return try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            
            if (response.isSuccessful) {
                logger.info("Successfully connected to Jira")
                Result.success(true)
            } else {
                val errorMessage = "Failed to connect to Jira. Status: ${response.code}, Body: $responseBody"
                logger.error(errorMessage)
                Result.failure(RuntimeException(errorMessage))
            }
        } catch (e: IOException) {
            logger.error("Network error while testing Jira connection", e)
            Result.failure(e)
        } catch (e: Exception) {
            logger.error("Unexpected error while testing Jira connection", e)
            Result.failure(e)
        }
    }
    
    private fun buildCreateTicketRequest(testFailure: TestFailure): JsonObject {
        val request = JsonObject()
        
        // Fields object
        val fields = JsonObject()
        
        // Project
        val project = JsonObject()
        project.addProperty("key", settings.projectKey)
        fields.add("project", project)
        
        // Issue type
        val issueType = JsonObject()
        issueType.addProperty("name", settings.defaultIssueType)
        fields.add("issuetype", issueType)
        
        // Summary
        fields.addProperty("summary", generateSummary(testFailure))
        
        // Description
        fields.addProperty("description", generateDescription(testFailure))
        
        // Priority
        val priority = JsonObject()
        priority.addProperty("name", settings.defaultPriority)
        fields.add("priority", priority)
        
        // Assignee (if configured)
        if (settings.defaultAssignee.isNotBlank()) {
            fields.addProperty("assignee", settings.defaultAssignee)
        }
        
        // Labels
        val labels = com.google.gson.JsonArray()
        labels.add("test-automation")
        labels.add("auto-generated")
        labels.add(testFailure.testFramework.name.lowercase())
        fields.add("labels", labels)
        
        request.add("fields", fields)
        return request
    }
    
    private fun buildCreateTicketRequest(requestBody: JsonObject): Request {
        val url = "${settings.getJiraBaseUrl()}/rest/api/3/issue"
        val mediaType = "application/json".toMediaType()
        val body = requestBody.toString().toRequestBody(mediaType)
        
        return Request.Builder()
            .url(url)
            .post(body)
            .addHeader("Authorization", getAuthHeader())
            .addHeader("Accept", "application/json")
            .addHeader("Content-Type", "application/json")
            .build()
    }
    
    private fun getAuthHeader(): String {
        val credentials = "${settings.email}:${settings.apiToken}"
        val encoded = java.util.Base64.getEncoder().encodeToString(credentials.toByteArray())
        return "Basic $encoded"
    }
    
    private fun generateSummary(testFailure: TestFailure): String {
        return "[Test Failure] ${testFailure.testName} - ${testFailure.errorMessage.take(100)}"
    }
    
    private fun generateDescription(testFailure: TestFailure): String {
        val description = StringBuilder()
        
        // Use AI-generated description if available
        val aiService = com.jira.testautomation.services.AIService.getInstance()
        val settings = com.jira.testautomation.settings.JiraSettings.getInstance()
        
        if (settings.isAIConfigured()) {
            try {
                val aiDescription = aiService.generateTicketDescription(testFailure)
                description.append(aiDescription)
                description.append("\n\n")
            } catch (e: Exception) {
                logger.warn("Failed to generate AI description, using basic", e)
            }
        }
        
        // Fallback to basic description
        if (description.isEmpty()) {
            description.append("h2. Test Failure Details\n\n")
            description.append("*Test:* ${testFailure.getTestIdentifier()}\n")
            description.append("*Framework:* ${testFailure.testFramework}\n")
            description.append("*Failure Time:* ${testFailure.failureTime}\n")
            description.append("*Project:* ${testFailure.projectName}\n")
            if (testFailure.moduleName != null) {
                description.append("*Module:* ${testFailure.moduleName}\n")
            }
            if (testFailure.testDuration != null) {
                description.append("*Duration:* ${testFailure.testDuration}ms\n")
            }
        }
        
        description.append("\nh3. Error Message\n")
        description.append("{code:java}\n")
        description.append(testFailure.errorMessage)
        description.append("\n{code}\n")
        
        description.append("\nh3. Stack Trace\n")
        description.append("{code:java}\n")
        description.append(testFailure.stackTrace)
        description.append("\n{code}\n")
        
        if (testFailure.additionalLogs != null) {
            description.append("\nh3. Additional Logs\n")
            description.append("{code}\n")
            description.append(testFailure.additionalLogs)
            description.append("\n{code}\n")
        }
        
        description.append("\nh3. Auto-Generated Ticket\n")
        description.append("This ticket was automatically created by the Jira Test Automation plugin.\n")
        description.append("Test Failure ID: ${testFailure.generateId()}")
        
        return description.toString()
    }
    
    /**
     * Response model for Jira ticket creation
     */
    data class JiraTicketResponse(
        val id: String,
        val key: String,
        val self: String
    )
}

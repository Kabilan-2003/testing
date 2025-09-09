package com.jira.plugin.services

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.jira.plugin.settings.JiraSettings
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * AI service for enhancing test failure descriptions and analysis
 * Phase 2: Full AI implementation with OpenAI integration
 */
@Service
class AIService {
    
    private val logger = thisLogger()
    private val settings = JiraSettings.getInstance()
    private val gson = Gson()
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
    
    companion object {
        fun getInstance(): AIService {
            return ApplicationManager.getApplication().getService(AIService::class.java)
        }
    }
    
    /**
     * Enhance test failure description with AI analysis
     */
    fun enhanceFailureDescription(testFailure: com.jira.testautomation.models.TestFailure): String {
        if (!settings.isAIConfigured()) {
            return generateBasicDescription(testFailure)
        }
        
        return try {
            val prompt = buildEnhancementPrompt(testFailure)
            val response = callOpenAI(prompt)
            parseEnhancementResponse(response)
        } catch (e: Exception) {
            logger.warn("AI enhancement failed, using basic description", e)
            generateBasicDescription(testFailure)
        }
    }
    
    /**
     * Predict severity of test failure using AI
     */
    fun predictSeverity(testFailure: com.jira.testautomation.models.TestFailure): String {
        if (!settings.isAIConfigured()) {
            return predictSeverityRuleBased(testFailure)
        }
        
        return try {
            val prompt = buildSeverityPrompt(testFailure)
            val response = callOpenAI(prompt)
            parseSeverityResponse(response)
        } catch (e: Exception) {
            logger.warn("AI severity prediction failed, using rule-based", e)
            predictSeverityRuleBased(testFailure)
        }
    }
    
    /**
     * Detect if this failure is a duplicate using AI
     */
    fun isDuplicate(testFailure: com.jira.testautomation.models.TestFailure, existingFailures: List<com.jira.testautomation.models.TestFailure>): Boolean {
        if (!settings.isAIConfigured() || existingFailures.isEmpty()) {
            return false
        }
        
        return try {
            val prompt = buildDuplicateDetectionPrompt(testFailure, existingFailures)
            val response = callOpenAI(prompt)
            parseDuplicateResponse(response)
        } catch (e: Exception) {
            logger.warn("AI duplicate detection failed", e)
            false
        }
    }
    
    /**
     * Suggest root cause for the test failure using AI
     */
    fun suggestRootCause(testFailure: com.jira.testautomation.models.TestFailure): String {
        if (!settings.isAIConfigured()) {
            return suggestRootCauseRuleBased(testFailure)
        }
        
        return try {
            val prompt = buildRootCausePrompt(testFailure)
            val response = callOpenAI(prompt)
            parseRootCauseResponse(response)
        } catch (e: Exception) {
            logger.warn("AI root cause analysis failed, using rule-based", e)
            suggestRootCauseRuleBased(testFailure)
        }
    }
    
    /**
     * Generate comprehensive ticket description with AI
     */
    fun generateTicketDescription(testFailure: com.jira.testautomation.models.TestFailure): String {
        if (!settings.isAIConfigured()) {
            return generateBasicTicketDescription(testFailure)
        }
        
        return try {
            val prompt = buildTicketDescriptionPrompt(testFailure)
            val response = callOpenAI(prompt)
            parseTicketDescriptionResponse(response)
        } catch (e: Exception) {
            logger.warn("AI ticket description generation failed, using basic", e)
            generateBasicTicketDescription(testFailure)
        }
    }
    
    /**
     * Cluster similar test failures using AI
     */
    fun clusterFailures(failures: List<com.jira.testautomation.models.TestFailure>): Map<String, List<com.jira.testautomation.models.TestFailure>> {
        if (!settings.isAIConfigured() || failures.size < 2) {
            return mapOf("single" to failures)
        }
        
        return try {
            val prompt = buildClusteringPrompt(failures)
            val response = callOpenAI(prompt)
            parseClusteringResponse(response, failures)
        } catch (e: Exception) {
            logger.warn("AI clustering failed, using single cluster", e)
            mapOf("single" to failures)
        }
    }
    
    // Private helper methods for AI integration
    
    private fun callOpenAI(prompt: String): String {
        val requestBody = buildOpenAIRequest(prompt)
        val request = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .post(requestBody)
            .addHeader("Authorization", "Bearer ${settings.aiApiKey}")
            .addHeader("Content-Type", "application/json")
            .build()
        
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string()
        
        if (!response.isSuccessful || responseBody == null) {
            throw IOException("OpenAI API call failed: ${response.code} - $responseBody")
        }
        
        val jsonResponse = gson.fromJson(responseBody, JsonObject::class.java)
        return jsonResponse.getAsJsonArray("choices")
            .get(0).asJsonObject
            .getAsJsonObject("message")
            .get("content").asString
    }
    
    private fun buildOpenAIRequest(prompt: String): RequestBody {
        val request = JsonObject()
        request.addProperty("model", settings.aiModel)
        request.addProperty("temperature", 0.3)
        request.addProperty("max_tokens", 1000)
        
        val messages = com.google.gson.JsonArray()
        val message = JsonObject()
        message.addProperty("role", "user")
        message.addProperty("content", prompt)
        messages.add(message)
        
        request.add("messages", messages)
        
        val mediaType = "application/json".toMediaType()
        return request.toString().toRequestBody(mediaType)
    }
    
    // Prompt building methods
    
    private fun buildEnhancementPrompt(testFailure: com.jira.testautomation.models.TestFailure): String {
        return """
            Analyze this test failure and provide an enhanced description for a Jira ticket:
            
            Test: ${testFailure.getTestIdentifier()}
            Framework: ${testFailure.testFramework}
            Error: ${testFailure.errorMessage}
            Stack Trace: ${testFailure.stackTrace.take(500)}
            
            Please provide:
            1. A clear, concise summary of what failed
            2. The likely impact on the system
            3. Suggested next steps for investigation
            
            Keep the response professional and technical, suitable for a bug report.
        """.trimIndent()
    }
    
    private fun buildSeverityPrompt(testFailure: com.jira.testautomation.models.TestFailure): String {
        return """
            Analyze this test failure and determine its severity level (Critical, High, Medium, Low):
            
            Test: ${testFailure.getTestIdentifier()}
            Error: ${testFailure.errorMessage}
            Stack Trace: ${testFailure.stackTrace.take(300)}
            
            Consider:
            - Impact on system functionality
            - Frequency of occurrence
            - Business criticality
            - User experience impact
            
            Respond with only one word: Critical, High, Medium, or Low
        """.trimIndent()
    }
    
    private fun buildDuplicateDetectionPrompt(testFailure: com.jira.testautomation.models.TestFailure, existingFailures: List<com.jira.testautomation.models.TestFailure>): String {
        val existingErrors = existingFailures.take(5).joinToString("\n") { 
            "Test: ${it.getTestIdentifier()}, Error: ${it.errorMessage.take(100)}" 
        }
        
        return """
            Determine if this new test failure is a duplicate of existing ones:
            
            NEW FAILURE:
            Test: ${testFailure.getTestIdentifier()}
            Error: ${testFailure.errorMessage}
            
            EXISTING FAILURES:
            $existingErrors
            
            Respond with only: true or false
        """.trimIndent()
    }
    
    private fun buildRootCausePrompt(testFailure: com.jira.testautomation.models.TestFailure): String {
        return """
            Analyze this test failure and suggest the most likely root cause:
            
            Test: ${testFailure.getTestIdentifier()}
            Framework: ${testFailure.testFramework}
            Error: ${testFailure.errorMessage}
            Stack Trace: ${testFailure.stackTrace.take(500)}
            
            Provide a concise explanation of what likely went wrong and why.
            Focus on the technical root cause, not just symptoms.
        """.trimIndent()
    }
    
    private fun buildTicketDescriptionPrompt(testFailure: com.jira.testautomation.models.TestFailure): String {
        return """
            Create a comprehensive Jira ticket description for this test failure:
            
            Test: ${testFailure.getTestIdentifier()}
            Framework: ${testFailure.testFramework}
            Error: ${testFailure.errorMessage}
            Stack Trace: ${testFailure.stackTrace}
            Project: ${testFailure.projectName}
            Module: ${testFailure.moduleName ?: "N/A"}
            
            Format as Jira markup with:
            - Executive summary
            - Technical details
            - Steps to reproduce
            - Expected vs actual behavior
            - Environment information
            - Suggested investigation steps
        """.trimIndent()
    }
    
    private fun buildClusteringPrompt(failures: List<com.jira.testautomation.models.TestFailure>): String {
        val failureList = failures.take(10).joinToString("\n") { 
            "Test: ${it.getTestIdentifier()}, Error: ${it.errorMessage.take(100)}" 
        }
        
        return """
            Group these test failures into clusters based on similar root causes:
            
            $failureList
            
            Return a JSON object with cluster names as keys and arrays of test identifiers as values.
            Example: {"network_issues": ["Test1", "Test2"], "data_issues": ["Test3"]}
        """.trimIndent()
    }
    
    // Response parsing methods
    
    private fun parseEnhancementResponse(response: String): String {
        return response.trim()
    }
    
    private fun parseSeverityResponse(response: String): String {
        val severity = response.trim().lowercase()
        return when {
            severity.contains("critical") -> "Critical"
            severity.contains("high") -> "High"
            severity.contains("medium") -> "Medium"
            severity.contains("low") -> "Low"
            else -> "Medium"
        }
    }
    
    private fun parseDuplicateResponse(response: String): Boolean {
        return response.trim().lowercase().contains("true")
    }
    
    private fun parseRootCauseResponse(response: String): String {
        return response.trim()
    }
    
    private fun parseTicketDescriptionResponse(response: String): String {
        return response.trim()
    }
    
    private fun parseClusteringResponse(response: String, failures: List<com.jira.testautomation.models.TestFailure>): Map<String, List<com.jira.testautomation.models.TestFailure>> {
        return try {
            val jsonResponse = gson.fromJson(response, JsonObject::class.java)
            val clusters = mutableMapOf<String, List<com.jira.testautomation.models.TestFailure>>()
            
            jsonResponse.keySet().forEach { clusterName ->
                val testIds = jsonResponse.getAsJsonArray(clusterName)
                val clusterFailures = failures.filter { failure ->
                    testIds.any { it.asString == failure.getTestIdentifier() }
                }
                clusters[clusterName] = clusterFailures
            }
            
            clusters
        } catch (e: Exception) {
            logger.warn("Failed to parse clustering response", e)
            mapOf("single" to failures)
        }
    }
    
    // Fallback methods for when AI is not available
    
    private fun predictSeverityRuleBased(testFailure: com.jira.testautomation.models.TestFailure): String {
        return when {
            testFailure.errorMessage.contains("timeout", ignoreCase = true) -> "High"
            testFailure.errorMessage.contains("connection", ignoreCase = true) -> "High"
            testFailure.errorMessage.contains("assertion", ignoreCase = true) -> "Medium"
            testFailure.errorMessage.contains("null", ignoreCase = true) -> "Medium"
            else -> "Low"
        }
    }
    
    private fun suggestRootCauseRuleBased(testFailure: com.jira.testautomation.models.TestFailure): String {
        return when {
            testFailure.errorMessage.contains("timeout", ignoreCase = true) -> 
                "Possible network timeout or slow response. Check server performance and network connectivity."
            testFailure.errorMessage.contains("connection", ignoreCase = true) -> 
                "Connection issue. Verify server is running and accessible."
            testFailure.errorMessage.contains("assertion", ignoreCase = true) -> 
                "Assertion failed. Check expected vs actual values in the test."
            testFailure.errorMessage.contains("null", ignoreCase = true) -> 
                "Null pointer exception. Check for uninitialized objects or missing data."
            else -> "Review the error message and stack trace for specific issues."
        }
    }
    
    private fun generateBasicDescription(testFailure: com.jira.testautomation.models.TestFailure): String {
        val sb = StringBuilder()
        sb.append("Test Failure: ${testFailure.getTestIdentifier()}\n")
        sb.append("Error: ${testFailure.errorMessage}\n")
        sb.append("Framework: ${testFailure.testFramework}\n")
        return sb.toString()
    }
    
    private fun generateBasicTicketDescription(testFailure: com.jira.testautomation.models.TestFailure): String {
        val sb = StringBuilder()
        sb.append("h2. Test Failure Details\n\n")
        sb.append("*Test:* ${testFailure.getTestIdentifier()}\n")
        sb.append("*Framework:* ${testFailure.testFramework}\n")
        sb.append("*Failure Time:* ${testFailure.failureTime}\n")
        sb.append("*Project:* ${testFailure.projectName}\n")
        if (testFailure.moduleName != null) {
            sb.append("*Module:* ${testFailure.moduleName}\n")
        }
        
        sb.append("\nh3. Error Message\n")
        sb.append("{code:java}\n")
        sb.append(testFailure.errorMessage)
        sb.append("\n{code}\n")
        
        sb.append("\nh3. Stack Trace\n")
        sb.append("{code:java}\n")
        sb.append(testFailure.stackTrace)
        sb.append("\n{code}\n")
        
        return sb.toString()
    }
}

package com.jira.testautomation.services

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.jira.testautomation.models.TestFailure
import com.jira.testautomation.models.JiraTicket
import com.jira.testautomation.settings.JiraSettings

/**
 * Main service for Jira integration
 */
@Service
class JiraIntegrationService {
    
    private val logger = thisLogger()
    private val settings = JiraSettings.getInstance()
    private val jiraApiService = JiraApiService()
    private val aiService = AIService.getInstance()
    private val storageService = TestFailureStorageService.getInstance()
    
    companion object {
        fun getInstance(): JiraIntegrationService {
            return ApplicationManager.getApplication().getService(JiraIntegrationService::class.java)
        }
    }
    
    /**
     * Handle a test failure - main entry point
     */
    fun handleTestFailure(testFailure: TestFailure) {
        logger.info("Handling test failure: ${testFailure.getTestIdentifier()}")
        
        if (!settings.autoDetectFailures) {
            logger.info("Auto-detection is disabled, skipping test failure")
            return
        }
        
        // Store the test failure and check for duplicates
        val storageResult = storageService.storeTestFailure(testFailure)
        
        when (storageResult) {
            is TestFailureStorageService.TestFailureStorageResult.Duplicate -> {
                logger.info("Duplicate failure detected, skipping ticket creation")
                showDuplicateNotification(testFailure)
                return
            }
            is TestFailureStorageService.TestFailureStorageResult.New -> {
                logger.info("New failure detected, proceeding with ticket creation")
            }
        }
        
        if (!settings.isJiraConfigured()) {
            logger.warn("Jira is not configured, cannot create ticket")
            showConfigurationRequiredDialog()
            return
        }
        
        // Show approval dialog if enabled
        if (settings.showApprovalDialog) {
            showApprovalDialog(testFailure)
        } else {
            createTicketDirectly(testFailure)
        }
    }
    
    private fun showApprovalDialog(testFailure: TestFailure) {
        val message = buildApprovalMessage(testFailure)
        val result = Messages.showYesNoDialog(
            message,
            "Create Jira Ticket?",
            "Create Ticket",
            "Skip",
            Messages.getQuestionIcon()
        )
        
        if (result == Messages.YES) {
            createTicketDirectly(testFailure)
        } else {
            logger.info("User declined to create ticket for: ${testFailure.getTestIdentifier()}")
        }
    }
    
    private fun buildApprovalMessage(testFailure: TestFailure): String {
        val sb = StringBuilder()
        sb.append("Test Failure Detected:\n\n")
        sb.append("Test: ${testFailure.getTestIdentifier()}\n")
        sb.append("Framework: ${testFailure.testFramework}\n")
        sb.append("Error: ${testFailure.errorMessage.take(100)}...\n\n")
        sb.append("Would you like to create a Jira ticket for this failure?")
        return sb.toString()
    }
    
    private fun createTicketDirectly(testFailure: TestFailure) {
        try {
            // Enhance description with AI if enabled
            val enhancedFailure = if (settings.isAIConfigured()) {
                enhanceWithAI(testFailure)
            } else {
                testFailure
            }
            
            val result = jiraApiService.createTicket(enhancedFailure)
            
            result.fold(
                onSuccess = { ticketResponse ->
                    logger.info("Successfully created Jira ticket: ${ticketResponse.key}")
                    storeTicketInfo(testFailure, ticketResponse)
                    showSuccessNotification(testFailure, ticketResponse)
                },
                onFailure = { error ->
                    logger.error("Failed to create Jira ticket", error)
                    showErrorNotification("Failed to create Jira ticket: ${error.message}")
                }
            )
        } catch (e: Exception) {
            logger.error("Unexpected error creating Jira ticket", e)
            showErrorNotification("Unexpected error: ${e.message}")
        }
    }
    
    private fun enhanceWithAI(testFailure: TestFailure): TestFailure {
        return try {
            // AI enhancement is now available in Phase 2
            val enhancedDescription = aiService.enhanceFailureDescription(testFailure)
            val severity = aiService.predictSeverity(testFailure)
            val rootCause = aiService.suggestRootCause(testFailure)
            
            // Create enhanced test failure with AI insights
            testFailure.copy(
                additionalLogs = buildAIInsights(enhancedDescription, severity, rootCause)
            )
        } catch (e: Exception) {
            logger.warn("AI enhancement failed, using original failure", e)
            testFailure
        }
    }
    
    private fun buildAIInsights(description: String, severity: String, rootCause: String): String {
        val sb = StringBuilder()
        sb.append("=== AI ANALYSIS ===\n")
        sb.append("Enhanced Description: $description\n\n")
        sb.append("Predicted Severity: $severity\n\n")
        sb.append("Suggested Root Cause: $rootCause\n")
        return sb.toString()
    }
    
    private fun storeTicketInfo(testFailure: TestFailure, ticketResponse: com.jira.testautomation.services.JiraApiService.JiraTicketResponse) {
        // Store ticket information for tracking
        val jiraTicket = JiraTicket(
            ticketId = ticketResponse.id,
            ticketKey = ticketResponse.key,
            ticketUrl = ticketResponse.self,
            testFailureId = testFailure.generateId()
        )
        storageService.storeJiraTicket(jiraTicket)
        logger.info("Stored ticket info: ${ticketResponse.key} for test: ${testFailure.getTestIdentifier()}")
    }
    
    private fun showDuplicateNotification(testFailure: TestFailure) {
        if (settings.showNotifications) {
            val message = "Duplicate test failure detected: ${testFailure.getTestIdentifier()}\nNo new ticket will be created."
            Messages.showInfoMessage(message, "Duplicate Detected")
        }
    }
    
    private fun showSuccessNotification(testFailure: TestFailure, ticketResponse: com.jira.testautomation.services.JiraApiService.JiraTicketResponse) {
        if (settings.showNotifications) {
            val message = "Jira ticket created: ${ticketResponse.key}\nTest: ${testFailure.getTestIdentifier()}"
            Messages.showInfoMessage(message, "Ticket Created")
        }
    }
    
    private fun showErrorNotification(message: String) {
        if (settings.showNotifications) {
            Messages.showErrorDialog(message, "Error Creating Ticket")
        }
    }
    
    private fun showConfigurationRequiredDialog() {
        val result = Messages.showYesNoDialog(
            "Jira is not configured. Would you like to open settings?",
            "Configuration Required",
            "Open Settings",
            "Cancel",
            Messages.getQuestionIcon()
        )
        
        if (result == Messages.YES) {
            // Open settings - this will be implemented with proper settings navigation
            logger.info("User requested to open settings")
        }
    }
    
    /**
     * Test Jira connection
     */
    fun testConnection(): Result<Boolean> {
        return jiraApiService.testConnection()
    }
}

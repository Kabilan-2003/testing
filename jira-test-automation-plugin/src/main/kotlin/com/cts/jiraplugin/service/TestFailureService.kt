package com.cts.jiraplugin.service

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.cts.jiraplugin.model.JiraIssue
import com.cts.jiraplugin.model.TestFailureDetails
import com.cts.jiraplugin.settings.JiraSettings
import com.cts.jiraplugin.ui.JiraTicketDialog
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage

/**
 * Service responsible for handling test failures and creating Jira tickets.
 */
@Service(Service.Level.PROJECT)
class TestFailureService(private val project: Project) {
    private val settings = JiraSettings.getInstance()
    private var jiraService: JiraService? = null

    init {
        initializeJiraService()
    }

    companion object {
        @JvmStatic
        fun getInstance(project: Project): TestFailureService {
            return project.service()
        }
    }

    private fun initializeJiraService() {
        if (settings.jiraUrl.isNotBlank() && settings.email.isNotBlank() && settings.apiToken.isNotBlank()) {
            jiraService = JiraService(settings.jiraUrl, settings.email, settings.apiToken)
        }
    }

    /**
     * Handles a test failure by either automatically creating a Jira ticket
     * or showing a dialog to the user based on the plugin settings.
     */
    fun handleTestFailure(failureDetails: TestFailureDetails) {
        if (!isConfigured()) {
            showNotification(
                "Jira Test Automation",
                "Jira integration is not configured. Please configure it in Settings > Tools > Jira Test Automation",
                NotificationType.WARNING
            )
            return
        }

        if (settings.autoCreateTickets) {
            createJiraTicket(failureDetails)
        } else {
            showTicketCreationDialog(failureDetails)
        }
    }

    private fun showTicketCreationDialog(failureDetails: TestFailureDetails) {
        JiraTicketDialog(project, failureDetails, { summary, description ->
            createJiraTicket(failureDetails, summary, description)
        }).show()
    }

    /**
     * Creates a Jira ticket for the given test failure.
     * 
     * @param failureDetails Details about the test failure
     * @param customSummary Optional custom summary for the Jira ticket
     * @param customDescription Optional custom description for the Jira ticket
     */
    private fun createJiraTicket(
        failureDetails: TestFailureDetails,
        customSummary: String? = null,
        customDescription: String? = null
    ) {
        val summary = customSummary ?: "Test Failed: ${failureDetails.testName}"
        val description = customDescription ?: """
            ||Test Name||${failureDetails.testName}|
            |Class|${failureDetails.className}|
            |Error Message|${failureDetails.errorMessage}|
            |Test Run ID|${failureDetails.testRunId}|
            
            Stack Trace:
            ${failureDetails.stackTrace}
        """.trimMargin()

        val issue = JiraIssue(
            summary = summary,
            description = description,
            projectKey = settings.projectKey,
            issueType = "Bug" // Default issue type
        )

        try {
            jiraService?.createIssue(issue)?.whenComplete { issueKey, error ->
                if (error == null && issueKey != null) {
                    showNotification(
                        "Jira Ticket Created",
                        "Created ticket: $issueKey for test failure: ${failureDetails.testName}",
                        NotificationType.INFORMATION
                    )
                    // TODO: Store the mapping between test failure and Jira issue
                } else {
                    showNotification(
                        "Failed to Create Jira Ticket",
                        "Error: ${error?.message ?: "Unknown error"}",
                        NotificationType.ERROR
                    )
                }
            }
        } catch (e: Exception) {
            showNotification(
                "Error",
                "Failed to create Jira ticket: ${e.message}",
                NotificationType.ERROR
            )
        }
    }

    /**
     * Checks if the Jira integration is properly configured.
     */
    private fun isConfigured(): Boolean {
        return settings.jiraUrl.isNotBlank() && 
               settings.email.isNotBlank() && 
               settings.apiToken.isNotBlank() && 
               settings.projectKey.isNotBlank()
    }

    private fun showNotification(title: String, content: String, type: NotificationType) {
        Notifications.Bus.notify(
            Notification("JiraTestAutomation", title, content, type),
            project
        )
    }
}

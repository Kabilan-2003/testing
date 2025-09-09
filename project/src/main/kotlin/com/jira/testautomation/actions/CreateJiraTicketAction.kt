package com.jira.testautomation.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.Messages
import com.jira.testautomation.services.JiraIntegrationService
import com.jira.testautomation.settings.JiraSettings

/**
 * Action to create a Jira ticket from the current context
 */
class CreateJiraTicketAction : AnAction(), DumbAware {
    
    private val logger = thisLogger()
    private val settings = JiraSettings.getInstance()
    private val jiraService = JiraIntegrationService.getInstance()
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        
        if (!settings.isJiraConfigured()) {
            Messages.showErrorDialog(
                "Jira is not configured. Please configure Jira settings first.",
                "Configuration Required"
            )
            return
        }
        
        // For now, show a placeholder message
        // In a real implementation, this would get the current test failure context
        Messages.showInfoMessage(
            "This feature will create a Jira ticket from the selected test failure.\n" +
            "Currently in development for Phase 1 MVP.",
            "Create Jira Ticket"
        )
    }
    
    override fun update(e: AnActionEvent) {
        val project = e.project
        e.presentation.isEnabledAndVisible = project != null && settings.isJiraConfigured()
    }
}

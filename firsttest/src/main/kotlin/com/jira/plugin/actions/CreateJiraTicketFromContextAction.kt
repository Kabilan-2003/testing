package com.jira.plugin.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.Messages
import com.jira.plugin.settings.JiraSettings

/**
 * Action to create a Jira ticket from test context (right-click menu)
 */
class CreateJiraTicketFromContextAction : AnAction(), DumbAware {
    
    private val logger = thisLogger()
    private val settings = JiraSettings.getInstance()
    
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
        // In a real implementation, this would extract test failure from context
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

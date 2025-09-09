package com.cts.jiraplugin.ui

import com.cts.jiraplugin.auth.OAuthManager
import com.cts.jiraplugin.ui.EnhancedPluginPanel
import com.cts.jiraplugin.ui.LoginDialog
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.wm.ToolWindowManager

private fun getEnhancedPluginPanel(e: AnActionEvent): EnhancedPluginPanel? {
    val project = e.project ?: return null
    val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Jira Test Failures") ?: return null
    val content = toolWindow.contentManager.selectedContent ?: return null
    return content.component as? EnhancedPluginPanel
}

class LoginAction : AnAction("Login", "Login to Jira", null) {
    override fun actionPerformed(e: AnActionEvent) {
        val dialog = LoginDialog()
        dialog.show()
    }
}

class LogoutAction : AnAction("Logout", "Logout from Jira", null) {
    override fun actionPerformed(e: AnActionEvent) {
        OAuthManager.logout()
        getEnhancedPluginPanel(e)?.showLoginPrompt()
    }
}

class RefreshAction : AnAction("Refresh", "Refresh data", null) {
    override fun actionPerformed(e: AnActionEvent) {
        getEnhancedPluginPanel(e)?.refresh()
    }
}

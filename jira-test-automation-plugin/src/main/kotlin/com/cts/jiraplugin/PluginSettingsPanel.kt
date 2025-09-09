package com.cts.jiraplugin

import com.cts.jiraplugin.auth.OAuthManager
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.panel
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel

class PluginSettingsPanel {
    private val jiraUrlField = JBTextField()
    private val projectKeyField = JBTextField()
    private val loginButton = JButton("Login with Atlassian")
    private val statusLabel = JLabel()

    val panel: JPanel = panel {
        row("Jira URL:") {
            cell(jiraUrlField)
        }
        row("Project Key:") {
            cell(projectKeyField)
        }
        row {
            cell(loginButton)
        }
        row {
            cell(statusLabel)
        }
    }

    init {
        loadSettings()
        loginButton.addActionListener {
            OAuthManager.startLoginFlow()
            updateStatusLabel()
        }
        updateStatusLabel()
    }

    private fun loadSettings() {
        val settings = com.cts.jiraplugin.PluginSettings.getInstance()
        jiraUrlField.text = settings.jiraUrl
        projectKeyField.text = settings.projectKey
    }

    private fun updateStatusLabel() {
        val settings = com.cts.jiraplugin.PluginSettings.getInstance()
        statusLabel.text = if (settings.accessToken.isNullOrBlank()) "❌ Not logged in" else "✅ Logged in"
    }

    fun isModified(): Boolean {
        val settings = com.cts.jiraplugin.PluginSettings.getInstance()
        return jiraUrlField.text != settings.jiraUrl ||
                projectKeyField.text != settings.projectKey
    }

    fun apply() {
        PluginSettings.getInstance().saveSettings(
            jiraUrlField.text,
            projectKeyField.text
        )
        updateStatusLabel()
    }

    fun reset() {
        loadSettings()
        updateStatusLabel()
    }
}

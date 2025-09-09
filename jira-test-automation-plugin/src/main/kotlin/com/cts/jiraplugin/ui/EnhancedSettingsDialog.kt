package com.cts.jiraplugin.ui

import com.cts.jiraplugin.PluginSettings
import com.cts.jiraplugin.auth.OAuthManager
import com.intellij.openapi.ui.DialogWrapper
import java.awt.BorderLayout
import java.awt.GridLayout
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JPasswordField
import javax.swing.JButton
import javax.swing.JTextField

class EnhancedSettingsDialog : DialogWrapper(true) {
    private val jiraUrlField = JTextField()
    private val projectKeyField = JTextField()
    private val emailField = JTextField()
    private val apiTokenField = JPasswordField()
    private val useOAuthCheckbox = JCheckBox("Use OAuth 2.0 Authentication")
    private val loginButton = JButton("Login with Atlassian")

    init {
        init()
        title = "Jira Plugin Settings"

        // Load existing settings
        val settings = PluginSettings
        jiraUrlField.text = settings.jiraUrl ?: ""
        projectKeyField.text = settings.projectKey ?: ""
        emailField.text = settings.email ?: ""
        apiTokenField.text = settings.apiToken ?: ""
        useOAuthCheckbox.isSelected = settings.useOAuth

        // Add listener to toggle between OAuth and API token
        useOAuthCheckbox.addActionListener {
            val useOAuth = useOAuthCheckbox.isSelected
            emailField.isEnabled = !useOAuth
            apiTokenField.isEnabled = !useOAuth
            loginButton.isEnabled = useOAuth
        }

        // Set initial state
        emailField.isEnabled = !useOAuthCheckbox.isSelected
        apiTokenField.isEnabled = !useOAuthCheckbox.isSelected
        loginButton.isEnabled = useOAuthCheckbox.isSelected

        // Add login button action
        loginButton.addActionListener {
            OAuthManager.initiateOAuthFlow()
        }
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout(5, 5))

        // Main settings panel
        val settingsPanel = JPanel(GridLayout(0, 2, 5, 5))

        settingsPanel.add(JLabel("Jira URL:"))
        settingsPanel.add(jiraUrlField)

        settingsPanel.add(JLabel("Project Key:"))
        settingsPanel.add(projectKeyField)

        settingsPanel.add(JLabel("Authentication:"))
        settingsPanel.add(useOAuthCheckbox)

        settingsPanel.add(JLabel("Email:"))
        settingsPanel.add(emailField)

        settingsPanel.add(JLabel("API Token:"))
        settingsPanel.add(apiTokenField)

        settingsPanel.add(JLabel("")) // Empty cell
        settingsPanel.add(loginButton)

        panel.add(settingsPanel, BorderLayout.CENTER)

        return panel
    }

    override fun doOKAction() {
        PluginSettings.saveSettings(
            jiraUrlField.text,
            projectKeyField.text,
            emailField.text,
            String(apiTokenField.password),
            useOAuthCheckbox.isSelected
        )
        super.doOKAction()
    }
}
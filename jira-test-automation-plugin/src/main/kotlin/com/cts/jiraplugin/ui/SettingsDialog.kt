package com.cts.jiraplugin.ui

import com.intellij.openapi.ui.DialogWrapper
import com.cts.jiraplugin.PluginSettings
import java.awt.GridLayout
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JPasswordField
import javax.swing.JTextField

class SettingsDialog : DialogWrapper(true) {
    private val jiraUrlField = JTextField()
    private val projectKeyField = JTextField()
    private val emailField = JTextField()
    private val apiTokenField = JPasswordField()
    private val useOAuthCheckbox = JCheckBox("Use OAuth 2.0")

    init {
        init()
        title = "Jira Plugin Settings"

        // Load existing settings
        val settings = PluginSettings.getInstance()
        jiraUrlField.text = settings.jiraUrl
        projectKeyField.text = settings.projectKey
        emailField.text = settings.email
        apiTokenField.text = settings.apiToken
        useOAuthCheckbox.isSelected = settings.useOAuth
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(GridLayout(0, 2, 5, 5))

        panel.add(JLabel("Jira URL:"))
        panel.add(jiraUrlField)

        panel.add(JLabel("Project Key:"))
        panel.add(projectKeyField)

        panel.add(JLabel("Email:"))
        panel.add(emailField)

        panel.add(JLabel("API Token:"))
        panel.add(apiTokenField)

        panel.add(useOAuthCheckbox)

        return panel
    }

    override fun doOKAction() {
        PluginSettings.getInstance().saveSettings(
            jiraUrlField.text,
            projectKeyField.text,
            emailField.text,
            String(apiTokenField.password),
            useOAuthCheckbox.isSelected
        )
        super.doOKAction()
    }
}
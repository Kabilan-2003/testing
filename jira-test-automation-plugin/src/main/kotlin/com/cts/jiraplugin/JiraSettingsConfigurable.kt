package com.cts.jiraplugin.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import java.awt.*
import javax.swing.*

class JiraSettingsConfigurable(private val project: Project) : Configurable {
    private var mainPanel: JPanel? = null
    private lateinit var jiraUrlField: JBTextField
    private lateinit var emailField: JBTextField
    private lateinit var apiTokenField: JBPasswordField
    private lateinit var projectKeyField: JBTextField
    private lateinit var autoCreateCheckbox: JCheckBox

    private val settings = JiraSettings.getInstance()

    init {
        createUI()
    }

    private fun createUI() {
        jiraUrlField = JBTextField(settings.jiraUrl, 40)
        emailField = JBTextField(settings.email, 40)
        apiTokenField = JBPasswordField()
        apiTokenField.text = settings.apiToken
        projectKeyField = JBTextField(settings.projectKey, 40)
        autoCreateCheckbox = JCheckBox("Automatically create tickets for test failures", settings.autoCreateTickets)

        mainPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent("Jira URL:", jiraUrlField)
            .addLabeledComponent("Email:", emailField)
            .addLabeledComponent("API Token:", apiTokenField)
            .addLabeledComponent("Project Key:", projectKeyField)
            .addComponent(autoCreateCheckbox)
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }

    override fun createComponent(): JComponent? {
        return mainPanel
    }

    override fun isModified(): Boolean {
        return jiraUrlField.text != settings.jiraUrl ||
                emailField.text != settings.email ||
                String(apiTokenField.password) != settings.apiToken ||
                projectKeyField.text != settings.projectKey ||
                autoCreateCheckbox.isSelected != settings.autoCreateTickets
    }

    override fun apply() {
        settings.jiraUrl = jiraUrlField.text.trim()
        settings.email = emailField.text.trim()
        settings.apiToken = String(apiTokenField.password)
        settings.projectKey = projectKeyField.text.trim()
        settings.autoCreateTickets = autoCreateCheckbox.isSelected
    }

    override fun reset() {
        jiraUrlField.text = settings.jiraUrl
        emailField.text = settings.email
        apiTokenField.text = settings.apiToken
        projectKeyField.text = settings.projectKey
        autoCreateCheckbox.isSelected = settings.autoCreateTickets
    }

    override fun getDisplayName(): String = "Jira Test Automation"

    override fun disposeUIResources() {
        // Cleanup resources if needed
    }
}

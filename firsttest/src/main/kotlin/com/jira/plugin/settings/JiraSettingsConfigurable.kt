package com.jira.plugin.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * Settings UI for Jira Test Automation plugin
 */
class JiraSettingsConfigurable : Configurable {
    
    private val settings = JiraSettings.getInstance()
    private val panel: JPanel = JPanel()
    
    // Jira Configuration Fields
    private val jiraUrlField = JBTextField(settings.jiraUrl)
    private val projectKeyField = JBTextField(settings.projectKey)
    private val emailField = JBTextField(settings.email)
    private val apiTokenField = JBPasswordField()
    
    // Plugin Configuration Fields
    private val autoDetectCheckBox = JBCheckBox("Auto-detect test failures", settings.autoDetectFailures)
    private val showApprovalCheckBox = JBCheckBox("Show approval dialog before creating tickets", settings.showApprovalDialog)
    private val defaultAssigneeField = JBTextField(settings.defaultAssignee)
    private val defaultPriorityCombo = ComboBox(arrayOf("Lowest", "Low", "Medium", "High", "Highest"))
    private val defaultIssueTypeCombo = ComboBox(arrayOf("Bug", "Task", "Story", "Epic"))
    
    // AI Configuration Fields
    private val enableAICheckBox = JBCheckBox("Enable AI features", settings.enableAI)
    private val aiProviderCombo = ComboBox(arrayOf("OpenAI", "Local"))
    private val aiApiKeyField = JBPasswordField()
    private val aiModelField = JBTextField(settings.aiModel)
    
    // Notification Fields
    private val showNotificationsCheckBox = JBCheckBox("Show notifications", settings.showNotifications)
    private val notificationSoundCheckBox = JBCheckBox("Play notification sound", settings.notificationSound)
    
    init {
        // Set initial values
        apiTokenField.text = settings.apiToken
        aiApiKeyField.text = settings.aiApiKey
        defaultPriorityCombo.selectedItem = settings.defaultPriority
        defaultIssueTypeCombo.selectedItem = settings.defaultIssueType
        aiProviderCombo.selectedItem = if (settings.aiProvider == "openai") "OpenAI" else "Local"
        
        // Enable/disable AI fields based on checkbox
        enableAICheckBox.addActionListener {
            val enabled = enableAICheckBox.isSelected
            aiProviderCombo.isEnabled = enabled
            aiApiKeyField.isEnabled = enabled
            aiModelField.isEnabled = enabled
        }
        
        // Initial state
        val aiEnabled = enableAICheckBox.isSelected
        aiProviderCombo.isEnabled = aiEnabled
        aiApiKeyField.isEnabled = aiEnabled
        aiModelField.isEnabled = aiEnabled
    }
    
    override fun getDisplayName(): String = "Jira Test Automation"
    
    override fun createComponent(): JComponent {
        return FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("Jira URL:"), jiraUrlField, 1, false)
            .addLabeledComponent(JBLabel("Project Key:"), projectKeyField, 1, false)
            .addLabeledComponent(JBLabel("Email:"), emailField, 1, false)
            .addLabeledComponent(JBLabel("API Token:"), apiTokenField, 1, false)
            .addSeparator(1)
            .addComponent(JBLabel("<html><b>Plugin Configuration</b></html>"), 1)
            .addComponent(autoDetectCheckBox, 1)
            .addComponent(showApprovalCheckBox, 1)
            .addLabeledComponent(JBLabel("Default Assignee:"), defaultAssigneeField, 1, false)
            .addLabeledComponent(JBLabel("Default Priority:"), defaultPriorityCombo, 1, false)
            .addLabeledComponent(JBLabel("Default Issue Type:"), defaultIssueTypeCombo, 1, false)
            .addSeparator(1)
            .addComponent(JBLabel("<html><b>AI Configuration</b></html>"), 1)
            .addComponent(enableAICheckBox, 1)
            .addLabeledComponent(JBLabel("AI Provider:"), aiProviderCombo, 1, false)
            .addLabeledComponent(JBLabel("AI API Key:"), aiApiKeyField, 1, false)
            .addLabeledComponent(JBLabel("AI Model:"), aiModelField, 1, false)
            .addSeparator(1)
            .addComponent(JBLabel("<html><b>Notifications</b></html>"), 1)
            .addComponent(showNotificationsCheckBox, 1)
            .addComponent(notificationSoundCheckBox, 1)
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }
    
    override fun isModified(): Boolean {
        return jiraUrlField.text != settings.jiraUrl ||
               projectKeyField.text != settings.projectKey ||
               emailField.text != settings.email ||
               String(apiTokenField.password) != settings.apiToken ||
               autoDetectCheckBox.isSelected != settings.autoDetectFailures ||
               showApprovalCheckBox.isSelected != settings.showApprovalDialog ||
               defaultAssigneeField.text != settings.defaultAssignee ||
               defaultPriorityCombo.selectedItem != settings.defaultPriority ||
               defaultIssueTypeCombo.selectedItem != settings.defaultIssueType ||
               enableAICheckBox.isSelected != settings.enableAI ||
               (if (aiProviderCombo.selectedItem == "OpenAI") "openai" else "local") != settings.aiProvider ||
               String(aiApiKeyField.password) != settings.aiApiKey ||
               aiModelField.text != settings.aiModel ||
               showNotificationsCheckBox.isSelected != settings.showNotifications ||
               notificationSoundCheckBox.isSelected != settings.notificationSound
    }
    
    override fun apply() {
        settings.jiraUrl = jiraUrlField.text
        settings.projectKey = projectKeyField.text
        settings.email = emailField.text
        settings.apiToken = String(apiTokenField.password)
        settings.autoDetectFailures = autoDetectCheckBox.isSelected
        settings.showApprovalDialog = showApprovalCheckBox.isSelected
        settings.defaultAssignee = defaultAssigneeField.text
        settings.defaultPriority = defaultPriorityCombo.selectedItem as String
        settings.defaultIssueType = defaultIssueTypeCombo.selectedItem as String
        settings.enableAI = enableAICheckBox.isSelected
        settings.aiProvider = if (aiProviderCombo.selectedItem == "OpenAI") "openai" else "local"
        settings.aiApiKey = String(aiApiKeyField.password)
        settings.aiModel = aiModelField.text
        settings.showNotifications = showNotificationsCheckBox.isSelected
        settings.notificationSound = notificationSoundCheckBox.isSelected
    }
    
    override fun reset() {
        jiraUrlField.text = settings.jiraUrl
        projectKeyField.text = settings.projectKey
        emailField.text = settings.email
        apiTokenField.text = settings.apiToken
        autoDetectCheckBox.isSelected = settings.autoDetectFailures
        showApprovalCheckBox.isSelected = settings.showApprovalDialog
        defaultAssigneeField.text = settings.defaultAssignee
        defaultPriorityCombo.selectedItem = settings.defaultPriority
        defaultIssueTypeCombo.selectedItem = settings.defaultIssueType
        enableAICheckBox.isSelected = settings.enableAI
        aiProviderCombo.selectedItem = if (settings.aiProvider == "openai") "OpenAI" else "Local"
        aiApiKeyField.text = settings.aiApiKey
        aiModelField.text = settings.aiModel
        showNotificationsCheckBox.isSelected = settings.showNotifications
        notificationSoundCheckBox.isSelected = settings.notificationSound
    }
}

package com.jira.plugin.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

/**
 * Persistent settings for Jira integration
 */
@Service
@State(name = "JiraTestAutomationSettings", storages = [Storage("jira-test-automation.xml")])
class JiraSettings : PersistentStateComponent<JiraSettings> {
    
    // Jira Configuration
    var jiraUrl: String = ""
    var projectKey: String = ""
    var email: String = ""
    var apiToken: String = ""
    
    // Plugin Configuration
    var autoDetectFailures: Boolean = true
    var showApprovalDialog: Boolean = true
    var defaultAssignee: String = ""
    var defaultPriority: String = "Medium"
    var defaultIssueType: String = "Bug"
    
    // AI Configuration
    var enableAI: Boolean = false
    var aiProvider: String = "openai" // openai, local
    var aiApiKey: String = ""
    var aiModel: String = "gpt-3.5-turbo"
    
    // Notification Settings
    var showNotifications: Boolean = true
    var notificationSound: Boolean = false
    
    companion object {
        fun getInstance(): JiraSettings {
            return ApplicationManager.getApplication().getService(JiraSettings::class.java)
        }
    }
    
    override fun getState(): JiraSettings {
        return this
    }
    
    override fun loadState(state: JiraSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }
    
    /**
     * Validate if all required Jira settings are configured
     */
    fun isJiraConfigured(): Boolean {
        return jiraUrl.isNotBlank() && 
               projectKey.isNotBlank() && 
               email.isNotBlank() && 
               apiToken.isNotBlank()
    }
    
    /**
     * Validate if AI settings are configured
     */
    fun isAIConfigured(): Boolean {
        return enableAI && aiApiKey.isNotBlank()
    }
    
    /**
     * Get the base Jira URL without trailing slash
     */
    fun getJiraBaseUrl(): String {
        return jiraUrl.trimEnd('/')
    }
}

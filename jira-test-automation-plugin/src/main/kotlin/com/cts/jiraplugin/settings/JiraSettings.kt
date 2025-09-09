package com.cts.jiraplugin.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable

@State(
    name = "JiraTestAutomationSettings",
    storages = [Storage("JiraTestAutomationPlugin.xml")],
    reportStatistic = true
)
class JiraSettings : PersistentStateComponent<JiraSettings> {
    var jiraUrl: String = ""
    var email: String = ""
    var apiToken: String = ""
    var projectKey: String = ""
    var autoCreateTickets: Boolean = false

    @Nullable
    override fun getState(): JiraSettings {
        return this
    }

    override fun loadState(@NotNull state: JiraSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }

    override fun noStateLoaded() {
        // Initialize default values if no saved state exists
        jiraUrl = ""
        email = ""
        apiToken = ""
        projectKey = ""
        autoCreateTickets = false
    }

    companion object {
        @JvmStatic
        fun getInstance(): JiraSettings {
            return ApplicationManager.getApplication().getService(JiraSettings::class.java)
        }
    }
}

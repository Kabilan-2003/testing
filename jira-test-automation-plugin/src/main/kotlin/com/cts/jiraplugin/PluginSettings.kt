package com.cts.jiraplugin

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@State(name = "JiraPluginSettings", storages = [Storage("jiraPluginSettings.xml")])
class PluginSettings : PersistentStateComponent<PluginSettings.State> {

    data class State(
        var jiraUrl: String = "",
        var projectKey: String = "",
        var accessToken: String? = null,
        var refreshToken: String? = null,
        var tokenExpiry: Long = 0
    )

    private var state = State()

    var jiraUrl: String
        get() = state.jiraUrl
        set(value) { state.jiraUrl = value }

    var projectKey: String
        get() = state.projectKey
        set(value) { state.projectKey = value }

    var accessToken: String?
        get() = state.accessToken
        set(value) { state.accessToken = value }

    var refreshToken: String?
        get() = state.refreshToken
        set(value) { state.refreshToken = value }

    var tokenExpiry: Long
        get() = state.tokenExpiry
        set(value) { state.tokenExpiry = value }

    val areSettingsConfigured: Boolean
        get() = jiraUrl.isNotBlank() && projectKey.isNotBlank()

    fun saveSettings(jiraUrl: String, projectKey: String) {
        this.jiraUrl = jiraUrl
        this.projectKey = projectKey
    }

    override fun getState(): State = state
    override fun loadState(state: State) { this.state = state }

    companion object {
        val instance: PluginSettings
            get() = com.intellij.openapi.application.ApplicationManager
                .getApplication()
                .getService(PluginSettings::class.java)
    }
}

package com.cts.jiraplugin

import com.cts.jiraplugin.PluginSettings
import com.cts.jiraplugin.PluginSettingsPanel
import com.cts.jiraplugin.listener.JUnitTestListener
import com.intellij.execution.ExecutionManager
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.testframework.TestFrameworkRunningModel
import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.util.messages.MessageBusConnection
import javax.swing.JComponent

/**
 * Provides a settings page in the IDE for configuring Jira Test Automation Plugin.
 * Connects the settings UI (PluginSettingsPanel) with the persistent storage (PluginSettings).
 */
class PluginSettingsConfigurable : Configurable {

    private var settingsPanel: PluginSettingsPanel? = null
    private val settings get() = PluginSettings.getInstance()

    override fun getDisplayName(): String = "Jira Test Automation Plugin"

    override fun createComponent(): JComponent? {
        settingsPanel = PluginSettingsPanel()
        // Initialize panel with current settings
        settingsPanel?.setJiraUrl(settings.jiraUrl)
        settingsPanel?.setProjectKey(settings.projectKey)
        settingsPanel?.updateLoginStatus()
        return settingsPanel?.panel
    }

    override fun isModified(): Boolean {
        return settingsPanel?.let {
            it.getJiraUrl() != settings.jiraUrl || it.getProjectKey() != settings.projectKey
        } ?: false
    }

    override fun apply() {
        settingsPanel?.let {
            settings.jiraUrl = it.getJiraUrl()
            settings.projectKey = it.getProjectKey()
        }
    }

    override fun reset() {
        settingsPanel?.let {
            it.setJiraUrl(settings.jiraUrl)
            it.setProjectKey(settings.projectKey)
            it.updateLoginStatus()
        }
    }

    override fun disposeUIResources() {
        settingsPanel = null
    }
}

class JiraTestAutomationPlugin(private val project: Project) : ProjectComponent {
    private var connection: MessageBusConnection? = null
    private var testListener: JUnitTestListener? = null

    override fun projectOpened() {
        // Register the test listener when the project is opened
        registerTestListener()
    }

    private fun registerTestListener() {
        connection = project.messageBus.connect()

        // Listen for test execution events
        connection?.subscribe(
            TestFrameworkRunningModel.TEST_FRAMEWORK_MODEL_CHANGED_TOPIC,
            TestFrameworkRunningModel.TestFrameworkRunningModelListener { model ->
                // Register our test listener when a test run starts
                model.addTestsComparator { test1, test2 -> 0 } // Just a dummy comparator

                // Get the results viewer
                val resultsViewer = model.resultsViewer as? SMTestRunnerResultsViewer ?: return@TestFrameworkRunningModelListener

                // Create and register our test listener
                testListener = JUnitTestListener(project).also { listener ->
                    resultsViewer.addTestListener(listener)
                }
            }
        )

        // Listen for execution events to clean up
        connection?.subscribe(
            ExecutionManager.Companion.EXECUTION_TOPIC,
            object : ExecutionManager.ExecutionListener {
                override fun processTerminating(
                    executorId: String,
                    env: ExecutionEnvironment,
                    handler: ProcessHandler
                ) {
                    // Clean up when the test process is terminating
                    if (executorId == DefaultRunExecutor.EXECUTOR_ID) {
                        testListener = null
                    }
                }
            }
        )
    }

    override fun projectClosed() {
        // Clean up when the project is closed
        connection?.disconnect()
        testListener = null
    }

    override fun initComponent() {}
    override fun disposeComponent() {}

    companion object {
        const val PLUGIN_ID = "com.jira.test.automation.plugin"

        fun getInstance(project: Project): JiraTestAutomationPlugin {
            return project.getService(JiraTestAutomationPlugin::class.java)
        }
    }
}
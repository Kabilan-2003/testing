package com.cts.jiraplugin

import com.intellij.execution.ExecutionManager
import com.intellij.execution.Executor
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.testframework.TestFrameworkRunningModel
import com.intellij.execution.testframework.sm.runner.SMTestProxy
import com.intellij.execution.testframework.sm.runner.ui.SMTRunnerConsoleView
import com.intellij.execution.testframework.sm.runner.ui.SMTestRunnerResultsViewer
import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.util.messages.MessageBusConnection
import com.cts.jiraplugin.listener.JUnitTestListener

class JiraTestAutomationPlugin(private val project: Project) : ProjectComponent {
    private var connection: MessageBusConnection? = null
    private var testListener: JUnitTestListener? = null

    override fun projectOpened() {
        registerTestListener()
    }

    private fun registerTestListener() {
        connection = project.messageBus.connect()
        
        // Listen for test execution events
        connection?.subscribe(
            TestFrameworkRunningModel.TEST_FRAMEWORK_MODEL_CHANGED,
            TestFrameworkRunningModel.TestFrameworkRunningModelListener { model ->
                // Register our test listener when a test run starts
                model.addTestsComparator { test1, test2 -> 0 } // Dummy comparator
                
                // Get the results viewer and register our listener
                val resultsViewer = model.resultsViewer as? SMTestRunnerResultsViewer ?: return@TestFrameworkRunningModelListener
                
                // Create and register our test listener
                testListener = JUnitTestListener(project).also { listener ->
                    resultsViewer.addListener(listener)
                }
            }
        )
        
        // Listen for execution events to clean up
        connection?.subscribe(
            ExecutionManager.EXECUTION_TOPIC,
            object : ExecutionManager.ExecutionListener {
                override fun processTerminating(
                    executorId: String,
                    env: ExecutionEnvironment,
                    handler: ProcessHandler,
                    processHandler: ProcessHandler
                ) {
                    // Clean up when the test process is terminating
                    testListener = null
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
        const val PLUGIN_ID = "com.cts.jira.test.automation"
        
        @JvmStatic
        fun getInstance(project: Project): JiraTestAutomationPlugin {
            return project.service()
        }
    }
}

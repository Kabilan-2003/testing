package com.cts.jiraplugin

import com.cts.jiraplugin.core.TestFailureDetector
import com.cts.jiraplugin.persistence.DatabaseManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsListener
import com.intellij.execution.testframework.sm.runner.SMTestProxy
import com.intellij.util.messages.MessageBusConnection

class PluginStartupActivity : StartupActivity {
    override fun runActivity(project: Project) {
        // Initialize DB
        DatabaseManager.init()

        // Subscribe to test framework events
        val connection: MessageBusConnection = project.messageBus.connect()
        val detector = TestFailureDetector(project)
        connection.subscribe(
            SMTRunnerEventsListener.TEST_STATUS,
            object : SMTRunnerEventsListener {
                override fun onTestFailed(test: SMTestProxy) {
                    detector.onTestFailure(test)
                }

                override fun onTestingStarted(testsRoot: SMTestProxy.SMRootTestProxy) {}
                override fun onTestingFinished(testsRoot: SMTestProxy.SMRootTestProxy) {}
                override fun onTestsCountInSuite(count: Int) {}
                override fun onTestStarted(test: SMTestProxy) {}
                override fun onTestFinished(test: SMTestProxy) {}
                override fun onTestIgnored(test: SMTestProxy) {}
                override fun onSuiteFinished(suite: SMTestProxy) {}
                override fun onSuiteStarted(suite: SMTestProxy) {}
                override fun onCustomProgressTestsCategory(categoryName: String?, testCount: Int) {}
                override fun onCustomProgressTestStarted() {}
                override fun onCustomProgressTestFailed() {}
                override fun onCustomProgressTestFinished() {}
                override fun onSuiteTreeNodeAdded(testProxy: SMTestProxy) {}
                override fun onSuiteTreeStarted(suite: SMTestProxy) {}
            }
        )
    }
}

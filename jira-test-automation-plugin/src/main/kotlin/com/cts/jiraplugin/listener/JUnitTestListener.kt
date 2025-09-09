package com.cts.jiraplugin.listener

import com.intellij.execution.testframework.sm.runner.SMTestProxy
import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsListener
import com.intellij.execution.testframework.sm.runner.ui.SMTestRunnerResultsForm
import com.intellij.execution.testframework.sm.runner.events.TestOutputEvent
import com.intellij.openapi.project.Project
import com.cts.jiraplugin.model.TestFailureDetails
import com.cts.jiraplugin.service.TestFailureService

class JUnitTestListener(private val project: Project) : SMTRunnerEventsListener.Adapter() {
    private val processedFailures = mutableSetOf<String>()
    private val testFailureService = TestFailureService.getInstance(project)

    override fun onTestingFinished(testRunner: SMTestRunnerResultsForm.TestResultsViewer) {
        val root = testRunner.testsRootNode
        if (root != null) {
            processFailedTests(root)
        }
    }

    private fun processFailedTests(proxy: SMTestProxy) {
        if (proxy.isLeaf) {
            if (proxy.isInProgress || proxy.isPassed) return
            
            val testName = proxy.name ?: "UnnamedTest"
            val className = proxy.locationUrl?.split("(")?.getOrNull(1)?.removeSuffix(")") ?: "UnknownClass"
            
            // Create a unique identifier for this test failure
            val failureId = "$className.$testName"
            
            if (processedFailures.add(failureId)) {
                val errorMessage = proxy.errorMessage ?: "No error message available"
                val stackTrace = proxy.stackTrace ?: "No stack trace available"
                
                val failureDetails = TestFailureDetails(
                    testName = testName,
                    className = className,
                    errorMessage = errorMessage,
                    stackTrace = stackTrace,
                    testRunId = System.currentTimeMillis().toString()
                )
                
                // Handle the test failure (this will show the dialog if auto-create is off)
                testFailureService.handleTestFailure(failureDetails)
            }
        } else {
            // Process children recursively
            proxy.children?.forEach { processFailedTests(it) }
        }
    }

    // Override only the methods we need from the Adapter
    override fun onTestFailed(test: SMTestProxy, e: Throwable) {
        processFailedTests(test)
    }

    override fun onTestFailed(test: SMTestProxy) {
        processFailedTests(test)
    }

    override fun onTestFinished(test: SMTestProxy) {
        // Handle test finished if needed
    }

    override fun onUncapturedOutput(text: String, type: TestOutputEvent.OutputType) {
        // Handle uncaptured output if needed
    }
}

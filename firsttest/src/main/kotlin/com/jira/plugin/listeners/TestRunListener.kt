package com.jira.testautomation.listeners

import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleView
import com.intellij.execution.testframework.sm.runner.SMTestProxy
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.jira.testautomation.models.TestFailure
import com.jira.testautomation.models.TestFramework
import com.jira.testautomation.services.JiraIntegrationService
import java.time.LocalDateTime

/**
 * Listens to test execution and detects failures
 */
class TestRunListener : com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleView.Listener {

    private val logger = thisLogger()
    private val jiraService = JiraIntegrationService.getInstance()

    override fun onTestFailed(test: SMTestProxy, cause: Throwable?) {
        logger.info("Test failed detected: ${test.name}")

        try {
            val testFailure = createTestFailureFromTest(test, cause)
            jiraService.handleTestFailure(testFailure)
        } catch (e: Exception) {
            logger.error("Error handling test failure", e)
        }
    }

    private fun createTestFailureFromTest(test: SMTestProxy, cause: Throwable?): TestFailure {
        val testName = test.name ?: "Unknown Test"
        val className = extractClassName(test)
        val methodName = extractMethodName(test)
        val errorMessage = cause?.message ?: "Test failed without specific error message"
        val stackTrace = cause?.stackTraceToString() ?: "No stack trace available"
        val testFramework = detectTestFramework(test)

        return TestFailure(
            testName = testName,
            className = className,
            methodName = methodName,
            errorMessage = errorMessage,
            stackTrace = stackTrace,
            testFramework = testFramework,
            failureTime = LocalDateTime.now(),
            projectName = getProjectName(test),
            moduleName = getModuleName(test),
            testDuration = getTestDuration(test),
            additionalLogs = getAdditionalLogs(test)
        )
    }

    private fun extractClassName(test: SMTestProxy): String {
        val locationUrl = test.locationUrl
        return if (locationUrl != null) {
            // Extract class name from location URL
            locationUrl.substringAfterLast("/").substringBefore(".")
        } else {
            test.parent?.name ?: "UnknownClass"
        }
    }

    private fun extractMethodName(test: SMTestProxy): String {
        return test.name ?: "unknownMethod"
    }

    private fun detectTestFramework(test: SMTestProxy): TestFramework {
        val locationUrl = test.locationUrl ?: return TestFramework.UNKNOWN

        return when {
            locationUrl.contains("RestAssured") ||
                    locationUrl.contains("restassured") -> TestFramework.REST_ASSURED
            locationUrl.contains("junit") -> {
                if (locationUrl.contains("junit5") || locationUrl.contains("jupiter")) {
                    TestFramework.JUNIT5
                } else {
                    TestFramework.JUNIT4
                }
            }
            locationUrl.contains("testng") -> TestFramework.TESTNG
            locationUrl.contains("spock") -> TestFramework.SPOCK
            else -> TestFramework.UNKNOWN
        }
    }

    private fun getProjectName(test: SMTestProxy): String {
        // Try to get project from test context
        val project = test.project
        return project?.name ?: "Unknown Project"
    }

    private fun getModuleName(test: SMTestProxy): String? {
        // Try to extract module name from test path
        val locationUrl = test.locationUrl
        return if (locationUrl != null && locationUrl.contains("/")) {
            val parts = locationUrl.split("/")
            if (parts.size > 2) parts[parts.size - 3] else null
        } else null
    }

    private fun getTestDuration(test: SMTestProxy): Long? {
        return test.duration?.toLong()
    }

    private fun getAdditionalLogs(test: SMTestProxy): String? {
        // Extract additional logs from test output
        val output = test.output
        return if (output != null && output.isNotBlank()) {
            output
        } else null
    }
}

package com.cts.jiraplugin.ui

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.cts.jiraplugin.core.TestFailureDetector

// Action to run all test cases
@Suppress("unused")
class RunAllTestsAction : AnAction("Run All Test Cases") {
    override fun actionPerformed(e: AnActionEvent) {
        TestFailureDetector.runAllTestCases()
    }
}

@Suppress("unused")
class RunSingleTestAction : AnAction("Run Single Test Case") {
    override fun actionPerformed(e: AnActionEvent) {
        val testFailure = TestDataGenerator.generateTestFailure()
        // Use sample/hardcoded test failure data instead of TestDataGenerator
        TestFailureDetector.onTestFailure(
            testName = "SampleTestCase",
            errorMessage = "Assertion failed: expected true but was false",
            stackTrace = "at SampleTestCase.testMethod(SampleTestCase.kt:42)"
        )
    }
}

package com.cts.jiraplugin.core

import com.cts.jiraplugin.ai.AiService
import com.cts.jiraplugin.persistence.DatabaseManager
import com.intellij.execution.testframework.sm.runner.events.TestFailedEvent
import com.intellij.openapi.project.Project

class TestFailureDetector(private val project: Project) {

    fun onTestFailure(event: TestFailedEvent) {
        val testName = event.name ?: "unknownTest"
        val summary = "Test $testName failed"
        val description = event.stacktrace ?: "No stacktrace available"

        // Run AI analysis (severity, duplicate/cluster, root cause)
        val analysis = AiService.analyze(description)

        // Save draft into DB (jiraKey empty)
        DatabaseManager.saveIssue(
            testName = testName,
            jiraKey = "",
            summary = summary,
            description = description,
            severity = analysis.severity,
            clusterId = analysis.clusterId,
            rootCause = analysis.rootCause
        )

        // Notify user: draft created
        NotificationManager.notifyInfo(
            project,
            "Test failed: $testName",
            "Draft created in 'Jira Issues' panel. Severity: ${analysis.severity}"
        )

        // Optional: open tool window automatically (uncomment if you want an active popup)
        // com.intellij.openapi.wm.ToolWindowManager.getInstance(project)
        //    .getToolWindow("Jira Issues")?.activate(null)
    }
}


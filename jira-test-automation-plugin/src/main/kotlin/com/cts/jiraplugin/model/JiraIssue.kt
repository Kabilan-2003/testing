package com.cts.jiraplugin.model

data class JiraIssue(
    val key: String = "",
    val summary: String,
    val description: String,
    val issueType: String = "Bug",
    val projectKey: String,
    val priority: String = "Medium",
    val labels: List<String> = emptyList(),
    val testFailureDetails: TestFailureDetails? = null
)

data class TestFailureDetails(
    val testName: String,
    val className: String,
    val errorMessage: String,
    val stackTrace: String,
    val testRunId: String
)

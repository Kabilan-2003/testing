package com.jira.testautomation.models

import java.time.LocalDateTime

/**
 * Represents a test failure with all relevant information for Jira ticket creation
 */
data class TestFailure(
    val testName: String,
    val className: String,
    val methodName: String,
    val errorMessage: String,
    val stackTrace: String,
    val testFramework: TestFramework,
    val failureTime: LocalDateTime = LocalDateTime.now(),
    val projectName: String,
    val moduleName: String? = null,
    val testDuration: Long? = null, // in milliseconds
    val additionalLogs: String? = null
) {
    /**
     * Generate a unique identifier for this test failure
     */
    fun generateId(): String {
        return "${className}_${methodName}_${failureTime.toEpochSecond(java.time.ZoneOffset.UTC)}"
    }
    
    /**
     * Get a human-readable test identifier
     */
    fun getTestIdentifier(): String {
        return "$className.$methodName"
    }
}

enum class TestFramework {
    REST_ASSURED,
    JUNIT4,
    JUNIT5,
    TESTNG,
    SPOCK,
    UNKNOWN
}

/**
 * Represents a Jira ticket created from a test failure
 */
data class JiraTicket(
    val ticketId: String,
    val ticketKey: String,
    val ticketUrl: String,
    val testFailureId: String,
    val createdTime: LocalDateTime = LocalDateTime.now(),
    val status: String = "Open",
    val assignee: String? = null,
    val priority: String? = null
)

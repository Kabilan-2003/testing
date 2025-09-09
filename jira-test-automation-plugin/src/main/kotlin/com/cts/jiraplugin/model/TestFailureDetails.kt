package com.cts.jiraplugin.model

/**
 * Data class representing details of a test failure.
 * 
 * @property testName The name of the failed test
 * @property className The fully qualified name of the test class
 * @property errorMessage The error message from the test failure
 * @property stackTrace The stack trace from the test failure
 * @property testRunId A unique identifier for the test run
 */
data class TestFailureDetails(
    val testName: String,
    val className: String,
    val errorMessage: String,
    val stackTrace: String,
    val testRunId: String
)

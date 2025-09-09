package com.jira.testautomation

import com.jira.testautomation.models.TestFailure
import com.jira.testautomation.models.TestFramework
import com.jira.testautomation.services.AIService
import org.junit.Test
import java.time.LocalDateTime

class TestFailureTest {
    
    @Test
    fun testTestFailureCreation() {
        val testFailure = TestFailure(
            testName = "testUserLogin",
            className = "UserLoginTest",
            methodName = "testUserLogin",
            errorMessage = "Assertion failed: expected 'Welcome' but was 'Login Failed'",
            stackTrace = "at UserLoginTest.testUserLogin(UserLoginTest.java:25)",
            testFramework = TestFramework.REST_ASSURED,
            failureTime = LocalDateTime.now(),
            projectName = "TestProject",
            moduleName = "auth-module"
        )
        
        assert(testFailure.getTestIdentifier() == "UserLoginTest.testUserLogin")
        assert(testFailure.generateId().isNotEmpty())
    }
    
    @Test
    fun testAIServiceRuleBased() {
        val aiService = AIService.getInstance()
        
        val testFailure = TestFailure(
            testName = "testTimeout",
            className = "TimeoutTest",
            methodName = "testTimeout",
            errorMessage = "Connection timeout after 30 seconds",
            stackTrace = "at TimeoutTest.testTimeout(TimeoutTest.java:15)",
            testFramework = TestFramework.REST_ASSURED,
            failureTime = LocalDateTime.now(),
            projectName = "TestProject"
        )
        
        val severity = aiService.predictSeverity(testFailure)
        assert(severity == "High")
        
        val rootCause = aiService.suggestRootCause(testFailure)
        assert(rootCause.contains("timeout"))
    }
}

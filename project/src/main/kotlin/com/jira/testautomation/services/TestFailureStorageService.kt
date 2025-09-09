package com.jira.testautomation.services

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.jira.testautomation.models.TestFailure
import com.jira.testautomation.models.JiraTicket
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

/**
 * Service for storing and managing test failures and Jira tickets
 * Phase 2: Enhanced with AI-powered duplicate detection and clustering
 */
@Service
class TestFailureStorageService {
    
    private val logger = thisLogger()
    private val aiService = AIService.getInstance()
    
    // In-memory storage (Phase 3 will add persistent storage)
    private val testFailures = ConcurrentHashMap<String, TestFailure>()
    private val jiraTickets = ConcurrentHashMap<String, JiraTicket>()
    private val failureClusters = ConcurrentHashMap<String, List<TestFailure>>()
    
    companion object {
        fun getInstance(): TestFailureStorageService {
            return ApplicationManager.getApplication().getService(TestFailureStorageService::class.java)
        }
    }
    
    /**
     * Store a test failure and check for duplicates
     */
    fun storeTestFailure(testFailure: TestFailure): TestFailureStorageResult {
        val failureId = testFailure.generateId()
        
        // Check for duplicates using AI
        val existingFailures = testFailures.values.toList()
        val isDuplicate = if (existingFailures.isNotEmpty()) {
            aiService.isDuplicate(testFailure, existingFailures)
        } else {
            false
        }
        
        if (isDuplicate) {
            logger.info("Duplicate test failure detected: ${testFailure.getTestIdentifier()}")
            return TestFailureStorageResult.Duplicate(testFailure)
        }
        
        // Store the failure
        testFailures[failureId] = testFailure
        
        // Update clusters
        updateClusters()
        
        logger.info("Stored test failure: ${testFailure.getTestIdentifier()}")
        return TestFailureStorageResult.New(testFailure)
    }
    
    /**
     * Store a Jira ticket
     */
    fun storeJiraTicket(jiraTicket: JiraTicket) {
        jiraTickets[jiraTicket.ticketId] = jiraTicket
        logger.info("Stored Jira ticket: ${jiraTicket.ticketKey}")
    }
    
    /**
     * Get all test failures
     */
    fun getAllTestFailures(): List<TestFailure> {
        return testFailures.values.toList().sortedByDescending { it.failureTime }
    }
    
    /**
     * Get all Jira tickets
     */
    fun getAllJiraTickets(): List<JiraTicket> {
        return jiraTickets.values.toList().sortedByDescending { it.createdTime }
    }
    
    /**
     * Get test failures by cluster
     */
    fun getFailuresByCluster(): Map<String, List<TestFailure>> {
        return failureClusters.toMap()
    }
    
    /**
     * Get recent test failures (last 24 hours)
     */
    fun getRecentFailures(): List<TestFailure> {
        val cutoff = LocalDateTime.now().minusHours(24)
        return testFailures.values.filter { it.failureTime.isAfter(cutoff) }
            .sortedByDescending { it.failureTime }
    }
    
    /**
     * Get failures by framework
     */
    fun getFailuresByFramework(framework: com.jira.testautomation.models.TestFramework): List<TestFailure> {
        return testFailures.values.filter { it.testFramework == framework }
            .sortedByDescending { it.failureTime }
    }
    
    /**
     * Get failures by severity (AI-predicted)
     */
    fun getFailuresBySeverity(severity: String): List<TestFailure> {
        return testFailures.values.filter { failure ->
            aiService.predictSeverity(failure) == severity
        }.sortedByDescending { it.failureTime }
    }
    
    /**
     * Get statistics
     */
    fun getStatistics(): TestFailureStatistics {
        val totalFailures = testFailures.size
        val totalTickets = jiraTickets.size
        val recentFailures = getRecentFailures().size
        
        val frameworkStats = testFailures.values.groupBy { it.testFramework }
            .mapValues { it.value.size }
        
        val severityStats = testFailures.values.groupBy { aiService.predictSeverity(it) }
            .mapValues { it.value.size }
        
        return TestFailureStatistics(
            totalFailures = totalFailures,
            totalTickets = totalTickets,
            recentFailures = recentFailures,
            frameworkStats = frameworkStats,
            severityStats = severityStats,
            clusterCount = failureClusters.size
        )
    }
    
    /**
     * Update failure clusters using AI
     */
    private fun updateClusters() {
        val failures = testFailures.values.toList()
        if (failures.size >= 2) {
            try {
                val clusters = aiService.clusterFailures(failures)
                failureClusters.clear()
                failureClusters.putAll(clusters)
                logger.info("Updated failure clusters: ${clusters.size} clusters")
            } catch (e: Exception) {
                logger.warn("Failed to update clusters", e)
            }
        }
    }
    
    /**
     * Clear all data (for testing)
     */
    fun clearAll() {
        testFailures.clear()
        jiraTickets.clear()
        failureClusters.clear()
        logger.info("Cleared all test failure data")
    }
    
    /**
     * Result of storing a test failure
     */
    sealed class TestFailureStorageResult {
        data class New(val testFailure: TestFailure) : TestFailureStorageResult()
        data class Duplicate(val testFailure: TestFailure) : TestFailureStorageResult()
    }
    
    /**
     * Statistics about test failures
     */
    data class TestFailureStatistics(
        val totalFailures: Int,
        val totalTickets: Int,
        val recentFailures: Int,
        val frameworkStats: Map<com.jira.testautomation.models.TestFramework, Int>,
        val severityStats: Map<String, Int>,
        val clusterCount: Int
    )
}

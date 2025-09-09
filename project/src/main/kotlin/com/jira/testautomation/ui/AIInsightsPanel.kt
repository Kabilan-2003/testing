package com.jira.testautomation.ui

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.table.TableView
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.ListTableModel
import com.jira.testautomation.models.TestFailure
import com.jira.testautomation.services.TestFailureStorageService
import com.jira.testautomation.services.AIService
import java.awt.BorderLayout
import java.awt.GridLayout
import javax.swing.JPanel
import javax.swing.JSplitPane

/**
 * Enhanced panel showing AI insights, clustering, and analytics
 * Phase 2: AI-powered features
 */
class AIInsightsPanel(private val project: Project) : JPanel() {
    
    private val storageService = TestFailureStorageService.getInstance()
    private val aiService = AIService.getInstance()
    
    private val clustersTable: TableView<ClusterInfo>
    private val insightsTextArea: JBTextArea
    private val statisticsPanel: JPanel
    
    init {
        layout = BorderLayout()
        
        // Create clusters table
        clustersTable = createClustersTable()
        val clustersScrollPane = JBScrollPane(clustersTable)
        
        // Create insights text area
        insightsTextArea = JBTextArea()
        insightsTextArea.isEditable = false
        insightsTextArea.lineWrap = true
        insightsTextArea.wrapStyleWord = true
        val insightsScrollPane = JBScrollPane(insightsTextArea)
        
        // Create statistics panel
        statisticsPanel = createStatisticsPanel()
        
        // Create split pane for clusters and insights
        val splitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT)
        splitPane.leftComponent = clustersScrollPane
        splitPane.rightComponent = insightsScrollPane
        splitPane.dividerLocation = 400
        
        // Add components
        add(statisticsPanel, BorderLayout.NORTH)
        add(splitPane, BorderLayout.CENTER)
        
        // Load initial data
        refreshData()
    }
    
    private fun createClustersTable(): TableView<ClusterInfo> {
        val columns = arrayOf(
            ClusterNameColumn(),
            FailureCountColumn(),
            SeverityColumn(),
            FrameworkColumn(),
            LastFailureColumn()
        )
        
        val model = ListTableModel(columns)
        val table = TableView(model)
        
        // Configure table
        table.setShowGrid(true)
        table.setStriped(true)
        table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION)
        
        // Add selection listener
        table.selectionModel.addListSelectionListener {
            if (!it.valueIsAdjusting) {
                val selectedRow = table.selectedRow
                if (selectedRow >= 0) {
                    val cluster = (table.model as ListTableModel<ClusterInfo>).getItem(selectedRow)
                    showClusterInsights(cluster)
                }
            }
        }
        
        return table
    }
    
    private fun createStatisticsPanel(): JPanel {
        val panel = JPanel(GridLayout(2, 4, 10, 5))
        
        val totalFailuresLabel = JBLabel("Total Failures: 0")
        val totalTicketsLabel = JBLabel("Total Tickets: 0")
        val recentFailuresLabel = JBLabel("Recent (24h): 0")
        val clustersLabel = JBLabel("Clusters: 0")
        
        val criticalLabel = JBLabel("Critical: 0")
        val highLabel = JBLabel("High: 0")
        val mediumLabel = JBLabel("Medium: 0")
        val lowLabel = JBLabel("Low: 0")
        
        panel.add(totalFailuresLabel)
        panel.add(totalTicketsLabel)
        panel.add(recentFailuresLabel)
        panel.add(clustersLabel)
        panel.add(criticalLabel)
        panel.add(highLabel)
        panel.add(mediumLabel)
        panel.add(lowLabel)
        
        // Store references for updates
        panel.putClientProperty("totalFailures", totalFailuresLabel)
        panel.putClientProperty("totalTickets", totalTicketsLabel)
        panel.putClientProperty("recentFailures", recentFailuresLabel)
        panel.putClientProperty("clusters", clustersLabel)
        panel.putClientProperty("critical", criticalLabel)
        panel.putClientProperty("high", highLabel)
        panel.putClientProperty("medium", mediumLabel)
        panel.putClientProperty("low", lowLabel)
        
        return panel
    }
    
    private fun showClusterInsights(cluster: ClusterInfo) {
        val insights = buildClusterInsights(cluster)
        insightsTextArea.text = insights
    }
    
    private fun buildClusterInsights(cluster: ClusterInfo): String {
        val sb = StringBuilder()
        
        sb.append("=== CLUSTER INSIGHTS ===\n\n")
        sb.append("Cluster: ${cluster.name}\n")
        sb.append("Failures: ${cluster.failureCount}\n")
        sb.append("Severity: ${cluster.severity}\n")
        sb.append("Framework: ${cluster.framework}\n")
        sb.append("Last Failure: ${cluster.lastFailure}\n\n")
        
        sb.append("=== AI ANALYSIS ===\n")
        if (cluster.failures.isNotEmpty()) {
            val sampleFailure = cluster.failures.first()
            val rootCause = aiService.suggestRootCause(sampleFailure)
            val severity = aiService.predictSeverity(sampleFailure)
            
            sb.append("Predicted Root Cause: $rootCause\n")
            sb.append("Predicted Severity: $severity\n\n")
        }
        
        sb.append("=== FAILURE DETAILS ===\n")
        cluster.failures.forEachIndexed { index, failure ->
            sb.append("${index + 1}. ${failure.getTestIdentifier()}\n")
            sb.append("   Error: ${failure.errorMessage.take(100)}...\n")
            sb.append("   Time: ${failure.failureTime}\n\n")
        }
        
        return sb.toString()
    }
    
    /**
     * Refresh all data in the panel
     */
    fun refreshData() {
        refreshClusters()
        refreshStatistics()
    }
    
    private fun refreshClusters() {
        val clusters = storageService.getFailuresByCluster()
        val clusterInfos = clusters.map { (name, failures) ->
            ClusterInfo(
                name = name,
                failures = failures,
                failureCount = failures.size,
                severity = getMostCommonSeverity(failures),
                framework = getMostCommonFramework(failures),
                lastFailure = failures.maxByOrNull { it.failureTime }?.failureTime?.toString() ?: "N/A"
            )
        }.sortedByDescending { it.failureCount }
        
        val model = clustersTable.model as ListTableModel<ClusterInfo>
        model.items = clusterInfos
    }
    
    private fun refreshStatistics() {
        val stats = storageService.getStatistics()
        
        val totalFailuresLabel = statisticsPanel.getClientProperty("totalFailures") as JBLabel
        val totalTicketsLabel = statisticsPanel.getClientProperty("totalTickets") as JBLabel
        val recentFailuresLabel = statisticsPanel.getClientProperty("recentFailures") as JBLabel
        val clustersLabel = statisticsPanel.getClientProperty("clusters") as JBLabel
        val criticalLabel = statisticsPanel.getClientProperty("critical") as JBLabel
        val highLabel = statisticsPanel.getClientProperty("high") as JBLabel
        val mediumLabel = statisticsPanel.getClientProperty("medium") as JBLabel
        val lowLabel = statisticsPanel.getClientProperty("low") as JBLabel
        
        totalFailuresLabel.text = "Total Failures: ${stats.totalFailures}"
        totalTicketsLabel.text = "Total Tickets: ${stats.totalTickets}"
        recentFailuresLabel.text = "Recent (24h): ${stats.recentFailures}"
        clustersLabel.text = "Clusters: ${stats.clusterCount}"
        
        criticalLabel.text = "Critical: ${stats.severityStats["Critical"] ?: 0}"
        highLabel.text = "High: ${stats.severityStats["High"] ?: 0}"
        mediumLabel.text = "Medium: ${stats.severityStats["Medium"] ?: 0}"
        lowLabel.text = "Low: ${stats.severityStats["Low"] ?: 0}"
    }
    
    private fun getMostCommonSeverity(failures: List<TestFailure>): String {
        return failures.groupBy { aiService.predictSeverity(it) }
            .maxByOrNull { it.value.size }?.key ?: "Unknown"
    }
    
    private fun getMostCommonFramework(failures: List<TestFailure>): String {
        return failures.groupBy { it.testFramework }
            .maxByOrNull { it.value.size }?.key?.name ?: "Unknown"
    }
    
    /**
     * Data class for cluster information
     */
    data class ClusterInfo(
        val name: String,
        val failures: List<TestFailure>,
        val failureCount: Int,
        val severity: String,
        val framework: String,
        val lastFailure: String
    )
    
    // Column definitions for clusters table
    private inner class ClusterNameColumn : ColumnInfo<ClusterInfo, String>("Cluster Name") {
        override fun valueOf(item: ClusterInfo): String = item.name
        override fun getPreferredWidth(): Int = 150
    }
    
    private inner class FailureCountColumn : ColumnInfo<ClusterInfo, String>("Failures") {
        override fun valueOf(item: ClusterInfo): String = item.failureCount.toString()
        override fun getPreferredWidth(): Int = 80
    }
    
    private inner class SeverityColumn : ColumnInfo<ClusterInfo, String>("Severity") {
        override fun valueOf(item: ClusterInfo): String = item.severity
        override fun getPreferredWidth(): Int = 100
    }
    
    private inner class FrameworkColumn : ColumnInfo<ClusterInfo, String>("Framework") {
        override fun valueOf(item: ClusterInfo): String = item.framework
        override fun getPreferredWidth(): Int = 100
    }
    
    private inner class LastFailureColumn : ColumnInfo<ClusterInfo, String>("Last Failure") {
        override fun valueOf(item: ClusterInfo): String = item.lastFailure
        override fun getPreferredWidth(): Int = 150
    }
}

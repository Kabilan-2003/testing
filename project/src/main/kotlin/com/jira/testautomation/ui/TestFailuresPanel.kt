package com.jira.testautomation.ui

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.TableView
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.ListTableModel
import com.jira.testautomation.models.TestFailure
import com.jira.testautomation.models.JiraTicket
import java.time.format.DateTimeFormatter
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.table.DefaultTableCellRenderer

/**
 * Main panel for displaying test failures and Jira tickets
 */
class TestFailuresPanel(private val project: Project) : JPanel() {
    
    private val testFailuresTable: TableView<TestFailure>
    private val jiraTicketsTable: TableView<JiraTicket>
    
    init {
        layout = java.awt.BorderLayout()
        
        // Create test failures table
        testFailuresTable = createTestFailuresTable()
        val testFailuresScrollPane = JBScrollPane(testFailuresTable)
        
        // Create Jira tickets table
        jiraTicketsTable = createJiraTicketsTable()
        val jiraTicketsScrollPane = JBScrollPane(jiraTicketsTable)
        
        // Create AI insights panel
        val aiInsightsPanel = AIInsightsPanel(project)
        
        // Create tabbed pane
        val tabbedPane = javax.swing.JTabbedPane()
        tabbedPane.addTab("Test Failures", testFailuresScrollPane)
        tabbedPane.addTab("Jira Tickets", jiraTicketsScrollPane)
        tabbedPane.addTab("AI Insights", aiInsightsPanel)
        
        add(tabbedPane, java.awt.BorderLayout.CENTER)
        
        // Add status label
        val statusLabel = JBLabel("Ready to monitor test failures...")
        add(statusLabel, java.awt.BorderLayout.SOUTH)
    }
    
    private fun createTestFailuresTable(): TableView<TestFailure> {
        val columns = arrayOf(
            TestNameColumn(),
            TestFrameworkColumn(),
            ErrorMessageColumn(),
            FailureTimeColumn(),
            StatusColumn()
        )
        
        val model = ListTableModel(columns)
        val table = TableView(model)
        
        // Configure table appearance
        table.setShowGrid(true)
        table.setStriped(true)
        table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION)
        
        return table
    }
    
    private fun createJiraTicketsTable(): TableView<JiraTicket> {
        val columns = arrayOf(
            TicketKeyColumn(),
            TestIdentifierColumn(),
            StatusColumn(),
            CreatedTimeColumn(),
            PriorityColumn()
        )
        
        val model = ListTableModel(columns)
        val table = TableView(model)
        
        // Configure table appearance
        table.setShowGrid(true)
        table.setStriped(true)
        table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION)
        
        return table
    }
    
    /**
     * Add a test failure to the table
     */
    fun addTestFailure(testFailure: TestFailure) {
        val model = testFailuresTable.model as ListTableModel<TestFailure>
        model.addRow(testFailure)
    }
    
    /**
     * Add a Jira ticket to the table
     */
    fun addJiraTicket(jiraTicket: JiraTicket) {
        val model = jiraTicketsTable.model as ListTableModel<JiraTicket>
        model.addRow(jiraTicket)
    }
    
    /**
     * Get selected test failure
     */
    fun getSelectedTestFailure(): TestFailure? {
        val selectedRow = testFailuresTable.selectedRow
        return if (selectedRow >= 0) {
            val model = testFailuresTable.model as ListTableModel<TestFailure>
            model.getItem(selectedRow)
        } else null
    }
    
    /**
     * Get selected Jira ticket
     */
    fun getSelectedJiraTicket(): JiraTicket? {
        val selectedRow = jiraTicketsTable.selectedRow
        return if (selectedRow >= 0) {
            val model = jiraTicketsTable.model as ListTableModel<JiraTicket>
            model.getItem(selectedRow)
        } else null
    }
    
    // Column definitions for test failures table
    private inner class TestNameColumn : ColumnInfo<TestFailure, String>("Test Name") {
        override fun valueOf(item: TestFailure): String = item.getTestIdentifier()
        override fun getPreferredWidth(): Int = 200
    }
    
    private inner class TestFrameworkColumn : ColumnInfo<TestFailure, String>("Framework") {
        override fun valueOf(item: TestFailure): String = item.testFramework.name
        override fun getPreferredWidth(): Int = 100
    }
    
    private inner class ErrorMessageColumn : ColumnInfo<TestFailure, String>("Error Message") {
        override fun valueOf(item: TestFailure): String = item.errorMessage.take(100) + if (item.errorMessage.length > 100) "..." else ""
        override fun getPreferredWidth(): Int = 300
    }
    
    private inner class FailureTimeColumn : ColumnInfo<TestFailure, String>("Failure Time") {
        override fun valueOf(item: TestFailure): String = item.failureTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        override fun getPreferredWidth(): Int = 150
    }
    
    private inner class StatusColumn : ColumnInfo<TestFailure, String>("Status") {
        override fun valueOf(item: TestFailure): String = "Failed"
        override fun getPreferredWidth(): Int = 80
    }
    
    // Column definitions for Jira tickets table
    private inner class TicketKeyColumn : ColumnInfo<JiraTicket, String>("Ticket Key") {
        override fun valueOf(item: JiraTicket): String = item.ticketKey
        override fun getPreferredWidth(): Int = 120
    }
    
    private inner class TestIdentifierColumn : ColumnInfo<JiraTicket, String>("Test") {
        override fun valueOf(item: JiraTicket): String = item.testFailureId
        override fun getPreferredWidth(): Int = 200
    }
    
    private inner class StatusColumn : ColumnInfo<JiraTicket, String>("Status") {
        override fun valueOf(item: JiraTicket): String = item.status
        override fun getPreferredWidth(): Int = 100
    }
    
    private inner class CreatedTimeColumn : ColumnInfo<JiraTicket, String>("Created") {
        override fun valueOf(item: JiraTicket): String = item.createdTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        override fun getPreferredWidth(): Int = 150
    }
    
    private inner class PriorityColumn : ColumnInfo<JiraTicket, String>("Priority") {
        override fun valueOf(item: JiraTicket): String = item.priority ?: "N/A"
        override fun getPreferredWidth(): Int = 80
    }
}

package com.cts.jiraplugin.ui

import com.cts.jiraplugin.model.TestFailureDetails
import com.cts.jiraplugin.persistence.DatabaseManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.table.DefaultTableModel

class PluginPanel : JPanel(BorderLayout()) {

    private val tableModel = DefaultTableModel(
        arrayOf("ID", "Test Name", "Jira Key", "Summary", "Severity", "ClusterId", "Status"), 0
    )
    private val table = JTable(tableModel)

    private val severityFilter = JComboBox(arrayOf("All", "Critical", "Major", "Minor"))
    private val statusFilter = JComboBox(arrayOf("All", "Pending", "Created", "Ignored"))
    private val clusterFilter = JTextField(10)
    private val applyFilterBtn = JButton("Apply")

    init {
        val filterPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        filterPanel.add(JLabel("Severity:"))
        filterPanel.add(severityFilter)
        filterPanel.add(JLabel("Status:"))
        filterPanel.add(statusFilter)
        filterPanel.add(JLabel("ClusterId:"))
        filterPanel.add(clusterFilter)
        filterPanel.add(applyFilterBtn)

        add(filterPanel, BorderLayout.NORTH)
        add(JScrollPane(table), BorderLayout.CENTER)

        loadIssues()

        applyFilterBtn.addActionListener {
            applyFilters()
        }

        // ✅ Add right-click menu
        val popupMenu = JPopupMenu()
        val ignoreItem = JMenuItem("Mark as Ignored")
        val reopenItem = JMenuItem("Reopen Pending")

        ignoreItem.addActionListener {
            val row = table.selectedRow
            if (row >= 0) {
                val id = tableModel.getValueAt(row, 0).toString().toInt()
                DatabaseManager.updateStatus(id, "ignored")
                loadIssues()
            }
        }

        reopenItem.addActionListener {
            val row = table.selectedRow
            if (row >= 0) {
                val id = tableModel.getValueAt(row, 0).toString().toInt()
                DatabaseManager.updateStatus(id, "pending")
                loadIssues()
            }
        }

        popupMenu.add(ignoreItem)
        popupMenu.add(reopenItem)

        table.componentPopupMenu = popupMenu

        // Also support right-click selection
        table.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                if (e.isPopupTrigger) selectRow(e)
            }
            override fun mouseReleased(e: MouseEvent) {
                if (e.isPopupTrigger) selectRow(e)
            }
            private fun selectRow(e: MouseEvent) {
                val row = table.rowAtPoint(e.point)
                if (row >= 0 && row < table.rowCount) {
                    table.setRowSelectionInterval(row, row)
                }
            }
        })
    }

    private fun loadIssues(resultSet: java.sql.ResultSet? = null) {
        tableModel.setRowCount(0)
        val rs = resultSet ?: DatabaseManager.getAllIssues()
        while (rs.next()) {
            tableModel.addRow(
                arrayOf(
                    rs.getInt("id"),
                    rs.getString("testName"),
                    rs.getString("jiraKey"),
                    rs.getString("summary"),
                    rs.getString("severity"),
                    rs.getString("clusterId"),
                    rs.getString("status")
                )
            )
        }
    }

    private fun applyFilters() {
        val severity = severityFilter.selectedItem as String
        val status = statusFilter.selectedItem as String
        val clusterId = clusterFilter.text.trim()

        var rs: java.sql.ResultSet? = null

        when {
            severity != "All" -> rs = DatabaseManager.getIssuesBySeverity(severity)
            status != "All" -> rs = DatabaseManager.getIssuesByStatus(status.lowercase())
            clusterId.isNotEmpty() -> rs = DatabaseManager.getIssuesByCluster(clusterId)
        }

        loadIssues(rs)
    }
}

class JiraTicketDialog(
    project: Project?,
    private val failureDetails: TestFailureDetails,
    private val onCreate: (String, String) -> Unit
) : DialogWrapper(project) {
    private val summaryField = JBTextField("Test Failed: ${failureDetails.testName}")
    private val descriptionArea = JBTextArea().apply {
        text = """
            ||Test Name||${failureDetails.testName}|
            |Class|${failureDetails.className}|
            |Error Message|${failureDetails.errorMessage}|
            
            Stack Trace:
            ${failureDetails.stackTrace}
        """.trimMargin()
        lineWrap = true
        wrapStyleWord = true
    }

    init {
        title = "Create Jira Ticket for Test Failure"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val formPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent("Summary:", summaryField)
            .addLabeledComponent("Description:", JScrollPane(descriptionArea).apply {
                preferredSize = Dimension(500, 300)
            })
            .addComponent(createInfoPanel(), 1)
            .panel

        val panel = JPanel(BorderLayout())
        panel.add(formPanel, BorderLayout.CENTER)
        return panel
    }

    private fun createInfoPanel(): JComponent {
        return FormBuilder.createFormBuilder()
            .addLabeledComponent("Test:", JBTextField(failureDetails.testName).apply { isEditable = false })
            .addLabeledComponent("Class:", JBTextField(failureDetails.className).apply { isEditable = false })
            .addLabeledComponent("Error:", JBTextArea(failureDetails.errorMessage).apply {
                isEditable = false
                lineWrap = true
                wrapStyleWord = true
            })
            .panel
    }

    override fun getPreferredFocusedComponent() = summaryField

    override fun doOKAction() {
        if (validateInput()) {
            super.doOKAction()
            onCreate(summaryField.text, descriptionArea.text)
        }
    }

    private fun validateInput(): Boolean {
        if (summaryField.text.isBlank()) {
            errorText = "Summary cannot be empty"
            return false
        }
        return true
    }

    companion object {
        fun show(project: Project, failureDetails: TestFailureDetails, onCreate: (String, String) -> Unit) {
            JiraTicketDialog(project, failureDetails, onCreate).show()
        }
    }
}
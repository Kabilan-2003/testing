package com.cts.jiraplugin.ui

import com.cts.jiraplugin.PluginSettings
import com.cts.jiraplugin.auth.OAuthManager
import com.cts.jiraplugin.core.AIClient
import com.cts.jiraplugin.core.FailureCluster
import com.cts.jiraplugin.core.JiraClient
import com.cts.jiraplugin.core.TestFailureDetector
import com.cts.jiraplugin.persistence.DatabaseManager
import com.cts.jiraplugin.persistence.TestFailure
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Component
import java.awt.FlowLayout
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JTabbedPane
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.DefaultTreeModel

class EnhancedPluginPanel(private val project: Project) : SimpleToolWindowPanel(false, true) {

    private val jiraClient: JiraClient by lazy {
        val settings = PluginSettings
        JiraClient(settings.jiraUrl ?: "", settings.email, settings.apiToken)
    }

    private val myTicketsTableModel = javax.swing.table.DefaultTableModel(arrayOf("Key", "Summary", "Status", "Assignee"), 0)
    private val failuresTreeModel = DefaultTreeModel(DefaultMutableTreeNode("Failures"))
    private val failuresTree = Tree(failuresTreeModel)

    init {
        val toolbar = createToolbar()
        setToolbar(toolbar.component)
        refresh()
    }

    private fun createToolbar(): ActionToolbar {
        val actionGroup = DefaultActionGroup().apply {
            add(LoginAction())
            add(RefreshAction())
            add(LogoutAction())
        }
        return ActionManager.getInstance().createActionToolbar("JiraPluginToolbar", actionGroup, false)
    }

    private fun showLoginPrompt() {
        val loginPanel = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(20)
            add(JBLabel("<html><center>Please log in to Jira to use the plugin features</center></html>"), BorderLayout.CENTER)
        }
        setContent(loginPanel)
    }

    private fun showContent() {
        val tabbedPane = JTabbedPane()

        tabbedPane.addTab("My Tickets", AllIcons.Debugger.Db_marked_breakpoint, createMyTicketsPanel())
        tabbedPane.addTab("Failures", AllIcons.Toolwindows.ToolWindowBuild, createFailuresPanel())
        tabbedPane.addTab("Analytics", AllIcons.Graph.Chart, AnalyticsPanel(project))

        setContent(tabbedPane)
    }

    private fun createMyTicketsPanel(): JPanel {
        val table = JBTable(myTicketsTableModel)
        val scrollPane = JBScrollPane(table)

        val assignToSelfButton = JButton("Assign to Me").apply {
            addActionListener {
                val selectedRow = table.selectedRow
                if (selectedRow >= 0) {
                    val issueKey = myTicketsTableModel.getValueAt(selectedRow, 0) as String
                    ApplicationManager.getApplication().executeOnPooledThread {
                        jiraClient.assignToSelf(issueKey)
                        ApplicationManager.getApplication().invokeLater { refresh() }
                    }
                }
            }
        }

        val buttonPanel = JPanel(FlowLayout(FlowLayout.LEFT)).apply { add(assignToSelfButton) }

        return JPanel(BorderLayout()).apply {
            add(buttonPanel, BorderLayout.NORTH)
            add(scrollPane, BorderLayout.CENTER)
        }
    }

    private fun createFailuresPanel(): JPanel {
        failuresTree.cellRenderer = FailureTreeCellRenderer()
        failuresTree.isRootVisible = false
        val scrollPane = JBScrollPane(failuresTree)

        val createTicketButton = JButton("Create Jira Ticket").apply {
            addActionListener {
                val selectedNode = failuresTree.lastSelectedPathComponent as? DefaultMutableTreeNode ?: return@addActionListener
                val userObject = selectedNode.userObject

                ApplicationManager.getApplication().executeOnPooledThread {
                    when (userObject) {
                        is FailureCluster -> {
                            val representative = userObject.failures.first()
                            val aiResult = AIClient.analyzeFailure(
                                representative["test_name"] as String,
                                representative["error_message"] as String,
                                representative["stack_trace"] as String
                            )
                            ApplicationManager.getApplication().invokeLater {
                                val dialog = ApprovalDialog(
                                    testName = userObject.representativeName,
                                    errorMessage = representative["error_message"] as String,
                                    stackTrace = representative["stack_trace"] as String,
                                    severity = aiResult?.severity,
                                    probableCause = aiResult?.probableCause,
                                    isClustered = true,
                                    clusterInfo = "Create 1 ticket for ${userObject.count} similar failures?"
                                )
                                dialog.show()
                                if (dialog.isApproved) {
                                    TestFailureDetector.createJiraTicketForCluster(userObject, aiResult)
                                    refresh()
                                }
                            }
                        }
                        is TestFailure -> {
                            val aiResult = AIClient.analyzeFailure(userObject.testName, userObject.errorMessage, userObject.stackTrace)
                            ApplicationManager.getApplication().invokeLater {
                                val dialog = ApprovalDialog(
                                    testName = userObject.testName,
                                    errorMessage = userObject.errorMessage,
                                    stackTrace = userObject.stackTrace,
                                    severity = aiResult?.severity,
                                    probableCause = aiResult?.probableCause
                                )
                                dialog.show()
                                if (dialog.isApproved) {
                                    TestFailureDetector.createJiraTicket(userObject.testName, userObject.errorMessage, userObject.stackTrace, aiResult)
                                    refresh()
                                }
                            }
                        }
                    }
                }
            }
        }

        val buttonPanel = JPanel(FlowLayout(FlowLayout.LEFT)).apply { add(createTicketButton) }

        return JPanel(BorderLayout()).apply {
            add(buttonPanel, BorderLayout.NORTH)
            add(scrollPane, BorderLayout.CENTER)
        }
    }

    fun refresh() {
        OAuthManager.loadTokens()
        if ((PluginSettings.useOAuth && OAuthManager.getAccessToken() == null) || !PluginSettings.areSettingsConfigured) {
            showLoginPrompt()
        } else {
            if (content !is JTabbedPane) showContent()

            ApplicationManager.getApplication().executeOnPooledThread {
                val myIssues = jiraClient.searchIssues("assignee = currentUser() ORDER BY updated DESC")
                val pendingFailures = DatabaseManager.getPendingFailures()
                val recentFailuresMap = DatabaseManager.getRecentFailures(24 * 7)

                val clusters = AIClient.clusterFailures(
                    recentFailuresMap.filter { map -> pendingFailures.any { it.testName == map["test_name"] && it.errorMessage == map["error_message"] } }
                )

                val clusteredFailureIds = clusters.flatMap { it.failures }.map { it["test_name"] as String + it["error_message"] as String }.toSet()
                val uniqueFailures = pendingFailures.filter { (it.testName + it.errorMessage) !in clusteredFailureIds }

                ApplicationManager.getApplication().invokeLater {
                    // Populate my tickets table
                    myTicketsTableModel.rowCount = 0
                    myIssues.forEach { issue ->
                        myTicketsTableModel.addRow(arrayOf(issue.key, issue.summary, issue.status, issue.assignee))
                    }

                    // Populate failures tree
                    val root = DefaultMutableTreeNode("Failures")
                    clusters.forEach { cluster ->
                        val clusterNode = DefaultMutableTreeNode(cluster)
                        val failuresInCluster = pendingFailures.filter { pf -> cluster.failures.any { it["test_name"] == pf.testName && it["error_message"] == pf.errorMessage } }
                        failuresInCluster.forEach { failure -> clusterNode.add(DefaultMutableTreeNode(failure)) }
                        root.add(clusterNode)
                    }
                    uniqueFailures.forEach { failure -> root.add(DefaultMutableTreeNode(failure)) }
                    failuresTreeModel.setRoot(root)
                    for (i in 0 until failuresTree.rowCount) failuresTree.expandRow(i)
                }
            }
        }
    }

    private class FailureTreeCellRenderer : DefaultTreeCellRenderer() {
        override fun getTreeCellRendererComponent(
            tree: javax.swing.JTree, value: Any, sel: Boolean, expanded: Boolean, leaf: Boolean, row: Int, hasFocus: Boolean
        ): Component {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus)
            val node = value as DefaultMutableTreeNode
            when (val userObject = node.userObject) {
                is FailureCluster -> {
                    text = "Cluster: ${userObject.representativeName} (${userObject.count} failures)"
                    icon = AllIcons.Nodes.Folder
                }
                is TestFailure -> {
                    text = userObject.testName
                    icon = AllIcons.RunConfigurations.TestError
                }
            }
            return this
        }
    }
}

package com.cts.jiraplugin.ui

import com.cts.jiraplugin.persistence.DatabaseManager
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

class ClusterPanel : JPanel() {
    private val root = DefaultMutableTreeNode("Failure Clusters")
    private val treeModel = DefaultTreeModel(root)
    private val tree = JTree(treeModel)

    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        add(JScrollPane(tree))
        refreshClusters()
    }

    fun refreshClusters() {
        root.removeAllChildren()

        val issues = DatabaseManager.getAllIssues()

        // Group by clusterId
        val grouped = issues.groupBy { it["clusterId"] ?: "unknown" }

        grouped.forEach { (clusterId, failures) ->
            val clusterNode = DefaultMutableTreeNode("Cluster $clusterId (${failures.size} failures)")
            failures.forEach { issue ->
                val summary = issue["summary"] ?: "No summary"
                val severity = issue["severity"] ?: "Unknown"
                val rootCause = issue["rootCause"] ?: "N/A"
                clusterNode.add(
                    DefaultMutableTreeNode(
                        "${issue["testName"]} | $severity | $rootCause | $summary"
                    )
                )
            }
            root.add(clusterNode)
        }

        treeModel.reload()
        expandAll()
    }

    private fun expandAll() {
        for (i in 0 until tree.rowCount) {
            tree.expandRow(i)
        }
    }
}

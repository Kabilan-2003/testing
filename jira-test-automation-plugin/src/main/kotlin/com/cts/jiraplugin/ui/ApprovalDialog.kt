package com.cts.jiraplugin.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import javax.swing.*

class ApprovalDialog(
    project: Project,
    private val testName: String,
    private val failureMessage: String
) : DialogWrapper(project) {

    init {
        title = "Create Jira Ticket?"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)

        val label = JLabel("Test failed: $testName")
        val message = JTextArea(failureMessage)
        message.isEditable = false
        message.wrapStyleWord = true
        message.lineWrap = true

        panel.add(label)
        panel.add(JScrollPane(message))
        return panel
    }
}

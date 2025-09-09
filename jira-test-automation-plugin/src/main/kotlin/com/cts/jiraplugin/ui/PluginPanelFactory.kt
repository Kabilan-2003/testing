package com.cts.jiraplugin.ui

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import javax.swing.JPanel

class PluginPanelFactory : ToolWindowFactory, DumbAware {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = PluginPanel()
        val contentManager = toolWindow.contentManager
        val content = contentManager.factory.createContent(panel, "", false)
        contentManager.addContent(content)
    }

    override fun isApplicable(project: Project): Boolean = true
}

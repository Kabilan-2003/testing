package com.jira.testautomation.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.DumbAware

/**
 * Action to open the plugin settings
 */
class OpenSettingsAction : AnAction(), DumbAware {
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        ShowSettingsUtil.getInstance().showSettingsDialog(
            project,
            "Jira Test Automation"
        )
    }
}

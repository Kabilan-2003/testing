package com.cts.jiraplugin.ui

import com.cts.jiraplugin.PluginSettingsConfigurable
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.options.ShowSettingsUtil
import org.example.jiratestautomationplugin.ui.PluginSettingsConfigurable

/**
 * Action to open the plugin's settings dialog.
 */
class OpenSettingsAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        ShowSettingsUtil.getInstance().showSettingsDialog(
            e.project,
            PluginSettingsConfigurable::class.java
        )
    }
}
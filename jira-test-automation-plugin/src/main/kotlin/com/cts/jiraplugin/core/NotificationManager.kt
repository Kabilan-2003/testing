package com.cts.jiraplugin.core

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project

/**
 * Small wrapper to show IntelliJ balloon notifications safely on the EDT.
 * Uses Notifications.Bus.notify(...) so you don't need to register a notification group in plugin.xml.
 */
object NotificationManager {
    private const val GROUP_ID = "Jira Test Automation"

    fun notifyInfo(project: Project?, title: String, content: String) {
        ApplicationManager.getApplication().invokeLater {
            val notification = Notification(GROUP_ID, title, content, NotificationType.INFORMATION)
            Notifications.Bus.notify(notification, project)
        }
    }

    fun notifyWarning(project: Project?, title: String, content: String) {
        ApplicationManager.getApplication().invokeLater {
            val notification = Notification(GROUP_ID, title, content, NotificationType.WARNING)
            Notifications.Bus.notify(notification, project)
        }
    }

    fun notifyError(project: Project?, title: String, content: String) {
        ApplicationManager.getApplication().invokeLater {
            val notification = Notification(GROUP_ID, title, content, NotificationType.ERROR)
            Notifications.Bus.notify(notification, project)
        }
    }
}

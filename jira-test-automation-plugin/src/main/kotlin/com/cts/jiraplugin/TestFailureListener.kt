package com.cts.jiraplugin

import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsListener
import com.intellij.execution.testframework.sm.runner.SMTestProxy
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project

class TestFailureListener : SMTRunnerEventsListener {
    override fun onTestFailed(test: SMTestProxy) {
        super.onTestFailed(test)

        // We only care about individual test failures, not suites
        if (!test.isLeaf) {
            return
        }

        val project = test.project
        // Make sure settings are configured before showing a notification
        if (!PluginSettings.areSettingsConfigured) {
            return
        }

        val notificationGroup = NotificationGroupManager.getInstance()
            .getNotificationGroup("Jira Plugin Notifications")

        val notification = notificationGroup.createNotification(
            "Test Failed: ${test.presentableName}",
            "Would you like to create a Jira ticket?",
            NotificationType.WARNING
        )

        // In a later step, we'll add an action here to create the ticket.

        notification.notify(project)
    }
}

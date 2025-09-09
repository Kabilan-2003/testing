package com.cts.jiraplugin.ui

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.vfs.VirtualFile
import com.cts.jiraplugin.persistence.DatabaseManager
import java.io.File
import java.io.FileWriter
import java.time.format.DateTimeFormatter

class ExportAction : AnAction("Export Data") {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        val descriptor = FileChooserDescriptor(false, true, false, false, false, false)
            .withTitle("Select Export Directory")

        FileChooser.chooseFiles(descriptor, project, null) { files ->
            if (files.isNotEmpty()) {
                val directory = files[0]
                exportData(File(directory.path))
            }
        }
    }

    private fun exportData(directory: File) {
        // Export failures
        exportFailuresToCSV(File(directory, "test_failures_export.csv"))

        // Export tickets
        exportTicketsToCSV(File(directory, "jira_tickets_export.csv"))

        // Export analytics
        exportAnalyticsToCSV(File(directory, "analytics_export.csv"))
    }

    private fun exportFailuresToCSV(file: File) {
        FileWriter(file).use { writer ->
            writer.write("Test Name,Error Message,Status,Severity,Detected At\n")

            val failures = DatabaseManager.getFailures()
            failures.forEach { failure ->
                writer.write("${escapeCsv(failure.testName)},${escapeCsv(failure.errorMessage)},${failure.status},${failure.severity},${failure.detectedAt}\n")
            }
        }
    }

    private fun exportTicketsToCSV(file: File) {
        FileWriter(file).use { writer ->
            writer.write("Test Name,Jira Key,Jira URL,Status,Severity,Created At\n")

            val tickets = DatabaseManager.getTickets()
            tickets.forEach { ticket ->
                writer.write("${escapeCsv(ticket.testName)},${ticket.jiraKey},${ticket.jiraUrl},${ticket.status},${ticket.severity},${ticket.createdAt}\n")
            }
        }
    }

    private fun exportAnalyticsToCSV(file: File) {
        FileWriter(file).use { writer ->
            writer.write("Date,Passed Tests,Failed Tests\n")

            val data = DatabaseManager.getPassFailData()
            for (i in data.dates.indices) {
                writer.write("${data.dates[i]},${data.passed[i]},${data.failed[i]}\n")
            }
        }
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
}
package com.cts.jiraplugin.ui

import org.example.jiratestautomationplugin.auth.OAuthManager
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBLabel
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel

class LoginDialog : DialogWrapper(true) {
    init {
        init()
        title = "Login to Jira"
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout(10, 10))

        val messageLabel = JBLabel("<html>Please log in to your Jira account to continue.<br>You will be redirected to your browser for authentication.</html>")
        panel.add(messageLabel, BorderLayout.CENTER)

        val buttonPanel = JPanel(FlowLayout(FlowLayout.CENTER))
        val loginButton = JButton("Login with Atlassian").apply {
            addActionListener {
                OAuthManager.initiateOAuthFlow()
                close(DialogWrapper.OK_EXIT_CODE)
            }
        }
        buttonPanel.add(loginButton)

        panel.add(buttonPanel, BorderLayout.SOUTH)

        return panel
    }
}
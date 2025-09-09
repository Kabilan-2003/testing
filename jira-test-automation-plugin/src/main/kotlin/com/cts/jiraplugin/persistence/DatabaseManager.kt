package com.cts.jiraplugin.persistence

import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet

object DatabaseManager {
    private const val DB_URL = "jdbc:sqlite:jira_plugin.db"

    init {
        getConnection().use { conn ->
            conn.createStatement().use { stmt ->
                stmt.executeUpdate(
                    """
                    CREATE TABLE IF NOT EXISTS issues (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        testName TEXT,
                        jiraKey TEXT,
                        summary TEXT,
                        description TEXT,
                        severity TEXT,
                        clusterId TEXT,
                        rootCause TEXT,
                        status TEXT DEFAULT 'pending',
                        createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                    """
                )
            }
        }
    }

    private fun getConnection(): Connection = DriverManager.getConnection(DB_URL)

    fun saveIssue(
        testName: String,
        jiraKey: String,
        summary: String,
        description: String,
        severity: String,
        clusterId: String?,
        rootCause: String?
    ) {
        getConnection().use { conn ->
            conn.prepareStatement(
                """
                INSERT INTO issues (testName, jiraKey, summary, description, severity, clusterId, rootCause, status) 
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """
            ).apply {
                setString(1, testName)
                setString(2, jiraKey)
                setString(3, summary)
                setString(4, description)
                setString(5, severity)
                setString(6, clusterId)
                setString(7, rootCause)
                setString(8, if (jiraKey.isBlank()) "pending" else "created")
                executeUpdate()
            }
        }
    }

    fun updateStatus(id: Int, status: String) {
        getConnection().use { conn ->
            conn.prepareStatement("UPDATE issues SET status = ? WHERE id = ?").apply {
                setString(1, status)
                setInt(2, id)
                executeUpdate()
            }
        }
    }

    fun markCreated(id: Int, jiraKey: String) {
        getConnection().use { conn ->
            conn.prepareStatement(
                "UPDATE issues SET jiraKey = ?, status = 'created' WHERE id = ?"
            ).apply {
                setString(1, jiraKey)
                setInt(2, id)
                executeUpdate()
            }
        }
    }

    fun getAllIssues(): ResultSet {
        return getConnection().createStatement().executeQuery("SELECT * FROM issues")
    }

    fun getIssuesBySeverity(severity: String): ResultSet {
        return getConnection().prepareStatement(
            "SELECT * FROM issues WHERE severity = ?"
        ).apply {
            setString(1, severity)
        }.executeQuery()
    }

    fun getIssuesByStatus(status: String): ResultSet {
        return getConnection().prepareStatement(
            "SELECT * FROM issues WHERE status = ?"
        ).apply {
            setString(1, status)
        }.executeQuery()
    }

    fun getIssuesByCluster(clusterId: String): ResultSet {
        return getConnection().prepareStatement(
            "SELECT * FROM issues WHERE clusterId = ?"
        ).apply {
            setString(1, clusterId)
        }.executeQuery()
    }

    // ✅ Helper for Analytics
    fun getTicketStats(): ResultSet {
        return getConnection().createStatement().executeQuery(
            "SELECT status, COUNT(*) as count FROM issues GROUP BY status"
        )
    }

    fun getTrendOverTime(): ResultSet {
        return getConnection().createStatement().executeQuery(
            """
            SELECT date(createdAt) as day, 
                   SUM(CASE WHEN status='created' THEN 1 ELSE 0 END) as createdCount,
                   SUM(CASE WHEN status='pending' THEN 1 ELSE 0 END) as pendingCount
            FROM issues
            GROUP BY day
            ORDER BY day
            """
        )
    }
}

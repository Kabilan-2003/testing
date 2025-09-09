package com.cts.jiraplugin.ui

import com.cts.jiraplugin.persistence.DatabaseManager
import org.jfree.chart.ChartFactory
import org.jfree.chart.ChartPanel
import org.jfree.data.category.DefaultCategoryDataset
import org.jfree.data.general.DefaultPieDataset
import java.awt.BorderLayout
import javax.swing.JPanel
import javax.swing.JTabbedPane

class AnalyticsPanel : JPanel(BorderLayout()) {

    private val tabs = JTabbedPane()

    init {
        // Pie chart (status breakdown)
        val pieDataset = DefaultPieDataset()
        val stats = DatabaseManager.getTicketStats()
        while (stats.next()) {
            pieDataset.setValue(stats.getString("status"), stats.getInt("count"))
        }
        val pieChart = ChartFactory.createPieChart("Tickets by Status", pieDataset, true, true, false)
        tabs.addTab("Status Breakdown", ChartPanel(pieChart))

        // Line chart (trend over time)
        val lineDataset = DefaultCategoryDataset()
        val trend = DatabaseManager.getTrendOverTime()
        while (trend.next()) {
            val day = trend.getString("day")
            lineDataset.addValue(trend.getInt("createdCount"), "Created", day)
            lineDataset.addValue(trend.getInt("pendingCount"), "Pending", day)
        }
        val lineChart = ChartFactory.createLineChart(
            "Ticket Trend Over Time",
            "Day",
            "Count",
            lineDataset
        )
        tabs.addTab("Trend", ChartPanel(lineChart))

        add(tabs, BorderLayout.CENTER)
    }
}

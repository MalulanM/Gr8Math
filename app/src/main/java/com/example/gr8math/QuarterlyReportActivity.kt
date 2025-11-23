package com.example.gr8math // Make sure this matches your package name

import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton

class QuarterlyReportActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quarterly_report)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        val reportTable = findViewById<TableLayout>(R.id.reportTable)

        // Dummy Data for Rows
        val reportData = listOf(
            ReportRow(1, 10, "100%", 10),
            ReportRow(2, 10, "100%", 10),
            ReportRow(3, 8, "80%", 10)
        )

        var totalScore = 0
        var totalItems = 0

        // --- Style Header Row Cells ---
        val headerRow = reportTable.getChildAt(0) as TableRow
        for (i in 0 until headerRow.childCount) {
            val cell = headerRow.getChildAt(i)
            cell.setBackgroundColor(ContextCompat.getColor(this, R.color.colorAccent))
        }

        // Add Data Rows dynamically
        for ((index, data) in reportData.withIndex()) {
            val row = TableRow(this)

            row.addView(createCell(data.testNo.toString()))
            row.addView(createCell(data.score.toString()))
            row.addView(createCell(data.percentage))
            row.addView(createCell(data.items.toString()))

            reportTable.addView(row, 1 + index)

            totalScore += data.score
            totalItems += data.items
        }

        // --- Style Footer Row ---
        val footerRow = reportTable.getChildAt(reportTable.childCount - 1) as TableRow

        // 1. Update Values
        findViewById<TextView>(R.id.tvTotalScore).text = totalScore.toString()
        findViewById<TextView>(R.id.tvTotalItems).text = totalItems.toString()

        // 2. Fix the "Total No. of Items" label cell
        val totalLabelCell = footerRow.getChildAt(2) as TextView
        totalLabelCell.text = getString(R.string.total_items)
        totalLabelCell.gravity = Gravity.CENTER

        // --- THIS IS THE FIX: Apply Lexend Font explicitly ---
        try {
            val typeface = ResourcesCompat.getFont(this, R.font.lexend)
            totalLabelCell.setTypeface(typeface, Typeface.BOLD)
        } catch (_: Exception) {
            totalLabelCell.setTypeface(null, Typeface.BOLD) // Fallback
        }
        // ----------------------------------------------------

        // 3. Apply Background Color
        for (i in 0 until footerRow.childCount) {
            val cell = footerRow.getChildAt(i)
            cell.setBackgroundColor(ContextCompat.getColor(this, R.color.colorAccent))
        }

        findViewById<MaterialButton>(R.id.btnGenerateCopy).setOnClickListener {
            Toast.makeText(this, "Copy generated!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createCell(text: String): TextView {
        val tv = TextView(this)
        tv.text = text
        tv.gravity = Gravity.CENTER
        tv.setPadding(16, 16, 16, 16)

        // Apply the border drawable
        tv.setBackgroundResource(R.drawable.table_cell_border)
        tv.setTextColor(ContextCompat.getColor(this, R.color.colorText))

        try {
            val typeface = ResourcesCompat.getFont(this, R.font.lexend)
            tv.typeface = typeface
        } catch (_: Exception) { }

        return tv
    }

    data class ReportRow(val testNo: Int, val score: Int, val percentage: String, val items: Int)
}
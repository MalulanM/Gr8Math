package com.example.gr8math.Activity.StudentModule.Grades

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.Gravity
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.example.gr8math.Data.Model.ReportItem
import com.example.gr8math.R
import com.example.gr8math.Utils.ShowToast
import com.example.gr8math.ViewModel.QuarterlyReportViewModel
import com.example.gr8math.ViewModel.ReportState
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import java.io.File
import java.io.FileOutputStream
import java.text.DateFormatSymbols

class MonthlyReportActivity : AppCompatActivity() {

    private val viewModel: QuarterlyReportViewModel by viewModels()
    private lateinit var reportTable: TableLayout
    private lateinit var tvTotalScore: TextView
    private lateinit var tvTotalItems: TextView

    // 🌟 Class-level variables so the PDF generator can use them later!
    private var currentMonth = -1
    private var currentYear = -1
    private var studentId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quarterly_report)

        initViews()
        setupTableStyling()

        // 🌟 Grab ALL the data passed from the StudentScoresActivity
        studentId = intent.getIntExtra("EXTRA_STUDENT_ID", 0)
        currentMonth = intent.getIntExtra("EXTRA_MONTH", -1)
        currentYear = intent.getIntExtra("EXTRA_YEAR", -1)

        // Observe Data
        setupObservers()

        // Load Data (Passing the month and year to the ViewModel!)
        if (studentId != 0 && currentMonth != -1) {
            viewModel.loadReport(studentId, currentMonth, currentYear)
        } else {
            ShowToast.showMessage(this, "Invalid Data")
        }

        // PDF Button
        findViewById<MaterialButton>(R.id.btnGenerateCopy).setOnClickListener {
            requestStoragePermission()
        }
    }

    private fun initViews() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        reportTable = findViewById(R.id.reportTable)
        tvTotalScore = findViewById(R.id.tvTotalScore)
        tvTotalItems = findViewById(R.id.tvTotalItems)
    }

    private fun setupTableStyling() {
        // Style Header Row
        val headerRow = reportTable.getChildAt(0) as TableRow
        for (i in 0 until headerRow.childCount) {
            val cell = headerRow.getChildAt(i)
            cell.setBackgroundColor(ContextCompat.getColor(this, R.color.colorAccent))
        }

        // Style Footer Row (Pre-styling)
        val footerRow = reportTable.getChildAt(reportTable.childCount - 1) as TableRow
        val labelCell = footerRow.getChildAt(2) as TextView
        labelCell.text = getString(R.string.total_items)
        labelCell.gravity = Gravity.CENTER

        try {
            val tf = ResourcesCompat.getFont(this, R.font.lexend)
            labelCell.setTypeface(tf, Typeface.BOLD)
        } catch (_: Exception) {
            labelCell.setTypeface(null, Typeface.BOLD)
        }

        for (i in 0 until footerRow.childCount) {
            val c = footerRow.getChildAt(i)
            c.setBackgroundColor(ContextCompat.getColor(this, R.color.colorAccent))
        }
    }

    private fun setupObservers() {
        viewModel.state.observe(this) { state ->
            when (state) {
                is ReportState.Loading -> {
                    // Optional: Show loading
                }
                is ReportState.Success -> {
                    populateTable(state.data.items)
                    tvTotalScore.text = state.data.totalScore.toString()
                    tvTotalItems.text = state.data.totalItems.toString()
                }
                is ReportState.Error -> {
                    ShowToast.showMessage(this, state.message)
                }
            }
        }
    }

    private fun populateTable(items: List<ReportItem>) {
        // Remove existing data rows (Keep Header [0] and Footer [Last])
        while (reportTable.childCount > 2) {
            reportTable.removeViewAt(1)
        }

        // 🌟 Check if there are no records for this month
        if (items.isEmpty()) {
            val emptyRow = TableRow(this)
            val emptyText = TextView(this).apply {
                text = "No assessments recorded for this month."
                gravity = Gravity.CENTER
                setPadding(32, 32, 32, 32)
                setTextColor(ContextCompat.getColor(this@MonthlyReportActivity, R.color.colorSubtleText))

                // Make this single text view stretch across all 4 columns
                val params = TableRow.LayoutParams()
                params.span = 4
                layoutParams = params
            }
            emptyRow.addView(emptyText)
            reportTable.addView(emptyRow, 1) // Insert right below the header

            ShowToast.showMessage(this, "No scores found for this month.")
            return // Stop here
        }

        // Normal logic if there ARE records
        var insertIndex = 1
        for (item in items) {
            val row = TableRow(this)
            row.addView(createCell(item.assessmentNumber.toString()))
            row.addView(createCell(item.score.toString()))
            row.addView(createCell(item.percentString))
            row.addView(createCell(item.items.toString()))

            reportTable.addView(row, insertIndex++)
        }
    }

    private fun createCell(text: String): TextView {
        val tv = TextView(this)
        tv.text = text
        tv.gravity = Gravity.CENTER
        tv.setPadding(16, 16, 16, 16)
        tv.setBackgroundResource(R.drawable.table_cell_border)
        tv.setTextColor(ContextCompat.getColor(this, R.color.colorText))

        try {
            val tf = ResourcesCompat.getFont(this, R.font.lexend)
            tv.typeface = tf
        } catch (_: Exception) { }

        return tv
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(permission), 101)
                return
            }
        }
        generatePdf()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            generatePdf()
        }
    }

    private fun generatePdf() {
        val pdfDocument = PdfDocument()
        val paint = Paint()
        val titlePaint = Paint()

        val pageInfo = PdfDocument.PageInfo.Builder(1200, 1800, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        // 🌟 Get the month name (e.g., "February")
        val monthName = if (currentMonth != -1) DateFormatSymbols().months[currentMonth - 1] else ""
        val pdfTitle = "Monthly Report - $monthName $currentYear"

        // Title
        titlePaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        titlePaint.textSize = 48f
        canvas.drawText(pdfTitle, 350f, 80f, titlePaint)

        var yPos = 160f

        // Column headers
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 32f
        canvas.drawText("Test", 80f, yPos, paint)
        canvas.drawText("Score", 280f, yPos, paint)
        canvas.drawText("Percent", 480f, yPos, paint)
        canvas.drawText("Items", 700f, yPos, paint)

        yPos += 40f

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 30f

        for (i in 1 until reportTable.childCount - 1) {
            val row = reportTable.getChildAt(i) as TableRow
            // Safely cast children to TextView
            val t1 = (row.getChildAt(0) as? TextView)?.text.toString() ?: ""
            val t2 = (row.getChildAt(1) as? TextView)?.text.toString() ?: ""
            val t3 = (row.getChildAt(2) as? TextView)?.text.toString() ?: ""
            val t4 = (row.getChildAt(3) as? TextView)?.text.toString() ?: ""

            // Skip drawing if this is our "Empty State" row!
            if (t1 == "No assessments recorded for this month.") {
                canvas.drawText(t1, 80f, yPos, paint)
                yPos += 40f
                continue
            }

            canvas.drawText(t1, 80f, yPos, paint)
            canvas.drawText(t2, 280f, yPos, paint)
            canvas.drawText(t3, 480f, yPos, paint)
            canvas.drawText(t4, 700f, yPos, paint)

            yPos += 40f
        }

        // Footer totals
        yPos += 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Total Score: ${tvTotalScore.text}", 80f, yPos, paint)
        yPos += 40f
        canvas.drawText("Total Items: ${tvTotalItems.text}", 80f, yPos, paint)

        pdfDocument.finishPage(page)

        val directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

        // 🌟 Updated the file name so it says "Monthly_Report_February_2026_..."
        val file = File(
            directory,
            "Monthly_Report_${monthName}_${currentYear}_${System.currentTimeMillis()}.pdf"
        )

        try {
            pdfDocument.writeTo(FileOutputStream(file))
            ShowToast.showMessage(this, "PDF Saved to Downloads!")
        } catch (e: Exception) {
            ShowToast.showMessage(this, "Error saving PDF: ${e.message}")
        }
        pdfDocument.close()
    }
}
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
import android.view.View
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
    private var currentMonth = -1
    private var currentYear = -1
    private var studentId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quarterly_report)

        initViews()
        setupTableStyling()
        setupObservers()

        val studentId = intent.getIntExtra("EXTRA_STUDENT_ID", 0)
        val courseId = intent.getIntExtra("EXTRA_COURSE_ID", 0) // 🌟 ADD THIS
        val currentMonth = intent.getIntExtra("EXTRA_MONTH", -1)
        val currentYear = intent.getIntExtra("EXTRA_YEAR", -1)

        if (studentId != 0 && courseId != 0) {
            viewModel.loadReport(courseId, studentId, currentMonth, currentYear) // 🌟 Pass it here
        }

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
        val headerRow = reportTable.getChildAt(0) as TableRow
        for (i in 0 until headerRow.childCount) {
            headerRow.getChildAt(i).setBackgroundColor(ContextCompat.getColor(this, R.color.colorAccent))
        }

        // Style Footer Row
        val footerRow = reportTable.getChildAt(reportTable.childCount - 1) as TableRow

        // Update Footer Labels to be clear
        val scoreLabelCell = footerRow.getChildAt(0) as TextView
        scoreLabelCell.text = "Overall Total Points"

        val itemsLabelCell = footerRow.getChildAt(2) as TextView
        itemsLabelCell.text = "Total No. of Items"
        itemsLabelCell.gravity = Gravity.CENTER

        try {
            val tf = ResourcesCompat.getFont(this, R.font.lexend)
            scoreLabelCell.setTypeface(tf, Typeface.BOLD)
            itemsLabelCell.setTypeface(tf, Typeface.BOLD)
        } catch (_: Exception) {
            scoreLabelCell.setTypeface(null, Typeface.BOLD)
            itemsLabelCell.setTypeface(null, Typeface.BOLD)
        }

        for (i in 0 until footerRow.childCount) {
            footerRow.getChildAt(i).setBackgroundColor(ContextCompat.getColor(this, R.color.colorAccent))
        }
    }

    private fun setupObservers() {
        viewModel.state.observe(this) { state ->
            when (state) {
                is ReportState.Loading -> { }
                is ReportState.Success -> {
                    populateTable(state.data.items)
                    // tvTotalScore should show total points earned vs total possible
                    tvTotalScore.text = "${state.data.totalScore}"
                    tvTotalItems.text = state.data.totalItems.toString()
                }
                is ReportState.Error -> {
                    ShowToast.showMessage(this, state.message)
                }
            }
        }
    }

    private fun populateTable(items: List<ReportItem>) {
        // Keep Header [0] and Footer [Last], remove old data
        while (reportTable.childCount > 2) {
            reportTable.removeViewAt(1)
        }

        if (items.isEmpty()) {
            val emptyRow = TableRow(this)
            val emptyText = TextView(this).apply {
                text = "No assessments recorded for this month."
                gravity = Gravity.CENTER
                setPadding(32, 32, 32, 32)
                setTextColor(ContextCompat.getColor(this@MonthlyReportActivity, R.color.colorSubtleText))
                val params = TableRow.LayoutParams()
                params.span = 4
                layoutParams = params
            }
            emptyRow.addView(emptyText)
            reportTable.addView(emptyRow, 1)
            return
        }

        var insertIndex = 1
        val df = java.text.DecimalFormat("#") // Removes decimals for whole points

        for (item in items) {
            val row = TableRow(this)

            // 1. Assessment Number
            row.addView(createCell(item.assessmentNumber.toString()))

            // 2. Score Column (Shows 2/2)
            // 🌟 Uses totalPoints directly from the ReportItem
            val scoreDisplay = "${df.format(item.score)}/${df.format(item.totalPoints)}"
            row.addView(createCell(scoreDisplay))

            // 3. Percentage
            row.addView(createCell(item.percentString))

            // 4. Items Column (Shows 1)
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
            tv.typeface = ResourcesCompat.getFont(this, R.font.lexend)
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

        val monthName = if (currentMonth != -1) DateFormatSymbols().months[currentMonth - 1] else ""
        val pdfTitle = "Monthly Report - $monthName $currentYear"

        titlePaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        titlePaint.textSize = 40f
        canvas.drawText(pdfTitle, 80f, 80f, titlePaint)

        var yPos = 160f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 28f

        // Headers
        canvas.drawText("No.", 80f, yPos, paint)
        canvas.drawText("Assessment Score", 200f, yPos, paint)
        canvas.drawText("Percentage", 550f, yPos, paint)
        canvas.drawText("No. of Items", 850f, yPos, paint)

        yPos += 40f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

        for (i in 1 until reportTable.childCount - 1) {
            val row = reportTable.getChildAt(i) as TableRow
            val t1 = (row.getChildAt(0) as? TextView)?.text.toString() ?: ""
            val t2 = (row.getChildAt(1) as? TextView)?.text.toString() ?: ""
            val t3 = (row.getChildAt(2) as? TextView)?.text.toString() ?: ""
            val t4 = (row.getChildAt(3) as? TextView)?.text.toString() ?: ""

            if (t1.contains("No assessments")) {
                canvas.drawText(t1, 80f, yPos, paint)
                yPos += 40f
                continue
            }

            canvas.drawText(t1, 80f, yPos, paint)
            canvas.drawText(t2, 200f, yPos, paint)
            canvas.drawText(t3, 550f, yPos, paint)
            canvas.drawText(t4, 850f, yPos, paint)
            yPos += 40f
        }

        yPos += 40f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Overall Total Points: ${tvTotalScore.text}", 80f, yPos, paint)
        yPos += 40f
        canvas.drawText("Total No. of Items: ${tvTotalItems.text}", 80f, yPos, paint)

        pdfDocument.finishPage(page)
        val directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(directory, "Monthly_Report_${monthName}_${currentYear}_${System.currentTimeMillis()}.pdf")

        try {
            pdfDocument.writeTo(FileOutputStream(file))
            ShowToast.showMessage(this, "PDF Saved to Downloads!")
        } catch (e: Exception) {
            ShowToast.showMessage(this, "Error saving PDF: ${e.message}")
        }
        pdfDocument.close()
    }
}
package com.example.gr8math.Activity.StudentModule.Grades

import android.Manifest
import android.app.Dialog
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.HorizontalScrollView
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
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar
import java.util.Locale

class MonthlyReportActivity : AppCompatActivity() {

    private val viewModel: QuarterlyReportViewModel by viewModels()

    // UI Elements
    private lateinit var reportTable: TableLayout
    private lateinit var tvTotalScore: TextView
    private lateinit var tvTotalItems: TextView
    private lateinit var tableScrollView: HorizontalScrollView
    private lateinit var tvEmptyState: TextView

    // Global State Variables
    private var courseId = 0
    private var studentId = 0
    private var studentName = "Student" // Default fallback
    private var selectedMonthValue: String = "" // e.g., "2026-05"
    private var currentDisplayLabel: String = "" // e.g., "May 2026"
    private var pendingDownloadIsPdf = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quarterly_report)

        // Get intents needed for the ViewModel
        studentId = intent.getIntExtra("EXTRA_STUDENT_ID", 0)
        courseId = intent.getIntExtra("EXTRA_COURSE_ID", 0)
        intent.getStringExtra("EXTRA_STUDENT_NAME")?.let { studentName = it }

        initViews()
        setupObservers()

        // Setup Features
        setupAcademicDropdown()
        setupDownloadDialog()
    }

    private fun initViews() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        reportTable = findViewById(R.id.reportTable)
        tvTotalScore = findViewById(R.id.tvTotalScore)
        tvTotalItems = findViewById(R.id.tvTotalItems)
        tableScrollView = findViewById(R.id.tableScrollView)
        tvEmptyState = findViewById(R.id.tvEmptyState)
    }

    // ==========================================
    // 1. ACADEMIC DROPDOWN LOGIC
    // ==========================================
    private fun setupAcademicDropdown() {
        val actvMonth = findViewById<AutoCompleteTextView>(R.id.actvMonth)
        val tvQuarter = findViewById<TextView>(R.id.tvQuarter)

        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH) + 1 // Calendar.MONTH is 0-11
        val START_MONTH = 6 // Academic Year starts in June

        val acadStartYear = if (currentMonth >= START_MONTH) currentYear else currentYear - 1

        val valuesList = mutableListOf<String>()
        val displayList = mutableListOf<String>()

        var iterYear = acadStartYear
        var iterMonth = START_MONTH

        while (iterYear < currentYear || (iterYear == currentYear && iterMonth <= currentMonth)) {
            val value = String.format(Locale.US, "%04d-%02d", iterYear, iterMonth)
            valuesList.add(value)

            val tempCal = Calendar.getInstance().apply {
                set(Calendar.YEAR, iterYear)
                set(Calendar.MONTH, iterMonth - 1)
            }
            val display = java.text.SimpleDateFormat("MMMM yyyy", Locale.US).format(tempCal.time)
            displayList.add(display)

            iterMonth++
            if (iterMonth > 12) {
                iterMonth = 1
                iterYear++
            }
        }

        valuesList.reverse()
        displayList.reverse()

        if (displayList.isNotEmpty()) {
            selectedMonthValue = valuesList[0]
            currentDisplayLabel = displayList[0]
            tvQuarter.text = currentDisplayLabel
            actvMonth.setText(currentDisplayLabel, false)
            triggerNetworkLoad()
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, displayList)
        actvMonth.setAdapter(adapter)

        actvMonth.setOnItemClickListener { _, _, position, _ ->
            selectedMonthValue = valuesList[position]
            currentDisplayLabel = displayList[position]
            tvQuarter.text = currentDisplayLabel
            triggerNetworkLoad()
        }
    }

    private fun triggerNetworkLoad() {
        if (courseId != 0 && studentId != 0 && selectedMonthValue.isNotEmpty()) {
            val parts = selectedMonthValue.split("-")
            val year = parts[0].toInt()
            val month = parts[1].toInt()
            viewModel.loadReport(courseId, studentId, month, year)
        }
    }

    // ==========================================
    // 2. TABLE & EMPTY STATE LOGIC
    // ==========================================
    private fun setupObservers() {
        viewModel.state.observe(this) { state ->
            when (state) {
                is ReportState.Loading -> { }
                is ReportState.Success -> {
                    val items = state.data.items
                    val df = java.text.DecimalFormat("#")
                    tvTotalScore.text = df.format(state.data.totalScore)
                    tvTotalItems.text = state.data.totalItems.toString()

                    if (items.isEmpty()) {
                        tvEmptyState.text = "No assessments found for $currentDisplayLabel."
                        tvEmptyState.visibility = View.VISIBLE
                        tableScrollView.visibility = View.GONE
                    } else {
                        tvEmptyState.visibility = View.GONE
                        tableScrollView.visibility = View.VISIBLE
                        populateTable(items)
                    }
                }
                is ReportState.Error -> {
                    ShowToast.showMessage(this, state.message)
                    tvEmptyState.text = "Failed to load data."
                    tvEmptyState.visibility = View.VISIBLE
                    tableScrollView.visibility = View.GONE
                }
            }
        }
    }

    private fun populateTable(items: List<ReportItem>) {
        while (reportTable.childCount > 2) {
            reportTable.removeViewAt(1)
        }

        var insertIndex = 1
        val df = java.text.DecimalFormat("#")

        for (item in items) {
            val row = TableRow(this)
            row.addView(createCell(item.assessmentNumber.toString()))
            val scoreDisplay = "${df.format(item.score)}/${df.format(item.totalPoints)}"
            row.addView(createCell(scoreDisplay))
            row.addView(createCell(item.percentString))
            row.addView(createCell(item.items.toString()))
            reportTable.addView(row, insertIndex++)
        }
    }

    private fun createCell(text: String): TextView {
        val tv = TextView(this)
        tv.text = text
        tv.gravity = Gravity.CENTER
        tv.setPadding(16, 40, 16, 40)
        tv.setTextColor(Color.parseColor("#101720"))
        try { tv.typeface = ResourcesCompat.getFont(this, R.font.lexend) } catch (_: Exception) { }
        return tv
    }

    // ==========================================
    // 3. EXPORT & DOWNLOAD LOGIC
    // ==========================================
    private fun setupDownloadDialog() {
        val btnGenerateCopy = findViewById<Button>(R.id.btnGenerateCopy)

        btnGenerateCopy.setOnClickListener {
            if (tableScrollView.visibility == View.GONE) {
                ShowToast.showMessage(this, "No data available to export.")
                return@setOnClickListener
            }

            // Inflates the CUSTOM Dialog XML you provided
            val dialog = Dialog(this)
            dialog.setContentView(R.layout.dialog_export_report)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

            // Update text to match student name context
            val tvSubtitle = dialog.findViewById<TextView>(R.id.tvSubtitle)
            tvSubtitle.text = "Choose a format to download the monthly report for $studentName."

            dialog.findViewById<Button>(R.id.btnDownloadPdf).setOnClickListener {
                pendingDownloadIsPdf = true
                dialog.dismiss()
                requestStoragePermission()
            }

            dialog.findViewById<Button>(R.id.btnDownloadCsv).setOnClickListener {
                pendingDownloadIsPdf = false
                dialog.dismiss()
                requestStoragePermission()
            }

            dialog.findViewById<Button>(R.id.btnCancel).setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()
        }
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(permission), 101)
                return
            }
        }
        if (pendingDownloadIsPdf) generatePdf() else generateCsv()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (pendingDownloadIsPdf) generatePdf() else generateCsv()
        }
    }

    // --- EXACT WEB EXCEL FORMAT ---
    private fun generateCsv() {
        val directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val safeName = studentName.replace(Regex("[^a-zA-Z0-9]"), "_").lowercase()
        val fileName = "Monthly_Report_${safeName}_${selectedMonthValue}.csv"
        val file = File(directory, fileName)

        try {
            val writer = file.bufferedWriter()
            writer.write("Assessment Test No.,Assessment Test Score,Percentage of Score,No. of Items\n")

            for (i in 1 until reportTable.childCount - 1) {
                val row = reportTable.getChildAt(i) as TableRow
                val t1 = (row.getChildAt(0) as? TextView)?.text.toString()
                val t2 = (row.getChildAt(1) as? TextView)?.text.toString()
                val t3 = (row.getChildAt(2) as? TextView)?.text.toString()
                val t4 = (row.getChildAt(3) as? TextView)?.text.toString()
                writer.write("$t1,=\"$t2\",$t3,$t4\n") // Enclosed for Excel format protection
            }

            writer.write("Total Score,${tvTotalScore.text},Total No. of Items,${tvTotalItems.text}\n")
            writer.close()
            ShowToast.showMessage(this, "Excel (CSV) Saved to Downloads!")
        } catch (e: Exception) {
            ShowToast.showMessage(this, "Error saving CSV: ${e.message}")
        }
    }

    // --- EXACT WEB HIDDEN PDF FORMAT (Gray & White Grid) ---
    private fun generatePdf() {
        val pdfDocument = PdfDocument()

        // Setup matching Web Styles
        val textPaint = Paint()
        val bgPaint = Paint()
        val borderPaint = Paint().apply {
            color = Color.parseColor("#CCCCCC") // Web table border color
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }

        // Standard PDF Document size (Letter size approximation)
        val pageWidth = 1200
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, 1600, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val leftMargin = 80f
        var yPos = 80f

        // 1. Draw Titles (Matches Web <h1>, <h2>, <h3>)
        textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        textPaint.textSize = 36f
        textPaint.color = Color.BLACK
        canvas.drawText("Monthly Completion Report", leftMargin, yPos, textPaint)

        yPos += 40f
        textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        textPaint.textSize = 28f
        textPaint.color = Color.parseColor("#555555")
        canvas.drawText("Student: $studentName", leftMargin, yPos, textPaint)

        yPos += 36f
        textPaint.textSize = 24f
        canvas.drawText("Month: $currentDisplayLabel", leftMargin, yPos, textPaint)

        yPos += 60f

        // 2. Setup Table Matrix Parameters
        val tableWidth = pageWidth - (leftMargin * 2)
        val colWidth = tableWidth / 4
        val rowHeight = 60f

        // 3. Draw Header Background (#f0f0f0) & Borders
        bgPaint.color = Color.parseColor("#F0F0F0")
        bgPaint.style = Paint.Style.FILL
        canvas.drawRect(leftMargin, yPos, leftMargin + tableWidth, yPos + rowHeight, bgPaint)

        // Draw Header Cell Borders
        for (i in 0..4) {
            canvas.drawLine(leftMargin + (colWidth * i), yPos, leftMargin + (colWidth * i), yPos + rowHeight, borderPaint)
        }
        canvas.drawLine(leftMargin, yPos, leftMargin + tableWidth, yPos, borderPaint)
        canvas.drawLine(leftMargin, yPos + rowHeight, leftMargin + tableWidth, yPos + rowHeight, borderPaint)

        // Header Text
        textPaint.color = Color.BLACK
        textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        textPaint.textSize = 20f
        textPaint.textAlign = Paint.Align.CENTER

        val textYOffset = yPos + 40f
        canvas.drawText("Assessment Test No.", leftMargin + (colWidth * 0.5f), textYOffset, textPaint)
        canvas.drawText("Assessment Test Score", leftMargin + (colWidth * 1.5f), textYOffset, textPaint)
        canvas.drawText("Percentage of Score", leftMargin + (colWidth * 2.5f), textYOffset, textPaint)
        canvas.drawText("No. of Items", leftMargin + (colWidth * 3.5f), textYOffset, textPaint)

        yPos += rowHeight

        // 4. Draw Data Rows
        textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        for (i in 1 until reportTable.childCount - 1) {
            val row = reportTable.getChildAt(i) as TableRow
            val t1 = (row.getChildAt(0) as? TextView)?.text.toString()
            val t2 = (row.getChildAt(1) as? TextView)?.text.toString()
            val t3 = (row.getChildAt(2) as? TextView)?.text.toString()
            val t4 = (row.getChildAt(3) as? TextView)?.text.toString()

            // Draw Data Borders
            for (col in 0..4) {
                canvas.drawLine(leftMargin + (colWidth * col), yPos, leftMargin + (colWidth * col), yPos + rowHeight, borderPaint)
            }
            canvas.drawLine(leftMargin, yPos + rowHeight, leftMargin + tableWidth, yPos + rowHeight, borderPaint)

            // Draw Data Text
            val rowTextY = yPos + 40f
            canvas.drawText(t1, leftMargin + (colWidth * 0.5f), rowTextY, textPaint)
            canvas.drawText(t2, leftMargin + (colWidth * 1.5f), rowTextY, textPaint)
            canvas.drawText(t3, leftMargin + (colWidth * 2.5f), rowTextY, textPaint)
            canvas.drawText(t4, leftMargin + (colWidth * 3.5f), rowTextY, textPaint)

            yPos += rowHeight
        }

        // 5. Draw Footer Background (#f0f0f0) & Borders
        canvas.drawRect(leftMargin, yPos, leftMargin + tableWidth, yPos + rowHeight, bgPaint)
        for (i in 0..4) {
            canvas.drawLine(leftMargin + (colWidth * i), yPos, leftMargin + (colWidth * i), yPos + rowHeight, borderPaint)
        }
        canvas.drawLine(leftMargin, yPos + rowHeight, leftMargin + tableWidth, yPos + rowHeight, borderPaint)

        // Footer Text
        textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        val footerTextY = yPos + 40f
        canvas.drawText("Total Score", leftMargin + (colWidth * 0.5f), footerTextY, textPaint)
        canvas.drawText(tvTotalScore.text.toString(), leftMargin + (colWidth * 1.5f), footerTextY, textPaint)
        canvas.drawText("Total No. of Items", leftMargin + (colWidth * 2.5f), footerTextY, textPaint)
        canvas.drawText(tvTotalItems.text.toString(), leftMargin + (colWidth * 3.5f), footerTextY, textPaint)

        pdfDocument.finishPage(page)

        // Save Document
        val directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val safeName = studentName.replace(Regex("[^a-zA-Z0-9]"), "_").lowercase()
        val file = File(directory, "Monthly_Report_${safeName}_${selectedMonthValue}.pdf")

        try {
            pdfDocument.writeTo(FileOutputStream(file))
            ShowToast.showMessage(this, "PDF Saved to Downloads!")
        } catch (e: Exception) {
            ShowToast.showMessage(this, "Error saving PDF: ${e.message}")
        }
        pdfDocument.close()
    }
}
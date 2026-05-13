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
    private var isStudentView = false // Track if viewed by a student
    private var selectedMonthValue: String = "" // e.g., "2026-05"
    private var currentDisplayLabel: String = "" // e.g., "May 2026"
    private var pendingDownloadIsPdf = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quarterly_report)

        // Get intents needed for the ViewModel
        studentId = intent.getIntExtra("EXTRA_STUDENT_ID", 0)
        courseId = intent.getIntExtra("EXTRA_COURSE_ID", 0)
        isStudentView = intent.getBooleanExtra("EXTRA_IS_STUDENT", false)
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
                    val allItems = state.data.items

                    // 1. FILTER THE LIST based on the selected dropdown ("2026-04")
                    val filteredItems = allItems.filter { item ->
                        val dateStr = item.dateAccomplished
                        if (dateStr.length >= 7) {
                            val itemMonthYear = dateStr.substring(0, 7)
                            itemMonthYear == selectedMonthValue // Compare to dropdown
                        } else {
                            false
                        }
                    }

                    // 2. RECALCULATE TOTALS based only on the filtered items
                    val df = java.text.DecimalFormat("#")
                    val calculatedTotalScore = filteredItems.sumOf { it.score }
                    val calculatedTotalItems = filteredItems.sumOf { it.items }

                    tvTotalScore.text = df.format(calculatedTotalScore)
                    tvTotalItems.text = calculatedTotalItems.toString()

                    tvEmptyState.visibility = View.GONE
                    tableScrollView.visibility = View.VISIBLE

                    populateTable(filteredItems)
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

        if (items.isEmpty()) {
            val emptyRow = TableRow(this)
            val tvEmpty = TextView(this)

            tvEmpty.text = "No assessments found for $currentDisplayLabel."
            tvEmpty.gravity = Gravity.CENTER
            tvEmpty.setPadding(16, 80, 16, 80)
            tvEmpty.setTextColor(Color.parseColor("#A0A0A0"))
            tvEmpty.setTypeface(null, Typeface.ITALIC)

            // Span across all 4 columns
            val params = TableRow.LayoutParams()
            params.span = 4
            tvEmpty.layoutParams = params

            emptyRow.addView(tvEmpty)
            reportTable.addView(emptyRow, 1)

            // Ensure totals reflect zero
            tvTotalScore.text = "0"
            tvTotalItems.text = "0"
            return
        }

        // Proceed normally if there are items
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

        // Hide button if the activity is being viewed by a student
        if (isStudentView) {
            btnGenerateCopy.visibility = View.GONE
            return
        }

        btnGenerateCopy.visibility = View.VISIBLE
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

    // --- EXACT HTML TO PDF GENERATOR (Matches Web Layout) ---
    private fun generatePdf() {
        // Extract row data to inject into HTML
        val rowsHtml = java.lang.StringBuilder()
        for (i in 1 until reportTable.childCount - 1) {
            val row = reportTable.getChildAt(i) as TableRow
            val t1 = (row.getChildAt(0) as? TextView)?.text.toString()
            val t2 = (row.getChildAt(1) as? TextView)?.text.toString()
            val t3 = (row.getChildAt(2) as? TextView)?.text.toString()
            val t4 = (row.getChildAt(3) as? TextView)?.text.toString()

            // Apply inline border styles directly to the <td> elements
            rowsHtml.append("""
                <tr>
                    <td style="border: 1px solid #cccccc; padding: 10px;">$t1</td>
                    <td style="border: 1px solid #cccccc; padding: 10px;">$t2</td>
                    <td style="border: 1px solid #cccccc; padding: 10px;">$t3</td>
                    <td style="border: 1px solid #cccccc; padding: 10px;">$t4</td>
                </tr>
            """.trimIndent())
        }

        // Build HTML string exact to the Web version without a <style> block
        val htmlString = """
            <!DOCTYPE html>
            <html>
            <body style="font-family: sans-serif; padding: 40px; color: black; background: white;">
                <div style="margin-bottom: 30px;">
                    <h1 style="font-size: 24px; margin: 0 0 10px 0; font-weight: bold;">Monthly Completion Report</h1>
                    <h2 style="font-size: 18px; margin: 0; color: #555555;">Student: $studentName</h2>
                    <h3 style="font-size: 16px; margin: 5px 0 0 0; color: #555555;">Month: $currentDisplayLabel</h3>
                </div>
                
                <table style="width: 100%; border-collapse: collapse; text-align: center; margin-top: 30px;">
                    <thead>
                        <tr style="background-color: #f0f0f0; font-weight: bold;">
                            <th style="border: 1px solid #cccccc; padding: 12px;">Assessment Test No.</th>
                            <th style="border: 1px solid #cccccc; padding: 12px;">Assessment Test Score</th>
                            <th style="border: 1px solid #cccccc; padding: 12px;">Percentage of Score</th>
                            <th style="border: 1px solid #cccccc; padding: 12px;">No. of Items</th>
                        </tr>
                    </thead>
                    <tbody>
                        $rowsHtml
                    </tbody>
                    <tfoot>
                        <tr style="background-color: #f0f0f0; font-weight: bold;">
                            <td style="border: 1px solid #cccccc; padding: 12px;">Total Score</td>
                            <td style="border: 1px solid #cccccc; padding: 12px;">${tvTotalScore.text}</td>
                            <td style="border: 1px solid #cccccc; padding: 12px;">Total No. of Items</td>
                            <td style="border: 1px solid #cccccc; padding: 12px;">${tvTotalItems.text}</td>
                        </tr>
                    </tfoot>
                </table>
            </body>
            </html>
        """.trimIndent()

        // Use Android's WebView Print Manager to render the HTML perfectly to PDF
        val webView = android.webkit.WebView(this)
        webView.webViewClient = object : android.webkit.WebViewClient() {
            override fun onPageFinished(view: android.webkit.WebView, url: String) {
                val printManager = getSystemService(android.content.Context.PRINT_SERVICE) as android.print.PrintManager
                val safeName = studentName.replace(Regex("[^a-zA-Z0-9]"), "_").lowercase()
                val jobName = "Monthly_Report_${safeName}_${selectedMonthValue}"

                val printAdapter = webView.createPrintDocumentAdapter(jobName)
                printManager.print(jobName, printAdapter, android.print.PrintAttributes.Builder().build())

                ShowToast.showMessage(this@MonthlyReportActivity, "Preparing PDF Download...")
            }
        }
        webView.loadDataWithBaseURL(null, htmlString, "text/HTML", "UTF-8", null)
    }
}
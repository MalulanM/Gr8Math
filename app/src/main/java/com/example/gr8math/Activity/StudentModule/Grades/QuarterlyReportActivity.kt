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

class QuarterlyReportActivity : AppCompatActivity() {

    private val viewModel: QuarterlyReportViewModel by viewModels()
    private lateinit var reportTable: TableLayout
    private lateinit var tvTotalScore: TextView
    private lateinit var tvTotalItems: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quarterly_report)

        initViews()
        setupTableStyling()

        val studentId = intent.getIntExtra("EXTRA_STUDENT_ID", 0)

        // Observe Data
        setupObservers()

        // Load Data
        if (studentId != 0) {
            viewModel.loadReport(studentId)
        } else {
            ShowToast.showMessage(this, "Invalid Student ID")
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
        // We repeatedly remove index 1 until only Header and Footer remain
        while (reportTable.childCount > 2) {
            reportTable.removeViewAt(1)
        }

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

    // ================= PDF PERMISSION & GENERATION =====================
    // Kept mostly identical to your logic as it deals with System APIs

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

        // Title
        titlePaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        titlePaint.textSize = 48f
        canvas.drawText("Quarterly Report", 400f, 80f, titlePaint)

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


        val dataRowCount = reportTable.childCount - 2


        for (i in 1 until reportTable.childCount - 1) {
            val row = reportTable.getChildAt(i) as TableRow
            // Safely cast children to TextView
            val t1 = (row.getChildAt(0) as? TextView)?.text.toString()
            val t2 = (row.getChildAt(1) as? TextView)?.text.toString()
            val t3 = (row.getChildAt(2) as? TextView)?.text.toString()
            val t4 = (row.getChildAt(3) as? TextView)?.text.toString()

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
        val file = File(
            directory,
            "Quarterly_Report_${System.currentTimeMillis()}.pdf"
        ) // Added timestamp to avoid overwrite

        try {
            pdfDocument.writeTo(FileOutputStream(file))
            ShowToast.showMessage(this, "PDF Saved to Downloads!")
        } catch (e: Exception) {
            ShowToast.showMessage(this, "Error saving PDF: ${e.message}")
        }
        pdfDocument.close()
    }
}
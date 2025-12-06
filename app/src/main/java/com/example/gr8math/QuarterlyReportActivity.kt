package com.example.gr8math

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.Gravity
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.example.gr8math.api.ConnectURL
import com.example.gr8math.dataObject.CurrentCourse
import com.example.gr8math.utils.ShowToast
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import java.io.File
import java.io.FileOutputStream

class QuarterlyReportActivity : AppCompatActivity() {

    private var studentId = 0
    private lateinit var reportTable: TableLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quarterly_report)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        reportTable = findViewById(R.id.reportTable)
        studentId = intent.getIntExtra("EXTRA_STUDENT_ID", 0)

        val headerRow = reportTable.getChildAt(0) as TableRow
        for (i in 0 until headerRow.childCount) {
            val cell = headerRow.getChildAt(i)
            cell.setBackgroundColor(ContextCompat.getColor(this, R.color.colorAccent))
        }

        displayQuarter1Assessments()

        // PDF EXPORT BUTTON
        findViewById<MaterialButton>(R.id.btnGenerateCopy).setOnClickListener {
            requestStoragePermission()
        }
    }

    // ================= PDF PERMISSION =====================

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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 101 &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            generatePdf()
        }
    }


    // ==================== TABLE CELL ======================

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

    // =============== FETCH QUARTER 1 ASSESSMENTS ==============

    private fun displayQuarter1Assessments() {
        val call = ConnectURL.api.getStudentAssessment(CurrentCourse.courseId, studentId)

        call.enqueue(object : retrofit2.Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: retrofit2.Response<ResponseBody>) {
                val body = response.body()?.string() ?: return
                Log.d("API_RESPONSE", body)

                try {
                    val json = JSONObject(body)
                    val arr = json.getJSONArray("answered_assessments")

                    while (reportTable.childCount > 2) reportTable.removeViewAt(1)

                    var totalScore = 0
                    var totalItems = 0
                    var insertIndex = 1

                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)

                        if (obj.getInt("assessment_quarter") != 1) continue

                        val testNo = obj.getInt("assessment_number")
                        val score = obj.getInt("score")
                        val items = obj.getInt("assessment_items")

                        val percent = ((score.toFloat() / items) * 100).toInt().toString() + "%"

                        val row = TableRow(this@QuarterlyReportActivity)
                        row.addView(createCell(testNo.toString()))
                        row.addView(createCell(score.toString()))
                        row.addView(createCell(percent))
                        row.addView(createCell(items.toString()))

                        reportTable.addView(row, insertIndex++)
                        totalScore += score
                        totalItems += items
                    }

                    findViewById<TextView>(R.id.tvTotalScore).text = totalScore.toString()
                    findViewById<TextView>(R.id.tvTotalItems).text = totalItems.toString()

                    val footerRow = reportTable.getChildAt(reportTable.childCount - 1) as TableRow
                    val labelCell = footerRow.getChildAt(2) as TextView
                    labelCell.text = getString(R.string.total_items)
                    labelCell.gravity = Gravity.CENTER

                    try {
                        val tf = ResourcesCompat.getFont(this@QuarterlyReportActivity, R.font.lexend)
                        labelCell.setTypeface(tf, Typeface.BOLD)
                    } catch (_: Exception) {
                        labelCell.setTypeface(null, Typeface.BOLD)
                    }

                    for (i in 0 until footerRow.childCount) {
                        val c = footerRow.getChildAt(i)
                        c.setBackgroundColor(ContextCompat.getColor(this@QuarterlyReportActivity, R.color.colorAccent))
                    }

                } catch (e: Exception) {

                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                ShowToast.showMessage(this@QuarterlyReportActivity, "Failed to connect to server.")
            }
        })
    }

    // ===================== PDF GENERATE =======================

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

        // Table rows
        for (i in 1 until reportTable.childCount - 1) {
            val row = reportTable.getChildAt(i) as TableRow

            canvas.drawText((row.getChildAt(0) as TextView).text.toString(), 80f, yPos, paint)
            canvas.drawText((row.getChildAt(1) as TextView).text.toString(), 280f, yPos, paint)
            canvas.drawText((row.getChildAt(2) as TextView).text.toString(), 480f, yPos, paint)
            canvas.drawText((row.getChildAt(3) as TextView).text.toString(), 700f, yPos, paint)

            yPos += 40f
        }

        // Footer totals
        yPos += 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        canvas.drawText("Total Score: ${findViewById<TextView>(R.id.tvTotalScore).text}", 80f, yPos, paint)
        yPos += 40f
        canvas.drawText("Total Items: ${findViewById<TextView>(R.id.tvTotalItems).text}", 80f, yPos, paint)

        pdfDocument.finishPage(page)

        val directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(directory, "Quarterly_Report.pdf")

        try {
            pdfDocument.writeTo(FileOutputStream(file))
            ShowToast.showMessage(this, "PDF Saved to Downloads!")
        } catch (e: Exception) {
            ShowToast.showMessage(this,"Error saving PDF" )

               }

        pdfDocument.close()
    }
}

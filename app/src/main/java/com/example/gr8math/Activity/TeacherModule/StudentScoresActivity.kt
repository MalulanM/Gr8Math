package com.example.gr8math.Activity.TeacherModule

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gr8math.Activity.StudentModule.Grades.QuarterlyReportActivity
import com.example.gr8math.Data.Model.StudentScore
import com.example.gr8math.Model.CurrentCourse
import com.example.gr8math.R
import com.example.gr8math.Utils.ShowToast
import com.example.gr8math.ViewModel.ScoreState
import com.example.gr8math.ViewModel.StudentScoresViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class StudentScoresActivity : AppCompatActivity() {

    private val viewModel: StudentScoresViewModel by viewModels()
    private lateinit var rvScores: RecyclerView

    private var studentName = ""
    private var studentId = 0
    private var autoOpenAssessmentId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_scores)

        studentName = intent.getStringExtra("EXTRA_STUDENT_NAME") ?: "Student"
        studentId = intent.getIntExtra("EXTRA_STUDENT_ID", 0)
        autoOpenAssessmentId = intent.getIntExtra("AUTO_ASSESSMENT_ID", -1)
        if (autoOpenAssessmentId == -1) autoOpenAssessmentId = null

        setupToolbar()
        findViewById<TextView>(R.id.tvStudentName).text = studentName

        val btnQuarterlyReport = findViewById<Button>(R.id.btnQuarterlyReport)
        btnQuarterlyReport.setOnClickListener {
            val intent = Intent(this, QuarterlyReportActivity::class.java)
            intent.putExtra("EXTRA_STUDENT_ID", studentId)
            startActivity(intent)
        }

        rvScores = findViewById(R.id.rvScores)
        rvScores.layoutManager = LinearLayoutManager(this)

        setupObservers()
        viewModel.loadScores(CurrentCourse.courseId, studentId)
    }

    private fun setupToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupObservers() {
        viewModel.state.observe(this) { state ->
            when (state) {
                is ScoreState.Loading -> { }
                is ScoreState.Success -> {
                    val list = state.data
                    if (list.isEmpty()) {
                        ShowToast.showMessage(this, "No scores found.")
                    }

                    rvScores.adapter = ScoreAdapter(list) { scoreItem ->
                        showAssessmentDetailsDialog(scoreItem)
                    }

                    autoOpenAssessmentId?.let { idToOpen ->
                        val match = list.firstOrNull { it.id == idToOpen }
                        if (match != null) {
                            showAssessmentDetailsDialog(match)
                            autoOpenAssessmentId = null
                        }
                    }
                }
                is ScoreState.Error -> {
                    ShowToast.showMessage(this, state.message)
                }
            }
        }
    }

    private fun showAssessmentDetailsDialog(scoreItem: StudentScore) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_assessment_details, null)
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create()

        dialogView.findViewById<TextView>(R.id.tvDetailNumber).text = "${scoreItem.assessmentNumber}"
        dialogView.findViewById<TextView>(R.id.tvDetailTitle).text = scoreItem.title

        val df = java.text.DecimalFormat("#.##")
        dialogView.findViewById<TextView>(R.id.tvDetailScore).text = df.format(scoreItem.score)
        dialogView.findViewById<TextView>(R.id.tvDetailItems).text = "${scoreItem.assessmentItems}"

        val percentage = if (scoreItem.assessmentItems > 0) {
            (scoreItem.score.toDouble() / scoreItem.assessmentItems) * 100
        } else 0.0

        dialogView.findViewById<TextView>(R.id.tvDetailPercentage).text = "%.0f%%".format(percentage)

        // Set Date and Time
        dialogView.findViewById<TextView>(R.id.tvDetailDate).text = formatDate(scoreItem.dateAccomplished)
        dialogView.findViewById<TextView>(R.id.tvDetailTime).text = formatTime(scoreItem.dateAccomplished)

        dialogView.findViewById<View>(R.id.btnClose).setOnClickListener {
            dialog.dismiss()
        }

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }

    class ScoreAdapter(
        private val scores: List<StudentScore>,
        private val onClick: (StudentScore) -> Unit
    ) : RecyclerView.Adapter<ScoreAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val title: TextView = view.findViewById(R.id.tvTitle)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_class_assessment_card, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = scores[position]
            holder.title.text = "Assessment ${item.assessmentNumber}"
            holder.itemView.setOnClickListener { onClick(item) }
        }

        override fun getItemCount() = scores.size
    }

    // --- REFINED DATE FORMATTING ---
    private fun formatDate(dateString: String): String {
        val formats = arrayOf(
            "yyyy-MM-dd'T'HH:mm:ss",          // Matches your image sample
            "yyyy-MM-dd'T'HH:mm:ssXXX",       // Supabase Default
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",   // With Millis
            "yyyy-MM-dd"                      // Short Date
        )

        for (format in formats) {
            try {
                val inputFormat = SimpleDateFormat(format, Locale.US)
                // Supabase timestamps are in UTC
                inputFormat.timeZone = TimeZone.getTimeZone("UTC")

                // If the string is long (like your sample), we take the first 19 chars
                // to match the "yyyy-MM-dd'T'HH:mm:ss" pattern perfectly.
                val parseableString = if (dateString.length > 19) dateString.substring(0, 19) else dateString

                val date = inputFormat.parse(parseableString)
                if (date != null) {
                    // Returns "Jan. 12, 2026"
                    val outputFormat = SimpleDateFormat("MMM. dd, yyyy", Locale.US)
                    outputFormat.timeZone = TimeZone.getDefault() // Convert to your local time
                    return outputFormat.format(date)
                }
            } catch (e: Exception) {
                continue
            }
        }
        return dateString
    }

    private fun formatTime(dateString: String): String {
        val formats = arrayOf(
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"
        )

        for (format in formats) {
            try {
                val inputFormat = SimpleDateFormat(format, Locale.US)
                inputFormat.timeZone = TimeZone.getTimeZone("UTC")

                val parseableString = if (dateString.length > 19) dateString.substring(0, 19) else dateString

                val date = inputFormat.parse(parseableString)
                if (date != null) {
                    // Returns "11:00 AM"
                    val outputFormat = SimpleDateFormat("h:mm a", Locale.US)
                    outputFormat.timeZone = TimeZone.getDefault() // Local time conversion
                    return outputFormat.format(date).uppercase()
                }
            } catch (e: Exception) {
                continue
            }
        }
        return dateString
    }
}
package com.example.gr8math // Make sure this matches your package name

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder

// Simple data class for scores
data class StudentScore(val title: String, val score: Int)

class StudentScoresActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_scores)

        // Get student name
        val studentName = intent.getStringExtra("EXTRA_STUDENT_NAME") ?: "Student"

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        val tvStudentName = findViewById<TextView>(R.id.tvStudentName)
        tvStudentName.text = studentName

        val btnQuarterlyReport = findViewById<Button>(R.id.btnQuarterlyReport)
        btnQuarterlyReport.setOnClickListener {
            startActivity(Intent(this, QuarterlyReportActivity::class.java))
        }

        // --- Setup Scores List ---
        val scores = listOf(
            StudentScore("Assessment 1", 10),
            StudentScore("Assessment 2", 8)
        )

        val rvScores = findViewById<RecyclerView>(R.id.rvScores)
        rvScores.layoutManager = LinearLayoutManager(this)
        rvScores.adapter = ScoreAdapter(scores) { scoreItem ->
            showAssessmentDetailsDialog(scoreItem)
        }
    }

    private fun showAssessmentDetailsDialog(scoreItem: StudentScore) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_assessment_details, null)
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create()

        // Populate Dialog Data (Hardcoded based on your request, but you can use scoreItem)
        dialogView.findViewById<TextView>(R.id.tvDetailTitle).text = scoreItem.title
        dialogView.findViewById<TextView>(R.id.tvDetailScore).text = scoreItem.score.toString()
        // ... set other fields ...

        dialogView.findViewById<View>(R.id.btnClose).setOnClickListener {
            dialog.dismiss()
        }

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }

    // --- Internal Adapter Class for Scores ---
    class ScoreAdapter(
        private val scores: List<StudentScore>,
        private val onClick: (StudentScore) -> Unit
    ) : RecyclerView.Adapter<ScoreAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val title: TextView = view.findViewById(R.id.tvTitle)

            // --- FIX IS HERE: Added <View> to findViewById ---
            // We must tell Kotlin what type of view 'ivArrow' is before asking for its parent
            val card: View = view.findViewById<View>(R.id.ivArrow).parent as View
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            // Reusing your existing assessment card layout!
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_class_assessment_card, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = scores[position]
            holder.title.text = item.title
            holder.itemView.setOnClickListener { onClick(item) }
        }

        override fun getItemCount() = scores.size
    }
}
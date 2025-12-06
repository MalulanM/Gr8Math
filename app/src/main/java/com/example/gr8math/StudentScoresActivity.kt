package com.example.gr8math

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gr8math.api.ConnectURL
import com.example.gr8math.dataObject.CurrentCourse
import com.example.gr8math.utils.ShowToast
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

// Data class that matches Laravel response
data class StudentScore(
    val id: Int,
    val assessmentNumber: Int,
    val title: String,
    val score: Int,
    val dateAccomplished: String,
    val assessment_items : Int
)

class StudentScoresActivity : AppCompatActivity() {

    private var studentName = ""
    private var studentId = 0
    private lateinit var rvScores: RecyclerView
    private val assessmentList = ArrayList<StudentScore>()

    private var autoOpenAssessmentId: Int? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_scores)

        // Get passed values
        studentName = intent.getStringExtra("EXTRA_STUDENT_NAME") ?: "Student"
        studentId = intent.getIntExtra("EXTRA_STUDENT_ID", 0)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        val tvStudentName = findViewById<TextView>(R.id.tvStudentName)
        tvStudentName.text = studentName

        val btnQuarterlyReport = findViewById<Button>(R.id.btnQuarterlyReport)
        btnQuarterlyReport.setOnClickListener {
            val intent = Intent(this, QuarterlyReportActivity::class.java)
            intent.putExtra("EXTRA_STUDENT_ID", studentId)
            startActivity(intent)
        }

        // RecyclerView setup
        rvScores = findViewById(R.id.rvScores)
        rvScores.layoutManager = LinearLayoutManager(this)

        displayAllAssessments()

        autoOpenAssessmentId = intent.getIntExtra("AUTO_ASSESSMENT_ID", -1)
        if (autoOpenAssessmentId == -1) autoOpenAssessmentId = null

    }

    private fun showAssessmentDetailsDialog(scoreItem: StudentScore) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_assessment_details, null)
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create()

        dialogView.findViewById<TextView>(R.id.tvDetailNumber)
            .text = "Assessment ${scoreItem.assessmentNumber}"

        dialogView.findViewById<TextView>(R.id.tvDetailTitle)
            .text = scoreItem.title

        dialogView.findViewById<TextView>(R.id.tvDetailScore)
            .text = "${scoreItem.score}"

        dialogView.findViewById<TextView>(R.id.tvDetailItems)
            .text = "${scoreItem.assessment_items}"

        val percentage = (scoreItem.score/scoreItem.assessment_items)*100

        dialogView.findViewById<TextView>(R.id.tvDetailPercentage)
            .text = "${percentage}%"

        dialogView.findViewById<TextView>(R.id.tvDetailDate)
            .text = formatDate(scoreItem.dateAccomplished)

        dialogView.findViewById<TextView>(R.id.tvDetailTime)
            .text = formatTime(scoreItem.dateAccomplished)


        dialogView.findViewById<View>(R.id.btnClose).setOnClickListener {
            dialog.dismiss()
        }

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }

    // RecyclerView Adapter
    class ScoreAdapter(
        private val scores: List<StudentScore>,
        private val onClick: (StudentScore) -> Unit
    ) : RecyclerView.Adapter<ScoreAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val title: TextView = view.findViewById(R.id.tvTitle)
            val card: View = view.findViewById<View>(R.id.ivArrow).parent as View
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

    private fun displayAllAssessments() {

        val call = ConnectURL.api.getStudentAssessment(CurrentCourse.courseId, studentId)

        call.enqueue(object : retrofit2.Callback<ResponseBody> {
            override fun onResponse(
                call: Call<ResponseBody>,
                response: retrofit2.Response<ResponseBody>
            ) {
                val body = response.body()?.string()
                Log.d("API_RESPONSE", body.toString())


                if (body == null) {
                    ShowToast.showMessage(this@StudentScoresActivity, "Empty server response.")
                    return
                }

                try {
                    val json = JSONObject(body)
                    val arr = json.getJSONArray("answered_assessments")

                    assessmentList.clear()

                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)

                        assessmentList.add(
                            StudentScore(
                                id = obj.getInt("id"),
                                assessmentNumber = obj.getInt("assessment_number"),
                                title = obj.getString("title"),
                                score = obj.getInt("score"),
                                dateAccomplished = obj.getString("date_accomplished"),
                                assessment_items = obj.getInt("assessment_items")
                            )
                        )
                    }

                    rvScores.adapter = ScoreAdapter(assessmentList) { scoreItem ->
                        showAssessmentDetailsDialog(scoreItem)
                    }

                    // AUTO OPEN TARGET ASSESSMENT FROM NOTIF
                    autoOpenAssessmentId?.let { idToOpen ->
                        val match = assessmentList.firstOrNull { it.id == idToOpen }
                        if (match != null) {
                            showAssessmentDetailsDialog(match)
                            autoOpenAssessmentId = null
                        }
                    }


                } catch (e: Exception) {

                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {

                ShowToast.showMessage(this@StudentScoresActivity, "Failed to connect to server.")
            }
        })
    }

    private fun formatDate(timestamp: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSS", Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val date = inputFormat.parse(timestamp) ?: return timestamp

            val output = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            output.format(date)
        } catch (e: Exception) {
            timestamp
        }
    }

    private fun formatTime(timestamp: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSS", Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val date = inputFormat.parse(timestamp) ?: return timestamp

            val output = SimpleDateFormat("h:mm a", Locale.getDefault())
            output.format(date)
        } catch (e: Exception) {
            timestamp
        }
    }


}


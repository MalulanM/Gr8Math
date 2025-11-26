package com.example.gr8math

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gr8math.adapter.GradeItem
import com.example.gr8math.adapter.StudentGradesAdapter
import com.example.gr8math.api.ConnectURL
import com.example.gr8math.dataObject.CurrentCourse
import com.example.gr8math.utils.NotificationHelper
import com.example.gr8math.utils.ShowToast
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Locale

class StudentGradesActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView
    private lateinit var rvGrades: RecyclerView
    private val gradeList = ArrayList<GradeItem>()
    private val TAG = "StudentGradesActivity"

    private var studentId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_grades)

        // Toolbar
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        // Bottom Nav
        bottomNav = findViewById(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.nav_grades
        NotificationHelper.fetchUnreadCount(bottomNav)
        setupBottomNav()

        // Quarterly report button
        findViewById<Button>(R.id.btnQuarterlyReport).setOnClickListener {
            val intent = Intent(this, QuarterlyReportActivity::class.java)
            intent.putExtra("EXTRA_STUDENT_ID", studentId)
            startActivity(intent)
        }

        // RecyclerView
        rvGrades = findViewById(R.id.rvGrades)
        rvGrades.layoutManager = LinearLayoutManager(this)

        Log.d("DEBUGdedefrebr", "Sending course_id=${CurrentCourse.courseId}, student_id=${CurrentCourse.userId}")

        // Load assessments
        displayAllAssessments()
    }

    private fun setupBottomNav() {
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {

                R.id.nav_class -> {
                    startActivity(Intent(this, StudentClassPageActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK))
                    finish()
                    true
                }

                R.id.nav_badges -> {
                    startActivity(Intent(this, StudentClassPageActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK))
                    finish()
                    true
                }

                R.id.nav_notifications -> {
                    startActivity(Intent(this, StudentNotificationsActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK))
                    finish()
                    true
                }

                R.id.nav_grades -> {
                    // Prevent crash by NOT reloading instantly
                    true
                }

                else -> false
            }
        }
    }

    // Refresh bottom nav state
    override fun onResume() {
        super.onResume()
        if (::bottomNav.isInitialized) {
            bottomNav.selectedItemId = R.id.nav_grades
        }
    }

    // ------------------------------------------
    // LOAD ALL ASSESSMENTS
    // ------------------------------------------
    private fun displayAllAssessments() {

        val call = ConnectURL.api.getStudentOwnAssessment(
            CurrentCourse.courseId,
            CurrentCourse.userId
        )

        call.enqueue(object : Callback<ResponseBody> {

            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {

                val body = response.body()?.string()
                if (body.isNullOrEmpty()) {
                    ShowToast.showMessage(this@StudentGradesActivity, "Empty server response.")
                    return
                }

                try {
                    val json = JSONObject(body)
                    studentId = json.getInt("studentId")

                    if (!json.has("answered_assessments")) {
                        ShowToast.showMessage(this@StudentGradesActivity, "No assessments found.")
                        return
                    }


                    val arr = json.getJSONArray("answered_assessments")

                    Log.e("JEHDWYECHVWBUCRJE", arr.toString())
                    gradeList.clear()

                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)

                        gradeList.add(
                            GradeItem(
                                title = obj.getString("title"),
                                score = obj.getInt("score"),
                                assessmentNumber = obj.getInt("assessment_number"),
                                id = obj.getInt("id"),
                                dateAccomplished = obj.getString("date_accomplished"),
                                assessment_items = obj.getInt("assessment_items")
                            )
                        )
                    }

                    rvGrades.adapter = StudentGradesAdapter(gradeList) { grade ->
                        showAssessmentDetailsDialog(grade)
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "JSON ERROR: ${e.message}")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e(TAG, "RetrofitError: ${t.localizedMessage}", t)
                ShowToast.showMessage(this@StudentGradesActivity, "Failed to connect to server.")
            }
        })
    }

    // ------------------------------------------
    // SHOW DETAILS POPUP
    // ------------------------------------------
    private fun showAssessmentDetailsDialog(gradeItem: GradeItem) {

        val dialogView = LayoutInflater.from(this)
            .inflate(R.layout.dialog_assessment_details, null)

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create()

        dialogView.findViewById<TextView>(R.id.tvDetailNumber)
            .text = "Assessment ${gradeItem.assessmentNumber}"

        dialogView.findViewById<TextView>(R.id.tvDetailTitle)
            .text = gradeItem.title

        dialogView.findViewById<TextView>(R.id.tvDetailScore)
            .text = "${gradeItem.score}"

        dialogView.findViewById<TextView>(R.id.tvDetailItems)
            .text = "${gradeItem.assessment_items}"

        // SAFE PERCENTAGE
        val percentage =
            if (gradeItem.assessment_items > 0)
                (gradeItem.score.toFloat() / gradeItem.assessment_items.toFloat()) * 100
            else 0f

        dialogView.findViewById<TextView>(R.id.tvDetailPercentage)
            .text = "${percentage.toInt()}%"

        dialogView.findViewById<TextView>(R.id.tvDetailDate)
            .text = formatDate(gradeItem.dateAccomplished)

        dialogView.findViewById<TextView>(R.id.tvDetailTime)
            .text = formatTime(gradeItem.dateAccomplished)

        dialogView.findViewById<View>(R.id.btnClose).setOnClickListener {
            dialog.dismiss()
        }

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }

    private fun formatDate(timestamp: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSS", Locale.getDefault())
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
            val date = inputFormat.parse(timestamp) ?: return timestamp

            val output = SimpleDateFormat("h:mm a", Locale.getDefault())
            output.format(date)
        } catch (e: Exception) {
            timestamp
        }
    }
}

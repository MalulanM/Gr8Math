package com.example.gr8math // Make sure this matches your package name

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.gr8math.api.ConnectURL
import com.example.gr8math.dataObject.CurrentCourse
import com.example.gr8math.utils.NotificationHelper
import com.example.gr8math.utils.ShowToast
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Response

class StudentClassPageActivity : AppCompatActivity() {

    private var id: Int = 0
    private var courseId: Int = 0
    private lateinit var role: String
    private lateinit var sectionName: String
    private lateinit var parentLayout : LinearLayout


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_classpage_student)

        val toastMsg = intent.getStringExtra("toast_msg")
        if(!toastMsg.isNullOrEmpty()){
            ShowToast.showMessage(this, toastMsg)
        }


        val incomingCourseId = intent.getIntExtra("courseId", -1)
        val incomingSectionName = intent.getStringExtra("sectionName")
        val incomingRole = intent.getStringExtra("role")
        val incomingUserId = intent.getIntExtra("id", -1)

        if (incomingCourseId != -1 && incomingCourseId != CurrentCourse.courseId) {

            // Update global course data
            CurrentCourse.courseId = incomingCourseId
            CurrentCourse.sectionName = incomingSectionName ?: ""
            CurrentCourse.currentRole = incomingRole ?: ""
            CurrentCourse.userId = incomingUserId

//            Log.d("TeacherClassPage", "Switched to NEW CLASS: ${CurrentCourse.sectionName}")
        } else {
//            Log.d("TeacherClassPage", "Same class — keeping CurrentCourse data.")
        }


        courseId = CurrentCourse.courseId
        sectionName = CurrentCourse.sectionName
        role = CurrentCourse.currentRole
        id = CurrentCourse.userId
        parentLayout = findViewById(R.id.parentLayout)

        // --- Setup Toolbar ---
        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        toolbar.title = sectionName
        toolbar.setNavigationOnClickListener {
            finish() // Go back
        }

        // --- Setup Bottom Navigation ---
        val bottomNav: BottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.nav_class
        bottomNav.setOnItemSelectedListener { item ->
            if (item.itemId == bottomNav.selectedItemId) {
                return@setOnItemSelectedListener true
            }

            val intent = when (item.itemId) {
                R.id.nav_class -> null // already in this Activity
                R.id.nav_badges -> Intent(this, StudentBadgesActivity::class.java)
                R.id.nav_notifications -> Intent(this, StudentNotificationsActivity::class.java)
                R.id.nav_grades -> Intent(this, StudentGradesActivity::class.java)
                else -> null
            }

            intent?.let {
                // Prevent stacking
                it.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(it)
            }

            true
        }


        NotificationHelper.fetchUnreadCount(bottomNav)
        // --- Customize the Cards ---

        // 1. "Game" card
        val gameCard: View = findViewById(R.id.game_card)
        gameCard.findViewById<ImageView>(R.id.ivIcon).contentDescription = getString(R.string.play_a_game)
        gameCard.findViewById<TextView>(R.id.tvTitle).text = getString(R.string.play_a_game)
        inflateAssessmentAndLesson(courseId)

        handleNotificationIntent(intent)

    }

    private fun handleNotificationIntent(intent: Intent?) {
        intent?.let {
            val lessonIdFromNotif = it.getIntExtra("lessonId", -1)
            val assessmentIdFromNotif = it.getIntExtra("assessmentId", -1)

            if (lessonIdFromNotif != -1) {
                // Open lesson detail immediately
                val lessonIntent = Intent(this, LessonDetailActivity::class.java)
                lessonIntent.putExtra("lesson_id", lessonIdFromNotif)
                startActivity(lessonIntent)
            } else if (assessmentIdFromNotif != -1) {
                // Wait for API check before opening activity to avoid brief flash
                withRecord(assessmentIdFromNotif, object : RecordCallback {
                    override fun onResult(hasRecord: Boolean) {
                        val targetIntent = if (hasRecord) {
                            // Go directly to results if record exists or deadline passed
                            Intent(this@StudentClassPageActivity, AssessmentResultActivity::class.java)
                        } else {
                            // Otherwise, go to detail
                            Intent(this@StudentClassPageActivity, AssessmentDetailActivity::class.java)
                        }
                        targetIntent.putExtra("assessment_id", assessmentIdFromNotif)
                        targetIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        startActivity(targetIntent)
                    }
                })
            }
        }
    }


    // --- OVERRIDE onNewIntent at class level ---
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent) // update activity intent
        handleNotificationIntent(intent)
    }
    interface RecordCallback {
        fun onResult(hasRecord: Boolean)
    }

    fun inflateAssessmentAndLesson(courseId: Int) {

        val apiService = ConnectURL.api
        val call = apiService.getClassContent(courseId)

        call.enqueue(object : retrofit2.Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                val responseString = response.body()?.string() ?: response.errorBody()?.string()
                if (responseString.isNullOrEmpty()) {
                    Log.e("API_ERROR", "Empty response")
                    return
                }

                try {
                    val jsonObj = org.json.JSONObject(responseString)
                    val dataArray = jsonObj.optJSONArray("data") ?: org.json.JSONArray()
                    Log.e("COURSE_CONTENT_JSON", responseString ?: "empty")


                    for (i in 0 until dataArray.length()) {
                        val item = dataArray.getJSONObject(i)
                        val type = item.optString("type", "lesson")

                        val itemView: View = when (type) {
                            "lesson" -> layoutInflater.inflate(R.layout.item_class_lesson_card, parentLayout, false)
                            "assessment" -> layoutInflater.inflate(R.layout.item_class_assessment_card, parentLayout, false)
                            else -> continue
                        }

                        val id = item.optInt("id")
                        val cleanContent = removeBase64(item.optString("lesson_content", ""))

                        when (type) {
                            "lesson" -> {
                                val week = itemView.findViewById<TextView>(R.id.tvWeek)
                                val title = itemView.findViewById<TextView>(R.id.tvTitle)
                                val previewDesc = itemView.findViewById<TextView>(R.id.tvDescription)
                                val seeMore = itemView.findViewById<TextView>(R.id.tvSeeMore)
                                val editLesson = itemView.findViewById<ImageButton>(R.id.ibEditLesson)


                                editLesson.visibility= View.GONE
                                week.text = item.optInt("week_number").toString()
                                title.text = item.optString("lesson_title")
                                previewDesc.text = getFirstSentence(cleanContent)

                                seeMore.setOnClickListener {
                                    val intent = Intent(this@StudentClassPageActivity, LessonDetailActivity::class.java)
                                    intent.putExtra("lesson_id", id)
                                    startActivity(intent)
                                }

                            }
                            "assessment" -> {
                                val assessmentTitle = itemView.findViewById<TextView>(R.id.tvTitle)
                                val arrowView = itemView.findViewById<ImageView>(R.id.ivArrow)
                                assessmentTitle.text = "Assessment ${item.optInt("assessment_number")}"
                                val assessmentId = item.optInt("id")
                                arrowView.setOnClickListener {
                                    // Open AssessmentDetailActivity immediately
                                    val intent = Intent(this@StudentClassPageActivity, AssessmentDetailActivity::class.java)
                                    intent.putExtra("assessment_id", assessmentId)
                                    withRecord(assessmentId, object : RecordCallback {
                                        override fun onResult(hasRecordOrDeadline: Boolean) {

                                            if (hasRecordOrDeadline) {
                                                val resultIntent = Intent(this@StudentClassPageActivity, AssessmentResultActivity::class.java)
                                                resultIntent.putExtra("assessment_id", assessmentId)
                                                startActivity(resultIntent)
                                            } else {
                                                val detailIntent = Intent(this@StudentClassPageActivity, AssessmentDetailActivity::class.java)
                                                detailIntent.putExtra("assessment_id", assessmentId)
                                                startActivity(detailIntent)
                                            }
                                        }
                                    })

                                }

                            }
                        }

                        parentLayout.addView(itemView)
                    }

                } catch (e: Exception) {
                    Log.e("API_ERROR", "Failed to parse response: ${e.localizedMessage}", e)
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e("API_ERROR", "Internet: ${t.localizedMessage}", t)
            }
        })
    }


    private fun withRecord(assessmentId: Int, callback: RecordCallback) {
        val call = ConnectURL.api.hasRecord(id, assessmentId)
        call.enqueue(object : retrofit2.Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                val responseString = response.body()?.string() ?: ""
                try {
                    val jsonObj = JSONObject(responseString)
                    val hasRecord = jsonObj.optBoolean("has_record", false)
                    val deadlinePassed = jsonObj.optBoolean("deadline_passed", false)

                    // If either hasRecord or deadlinePassed is true, redirect to result
                    callback.onResult(hasRecord || deadlinePassed)

                } catch (e: Exception) {
                    callback.onResult(false)
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                callback.onResult(false)
            }
        })
    }


    private fun removeBase64(content: String): String {
        return content.replace(Regex("data:image/[^;]+;base64,[A-Za-z0-9+/=]+"), "")
            .replace(Regex("[A-Za-z0-9+/=]{50,}"), "") // long base64 block
            .trim()
    }

    private fun getFirstSentence(text: String): String {
        // Remove all <ic_read_admin> tags completely
        val noImg = text.replace(Regex("<ic_read_admin[^>]*>"), "").trim()

        // Split into lines
        val lines = noImg.split("\n").map { it.trim() }.filter { it.isNotEmpty() }

        if (lines.isEmpty()) return ""

        // Take ONLY the first line
        val firstLine = lines[0]

        // Extract 1 sentence
        val period = firstLine.indexOf(".")
        return if (period != -1) {
            firstLine.substring(0, period + 1)
        } else {
            firstLine
        }
    }
}
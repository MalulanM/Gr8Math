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
    private lateinit var parentLayout : LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_classpage_student)

        if (CurrentCourse.userId == 0) CurrentCourse.userId = intent.getIntExtra("id", 0)
        if (CurrentCourse.courseId == 0) CurrentCourse.courseId = intent.getIntExtra("courseId", 0)
        if (CurrentCourse.currentRole.isEmpty()) CurrentCourse.currentRole = intent.getStringExtra("role") ?: ""
        if (CurrentCourse.sectionName.isEmpty()) CurrentCourse.sectionName = intent.getStringExtra("sectionName") ?: ""

        id = CurrentCourse.userId
        courseId = CurrentCourse.courseId
        role = CurrentCourse.currentRole
        parentLayout = findViewById(R.id.parentLayout)

        // --- Setup Toolbar ---
        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            finish() // Go back
        }

        // --- Setup Bottom Navigation ---
        val bottomNav: BottomNavigationView = findViewById(R.id.bottom_navigation)
        // Set "Class" as the selected item
        bottomNav.selectedItemId = R.id.nav_class

        // --- Customize the Cards ---

        // 1. "Game" card
        val gameCard: View = findViewById(R.id.game_card)
        gameCard.findViewById<ImageView>(R.id.ivIcon).contentDescription = getString(R.string.play_a_game)
        gameCard.findViewById<TextView>(R.id.tvTitle).text = getString(R.string.play_a_game)
        inflateAssessmentAndLesson(courseId)

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
                                    withRecord(assessmentId, object : RecordCallback {
                                        override fun onResult(hasRecord: Boolean) {
                                            if (hasRecord) {
                                                val intent = Intent(this@StudentClassPageActivity,
                                                    AssessmentResultActivity::class.java)
                                                intent.putExtra("assessment_id", assessmentId)
                                                startActivity(intent)
                                            } else {
                                                val intent = Intent(this@StudentClassPageActivity,
                                                    AssessmentDetailActivity::class.java)
                                                intent.putExtra("assessment_id", assessmentId)
                                                startActivity(intent)
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
                    val hasRecord = jsonObj.optBoolean("success", false)
                    val deadlinePassed = jsonObj.optBoolean("deadline_passed", false)

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
        // Remove all <img> tags completely
        val noImg = text.replace(Regex("<img[^>]*>"), "").trim()

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
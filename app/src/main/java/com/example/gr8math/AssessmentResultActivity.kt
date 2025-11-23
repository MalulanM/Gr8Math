package com.example.gr8math

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.gr8math.api.ConnectURL
import com.example.gr8math.dataObject.CurrentCourse
import com.example.gr8math.utils.ShowToast
import com.google.android.material.appbar.MaterialToolbar
import okhttp3.ResponseBody
import org.json.JSONObject

class AssessmentResultActivity : AppCompatActivity() {

    private var assessmentId = 0
    private lateinit var tvAssessmentNumber: TextView
    private lateinit var tvAssessmentTitle: TextView
    private lateinit var tvScore: TextView
    private lateinit var tvNumberOfItems: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvCompletionMessage : TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_assessment_result)

        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        tvAssessmentNumber = findViewById(R.id.tvAssessmentNumber)
        tvAssessmentTitle = findViewById(R.id.tvAssessmentTitle)
        tvScore = findViewById(R.id.tvScore)
        tvNumberOfItems = findViewById(R.id.tvNumberOfItems)
        tvDate = findViewById(R.id.tvDate)
        tvCompletionMessage = findViewById(R.id.tvCompletionMessage)

        assessmentId = intent.getIntExtra("assessment_id", 0)

        toolbar.setNavigationOnClickListener { finish() }

        displayResult()
    }

    private fun displayResult() {
        val apiService = ConnectURL.api

        val studentId = CurrentCourse.userId

        apiService.displayScore(studentId, assessmentId)
            .enqueue(object : retrofit2.Callback<ResponseBody> {
                override fun onResponse(
                    call: retrofit2.Call<ResponseBody>,
                    response: retrofit2.Response<ResponseBody>
                ) {
                    if (!response.isSuccessful || response.body() == null) {
                        ShowToast.showMessage(this@AssessmentResultActivity, "Failed to load result")
                        return
                    }

                    try {
                        val responseString = response.body()!!.string()
                        val jsonObj = JSONObject(responseString)
                        val success = jsonObj.optBoolean("success", false)

                        if (success) {
                            val recordObj = jsonObj.optJSONObject("record")
                            val assessmentObj = jsonObj.optJSONObject("assessment_details")

                            tvAssessmentNumber.text = "Assessment ${assessmentObj?.optInt("assessment_number", 0).toString()}"
                            tvAssessmentTitle.text = "${assessmentObj?.optString("title", "")}"
                            tvCompletionMessage.text = "Assessment Test completed!"
                            tvNumberOfItems.text = "Number of items: ${assessmentObj?.optInt("assessment_items", 0).toString()}"
                            tvScore.text = "Score: ${recordObj?.optInt("score", 0).toString()}"

                            val rawDate = recordObj?.optString("date_accomplished", "") ?: ""

                                    val dateOnly = rawDate.substring(0, 10)
                                    val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                                    val outputFormat = java.text.SimpleDateFormat("MMM. dd, yyyy", java.util.Locale.US)

                                    val date = inputFormat.parse(dateOnly)
                                    val formattedDate = outputFormat.format(date)

                                    tvDate.text = "Date Accomplished: $formattedDate"

                        } else {
                            val message = jsonObj.optString("message", "Failed to fetch result")
                            ShowToast.showMessage(this@AssessmentResultActivity, message)
                        }
                    } catch (e: Exception) {
                        Log.e("API_PARSE_ERROR", e.localizedMessage ?: "")
                        ShowToast.showMessage(this@AssessmentResultActivity, "Error parsing result")
                    }
                }

                override fun onFailure(call: retrofit2.Call<ResponseBody>, t: Throwable) {
                    Log.e("API_ERROR", t.localizedMessage ?: "")
                    ShowToast.showMessage(this@AssessmentResultActivity, "Network error: ${t.localizedMessage}")
                }
            })
    }
}

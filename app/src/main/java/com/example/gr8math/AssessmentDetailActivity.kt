package com.example.gr8math // Make sure this matches your package name

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.gr8math.api.ConnectURL
import com.example.gr8math.dataObject.AssessmentResponse
import com.example.gr8math.utils.ShowToast
import com.google.android.material.appbar.MaterialToolbar
import com.google.gson.Gson
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Locale

class AssessmentDetailActivity : AppCompatActivity() {

    private var assessmentId = 0
    private lateinit var tvAssessmentNumber: TextView
    private lateinit var tvAssessmentTitle: TextView
    private lateinit var tvNumberOfItems: TextView
    private lateinit var tvStartsAt: TextView
    private lateinit var tvEndsAt: TextView
    private lateinit var btnStartAssessment: TextView
    private lateinit var toolbar : MaterialToolbar
    private lateinit var fullAssessmentJson: String



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_assessment_detail)

        assessmentId = intent.getIntExtra("assessment_id", 0)

        // Find Views
        toolbar= findViewById(R.id.toolbar)
        tvAssessmentNumber = findViewById(R.id.tvAssessmentNumber)
        tvAssessmentTitle = findViewById(R.id.tvAssessmentTitle)
        tvNumberOfItems = findViewById(R.id.tvNumberOfItems)
        tvStartsAt = findViewById(R.id.tvStartsAt)
        tvEndsAt = findViewById(R.id.tvEndsAt)
        btnStartAssessment = findViewById(R.id.btnStartAssessment)

        displayQuiz(assessmentId)

        // Toolbar Back Button
        toolbar.setNavigationOnClickListener {
            finish()
        }

        btnStartAssessment.setOnClickListener {
            val intent = Intent(this, TakeAssessmentActivity::class.java)
            intent.putExtra("assessment_data", fullAssessmentJson)
            startActivity(intent)
        }

    }

    fun displayQuiz(assessmentId: Int){
        val apiService = ConnectURL.api
        val call = apiService.displayAssessment(assessmentId)

        call.enqueue(object : Callback<AssessmentResponse> {
            override fun onResponse(
                call: Call<AssessmentResponse>,
                response: Response<AssessmentResponse>
            ) {
                val body = response.body()
                if (body == null || !body.success) return
                Log.e("sswjhd", body.toString())
                val a = body.assessment

                val formattedStart = formatTimestamp(a.start_time)
                val formattedEnd = formatTimestamp(a.end_time)

                tvAssessmentNumber.text = "Assessment ${a.assessment_number}"
                tvAssessmentTitle.text = a.title
                tvNumberOfItems.text = "Number of Items: ${a.assessment_items}"
                tvStartsAt.text = "Starts: $formattedStart"
                tvEndsAt.text = "Ends: $formattedEnd"

                fullAssessmentJson = Gson().toJson(a)
            }

            override fun onFailure(call: Call<AssessmentResponse>, t: Throwable) {
                Log.e("wdje", t.message.toString())
                ShowToast.showMessage(this@AssessmentDetailActivity, "Server error")
            }
        })
    }


    fun formatTimestamp(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
            val date = inputFormat.parse(dateString)

            val outputFormat = SimpleDateFormat("M/d/yy - h:mm a", Locale.ENGLISH)
            outputFormat.format(date!!)
        } catch (e: Exception) {
            dateString
        }
    }


}
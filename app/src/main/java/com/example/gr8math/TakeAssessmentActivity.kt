package com.example.gr8math

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.gr8math.dataObject.AssessmentData
import com.example.gr8math.dataObject.QuestionData
import com.example.gr8math.api.ConnectURL
import com.example.gr8math.dataObject.AnswerItem
import com.example.gr8math.dataObject.AnswerPayload
import com.example.gr8math.dataObject.CurrentCourse
import com.example.gr8math.utils.ShowToast
import com.example.gr8math.utils.UIUtils
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class TakeAssessmentActivity : AppCompatActivity() {

    private lateinit var tvTimer: TextView
    private lateinit var tvQuestion: TextView
    private lateinit var rgChoices: RadioGroup
    private lateinit var btnPrevious: Button
    private lateinit var btnNext: Button

    private var countDownTimer: CountDownTimer? = null

    private lateinit var questions: List<QuestionData>
    private var currentQuestionIndex = 0

    private var assessmentId: Int = 0

    private val selectedAnswers = mutableMapOf<Int, Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_take_assessment)

        val assessmentJson = intent.getStringExtra("assessment_data")
        val assessment = Gson().fromJson(assessmentJson, AssessmentData::class.java)

        questions = assessment.questions

        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        tvTimer = findViewById(R.id.tvTimer)
        tvQuestion = findViewById(R.id.tvQuestion)
        rgChoices = findViewById(R.id.rgChoices)
        btnPrevious = findViewById(R.id.btnPrevious)
        btnNext = findViewById(R.id.btnNext)

        assessmentId = assessment.id

        toolbar.setNavigationOnClickListener {
            finish()
        }

        loadQuestion(currentQuestionIndex)
        startTimer()

        btnNext.setOnClickListener { onNextClicked() }
        btnPrevious.setOnClickListener { onPreviousClicked() }
    }

    private fun startTimer() {
        val testDurationMillis: Long = 60 * 60 * 1000

        countDownTimer = object : CountDownTimer(testDurationMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val hours = TimeUnit.MILLISECONDS.toHours(millisUntilFinished)
                val minutes = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished) % 60
                val seconds = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) % 60
                tvTimer.text = String.format("%02d:%02d:%02d", hours, minutes, seconds)
            }

            override fun onFinish() {
                tvTimer.text = "00:00:00"
                finishAssessment()
            }
        }.start()
    }

    private fun loadQuestion(index: Int) {
        val q = questions[index]

        tvQuestion.text = q.question_text
        rgChoices.removeAllViews()

        q.choices.forEach { choice ->
            val rb = RadioButton(this)
            rb.text = choice.choice_text
            rb.tag = choice.id
            rb.setPadding(8, 24, 8, 24)
            rb.textSize = 16f
            rb.layoutParams = RadioGroup.LayoutParams(
                RadioGroup.LayoutParams.MATCH_PARENT,
                RadioGroup.LayoutParams.WRAP_CONTENT
            )


            if (selectedAnswers[q.id] == choice.id) {
                rb.isChecked = true
            }


            rb.setOnClickListener {
                selectedAnswers[q.id] = choice.id
            }

            rgChoices.addView(rb)
        }

        updateNavigationButtons()
    }

    private fun updateNavigationButtons() {
        btnPrevious.visibility = if (currentQuestionIndex > 0) View.VISIBLE else View.GONE

        btnNext.text =
            if (currentQuestionIndex == questions.size - 1) "Submit"
            else "Next"
    }

    private fun onNextClicked() {
        if (currentQuestionIndex < questions.size - 1) {
            currentQuestionIndex++
            loadQuestion(currentQuestionIndex)
        } else {
            showReviewDialog()
        }
    }

    private fun onPreviousClicked() {
        if (currentQuestionIndex > 0) {
            currentQuestionIndex--
            loadQuestion(currentQuestionIndex)
        }
    }

    private fun showReviewDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_review_title)
            .setMessage(R.string.dialog_review_message)
            .setNegativeButton(R.string.yes) { _, _ ->
                currentQuestionIndex = 0
                loadQuestion(currentQuestionIndex)
            }
            .setPositiveButton(R.string.no) { _, _ ->
                finishAssessment()
            }
            .show()
    }

    private fun finishAssessment() {
        countDownTimer?.cancel()

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val currentTimestamp = sdf.format(Date())

        val answersList = questions.map { q ->
            AnswerItem(
                question_id = q.id,
                choice_id = selectedAnswers[q.id] ?: 0,
                timestamp = currentTimestamp
            )
        }

        val payload = AnswerPayload(
            student_id = CurrentCourse.userId,
            assessment_id = assessmentId,
            answers = answersList
        )

        ConnectURL.api.answerAssessment(payload).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                try {
                    if (!response.isSuccessful || response.body() == null) {
                        ShowToast.showMessage(this@TakeAssessmentActivity, "Error submitting")
                        return
                    }

                    val res = response.body()!!
                    val assessmentId = (res["assessment_id"] as Double).toInt()

                    val intent = Intent(this@TakeAssessmentActivity, AssessmentResultActivity::class.java)
                    intent.putExtra("assessment_id", assessmentId)
                    startActivity(intent)
                    finish()
                } catch (e: Exception) {
                    Log.e("TakeAssessmentActivity", "Error parsing response", e)
                    ShowToast.showMessage(this@TakeAssessmentActivity, "Unexpected error occurred")
                }
            }

            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                ShowToast.showMessage(this@TakeAssessmentActivity, "Network error")
            }
        })
    }


    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}

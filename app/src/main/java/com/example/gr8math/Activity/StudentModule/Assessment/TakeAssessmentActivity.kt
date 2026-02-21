package com.example.gr8math.Activity

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.gr8math.Activity.StudentModule.Assessment.AssessmentResultActivity
import com.example.gr8math.Model.CurrentCourse
import com.example.gr8math.R
import com.example.gr8math.Utils.ShowToast
import com.example.gr8math.Utils.UIUtils
import com.example.gr8math.ViewModel.TakeAssessmentState
import com.example.gr8math.ViewModel.TakeAssessmentViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.concurrent.TimeUnit

class TakeAssessmentActivity : AppCompatActivity() {

    private val viewModel: TakeAssessmentViewModel by viewModels()

    // Views
    private lateinit var tvTimer: TextView
    private lateinit var tvQuestion: TextView
    private lateinit var rgChoices: RadioGroup
    private lateinit var btnPrevious: Button
    private lateinit var btnNext: Button
    private lateinit var loadingLayout: View
    private lateinit var loadingProgress: View
    private lateinit var loadingText: TextView
    private lateinit var toolbar: MaterialToolbar

    private var countDownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_take_assessment)

        // 1. Init Views
        initViews()

        // 2. Parse Data
        val json = intent.getStringExtra("assessment_data")
        if (json != null) {
            viewModel.parseAssessmentData(json)
        } else {
            ShowToast.showMessage(this, "Error loading assessment")
            finish()
            return
        }

        // 3. Setup Observers
        setupObservers()

        // 4. Start Timer (1 Hour hardcoded as per original, or calculate from end_time)
        startTimer(60 * 60 * 1000)
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        tvTimer = findViewById(R.id.tvTimer)
        tvQuestion = findViewById(R.id.tvQuestion)
        rgChoices = findViewById(R.id.rgChoices)
        btnPrevious = findViewById(R.id.btnPrevious)
        btnNext = findViewById(R.id.btnNext)
        loadingLayout = findViewById(R.id.loadingLayout)
        loadingProgress = findViewById(R.id.loadingProgressBg)
        loadingText = findViewById(R.id.loadingText)

        toolbar.setNavigationOnClickListener { finish() }

        btnNext.setOnClickListener {
            val total = viewModel.assessment?.questions?.size ?: 0
            val current = viewModel.currentIndex.value ?: 0

            if (current < total - 1) {
                viewModel.nextQuestion()
            } else {
                showReviewDialog()
            }
        }

        btnPrevious.setOnClickListener {
            viewModel.prevQuestion()
        }
    }

    private fun setupObservers() {
        // Observe Question Index Change
        viewModel.currentIndex.observe(this) { index ->
            val data = viewModel.assessment ?: return@observe
            if (index < data.questions.size) {
                loadQuestionUI(index)
            }
        }

        // Observe Submission State
        viewModel.state.observe(this) { state ->
            when (state) {
                is TakeAssessmentState.Loading -> {
                    UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, true)
                }
                is TakeAssessmentState.Submitted -> {
                    UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                    countDownTimer?.cancel()

                    // Go to Results
                    val intent = Intent(this, AssessmentResultActivity::class.java)
                    intent.putExtra("assessment_id", viewModel.assessment?.id)
                    intent.putExtra("student_id", CurrentCourse.userId)
                    intent.putExtra("is_newly_completed", true)
                    startActivity(intent)
                    finish()
                }
                is TakeAssessmentState.Error -> {
                    UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                    ShowToast.showMessage(this, state.message)
                }
                else -> {}
            }
        }
    }

    private fun loadQuestionUI(index: Int) {
        val question = viewModel.assessment!!.questions[index]

        tvQuestion.text = "${index + 1}. ${question.questionText}"
        rgChoices.removeAllViews()

        val selectedId = viewModel.getSelectedAnswer(question.id)

        question.choices.forEach { choice ->
            val rb = RadioButton(this)
            rb.text = choice.choiceText
            rb.tag = choice.id
            rb.setPadding(8, 24, 8, 24)
            rb.textSize = 16f
            rb.layoutParams = RadioGroup.LayoutParams(
                RadioGroup.LayoutParams.MATCH_PARENT,
                RadioGroup.LayoutParams.WRAP_CONTENT
            )

            if (selectedId == choice.id) {
                rb.isChecked = true
            }

            rb.setOnClickListener {
                viewModel.selectAnswer(question.id, choice.id)
            }
            rgChoices.addView(rb)
        }

        // Update Buttons
        val total = viewModel.assessment!!.questions.size
        btnPrevious.visibility = if (index > 0) View.VISIBLE else View.GONE
        btnNext.text = if (index == total - 1) "Submit" else "Next"
    }

    private fun showReviewDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_review_title)
            .setMessage(R.string.dialog_review_message)
            .setNegativeButton("Review") { _, _ ->
                viewModel.jumpToFirst()
            }
            .setPositiveButton("Submit") { _, _ ->
                viewModel.submitAssessment()
            }
            .show()
    }

    private fun startTimer(durationMillis: Long) {
        countDownTimer = object : CountDownTimer(durationMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val hours = TimeUnit.MILLISECONDS.toHours(millisUntilFinished)
                val minutes = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished) % 60
                val seconds = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) % 60
                tvTimer.text = String.format("%02d:%02d:%02d", hours, minutes, seconds)
            }

            override fun onFinish() {
                tvTimer.text = "00:00:00"
                ShowToast.showMessage(this@TakeAssessmentActivity, "Time is up! Submitting...")
                viewModel.submitAssessment()
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}
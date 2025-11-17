package com.example.gr8math // Make sure this matches your package name

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.concurrent.TimeUnit

// Simple data class for a question
data class QuizQuestion(
    val questionText: String,
    val choices: List<String>
)

class TakeAssessmentActivity : AppCompatActivity() {

    private lateinit var tvTimer: TextView
    private lateinit var tvQuestion: TextView
    private lateinit var rgChoices: RadioGroup
    private lateinit var btnPrevious: Button
    private lateinit var btnNext: Button

    private var countDownTimer: CountDownTimer? = null

    // --- TODO: Replace with real data from Intent ---
    private val questions = listOf(
        QuizQuestion("Lorem ipsum dolor sit amet.",
            listOf("Choice A", "Choice B", "Choice C", "Choice D")),
        QuizQuestion("This is the second question.",
            listOf("Option 1", "Option 2", "Option 3")),
        QuizQuestion("This is the final question.",
            listOf("Answer X", "Answer Y", "Answer Z", "Answer W", "Answer V"))
    )

    private var currentQuestionIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_take_assessment)

        // Find Views
        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        tvTimer = findViewById(R.id.tvTimer)
        tvQuestion = findViewById(R.id.tvQuestion)
        rgChoices = findViewById(R.id.rgChoices)
        btnPrevious = findViewById(R.id.btnPrevious)
        btnNext = findViewById(R.id.btnNext)

        // Toolbar Back Button
        toolbar.setNavigationOnClickListener {
            // TODO: Add a "Are you sure you want to quit?" dialog
            finish()
        }

        // Load the first question
        loadQuestion(currentQuestionIndex)
        startTimer()

        // Button Click Listeners
        btnNext.setOnClickListener {
            onNextClicked()
        }
        btnPrevious.setOnClickListener {
            onPreviousClicked()
        }
    }

    private fun startTimer() {
        // --- TODO: Set a real duration ---
        val testDurationMillis: Long = 60 * 60 * 1000 // 60 minutes

        countDownTimer = object : CountDownTimer(testDurationMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                // Format as HH:mm:ss
                val minutes = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished) % 60
                val seconds = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) % 60
                val hours = TimeUnit.MILLISECONDS.toHours(millisUntilFinished)

                tvTimer.text = String.format("%02d:%02d:%02d", hours, minutes, seconds)
            }

            override fun onFinish() {
                tvTimer.text = "00:00:00"
                // TODO: Auto-submit the test
                Toast.makeText(this@TakeAssessmentActivity, "Time's up!", Toast.LENGTH_SHORT).show()
                finishAssessment()
            }
        }.start()
    }

    private fun loadQuestion(index: Int) {
        val question = questions[index]

        // Set question text
        tvQuestion.text = question.questionText

        // Clear old choices
        rgChoices.removeAllViews()

        // Add new choices
        question.choices.forEach { choiceText ->
            val radioButton = RadioButton(this)
            radioButton.text = choiceText
            radioButton.setPadding(8, 24, 8, 24)
            radioButton.textSize = 16f
            radioButton.layoutParams = RadioGroup.LayoutParams(
                RadioGroup.LayoutParams.MATCH_PARENT,
                RadioGroup.LayoutParams.WRAP_CONTENT
            )
            rgChoices.addView(radioButton)
        }

        // --- TODO: Restore saved answer for this question ---
        // rgChoices.check(savedAnswerId)

        // Update button visibility
        updateNavigationButtons()
    }

    private fun updateNavigationButtons() {
        // Show "Previous" button if not on the first question
        btnPrevious.visibility = if (currentQuestionIndex > 0) View.VISIBLE else View.GONE

        // Change "Next" button text to "Submit" on the last question
        if (currentQuestionIndex == questions.size - 1) {
            btnNext.text = "Submit" // Or "Finish"
        } else {
            btnNext.text = "Next"
        }
    }

    private fun onNextClicked() {
        // --- TODO: Save the current answer ---
        // val selectedId = rgChoices.checkedRadioButtonId

        if (currentQuestionIndex < questions.size - 1) {
            // Go to next question
            currentQuestionIndex++
            loadQuestion(currentQuestionIndex)
        } else {
            // This is the last question, show the review dialog
            showReviewDialog()
        }
    }

    private fun onPreviousClicked() {
        if (currentQuestionIndex > 0) {
            // --- TODO: Save the current answer ---

            // Go to previous question
            currentQuestionIndex--
            loadQuestion(currentQuestionIndex)
        }
    }

    private fun showReviewDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_review_title)
            .setMessage(R.string.dialog_review_message)
            // "Yes" is the Negative button to be on the left
            .setNegativeButton(R.string.yes) { _, _ ->
                // TODO: User wants to review. Implement review logic.
                Toast.makeText(this, "Review logic not implemented.", Toast.LENGTH_SHORT).show()
            }
            // "No" is the Positive button to be on the right
            .setPositiveButton(R.string.no) { _, _ ->
                // User does not want to review. Finish the assessment.
                finishAssessment()
            }
            .show()
    }

    private fun finishAssessment() {
        countDownTimer?.cancel() // Stop the timer
        // --- TODO: Calculate score ---
        val score = 10 // Placeholder
        val items = questions.size

        val intent = Intent(this, AssessmentResultActivity::class.java).apply {
            // Pass results to the next screen
            putExtra("EXTRA_SCORE", score)
            putExtra("EXTRA_ITEMS", items)
            putExtra("EXTRA_TITLE", "Polynomial") // TODO: Pass real title
            putExtra("EXTRA_NUMBER", "Assessment 2") // TODO: Pass real number
        }
        startActivity(intent)
        finish() // Finish this activity
    }

    override fun onDestroy() {
        super.onDestroy()
        // Make sure to cancel the timer to prevent memory leaks
        countDownTimer?.cancel()
    }
}
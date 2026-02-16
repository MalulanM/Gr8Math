package com.example.gr8math.Activity.StudentModule.Assessment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.gr8math.Activity.StudentModule.ClassManager.StudentClassPageActivity
import com.example.gr8math.R
import com.example.gr8math.Utils.ShowToast
import com.example.gr8math.Utils.UIUtils
import com.example.gr8math.ViewModel.AssessmentResultViewModel
import com.example.gr8math.ViewModel.ResultState
import com.google.android.material.appbar.MaterialToolbar
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class AssessmentResultActivity : AppCompatActivity() {

    private val viewModel: AssessmentResultViewModel by viewModels()

    private lateinit var tvAssessmentNumber: TextView
    private lateinit var tvAssessmentTitle: TextView
    private lateinit var tvScore: TextView
    private lateinit var tvNumberOfItems: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvCompletionMessage: TextView

    lateinit var loadingLayout: View
    lateinit var loadingProgress: View
    lateinit var loadingText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_assessment_result)

        initViews()
        setupObservers()

        val assessmentId = intent.getIntExtra("assessment_id", 0)
        Log.e("ASSESwderf", assessmentId.toString())
        if (assessmentId != 0) {
            viewModel.loadResult(assessmentId)
        } else {
            ShowToast.showMessage(this, "Invalid Assessment ID")
            navigateToClassPage()
        }
    }

    private fun initViews() {
        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            navigateToClassPage()
        }

        tvAssessmentNumber = findViewById(R.id.tvAssessmentNumber)
        tvAssessmentTitle = findViewById(R.id.tvAssessmentTitle)
        tvScore = findViewById(R.id.tvScore)
        tvNumberOfItems = findViewById(R.id.tvNumberOfItems)
        tvDate = findViewById(R.id.tvDate)
        tvCompletionMessage = findViewById(R.id.tvCompletionMessage)

        loadingLayout = findViewById(R.id.loadingLayout)
        loadingProgress = findViewById(R.id.loadingProgressBg)
        loadingText = findViewById(R.id.loadingText)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        navigateToClassPage()
        super.onBackPressed()
    }

    private fun setupObservers() {
        viewModel.state.observe(this) { state ->
            when (state) {
                is ResultState.Loading -> {
                    UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, true)
                }
                is ResultState.Success -> {
                    UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                    val data = state.data

                    tvAssessmentNumber.text = "Assessment ${data.assessmentNumber}"
                    tvAssessmentTitle.text = data.title
                    tvCompletionMessage.text = "Assessment Test completed!"
                    tvNumberOfItems.text = "Number of items: ${data.assessmentItems}"

                    val df = DecimalFormat("#.##")
                    tvScore.text = "Score: ${df.format(data.score)}"

                    // FIX: Use the robust date formatter
                    tvDate.text = "Date Accomplished: ${formatDate(data.dateAccomplished)}"
                }
                is ResultState.Error -> {
                    UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                    ShowToast.showMessage(this, state.message)
                }
            }
        }
    }

    // FIX: Robust Date Parsing for Result Screen
    private fun formatDate(dateString: String): String {
        val formats = arrayOf(
            "yyyy-MM-dd'T'HH:mm:ssXXX",      // Supabase Default
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",  // With Millis
            "yyyy-MM-dd"                     // Short Date
        )

        for (format in formats) {
            try {
                val inputFormat = SimpleDateFormat(format, Locale.US)
                if (!format.contains("XXX")) {
                    inputFormat.timeZone = TimeZone.getTimeZone("UTC")
                }

                val date = inputFormat.parse(dateString)
                if (date != null) {
                    val outputFormat = SimpleDateFormat("MMM. dd, yyyy", Locale.US)
                    return outputFormat.format(date)
                }
            } catch (e: Exception) {
                continue
            }
        }
        return dateString
    }

    private fun navigateToClassPage() {
        val intent = Intent(this, StudentClassPageActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish()
    }
}
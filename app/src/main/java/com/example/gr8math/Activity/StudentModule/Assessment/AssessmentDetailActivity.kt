package com.example.gr8math.Activity.StudentModule.Assessment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.gr8math.Activity.StudentModule.ClassManager.StudentClassPageActivity
import com.example.gr8math.Activity.TakeAssessmentActivity
import com.example.gr8math.R
import com.example.gr8math.Utils.ShowToast
import com.example.gr8math.ViewModel.AssessmentDetailState
import com.example.gr8math.ViewModel.AssessmentDetailViewModel
import com.google.android.material.appbar.MaterialToolbar
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class AssessmentDetailActivity : AppCompatActivity() {

    private val viewModel: AssessmentDetailViewModel by viewModels()
    private var assessmentId = 0
    private var fullAssessmentJson: String = ""

    private lateinit var tvAssessmentNumber: TextView
    private lateinit var tvAssessmentTitle: TextView
    private lateinit var tvNumberOfItems: TextView
    private lateinit var tvStartsAt: TextView
    private lateinit var tvEndsAt: TextView
    private lateinit var btnStartAssessment: TextView
    private lateinit var toolbar: MaterialToolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_assessment_detail)

        assessmentId = intent.getIntExtra("assessment_id", 0)
        Log.e("ASSES", assessmentId.toString())

        initViews()
        setupObservers()

        if (assessmentId != 0) {
            viewModel.loadAssessment(assessmentId)
        } else {
            ShowToast.showMessage(this, "Invalid Assessment ID")
            navigateBack()
        }
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        tvAssessmentNumber = findViewById(R.id.tvAssessmentNumber)
        tvAssessmentTitle = findViewById(R.id.tvAssessmentTitle)
        tvNumberOfItems = findViewById(R.id.tvNumberOfItems)
        tvStartsAt = findViewById(R.id.tvStartsAt)
        tvEndsAt = findViewById(R.id.tvEndsAt)
        btnStartAssessment = findViewById(R.id.btnStartAssessment)

        toolbar.setNavigationOnClickListener { navigateBack() }

        btnStartAssessment.setOnClickListener {
            if (fullAssessmentJson.isNotEmpty()) {
                val intent = Intent(this, TakeAssessmentActivity::class.java)
                intent.putExtra("assessment_data", fullAssessmentJson)
                startActivity(intent)
            } else {
                ShowToast.showMessage(this, "Loading data...")
            }
        }
    }

    private fun setupObservers() {
        viewModel.state.observe(this) { state ->
            when (state) {
                is AssessmentDetailState.Loading -> {
                    btnStartAssessment.isEnabled = false
                    btnStartAssessment.text = "Loading..."
                }
                is AssessmentDetailState.Success -> {
                    btnStartAssessment.isEnabled = true
                    btnStartAssessment.text = "Start Assessment"

                    val a = state.assessment
                    fullAssessmentJson = state.jsonString

                    tvAssessmentNumber.text = "Assessment ${a.assessmentNumber}"
                    tvAssessmentTitle.text = a.title
                    tvNumberOfItems.text = "Number of Items: ${a.assessmentItems}"

                    // SAFE FORMATTING
                    tvStartsAt.text = "Starts: ${formatTimestamp(a.startTime)}"
                    tvEndsAt.text = "Ends: ${formatTimestamp(a.endTime)}"
                }
                is AssessmentDetailState.Error -> {
                    // Prevent Blank Screen by showing error
                    ShowToast.showMessage(this, "Error: ${state.message}")
                    tvAssessmentTitle.text = "Error Loading Data"
                    btnStartAssessment.text = "Retry"
                    btnStartAssessment.setOnClickListener {
                        viewModel.loadAssessment(assessmentId)
                    }
                }
            }
        }
    }

    private fun formatTimestamp(dateString: String): String {
        // Must handle "+00:00" from Supabase using 'XXX'
        val formats = arrayOf("yyyy-MM-dd'T'HH:mm:ssXXX", "yyyy-MM-dd HH:mm:ss")
        for (format in formats) {
            try {
                val sdf = java.text.SimpleDateFormat(format, java.util.Locale.getDefault())
                val date = sdf.parse(dateString)
                if (date != null) return java.text.SimpleDateFormat("M/d/yy - h:mm a", java.util.Locale.getDefault()).format(date)
            } catch (e: Exception) { continue }
        }
        return dateString
    }
    private fun navigateBack() {
        val intent = Intent(this, StudentClassPageActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish()
    }
}
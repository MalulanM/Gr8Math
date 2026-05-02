package com.example.gr8math.Activity.StudentModule.Assessment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.gr8math.Activity.StudentModule.ClassManager.StudentClassPageActivity
import com.example.gr8math.Activity.TakeAssessmentActivity
import com.example.gr8math.R
import com.example.gr8math.Utils.NetworkUtils
import com.example.gr8math.Utils.ShowToast
import com.example.gr8math.ViewModel.AssessmentDetailState
import com.example.gr8math.ViewModel.AssessmentDetailViewModel
import com.google.android.material.appbar.MaterialToolbar
import java.text.SimpleDateFormat
import java.util.Date
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


        val btnRefresh = findViewById<Button>(R.id.btnRefresh)
        if (btnRefresh != null) {
            btnRefresh.setOnClickListener {
                loadData()
            }
        }

        // Let the gatekeeper handle parsing the data and starting the timer!
        loadData()
    }

    private fun loadData() {
        val noInternetView = findViewById<View>(R.id.no_internet_view)
        val mainContent = findViewById<View>(R.id.mainContent)
        val btnStartAssessment = findViewById<View>(R.id.btnStartAssessment)

        // 1. Check for Internet
        if (!NetworkUtils.isConnected(this)) {
            // Show No Internet Screen, hide the content
            noInternetView?.visibility = View.VISIBLE
            // Use INVISIBLE instead of GONE so it doesn't break the layout constraints (vertical bias)
            mainContent?.visibility = View.INVISIBLE
            btnStartAssessment?.visibility = View.GONE
            return
        }

        // 2. HAS INTERNET: Hide error screen, show content
        noInternetView?.visibility = View.GONE
        mainContent?.visibility = View.VISIBLE
        btnStartAssessment?.visibility = View.VISIBLE

        // 3. Fetch your actual data
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
                    btnStartAssessment.alpha = 0.5f
                }
                is AssessmentDetailState.Success -> {
                    val a = state.assessment
                    fullAssessmentJson = state.jsonString

                    tvAssessmentNumber.text = "Assessment ${a.assessmentNumber}"
                    tvAssessmentTitle.text = a.title
                    tvNumberOfItems.text = "Number of Items: ${a.assessmentItems}"

                    // SAFE FORMATTING
                    tvStartsAt.text = "Starts: ${formatTimestamp(a.startTime)}"
                    tvEndsAt.text = "Ends: ${formatTimestamp(a.endTime)}"

                    // --- NEW LOGIC: CHECK IF ASSESSMENT IS EARLY ---
                    if (isEarly(a.startTime)) {
                        btnStartAssessment.isEnabled = false
                        btnStartAssessment.text = "Not Yet Started"
                        btnStartAssessment.alpha = 0.5f // Make it look disabled visually
                    } else {
                        btnStartAssessment.isEnabled = true
                        btnStartAssessment.text = "Start Assessment"
                        btnStartAssessment.alpha = 1.0f
                    }
                }
                is AssessmentDetailState.Error -> {
                    // Prevent Blank Screen by showing error
                    ShowToast.showMessage(this, "Error: ${state.message}")
                    tvAssessmentTitle.text = "Error Loading Data"
                    btnStartAssessment.text = "Retry"
                    btnStartAssessment.alpha = 1.0f
                    btnStartAssessment.isEnabled = true
                    btnStartAssessment.setOnClickListener {
                        viewModel.loadAssessment(assessmentId)
                    }
                }
            }
        }
    }

    // --- NEW HELPER METHOD ---
    private fun isEarly(dateString: String?): Boolean {
        if (dateString.isNullOrEmpty()) return false

        // Match the same formats used by Supabase
        val formats = arrayOf("yyyy-MM-dd'T'HH:mm:ssXXX", "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss")
        for (format in formats) {
            try {
                val sdf = SimpleDateFormat(format, Locale.getDefault())
                val startDate = sdf.parse(dateString)
                if (startDate != null) {
                    val now = Date()
                    // Returns true if current time is BEFORE the start time
                    return now.before(startDate)
                }
            } catch (e: Exception) { continue }
        }
        return false
    }

    private fun formatTimestamp(dateString: String?): String {
        if (dateString == null) return ""
        // Must handle "+00:00" from Supabase using 'XXX'
        val formats = arrayOf("yyyy-MM-dd'T'HH:mm:ssXXX", "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss")
        for (format in formats) {
            try {
                val sdf = SimpleDateFormat(format, Locale.getDefault())
                val date = sdf.parse(dateString)
                if (date != null) return SimpleDateFormat("M/d/yy - h:mm a", Locale.getDefault()).format(date)
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
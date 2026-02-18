package com.example.gr8math.Activity.StudentModule

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.gr8math.Activity.StudentModule.ClassManager.StudentClassPageActivity
import com.example.gr8math.R
import com.example.gr8math.Utils.ShowToast
import com.example.gr8math.Utils.UIUtils
import com.example.gr8math.ViewModel.JoinClassState
import com.example.gr8math.ViewModel.JoinClassViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class StudentAddClassActivity : AppCompatActivity() {

    private val viewModel: JoinClassViewModel by viewModels()

    private lateinit var etClassCode: TextInputEditText
    private lateinit var tilClassCode: TextInputLayout
    private lateinit var joinButton: Button

    private var userId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_add_class)

        // Get User ID from Intent (or CurrentCourse singleton if available)
        userId = intent.getIntExtra("id", 0)

        initViews()
        setupObservers()
    }

    private fun initViews() {
        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        joinButton = findViewById(R.id.btnJoinClass)
        etClassCode = findViewById(R.id.etClassCode)
        tilClassCode = findViewById(R.id.tilClassCode)

        toolbar.setNavigationOnClickListener {
            setResult(RESULT_CANCELED) // Or OK if you prefer
            finish()
        }

        joinButton.setOnClickListener {
            val code = etClassCode.text.toString().trim()
            viewModel.joinClass(userId, code)
        }
    }

    private fun setupObservers() {
        viewModel.state.observe(this) { state ->
            when (state) {
                is JoinClassState.Idle -> {
                    joinButton.isEnabled = true
                    // Clear errors if any
                    tilClassCode.error = null
                }
                is JoinClassState.Loading -> {
                    joinButton.isEnabled = false
                    // Optionally show a loading dialog or progress bar here
                }
                is JoinClassState.Success -> {
                    joinButton.isEnabled = true
                    val data = state.data

                    // Navigate to Class Page
                    val intent = Intent(this, StudentClassPageActivity::class.java).apply {
                        putExtra("courseId", data.courseId)
                        putExtra("sectionName", data.className)
                        putExtra("role", "student")
                        putExtra("id", userId)
                        putExtra("toast_msg", data.message)
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    startActivity(intent)
                    setResult(RESULT_OK)
                    finish()
                }
                is JoinClassState.Error -> {
                    joinButton.isEnabled = true
                    UIUtils.errorDisplay(
                        this,
                        tilClassCode,
                        etClassCode,
                        true,
                        state.message
                    )

                }
            }
        }
    }
}
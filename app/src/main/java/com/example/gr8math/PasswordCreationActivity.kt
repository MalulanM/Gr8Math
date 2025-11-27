package com.example.gr8math

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.gr8math.api.ConnectURL
import com.example.gr8math.dataObject.User
import com.example.gr8math.utils.ShowToast
import com.example.gr8math.utils.UIUtils
import com.google.android.material.appbar.MaterialToolbar
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response

class PasswordCreationActivity : AppCompatActivity() {

    private lateinit var passwordInput: EditText
    private lateinit var confirmPasswordInput: EditText
    private lateinit var saveButton: Button

    // Loading Views
    private lateinit var loadingLayout: View
    private lateinit var loadingProgress: View
    private lateinit var loadingText: TextView

    // Data passed from previous activity
    private var email: String = ""
    private var firstName: String = ""
    private var lastName: String = ""
    private var lrn: String = "" // For Student
    private var teachingPos: String = "" // For Teacher
    private var gender: String = ""
    private var birthdate: String = ""
    private var role: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.change_password_activity)

        // 1. Get Data from Intent
        val intent = intent
        email = intent.getStringExtra("EXTRA_EMAIL") ?: ""
        firstName = intent.getStringExtra("EXTRA_FIRST_NAME") ?: ""
        lastName = intent.getStringExtra("EXTRA_LAST_NAME") ?: ""
        lrn = intent.getStringExtra("EXTRA_LRN") ?: "" // Might be empty if Teacher
        teachingPos = intent.getStringExtra("EXTRA_TEACHING_POS") ?: "" // Might be empty if Student
        gender = intent.getStringExtra("EXTRA_GENDER") ?: ""
        birthdate = intent.getStringExtra("EXTRA_BIRTHDATE") ?: ""
        role = intent.getStringExtra("EXTRA_ROLE") ?: ""

        initViews()
        setupListeners()
    }

    private fun initViews() {
        // Toolbar
        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }

        // Inputs
        passwordInput = findViewById(R.id.etNewPass)
        confirmPasswordInput = findViewById(R.id.etRePass)
        saveButton = findViewById(R.id.btnSave)

        // Loading
        loadingLayout = findViewById(R.id.loadingLayout)
        loadingProgress = findViewById(R.id.loadingProgressBg)
        loadingText = findViewById(R.id.loadingText)
    }

    private fun setupListeners() {
        // Enable button only when both fields have text
        val textWatcher = object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val pass = passwordInput.text.toString()
                val confirm = confirmPasswordInput.text.toString()
                saveButton.isEnabled = pass.isNotEmpty() && confirm.isNotEmpty()
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        }

        passwordInput.addTextChangedListener(textWatcher)
        confirmPasswordInput.addTextChangedListener(textWatcher)

        saveButton.setOnClickListener {
            registerUser()
        }
    }

    private fun isValidPassword(password: String): Boolean {
        // Regex: At least 1 Upper, 1 Lower, 1 Digit, 1 Special Char, 8-16 length
        val passwordPattern = Regex("^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[^A-Za-z\\d])[A-Za-z\\d[^A-Za-z\\d]]{8,16}\$")
        return passwordPattern.matches(password)
    }

    private fun registerUser() {
        val passwordText = passwordInput.text.toString().trim()
        val confirmPassText = confirmPasswordInput.text.toString().trim()

        // Validation
        if (passwordText != confirmPassText) {
            ShowToast.showMessage(this, "Passwords do not match")
            return
        }

        if (!isValidPassword(passwordText)) {
            ShowToast.showMessage(this, "Password does not meet requirements")
            return
        }

        // Prepare Data Object
        // Note: You might need to adjust your User data class if it doesn't handle 'role' or 'teachingPos' yet.
        // Assuming User class handles basic info:
        val user = User(
            firstName = firstName,
            lastName = lastName,
            emailAdd = email,
            passwordHash = passwordText,
            passwordHashConfirmation = confirmPassText,
            gender = gender,
            birthdate = birthdate,
            LRN = if (role == "Student") lrn else "",
            // teachingPosition = if (role == "Teacher") teachingPos else "" // Uncomment if your User model has this
        )

        // Show Loading
        UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, true)
        saveButton.isEnabled = false

        // API Call
        val apiService = ConnectURL.api
        val call = apiService.registerUser(user)

        call.enqueue(object : retrofit2.Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                saveButton.isEnabled = true

                try {
                    if (response.isSuccessful) {
                        ShowToast.showMessage(this@PasswordCreationActivity, "Account created successfully! Please wait for approval.")

                        // Navigate back to Login
                        val intent = Intent(this@PasswordCreationActivity, AppLoginActivity::class.java)
                        // Clear stack so user can't go back to registration
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        val errorMsg = response.errorBody()?.string() ?: "Registration failed"
                        Log.e("RegisterError", errorMsg)
                        ShowToast.showMessage(this@PasswordCreationActivity, "Registration failed. Please try again.")
                    }
                } catch (e: Exception) {
                    Log.e("RegisterException", "Error: ${e.message}")
                    ShowToast.showMessage(this@PasswordCreationActivity, "An error occurred.")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                saveButton.isEnabled = true
                Log.e("NetworkError", "onFailure: ${t.localizedMessage}")
                ShowToast.showMessage(this@PasswordCreationActivity, "Failed to connect. Check internet.")
            }
        })
    }
}
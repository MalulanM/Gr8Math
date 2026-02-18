package com.example.gr8math.Activity.LoginAndRegister

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.gr8math.Activity.StudentModule.ClassManager.StudentClassManagerActivity
import com.example.gr8math.Activity.TeacherModule.ClassManager.TeacherClassManagerActivity
import com.example.gr8math.R
import com.example.gr8math.Utils.ShowToast
import com.example.gr8math.Utils.UIUtils
import com.example.gr8math.ViewModel.ForgotPasswordViewModel
import com.example.gr8math.ViewModel.ForgotState
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.TextInputLayout

class ForgotPasswordActivity : AppCompatActivity() {

    private val viewModel: ForgotPasswordViewModel by viewModels()
    private var isDirectChange: Boolean = false

    private var id: Int = 0
    private var name: String? = ""
    private var profilePic: String? = ""
    private var fromProfileRole: String? = ""
    // Screen 1 UI
    private lateinit var emailInput: EditText
    private lateinit var codeInput: EditText
    private lateinit var verifyBtn: Button
    private lateinit var txtSendCode: TextView
    private lateinit var tilCode: TextInputLayout

    // Screen 2 UI
    private lateinit var newPasswordInput: EditText
    private lateinit var confirmPasswordInput: EditText
    private lateinit var savePassBtn: Button
    private lateinit var tilNewPassLayout: TextInputLayout
    private lateinit var tilConfirmPassLayout: TextInputLayout

    // Loading UI
    private lateinit var loadingLayout: View
    private lateinit var loadingProgress: View
    private lateinit var loadingText: TextView



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fromProfileRole = intent.getStringExtra("EXTRA_ROLE")
        isDirectChange = intent.getBooleanExtra("EXTRA_IS_DIRECT_CHANGE", false)

        id = intent.getIntExtra("id", 0)
        name = intent.getStringExtra("name")
        profilePic = intent.getStringExtra("profilePic")

        if (isDirectChange) {
            showForgotPasswordActivityOne()
        } else {
            showForgotPasswordActivity()
        }

        // Listen to Supabase responses
        setupObservers()
    }

    // --- OBSERVERS (The Bridge to Supabase) ---
    private fun setupObservers() {
        viewModel.state.observe(this) { state ->

            // Hide loading by default unless state is specifically Loading
            if (state !is ForgotState.Loading) {
                UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
            }

            when (state) {
                is ForgotState.Loading -> {
                    UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, true)
                }
                is ForgotState.CodeSent -> {
                    ShowToast.showMessage(this, "Verification code sent!")
                    enableCodeInput()
                    startCountdown()
                }
                is ForgotState.CodeVerified -> {
                    // Success Step 1: Move to Password Screen
                    showForgotPasswordActivityOne()
                }
                is ForgotState.PasswordUpdated -> {
                    // Success Step 2: Done
                    ShowToast.showMessage(this, "Password updated successfully!")
                    navigateToLoginOrDashboard()
                }
                is ForgotState.Error -> {
                    ShowToast.showMessage(this, state.message)
                    // Reset buttons if error occurred
                    if (::txtSendCode.isInitialized) txtSendCode.isEnabled = true
                    if (::verifyBtn.isInitialized) verifyBtn.isEnabled = true
                    if (::savePassBtn.isInitialized) savePassBtn.isEnabled = true
                }
            }
        }
    }

    // --- BUTTON ACTIONS ---

    private fun sendCode() {
        val email = emailInput.text.toString().trim()
        viewModel.sendCode(email) // Calls Supabase
    }

    private fun verifyCode() {
        val email = emailInput.text.toString().trim()
        val code = codeInput.text.toString().trim()
        viewModel.verifyCode(email, code) // Calls Supabase
    }

    private fun updatePassword() {
        val newPass = newPasswordInput.text.toString()
        val confirmPass = confirmPasswordInput.text.toString()

        // 1. Log to see if click works
        android.util.Log.d("DEBUG_PASS", "Button Clicked. New: $newPass, Confirm: $confirmPass")

        if (newPass.isEmpty() || confirmPass.isEmpty()) {
            ShowToast.showMessage(this, "Please fill in all fields")
            return
        }

        if (newPass != confirmPass) {
            ShowToast.showMessage(this, "Passwords do not match")
            return
        }


        if (!isValidPassword(newPass)) {
            ShowToast.showMessage(this, "Password must have 1 Upper, 1 Lower, 1 Number, and 1 Special Char")
            return
        }

        // 3. Disable button so they don't click twice
        savePassBtn.isEnabled = false
        viewModel.updatePassword(newPass)
    }

    // --- UI HELPERS ---

    private fun startCountdown() {
        txtSendCode.isEnabled = false

        object : CountDownTimer(180000, 1000) {
            override fun onTick(ms: Long) {
                txtSendCode.text = "${ms / 1000} s"
            }
            override fun onFinish() {
                txtSendCode.text = "Get Code"
                txtSendCode.isEnabled = true
                txtSendCode.setTextColor(ContextCompat.getColor(this@ForgotPasswordActivity, R.color.colorLight))
            }
        }.start()
    }

    private fun enableCodeInput() {
        tilCode.setEndIconDrawable(null)
        codeInput.isEnabled = true
        codeInput.requestFocus()
        verifyBtn.isEnabled = true
    }

    private fun navigateToLoginOrDashboard() {
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = when (fromProfileRole) {
                "Teacher" -> Intent(this, TeacherClassManagerActivity::class.java).apply {
                    putExtra("id", id)
                    putExtra("role", "Teacher")
                    putExtra("name", name)
                    putExtra("profilePic", profilePic)
                    putExtra("toast_msg", "Password updated successfully!")
                }
                "Student" -> Intent(this, StudentClassManagerActivity::class.java).apply {
                    putExtra("id", id)
                    putExtra("role", "Student")
                    putExtra("name", name)
                    putExtra("profilePic", profilePic)
                    putExtra("toast_msg", "Password updated successfully!")
                }
                else -> Intent(this, AppLoginActivity::class.java)
            }

            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }, 800)
    }

    // --- SCREEN SETUP ---

    private fun showForgotPasswordActivity() {
        setContentView(R.layout.forgot_password_activity)
        initScreen1()
        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }

        txtSendCode.setOnClickListener { sendCode() }
        verifyBtn.setOnClickListener { verifyCode() }
    }


    private fun showForgotPasswordActivityOne() {
        setContentView(R.layout.forgot_password_activity_one)
        initScreen2()

        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }

        // 1. Initially disable the button
        savePassBtn.isEnabled = false

        // 2. Create the TextWatcher
        val passwordWatcher = object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val pass1 = newPasswordInput.text.toString().trim()
                val pass2 = confirmPasswordInput.text.toString().trim()

                // Enable button ONLY if both fields are not empty
                savePassBtn.isEnabled = pass1.isNotEmpty() && pass2.isNotEmpty()

                // Optional: Clear errors when user starts typing again
                tilNewPassLayout.error = null
                tilConfirmPassLayout.error = null
            }
        }

        // 3. Attach the watcher to both fields
        newPasswordInput.addTextChangedListener(passwordWatcher)
        confirmPasswordInput.addTextChangedListener(passwordWatcher)

        savePassBtn.setOnClickListener { updatePassword() }
    }


    private fun initScreen1() {
        codeInput = findViewById(R.id.etCode)
        emailInput = findViewById(R.id.etEmailCode)
        verifyBtn = findViewById(R.id.btnVerify)
        txtSendCode = findViewById(R.id.tvGetCode)
        tilCode = findViewById(R.id.tilCode)

        // FIXED: Initialize loading views here to prevent crash
        loadingLayout = findViewById(R.id.loadingLayout)
        loadingProgress = findViewById(R.id.loadingProgressBg)
        loadingText = findViewById(R.id.loadingText)

        codeInput.isEnabled = false
        verifyBtn.isEnabled = false
    }

    private fun initScreen2() {
        newPasswordInput = findViewById(R.id.etNewPass)
        confirmPasswordInput = findViewById(R.id.etRePass)
        savePassBtn = findViewById(R.id.btnSave)
        tilNewPassLayout = findViewById(R.id.tilNewPassword)
        tilConfirmPassLayout = findViewById(R.id.tilReEnterPassword)

        loadingLayout = findViewById(R.id.loadingLayout)
        loadingProgress = findViewById(R.id.loadingProgressBg)
        loadingText = findViewById(R.id.loadingText)
    }

    private fun isValidPassword(pass: String): Boolean {
        // 1. (?=.*[0-9])       -> At least one digit
        // 2. (?=.*[a-z])       -> At least one lowercase letter
        // 3. (?=.*[A-Z])       -> At least one uppercase letter
        // 4. (?=.*[^a-zA-Z0-9]) -> At least one SPECIAL character (Any symbol, including ^)
        // 5. .{8,16}           -> Must be between 8 and 16 characters long

        val pattern = Regex("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[^a-zA-Z0-9]).{8,16}$")

        return pattern.matches(pass)
    }
}
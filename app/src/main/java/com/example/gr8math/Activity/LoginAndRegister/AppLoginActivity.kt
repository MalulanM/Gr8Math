package com.example.gr8math.Activity.LoginAndRegister

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.gr8math.Activity.StudentModule.ClassManager.StudentClassManagerActivity
import com.example.gr8math.Activity.TeacherModule.ClassManager.TeacherClassManagerActivity
import com.example.gr8math.Data.Model.UserProfile
import com.example.gr8math.R
import com.example.gr8math.Utils.ShowToast
import com.example.gr8math.Utils.UIUtils
import com.example.gr8math.ViewModel.LoginState
import com.example.gr8math.ViewModel.LoginViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputLayout

class AppLoginActivity : AppCompatActivity() {

    // 1. Initialize ViewModel
    private val viewModel: LoginViewModel by viewModels()

    // Views
    lateinit var btnLogin: Button
    lateinit var etEmail: TextView
    lateinit var etPassword: TextView
    lateinit var tilPassword: TextInputLayout
    lateinit var tilEmail: TextInputLayout
    lateinit var loadingLayout: View
    lateinit var loadingProgress: View
    lateinit var loadingText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pref = getSharedPreferences("user_session", MODE_PRIVATE)
        val isLoggedIn = pref.getBoolean("isLoggedIn", false)

        if (isLoggedIn) {
            // Bypass the login screen and go straight to dashboard
            autoLogin(pref)
            return // Stop onCreate here so the login screen never loads!
        }

        // 2. If NOT logged in, load the normal login screen
        setContentView(R.layout.app_login_activity)


        initViews()
        setupObservers()

        val toastMsg = intent.getStringExtra("toast_msg")
        if (!toastMsg.isNullOrEmpty()) {
            ShowToast.showMessage(this, toastMsg)
        }

        // Register Button
        findViewById<MaterialButton>(R.id.btnRegister).setOnClickListener {
            startActivity(Intent(this, RegisterRoleActivity::class.java))
        }

        // Forgot Password Button
        findViewById<View>(R.id.tvForgot).setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        // Login Button
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()

            viewModel.login(email, password)
        }


    }

    private fun initViews() {
        btnLogin = findViewById(R.id.btnLogin)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        tilPassword = findViewById(R.id.tilPassword)
        tilEmail = findViewById(R.id.tilEmail)
        loadingLayout = findViewById(R.id.loadingLayout)
        loadingProgress = findViewById(R.id.loadingProgressBg)
        loadingText = findViewById(R.id.loadingText)
    }

    // React to state changes
    private fun setupObservers() {
        viewModel.loginState.observe(this) { state ->

            // Always clear errors at the start of a state change
            clearErrors()

            when (state) {
                is LoginState.Loading -> {
                    UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, true)
                    btnLogin.isEnabled = false
                }

                is LoginState.Success -> {
                    UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                    btnLogin.isEnabled = true
                    navigateToDashboard(state.user)
                }

                is LoginState.ShowTerms -> {
                    UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                    btnLogin.isEnabled = true
                }

                is LoginState.InputError -> {
                    // CASE 1: Empty Fields -> Show RED TEXT on fields (No Toast)
                    UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                    btnLogin.isEnabled = true

                    if (state.emailMsg != null) {
                        UIUtils.errorDisplay(this, tilEmail, etEmail, true, state.emailMsg)
                    }
                    if (state.passMsg != null) {
                        UIUtils.errorDisplay(this, tilPassword, etPassword, true, state.passMsg)
                    }
                }

                is LoginState.Error -> {
                    // CASE 2: Wrong Password/Email -> Show TOAST (and highlight fields)
                    UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                    btnLogin.isEnabled = true


                    ShowToast.showMessage(this, state.message)

                }
            }
        }
    }

    private fun clearErrors() {
        tilEmail.error = null
        tilEmail.isErrorEnabled = false
        tilPassword.error = null
        tilPassword.isErrorEnabled = false
    }

    private fun navigateToDashboard(user: UserProfile) {
        val pref = getSharedPreferences("user_session", MODE_PRIVATE)
        pref.edit().apply {
            putBoolean("isLoggedIn", true)
            putInt("id", user.id ?: -1)
            putString("role", user.roles)
            putString("name", user.firstName)
            putString("profilePic", user.profilePic)
            apply()
        }

        val nextIntent = when (user.roles.lowercase()) {
            "student" -> Intent(this, StudentClassManagerActivity::class.java)
            "teacher" -> Intent(this, TeacherClassManagerActivity::class.java)
            else -> Intent(this, AppLoginActivity::class.java)
        }

        nextIntent.putExtra("toast_msg", "Welcome back, ${user.firstName}")
        nextIntent.putExtra("id", user.id)
        nextIntent.putExtra("role", user.roles)
        nextIntent.putExtra("name", user.firstName)
        nextIntent.putExtra("profilePic", user.profilePic)
        nextIntent.putExtra("notif_type", intent.getStringExtra("notif_type"))
        nextIntent.putExtra("notif_meta", intent.getStringExtra("notif_meta"))

        startActivity(nextIntent)
        finish()
    }

    private fun autoLogin(pref: android.content.SharedPreferences) {
        val role = pref.getString("role", "") ?: ""
        val id = pref.getInt("id", -1)
        val name = pref.getString("name", "")
        val profilePic = pref.getString("profilePic", "")

        val nextIntent = when (role.lowercase()) {
            "student" -> Intent(this, StudentClassManagerActivity::class.java)
            "teacher" -> Intent(this, TeacherClassManagerActivity::class.java)
            else -> Intent(this, AppLoginActivity::class.java)
        }

        nextIntent.putExtra("id", id)
        nextIntent.putExtra("role", role)
        nextIntent.putExtra("name", name)
        nextIntent.putExtra("profilePic", profilePic)

        // Pass along any push notification data if they tapped a banner while closed!
        nextIntent.putExtra("notif_type", intent.getStringExtra("notif_type"))
        nextIntent.putExtra("notif_meta", intent.getStringExtra("notif_meta"))

        startActivity(nextIntent)
        finish()
    }

}
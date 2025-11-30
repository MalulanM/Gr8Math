package com.example.gr8math

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.gr8math.api.ConnectURL
import com.example.gr8math.utils.ShowToast
import com.example.gr8math.utils.UIUtils
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.TextInputLayout
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var emailInput: EditText
    private lateinit var codeInput: EditText
    private lateinit var verifyBtn: Button
    private lateinit var txtSendCode: TextView
    private lateinit var tilCode: TextInputLayout

    private lateinit var newPasswordInput: EditText
    private lateinit var confirmPasswordInput: EditText
    private lateinit var savePassBtn: Button
    private lateinit var tilNewPassLayout: TextInputLayout
    private lateinit var tilConfirmPassLayout: TextInputLayout

    private lateinit var loadingLayout: View
    private lateinit var loadingProgress: View
    private lateinit var loadingText: TextView

    private var fromProfileRole: String? = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fromProfileRole = intent.getStringExtra("EXTRA_ROLE")
        showForgotPasswordActivity()
    }

    /** FIRST SCREEN */
    private fun showForgotPasswordActivity() {
        setContentView(R.layout.forgot_password_activity)

        initScreen1()

        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }

        txtSendCode.setOnClickListener { sendCode() }
        verifyBtn.setOnClickListener { verifyCode() }
    }

    /** SECOND SCREEN */
    private fun showForgotPasswordActivityOne() {
        setContentView(R.layout.forgot_password_activity_one)

        initScreen2()

        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }

        savePassBtn.isEnabled = true

        newPasswordInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                UIUtils.errorDisplay(this@ForgotPasswordActivity, tilNewPassLayout, newPasswordInput, false,"Please enter a password")
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        confirmPasswordInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                UIUtils.errorDisplay(this@ForgotPasswordActivity, tilConfirmPassLayout, confirmPasswordInput, false,"Please enter a password")
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        savePassBtn.setOnClickListener { updatePassword() }
    }

    /** INITIALIZE FIRST SCREEN */
    private fun initScreen1() {
        codeInput = findViewById(R.id.etCode)
        emailInput = findViewById(R.id.etEmailCode)
        verifyBtn = findViewById(R.id.btnVerify)
        txtSendCode = findViewById(R.id.tvGetCode)
        codeInput.isEnabled = false
        tilCode = findViewById(R.id.tilCode)
    }

    /** INITIALIZE SECOND SCREEN */
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

    /** SEND CODE */
    private fun sendCode() {
        txtSendCode.isEnabled = false
        txtSendCode.isClickable = false
        txtSendCode.setBackgroundColor(ContextCompat.getColor(this, R.color.colorLight))

        val api = ConnectURL.api
        val call = api.sendCode(emailInput.text.toString())

        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {

                val body = response.errorBody()?.string()
                    ?: response.body()?.string()
                    ?: ""

                Log.e("sendCode", "Code=${response.code()} | Body=$body")

                if (body.isEmpty()) {
                    ShowToast.showMessage(this@ForgotPasswordActivity, "Empty response.")
                    txtSendCode.isEnabled = true
                    txtSendCode.isClickable = true
                    return
                }

                val json = JSONObject(body)
                val success = json.optBoolean("success", false)
                val msg = json.optString("message", "")

                if (!success) {
                    ShowToast.showMessage(this@ForgotPasswordActivity, msg)
                    txtSendCode.isEnabled = true
                    txtSendCode.isClickable = true
                    return
                }

                ShowToast.showMessage(this@ForgotPasswordActivity, "Verification code sent!")

                tilCode.setEndIconDrawable(null)

                codeInput.isEnabled = true
                codeInput.requestFocus()

                codeInput.setOnKeyListener { _, _, _ ->
                    verifyBtn.isEnabled = codeInput.text.isNotEmpty()
                    false
                }

                /** COUNTDOWN */
                object : CountDownTimer(180000, 1000) {
                    override fun onTick(ms: Long) {
                        txtSendCode.text = "${ms / 1000} s"
                    }

                    override fun onFinish() {
                        txtSendCode.text = "Get Code"
                        txtSendCode.isClickable = true
                        txtSendCode.isEnabled = true
                        codeInput.isEnabled = false
                    }
                }.start()
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e("sendCode", "Failure ${t.message}")
                ShowToast.showMessage(this@ForgotPasswordActivity, "Failed to connect.")
                txtSendCode.isEnabled = true
                txtSendCode.isClickable = true
            }
        })
    }

    /** VERIFY CODE */
    private fun verifyCode() {
        verifyBtn.isEnabled = false

        val api = ConnectURL.api
        val call = api.verifyCode(emailInput.text.toString(), codeInput.text.toString())

        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {

                val raw = response.errorBody()?.string()
                    ?: response.body()?.string()
                    ?: "{}"

                Log.e("verifyCode", "Code=${response.code()} | Body=$raw")

                val json = JSONObject(raw)
                val success = json.optBoolean("success", false)
                val msg = json.optString("message", "")

                if (success) {
                    showForgotPasswordActivityOne()
                } else {
                    ShowToast.showMessage(this@ForgotPasswordActivity, msg)
                    verifyBtn.isEnabled = true
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                verifyBtn.isEnabled = true
                ShowToast.showMessage(this@ForgotPasswordActivity, "Connection error.")
            }
        })
    }

    /** PASSWORD REGEX FIXED */
    private fun isValidPassword(pass: String): Boolean {
        val pattern = Regex("^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[@\$!%*?&])[A-Za-z\\d@\$!%*?&]{8,16}\$")
        return pattern.matches(pass)
    }

    /** UPDATE PASSWORD */
    private fun updatePassword() {

        if (newPasswordInput.text.isEmpty() ||
            confirmPasswordInput.text.isEmpty()
        ) {
            UIUtils.errorDisplay(this, tilNewPassLayout, newPasswordInput, false, "Please enter password")
            UIUtils.errorDisplay(this, tilConfirmPassLayout, confirmPasswordInput, false, "Please enter password")
            return
        }

        if (newPasswordInput.text.toString() != confirmPasswordInput.text.toString()) {
            ShowToast.showMessage(this, "Passwords do not match")
            return
        }

        if (!isValidPassword(newPasswordInput.text.toString())) {
            ShowToast.showMessage(this, "Password invalid")
            return
        }

        newPasswordInput.isEnabled = false
        confirmPasswordInput.isEnabled = false
        savePassBtn.isEnabled = false

        UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, true)

        val api = ConnectURL.api
        val call = api.savePass(
            emailInput.text.toString(),
            codeInput.text.toString(),
            newPasswordInput.text.toString(),
            confirmPasswordInput.text.toString()
        )

        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {

                val jsonStr = response.errorBody()?.string()
                    ?: response.body()?.string()
                    ?: "{}"

                val json = JSONObject(jsonStr)
                val msg = json.optString("message", "")

                Log.e("newPass", "Code=${response.code()} | Body=$jsonStr")

                UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)

                if (response.isSuccessful) {

                    android.os.Handler(mainLooper).postDelayed({

                        when (fromProfileRole) {
                            "Teacher" -> {
                                startActivity(Intent(this@ForgotPasswordActivity, TeacherClassManagerActivity::class.java))
                            }
                            "Student" -> {
                                startActivity(Intent(this@ForgotPasswordActivity,
                                    StudentClassManagerActivity::class.java))
                            }
                            else -> {
                                startActivity(Intent(this@ForgotPasswordActivity, AppLoginActivity::class.java))
                            }
                        }

                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                        finish()

                    }, 800)

                } else {
                    ShowToast.showMessage(this@ForgotPasswordActivity, msg)
                    newPasswordInput.isEnabled = true
                    confirmPasswordInput.isEnabled = true
                    savePassBtn.isEnabled = true
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                newPasswordInput.isEnabled = true
                confirmPasswordInput.isEnabled = true
                savePassBtn.isEnabled = true
                ShowToast.showMessage(this@ForgotPasswordActivity, "Connection error.")
            }
        })
    }
}

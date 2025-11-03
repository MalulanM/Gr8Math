package com.example.gr8math // <-- match your package

import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.gr8math.utils.UIUtils
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import retrofit2.Call
import kotlinx.coroutines.*


class ForgotPasswordActivity : AppCompatActivity() {
    lateinit var codeInput: EditText
    lateinit var emailInput: EditText
    lateinit var newPasswordInput: EditText
    lateinit var confirmPasswordInput: EditText

    lateinit var verifyBtn: Button

    lateinit var savePassBtn : Button

    lateinit var txtSendCode : TextView

    lateinit var tillNewPassLayout : TextInputLayout

    lateinit var tillConfirmPassLayout : TextInputLayout

    lateinit var tilCode : TextInputLayout

    lateinit var loadingLayout : View

    lateinit var loadingProgress : View

    lateinit var loadingText : TextView



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        showForgotPasswordActivity()
    }

    private fun showForgotPasswordActivity(){
        setContentView(R.layout.forgot_password_activity)
        init()

        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }

        txtSendCode.setOnClickListener {
            sendCode()
        }

        verifyBtn.setOnClickListener {
            verifyCode()
        }

    }

    private fun showForgotPasswordActivityOne(){
        setContentView(R.layout.forgot_password_activity_one)
        init2()
        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }

        savePassBtn.isEnabled = true

        newPasswordInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                UIUtils.errorDisplay(this@ForgotPasswordActivity, tillNewPassLayout, newPasswordInput, false,"Please enter a password")
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        confirmPasswordInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                UIUtils.errorDisplay(this@ForgotPasswordActivity, tillConfirmPassLayout, confirmPasswordInput, false,"Please enter a password")
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        savePassBtn.setOnClickListener {
            updatePassword()
        }
    }


    fun init(){
        codeInput = findViewById(R.id.etCode);
        emailInput = findViewById(R.id.etEmailCode);
        verifyBtn = findViewById(R.id.btnVerify);
        txtSendCode = findViewById(R.id.tvGetCode);
        codeInput.isEnabled = false;
        tilCode = findViewById(R.id.tilCode)
    }

    fun init2(){
        newPasswordInput = findViewById(R.id.etNewPass);
        confirmPasswordInput = findViewById(R.id.etRePass);
        savePassBtn = findViewById(R.id.btnSave);
        tillNewPassLayout = findViewById(R.id.tilNewPassword)
        tillConfirmPassLayout = findViewById(R.id.tilReEnterPassword)
        loadingLayout = findViewById(R.id.loadingLayout)
        loadingProgress = findViewById(R.id.loadingProgressBg)
        loadingText = findViewById(R.id.loadingText)
    }

    fun sendCode() {
        txtSendCode.setBackgroundColor(ContextCompat.getColor(this, R.color.colorLight))

        val apiService = ConnectURL.api
        val call = apiService.sendCode(emailInput.text.toString())

        call.enqueue(object : retrofit2.Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: retrofit2.Response<ResponseBody>) {
                try {
                    val rawResponse = response.errorBody()?.string() ?: response.body()?.string() ?: ""
                    Log.e("sendCode", "Code: ${response.code()} | Body: $rawResponse")

                    if (rawResponse.isEmpty()) {
                        ShowToast.showMessage(this@ForgotPasswordActivity,"Empty response from server.")
                        return
                    }

                    val jsonObj = org.json.JSONObject(rawResponse)
                    val success = jsonObj.optBoolean("success", false)
                    val msg = jsonObj.optString("message", "No message from server")

                    if (success) {
                        ShowToast.showMessage(this@ForgotPasswordActivity, "A verification code has been sent to your email.")

                        tilCode.setEndIconDrawable(null)
                        txtSendCode.isClickable = false
                        txtSendCode.isEnabled = false

                        codeInput.isEnabled = true
                        codeInput.requestFocus()

                        codeInput.setOnKeyListener { _, _, _ ->
                            verifyBtn.isEnabled = codeInput.text.isNotEmpty()
                            false
                        }

                        object : CountDownTimer(180000, 1000){
                            override fun onTick(millisUntilFinished: Long){
                                val seconds = millisUntilFinished / 1000
                                txtSendCode.text = "${seconds} s"
                            }

                            override fun onFinish() {
                                txtSendCode.text = "Get Code"
                                txtSendCode.isClickable = true
                                txtSendCode.isEnabled = true
                                codeInput.isEnabled = false
                                tilCode.setEndIconDrawable(R.drawable.ic_disable)
                            }
                        }.start()
                    } else {
                        ShowToast.showMessage(this@ForgotPasswordActivity, msg)
                    }

                } catch (e: Exception) {
                    Log.e("sendCode", "Exception: ${e.message}", e)
                    ShowToast.showMessage(this@ForgotPasswordActivity, "An error occurred while handling the response.")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e("sendCode", "onFailure: ${t.localizedMessage}", t)
                ShowToast.showMessage(this@ForgotPasswordActivity, "Failed to connect to server. Check your internet connection.")
                  }
        })
    }

    fun verifyCode() {
        val apiService = ConnectURL.api
        val call = apiService.verifyCode(emailInput.text.toString().trim(), codeInput.text.toString().trim())
        call.enqueue(object : retrofit2.Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: retrofit2.Response<ResponseBody>) {
                try {
                    val raw = response.errorBody()?.string() ?: response.body()?.string() ?: ""
                    Log.e("verifyCode", "Code: ${response.code()} | Body: $raw")

                    val jsonObj = org.json.JSONObject(raw)
                    val success = jsonObj.optBoolean("success", false)
                    val msg = jsonObj.optString("message", "No message from server")

                    if (success) {
//                        ShowToast.showMessage(this@ForgotPasswordActivity, "Code verified successfully!")
                        showForgotPasswordActivityOne()
                    } else {
                        ShowToast.showMessage(this@ForgotPasswordActivity, msg)

                        if (msg.contains("expired", true)) {
                            txtSendCode.isClickable = true
                            txtSendCode.isEnabled = true
                            codeInput.isEnabled = false
                            verifyBtn.isEnabled = false
                        }
                    }

                } catch (e: Exception) {
                    Log.e("verifyCode", "Exception: ${e.message}", e)
                    ShowToast.showMessage(this@ForgotPasswordActivity, "An error occurred while processing the response.")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e("verifyCode", "onFailure: ${t.localizedMessage}", t)
                ShowToast.showMessage(this@ForgotPasswordActivity, "Failed to connect to the server. Please check your internet connection.")
            }
        })
    }

    fun isValidPassword(password: String): Boolean {
        val passwordPattern = Regex("^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[^A-Za-z\\d])[A-Za-z\\d[^A-Za-z\\d]]{8,16}\$")
        return passwordPattern.matches(password)
    }

    fun updatePassword(){

        if(newPasswordInput.text.toString().isEmpty() || confirmPasswordInput.text.toString().isEmpty()){
            UIUtils.errorDisplay(this@ForgotPasswordActivity, tillNewPassLayout, newPasswordInput, false,"Please enter a password")
            UIUtils.errorDisplay(this@ForgotPasswordActivity, tillConfirmPassLayout, confirmPasswordInput, false,"Please enter a password")
            return
        }


        if(newPasswordInput.text.toString() != confirmPasswordInput.text.toString()){
            ShowToast.showMessage(this@ForgotPasswordActivity, "Passwords do not match")
            return
        }

        if(!isValidPassword(newPasswordInput.text.toString())){
            ShowToast.showMessage(this@ForgotPasswordActivity, "Password Invalid")
            return
        }

        newPasswordInput.isEnabled = false
        confirmPasswordInput.isEnabled = false
        savePassBtn.isEnabled = false
        UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, true)

        val apiService = ConnectURL.api
        val call = apiService.savePass(emailInput.text.toString(), codeInput.text.toString(),newPasswordInput.text.toString(), confirmPasswordInput.text.toString())
        call.enqueue(object : retrofit2.Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: retrofit2.Response<ResponseBody>) {
                UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                val responseBody = response.body()?.string()
                val errorBody = response.errorBody()?.string()
                val responseString = responseBody ?: errorBody

                Log.e("newPass", "Code: ${response.code()} | Body: ${responseString}")

                val jsonObj = org.json.JSONObject(responseString);
                val msg = jsonObj.getString("message")
                if(response.isSuccessful){
//                    ShowToast.showMessage(this@ForgotPasswordActivity, msg)

                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        val intent = Intent(this@ForgotPasswordActivity, AppLoginActivity::class.java)
                        startActivity(intent)
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)

                        UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)

                        finish()
                    }, 800)

                } else {
                    ShowToast.showMessage(this@ForgotPasswordActivity, msg)
                    UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                    newPasswordInput.isEnabled = true
                    confirmPasswordInput.isEnabled = true
                    savePassBtn.isEnabled = true
                }
            }
            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                newPasswordInput.isEnabled = true
                confirmPasswordInput.isEnabled = true
                savePassBtn.isEnabled = true
                UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)

                Log.e("RetrofitError", "onFailure: ${t.localizedMessage}", t)
                ShowToast.showMessage(this@ForgotPasswordActivity, "Failed to connect to the server. Please check your internet connection.")
            }
        })
    }





}

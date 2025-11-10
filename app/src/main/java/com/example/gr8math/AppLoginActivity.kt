package com.example.gr8math   // <-- match your real package!

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.content.Intent
import android.util.Log
import android.widget.Button
import android.widget.TextView
import com.google.android.material.textfield.TextInputLayout
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import com.example.gr8math.api.ConnectURL
import com.example.gr8math.dataObject.LoginUser
import com.example.gr8math.utils.ShowToast
import com.example.gr8math.utils.UIUtils

class AppLoginActivity : AppCompatActivity() {
    lateinit var btnLogin: Button
    lateinit var etEmail : TextView
    lateinit var etPassword : TextView
    lateinit var tilPassword: TextInputLayout
    lateinit var tilEmail: TextInputLayout
    lateinit var loadingLayout : View

    lateinit var loadingProgress : View

    lateinit var loadingText : TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.app_login_activity)   // <-- must match your XML file name
        ConnectURL.init(this)
        // inside AppLoginActivity after setContentView(...)

        val toastMsg = intent.getStringExtra("toast_msg")
        if (!toastMsg.isNullOrEmpty()) {
            ShowToast.showMessage(this, toastMsg)
        }

        init()
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnRegister).setOnClickListener {
            startActivity(android.content.Intent(this, RegisterActivity::class.java))
        }

        // "Forgot Password?" -> open ForgotPasswordActivity (UI only)
        findViewById<View>(R.id.tvForgot).setOnClickListener {
            startActivity(Intent(this@AppLoginActivity, ForgotPasswordActivity::class.java))
        }

        btnLogin.setOnClickListener {
            login()
        }

    }

    fun init(){
        btnLogin = findViewById(R.id.btnLogin)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        tilPassword = findViewById(R.id.tilPassword)
        tilEmail = findViewById(R.id.tilEmail)
        loadingLayout =  findViewById<View>(R.id.loadingLayout)
        loadingProgress = findViewById<View>(R.id.loadingProgressBg)
        loadingText = findViewById<TextView>(R.id.loadingText)

    }
    fun login(){

        if (etEmail.text.toString().isEmpty() || etPassword.text.toString().isEmpty()) {
            UIUtils.errorDisplay(this@AppLoginActivity, tilEmail, etEmail, true, "Please input valid credentials")
            UIUtils.errorDisplay(this@AppLoginActivity, tilPassword, etPassword, false, "Please input valid credentials")
            return
        }

        val apiService = ConnectURL.publicApi
        val user = LoginUser(
            email = etEmail.text.toString(),
            password = etPassword.text.toString()
        )
        btnLogin.isEnabled = false
        UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, true)
        val call = apiService.loginUser(user)
        call.enqueue(object : retrofit2.Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                try {
                    val responseString = response.errorBody()?.string() ?: response.body()?.string()

//                    Log.e("LoginResponse", "${responseString}")

                    if (responseString.isNullOrEmpty()) {
                        ShowToast.showMessage(this@AppLoginActivity, "Empty response from server.")
                        UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                        btnLogin.isEnabled = true
                        return
                    }

                    if (!responseString.trimStart().startsWith("{")) {
                        ShowToast.showMessage(this@AppLoginActivity, "Invalid response from server.")
                        UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                        btnLogin.isEnabled = true
                        return
                    }

                    val jsonObj = org.json.JSONObject(responseString)
                    val success = jsonObj.optBoolean("status", false)
                    val msg = jsonObj.optString("message", "No message from server")

                    if (success) {
                        val data = jsonObj.optJSONObject("data")
                        val user =  data.optJSONObject("user")
//                        val token = data.optString("token")
                        val role = data.optString("role")
                        val id = user.optInt("id")
                        val pref = getSharedPreferences("user_session", MODE_PRIVATE)
//                        pref.edit().putString("auth_token", token).apply()
                        ConnectURL.init(this@AppLoginActivity)

                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            val nextIntent = when(role) {
                                "student" -> Intent(this@AppLoginActivity, StudentClassManagerActivity::class.java)
                                "teacher" -> Intent(this@AppLoginActivity, TeacherClassManagerActivity::class.java)
                                else -> Intent(this@AppLoginActivity, AccountManagementActivity::class.java)
                            }
                            nextIntent.putExtra("toast_msg", msg)
                            nextIntent.putExtra("id", id)
                            nextIntent.putExtra("role", role)

                            UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                            startActivity(nextIntent)
                            finish()
                        }, 3000)

                    } else {
                        btnLogin.isEnabled = true
                        UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                        ShowToast.showMessage(this@AppLoginActivity, msg)
                    }
                } catch (e: Exception) {
                    btnLogin.isEnabled = true
//                    Log.e("loginSession", "Exception: ${e.message}", e)
                    UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                    ShowToast.showMessage(this@AppLoginActivity, "An error occurred while handling the response.")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                btnLogin.isEnabled = true
                Log.e("RetrofitError", "onFailure: ${t.localizedMessage}", t)
                UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                ShowToast.showMessage(this@AppLoginActivity, "Failed to connect to server. Check your internet connection.")
            }
        })

    }

}

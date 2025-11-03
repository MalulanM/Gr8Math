package com.example.gr8math   // <-- match your real package!

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.content.Intent
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.textfield.TextInputLayout
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import android.text.Editable
import android.text.TextWatcher
import com.example.gr8math.utils.UIUtils

class AppLoginActivity : AppCompatActivity() {
    lateinit var btnLogin: Button
    lateinit var etEmail : TextView
    lateinit var etPassword : TextView
    lateinit var tilPassword: TextInputLayout
    lateinit var tilEmail: TextInputLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.app_login_activity)   // <-- must match your XML file name
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

        etEmail.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                UIUtils.errorDisplay(this@AppLoginActivity, tilEmail, etEmail, true, "Please input valid credentials")
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        etPassword.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                UIUtils.errorDisplay(this@AppLoginActivity, tilPassword, etPassword, false, "Please input valid credentials")
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

    }
    fun login(){

        if (etEmail.text.toString().isEmpty() || etPassword.text.toString().isEmpty()) {
            UIUtils.errorDisplay(this@AppLoginActivity, tilEmail, etEmail, true, "Please input valid credentials")
            UIUtils.errorDisplay(this@AppLoginActivity, tilPassword, etPassword, false, "Please input valid credentials")
            return
        }

        val apiService = ConnectURL.api
        val user = LoginUser(
            email = etEmail.text.toString(),
            password = etPassword.text.toString()
        )

        val call = apiService.loginUser(user)
        call.enqueue(object : retrofit2.Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                try {
                    val responseString = response.errorBody()?.string() ?: response.body()?.string()

                    Log.e("LoginResponse", "Code: ${response.code()} | Body: ${responseString}")

                    if (responseString.isNullOrEmpty()) {
                        ShowToast.showMessage(this@AppLoginActivity, "Empty response from server.")
                        return
                    }

                    val jsonObj = org.json.JSONObject(responseString)
                    val success = jsonObj.optBoolean("status", false)
                    val msg = jsonObj.optString("message", "No message from server")

                    if (success) {
                        ShowToast.showMessage(this@AppLoginActivity, "Login Successful")
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            val intent = Intent(this@AppLoginActivity, AppLoginActivity::class.java)
                            startActivity(intent)
                            finish()
                        }, 3000)
                    } else {
                        ShowToast.showMessage(this@AppLoginActivity, msg)
                    }
                } catch (e: Exception) {
                    Log.e("loginSession", "Exception: ${e.message}", e)
                    ShowToast.showMessage(this@AppLoginActivity, "An error occurred while handling the response.")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e("RetrofitError", "onFailure: ${t.localizedMessage}", t)
                ShowToast.showMessage(this@AppLoginActivity, "Failed to connect to server. Check your internet connection.")
            }
        })

    }

}

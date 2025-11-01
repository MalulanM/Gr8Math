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

    }
    fun login(){
        if(etEmail.text.toString().isEmpty() || etPassword.text.toString().isEmpty()){
            if (etEmail.text.toString().isEmpty()) {
                tilEmail.isErrorEnabled = true
                tilEmail.error = "Please input valid credentials"
                tilEmail.setErrorIconDrawable(R.drawable.ic_warning)
                tilEmail.setErrorTextColor(ContextCompat.getColorStateList(this, R.color.colorRed))
                tilEmail.setErrorIconTintList(ContextCompat.getColorStateList(this, R.color.colorRed))
                tilEmail.setBoxStrokeColorStateList(ContextCompat.getColorStateList(this, R.color.colorRed))
            } else {
                tilEmail.isErrorEnabled = false
                tilEmail.error = null
                tilEmail.setBoxStrokeColorStateList(
                    ContextCompat.getColorStateList(this, R.color.til_stroke)
                )
            }

            if (etPassword.text.toString().isEmpty()) {
                tilPassword.isErrorEnabled = true
                tilPassword.error = "Please input valid credentials"
                tilPassword.setErrorIconDrawable(null)
                tilPassword.setErrorTextColor(ContextCompat.getColorStateList(this, R.color.colorRed))
                tilPassword.setErrorIconTintList(ContextCompat.getColorStateList(this, R.color.colorRed))
                tilPassword.setBoxStrokeColorStateList(ContextCompat.getColorStateList(this, R.color.colorRed))
            } else {
                tilPassword.isErrorEnabled = false
                tilPassword.error = null
                tilPassword.setBoxStrokeColorStateList(
                    ContextCompat.getColorStateList(this, R.color.til_stroke)
                )
            }

            return
        }

        tilEmail.isErrorEnabled = false
        tilEmail.error = null
        tilEmail.setBoxStrokeColorStateList(
            ContextCompat.getColorStateList(this, R.color.til_stroke)
        )
        tilPassword.isErrorEnabled = false
        tilPassword.error = null
        tilPassword.setBoxStrokeColorStateList(
            ContextCompat.getColorStateList(this, R.color.til_stroke)
        )

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

package com.example.gr8math // <-- match your package

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import okhttp3.ResponseBody
import retrofit2.Call

class ForgotPasswordActivity : AppCompatActivity() {
    lateinit var codeInput: EditText;
    lateinit var emailInput: EditText;
    lateinit var newPasswordInput: EditText;
    lateinit var confirmPasswordInput: EditText;

    lateinit var verifyBtn: Button;

    lateinit var savePassBtn : Button;

    lateinit var txtSendCode : TextView;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.forgot_password_activity)

        /*findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }
        init()

        txtSendCode.setOnClickListener {
            sendCode()
        }

        verifyBtn.setOnClickListener {
            verifyCode()
        }

        savePassBtn.setOnClickListener {
            updatePassword()
        }*/
    }

    fun init(){
        codeInput = findViewById(R.id.etCode);
        emailInput = findViewById(R.id.etEmailCode);
        newPasswordInput = findViewById(R.id.etNewPass);
        confirmPasswordInput = findViewById(R.id.etRePass);
        verifyBtn = findViewById(R.id.btnVerify);
        savePassBtn = findViewById(R.id.btnSave);
        txtSendCode = findViewById(R.id.tvGetCode);
        codeInput.isEnabled = false;
        newPasswordInput.isEnabled = false;
        confirmPasswordInput.isEnabled = false;
    }

    fun sendCode() {
        txtSendCode.setBackgroundColor(ContextCompat.getColor(this, R.color.ecf1f4))

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

                        txtSendCode.isClickable = false
                        txtSendCode.isEnabled = false

                        codeInput.isEnabled = true
                        verifyBtn.isEnabled = true
                        codeInput.requestFocus()
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
                        ShowToast.showMessage(this@ForgotPasswordActivity, "Code verified successfully!")
                        newPasswordInput.requestFocus()
                        newPasswordInput.isEnabled = true
                        confirmPasswordInput.isEnabled = true
                        savePassBtn.isEnabled = newPasswordInput.text.isNotEmpty() && confirmPasswordInput.text.isNotEmpty()

                        newPasswordInput.setOnKeyListener { _, _, _ ->
                            savePassBtn.isEnabled = newPasswordInput.text.isNotEmpty() && confirmPasswordInput.text.isNotEmpty()
                            false
                        }

                        confirmPasswordInput.setOnKeyListener { _, _, _ ->
                            savePassBtn.isEnabled = newPasswordInput.text.isNotEmpty() && confirmPasswordInput.text.isNotEmpty()
                            false
                        }
                        verifyBtn.isEnabled = false
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

        if(newPasswordInput.text.toString() != confirmPasswordInput.text.toString()){
            ShowToast.showMessage(this@ForgotPasswordActivity, "Passwords do not match")
            return
        }

        if(!isValidPassword(newPasswordInput.text.toString())){
            ShowToast.showMessage(this@ForgotPasswordActivity, "Password Requirement:\n- 8-16 characters\n- At least one uppercase letter\n- At least one number\n- At least one special character")
            return
        }

        val apiService = ConnectURL.api
        val call = apiService.savePass(emailInput.text.toString(), codeInput.text.toString(),newPasswordInput.text.toString(), confirmPasswordInput.text.toString())
        call.enqueue(object : retrofit2.Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: retrofit2.Response<ResponseBody>) {
                val responseBody = response.body()?.string()
                val errorBody = response.errorBody()?.string()
                val responseString = responseBody ?: errorBody

                Log.e("newPass", "Code: ${response.code()} | Body: ${responseString}")

                val jsonObj = org.json.JSONObject(responseString);
                val msg = jsonObj.getString("message")
                if(response.isSuccessful){
                    ShowToast.showMessage(this@ForgotPasswordActivity, msg)

                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        val intent = Intent(this@ForgotPasswordActivity, AppLoginActivity::class.java)
                        startActivity(intent)
                        finish()
                    }, 3000)

                } else {
                    ShowToast.showMessage(this@ForgotPasswordActivity, msg)
                }
            }
            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e("RetrofitError", "onFailure: ${t.localizedMessage}", t)
                ShowToast.showMessage(this@ForgotPasswordActivity, "Failed to connect to the server. Please check your internet connection.")
            }
        })
    }

}

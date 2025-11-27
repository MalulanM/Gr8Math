package com.example.gr8math   // <-- match your real package!

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import com.google.android.material.textfield.TextInputLayout
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import com.example.gr8math.api.ConnectURL
import com.example.gr8math.dataObject.LoginUser
import com.example.gr8math.utils.ShowToast
import com.example.gr8math.utils.UIUtils
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class AppLoginActivity : AppCompatActivity() {
    lateinit var btnLogin: Button
    lateinit var etEmail : TextView
    lateinit var etPassword : TextView
    lateinit var tilPassword: TextInputLayout
    lateinit var tilEmail: TextInputLayout
    lateinit var loadingLayout : View

    lateinit var loadingProgress : View

    lateinit var loadingText : TextView

    //for teacher initial login
    lateinit var newPasswordInput: EditText
    lateinit var confirmPasswordInput: EditText
    lateinit var savePassBtn : Button
    lateinit var tillNewPassLayout : TextInputLayout
    lateinit var tillConfirmPassLayout : TextInputLayout



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

                    Log.e("LoginResponse", "${responseString}")

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
                        val firstName = user.optString("first_name")
                        val isfirstLogin = user.optBoolean("first_login")
                        val pref = getSharedPreferences("user_session", MODE_PRIVATE)
//                        pref.edit().putString("auth_token", token).apply()
                        ConnectURL.init(this@AppLoginActivity)

                        Log.e("isFirst", firstName)
                        if(isfirstLogin == true && role == "student"){
                            showUserAgreement(role, id)
                        } else if (isfirstLogin == true && role == "teacher"){
                            showForgotPasswordActivity(etEmail.text.toString(), id, role)
                        } else {

                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                val nextIntent = when (role) {
                                    "student" -> Intent(
                                        this@AppLoginActivity,
                                        StudentClassManagerActivity::class.java
                                    )

                                    "teacher" -> Intent(
                                        this@AppLoginActivity,
                                        TeacherClassManagerActivity::class.java
                                    )

                                    else -> Intent(
                                        this@AppLoginActivity,
                                        AccountManagementActivity::class.java
                                    )
                                }
                                nextIntent.putExtra("toast_msg", msg)
                                nextIntent.putExtra("id", id)
                                nextIntent.putExtra("role", role)
                                nextIntent.putExtra("name", firstName)

                                UIUtils.showLoading(
                                    loadingLayout,
                                    loadingProgress,
                                    loadingText,
                                    false
                                )
                                startActivity(nextIntent)
                                finish()
                            }, 3000)
                        }

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

    private fun showForgotPasswordActivity(email : String, id : Int, role : String){
        setContentView(R.layout.forgot_password_activity_one)
        init2()
        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }

        savePassBtn.isEnabled = true

        newPasswordInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                UIUtils.errorDisplay(this@AppLoginActivity, tillNewPassLayout, newPasswordInput, false,"Please enter a password")
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        confirmPasswordInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                UIUtils.errorDisplay(this@AppLoginActivity, tillConfirmPassLayout, confirmPasswordInput, false,"Please enter a password")
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        savePassBtn.setOnClickListener {
            updatePassword(email, id, role)
        }
    }

    fun isValidPassword(password: String): Boolean {
        val passwordPattern = Regex("^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[^A-Za-z\\d])[A-Za-z\\d[^A-Za-z\\d]]{8,16}\$")
        return passwordPattern.matches(password)
    }
    fun updatePassword(email: String, id: Int, role : String){

        if(newPasswordInput.text.toString().isEmpty() || confirmPasswordInput.text.toString().isEmpty()){
            UIUtils.errorDisplay(this@AppLoginActivity, tillNewPassLayout, newPasswordInput, false,"Please enter a password")
            UIUtils.errorDisplay(this@AppLoginActivity, tillConfirmPassLayout, confirmPasswordInput, false,"Please enter a password")
            return
        }


        if(newPasswordInput.text.toString() != confirmPasswordInput.text.toString()){
            ShowToast.showMessage(this@AppLoginActivity, "Passwords do not match")
            return
        }

        if(!isValidPassword(newPasswordInput.text.toString())){
            ShowToast.showMessage(this@AppLoginActivity, "Password Invalid")
            return
        }

        newPasswordInput.isEnabled = false
        confirmPasswordInput.isEnabled = false
        savePassBtn.isEnabled = false
        UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, true)

        val apiService = ConnectURL.api
        val call = apiService.savePass(email, null, newPasswordInput.text.toString(), confirmPasswordInput.text.toString())
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
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                        UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                        showUserAgreement(role, id);
                    }, 800)

                } else {
                    ShowToast.showMessage(this@AppLoginActivity, msg)
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
                ShowToast.showMessage(this@AppLoginActivity, "Failed to connect to the server. Please check your internet connection.")
            }
        })
    }

    private fun showUserAgreement(role : String, id : Int) {
        // Inflate the custom dialog layout
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_terms_and_conditions, null)

        // Find the views inside the custom dialog
        val chkBoxAgree = dialogView.findViewById<CheckBox>(R.id.cbTerms)
        val btnProceed =  dialogView.findViewById<Button>(R.id.btnProceed)


        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()


        btnProceed.isEnabled = false

        chkBoxAgree.setOnCheckedChangeListener { _, isChecked ->
            btnProceed.isEnabled = isChecked
        }

        btnProceed.setOnClickListener {
            UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, true)
            dialog.dismiss()
            updateStatus(id)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                val nextIntent = when(role) {
                    "student" -> Intent(this@AppLoginActivity, StudentClassManagerActivity::class.java)
                    "teacher" -> Intent(this@AppLoginActivity, TeacherClassManagerActivity::class.java)
                    else -> Intent(this@AppLoginActivity, AccountManagementActivity::class.java)
                }
                val msg = if (role == "teacher") {
                    "Password saved"
                } else {
                    "Login Successful"
                }
                nextIntent.putExtra("toast_msg", msg)
                nextIntent.putExtra("id", id)
                nextIntent.putExtra("role", role)

                UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                startActivity(nextIntent)
                finish()
            }, 3000)
            setResult(RESULT_OK)
        }
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialog.show()

    }

    private fun updateStatus(id:Int){
        val apiService = ConnectURL.api
        val call = apiService.updateStatus(id)
        call.enqueue(object : retrofit2.Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody?>, response: Response<ResponseBody?>) {
                val responseBody = response.body()?.string()
                val errorBody = response.errorBody()?.string()
                val responseString = responseBody ?: errorBody

                Log.e("updateStat", "Code: ${response.code()} | Body: ${responseString}")
            }

            override fun onFailure(call: Call<ResponseBody?>, t: Throwable) {

            }
        })
    }
}

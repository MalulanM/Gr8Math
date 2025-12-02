package com.example.gr8math  // <-- match your actual package

import android.os.Bundle
import android.widget.EditText
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.widget.ArrayAdapter
import android.widget.TextView
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import okhttp3.ResponseBody
import retrofit2.Call
import java.text.SimpleDateFormat
import java.util.*
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import com.example.gr8math.api.ConnectURL
import com.example.gr8math.dataObject.User
import com.example.gr8math.utils.ShowToast
import com.example.gr8math.utils.UIUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import retrofit2.Response


class RegisterActivity : AppCompatActivity() {
    lateinit  var genderField: MaterialAutoCompleteTextView
    lateinit  var date: EditText
    lateinit  var registerButton: Button

    lateinit var email: EditText
    lateinit var password: EditText

    lateinit var confirmPassword: EditText
    lateinit var firstName: EditText
    lateinit var lastName: EditText
    lateinit var LRN: EditText

    lateinit var MessageBox : TextView

    lateinit var nextButton :Button;

    lateinit var loadingLayout : View

    lateinit var loadingProgress : View

    lateinit var loadingText : TextView

    lateinit var tilEmail : TextInputLayout
    lateinit var tilFirstName : TextInputLayout
    lateinit var tilLastName : TextInputLayout
    lateinit var tilLRN : TextInputLayout
    lateinit var tilGender : TextInputLayout
    lateinit var tilBirthDate : TextInputLayout

    var firstNameText = ""
    var lastNameText = ""
    var emailText = ""
    var LRNText = ""
    var birthDateText = ""
    var genderText = ""

    private var newUserId: Int = -1
    var selectedGender: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        showRegisterInfo()
    }

    fun showRegisterInfo(){
        setContentView(R.layout.register_activity)
        Init()
        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }

        genderField.setOnItemClickListener { parent, view, position, id ->
            selectedGender = parent.getItemAtPosition(position).toString()
        }

        date.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePicker = DatePickerDialog(
                this,
                { _, selectedYear, selectedMonth, selectedDay ->

                    val selectedDate = Calendar.getInstance()
                    selectedDate.set(selectedYear, selectedMonth, selectedDay)


                    val formatter = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
                    val formattedDate = formatter.format(selectedDate.time)


                    date.setText(formattedDate)
                },
                year,
                month,
                day
            )
            datePicker.datePicker.maxDate = System.currentTimeMillis() -  24 * 60 * 60 * 1000
            datePicker.show()
        }


        nextButton.setOnClickListener {

            val fields = listOf(email, firstName, lastName, LRN, date, genderField)
            val tils = listOf(tilEmail, tilFirstName, tilLastName, tilLRN, tilBirthDate, tilGender)

            var hasError = false
            for (i in fields.indices) {
                val field = fields[i]
                val til = tils[i]
                UIUtils.errorDisplay(this@RegisterActivity,til, field, true, "Please enter the needed details.")
                if (field.text.toString().trim().isEmpty()) {
                    hasError = true
                }
            }

            if (hasError) return@setOnClickListener


            firstNameText = firstName.text.toString().trim()
            lastNameText = lastName.text.toString().trim()
            emailText = email.text.toString().trim()
            LRNText = LRN.text.toString().trim()
            birthDateText = date.text.toString().trim()
            genderText = selectedGender.trim()

            if (!LRNText.matches(Regex("^\\d{12}$"))) {
                UIUtils.errorDisplay(
                    this@RegisterActivity,
                    tilLRN,
                    LRN,
                    true,
                    "LRN requires at least 12 digits",
                    true
                )
                return@setOnClickListener
            }


            UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, true)
            ConnectURL.api.checkEmail(emailText, LRNText).enqueue(object : retrofit2.Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: retrofit2.Response<ResponseBody>) {

                    val responseString = response.body()?.string() ?: response.errorBody()?.string()
                    Log.e("OWSQJEWR", responseString.toString())
                    if (responseString == null || !responseString.trim().startsWith("{")) {
                        ShowToast.showMessage(
                            this@RegisterActivity,
                            "Server returned invalid response. Check API route."
                        )
                        UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                        return
                    }

                    val jsonObj = org.json.JSONObject(responseString)

                    val success = jsonObj.optBoolean("success")
                    val msg = jsonObj.optString("msg")

                    if (success) {

                        when {
                            msg.contains("Email", ignoreCase = true) -> {
                                UIUtils.errorDisplay(
                                    this@RegisterActivity,
                                    tilEmail,
                                    email,
                                    true,
                                    msg,
                                    true
                                )
                            }

                            msg.contains("LRN", ignoreCase = true) -> {
                                UIUtils.errorDisplay(
                                    this@RegisterActivity,
                                    tilLRN,
                                    LRN,
                                    true,
                                    msg,
                                    true
                                )
                            }
                        }

                        UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                        return
                    }


                    UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                    showPasswordRegistration()
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Log.e("RetrofitError", "onFailure: ${t.localizedMessage}", t)
                    ShowToast.showMessage(this@RegisterActivity, "Failed to connect to server. Check your internet connection.")
                    UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                }
            })

        }


    }



    fun showPasswordRegistration(){
        setContentView(R.layout.change_password_activity)
        Init2()
        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }

        val enableBtn = {
            registerButton.isEnabled = password.text.toString().isNotEmpty() && confirmPassword.text.toString().isNotEmpty()
        }

        password.setOnKeyListener { _, _, _ ->
            enableBtn()
            false
        }

        confirmPassword.setOnKeyListener { _, _, _ ->
            enableBtn()
            false
        }

        registerButton.setOnClickListener {
            UserRegister()
        }

    }

    fun Init() {
         email = findViewById<EditText>(R.id.email)
         firstName = findViewById<EditText>(R.id.firstName)
         lastName = findViewById<EditText>(R.id.lastName)
         LRN = findViewById<EditText>(R.id.LRN)
        date = findViewById<EditText>(R.id.etDob)
        genderField = findViewById<MaterialAutoCompleteTextView>(R.id.etGender)
        val items = listOf("Male", "Female")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, items)
        genderField.setAdapter(adapter)
        MessageBox = findViewById<TextView>(R.id.message)
        nextButton = findViewById(R.id.btnNext)
        tilEmail = findViewById(R.id.tilEmail)
        tilFirstName = findViewById(R.id.tilFirstName)
        tilLastName = findViewById(R.id.tilLastName)
        tilLRN = findViewById(R.id.tilLRN)
        tilGender = findViewById(R.id.tilGender)
        tilBirthDate = findViewById(R.id.tilBirthdate)
        loadingLayout =  findViewById<View>(R.id.loadingLayout)
        loadingProgress = findViewById<View>(R.id.loadingProgressBg)
        loadingText = findViewById<TextView>(R.id.loadingText)
    }

    fun Init2(){
        password = findViewById<EditText>(R.id.etNewPass)
        confirmPassword = findViewById<EditText>(R.id.etRePass)
        registerButton = findViewById<Button>(R.id.btnSave)
        loadingLayout =  findViewById<View>(R.id.loadingLayout)
        loadingProgress = findViewById<View>(R.id.loadingProgressBg)
        loadingText = findViewById<TextView>(R.id.loadingText)
    }

    fun isValidPassword(password: String): Boolean {
        val passwordPattern = Regex("^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[^A-Za-z\\d])[A-Za-z\\d[^A-Za-z\\d]]{8,16}\$")
        return passwordPattern.matches(password)
    }

    fun UserRegister() {
        val emailText = email.text.toString().trim()
        val passwordText = password.text.toString().trim()
        val confirmPassText = confirmPassword.text.toString().trim()
        val firstNameText = firstName.text.toString().trim()
        val lastNameText = lastName.text.toString().trim()
        val LRNText = LRN.text.toString().trim()
        val birthDateText = date.text.toString().trim()
        val genderText = selectedGender.trim()


        if(passwordText != confirmPassText){
            ShowToast.showMessage(this@RegisterActivity, "Passwords do not match")
          return
        }

        if(!isValidPassword(passwordText)){
             ShowToast.showMessage(this@RegisterActivity, "Password Invalid")
          return
        }

        val apiService = ConnectURL.api
        // API call
            val user = User(
                firstName = firstNameText,
                lastName = lastNameText,
                emailAdd = emailText,
                passwordHash = passwordText,
                passwordHashConfirmation = confirmPassText,
                gender = genderText,
                birthdate = birthDateText,
                LRN = LRNText
            )

        password.isEnabled = false
        confirmPassword.isEnabled = false
        UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, true)
        val call = apiService.registerUser(user)
        call.enqueue(object : retrofit2.Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: retrofit2.Response<ResponseBody>) {
                try {

                    val rawBody = response.body()?.string()
                    val rawError = response.errorBody()?.string()

                    Log.e(
                        "RegisterResponse",
                        "Code: ${response.code()} | Body: $rawBody | ErrorBody: $rawError"
                    )

                    // Use what was read above
                    val body = rawBody ?: rawError
                    val json = org.json.JSONObject(body)

                    val id = json.optInt("id", -1)
                    val isFirst = json.optBoolean("is_first", false)

                    newUserId = id

                    UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                    showUserAgreement(isFirst)

                } catch (e: Exception) {
                    Log.e("registerAcc", "Exception: ${e.message}", e)
                    ShowToast.showMessage(
                        this@RegisterActivity,
                        "An error occurred while handling the response."
                    )
                    UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                }
            }


            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e("RetrofitError", "onFailure: ${t.localizedMessage}", t)
                ShowToast.showMessage(this@RegisterActivity, "Failed to connect to server. Check your internet connection.")
                UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
            }
        })
    }


    // ----------------------------------------------------------
    // TERMS & CONDITIONS
    // ----------------------------------------------------------
    // ... inside RegisterActivity.kt

    // ----------------------------------------------------------
// TERMS & CONDITIONS AND PRIVACY POLICY
// ----------------------------------------------------------
    private fun showUserAgreement(isFirstTime: Boolean) {

        val dialogView = LayoutInflater.from(this)
            .inflate(R.layout.dialog_terms_and_conditions, null)

        val chkBoxAgree = dialogView.findViewById<CheckBox>(R.id.cbTerms)
        val btnProceed = dialogView.findViewById<Button>(R.id.btnProceed)

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        btnProceed.isEnabled = false

        // --- CODE TO MAKE "Terms and Conditions" AND "Privacy Policy" CLICKABLE ---

        val fullText = chkBoxAgree.text.toString()
        val spannable = android.text.SpannableString(fullText)

        // Define the style for the hyperlinks
        val setLinkStyle = { ds: android.text.TextPaint ->
            ds.isUnderlineText = true
            ds.color = resources.getColor(R.color.colorMatisse, theme) // Use your link color
        }


        // 1. TERMS AND CONDITIONS Link
        val termsLinkText = "Terms and Conditions"
        val termsStartIndex = fullText.indexOf(termsLinkText)
        val termsEndIndex = termsStartIndex + termsLinkText.length

        if (termsStartIndex != -1) {
            val termsClickableSpan = object : android.text.style.ClickableSpan() {
                override fun onClick(widget: View) {
                    val intent = Intent(this@RegisterActivity, TermsAndConditionsActivity::class.java)
                    startActivity(intent)
                }
                override fun updateDrawState(ds: android.text.TextPaint) { setLinkStyle(ds) }
            }

            spannable.setSpan(
                termsClickableSpan,
                termsStartIndex,
                termsEndIndex,
                android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }


        // 2. PRIVACY POLICY Link
        val privacyLinkText = "Privacy Policy"
        val privacyStartIndex = fullText.indexOf(privacyLinkText)
        val privacyEndIndex = privacyStartIndex + privacyLinkText.length

        if (privacyStartIndex != -1) {
            val privacyClickableSpan = object : android.text.style.ClickableSpan() {
                override fun onClick(widget: View) {
                    // Launch the new PrivacyActivity
                    val intent = Intent(this@RegisterActivity, PrivacyPolicyActivity::class.java)
                    startActivity(intent)
                }
                override fun updateDrawState(ds: android.text.TextPaint) { setLinkStyle(ds) }
            }

            spannable.setSpan(
                privacyClickableSpan,
                privacyStartIndex,
                privacyEndIndex,
                android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        // Set the modified text and enable link movement
        chkBoxAgree.text = spannable
        chkBoxAgree.movementMethod = android.text.method.LinkMovementMethod.getInstance()

        // --- END CLICKABLE CODE ---


        // Checkbox listener for enabling the button
        chkBoxAgree.setOnCheckedChangeListener { _, checked ->
            btnProceed.isEnabled = checked
        }

        // Button click listener to proceed
        btnProceed.setOnClickListener {
            // ... (rest of your button click logic) ...
            dialog.dismiss()
            UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, true)

            updateStatus(newUserId)

            Handler(mainLooper).postDelayed({
                UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)

                val nextIntent = Intent(
                    this@RegisterActivity,
                    AppLoginActivity::class.java
                )
                nextIntent.putExtra("toast_msg", "Registered successfully")
                startActivity(nextIntent)
                setResult(RESULT_OK)
                finish()

            }, 1000)
        }

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }
    // ----------------------------------------------------------
    // UPDATE STATUS API
    // ----------------------------------------------------------
    private fun updateStatus(id: Int) {
        ConnectURL.api.updateStatus(id).enqueue(object : retrofit2.Callback<ResponseBody> {

            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                val body = response.body()?.string()
                val error = response.errorBody()?.string()
                val result = body ?: error
                Log.e("updateStat", "Code: ${response.code()} | Body: $result")
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e("updateStat", "Failed: ${t.message}")
            }
        })
    }


}

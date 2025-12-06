package com.example.gr8math

import android.os.Bundle
import android.widget.EditText
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
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
import com.example.gr8math.utils.UIUtils
import com.google.android.material.textfield.TextInputLayout
import com.example.gr8math.api.ConnectURL
import com.example.gr8math.utils.ShowToast
import com.example.gr8math.dataObject.User
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import retrofit2.Response
import android.os.Handler

class AddAccountActivity : AppCompatActivity() {

    // ORIGINAL UI
    lateinit var genderField: MaterialAutoCompleteTextView
    lateinit var date: EditText
    lateinit var email: EditText
    lateinit var firstName: EditText
    lateinit var lastName: EditText
    lateinit var teachingPos: MaterialAutoCompleteTextView
    lateinit var MessageBox: TextView
    lateinit var addButton: Button

    lateinit var loadingLayout: View
    lateinit var loadingProgress: View
    lateinit var loadingText: TextView

    lateinit var tilEmail: TextInputLayout
    lateinit var tilFirstName: TextInputLayout
    lateinit var tilLastName: TextInputLayout
    lateinit var tilTeachingPos: TextInputLayout
    lateinit var tilGender: TextInputLayout
    lateinit var tilBirthDate: TextInputLayout

    // PASSWORD SCREEN UI
    lateinit var password: EditText
    lateinit var confirmPassword: EditText
    lateinit var registerButton: Button

    // Temp store user info
    var selectedGender = ""
    var selectedTeachingPos = ""

    var firstNameTemp = ""
    var lastNameTemp = ""
    var emailTemp = ""
    var birthDateTemp = ""

    private var newUserId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showRegisterInfo()
    }

    // ----------------------------------------------------------
    // FIRST SCREEN
    // ----------------------------------------------------------
    fun showRegisterInfo() {
        setContentView(R.layout.activity_teacher_register)
        Init()

        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }

        genderField.setOnItemClickListener { parent, _, position, _ ->
            selectedGender = parent.getItemAtPosition(position).toString()
        }

        teachingPos.setOnItemClickListener { parent, _, position, _ ->
            selectedTeachingPos = parent.getItemAtPosition(position).toString()
        }

        date.setOnClickListener {
            val calendar = Calendar.getInstance()

            val datePicker = DatePickerDialog(
                this,
                { _, y, m, d ->
                    val selectedDate = Calendar.getInstance()
                    selectedDate.set(y, m, d)

                    val formatted = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
                    date.setText(formatted.format(selectedDate.time))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )

            datePicker.datePicker.maxDate = System.currentTimeMillis() - 86400000
            datePicker.show()
        }

        addButton.setOnClickListener {

            val fields = listOf(email, firstName, lastName, teachingPos, date, genderField)
            val tils = listOf(tilEmail, tilFirstName, tilLastName, tilTeachingPos, tilBirthDate, tilGender)

            var hasError = false

            for (i in fields.indices) {
                UIUtils.errorDisplay(
                    this,
                    tils[i],
                    fields[i],
                    true,
                    "Please enter the needed details"
                )
                if (fields[i].text.toString().trim().isEmpty()) hasError = true
            }

            if (hasError) return@setOnClickListener

            UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, true)

            val emailText = email.text.toString().trim()

            ConnectURL.api.checkEmail(emailText).enqueue(object : retrofit2.Callback<ResponseBody> {

                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    val responseString = response.body()?.string()
                        ?: response.errorBody()?.string()

                    if (responseString == null || !responseString.trim().startsWith("{")) {
                        ShowToast.showMessage(this@AddAccountActivity, "Server returned invalid response.")
                        UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                        return
                    }

                    val jsonObj = org.json.JSONObject(responseString)
                    val success = jsonObj.optBoolean("success")
                    val msg = jsonObj.optString("msg")

                    if (success && msg.contains("Email", ignoreCase = true)) {

                        UIUtils.errorDisplay(
                            this@AddAccountActivity,
                            tilEmail,
                            email,
                            true,
                            msg,
                            true
                        )

                        UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                        return
                    }

                    // Save temp
                    firstNameTemp = firstName.text.toString().trim()
                    lastNameTemp = lastName.text.toString().trim()
                    emailTemp = email.text.toString().trim()
                    birthDateTemp = date.text.toString().trim()

                    UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                    showPasswordRegistration()
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    ShowToast.showMessage(this@AddAccountActivity, "Failed to connect to server.")
                    UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                }
            })
        }
    }

    fun Init() {
        email = findViewById(R.id.email)
        firstName = findViewById(R.id.firstName)
        lastName = findViewById(R.id.lastName)
        teachingPos = findViewById(R.id.etTeachingPos)
        date = findViewById(R.id.etDob)
        genderField = findViewById(R.id.etGender)

        genderField.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_list_item_1, listOf("Male", "Female"))
        )

        teachingPos.setAdapter(
            ArrayAdapter(
                this,
                android.R.layout.simple_list_item_1,
                listOf(
                    "Teacher I", "Teacher II", "Teacher III", "Teacher IV",
                    "Teacher V", "Teacher VI", "Teacher VII",
                    "Master I", "Master II", "Master III", "Master IV", "Master V"
                )
            )
        )

        MessageBox = findViewById(R.id.message)
        addButton = findViewById(R.id.btnNext)

        tilEmail = findViewById(R.id.tilEmail)
        tilFirstName = findViewById(R.id.tilFirstName)
        tilLastName = findViewById(R.id.tilLastName)
        tilTeachingPos = findViewById(R.id.tilTeachingPos)
        tilGender = findViewById(R.id.tilGender)
        tilBirthDate = findViewById(R.id.tilBirthdate)

        loadingLayout = findViewById(R.id.loadingLayout)
        loadingProgress = findViewById(R.id.loadingProgressBg)
        loadingText = findViewById(R.id.loadingText)
    }

    // ----------------------------------------------------------
    // PASSWORD SCREEN
    // ----------------------------------------------------------
    fun showPasswordRegistration() {
        setContentView(R.layout.change_password_activity)
        Init2()

        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }

        registerButton.isEnabled = false

        val enableBtn = {
            registerButton.isEnabled =
                password.text.toString().isNotEmpty() &&
                        confirmPassword.text.toString().isNotEmpty()
        }

        password.setOnKeyListener { _, _, _ -> enableBtn(); false }
        confirmPassword.setOnKeyListener { _, _, _ -> enableBtn(); false }

        registerButton.setOnClickListener { registerFinal() }
    }

    fun Init2() {
        password = findViewById(R.id.etNewPass)
        confirmPassword = findViewById(R.id.etRePass)
        registerButton = findViewById(R.id.btnSave)

        loadingLayout = findViewById(R.id.loadingLayout)
        loadingProgress = findViewById(R.id.loadingProgressBg)
        loadingText = findViewById(R.id.loadingText)
    }

    fun isValidPassword(password: String): Boolean {
        val pattern = Regex("^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,16}$")
        return pattern.matches(password)
    }

    // ----------------------------------------------------------
    // FINAL SUBMISSION
    // ----------------------------------------------------------
    fun registerFinal() {

        if (password.text.toString() != confirmPassword.text.toString()) {
            ShowToast.showMessage(this, "Passwords do not match")
            return
        }

        if (!isValidPassword(password.text.toString())) {
            ShowToast.showMessage(this, "Password Invalid")
            return
        }

        val user = User(
            firstName = firstNameTemp,
            lastName = lastNameTemp,
            emailAdd = emailTemp,
            passwordHash = password.text.toString().trim(),
            passwordHashConfirmation = confirmPassword.text.toString().trim(),
            gender = selectedGender,
            birthdate = birthDateTemp,
            teacherPosition = selectedTeachingPos
        )

        registerButton.isEnabled = false
        UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, true)

        ConnectURL.api.registerAdmin(user).enqueue(object : retrofit2.Callback<ResponseBody> {

            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                val body = response.body()?.string() ?: response.errorBody()?.string()
                val json = org.json.JSONObject(body)
                val id = json.optInt("id", -1)
                val isFirst = json.optBoolean("is_first", false)

                newUserId = id

                UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                showUserAgreement(isFirst)
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                ShowToast.showMessage(this@AddAccountActivity, "Failed to connect to server.")
                UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                registerButton.isEnabled = true
            }
        })
    }

    // ----------------------------------------------------------
    // TERMS & CONDITIONS
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
                    val intent = Intent(this@AddAccountActivity, TermsAndConditionsActivity::class.java)
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
                    val intent = Intent(this@AddAccountActivity, PrivacyPolicyActivity::class.java)
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
                    this@AddAccountActivity,
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

            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {

            }
        })
    }
}

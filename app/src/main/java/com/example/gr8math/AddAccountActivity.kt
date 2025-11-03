package com.example.gr8math  // <-- Make sure this matches your actual package

import android.os.Bundle
import android.widget.EditText
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import android.app.DatePickerDialog
import android.content.Intent
import android.widget.ArrayAdapter
import android.widget.TextView
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.sql.Date
import java.text.SimpleDateFormat
import java.util.*
import android.widget.Toast
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import com.example.gr8math.utils.UIUtils // Make sure this import path is correct
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.*
// Make sure you have these classes (or remove the lines if you don't use them)
import com.example.gr8math.AppLoginActivity
import com.example.gr8math.ConnectURL
import com.example.gr8math.ShowToast
import com.example.gr8math.User


class AddAccountActivity : AppCompatActivity() {
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

    lateinit var addButton :Button;

    lateinit var loadingLayout : View

    lateinit var loadingProgress : View

    lateinit var loadingText : TextView

    lateinit var tilEmail : TextInputLayout
    lateinit var tilFirstName : TextInputLayout
    lateinit var tilLastName : TextInputLayout
    lateinit var tilLRN : TextInputLayout
    lateinit var tilGender : TextInputLayout
    lateinit var tilBirthDate : TextInputLayout



    var selectedGender: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        showRegisterInfo()
    }

    fun showRegisterInfo(){
        // UPDATED: Using your new layout file
        setContentView(R.layout.add_account_activity)
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

        addButton.setOnClickListener {

            val fields = listOf(email, firstName, lastName, LRN, date, genderField)
            val tils = listOf(tilEmail, tilFirstName, tilLastName, tilLRN, tilBirthDate, tilGender)

            var hasError = false
            for (i in fields.indices) {
                val field = fields[i]
                val til = tils[i]
                // Assuming UIUtils is available
                UIUtils.errorDisplay(this@AddAccountActivity,til, field, true, "Please enter the needed details")
                if (field.text.toString().trim().isEmpty()) {
                    hasError = true
                }
            }

            if (!hasError) {
                showPasswordRegistration()
            }

        }

    }

    fun showPasswordRegistration(){
        // Assuming you have a password layout named this (from your RegisterActivity)
        setContentView(R.layout.change_password_activity)
        Init2()
        // Find the toolbar on this new layout
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        // Set its title (optional)
        toolbar.title = "Create Password"
        // Set its back button to go back to the info screen
        toolbar.setNavigationOnClickListener { showRegisterInfo() }


        val enableBtn = {
            registerButton.isEnabled = password.text.toString().isNotEmpty() && confirmPassword.text.toString().isNotEmpty()
        }

        val keyListener = { _: Any, _:Any, _:Any ->
            enableBtn()
            false
        }

        password.setOnKeyListener(keyListener)
        confirmPassword.setOnKeyListener(keyListener)

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
        addButton = findViewById(R.id.btnAdd)
        tilEmail = findViewById(R.id.tilEmail)
        tilFirstName = findViewById(R.id.tilFirstName)
        tilLastName = findViewById(R.id.tilLastName)
        tilLRN = findViewById(R.id.tilLRN)
        tilGender = findViewById(R.id.tilGender)
        tilBirthDate = findViewById(R.id.tilBirthdate)
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
        // Assuming your password requirements are the same
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
            ShowToast.showMessage(this@AddAccountActivity, "Passwords do not match")
            return
        }

        if(!isValidPassword(passwordText)){
            ShowToast.showMessage(this@AddAccountActivity, "Password Invalid")
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
            roles = "Student", // You might want to change this role
            LRN = LRNText
        )

        password.isEnabled = false
        confirmPassword.isEnabled = false
        UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, true)
        val call = apiService.registerUser(user) // Assuming you use the same API endpoint
        call.enqueue(object : retrofit2.Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: retrofit2.Response<ResponseBody>) {
                try {
                    Log.e(
                        "AddAccountResponse", // Changed log tag
                        "Code: ${response.code()} | Body: ${
                            response.body()?.string()
                        } | ErrorBody: ${response.errorBody()?.string()}"
                    )
                    if (response.isSuccessful) {
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({

                            // UPDATED: Show a toast and finish() instead of going to Login
                            ShowToast.showMessage(this@AddAccountActivity, "Account successfully created.")
                            UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                            finish() // Closes this page and returns to Account Management
                        }, 800)
                    } else {
                        ShowToast.showMessage(this@AddAccountActivity, "An error occurred while handling the response.")

                        UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                    }
                } catch (e: Exception) {
                    Log.e("addAccount", "Exception: ${e.message}", e)
                    ShowToast.showMessage(this@AddAccountActivity, "An error occurred while handling the response.")
                    UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                }
            }


            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e("RetrofitError", "onFailure: ${t.localizedMessage}", t)
                ShowToast.showMessage(this@AddAccountActivity, "Failed to connect to server. Check your internet connection.")
                UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
            }
        })
    }
}


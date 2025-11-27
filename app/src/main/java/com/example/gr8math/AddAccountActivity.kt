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
import java.text.SimpleDateFormat
import java.util.*
import android.util.Log
import android.view.View
import com.example.gr8math.utils.UIUtils // Make sure this import path is correct
import com.google.android.material.textfield.TextInputLayout
// Make sure you have these classes (or remove the lines if you don't use them)
import com.example.gr8math.api.ConnectURL
import com.example.gr8math.utils.ShowToast
import com.example.gr8math.dataObject.User


class AddAccountActivity : AppCompatActivity() {
    lateinit  var genderField: MaterialAutoCompleteTextView
    lateinit  var date: EditText

    lateinit var email: EditText
    lateinit var firstName: EditText
    lateinit var lastName: EditText

    lateinit var teachingPos : MaterialAutoCompleteTextView

    lateinit var MessageBox : TextView

    lateinit var addButton :Button;

    lateinit var loadingLayout : View

    lateinit var loadingProgress : View

    lateinit var loadingText : TextView

    lateinit var tilEmail : TextInputLayout
    lateinit var tilFirstName : TextInputLayout
    lateinit var tilLastName : TextInputLayout
    lateinit var tilTeachingPos : TextInputLayout
    lateinit var tilGender : TextInputLayout
    lateinit var tilBirthDate : TextInputLayout



    var selectedGender: String = ""
    var selectedTeachingPos: String = ""


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

        teachingPos.setOnItemClickListener { parent, view, position, id ->
            selectedTeachingPos = parent.getItemAtPosition(position).toString()
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

            val fields = listOf(email, firstName, lastName, teachingPos, date, genderField)
            val tils = listOf(tilEmail, tilFirstName, tilLastName, tilTeachingPos, tilBirthDate, tilGender)

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
                UserRegister()
            }

        }

    }


    fun Init() {
        email = findViewById<EditText>(R.id.email)
        firstName = findViewById<EditText>(R.id.firstName)
        lastName = findViewById<EditText>(R.id.lastName)
        teachingPos = findViewById<MaterialAutoCompleteTextView>(R.id.etTeachingPos)
        date = findViewById<EditText>(R.id.etDob)
        genderField = findViewById<MaterialAutoCompleteTextView>(R.id.etGender)
        val items = listOf("Male", "Female")
        val itemsTeachingPosition = listOf("Teacher I", "Teacher II","Teacher III", "Teacher IV", "Teacher V", "Teacher VI", "Teacher VII","Master I", "Master II","Master III", "Master IV", "Master V")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, items)
        val adapterTeachingPosition = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, itemsTeachingPosition)
        genderField.setAdapter(adapter)
        teachingPos.setAdapter(adapterTeachingPosition)
        MessageBox = findViewById<TextView>(R.id.message)
        addButton = findViewById(R.id.btnAdd)
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


    fun UserRegister() {
        val emailText = email.text.toString().trim()
        val firstNameText = firstName.text.toString().trim()
        val lastNameText = lastName.text.toString().trim()
        val teacherPosition = selectedTeachingPos.trim()
        val birthDateText = date.text.toString().trim()
        val genderText = selectedGender.trim()

        val apiService = ConnectURL.api
        // API call
        val user = User(
            firstName = firstNameText,
            lastName = lastNameText,
            emailAdd = emailText,
            gender = genderText,
            birthdate = birthDateText,
            teacherPosition = teacherPosition
        )

       addButton.isEnabled =  false
        UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, true)
        val call = apiService.registerAdmin(user) // Assuming you use the same API endpoint
        call.enqueue(object : retrofit2.Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: retrofit2.Response<ResponseBody>) {
                val responseString = response.body()?.string()?:response.errorBody()?.string()
                val jsonObj = org.json.JSONObject(responseString)
                val message = jsonObj.getString("message")
                try {
                    Log.e(
                        "AddAccountResponse", // Changed log tag
                        "Code: ${response.code()} | Body: ${
                            response.body()?.string()
                        } | ErrorBody: ${response.errorBody()?.string()}"
                    )
                    if (response.isSuccessful) {
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            val intent = Intent(this@AddAccountActivity, AccountManagementActivity::class.java)
                            intent.putExtra("toast_msg", message)
                            UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                            startActivity(intent)
                            finish() // Closes this page and returns to Account Management
                        }, 800)
                    } else {
                        ShowToast.showMessage(this@AddAccountActivity, "${message}")
                        UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                        addButton.isEnabled = true
                    }
                } catch (e: Exception) {
                    Log.e("addAccount", "Exception: ${e.message}", e)
                    ShowToast.showMessage(this@AddAccountActivity, "An error occurred while handling the response.")
                    UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                   addButton.isEnabled = true
                }
            }


            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e("RetrofitError", "onFailure: ${t.localizedMessage}", t)
                ShowToast.showMessage(this@AddAccountActivity, "Failed to connect to server. Check your internet connection.")
                UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
               addButton.isEnabled = true
            }
        })
    }
}


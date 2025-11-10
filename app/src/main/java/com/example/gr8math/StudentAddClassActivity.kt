package com.example.gr8math // Make sure this matches your package name

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.gr8math.api.ConnectURL
import com.example.gr8math.utils.UIUtils
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import okhttp3.ResponseBody

class StudentAddClassActivity : AppCompatActivity() {
    private lateinit var etClassCode : TextInputEditText
    private lateinit var tilClassCode : TextInputLayout

    private lateinit var joinButton: Button



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_add_class)
        val id = intent.getIntExtra("id",0)
        // Find views
        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        joinButton = findViewById(R.id.btnJoinClass)
        etClassCode = findViewById(R.id.etClassCode)
        tilClassCode = findViewById(R.id.tilClassCode)


        // 1. Make the toolbar's back button work
        toolbar.setNavigationOnClickListener {
            setResult(RESULT_OK)
            finish() // Closes this page and goes back
        }

        // 2. Set click listener for the "Join Class" button
        joinButton.setOnClickListener {
            if(etClassCode.text.toString().trim().isNotEmpty()) {
                joinButton.isEnabled = false
                val intent = Intent()
                intent.putExtra("show_loading", true)
                setResult(RESULT_FIRST_USER, intent)
                joinClass(id)

            } else {
                joinButton.isEnabled = true
                UIUtils.errorDisplay(
                    this,
                    tilClassCode,
                    etClassCode,
                    true,
                    "Enter code before submitting"
                )
            }
        }



    }

    fun joinClass(id: Int) {
        val apiService = ConnectURL.api
        val call = apiService.joinClass(id, etClassCode.text.toString().trim())

        call.enqueue(object : retrofit2.Callback<ResponseBody> {
            override fun onResponse(call: retrofit2.Call<ResponseBody>, response: retrofit2.Response<ResponseBody>) {
                val responseString = response.body()?.string() ?: response.errorBody()?.string() ?: ""
                val jsonObject = org.json.JSONObject(responseString)
                val message = jsonObject.getString("message")
                if (response.isSuccessful) {
                    val intent =
                        Intent(
                        this@StudentAddClassActivity,
                        StudentClassManagerActivity::class.java
                    )
                    intent.putExtra("id", id)
                    intent.putExtra("toast_msg", message)
                    setResult(RESULT_OK)
                    finish()
                }
            }
            override fun onFailure(call: retrofit2.Call<ResponseBody>, t: Throwable) {

            }
        })
    }
}
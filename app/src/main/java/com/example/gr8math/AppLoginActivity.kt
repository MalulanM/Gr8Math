package com.example.gr8math   // <-- match your real package!

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.content.Intent

class AppLoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.app_login_activity)   // <-- must match your XML file name
        // inside AppLoginActivity after setContentView(...)
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnRegister).setOnClickListener {
            startActivity(android.content.Intent(this, RegisterActivity::class.java))
        }

        // "Forgot Password?" -> open ForgotPasswordActivity (UI only)
        findViewById<View>(R.id.tvForgot).setOnClickListener {
            startActivity(Intent(this@AppLoginActivity, ForgotPasswordActivity::class.java))
        }

    }

}

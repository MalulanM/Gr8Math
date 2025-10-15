package com.example.gr8math // <-- match your package

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.example.gr8math.R

class ForgotPasswordActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.forgot_password_activity)

        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }
    }
}

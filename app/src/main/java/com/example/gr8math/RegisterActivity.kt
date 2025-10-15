package com.example.gr8math  // <-- match your actual package

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar

class RegisterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.register_activity)

        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }
    }
}

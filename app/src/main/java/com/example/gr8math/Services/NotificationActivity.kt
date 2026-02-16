package com.example.gr8math

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView

class NotificationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification)

        val title = intent.getStringExtra("title") ?: ""
        val message = intent.getStringExtra("message") ?: ""

        findViewById<TextView>(R.id.tvTitle).text = title
        findViewById<TextView>(R.id.tvMessage).text = message
    }
}

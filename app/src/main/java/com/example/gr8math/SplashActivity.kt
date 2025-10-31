package com.example.gr8math

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Delay transition to the login screen for 3 seconds
        Handler().postDelayed({
            // Start the LoginActivity
            val intent = Intent(this@SplashActivity, AppLoginActivity::class.java)
            startActivity(intent)

            // Apply the fade-out animation to the SplashActivity
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)

            // Close the SplashActivity
            finish()
        }, 3000)  // 3000 milliseconds = 3 seconds
    }
}

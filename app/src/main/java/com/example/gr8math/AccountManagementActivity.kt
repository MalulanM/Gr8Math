package com.example.gr8math // Make sure this package name matches yours

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

// Make sure these imports are here
import com.example.gr8math.ActiveAccountsListActivity
import com.example.gr8math.AccountRequestsListActivity
import com.example.gr8math.AddAccountActivity

class AccountManagementActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account_management)

        // --- 1. Find all the clickable Views ---

        // Find the "See More" TextViews
        val tvSeeMoreActive = findViewById<TextView>(R.id.tvSeeMoreActive)
        val tvSeeMoreRequests = findViewById<TextView>(R.id.tvSeeMoreRequests)

        // Find the "Add Account" Button
        val btnAddAccount = findViewById<Button>(R.id.btnAddAccount) // <-- NEW


        // --- 2. Set click listener for "See More - Active Accounts" ---
        tvSeeMoreActive.setOnClickListener {
            // FIX: Use "this@AccountManagementActivity" to specify the context
            val intent = Intent(this@AccountManagementActivity, ActiveAccountsListActivity::class.java)
            startActivity(intent)
        }

        // --- 3. Set click listener for "See More - Account Requests" ---
        tvSeeMoreRequests.setOnClickListener {
            // FIX: Use "this@AccountManagementActivity" to specify the context
            val intent = Intent(this@AccountManagementActivity, AccountRequestsListActivity::class.java)
            startActivity(intent)
        }

        // --- 4. Set click listener for "Add Account" Button --- (THIS IS THE NEW CODE)
        btnAddAccount.setOnClickListener {
            // Create an Intent to open your new AddAccountActivity
            val intent = Intent(this@AccountManagementActivity, AddAccountActivity::class.java)
            startActivity(intent)
        }

    }
}


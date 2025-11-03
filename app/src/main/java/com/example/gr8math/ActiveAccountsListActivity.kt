package com.example.gr8math // Make sure this package name matches yours

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar

// Make sure this import is here
import com.example.gr8math.AddAccountActivity

class ActiveAccountsListActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set the layout file for this activity
        setContentView(R.layout.activity_active_accounts_list)

        // --- 1. Find the Toolbar and set the back button ---
        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            finish() // This will close the page and go back
        }

        // --- 2. Find the "Add Account" Button ---
        val btnAddAccount = findViewById<Button>(R.id.btnAddAccount)

        // --- 3. Set the click listener for the "Add Account" Button ---
        btnAddAccount.setOnClickListener {
            // FIX: Use "this@ActiveAccountsListActivity" to specify the context
            val intent = Intent(this@ActiveAccountsListActivity, AddAccountActivity::class.java)
            startActivity(intent)
        }
    }
}


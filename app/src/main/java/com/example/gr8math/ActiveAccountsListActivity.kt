package com.example.gr8math // Make sure this package name matches yours

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.gr8math.api.ConnectURL
import com.google.android.material.appbar.MaterialToolbar

// Make sure this import is here
import retrofit2.Call
import retrofit2.Response

class ActiveAccountsListActivity : AppCompatActivity() {
    lateinit var ParentLayoutActive : LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_active_accounts_list)
        ParentLayoutActive = findViewById(R.id.activeAccountsContainer)
        inflateActive()

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

    private fun inflateActive() {
        val apiService = ConnectURL.api
        apiService.getActive().enqueue(object : retrofit2.Callback<AccountRequestResponse> {
            override fun onResponse(
                call: Call<AccountRequestResponse>,
                response: Response<AccountRequestResponse>
            ) {
                if (response.isSuccessful) {
                    val users = response.body()?.data ?: emptyList()
                    ParentLayoutActive.removeAllViews()
                    users.forEach { user ->
                        val itemView = layoutInflater.inflate(
                            R.layout.item_active_account,
                            ParentLayoutActive,
                            false
                        )
                        itemView.findViewById<TextView>(R.id.name).text =
                            "${user.first_name} ${user.last_name}"
                        itemView.findViewById<TextView>(R.id.role).text = user.roles
                        ParentLayoutActive.addView(itemView)
                    }
                }
            }

            override fun onFailure(call: Call<AccountRequestResponse>, t: Throwable) {
                   }
        })
    }
}


package com.example.gr8math

import android.app.Dialog // <--- Changed import
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window // <--- New Import
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.gr8math.api.ConnectURL
import com.example.gr8math.utils.ShowToast
import com.google.android.material.appbar.MaterialToolbar
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response

class AccountManagementActivity : AppCompatActivity() {

    lateinit var ParentLayoutReq: LinearLayout
    lateinit var ParentLayoutActive: LinearLayout

    lateinit var noRequest : TextView
    lateinit var seeMoreReq : TextView
    lateinit var seeMoreActive : TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account_management)

        // --- Setup Toolbar Navigation ---
        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            showFacultyMenu()
        }

        val toastMsg = intent.getStringExtra("toast_msg")
        if(!toastMsg.isNullOrEmpty()){
            ShowToast.showMessage(this, toastMsg)
        }
        init()
        inflateRequest()
        inflateActive()

        // See More / Add Account Buttons
        findViewById<TextView>(R.id.tvSeeMoreActive).setOnClickListener {
            finish()
            startActivity(Intent(this, ActiveAccountsListActivity::class.java))
        }
        findViewById<TextView>(R.id.tvSeeMoreRequests).setOnClickListener {
            finish()
            startActivity(Intent(this, AccountRequestsListActivity::class.java))
        }
        findViewById<Button>(R.id.btnAddAccount).setOnClickListener {
            finish()
            startActivity(Intent(this, AddAccountActivity::class.java))
        }
    }

    // --- FIX: Use standard Dialog to force full-height sidebar ---
    private fun showFacultyMenu() {
        // 1. Create a standard Dialog (Not MaterialAlertDialogBuilder)
        val dialog = Dialog(this)

        // 2. Remove the default title bar
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        // 3. Inflate layout
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_faculty_menu, null)
        dialog.setContentView(dialogView)

        dialogView.findViewById<View>(R.id.btnAccountSettings).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<View>(R.id.btnTerms).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<View>(R.id.btnPrivacy).setOnClickListener {
            dialog.dismiss()
        }

        // 4. Configure the Window to stretch properly
        val window = dialog.window
        if (window != null) {
            // Make background transparent so we only see our layout
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            // Position at the Top-Left
            val params = window.attributes
            params.gravity = Gravity.START or Gravity.TOP

            // Set Width to 85% of screen, Height to MATCH_PARENT (Full Screen Vertical)
            params.width = (resources.displayMetrics.widthPixels * 0.85).toInt()
            params.height = WindowManager.LayoutParams.MATCH_PARENT

            window.attributes = params
        }

        dialog.show()
    }

    private fun init() {
        ParentLayoutReq = findViewById(R.id.account_requests_list)
        ParentLayoutActive = findViewById(R.id.active_accounts_list)
        noRequest = findViewById(R.id.tvNoRequest)
        seeMoreReq = findViewById(R.id.tvSeeMoreRequests)
        seeMoreActive = findViewById(R.id.tvSeeMoreActive)
    }

    private fun inflateRequest() {
        val apiService = ConnectURL.api
        apiService.getRequest().enqueue(object : retrofit2.Callback<AccountRequestResponse> {
            override fun onResponse(
                call: Call<AccountRequestResponse>,
                response: Response<AccountRequestResponse>
            ) {
                if (response.isSuccessful) {
                    val users = response.body()?.data ?: emptyList()
                    ParentLayoutReq.removeAllViews()
                    users.take(5).forEach { user ->
                        val itemView = layoutInflater.inflate(
                            R.layout.item_account_request,
                            ParentLayoutReq,
                            false
                        )

                        if(users.isNotEmpty()){
                            noRequest.visibility = View.GONE
                            seeMoreReq.visibility = View.VISIBLE
                        } else{
                            noRequest.visibility = View.VISIBLE
                            seeMoreReq.visibility = View.GONE
                        }
                        itemView.findViewById<TextView>(R.id.name).text =
                            "${user.first_name} ${user.last_name}"
                        itemView.findViewById<TextView>(R.id.role).text = user.roles

                        val acceptBtn = itemView.findViewById<ImageButton>(R.id.acceptBtn)
                        val rejectBtn = itemView.findViewById<ImageButton>(R.id.rejectBtn)

                        acceptBtn.setOnClickListener { acceptRequest(user.id) }
                        rejectBtn.setOnClickListener { rejectRequest(user.id) }

                        ParentLayoutReq.addView(itemView)
                    }
                }
            }

            override fun onFailure(call: Call<AccountRequestResponse>, t: Throwable) {
            }
        })
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
                    users.take(5).forEach { user ->
                        val itemView = layoutInflater.inflate(
                            R.layout.item_active_account,
                            ParentLayoutActive,
                            false
                        )

                        if(users.isNotEmpty()){
                            seeMoreActive.visibility = View.VISIBLE
                        } else{
                            seeMoreActive.visibility = View.GONE
                        }

                        itemView.findViewById<TextView>(R.id.name).text =
                            "${user.first_name} ${user.last_name}"
                        itemView.findViewById<TextView>(R.id.role).text = user.roles
                        ParentLayoutActive.addView(itemView)
                    }
                }
            }

            override fun onFailure(call: Call<AccountRequestResponse>, t: Throwable) {
                // Log error
            }
        })
    }

    private fun acceptRequest(userId: Int) {
        val apiService = ConnectURL.api
        apiService.acceptRequest(userId).enqueue(object : retrofit2.Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                val responseString = response.body()?.string()?:response.errorBody()?.string()
                val jsonObj = org.json.JSONObject(responseString)
                val message = jsonObj.getString("message")

                if (response.isSuccessful) {
                    ShowToast.showMessage(this@AccountManagementActivity, "${message}")
                    inflateRequest()
                } else {
                    ShowToast.showMessage(this@AccountManagementActivity, "${message}")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                ShowToast.showMessage(this@AccountManagementActivity, "Error")
            }
        })
    }

    private fun rejectRequest(userId: Int) {
        val apiService = ConnectURL.api
        apiService.rejectRequest(userId).enqueue(object : retrofit2.Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                val responseString = response.body()?.string()?:response.errorBody()?.string()
                val jsonObj = org.json.JSONObject(responseString)
                val message = jsonObj.getString("message")

                if (response.isSuccessful) {
                    ShowToast.showMessage(this@AccountManagementActivity, "${message}")
                    inflateRequest() // Refresh request list
                } else {
                    ShowToast.showMessage(this@AccountManagementActivity, "${message}")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                ShowToast.showMessage(this@AccountManagementActivity, "Error")
            }
        })
    }
}
package com.example.gr8math

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.gr8math.api.ConnectURL
import com.example.gr8math.utils.ShowToast
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response

class AccountManagementActivity : AppCompatActivity() {

    lateinit var ParentLayoutReq: LinearLayout
    lateinit var ParentLayoutActive: LinearLayout

    lateinit var noRequest : TextView
    lateinit var  seeMoreReq : TextView
    lateinit var seeMoreActive : TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account_management)
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
                } else {
//                    Log.e("API_ERROR", "Failed to load requests: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<AccountRequestResponse>, t: Throwable) {
//                Log.e("API_ERROR", "Request failed: ${t.localizedMessage}", t)
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
                Log.e("API_ERROR", "Active accounts failed: ${t.localizedMessage}", t)
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
//                    Log.e("ACCEPT", "Accept failed: ${response.errorBody()?.string()}")
                    ShowToast.showMessage(this@AccountManagementActivity, "${message}")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
//                Log.e("ACCEPT", "Accept failed: ${t.localizedMessage}", t)
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
//                    Log.e("REJECT", "Reject failed: ${response.errorBody()?.string()}")
                    ShowToast.showMessage(this@AccountManagementActivity, "${message}")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
//                Log.e("REJECT", "Reject failed: ${t.localizedMessage}", t)
                ShowToast.showMessage(this@AccountManagementActivity, "Error")
            }
        })
    }

}

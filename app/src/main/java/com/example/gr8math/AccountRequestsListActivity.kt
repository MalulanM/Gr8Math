package com.example.gr8math // Make sure this package name is correct

import android.os.Bundle
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

class AccountRequestsListActivity : AppCompatActivity() {

    lateinit var ParentLayoutReq: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account_requests_list)
        ParentLayoutReq = findViewById<LinearLayout>(R.id.accountRequestsContainer)
        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        inflateRequest()
        toolbar.setNavigationOnClickListener {
            finish() // Go back
        }
    }


    private fun inflateRequest() {
        val apiService = ConnectURL.publicApi
        apiService.getRequest().enqueue(object : retrofit2.Callback<AccountRequestResponse> {
            override fun onResponse(
                call: Call<AccountRequestResponse>,
                response: Response<AccountRequestResponse>
            ) {
                if (response.isSuccessful) {
                    val users = response.body()?.data ?: emptyList()
                    ParentLayoutReq.removeAllViews() // Clear old items
                    users.forEach { user ->
                        val itemView = layoutInflater.inflate(
                            R.layout.item_account_request,
                            ParentLayoutReq,
                            false
                        )
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

    private fun acceptRequest(userId: Int) {
        val apiService = ConnectURL.api
        apiService.acceptRequest(userId).enqueue(object : retrofit2.Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                val responseString = response.body()?.string()?:response.errorBody()?.string()
                val jsonObj = org.json.JSONObject(responseString)
                val message = jsonObj.getString("message")

                if (response.isSuccessful) {
                    ShowToast.showMessage(this@AccountRequestsListActivity, "${message}")
                    inflateRequest()
                } else {
//                    Log.e("ACCEPT", "Accept failed: ${response.errorBody()?.string()}")
                    ShowToast.showMessage(this@AccountRequestsListActivity, "${message}")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
//                Log.e("ACCEPT", "Accept failed: ${t.localizedMessage}", t)
                ShowToast.showMessage(this@AccountRequestsListActivity, "Error")
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
                    ShowToast.showMessage(this@AccountRequestsListActivity, "${message}")
                    inflateRequest() // Refresh request list
                } else {
//                    Log.e("REJECT", "Reject failed: ${response.errorBody()?.string()}")
                    ShowToast.showMessage(this@AccountRequestsListActivity, "${message}")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
//                Log.e("REJECT", "Reject failed: ${t.localizedMessage}", t)
                ShowToast.showMessage(this@AccountRequestsListActivity, "Error")
            }
        })
    }

}


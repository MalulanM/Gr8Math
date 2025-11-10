package com.example.gr8math // Make sure this matches your package name

import android.content.Intent // <-- IMPORT ADDED
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.example.gr8math.adapter.ClassAdapter
import com.example.gr8math.api.ConnectURL
import com.example.gr8math.utils.ShowToast
import com.example.gr8math.utils.UIUtils
import com.google.android.material.appbar.MaterialToolbar // <-- IMPORT ADDED
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Locale

class StudentClassManagerActivity : AppCompatActivity() {

    // Declare all the views
    private lateinit var mainToolbar: MaterialToolbar

    private lateinit var addClassesButton: Button
    private lateinit var addClassLauncher: ActivityResultLauncher<Intent>
    private lateinit var parentLayout : LinearLayout
    private lateinit var role: String

    lateinit var loadingLayout : View

    lateinit var loadingProgress : View

    lateinit var loadingText : TextView
    private var id: Int = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // This line connects your layout file
        setContentView(R.layout.activity_class_manager_student)
        id = intent.getIntExtra("id", 0)
        role = intent.getStringExtra("role")?:""

        if (!role.isNullOrEmpty() && id > 0) {
            inflateClasses(role, id)
        }
        val toastMsg = intent.getStringExtra("toast_msg")
        if(!toastMsg.isNullOrEmpty()){
            ShowToast.showMessage(this, toastMsg)
        }
        // Find all the views from the XML
        mainToolbar = findViewById(R.id.toolbar)
        addClassesButton = findViewById(R.id.btnAddClasses)
        parentLayout = findViewById(R.id.class_list_container)
        loadingLayout =  findViewById<View>(R.id.loadingLayout)
        loadingProgress = findViewById<View>(R.id.loadingProgressBg)
        loadingText = findViewById<TextView>(R.id.loadingText)
        // --- Set up Click Listeners ---

        // 1. Main toolbar's profile icon/back button
        mainToolbar.setNavigationOnClickListener {
            finish() // Closes the page
        }

        // 2. "Add Classes" button
        addClassesButton.setOnClickListener {
            // UPDATED: This now opens your new page!
            val intent = Intent(this@StudentClassManagerActivity, StudentAddClassActivity::class.java)
            intent.putExtra("id", id)
            addClassLauncher.launch(intent)
        }

        addClassLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->

            if (result.resultCode == RESULT_FIRST_USER) {
                UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, true)
            }

            // When dialog closes successfully, refresh
            if (result.resultCode == RESULT_OK) {
                val role = intent.getStringExtra("role") ?: return@registerForActivityResult
                val id = intent.getIntExtra("id", 0)
                UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, true)
                inflateClasses(role, id)
            }
        }



    }

    fun inflateClasses(role: String, id: Int) {
        val apiService = ConnectURL.api
        val call = apiService.getClasses(id, role)

        call.enqueue(object : retrofit2.Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                val responseString = response.body()?.string() ?: response.errorBody()?.string()

                if (responseString.isNullOrEmpty()) {
                    Log.e("API_ERROR", "Empty response")
                    return
                }

                try {
                    val jsonObj = org.json.JSONObject(responseString)
                    val success = jsonObj.optBoolean("success", false)
                    val message = jsonObj.optString("message", "No message")
                    val dataArray = jsonObj.optJSONArray("data") ?: org.json.JSONArray()

                    Log.e("API_RESPONSE", message)
//                    Toast.makeText(this@StudentClassManagerActivity, message, Toast.LENGTH_SHORT).show()

                    parentLayout.removeAllViews()

                    val inputFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    val outputFormat = SimpleDateFormat("h:mm a", Locale.getDefault())


                    for (i in 0 until dataArray.length()) {
                        val item = dataArray.getJSONObject(i)
                        val sectionName = item.optString("class_name", "Unknown Class")


                        val adviserName = item.optString("adviser_name")
                        val arrival = item.optString("arrival_time", "-")
                        val dismiss = item.optString("dismissal_time", "-")

                        val schedule = if (arrival != "-" && dismiss != "-") {
                            "${outputFormat.format(inputFormat.parse(arrival))}-${outputFormat.format(inputFormat.parse(dismiss))}"
                        } else "-"

                        val itemView = layoutInflater.inflate(
                            R.layout.item_student_class_card,
                            parentLayout,
                            false
                        )

                        itemView.findViewById<TextView>(R.id.tvSectionName).text = sectionName
                        itemView.findViewById<TextView>(R.id.tvSchedule).text = schedule
                        itemView.findViewById<TextView>(R.id.tvTeacherName).text = adviserName

                        parentLayout.addView(itemView)
                    }
                    UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)


                } catch (e: Exception) {
                    Log.e("API_ERROR", "Failed to parse response: ${e.localizedMessage}", e)
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e("API_ERROR", "Request failed: ${t.localizedMessage}", t)
                UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
            }
        })
    }
}
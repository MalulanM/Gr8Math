package com.example.gr8math // Make sure this matches your package name

import android.app.Dialog
import android.content.Intent // <-- IMPORT ADDED
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.view.WindowManager
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
import com.bumptech.glide.Glide
import com.example.gr8math.adapter.ClassAdapter
import com.example.gr8math.api.ConnectURL
import com.example.gr8math.dataObject.CurrentCourse
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

    lateinit var loadingLayout : View

    lateinit var loadingProgress : View

    lateinit var loadingText : TextView
    private lateinit var role: String
    private var id: Int = 0

    private var profilePic: String? = null
    private lateinit var name:String



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // This line connects your layout file
        setContentView(R.layout.activity_class_manager_student)
        loadingLayout =  findViewById<View>(R.id.loadingLayout)
        loadingProgress = findViewById<View>(R.id.loadingProgressBg)
        loadingText = findViewById<TextView>(R.id.loadingText)
        id = intent.getIntExtra("id", 0)
        role = intent.getStringExtra("role")?:""
        name = intent.getStringExtra("name")?:""
        profilePic = intent.getStringExtra("profilePic")
            ?.takeIf { it.isNotBlank() }
        Log.d("PROFILE_DEBUG", "Received ProfilePic URL: $profilePic")
        if (!role.isNullOrEmpty() && id > 0) {
            inflateClasses(role, id)
        }
        val toastMsg = intent.getStringExtra("toast_msg")
        if(!toastMsg.isNullOrEmpty()){
            ShowToast.showMessage(this, toastMsg)
        }

        CurrentCourse.userId = id
        // Find all the views from the XML
        mainToolbar = findViewById(R.id.toolbar)
        addClassesButton = findViewById(R.id.btnAddClasses)
        parentLayout = findViewById(R.id.class_list_container)

        // --- Set up Click Listeners ---

        // 1. Main toolbar's profile icon/back button
        mainToolbar.setNavigationOnClickListener {
            showFacultyMenu()
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

    private fun showFacultyMenu() {
        // 1. Create a standard Dialog (Not MaterialAlertDialogBuilder)
        val dialog = Dialog(this)

        // 2. Remove the default title bar
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        // 3. Inflate layout
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_faculty_menu, null)
        dialog.setContentView(dialogView)

        val ivProfile = dialog.findViewById<ImageView>(R.id.ivProfile)

        if (profilePic.isNullOrEmpty()) {
            ivProfile.setImageResource(R.drawable.ic_profile_default)
        } else if (profilePic!!.startsWith("http")) {
            Glide.with(this@StudentClassManagerActivity) // <--- Use qualified 'this'
                .load(profilePic)
                .placeholder(R.drawable.ic_profile_default)
                .error(R.drawable.ic_profile_default)
                .circleCrop()
                .into(ivProfile)
        } else {
            try {
                val decodedBytes = Base64.decode(profilePic, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                ivProfile.setImageBitmap(bitmap)
            } catch (e: Exception) {
                ivProfile.setImageResource(R.drawable.ic_profile_default)
            }
        }


        val name = dialog.findViewById<TextView>(R.id.tvGreeting)
        name.text = "Hi, ${this.name}!"


        dialogView.findViewById<View>(R.id.btnAccountSettings).setOnClickListener {
            startActivity(Intent(this@StudentClassManagerActivity, ProfileActivity::class.java))
            dialog.dismiss()
        }

        dialogView.findViewById<View>(R.id.btnTerms).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<View>(R.id.btnPrivacy).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<View>(R.id.btnLogout).setOnClickListener {
            dialog.dismiss()
            logout()
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

    private fun logout() {
        val preferences = getSharedPreferences("user_session", MODE_PRIVATE)
        preferences.edit().clear().apply() // Clears all session data (user ID, token, role, etc.)
        CurrentCourse.userId = 0
        val intent = Intent(this, AppLoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)
        finish()
    }
    fun inflateClasses(role: String, id: Int) {
        UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, true)
        val apiService = ConnectURL.api
        val call = apiService.getClasses(id, role)

        call.enqueue(object : retrofit2.Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                val responseString = response.body()?.string() ?: response.errorBody()?.string()

                if (responseString.isNullOrEmpty()) {
                    UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                    Log.e("API_ERROR", "Empty response")
                    return
                }

                try {
                    UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
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
                        val courseId = item.optInt("course_content_id", 0)
                        val schedule = if (arrival != "-" && dismiss != "-") {
                            "${outputFormat.format(inputFormat.parse(arrival))}-${outputFormat.format(inputFormat.parse(dismiss))}"
                        } else "-"

                        val itemView = layoutInflater.inflate(
                            R.layout.item_student_class_card,
                            parentLayout,
                            false
                        )

                        itemView.isClickable = true
                        itemView.setOnClickListener {
                            val intent = Intent(this@StudentClassManagerActivity,
                                StudentClassPageActivity::class.java)
                            intent.putExtra("id", id)
                            intent.putExtra("role", role)
                            intent.putExtra("courseId", courseId)
                            intent.putExtra("sectionName", sectionName)
                            startActivity(intent)
                        }

                        itemView.findViewById<TextView>(R.id.tvSectionName).text = sectionName
                        itemView.findViewById<TextView>(R.id.tvSchedule).text = schedule
                        itemView.findViewById<TextView>(R.id.tvTeacherName).text = adviserName

                        parentLayout.addView(itemView)
                    }
                    UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)


                } catch (e: Exception) {
                    UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                    Log.e("API_ERROR", "Failed to parse response: ${e.localizedMessage}", e)
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                Log.e("API_ERROR", "Request failed: ${t.localizedMessage}", t)
                UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
            }
        })
    }
}
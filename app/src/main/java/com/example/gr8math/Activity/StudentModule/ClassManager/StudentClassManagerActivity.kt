package com.example.gr8math.Activity.StudentModule.ClassManager

import android.app.Dialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Base64
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.gr8math.Activity.LoginAndRegister.AppLoginActivity
import com.example.gr8math.Activity.StudentModule.Profile.ProfileActivity
import com.example.gr8math.Activity.StudentModule.StudentAddClassActivity
import com.example.gr8math.Helper.NotificationMethods
import com.example.gr8math.Model.CurrentCourse
import com.example.gr8math.R
import com.example.gr8math.Utils.ShowToast
import com.example.gr8math.Utils.UIUtils
import com.example.gr8math.ViewModel.ClassManagerViewModel
import com.example.gr8math.ViewModel.ClassState
import com.google.android.material.appbar.MaterialToolbar

class StudentClassManagerActivity : AppCompatActivity() {

    private val viewModel: ClassManagerViewModel by viewModels()

    private lateinit var mainToolbar: MaterialToolbar
    private lateinit var addClassesButton: Button
    private lateinit var parentLayout: LinearLayout
    lateinit var loadingLayout: View
    lateinit var loadingProgress: View
    lateinit var loadingText: TextView

    private lateinit var addClassLauncher: ActivityResultLauncher<Intent>

    private var role: String = ""
    private var id: Int = 0
    private var profilePic: String? = null
    private var name: String = ""

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            NotificationMethods.registerToken(id, lifecycleScope)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_class_manager_student)

        initViews()
        initData()
        setupListeners()
        setupObservers()
        NotificationMethods.handleNotificationIntent(this, intent, id, role)


        if (id > 0) {
            viewModel.loadClasses(id, role)
            NotificationMethods.setupNotifications(this, id, requestPermissionLauncher, lifecycleScope)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        NotificationMethods.handleNotificationIntent(this, intent, id, role)
    }

    private fun initViews() {
        loadingLayout = findViewById(R.id.loadingLayout)
        loadingProgress = findViewById(R.id.loadingProgressBg)
        loadingText = findViewById(R.id.loadingText)
        mainToolbar = findViewById(R.id.toolbar)
        addClassesButton = findViewById(R.id.btnAddClasses)
        parentLayout = findViewById(R.id.class_list_container)
    }

    private fun initData() {
        id = intent.getIntExtra("id", 0)
        role = intent.getStringExtra("role") ?: "student"
        name = intent.getStringExtra("name") ?: ""
        profilePic = intent.getStringExtra("profilePic")?.takeIf { it.isNotBlank() }

        CurrentCourse.userId = id

        val toastMsg = intent.getStringExtra("toast_msg")
        if (!toastMsg.isNullOrEmpty()) ShowToast.showMessage(this, toastMsg)
    }

    private fun setupListeners() {
        mainToolbar.setNavigationOnClickListener { showFacultyMenu() }

        addClassesButton.setOnClickListener {
            val intent = Intent(this, StudentAddClassActivity::class.java)
            intent.putExtra("id", id)
            addClassLauncher.launch(intent)
        }

        addClassLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                // Refresh list on return
                viewModel.loadClasses(id, role, forceReload = true)
            }
        }
    }

    private fun setupObservers() {
        viewModel.classState.observe(this) { state ->
            when (state) {
                is ClassState.Loading -> {
                    UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, true)
                }
                is ClassState.Success -> {
                    UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                    parentLayout.removeAllViews()

                    state.data.forEach { item ->
                        val itemView = layoutInflater.inflate(R.layout.item_student_class_card, parentLayout, false)

                        itemView.findViewById<TextView>(R.id.tvSectionName).text = item.sectionName
                        itemView.findViewById<TextView>(R.id.tvSchedule).text = item.schedule
                        itemView.findViewById<TextView>(R.id.tvTeacherName).text = item.teacherName

                        itemView.setOnClickListener {
                            val intent = Intent(this, StudentClassPageActivity::class.java)
                            intent.putExtra("id", id)
                            intent.putExtra("role", role)
                            intent.putExtra("courseId", item.courseId)
                            intent.putExtra("sectionName", item.sectionName)
                            startActivity(intent)
                        }
                        parentLayout.addView(itemView)
                    }
                }
                is ClassState.Empty -> {
                    UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                    parentLayout.removeAllViews()
                    ShowToast.showMessage(this, "No classes found. Join one!")
                }
                is ClassState.Error -> {
                    UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                    ShowToast.showMessage(this, state.message)
                }
            }
        }
    }

    // --- Menu & Logout (Kept exactly as is) ---
    private fun showFacultyMenu() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_faculty_menu, null)
        dialog.setContentView(dialogView)

        val ivProfile = dialog.findViewById<ImageView>(R.id.ivProfile)
        if (profilePic.isNullOrEmpty()) {
            ivProfile.setImageResource(R.drawable.ic_profile_default)
        } else if (profilePic!!.startsWith("http")) {
            Glide.with(this).load(profilePic).placeholder(R.drawable.ic_profile_default).circleCrop().into(ivProfile)
        } else {
            try {
                val decodedBytes = Base64.decode(profilePic, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                ivProfile.setImageBitmap(bitmap)
            } catch (e: Exception) {
                ivProfile.setImageResource(R.drawable.ic_profile_default)
            }
        }

        dialog.findViewById<TextView>(R.id.tvGreeting).text = "Hi, ${this.name}!"

        dialogView.findViewById<View>(R.id.btnAccountSettings).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java)); dialog.dismiss()
        }
        dialogView.findViewById<View>(R.id.btnLogout).setOnClickListener {
            dialog.dismiss(); logout()
        }

        // Window config
        val window = dialog.window
        if (window != null) {
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val params = window.attributes
            params.gravity = Gravity.START or Gravity.TOP
            params.width = (resources.displayMetrics.widthPixels * 0.85).toInt()
            params.height = WindowManager.LayoutParams.MATCH_PARENT
            window.attributes = params
        }
        dialog.show()
    }

    private fun logout() {
        NotificationMethods.removeTokenOnLogout(id, lifecycleScope) {
            val preferences = getSharedPreferences("user_session", MODE_PRIVATE)
            preferences.edit().clear().apply()

            val intent = Intent(this, AppLoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}
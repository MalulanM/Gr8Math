package com.example.gr8math.Activity.TeacherModule.ClassManager

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.example.gr8math.Activity.LoginAndRegister.AppLoginActivity
import com.example.gr8math.Activity.PrivacyPolicyActivity
import com.example.gr8math.Activity.TeacherModule.Notification.TeacherNotificationsActivity
import com.example.gr8math.Activity.TeacherModule.Profile.TeacherProfileActivity
import com.example.gr8math.Activity.TermsAndConditionsActivity
import com.example.gr8math.Adapter.ClassAdapter
import com.example.gr8math.Data.Model.ClassUiModel
import com.example.gr8math.Helper.NotificationMethods
import com.example.gr8math.Model.CurrentCourse
import com.example.gr8math.Model.TeacherClass
import com.example.gr8math.R
import com.example.gr8math.Utils.NetworkUtils
import com.example.gr8math.Utils.ShowToast
import com.example.gr8math.Utils.UIUtils
import com.example.gr8math.ViewModel.ClassManagerViewModel
import com.example.gr8math.ViewModel.ClassState
import com.example.gr8math.ViewModel.LogoutState
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class TeacherClassManagerActivity : AppCompatActivity() {

    private val viewModel: ClassManagerViewModel by viewModels()

    // Views
    private lateinit var defaultView: ConstraintLayout
    private lateinit var searchView: ConstraintLayout
    private lateinit var mainToolbar: MaterialToolbar
    private lateinit var tilSearch: TextInputLayout
    private lateinit var searchIcon: ImageView
    private lateinit var etSearch: TextInputEditText
    private lateinit var addClassesButton: Button
    private lateinit var llPastSearches: LinearLayout
    private lateinit var tvNoResults: TextView
    private lateinit var searchLayout: RecyclerView
    private lateinit var parentLayout: LinearLayout
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private lateinit var classAdapter: ClassAdapter

    // Data
    private lateinit var role: String
    private var id: Int = 0
    private lateinit var profilePic: String
    private lateinit var name: String

    private lateinit var addClassLauncher: ActivityResultLauncher<Intent>
    lateinit var loadingLayout: View
    lateinit var loadingProgress: View
    lateinit var loadingText: TextView
    private lateinit var emptyStateLayout: View

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            NotificationMethods.registerToken(id, lifecycleScope)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_class_manager_teacher)

        id = intent.getIntExtra("id", 0)
        role = intent.getStringExtra("role") ?: ""
        name = intent.getStringExtra("name") ?: ""
        profilePic = intent.getStringExtra("profilePic") ?: ""
        CurrentCourse.userId = id

        val toastMsg = intent.getStringExtra("toast_msg")
        if (!toastMsg.isNullOrEmpty()) ShowToast.showMessage(this, toastMsg)

        initViews()
        setupListeners()
        setupObservers()
        NotificationMethods.handleNotificationIntent(this, intent, id, role)

        if (id > 0) {
            viewModel.loadClasses(id, role)
            viewModel.loadHistory(id)
            NotificationMethods.setupNotifications(this, id, requestPermissionLauncher, lifecycleScope)
        }

        val btnRefresh = findViewById<Button>(R.id.btnRefresh)
        btnRefresh.setOnClickListener {
            loadData()
        }

        loadData()
    }

    private fun loadData() {
        val noInternetView = findViewById<View>(R.id.no_internet_view)
        val defaultView = findViewById<View>(R.id.default_view)
        val loadingLayout = findViewById<View>(R.id.loadingLayout)
        val loadingProgressBg = findViewById<View>(R.id.loadingProgressBg)

        if (!NetworkUtils.isConnected(this)) {
            noInternetView.visibility = View.VISIBLE
            defaultView.visibility = View.GONE
            loadingLayout.visibility = View.GONE
            loadingProgressBg.visibility = View.GONE

            // Hide the button so they can't attempt to create
            addClassesButton.visibility = View.GONE
            return
        }

        noInternetView.visibility = View.GONE
        defaultView.visibility = View.VISIBLE

        // Re-enable the button once internet is confirmed
        addClassesButton.visibility = View.VISIBLE

        if (id > 0) {
            viewModel.loadClasses(id, role)
            viewModel.loadHistory(id)
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

        parentLayout = findViewById(R.id.class_list_container)
        defaultView = findViewById(R.id.default_view)
        searchView = findViewById(R.id.search_view)
        mainToolbar = findViewById(R.id.toolbar_main)
        tilSearch = findViewById(R.id.tilSearch)
        searchIcon = findViewById(R.id.iv_search)
        addClassesButton = findViewById(R.id.btnAddClasses)
        etSearch = findViewById(R.id.etSearch)
        tvNoResults = findViewById(R.id.tvNoResults)
        llPastSearches = findViewById(R.id.llPastSearches)
        searchLayout = findViewById(R.id.rvSearchResults)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)

        classAdapter = ClassAdapter(mutableListOf()) { selectedClass ->
            openClass(selectedClass.courseId, selectedClass.sectionName)
        }
        searchLayout.adapter = classAdapter
        searchLayout.layoutManager = LinearLayoutManager(this)

    }

    private fun setupListeners() {
        mainToolbar.setNavigationOnClickListener { showFacultyMenu() }

        searchIcon.setOnClickListener {
            defaultView.visibility = View.GONE
            searchView.visibility = View.VISIBLE
            viewModel.loadHistory(id)
        }

        tilSearch.setStartIconOnClickListener {
            closeSearchMode()
        }

        etSearch.addTextChangedListener { text ->
            val query = text.toString().trim()
            if (query.isNotEmpty()) {
                llPastSearches.visibility = View.GONE
                if (!NetworkUtils.isConnected(this@TeacherClassManagerActivity)) {
                    ShowToast.showMessage(this@TeacherClassManagerActivity, "No internet connection. Please check your network.")
                    return@addTextChangedListener
                }
                viewModel.searchClasses(id, role, query)
            } else {
                llPastSearches.visibility = View.VISIBLE
                searchLayout.visibility = View.GONE
                tvNoResults.visibility = View.GONE
            }
        }

        addClassLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                viewModel.loadClasses(id, role, forceReload = true)
            }
        }

        addClassesButton.setOnClickListener {
            if (!NetworkUtils.isConnected(this)) {
                ShowToast.showMessage(this, "Unstable internet connection. Please wait for a stable signal to create a class.")

                // 2. Hide the button immediately as a visual cue
                it.visibility = View.GONE
                return@setOnClickListener
            }
            val intent = Intent(this, TeacherAddClassActivity::class.java)
            intent.putExtra("id", id)
            addClassLauncher.launch(intent)
        }

        swipeRefreshLayout.setOnRefreshListener {
            if (!NetworkUtils.isConnected(this)) {
                swipeRefreshLayout.isRefreshing = false
                loadData()
            } else {
                viewModel.loadClasses(id, role, forceReload = true)
            }
        }
    }

    private fun setupObservers() {
        viewModel.classState.observe(this) { state ->
            val isLoading = state is ClassState.Loading
            if (!swipeRefreshLayout.isRefreshing) {
                UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, isLoading)
            }

            val isSearchMode = searchView.visibility == View.VISIBLE

            when (state) {
                is ClassState.Loading -> {
                    emptyStateLayout.visibility = View.GONE
                }
                is ClassState.Success -> {
                    swipeRefreshLayout.isRefreshing = false
                    tvNoResults.visibility = View.GONE

                    if (isSearchMode) {
                        searchLayout.visibility = View.VISIBLE
                        val adapterList = state.data.map {
                            TeacherClass(it.sectionName, it.schedule, it.studentCount, it.courseId)
                        }.toMutableList()
                        classAdapter.updateData(adapterList)
                    } else {
                        emptyStateLayout.visibility = View.GONE
                        // 1. Load the manual cards
                        populateLinearLayout(state.data)

                        // 2. Resolve Course ID for Push Notifications
                        var targetCourseId = intent.getIntExtra("courseId", -1)
                        val metaString = intent.getStringExtra("notif_meta")

                        if (targetCourseId == -1 && !metaString.isNullOrEmpty()) {
                            try {
                                val metaJson = org.json.JSONObject(metaString)
                                targetCourseId = metaJson.optInt("course_id", -1)
                            } catch (e: Exception) { e.printStackTrace() }
                        }

                        // 🌟 3. DIRECT JUMP LOGIC
                        if (targetCourseId != -1) {
                            val targetClass = state.data.find { it.courseId == targetCourseId }
                            if (targetClass != null) {
                                // Go directly to TeacherNotificationsActivity
                                val pushIntent = Intent(this, TeacherNotificationsActivity::class.java)
                                pushIntent.putExtra("id", id)
                                pushIntent.putExtra("role", role)
                                pushIntent.putExtra("courseId", targetClass.courseId)
                                pushIntent.putExtra("sectionName", targetClass.sectionName)

                                this.intent.extras?.let { pushIntent.putExtras(it) }

                                // Clean up the intent
                                intent.removeExtra("courseId")
                                intent.removeExtra("notif_meta")
                                intent.removeExtra("notif_type")

                                startActivity(pushIntent)
                            } else {
                                ShowToast.showMessage(this, "Push Notif target (Course ID: $targetCourseId) not found.")
                            }
                        }
                    }
                }
                is ClassState.Empty -> {
                    swipeRefreshLayout.isRefreshing = false
                    if (isSearchMode) {
                        searchLayout.visibility = View.GONE
                        tvNoResults.visibility = View.VISIBLE
                    } else {
                        parentLayout.removeAllViews()
                        emptyStateLayout.visibility = View.VISIBLE
                    }
                }
                is ClassState.Error -> {
                    swipeRefreshLayout.isRefreshing = false
                    emptyStateLayout.visibility = View.GONE
                    if (!NetworkUtils.isConnected(this)) {
                        ShowToast.showMessage(this, "No internet connection. Please check your network.")
                        addClassesButton.visibility = View.GONE
                    } else {
                        ShowToast.showMessage(this, "Something went wrong while fetching data. Please try again.")
                        addClassesButton.visibility = View.VISIBLE
                    }
                }
                else -> {}
            }
        }

        viewModel.searchHistory.observe(this) { history ->
            inflatePastSearches(history)
        }

        viewModel.logoutState.observe(this) { state ->
            when (state) {
                is LogoutState.Loading -> {
                    UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, true)
                }
                is LogoutState.Success -> {
                    UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                    UIUtils.performLogout(this, CurrentCourse.courseId)
                }
                is LogoutState.Idle -> {}
            }
        }
    }

    private fun populateLinearLayout(data: List<ClassUiModel>) {
        parentLayout.removeAllViews()
        for (item in data) {
            val itemView = layoutInflater.inflate(R.layout.item_teacher_class_card, parentLayout, false)

            itemView.findViewById<TextView>(R.id.tvSectionName).text = item.sectionName
            itemView.findViewById<TextView>(R.id.tvSchedule).text = item.schedule
            itemView.findViewById<TextView>(R.id.tvStudentCount).text = "${item.studentCount} students"

            itemView.setOnClickListener {
                openClass(item.courseId, item.sectionName)
            }
            parentLayout.addView(itemView)
        }
    }

    private fun openClass(courseId: Int, sectionName: String) {
        val intent = Intent(this, TeacherClassPageActivity::class.java)
        intent.putExtra("id", id)
        intent.putExtra("role", role)
        intent.putExtra("courseId", courseId)
        intent.putExtra("sectionName", sectionName)

        this.intent.extras?.let { extras ->
            if (extras.containsKey("lessonId")) intent.putExtra("lessonId", extras.getInt("lessonId"))
            if (extras.containsKey("assessmentId")) intent.putExtra("assessmentId", extras.getInt("assessmentId"))
            if (extras.containsKey("studentId")) intent.putExtra("studentId", extras.getInt("studentId"))
            if (extras.containsKey("notif_type")) intent.putExtra("notif_type", extras.getString("notif_type"))
            if (extras.containsKey("notif_meta")) intent.putExtra("notif_meta", extras.getString("notif_meta"))
        }

        startActivity(intent)
    }

    private fun closeSearchMode() {
        searchView.visibility = View.GONE
        defaultView.visibility = View.VISIBLE
        etSearch.setText("")
        searchLayout.visibility = View.GONE
        tvNoResults.visibility = View.GONE
        llPastSearches.visibility = View.VISIBLE
        viewModel.loadClasses(id, role)
    }

    private fun inflatePastSearches(historyList: List<String>) {
        llPastSearches.removeAllViews()
        if (historyList.isEmpty()) {
            llPastSearches.visibility = View.GONE
            return
        }

        llPastSearches.visibility = View.VISIBLE
        for (text in historyList) {
            val tv = TextView(this)
            tv.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            tv.setPadding(16, 16, 16, 16)
            tv.text = text
            tv.setTextColor(resources.getColor(R.color.colorSubtleText, theme))
            tv.textSize = 14f

            val typedValue = TypedValue()
            theme.resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true)
            tv.setBackgroundResource(typedValue.resourceId)

            tv.setOnClickListener {
                etSearch.setText(text)
                viewModel.searchClasses(id, role, text)
            }
            llPastSearches.addView(tv)
        }
    }

    override fun onBackPressed() {
        if (searchView.visibility == View.VISIBLE) {
            closeSearchMode()
        } else {
            super.onBackPressed()
        }
    }

    private fun showFacultyMenu() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_faculty_menu, null)
        dialog.setContentView(dialogView)

        val ivProfile = dialog.findViewById<ImageView>(R.id.ivProfile)

        if (profilePic.isNotEmpty()) {
            Glide.with(this).load(profilePic).placeholder(R.drawable.ic_profile_default).error(R.drawable.ic_profile_default).circleCrop().into(ivProfile)
        }

        val tvGreeting = dialog.findViewById<TextView>(R.id.tvGreeting)
        tvGreeting.text = "Hi, ${this.name}!"

        tvGreeting.maxLines = 1
        tvGreeting.ellipsize = android.text.TextUtils.TruncateAt.END
        tvGreeting.isSingleLine = true

        dialogView.findViewById<View>(R.id.btnAccountSettings).setOnClickListener {
            startActivity(Intent(this, TeacherProfileActivity::class.java))
            dialog.dismiss()
        }
        dialogView.findViewById<View>(R.id.btnTerms).setOnClickListener {
            startActivity(Intent(this, TermsAndConditionsActivity::class.java))
            dialog.dismiss()
        }
        dialogView.findViewById<View>(R.id.btnPrivacy).setOnClickListener {
            startActivity(Intent(this, PrivacyPolicyActivity::class.java))
            dialog.dismiss()
        }

        dialogView.findViewById<View>(R.id.btnLogout).setOnClickListener {
            dialog.dismiss()
            viewModel.performLogoutAndClearToken()
        }

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
}
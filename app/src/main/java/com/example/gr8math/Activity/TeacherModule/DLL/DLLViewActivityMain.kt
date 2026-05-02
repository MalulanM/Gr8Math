package com.example.gr8math.Activity.TeacherModule.DLL

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.gr8math.Activity.TeacherModule.Notification.TeacherNotificationsActivity
import com.example.gr8math.Activity.TeacherModule.ClassManager.TeacherClassPageActivity
import com.example.gr8math.Activity.TeacherModule.Participants.TeacherParticipantsActivity
import com.example.gr8math.Data.Repository.DllMainEntity
import com.example.gr8math.Data.Repository.DllRepository
import com.example.gr8math.Model.CurrentCourse
import com.example.gr8math.R
import com.example.gr8math.Utils.NetworkUtils
import com.example.gr8math.Utils.NotificationHelper
import com.example.gr8math.Utils.ShowToast
import com.example.gr8math.Utils.UIUtils
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DLLViewActivityMain : AppCompatActivity() {

    private lateinit var loadingLayout: View
    private lateinit var loadingProgress: View
    private lateinit var loadingText: TextView
    private lateinit var dllContainer: LinearLayout

    private val repository = DllRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dll_view_main)

        initViews()
        setupBottomNav()


        val btnRefresh = findViewById<Button>(R.id.btnRefresh)
        if (btnRefresh != null) {
            btnRefresh.setOnClickListener {
                loadData()
            }
        }

        // Let the gatekeeper handle the network check and data loading
        loadData()
    }

    override fun onResume() {
        super.onResume()

        val bottomNav: BottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNav.setOnItemSelectedListener(null)
        bottomNav.selectedItemId = R.id.nav_dll
        NotificationHelper.fetchUnreadCount(bottomNav)
        setupBottomNav()

        loadData()
    }

    private fun loadData() {
        val noInternetView = findViewById<View>(R.id.no_internet_view)
        val scrollView = findViewById<View>(R.id.scrollView)

        // 1. Check for Internet
        if (!NetworkUtils.isConnected(this)) {
            // Show No Internet Screen, hide the main content
            noInternetView?.visibility = View.VISIBLE
            scrollView?.visibility = View.GONE
            return
        }

        // 2. HAS INTERNET: Hide error screen, show main content
        noInternetView?.visibility = View.GONE
        scrollView?.visibility = View.VISIBLE

        // 3. Fetch your actual data
        loadDllData(CurrentCourse.courseId)
    }


    private fun initViews() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        loadingLayout = findViewById(R.id.loadingLayout)
        loadingProgress = findViewById(R.id.loadingProgressBg)
        loadingText = findViewById(R.id.loadingText)
        dllContainer = findViewById(R.id.dllContainer)
    }

    private fun loadDllData(courseId: Int) {
        UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, true)

        lifecycleScope.launch {
            val result = repository.getDllMains(courseId)
            UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)

            result.onSuccess { dllList ->
                populateDllList(dllList)
            }.onFailure {
                ShowToast.showMessage(this@DLLViewActivityMain, "Failed to load DLLs")
            }
        }
    }

    private fun populateDllList(dllList: List<DllMainEntity>) {
        dllContainer.removeAllViews()

        if (dllList.isEmpty()) {
            val emptyView = layoutInflater.inflate(R.layout.item_dll_empty_state, dllContainer, false)
            dllContainer.addView(emptyView)
            return
        }

        for (dll in dllList) {
            val cardView = layoutInflater.inflate(R.layout.item_class_assessment_card, dllContainer, false)

            val tvTitle = cardView.findViewById<TextView>(R.id.tvTitle)
            val editBtn = cardView.findViewById<ImageButton>(R.id.ibEditAssessment)
            val removeBtn = cardView.findViewById<ImageButton>(R.id.ibDeleteAssessment)
            editBtn.visibility = View.GONE
            removeBtn.visibility = View.GONE
            val dateRange = formatDateRange(dll.availableFrom, dll.availableUntil)

            tvTitle.text = "DLL ($dateRange)"

            cardView.setOnClickListener {
                val intent = Intent(this@DLLViewActivityMain, DllMasterEditorActivity::class.java).apply {
                    putExtra("EXTRA_IS_EXISTING", true)
                    putExtra("EXTRA_MODE_EDIT", true)
                    putExtra("EXTRA_DLL_MAIN_ID", dll.id)
                    putExtra("EXTRA_COURSE_ID", dll.courseId)
                    putExtra("EXTRA_QUARTER", dll.quarterNumber ?: 1)
                    putExtra("EXTRA_WEEK", dll.weekNumber ?: 1)
                    putExtra("EXTRA_FROM", dll.availableFrom)
                    putExtra("EXTRA_UNTIL", dll.availableUntil)
                }
                startActivity(intent)
            }

            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 24 }

            cardView.layoutParams = layoutParams
            dllContainer.addView(cardView)
        }
    }

    private fun formatDateRange(fromDateStr: String?, untilDateStr: String?): String {
        if (fromDateStr.isNullOrEmpty() || untilDateStr.isNullOrEmpty()) return "TBD"

        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val fromDate = inputFormat.parse(fromDateStr)
            val untilDate = inputFormat.parse(untilDateStr)

            if (fromDate == null || untilDate == null) return "Unknown"

            val calFrom = Calendar.getInstance().apply { time = fromDate }
            val calUntil = Calendar.getInstance().apply { time = untilDate }

            if (calFrom.get(Calendar.YEAR) == calUntil.get(Calendar.YEAR)) {
                // Same year: "May 12 - May 17, 2026"
                val fromFormat = SimpleDateFormat("MMM d", Locale.getDefault())
                val untilFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                "${fromFormat.format(fromDate)} - ${untilFormat.format(untilDate)}"
            } else {
                // Different years: "Dec 28, 2025 - Jan 3, 2026"
                val fullFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                "${fullFormat.format(fromDate)} - ${fullFormat.format(untilDate)}"
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }

    private fun setupBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.nav_dll
        NotificationHelper.fetchUnreadCount(bottomNav)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_class -> {
                    startActivity(Intent(this, TeacherClassPageActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK))
                    finish(); true
                }
                R.id.nav_participants -> {
                    startActivity(Intent(this, TeacherParticipantsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK))
                    finish(); true
                }
                R.id.nav_notifications -> {
                    startActivity(Intent(this, TeacherNotificationsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK))
                    finish(); true
                }
                R.id.nav_dll -> true
                else -> false
            }
        }
    }
}
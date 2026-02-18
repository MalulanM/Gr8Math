package com.example.gr8math.Activity.TeacherModule.DLL

import android.content.Intent
import android.os.Bundle
import android.view.View
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
import com.example.gr8math.Utils.ShowToast
import com.example.gr8math.Utils.UIUtils
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
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
        loadDllData(CurrentCourse.courseId)
    }

    private fun initViews() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        loadingLayout = findViewById(R.id.loadingLayout)
        loadingProgress = findViewById(R.id.loadingProgressBg)
        loadingText = findViewById(R.id.loadingText)

        // Find the empty container from your XML
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
        dllContainer.removeAllViews() // Clear any old views first

        if (dllList.isEmpty()) {
            ShowToast.showMessage(this, "No DLLs found.")
            return
        }

        for (dll in dllList) {
            val cardView = layoutInflater.inflate(R.layout.item_class_assessment_card, dllContainer, false)


            val tvTitle = cardView.findViewById<TextView>(R.id.tvTitle)
            val ivIcon = cardView.findViewById<ImageView>(R.id.ivIcon)
            val formattedFrom = formatDate(dll.availableFrom)
            val formattedUntil = formatDate(dll.availableUntil)


            tvTitle.text = "DLL ($formattedFrom - $formattedUntil)"

            // 6. ADD CLICK LISTENER TO OPEN DLLViewActivity
            cardView.setOnClickListener {
                val intent = Intent(this, DLLViewActivity::class.java)
                // You can pass the specific DLL ID if your DLLViewActivity needs it later
                intent.putExtra("target_dll_id", dll.id)
                startActivity(intent)
            }

            // 7. ADD MARGIN TO SEPARATE CARDS
            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 24 // Adds space below each card
            }
            cardView.layoutParams = layoutParams

            // 8. ADD TO CONTAINER
            dllContainer.addView(cardView)
        }
    }

    // Helper: Converts "YYYY-MM-DD" into "MMM d" (e.g., "Dec 2")
    private fun formatDate(dateString: String?): String {
        if (dateString.isNullOrEmpty()) return "TBD"
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("MMM d", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            if (date != null) outputFormat.format(date) else "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    private fun setupBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.nav_dll
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_class -> {
                    startActivity(Intent(this, TeacherClassPageActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK))
                    finish()
                    true
                }
                R.id.nav_participants -> {
                    startActivity(Intent(this, TeacherParticipantsActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK))
                    finish()
                    true
                }
                R.id.nav_notifications -> {
                    startActivity(Intent(this, TeacherNotificationsActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK))
                    finish()
                    true
                }
                R.id.nav_dll -> true
                else -> false
            }
        }
    }
}
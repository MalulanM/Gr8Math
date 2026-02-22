package com.example.gr8math.Activity.StudentModule.Badges

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gr8math.Activity.StudentModule.ClassManager.StudentClassPageActivity
import com.example.gr8math.Activity.StudentModule.Grades.StudentGradesActivity
import com.example.gr8math.Activity.StudentModule.Notification.StudentNotificationsActivity
import com.example.gr8math.Activity.StudentModule.Profile.StudentProfileViewModel // 🌟 Reusing ViewModel
import com.example.gr8math.Adapter.BadgesAdapter
import com.example.gr8math.Data.Repository.BadgeUiModel
import com.example.gr8math.Data.Repository.StudentProfileData
import com.example.gr8math.Model.CurrentCourse
import com.example.gr8math.R
import com.example.gr8math.Utils.ShowToast
import com.example.gr8math.Utils.UIUtils
import com.example.gr8math.ViewModel.ProfileUiState
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class StudentBadgesActivity : AppCompatActivity() {

    private val viewModel: StudentProfileViewModel by viewModels()
    private val userId = CurrentCourse.userId

    private lateinit var bottomNav: BottomNavigationView
    private lateinit var rvBadges: RecyclerView
    private lateinit var adapter: BadgesAdapter

    // Optional: Add loading layout IDs here if your XML has them
    // private lateinit var loadingLayout: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_badges)

        initViews()
        setupBottomNav()
        observeViewModel()
        viewModel.loadProfile(userId)
    }

    private fun initViews() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        rvBadges = findViewById(R.id.rvBadges)
        rvBadges.layoutManager = LinearLayoutManager(this)

        // Initialize adapter with empty list first
        adapter = BadgesAdapter(emptyList()) { badge ->
            showAcquiredBadgeDialog(badge)
        }
        rvBadges.adapter = adapter
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is ProfileUiState.Loading -> {
                        // Optional: Show loading spinner
                    }
                    is ProfileUiState.Success -> {
                        val profileData = state.data as StudentProfileData
                        adapter.updateData(profileData.badges)
                    }
                    is ProfileUiState.Error -> {
                        ShowToast.showMessage(this@StudentBadgesActivity, state.message)
                    }
                }
            }
        }
    }

    private fun showAcquiredBadgeDialog(badge: BadgeUiModel) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_badge_acquired, null)
        val dialog = MaterialAlertDialogBuilder(this).setView(dialogView).create()

        dialogView.findViewById<ImageView>(R.id.ivDialogBadge).setImageResource(badge.imageRes)
        dialogView.findViewById<TextView>(R.id.tvDialogTitle).text = badge.name

        dialogView.findViewById<ImageButton>(R.id.btnClose).setOnClickListener {
            dialog.dismiss()
        }

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }

    // --- Navigation Logic ---
    private fun setupBottomNav() {
        bottomNav = findViewById(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.nav_badges
        setupBottomNavListeners(bottomNav)
    }

    // Extracted so we can safely reuse it in onResume
    private fun setupBottomNavListeners(navView: BottomNavigationView) {
        navView.setOnItemSelectedListener { item ->
            if (item.itemId == navView.selectedItemId) return@setOnItemSelectedListener true

            val intent = when (item.itemId) {
                R.id.nav_class -> Intent(this, StudentClassPageActivity::class.java)
                R.id.nav_notifications -> Intent(this, StudentNotificationsActivity::class.java)
                R.id.nav_grades -> Intent(this, StudentGradesActivity::class.java)
                else -> null
            }

            intent?.let {
                it.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(it)
            }
            true
        }
    }

    override fun onResume() {
        super.onResume()
        if (::bottomNav.isInitialized) {
            // Remove listener so it doesn't trigger a fake click
            bottomNav.setOnItemSelectedListener(null)

            // Force highlight the Badges icon
            bottomNav.selectedItemId = R.id.nav_badges

            // Re-attach the listener
            setupBottomNavListeners(bottomNav)
        }

        viewModel.loadProfile(userId) // Refresh in case they earned a badge elsewhere
    }
}
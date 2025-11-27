package com.example.gr8math // Make sure this matches your package name

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gr8math.adapter.Badge
import com.example.gr8math.adapter.BadgesAdapter
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class StudentBadgesActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_badges)

        Log.d("DEBUG", "StudentBadgesActivity started")
        // --- Toolbar ---
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        // --- Bottom Navigation ---
        bottomNav = findViewById(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.nav_badges

        bottomNav.setOnItemSelectedListener { item ->
            if (item.itemId == bottomNav.selectedItemId) {
                return@setOnItemSelectedListener true
            }

            val intent = when (item.itemId) {
                R.id.nav_class -> Intent(this, StudentClassPageActivity::class.java)
                R.id.nav_badges -> null
                R.id.nav_notifications -> Intent(this, StudentNotificationsActivity::class.java)
                R.id.nav_grades -> Intent(this, StudentGradesActivity::class.java)
                else -> null
            }

            intent?.let {
                // Prevent stacking
                it.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(it)
            }

            true
        }

        // --- Badge List Data ---
        // We use separate strings for List Title vs Dialog Title to match Figma
        val badgeList = listOf(
            Badge(
                1,
                getString(R.string.badge_first_ace_list_title),
                getString(R.string.badge_first_ace_dialog_title),
                getString(R.string.badge_first_ace_desc),
                getString(R.string.badge_date_placeholder),
                R.drawable.badge_firstace,
                true
            ),
            Badge(
                2,
                getString(R.string.badge_first_timer_list_title),
                getString(R.string.badge_first_timer_dialog_title),
                getString(R.string.badge_first_timer_desc),
                getString(R.string.badge_date_placeholder),
                R.drawable.badge_firsttimer,
                true
            ),
            Badge(
                3,
                getString(R.string.badge_first_escape_list_title),
                getString(R.string.badge_first_escape_dialog_title),
                getString(R.string.badge_first_escape_desc),
                getString(R.string.badge_date_locked),
                R.drawable.badge_firstescape,
                false // Locked
            ),
            Badge(
                4,
                getString(R.string.badge_perfect_escape_list_title),
                getString(R.string.badge_perfect_escape_dialog_title),
                getString(R.string.badge_perfect_escape_desc),
                getString(R.string.badge_date_placeholder),
                R.drawable.badge_perfectescape,
                true
            ),
            Badge(
                5,
                getString(R.string.badge_first_explo_list_title),
                getString(R.string.badge_first_explo_dialog_title),
                getString(R.string.badge_first_explo_desc),
                getString(R.string.badge_date_locked),
                R.drawable.badge_firstexplo,
                false // Locked
            ),
            Badge(
                6,
                getString(R.string.badge_full_explo_list_title),
                getString(R.string.badge_full_explo_dialog_title),
                getString(R.string.badge_full_explo_desc),
                getString(R.string.badge_date_placeholder),
                R.drawable.badge_fullexplo,
                true
            ),
            Badge(
                7,
                getString(R.string.badge_three_quarter_list_title),
                getString(R.string.badge_three_quarter_dialog_title),
                getString(R.string.badge_three_quarter_desc),
                getString(R.string.badge_date_locked),
                R.drawable.badge_threequarter,
                false // Locked
            ),
            Badge(
                8,
                getString(R.string.badge_triple_ace_list_title),
                getString(R.string.badge_triple_ace_dialog_title),
                getString(R.string.badge_triple_ace_desc),
                getString(R.string.badge_date_placeholder),
                R.drawable.badge_tripleace,
                true
            )
        )

        val rvBadges = findViewById<RecyclerView>(R.id.rvBadges)

        // Use LinearLayoutManager for Vertical List
        rvBadges.layoutManager = LinearLayoutManager(this)

        rvBadges.adapter = BadgesAdapter(badgeList) { badge ->
            showAcquiredBadgeDialog(badge)
        }
    }

    private fun showAcquiredBadgeDialog(badge: Badge) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_badge_acquired, null)
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create()

        // Populate Dialog
        dialogView.findViewById<ImageView>(R.id.ivDialogBadge).setImageResource(badge.iconResId)

        // Use the Long Title for the Dialog
        dialogView.findViewById<TextView>(R.id.tvDialogTitle).text = badge.dialogTitle

        // --- FIX: Close Button Logic ---
        dialogView.findViewById<ImageButton>(R.id.btnClose).setOnClickListener {
            dialog.dismiss()
        }

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }

    override fun onResume() {
        super.onResume()
        if (::bottomNav.isInitialized) {
            bottomNav.selectedItemId = R.id.nav_badges
        }
    }
}
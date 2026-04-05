package com.example.gr8math.Activity.TeacherModule.DLL

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.gr8math.R
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import androidx.lifecycle.lifecycleScope
import com.example.gr8math.Data.Repository.ClassPageRepository
import com.example.gr8math.Model.CurrentCourse
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class DllMasterEditorActivity : AppCompatActivity() {

    var courseId: Int = -1
    var quarterNumber: Int = 1
    var weekNumber: Int = 1
    var availableFrom: String = ""
    var availableUntil: String = ""

    var dllMainId: Int = -1
    var isEditMode: Boolean = false
    var isEditable: Boolean = true

    private val repository = ClassPageRepository()
    private var userProfile: ClassPageRepository.UserProfile? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dll_master_editor)

        // Gather Data from Intent
        courseId = intent.getIntExtra("EXTRA_COURSE_ID", -1)
        quarterNumber = intent.getIntExtra("EXTRA_QUARTER", 1)
        weekNumber = intent.getIntExtra("EXTRA_WEEK", 1)
        availableFrom = intent.getStringExtra("EXTRA_FROM") ?: ""
        availableUntil = intent.getStringExtra("EXTRA_UNTIL") ?: ""
        dllMainId = intent.getIntExtra("EXTRA_DLL_MAIN_ID", -1)
        isEditMode = intent.getBooleanExtra("EXTRA_MODE_EDIT", false)

        val isExistingDll = intent.getBooleanExtra("EXTRA_IS_EXISTING", false)
        if (isExistingDll) {
            isEditable = false
        }
        checkUserModerationStatus()

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        val viewPager = findViewById<ViewPager2>(R.id.viewPager)
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        val btnEditMode = findViewById<ImageView>(R.id.btnEditMode)

        toolbar.title = "Lesson Log"

        toolbar.setNavigationOnClickListener { handleBackNavigation() }


        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackNavigation()
            }
        })



        btnEditMode.visibility = if (isEditable) View.GONE else View.VISIBLE

        btnEditMode.setOnClickListener {
            if (userProfile?.isRestricted == true) {
                showRestrictionModal()
            } else {
                isEditable = true
                btnEditMode.visibility = View.GONE
                refreshFragments()
            }
        }

        viewPager.adapter = DllEditorPagerAdapter(this)
        viewPager.isUserInputEnabled = true

        val tabNames = arrayOf("Objectives", "Resources", "Procedures", "Reflection")
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = tabNames[position]
        }.attach()


        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            private var lastPos = viewPager.currentItem
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                // If we swipe to a DIFFERENT tab and we were editing, lock it.
                if (position != lastPos && isEditable && dllMainId != -1) {
                    isEditable = false
                    btnEditMode.visibility = View.VISIBLE
                    viewPager.post { refreshFragments() }
                }
                lastPos = position
            }
        })
    }

    private fun checkUserModerationStatus() {
        lifecycleScope.launch {
            userProfile = repository.getUserProfile(CurrentCourse.userId)
            val flashNotif = repository.getUnreadFlashWarning(CurrentCourse.userId)
            if (flashNotif != null) showFlashWarningOverlay(flashNotif)
        }
    }

    private fun showFlashWarningOverlay(notif: ClassPageRepository.FlashNotification) {
        val overlay = findViewById<FrameLayout>(R.id.flashWarningOverlay)
        findViewById<TextView>(R.id.tvFlashMessage).text = "${notif.message}\nWarning Strike: ${notif.meta?.warningCount ?: 0}/3"
        overlay.visibility = View.VISIBLE
        findViewById<Button>(R.id.btnDismissFlash).setOnClickListener {
            lifecycleScope.launch { repository.markNotificationRead(notif.id); overlay.visibility = View.GONE }
        }
    }

    private fun showRestrictionModal() {
        val overlay = findViewById<FrameLayout>(R.id.restrictionOverlay)
        val tvTimer = findViewById<TextView>(R.id.tvRestrictionTimer)

        userProfile?.restrictedAt?.let { timestamp ->
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
                val diffMs = System.currentTimeMillis() - (sdf.parse(timestamp.replace("Z", ""))?.time ?: 0L)
                val remainingMs = (24 * 60 * 60 * 1000) - diffMs

                if (remainingMs <= 0) {
                    overlay.visibility = View.GONE
                    userProfile = userProfile?.copy(isRestricted = false)
                    return
                }
                tvTimer.text = "${remainingMs / 3600000}h ${(remainingMs / 60000) % 60}m"
            } catch (e: Exception) { tvTimer.text = "24h 00m" }
        }

        overlay.visibility = View.VISIBLE
        findViewById<ImageButton>(R.id.btnCloseRestriction).setOnClickListener { overlay.visibility = View.GONE }
    }

    private fun handleBackNavigation() {
        if (isEditable) {
            val dialog = MaterialAlertDialogBuilder(this)
                .setTitle("Discard Changes?")
                .setMessage("You have unsaved content. If you go back, your changes will be lost.")
                .setNegativeButton("Yes") { _, _ -> finish() }
                .setPositiveButton("No", null)
                .show()

            val redColor = android.graphics.Color.parseColor("#E53935")
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setTextColor(redColor)
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE)
                .setTextColor(redColor)
        } else {
            finish()
        }
    }

    fun switchToViewMode() {
        isEditable = false
        findViewById<ImageView>(R.id.btnEditMode).visibility = View.VISIBLE
        refreshFragments()
    }

    private fun refreshFragments() {
        val viewPager = findViewById<ViewPager2>(R.id.viewPager)
        val currentTab = viewPager.currentItem
        viewPager.adapter = DllEditorPagerAdapter(this)
        viewPager.setCurrentItem(currentTab, false)
    }

    private inner class DllEditorPagerAdapter(activity: AppCompatActivity) :
        FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = 4
        override fun createFragment(position: Int): androidx.fragment.app.Fragment {
            return when (position) {
                0 -> DllObjectivesEditorFragment()
                1 -> DllResourcesEditorFragment()
                2 -> DllProceduresEditorFragment()
                3 -> DllReflectionEditorFragment()
                else -> DllObjectivesEditorFragment()
            }
        }
    }
}
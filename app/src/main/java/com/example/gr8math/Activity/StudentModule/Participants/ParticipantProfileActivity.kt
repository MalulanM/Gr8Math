package com.example.gr8math.Activity.StudentModule.Participants

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.gr8math.Data.Model.ProfileDisplayItem
import com.example.gr8math.R
import com.google.android.material.appbar.MaterialToolbar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ParticipantProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_participant_profile)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        // --- INTENT DATA ---
        val name = intent.getStringExtra("EXTRA_NAME") ?: ""
        val role = intent.getStringExtra("EXTRA_ROLE") ?: "Student"
        val profilePic = intent.getStringExtra("EXTRA_PROFILE_PIC") ?: ""

        // Shared views
        findViewById<TextView>(R.id.tvName).text = name
        findViewById<TextView>(R.id.tvRole).text = "Role: $role"
        findViewById<TextView>(R.id.tvBirthdate).text = "Birthdate: ${intent.getStringExtra("EXTRA_BIRTHDATE") ?: "-"}"

        val ivProfilePic = findViewById<ImageView>(R.id.ivProfile)
        Glide.with(this).load(profilePic).placeholder(R.drawable.ic_profile_default).circleCrop().into(ivProfilePic)

        val tvLRN = findViewById<TextView>(R.id.tvLRN)
        val tvDynamicHeader = findViewById<TextView>(R.id.tvDynamicHeader)

        val itemsJson = intent.getStringExtra("EXTRA_ITEMS_JSON")
        val itemsType = object : TypeToken<List<ProfileDisplayItem>>() {}.type
        val items: List<ProfileDisplayItem> = if (itemsJson != null) Gson().fromJson(itemsJson, itemsType) else emptyList()

        val emptyState = findViewById<TextView>(R.id.tvEmptyState)
        val badgesContainer = findViewById<LinearLayout>(R.id.llBadgesContainer)
        val certsContainer = findViewById<LinearLayout>(R.id.llCertificatesContainer)

        if (items.isEmpty()) {
            emptyState.visibility = View.VISIBLE
        }

        // --- ROLE SPECIFIC LOGIC ---
        if (role == "Teacher") {
            tvLRN.text = "Position: ${intent.getStringExtra("EXTRA_TEACHING_POSITION") ?: "-"}"
            tvDynamicHeader.text = intent.getStringExtra("EXTRA_ACHIEVEMENTS_HEADER") ?: "Achievements"

            if (items.isNotEmpty()) {
                certsContainer.visibility = View.VISIBLE
                items.forEach { item ->
                    val certView = LayoutInflater.from(this).inflate(R.layout.item_achievement, certsContainer, false)
                    certView.findViewById<ImageView>(R.id.ivDeleteAchievement).visibility = View.GONE
                    certView.findViewById<TextView>(R.id.tvItemCertName).text = item.title
                    certView.findViewById<TextView>(R.id.tvItemCertDate).text = item.subtitle

                    val img = certView.findViewById<ImageView>(R.id.ivItemCertPreview)
                    Glide.with(this).load(item.imageUrl).placeholder(R.drawable.ic_cert).into(img)

                    certsContainer.addView(certView)
                }
            }
        } else {
            tvLRN.text = "LRN: ${intent.getStringExtra("EXTRA_LRN") ?: "-"}"
            tvDynamicHeader.text = "Top Badges"

            if (items.isNotEmpty()) {
                badgesContainer.visibility = View.VISIBLE

                if (items.isNotEmpty()) bindBadgeSlot(R.id.llBadgeSlot1, R.id.ivBadge1, R.id.tvBadgeName1, items[0])
                if (items.size > 1) bindBadgeSlot(R.id.llBadgeSlot2, R.id.ivBadge2, R.id.tvBadgeName2, items[1])
                if (items.size > 2) bindBadgeSlot(R.id.llBadgeSlot3, R.id.ivBadge3, R.id.tvBadgeName3, items[2])
            }
        }
    }

    private fun bindBadgeSlot(slotId: Int, imgId: Int, txtId: Int, item: ProfileDisplayItem) {
        findViewById<View>(slotId).visibility = View.VISIBLE
        findViewById<TextView>(txtId).text = item.title
        findViewById<ImageView>(imgId).setImageResource(getBadgeDrawable(item.imageUrl?.toIntOrNull() ?: 0))
    }

    private fun getBadgeDrawable(id: Int): Int = when(id) {
        1 -> R.drawable.badge_firstace
        2 -> R.drawable.badge_firsttimer
        3 -> R.drawable.badge_firstescape
        4 -> R.drawable.badge_perfectescape
        5 -> R.drawable.badge_firstexplo
        6 -> R.drawable.badge_fullexplo
        7 -> R.drawable.badge_threequarter
        8 -> R.drawable.badge_tripleace
        else -> R.drawable.ic_cert
    }
}
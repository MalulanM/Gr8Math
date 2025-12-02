package com.example.gr8math

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.gr8math.dataObject.BadgeModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.gson.Gson

class ParticipantProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_participant_profile)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        // -------------------------------
        // INTENT DATA
        // -------------------------------
        val name = intent.getStringExtra("EXTRA_NAME") ?: ""
        val role = intent.getStringExtra("EXTRA_ROLE") ?: "Student"
        val profilePic = intent.getStringExtra("EXTRA_PROFILE_PIC") ?: ""

        // Student
        val lrn = intent.getStringExtra("EXTRA_LRN") ?: "Unknown"
        val gradeLevel = intent.getStringExtra("EXTRA_GRADE_LEVEL") ?: "Unknown"
        val badgeHeader = intent.getStringExtra("EXTRA_BADGE_HEADER") ?: "Badges"
        val birthdate = intent.getStringExtra("EXTRA_BIRTHDATE")?:"1/1/2025"
        // Teacher
        val teachingPosition = intent.getStringExtra("EXTRA_TEACHING_POSITION") ?: "Teacher I"
        val achievementsHeader = intent.getStringExtra("EXTRA_ACHIEVEMENTS_HEADER") ?: "Teaching Achievements"

        // Parse badge list if available
        val badgeJson = intent.getStringExtra("EXTRA_BADGE_LIST")
        val badges: List<BadgeModel> =
            if (badgeJson != null) Gson().fromJson(badgeJson, Array<BadgeModel>::class.java).toList()
            else emptyList()

        // -------------------------------
        // VIEW BINDINGS
        // -------------------------------
        val ivProfilePic = findViewById<ImageView>(R.id.ivProfile)
        val tvName = findViewById<TextView>(R.id.tvName)
        val tvRole = findViewById<TextView>(R.id.tvRole)
        val tvLRN = findViewById<TextView>(R.id.tvLRN)
        val tvBadgesHeader = findViewById<TextView>(R.id.tvBadgesHeader)
        val tvBirthdate = findViewById<TextView>(R.id.tvBirthdate)

        // -------------------------------
        // SET DATA
        // -------------------------------
        tvName.text = name
        tvRole.text = "Role: $role"
        tvBirthdate.text = "Birthdate: $birthdate"

        Glide.with(this)
            .load(profilePic)
            .placeholder(R.drawable.ic_profile_default)
            .circleCrop()
            .into(ivProfilePic)

        // -------------------------------
        // ROLE-BASED UI
        // -------------------------------
        if (role == "Teacher") {
            tvLRN.text = "Position: $teachingPosition"
            tvBadgesHeader.text = achievementsHeader

        } else {
            tvLRN.text = "LRN: $lrn"
            tvBadgesHeader.text = "$badgeHeader (${badges.size})"
        }
    }
}

package com.example.gr8math.Activity.StudentModule.Participants

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.gr8math.Activity.StudentModule.Badges.StudentBadgesActivity
import com.example.gr8math.Activity.StudentModule.ClassManager.StudentClassPageActivity
import com.example.gr8math.Activity.StudentModule.Grades.StudentGradesActivity
import com.example.gr8math.Activity.StudentModule.Notification.StudentNotificationsActivity
import com.example.gr8math.Data.Model.StudentInfoSide
import com.example.gr8math.Data.Model.TeacherInfo
import com.example.gr8math.R
import com.example.gr8math.Utils.ShowToast
import com.example.gr8math.ViewModel.StudentParticipantsState
import com.example.gr8math.ViewModel.StudentParticipantsViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.gson.Gson

class StudentParticipantsActivity : AppCompatActivity() {

    private val viewModel: StudentParticipantsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_participants)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        val rvStudents = findViewById<RecyclerView>(R.id.rvStudentParticipants)
        rvStudents.layoutManager = LinearLayoutManager(this)

        setupBottomNav()
        setupObservers()

        viewModel.loadParticipants()
    }

    private fun setupObservers() {
        viewModel.state.observe(this) { state ->

            when (state) {
                is StudentParticipantsState.Loading -> {

                }
                is StudentParticipantsState.Success -> {

                    val data = state.data


                    setupTeacherCard(data.teacher)
                    setupStudentList(data.students)
                }
                is StudentParticipantsState.Error -> {
                    ShowToast.showMessage(this, state.message)
                }
            }
        }
    }

    private fun setupTeacherCard(teacher: TeacherInfo?) {
        val teacherCard = findViewById<View>(R.id.cardTeacher)
        if (teacher == null) {
            teacherCard.visibility = View.GONE
            return
        }

        val tvTeacherName = teacherCard.findViewById<TextView>(R.id.tvName)
        val ivTeacherProfile = teacherCard.findViewById<ImageView>(R.id.ivProfile)

        val fullName = "${teacher.lastName}, ${teacher.firstName}"
        tvTeacherName.text = fullName

        val profilePicUrl = teacher.profilePic ?: ""
        if (profilePicUrl.isNotEmpty()) {
            Glide.with(this)
                .load(profilePicUrl)
                .placeholder(R.drawable.ic_profile_default)
                .error(R.drawable.ic_profile_default)
                .circleCrop()
                .into(ivTeacherProfile)
        } else {
            ivTeacherProfile.setImageResource(R.drawable.ic_profile_default)
        }

        teacherCard.setOnClickListener {
            val intent = Intent(this, ParticipantProfileActivity::class.java).apply {
                putExtra("EXTRA_ROLE", "Teacher")
                putExtra("EXTRA_NAME", fullName)
                putExtra("EXTRA_USER_ID", teacher.userId)
                putExtra("EXTRA_PROFILE_PIC", profilePicUrl)
                putExtra("EXTRA_TEACHING_POSITION", teacher.roles ?: "Teacher I")
                putExtra("EXTRA_ACHIEVEMENTS_HEADER", "Teaching Achievements")
                putExtra("EXTRA_BIRTHDATE", teacher.birthdate)
                putExtra("EXTRA_ITEMS_JSON", Gson().toJson(teacher.achievements))
            }
            startActivity(intent)
        }
    }

    private fun setupStudentList(students: List<StudentInfoSide>) {
        val rv = findViewById<RecyclerView>(R.id.rvStudentParticipants)

        rv.adapter = StudentListAdapter(students) { student ->
            val intent = Intent(this, ParticipantProfileActivity::class.java)
            val fullName = "${student.lastName}, ${student.firstName}"

            intent.putExtra("EXTRA_NAME", fullName)
            intent.putExtra("EXTRA_ROLE", "Student")
            intent.putExtra("EXTRA_USER_ID", student.userId)
            intent.putExtra("EXTRA_LRN", student.userId.toString())
            intent.putExtra("EXTRA_GRADE_LEVEL", student.gradeLevel?.toString() ?: "N/A")
            intent.putExtra("EXTRA_PROFILE_PIC", student.profilePic ?: "")
            intent.putExtra("EXTRA_BIRTHDATE", student.birthdate)
            intent.putExtra("EXTRA_BADGE_HEADER", "Badges")
            intent.putExtra("EXTRA_BADGE_LIST", Gson().toJson(student.badges))
            intent.putExtra("EXTRA_ITEMS_JSON", Gson().toJson(student.badges))
            startActivity(intent)
        }
    }

    private fun setupBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.nav_class

        bottomNav.setOnItemSelectedListener { item ->
            if (item.itemId == bottomNav.selectedItemId) return@setOnItemSelectedListener true

            val intent = when (item.itemId) {
                R.id.nav_class -> Intent(this, StudentClassPageActivity::class.java)
                R.id.nav_badges -> Intent(this, StudentBadgesActivity::class.java)
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

    class StudentListAdapter(
        private val students: List<StudentInfoSide>,
        private val onClick: (StudentInfoSide) -> Unit
    ) : RecyclerView.Adapter<StudentListAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView = view.findViewById(R.id.tvName)
            val ivProfile: ImageView = view.findViewById(R.id.ivProfile)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_participant_card, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val student = students[position]
            val fullName = "${student.lastName}, ${student.firstName}"
            holder.tvName.text = fullName

            val profilePicUrl = student.profilePic ?: ""
            if (profilePicUrl.isNotEmpty()) {
                Glide.with(holder.itemView.context)
                    .load(profilePicUrl)
                    .placeholder(R.drawable.ic_profile_default)
                    .error(R.drawable.ic_profile_default)
                    .circleCrop()
                    .into(holder.ivProfile)
            } else {
                holder.ivProfile.setImageResource(R.drawable.ic_profile_default)
            }

            holder.itemView.setOnClickListener { onClick(student) }
        }

        override fun getItemCount() = students.size
    }
}
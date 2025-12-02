package com.example.gr8math

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.gr8math.api.ConnectURL
import com.example.gr8math.dataObject.CurrentCourse
import com.example.gr8math.dataObject.ParticipantResponse
import com.example.gr8math.dataObject.StudentModel
import com.example.gr8math.dataObject.TeacherModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.gson.Gson

class StudentParticipantsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_participants)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        val rvStudents = findViewById<RecyclerView>(R.id.rvStudentParticipants)
        rvStudents.layoutManager = LinearLayoutManager(this)

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

        displayParticipant()
    }

    fun displayParticipant() {

        val call = ConnectURL.api.getParticipants(CurrentCourse.courseId)

        call.enqueue(object : retrofit2.Callback<ParticipantResponse> {
            override fun onResponse(
                call: retrofit2.Call<ParticipantResponse>,
                response: retrofit2.Response<ParticipantResponse>
            ) {

                if (!response.isSuccessful || response.body() == null) {
                    Log.e("API_DEBUG", "Response failed or body null")
                    return
                }

                val data = response.body()!!

                // -------------------------------
                // HANDLE TEACHER CARD
                // -------------------------------
                val teacher = data.teacher
                val teacherCard = findViewById<View>(R.id.cardTeacher)
                val tvTeacherName = teacherCard.findViewById<TextView>(R.id.tvName)

                val ivTeacherProfile = teacherCard.findViewById<ImageView>(R.id.ivProfile)
                if (teacher != null) {
                    val fullName = "${teacher.last_name}, ${teacher.first_name}"
                    tvTeacherName.text = fullName

                    val profilePicUrl = teacher.profile_pic ?: ""
                    if (profilePicUrl.isNotEmpty()) {
                        Glide.with(this@StudentParticipantsActivity)
                            .load(profilePicUrl)
                            .placeholder(R.drawable.ic_profile_default)
                            .error(R.drawable.ic_profile_default)
                            .circleCrop()
                            .into(ivTeacherProfile)
                    } else {
                        ivTeacherProfile.setImageResource(R.drawable.ic_profile_default)
                    }

                    teacherCard.setOnClickListener {
                        val intent = Intent(this@StudentParticipantsActivity, ParticipantProfileActivity::class.java)

                        intent.putExtra("EXTRA_ROLE", "Teacher")
                        intent.putExtra("EXTRA_NAME", fullName)
                        intent.putExtra("EXTRA_USER_ID", teacher.user_id)
                        intent.putExtra("EXTRA_PROFILE_PIC", teacher.profile_pic ?: "")
                        intent.putExtra("EXTRA_TEACHING_POSITION", teacher.roles ?: "Teacher I")
                        intent.putExtra("EXTRA_ACHIEVEMENTS_HEADER", "Teaching Achievements")
                        intent.putExtra("EXTRA_BIRTHDATE", teacher.birthdate)

                        startActivity(intent)
                    }
                }

                val rv = findViewById<RecyclerView>(R.id.rvStudentParticipants)

                rv.adapter = StudentListAdapter(data.students) { student ->
                    val intent = Intent(this@StudentParticipantsActivity, ParticipantProfileActivity::class.java)

                    val fullName = "${student.last_name}, ${student.first_name}"

                    intent.putExtra("EXTRA_NAME", fullName)
                    intent.putExtra("EXTRA_ROLE", "Student")
                    intent.putExtra("EXTRA_USER_ID", student.user_id)
                    intent.putExtra("EXTRA_LRN", student.user_id.toString())   // Replace when LRN field exists
                    intent.putExtra("EXTRA_GRADE_LEVEL", student.grade_level ?: "N/A")
                    intent.putExtra("EXTRA_PROFILE_PIC", student.profile_pic ?: "")
                    intent.putExtra("EXTRA_BIRTHDATE", student.birthdate)
                    intent.putExtra("EXTRA_BADGE_HEADER", "Badges")

                    // Convert badge list to JSON
                    val gson = Gson()
                    intent.putExtra("EXTRA_BADGE_LIST", gson.toJson(student.badges))

                    startActivity(intent)
                }
            }

            override fun onFailure(call: retrofit2.Call<ParticipantResponse>, t: Throwable) {
                Log.e("API_DEBUG", "API CALL FAILED: ${t.localizedMessage}")
            }
        })
    }

    // ============================
    // STUDENT ADAPTER
    // ============================
    class StudentListAdapter(
        private val students: List<StudentModel>,
        private val onClick: (StudentModel) -> Unit
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
            val fullName = "${student.last_name}, ${student.first_name}"
            holder.tvName.text = fullName
            val profilePicUrl = student.profile_pic ?: ""
            if (profilePicUrl.isNotEmpty()) {
                Glide.with(holder.itemView.context)
                    .load(profilePicUrl)
                    .placeholder(R.drawable.ic_profile_default) // Assume you have a default image
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

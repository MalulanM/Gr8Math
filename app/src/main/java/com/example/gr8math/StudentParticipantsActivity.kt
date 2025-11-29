package com.example.gr8math

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView

class StudentParticipantsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_participants)

        // 1. Setup Toolbar
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        // 2. Setup Teacher Card Data & Click Listener
        val teacherCard = findViewById<View>(R.id.cardTeacher)
        val teacherNameView = teacherCard.findViewById<TextView>(R.id.tvName)
        val teacherName = "Dela Cruz, Juan"
        teacherNameView.text = teacherName

        teacherCard.setOnClickListener {
            // Open Profile for Teacher
            val intent = Intent(this, ParticipantProfileActivity::class.java)
            intent.putExtra("EXTRA_NAME", teacherName)
            intent.putExtra("EXTRA_ROLE", "Teacher") // <--- PASS TEACHER ROLE
            startActivity(intent)
        }

        // 3. Setup Student List (RecyclerView)
        val rvStudents = findViewById<RecyclerView>(R.id.rvStudentParticipants)
        rvStudents.layoutManager = LinearLayoutManager(this)

        val studentList = listOf(
            "Dela Cruz, Juan", "Dela Cruz, Juan", "Dela Cruz, Juan",
            "Dela Cruz, Juan", "Dela Cruz, Juan", "Dela Cruz, Juan",
            "Dela Cruz, Juan", "Dela Cruz, Juan", "Dela Cruz, Juan"
        )

        rvStudents.adapter = StudentListAdapter(studentList) { studentName ->
            // Open Profile for Student
            val intent = Intent(this, ParticipantProfileActivity::class.java)
            intent.putExtra("EXTRA_NAME", studentName)
            intent.putExtra("EXTRA_ROLE", "Student") // <--- PASS STUDENT ROLE
            startActivity(intent)
        }

        // 4. Setup Bottom Navigation
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.nav_class

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_class -> {
                    finish()
                    true
                }
                R.id.nav_badges -> {
                    startActivity(Intent(this, StudentBadgesActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_notifications -> {
                    startActivity(Intent(this, StudentNotificationsActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_grades -> {
                    startActivity(Intent(this, StudentGradesActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    class StudentListAdapter(
        private val students: List<String>,
        private val onClick: (String) -> Unit
    ) : RecyclerView.Adapter<StudentListAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView = view.findViewById(R.id.tvName)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_participant_card, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val name = students[position]
            holder.tvName.text = name
            holder.itemView.setOnClickListener { onClick(name) }
        }

        override fun getItemCount() = students.size
    }
}
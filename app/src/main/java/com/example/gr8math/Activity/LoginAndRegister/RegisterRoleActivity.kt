package com.example.gr8math.Activity.LoginAndRegister

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.gr8math.R
import com.google.android.material.appbar.MaterialToolbar

class RegisterRoleActivity : AppCompatActivity() {

    private lateinit var ivStudent: ImageView
    private lateinit var ivTeacher: ImageView
    private lateinit var tvStudentLabel: TextView
    private lateinit var tvTeacherLabel: TextView
    private lateinit var tvSelectedRole: TextView
    private lateinit var btnNext: Button

    private var selectedRole: String? = null // "Student" or "Teacher"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_role_selection)

        // 1. Find the layout
        val mainContainer = findViewById<ConstraintLayout>(R.id.mainContainer)

        // 2. Ask the system for the "safe area" measurements
        ViewCompat.setOnApplyWindowInsetsListener(mainContainer) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            // 3. Add padding to avoid the notch (top) and navigation bar (bottom)
            view.setPadding(insets.left, insets.top, insets.right, insets.bottom)

            WindowInsetsCompat.CONSUMED
        }
        // Toolbar
        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }

        // Find Views
        val btnStudent = findViewById<LinearLayout>(R.id.btnStudent)
        val btnTeacher = findViewById<LinearLayout>(R.id.btnTeacher)

        ivStudent = findViewById(R.id.ivStudent)
        ivTeacher = findViewById(R.id.ivTeacher)
        tvStudentLabel = findViewById(R.id.tvStudentLabel)
        tvTeacherLabel = findViewById(R.id.tvTeacherLabel)
        tvSelectedRole = findViewById(R.id.tvSelectedRole)
        btnNext = findViewById(R.id.btnNext)

        // Listeners
        btnStudent.setOnClickListener { selectRole("Student") }
        btnTeacher.setOnClickListener { selectRole("Teacher") }


        btnNext.setOnClickListener {
            if (selectedRole == "Student") {
                val intent = Intent(this, StudentRegisterActivity::class.java)
                startActivity(intent)

            } else if (selectedRole == "Teacher") {
                val intent = Intent(this, TeacherRegisterActivity::class.java)
                startActivity(intent)
            }
        }
    }

    private fun selectRole(role: String) {
        selectedRole = role
        btnNext.isEnabled = true

        if (role == "Student") {
            // Activate Student
            ivStudent.setImageResource(R.drawable.ic_student_role_yellow)
            tvStudentLabel.setTextColor(ContextCompat.getColor(this, R.color.saffron))

            // Deactivate Teacher
            ivTeacher.setImageResource(R.drawable.ic_teacher_role_blue)
            tvTeacherLabel.setTextColor(ContextCompat.getColor(this, R.color.colorMatisse))

            // Update Text
            tvSelectedRole.text = "You are a Student!"
            tvSelectedRole.setTextColor(ContextCompat.getColor(this, R.color.colorText))

        } else {
            // Activate Teacher
            ivTeacher.setImageResource(R.drawable.ic_teacher_role_yellow)
            tvTeacherLabel.setTextColor(ContextCompat.getColor(this, R.color.saffron))

            // Deactivate Student
            ivStudent.setImageResource(R.drawable.ic_student_role_blue)
            tvStudentLabel.setTextColor(ContextCompat.getColor(this, R.color.colorMatisse))

            // Update Text
            tvSelectedRole.text = "You are a Teacher!"
            tvSelectedRole.setTextColor(ContextCompat.getColor(this, R.color.colorText))
        }
    }
}
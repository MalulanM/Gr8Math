package com.example.gr8math.Activity.StudentModule.Profile

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.gr8math.Activity.LoginAndRegister.ForgotPasswordActivity
import com.example.gr8math.Adapter.BadgeSelectionAdapter
import com.example.gr8math.Data.Repository.BadgeUiModel
import com.example.gr8math.Data.Repository.StudentProfileData
import com.example.gr8math.Model.CurrentCourse
import com.example.gr8math.R
import com.example.gr8math.Utils.ShowToast
import com.example.gr8math.Utils.UIUtils
import com.example.gr8math.ViewModel.ProfileUiState
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ProfileActivity : AppCompatActivity() {
    private val viewModel: StudentProfileViewModel by viewModels()
    private val userId = CurrentCourse.userId

    private var studentId: Int? = null
    private var cameraUri: Uri? = null
    private var currentProfilePicUrl: String? = ""
    private var currentName: String = ""
    private var currentBadges: List<BadgeUiModel> = emptyList()

    // Loading UI
    private lateinit var loadingLayout: View
    private lateinit var loadingProgress: View
    private lateinit var loadingText: TextView

    // Basic Form Elements
    private lateinit var etFirstName: TextInputEditText
    private lateinit var ivEditFirstName: ImageView
    private lateinit var etLastName: TextInputEditText
    private lateinit var ivEditLastName: ImageView
    private lateinit var etLRN: TextInputEditText
    private lateinit var ivEditLRN: ImageView
    private lateinit var etDob: TextInputEditText
    private lateinit var etGender: MaterialAutoCompleteTextView
    private lateinit var ivProfile: ImageView
    private lateinit var editPfp: ImageView
    private lateinit var tvChangePassword: TextView
    private lateinit var ivEditBadges: ImageView

    // Badge Display Elements (Slots with Names)
    private lateinit var tvNoBadges: TextView
    private lateinit var llBadgeContainer: View

    private lateinit var llBadgeSlot1: View
    private lateinit var ivBadge1: ImageView
    private lateinit var tvBadgeName1: TextView

    private lateinit var llBadgeSlot2: View
    private lateinit var ivBadge2: ImageView
    private lateinit var tvBadgeName2: TextView

    private lateinit var llBadgeSlot3: View
    private lateinit var ivBadge3: ImageView
    private lateinit var tvBadgeName3: TextView

    private var isEditingFirstName = false
    private var isEditingLastName = false
    private var isEditingLRN = false
    private lateinit var genderAdapter: ArrayAdapter<String>

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { handleImageResult(it) }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && cameraUri != null) handleImageResult(cameraUri!!)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_profile)

        initViews()
        setupListeners()
        observeViewModel()

        viewModel.loadProfile(userId)
    }

    private fun initViews() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        etFirstName = findViewById(R.id.etFirstName)
        ivEditFirstName = findViewById(R.id.ivEditFirstName)
        etLastName = findViewById(R.id.etLastName)
        ivEditLastName = findViewById(R.id.ivEditLastName)
        etLRN = findViewById(R.id.etLRN)
        ivEditLRN = findViewById(R.id.ivEditLRN)
        etDob = findViewById(R.id.etDob)
        etGender = findViewById(R.id.etGender)
        ivProfile = findViewById(R.id.ivProfile)
        editPfp = findViewById(R.id.editPfp)
        tvChangePassword = findViewById(R.id.tvChangePassword)
        ivEditBadges = findViewById(R.id.ivEditBadges)

        loadingLayout = findViewById(R.id.loadingLayout)
        loadingProgress = findViewById(R.id.loadingProgressBg)
        loadingText = findViewById(R.id.loadingText)

        // Badge Container UI
        tvNoBadges = findViewById(R.id.tvNoBadges)
        llBadgeContainer = findViewById(R.id.llBadgeContainer)

        llBadgeSlot1 = findViewById(R.id.llBadgeSlot1)
        ivBadge1 = findViewById(R.id.ivBadge1)
        tvBadgeName1 = findViewById(R.id.tvBadgeName1)

        llBadgeSlot2 = findViewById(R.id.llBadgeSlot2)
        ivBadge2 = findViewById(R.id.ivBadge2)
        tvBadgeName2 = findViewById(R.id.tvBadgeName2)

        llBadgeSlot3 = findViewById(R.id.llBadgeSlot3)
        ivBadge3 = findViewById(R.id.ivBadge3)
        tvBadgeName3 = findViewById(R.id.tvBadgeName3)

        setEditMode(etFirstName, ivEditFirstName, false)
        setEditMode(etLastName, ivEditLastName, false)
        setEditMode(etLRN, ivEditLRN, false)

        val genderItems = listOf("Male", "Female")
        genderAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, genderItems)
        etGender.setAdapter(genderAdapter)
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is ProfileUiState.Loading -> UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, true)
                    is ProfileUiState.Success -> {
                        UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                        populateUI(state.data as StudentProfileData)
                    }
                    is ProfileUiState.Error -> {
                        UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                        ShowToast.showMessage(this@ProfileActivity, state.message)
                    }
                }
            }
        }
    }

    private fun populateUI(data: StudentProfileData) {
        studentId = data.student?.id
        currentProfilePicUrl = data.user.profilePic
        currentName = "${data.user.firstName ?: ""} ${data.user.lastName ?: ""}".trim()
        currentBadges = data.badges

        etFirstName.setText(data.user.firstName ?: "")
        etLastName.setText(data.user.lastName ?: "")
        etGender.setText(data.user.gender ?: "", false)
        etDob.setText(formatDate(data.user.birthdate) ?: "")
        etLRN.setText(data.student?.lrn ?: "")

        if (!data.user.profilePic.isNullOrEmpty()) {
            Glide.with(this).load(data.user.profilePic).circleCrop()
                .placeholder(R.drawable.ic_profile_default).error(R.drawable.ic_profile_default).into(ivProfile)
        } else {
            ivProfile.setImageResource(R.drawable.ic_profile_default)
        }

        // --- POPULATE THE 3 BADGE SLOTS WITH NAMES ---
        val rankedBadges = currentBadges.filter { it.rank != null }.sortedBy { it.rank }

        // Hide slots initially
        llBadgeSlot1.visibility = View.INVISIBLE
        llBadgeSlot2.visibility = View.INVISIBLE
        llBadgeSlot3.visibility = View.INVISIBLE

        if (rankedBadges.isEmpty()) {
            tvNoBadges.visibility = View.VISIBLE
            llBadgeContainer.visibility = View.GONE
        } else {
            tvNoBadges.visibility = View.GONE
            llBadgeContainer.visibility = View.VISIBLE

            if (rankedBadges.isNotEmpty()) {
                ivBadge1.setImageResource(rankedBadges[0].imageRes)
                tvBadgeName1.text = rankedBadges[0].name
                llBadgeSlot1.visibility = View.VISIBLE
            }
            if (rankedBadges.size > 1) {
                ivBadge2.setImageResource(rankedBadges[1].imageRes)
                tvBadgeName2.text = rankedBadges[1].name
                llBadgeSlot2.visibility = View.VISIBLE
            }
            if (rankedBadges.size > 2) {
                ivBadge3.setImageResource(rankedBadges[2].imageRes)
                tvBadgeName3.text = rankedBadges[2].name
                llBadgeSlot3.visibility = View.VISIBLE
            }
        }
    }

    private fun setupListeners() {
        ivEditFirstName.setOnClickListener {
            isEditingFirstName = !isEditingFirstName
            setEditMode(etFirstName, ivEditFirstName, isEditingFirstName)
            if (isEditingFirstName) showKeyboard(etFirstName) else saveData("First Name", etFirstName.text.toString())
        }

        ivEditLastName.setOnClickListener {
            isEditingLastName = !isEditingLastName
            setEditMode(etLastName, ivEditLastName, isEditingLastName)
            if (isEditingLastName) showKeyboard(etLastName) else saveData("Last Name", etLastName.text.toString())
        }

        ivEditLRN.setOnClickListener {
            isEditingLRN = !isEditingLRN
            setEditMode(etLRN, ivEditLRN, isEditingLRN)
            if (isEditingLRN) showKeyboard(etLRN) else saveData("LRN", etLRN.text.toString())
        }

        etGender.setOnItemClickListener { parent, _, position, _ ->
            saveData("Gender", parent.getItemAtPosition(position).toString())
        }

        etDob.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, day ->
                val selected = Calendar.getInstance().apply { set(year, month, day) }
                val formattedDate = SimpleDateFormat("MM/dd/yyyy", Locale.US).format(selected.time)
                etDob.setText(formattedDate)
                val dbFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(selected.time)
                viewModel.updateField(userId, "user", "birthdate", dbFormat)
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        ivEditBadges.setOnClickListener { showBadgeSelectionDialog() }

        tvChangePassword.setOnClickListener {
            val intent = Intent(this, ForgotPasswordActivity::class.java).apply {
                putExtra("id", userId)
                putExtra("EXTRA_ROLE", "Student")
                putExtra("name", currentName)
                putExtra("profilePic", currentProfilePicUrl)
                putExtra("EXTRA_IS_DIRECT_CHANGE", true)
            }
            startActivity(intent)
        }

        editPfp.setOnClickListener { showUploadSourceDialog() }
    }

    private fun showBadgeSelectionDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_select_badges, null)
        val dialog = MaterialAlertDialogBuilder(this).setView(dialogView).create()

        val acquiredBadges = currentBadges.filter { it.isAcquired }
        val rvSelection = dialogView.findViewById<RecyclerView>(R.id.rvBadgeSelection)
        var adapter: BadgeSelectionAdapter? = null

        if (acquiredBadges.isEmpty()) {
            rvSelection.visibility = View.GONE
            val tvEmpty = TextView(this).apply {
                text = "No badges acquired yet."
                textSize = 14f
                setTextColor(ContextCompat.getColor(context, R.color.colorSubtleText))
                gravity = Gravity.CENTER
                setPadding(0, 60, 0, 60)
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                try { typeface = ResourcesCompat.getFont(context, R.font.lexend) } catch (_: Exception) {}
            }
            (rvSelection.parent as ViewGroup).addView(tvEmpty, 1)
        } else {
            rvSelection.visibility = View.VISIBLE
            rvSelection.layoutManager = LinearLayoutManager(this)
            adapter = BadgeSelectionAdapter(acquiredBadges)
            adapter.selectedBadges.addAll(acquiredBadges.filter { it.rank != null })
            rvSelection.adapter = adapter
        }

        dialogView.findViewById<View>(R.id.btnClose).setOnClickListener { dialog.dismiss() }

        dialogView.findViewById<View>(R.id.btnSaveSelection).setOnClickListener {
            if (studentId == null) {
                ShowToast.showMessage(this, "Profile not fully loaded yet.")
                return@setOnClickListener
            }

            val selected = adapter?.selectedBadges ?: emptyList()

            if (selected.size > 3) {
                ShowToast.showMessage(this, "You can only display a maximum of 3 badges.")
                return@setOnClickListener
            }

            val messageText = if (selected.isEmpty()) "Are you sure you want to clear your displayed badges?" else "Are you sure you want to save\nthese ${selected.size} badges?"
            val messageView = TextView(this).apply {
                text = messageText
                textSize = 16f
                setTypeface(null, Typeface.BOLD)
                setTextColor(ContextCompat.getColor(context, R.color.colorText))
                setPadding(70, 60, 50, 10)
                try { typeface = ResourcesCompat.getFont(context, R.font.lexend) } catch (_: Exception) { }
            }

            val confirmDialog = MaterialAlertDialogBuilder(this)
                .setView(messageView)
                .setPositiveButton("No") { d, _ -> d.dismiss() }
                .setNegativeButton("Yes") { d, _ ->
                    d.dismiss()
                    dialog.dismiss()
                    val selectedBadgeIds = selected.map { it.id }
                    viewModel.updateDisplayedBadges(studentId!!, selectedBadgeIds, userId)
                }.create()

            confirmDialog.show()
            confirmDialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(this, R.color.colorRed))
            confirmDialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(ContextCompat.getColor(this, R.color.colorRed))
        }

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }

    private fun showUploadSourceDialog() {
        val sheetView = LayoutInflater.from(this).inflate(R.layout.dialog_upload_certificate_source, null)
        val bottomSheetDialog = BottomSheetDialog(this)
        bottomSheetDialog.setContentView(sheetView)
        (sheetView.parent as? View)?.setBackgroundColor(Color.TRANSPARENT)

        sheetView.findViewById<View>(R.id.btnPhotoAlbum).setOnClickListener {
            galleryLauncher.launch("image/*")
            bottomSheetDialog.dismiss()
        }

        sheetView.findViewById<View>(R.id.btnCamera).setOnClickListener {
            cameraUri = FileProvider.getUriForFile(this, "${packageName}.provider", createImageFile())
            cameraUri?.let { cameraLauncher.launch(it) }
            bottomSheetDialog.dismiss()
        }

        sheetView.findViewById<View>(R.id.btnCancelUpload).setOnClickListener { bottomSheetDialog.dismiss() }
        bottomSheetDialog.show()
    }

    private fun handleImageResult(uri: Uri) {
        try {
            Glide.with(this).load(uri).placeholder(R.drawable.ic_profile_default)
                .circleCrop().diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true).into(ivProfile)

            val bytes = contentResolver.openInputStream(uri)?.readBytes()
            val mime = contentResolver.getType(uri) ?: "image/jpeg"

            if (bytes != null) viewModel.updateProfilePic(userId, bytes, mime)
            else ShowToast.showMessage(this, "Failed to read image")
        } catch (e: Exception) {
            ivProfile.setImageResource(R.drawable.ic_profile_default)
        }
    }

    private fun setEditMode(editText: EditText, iconView: ImageView, isEditing: Boolean) {
        editText.isEnabled = isEditing; editText.isFocusable = isEditing; editText.isFocusableInTouchMode = isEditing
        if (isEditing) {
            iconView.setImageResource(R.drawable.ic_check)
            iconView.setColorFilter(ContextCompat.getColor(this, R.color.colorMatisse))
        } else {
            iconView.setImageResource(R.drawable.ic_edit)
            iconView.setColorFilter(ContextCompat.getColor(this, R.color.colorDarkCyan))
        }
    }

    private fun saveData(field: String, value: String) {
        when (field) {
            "First Name" -> viewModel.updateField(userId, "user", "first_name", value)
            "Last Name" -> viewModel.updateField(userId, "user", "last_name", value)
            "Gender" -> viewModel.updateField(userId, "user", "gender", value)
            "LRN" -> viewModel.updateField(userId, "student", "learners_ref_number", value)
        }
    }

    private fun formatDate(dbDate: String?): String? {
        if (dbDate.isNullOrEmpty()) return null
        return try {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dbDate)
            SimpleDateFormat("MM/dd/yyyy", Locale.US).format(date!!)
        } catch (e: Exception) { dbDate }
    }

    private fun showKeyboard(view: View) {
        view.requestFocus()
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun createImageFile(): File = File.createTempFile(
        "JPEG_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}_", ".jpg", getExternalFilesDir("images")
    )
}
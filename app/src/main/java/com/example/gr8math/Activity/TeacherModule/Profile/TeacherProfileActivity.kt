package com.example.gr8math.Activity.TeacherModule.Profile

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.NumberPicker
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
import com.example.gr8math.Data.Repository.TeacherAchievementEntity
import com.example.gr8math.Model.CurrentCourse
import com.example.gr8math.R
import com.example.gr8math.Utils.ShowToast
import com.example.gr8math.Utils.UIUtils
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

class TeacherProfileActivity : AppCompatActivity() {

    private val viewModel: TeacherProfileViewModel by viewModels()
    private val userId = CurrentCourse.userId

    private var cameraUri: Uri? = null
    private var dialogPreviewContainer: View? = null
    private var dialogCertPreview: ImageView? = null
    private var dialogUploadBtn: View? = null

    private var isUploadingProfilePic = true
    private var pendingCertUri: Uri? = null
    private var currentProfilePicUrl: String? = ""

    // UI Elements
    private lateinit var loadingLayout: View
    private lateinit var loadingProgress: View
    private lateinit var loadingText: TextView
    private lateinit var etFirstName: TextInputEditText
    private lateinit var ivEditFirstName: ImageView
    private lateinit var etLastName: TextInputEditText
    private lateinit var ivEditLastName: ImageView
    private lateinit var etTeachingPos: MaterialAutoCompleteTextView
    private lateinit var etDob: TextInputEditText
    private lateinit var etGender: MaterialAutoCompleteTextView
    private lateinit var tvChangePassword: TextView
    private lateinit var editPfp: ImageView
    private lateinit var ivProfile: ImageView
    private lateinit var ivEditAchievements: ImageView

    private lateinit var tvAchievementsEmpty: TextView
    private lateinit var certContainer: RecyclerView
    private lateinit var achievementAdapter: AchievementAdapter

    private var isEditingFirstName = false
    private var isEditingLastName = false


    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { handleImageResult(it) }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && cameraUri != null) handleImageResult(cameraUri!!)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_teacher_profile)

        initViews()
        setupListeners()
        observeViewModel()

        viewModel.loadProfile(userId)
    }

    private fun initViews() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        loadingLayout = findViewById(R.id.loadingLayout)
        loadingProgress = findViewById(R.id.loadingProgressBg)
        loadingText = findViewById(R.id.loadingText)

        etFirstName = findViewById(R.id.etFirstName)
        ivEditFirstName = findViewById(R.id.ivEditFirstName)
        etLastName = findViewById(R.id.etLastName)
        ivEditLastName = findViewById(R.id.ivEditLastName)
        etTeachingPos = findViewById(R.id.etTeachingPos)
        etDob = findViewById(R.id.etDob)
        etGender = findViewById(R.id.etGender)
        tvChangePassword = findViewById(R.id.tvChangePassword)
        editPfp = findViewById(R.id.editPfp)
        ivProfile = findViewById(R.id.ivProfile)
        ivEditAchievements = findViewById(R.id.ivEditBadges)

        tvAchievementsEmpty = findViewById(R.id.tvAchievementsList)

        certContainer = findViewById(R.id.certContainer)
        achievementAdapter = AchievementAdapter(
            onCertClick = { imageUrl -> showFullCertificateDialog(imageUrl) },
            onDeleteClick = { achievement -> showDeleteDialog(achievement) }
        )
        certContainer.layoutManager = LinearLayoutManager(this)
        certContainer.adapter = achievementAdapter

        setEditMode(etFirstName, ivEditFirstName, false)
        setEditMode(etLastName, ivEditLastName, false)

        etGender.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, listOf("Male", "Female")))
        etTeachingPos.setAdapter(
            ArrayAdapter(
                this,
                android.R.layout.simple_list_item_1,
                listOf(
                    "Teacher I", "Teacher II", "Teacher III", "Teacher IV",
                    "Teacher V", "Teacher VI", "Teacher VII",
                    "Master I", "Master II", "Master III", "Master IV", "Master V"
                )
            )
        )
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is ProfileUiState.Loading -> UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, true)
                    is ProfileUiState.Success -> {
                        UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                        populateUI(state.data)
                    }
                    is ProfileUiState.Error -> {
                        UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                        ShowToast.showMessage(this@TeacherProfileActivity, state.message)
                    }
                }
            }
        }
    }

    private fun populateUI(data: com.example.gr8math.Data.Repository.TeacherProfileData) {
        etFirstName.setText(data.user.firstName ?: "")
        etLastName.setText(data.user.lastName ?: "")
        etDob.setText(formatDbDateToUi(data.user.birthdate))
        etGender.setText(data.user.gender ?: "", false)
        etTeachingPos.setText(data.teacher?.teachingPosition ?: "", false)

        currentProfilePicUrl = data.user.profilePic
        loadProfileImage(data.user.profilePic)
        displayAchievements(data.achievements)
    }

    private fun displayAchievements(achievements: List<TeacherAchievementEntity>) {
        if (achievements.isEmpty()) {
            tvAchievementsEmpty.visibility = View.VISIBLE
            certContainer.visibility = View.GONE
        } else {
            tvAchievementsEmpty.visibility = View.GONE
            certContainer.visibility = View.VISIBLE

            // Push the full list (Old Supabase + New Tigris) to the adapter
            achievementAdapter.setAchievements(achievements)
        }
    }

    private fun setupListeners() {
        ivEditFirstName.setOnClickListener {
            isEditingFirstName = !isEditingFirstName
            setEditMode(etFirstName, ivEditFirstName, isEditingFirstName)
            if (isEditingFirstName) showKeyboard(etFirstName)
            else viewModel.updateUserProfile(userId, "first_name", etFirstName.text.toString())
        }

        ivEditLastName.setOnClickListener {
            isEditingLastName = !isEditingLastName
            setEditMode(etLastName, ivEditLastName, isEditingLastName)
            if (isEditingLastName) showKeyboard(etLastName)
            else viewModel.updateUserProfile(userId, "last_name", etLastName.text.toString())
        }

        etTeachingPos.setOnItemClickListener { parent, _, position, _ ->
            viewModel.updateTeacherProfile(userId, "teaching_position", parent.getItemAtPosition(position).toString())
        }

        etGender.setOnItemClickListener { parent, _, position, _ ->
            viewModel.updateUserProfile(userId, "gender", parent.getItemAtPosition(position).toString())
        }

        etDob.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, day ->
                val selected = Calendar.getInstance().apply { set(year, month, day) }
                viewModel.updateUserProfile(userId, "birthdate", SimpleDateFormat("yyyy-MM-dd", Locale.US).format(selected.time))
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }


        tvChangePassword.setOnClickListener {
            val intent = Intent(this, ForgotPasswordActivity::class.java).apply {
                putExtra("id", userId)
                putExtra("EXTRA_ROLE", "Teacher")
                putExtra("name", etFirstName.text.toString())
                putExtra("profilePic", currentProfilePicUrl)
                putExtra("EXTRA_IS_DIRECT_CHANGE", true)
            }
            startActivity(intent)
        }

        ivEditAchievements.setOnClickListener { showAddAchievementDialog() }

        editPfp.setOnClickListener {
            isUploadingProfilePic = true
            showUploadSourceDialog()
        }
    }

    private fun showAddAchievementDialog() {
        pendingCertUri = null
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_achievement, null)
        val dialog = MaterialAlertDialogBuilder(this).setView(dialogView).setCancelable(false).create()

        val etAchievementName = dialogView.findViewById<TextInputEditText>(R.id.etAchievementName)
        val etYear = dialogView.findViewById<TextInputEditText>(R.id.etYearReceived)
        dialogUploadBtn = dialogView.findViewById<View>(R.id.btnUploadCertificate)
        dialogPreviewContainer = dialogView.findViewById<View>(R.id.rlCertPreviewContainer)
        dialogCertPreview = dialogView.findViewById<ImageView>(R.id.ivDialogCertPreview)
        val btnRemoveCertPreview = dialogView.findViewById<View>(R.id.btnRemoveCertPreview)

        dialogUploadBtn?.visibility = View.VISIBLE
        etYear.setOnClickListener { showYearPickerDialog(etYear) }

        dialogUploadBtn?.setOnClickListener {
            isUploadingProfilePic = false
            showUploadSourceDialog()
        }

        btnRemoveCertPreview.setOnClickListener {
            pendingCertUri = null
            dialogPreviewContainer?.visibility = View.GONE
            dialogUploadBtn?.visibility = View.VISIBLE
        }

        dialogView.findViewById<View>(R.id.btnSaveAchievement).setOnClickListener {
            val name = etAchievementName.text.toString().trim()
            val year = etYear.text.toString().trim()
            if (name.isEmpty() || year.isEmpty()) {
                Toast.makeText(this, "Please enter achievement name and year", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (pendingCertUri == null) {
                ShowToast.showMessage(this, "Please upload a certificate image first!")
                return@setOnClickListener
            }
            showSaveConfirmation(dialog, name, year)
        }

        dialogView.findViewById<View>(R.id.btnCloseDialog).setOnClickListener {
            pendingCertUri = null
            dialogPreviewContainer = null
            dialogCertPreview = null
            dialogUploadBtn = null
            dialog.dismiss()
        }

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }

    private fun showDeleteDialog(achievement: TeacherAchievementEntity) {
        val achievementId = achievement.id ?: return

        MaterialAlertDialogBuilder(this)
            .setTitle("Remove Achievement")
            .setMessage("Are you sure you want to delete this achievement?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteAchievement(achievementId, achievement.certificate)
            }
            .show()
    }

    private fun showSaveConfirmation(parentDialog: androidx.appcompat.app.AlertDialog, name: String, year: String) {
        val messageView = TextView(this).apply {
            text = "Are you sure you want to save\nthis achievement?"
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, R.color.colorText))
            setPadding(70, 60, 50, 10)
            try { typeface = ResourcesCompat.getFont(context, R.font.lexend) } catch (_: Exception) {}
        }

        val confirmDialog = MaterialAlertDialogBuilder(this)
            .setView(messageView)
            .setPositiveButton("No") { d, _ -> d.dismiss() }
            .setNegativeButton("Yes") { d, _ ->
                val dbFormattedDate = year
                var imageBytes: ByteArray? = null
                var mimeType: String? = null

                pendingCertUri?.let { uri ->
                    imageBytes = contentUriToByteArray(uri)
                    mimeType = contentResolver.getType(uri) ?: "image/jpeg"
                }

                viewModel.addAchievement(userId, name, dbFormattedDate, imageBytes, mimeType)
                d.dismiss()
                parentDialog.dismiss()
            }.create()

        confirmDialog.show()
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
        if (isUploadingProfilePic) {
            try {
                Glide.with(this).load(uri).placeholder(R.drawable.ic_profile_default)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .circleCrop()
                    .into(ivProfile)

                val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
                contentUriToByteArray(uri)?.let { imageBytes ->
                    viewModel.uploadAndUpdateProfilePicture(userId, imageBytes, mimeType)
                }
            } catch (e: Exception) {
                ivProfile.setImageResource(R.drawable.ic_profile_default)
            }
        } else {
            pendingCertUri = uri
            dialogUploadBtn?.visibility = View.GONE
            dialogPreviewContainer?.visibility = View.VISIBLE
            dialogCertPreview?.let { previewImage ->
                Glide.with(this).load(uri).into(previewImage)
            }
        }
    }

    private fun contentUriToByteArray(uri: Uri): ByteArray? {
        return try { contentResolver.openInputStream(uri)?.use { it.readBytes() } } catch (e: Exception) { null }
    }

    private fun loadProfileImage(imageUrl: String?) {
        if (!imageUrl.isNullOrEmpty()) {
            Glide.with(this).load(imageUrl)
                .placeholder(R.drawable.ic_profile_default)
                .error(R.drawable.ic_profile_default)
                .circleCrop()
                .into(ivProfile)
        } else {
            ivProfile.setImageResource(R.drawable.ic_profile_default)
        }
    }

    private fun showFullCertificateDialog(imageUrl: String) {
        val dialog = android.app.Dialog(this, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen)
        val rootLayout = android.widget.RelativeLayout(this).apply {
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#CC000000"))
        }

        val fullScreenImageView = ImageView(this).apply {
            val params = android.widget.RelativeLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
            params.setMargins(60, 60, 60, 60)
            layoutParams = params
            scaleType = ImageView.ScaleType.FIT_CENTER
        }

        val closeButton = ImageView(this).apply {
            val btnSize = 120
            val params = android.widget.RelativeLayout.LayoutParams(btnSize, btnSize)
            params.addRule(android.widget.RelativeLayout.ALIGN_PARENT_TOP)
            params.addRule(android.widget.RelativeLayout.ALIGN_PARENT_END)
            params.setMargins(0, 80, 40, 0)
            layoutParams = params
            setImageResource(R.drawable.ic_close)
            setColorFilter(Color.WHITE)
            val outValue = android.util.TypedValue()
            theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true)
            setBackgroundResource(outValue.resourceId)
            isClickable = true
            isFocusable = true
        }

        Glide.with(this).load(imageUrl).diskCacheStrategy(DiskCacheStrategy.ALL).into(fullScreenImageView)
        closeButton.setOnClickListener { dialog.dismiss() }
        rootLayout.setOnClickListener { dialog.dismiss() }
        rootLayout.addView(fullScreenImageView)
        rootLayout.addView(closeButton)
        dialog.setContentView(rootLayout)
        dialog.show()
    }

    private fun formatDbDateToUi(dbDate: String?): String {
        if (dbDate.isNullOrEmpty()) return ""
        return try {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dbDate)
            SimpleDateFormat("MM/dd/yyyy", Locale.US).format(date!!)
        } catch (e: Exception) { dbDate }
    }

    private fun formatDbDateToFull(dbDate: String?): String {
        if (dbDate.isNullOrEmpty()) return ""
        return try {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dbDate)
            SimpleDateFormat("yyyy", Locale.US).format(date!!)
        } catch (e: Exception) { dbDate }
    }

    private fun showYearPickerDialog(editText: EditText) {
        val picker = NumberPicker(ContextThemeWrapper(this, R.style.Theme_Gr8Math)).apply {
            val year = Calendar.getInstance().get(Calendar.YEAR)
            minValue = 1980; maxValue = year + 10; value = year
            wrapSelectorWheel = false; descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("Select Year")
            .setView(picker)
            .setPositiveButton("OK") { _, _ -> editText.setText(picker.value.toString()) }
            .setNegativeButton("CANCEL", null)
            .show()
    }

    private fun setEditMode(editText: EditText, iconView: ImageView, isEditing: Boolean) {
        editText.isEnabled = isEditing; editText.isFocusable = isEditing; editText.isFocusableInTouchMode = isEditing
        iconView.setImageResource(if (isEditing) R.drawable.ic_check else R.drawable.ic_edit)
        iconView.setColorFilter(ContextCompat.getColor(this, R.color.colorDarkCyan))
    }

    private fun showKeyboard(view: View) {
        view.requestFocus()
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun createImageFile(): File {
        return File.createTempFile("JPEG_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}_", ".jpg", getExternalFilesDir("images"))
    }
}
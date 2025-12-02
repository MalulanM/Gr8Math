package com.example.gr8math

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.gr8math.adapter.Badge
import com.example.gr8math.adapter.BadgeSelectionAdapter
import com.example.gr8math.adapter.TeacherNotificationResponse
import com.example.gr8math.api.ConnectURL
import com.example.gr8math.dataObject.CurrentCourse
import com.example.gr8math.dataObject.ProfileResponse
import com.example.gr8math.dataObject.TeacherAchievement
import com.example.gr8math.dataObject.UpdateProfileRequest
import com.example.gr8math.utils.ShowToast
import com.example.gr8math.utils.UIUtils
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone


class TeacherProfileActivity : AppCompatActivity() {
    private var tempCertificateLink: String? = null
    private var cameraUri: Uri? = null

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { handleImageResult(it) }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && cameraUri != null) {
            handleImageResult(cameraUri!!)
        }
    }

    lateinit var loadingLayout : View
    lateinit var loadingProgress : View
    lateinit var loadingText : TextView


    // UI Elements
    private lateinit var etFirstName: TextInputEditText
    private lateinit var ivEditFirstName: ImageView

    private lateinit var etLastName: TextInputEditText
    private lateinit var ivEditLastName: ImageView

    private lateinit var etTeachingPos: MaterialAutoCompleteTextView
    private lateinit var etDob: TextInputEditText
    private lateinit var etGender: MaterialAutoCompleteTextView
    private lateinit var tvChangePassword: TextView

    private lateinit var editPfp : ImageView
    private lateinit var ivProfile : ImageView
    private var selectedImageBase64: String? = null

    // Achievement Edit Icon
    private lateinit var ivEditAchievements: ImageView

    // State trackers for edit mode
    private var isEditingFirstName = false
    private var isEditingLastName = false

    private var id = CurrentCourse.userId
    private lateinit var tvAchievementsList: TextView
    private lateinit var ivCertificatePreview: ImageView

    private lateinit var genderAdapter: ArrayAdapter<String>
    private lateinit var posAdapter: ArrayAdapter<String>



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_teacher_profile)
        Log.e("KEN2BDWBRE", id.toString())
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        loadingLayout =  findViewById<View>(R.id.loadingLayout)
        loadingProgress = findViewById<View>(R.id.loadingProgressBg)
        loadingText = findViewById<TextView>(R.id.loadingText)
        // --- Find Views ---
        etFirstName = findViewById(R.id.etFirstName)
        ivEditFirstName = findViewById(R.id.ivEditFirstName)

        etLastName = findViewById(R.id.etLastName)
        ivEditLastName = findViewById(R.id.ivEditLastName)
        editPfp = findViewById(R.id.editPfp)
        ivProfile = findViewById(R.id.ivProfile)
        tvAchievementsList = findViewById(R.id.tvAchievementsList)
        ivCertificatePreview = findViewById(R.id.ivCertificatePreview)


        etTeachingPos = findViewById(R.id.etTeachingPos)
        etDob = findViewById(R.id.etDob)
        etGender = findViewById(R.id.etGender)
        tvChangePassword = findViewById(R.id.tvChangePassword)
        ivEditAchievements = findViewById(R.id.ivEditBadges) // Reusing the ID from layout

        // --- Initialize View Mode ---
        setEditMode(etFirstName, ivEditFirstName, false)
        setEditMode(etLastName, ivEditLastName, false)

        // --- Setup Dropdowns ---
        val genderItems = listOf("Male", "Female")
        genderAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, genderItems)
        etGender.setAdapter(genderAdapter)


        val posItems = listOf("Teacher I", "Teacher II", "Teacher III", "Teacher IV", "Teacher V", "Teacher VI", "Teacher VII", "Master Teacher I", "Master Teacher II", "Master Teacher III", "Master Teacher IV", "Master Teacher V")
        posAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, posItems)
        etTeachingPos.setAdapter(posAdapter)

        // --- Setup Click Listeners ---

        ivEditFirstName.setOnClickListener {
            isEditingFirstName = !isEditingFirstName
            if (isEditingFirstName) {
                setEditMode(etFirstName, ivEditFirstName, true)
                etFirstName.requestFocus()
                showKeyboard(etFirstName)
            } else {
                setEditMode(etFirstName, ivEditFirstName, false)
                saveData("First Name", etFirstName.text.toString())
            }
        }

        ivEditLastName.setOnClickListener {
            isEditingLastName = !isEditingLastName
            if (isEditingLastName) {
                setEditMode(etLastName, ivEditLastName, true)
                etLastName.requestFocus()
                showKeyboard(etLastName)
            } else {
                setEditMode(etLastName, ivEditLastName, false)
                saveData("Last Name", etLastName.text.toString())
            }
        }

        etTeachingPos.setOnItemClickListener { parent, _, position, _ ->
            val selected = parent.getItemAtPosition(position).toString()
            saveData("Teaching Position", selected)
        }

        etGender.setOnItemClickListener { parent, _, position, _ ->
            val selected = parent.getItemAtPosition(position).toString()
            saveData("Gender", selected)
        }

        // --- UPDATE: Achievement Dialog Call ---
        ivEditAchievements.setOnClickListener {
            showAddAchievementDialog()
        }

        tvChangePassword.setOnClickListener {
            // Reusing Password Creation Activity with Teacher context
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            intent.putExtra("EXTRA_ROLE", "Teacher")
            startActivity(intent)
        }

        etDob.setOnClickListener {
            val calendar = Calendar.getInstance()
            val datePicker = DatePickerDialog(
                this,
                { _, year, month, day ->
                    val selectedDate = Calendar.getInstance()
                    selectedDate.set(year, month, day)
                    val formatter = SimpleDateFormat("MM/dd/yyyy", Locale.US)
                    val formattedDate = formatter.format(selectedDate.time)
                    etDob.setText(formattedDate)
                    saveData("Birthdate", formattedDate)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePicker.show()
        }

        editPfp.setOnClickListener {
            val sheetView = LayoutInflater.from(this).inflate(R.layout.dialog_upload_certificate_source, null)
            val bottomSheetDialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
            bottomSheetDialog.setContentView(sheetView)
            (sheetView.parent as? View)?.setBackgroundColor(Color.TRANSPARENT)

            sheetView.findViewById<View>(R.id.btnPhotoAlbum).setOnClickListener {
                galleryLauncher.launch("image/*")
                bottomSheetDialog.dismiss()
            }

            sheetView.findViewById<View>(R.id.btnCamera).setOnClickListener {
                cameraUri = androidx.core.content.FileProvider.getUriForFile(
                    this,
                    "${applicationContext.packageName}.provider",
                    createImageFile()
                )
                cameraUri?.let { cameraLauncher.launch(it) }
                bottomSheetDialog.dismiss()
            }


            sheetView.findViewById<View>(R.id.btnCancelUpload).setOnClickListener {
                bottomSheetDialog.dismiss()
            }

            bottomSheetDialog.show()
        }


        displayProfile()
    }


    private fun createImageFile(): java.io.File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir = getExternalFilesDir("images") // persistent for this app
        return java.io.File.createTempFile("JPEG_${timestamp}_", ".jpg", storageDir)

    }


    private val selectImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            // 1. Display the selected image immediately
            Glide.with(this).load(it).placeholder(R.drawable.ic_profile_default).into(ivProfile)

            // 2. Convert to Base64 (This should be done on a background thread in a real app)
            selectedImageBase64 = contentUriToBase64(it)

            // 3. Save the picture
            if (selectedImageBase64 != null) {
                saveData("Profile Picture", selectedImageBase64!!)
            } else {
                ShowToast.showMessage(this, "Failed to encode image.")
            }
        }
    }

    private fun contentUriToBase64(uri: Uri): String? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes()
            inputStream?.close()

            bytes?.let {
                val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
                val base64String = android.util.Base64.encodeToString(it, android.util.Base64.NO_WRAP)
                "data:$mimeType;base64,$base64String"
            }
        } catch (e: Exception) {
            Log.e("Base64Convert", "Error converting image to Base64", e)
            null
        }
    }


    private fun handleImageResult(uri: Uri) {
        try {
            // Display immediately in ImageView
            Glide.with(this)
                .load(uri)
                .placeholder(R.drawable.ic_profile_default)
                .error(R.drawable.ic_profile_default)
                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(ivProfile)

            // Convert to Base64 for server upload
            selectedImageBase64 = contentUriToBase64(uri)
            selectedImageBase64?.let { saveData("Profile Picture", it) }
                ?: ShowToast.showMessage(this, "Failed to read image")
        } catch (e: Exception) {
            Log.e("ImageError", "Failed to handle image result", e)
            ivProfile.setImageResource(R.drawable.ic_profile_default)
        }
    }

    private fun displayProfile() {
        UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, true)
        ConnectURL.api.getProfile(id).enqueue(object : Callback<ProfileResponse> {
            override fun onResponse(call: Call<ProfileResponse>, response: Response<ProfileResponse>) {
                if (!response.isSuccessful) return
                UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                val body = response.body() ?: return
                if (body.status != "success") return

                val profile = body.data.profile
                val pic = profile.profilePic

                if (!pic.isNullOrEmpty()) {
                    if (pic.startsWith("data:image")) {
                        // Decode Base64
                        try {
                            val pureBase64 = pic.substringAfter("base64,")
                            val decodedBytes = Base64.decode(pureBase64, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                            ivProfile.setImageBitmap(bitmap)
                        } catch (e: Exception) {
                            Log.e("Base64Load", "Failed to decode Base64", e)
                            ivProfile.setImageResource(R.drawable.ic_profile_default)
                        }
                    } else {

                        Glide.with(this@TeacherProfileActivity)
                            .load(pic)
                            .placeholder(R.drawable.ic_profile_default)
                            .circleCrop()
                            .error(R.drawable.ic_profile_default)
                            .into(ivProfile)
                    }
                } else {
                    ivProfile.setImageResource(R.drawable.ic_profile_default)
                }

                // Update other fields
                etFirstName.setText(profile.firstName ?: "")
                etLastName.setText(profile.lastName ?: "")
                val formattedBirthdate = formatDate(profile.birthdate)
                etDob.setText(formattedBirthdate ?: "")
                etTeachingPos.setText(body.data.teachingPosition ?: "", false)
                etGender.setText(profile.gender ?: "", false)
                displayAchievements(body.data.achievements)
            }

            override fun onFailure(call: Call<ProfileResponse>, t: Throwable) {
                UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                Log.e("ProfileError", t.localizedMessage ?: "")
                ShowToast.showMessage(this@TeacherProfileActivity, "Failed to load profile")
            }
        })
    }

    private fun updateField(fieldName: String, newValue: String) {
        val request = UpdateProfileRequest(
            userId = id,
            firstName = if (fieldName == "first_name") newValue else null,
            lastName = if (fieldName == "last_name") newValue else null,
            gender = if (fieldName == "gender") newValue else null,
            birthdate = if (fieldName == "birthdate") newValue else null,
            teachingPosition = if (fieldName == "teaching_position") newValue else null,
            profilePic = if (fieldName == "profilePic") newValue else null
        )

        ConnectURL.api.updateProfile(request).enqueue(object : Callback<ProfileResponse> {
            override fun onResponse(call: Call<ProfileResponse>, response: Response<ProfileResponse>) {
                if (response.isSuccessful && response.body()?.status == "success") {
                    ShowToast.showMessage(this@TeacherProfileActivity, "Change successfully saved!")
                    selectedImageBase64 = null

                    // Only refresh profile if NOT updating profile picture
                    if (fieldName != "profile_pic") {
                        displayProfile()
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("UpdateProfileError", "code:${response.code()} body:$errorBody")
                    ShowToast.showMessage(this@TeacherProfileActivity, "Update failed: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<ProfileResponse>, t: Throwable) {
                ShowToast.showMessage(this@TeacherProfileActivity, "Server error")
            }
        })
    }


    private fun displayAchievements(list: List<TeacherAchievement>) {

        if (list.isEmpty()) {
            tvAchievementsList.text = "No achievements yet"
            tvAchievementsList.gravity = Gravity.CENTER
            ivCertificatePreview.visibility = View.GONE
            return
        }

        val builder = StringBuilder()

        list.forEach { ach ->
            builder.append("• ${ach.description}\n")
            builder.append("   Year: ${ach.dateAcquired}\n\n")
        }

        tvAchievementsList.text = builder.toString().trim()
        tvAchievementsList.gravity = Gravity.START

        // DISPLAY THE FIRST CERTIFICATE IMAGE (or last)
        val first = list.firstOrNull()

        if (!first?.certificate.isNullOrEmpty()) {
            ivCertificatePreview.visibility = View.VISIBLE

            Glide.with(this)
                .load(first!!.certificate)
                .into(ivCertificatePreview)

        } else {
            ivCertificatePreview.visibility = View.GONE
        }
    }

    // In ProfileActivity.kt

    private fun formatDate(timestamp: String?): String? {
        if (timestamp.isNullOrEmpty()) return null

        // This input pattern should match yyyy-MM-dd HH:mm:ss.SSSS
        // If your server returns 'T' and 'Z', use: yyyy-MM-dd'T'HH:mm:ss.SSSS'Z'
        // Since you didn't show the exact input here, let's keep it close to your original logic:
        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSS", Locale.US)
        inputFormat.timeZone = TimeZone.getTimeZone("UTC") // Input is typically UTC

        return try {
            val date: Date? = inputFormat.parse(timestamp)
            date?.let {
                // --- FIX: Ensure the final output is simple date-only ---
                val outputFormat = SimpleDateFormat("MM/dd/yyyy", Locale.US) // Simple Date
                outputFormat.format(it)
            }
        } catch (e: Exception) {
            // If the S.SSSS format fails, try the date-only format (yyyy-MM-dd)
            try {
                val simpleInputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val simpleDate: Date? = simpleInputFormat.parse(timestamp)
                simpleDate?.let {
                    val outputFormat = SimpleDateFormat("MM/dd/yyyy", Locale.US)
                    outputFormat.format(it)
                }
            } catch (e2: Exception) {
                Log.e("DateFormatter", "Failed to parse date: $timestamp", e2)
                timestamp // Return unformatted as fallback
            }
        }
    }

    // In TeacherProfileActivity.kt

// REMOVE THIS CLASS VARIABLE:
// private var tempCertificateLink: String? = null

    private fun showAddAchievementDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_achievement, null)
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        val etAchievementName = dialogView.findViewById<TextInputEditText>(R.id.etAchievementName) // Assuming this ID exists
        val etYear = dialogView.findViewById<TextInputEditText>(R.id.etYearReceived)
         val btnUpload = dialogView.findViewById<View>(R.id.btnUploadCertificate) // REMOVE REFERENCE
        btnUpload.visibility = View.GONE

        val btnSave = dialogView.findViewById<View>(R.id.btnSaveAchievement)
        val btnClose = dialogView.findViewById<View>(R.id.btnCloseDialog)

        // 1. Year Picker
        etYear.setOnClickListener {
            showYearPickerDialog(etYear)
        }

        // 2. Remove btnUpload listener here. (The button should be removed from XML)

        // 3. Save Logic
        btnSave.setOnClickListener {
            val name = etAchievementName.text.toString().trim()
            val year = etYear.text.toString().trim()

            if(name.isEmpty() || year.isEmpty()) {
                Toast.makeText(this, "Please enter achievement name and year", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Pass essential data (name and year) to the confirmation dialog
            showSaveConfirmation(dialog, name, year)
        }

        btnClose.setOnClickListener { dialog.dismiss() }

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }

    // --- Year-Only Picker ---
    private fun showYearPickerDialog(editText: EditText) {
        val themedContext = android.view.ContextThemeWrapper(this, R.style.Theme_Gr8Math)

        val numberPicker = android.widget.NumberPicker(themedContext)
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        numberPicker.minValue = 1980
        numberPicker.maxValue = currentYear + 10
        numberPicker.value = currentYear
        numberPicker.wrapSelectorWheel = false
        numberPicker.descendantFocusability = android.widget.NumberPicker.FOCUS_BLOCK_DESCENDANTS

        val titleView = TextView(this).apply {
            text = "Select Year"
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            setTextColor(ContextCompat.getColor(context, R.color.colorMatisse))
            setPadding(0, 40, 0, 20)
            try { typeface = ResourcesCompat.getFont(context, R.font.lexend) } catch (_: Exception) { }
        }

        try {
            val count = numberPicker.childCount
            for (i in 0 until count) {
                val child = numberPicker.getChildAt(i)
                if (child is EditText) {
                    child.setTextColor(ContextCompat.getColor(this, R.color.colorText))
                    child.textSize = 18f
                    child.typeface = ResourcesCompat.getFont(this, R.font.lexend)
                    child.isFocusable = false
                }
            }
        } catch (e: Exception) { }

        val pickerDialog = MaterialAlertDialogBuilder(this)
            .setCustomTitle(titleView)
            .setView(numberPicker)
            .setPositiveButton("OK") { _, _ ->
                editText.setText(numberPicker.value.toString())
            }
            // FIX: Changed "Cancel" to "CANCEL" here
            .setNegativeButton("CANCEL", null)
            .create()

        pickerDialog.show()

        pickerDialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)?.apply {
            setTextColor(ContextCompat.getColor(context, R.color.colorMatisse))
            typeface = ResourcesCompat.getFont(context, R.font.lexend)
            setTypeface(typeface, Typeface.BOLD)
        }

        pickerDialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE)?.apply {
            setTextColor(ContextCompat.getColor(context, R.color.colorRed))
            typeface = ResourcesCompat.getFont(context, R.font.lexend)
            setTypeface(typeface, Typeface.BOLD)
        }
    }

    // --- Upload Source (Bottom Sheet) ---
    private fun showUploadSourceDialog() {
        val sheetView = LayoutInflater.from(this).inflate(R.layout.dialog_upload_certificate_source, null)
        val bottomSheetDialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        bottomSheetDialog.setContentView(sheetView)

        (sheetView.parent as? View)?.setBackgroundColor(Color.TRANSPARENT)

        sheetView.findViewById<View>(R.id.btnPhotoAlbum).setOnClickListener {
            Toast.makeText(this, "Opening Photo Album...", Toast.LENGTH_SHORT).show()
            bottomSheetDialog.dismiss()
        }

        sheetView.findViewById<View>(R.id.btnCamera).setOnClickListener {
            Toast.makeText(this, "Opening Camera...", Toast.LENGTH_SHORT).show()
            bottomSheetDialog.dismiss()
        }

        sheetView.findViewById<View>(R.id.btnCancelUpload).setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.show()
    }

    // --- Save Confirmation Dialog ---
    private fun showSaveConfirmation(parentDialog: androidx.appcompat.app.AlertDialog,
                                     name: String,
                                     year: String,  ) {
        val messageView = TextView(this).apply {
            text = "Are you sure you want to save\nthis achievements?"
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            // Left aligned by default (Removed Gravity.CENTER)
            setTextColor(ContextCompat.getColor(context, R.color.colorText))
            setPadding(70, 60, 50, 10)
            try { typeface = ResourcesCompat.getFont(context, R.font.lexend) } catch (_: Exception) { }
        }

        val confirmDialog = MaterialAlertDialogBuilder(this)
            .setView(messageView)

            // FIX: SWAPPED POSITIONS
            // "No" is now the Positive Button (Right Side)
            .setPositiveButton("No") { dialogInterface, _ ->
                dialogInterface.dismiss() // Just dismiss
            }
            // "Yes" is now the Negative Button (Left Side)
            .setNegativeButton("Yes") { dialogInterface, _ ->
                // Actual Saving Logic
                updateAchievement(
                    achievementName = name,
                    yearAcquired = year,
                    certificateFile = null, // *** CERTIFICATE IS EXPLICITLY NULL ***
                    onSuccess = {
                        displayProfile()
                        dialogInterface.dismiss()
                        parentDialog.dismiss()
                    }
                )
                ShowToast.showMessage(this@TeacherProfileActivity, "Achievement Saved!")
                dialogInterface.dismiss() // Close confirmation
                parentDialog.dismiss()    // Close main dialog
            }
            .create()

        confirmDialog.show()

        // FIX: Ensure both are Red
        confirmDialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(this, R.color.colorRed))
        confirmDialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE)?.setTextColor(ContextCompat.getColor(this, R.color.colorRed))
    }

    private fun setEditMode(editText: EditText, iconView: ImageView, isEditing: Boolean) {
        editText.isEnabled = isEditing
        editText.isFocusable = isEditing
        editText.isFocusableInTouchMode = isEditing

        if (isEditing) {
            iconView.setImageResource(R.drawable.ic_check)
            iconView.setColorFilter(ContextCompat.getColor(this, R.color.colorDarkCyan))
        } else {
            iconView.setImageResource(R.drawable.ic_edit)
            iconView.setColorFilter(ContextCompat.getColor(this, R.color.colorDarkCyan))
        }
    }

    private fun saveData(field: String, value: String) {
        when (field) {
            "First Name" -> updateField("first_name", value)
            "Last Name" -> updateField("last_name", value)
            "Gender" -> updateField("gender", value)
            "Birthdate" -> updateField("birthdate", value)
            "LRN" -> updateField("LRN", value)
            "Profile Picture" -> updateField("profilePic", value) // FIXED
            "Teaching Position" -> updateField("teaching_position", value)
        }
    }



    private fun showKeyboard(view: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun updateAchievement(
        achievementName: String,
        yearAcquired: String,
        certificateFile: String?,
        onSuccess: () -> Unit
    ) {
        val achievement = TeacherAchievement(
            id = 0,
            description = achievementName,
            dateAcquired = yearAcquired,
            certificate = certificateFile
        )
        val request = UpdateProfileRequest(
            userId = CurrentCourse.userId,
            achievements = listOf(achievement)
        )

        ConnectURL.api.updateProfile(request).enqueue(object : retrofit2.Callback<ProfileResponse> {
            override fun onResponse(
                call: retrofit2.Call<ProfileResponse>,
                response: retrofit2.Response<ProfileResponse>
            ) {
                if (response.isSuccessful && response.body()?.status == "success") {
                    Log.i("AchievementUpdate", "SUCCESS: Achievement successfully saved.")
                    onSuccess()
                } else {
                    // --- Logging the Error Response Body ---
                    val errorBody = response.errorBody()?.string()
                    val code = response.code()

                    // Log the full HTTP error code and body content
                    Log.e("AchievementUpdate", "API FAILED. Code: $code, Body: $errorBody")

                    // Check if the error body is valid JSON to extract a clean message
                    try {
                        val errorJson = org.json.JSONObject(errorBody ?: "{}")
                        val errorMessage = errorJson.optString("error", "Failed to save achievement.")
                        ShowToast.showMessage(this@TeacherProfileActivity, errorMessage)
                    } catch (e: Exception) {
                        ShowToast.showMessage(this@TeacherProfileActivity, "Update failed (Error $code). Check logs.")
                    }
                    // ----------------------------------------
                }
            }

            override fun onFailure(call: retrofit2.Call<ProfileResponse>, t: Throwable) {
                Log.e("AchievementUpdate", "NETWORK FAILED: ${t.localizedMessage}", t)
                ShowToast.showMessage(this@TeacherProfileActivity, "Server error: Check connection or logs.")
            }
        })


    }



}
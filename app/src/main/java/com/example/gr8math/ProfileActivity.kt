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
import com.example.gr8math.api.ConnectURL
import com.example.gr8math.dataObject.CurrentCourse
import com.example.gr8math.dataObject.ProfileResponse
import com.example.gr8math.dataObject.StudentProfileResponse
import com.example.gr8math.dataObject.UpdateProfileRequest
import com.example.gr8math.dataObject.UpdateStudentProfileRequest
import com.example.gr8math.utils.ShowToast
import com.example.gr8math.utils.UIUtils
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class ProfileActivity : AppCompatActivity() {
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

    private lateinit var etLRN: TextInputEditText
    private lateinit var ivEditLRN: ImageView

    private lateinit var etDob: TextInputEditText
    private lateinit var etGender: MaterialAutoCompleteTextView
    private lateinit var ivProfile: ImageView

    private lateinit var editPfp : ImageView
    private lateinit var tvChangePassword: TextView


    // Badge Edit Icon
    private lateinit var ivEditBadges: ImageView

    // State trackers for edit mode
    private var isEditingFirstName = false
    private var isEditingLastName = false
    private var isEditingLRN = false

    private var id = CurrentCourse.userId
    private var selectedImageBase64: String? = null
    private lateinit var genderAdapter: ArrayAdapter<String>




    // FIX 1: Use 'by lazy' to prevent crash on startup (getString requires Context)
    private val allBadges by lazy {
        listOf(
            Badge(1, getString(R.string.badge_first_ace_list_title), getString(R.string.badge_first_ace_dialog_title), getString(R.string.badge_first_ace_desc), getString(R.string.badge_date_placeholder), R.drawable.badge_firstace, true),
            Badge(2, getString(R.string.badge_first_timer_list_title), getString(R.string.badge_first_timer_dialog_title), getString(R.string.badge_first_timer_desc), getString(R.string.badge_date_placeholder), R.drawable.badge_firsttimer, true),
            Badge(3, getString(R.string.badge_first_escape_list_title), getString(R.string.badge_first_escape_dialog_title), getString(R.string.badge_first_escape_desc), getString(R.string.badge_date_locked), R.drawable.badge_firstescape, false),
            Badge(4, getString(R.string.badge_perfect_escape_list_title), getString(R.string.badge_perfect_escape_dialog_title), getString(R.string.badge_perfect_escape_desc), getString(R.string.badge_date_placeholder), R.drawable.badge_perfectescape, true),
            Badge(5, getString(R.string.badge_first_explo_list_title), getString(R.string.badge_first_explo_dialog_title), getString(R.string.badge_first_explo_desc), getString(R.string.badge_date_locked), R.drawable.badge_firstexplo, false),
            Badge(6, getString(R.string.badge_full_explo_list_title), getString(R.string.badge_full_explo_dialog_title), getString(R.string.badge_full_explo_desc), getString(R.string.badge_date_placeholder), R.drawable.badge_fullexplo, true),
            Badge(7, getString(R.string.badge_three_quarter_list_title), getString(R.string.badge_three_quarter_dialog_title), getString(R.string.badge_three_quarter_desc), getString(R.string.badge_date_locked), R.drawable.badge_threequarter, false),
            Badge(8, getString(R.string.badge_triple_ace_list_title), getString(R.string.badge_triple_ace_dialog_title), getString(R.string.badge_triple_ace_desc), getString(R.string.badge_date_placeholder), R.drawable.badge_tripleace, true)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_profile)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        // --- Find Views ---
        etFirstName = findViewById(R.id.etFirstName)
        ivEditFirstName = findViewById(R.id.ivEditFirstName)

        etLastName = findViewById(R.id.etLastName)
        ivEditLastName = findViewById(R.id.ivEditLastName)

        etLRN = findViewById(R.id.etLRN)
        ivEditLRN = findViewById(R.id.ivEditLRN)
        editPfp = findViewById(R.id.editPfp)
        etDob = findViewById(R.id.etDob)
        etGender = findViewById(R.id.etGender)
        ivProfile = findViewById(R.id.ivProfile)
        tvChangePassword = findViewById(R.id.tvChangePassword)
        loadingLayout =  findViewById<View>(R.id.loadingLayout)
        loadingProgress = findViewById<View>(R.id.loadingProgressBg)
        loadingText = findViewById<TextView>(R.id.loadingText)


        // IMPORTANT: Ensure this ID exists in your XML layout inside the Badges card
        ivEditBadges = findViewById(R.id.ivEditBadges)

        // --- Initialize View Mode ---
        setEditMode(etFirstName, ivEditFirstName, false)
        setEditMode(etLastName, ivEditLastName, false)
        setEditMode(etLRN, ivEditLRN, false)

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

        ivEditLRN.setOnClickListener {
            isEditingLRN = !isEditingLRN
            if (isEditingLRN) {
                setEditMode(etLRN, ivEditLRN, true)
                etLRN.requestFocus()
                showKeyboard(etLRN)
            } else {
                setEditMode(etLRN, ivEditLRN, false)
                saveData("LRN", etLRN.text.toString())
            }
        }

        // --- Badge Selection Dialog Click ---
        ivEditBadges.setOnClickListener {
            showBadgeSelectionDialog()
        }

        tvChangePassword.setOnClickListener {
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            intent.putExtra("EXTRA_ROLE", "Teacher")
            startActivity(intent)
        }

        val genderItems = listOf("Male", "Female")
        genderAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, genderItems)
        etGender.setAdapter(genderAdapter)

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


    private fun handleImageResult(uri: Uri) {
        try {
            // Display immediately in ImageView
            Glide.with(this)
                .load(uri)
                .placeholder(R.drawable.ic_profile_default)
                .circleCrop()
                .error(R.drawable.ic_profile_default)
                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(ivProfile)

            // Convert to Base64 for server upload
            selectedImageBase64 = contentUriToBase64(uri)
            selectedImageBase64?.let { saveData("Profile Picture", it) }
                ?: ShowToast.showMessage(this, "Failed to read image")
        } catch (e: Exception) {

            ivProfile.setImageResource(R.drawable.ic_profile_default)
        }
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

            null
        }
    }


    private fun displayProfile() {
        UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, true)
        ConnectURL.api.getStudentProfile(id).enqueue(object : Callback<StudentProfileResponse> {
            override fun onResponse(call: Call<StudentProfileResponse>, response: Response<StudentProfileResponse>) {
                if (!response.isSuccessful) return
                UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                val data = response.body()?.data ?: return
                val profile = data.profile

                val pic = profile.profilePic

                // --- PROFILE PIC LOADING ---
                if (!pic.isNullOrEmpty()) {
                    if (pic.startsWith("data:image")) {
                        try {
                            val pureBase64 = pic.substringAfter("base64,")
                            val decodedBytes = Base64.decode(pureBase64, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                            ivProfile.setImageBitmap(bitmap)
                        }
                        catch (e: Exception) {

                            ivProfile.setImageResource(R.drawable.ic_profile_default)
                        }
                    } else {
                        Glide.with(this@ProfileActivity)
                            .load(pic)
                            .circleCrop()
                            .placeholder(R.drawable.ic_profile_default)
                            .error(R.drawable.ic_profile_default)
                            .into(ivProfile)
                    }
                }
                else {
                    ivProfile.setImageResource(R.drawable.ic_profile_default)
                }

                // --- STUDENT INFO ---
                etFirstName.setText(profile.firstName ?: "")
                etLastName.setText(profile.lastName ?: "")
                etGender.setText(profile.gender ?: "", false)
                etDob.setText(formatDate(profile.birthdate) ?: "")
                etLRN.setText(data.lrn ?: "")
            }

            override fun onFailure(call: Call<StudentProfileResponse>, t: Throwable) {
                ShowToast.showMessage(this@ProfileActivity, "Failed to load profile")
                UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
            }
        })
    }



    private fun updateField(fieldName: String, value: String) {

        val request = UpdateStudentProfileRequest(
            userId = id,
            firstName = if (fieldName == "first_name") value else null,
            lastName = if (fieldName == "last_name") value else null,
            gender = if (fieldName == "gender") value else null,
            birthdate = if (fieldName == "birthdate") value else null,
            lrn = if (fieldName == "LRN") value else null,
            profilePic = if (fieldName == "profile_pic") value else null,
        )

        ConnectURL.api.updateStudentProfile(request).enqueue(object : Callback<StudentProfileResponse> {
            override fun onResponse(call: Call<StudentProfileResponse>, response: Response<StudentProfileResponse>) {
                if (response.isSuccessful && response.body()?.status == "success") {
                    ShowToast.showMessage(this@ProfileActivity, "Change successfully saved!")
                }
            }

            override fun onFailure(call: Call<StudentProfileResponse>, t: Throwable) {
                ShowToast.showMessage(this@ProfileActivity, "Server error")
            }
        })
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

                timestamp // Return unformatted as fallback
            }
        }
    }
    // --- Show Badge Selection Dialog with Confirmation ---
    private fun showBadgeSelectionDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_select_badges, null)
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create()

        // 1. Filter for ACQUIRED badges only
        val acquiredBadges = allBadges.filter { it.isAcquired }

        // 2. Setup RecyclerView
        val rvSelection = dialogView.findViewById<RecyclerView>(R.id.rvBadgeSelection)
        rvSelection.layoutManager = LinearLayoutManager(this)

        // Pass the filtered list to the adapter
        val adapter = BadgeSelectionAdapter(acquiredBadges)
        rvSelection.adapter = adapter

        // 3. Setup Buttons
        dialogView.findViewById<View>(R.id.btnClose).setOnClickListener {
            dialog.dismiss()
        }

        // FIX 2: Intercept Save Click to show Custom Confirmation Dialog
        dialogView.findViewById<View>(R.id.btnSaveSelection).setOnClickListener {
            val selected = adapter.selectedBadges

            // Validation
            if (selected.isEmpty()) {
                Toast.makeText(this, "No badges selected to display", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Create Custom Message View (Align Left, Bold)
            val messageView = TextView(this).apply {
                text = "Are you sure you want to save\nthis badges?"
                textSize = 16f
                setTypeface(null, Typeface.BOLD)
                // gravity = Gravity.CENTER // REMOVED THIS to keep standard alignment
                setTextColor(ContextCompat.getColor(context, R.color.colorText))
                setPadding(70, 60, 50, 10) // Adjusted padding
                try {
                    typeface = ResourcesCompat.getFont(context, R.font.lexend)
                } catch (_: Exception) { }
            }

            // Build Confirmation Dialog
            val confirmDialog = MaterialAlertDialogBuilder(this)
                .setView(messageView)

                // SWAPPED: "No" is now Positive (Right), "Yes" is now Negative (Left)
                .setPositiveButton("No") { dialogInterface, _ ->
                    dialogInterface.dismiss() // Just close confirmation
                }
                .setNegativeButton("Yes") { dialogInterface, _ ->
                    // --- SAVE LOGIC ---
                    Toast.makeText(this, "Displaying ${selected.size} badges on profile", Toast.LENGTH_SHORT).show()
                    // TODO: Update backend/UI with 'selected' list

                    dialogInterface.dismiss() // Close confirmation
                    dialog.dismiss()          // Close selection list
                }
                .create()

            confirmDialog.show()

            // Set Buttons to Red
            confirmDialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(this, R.color.colorRed))
            confirmDialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE)?.setTextColor(ContextCompat.getColor(this, R.color.colorRed))
        }

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }

    private fun setEditMode(editText: EditText, iconView: ImageView, isEditing: Boolean) {
        editText.isEnabled = isEditing
        editText.isFocusable = isEditing
        editText.isFocusableInTouchMode = isEditing

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
            "First Name" -> updateField("first_name", value)
            "Last Name" -> updateField("last_name", value)
            "Gender" -> updateField("gender", value)
            "Birthdate" -> updateField("birthdate", value)
            "LRN" -> updateField("LRN", value)
            "Profile Picture" -> updateField("profile_pic", value)
        }
    }


    private fun showKeyboard(view: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }
}
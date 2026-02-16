package com.example.gr8math.Activity.TeacherModule.Lesson

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ImageSpan
import android.util.Base64
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.gr8math.Model.CurrentCourse
import com.example.gr8math.R
import com.example.gr8math.Utils.ShowToast
import com.example.gr8math.Utils.UIUtils
import com.example.gr8math.ViewModel.LessonContentViewModel
import com.example.gr8math.ViewModel.LessonState
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.regex.Pattern

class LessonContentActivity : AppCompatActivity() {

    // MVVM
    private val viewModel: LessonContentViewModel by viewModels()

    private lateinit var etLessonContent: EditText
    private var weekNumber: String? = null
    private var lessonTitle: String? = null
    private var courseId: Int = 0
    private var lessonId: Int = 0

    lateinit var loadingLayout: View
    lateinit var loadingProgress: View
    lateinit var loadingText: TextView

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) insertImageFromUri(uri, etLessonContent)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lesson_content)

        // Init Data
        courseId = CurrentCourse.courseId
        weekNumber = intent.getStringExtra("EXTRA_WEEK_NUMBER")
        lessonTitle = intent.getStringExtra("EXTRA_LESSON_TITLE")
        lessonId = intent.getIntExtra("EXTRA_LESSON_ID", 0)

        initViews()
        setupListeners()
        setupObservers() // Connect to ViewModel

        // Load data if editing
        if (lessonId > 0) {
            viewModel.loadLesson(lessonId)
        }
    }

    private fun initViews() {
        etLessonContent = findViewById(R.id.etLessonContent)
        loadingLayout = findViewById(R.id.loadingLayout)
        loadingProgress = findViewById(R.id.loadingProgressBg)
        loadingText = findViewById(R.id.loadingText)
    }

    private fun setupListeners() {
        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        val btnAddMedia: MaterialButton = findViewById(R.id.btnAddMedia)
        val btnSave: MaterialButton = findViewById(R.id.btnSave)

        btnSave.setOnClickListener { showSaveConfirmationDialog() }
        btnAddMedia.setOnClickListener { pickImageLauncher.launch("image/*") }

        toolbar.setNavigationOnClickListener { checkUnsavedContentAndGoBack() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { checkUnsavedContentAndGoBack() }
        })
    }

    // --- VIEWMODEL CONNECTION ---
    private fun setupObservers() {
        viewModel.state.observe(this) { state ->
            when (state) {
                is LessonState.Loading -> {
                    UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, true)
                }
                is LessonState.Saved -> {
                    UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                    ShowToast.showMessage(this, "Lesson saved successfully!")
                    // Return OK to previous screen to trigger refresh
                    setResult(RESULT_OK)
                    finish()
                }
                is LessonState.ContentLoaded -> {
                    UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                    // FIX: Access content via state.lesson.lessonContent
                    displayLessonContent(state.lesson.lessonContent)
                }
                is LessonState.Error -> {
                    UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                    ShowToast.showMessage(this, state.message)
                }
                is LessonState.Idle -> {
                    UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                }
            }
        }
    }

    private fun showSaveConfirmationDialog() {
        val customMessage = TextView(this).apply {
            text = getString(R.string.dialog_save_message)
            setTextColor(ContextCompat.getColor(this@LessonContentActivity, R.color.colorText))
            textSize = 18f
            setPadding(60, 50, 60, 30)
            try { typeface = ResourcesCompat.getFont(this@LessonContentActivity, R.font.lexend) } catch (_: Exception) {}
        }
        MaterialAlertDialogBuilder(this)
            .setCustomTitle(customMessage)
            .setNegativeButton(R.string.yes) { _, _ ->
                processAndSave() // Trigger logic
            }
            .setPositiveButton(R.string.no) { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun processAndSave() {
        // 1. Convert Rich Text (Spans) to String (with Base64)
        val finalContent = buildLessonContentWithBase64(etLessonContent)

        // 2. Send to ViewModel
        if (lessonId > 0) {
            viewModel.updateLesson(lessonId, courseId, weekNumber ?: "0", lessonTitle ?: "", finalContent)
        } else {
            viewModel.saveLesson(courseId, weekNumber ?: "0", lessonTitle ?: "", finalContent)
        }
    }

    // --- NAVIGATION LOGIC ---
    private fun checkUnsavedContentAndGoBack() {
        val content = etLessonContent.text.toString().trim()
        if (content.isNotEmpty()) showDiscardChangesDialog() else goBackToStep1()
    }

    private fun showDiscardChangesDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.discard_title)
            .setMessage(R.string.discard_message)
            .setNegativeButton(R.string.discard_action) { _, _ -> goBackToStep1() }
            .setPositiveButton(R.string.cancel_action) { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun goBackToStep1() {
        setResult(RESULT_CANCELED)
        finish()
    }

    // =========================================================================
    // RICH TEXT EDITOR LOGIC
    // =========================================================================

    private fun displayLessonContent(content: String) {
        etLessonContent.text.clear()
        val builder = SpannableStringBuilder()
        val maxDim = 800

        val regex = ("(data:image/[^;]+;base64,[A-Za-z0-9+/=]+)|" +
                "(https?://[^\\s\"'<>]+\\.(?:png|jpg|jpeg|gif|webp))")
        val pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(content)

        var lastEnd = 0

        while (matcher.find()) {
            val start = matcher.start()
            if (start > lastEnd) builder.append(content.substring(lastEnd, start))

            val base64Part = matcher.group(1)
            val urlPart = matcher.group(2)

            if (!base64Part.isNullOrEmpty()) {
                try {
                    val bytes = Base64.decode(base64Part.substringAfter("base64,"), Base64.DEFAULT)
                    val optsBounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, optsBounds)
                    val opts = BitmapFactory.Options()
                    opts.inSampleSize = calculateInSampleSize(optsBounds, maxDim, maxDim)
                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)?.let { resizeBitmap(it, maxDim) }

                    if (bmp != null) {
                        val spanStart = builder.length
                        builder.append("\uFFFC\n\n")
                        builder.setSpan(ImageSpan(this, bmp), spanStart, spanStart + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
                } catch (e: Exception) { }
            } else if (!urlPart.isNullOrEmpty()) {
                val spanStart = builder.length
                builder.append("\uFFFC\n\n")
                etLessonContent.text = builder // Set temporarily

                Glide.with(this).asBitmap().load(urlPart).into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        try {
                            val bmp = resizeBitmap(resource, maxDim)
                            runOnUiThread {
                                val text = etLessonContent.text
                                if (text is Spannable) {
                                    val placeholderIndex = text.toString().indexOf('\uFFFC', spanStart) // Search near insertion
                                    if (placeholderIndex >= 0) {
                                        text.setSpan(ImageSpan(this@LessonContentActivity, bmp), placeholderIndex, placeholderIndex + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                                    }
                                }
                            }
                        } catch (e: Exception) { }
                    }
                    override fun onLoadCleared(placeholder: Drawable?) {}
                })
            }
            lastEnd = matcher.end()
        }

        if (lastEnd < content.length) builder.append(content.substring(lastEnd))
        etLessonContent.text = builder
    }

    private fun buildLessonContentWithBase64(editText: EditText): String {
        val sb = StringBuilder()
        val text = editText.text
        if (text !is Spannable) return text.toString()

        val spans = text.getSpans(0, text.length, ImageSpan::class.java)
        if (spans.isEmpty()) return text.toString()

        val sortedSpans = spans.map { Pair(text.getSpanStart(it), it) }.sortedBy { it.first }
        var cursor = 0

        for ((start, span) in sortedSpans) {
            if (start > cursor) sb.append(text.subSequence(cursor, start).toString())
            val bmp = span.drawable?.toBitmap()
            if (bmp != null) {
                sb.append(bitmapToBase64(resizeBitmap(bmp, 800)))
                sb.append("\n\n")
            }
            cursor = text.getSpanEnd(span)
        }

        if (cursor < text.length) sb.append(text.subSequence(cursor, text.length).toString())
        return sb.toString()
    }

    private fun insertImageFromUri(uri: Uri, editText: EditText) {
        Glide.with(this).asBitmap().load(uri).override(1600, 1600)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    CoroutineScope(Dispatchers.Main).launch {
                        try {
                            val bmp = resizeBitmap(resource, 800)
                            val spannable: Editable = editText.text
                            val cursor = editText.selectionStart.coerceAtLeast(0)
                            spannable.insert(cursor, "\uFFFC\n\n")
                            spannable.setSpan(ImageSpan(this@LessonContentActivity, bmp), cursor, cursor + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                }
                override fun onLoadCleared(placeholder: Drawable?) {}
            })
    }

    private fun resizeBitmap(bitmap: Bitmap, maxDim: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDim && height <= maxDim) return bitmap
        val aspect = width.toFloat() / height.toFloat()
        val (newW, newH) = if (aspect >= 1f) Pair(maxDim, (maxDim / aspect).toInt()) else Pair((maxDim * aspect).toInt(), maxDim)
        return Bitmap.createScaledBitmap(bitmap, newW, newH, true)
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 90, outputStream)
        return "data:image/png;base64," + Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height, width) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) inSampleSize *= 2
        }
        return inSampleSize
    }
}
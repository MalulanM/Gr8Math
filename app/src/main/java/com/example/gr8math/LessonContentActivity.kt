package com.example.gr8math

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ImageSpan
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.gr8math.api.ConnectURL
import com.example.gr8math.dataObject.CurrentCourse
import com.example.gr8math.utils.ShowToast
import com.example.gr8math.utils.UIUtils
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.regex.Pattern

class LessonContentActivity : AppCompatActivity() {

    private lateinit var etLessonContent: EditText
    private var weekNumber: String? = null
    private var lessonTitle: String? = null
    private var id : Int = 0
    private var courseId : Int = 0
    private var lessonId: Int = 0

    lateinit var loadingLayout : View
    lateinit var loadingProgress : View
    lateinit var loadingText : TextView

    private val pickImageLauncher =
        registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) insertImageFromUri(uri, etLessonContent)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lesson_content)

        id = CurrentCourse.userId
        weekNumber = intent.getStringExtra("EXTRA_WEEK_NUMBER")
        lessonTitle = intent.getStringExtra("EXTRA_LESSON_TITLE")
        courseId = CurrentCourse.courseId

        etLessonContent = findViewById(R.id.etLessonContent)
        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        val btnAddMedia: MaterialButton = findViewById(R.id.btnAddMedia)
        val btnSave: MaterialButton = findViewById(R.id.btnSave)
        loadingLayout = findViewById(R.id.loadingLayout)
        loadingProgress = findViewById(R.id.loadingProgressBg)
        loadingText = findViewById(R.id.loadingText)

        lessonId = intent.getIntExtra("EXTRA_LESSON_ID", 0)
        if (lessonId > 0) loadExistingLesson(lessonId)

        btnSave.setOnClickListener { showSaveConfirmationDialog() }
        btnAddMedia.setOnClickListener { pickImageLauncher.launch("image/*") }

        toolbar.setNavigationOnClickListener { checkUnsavedContentAndGoBack() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { checkUnsavedContentAndGoBack() }
        })
    }

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
        val resultIntent = Intent()
        resultIntent.putExtra("EXTRA_WEEK_NUMBER", weekNumber)
        resultIntent.putExtra("EXTRA_LESSON_TITLE", lessonTitle)
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
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
            .setNegativeButton(R.string.yes) { _, _ -> if (lessonId > 0) updateLesson() else saveLesson() }
            .setPositiveButton(R.string.no) { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun saveLesson() {
        UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, true)
        val textRaw = etLessonContent.text
        if (textRaw.isNullOrBlank()) {
            Toast.makeText(this, "Cannot save an empty lesson", Toast.LENGTH_SHORT).show()
            UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
            return
        }

        val lessonContentCombined = buildLessonContentWithBase64(etLessonContent)
        val courseIdBody = textToPart(courseId.toString())
        val weekBody = textToPart(weekNumber ?: "")
        val titleBody = textToPart(lessonTitle ?: "")
        val contentBody = textToPart(lessonContentCombined)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ConnectURL.api.storeLesson(
                    courseId = courseIdBody,
                    weekNumber = weekBody,
                    lessonTitle = titleBody,
                    lessonContent = contentBody
                )

                withContext(Dispatchers.Main) {
                    UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                    val responseString = response.body()?.string() ?: response.errorBody()?.string()
                    if (response.isSuccessful && !responseString.isNullOrEmpty()) {
                        val jsonObj = JSONObject(responseString)
                        val success = jsonObj.optBoolean("success", false)
                        val message = jsonObj.optString("message", "No message")
                        val newLessonId = jsonObj.optInt("lesson_id", 0)
                        if (success) {
                            ShowToast.showMessage(this@LessonContentActivity, message)
                            finish()
                        } else {
                            ShowToast.showMessage(this@LessonContentActivity, "Failed: $message")
                        }
                    } else {
                        ShowToast.showMessage(this@LessonContentActivity, "Upload failed")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                    ShowToast.showMessage(this@LessonContentActivity, "Failed to connect. Check your internet connection.")
                }
            }
        }
    }

    private fun updateLesson() {
        UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, true)
        val textRaw = etLessonContent.text
        if (textRaw.isNullOrBlank()) {
            Toast.makeText(this, "Cannot save an empty lesson", Toast.LENGTH_SHORT).show()
            UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
            return
        }

        val lessonContentCombined = buildLessonContentWithBase64(etLessonContent)
        val courseIdBody = textToPart(courseId.toString())
        val lessonIdBody = textToPart(lessonId.toString())
        val weekBody = textToPart(weekNumber ?: "")
        val titleBody = textToPart(lessonTitle ?: "")
        val contentBody = textToPart(lessonContentCombined)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ConnectURL.api.updateLesson(
                    lessonId = lessonIdBody,
                    courseId = courseIdBody,
                    weekNumber = weekBody,
                    lessonTitle = titleBody,
                    lessonContent = contentBody
                )

                withContext(Dispatchers.Main) {
                    UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                    val responseString = response.body()?.string() ?: response.errorBody()?.string()
                    if (response.isSuccessful && !responseString.isNullOrEmpty()) {
                        val jsonObj = JSONObject(responseString)
                        val success = jsonObj.optBoolean("success", false)
                        val message = jsonObj.optString("message", "No message")
                        if (success) {
                            ShowToast.showMessage(this@LessonContentActivity, message)
                            finish()
                        } else {
                            ShowToast.showMessage(this@LessonContentActivity, "Failed: $message")
                        }
                    } else {
                        ShowToast.showMessage(this@LessonContentActivity, "Update failed")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                    ShowToast.showMessage(this@LessonContentActivity, "Failed to connect. Check your internet connection.")
                }
            }
        }
    }

    private fun loadExistingLesson(lessonId: Int) {
        UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, true)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ConnectURL.api.getLesson(lessonId).execute()
                withContext(Dispatchers.Main) {
                    UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                    if (response.isSuccessful) {
                        val responseBody = response.body()?.string()
                        if (!responseBody.isNullOrEmpty()) {
                            val jsonObj = JSONObject(responseBody)
                            val success = jsonObj.optBoolean("success", false)
                            if (success) {
                                val dataObj = jsonObj.optJSONObject("data")
                                val lessonContentRaw = dataObj?.optString("lesson_content", "") ?: ""

                                // If backend accidentally returns base64-encoded whole content (older bug),
                                // try to decode; otherwise use raw string.
                                val contentToDisplay = try {
                                    val maybeDecoded = String(Base64.decode(lessonContentRaw, Base64.DEFAULT))
                                    // Heuristic: decoded result should contain either data:image or http
                                    if (maybeDecoded.contains("data:image") || maybeDecoded.contains("http")) {
                                        maybeDecoded
                                    } else {
                                        // Decoded text did not look like HTML/content -> use raw
                                        lessonContentRaw
                                    }
                                } catch (e: IllegalArgumentException) {
                                    // not base64 -> use raw
                                    lessonContentRaw
                                }

                                if (contentToDisplay.isNotEmpty()) {
                                    displayLessonContent(contentToDisplay)
                                }
                            } else {
                                val message = jsonObj.optString("message", "Failed to load lesson")
                                ShowToast.showMessage(this@LessonContentActivity, message)
                            }
                        }
                    } else {
                        ShowToast.showMessage(this@LessonContentActivity, "Error loading lesson: ${response.code()}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                    ShowToast.showMessage(this@LessonContentActivity, "Failed to load lesson. Check internet.")
                }
            }
        }
    }

    /**
     * Display content that may contain:
     * - embedded base64 images like: data:image/png;base64,AAAA...
     * - image URLs like: https://.../image.png (supabase public url)
     *
     * Strategy:
     * - Build a SpannableStringBuilder with text and placeholders for images.
     * - For base64 images, decode synchronously and insert ImageSpan immediately.
     * - For image URLs, insert placeholder then asynchronously load image with Glide.
     *   When Glide returns, replace the placeholder with an ImageSpan on the EditText's Spannable.
     */
    private fun displayLessonContent(content: String) {
        etLessonContent.text.clear()
        val builder = SpannableStringBuilder()
        val maxDim = 800

        // Combined regex: group1 = base64, group2 = url (png/jpg/jpeg/gif/webp)
        val regex = ("(data:image/[^;]+;base64,[A-Za-z0-9+/=]+)|" +
                "(https?://[^\\s\"'<>]+\\.(?:png|jpg|jpeg|gif|webp))")
        val pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(content)

        var lastEnd = 0

        while (matcher.find()) {
            val start = matcher.start()
            if (start > lastEnd) {
                builder.append(content.substring(lastEnd, start))
            }

            val base64Part = matcher.group(1)
            val urlPart = matcher.group(2)

            if (!base64Part.isNullOrEmpty()) {
                // decode synchronously
                try {
                    val bytes = Base64.decode(base64Part.substringAfter("base64,"), Base64.DEFAULT)
                    val optsBounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, optsBounds)
                    val opts = BitmapFactory.Options()
                    opts.inSampleSize = calculateInSampleSize(optsBounds, maxDim, maxDim)
                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)?.let { resizeBitmap(it, maxDim) }

                    if (bmp != null) {
                        val spanStart = builder.length
                        builder.append("\uFFFC") // placeholder object replacement char
                        builder.setSpan(ImageSpan(this@LessonContentActivity, bmp), spanStart, spanStart + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        builder.append("\n\n")
                    }
                } catch (e: Exception) {

                }
            } else if (!urlPart.isNullOrEmpty()) {
                // insert placeholder for async load
                val spanStart = builder.length
                builder.append("\uFFFC")
                // store the url as text right after placeholder so we can find/replace later if needed
                // but do NOT expose the raw URL to user; append a newline marker
                builder.append("\n\n")
                // set the temp text to the EditText now so Glide callbacks can update spans live
                etLessonContent.text = builder

                // asynchronous Glide load - will update the EditText's Spannable when ready
                val imageUrl = urlPart
                Glide.with(this)
                    .asBitmap()
                    .load(imageUrl)
                    .into(object : CustomTarget<Bitmap>() {
                        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                            try {
                                val bmp = resizeBitmap(resource, maxDim)
                                runOnUiThread {
                                    try {
                                        val text = etLessonContent.text
                                        if (text !is Spannable) return@runOnUiThread
                                        // find the next occurrence of the placeholder char starting at spanStart
                                        // Note: spanStart index was relative to builder at the moment we appended.
                                        // Because other async loads can modify the text, search for the first placeholder
                                        // near where we inserted it by scanning for "\uFFFC".
                                        val placeholderIndex = text.toString().indexOf('\uFFFC')
                                        if (placeholderIndex >= 0) {
                                            val spanEnd = placeholderIndex + 1
                                            // Remove any existing spans overlapping that range
                                            val existingSpans = text.getSpans(placeholderIndex, spanEnd, ImageSpan::class.java)
                                            for (s in existingSpans) text.removeSpan(s)
                                            text.setSpan(ImageSpan(this@LessonContentActivity, bmp), placeholderIndex, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                                        } else {
                                            // if no placeholder (unlikely), append at end
                                            val insertIndex = text.length
                                            text.append("\uFFFC\n\n")
                                            text.setSpan(ImageSpan(this@LessonContentActivity, bmp), insertIndex, insertIndex + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                                        }
                                    } catch (e: Exception) {

                                    }
                                }
                            } catch (e: Exception) {

                            }
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {}
                        override fun onLoadFailed(errorDrawable: Drawable?) {

                        }
                    })
            }

            lastEnd = matcher.end()
        }

        if (lastEnd < content.length) {
            builder.append(content.substring(lastEnd))
        }

        // finally set the built text (base64 images already have spans; URL images will be replaced asynchronously)
        etLessonContent.text = builder
    }

    // Convert EditText content (text + ImageSpans) back to Base64 for saving
    private fun buildLessonContentWithBase64(editText: EditText): String {
        val sb = StringBuilder()
        val text = editText.text
        if (text !is Spannable) return text.toString()

        val spans = text.getSpans(0, text.length, ImageSpan::class.java)
        if (spans.isEmpty()) return text.toString()

        // Sort spans by start position
        val sortedSpans = spans.map { Pair(text.getSpanStart(it), it) }.sortedBy { it.first }
        var cursor = 0

        for ((start, span) in sortedSpans) {
            // Append text before the span
            if (start > cursor) sb.append(text.subSequence(cursor, start).toString())

            // Convert ImageSpan to Base64
            val bmp = span.drawable?.toBitmap()
            if (bmp != null) {
                sb.append(bitmapToBase64(resizeBitmap(bmp, 800)))
                sb.append("\n\n")
            }

            cursor = text.getSpanEnd(span)
        }

        // Append remaining text
        if (cursor < text.length) sb.append(text.subSequence(cursor, text.length).toString())
        return sb.toString()
    }

    private fun textToPart(text: String): RequestBody {
        return text.toRequestBody("text/plain".toMediaTypeOrNull())
    }

    private fun insertImageFromUri(uri: android.net.Uri, editText: EditText) {
        Glide.with(this).asBitmap().load(uri).override(1600,1600)
            .into(object: CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val bmp = resizeBitmap(resource, 800)
                            withContext(Dispatchers.Main) {
                                val spannable: Editable = editText.text
                                val cursor = editText.selectionStart.coerceAtLeast(0)
                                spannable.insert(cursor, "\uFFFC")
                                val spanStart = cursor
                                val spanEnd = cursor + 1
                                val imageSpan = ImageSpan(this@LessonContentActivity, bmp)
                                spannable.setSpan(imageSpan, spanStart, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                                spannable.insert(spanEnd, "\n\n")
                            }
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                }
                override fun onLoadCleared(placeholder: Drawable?) {}
            })
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height, width) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            var halfHeight = height / 2
            var halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) inSampleSize *= 2
        }
        return inSampleSize
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
        val byteArray = outputStream.toByteArray()
        return "data:image/png;base64," + Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }
}

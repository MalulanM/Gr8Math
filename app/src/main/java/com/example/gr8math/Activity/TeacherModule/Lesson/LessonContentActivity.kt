package com.example.gr8math.Activity.TeacherModule.Lesson

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.Editable
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.AlignmentSpan
import android.text.style.BackgroundColorSpan
import android.text.style.BulletSpan
import android.text.style.ForegroundColorSpan
import android.text.style.ImageSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.text.style.UnderlineSpan
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.text.HtmlCompat
import androidx.lifecycle.lifecycleScope
import aws.sdk.kotlin.services.s3.model.PutObjectRequest
import aws.smithy.kotlin.runtime.content.ByteStream
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.gr8math.Model.CurrentCourse
import com.example.gr8math.R
import com.example.gr8math.Services.TigrisService
import com.example.gr8math.Utils.ShowToast
import com.example.gr8math.Utils.UIUtils
import com.example.gr8math.ViewModel.LessonContentViewModel
import com.example.gr8math.ViewModel.LessonState
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.skydoves.colorpickerview.ColorPickerDialog
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class LessonContentActivity : AppCompatActivity() {

    private val viewModel: LessonContentViewModel by viewModels()

    private lateinit var etLessonContent: EditText
    private var weekNumber: String? = null
    private var lessonTitle: String? = null
    private var courseId: Int = 0
    private var lessonId: Int = 0

    // Loading UI
    lateinit var loadingLayout: View
    lateinit var loadingProgress: View
    lateinit var loadingText: TextView

    // Editor States
    private var currentFontSize = 16
    private lateinit var tvFontSize: TextView

    // 🌟 DEFERRED UPLOAD QUEUE
    data class PendingMedia(val localUri: Uri, val mimeType: String, val fileName: String)
    private val mediaQueue = mutableMapOf<String, PendingMedia>()
    // Views
    private lateinit var editorToolbar: View
    private lateinit var btnToggleFormat: MaterialButton

    private val pickMediaLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            queueMediaForLater(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lesson_content)

        courseId = CurrentCourse.courseId
        weekNumber = intent.getStringExtra("EXTRA_WEEK_NUMBER")
        lessonTitle = intent.getStringExtra("EXTRA_LESSON_TITLE")
        lessonId = intent.getIntExtra("EXTRA_LESSON_ID", 0)

        initViews()
        setupListeners()
        setupEditorToolbar()
        setupObservers()

        if (lessonId > 0) {
            viewModel.loadLesson(lessonId)
        }
    }

    private fun initViews() {
        etLessonContent = findViewById(R.id.etLessonContent)
        loadingLayout = findViewById(R.id.loadingLayout)
        loadingProgress = findViewById(R.id.loadingProgressBg)
        loadingText = findViewById(R.id.loadingText)
        tvFontSize = findViewById(R.id.action_font_size_text)
        editorToolbar = findViewById(R.id.editorToolbar)
        btnToggleFormat = findViewById(R.id.btnToggleFormat)
        editorToolbar.visibility = View.GONE
    }

    private fun setupListeners() {
        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        val btnSave: MaterialButton = findViewById(R.id.btnSave)

        btnSave.setOnClickListener { showSaveConfirmationDialog() }
        toolbar.setNavigationOnClickListener { checkUnsavedContentAndGoBack() }

        btnToggleFormat.setOnClickListener {
            if (editorToolbar.visibility == View.VISIBLE) {
                editorToolbar.visibility = View.GONE
                btnToggleFormat.alpha = 1.0f // Make button fully solid when toolbar is hidden
            } else {
                editorToolbar.visibility = View.VISIBLE
                btnToggleFormat.alpha = 0.5f // Fade the button slightly when toolbar is open
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { checkUnsavedContentAndGoBack() }
        })
    }

    // =========================================================================
    // 🌟 RICH TEXT EDITOR TOOLBAR LOGIC
    // =========================================================================
    private fun setupEditorToolbar() {
        findViewById<View>(R.id.action_add_media).setOnClickListener { pickMediaLauncher.launch("*/*") }

        findViewById<ImageView>(R.id.action_bold).setOnClickListener { view -> toggleStyle(Typeface.BOLD, view as ImageView) }
        findViewById<ImageView>(R.id.action_italic).setOnClickListener { view -> toggleStyle(Typeface.ITALIC, view as ImageView) }
        findViewById<ImageView>(R.id.action_underline).setOnClickListener { view -> toggleSpan(UnderlineSpan::class.java, view as ImageView) { UnderlineSpan() } }
        findViewById<ImageView>(R.id.action_strikethrough).setOnClickListener { view -> toggleSpan(StrikethroughSpan::class.java, view as ImageView) { StrikethroughSpan() } }

        findViewById<ImageView>(R.id.action_text_color).setOnClickListener { showColorPicker("Text Color") { color -> applySpan(ForegroundColorSpan(color)) } }
        findViewById<ImageView>(R.id.action_highlight).setOnClickListener { showColorPicker("Highlight Color") { color -> applySpan(BackgroundColorSpan(color)) } }

        findViewById<View>(R.id.action_align_left).setOnClickListener { applyAlignment(Layout.Alignment.ALIGN_NORMAL) }
        findViewById<View>(R.id.action_align_center).setOnClickListener { applyAlignment(Layout.Alignment.ALIGN_CENTER) }
        findViewById<View>(R.id.action_align_right).setOnClickListener { applyAlignment(Layout.Alignment.ALIGN_OPPOSITE) }

        findViewById<View>(R.id.action_h1).setOnClickListener { applyHeading(2.0f, "h1") }
        findViewById<View>(R.id.action_h2).setOnClickListener { applyHeading(1.8f, "h2") }
        findViewById<View>(R.id.action_h3).setOnClickListener { applyHeading(1.6f, "h3") }
        findViewById<View>(R.id.action_h4).setOnClickListener { applyHeading(1.4f, "h4") }
        findViewById<View>(R.id.action_h5).setOnClickListener { applyHeading(1.2f, "h5") }

        findViewById<ImageView>(R.id.action_bulleted_list).setOnClickListener { view -> toggleSpan(BulletSpan::class.java, view as ImageView) { BulletSpan(40, Color.BLACK) } }
        findViewById<View>(R.id.action_numbered_list).setOnClickListener { applyNumberedList() }

        findViewById<ImageView>(R.id.action_font_size_up).setOnClickListener { changeFontSize(2) }
        findViewById<ImageView>(R.id.action_font_size_down).setOnClickListener { changeFontSize(-2) }
    }

    private fun setButtonActive(button: ImageView, isActive: Boolean) {
        val color = if (isActive) ContextCompat.getColor(this, R.color.colorMatisse) else Color.parseColor("#4B5563")
        button.setColorFilter(color)
    }

    private fun toggleStyle(style: Int, view: ImageView) {
        val start = etLessonContent.selectionStart
        val end = etLessonContent.selectionEnd
        val spannable = etLessonContent.text
        val spans = spannable.getSpans(start, end, StyleSpan::class.java).filter { it.style == style }

        if (spans.isNotEmpty()) {
            for (span in spans) spannable.removeSpan(span)
            setButtonActive(view, false)
        } else {
            val flag = if (start == end) Spanned.SPAN_INCLUSIVE_INCLUSIVE else Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            spannable.setSpan(StyleSpan(style), start, end, flag)
            setButtonActive(view, true)
        }
    }

    private fun <T> toggleSpan(spanClass: Class<T>, view: ImageView, spanFactory: () -> Any) {
        val start = etLessonContent.selectionStart
        val end = etLessonContent.selectionEnd
        val spannable = etLessonContent.text
        val existingSpans = spannable.getSpans(start, end, spanClass)

        if (existingSpans.isNotEmpty()) {
            for (span in existingSpans) spannable.removeSpan(span)
            setButtonActive(view, false)
        } else {
            val flag = if (start == end) Spanned.SPAN_INCLUSIVE_INCLUSIVE else Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            spannable.setSpan(spanFactory(), start, end, flag)
            setButtonActive(view, true)
        }
    }

    private fun applySpan(span: Any) {
        val start = etLessonContent.selectionStart
        val end = etLessonContent.selectionEnd
        val flag = if (start == end) Spanned.SPAN_INCLUSIVE_INCLUSIVE else Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        etLessonContent.text.setSpan(span, start, end, flag)
    }

    private fun showColorPicker(title: String, onColorSelected: (Int) -> Unit) {
        ColorPickerDialog.Builder(this)
            .setTitle(title)
            .setPreferenceName("ColorPicker_$title")
            .setPositiveButton("Select", ColorEnvelopeListener { envelope, _ -> onColorSelected(envelope.color) })
            .setNegativeButton("Cancel") { dialogInterface, _ -> dialogInterface.dismiss() }
            .attachAlphaSlideBar(false)
            .attachBrightnessSlideBar(true)
            .show()
    }

    private fun applyNumberedList() {
        val start = etLessonContent.selectionStart
        val end = etLessonContent.selectionEnd
        val text = etLessonContent.text.toString()

        if (start == end) {
            etLessonContent.text.insert(start, "1. ")
            return
        }

        val lines = text.substring(start, end).split("\n")
        val builder = StringBuilder()
        for ((index, line) in lines.withIndex()) {
            if (line.trim().isNotEmpty()) builder.append("${index + 1}. $line") else builder.append(line)
            if (index < lines.size - 1) builder.append("\n")
        }
        etLessonContent.text.replace(start, end, builder.toString())
    }

    private fun applyAlignment(alignment: Layout.Alignment) {
        val start = etLessonContent.selectionStart
        val end = etLessonContent.selectionEnd
        val spannable = etLessonContent.text

        var pStart = start
        while (pStart > 0 && spannable[pStart - 1] != '\n') pStart--
        var pEnd = end
        while (pEnd < spannable.length && spannable[pEnd] != '\n') pEnd++

        val spans = spannable.getSpans(pStart, pEnd, AlignmentSpan::class.java)
        for (span in spans) spannable.removeSpan(span)
        spannable.setSpan(AlignmentSpan.Standard(alignment), pStart, pEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    private fun applyHeading(sizeMultiplier: Float, headingTag: String) {
        val start = etLessonContent.selectionStart
        val end = etLessonContent.selectionEnd
        if (start == end) return
        val spannable = etLessonContent.text
        spannable.setSpan(RelativeSizeSpan(sizeMultiplier), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        // Hidden marker so we can convert it to HTML later
        spannable.setSpan(TypefaceSpan(headingTag), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    private fun changeFontSize(increment: Int) {
        currentFontSize += increment
        if (currentFontSize < 8) currentFontSize = 8
        if (currentFontSize > 64) currentFontSize = 64
        tvFontSize.text = currentFontSize.toString()

        val start = etLessonContent.selectionStart
        val end = etLessonContent.selectionEnd
        if (start != end) {
            val spans = etLessonContent.text.getSpans(start, end, AbsoluteSizeSpan::class.java)
            for (s in spans) etLessonContent.text.removeSpan(s)
            etLessonContent.text.setSpan(AbsoluteSizeSpan(currentFontSize, true), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

            val htmlSize = when (currentFontSize) {
                in 8..12 -> 1
                in 13..15 -> 2
                in 16..18 -> 3
                in 19..23 -> 4
                in 24..31 -> 5
                in 32..47 -> 6
                else -> 7
            }
            etLessonContent.text.setSpan(TypefaceSpan("size_$htmlSize"), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    // =========================================================================
    // 🌟 DEFERRED TIGRIS UPLOAD, HTML GENERATION & SAVING
    // =========================================================================

    private fun queueMediaForLater(uri: Uri) {
        val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
        val fileName = getFileName(uri) ?: UUID.randomUUID().toString()
        val localKey = "local-media://${UUID.randomUUID()}"

        mediaQueue[localKey] = PendingMedia(uri, mimeType, fileName)
        insertLocalPreviewIntoEditor(localKey, mimeType, fileName, uri)
    }

    private fun insertLocalPreviewIntoEditor(localKey: String, mimeType: String, fileName: String, uri: Uri) {
        val spannable: Editable = etLessonContent.text
        val cursor = etLessonContent.selectionStart.coerceAtLeast(0)

        spannable.insert(cursor, "\uFFFC\n\n")

        if (mimeType.startsWith("image/") || mimeType.startsWith("video/")) {
            Glide.with(this).asBitmap().load(uri).into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    var bitmapToDraw = resizeBitmap(resource, 800)

                    if (mimeType.startsWith("video/")) {
                        bitmapToDraw = drawPlayButtonOverlay(bitmapToDraw)
                    }

                    val drawable = BitmapDrawable(resources, bitmapToDraw)
                    drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)

                    val newCursor = spannable.toString().indexOf('\uFFFC', cursor)
                    if (newCursor != -1) {
                        spannable.setSpan(ImageSpan(drawable, localKey), newCursor, newCursor + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
                }
                override fun onLoadCleared(placeholder: Drawable?) {}
            })
        } else if (mimeType == "application/pdf") {
            val pdfThumbnail = createPdfThumbnail(fileName)
            val drawable = BitmapDrawable(resources, pdfThumbnail)
            drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)

            val newCursor = spannable.toString().indexOf('\uFFFC', cursor)
            if (newCursor != -1) {
                spannable.setSpan(ImageSpan(drawable, localKey), newCursor, newCursor + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        } else {
            val linkText = "\n\n📄 Attached File: $fileName\n\n"
            spannable.insert(cursor, HtmlCompat.fromHtml("<a href=\"$localKey\">$linkText</a>", HtmlCompat.FROM_HTML_MODE_LEGACY))
        }
    }

    private fun processAndSave() {
        UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, true)
        loadingText.text = "Uploading Media..."

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val replacementHtmlTags = mutableMapOf<String, String>()

                for ((localKey, pendingMedia) in mediaQueue) {
                    val inputStream = contentResolver.openInputStream(pendingMedia.localUri)
                    val byteArray = inputStream?.readBytes() ?: continue
                    inputStream.close()

                    val filePath = "course_${courseId}/${System.currentTimeMillis()}_${pendingMedia.fileName}"
                    val request = PutObjectRequest {
                        bucket = TigrisService.BUCKET_NAME
                        key = filePath
                        body = ByteStream.fromBytes(byteArray)
                        contentType = pendingMedia.mimeType
                    }
                    TigrisService.s3Client.putObject(request)

                    val publicUrl = "https://${TigrisService.BUCKET_NAME}.fly.storage.tigris.dev/$filePath"

                    val htmlTag = when {
                        pendingMedia.mimeType.startsWith("image/") ->
                            "<img src=\"$publicUrl\" width=\"100%\"/>"
                        pendingMedia.mimeType.startsWith("video/") ->
                            "<video width=\"100%\" controls><source src=\"$publicUrl\" type=\"${pendingMedia.mimeType}\"></video>"
                        pendingMedia.mimeType == "application/pdf" ->
                            "<iframe src=\"https://docs.google.com/gview?embedded=true&url=$publicUrl\" width=\"100%\" height=\"500px\" style=\"border: none;\"></iframe>"
                        else ->
                            "<a href=\"$publicUrl\">Download ${pendingMedia.fileName}</a>"
                    }
                    replacementHtmlTags[localKey] = htmlTag
                }

                withContext(Dispatchers.Main) {
                    var finalHtml = HtmlCompat.toHtml(etLessonContent.text, HtmlCompat.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE)

                    // Swap out the <img src="localKey"> spans with the actual HTML tags
                    for ((localKey, htmlTag) in replacementHtmlTags) {
                        finalHtml = finalHtml.replace(Regex("<img[^>]*src=\"$localKey\"[^>]*>"), htmlTag)
                    }

                    finalHtml = fixAndroidHtmlBugs(finalHtml)

                    if (lessonId > 0) {
                        viewModel.updateLesson(lessonId, courseId, weekNumber ?: "0", lessonTitle ?: "", finalHtml)
                    } else {
                        viewModel.saveLesson(courseId, weekNumber ?: "0", lessonTitle ?: "", finalHtml)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                    ShowToast.showMessage(this@LessonContentActivity, "Save Failed: ${e.message}")
                }
            }
        }
    }

    private fun fixAndroidHtmlBugs(html: String): String {
        var cleanHtml = html
        cleanHtml = cleanHtml.replace(Regex("style=\"[^\"]*text-align:\\s*center;[^\"]*\""), "align=\"center\"")
        cleanHtml = cleanHtml.replace(Regex("style=\"[^\"]*text-align:\\s*(right|end);[^\"]*\""), "align=\"right\"")

        cleanHtml = cleanHtml.replace(Regex("<span style=\"font-family: [\"'&a-z;]*h([1-5])[\"'&a-z;]*;\">(.*?)</span>", RegexOption.DOT_MATCHES_ALL)) { match ->
            val level = match.groupValues[1]
            val content = match.groupValues[2]
            "<h$level>$content</h$level>"
        }

        cleanHtml = cleanHtml.replace(Regex("<span style=\"font-family: [\"'&a-z;]*size_([1-7])[\"'&a-z;]*;\">(.*?)</span>", RegexOption.DOT_MATCHES_ALL)) { match ->
            val size = match.groupValues[1]
            val content = match.groupValues[2]
            "<font size=\"$size\">$content</font>"
        }
        return cleanHtml
    }

    // --- Drawing Helpers for Editor Thumbnails ---
    private fun drawPlayButtonOverlay(bitmap: Bitmap): Bitmap {
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val paint = Paint().apply {
            color = Color.parseColor("#80000000")
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, result.width.toFloat(), result.height.toFloat(), paint)

        paint.color = Color.WHITE
        val path = Path()
        val cx = result.width / 2f
        val cy = result.height / 2f
        val size = result.width / 10f
        path.moveTo(cx - size / 2, cy - size)
        path.lineTo(cx + size, cy)
        path.lineTo(cx - size / 2, cy + size)
        path.close()
        canvas.drawPath(path, paint)
        return result
    }

    private fun createPdfThumbnail(fileName: String): Bitmap {
        val bitmap = Bitmap.createBitmap(800, 400, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            color = Color.parseColor("#EF4444")
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, 800f, 400f, paint)

        paint.color = Color.WHITE
        paint.textSize = 60f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("📄 PDF Document", 400f, 180f, paint)

        paint.textSize = 30f
        canvas.drawText(fileName.take(30) + if(fileName.length > 30) "..." else "", 400f, 250f, paint)
        return bitmap
    }

    // --- Loading Existing Content ---
    private fun displayLessonContent(content: String) {
        val spanned = HtmlCompat.fromHtml(content, HtmlCompat.FROM_HTML_MODE_LEGACY)
        etLessonContent.text = spanned as Editable? ?: SpannableStringBuilder(spanned)

        val spans = etLessonContent.text.getSpans(0, etLessonContent.text.length, ImageSpan::class.java)
        for (span in spans) {
            val source = span.source
            if (source != null && source.startsWith("http")) {
                Glide.with(this).asBitmap().load(source).into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        val bmp = resizeBitmap(resource, 800)
                        val drawable = BitmapDrawable(resources, bmp)
                        drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)

                        val start = etLessonContent.text.getSpanStart(span)
                        val end = etLessonContent.text.getSpanEnd(span)
                        if (start != -1 && end != -1) {
                            etLessonContent.text.removeSpan(span)
                            etLessonContent.text.setSpan(ImageSpan(drawable, source), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        }
                    }
                    override fun onLoadCleared(placeholder: Drawable?) {}
                })
            }
        }
    }

    private fun resizeBitmap(bitmap: Bitmap, maxDim: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDim && height <= maxDim) return bitmap
        val aspect = width.toFloat() / height.toFloat()
        val (newW, newH) = if (aspect >= 1f) Pair(maxDim, (maxDim / aspect).toInt()) else Pair((maxDim * aspect).toInt(), maxDim)
        return Bitmap.createScaledBitmap(bitmap, newW, newH, true)
    }

    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) result = cursor.getString(index)
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != null && cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result
    }

    // --- VIEWMODEL & NAVIGATION Logic ---
    private fun setupObservers() {
        viewModel.state.observe(this) { state ->
            when (state) {
                is LessonState.Loading -> UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, true)
                is LessonState.Saved -> {
                    UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                    ShowToast.showMessage(this, "Lesson saved successfully!")
                    mediaQueue.clear()
                    setResult(RESULT_OK)
                    finish()
                }
                is LessonState.ContentLoaded -> {
                    UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                    displayLessonContent(state.lesson.lessonContent)
                }
                is LessonState.Error -> {
                    UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                    ShowToast.showMessage(this, state.message)
                }
                is LessonState.Idle -> UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
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
            .setNegativeButton(R.string.yes) { _, _ -> processAndSave() }
            .setPositiveButton(R.string.no) { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun checkUnsavedContentAndGoBack() {
        if (etLessonContent.text.toString().trim().isNotEmpty()) showDiscardChangesDialog() else goBackToStep1()
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
}
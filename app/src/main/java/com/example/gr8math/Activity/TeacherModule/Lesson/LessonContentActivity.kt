package com.example.gr8math.Activity.TeacherModule.Lesson

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.lifecycle.lifecycleScope
import aws.sdk.kotlin.services.s3.model.PutObjectRequest
import aws.smithy.kotlin.runtime.content.ByteStream
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

    private var weekNumber: String? = null
    private var lessonTitle: String? = null
    private var courseId: Int = 0
    private var lessonId: Int = 0

    private var currentFontSize = 16
    private lateinit var tvFontSize: TextView

    // UI State
    lateinit var loadingLayout: View
    lateinit var loadingProgress: View
    lateinit var loadingText: TextView
    private lateinit var editorToolbar: View
    private lateinit var btnToggleFormat: MaterialButton
    private lateinit var webViewEditor: WebView

    private var isLoaded = false
    private var hasUnsavedChanges = false

    data class PendingMedia(val localUri: Uri, val mimeType: String, val fileName: String)
    private val mediaQueue = mutableMapOf<String, PendingMedia>()

    private val pickMediaLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) queueMediaForLater(uri)
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
        webViewEditor = findViewById(R.id.webViewEditor)
        loadingLayout = findViewById(R.id.loadingLayout)
        loadingProgress = findViewById(R.id.loadingProgressBg)
        loadingText = findViewById(R.id.loadingText)
        editorToolbar = findViewById(R.id.editorToolbar)
        btnToggleFormat = findViewById(R.id.btnToggleFormat)

        // Initialize font size display
        tvFontSize = findViewById(R.id.action_font_size_text)

        setupWebView()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webViewEditor.settings.javaScriptEnabled = true
        webViewEditor.settings.domStorageEnabled = true
        webViewEditor.settings.allowFileAccess = true

        webViewEditor.addJavascriptInterface(object {
            @JavascriptInterface
            fun onContentChanged() {
                runOnUiThread { hasUnsavedChanges = true }
            }
        }, "Android")

        webViewEditor.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                isLoaded = true
            }
        }

        webViewEditor.loadUrl("file:///android_asset/editor.html")
    }

    private fun setupListeners() {
        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        val btnSave: MaterialButton = findViewById(R.id.btnSave)

        btnSave.setOnClickListener { showSaveConfirmationDialog() }
        toolbar.setNavigationOnClickListener { checkUnsavedContentAndGoBack() }

        btnToggleFormat.setOnClickListener {
            if (editorToolbar.visibility == View.VISIBLE) {
                editorToolbar.visibility = View.GONE
                btnToggleFormat.alpha = 1.0f
            } else {
                editorToolbar.visibility = View.VISIBLE
                btnToggleFormat.alpha = 0.5f
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { checkUnsavedContentAndGoBack() }
        })
    }

    private fun setupEditorToolbar() {
        findViewById<View>(R.id.action_add_media).setOnClickListener { pickMediaLauncher.launch("*/*") }

        // Core Formatting
        findViewById<View>(R.id.action_bold).setOnClickListener { execJs("bold") }
        findViewById<View>(R.id.action_italic).setOnClickListener { execJs("italic") }
        findViewById<View>(R.id.action_underline).setOnClickListener { execJs("underline") }
        findViewById<View>(R.id.action_strikethrough).setOnClickListener { execJs("strikeThrough") }

        findViewById<View>(R.id.action_text_color).setOnClickListener {
            showColorPicker("Text Color") { color ->
                hasUnsavedChanges = true
                val hex = String.format("#%06X", (0xFFFFFF and color))
                webViewEditor.evaluateJavascript("document.execCommand('foreColor', false, '$hex');", null)
            }
        }

        findViewById<View>(R.id.action_align_left).setOnClickListener { execJs("justifyLeft") }
        findViewById<View>(R.id.action_align_center).setOnClickListener { execJs("justifyCenter") }
        findViewById<View>(R.id.action_align_right).setOnClickListener { execJs("justifyRight") }

        findViewById<View>(R.id.action_h1).setOnClickListener { execJs("formatBlock", "H1") }
        findViewById<View>(R.id.action_h2).setOnClickListener { execJs("formatBlock", "H2") }
        findViewById<View>(R.id.action_h3).setOnClickListener { execJs("formatBlock", "H3") }

        findViewById<View>(R.id.action_bulleted_list).setOnClickListener { execJs("insertUnorderedList") }
        findViewById<View>(R.id.action_numbered_list).setOnClickListener { execJs("insertOrderedList") }

        // Font Size Buttons
        findViewById<View>(R.id.action_font_size_up).setOnClickListener { changeFontSize(2) }
        findViewById<View>(R.id.action_font_size_down).setOnClickListener { changeFontSize(-2) }
    }

    private fun execJs(command: String, value: String? = null) {
        hasUnsavedChanges = true
        val valArg = if (value != null) "'$value'" else "null"
        webViewEditor.evaluateJavascript("document.execCommand('$command', false, $valArg);", null)
    }

    private fun showColorPicker(title: String, onColorSelected: (Int) -> Unit) {
        ColorPickerDialog.Builder(this)
            .setTitle(title)
            .setPositiveButton("Select", ColorEnvelopeListener { envelope, _ -> onColorSelected(envelope.color) })
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .attachAlphaSlideBar(false)
            .show()
    }

    private fun changeFontSize(increment: Int) {
        hasUnsavedChanges = true
        currentFontSize += increment
        if (currentFontSize < 8) currentFontSize = 8
        if (currentFontSize > 72) currentFontSize = 72

        tvFontSize.text = currentFontSize.toString()

        webViewEditor.evaluateJavascript(
            "document.execCommand('fontSize', false, '7'); " +
                    "var fontSpans = document.getElementsByTagName('font'); " +
                    "for (var i = 0; i < fontSpans.length; i++) { " +
                    "  if (fontSpans[i].size == '7') { " +
                    "    fontSpans[i].removeAttribute('size'); " +
                    "    fontSpans[i].style.fontSize = '${currentFontSize}px'; " +
                    "  } " +
                    "}", null
        )
    }

    private fun getBase64FromUri(uri: Uri): String? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes()
            inputStream?.close()
            android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        } catch (e: Exception) { null }
    }

    private fun queueMediaForLater(uri: Uri) {
        val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
        val fileName = getFileName(uri) ?: UUID.randomUUID().toString()
        val localKey = "local-media://${UUID.randomUUID()}"

        mediaQueue[localKey] = PendingMedia(uri, mimeType, fileName)
        insertLocalPreviewIntoEditor(localKey, mimeType, fileName, uri)
    }

    private fun insertLocalPreviewIntoEditor(localKey: String, mimeType: String, fileName: String, uri: Uri) {
        hasUnsavedChanges = true
        var previewHtml = ""

        when {
            // 1. IMAGE PREVIEW
            mimeType.startsWith("image/") -> {
                val base64 = getBase64FromUri(uri)
                previewHtml = "<img src=\"data:$mimeType;base64,$base64\" style=\"width:100%; border-radius:8px;\" />"
            }

            // 2. PDF PREVIEW (Page 1 Snapshot)
            mimeType == "application/pdf" -> {
                val base64 = getPdfThumbnailBase64(uri)
                previewHtml = """
                <div style="position:relative; border:2px solid #D1D8DD; border-radius:8px; overflow:hidden;">
                    <img src="data:image/jpeg;base64,$base64" style="width:100%; opacity:0.6;" />
                    <div style="position:absolute; top:50%; left:50%; transform:translate(-50%,-50%); background:rgba(26,76,139,0.9); color:white; padding:12px 20px; border-radius:30px; font-weight:bold; font-size:14px;">📄 PDF PREVIEW: $fileName</div>
                </div>
            """.trimIndent()
            }

            // 3. VIDEO PREVIEW (Frame Snapshot)
            mimeType.startsWith("video/") -> {
                val base64 = getVideoThumbnailBase64(uri)
                previewHtml = """
                <div style="position:relative; border-radius:8px; overflow:hidden; background:#000;">
                    <img src="data:image/jpeg;base64,$base64" style="width:100%; opacity:0.5;" />
                    <div style="position:absolute; top:50%; left:50%; transform:translate(-50%,-50%); color:white; text-align:center;">
                        <div style="font-size:40px;">▶</div>
                        <div style="font-weight:bold;">VIDEO PREVIEW</div>
                        <div style="font-size:12px;">$fileName</div>
                    </div>
                </div>
            """.trimIndent()
            }

            // 4. DOCS / OTHERS
            else -> {
                previewHtml = "<div style=\"color:#1A4C8B; font-weight:bold; background:#F4F6F8; padding:15px; border-radius:8px; border:1px solid #D1D8DD;\">📎 Attached: $fileName</div>"
            }
        }

        val finalHtml = """
        <div data-local="$localKey" class="gr8-media-wrapper" contenteditable="false" style="width:100%; margin:20px 0; text-align:center;">
            $previewHtml
        </div>
        <p><br></p>
        """.trimIndent()

        // Safely pass the HTML by encoding it, then decoding it on the JS side
        val encodedHtml = android.util.Base64.encodeToString(finalHtml.toByteArray(), android.util.Base64.NO_WRAP)

        webViewEditor.evaluateJavascript(
            "insertHtml(decodeURIComponent(escape(window.atob('$encodedHtml'))))",
            null
        )
    }
    private fun processAndSave() {
        webViewEditor.evaluateJavascript("getHtml()") { rawJsonHtml ->
            var currentHtml = rawJsonHtml.removeSurrounding("\"")
                .replace("\\u003C", "<")
                .replace("\\\"", "\"")
                .replace("\\n", "")

            UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, true)
            loadingText.text = "Uploading to Tigris..."

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    for ((localKey, pendingMedia) in mediaQueue) {
                        if (!currentHtml.contains(localKey)) continue

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

                        // 🌟 FIX 1: Added PDF handling back in!
                        val cleanTag = when {
                            pendingMedia.mimeType.startsWith("image/") ->
                                "<img src=\"$publicUrl\" style=\"max-width:100%; border-radius:8px;\" />"
                            pendingMedia.mimeType == "application/pdf" ->
                                "<iframe src=\"https://docs.google.com/gview?embedded=true&url=$publicUrl\" width=\"100%\" height=\"600px\" style=\"border:none;\"></iframe>"
                            pendingMedia.mimeType.startsWith("video/") ->
                                "<video controls style=\"width:100%; border-radius:8px;\"><source src=\"$publicUrl\" type=\"${pendingMedia.mimeType}\"></video>"
                            else -> "<a href=\"$publicUrl\">Download ${pendingMedia.fileName}</a>"
                        }

                        // This looks for the start of the wrapper and grabs everything until the closing </div>
                        val regex = Regex("<div[^>]*data-local=\"$localKey\"[^>]*>[\\s\\S]*?</div>", RegexOption.IGNORE_CASE)
                        currentHtml = currentHtml.replace(regex, cleanTag)
                    }

                    withContext(Dispatchers.Main) {
                        if (lessonId > 0) {
                            viewModel.updateLesson(lessonId, courseId, weekNumber ?: "0", lessonTitle ?: "", currentHtml)
                        } else {
                            viewModel.saveLesson(courseId, weekNumber ?: "0", lessonTitle ?: "", currentHtml)
                        }
                    }
                }catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                        ShowToast.showMessage(this@LessonContentActivity, "Upload failed: ${e.message}")
                    }
                }
            }
        }
    }

    // Helper to get a snapshot of the first page of a PDF
    private fun getPdfThumbnailBase64(uri: Uri): String? {
        return try {
            val fd = contentResolver.openFileDescriptor(uri, "r") ?: return null
            val renderer = android.graphics.pdf.PdfRenderer(fd)
            val page = renderer.openPage(0)
            val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

            val outputStream = java.io.ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream) // Compressed for editor speed
            val bytes = outputStream.toByteArray()

            page.close()
            renderer.close()
            android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        } catch (e: Exception) { null }
    }

    // Helper to get a frame from a video file
    private fun getVideoThumbnailBase64(uri: Uri): String? {
        val retriever = android.media.MediaMetadataRetriever()
        return try {
            retriever.setDataSource(this, uri)
            val bitmap = retriever.getFrameAtTime(1000000) // Get frame at 1 second
            val outputStream = java.io.ByteArrayOutputStream()
            bitmap?.compress(Bitmap.CompressFormat.JPEG, 50, outputStream)
            android.util.Base64.encodeToString(outputStream.toByteArray(), android.util.Base64.NO_WRAP)
        } catch (e: Exception) { null } finally { retriever.release() }
    }

    private fun displayLessonContent(content: String) {
        if (isLoaded) {
            val escapedHtml = content.replace("'", "\\'").replace("\n", "")
            webViewEditor.evaluateJavascript("setHtml('$escapedHtml')", null)
        } else {
            webViewEditor.postDelayed({ displayLessonContent(content) }, 300)
        }
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
        return result ?: uri.path?.substringAfterLast('/')
    }

    private fun setupObservers() {
        viewModel.state.observe(this) { state ->
            when (state) {
                is LessonState.Loading -> UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, true)
                is LessonState.Saved -> {
                    UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                    ShowToast.showMessage(this, "Lesson saved!")
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
                else -> {}
            }
        }
    }

    private fun showSaveConfirmationDialog() {
        MaterialAlertDialogBuilder(this)
            .setMessage(HtmlCompat.fromHtml("<b>Are you sure you want to save?</b>", HtmlCompat.FROM_HTML_MODE_LEGACY))
            .setNegativeButton("Yes") { _, _ -> processAndSave() }
            .setPositiveButton("No") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun checkUnsavedContentAndGoBack() {
        if (hasUnsavedChanges) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Discard changes?")
                .setMessage("You have unsaved content. If you go back, your changes will be lost.")
                .setNegativeButton("Yes") { _, _ -> finish() }
                .setPositiveButton("No") { dialog, _ -> dialog.dismiss() }
                .show()
        } else {
            finish()
        }
    }
}
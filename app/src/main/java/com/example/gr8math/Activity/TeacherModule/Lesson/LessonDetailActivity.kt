package com.example.gr8math.Activity.TeacherModule.Lesson

import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.gr8math.R
import com.example.gr8math.Utils.ShowToast
import com.example.gr8math.Utils.UIUtils
import com.example.gr8math.ViewModel.LessonContentViewModel
import com.example.gr8math.ViewModel.LessonState
import com.google.android.material.appbar.MaterialToolbar

class LessonDetailActivity : AppCompatActivity() {

    private val viewModel: LessonContentViewModel by viewModels()

    lateinit var loadingLayout: View
    lateinit var loadingProgress: View
    lateinit var loadingText: TextView

    private lateinit var tvWeek: TextView
    private lateinit var tvTitle: TextView

    // The WebView engine
    private lateinit var webViewContent: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lesson_detail)

        initViews()
        setupObservers()

        val lessonId = intent.getIntExtra("lesson_id", 0)
        if (lessonId > 0) {
            viewModel.loadLesson(lessonId)
        }
    }

    private fun initViews() {
        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        tvWeek = findViewById(R.id.tvWeek)
        tvTitle = findViewById(R.id.tvTitle)

        loadingLayout = findViewById(R.id.loadingLayout)
        loadingProgress = findViewById(R.id.loadingProgressBg)
        loadingText = findViewById(R.id.loadingText)

        webViewContent = findViewById(R.id.webViewContent)
        setupWebView()

        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupWebView() {
        webViewContent.settings.apply {
            javaScriptEnabled = true // Required for Video players and PDF iframes to work
            domStorageEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
        }
        // Keeps videos and PDFs inside the app instead of launching external browsers
        webViewContent.webViewClient = WebViewClient()
        webViewContent.webChromeClient = WebChromeClient()
    }

    private fun setupObservers() {
        viewModel.state.observe(this) { state ->
            when (state) {
                is LessonState.Loading -> {
                    UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, true)
                }
                is LessonState.ContentLoaded -> {
                    UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)

                    val lesson = state.lesson
                    tvWeek.text = "Week ${lesson.weekNumber}"
                    tvTitle.text = lesson.lessonTitle

                    // Load HTML into WebView
                    displayHtmlInWebView(lesson.lessonContent)
                }
                is LessonState.Error -> {
                    UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                    ShowToast.showMessage(this, state.message)
                }
                else -> {}
            }
        }
    }

    private fun displayHtmlInWebView(htmlContent: String) {
        // We inject CSS to make sure the text uses your brand styling,
        // and that images, videos, and PDFs perfectly fit the width of the screen.
        val formattedHtml = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1">
                <style>
                    body {
                        font-family: 'sans-serif';
                        font-size: 16px;
                        color: #1F2937;
                        line-height: 1.6;
                        padding: 0px;
                        margin: 0px;
                    }
                    img, video {
                        max-width: 100%;
                        height: auto;
                        border-radius: 8px;
                        margin-top: 12px;
                        margin-bottom: 12px;
                    }
                    iframe {
                        width: 100%;
                        height: 500px;
                        border-radius: 8px;
                        margin-top: 12px;
                        margin-bottom: 12px;
                        border: 1px solid #E5E7EB;
                    }
                    h1, h2, h3, h4, h5 {
                        color: #111827;
                        margin-top: 16px;
                        margin-bottom: 8px;
                    }
                </style>
            </head>
            <body>
                $htmlContent
            </body>
            </html>
        """.trimIndent()

        // Load the styled HTML into the view
        webViewContent.loadDataWithBaseURL(null, formattedHtml, "text/html", "UTF-8", null)
    }
}
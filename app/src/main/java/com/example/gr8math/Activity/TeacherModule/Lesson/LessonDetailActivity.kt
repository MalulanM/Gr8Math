package com.example.gr8math.Activity.TeacherModule.Lesson

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ImageSpan
import android.util.Base64
import android.view.View
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.gr8math.R
import com.example.gr8math.Utils.ShowToast
import com.example.gr8math.Utils.UIUtils
import com.example.gr8math.ViewModel.LessonContentViewModel
import com.example.gr8math.ViewModel.LessonState
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LessonDetailActivity : AppCompatActivity() {

    // Reuse the same ViewModel since it already has loadLesson()
    private val viewModel: LessonContentViewModel by viewModels()

    lateinit var loadingLayout: View
    lateinit var loadingProgress: View
    lateinit var loadingText: TextView

    private lateinit var tvWeek: TextView
    private lateinit var tvTitle: TextView
    private lateinit var tvDescription: TextView

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
        tvDescription = findViewById(R.id.tvDescription)
        loadingLayout = findViewById(R.id.loadingLayout)
        loadingProgress = findViewById(R.id.loadingProgressBg)
        loadingText = findViewById(R.id.loadingText)

        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupObservers() {
        viewModel.state.observe(this) { state ->
            when (state) {
                is LessonState.Loading -> {
                    UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, true)
                }
                is LessonState.ContentLoaded -> {
                    UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)

                    // FIX: Access fields from the 'lesson' object
                    val lesson = state.lesson
                    tvWeek.text = "Week ${lesson.weekNumber}"
                    tvTitle.text = lesson.lessonTitle

                    // Use the existing content renderer
                    displayLazyContent(tvDescription, lesson.lessonContent)
                }
                is LessonState.Error -> {
                    UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                    ShowToast.showMessage(this, state.message)
                }
                else -> {}
            }
        }
    }

    /**
     * Renders text containing Base64 images or Image URLs into the TextView.
     */
    private fun displayLazyContent(tv: TextView, content: String) {
        lifecycleScope.launch(Dispatchers.Default) {
            val spannable = SpannableStringBuilder()

            // Match BOTH Base64 and URL
            val regex = """(data:image/[^;]+;base64,[A-Za-z0-9+/=]+|https?://[^\s]+?\.(png|jpg|jpeg|webp))""".toRegex()
            val matches = regex.findAll(content).toList()

            var lastIndex = 0

            for (match in matches) {
                val start = match.range.first
                val end = match.range.last + 1

                if (start > lastIndex) {
                    spannable.append(content.substring(lastIndex, start))
                }
                lastIndex = end

                val spanStart = spannable.length
                spannable.append("\uFFFC\n\n")

                withContext(Dispatchers.Main) {
                    tv.text = spannable
                }

                val token = match.value
                val bitmap = if (token.startsWith("data:image")) {
                    loadBase64Image(token)
                } else {
                    loadUrlImage(token)
                }

                if (bitmap != null) {
                    withContext(Dispatchers.Main) {
                        spannable.setSpan(
                            ImageSpan(tv.context, bitmap),
                            spanStart,
                            spanStart + 1,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        tv.text = spannable
                    }
                }
            }

            if (lastIndex < content.length) {
                spannable.append(content.substring(lastIndex))
            }
            withContext(Dispatchers.Main) {
                tv.text = spannable
            }
        }
    }

    private fun loadBase64Image(base64Image: String): Bitmap? {
        return try {
            val pureBase64 = base64Image.substringAfter("base64,")
            val bytes = Base64.decode(pureBase64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            null
        }
    }

    private fun loadUrlImage(url: String): Bitmap? {
        return try {
            Glide.with(this@LessonDetailActivity)
                .asBitmap()
                .load(url)
                .submit()
                .get()
        } catch (e: Exception) {
            null
        }
    }
}
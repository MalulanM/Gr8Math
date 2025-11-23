package com.example.gr8math

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ImageSpan
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.gr8math.api.ConnectURL
import com.example.gr8math.dataObject.CurrentCourse
import com.example.gr8math.utils.ShowToast
import com.example.gr8math.utils.UIUtils
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.*
import okhttp3.ResponseBody
import retrofit2.Call
import java.util.concurrent.Executors

class LessonDetailActivity : AppCompatActivity() {

    lateinit var loadingLayout: View
    lateinit var loadingProgress: View
    lateinit var loadingText: TextView

    private lateinit var tvWeek: TextView
    private lateinit var tvTitle: TextView
    private lateinit var tvDescription: TextView

    private val imageDecoder = Executors.newFixedThreadPool(2).asCoroutineDispatcher()
    private val decodingScope = CoroutineScope(SupervisorJob() + imageDecoder)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lesson_detail)

        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        tvWeek = findViewById(R.id.tvWeek)
        tvTitle = findViewById(R.id.tvTitle)
        tvDescription = findViewById(R.id.tvDescription)
        loadingLayout = findViewById(R.id.loadingLayout)
        loadingProgress = findViewById(R.id.loadingProgressBg)
        loadingText = findViewById(R.id.loadingText)

        val lessonId = intent.getIntExtra("lesson_id", 0)
        val role = CurrentCourse.currentRole
        displayLesson(lessonId, role)

        toolbar.setNavigationOnClickListener { finish() }
    }

    override fun onDestroy() {
        super.onDestroy()
        decodingScope.cancel()
        imageDecoder.close()
    }

    private fun displayLesson(lessonId: Int, role : String) {
        UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, true)

        ConnectURL.api.getLesson(lessonId).enqueue(object : retrofit2.Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: retrofit2.Response<ResponseBody>) {
                UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)

                val body = response.body()?.string() ?: response.errorBody()?.string()
                if (body.isNullOrEmpty()) return
                
                Log.e("APIIDEJB", body.toString())

                try {
                    val json = org.json.JSONObject(body)
                    val data = json.optJSONObject("data") ?: return

                    val lessonContent = data.optString("lesson_content", "")
                    val title = data.optString("lesson_title", "")
                    val weekNum = data.optInt("week_number", 0)

                    tvWeek.text = "Week $weekNum"
                    tvTitle.text = title

                    CoroutineScope(Dispatchers.Main).launch {
                        displayLazyContent(tvDescription, lessonContent)
                    }

                } catch (e: Exception) {
                    Log.e("API_ERROR", "JSON parse error ${e.message}")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                UIUtils.showLoading(loadingLayout, loadingProgress, loadingText, false)
                ShowToast.showMessage(this@LessonDetailActivity, "Check your internet.")
            }
        })
    }

    /**
     * NEW VERSION:
     * Supports BOTH Base64 and URL images from Laravel/Supabase
     */
    private suspend fun displayLazyContent(tv: TextView, content: String) {
        val spannable = SpannableStringBuilder()

        // Match BOTH formats:
        // 1. Base64: data:image/png;base64,...
        // 2. URL: https://supabase/... .png or .jpg
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
            tv.text = spannable

            val token = match.value

            decodingScope.launch {
                val bitmap =
                    if (token.startsWith("data:image")) loadBase64Image(token)
                    else loadUrlImage(token)

                bitmap?.let {
                    withContext(Dispatchers.Main) {
                        spannable.setSpan(
                            ImageSpan(tv.context, it),
                            spanStart,
                            spanStart + 1,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        tv.text = spannable
                    }
                }
            }
        }

        if (lastIndex < content.length)
            spannable.append(content.substring(lastIndex))

        tv.text = spannable
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

    private suspend fun loadUrlImage(url: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            Glide.with(this@LessonDetailActivity)
                .asBitmap()
                .load(url)
                .submit()
                .get()
        } catch (e: Exception) {
            Log.e("IMG_LOAD", "Failed URL image: ${e.message}")
            null
        }
    }
}

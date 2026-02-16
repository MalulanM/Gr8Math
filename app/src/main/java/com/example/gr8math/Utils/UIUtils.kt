package com.example.gr8math.Utils

import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.gr8math.R
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object UIUtils {

    fun showLoading(
        loadingLayout: View,
        loadingProgress: View,
        loadingText: TextView,
        isShowing: Boolean
    ) {
        val fade = 200L
        val baseText = loadingText.text.toString()
        val scope = CoroutineScope(Dispatchers.Main)

        if (isShowing) {
            loadingLayout.apply {
                alpha = 0f
                visibility = View.VISIBLE
                animate().alpha(1f).setDuration(fade).start()
            }
            loadingProgress.apply {
                alpha = 0f
                visibility = View.VISIBLE
                animate().alpha(1f).setDuration(fade).start()
            }

            // animate dots
            val job = scope.launch {
                var i = 0
                while (isActive) {
                    loadingText.text = baseText + ".".repeat(i)
                    i = (i + 1) % 4
                    delay(500)
                }
            }
            loadingText.tag = job
        } else {
            loadingLayout.animate().alpha(0f).setDuration(fade).withEndAction {
                loadingLayout.visibility = View.GONE
            }.start()
            loadingProgress.animate().alpha(0f).setDuration(fade).withEndAction {
                loadingProgress.visibility = View.GONE
            }.start()

            (loadingText.tag as? Job)?.cancel()
            loadingText.text = baseText
        }
    }

    // Error display function
    fun errorDisplay(
        context: Context,
        til: TextInputLayout,
        field: TextView,
        showIcon: Boolean,
        errorText: String = "Please input valid credentials",
        forceError: Boolean = false
    ) {
        val textValue = field.text?.toString()?.trim() ?: ""

        if (forceError || textValue.isEmpty()) {
            til.isErrorEnabled = true
            til.error = errorText
            if (showIcon) {
                til.setErrorIconDrawable(R.drawable.ic_warning)
            } else {
                til.setErrorIconDrawable(null)
            }
            til.setErrorTextColor(ContextCompat.getColorStateList(context, R.color.colorRed))
            til.setBoxStrokeColorStateList(ContextCompat.getColorStateList(context, R.color.colorRed)!!)
        } else {
            til.isErrorEnabled = false
            til.error = null
            til.setBoxStrokeColorStateList(
                ContextCompat.getColorStateList(context, R.color.til_stroke)!!
            )
        }
    }

}

package com.example.gr8math.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.gr8math.R
import java.text.SimpleDateFormat
import java.util.Locale


data class StudentNotificationResponse(
    val success: Boolean,
    val notifications: List<StudentNotification>
)
// Data Class
data class StudentNotification(
    val title: String,
    val message: String,
    val created_at: String,
    var is_read: Boolean,
    val type : String,
    val id : Int,
    val course_id: Int?,
    val lesson_id: Int?,
    val assessment_id: Int?
)

class StudentNotificationAdapter(
    private val notificationList: List<StudentNotification>,
    private val onItemClick: (StudentNotification, Int) -> Unit) :
    RecyclerView.Adapter<StudentNotificationAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivIcon: ImageView = view.findViewById(R.id.ivIcon) // Reusing item_teacher_notification layout is fine
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvDescription: TextView = view.findViewById(R.id.tvDescription)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Reusing the teacher item layout as it looks identical
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = notificationList[position]

        holder.tvTitle.text = item.title
        holder.tvDescription.text = item.message
        holder.tvTime.text = formatTime(item.created_at)

        val iconRes = when (item.type) {
            "class_time" -> if (item.is_read) {
                R.drawable.ic_read_class
            } else {
                R.drawable.ic_unread_class
            }

            "lesson" -> if (item.is_read) {
                R.drawable.ic_read_lesson
            } else {
                R.drawable.ic_unread_lesson
            }

            "assessment" -> if (item.is_read) {
                R.drawable.ic_read_lesson
            } else {
                R.drawable.ic_unread_lesson
            }

            else -> if (item.is_read) {
                R.drawable.ic_read_lesson
            } else {
                R.drawable.ic_unread_lesson
            }
        }

        holder.ivIcon.setImageResource(iconRes)

        // Remove color filter — this ruins your icons!
        holder.ivIcon.clearColorFilter()
        holder.itemView.setOnClickListener {
            onItemClick(item, position)
        }
    }


    override fun getItemCount() = notificationList.size

    private fun formatTime(raw: String): String {
        return try {
            // Parse Supabase timestamp: yyyy-MM-dd HH:mm:ss.SSSS
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSS", Locale.getDefault())
            inputFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")
            val date = inputFormat.parse(raw) ?: return raw

            val now = System.currentTimeMillis()
            val diff = now - date.time

            val oneHour = 60 * 60 * 1000L
            val oneDay = 24 * oneHour
            val oneYear = 365 * oneDay

            return when {
                diff < oneDay -> {
                    val timeFormat = SimpleDateFormat("h:mma", Locale.getDefault())
                    timeFormat.format(date)  // 1:00AM
                }
                diff < oneYear -> {
                    val monthDayFormat = SimpleDateFormat("MMM d", Locale.getDefault())
                    monthDayFormat.format(date)  // May 16
                }
                else -> {
                    val fullFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                    fullFormat.format(date)  // May 16, 2025
                }
            }

        } catch (e: Exception) {
            raw
        }
    }
}
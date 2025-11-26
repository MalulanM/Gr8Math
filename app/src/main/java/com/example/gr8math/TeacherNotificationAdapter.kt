package com.example.gr8math.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.gr8math.R
import com.google.gson.annotations.SerializedName
import java.text.SimpleDateFormat
import java.util.Locale

// API Response Model
data class MarkAllReadRequest(
    @SerializedName("ids")
    val ids: List<Int>
)


data class TeacherNotificationResponse(
    val success: Boolean,
    val notifications: List<TeacherNotification>
)

data class TeacherNotification(
    val title: String,
    val message: String,
    val created_at: String,
    var is_read: Boolean,
    val type: String,
    val id : Int
)

class TeacherNotificationAdapter(
    private val notificationList: List<TeacherNotification>,
    private val onItemClick: (TeacherNotification, Int) -> Unit
) : RecyclerView.Adapter<TeacherNotificationAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivIcon: ImageView = view.findViewById(R.id.ivIcon)
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvDescription: TextView = view.findViewById(R.id.tvDescription)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
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
            "class" -> if (item.is_read) R.drawable.ic_read_class else R.drawable.ic_unread_class
            "assessment_submission" -> if (item.is_read) R.drawable.ic_read_lesson else R.drawable.ic_unread_lesson
            else -> if (item.is_read) R.drawable.ic_read_lesson else R.drawable.ic_unread_lesson
        }

        holder.ivIcon.setImageResource(iconRes)

        holder.itemView.setOnClickListener {
            onItemClick(item, position)
        }
    }

    override fun getItemCount() = notificationList.size

    private fun formatTime(raw: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSS", Locale.getDefault())
            val date = inputFormat.parse(raw) ?: return raw

            val now = System.currentTimeMillis()
            val diff = now - date.time

            val oneHour = 60 * 60 * 1000L
            val oneDay = 24 * oneHour
            val oneYear = 365 * oneDay

            when {
                diff < oneDay -> SimpleDateFormat("h:mma", Locale.getDefault()).format(date)
                diff < oneYear -> SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
                else -> SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(date)
            }
        } catch (e: Exception) {
            raw
        }
    }
}

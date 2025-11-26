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
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.TimeZone

// specific data class for Faculty
data class FacultyNotification(
    val title: String,
    val message : String,
    val created_at: String,
    var is_read: Boolean,
    val id : Int
)

class FacultyNotificationAdapter(private val notificationList: List<FacultyNotification>,
                                 private val onItemClick: (FacultyNotification, Int) -> Unit) :
    RecyclerView.Adapter<FacultyNotificationAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // We can reuse the XML layout because the views (Icon, Title, Desc) are the same
        val ivIcon: ImageView = view.findViewById(R.id.ivIcon)
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvDescription: TextView = view.findViewById(R.id.tvDescription)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Reusing the 'item_notification' layout is fine since it looks the same visually
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = notificationList[position]
        val context = holder.itemView.context

        holder.tvTitle.text = item.title
        holder.tvDescription.text = item.message

        holder.tvTime.text = formatTime(item.created_at)

        // Logic for Faculty Icons
        // Read = Green (DarkCyan), Unread = Yellow (Saffron)
        if (item.is_read) {
            holder.ivIcon.setImageResource(R.drawable.ic_read_admin)
        } else {
            holder.ivIcon.setImageResource(R.drawable.ic_unread_admin)
        }

    }

    override fun getItemCount() = notificationList.size

    private fun formatTime(raw: String): String {
        return try {
            // Parse Supabase timestamp: yyyy-MM-dd HH:mm:ss.SSSS
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSS", Locale.getDefault())
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
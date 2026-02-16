package com.example.gr8math.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.gr8math.Data.Model.StudentNotificationUI
import com.example.gr8math.R
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class StudentNotificationAdapter(
    private var notificationList: MutableList<StudentNotificationUI>,
    private val onItemClick: (StudentNotificationUI, Int) -> Unit
) : RecyclerView.Adapter<StudentNotificationAdapter.ViewHolder>() {

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
        holder.tvTime.text = formatTime(item.createdAt)

        val iconRes = when (item.type) {
            "class_time" -> if (item.isRead) R.drawable.ic_read_class else R.drawable.ic_unread_class
            "lesson" -> if (item.isRead) R.drawable.ic_read_lesson else R.drawable.ic_unread_lesson
            "assessment" -> if (item.isRead)  R.drawable.ic_read_lesson else R.drawable.ic_unread_lesson
            else -> if (item.isRead) R.drawable.ic_read_lesson else R.drawable.ic_unread_lesson
        }

        holder.ivIcon.setImageResource(iconRes)
        holder.ivIcon.clearColorFilter()

        holder.itemView.setOnClickListener {
            onItemClick(item, position)
        }
    }

    override fun getItemCount() = notificationList.size

    fun updateList(newList: List<StudentNotificationUI>) {
        notificationList.clear()
        notificationList.addAll(newList)
        notifyDataSetChanged()
    }

    fun getList(): List<StudentNotificationUI> = notificationList

    private fun formatTime(raw: String): String {
        return try {
            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            isoFormat.timeZone = TimeZone.getTimeZone("UTC")
            val date = isoFormat.parse(raw) ?: return raw
            val now = System.currentTimeMillis()
            val diff = now - date.time
            val oneDay = 24 * 60 * 60 * 1000L

            when {
                diff < oneDay -> SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)
                else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
            }
        } catch (e: Exception) {
            raw
        }
    }
}
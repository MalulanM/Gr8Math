package com.example.gr8math.adapter // Or your package

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.gr8math.R

// Simple data class
data class TeacherNotification(
    val title: String,
    val description: String,
    val time: String,
    var isRead: Boolean // Used to determine color
)

class TeacherNotificationAdapter(private val notificationList: List<TeacherNotification>) :
    RecyclerView.Adapter<TeacherNotificationAdapter.ViewHolder>() {

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
        val context = holder.itemView.context

        holder.tvTitle.text = item.title
        holder.tvDescription.text = item.description
        holder.tvTime.text = item.time

        // --- LOGIC: Change Icon Color based on Read Status ---
        if (item.isRead) {
            // Green for Read
            holder.ivIcon.setColorFilter(ContextCompat.getColor(context, R.color.colorDarkCyan))
        } else {
            // Yellow (Saffron) for Unread
            holder.ivIcon.setColorFilter(ContextCompat.getColor(context, R.color.saffron))
        }
    }

    override fun getItemCount() = notificationList.size
}
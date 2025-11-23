package com.example.gr8math.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.gr8math.R

// specific data class for Faculty
data class FacultyNotification(
    val title: String,
    val description: String,
    val time: String,
    var isRead: Boolean
)

class FacultyNotificationAdapter(private val notificationList: List<FacultyNotification>) :
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
        holder.tvDescription.text = item.description
        holder.tvTime.text = item.time

        // Logic for Faculty Icons
        // Read = Green (DarkCyan), Unread = Yellow (Saffron)
        if (item.isRead) {
            holder.ivIcon.setColorFilter(ContextCompat.getColor(context, R.color.colorDarkCyan))
        } else {
            holder.ivIcon.setColorFilter(ContextCompat.getColor(context, R.color.saffron))
        }
    }

    override fun getItemCount() = notificationList.size
}
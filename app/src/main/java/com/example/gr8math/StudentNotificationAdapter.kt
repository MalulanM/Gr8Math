package com.example.gr8math.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.gr8math.R

// Data Class
data class StudentNotification(
    val title: String,
    val description: String,
    val time: String,
    var isRead: Boolean
)

class StudentNotificationAdapter(private val notificationList: List<StudentNotification>) :
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
        val context = holder.itemView.context

        holder.tvTitle.text = item.title
        holder.tvDescription.text = item.description
        holder.tvTime.text = item.time

        if (item.isRead) {
            holder.ivIcon.setColorFilter(ContextCompat.getColor(context, R.color.colorDarkCyan))
        } else {
            holder.ivIcon.setColorFilter(ContextCompat.getColor(context, R.color.saffron))
        }
    }

    override fun getItemCount() = notificationList.size
}
package com.example.gr8math.adapter

import android.graphics.Color
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.gr8math.R

class BadgesAdapter(
    private val badges: List<Badge>,
    private val onBadgeClick: (Badge) -> Unit
) : RecyclerView.Adapter<BadgesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivIcon: ImageView = view.findViewById(R.id.ivBadgeIcon)
        val tvTitle: TextView = view.findViewById(R.id.tvBadgeTitle)
        val tvDesc: TextView = view.findViewById(R.id.tvBadgeDesc)       // New
        val tvDate: TextView = view.findViewById(R.id.tvDateAcquired)   // New
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_student_badge, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val badge = badges[position]

        holder.ivIcon.setImageResource(badge.iconResId)
        holder.tvTitle.text = badge.listTitle // <--- Use listTitle here
        holder.tvDesc.text = badge.description
        holder.tvDate.text = badge.dateAcquired

        if (badge.isAcquired) {
            // --- ACQUIRED STATE ---
            holder.tvTitle.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.colorText))
            holder.ivIcon.clearColorFilter()

            holder.itemView.setOnClickListener { onBadgeClick(badge) }
            holder.itemView.alpha = 1.0f

        } else {
            // --- UNACQUIRED STATE ---
            // Gray out title and icon
            holder.tvTitle.setTextColor(Color.GRAY)
            holder.tvDesc.setTextColor(Color.GRAY)
            holder.tvDate.visibility = View.INVISIBLE // Hide date if not acquired

            holder.ivIcon.setColorFilter(Color.DKGRAY, PorterDuff.Mode.SRC_IN)

            holder.itemView.setOnClickListener(null)
            holder.itemView.alpha = 0.7f
        }
    }

    override fun getItemCount() = badges.size
}
package com.example.gr8math.Adapter

import android.graphics.Color
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.gr8math.Data.Repository.BadgeUiModel
import com.example.gr8math.R
import java.text.SimpleDateFormat
import java.util.Locale

class BadgesAdapter(
    private var badges: List<BadgeUiModel>,
    private val onBadgeClick: (BadgeUiModel) -> Unit
) : RecyclerView.Adapter<BadgesAdapter.ViewHolder>() {

    fun updateData(newBadges: List<BadgeUiModel>) {
        badges = newBadges
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivIcon: ImageView = view.findViewById(R.id.ivBadgeIcon)
        val tvTitle: TextView = view.findViewById(R.id.tvBadgeTitle)
        val tvDesc: TextView = view.findViewById(R.id.tvBadgeDesc)
        val tvDate: TextView = view.findViewById(R.id.tvDateAcquired)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_student_badge, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val badge = badges[position]

        holder.ivIcon.setImageResource(badge.imageRes)
        holder.tvTitle.text = badge.name
        holder.tvDesc.text = badge.description

        if (badge.isAcquired) {
            // --- ACQUIRED STATE ---
            holder.tvTitle.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.colorText))
            holder.tvDesc.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.colorSubtleText))

            // Show Date
            holder.tvDate.visibility = View.VISIBLE
            holder.tvDate.text = "Acquired: ${formatDate(badge.dateRewarded)}"

            // Full Color
            holder.ivIcon.clearColorFilter()
            holder.itemView.alpha = 1.0f

            // Enable Click
            holder.itemView.setOnClickListener { onBadgeClick(badge) }

        } else {
            // --- LOCKED STATE ---
            holder.tvTitle.setTextColor(Color.GRAY)
            holder.tvDesc.setTextColor(Color.GRAY)

            // Hide Date
            holder.tvDate.visibility = View.INVISIBLE

            // Gray Tint
            holder.ivIcon.setColorFilter(Color.DKGRAY, PorterDuff.Mode.SRC_IN)
            holder.itemView.alpha = 0.6f

            // Disable Click
            holder.itemView.setOnClickListener(null)
        }
    }

    override fun getItemCount() = badges.size

    private fun formatDate(dbDate: String?): String {
        if (dbDate.isNullOrEmpty()) return "Unknown Date"

        return try {
            val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)

            // Ensure UTC timezone since Supabase saves it in UTC
            inputFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")

            val parsedDate = inputFormat.parse(dbDate)

            // Output format: e.g., "Feb 18, 2026"
            val outputFormat = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.US)
            outputFormat.timeZone = java.util.TimeZone.getDefault() // Convert to user's local time

            outputFormat.format(parsedDate!!)

        } catch (e: Exception) {
            // If it fails, try simple date format just in case
            try {
                val simpleInput = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                val parsedDate = simpleInput.parse(dbDate)
                java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.US).format(parsedDate!!)
            } catch (e2: Exception) {
                dbDate // Absolute fallback: just show the raw string from DB
            }
        }
    }
}
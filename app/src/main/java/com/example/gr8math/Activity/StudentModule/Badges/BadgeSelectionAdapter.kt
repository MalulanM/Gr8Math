package com.example.gr8math.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.gr8math.R

class BadgeSelectionAdapter(
    private val badges: List<Badge>,
    private val maxSelection: Int = 3
) : RecyclerView.Adapter<BadgeSelectionAdapter.ViewHolder>() {

    // Track selected IDs
    val selectedBadges = mutableListOf<Badge>()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val radioButton: RadioButton = view.findViewById(R.id.rbSelect)
        val ivIcon: ImageView = view.findViewById(R.id.ivBadgeIcon)
        val tvTitle: TextView = view.findViewById(R.id.tvBadgeTitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_select_badge, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val badge = badges[position]

        holder.tvTitle.text = badge.listTitle // Use short title
        holder.ivIcon.setImageResource(badge.iconResId)

        // Check state
        holder.radioButton.isChecked = selectedBadges.contains(badge)

        // Handle Click
        holder.itemView.setOnClickListener {
            if (selectedBadges.contains(badge)) {
                // Deselect
                selectedBadges.remove(badge)
                notifyItemChanged(position)
            } else {
                // Attempt Select
                if (selectedBadges.size < maxSelection) {
                    selectedBadges.add(badge)
                    notifyItemChanged(position)
                } else {
                    Toast.makeText(holder.itemView.context, "Maximum of $maxSelection badges allowed.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun getItemCount() = badges.size
}
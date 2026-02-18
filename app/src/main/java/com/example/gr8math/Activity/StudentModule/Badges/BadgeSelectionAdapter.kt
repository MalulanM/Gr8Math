package com.example.gr8math.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.gr8math.Data.Repository.BadgeUiModel
import com.example.gr8math.R

class BadgeSelectionAdapter(
    private val badges: List<BadgeUiModel>,
    private val maxSelection: Int = 3
) : RecyclerView.Adapter<BadgeSelectionAdapter.ViewHolder>() {

    val selectedBadges = mutableListOf<BadgeUiModel>()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val radioButton: RadioButton = view.findViewById(R.id.rbSelect)
        val ivIcon: ImageView = view.findViewById(R.id.ivBadgeIcon)
        val tvTitle: TextView = view.findViewById(R.id.tvBadgeTitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Using your exact layout name!
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_select_badge, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val badge = badges[position]

        // 🌟 CHANGED: Mapped to the new BadgeUiModel fields from your DB
        holder.tvTitle.text = badge.name
        holder.ivIcon.setImageResource(badge.imageRes)

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
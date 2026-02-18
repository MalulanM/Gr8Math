package com.example.gr8math.Activity.TeacherModule.Profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.gr8math.Data.Repository.TeacherAchievementEntity
import com.example.gr8math.R
import java.text.SimpleDateFormat
import java.util.Locale

class AchievementAdapter(
    private val onCertClick: (String) -> Unit,
    private val onDeleteClick: (TeacherAchievementEntity) -> Unit // 🌟 ADDED
) : RecyclerView.Adapter<AchievementAdapter.ViewHolder>() {

    private var items = listOf<TeacherAchievementEntity>()

    fun setAchievements(newList: List<TeacherAchievementEntity>) {
        this.items = newList
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivPreview: ImageView = view.findViewById(R.id.ivItemCertPreview)
        val tvName: TextView = view.findViewById(R.id.tvItemCertName)
        val tvDate: TextView = view.findViewById(R.id.tvItemCertDate)
        val ivDelete: ImageView = view.findViewById(R.id.ivDeleteAchievement) // 🌟 ADDED
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_achievement, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvName.text = item.achievementDesc
        holder.tvDate.text = "Achieved: ${item.dateAcquired}" // Or your date formatting

        if (!item.certificate.isNullOrEmpty()) {
            Glide.with(holder.itemView.context).load(item.certificate).into(holder.ivPreview)
            holder.ivPreview.setOnClickListener { onCertClick(item.certificate) }
        }


        holder.ivDelete.setOnClickListener { onDeleteClick(item) }
    }

    override fun getItemCount() = items.size
}
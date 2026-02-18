package com.example.gr8math.Activity.TeacherModule.Participants

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.gr8math.Data.Model.Participant
import com.example.gr8math.R

class ParticipantsAdapter(
    private val participants: List<Participant>,
    private val onParticipantClick: (Participant) -> Unit
) : RecyclerView.Adapter<ParticipantsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvRank: TextView = view.findViewById(R.id.tvRank)
        val tvName: TextView = view.findViewById(R.id.tvParticipantName)
        val card: View = view.findViewById<View>(R.id.ivArrow).parent as View
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_participant_rank, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val student = participants[position]

        val rankSuffix = getRankSuffix(student.rank)
        holder.tvRank.text = "${student.rank}$rankSuffix"
        holder.tvName.text = student.name

        holder.card.setOnClickListener {
            onParticipantClick(student)
        }
    }

    override fun getItemCount() = participants.size

    private fun getRankSuffix(rank: Int): String {
        if (rank in 11..13) return "th"
        return when (rank % 10) {
            1 -> "st"
            2 -> "nd"
            3 -> "rd"
            else -> "th"
        }
    }
}
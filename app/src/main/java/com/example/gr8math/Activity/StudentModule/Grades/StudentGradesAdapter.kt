package com.example.gr8math.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.gr8math.Data.Model.StudentScore
import com.example.gr8math.R

class StudentGradesAdapter(
    private var gradeList: List<StudentScore>,
    private val onClick: (StudentScore) -> Unit
) : RecyclerView.Adapter<StudentGradesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tvTitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_class_assessment_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = gradeList[position]
        holder.title.text = "Assessment ${item.assessmentNumber}"
        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = gradeList.size

    fun updateList(newList: List<StudentScore>) {
        gradeList = newList
        notifyDataSetChanged()
    }
}
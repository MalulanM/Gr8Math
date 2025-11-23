package com.example.gr8math.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.gr8math.R
import com.example.gr8math.dataObject.TeacherClass

class ClassAdapter(
    private var classList: MutableList<TeacherClass>,
    private val onItemClick: (TeacherClass) -> Unit   // <-- NEW
) : RecyclerView.Adapter<ClassAdapter.ClassViewHolder>() {

    inner class ClassViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvSectionName: TextView = itemView.findViewById(R.id.tvSectionName)
        val tvSchedule: TextView = itemView.findViewById(R.id.tvSchedule)
        val tvStudentCount: TextView = itemView.findViewById(R.id.tvStudentCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClassViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_teacher_class_card, parent, false)
        return ClassViewHolder(view)
    }

    override fun onBindViewHolder(holder: ClassViewHolder, position: Int) {
        val item = classList[position]
        holder.tvSectionName.text = item.sectionName
        holder.tvSchedule.text = item.schedule
        holder.tvStudentCount.text = "${item.studentCount} students"


        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount(): Int = classList.size

    fun updateData(newList: List<TeacherClass>) {
        classList.clear()
        classList.addAll(newList)
        notifyDataSetChanged()
    }
}

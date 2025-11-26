package com.example.gr8math.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.gr8math.R

data class GradeItem(
    val title: String,
    val score: Int,
    val assessmentNumber : Int,
    val id : Int,
    val dateAccomplished : String,
    val assessment_items : Int)

class StudentGradesAdapter(
    private val grades: List<GradeItem>,
    private val onClick: (GradeItem) -> Unit
) : RecyclerView.Adapter<StudentGradesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tvTitle)
        // We use the arrow ID to get the parent card or click area
        val card: View = view.findViewById<View>(R.id.ivArrow).parent as View
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Reusing the existing assessment card layout
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_class_assessment_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = grades[position]
        holder.title.text = item.title

        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = grades.size
}
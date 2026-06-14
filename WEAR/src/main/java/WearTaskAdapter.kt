package com.example.todolist.wear

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class WearTaskAdapter(
    private val onToggle: (WearTask) -> Unit,
    private val onDelete: (WearTask) -> Unit
) : ListAdapter<WearTask, WearTaskAdapter.TaskViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_wear_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtTitle: TextView = itemView.findViewById(R.id.txtWearTaskTitle)
        private val txtCategory: TextView = itemView.findViewById(R.id.txtWearCategory)
        private val btnCheck: ImageButton = itemView.findViewById(R.id.btnWearCheck)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnWearDelete)
        private val priorityBar: View = itemView.findViewById(R.id.viewPriorityBar)

        fun bind(task: WearTask) {
            txtTitle.text = task.title
            txtCategory.text = getCategoryEmoji(task.category)

            // Strikethrough agar done
            if (task.done) {
                txtTitle.paintFlags = txtTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                txtTitle.alpha = 0.5f
                btnCheck.setImageResource(R.drawable.ic_wear_check_filled)
                btnCheck.alpha = 1f
            } else {
                txtTitle.paintFlags = txtTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                txtTitle.alpha = 1f
                btnCheck.setImageResource(R.drawable.ic_wear_check_outline)
                btnCheck.alpha = 0.7f
            }

            // Priority color bar
            priorityBar.setBackgroundColor(getPriorityColor(task.priority))

            btnCheck.setOnClickListener { onToggle(task) }
            btnDelete.setOnClickListener { onDelete(task) }
        }

        private fun getCategoryEmoji(category: String): String = when (category.uppercase()) {
            "GYM", "FITNESS" -> "💪"
            "STUDY" -> "📚"
            "WORK" -> "💼"
            "GAMING" -> "🎮"
            "FOOD" -> "🍽️"
            "HEALTH" -> "❤️"
            "CODING" -> "💻"
            "SOCIAL" -> "👥"
            "SLEEP" -> "😴"
            "MUSIC" -> "🎵"
            "TRAVEL" -> "✈️"
            "FINANCE" -> "💰"
            else -> "📌"
        }

        private fun getPriorityColor(priority: String): Int = when (priority.uppercase()) {
            "HIGH" -> 0xFFFF5252.toInt()
            "MEDIUM" -> 0xFFFFB74D.toInt()
            "LOW" -> 0xFF69F0AE.toInt()
            else -> 0xFF7B7B7B.toInt()
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<WearTask>() {
            override fun areItemsTheSame(old: WearTask, new: WearTask) =
                old.firestoreId == new.firestoreId

            override fun areContentsTheSame(old: WearTask, new: WearTask) = old == new
        }
    }
}
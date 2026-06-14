package com.example.todolist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SuggestionAdapter(private val onSuggestionClick: (String) -> Unit) :
    RecyclerView.Adapter<SuggestionAdapter.ViewHolder>() {

    private var suggestions = listOf<String>()

    fun submitList(list: List<String>) {
        suggestions = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_suggestion, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val suggestion = suggestions[position]
        holder.textSuggestion.text = suggestion
        holder.itemView.setOnClickListener { onSuggestionClick(suggestion) }
    }

    override fun getItemCount() = suggestions.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textSuggestion: TextView = view.findViewById(R.id.textSuggestion)
    }
}

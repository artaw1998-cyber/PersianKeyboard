package com.example.persiankeyboard.clipboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ClipboardAdapter(
    private val items: List<ClipboardEntity>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<ClipboardAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(android.R.id.text1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val displayText = if (item.text.length > 80) item.text.take(80) + "..." else item.text
        holder.textView.text = displayText
        holder.textView.setTextColor(android.graphics.Color.WHITE)
        
        holder.itemView.setOnClickListener {
            onItemClick(item.text)
        }
    }

    override fun getItemCount(): Int = items.size
}

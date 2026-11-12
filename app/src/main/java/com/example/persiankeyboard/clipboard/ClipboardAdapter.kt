package com.example.persiankeyboard.clipboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ClipboardAdapter(
    private val items: List<ClipboardEntity>,
    private val onItemClick: (ClipboardEntity) -> Unit,
    private val onDeleteClick: (ClipboardEntity) -> Unit
) : RecyclerView.Adapter<ClipboardAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNumber: TextView = view.findViewById(R.id.tvItemNumber)
        val tvText: TextView = view.findViewById(R.id.tvItemText)
        val btnEdit: TextView = view.findViewById(R.id.btnEdit)
        val btnDelete: TextView = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_clipboard, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvNumber.text = (position + 1).toString()
        holder.tvText.text = item.text
        
        // کلیک روی متن (تایپ شدن متن)
        holder.itemView.setOnClickListener { onItemClick(item) }
        
        // کلیک روی مداد (فعلاً متن رو تایپ می‌کنه تا کاربر خودش ویرایش کنه)
        holder.btnEdit.setOnClickListener { onItemClick(item) }
        
        // کلیک روی سطل زباله (حذف)
        holder.btnDelete.setOnClickListener { onDeleteClick(item) }
    }

    override fun getItemCount(): Int = items.size
}

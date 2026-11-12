package com.example.persiankeyboard

import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class EmojiAdapter(
    private val emojis: List<String>,
    private val onEmojiClick: (String) -> Unit
) : RecyclerView.Adapter<EmojiAdapter.EmojiViewHolder>() {

    class EmojiViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmojiViewHolder {
        val textView = TextView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            gravity = Gravity.CENTER
            textSize = 26f 
            setPadding(0, 16, 0, 16)
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
        }
        return EmojiViewHolder(textView)
    }

    override fun onBindViewHolder(holder: EmojiViewHolder, position: Int) {
        val emoji = emojis[position]
        holder.textView.text = emoji
        
        holder.textView.setOnClickListener(null)
        holder.textView.setOnClickListener {
            onEmojiClick(emoji)
        }
    }

    override fun getItemCount(): Int = emojis.size
}

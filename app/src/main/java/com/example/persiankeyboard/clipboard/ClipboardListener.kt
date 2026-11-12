package com.example.persiankeyboard.clipboard

import android.content.ClipboardManager
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ClipboardListener(
    private val context: Context,
    private val dao: ClipboardDao
) {
    private val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    private val scope = CoroutineScope(Dispatchers.IO)

    private val listener = ClipboardManager.OnPrimaryClipChangedListener {
        if (clipboardManager.hasPrimaryClip()) {
            val clipData = clipboardManager.primaryClip
            if (clipData != null && clipData.itemCount > 0) {
                val text = clipData.getItemAt(0).text?.toString() ?: ""
                if (text.isNotBlank()) {
                    scope.launch {
                        dao.insert(ClipboardEntity(text = text))
                        dao.keepOnlyLatest200()
                    }
                }
            }
        }
    }

    fun startListening() {
        clipboardManager.addPrimaryClipChangedListener(listener)
    }

    fun stopListening() {
        clipboardManager.removePrimaryClipChangedListener(listener)
    }
}

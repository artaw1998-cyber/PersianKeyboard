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

    // این شنونده هر بار که محتوای کلیپ‌بورد عوض شود (چیزی کپی شود) اجرا می‌شود
    private val listener = ClipboardManager.OnPrimaryClipChangedListener {
        if (clipboardManager.hasPrimaryClip()) {
            val clipData = clipboardManager.primaryClip
            if (clipData != null && clipData.itemCount > 0) {
                val text = clipData.getItemAt(0).text?.toString() ?: ""
                if (text.isNotBlank()) {
                    scope.launch {
                        // ذخیره در دیتابیس
                        dao.insert(ClipboardEntity(text = text))
                        // 🌟 جادوی ۲۰۰ تایی: پاک کردن موارد قدیمی
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

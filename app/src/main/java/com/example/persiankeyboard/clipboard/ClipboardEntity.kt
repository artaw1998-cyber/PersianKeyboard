package com.example.persiankeyboard.clipboard

import androidx.room.Entity
import androidx.room.PrimaryKey

// این کلاس مشخص می‌کند که ما یک جدول به اسم clipboard_items داریم
@Entity(tableName = "clipboard_items")
data class ClipboardEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val text: String,        // خود متن (می‌تواند طولانی باشد)
    val timestamp: Long = System.currentTimeMillis() // زمان کپی شدن برای مرتب‌سازی
)

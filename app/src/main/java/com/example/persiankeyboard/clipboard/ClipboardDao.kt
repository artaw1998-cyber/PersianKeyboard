package com.example.persiankeyboard.clipboard

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ClipboardDao {
    // اضافه کردن یک متن جدید
    @Insert
    fun insert(item: ClipboardEntity)

    // گرفتن تمام متن‌ها به ترتیب زمان (جدیدترین اول)
    @Query("SELECT * FROM clipboard_items ORDER BY timestamp DESC")
    fun getAllItems(): List<ClipboardEntity>

    // پاک کردن یک متن با آیدی
    @Query("DELETE FROM clipboard_items WHERE id = :id")
    fun deleteById(id: Int)

    // پاک کردن کل تاریخچه
    @Query("DELETE FROM clipboard_items")
    fun clearAll()

    // 🌟 جادوی ۲۰۰ تایی: نگه داشتن فقط ۲۰۰ مورد آخر و حذف بقیه
    @Query("DELETE FROM clipboard_items WHERE id NOT IN (SELECT id FROM clipboard_items ORDER BY timestamp DESC LIMIT 200)")
    fun keepOnlyLatest200()
}

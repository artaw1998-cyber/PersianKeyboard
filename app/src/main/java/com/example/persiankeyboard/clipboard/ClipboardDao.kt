package com.example.persiankeyboard.clipboard

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface ClipboardDao {
    @Insert
    fun insert(item: ClipboardEntity)

    @Update
    suspend fun update(item: ClipboardEntity)

    @Query("SELECT * FROM clipboard_items ORDER BY timestamp DESC")
    fun getAllItems(): List<ClipboardEntity>

    @Query("DELETE FROM clipboard_items WHERE id = :id")
    fun deleteById(id: Int)

    @Query("DELETE FROM clipboard_items")
    fun clearAll()

    @Query("DELETE FROM clipboard_items")
    suspend fun deleteAll()

    @Query("DELETE FROM clipboard_items WHERE id NOT IN (SELECT id FROM clipboard_items ORDER BY timestamp DESC LIMIT 200)")
    fun keepOnlyLatest200()
}

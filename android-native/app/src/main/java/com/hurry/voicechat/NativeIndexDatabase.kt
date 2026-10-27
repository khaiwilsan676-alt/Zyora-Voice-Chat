package com.hurry.voicechat

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

internal class NativeIndexDatabase(context: Context) : SQLiteOpenHelper(context.applicationContext, "index.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS chat_cache (chat_key TEXT PRIMARY KEY NOT NULL, payload TEXT NOT NULL, updated_at INTEGER NOT NULL)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
    fun saveChat(key: String, payload: String) {
        val values = ContentValues()
        values.put("chat_key", key)
        values.put("payload", payload)
        values.put("updated_at", System.currentTimeMillis())
        writableDatabase.insertWithOnConflict("chat_cache", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }
    fun loadChat(key: String): String? {
        readableDatabase.query("chat_cache", arrayOf("payload"), "chat_key=?", arrayOf(key), null, null, null).use { cursor ->
            return if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }
}
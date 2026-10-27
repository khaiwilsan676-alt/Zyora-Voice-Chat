package com.hurry.voicechat

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONObject

internal class NativeIndexDatabase(context: Context) : SQLiteOpenHelper(context.applicationContext, "index.db", null, 2) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS chat_cache (chat_key TEXT PRIMARY KEY NOT NULL, payload TEXT NOT NULL, updated_at INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS profile_cache (profile_key TEXT PRIMARY KEY NOT NULL, payload TEXT NOT NULL, updated_at INTEGER NOT NULL)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) db.execSQL("CREATE TABLE IF NOT EXISTS profile_cache (profile_key TEXT PRIMARY KEY NOT NULL, payload TEXT NOT NULL, updated_at INTEGER NOT NULL)")
    }
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
    fun saveProfile(key: String, payload: JSONObject) {
        if (key.isBlank()) return
        val values = ContentValues().apply {
            put("profile_key", key)
            put("payload", payload.toString())
            put("updated_at", System.currentTimeMillis())
        }
        writableDatabase.insertWithOnConflict("profile_cache", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }
    fun loadProfile(key: String): JSONObject? {
        if (key.isBlank()) return null
        readableDatabase.query("profile_cache", arrayOf("payload"), "profile_key=?", arrayOf(key), null, null, null).use { cursor ->
            return if (cursor.moveToFirst()) runCatching { JSONObject(cursor.getString(0)) }.getOrNull() else null
        }
    }
}
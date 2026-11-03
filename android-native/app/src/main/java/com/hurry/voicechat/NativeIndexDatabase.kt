package com.hurry.voicechat

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONObject

internal class NativeIndexDatabase(context: Context) : SQLiteOpenHelper(context.applicationContext, "index.db", null, 4) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS chat_cache (chat_key TEXT PRIMARY KEY NOT NULL, payload TEXT NOT NULL, updated_at INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS profile_cache (profile_key TEXT PRIMARY KEY NOT NULL, payload TEXT NOT NULL, updated_at INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS conversation_cache (conversation_key TEXT PRIMARY KEY NOT NULL, peer_id TEXT NOT NULL, payload TEXT NOT NULL, updated_at INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS voice_cache (message_id TEXT PRIMARY KEY NOT NULL, peer_id TEXT NOT NULL, audio BLOB NOT NULL, duration_seconds INTEGER NOT NULL, updated_at INTEGER NOT NULL)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) db.execSQL("CREATE TABLE IF NOT EXISTS profile_cache (profile_key TEXT PRIMARY KEY NOT NULL, payload TEXT NOT NULL, updated_at INTEGER NOT NULL)")
        if (oldVersion < 3) db.execSQL("CREATE TABLE IF NOT EXISTS conversation_cache (conversation_key TEXT PRIMARY KEY NOT NULL, peer_id TEXT NOT NULL, payload TEXT NOT NULL, updated_at INTEGER NOT NULL)")
        if (oldVersion < 4) db.execSQL("CREATE TABLE IF NOT EXISTS voice_cache (message_id TEXT PRIMARY KEY NOT NULL, peer_id TEXT NOT NULL, audio BLOB NOT NULL, duration_seconds INTEGER NOT NULL, updated_at INTEGER NOT NULL)")
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

    fun saveConversation(key: String, peerId: String, payload: JSONObject) {
        if (key.isBlank() || peerId.isBlank()) return
        val values = ContentValues().apply {
            put("conversation_key", key)
            put("peer_id", peerId)
            put("payload", payload.toString())
            put("updated_at", System.currentTimeMillis())
        }
        writableDatabase.insertWithOnConflict("conversation_cache", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }
    fun loadConversations(keyPrefix: String): List<JSONObject> {
        val result = mutableListOf<JSONObject>()
        readableDatabase.query(
            "conversation_cache", arrayOf("payload"), "conversation_key LIKE ?",
            arrayOf("$keyPrefix%"), null, null, "updated_at DESC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                runCatching { JSONObject(cursor.getString(0)) }.getOrNull()?.let(result::add)
            }
        }
        return result
    }

    fun saveVoiceNote(messageId: String, peerId: String, audio: ByteArray, durationSeconds: Long) {
        if (messageId.isBlank() || peerId.isBlank() || audio.isEmpty()) return
        val values = ContentValues().apply {
            put("message_id", messageId)
            put("peer_id", peerId)
            put("audio", audio)
            put("duration_seconds", durationSeconds.coerceIn(1L, 90L))
            put("updated_at", System.currentTimeMillis())
        }
        writableDatabase.insertWithOnConflict("voice_cache", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun loadVoiceNote(messageId: String): ByteArray? {
        if (messageId.isBlank()) return null
        readableDatabase.query("voice_cache", arrayOf("audio"), "message_id=?", arrayOf(messageId), null, null, null).use { cursor ->
            return if (cursor.moveToFirst()) cursor.getBlob(0) else null
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
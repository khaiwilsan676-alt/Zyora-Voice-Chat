package com.hurry.voicechat

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONObject

internal class NativeIndexDatabase(context: Context) : SQLiteOpenHelper(context.applicationContext, "index.db", null, 7) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS chat_cache (chat_key TEXT PRIMARY KEY NOT NULL, payload TEXT NOT NULL, updated_at INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS profile_cache (profile_key TEXT PRIMARY KEY NOT NULL, payload TEXT NOT NULL, updated_at INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS conversation_cache (conversation_key TEXT PRIMARY KEY NOT NULL, peer_id TEXT NOT NULL, payload TEXT NOT NULL, updated_at INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS voice_cache (message_id TEXT PRIMARY KEY NOT NULL, peer_id TEXT NOT NULL, audio BLOB NOT NULL, duration_seconds INTEGER NOT NULL, updated_at INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS room_lock_cache (room_id TEXT PRIMARY KEY NOT NULL, is_locked INTEGER NOT NULL, room_password TEXT NOT NULL, updated_at INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS lucky_bag_transactions (transaction_id TEXT PRIMARY KEY NOT NULL, user_id TEXT NOT NULL, type TEXT NOT NULL, amount INTEGER NOT NULL, bag_id TEXT NOT NULL, created_at INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS user_reports (report_id TEXT PRIMARY KEY NOT NULL, payload TEXT NOT NULL, created_at INTEGER NOT NULL)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) db.execSQL("CREATE TABLE IF NOT EXISTS profile_cache (profile_key TEXT PRIMARY KEY NOT NULL, payload TEXT NOT NULL, updated_at INTEGER NOT NULL)")
        if (oldVersion < 3) db.execSQL("CREATE TABLE IF NOT EXISTS conversation_cache (conversation_key TEXT PRIMARY KEY NOT NULL, peer_id TEXT NOT NULL, payload TEXT NOT NULL, updated_at INTEGER NOT NULL)")
        if (oldVersion < 4) db.execSQL("CREATE TABLE IF NOT EXISTS voice_cache (message_id TEXT PRIMARY KEY NOT NULL, peer_id TEXT NOT NULL, audio BLOB NOT NULL, duration_seconds INTEGER NOT NULL, updated_at INTEGER NOT NULL)")
        if (oldVersion < 5) db.execSQL("CREATE TABLE IF NOT EXISTS room_lock_cache (room_id TEXT PRIMARY KEY NOT NULL, is_locked INTEGER NOT NULL, room_password TEXT NOT NULL, updated_at INTEGER NOT NULL)")
        if (oldVersion < 6) db.execSQL("CREATE TABLE IF NOT EXISTS lucky_bag_transactions (transaction_id TEXT PRIMARY KEY NOT NULL, user_id TEXT NOT NULL, type TEXT NOT NULL, amount INTEGER NOT NULL, bag_id TEXT NOT NULL, created_at INTEGER NOT NULL)")
        if (oldVersion < 7) db.execSQL("CREATE TABLE IF NOT EXISTS user_reports (report_id TEXT PRIMARY KEY NOT NULL, payload TEXT NOT NULL, created_at INTEGER NOT NULL)")
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

    fun saveRoomLock(roomId: String, isLocked: Boolean, password: String) {
        if (roomId.isBlank()) return
        val values = ContentValues().apply {
            put("room_id", roomId)
            put("is_locked", if (isLocked) 1 else 0)
            put("room_password", password)
            put("updated_at", System.currentTimeMillis())
        }
        writableDatabase.insertWithOnConflict("room_lock_cache", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }
    fun loadRoomLock(roomId: String): Pair<Boolean, String>? {
        if (roomId.isBlank()) return null
        readableDatabase.query("room_lock_cache", arrayOf("is_locked", "room_password"), "room_id=?", arrayOf(roomId), null, null, null).use { cursor ->
            return if (cursor.moveToFirst()) Pair(cursor.getInt(0) == 1, cursor.getString(1).orEmpty()) else null
        }
    }

    /** Lucky Bag history is device-local in index.db; it is never uploaded to MongoDB. */
    fun saveLuckyBagTransaction(transactionId: String, userId: String, type: String, amount: Long, bagId: String, createdAt: Long) {
        if (transactionId.isBlank() || userId.isBlank() || type.isBlank()) return
        val values = ContentValues().apply {
            put("transaction_id", transactionId)
            put("user_id", userId)
            put("type", type)
            put("amount", amount)
            put("bag_id", bagId)
            put("created_at", createdAt)
        }
        writableDatabase.insertWithOnConflict("lucky_bag_transactions", null, values, SQLiteDatabase.CONFLICT_IGNORE)
    }

    fun loadLuckyBagTransactions(userId: String, limit: Int = 100): List<JSONObject> {
        if (userId.isBlank()) return emptyList()
        val result = mutableListOf<JSONObject>()
        readableDatabase.query(
            "lucky_bag_transactions",
            arrayOf("transaction_id", "user_id", "type", "amount", "bag_id", "created_at"),
            "user_id=?",
            arrayOf(userId),
            null, null, "created_at DESC", limit.coerceIn(1, 500).toString()
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result += JSONObject().put("id", cursor.getString(0))
                    .put("userId", cursor.getString(1))
                    .put("type", cursor.getString(2))
                    .put("amount", cursor.getLong(3))
                    .put("bagId", cursor.getString(4))
                    .put("createdAt", cursor.getLong(5))
            }
        }
        return result
    }

    /** User-submitted reports stay on this device in index.db; never sync this table to MongoDB. */
    fun saveUserReport(reportId: String, payload: JSONObject) {
        if (reportId.isBlank()) return
        val values = ContentValues().apply {
            put("report_id", reportId)
            put("payload", payload.toString())
            put("created_at", payload.optLong("timestamp", System.currentTimeMillis()))
        }
        writableDatabase.insertWithOnConflict("user_reports", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun loadUserReports(limit: Int = 100): List<JSONObject> {
        val result = mutableListOf<JSONObject>()
        readableDatabase.query("user_reports", arrayOf("payload"), null, null, null, null, "created_at DESC", limit.coerceIn(1, 500).toString()).use { cursor ->
            while (cursor.moveToNext()) {
                runCatching { JSONObject(cursor.getString(0)) }.getOrNull()?.let(result::add)
            }
        }
        return result
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
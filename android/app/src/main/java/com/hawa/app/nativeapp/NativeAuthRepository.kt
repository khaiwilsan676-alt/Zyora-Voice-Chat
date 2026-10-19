package com.hawa.app.nativeapp

import android.util.Base64
import org.json.JSONObject

class NativeAuthRepository(private val repository: NativeRepository = NativeRepository()) {
    fun accountNumber(uid: String): String {
        val special = mapOf("100002" to "100002", "100003" to "100003")
        special[uid]?.let { return it }
        if (uid in listOf("500001","500002","500003","500004","500005","700001","700002","700003")) return uid
        val bytes = uid.toByteArray()
        val digest = bytes.fold(0) { hash, b -> (hash * 31 + b.toInt()) }
        return (10000000 + (kotlin.math.abs(digest) % 90000000)).toString()
    }

    suspend fun loadUser(uid: String): JSONObject = repository.user(uid)

    suspend fun syncUser(uid: String, name: String, email: String, image: String): JSONObject {
        val existing = runCatching { loadUser(uid) }.getOrElse { JSONObject() }
        val account = existing.optString("accountId").ifBlank { accountNumber(uid) }
        val finalName = existing.optString("name").ifBlank { name.ifBlank { email.substringBefore("@").ifBlank { "User" } } }
        val finalImage = existing.optString("image").ifBlank { image.ifBlank { "/default-avatar.png" } }
        return repository.saveUser(JSONObject().apply {
            put("id", uid); put("uid", uid); put("appLongId", uid)
            put("name", finalName); put("email", email); put("gmail", email)
            put("image", finalImage); put("avatar", finalImage)
            put("accountId", account); put("accountNumber", account)
        })
    }
}

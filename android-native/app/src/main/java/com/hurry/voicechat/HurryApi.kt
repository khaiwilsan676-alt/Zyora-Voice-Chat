package com.hurry.voicechat

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private const val API_BASE = "https://zyora-server.onrender.com"

data class RoomSyncResult(val rooms: List<HurryRoom>, val serverTime: Long)

object HurryApi {
    suspend fun bindInvite(inviterId: String, targetId: String): JSONObject? = inviteRequest("/api/invites/bind", JSONObject().put("inviterId", inviterId).put("targetId", targetId))
    suspend fun claimInvite(userId: String): JSONObject? = inviteRequest("/api/invites/claim", JSONObject().put("userId", userId))
    suspend fun inviteStats(userId: String): JSONObject? = withContext(Dispatchers.IO) {
        runCatching {
            val encoded = java.net.URLEncoder.encode(userId, "UTF-8")
            val c = URL("$API_BASE/api/invites/$encoded").openConnection() as HttpURLConnection
            c.requestMethod = "GET"; c.connectTimeout = 8000; c.readTimeout = 8000
            try { val stream = if (c.responseCode in 200..299) c.inputStream else c.errorStream; JSONObject(stream.bufferedReader().use { it.readText() }) } finally { c.disconnect() }
        }.getOrNull()
    }
    private suspend fun inviteRequest(path: String, body: JSONObject): JSONObject? = withContext(Dispatchers.IO) {
        runCatching {
            val c = URL("$API_BASE$path").openConnection() as HttpURLConnection
            c.requestMethod = "POST"; c.doOutput = true; c.connectTimeout = 8000; c.readTimeout = 8000
            c.setRequestProperty("Content-Type", "application/json")
            c.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            try { val stream = if (c.responseCode in 200..299) c.inputStream else c.errorStream; JSONObject(stream.bufferedReader().use { it.readText() }) } finally { c.disconnect() }
        }.getOrNull()
    }

    suspend fun roomsUpdatedSince(sinceUpdatedAt: Long): RoomSyncResult? = withContext(Dispatchers.IO) {
        runCatching {
            val endpoint = "$API_BASE/api/rooms?sinceUpdatedAt=$sinceUpdatedAt"
            val c = URL(endpoint).openConnection() as HttpURLConnection
            c.requestMethod = "GET"
            c.connectTimeout = 5000
            c.readTimeout = 5000
            c.useCaches = true
            try {
                val status = c.responseCode
                if (status !in 200..299) throw java.io.IOException("Room sync failed: HTTP $status")
                val root = JSONObject(c.inputStream.bufferedReader().use { it.readText() })
                val array = root.optJSONArray("rooms") ?: JSONArray()
                val parsedRooms = buildList {
                    for (i in 0 until array.length()) {
                        val o = array.optJSONObject(i) ?: continue
                        val id = o.optString("roomId").takeIf { it.isNotBlank() && it != "null" }
                            ?: o.optString("accountId").takeIf { it.isNotBlank() && it != "null" }
                            ?: o.optString("id").takeIf { it.isNotBlank() && it != "null" }
                            ?: continue
                        val rawImage = sequenceOf("dp", "image", "roomDp", "roomImage", "avatarUrl", "profileImage")
                            .map { key -> o.optString(key, "").trim() }
                            .firstOrNull { it.isNotEmpty() && it != "null" && it != "undefined" }
                            .orEmpty()
                        val roomImage = when {
                            rawImage.startsWith("data:image/", ignoreCase = true) || rawImage.isBlank() ->
                                "$API_BASE/api/rooms/image?roomId=${java.net.URLEncoder.encode(id, "UTF-8")}"
                            rawImage.startsWith("/") -> "$API_BASE$rawImage"
                            else -> rawImage
                        }
                        add(
                            HurryRoom(
                                id = id,
                                name = o.optString("roomName", o.optString("name", "Room")),
                                image = roomImage,
                                announcement = o.optString("announcement", o.optString("message", "")),
                                country = sequenceOf("countryName", "country", "countryCode", "country_code", "nationality", "location", "flag")
                                    .map { key -> o.optString(key, "").trim() }
                                    .firstOrNull { it.isNotBlank() && it != "null" && it != "undefined" }
                                    .orEmpty(),
                                locked = o.optBoolean("isLocked", false),
                                accountId = o.optString("accountId", id).takeIf { it.isNotBlank() && it != "null" } ?: id
                            )
                        )
                    }
                }
                RoomSyncResult(parsedRooms, root.optLong("serverTime", 0L))
            } finally {
                c.disconnect()
            }
        }.getOrNull()
    }
}

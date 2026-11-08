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
                val body = c.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(body)
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
                        add(HurryRoom(
                            id = id,
                            name = o.optString("roomName", o.optString("name", "Room")),
                            image = roomImage,
                            announcement = o.optString("announcement", o.optString("message", "")),
                            country = sequenceOf("countryName", "country", "countryCode", "country_code", "nationality", "location", "flag").map { key -> o.optString(key, "").trim() }.firstOrNull { it.isNotBlank() && it != "null" && it != "undefined" }.orEmpty(),
                            locked = o.optBoolean("isLocked", false),
                            accountId = o.optString("accountId", id).takeIf { it.isNotBlank() && it != "null" } ?: id
                        ))
                    }
                }
                RoomSyncResult(parsedRooms, root.optLong("serverTime", 0L))
            } finally {
                c.disconnect()
            }
            /*
            val root = JSONObject(body)
            val array = root.optJSONArray("rooms") ?: JSONArray()
            val parsedRooms = buildList {
                for (i in 0 until array.length()) {
                    val o = array.optJSONObject(i) ?: continue
                    val id = o.optString("roomId").takeIf { it.isNotBlank() && it != "null" }
                        ?: o.optString("accountId").takeIf { it.isNotBlank() && it != "null" }
                        ?: o.optString("id").takeIf { it.isNotBlank() && it != "null" }
                        ?: continue
                    // Support DP field names used by both legacy and current room records.
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
                            country = sequenceOf("countryName", "country", "countryCode", "country_code", "nationality", "location", "flag").map { key -> o.optString(key, "").trim() }.firstOrNull { it.isNotBlank() && it != "null" && it != "undefined" }.orEmpty(),
                            locked = o.optBoolean("isLocked", false),
                            accountId = o.optString("accountId", id).takeIf { it.isNotBlank() && it != "null" } ?: id
                        )
                    )
                }
            }
            RoomSyncResult(parsedRooms, root.optLong("serverTime", 0L)) */
        }.getOrNull()
    }
}

package com.hurry.voicechat

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

internal object ProfilePersistence {
    private const val API = "https://zyora-server.onrender.com"


    suspend fun uploadDpToGitHub(dataUri: String, userId: String, folder: String = "user-dp"): String? = withContext(Dispatchers.IO) {
        if (!dataUri.startsWith("data:image/") || userId.isBlank()) return@withContext null
        val payload = JSONObject().put("userId", userId).put("folder", folder).put("imageData", dataUri).toString().toByteArray(Charsets.UTF_8)
        var endpoint = URL("$API/api/upload-image")
        var attempt = 0
        while (attempt < 3) {
            val connection = (endpoint.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                instanceFollowRedirects = false
                connectTimeout = 20000
                readTimeout = 60000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setFixedLengthStreamingMode(payload.size)
            }
            try {
                connection.outputStream.use { it.write(payload) }
                val status = connection.responseCode
                if (status in 300..399) {
                    val location = connection.getHeaderField("Location")
                    val redirected = location?.let { URL(endpoint, it) }
                    val sameUploadEndpoint = redirected != null &&
                        redirected.protocol.equals("https", ignoreCase = true) &&
                        redirected.host.equals(endpoint.host, ignoreCase = true) &&
                        redirected.path.trimEnd('/').equals("/api/upload-image", ignoreCase = true)
                    if (sameUploadEndpoint && attempt < 2) {
                        endpoint = redirected!!
                        attempt++
                        continue
                    }
                    throw IllegalStateException("Upload endpoint redirect (HTTP $status). Please retry; server route needs attention.")
                }
                val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (status !in 200..299) {
                    val message = runCatching { JSONObject(response).optString("error") }.getOrNull()
                    throw IllegalStateException(message?.takeIf { it.isNotBlank() } ?: "Image upload failed (HTTP $status).")
                }
                val hostedUrl = JSONObject(response).optString("url")
                return@withContext hostedUrl.takeIf { it.startsWith("$API/api/profile-image?path=") || it.startsWith("https://raw.githubusercontent.com/") }
                    ?: throw IllegalStateException("Image uploaded but the server returned an invalid image URL.")
            } finally {
                connection.disconnect()
            }
        }
        throw IllegalStateException("Image upload redirect could not be completed.")
    }

    fun imageDataUri(context: Context, uri: Uri): String? = runCatching {
        val source = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) } ?: return null
        val maxEdge = 1600
        val scale = minOf(1f, maxEdge.toFloat() / maxOf(source.width, source.height))
        var bitmap = if (scale < 1f) Bitmap.createScaledBitmap(source, (source.width * scale).toInt().coerceAtLeast(1), (source.height * scale).toInt().coerceAtLeast(1), true) else source
        var quality = 84
        var bytes = ByteArray(0)
        do {
            bytes = ByteArrayOutputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
                out.toByteArray()
            }
            if (bytes.size <= 4_800 * 1024 || quality <= 48) break
            quality -= 8
        } while (quality >= 48)
        if (bytes.size > 4_800 * 1024 && bitmap !== source) {
            bitmap.recycle()
            val reducedScale = 1200f / maxOf(source.width, source.height)
            bitmap = Bitmap.createScaledBitmap(source, (source.width * reducedScale).toInt().coerceAtLeast(1), (source.height * reducedScale).toInt().coerceAtLeast(1), true)
            bytes = ByteArrayOutputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 48, out)
                out.toByteArray()
            }
        }
        if (bitmap !== source) bitmap.recycle()
        if (source !== bitmap) source.recycle()
        if (bytes.isEmpty() || bytes.size > 4_800 * 1024) return null
        "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
    }.getOrNull()

    suspend fun save(target: TargetUser?, fields: Map<String, String>) = withContext(Dispatchers.IO) {
        runCatching {
            val uid = listOf(target?.uid, target?.id, target?.accountId, target?.displayAccountNumber).firstOrNull { !it.isNullOrBlank() && it != "—" }.orEmpty().trim()
            if (uid.isBlank() || fields.isEmpty()) return@runCatching false
            val payload = JSONObject().put("uid", uid).put("id", target?.id ?: uid)
            val account = target?.accountId ?: target?.displayAccountNumber
            if (!account.isNullOrBlank() && account != "—") payload.put("accountId", account)
            val removeFields = fields.filter { (_, value) -> value.isBlank() || value == "[]" }.keys.toList()
            fields.forEach { (key, value) -> payload.put(key, value) }
            if (removeFields.isNotEmpty()) payload.put("removeFields", JSONArray(removeFields))
            val connection = (URL("$API/api/users").openConnection() as HttpURLConnection).apply {
                requestMethod = "PUT"
                connectTimeout = 12000
                readTimeout = 12000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
            connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            connection.disconnect()
            status in 200..299
        }.getOrDefault(false)
    }

    suspend fun load(target: TargetUser?): JSONObject? = withContext(Dispatchers.IO) {
        val identifiers = listOf(target?.uid, target?.id, target?.accountId, target?.displayAccountNumber)
            .filterNotNull().map { it.trim() }.filter { it.isNotBlank() && it != "—" }.distinct()
        for (identifier in identifiers) {
            val result = runCatching {
                val encoded = URLEncoder.encode(identifier, "UTF-8")
                val connection = (URL("$API/api/users?search=$encoded&uid=$encoded&accountId=$encoded").openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 12000
                    readTimeout = 12000
                }
                val status = connection.responseCode
                val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                connection.disconnect()
                if (status !in 200..299 || body.isBlank()) null else {
                    val root = JSONObject(body)
                    root.optJSONObject("user") ?: root.optJSONArray("users")?.optJSONObject(0)
                }
            }.getOrNull()
            if (result != null) return@withContext result
        }
        null
    }

    fun updateCache(context: Context, keys: List<String>, fields: Map<String, String>) {
        val db = NativeIndexDatabase(context)
        val uniqueKeys = keys.filter { it.isNotBlank() }.distinct()
        uniqueKeys.forEach { key ->
            val cached = db.loadProfile(key) ?: JSONObject()
            fields.forEach { (field, value) ->
                if (value.isBlank() || value == "[]") cached.remove(field) else cached.put(field, value)
            }
            db.saveProfile(key, cached)
        }
    }

    fun stringList(raw: String?): List<String> = runCatching {
        if (raw.isNullOrBlank()) emptyList() else {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { array.optString(it).takeIf(String::isNotBlank) }.take(4)
        }
    }.getOrDefault(emptyList())
}

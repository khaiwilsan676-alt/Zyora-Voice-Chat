package com.hurry.voicechat

import android.content.Context
import android.util.Base64
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

internal object VoiceMessageStore {
    private const val PREFIX = "VOICEDB::"

    fun localMarker(messageId: String, durationSeconds: Long): String =
        "$PREFIX$messageId::${durationSeconds.coerceAtLeast(1)}"

    fun isVoice(value: String): Boolean =
        value.startsWith(PREFIX) || value.startsWith("VOICE::")

    fun localPath(value: String): String? {
        if (!value.startsWith(PREFIX)) return null
        return null
    }

    fun durationSeconds(value: String): Long {
        if (value.startsWith(PREFIX)) {
            return value.substringAfterLast("::").toLongOrNull()?.coerceAtLeast(1) ?: 1
        }
        return value.removePrefix("VOICE::").toLongOrNull()?.coerceAtLeast(1) ?: 1
    }

    fun displayDuration(value: String): String {
        val seconds = durationSeconds(value)
        return if (seconds >= 60) "%d:%02d".format(seconds / 60, seconds % 60)
        else "0:%02d".format(seconds)
    }

    fun saveIncoming(context: Context, payload: JSONObject): String? {
        val encoded = payload.optString("audioBase64")
        if (encoded.isBlank() || encoded.length > 900_000) return null
        return runCatching {
            val bytes = Base64.decode(encoded, Base64.DEFAULT)
            require(bytes.isNotEmpty() && bytes.size <= 700_000)
            val messageId = payload.optString("id").ifBlank { "voice_${System.currentTimeMillis()}" }
            val peerId = payload.optString("senderAccountId").ifBlank { payload.optString("senderId") }
            val duration = payload.optLong("durationSec", 1).coerceIn(1L, 90L)
            NativeIndexDatabase(context).saveVoiceNote(messageId, peerId, bytes, duration)
            val directory = File(context.filesDir, "voice_notes").apply { mkdirs() }
            val safeId = messageId.replace(Regex("[^A-Za-z0-9_-]"), "_").take(80)
            val file = File(directory, "voice_${safeId}.m4a")
            FileOutputStream(file).use { it.write(bytes) }
            localMarker(messageId, duration)
        }.getOrNull()
    }

    fun persistOutgoing(context: Context, messageId: String, peerId: String, file: File, durationSeconds: Long): String {
        val bytes = file.readBytes()
        NativeIndexDatabase(context).saveVoiceNote(messageId, peerId, bytes, durationSeconds)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun playableFile(context: Context, marker: String): File? {
        if (!marker.startsWith(PREFIX)) return localPath(marker)?.let(::File)?.takeIf { it.exists() }
        val messageId = marker.removePrefix(PREFIX).substringBeforeLast("::")
        if (messageId.isBlank()) return null
        return runCatching {
            val bytes = NativeIndexDatabase(context).loadVoiceNote(messageId) ?: return null
            val directory = File(context.cacheDir, "voice_playback").apply { mkdirs() }
            File(directory, "play_${messageId.hashCode()}.m4a").apply {
                writeBytes(bytes)
            }
        }.getOrNull()
    }
}

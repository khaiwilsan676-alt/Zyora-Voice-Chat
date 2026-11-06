package com.hurry.voicechat

import android.content.Context
import android.net.Uri
import android.util.Base64

internal suspend fun uploadChatImage(
    context: Context,
    uri: Uri,
    uniqueMessageId: String,
    folder: String
): String? {
    val mime = context.contentResolver.getType(uri)?.lowercase()?.substringBefore(';')
        ?.takeIf { it in setOf("image/jpeg", "image/png", "image/webp", "image/gif") }
        ?: "image/jpeg"
    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
    if (bytes.isEmpty() || bytes.size > 5 * 1024 * 1024) return null
    val dataUri = "data:$mime;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
    return ProfilePersistence.uploadImageToMongoDB(dataUri, uniqueMessageId, folder)
}

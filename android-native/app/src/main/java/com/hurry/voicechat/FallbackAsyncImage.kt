package com.hurry.voicechat

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter

/**
 * Shows the bundled Zyora fallback immediately while a remote avatar loads,
 * and keeps it visible if the source is missing, broken, or unavailable offline.
 */
private const val ZYORA_FALLBACK_ASSET = "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/file_00000000d0408211a5b096ee1cc2d12a.png"

@Composable
fun FallbackAsyncImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val source = model?.toString()?.trim().orEmpty()
    val validSource = source.isNotEmpty() &&
        !source.equals("null", ignoreCase = true) &&
        !source.equals("undefined", ignoreCase = true) &&
        !source.equals("/default-avatar.png", ignoreCase = true) &&
        !source.equals("default-avatar.png", ignoreCase = true)
    val sourcePainter = rememberAsyncImagePainter(model = if (validSource) model else ZYORA_FALLBACK_ASSET)
    val fallbackPainter = rememberAsyncImagePainter(model = ZYORA_FALLBACK_ASSET)
    val painter = if (validSource && sourcePainter.state is AsyncImagePainter.State.Success) sourcePainter else fallbackPainter
    Image(painter = painter, contentDescription = contentDescription, modifier = modifier, contentScale = contentScale)
}

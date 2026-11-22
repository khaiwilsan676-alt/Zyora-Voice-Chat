package com.hurry.voicechat

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.rememberAsyncImagePainter
import coil3.compose.AsyncImagePainter

/**
 * Shows the selected room/user DP whenever one is provided.
 * The bundled-style fallback is used only when no valid DP was supplied.
 */
const val ZYORA_DEFAULT_AVATAR_URL = "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/file_00000000d0408211a5b096ee1cc2d12a.png"

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
        !source.equals("default-avatar.png", ignoreCase = true) &&
        !source.substringBefore("?").endsWith("/default-avatar.png", ignoreCase = true)

    // Never cover an assigned DP with the fallback while its image is loading.
    val painter = rememberAsyncImagePainter(
        model = if (validSource) model else ZYORA_DEFAULT_AVATAR_URL
    )
    val painterState by painter.state.collectAsState()
    val fallbackPainter = rememberAsyncImagePainter(model = ZYORA_DEFAULT_AVATAR_URL)
    val displayedPainter = if (!validSource || painterState is AsyncImagePainter.State.Error) fallbackPainter else painter
    Image(
        painter = displayedPainter,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale
    )
}

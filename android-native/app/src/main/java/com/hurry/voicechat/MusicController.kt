package com.hurry.voicechat

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class MusicTrackNative(val id: String, val name: String, val url: String)

@Composable
fun MusicController(
    state: String = "hidden",
    currentTrack: MusicTrackNative? = null,
    isPlaying: Boolean = false,
    volume: Float = 1f,
    currentTime: Float = 0f,
    duration: Float = 0f,
    onTogglePlay: () -> Unit = {},
    onNextTrack: () -> Unit = {},
    onPrevTrack: () -> Unit = {},
    onClose: () -> Unit = {},
    onMinimize: () -> Unit = {},
    onMaximize: () -> Unit = {}
) {
    if (state == "hidden" || currentTrack == null) return

    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Text(currentTrack.name)
        Slider(
            value = if (duration > 0f) (currentTime / duration).coerceIn(0f, 1f) else 0f,
            onValueChange = {}
        )
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TextButton(onClick = onPrevTrack) { Text("◀") }
            Button(onClick = onTogglePlay) { Text(if (isPlaying) "Pause" else "Play") }
            TextButton(onClick = onNextTrack) { Text("▶") }
        }
        Slider(value = volume.coerceIn(0f, 1f), onValueChange = {})
        TextButton(onClick = onClose) { Text("Close") }
    }
}

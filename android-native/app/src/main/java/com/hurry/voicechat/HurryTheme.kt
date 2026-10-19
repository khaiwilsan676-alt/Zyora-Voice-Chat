package com.hurry.voicechat

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val HurryBlue = Color(0xFF008CFF)
val HurryText = Color(0xFF151515)
val HurryMuted = Color(0xFF777777)
val HurryBackground = Color(0xFFF7F8FA)

@Composable
fun HurryTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = HurryBlue,
            onPrimary = Color.White,
            background = Color.White,
            surface = Color.White,
            onSurface = HurryText
        ),
        content = content
    )
}

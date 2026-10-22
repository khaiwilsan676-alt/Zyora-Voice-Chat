package com.hurry.voicechat

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage

@Composable
fun SvipPage(onBack: () -> Unit = {}) {
    // SVIP Shine Animation (0.7 -> 1.0 -> 0.7)
    val infiniteTransition = rememberInfiniteTransition(label = "svip-shine")
    val shineAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shine_alpha"
    )

    // Base URL for your raw github images
    val baseUrl = "https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public"

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        val totalHeight = maxHeight
        val boxConstraints = constraints

        // ============ TOP SECTION (40% HEIGHT) ============
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(totalHeight * 0.4f)
                .align(Alignment.TopCenter)
                .zIndex(30f)
        ) {
            // Background Image
            AsyncImage(
                model = "$baseUrl/file_00000000bd448211892548a9f0469619.png",
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Header (Safe Area)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .zIndex(40f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Back arrow
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier
                        .size(28.dp)
                        .clickable { onBack() }
                )

                // Center: SVIP
                Text(
                    text = "SVIP",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    modifier = Modifier.offset(x = 12.dp) // centering offset balance
                )

                // Right: Images corner to corner
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = "$baseUrl/IMG_20260918_170725.png",
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        contentScale = ContentScale.Fit
                    )
                    AsyncImage(
                        model = "$baseUrl/IMG_20260918_170736.png",
                        contentDescription = "Crown",
                        modifier = Modifier.size(28.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            // Video Player (Loop, Muted, AutoPlay) - Placed under header, above background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(20f)
                    .alpha(0.85f), // Blend slight transparency
                contentAlignment = Alignment.Center
            ) {
                SvipVideoPlayer(videoUrl = "$baseUrl/1000197130-background%20(1).mp4")
            }
        }

        // ============ BOTTOM SECTION (60% HEIGHT) ============
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(totalHeight * 0.6f)
                .align(Alignment.BottomCenter)
                .background(Color(0xFF150C07)) // Dark Brown Color
                .zIndex(10f)
        ) {
            // Shining overlay (Golden Glow)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(shineAlpha)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(255, 215, 120, alpha = 46), // rgba(255, 215, 120, 0.18)
                                Color(255, 215, 120, alpha = 15), // rgba(255, 215, 120, 0.06)
                                Color.Transparent
                            ),
                            center = Offset(boxConstraints.maxWidth / 2f, 0f),
                            radius = boxConstraints.maxWidth.toFloat() * 0.7f
                        )
                    )
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(255, 200, 90, alpha = 20), // rgba(255, 200, 90, 0.08)
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = boxConstraints.maxHeight * 0.4f
                        )
                    )
            )

            // SVIP COMING SOON Text
            Text(
                text = "SVIP COMING SOON",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // ============ MIDDLE IMAGE (Exactly at connection line) ============
        AsyncImage(
            model = "$baseUrl/file_00000000e5d881faa59ce174257e208e.png",
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .zIndex(50f) // Subse upar taaki overlapping perfect ho
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    layout(placeable.width, placeable.height) {
                        // Place exactly at 40% height, translating Y by -50% of image height
                        val yOffset = (this@BoxWithConstraints.boxConstraints.maxHeight * 0.4f).toInt() - (placeable.height / 2)
                        placeable.placeRelative(0, yOffset)
                    }
                },
            contentScale = ContentScale.FillWidth
        )
    }
}

// ExoPlayer Implementation for AutoPlay Muted Video
@Composable
fun SvipVideoPlayer(videoUrl: String) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(videoUrl)
            setMediaItem(mediaItem)
            repeatMode = Player.REPEAT_MODE_ALL
            playWhenReady = true
            volume = 0f // Muted
            prepare()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = {
            PlayerView(context).apply {
                player = exoPlayer
                useController = false
                // Note: Realtime green screen remove on Android requires a Custom Shader
                // For now, setting a transparent shutter background helps blend it slightly
                setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
            }
        },
        modifier = Modifier.fillMaxWidth().aspectRatio(1f) // Adjust scaling to match JS 125vw feel
    )
}

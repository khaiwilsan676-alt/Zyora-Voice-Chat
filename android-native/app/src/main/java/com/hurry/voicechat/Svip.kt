package com.hurry.voicechat

import android.view.TextureView
import android.view.ViewGroup
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
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
                    .zIndex(20f),
                contentAlignment = Alignment.Center
            ) {
                SvipVideoPlayer(videoUrl = "$baseUrl/1000219772-background.mp4")
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
                        val yOffset = (boxConstraints.maxHeight * 0.4f).toInt() - (placeable.height / 2)
                        placeable.placeRelative(0, yOffset)
                    }
                },
            contentScale = ContentScale.FillWidth
        )
    }
}

// ExoPlayer Implementation for AutoPlay Muted Video with Green Screen Removal
@Composable
fun SvipVideoPlayer(videoUrl: String) {
    val context = LocalContext.current
    
    // Video ka real ratio track karne ke liye
    var videoAspectRatio by remember { mutableFloatStateOf(1f) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(videoUrl)
            setMediaItem(mediaItem)
            repeatMode = Player.REPEAT_MODE_ALL
            playWhenReady = true
            volume = 0f // Muted
            
            // Yahan hum video ka asli ratio nikal rahe hain taaki stretch na ho
            addListener(object : Player.Listener {
                override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                    if (videoSize.width > 0 && videoSize.height > 0) {
                        videoAspectRatio = videoSize.width.toFloat() / videoSize.height.toFloat()
                    }
                }
            })
            prepare()
        }
    }

    // 🔥 FIX: Yeh infinite loop Compose ko force karega har 33ms par video ka frame redraw karne ke liye.
    // Iske bina Compose GraphicsLayer video ke frames ko refresh nahi karta tha aur black square dikhata tha.
    val infiniteTransition = rememberInfiniteTransition(label = "video_refresh_loop")
    val invalidateTrigger by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(33, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "redraw_trigger"
    )

    DisposableEffect(Unit) {
        exoPlayer.play() // FIX: Explicitly play the video
        onDispose {
            exoPlayer.release()
        }
    }

    // CHROMA KEY MAGIC MATRIX: Yeh sirf aur sirf Green color ko transparent (alpha 0) karega!
    val greenScreenMatrix = remember {
        ColorMatrix(floatArrayOf(
            1f,   0f,    0f,   0f,  0f,    // Red 
            0f,   1f,    0f,   0f,  0f,    // Green 
            0f,   0f,    1f,   0f,  0f,    // Blue 
            2.5f, -3.5f, 2.5f, 0f,  255f   // Alpha 
        ))
    }

    AndroidView(
        factory = {
            TextureView(context).apply {
                isOpaque = false // Transparent background zaroori hai
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                exoPlayer.setVideoTextureView(this)
            }
        },
        modifier = Modifier
            .offset(y = 40.dp) // <-- EK DAM THORA SA NICHE KIYA BSS
            .fillMaxWidth(0.6f) // <-- Yahan maine 0.6f kar diya taaki video 10% aur choti ho jaye
            .aspectRatio(1f) // Video ekdam SQUARE box me rahegi
            .clip(RectangleShape) // Square se bahar jane wala hissa cut ho jayega
            .layout { measurable, constraints ->
                // === GUARANTEED CROP FIX === 
                val boxWidth = constraints.maxWidth
                val boxHeight = constraints.maxHeight

                val videoWidth: Int
                val videoHeight: Int

                if (videoAspectRatio > 1f) {
                    // Horizontal video
                    videoHeight = boxHeight
                    videoWidth = (boxHeight * videoAspectRatio).toInt()
                } else {
                    // Vertical ya Square video
                    videoWidth = boxWidth
                    videoHeight = (boxWidth / videoAspectRatio).toInt()
                }

                // Asli ratio mein draw karo
                val placeable = measurable.measure(
                    androidx.compose.ui.unit.Constraints.fixed(videoWidth, videoHeight)
                )

                // Center mein fit kar do bina dabaye
                layout(boxWidth, boxHeight) {
                    placeable.placeRelative(
                        x = (boxWidth - videoWidth) / 2,
                        y = (boxHeight - videoHeight) / 2
                    )
                }
            }
            .graphicsLayer {
                // 🔥 FIX: `invalidateTrigger` ko read karne se har frame Compose redraw hoga!
                // Ye line sabse imp hai video black screen issue hatane ke liye.
                val forceRedraw = invalidateTrigger 
                
                // Ye 0.99f alpha offscreen render force karega
                alpha = 0.99f 
                // ColorFilter apply kar diya jo sirf Green color ko nikal dega
                colorFilter = ColorFilter.colorMatrix(greenScreenMatrix)
            }
    )
}

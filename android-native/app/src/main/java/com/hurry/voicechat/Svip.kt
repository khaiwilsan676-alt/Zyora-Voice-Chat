package com.hurry.voicechat

import android.view.TextureView
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
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
            // Background Image (Updated as requested)
            AsyncImage(
                model = "$baseUrl/file_0000000099d88211a80bf7a8cf7aee62.png",
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
                fontSize = 22.sp,
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
                .zIndex(50f) 
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

// Fixed Video Player: No Black Boxes, No Green Screen!
@Composable
fun SvipVideoPlayer(videoUrl: String) {
    var videoAspectRatio by remember { mutableFloatStateOf(1f) }

    // 🔥 MAGIC MATRIX: Yeh Faltu Black Box aur Green Screen dono ko ukhaad fekega!
    val magicMatrix = remember {
        ColorMatrix(floatArrayOf(
            1f,    0f,    0f,   0f,  0f,    // Red 
            0f,    1f,    0f,   0f,  0f,    // Green 
            0f,    0f,    1f,   0f,  0f,    // Blue 
            2.0f, -1.5f,  2.0f, 0f,  0f     // Alpha: Sirf Original colors bachenge, Black/Green Transparent ho jayenge!
        ))
    }

    AndroidView(
        factory = { ctx ->
            android.view.TextureView(ctx).apply {
                isOpaque = false // Transparent background 
                surfaceTextureListener = object : android.view.TextureView.SurfaceTextureListener {
                    var mediaPlayer: android.media.MediaPlayer? = null

                    override fun onSurfaceTextureAvailable(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {
                        mediaPlayer = android.media.MediaPlayer().apply {
                            try {
                                setDataSource(videoUrl)
                                setSurface(android.view.Surface(surface))
                                
                                setOnVideoSizeChangedListener { _, w, h ->
                                    if (w > 0 && h > 0) {
                                        videoAspectRatio = w.toFloat() / h.toFloat()
                                    }
                                }

                                setOnPreparedListener { mp ->
                                    mp.isLooping = true 
                                    mp.setVolume(0f, 0f) 
                                    start()
                                }
                                prepareAsync()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }

                    override fun onSurfaceTextureSizeChanged(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {}
                    
                    override fun onSurfaceTextureDestroyed(surface: android.graphics.SurfaceTexture): Boolean {
                        mediaPlayer?.release()
                        mediaPlayer = null
                        return true
                    }
                    
                    override fun onSurfaceTextureUpdated(surface: android.graphics.SurfaceTexture) {}
                }
            }
        },
        modifier = Modifier
            .offset(y = 40.dp) 
            .fillMaxWidth(0.7f) // Yahan size thora increase kar diya hai (0.6f se 0.7f)
            .aspectRatio(if (videoAspectRatio > 0f) videoAspectRatio else 1f) // Asli aspect ratio, koi black crop nahi!
            .graphicsLayer {
                alpha = 0.99f 
                // Naya filter laga diya jo black aur green gayab karega
                colorFilter = ColorFilter.colorMatrix(magicMatrix)
            }
    )
}

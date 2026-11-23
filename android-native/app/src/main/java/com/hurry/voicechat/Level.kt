package com.hurry.voicechat

import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import coil3.compose.rememberAsyncImagePainter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val LEVEL_ASSET_BASE = "https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/"
private fun levelAsset(file: String) = LEVEL_ASSET_BASE + file

private data class NativeLevelTier(
    val range: String,
    val badge: String,
    val rewards: List<Pair<Int, String>>
)

internal data class NativeLevelMedalItem(
    val levelText: String,
    val imageFile: String,
    val videoFile: String
)

private data class NativeLevelFrameItem(
    val levelText: String,
    val imageFile: String
)

private data class NativeLevelVehicleItem(
    val levelText: String,
    val imageFile: String,
    val videoFile: String,
    val overlayImage: String
)

private val levelTiers = listOf(
    NativeLevelTier("Lv.1 - Lv.10","IMG_20261028_004355.png",listOf(2 to "16,000",4 to "25,000",6 to "35,000",8 to "46,000",10 to "58,000")),
    NativeLevelTier("Lv.11 - Lv.20","IMG_20261028_004435.png",listOf(12 to "70,000",14 to "84,000",16 to "98,000",18 to "114,000",20 to "130,000")),
    NativeLevelTier("Lv.21 - Lv.30","IMG_20261028_004507.png",listOf(22 to "146,000",24 to "164,000",26 to "280,000",28 to "430,000",30 to "676,000")),
    NativeLevelTier("Lv.31 - Lv.40","IMG_20261028_004536.png",listOf(32 to "1,000,000",34 to "1,600,000",36 to "11,200,000",38 to "11,400,000",40 to "16,000,000")),
    NativeLevelTier("Lv.41 - Lv.50","IMG_20261028_004609.png",listOf(42 to "22,000,000",44 to "29,000,000",46 to "37,000,000",48 to "47,000,000",50 to "58,000,000")),
    NativeLevelTier("Lv.51 - Lv.60","IMG_20261028_004845.png",listOf(52 to "70,000,000",54 to "84,000,000",56 to "100,000,000",58 to "120,000,000",60 to "140,000,000")),
    NativeLevelTier("Lv.61 - Lv.70","IMG_20261028_004817.png",listOf(62 to "160,000,000",64 to "180,000,000",66 to "200,000,000",68 to "230,000,000",70 to "260,000,000")),
    NativeLevelTier("Lv.71 - Lv.80","IMG_20261028_004637.png",listOf(72 to "290,000,000",74 to "320,000,000",76 to "360,000,000",78 to "400,000,000",80 to "440,000,000")),
    NativeLevelTier("Lv.81 - Lv.90","IMG_20261028_004700.png",listOf(82 to "480,000,000",84 to "520,000,000",86 to "570,000,000",88 to "620,000,000",90 to "680,000,000")),
    NativeLevelTier("Lv.91 - Lv.100","IMG_20261028_004732.png",listOf(92 to "740,000,000",94 to "800,000,000",96 to "860,000,000",98 to "930,000,000",100 to "1,000,000,000"))
)

internal val medalsList = listOf(
    NativeLevelMedalItem("Lv.10", "IMG_20261123_235528.png", "IMG_20261123_235528.png"),
    NativeLevelMedalItem("Lv.20", "IMG_20261123_235607.png", "IMG_20261123_235607.png"),
    NativeLevelMedalItem("Lv.30", "IMG_20261123_235628.png", "IMG_20261123_235628.png"),
    NativeLevelMedalItem("Lv.40", "IMG_20261123_235643.png", "IMG_20261123_235643.png"),
    NativeLevelMedalItem("Lv.50", "IMG_20261123_235703.png", "IMG_20261123_235703.png"),
    NativeLevelMedalItem("Lv.60", "IMG_20261123_235722.png", "IMG_20261123_235722.png"),
    NativeLevelMedalItem("Lv.70", "IMG_20261123_235749.png", "IMG_20261123_235749.png"),
    NativeLevelMedalItem("Lv.80", "IMG_20261123_235806.png", "IMG_20261123_235806.png"),
    NativeLevelMedalItem("Lv.90", "IMG_20261123_235821.png", "IMG_20261123_235821.png"),
    NativeLevelMedalItem("Lv.100", "IMG_20261123_235841.png", "IMG_20261123_235841.png")
)

private val framesList = listOf(
    NativeLevelFrameItem("Lv.5", "IMG_20261025_135250.png"),
    NativeLevelFrameItem("Lv.10", "IMG_20261025_135305.png"),
    NativeLevelFrameItem("Lv.25", "IMG_20261025_135331.png"),
    NativeLevelFrameItem("Lv.40", "IMG_20261025_135358.png"),
    NativeLevelFrameItem("Lv.55", "IMG_20261025_135714.png"),
    NativeLevelFrameItem("Lv.80", "IMG_20261025_135736.png"),
    NativeLevelFrameItem("Lv.100", "IMG_20261025_140657.png")
)

private val vehiclesList = listOf(
    NativeLevelVehicleItem("Lv.25", "IMG_20261025_124343.png", "alioss_16_2025080802181154988~2.mp4", "IMG_20261025_162952.png"),
    NativeLevelVehicleItem("Lv.45", "IMG_20261025_124403.png", "alioss_17_2025110614082840769~2.mp4", "IMG_20261025_163014.png"),
    NativeLevelVehicleItem("Lv.60", "IMG_20261025_124423.png", "alioss_17_2025111717234047534~2.mp4", "IMG_20261025_163038.png"),
    NativeLevelVehicleItem("Lv.75", "IMG_20261025_124548.png", "alioss_17_2025110916541444937~2.mp4", "IMG_20261025_163058.png"),
    NativeLevelVehicleItem("Lv.90", "IMG_20261025_124608.png", "alioss_17_2025103016391722282~2.mp4", "IMG_20261025_163130.png")
)

private val profileBgImages = listOf(
    "file_00000000edf082119f3c51da31cca16b.png",
    "file_00000000acd48211ba29883fe47a0215.png"
)

private val themeBgImages = listOf(
    "file_00000000d88082118c713ffd53d326e9.png",
    "1792930023475.png"
)

@Composable
private fun StarRow(starCount: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(0.dp)) {
        repeat(starCount) {
            AsyncImage(
                model = levelAsset("file_0000000004e881fa84e61b8616f8eaf8.png"),
                contentDescription = "Star",
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun SectionHeading(title: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f).height(1.5.dp).background(Brush.horizontalGradient(listOf(Color.Transparent, Color.White))))
        Spacer(Modifier.width(4.dp))
        Box(modifier = Modifier.size(3.dp).clip(CircleShape).background(Color(0xFF4DD0E1)))
        Spacer(Modifier.width(3.dp))
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFFFFEB3B)))

        Spacer(Modifier.width(12.dp))

        Text(
            text = title,
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.width(12.dp))

        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFFFFEB3B)))
        Spacer(Modifier.width(3.dp))
        Box(modifier = Modifier.size(3.dp).clip(CircleShape).background(Color(0xFF4DD0E1)))
        Spacer(Modifier.width(4.dp))
        Box(modifier = Modifier.weight(1f).height(1.5.dp).background(Brush.horizontalGradient(listOf(Color.White, Color.Transparent))))
    }
}

private data class LevelSparkleSpec(
    val xFrac: Float,
    val yFrac: Float,
    val maxRadius: Float,
    val phaseOffset: Float
)

private data class LevelShiningBubbleSpec(
    val xFrac: Float,
    val radiusPx: Float,
    val phaseOffset: Float,
    val driftAmp: Float
)

@Composable
private fun LevelMedalPreviewDialog(
    medal: NativeLevelMedalItem,
    onDismiss: () -> Unit
) {
    val popupScale = remember { Animatable(0.45f) }
    val popupAlpha = remember { Animatable(0f) }

    LaunchedEffect(medal.levelText) {
        popupScale.snapTo(0.45f)
        popupAlpha.snapTo(0f)
        launch {
            popupScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.55f,
                    stiffness = 260f
                )
            )
        }
        launch {
            popupAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 220)
            )
        }
    }

    val floatTransition = rememberInfiniteTransition(label = "levelMedalFloat")
    val floatOffsetY by floatTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "levelMedalFloatY"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        // Original background bina kisi black screen ya dim ke show hoga
        val dialogWindowProvider = androidx.compose.ui.platform.LocalView.current.parent as? androidx.compose.ui.window.DialogWindowProvider
        dialogWindowProvider?.window?.setDimAmount(0f)
        dialogWindowProvider?.window?.setBackgroundDrawableResource(android.R.color.transparent)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier.size(310.dp),
                contentAlignment = Alignment.Center
            ) {
                LevelGoldenSunburstEffect(modifier = Modifier.fillMaxSize())

                Box(
                    modifier = Modifier
                        .size(215.dp)
                        .graphicsLayer {
                            scaleX = popupScale.value
                            scaleY = popupScale.value
                            alpha = popupAlpha.value
                            translationY = floatOffsetY
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = levelAsset(medal.imageFile),
                        contentDescription = "${medal.levelText} Medal",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                LevelGoldenSparkleAndBubbleOverlay(modifier = Modifier.size(270.dp))
            }
        }
    }
}

@Composable
private fun LevelGoldenSunburstEffect(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "levelSunburst")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 28000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "levelSunburstRotation"
    )

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension * 0.80f
        val rayCount = 18

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFDF7A).copy(alpha = 0.45f),
                    Color(0xFFD9A12B).copy(alpha = 0.20f),
                    Color.Transparent
                ),
                center = center,
                radius = radius * 0.85f
            ),
            radius = radius * 0.85f,
            center = center
        )

        rotate(degrees = rotation, pivot = center) {
            val stepAngle = 360f / rayCount
            for (i in 0 until rayCount) {
                val startDeg = i * stepAngle
                val halfSpread = stepAngle * 0.28f
                val rad1 = Math.toRadians((startDeg - halfSpread).toDouble())
                val rad2 = Math.toRadians((startDeg + halfSpread).toDouble())

                val path = Path().apply {
                    moveTo(center.x, center.y)
                    lineTo(
                        center.x + (radius * cos(rad1)).toFloat(),
                        center.y + (radius * sin(rad1)).toFloat()
                    )
                    lineTo(
                        center.x + (radius * cos(rad2)).toFloat(),
                        center.y + (radius * sin(rad2)).toFloat()
                    )
                    close()
                }
                drawPath(
                    path = path,
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFE694).copy(alpha = 0.58f),
                            Color(0xFFE0A82E).copy(alpha = 0.28f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = radius
                    )
                )
            }
        }
    }
}

@Composable
private fun LevelGoldenSparkleAndBubbleOverlay(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "levelSparkleAndBubbles")

    val sparklePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "levelSparklePhase"
    )

    val bubblePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "levelBubblePhase"
    )

    val shinePulse by infiniteTransition.animateFloat(
        initialValue = 0.14f,
        targetValue = 0.34f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "levelShinePulse"
    )

    val sparkles = remember {
        listOf(
            LevelSparkleSpec(0.24f, 0.25f, 15f, 0.00f),
            LevelSparkleSpec(0.76f, 0.22f, 17f, 0.18f),
            LevelSparkleSpec(0.50f, 0.16f, 13f, 0.35f),
            LevelSparkleSpec(0.18f, 0.56f, 14f, 0.52f),
            LevelSparkleSpec(0.83f, 0.52f, 16f, 0.68f),
            LevelSparkleSpec(0.34f, 0.74f, 15f, 0.82f),
            LevelSparkleSpec(0.68f, 0.76f, 14f, 0.26f),
            LevelSparkleSpec(0.42f, 0.42f, 12f, 0.61f),
            LevelSparkleSpec(0.61f, 0.38f, 13f, 0.91f),
            LevelSparkleSpec(0.49f, 0.64f, 12f, 0.44f),
            LevelSparkleSpec(0.29f, 0.40f, 11f, 0.74f),
            LevelSparkleSpec(0.72f, 0.63f, 12f, 0.11f)
        )
    }

    val bubbles = remember {
        listOf(
            LevelShiningBubbleSpec(0.22f, 9f, 0.05f, 10f),
            LevelShiningBubbleSpec(0.35f, 13f, 0.22f, -12f),
            LevelShiningBubbleSpec(0.48f, 8f, 0.40f, 9f),
            LevelShiningBubbleSpec(0.62f, 14f, 0.58f, -11f),
            LevelShiningBubbleSpec(0.77f, 10f, 0.75f, 10f),
            LevelShiningBubbleSpec(0.29f, 7f, 0.88f, -8f),
            LevelShiningBubbleSpec(0.54f, 11f, 0.15f, 12f),
            LevelShiningBubbleSpec(0.70f, 8f, 0.33f, -9f),
            LevelShiningBubbleSpec(0.40f, 6f, 0.50f, 8f),
            LevelShiningBubbleSpec(0.82f, 9f, 0.67f, -10f),
            LevelShiningBubbleSpec(0.17f, 10f, 0.81f, 9f),
            LevelShiningBubbleSpec(0.58f, 7f, 0.94f, -8f)
        )
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val center = Offset(w / 2f, h / 2f)

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFF4C2).copy(alpha = shinePulse),
                    Color(0xFFFFD452).copy(alpha = shinePulse * 0.45f),
                    Color.Transparent
                ),
                center = center,
                radius = size.minDimension * 0.48f
            ),
            radius = size.minDimension * 0.48f,
            center = center
        )

        bubbles.forEach { b ->
            val progress = (bubblePhase + b.phaseOffset) % 1f
            val y = h * (0.90f - progress * 0.78f)
            val x = (w * b.xFrac) + (sin(progress * 2.0 * PI) * b.driftAmp).toFloat()
            val alpha = when {
                progress < 0.18f -> (progress / 0.18f)
                progress > 0.78f -> ((1f - progress) / 0.22f)
                else -> 1f
            }.coerceIn(0f, 1f)

            val bubbleCenter = Offset(x, y)
            val r = b.radiusPx * (0.85f + 0.25f * sin(progress * PI).toFloat())

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFE98A).copy(alpha = 0.50f * alpha),
                        Color(0xFFE5B230).copy(alpha = 0.22f * alpha),
                        Color.Transparent
                    ),
                    center = bubbleCenter,
                    radius = r * 2.0f
                ),
                radius = r * 2.0f,
                center = bubbleCenter
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFFBE0).copy(alpha = 0.35f * alpha),
                        Color(0xFFFFD863).copy(alpha = 0.22f * alpha),
                        Color(0xFFE5A924).copy(alpha = 0.42f * alpha)
                    ),
                    center = bubbleCenter,
                    radius = r
                ),
                radius = r,
                center = bubbleCenter
            )

            drawCircle(
                color = Color(0xFFFFF0A8).copy(alpha = 0.78f * alpha),
                radius = r,
                center = bubbleCenter,
                style = Stroke(width = 1.2f)
            )

            drawCircle(
                color = Color.White.copy(alpha = 0.90f * alpha),
                radius = r * 0.28f,
                center = Offset(x - r * 0.32f, y - r * 0.32f)
            )
        }

        sparkles.forEach { s ->
            val wave = ((sin((sparklePhase + s.phaseOffset) * 2.0 * PI) + 1.0) / 2.0).toFloat()
            val intensity = wave * wave
            if (intensity > 0.06f) {
                val cx = w * s.xFrac
                val cy = h * s.yFrac
                val outerR = s.maxRadius * (0.35f + 0.65f * intensity)
                val innerR = outerR * 0.22f

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFF8D2).copy(alpha = 0.85f * intensity),
                            Color(0xFFFFCC3D).copy(alpha = 0.40f * intensity),
                            Color.Transparent
                        ),
                        center = Offset(cx, cy),
                        radius = outerR * 1.45f
                    ),
                    radius = outerR * 1.45f,
                    center = Offset(cx, cy)
                )

                val starPath = Path().apply {
                    moveTo(cx, cy - outerR)
                    lineTo(cx + innerR, cy - innerR)
                    lineTo(cx + outerR, cy)
                    lineTo(cx + innerR, cy + innerR)
                    lineTo(cx, cy + outerR)
                    lineTo(cx - innerR, cy + innerR)
                    lineTo(cx - outerR, cy)
                    lineTo(cx - innerR, cy - innerR)
                    close()
                }
                drawPath(
                    path = starPath,
                    color = Color(0xFFFFF7CC).copy(alpha = 0.95f * intensity)
                )

                drawCircle(
                    color = Color.White.copy(alpha = 0.95f * intensity),
                    radius = innerR * 0.95f,
                    center = Offset(cx, cy)
                )
            }
        }
    }
}

@Composable
private fun FullScreenVideoDialog(videoUrl: String, isMedal: Boolean, overlayImageUrl: String?, onDismiss: () -> Unit) {
    var videoAspectRatio by remember { mutableStateOf(9f / 16f) }
    val configuration = LocalConfiguration.current

    val infiniteTransition = rememberInfiniteTransition()
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val dialogWindowProvider = androidx.compose.ui.platform.LocalView.current.parent as? androidx.compose.ui.window.DialogWindowProvider
        dialogWindowProvider?.window?.setDimAmount(0f)
        dialogWindowProvider?.window?.setBackgroundDrawableResource(android.R.color.transparent)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isMedal) Color.Black.copy(alpha = 0.6f) else Color.Transparent)
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { ctx ->
                    android.view.TextureView(ctx).apply {
                        isOpaque = false
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
                                            mp.isLooping = false
                                            start()
                                        }
                                        setOnCompletionListener {
                                            onDismiss()
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
                    .fillMaxWidth()
                    .aspectRatio(videoAspectRatio)
                    .graphicsLayer {
                        alpha = 0.99f
                        if (isMedal) {
                            val removeBlackMatrix = ColorMatrix(floatArrayOf(
                                1f, 0f, 0f, 0f, 0f,
                                0f, 1f, 0f, 0f, 0f,
                                0f, 0f, 1f, 0f, 0f,
                                1.2f, 1.2f, 1.2f, 0f, 0f
                            ))
                            colorFilter = ColorFilter.colorMatrix(removeBlackMatrix)
                        }
                    }
                    .drawWithContent {
                        drawContent()
                        if (!isMedal) {
                            drawRect(
                                brush = Brush.verticalGradient(
                                    0.0f to Color.Transparent,
                                    0.1f to Color.Black,
                                    0.9f to Color.Black,
                                    1.0f to Color.Transparent
                                ),
                                blendMode = BlendMode.DstIn
                            )
                        }
                    }
            )

            if (overlayImageUrl != null) {
                AsyncImage(
                    model = overlayImageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = (configuration.screenHeightDp * 0.10f).dp)
                        .offset(y = floatOffset.dp)
                        .fillMaxWidth(0.6f)
                        .wrapContentHeight(),
                    contentScale = ContentScale.FillWidth
                )
            }
        }
    }
}

@Composable
private fun LevelUpgradeDialog(onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        val cardShape = RoundedCornerShape(14.dp)
        val buttonShape = RoundedCornerShape(12.dp)
        val goldenBorderBrush = Brush.verticalGradient(
            listOf(Color(0xFFF3D578), Color(0xFFC59B38), Color(0xFFE5C158))
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.86f)
                    .clip(cardShape)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF0B4182),
                                Color(0xFF072957),
                                Color(0xFF041A3B)
                            )
                        )
                    )
                    .border(1.5.dp, goldenBorderBrush, cardShape)
                    .clickable(enabled = false) {}
                    .padding(horizontal = 22.dp, vertical = 26.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "How to upgrade",
                        style = TextStyle(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color.White, Color(0xFFFCE28B), Color(0xFFE5B83B))
                            ),
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 22.dp)
                    )

                    val methods = listOf(
                        Pair("Send Gifts", "1 coins = 1 EXP"),
                        Pair("Join rooms", "2000 EXP/Day"),
                        Pair("Share Rooms", "2000 EXP/Day"),
                        Pair("Take the mic and unmute in your own room", "1 min = 500 exp, 10000 exp/day"),
                        Pair("Take the mic and unmute in others' rooms", "1 min = 500 exp, 20000 exp/day")
                    )

                    methods.forEachIndexed { index, item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "${index + 1}.",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 20.sp,
                                modifier = Modifier.width(24.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.first,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    lineHeight = 20.sp
                                )
                                Text(
                                    text = item.second,
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                        if (index != methods.lastIndex) {
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(26.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(buttonShape)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFF1055A3), Color(0xFF0A3973))
                                )
                            )
                            .border(1.5.dp, Color(0xFFE5C158), buttonShape)
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Got it",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun Level(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    // Login and canonical own-profile fields are stored in hurry_auth.
    // Reading hurry_user returned blank identity and caused the Guest fallback.
    val authPrefs = remember { context.getSharedPreferences("hurry_auth", Context.MODE_PRIVATE) }
    val storedName = authPrefs.getString("name", "")?.trim().orEmpty()
    val userName = storedName.takeIf { it.isNotBlank() && it.lowercase() !in listOf("guest", "user", "null", "undefined") }
    val userUid = authPrefs.getString("uid", "")?.trim().orEmpty()
    val userAccountId = authPrefs.getString("accountId", "")?.trim().orEmpty()
    val userPhoto = sequenceOf("image", "photo", "avatar")
        .map { authPrefs.getString(it, "")?.trim().orEmpty() }
        .firstOrNull { it.isNotBlank() && it != "null" && it != "undefined" }.orEmpty()
    val profileLookupId = userAccountId.ifBlank {
        authPrefs.getString("id", "")?.trim().orEmpty().ifBlank { userUid }
    }
    var profileName by remember { mutableStateOf(userName.orEmpty()) }
    var profilePhoto by remember { mutableStateOf(userPhoto) }

    LaunchedEffect(profileLookupId) {
        if (profileLookupId.isNotBlank()) {
            val profile = withContext(Dispatchers.IO) {
                runCatching {
                    ProfilePersistence.load(
                        TargetUser(
                            id = authPrefs.getString("id", "")?.trim().orEmpty().ifBlank { userUid },
                            uid = userUid.ifBlank { profileLookupId },
                            name = userName,
                            accountId = userAccountId.ifBlank { profileLookupId },
                            displayAccountNumber = userAccountId.ifBlank { profileLookupId },
                            photo = userPhoto,
                            image = userPhoto
                        )
                    )
                }.getOrNull()
            }
            if (profile != null) {
                val canonicalName = sequenceOf("name", "displayName", "userName", "nickname")
                    .map { profile.optString(it, "").trim() }
                    .firstOrNull { it.isNotBlank() && it != "null" && it.lowercase() !in listOf("guest", "user", "undefined") }
                val canonicalPhoto = sequenceOf("image", "avatar", "profileImageUrl", "dp", "photoURL")
                    .map { profile.optString(it, "").trim() }
                    .firstOrNull { it.isNotBlank() && it != "null" && it != "undefined" }
                if (canonicalName != null) profileName = canonicalName
                if (canonicalPhoto != null) profilePhoto = canonicalPhoto
            }
        }
    }

    val profilePainter = rememberAsyncImagePainter(model = levelAsset("file_0000000074a4821189d05fde51ca98f9.png"))
    val displayName = profileName.takeIf { it.isNotBlank() && it.lowercase() !in listOf("guest", "user", "null", "undefined") }
        ?: userName ?: userUid.take(8).ifBlank { "User" }
    val displayPhoto = profilePhoto.ifBlank { userPhoto }

    var showUpgrade by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    var showAllLevels by remember { mutableStateOf(false) }
    var showAllRewards by remember { mutableStateOf(false) }
    var showAllFrames by remember { mutableStateOf(false) }
    var showAllMedals by remember { mutableStateOf(false) }
    var showAllVehicles by remember { mutableStateOf(false) }

    var selectedVideoData by remember { mutableStateOf<Triple<String, Boolean, String?>?>(null) }
    var selectedMedalPreview by remember { mutableStateOf<NativeLevelMedalItem?>(null) }

    val pagerState = rememberPagerState { profileBgImages.size }
    val themePagerState = rememberPagerState { themeBgImages.size }

    val topBannerSpace = 4.dp
    val newSectionTopPadding = 15.dp

    LaunchedEffect(pagerState) {
        while (true) {
            delay(5000)
            if (profileBgImages.isNotEmpty()) {
                val nextPage = (pagerState.currentPage + 1) % profileBgImages.size
                pagerState.animateScrollToPage(nextPage)
            }
        }
    }

    LaunchedEffect(themePagerState) {
        while (true) {
            delay(5000)
            if (themeBgImages.isNotEmpty()) {
                val nextPage = (themePagerState.currentPage + 1) % themeBgImages.size
                themePagerState.animateScrollToPage(nextPage)
            }
        }
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF04060A))) {
        AsyncImage(model = levelAsset("file_00000000e02481f4bb2153e2714aca47.png"), contentDescription = null, Modifier.fillMaxWidth().height(280.dp).align(Alignment.TopCenter), contentScale = ContentScale.Crop)
        Box(
            Modifier.align(Alignment.TopStart).offset(x = (-40).dp, y = (-40).dp).size(260.dp)
                .blur(90.dp).clip(CircleShape).background(Color(0x4D1D4ED8))
        )
        Box(Modifier.fillMaxWidth().height(280.dp).background(Brush.verticalGradient(listOf(Color.Transparent, Color(0x6604060A), Color(0xFF04060A)))))

        Column(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxWidth().statusBarsPadding()) {
                Box(Modifier.fillMaxWidth().height(56.dp).padding(top = 8.dp), contentAlignment = Alignment.Center) {
                    IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart).padding(start = 2.dp)) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White, modifier = Modifier.size(26.dp))
                    }
                    Text("Level", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    IconButton(onClick = { showUpgrade = true }, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 2.dp)) {
                        Icon(Icons.Default.HelpOutline, "How to upgrade", tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                }

                Spacer(Modifier.height(topBannerSpace))

                Box(Modifier.fillMaxWidth()) {
                    Image(
                        painter = profilePainter,
                        contentDescription = "Top Card Frame",
                        modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                        contentScale = ContentScale.FillWidth
                    )
                    Row(
                        modifier = Modifier.matchParentSize().padding(horizontal = 32.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(56.dp).clip(CircleShape).background(Color(0xFF626A75)), contentAlignment = Alignment.Center) {
                            if (displayPhoto.isNotBlank()) {
                                AsyncImage(
                                    model = displayPhoto,
                                    contentDescription = "User",
                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(displayName.take(1).uppercase(), color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = displayName,
                                    color = Color.White,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.offset(y = (-2).dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier.height(28.dp).width(58.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    AsyncImage(
                                        model = levelAsset(levelTiers[0].badge),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Text(
                                        text = "Lv.1",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Normal,
                                        maxLines = 1,
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd)
                                            .padding(end = 6.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(3.dp))
                            Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = .25f))) {
                                Box(Modifier.fillMaxWidth(.38f).fillMaxHeight().clip(RoundedCornerShape(20.dp)).background(Brush.horizontalGradient(listOf(Color(0xFFFFE072), Color(0xFFF4B63F))))) {
                                    Box(Modifier.align(Alignment.CenterEnd).offset(x = 6.dp).size(12.dp).border(2.dp, Color(0xFFF4B63F), CircleShape).clip(CircleShape).background(Color.White))
                                }
                            }
                            Text(
                                text = "4.5k/20.2k remaining to reach Level 5 >",
                                color = Color.White.copy(alpha = .9f),
                                fontSize = 10.5.sp,
                                modifier = Modifier.padding(top = 4.dp),
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = newSectionTopPadding, bottom = 40.dp)
            ) {
                // ==========================================
                // 1. LEVEL SECTION
                // ==========================================
                item {
                    SectionHeading("Level")
                    Spacer(Modifier.height(8.dp))
                }

                val visibleLevels = if (showAllLevels) levelTiers else levelTiers.take(9)
                val chunkedLevels = visibleLevels.chunked(3)

                items(chunkedLevels) { rowTiers ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        for (tier in rowTiers) {
                            Box(
                                Modifier
                                    .weight(1f)
                                    .aspectRatio(0.85f)
                                    .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                AsyncImage(
                                    model = levelAsset(tier.badge),
                                    contentDescription = null,
                                    modifier = Modifier.height(38.dp).align(Alignment.Center)
                                )

                                val formattedRange = tier.range.replace(" - Lv.", " - ")
                                Text(
                                    text = formattedRange,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.align(Alignment.BottomCenter)
                                )
                            }
                        }
                        repeat(3 - rowTiers.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }

                if (levelTiers.size > 9) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 12.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (showAllLevels) "View Less <" else "View More >",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { showAllLevels = !showAllLevels }
                                    .border(1.dp, Color(0xFF3B82F6), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                item { Spacer(Modifier.height(24.dp)) }

                // ==============================================
                // 2. REWARDS SECTION
                // ==============================================
                item {
                    SectionHeading("Rewards")
                    Spacer(Modifier.height(8.dp))
                }

                val allRewards = levelTiers.flatMap { it.rewards }
                val visibleRewards = if (showAllRewards) allRewards else allRewards.take(9)
                val chunkedRewards = visibleRewards.chunked(3)

                items(chunkedRewards) { rowItems ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        for (reward in rowItems) {
                            Box(
                                Modifier
                                    .weight(1f)
                                    .aspectRatio(0.85f)
                                    .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                    .clip(RoundedCornerShape(8.dp))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .background(
                                            color = Color.Black.copy(alpha = 0.4f),
                                            shape = RoundedCornerShape(bottomEnd = 8.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "Lv.${reward.first}",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                AsyncImage(
                                    model = levelAsset("file_00000000b2d481fd8cd233482dbeb9ef.png"),
                                    contentDescription = null,
                                    modifier = Modifier.size(56.dp).align(Alignment.Center)
                                )

                                Text(
                                    text = reward.second,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp)
                                )
                            }
                        }
                        repeat(3 - rowItems.size) { Spacer(Modifier.weight(1f)) }
                    }
                    Spacer(Modifier.height(10.dp))
                }

                if (allRewards.size > 9) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 12.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (showAllRewards) "View Less <" else "View More >",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { showAllRewards = !showAllRewards }
                                    .border(1.dp, Color(0xFF3B82F6), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                item { Spacer(Modifier.height(24.dp)) }

                // ==============================================
                // NEW FRAME SECTION
                // ==============================================
                item {
                    SectionHeading("Frame")
                    Spacer(Modifier.height(8.dp))
                }

                val visibleFrames = if (showAllFrames) framesList else framesList.take(4)
                val chunkedFrames = visibleFrames.chunked(2)

                items(chunkedFrames) { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        for (frame in rowItems) {
                            Box(
                                Modifier
                                    .weight(1f)
                                    .aspectRatio(0.85f)
                                    .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                    .clip(RoundedCornerShape(8.dp))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .background(
                                            color = Color.Black.copy(alpha = 0.4f),
                                            shape = RoundedCornerShape(bottomEnd = 8.dp)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = frame.levelText,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                AsyncImage(
                                    model = levelAsset(frame.imageFile),
                                    contentDescription = null,
                                    modifier = Modifier.size(110.dp).align(Alignment.Center).offset(y = (-15).dp)
                                )

                                val starCount = (frame.levelText.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0) % 3 + 3
                                Column(
                                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    StarRow(starCount = starCount)
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = "${frame.levelText} Frame",
                                        color = Color.LightGray,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        repeat(2 - rowItems.size) { Spacer(Modifier.weight(1f)) }
                    }
                    Spacer(Modifier.height(10.dp))
                }

                if (framesList.size > 4) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 12.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (showAllFrames) "View Less <" else "View More >",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { showAllFrames = !showAllFrames }
                                    .border(1.dp, Color(0xFF3B82F6), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                item { Spacer(Modifier.height(24.dp)) }

                // ==============================================
                // 3. MEDAL SECTION (Lv.10 to Lv.100 Rich Medal Images + Original BG Popup/Sparkle Animation)
                // ==============================================
                item {
                    SectionHeading("Medal")
                    Spacer(Modifier.height(8.dp))
                }

                val visibleMedals = if (showAllMedals) medalsList else medalsList.take(4)
                val chunkedMedals = visibleMedals.chunked(2)

                items(chunkedMedals) { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        for (medal in rowItems) {
                            Box(
                                Modifier
                                    .weight(1f)
                                    .aspectRatio(0.85f)
                                    .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        selectedMedalPreview = medal
                                    }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .background(
                                            color = Color.Black.copy(alpha = 0.4f),
                                            shape = RoundedCornerShape(bottomEnd = 8.dp)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = medal.levelText,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                AsyncImage(
                                    model = levelAsset(medal.imageFile),
                                    contentDescription = null,
                                    modifier = Modifier.size(90.dp).align(Alignment.Center)
                                )

                                val starCount = (medal.levelText.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0) % 3 + 3
                                Column(
                                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    StarRow(starCount = starCount)
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = "${medal.levelText} Medal",
                                        color = Color.LightGray,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        repeat(2 - rowItems.size) { Spacer(Modifier.weight(1f)) }
                    }
                    Spacer(Modifier.height(10.dp))
                }

                if (medalsList.size > 4) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 12.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (showAllMedals) "View Less <" else "View More >",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { showAllMedals = !showAllMedals }
                                    .border(1.dp, Color(0xFF3B82F6), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                item { Spacer(Modifier.height(24.dp)) }

                // ==============================================
                // VEHICLE/ENTRY SECTION
                // ==============================================
                item {
                    SectionHeading("Entry")
                    Spacer(Modifier.height(8.dp))
                }

                val visibleVehicles = if (showAllVehicles) vehiclesList else vehiclesList.take(4)
                val chunkedVehicles = visibleVehicles.chunked(2)

                items(chunkedVehicles) { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        for (vehicle in rowItems) {
                            Box(
                                Modifier
                                    .weight(1f)
                                    .aspectRatio(0.85f)
                                    .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        selectedVideoData = Triple(levelAsset(vehicle.videoFile), false, levelAsset(vehicle.overlayImage))
                                    }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .background(
                                            color = Color.Black.copy(alpha = 0.4f),
                                            shape = RoundedCornerShape(bottomEnd = 8.dp)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = vehicle.levelText,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                AsyncImage(
                                    model = levelAsset(vehicle.imageFile),
                                    contentDescription = null,
                                    modifier = Modifier.size(110.dp).align(Alignment.Center).offset(y = (-15).dp)
                                )

                                val starCount = (vehicle.levelText.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0) % 3 + 3
                                Column(
                                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    StarRow(starCount = starCount)
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = "${vehicle.levelText} Entry",
                                        color = Color.LightGray,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        repeat(2 - rowItems.size) { Spacer(Modifier.weight(1f)) }
                    }
                    Spacer(Modifier.height(10.dp))
                }

                if (vehiclesList.size > 4) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 12.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (showAllVehicles) "View Less <" else "View More >",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { showAllVehicles = !showAllVehicles }
                                    .border(1.dp, Color(0xFF3B82F6), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                item { Spacer(Modifier.height(24.dp)) }

                // ==============================================
                // 5. CUSTOMIZE PROFILE BACKGROUND SECTION
                // ==============================================
                item {
                    SectionHeading("Customize Profile Background")

                    Text(
                        text = "Customized Your Profile Background Upload Image",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 0.dp, bottom = 8.dp)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            AsyncImage(
                                model = levelAsset(profileBgImages[page]),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .background(
                                    color = Color.Black.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(bottomEnd = 8.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Lv.25",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            repeat(profileBgImages.size) { index ->
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (pagerState.currentPage == index) Color.White else Color.Black
                                        )
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }

                // ==============================================
                // NEW: CUSTOMIZED THEME SECTION
                // ==============================================
                item {
                    SectionHeading("Customized Theme")

                    Text(
                        text = "Customized your Room Theme upload image",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 0.dp, bottom = 8.dp)
                    )

                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.48f)
                                .height(280.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.1f))
                        ) {
                            HorizontalPager(
                                state = themePagerState,
                                modifier = Modifier.fillMaxSize()
                            ) { page ->
                                AsyncImage(
                                    model = levelAsset(themeBgImages[page]),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .background(
                                        color = Color.Black.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(bottomEnd = 8.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Lv.30",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                repeat(themeBgImages.size) { index ->
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (themePagerState.currentPage == index) Color.White else Color.Black
                                            )
                                    )
                                }
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        StarRow(starCount = 3)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Lv.30 Customized Theme",
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }

    if (showUpgrade) LevelUpgradeDialog { showUpgrade = false }

    selectedMedalPreview?.let { medal ->
        LevelMedalPreviewDialog(medal = medal) {
            selectedMedalPreview = null
        }
    }

    selectedVideoData?.let { (videoUrl, isMedal, overlayImg) ->
        FullScreenVideoDialog(videoUrl = videoUrl, isMedal = isMedal, overlayImageUrl = overlayImg) {
            selectedVideoData = null
        }
    }
}

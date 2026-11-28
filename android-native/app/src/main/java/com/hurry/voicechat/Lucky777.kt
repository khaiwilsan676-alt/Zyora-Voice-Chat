package com.hurry.voicechat

import android.content.Context
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.Locale
import kotlin.math.*

private const val LUCKY777_BASE =
    "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/"
private const val LUCKY777_BACKGROUND =
    "${LUCKY777_BASE}file_000000001d488211b1d4b391060d3c39.png"
private const val LUCKY777_LOGO =
    "${LUCKY777_BASE}IMG_20261127_231318.png"
private const val LUCKY777_COIN =
    "${LUCKY777_BASE}file_00000000e56882119c217d508b6733dc.png"
private const val LUCKY777_WHEEL_FRAME =
    "${LUCKY777_BASE}IMG_20261127_233303.png"
private const val LUCKY777_PEACH =
    "${LUCKY777_BASE}IMG_20261127_233200.png"
private const val LUCKY777_WATERMELON =
    "${LUCKY777_BASE}IMG_20261127_233115.png"
private const val LUCKY777_SEVEN =
    "${LUCKY777_BASE}IMG_20261127_233140.png"
private const val LUCKY777_CARDS_BG =
    "${LUCKY777_BASE}file_000000005e908211a7af81fdfe6c0b7a.png"
private const val LUCKY777_X2 =
    "${LUCKY777_BASE}IMG_20261127_233217.png"
private const val LUCKY777_X8 =
    "${LUCKY777_BASE}IMG_20261127_233249.png"
private const val LUCKY777_BOTTOM_LEFT_DECOR =
    "${LUCKY777_BASE}file_00000000993c8208ae462545f9ccb0ba.png"
private const val LUCKY777_WINNER_HEADING =
    "${LUCKY777_BASE}IMG_20260913_000423.png"
private const val LUCKY777_FRAME_2ND =
    "${LUCKY777_BASE}IMG_20260912_235156.png"
private const val LUCKY777_FRAME_1ST =
    "${LUCKY777_BASE}IMG_20260912_235215.png"
private const val LUCKY777_FRAME_3RD =
    "${LUCKY777_BASE}IMG_20260912_235230.png"

private val l7Gold = Color(0xFFFFD33D)

private enum class L7Phase { BETTING, SPINNING, RESULT }

private data class L7Item(
    val key: String,
    val image: String,
    val multiplier: Int,
    val multImage: String
)

private data class L7Chip(val label: String, val value: Long, val image: String)

private data class L7Round(
    val round: Int,
    val winner: String,
    val time: Long,
    val myBet: Long = 0L,
    val winAmount: Long = 0L,
    val betsMap: Map<String, Long> = emptyMap(),
    val balanceBefore: Long = 0L,
    val balanceAfter: Long = 0L
)

private val l7BetItems = listOf(
    L7Item("Peach", LUCKY777_PEACH, 2, LUCKY777_X2),
    L7Item("777", LUCKY777_SEVEN, 8, LUCKY777_X8),
    L7Item("Watermelon", LUCKY777_WATERMELON, 2, LUCKY777_X2)
)

// Exact 9-slice wheel order matching reference image:
// Slice 0 = 777 (Pink), then alternating Watermelon (Royal Blue) & Peach (Sky Blue)
private val l7WheelSlices = listOf(
    "777",
    "Watermelon",
    "Peach",
    "Watermelon",
    "Peach",
    "Watermelon",
    "Peach",
    "Watermelon",
    "Peach"
)

private val l7Chips = listOf(
    L7Chip("1K", 1000L, "${LUCKY777_BASE}IMG_20261121_102630.png"),
    L7Chip("10K", 10000L, "${LUCKY777_BASE}IMG_20261121_102707.png"),
    L7Chip("50K", 50000L, "${LUCKY777_BASE}IMG_20261121_102752.png"),
    L7Chip("500K", 500000L, "${LUCKY777_BASE}IMG_20261121_102814.png"),
    L7Chip("5M", 5000000L, "${LUCKY777_BASE}IMG_20261121_102835.png")
)

private fun l7ImageFor(key: String): String = when (key) {
    "777" -> LUCKY777_SEVEN
    "Watermelon" -> LUCKY777_WATERMELON
    else -> LUCKY777_PEACH
}

private fun l7MultiplierFor(key: String): Int = if (key == "777") 8 else 2

private fun l7Reset(now: Long = System.currentTimeMillis()): Long {
    val c = Calendar.getInstance().apply { timeInMillis = now }
    if (c.get(Calendar.HOUR_OF_DAY) < 5) c.add(Calendar.DAY_OF_YEAR, -1)
    c.set(Calendar.HOUR_OF_DAY, 5)
    c.set(Calendar.MINUTE, 0)
    c.set(Calendar.SECOND, 0)
    c.set(Calendar.MILLISECOND, 0)
    return c.timeInMillis
}

// Deterministic outcome per round -> returns slice index (0..8)
private fun l7OutcomeSlice(round: Int): Int {
    if (round <= 0) return 2
    val seed = sin(round * 91.7 + 173.3) * 43758.5453123
    val roll = (seed - floor(seed)) * 100.0
    return when {
        roll < 11.0 -> 0 // 777 (pink slice)
        roll < 55.5 -> listOf(2, 4, 6, 8)[floor(((roll - 11.0) / 44.5) * 4).toInt().coerceIn(0, 3)]
        else -> listOf(1, 3, 5, 7)[floor(((roll - 55.5) / 44.5) * 4).toInt().coerceIn(0, 3)]
    }
}

private fun l7TargetAngleForSlice(sliceIndex: Int): Float {
    val sliceAngle = 360f / l7WheelSlices.size
    return ((270f - sliceIndex * sliceAngle) % 360f + 360f) % 360f
}

private fun l7Format(v: Long): String = when {
    v >= 1_000_000L -> "${"%.1f".format(Locale.US, v / 1_000_000.0).removeSuffix(".0")}M"
    v >= 1000L -> "${"%.0f".format(Locale.US, v / 1000.0)}K"
    else -> v.toString()
}

private fun l7FormatComma(v: Long): String = String.format(Locale.US, "%,d", v)

private fun l7FormatRecordTime(timeMillis: Long): String {
    val c = Calendar.getInstance().apply { timeInMillis = timeMillis }
    return String.format(
        Locale.US,
        "%02d/%02d/%04d %02d:%02d:%02d",
        c.get(Calendar.DAY_OF_MONTH),
        c.get(Calendar.MONTH) + 1,
        c.get(Calendar.YEAR),
        c.get(Calendar.HOUR_OF_DAY),
        c.get(Calendar.MINUTE),
        c.get(Calendar.SECOND)
    )
}

@Composable
private fun L7TitleText(modifier: Modifier = Modifier) {
    Text(
        text = "Lucky 777",
        modifier = modifier,
        color = Color.White,
        fontSize = 19.sp,
        fontWeight = FontWeight.Black,
        fontFamily = FontFamily.Serif,
        fontStyle = FontStyle.Italic,
        style = TextStyle(
            shadow = Shadow(
                color = Color.Black.copy(alpha = 0.65f),
                offset = Offset(0f, 2f),
                blurRadius = 4f
            )
        )
    )
}

@Composable
private fun L7HistoryNewPill(modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .wrapContentSize(unbounded = true)
            .shadow(elevation = 2.dp, shape = shape, clip = false)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFF7A8A),
                        Color(0xFFE82542),
                        Color(0xFFB50D27)
                    )
                )
            )
            .border(0.7.dp, Color(0xFFFFD6DC), shape)
            .padding(horizontal = 3.5.dp, vertical = 0.5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "NEW",
            color = Color.White,
            fontSize = 5.5.sp,
            lineHeight = 6.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun L7WinnerAvatarPlaceholder(modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .clip(CircleShape)
            .background(Color(0xFF16171C))
    ) {
        val iconColor = Color(0xFF70747D)
        val cx = size.width / 2f
        drawCircle(
            color = iconColor,
            radius = size.minDimension * 0.17f,
            center = Offset(cx, size.height * 0.36f)
        )
        drawOval(
            color = iconColor,
            topLeft = Offset(size.width * 0.21f, size.height * 0.57f),
            size = Size(size.width * 0.58f, size.height * 0.36f)
        )
    }
}

@Composable
private fun L7WinnerFruitBadge(
    winKey: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(184.dp),
        contentAlignment = Alignment.Center
    ) {
        // Radiating white light beams + halo glow + stars + sparkles + confetti
        Canvas(Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val center = Offset(cx, cy)
            val maxR = size.minDimension / 2f

            // Soft white outer halo glow
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.0f to Color.White.copy(alpha = 0.90f),
                        0.55f to Color.White.copy(alpha = 0.42f),
                        0.82f to Color.White.copy(alpha = 0.12f),
                        1.0f to Color.Transparent
                    ),
                    center = center,
                    radius = maxR * 0.92f
                ),
                radius = maxR * 0.92f,
                center = center
            )

            // 12 Radiating sunburst light beams
            val rayCount = 12
            for (i in 0 until rayCount) {
                val angleDeg = i * (360f / rayCount) - 90f
                val halfSpread = if (i % 2 == 0) 9.5f else 6.0f
                val rayLen = if (i % 2 == 0) maxR * 0.98f else maxR * 0.86f
                val a1 = Math.toRadians((angleDeg - halfSpread).toDouble())
                val a2 = Math.toRadians((angleDeg + halfSpread).toDouble())
                val aMid = Math.toRadians(angleDeg.toDouble())

                val rayPath = Path().apply {
                    moveTo(cx, cy)
                    lineTo(cx + (cos(a1) * rayLen).toFloat(), cy + (sin(a1) * rayLen).toFloat())
                    lineTo(cx + (cos(a2) * rayLen).toFloat(), cy + (sin(a2) * rayLen).toFloat())
                    close()
                }
                drawPath(
                    path = rayPath,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.72f),
                            Color.White.copy(alpha = 0.28f),
                            Color.Transparent
                        ),
                        start = center,
                        end = Offset(cx + (cos(aMid) * rayLen).toFloat(), cy + (sin(aMid) * rayLen).toFloat())
                    )
                )
            }

            // Helper: 5-point golden star
            fun drawStar5(x: Float, y: Float, outerR: Float, innerR: Float, color: Color) {
                val p = Path()
                for (k in 0 until 10) {
                    val r = if (k % 2 == 0) outerR else innerR
                    val rad = Math.toRadians((k * 36.0) - 90.0)
                    val px = x + (cos(rad) * r).toFloat()
                    val py = y + (sin(rad) * r).toFloat()
                    if (k == 0) p.moveTo(px, py) else p.lineTo(px, py)
                }
                p.close()
                drawPath(p, color = color)
            }

            // Helper: 4-point white sparkle
            fun drawSparkle4(x: Float, y: Float, r: Float, color: Color = Color.White) {
                val inner = r * 0.24f
                val p = Path().apply {
                    moveTo(x, y - r)
                    lineTo(x + inner, y - inner)
                    lineTo(x + r, y)
                    lineTo(x + inner, y + inner)
                    lineTo(x, y + r)
                    lineTo(x - inner, y + inner)
                    lineTo(x - r, y)
                    lineTo(x - inner, y - inner)
                    close()
                }
                drawPath(p, color = color)
            }

            // Helper: tiny confetti shard
            fun drawShard(x: Float, y: Float, dx1: Float, dy1: Float, dx2: Float, dy2: Float, color: Color) {
                val p = Path().apply {
                    moveTo(x, y)
                    lineTo(x + dx1, y + dy1)
                    lineTo(x + dx2, y + dy2)
                    close()
                }
                drawPath(p, color = color)
            }

            // Golden stars (matching screenshot positions)
            drawStar5(cx - maxR * 0.50f, cy - maxR * 0.44f, 5.5.dp.toPx(), 2.6.dp.toPx(), Color(0xFFFFD83D))
            drawStar5(cx + maxR * 0.64f, cy + maxR * 0.22f, 5.2.dp.toPx(), 2.4.dp.toPx(), Color(0xFFFFD83D))
            drawStar5(cx - maxR * 0.62f, cy + maxR * 0.28f, 3.6.dp.toPx(), 1.7.dp.toPx(), Color(0xFFFFDF5D))
            drawStar5(cx + maxR * 0.60f, cy + maxR * 0.04f, 3.2.dp.toPx(), 1.5.dp.toPx(), Color(0xFFFFDF5D))

            // Crisp white 4-point sparkles
            drawSparkle4(cx - maxR * 0.65f, cy - maxR * 0.14f, 6.dp.toPx())
            drawSparkle4(cx - maxR * 0.64f, cy + maxR * 0.05f, 3.8.dp.toPx())
            drawSparkle4(cx + maxR * 0.42f, cy + maxR * 0.46f, 4.2.dp.toPx())
            drawSparkle4(cx + maxR * 0.62f, cy - maxR * 0.16f, 3.5.dp.toPx())

            // Colorful mini confetti triangles
            drawShard(cx - maxR * 0.42f, cy - maxR * 0.64f, 5.dp.toPx(), 11.dp.toPx(), -3.dp.toPx(), 9.dp.toPx(), Color(0xFF42D9FF))
            drawShard(cx - maxR * 0.52f, cy + maxR * 0.56f, 10.dp.toPx(), -9.dp.toPx(), 5.dp.toPx(), -1.dp.toPx(), Color(0xFFFF5E5E))
            drawShard(cx + maxR * 0.54f, cy + maxR * 0.42f, 10.dp.toPx(), 5.dp.toPx(), 7.dp.toPx(), 11.dp.toPx(), Color(0xFFB36BFF))
            drawShard(cx + maxR * 0.52f, cy - maxR * 0.52f, 9.dp.toPx(), -4.dp.toPx(), 4.dp.toPx(), -9.dp.toPx(), Color(0xFF4BFF5C))
        }

        // Glossy White Circular Badge
        Box(
            modifier = Modifier
                .size(112.dp)
                .shadow(elevation = 10.dp, shape = CircleShape, clip = false)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colorStops = arrayOf(
                            0.0f to Color.White,
                            0.74f to Color(0xFFF3F4F8),
                            0.90f to Color(0xFFE2E4EC),
                            1.0f to Color.White
                        )
                    )
                )
                .border(4.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = l7ImageFor(winKey),
                contentDescription = winKey,
                modifier = Modifier.size(if (winKey == "777") 86.dp else 80.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
private fun L7WheelSection(
    wheelRotation: Float,
    countdown: Int,
    phase: L7Phase,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        val frameSize = min(maxWidth.value * 0.88f, maxHeight.value * 0.98f).coerceAtLeast(180f).dp
        val innerWheelSize = frameSize * 0.68f
        val iconRadiusDp = innerWheelSize * 0.335f
        val iconSizeDp = innerWheelSize * 0.215f
        val hubSizeDp = innerWheelSize * 0.235f
        val sliceAngle = 360f / l7WheelSlices.size

        Box(
            modifier = Modifier.size(frameSize),
            contentAlignment = Alignment.Center
        ) {
            // 1. Rotating 9-Slice Wheel Background (Alternating Sky Blue / Royal Blue + Pink 777)
            Box(
                modifier = Modifier
                    .offset(y = (-frameSize.value * 0.018f).dp)
                    .size(innerWheelSize)
                    .clip(CircleShape)
                    .graphicsLayer { rotationZ = wheelRotation },
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val radius = size.minDimension / 2f
                    val center = Offset(size.width / 2f, size.height / 2f)

                    l7WheelSlices.forEachIndexed { idx, key ->
                        val startAngle = idx * sliceAngle - sliceAngle / 2f
                        val wedgeBrush = when (key) {
                            "777" -> Brush.radialGradient(
                                colorStops = arrayOf(
                                    0.0f to Color(0xFFFF4CF8),
                                    0.42f to Color(0xFFEA1BE3),
                                    0.78f to Color(0xFFD10DC9),
                                    1.0f to Color(0xFFAE08A8)
                                ),
                                center = center,
                                radius = radius
                            )
                            "Peach" -> Brush.radialGradient(
                                colorStops = arrayOf(
                                    0.0f to Color(0xFF66D8FF),
                                    0.40f to Color(0xFF42B9EF),
                                    0.78f to Color(0xFF2D9EE0),
                                    1.0f to Color(0xFF2084C9)
                                ),
                                center = center,
                                radius = radius
                            )
                            else -> Brush.radialGradient(
                                colorStops = arrayOf(
                                    0.0f to Color(0xFF3E8EEB),
                                    0.40f to Color(0xFF2A75D5),
                                    0.78f to Color(0xFF1F62BF),
                                    1.0f to Color(0xFF174DA4)
                                ),
                                center = center,
                                radius = radius
                            )
                        }

                        drawArc(
                            brush = wedgeBrush,
                            startAngle = startAngle,
                            sweepAngle = sliceAngle,
                            useCenter = true,
                            topLeft = Offset.Zero,
                            size = size
                        )
                    }

                    // Clean subtle straight radial dividers between slices
                    l7WheelSlices.forEachIndexed { idx, _ ->
                        val boundaryDeg = idx * sliceAngle - sliceAngle / 2f
                        val rad = Math.toRadians(boundaryDeg.toDouble())
                        val cosA = cos(rad).toFloat()
                        val sinA = sin(rad).toFloat()
                        drawLine(
                            color = Color(0xFF11408C).copy(alpha = 0.32f),
                            start = Offset(center.x + cosA * radius * 0.24f, center.y + sinA * radius * 0.24f),
                            end = Offset(center.x + cosA * radius, center.y + sinA * radius),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    // Glossy outer bevel ring highlight inside golden frame
                    drawCircle(
                        brush = Brush.radialGradient(
                            colorStops = arrayOf(
                                0.84f to Color.Transparent,
                                0.90f to Color.White.copy(alpha = 0.16f),
                                0.95f to Color(0xFF7BE0FF).copy(alpha = 0.22f),
                                1.0f to Color(0xFF0B2F73).copy(alpha = 0.55f)
                            ),
                            center = center,
                            radius = radius
                        ),
                        radius = radius,
                        center = center
                    )

                    // Thin bright glossy inner track line near rim
                    drawCircle(
                        color = Color.White.copy(alpha = 0.18f),
                        radius = radius * 0.915f,
                        center = center,
                        style = Stroke(width = 1.2.dp.toPx())
                    )

                    // Royal blue circular collar ring around center hub
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF164A9E),
                                Color(0xFF2167C7),
                                Color(0xFF1A52AA)
                            ),
                            center = center,
                            radius = radius * 0.34f
                        ),
                        radius = radius * 0.33f,
                        center = center
                    )
                    drawCircle(
                        color = Color(0xFF0E3578).copy(alpha = 0.55f),
                        radius = radius * 0.33f,
                        center = center,
                        style = Stroke(width = 1.4.dp.toPx())
                    )
                }
            }

            // 2. Real Glossy Shining Golden Highlight ONLY on Fruit Background Half Area (BEHIND Fruit/777 Icons!)
            if (phase == L7Phase.RESULT) {
                Canvas(
                    modifier = Modifier
                        .offset(y = (-frameSize.value * 0.018f).dp)
                        .size(innerWheelSize)
                        .clip(CircleShape)
                ) {
                    val radius = size.minDimension / 2f
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val startDeg = 270f - sliceAngle / 2f
                    val innerR = radius * 0.43f
                    val outerR = radius * 0.99f

                    val outerRect = Rect(
                        left = center.x - outerR,
                        top = center.y - outerR,
                        right = center.x + outerR,
                        bottom = center.y + outerR
                    )
                    val innerRect = Rect(
                        left = center.x - innerR,
                        top = center.y - innerR,
                        right = center.x + innerR,
                        bottom = center.y + innerR
                    )

                    val goldHalfWedge = Path().apply {
                        arcTo(
                            rect = outerRect,
                            startAngleDegrees = startDeg,
                            sweepAngleDegrees = sliceAngle,
                            forceMoveTo = true
                        )
                        arcTo(
                            rect = innerRect,
                            startAngleDegrees = startDeg + sliceAngle,
                            sweepAngleDegrees = -sliceAngle,
                            forceMoveTo = false
                        )
                        close()
                    }

                    clipPath(goldHalfWedge) {
                        drawRect(
                            brush = Brush.linearGradient(
                                colorStops = arrayOf(
                                    0.0f to Color(0xFFB86B00),
                                    0.18f to Color(0xFFF3B017),
                                    0.36f to Color(0xFFFFF5B8),
                                    0.52f to Color(0xFFFFCF2E),
                                    0.72f to Color(0xFFE08B00),
                                    0.88f to Color(0xFFFFE570),
                                    1.0f to Color(0xFFC47400)
                                ),
                                start = Offset(center.x - radius * 0.40f, center.y - outerR),
                                end = Offset(center.x + radius * 0.40f, center.y - innerR)
                            ),
                            topLeft = Offset(center.x - outerR, center.y - outerR),
                            size = Size(outerR * 2f, outerR)
                        )

                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFFFBE0).copy(alpha = 0.85f),
                                    Color(0xFFFFDF59).copy(alpha = 0.55f),
                                    Color.Transparent
                                ),
                                center = Offset(center.x, center.y - radius * 0.70f),
                                radius = radius * 0.34f
                            ),
                            radius = radius * 0.34f,
                            center = Offset(center.x, center.y - radius * 0.70f)
                        )

                        val gloss1 = Path().apply {
                            moveTo(center.x - radius * 0.34f, center.y - outerR * 0.96f)
                            lineTo(center.x - radius * 0.08f, center.y - outerR * 0.96f)
                            lineTo(center.x - radius * 0.24f, center.y - innerR * 0.95f)
                            lineTo(center.x - radius * 0.42f, center.y - innerR * 0.95f)
                            close()
                        }
                        drawPath(
                            path = gloss1,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.65f),
                                    Color(0xFFFFF7CC).copy(alpha = 0.28f),
                                    Color.Transparent
                                ),
                                startY = center.y - outerR,
                                endY = center.y - innerR
                            )
                        )

                        val gloss2 = Path().apply {
                            moveTo(center.x + radius * 0.04f, center.y - outerR * 0.98f)
                            lineTo(center.x + radius * 0.16f, center.y - outerR * 0.98f)
                            lineTo(center.x - radius * 0.02f, center.y - innerR * 0.95f)
                            lineTo(center.x - radius * 0.10f, center.y - innerR * 0.95f)
                            close()
                        }
                        drawPath(
                            path = gloss2,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.52f),
                                    Color(0xFFFFF3B0).copy(alpha = 0.18f),
                                    Color.Transparent
                                ),
                                startY = center.y - outerR,
                                endY = center.y - innerR
                            )
                        )

                        drawArc(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.72f),
                                    Color.Transparent
                                ),
                                startY = center.y - outerR,
                                endY = center.y - outerR * 0.84f
                            ),
                            startAngle = startDeg,
                            sweepAngle = sliceAngle,
                            useCenter = false,
                            topLeft = Offset(center.x - outerR * 0.95f, center.y - outerR * 0.95f),
                            size = Size(outerR * 1.90f, outerR * 1.90f),
                            style = Stroke(width = radius * 0.06f)
                        )
                    }

                    drawPath(
                        path = goldHalfWedge,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFFFF9CC),
                                Color(0xFFFFD23F),
                                Color(0xFFE08C00)
                            ),
                            startY = center.y - outerR,
                            endY = center.y - innerR
                        ),
                        style = Stroke(width = 1.6.dp.toPx(), join = StrokeJoin.Round)
                    )
                }
            }

            // 3. Rotating Fruit & 777 PNG Icons Layer (ABOVE Golden Highlight so Fruits/777 stay crisp & NEVER golden)
            Box(
                modifier = Modifier
                    .offset(y = (-frameSize.value * 0.018f).dp)
                    .size(innerWheelSize)
                    .clip(CircleShape)
                    .graphicsLayer { rotationZ = wheelRotation },
                contentAlignment = Alignment.Center
            ) {
                l7WheelSlices.forEachIndexed { idx, key ->
                    val deg = idx * sliceAngle
                    val rad = Math.toRadians(deg.toDouble())
                    val dx = (cos(rad) * iconRadiusDp.value).dp
                    val dy = (sin(rad) * iconRadiusDp.value).dp
                    val isSeven = key == "777"
                    AsyncImage(
                        model = l7ImageFor(key),
                        contentDescription = key,
                        modifier = Modifier
                            .offset(x = dx, y = dy)
                            .size(if (isSeven) iconSizeDp * 1.18f else iconSizeDp)
                            .graphicsLayer { rotationZ = deg + 90f },
                        contentScale = ContentScale.Fit
                    )
                }
            }

            // 4. Outer Wheel Frame Image (public/IMG_20261127_233303.png)
            AsyncImage(
                model = LUCKY777_WHEEL_FRAME,
                contentDescription = "Wheel Frame",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )

            // 5. Center Countdown Hub (Dark circle + Golden border + Red bold 20s countdown)
            Box(
                modifier = Modifier
                    .offset(y = (-frameSize.value * 0.018f).dp)
                    .size(hubSizeDp)
                    .shadow(4.dp, CircleShape)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF2B0E38),
                                Color(0xFF17071F),
                                Color(0xFF0A030E)
                            )
                        )
                    )
                    .border(
                        width = 2.4.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFFFF1A6),
                                Color(0xFFFFBE30),
                                Color(0xFFB56A0D),
                                Color(0xFFFFD863)
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (phase == L7Phase.BETTING) countdown.toString() else "0",
                    color = Color(0xFFE62028),
                    fontSize = (hubSizeDp.value * 0.48f).sp,
                    fontWeight = FontWeight.Black,
                    style = TextStyle(
                        shadow = Shadow(
                            color = Color(0xFFFF4D4D).copy(alpha = 0.45f),
                            offset = Offset.Zero,
                            blurRadius = 6f
                        )
                    )
                )
            }
        }
    }
}

@Composable
private fun L7LoadingPage(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val clampedProgress = progress.coerceIn(0f, 1f)
    val barShape = RoundedCornerShape(50)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF17100A))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
    ) {
        AsyncImage(
            model = LUCKY777_BACKGROUND,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.34f))
        )

        L7TitleText(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp)
        )

        AsyncImage(
            model = LUCKY777_LOGO,
            contentDescription = "Lucky 777 Logo",
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-14).dp)
                .size(126.dp),
            contentScale = ContentScale.Fit
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 22.dp, end = 22.dp, bottom = 26.dp)
                .height(25.dp)
                .shadow(elevation = 6.dp, shape = barShape, clip = false)
                .clip(barShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF562C05),
                            Color(0xFF7E460A),
                            Color(0xFF693706)
                        )
                    )
                )
                .border(
                    width = 2.8.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFFF8B8),
                            Color(0xFFFFDA3E),
                            Color(0xFFE09200),
                            Color(0xFFFFE56D)
                        )
                    ),
                    shape = barShape
                )
                .padding(2.8.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(barShape),
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.36f),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = size.height * 0.42f
                        )
                    )

                    val fillWidth = size.width * clampedProgress
                    if (fillWidth > 0f) {
                        clipRect(left = 0f, top = 0f, right = fillWidth, bottom = size.height) {
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF8BE8FF),
                                        Color(0xFF21A6FF),
                                        Color(0xFF006EE6),                                        Color(0xFF0047AB)
                                    )
                                ),
                                size = size
                            )

                            val stripeWidth = 14.dp.toPx()
                            val stripeGap = 15.dp.toPx()
                            val step = stripeWidth + stripeGap
                            val slant = size.height * 0.85f
                            var x = -slant
                            while (x < size.width + slant) {
                                val stripePath = Path().apply {
                                    moveTo(x, size.height)
                                    lineTo(x + slant, 0f)
                                    lineTo(x + slant + stripeWidth, 0f)
                                    lineTo(x + stripeWidth, size.height)
                                    close()
                                }
                                drawPath(
                                    path = stripePath,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.52f),
                                            Color(0xFFB3F0FF).copy(alpha = 0.28f),
                                            Color(0xFF66D1FF).copy(alpha = 0.16f)
                                        )
                                    )
                                )
                                x += step
                            }

                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.50f),
                                        Color.White.copy(alpha = 0.12f),
                                        Color.Transparent
                                    ),
                                    startY = 0f,
                                    endY = size.height * 0.50f
                                ),
                                size = Size(size.width, size.height * 0.50f)
                            )
                        }
                    }
                }

                Text(
                    text = "${(clampedProgress * 100).toInt()}%",
                    color = Color.White,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    style = TextStyle(
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.75f),
                            offset = Offset(0f, 1.2f),
                            blurRadius = 2.5f
                        )
                    )
                )
            }
        }
    }
}

@Composable
private fun L7GlassTopButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier
            .size(28.dp)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.42f),
                        Color(0xFFB8C4DC).copy(alpha = 0.16f),
                        Color(0xFF1F2640).copy(alpha = 0.24f),
                        Color.White.copy(alpha = 0.26f)
                    )
                )
            )
            .border(
                width = 1.25.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.92f),
                        Color.White.copy(alpha = 0.32f),
                        Color.White.copy(alpha = 0.58f)
                    )
                ),
                shape = shape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawOval(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.72f),
                        Color.White.copy(alpha = 0.18f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = size.height * 0.52f
                ),
                topLeft = Offset(size.width * 0.10f, size.height * 0.04f),
                size = Size(size.width * 0.80f, size.height * 0.44f)
            )
            drawOval(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.24f)
                    ),
                    startY = size.height * 0.72f,
                    endY = size.height
                ),
                topLeft = Offset(size.width * 0.16f, size.height * 0.74f),
                size = Size(size.width * 0.68f, size.height * 0.24f)
            )
        }
        content()
    }
}

@Composable
private fun L7BottomBalanceBar(
    coinResId: String,
    balance: Long,
    modifier: Modifier = Modifier
) {
    val pillShape = RoundedCornerShape(7.dp)
    val goldColor = Color(0xFFFFD84D)
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(pillShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.58f),
                            Color(0xFFD5BFA6).copy(alpha = 0.34f),
                            Color(0xFF5A3418).copy(alpha = 0.50f),
                            Color(0xFF3E200C).copy(alpha = 0.55f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.90f),
                            Color.White.copy(alpha = 0.38f),
                            Color(0xFFE6C8A0).copy(alpha = 0.45f)
                        )
                    ),
                    shape = pillShape
                )
                .drawBehind {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.42f),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = size.height * 0.48f
                        )
                    )
                }
                .padding(horizontal = 4.dp, vertical = 3.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                if (coinResId.isNotBlank()) {
                    AsyncImage(
                        model = coinResId,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        contentScale = ContentScale.Fit
                    )
                }
                Canvas(Modifier.size(width = 8.dp, height = 7.dp)) {
                    val tri = Path().apply {
                        moveTo(size.width / 2f, 0.8.dp.toPx())
                        lineTo(size.width - 0.8.dp.toPx(), size.height - 0.8.dp.toPx())
                        lineTo(0.8.dp.toPx(), size.height - 0.8.dp.toPx())
                        close()
                    }
                    drawPath(tri, goldColor, style = Fill)
                    drawPath(tri, goldColor, style = Stroke(1.3.dp.toPx(), join = StrokeJoin.Round))
                }
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                balance.toString(),
                color = goldColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                style = TextStyle(shadow = Shadow(Color.Black.copy(alpha = 0.65f), Offset(0f, 1.5f), 3f))
            )
            Canvas(Modifier.size(width = 7.dp, height = 11.dp)) {
                val chevron = Path().apply {
                    moveTo(1.5.dp.toPx(), 1.5.dp.toPx())
                    lineTo(size.width - 1.5.dp.toPx(), size.height / 2f)
                    lineTo(1.5.dp.toPx(), size.height - 1.5.dp.toPx())
                }
                drawPath(
                    chevron,
                    Color.Black.copy(alpha = 0.45f),
                    style = Stroke(2.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                drawPath(
                    chevron,
                    goldColor,
                    style = Stroke(2.0.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }
    }
}

@Composable
private fun L7GlossyRepeatButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val outerShape = RoundedCornerShape(9.dp)
    val innerShape = RoundedCornerShape(7.dp)
    Box(
        modifier = modifier
            .clip(outerShape)
            .background(Color(0xFF472710).copy(alpha = 0.72f))
            .border(
                width = 1.2.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.94f),
                        Color(0xFFEBDCD0).copy(alpha = 0.75f),
                        Color.White.copy(alpha = 0.85f)
                    )
                ),
                shape = outerShape
            )
            .clickable(onClick = onClick)
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .clip(innerShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.52f),
                            Color(0xFFCBB5A0).copy(alpha = 0.30f),
                            Color(0xFF563218).copy(alpha = 0.68f),
                            Color(0xFF6B4122).copy(alpha = 0.58f)
                        )
                    )
                )
                .border(
                    width = 0.8.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.85f),
                            Color.White.copy(alpha = 0.28f),
                            Color.White.copy(alpha = 0.52f)
                        )
                    ),
                    shape = innerShape
                )
                .drawBehind {
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.45f),
                                Color.White.copy(alpha = 0.10f),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = size.height * 0.52f
                        ),
                        topLeft = Offset(1.dp.toPx(), 0.5.dp.toPx()),
                        size = Size(size.width - 2.dp.toPx(), size.height * 0.48f),
                        cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                    )
                }
                .padding(horizontal = 7.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Repeat",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                style = TextStyle(shadow = Shadow(Color.Black.copy(alpha = 0.55f), Offset(0f, 1.5f), 2.5f))
            )
        }
    }
}

@Composable
private fun L7RulesPopupCard(
    sheetHeight: Dp,
    onDismiss: () -> Unit
) {
    val cardShape = RoundedCornerShape(10.dp)
    val rules = listOf(
        "Players can place bets on the fruit or 77 they predict the wheel will stop on, in order to earn rewards.",
        "Players need to select the betting amount before starting the game.",
        "If the result matches the player's bet, they will receive rewards based on the odds.",
        "When lucky 77 occurs, there is a chance to hit multiple awards at the same time."
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(sheetHeight),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.84f)
                    .wrapContentHeight()
                    .heightIn(max = sheetHeight * 0.62f)
                    .shadow(elevation = 10.dp, shape = cardShape, clip = false)
                    .clip(cardShape)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF2553A7),
                                Color(0xFF224DA0),
                                Color(0xFF1F4896)
                            )
                        )
                    )
                    .border(1.2.dp, Color(0xFF426EC2), cardShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 14.dp, top = 11.dp, bottom = 22.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Rule",
                            modifier = Modifier.align(Alignment.Center),
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif
                        )

                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF132E63))
                                .border(1.2.dp, Color(0xFF5D7EB8), CircleShape)
                                .clickable(onClick = onDismiss),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(Modifier.size(10.dp)) {
                                val pad = 1.dp.toPx()
                                drawLine(
                                    color = Color.White,
                                    start = Offset(pad, pad),
                                    end = Offset(size.width - pad, size.height - pad),
                                    strokeWidth = 2.2.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                                drawLine(
                                    color = Color.White,
                                    start = Offset(size.width - pad, pad),
                                    end = Offset(pad, size.height - pad),
                                    strokeWidth = 2.2.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rules.forEachIndexed { index, ruleText ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "${index + 1}.",
                                    color = Color(0xFFF3F6FC),
                                    fontSize = 12.5.sp,
                                    lineHeight = 17.sp,
                                    fontFamily = FontFamily.Serif
                                )
                                Text(
                                    text = ruleText,
                                    modifier = Modifier.weight(1f),
                                    color = Color(0xFFF3F6FC),
                                    fontSize = 12.5.sp,
                                    lineHeight = 17.sp,
                                    fontFamily = FontFamily.Serif
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "v1.0.14",
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 9.dp, bottom = 5.dp),
                    color = Color(0xFFDCE6FA),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Serif
                )
            }
        }
    }
}

@Composable
private fun L7HistoryPopupCard(
    history: List<L7Round>,
    sheetHeight: Dp,
    onDismiss: () -> Unit
) {
    val cardShape = RoundedCornerShape(12.dp)
    val recordShape = RoundedCornerShape(9.dp)
    val recentRecords = history.takeLast(200).reversed()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(sheetHeight),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .height(sheetHeight * 0.72f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 10.dp)
                        .shadow(elevation = 12.dp, shape = cardShape, clip = false)
                        .clip(cardShape)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF1D4BA8),
                                    Color(0xFF184299),
                                    Color(0xFF153B8B)
                                )
                            )
                        )
                        .border(1.2.dp, Color(0xFF3E70D4), cardShape)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Canvas(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .fillMaxWidth(0.62f)
                                .height(36.dp)
                        ) {
                            val w = size.width
                            val h = size.height
                            val tabPath = Path().apply {
                                moveTo(0f, 0f)
                                lineTo(w, 0f)
                                cubicTo(w * 0.90f, h * 0.08f, w * 0.88f, h, w * 0.76f, h)
                                lineTo(w * 0.24f, h)
                                cubicTo(w * 0.12f, h, w * 0.10f, h * 0.08f, 0f, 0f)
                                close()
                            }
                            drawPath(
                                path = tabPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF3E76E0),
                                        Color(0xFF2D62C7)
                                    )
                                )
                            )
                            drawPath(
                                path = tabPath,
                                color = Color(0xFF6B9CF5).copy(alpha = 0.55f),
                                style = Stroke(width = 1.dp.toPx())
                            )
                        }

                        Text(
                            text = "Game records",
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 11.dp),
                            color = Color(0xFFEAF2FF),
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 6.dp, end = 8.dp)
                                .size(25.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10285C))
                                .border(1.1.dp, Color(0xFF4A77CC), CircleShape)
                                .clickable(onClick = onDismiss),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(Modifier.size(9.5.dp)) {
                                val pad = 0.8.dp.toPx()
                                drawLine(
                                    color = Color.White,
                                    start = Offset(pad, pad),
                                    end = Offset(size.width - pad, size.height - pad),
                                    strokeWidth = 2.1.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                                drawLine(
                                    color = Color.White,
                                    start = Offset(size.width - pad, pad),
                                    end = Offset(pad, size.height - pad),
                                    strokeWidth = 2.1.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                            }
                        }
                    }

                    if (recentRecords.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No game records yet.",
                                color = Color(0xFFAEC7F7),
                                fontSize = 12.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(top = 2.dp, bottom = 6.dp)
                        ) {
                            items(recentRecords, key = { "${it.round}_${it.time}" }) { r ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(recordShape)
                                        .background(Color(0xFF2858BD))
                                        .border(1.dp, Color(0xFF0C2254), recordShape)
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Round: ${r.round}",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = l7FormatRecordTime(r.time),
                                            color = Color(0xFFC9DCFF),
                                            fontSize = 11.sp
                                        )
                                    }

                                    HorizontalDivider(
                                        thickness = 0.7.dp,
                                        color = Color(0xFF4D80E3).copy(alpha = 0.55f)
                                    )

                                    Text(
                                        text = "Featured Fruits:",
                                        color = Color(0xFFBBD3FF),
                                        fontSize = 11.sp
                                    )

                                    if (r.betsMap.isNotEmpty()) {
                                        Row(
                                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            r.betsMap.forEach { (fruitKey, amt) ->
                                                Row(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(Color(0xFF163885))
                                                        .padding(horizontal = 5.dp, vertical = 2.5.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    AsyncImage(
                                                        model = l7ImageFor(fruitKey),
                                                        contentDescription = fruitKey,
                                                        modifier = Modifier.size(14.dp),
                                                        contentScale = ContentScale.Fit
                                                    )
                                                    Text(
                                                        text = amt.toString(),
                                                        color = Color.White,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        Text(
                                            text = if (r.myBet > 0L) r.myBet.toString() else "-",
                                            color = Color(0xFFD6E5FF),
                                            fontSize = 11.sp
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "Winning Fruit:",
                                            color = Color(0xFFBBD3FF),
                                            fontSize = 11.sp
                                        )
                                        AsyncImage(
                                            model = l7ImageFor(r.winner),
                                            contentDescription = r.winner,
                                            modifier = Modifier.size(16.dp),
                                            contentScale = ContentScale.Fit
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Text(
                                            text = "Win coins:",
                                            color = Color(0xFFBBD3FF),
                                            fontSize = 11.sp
                                        )
                                        AsyncImage(
                                            model = LUCKY777_COIN,
                                            contentDescription = null,
                                            modifier = Modifier.size(13.dp),
                                            contentScale = ContentScale.Fit
                                        )
                                        Text(
                                            text = l7FormatComma(r.winAmount),
                                            color = if (r.winAmount > 0L) l7Gold else Color.White,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    Text(
                                        text = "Coin Balance:",
                                        color = Color(0xFFBBD3FF),
                                        fontSize = 11.sp
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        AsyncImage(
                                            model = LUCKY777_COIN,
                                            contentDescription = null,
                                            modifier = Modifier.size(13.dp),
                                            contentScale = ContentScale.Fit
                                        )
                                        Text(
                                            text = "${l7FormatComma(r.balanceBefore)} -> ${l7FormatComma(r.balanceAfter)}",
                                            color = Color.White,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Text(
                        text = "Show up to last 7 days of game records, maximum 200 records.",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        color = Color(0xFFA6C3F7),
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center
                    )
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-1).dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = "LUCKY",
                        color = Color(0xFFFFDE3B),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        style = TextStyle(
                            shadow = Shadow(
                                color = Color(0xFF5A1E00),
                                offset = Offset(0f, 2f),
                                blurRadius = 3f
                            )
                        )
                    )
                    Text(
                        text = "77",
                        color = Color(0xFFFF362E),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        fontStyle = FontStyle.Italic,
                        style = TextStyle(
                            shadow = Shadow(
                                color = Color(0xFFFFE066),
                                offset = Offset(0f, 1.5f),
                                blurRadius = 3f
                            )
                        )
                    )
                    Text(
                        text = "PRO",
                        color = Color(0xFFFFDE3B),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        style = TextStyle(
                            shadow = Shadow(
                                color = Color(0xFF5A1E00),
                                offset = Offset(0f, 2f),
                                blurRadius = 3f
                            )
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun Lucky777(
    onClose: () -> Unit = {},
    onMinimize: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dao = remember { WalletDatabase.get(context).walletDao() }

    var balance by remember { mutableLongStateOf(82927L) }
    var phase by remember { mutableStateOf(L7Phase.BETTING) }
    var round by remember { mutableIntStateOf(1) }
    var countdown by remember { mutableIntStateOf(20) }
    var winnerSlice by remember { mutableIntStateOf(2) }
    var wheelRotation by remember { mutableFloatStateOf(l7TargetAngleForSlice(2)) }

    var chipIndex by remember { mutableIntStateOf(1) }
    var bets by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }
    var lastBets by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }
    var history by remember { mutableStateOf<List<L7Round>>(emptyList()) }

    var muted by remember { mutableStateOf(false) }
    var historyOpen by remember { mutableStateOf(false) }
    var rulesOpen by remember { mutableStateOf(false) }
    var winnerOpen by remember { mutableStateOf(false) }
    var winAmount by remember { mutableLongStateOf(0L) }
    var betAmount by remember { mutableLongStateOf(0L) }
    var winnerTimer by remember { mutableIntStateOf(5) }
    var processedRound by remember { mutableIntStateOf(-1) }

    var loading by remember { mutableStateOf(true) }
    var loadingProgress by remember { mutableFloatStateOf(0f) }

    val todayWin by remember(history) {
        derivedStateOf { history.sumOf { it.winAmount } }
    }

    LaunchedEffect(Unit) {
        runCatching {
            balance = dao.getUserData()?.balance ?: 82927L
            val arr = JSONArray(
                context.getSharedPreferences("lucky777_native", Context.MODE_PRIVATE)
                    .getString("history", "[]")
            )
            history = (0 until arr.length()).mapNotNull { i ->
                arr.optJSONObject(i)?.let { obj ->
                    val betsObj = obj.optJSONObject("betsMap")
                    val loadedBets = buildMap<String, Long> {
                        if (betsObj != null) {
                            val keys = betsObj.keys()
                            while (keys.hasNext()) {
                                val k = keys.next()
                                val v = betsObj.optLong(k, 0L)
                                if (v > 0L) put(k, v)
                            }
                        }
                    }
                    L7Round(
                        round = obj.optInt("round"),
                        winner = obj.optString("winner"),
                        time = obj.optLong("time"),
                        myBet = obj.optLong("myBet", 0L),
                        winAmount = obj.optLong("winAmount", 0L),
                        betsMap = loadedBets,
                        balanceBefore = obj.optLong("balanceBefore", 0L),
                        balanceAfter = obj.optLong("balanceAfter", 0L)
                    )
                }
            }.filter { it.time >= l7Reset() }.sortedBy { it.round }.takeLast(200)
        }
        for (step in 1..50) {            loadingProgress = step / 50f
            delay(24)
        }
        delay(120)
        loading = false
    }

    suspend fun walletChange(delta: Long, title: String) {
        val old = dao.getUserData() ?: UserDataEntity(balance = balance)
        val next = (old.balance + delta).coerceAtLeast(0L)
        dao.saveUserData(old.copy(balance = next))
        balance = next
        val c = Calendar.getInstance()
        val date = String.format(
            Locale.US,
            "%04d.%02d.%02d %02d:%02d",
            c.get(Calendar.YEAR),
            c.get(Calendar.MONTH) + 1,
            c.get(Calendar.DAY_OF_MONTH),
            c.get(Calendar.HOUR_OF_DAY),
            c.get(Calendar.MINUTE)
        )
        dao.insertTransaction(
            TransactionEntity(
                title = title,
                amount = delta,
                date = date,
                timestamp = System.currentTimeMillis(),
                type = "coin"
            )
        )
    }

    fun persistHistory() {
        val arr = JSONArray()
        history.takeLast(200).forEach {
            val betsObj = JSONObject()
            it.betsMap.forEach { (k, v) -> betsObj.put(k, v) }
            arr.put(
                JSONObject()
                    .put("round", it.round)
                    .put("winner", it.winner)
                    .put("time", it.time)
                    .put("myBet", it.myBet)
                    .put("winAmount", it.winAmount)
                    .put("betsMap", betsObj)
                    .put("balanceBefore", it.balanceBefore)
                    .put("balanceAfter", it.balanceAfter)
            )
        }
        context.getSharedPreferences("lucky777_native", Context.MODE_PRIVATE)
            .edit()
            .putString("history", arr.toString())
            .apply()
    }

    // Synchronized 31s round loop: 20s BETTING + 6s SPINNING + 5s RESULT
    LaunchedEffect(loading) {
        if (loading) return@LaunchedEffect
        while (true) {
            val now = System.currentTimeMillis()
            val elapsedMs = (now - l7Reset(now)).coerceAtLeast(0L)
            val cycleMs = elapsedMs % 31_000L
            val cycleSec = (cycleMs / 1000L).toInt()
            val currentRound = (elapsedMs / 31_000L).toInt() + 1

            round = currentRound
            val currSlice = l7OutcomeSlice(currentRound)
            val prevSlice = l7OutcomeSlice(currentRound - 1)
            winnerSlice = currSlice

            val prevAngle = l7TargetAngleForSlice(prevSlice)
            val currAngle = l7TargetAngleForSlice(currSlice)
            val forwardDiff = ((currAngle - prevAngle) + 360f) % 360f

            when {
                cycleSec < 20 -> {
                    phase = L7Phase.BETTING
                    countdown = 20 - cycleSec
                    wheelRotation = prevAngle
                    winnerOpen = false
                    delay(100)
                }
                cycleSec < 26 -> {
                    phase = L7Phase.SPINNING
                    countdown = 26 - cycleSec
                    val p = ((cycleMs - 20_000L).coerceIn(0L, 6_000L)) / 6_000.0
                    val eased = 1.0 - (1.0 - p).pow(3.0)
                    wheelRotation = prevAngle + (eased * (360.0 * 5.0 + forwardDiff)).toFloat()
                    delay(16)
                }
                else -> {
                    phase = L7Phase.RESULT
                    countdown = 31 - cycleSec
                    wheelRotation = currAngle
                    delay(100)
                }
            }
        }
    }

    // Round settlement & Winner Page trigger
    LaunchedEffect(phase, round, loading) {
        if (loading) return@LaunchedEffect
        if (phase == L7Phase.BETTING) {
            if (bets.isNotEmpty()) {
                lastBets = bets
                bets = emptyMap()
            }
        }
        if (phase == L7Phase.RESULT && processedRound != round) {
            processedRound = round
            val winnerKey = l7WheelSlices[l7OutcomeSlice(round)]
            val roundBetsSnapshot = bets.toMap()
            betAmount = roundBetsSnapshot.values.sum()
            winAmount = (roundBetsSnapshot[winnerKey] ?: 0L) * l7MultiplierFor(winnerKey)
            val balBefore = balance
            val balAfter = balance + winAmount
            history = (history + L7Round(
                round = round,
                winner = winnerKey,
                time = System.currentTimeMillis(),
                myBet = betAmount,
                winAmount = winAmount,
                betsMap = roundBetsSnapshot,
                balanceBefore = balBefore,
                balanceAfter = balAfter
            )).takeLast(200)
            persistHistory()
            delay(1000)
            if (winAmount > 0L) {
                walletChange(winAmount, "Won in Lucky 777")
            }
            winnerTimer = 5
            winnerOpen = true
        }
    }

    LaunchedEffect(winnerOpen) {
        if (winnerOpen) {
            repeat(5) {
                delay(1000)
                winnerTimer = 4 - it
            }
            winnerOpen = false
        }
    }

    fun placeBet(key: String) {
        scope.launch {
            val amount = l7Chips[chipIndex].value
            if (phase == L7Phase.BETTING && balance >= amount) {
                walletChange(-amount, "Bet on $key in Lucky 777")
                bets = bets.toMutableMap().also { it[key] = (it[key] ?: 0L) + amount }
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent),
        contentAlignment = Alignment.BottomCenter
    ) {
        val h = maxHeight * 0.70f

        // Full screen dim backdrop behind the 70% game window
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.50f))
        )

        // 70% Game Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(h)
                .background(Color(0xFF17100A))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
        ) {
            // Main Screen Background + Dim Overlay
            AsyncImage(
                model = LUCKY777_BACKGROUND,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.34f))
            )

            // Main Vertical Layout
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 8.dp, bottom = 8.dp)
            ) {
                // Top Bar
                Box(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        L7GlassTopButton(onClick = { muted = !muted }) {
                            Canvas(Modifier.size(15.dp)) {
                                val p = Path().apply {
                                    moveTo(1.5.dp.toPx(), 5.dp.toPx())
                                    lineTo(5.dp.toPx(), 5.dp.toPx())
                                    lineTo(9.5.dp.toPx(), 1.8.dp.toPx())
                                    lineTo(9.5.dp.toPx(), 13.2.dp.toPx())
                                    lineTo(5.dp.toPx(), 10.dp.toPx())
                                    lineTo(1.5.dp.toPx(), 10.dp.toPx())
                                    close()
                                }
                                drawPath(p, Color.White.copy(alpha = if (muted) 0.55f else 1f), style = Fill)
                                drawPath(p, Color.White.copy(alpha = if (muted) 0.55f else 1f), style = Stroke(1.2.dp.toPx(), join = StrokeJoin.Round))
                                if (muted) {
                                    drawLine(
                                        Color.White,
                                        Offset(2.dp.toPx(), 2.dp.toPx()),
                                        Offset(13.dp.toPx(), 13.dp.toPx()),
                                        2.2.dp.toPx(),
                                        cap = StrokeCap.Round
                                    )
                                } else {
                                    drawArc(
                                        Color.White,
                                        -52f,
                                        104f,
                                        false,
                                        topLeft = Offset(7.2.dp.toPx(), 2.5.dp.toPx()),
                                        size = Size(7.2.dp.toPx(), 10.dp.toPx()),
                                        style = Stroke(1.9.dp.toPx(), cap = StrokeCap.Round)
                                    )
                                }
                            }
                        }
                        L7GlassTopButton(onClick = { rulesOpen = true }) {
                            Text(
                                "?",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                style = TextStyle(shadow = Shadow(Color.White.copy(alpha = 0.55f), Offset.Zero, 6f))
                            )
                        }
                    }

                    L7TitleText(modifier = Modifier.align(Alignment.Center))

                    Row(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        L7GlassTopButton(onClick = { historyOpen = true }) {
                            Canvas(Modifier.size(14.dp)) {
                                val hBar = 2.4.dp.toPx()
                                val r = CornerRadius(hBar / 2f, hBar / 2f)
                                drawRoundRect(Color.White, Offset(0.5.dp.toPx(), 2.dp.toPx()), Size(13.dp.toPx(), hBar), r)
                                drawRoundRect(Color.White, Offset(0.5.dp.toPx(), 6.dp.toPx()), Size(13.dp.toPx(), hBar), r)
                                drawRoundRect(Color.White, Offset(0.5.dp.toPx(), 10.dp.toPx()), Size(8.5.dp.toPx(), hBar), r)
                                drawRoundRect(Color.White, Offset(10.5.dp.toPx(), 10.dp.toPx()), Size(3.dp.toPx(), hBar), r)
                            }
                        }
                        L7GlassTopButton(onClick = { onMinimize() }) {
                            Canvas(Modifier.size(14.dp)) {
                                val tri = Path().apply {
                                    moveTo(2.2.dp.toPx(), 4.2.dp.toPx())
                                    lineTo(11.8.dp.toPx(), 4.2.dp.toPx())
                                    lineTo(7.dp.toPx(), 10.8.dp.toPx())
                                    close()
                                }
                                drawPath(tri, Color.White.copy(alpha = 0.35f), style = Stroke(3.6.dp.toPx(), join = StrokeJoin.Round))
                                drawPath(
                                    tri,
                                    Brush.verticalGradient(listOf(Color(0xFFDDE3EE), Color.White)),
                                    style = Fill
                                )
                                drawPath(
                                    tri,
                                    Brush.verticalGradient(listOf(Color(0xFFDDE3EE), Color.White)),
                                    style = Stroke(2.2.dp.toPx(), join = StrokeJoin.Round)
                                )
                            }
                        }
                        L7GlassTopButton(onClick = { onClose() }) {
                            Canvas(Modifier.size(14.dp)) {
                                val a = 3.1.dp.toPx()
                                val b = 10.9.dp.toPx()
                                drawLine(Color.White.copy(alpha = 0.35f), Offset(a, a), Offset(b, b), 4.6.dp.toPx(), cap = StrokeCap.Round)
                                drawLine(Color.White.copy(alpha = 0.35f), Offset(b, a), Offset(a, b), 4.6.dp.toPx(), cap = StrokeCap.Round)
                                drawLine(Color.White, Offset(a, a), Offset(b, b), 3.0.dp.toPx(), cap = StrokeCap.Round)
                                drawLine(Color.White, Offset(b, a), Offset(a, b), 3.0.dp.toPx(), cap = StrokeCap.Round)
                            }
                        }
                    }
                }

                // Center Rotating Wheel + Frame + 20s Countdown Hub
                L7WheelSection(
                    wheelRotation = wheelRotation,
                    countdown = countdown,
                    phase = phase,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 2.dp)
                )

                // Unified TODAY'S WIN + Results Bar + 3 Betting Cards Block (Matching 2nd Image)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 1. TODAY'S WIN Pill
                    val todayPillShape = RoundedCornerShape(50)
                    Row(
                        modifier = Modifier
                            .height(23.dp)
                            .clip(todayPillShape)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF1C2959).copy(alpha = 0.90f),
                                        Color(0xFF0A0F29).copy(alpha = 0.95f)
                                    )
                                )
                            )
                            .border(
                                width = 1.dp,
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFFCFA54E),
                                        Color(0xFFFFE599),
                                        Color(0xFFCFA54E)
                                    )
                                ),
                                shape = todayPillShape
                            )
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        Text(
                            text = "TODAY'S WIN",
                            color = Color(0xFFF3F6FF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Serif
                        )
                        Text(
                            text = l7FormatComma(todayWin),
                            color = Color(0xFFFFC933),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Black,
                            style = TextStyle(
                                shadow = Shadow(Color.Black.copy(alpha = 0.7f), Offset(0f, 1f), 2f)
                            )
                        )
                    }

                    Spacer(Modifier.height(3.dp))

                    // 2. Connected Results Bar (Directly attached on top of the 3-Card Image)
                    val resultsTabShape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                            .height(29.dp)
                            .clip(resultsTabShape)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF10173A).copy(alpha = 0.92f),
                                        Color(0xFF070A1F).copy(alpha = 0.96f)
                                    )
                                )
                            )
                            .drawBehind {
                                val strokePx = 1.1.dp.toPx()
                                val r = 12.dp.toPx()
                                val borderPath = Path().apply {
                                    moveTo(strokePx / 2f, size.height)
                                    lineTo(strokePx / 2f, r)
                                    quadraticTo(strokePx / 2f, strokePx / 2f, r, strokePx / 2f)
                                    lineTo(size.width - r, strokePx / 2f)
                                    quadraticTo(size.width - strokePx / 2f, strokePx / 2f, size.width - strokePx / 2f, r)
                                    lineTo(size.width - strokePx / 2f, size.height)
                                }
                                drawPath(
                                    path = borderPath,
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFFC99D44),
                                            Color(0xFFFFE79E),
                                            Color(0xFFC99D44)
                                        )
                                    ),
                                    style = Stroke(width = strokePx)
                                )
                            }
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Results:",
                            color = Color(0xFFDCE6FF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (history.isEmpty()) {
                                Text(
                                    text = "-",
                                    color = Color.White.copy(alpha = 0.50f),
                                    fontSize = 11.sp
                                )
                            } else {
                                // Newest winning fruit first (leftmost) with exact existing NEW pill tag
                                val recentStrip = history.takeLast(12).reversed()
                                recentStrip.forEachIndexed { idx, rec ->
                                    val isNewest = idx == 0
                                    Box(
                                        modifier = Modifier.size(22.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AsyncImage(
                                            model = l7ImageFor(rec.winner),
                                            contentDescription = rec.winner,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Fit
                                        )
                                        if (isNewest) {
                                            L7HistoryNewPill(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .offset(x = 3.dp, y = (-2).dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. 3-Card Betting Panel (Same PNG, full height, seamlessly joined with Results bar, NO golden border)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .height(106.dp)
                    ) {
                        AsyncImage(
                            model = LUCKY777_CARDS_BG,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.FillBounds
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 4.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            l7BetItems.forEach { item ->
                                val myBetVal = bets[item.key] ?: 0L
                                val betText = "${l7Format(myBetVal)}/${l7Format(myBetVal)}"
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) { placeBet(item.key) }
                                        .padding(horizontal = 4.dp, vertical = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = betText,
                                        color = if (myBetVal > 0L) l7Gold else Color(0xFFFFE599),
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        style = TextStyle(
                                            shadow = Shadow(Color.Black.copy(alpha = 0.7f), Offset(0f, 1f), 2f)
                                        )
                                    )
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AsyncImage(
                                            model = item.image,
                                            contentDescription = item.key,
                                            modifier = Modifier.size(if (item.key == "777") 48.dp else 44.dp),
                                            contentScale = ContentScale.Fit
                                        )
                                        Spacer(Modifier.width(3.dp))
                                        AsyncImage(
                                            model = item.multImage,
                                            contentDescription = "x${item.multiplier}",
                                            modifier = Modifier.height(30.dp),
                                            contentScale = ContentScale.Fit
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom Decor + Balance & Chips Controls
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(88.dp)
                ) {
                    AsyncImage(
                        model = LUCKY777_BOTTOM_LEFT_DECOR,
                        contentDescription = null,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 8.dp, bottom = 44.dp)
                            .size(42.dp),
                        contentScale = ContentScale.Fit,
                        alignment = Alignment.BottomStart
                    )

                    L7BottomBalanceBar(
                        coinResId = LUCKY777_COIN,
                        balance = balance,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 8.dp, bottom = 2.dp)
                    )

                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        L7GlossyRepeatButton(
                            onClick = {
                                scope.launch {
                                    val cost = lastBets.values.sum()
                                    if (phase == L7Phase.BETTING && cost > 0L && balance >= cost) {
                                        walletChange(-cost, "Repeat Bet in Lucky 777")
                                        bets = bets.toMutableMap().also { m ->
                                            lastBets.forEach { (k, v) -> m[k] = (m[k] ?: 0L) + v }
                                        }
                                    }
                                }
                            }
                        )
                        l7Chips.forEachIndexed { i, c ->
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .graphicsLayer {
                                        val scale = if (chipIndex == i) 1.08f else 1f
                                        scaleX = scale
                                        scaleY = scale
                                        translationY = if (chipIndex == i) -8.dp.toPx() else 0f
                                    }
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { chipIndex = i },
                                contentAlignment = Alignment.Center
                            ) {
                                if (c.image.isNotBlank()) {
                                    AsyncImage(
                                        model = c.image,
                                        contentDescription = c.label,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit
                                    )
                                } else {
                                    Text(c.label, color = Color.White, fontSize = 8.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Loading Page Overlay
            if (loading) {
                L7LoadingPage(progress = loadingProgress)
            }
        }

        // Compact History Popup Card
        if (historyOpen) {
            L7HistoryPopupCard(
                history = history,
                sheetHeight = h,
                onDismiss = { historyOpen = false }
            )
        }

        // Compact Rules Card Popup
        if (rulesOpen) {
            L7RulesPopupCard(
                sheetHeight = h,
                onDismiss = { rulesOpen = false }
            )
        }

        // Winner Page (Matching 1st Image: Top-Center Glowing Fruit Badge + Centered Prizes/Your Bet + Bigger Middle 1st Frame 12dp Lower)
        if (winnerOpen) {
            val winnerSheetShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            val winKey = l7WheelSlices[winnerSlice.coerceIn(0, l7WheelSlices.lastIndex)]
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f))
                        .clickable { winnerOpen = false }
                )

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    // Black Rounded Winner Sheet
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 64.dp)
                            .heightIn(max = h * 0.88f)
                            .clip(winnerSheetShape)
                            .background(Color(0xFF060608))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {}
                            )
                            .verticalScroll(rememberScrollState())
                            .padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Top-right Countdown Timer
                        Box(Modifier.fillMaxWidth()) {
                            Text(
                                text = "${winnerTimer}s",
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 2.dp, end = 2.dp),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }

                        // Space reserved for overlapping glowing Winner Fruit badge
                        Spacer(Modifier.height(74.dp))

                        // Centered Prizes & Your Bet rows (Matching 1st Image)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = "Prizes",
                                    modifier = Modifier.width(82.dp),
                                    color = Color.White,
                                    fontSize = 15.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.End
                                )
                                Row(
                                    modifier = Modifier.width(88.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    AsyncImage(
                                        model = LUCKY777_COIN,
                                        contentDescription = null,
                                        modifier = Modifier.size(19.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                    Text(
                                        text = "$winAmount",
                                        color = Color.White,
                                        fontSize = 15.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = "Your Bet",
                                    modifier = Modifier.width(82.dp),
                                    color = Color.White,
                                    fontSize = 15.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.End
                                )
                                Row(
                                    modifier = Modifier.width(88.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    AsyncImage(
                                        model = LUCKY777_COIN,
                                        contentDescription = null,
                                        modifier = Modifier.size(19.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                    Text(
                                        text = "$betAmount",
                                        color = Color.White,
                                        fontSize = 15.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // Top Winner Heading Artwork
                        AsyncImage(
                            model = LUCKY777_WINNER_HEADING,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth(0.94f)
                                .height(34.dp),
                            contentScale = ContentScale.Fit
                        )

                        Spacer(Modifier.height(10.dp))

                        // Podium: 2nd | 1st (Bigger + 12dp lower) | 3rd
                        val frameItems = listOf(
                            Triple("2nd", LUCKY777_FRAME_2ND, 0),
                            Triple("1st", LUCKY777_FRAME_1ST, 1),
                            Triple("3rd", LUCKY777_FRAME_3RD, 2)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            frameItems.forEachIndexed { index, item ->
                                val isFirst = index == 1
                                val frameSize = if (isFirst) 98.dp else 74.dp
                                val avatarSize = if (isFirst) 68.dp else 50.dp
                                val showMe = isFirst && winAmount > 0L

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .offset(y = if (isFirst) 12.dp else 0.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier.size(frameSize),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        L7WinnerAvatarPlaceholder(
                                            modifier = Modifier
                                                .size(avatarSize)
                                                .offset(y = 2.dp)
                                        )
                                        AsyncImage(
                                            model = item.second,
                                            contentDescription = item.first,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Fit
                                        )
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = if (showMe) "You" else "-",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        AsyncImage(
                                            model = LUCKY777_COIN,
                                            contentDescription = null,
                                            modifier = Modifier.size(15.dp),
                                            contentScale = ContentScale.Fit
                                        )
                                        Text(
                                            text = if (showMe) l7Format(winAmount) else "-",
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Protruding Glowing Winner Fruit Badge centered at Top of Sheet (+12dp down for clean fit)
                    L7WinnerFruitBadge(
                        winKey = winKey,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = 12.dp)
                    )
                }
            }
        }
    }
}
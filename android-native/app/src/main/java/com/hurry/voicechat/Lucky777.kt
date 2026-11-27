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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
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
    val winAmount: Long = 0L
)

private val l7BetItems = listOf(
    L7Item("Peach", LUCKY777_PEACH, 2, LUCKY777_X2),
    L7Item("777", LUCKY777_SEVEN, 8, LUCKY777_X8),
    L7Item("Watermelon", LUCKY777_WATERMELON, 2, LUCKY777_X2)
)

// Exact 9-slice wheel order matching reference images:
// Slice 0 = 777 (Pink), then alternating Watermelon & Peach around the circle
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
    v >= 1_000_000L -> "${"%.1f".format(v / 1_000_000.0).removeSuffix(".0")}M"
    v >= 1000L -> "${"%.0f".format(v / 1000.0)}K"
    else -> v.toString()
}

private fun l7FormatTime(timeMillis: Long): String {
    val c = Calendar.getInstance().apply { timeInMillis = timeMillis }
    return String.format(
        "%02d-%02d %02d:%02d",
        c.get(Calendar.MONTH) + 1,
        c.get(Calendar.DAY_OF_MONTH),
        c.get(Calendar.HOUR_OF_DAY),
        c.get(Calendar.MINUTE)
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
            // 1. Rotating Blue/Pink 9-Slice Wheel + Radially Placed Fruits & 777
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

                    // Draw each of the 9 glossy wedges
                    l7WheelSlices.forEachIndexed { idx, key ->
                        val startAngle = idx * sliceAngle - sliceAngle / 2f
                        val isSeven = key == "777"
                        val wedgeBrush = if (isSeven) {
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFF8CF7),
                                    Color(0xFFEB2BE2),
                                    Color(0xFFBD15CC),
                                    Color(0xFF930CA8)
                                ),
                                center = center,
                                radius = radius
                            )
                        } else {
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF0076EB),
                                    Color(0xFF0A8CFF),
                                    Color(0xFF1FA6FF),
                                    Color(0xFF0055BF)
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

                    // Inner glossy sky-blue highlight ring near outer edge (matching 2nd image)
                    drawCircle(
                        color = Color(0xFF66CEFF).copy(alpha = 0.24f),
                        radius = radius * 0.88f,
                        center = center,
                        style = Stroke(width = radius * 0.14f)
                    )
                    // Outer dark shadow rim inside golden frame
                    drawCircle(
                        color = Color(0xFF002B73).copy(alpha = 0.55f),
                        radius = radius * 0.97f,
                        center = center,
                        style = Stroke(width = radius * 0.06f)
                    )

                    // Jigsaw-style divider lines between slices (same subtle notch as 2nd image)
                    l7WheelSlices.forEachIndexed { idx, _ ->
                        val boundaryDeg = idx * sliceAngle - sliceAngle / 2f
                        val rad = Math.toRadians(boundaryDeg.toDouble())
                        val perpRad = rad + Math.PI / 2.0
                        val cosA = cos(rad).toFloat()
                        val sinA = sin(rad).toFloat()
                        val cosP = cos(perpRad).toFloat()
                        val sinP = sin(perpRad).toFloat()

                        val r1 = radius * 0.22f
                        val rMid1 = radius * 0.72f
                        val rMid = radius * 0.785f
                        val rMid2 = radius * 0.85f
                        val r2 = radius * 0.99f
                        val bump = radius * 0.055f

                        val seamPath = Path().apply {
                            moveTo(center.x + cosA * r1, center.y + sinA * r1)
                            lineTo(center.x + cosA * rMid1, center.y + sinA * rMid1)
                            cubicTo(
                                center.x + cosA * (rMid1 + radius * 0.01f) + cosP * bump,
                                center.y + sinA * (rMid1 + radius * 0.01f) + sinP * bump,
                                center.x + cosA * (rMid2 - radius * 0.01f) + cosP * bump,
                                center.y + sinA * (rMid2 - radius * 0.01f) + sinP * bump,
                                center.x + cosA * rMid2,
                                center.y + sinA * rMid2
                            )
                            lineTo(center.x + cosA * r2, center.y + sinA * r2)
                        }

                        drawPath(
                            path = seamPath,
                            color = Color(0xFF063782).copy(alpha = 0.85f),
                            style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
                        )
                        drawPath(
                            path = seamPath,
                            color = Color(0xFF7BD4FF).copy(alpha = 0.28f),
                            style = Stroke(width = 0.7.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // Subtle inner puzzle dot highlight
                        drawCircle(
                            color = Color(0xFF0A4BA8).copy(alpha = 0.22f),
                            radius = radius * 0.02f,
                            center = Offset(
                                center.x + cosA * rMid + cosP * (bump * 0.45f),
                                center.y + sinA * rMid + sinP * (bump * 0.45f)
                            )
                        )
                    }
                }

                // 9 Fruit & 777 Icons arranged radially on each wedge
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

            // 2. Golden Winner Wedge Highlight at Top V-Slot during RESULT phase (like 2nd image)
            if (phase == L7Phase.RESULT) {
                Canvas(
                    modifier = Modifier
                        .offset(y = (-frameSize.value * 0.018f).dp)
                        .size(innerWheelSize)
                        .clip(CircleShape)
                ) {
                    val radius = size.minDimension / 2f
                    val center = Offset(size.width / 2f, size.height / 2f)
                    drawArc(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFF7A26).copy(alpha = 0.65f),
                                Color(0xFFFFCC33).copy(alpha = 0.58f),
                                Color(0xFFFFF4A8).copy(alpha = 0.72f)
                            ),
                            center = center,
                            radius = radius
                        ),
                        startAngle = 270f - sliceAngle / 2f,
                        sweepAngle = sliceAngle,
                        useCenter = true,
                        topLeft = Offset.Zero,
                        size = size
                    )
                }
            }

            // 3. Outer Wheel Frame Image (public/IMG_20261127_233303.png)
            AsyncImage(
                model = LUCKY777_WHEEL_FRAME,
                contentDescription = "Wheel Frame",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )

            // 4. Center Countdown Hub (Dark circle + Golden border + Red bold 20s countdown)
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

        // Dim background overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.34f))
        )

        // Top Title: Lucky 777
        L7TitleText(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp)
        )

        // Center Game Logo
        AsyncImage(
            model = LUCKY777_LOGO,
            contentDescription = "Lucky 777 Logo",
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-14).dp)
                .size(126.dp),
            contentScale = ContentScale.Fit
        )

        // Bottom Golden-Bordered Glossy Blue Striped Loading Bar
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
                                        Color(0xFF006EE6),
                                        Color(0xFF0047AB)
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
private fun L7GlassBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier
            .size(30.dp)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.36f),
                        Color(0xFF424652).copy(alpha = 0.38f),
                        Color(0xFF14151A).copy(alpha = 0.85f),
                        Color.White.copy(alpha = 0.18f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.85f),
                        Color.White.copy(alpha = 0.25f),
                        Color.White.copy(alpha = 0.45f)
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
                        Color.White.copy(alpha = 0.58f),
                        Color.White.copy(alpha = 0.10f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = size.height * 0.50f
                ),
                topLeft = Offset(size.width * 0.12f, size.height * 0.04f),
                size = Size(size.width * 0.76f, size.height * 0.42f)
            )
        }
        Canvas(Modifier.size(13.dp)) {
            val chevron = Path().apply {
                moveTo(8.5.dp.toPx(), 2.2.dp.toPx())
                lineTo(3.8.dp.toPx(), size.height / 2f)
                lineTo(8.5.dp.toPx(), size.height - 2.2.dp.toPx())
            }
            drawPath(
                chevron,
                Color.White,
                style = Stroke(2.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
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
                .height(sheetHeight)
                .padding(horizontal = 10.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
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
                        .fillMaxSize()
                        .padding(start = 22.dp, end = 18.dp, top = 14.dp, bottom = 30.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Rule",
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(top = 2.dp),
                            color = Color.White,
                            fontSize = 23.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif
                        )

                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .offset(x = 4.dp, y = (-2).dp)
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF132E63))
                                .border(1.4.dp, Color(0xFF5D7EB8), CircleShape)
                                .clickable(onClick = onDismiss),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(Modifier.size(13.dp)) {
                                val pad = 1.dp.toPx()
                                drawLine(
                                    color = Color.White,
                                    start = Offset(pad, pad),
                                    end = Offset(size.width - pad, size.height - pad),
                                    strokeWidth = 2.8.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                                drawLine(
                                    color = Color.White,
                                    start = Offset(size.width - pad, pad),
                                    end = Offset(pad, size.height - pad),
                                    strokeWidth = 2.8.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(22.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        rules.forEachIndexed { index, ruleText ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "${index + 1}.",
                                    color = Color(0xFFF3F6FC),
                                    fontSize = 15.5.sp,
                                    lineHeight = 22.sp,
                                    fontFamily = FontFamily.Serif
                                )
                                Text(
                                    text = ruleText,
                                    modifier = Modifier.weight(1f),
                                    color = Color(0xFFF3F6FC),
                                    fontSize = 15.5.sp,
                                    lineHeight = 22.sp,
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
                        .padding(end = 10.dp, bottom = 8.dp),
                    color = Color(0xFFDCE6FA),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Serif
                )
            }
        }
    }
}

@Composable
private fun L7FullHeightSheet(
    title: String,
    sheetHeight: Dp,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable { onDismiss() }
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(sheetHeight)
                .background(Color.Black)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                L7GlassBackButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterStart)
                )
                Text(
                    title,
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            content()
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

    LaunchedEffect(Unit) {
        runCatching {
            balance = dao.getUserData()?.balance ?: 82927L
            val arr = JSONArray(
                context.getSharedPreferences("lucky777_native", Context.MODE_PRIVATE)
                    .getString("history", "[]")
            )
            history = (0 until arr.length()).mapNotNull { i ->
                arr.optJSONObject(i)?.let {
                    L7Round(
                        round = it.optInt("round"),
                        winner = it.optString("winner"),
                        time = it.optLong("time"),
                        myBet = it.optLong("myBet", 0L),
                        winAmount = it.optLong("winAmount", 0L)
                    )
                }
            }.filter { it.time >= l7Reset() }.sortedBy { it.round }.takeLast(50)
        }
        for (step in 1..50) {
            loadingProgress = step / 50f
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
        history.takeLast(50).forEach {
            arr.put(
                JSONObject()
                    .put("round", it.round)
                    .put("winner", it.winner)
                    .put("time", it.time)
                    .put("myBet", it.myBet)
                    .put("winAmount", it.winAmount)
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
            betAmount = bets.values.sum()
            winAmount = (bets[winnerKey] ?: 0L) * l7MultiplierFor(winnerKey)
            history = (history + L7Round(round, winnerKey, System.currentTimeMillis(), betAmount, winAmount)).takeLast(50)
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
                // Top Bar (Same to same buttons & layout as previous game)
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

                // History Strip (Black/60 pill patti)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(26.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.Black.copy(alpha = 0.60f))
                        .border(0.8.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(50))
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (history.isEmpty()) {
                        Text(
                            "History",
                            color = Color.White.copy(alpha = 0.55f),
                            fontSize = 10.sp,
                            fontStyle = FontStyle.Italic
                        )
                    } else {
                        val recentStrip = history.takeLast(12)
                        recentStrip.forEachIndexed { idx, rec ->
                            val isNewest = idx == recentStrip.lastIndex
                            Box(
                                modifier = Modifier.size(21.dp),
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

                Spacer(Modifier.height(6.dp))

                // 3-Card Betting Panel (Single 3-card image + Fruits/777 + x2/x8 images)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .height(74.dp)
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
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        l7BetItems.forEach { item ->
                            val myBetVal = bets[item.key] ?: 0L
                            val isWinner = phase == L7Phase.RESULT && l7WheelSlices[winnerSlice] == item.key
                            val betText = "${l7Format(myBetVal)}/${l7Format(myBetVal)}"
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(8.dp))
                                    .then(
                                        if (isWinner) {
                                            Modifier.border(1.8.dp, l7Gold, RoundedCornerShape(8.dp))
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { placeBet(item.key) }
                                    .padding(horizontal = 4.dp, vertical = 3.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = betText,
                                    color = if (myBetVal > 0L) l7Gold else Color(0xFFFFE599),
                                    fontSize = 10.sp,
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
                                        modifier = Modifier.size(if (item.key == "777") 42.dp else 36.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                    Spacer(Modifier.width(3.dp))
                                    AsyncImage(
                                        model = item.multImage,
                                        contentDescription = "x${item.multiplier}",
                                        modifier = Modifier.height(26.dp),
                                        contentScale = ContentScale.Fit
                                    )
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
                    // Bottom-Left Decor Image (from previous game)
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

                    // Bottom Balance Bar
                    L7BottomBalanceBar(
                        coinResId = LUCKY777_COIN,
                        balance = balance,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 8.dp, bottom = 2.dp)
                    )

                    // Bottom Repeat + 5 Chips Row (no card behind chips)
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

        // History Sheet (same layout as previous app)
        if (historyOpen) {
            L7FullHeightSheet(
                title = "History",
                sheetHeight = h,
                onDismiss = { historyOpen = false }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF151515))
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Time", Modifier.weight(1.35f), color = Color(0xFFAFAFAF), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Text("Result", Modifier.weight(0.90f), color = Color(0xFFAFAFAF), fontSize = 13.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
                    Text("My Bet", Modifier.weight(0.95f), color = Color(0xFFAFAFAF), fontSize = 13.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
                    Text("Win Amount", Modifier.weight(1.10f), color = Color(0xFFAFAFAF), fontSize = 13.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.End)
                }
                val recentTwenty = history.takeLast(20).reversed()
                if (recentTwenty.isEmpty()) {
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text("No History Yet", color = Color.White.copy(alpha = 0.45f), fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
                        items(recentTwenty, key = { it.round }) { r ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    l7FormatTime(r.time),
                                    Modifier.weight(1.35f),
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Box(Modifier.weight(0.90f), contentAlignment = Alignment.Center) {
                                    AsyncImage(
                                        model = l7ImageFor(r.winner),
                                        contentDescription = r.winner,
                                        modifier = Modifier.size(30.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                                Text(
                                    if (r.myBet > 0L) l7Format(r.myBet) else "0",
                                    Modifier.weight(0.95f),
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    if (r.winAmount > 0L) l7Format(r.winAmount) else "0",
                                    Modifier.weight(1.10f),
                                    color = if (r.winAmount > 0L) l7Gold else Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.End
                                )
                            }
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                thickness = 0.7.dp,
                                color = Color.White.copy(alpha = 0.08f)
                            )
                        }
                    }
                }
            }
        }

        // Rules Card Popup
        if (rulesOpen) {
            L7RulesPopupCard(
                sheetHeight = h,
                onDismiss = { rulesOpen = false }
            )
        }

        // Winner Page (Exact same layout & podium frames as previous app)
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = h * 0.86f)
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
                    // Top Section: Countdown + Winning Icon + Prizes & Your Bet
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
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp, start = 4.dp, end = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.size(82.dp), contentAlignment = Alignment.Center) {
                                AsyncImage(
                                    model = l7ImageFor(winKey),
                                    contentDescription = winKey,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            }

                            Spacer(Modifier.width(24.dp))

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Prizes",
                                        modifier = Modifier.weight(1f),
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Row(
                                        modifier = Modifier.weight(0.85f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        AsyncImage(
                                            model = LUCKY777_COIN,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            contentScale = ContentScale.Fit
                                        )
                                        Text(
                                            text = "$winAmount",
                                            color = Color.White,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Your Bet",
                                        modifier = Modifier.weight(1f),
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Row(
                                        modifier = Modifier.weight(0.85f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        AsyncImage(
                                            model = LUCKY777_COIN,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            contentScale = ContentScale.Fit
                                        )
                                        Text(
                                            text = "$betAmount",
                                            color = Color.White,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Top Winner Heading Artwork
                    AsyncImage(
                        model = LUCKY777_WINNER_HEADING,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth(0.94f)
                            .height(36.dp),
                        contentScale = ContentScale.Fit
                    )

                    Spacer(Modifier.height(14.dp))

                    // Podium: 2nd | 1st | 3rd
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
                            val frameSize = if (isFirst) 84.dp else 76.dp
                            val avatarSize = if (isFirst) 58.dp else 52.dp
                            val showMe = isFirst && winAmount > 0L

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(bottom = if (isFirst) 12.dp else 0.dp),
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
            }
        }
    }
}

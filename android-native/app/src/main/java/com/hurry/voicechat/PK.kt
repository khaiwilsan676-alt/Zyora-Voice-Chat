package com.hurry.voicechat

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

private const val BASE_IMAGE_URL =
    "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/"

private const val BATTLE_ICON_URL = "${BASE_IMAGE_URL}public/IMG_20261028_182422.png"
private const val BLUE_CARD_URL = "${BASE_IMAGE_URL}public/IMG_20261126_011922.png"
private const val RED_CARD_URL = "${BASE_IMAGE_URL}public/IMG_20261126_011946.png"

private const val SELECT_BLUE_BG_URL =
    "${BASE_IMAGE_URL}public/file_00000000c6848211b4e0d9153986df4d.png"
private const val SELECT_RED_BG_URL =
    "${BASE_IMAGE_URL}public/file_00000000b5d882119d8f0569b18d4e79.png"
private const val RULES_BG_URL =
    "${BASE_IMAGE_URL}public/file_00000000d178821186730c503cab3ecf.png"

// Active PK Battle Assets
private const val BATTLE_LEFT_CARD_URL = "${BASE_IMAGE_URL}public/IMG_20261126_144448.png"
private const val BATTLE_RIGHT_CARD_URL = "${BASE_IMAGE_URL}public/IMG_20261126_144502.png"
private const val VOTE_BLUE_BTN_URL = "${BASE_IMAGE_URL}public/IMG_20261126_144743.png"
private const val VOTE_RED_BTN_URL = "${BASE_IMAGE_URL}public/IMG_20261126_144805.png"
private const val TICKET_ICON_URL = "${BASE_IMAGE_URL}public/IMG_20261126_145343.png"

// Exact Image Colors (Matte Dark Green Start + Image Chips)
private val PkSheetBg = Color.Black
private val PkGreenSelectedBg = Color(0xFF0D6D50)
private val PkGreenSelectedBorder = Color(0xFF20B57A)
private val PkUnselectedBg = Color(0xFF1F2926)
private val PkUnselectedBorder = Color(0xFF3E4B47)
private val PkStartMatteTop = Color(0xFF16463D)
private val PkStartMatteBottom = Color(0xFF123C34)
private val PkStartGoldBorder = Color(0xFFA99047)

private val pkImageCache = ConcurrentHashMap<String, ImageBitmap>()

private enum class PkSheetView {
    MAIN,
    SELECT_BLUE,
    SELECT_RED,
    RULES,
    BATTLE_ACTIVE
}

private fun parseDurationSeconds(label: String): Int {
    return when (label) {
        "30 s" -> 30
        "1 min" -> 60
        "5 min" -> 300
        "10 min" -> 600
        "15 min" -> 900
        else -> 30
    }
}

@Composable
fun PKBattlePage(onClose: () -> Unit) {
    var selectedMode by remember { mutableStateOf("Votes") }
    var selectedTime by remember { mutableStateOf("30 s") }
    var activeSheet by remember { mutableStateOf(PkSheetView.MAIN) }
    var selectedBlueIndex by remember { mutableStateOf<Int?>(null) }
    var selectedRedIndex by remember { mutableStateOf<Int?>(null) }

    // Active PK Battle State
    var blueScore by remember { mutableStateOf(0) }
    var redScore by remember { mutableStateOf(0) }
    var votedSide by remember { mutableStateOf<String?>(null) }
    var remainingSeconds by remember { mutableStateOf(30) }

    LaunchedEffect(activeSheet) {
        if (activeSheet == PkSheetView.BATTLE_ACTIVE) {
            while (remainingSeconds > 0 && activeSheet == PkSheetView.BATTLE_ACTIVE) {
                delay(1000L)
                remainingSeconds -= 1
            }
        }
    }

    BackHandler {
        if (activeSheet != PkSheetView.MAIN) {
            activeSheet = PkSheetView.MAIN
        } else {
            onClose()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.52f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (activeSheet != PkSheetView.MAIN) {
                    activeSheet = PkSheetView.MAIN
                } else {
                    onClose()
                }
            }
    ) {
        if (activeSheet == PkSheetView.BATTLE_ACTIVE) {
            PkActiveBattleOverlay(
                remainingSeconds = remainingSeconds,
                blueScore = blueScore,
                redScore = redScore,
                votedSide = votedSide,
                onBlueVote = {
                    if (votedSide != "BLUE") {
                        if (votedSide == "RED" && redScore > 0) redScore -= 1
                        votedSide = "BLUE"
                        blueScore += 1
                    }
                },
                onRedVote = {
                    if (votedSide != "RED") {
                        if (votedSide == "BLUE" && blueScore > 0) blueScore -= 1
                        votedSide = "RED"
                        redScore += 1
                    }
                },
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 8.dp)
            )
        } else {
            PkMainBattleSheet(
                selectedMode = selectedMode,
                onModeSelect = { selectedMode = it },
                selectedTime = selectedTime,
                onTimeSelect = { selectedTime = it },
                onBlueCardClick = { activeSheet = PkSheetView.SELECT_BLUE },
                onRedCardClick = { activeSheet = PkSheetView.SELECT_RED },
                onHelpClick = { activeSheet = PkSheetView.RULES },
                onStartClick = {
                    blueScore = 0
                    redScore = 0
                    votedSide = null
                    remainingSeconds = parseDurationSeconds(selectedTime)
                    activeSheet = PkSheetView.BATTLE_ACTIVE
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )

            if (activeSheet != PkSheetView.MAIN) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.42f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            activeSheet = PkSheetView.MAIN
                        }
                )
            }

            when (activeSheet) {
                PkSheetView.SELECT_BLUE -> {
                    PkSelectPeopleBottomSheet(
                        bgImageUrl = SELECT_BLUE_BG_URL,
                        fallbackBgColor = Color(0xFF0B2732),
                        plusTint = Color(0xFF3FA9D6),
                        activeBorderColor = Color(0xFF60A5FA),
                        selectedSlotIndex = selectedBlueIndex,
                        onSlotClick = { index ->
                            selectedBlueIndex = if (selectedBlueIndex == index) null else index
                        },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }

                PkSheetView.SELECT_RED -> {
                    PkSelectPeopleBottomSheet(
                        bgImageUrl = SELECT_RED_BG_URL,
                        fallbackBgColor = Color(0xFF2E0F18),
                        plusTint = Color(0xFFB9435F),
                        activeBorderColor = Color(0xFFF87171),
                        selectedSlotIndex = selectedRedIndex,
                        onSlotClick = { index ->
                            selectedRedIndex = if (selectedRedIndex == index) null else index
                        },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }

                PkSheetView.RULES -> {
                    PkRulesBottomSheet(
                        onBack = { activeSheet = PkSheetView.MAIN },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }

                else -> Unit
            }
        }
    }
}

@Composable
private fun PkMainBattleSheet(
    selectedMode: String,
    onModeSelect: (String) -> Unit,
    selectedTime: String,
    onTimeSelect: (String) -> Unit,
    onBlueCardClick: () -> Unit,
    onRedCardClick: () -> Unit,
    onHelpClick: () -> Unit,
    onStartClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.60f)
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(PkSheetBg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { }
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Top Header: Square Battle Icon + Exact White-to-Gold "Battle" Text + Exact (?) Icon
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PkRemoteImage(
                url = BATTLE_ICON_URL,
                contentDescription = "Battle",
                modifier = Modifier.size(34.dp),
                contentScale = ContentScale.Fit,
                fallbackColor = Color.Transparent
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "Battle",
                style = TextStyle(
                    brush = Brush.horizontalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.White,
                            0.35f to Color.White,
                            0.68f to Color(0xFFFCE28A),
                            1.0f to Color(0xFFF0BE3C)
                        )
                    ),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(Modifier.weight(1f))
            PkHelpQuestionIcon(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onHelpClick)
            )
        }

        Spacer(Modifier.height(12.dp))

        // Select People Section
        Text(
            text = "Select People",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PkSelectPersonCard(
                imageUrl = BLUE_CARD_URL,
                modifier = Modifier.weight(1f),
                onClick = onBlueCardClick
            )
            PkSelectPersonCard(
                imageUrl = RED_CARD_URL,
                modifier = Modifier.weight(1f),
                onClick = onRedCardClick
            )
        }

        Spacer(Modifier.height(12.dp))

        // Mode Section
        Text(
            text = "Mode",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PkOptionChip(
                label = "Votes",
                selected = selectedMode == "Votes",
                modifier = Modifier.weight(1f),
                onClick = { onModeSelect("Votes") }
            )
            PkOptionChip(
                label = "Coins",
                selected = selectedMode == "Coins",
                modifier = Modifier.weight(1f),
                onClick = { onModeSelect("Coins") }
            )
            Spacer(Modifier.weight(1f))
        }

        Spacer(Modifier.height(12.dp))

        // Time Section
        Text(
            text = "Time",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            listOf("30 s", "1 min", "5 min").forEach { time ->
                PkOptionChip(
                    label = time,
                    selected = selectedTime == time,
                    modifier = Modifier.weight(1f),
                    onClick = { onTimeSelect(time) }
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            listOf("10 min", "15 min").forEach { time ->
                PkOptionChip(
                    label = time,
                    selected = selectedTime == time,
                    modifier = Modifier.weight(1f),
                    onClick = { onTimeSelect(time) }
                )
            }
            Spacer(Modifier.weight(1f))
        }

        Spacer(Modifier.height(14.dp))

        // Matte Dark Green Start Button (No brightness/glow, Gold Border same as Image)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(PkStartMatteTop, PkStartMatteBottom)
                    )
                )
                .border(1.3.dp, PkStartGoldBorder, RoundedCornerShape(12.dp))
                .clickable(onClick = onStartClick),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Start",
                color = Color(0xFFC2D6D0),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun PkActiveBattleOverlay(
    remainingSeconds: Int,
    blueScore: Int,
    redScore: Int,
    votedSide: String?,
    onBlueVote: () -> Unit,
    onRedVote: () -> Unit,
    modifier: Modifier = Modifier
) {
    val targetBlueFraction = remember(blueScore, redScore) {
        when {
            blueScore == 0 && redScore == 0 -> 0.5f
            else -> (blueScore.toFloat() / (blueScore + redScore).toFloat()).coerceIn(0.18f, 0.82f)
        }
    }

    val animatedBlueFraction by animateFloatAsState(
        targetValue = targetBlueFraction,
        animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing),
        label = "pk_bar_ratio"
    )

    val mins = remainingSeconds / 60
    val secs = remainingSeconds % 60
    val formattedTime = String.format(Locale.US, "%02d:%02d", mins, secs)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { }
    ) {
        // Top Loading Patti (Blue vs Red Progress Bar + Corner Tickets + Scores)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
                .clip(RoundedCornerShape(4.dp))
                .border(
                    width = 1.5.dp,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFE7C76E),
                            Color(0xFFC59B3A),
                            Color(0xFFE7C76E)
                        )
                    ),
                    shape = RoundedCornerShape(4.dp)
                )
        ) {
            // Dynamic Blue / Red Bar + Center Glow Sparkles
            Canvas(modifier = Modifier.fillMaxSize()) {
                val splitX = size.width * animatedBlueFraction

                // Left Blue/Teal Bar
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF0A434B),
                            Color(0xFF0E7482),
                            Color(0xFF1AA7B8)
                        ),
                        startX = 0f,
                        endX = splitX.coerceAtLeast(1f)
                    ),
                    topLeft = Offset.Zero,
                    size = Size(splitX, size.height)
                )

                // Right Red/Crimson Bar
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFD11635),
                            Color(0xFF9C0B24),
                            Color(0xFF5D0716)
                        ),
                        startX = splitX,
                        endX = size.width.coerceAtLeast(splitX + 1f)
                    ),
                    topLeft = Offset(splitX, 0f),
                    size = Size((size.width - splitX).coerceAtLeast(0f), size.height)
                )

                // Soft Center Glow Beam
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.85f),
                            Color.Transparent
                        ),
                        startX = (splitX - 18f).coerceAtLeast(0f),
                        endX = (splitX + 18f).coerceAtMost(size.width)
                    ),
                    topLeft = Offset((splitX - 18f).coerceAtLeast(0f), 0f),
                    size = Size(36f, size.height)
                )

                // Crisp White Center Line
                drawLine(
                    color = Color.White,
                    start = Offset(splitX, 0f),
                    end = Offset(splitX, size.height),
                    strokeWidth = 3f
                )

                // Tiny Sparkle Dots on both sides of split
                val leftSparkles = listOf(
                    Offset(-10f, 0.22f), Offset(-22f, 0.48f), Offset(-14f, 0.74f),
                    Offset(-30f, 0.30f), Offset(-26f, 0.82f), Offset(-8f, 0.56f)
                )
                leftSparkles.forEach { (dx, ry) ->
                    val px = splitX + dx
                    if (px > 40f) {
                        drawCircle(
                            color = Color(0xFFD6FCFF).copy(alpha = 0.85f),
                            radius = 2.2f,
                            center = Offset(px, size.height * ry)
                        )
                    }
                }

                val rightSparkles = listOf(
                    Offset(10f, 0.25f), Offset(22f, 0.52f), Offset(14f, 0.76f),
                    Offset(28f, 0.34f), Offset(24f, 0.80f), Offset(8f, 0.50f)
                )
                rightSparkles.forEach { (dx, ry) ->
                    val px = splitX + dx
                    if (px < size.width - 40f) {
                        drawCircle(
                            color = Color(0xFFFFD6DF).copy(alpha = 0.85f),
                            radius = 2.2f,
                            center = Offset(px, size.height * ry)
                        )
                    }
                }
            }

            // Left Corner: Ticket Icon + Blue Score
            Row(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PkRemoteImage(
                    url = TICKET_ICON_URL,
                    contentDescription = "Left Ticket",
                    modifier = Modifier.size(20.dp),
                    contentScale = ContentScale.Fit,
                    fallbackColor = Color.Transparent
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = blueScore.toString(),
                    color = Color(0xFFFFF5DC),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Right Corner: Red Score + Ticket Icon
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = redScore.toString(),
                    color = Color(0xFFFFF5DC),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(5.dp))
                PkRemoteImage(
                    url = TICKET_ICON_URL,
                    contentDescription = "Right Ticket",
                    modifier = Modifier.size(20.dp),
                    contentScale = ContentScale.Fit,
                    fallbackColor = Color.Transparent
                )
            }
        }

        // Battle Cards Row + Center Hanging Timer Badge
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Left Blue Card
                PkActiveBattleCard(
                    cardImageUrl = BATTLE_LEFT_CARD_URL,
                    voteButtonImageUrl = VOTE_BLUE_BTN_URL,
                    userName = "Shubh",
                    isLeftAvatar = true,
                    isVoted = votedSide == "BLUE",
                    onVoteClick = onBlueVote,
                    modifier = Modifier.weight(1f)
                )

                // Right Red Card
                PkActiveBattleCard(
                    cardImageUrl = BATTLE_RIGHT_CARD_URL,
                    voteButtonImageUrl = VOTE_RED_BTN_URL,
                    userName = "Demoon \uD83C\uDF1D",
                    isLeftAvatar = false,
                    isVoted = votedSide == "RED",
                    onVoteClick = onRedVote,
                    modifier = Modifier.weight(1f)
                )
            }

            // Center Countdown Timer Pill overlapping top center
            PkTimerTrapezoidBadge(
                formattedTime = formattedTime,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

@Composable
private fun PkActiveBattleCard(
    cardImageUrl: String,
    voteButtonImageUrl: String,
    userName: String,
    isLeftAvatar: Boolean,
    isVoted: Boolean,
    onVoteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.height(252.dp)
    ) {
        PkRemoteImage(
            url = cardImageUrl,
            contentDescription = userName,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds,
            fallbackColor = if (isLeftAvatar) Color(0xFF0E3D43) else Color(0xFF4B0F1A)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 34.dp, bottom = 26.dp, start = 10.dp, end = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Fake User Circular Avatar
            PkFakeUserAvatar(
                isLeft = isLeftAvatar,
                modifier = Modifier.size(72.dp)
            )

            Spacer(Modifier.height(14.dp))

            // Fake User Name
            Text(
                text = userName,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                maxLines = 1
            )

            Spacer(Modifier.weight(1f))

            // Vote / Voted Button Image with Exact Centered Text
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.84f)
                    .height(42.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onVoteClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                PkRemoteImage(
                    url = voteButtonImageUrl,
                    contentDescription = if (isVoted) "Voted" else "Vote",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    fallbackColor = Color.Transparent
                )

                Text(
                    text = if (isVoted) "Voted" else "Vote",
                    color = Color(0xFFFFE7A3),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun PkTimerTrapezoidBadge(
    formattedTime: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(126.dp)
            .height(30.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val badgePath = Path().apply {
                moveTo(0f, 0f)
                lineTo(w, 0f)
                lineTo(w * 0.87f, h * 0.88f)
                quadraticTo(w * 0.84f, h, w * 0.76f, h)
                lineTo(w * 0.24f, h)
                quadraticTo(w * 0.16f, h, w * 0.13f, h * 0.88f)
                close()
            }

            drawPath(
                path = badgePath,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF281915), Color(0xFF3A1815))
                )
            )

            val borderPath = Path().apply {
                moveTo(w * 0.03f, 0f)
                lineTo(w * 0.13f, h * 0.88f)
                quadraticTo(w * 0.16f, h, w * 0.24f, h)
                lineTo(w * 0.76f, h)
                quadraticTo(w * 0.84f, h, w * 0.87f, h * 0.88f)
                lineTo(w * 0.97f, 0f)
            }

            drawPath(
                path = borderPath,
                color = Color(0xFFCDA54E),
                style = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(bottom = 2.dp)
        ) {
            PkStopwatchMiniIcon(modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(5.dp))
            Text(
                text = formattedTime,
                color = Color(0xFFFFF4D6),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun PkStopwatchMiniIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = w * 0.11f
        val tint = Color(0xFFFFEAB5)

        // Top button bar
        drawLine(
            color = tint,
            start = Offset(w * 0.36f, h * 0.10f),
            end = Offset(w * 0.64f, h * 0.10f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        // Small stem
        drawLine(
            color = tint,
            start = Offset(w * 0.50f, h * 0.10f),
            end = Offset(w * 0.50f, h * 0.24f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        // Watch circle
        drawCircle(
            color = tint,
            radius = w * 0.34f,
            center = Offset(w * 0.50f, h * 0.58f),
            style = Stroke(width = stroke)
        )
        // Clock hands
        drawLine(
            color = tint,
            start = Offset(w * 0.50f, h * 0.58f),
            end = Offset(w * 0.50f, h * 0.38f),
            strokeWidth = stroke * 0.9f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.50f, h * 0.58f),
            end = Offset(w * 0.66f, h * 0.58f),
            strokeWidth = stroke * 0.9f,
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun PkFakeUserAvatar(
    isLeft: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .clip(CircleShape)
            .border(1.2.dp, Color.White.copy(alpha = 0.65f), CircleShape)
    ) {
        val w = size.width
        val h = size.height

        if (isLeft) {
            // Left Fake Avatar (Cool Outdoor Portrait Style)
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFCDE5F0), Color(0xFF9AC5A8))
                )
            )
            // Suit shoulders
            drawOval(
                color = Color(0xFF1D2228),
                topLeft = Offset(w * 0.14f, h * 0.68f),
                size = Size(w * 0.72f, h * 0.48f)
            )
            // White shirt V
            val shirtPath = Path().apply {
                moveTo(w * 0.42f, h * 0.68f)
                lineTo(w * 0.58f, h * 0.68f)
                lineTo(w * 0.50f, h * 0.92f)
                close()
            }
            drawPath(shirtPath, color = Color.White)
            // Neck
            drawRect(
                color = Color(0xFFD8A888),
                topLeft = Offset(w * 0.43f, h * 0.56f),
                size = Size(w * 0.14f, h * 0.15f)
            )
            // Face
            drawOval(
                color = Color(0xFFE3B594),
                topLeft = Offset(w * 0.31f, h * 0.24f),
                size = Size(w * 0.38f, h * 0.40f)
            )
            // Turban / Hair crown
            drawOval(
                color = Color(0xFF16191D),
                topLeft = Offset(w * 0.26f, h * 0.10f),
                size = Size(w * 0.48f, h * 0.30f)
            )
            // Sunglasses
            drawRoundRect(
                color = Color(0xFF111317),
                topLeft = Offset(w * 0.34f, h * 0.39f),
                size = Size(w * 0.13f, h * 0.08f)
            )
            drawRoundRect(
                color = Color(0xFF111317),
                topLeft = Offset(w * 0.53f, h * 0.39f),
                size = Size(w * 0.13f, h * 0.08f)
            )
            // Beard
            drawOval(
                color = Color(0xFF1E1E22),
                topLeft = Offset(w * 0.34f, h * 0.50f),
                size = Size(w * 0.32f, h * 0.16f)
            )
        } else {
            // Right Fake Avatar (Minimal Studio Aesthetic Portrait)
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFD6CEC2), Color(0xFFBDB3A5))
                )
            )
            // Crisp white shirt shoulders
            drawOval(
                color = Color(0xFFF3F1EE),
                topLeft = Offset(w * 0.14f, h * 0.58f),
                size = Size(w * 0.76f, h * 0.54f)
            )
            // Subtle shadow fold on shirt
            drawLine(
                color = Color(0xFFD6CFC6),
                start = Offset(w * 0.46f, h * 0.66f),
                end = Offset(w * 0.40f, h * 0.96f),
                strokeWidth = 3f
            )
            // Neck
            drawRect(
                color = Color(0xFFE0B89E),
                topLeft = Offset(w * 0.41f, h * 0.48f),
                size = Size(w * 0.14f, h * 0.15f)
            )
            // Face
            drawOval(
                color = Color(0xFFEAC5AC),
                topLeft = Offset(w * 0.30f, h * 0.20f),
                size = Size(w * 0.36f, h * 0.36f)
            )
            // Stylish dark hair
            drawOval(
                color = Color(0xFF1D1A18),
                topLeft = Offset(w * 0.27f, h * 0.12f),
                size = Size(w * 0.42f, h * 0.24f)
            )
        }
    }
}

@Composable
private fun PkHelpQuestionIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val ringStroke = w * 0.08f

        val ringBrush = Brush.linearGradient(
            colorStops = arrayOf(
                0.0f to Color.White,
                0.35f to Color(0xFFFFF5D6),
                0.70f to Color(0xFFF2CC63),
                1.0f to Color(0xFFE0AE35)
            ),
            start = Offset(w * 0.15f, h * 0.10f),
            end = Offset(w * 0.88f, h * 0.90f)
        )

        drawCircle(
            brush = ringBrush,
            radius = (minOf(w, h) - ringStroke) / 2f,
            style = Stroke(width = ringStroke)
        )

        val qStroke = w * 0.082f
        val qBrush = Brush.verticalGradient(
            colorStops = arrayOf(
                0.0f to Color.White,
                0.55f to Color(0xFFFFF9E6),
                1.0f to Color(0xFFF5D47A)
            ),
            startY = h * 0.24f,
            endY = h * 0.78f
        )

        val qPath = Path().apply {
            moveTo(w * 0.37f, h * 0.38f)
            cubicTo(
                w * 0.37f, h * 0.27f,
                w * 0.63f, h * 0.27f,
                w * 0.63f, h * 0.38f
            )
            cubicTo(
                w * 0.63f, h * 0.46f,
                w * 0.50f, h * 0.47f,
                w * 0.50f, h * 0.57f
            )
        }

        drawPath(
            path = qPath,
            brush = qBrush,
            style = Stroke(
                width = qStroke,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        drawCircle(
            brush = qBrush,
            radius = w * 0.048f,
            center = Offset(w * 0.50f, h * 0.71f)
        )
    }
}

@Composable
private fun PkSelectPeopleBottomSheet(
    bgImageUrl: String,
    fallbackBgColor: Color,
    plusTint: Color,
    activeBorderColor: Color,
    selectedSlotIndex: Int?,
    onSlotClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetShape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(sheetShape)
            .background(fallbackBgColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { }
    ) {
        PkRemoteImage(
            url = bgImageUrl,
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.FillBounds,
            fallbackColor = fallbackBgColor
        )

        // Smaller +15 circles shifted slightly lower toward bottom
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 18.dp, end = 18.dp, top = 42.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            for (row in 0 until 3) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (col in 0 until 5) {
                        val index = row * 5 + col
                        val isSelected = selectedSlotIndex == index
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.32f))
                                .then(
                                    if (isSelected) {
                                        Modifier.border(1.4.dp, activeBorderColor, CircleShape)
                                    } else {
                                        Modifier
                                    }
                                )
                                .clickable { onSlotClick(index) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Person",
                                tint = if (isSelected) Color.White else plusTint,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PkRulesBottomSheet(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetShape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 270.dp)
            .clip(sheetShape)
            .background(Color(0xFF143127))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { }
    ) {
        PkRemoteImage(
            url = RULES_BG_URL,
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.FillBounds,
            fallbackColor = Color(0xFF143127)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = "Rules",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(26.dp))

            Text(
                text = "Number of votes",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = "PK according to number of votes received, 1 person 1 vote.",
                color = Color.White.copy(alpha = 0.88f),
                fontSize = 14.sp,
                lineHeight = 20.sp
            )

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Number of coins",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = "PK according to number of coins received.",
                color = Color.White.copy(alpha = 0.88f),
                fontSize = 14.sp,
                lineHeight = 20.sp
            )

            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun PkSelectPersonCard(
    imageUrl: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(98.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        PkRemoteImage(
            url = imageUrl,
            contentDescription = "Select Person",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
            fallbackColor = Color.Transparent
        )

        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.28f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun PkOptionChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) PkGreenSelectedBg else PkUnselectedBg)
            .border(
                width = if (selected) 1.2.dp else 1.dp,
                color = if (selected) PkGreenSelectedBorder else PkUnselectedBorder,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun PkRemoteImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    fallbackColor: Color = Color.Transparent
) {
    var bitmap by remember(url) { mutableStateOf(pkImageCache[url]) }

    LaunchedEffect(url) {
        if (bitmap == null) {
            val loaded = withContext(Dispatchers.IO) {
                try {
                    val conn = URL(url).openConnection() as HttpURLConnection
                    conn.connectTimeout = 8000
                    conn.readTimeout = 8000
                    conn.doInput = true
                    conn.connect()
                    conn.inputStream.use { input ->
                        BitmapFactory.decodeStream(input)?.asImageBitmap()
                    }
                } catch (_: Exception) {
                    null
                }
            }
            if (loaded != null) {
                pkImageCache[url] = loaded
                bitmap = loaded
            }
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
    } else {
        Box(modifier = modifier.background(fallbackColor))
    }
}

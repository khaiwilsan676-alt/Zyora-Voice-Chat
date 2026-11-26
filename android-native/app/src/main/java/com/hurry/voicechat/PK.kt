package com.hurry.voicechat

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.material.icons.filled.Check
import io.socket.client.Socket
import io.socket.emitter.Emitter
import org.json.JSONObject
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
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
import kotlin.math.PI
import kotlin.math.sin

private const val BASE_IMAGE_URL =
    "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/"

private const val BATTLE_ICON_URL = "${BASE_IMAGE_URL}public/IMG_20261126_214316.png"
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
private const val WIN_IMAGE_URL = "${BASE_IMAGE_URL}public/IMG_20261126_144416.png"
private const val LOSS_IMAGE_URL = "${BASE_IMAGE_URL}public/IMG_20261126_144432.png"

// Exact Image Colors
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

private enum class PkCardResult {
    NONE,
    WIN,
    LOSS
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

data class PkUser(val accountId: String, val name: String, val image: String)

@Composable
fun PKBattlePage(
    onClose: () -> Unit,
    roomId: String,
    accountId: String,
    userName: String,
    userImage: String,
    roomSocket: Socket?,
    roomSeats: List<NativeRoomSeat>,
    onSendGift: (PkUser) -> Unit
) {
    var selectedMode by remember { mutableStateOf("Votes") }
    var selectedTime by remember { mutableStateOf("30 s") }
    var activeSheet by remember { mutableStateOf(PkSheetView.MAIN) }
    var selectedBlueId by remember(roomId) { mutableStateOf("") }
    var selectedRedId by remember(roomId) { mutableStateOf("") }
    var blueUser by remember(roomId) { mutableStateOf<PkUser?>(null) }
    var redUser by remember(roomId) { mutableStateOf<PkUser?>(null) }
    var blueScore by remember(roomId) { mutableIntStateOf(0) }
    var redScore by remember(roomId) { mutableIntStateOf(0) }
    var votedSide by remember(roomId) { mutableStateOf<String?>(null) }
    var remainingSeconds by remember(roomId) { mutableIntStateOf(0) }
    var battleId by remember(roomId) { mutableStateOf("") }
    var battleMode by remember(roomId) { mutableStateOf("Votes") }
    var battleEndsAt by remember(roomId) { mutableLongStateOf(0L) }
    var showSelectPrompt by remember { mutableStateOf(false) }
    var battleFinished by remember(roomId) { mutableStateOf(false) }
    var selectPromptText by remember { mutableStateOf("Select the user") }

    fun parsePkUser(value: JSONObject?): PkUser? {
        if (value == null) return null
        val id = value.optString("accountId").trim()
        if (id.isBlank()) return null
        return PkUser(id, value.optString("name").ifBlank { "User" }, value.optString("image"))
    }

    DisposableEffect(roomSocket, roomId) {
        val started = Emitter.Listener { args ->
            val p = args.firstOrNull() as? JSONObject ?: return@Listener
            if (p.optString("roomId") != roomId) return@Listener
            val b = parsePkUser(p.optJSONObject("blueUser"))
            val r = parsePkUser(p.optJSONObject("redUser"))
            if (b != null && r != null) {
                battleId = p.optString("battleId")
                battleMode = p.optString("mode", "Votes")
                blueUser = b
                redUser = r
                blueScore = p.optInt("blueScore", 0)
                redScore = p.optInt("redScore", 0)
                battleEndsAt = p.optLong("endsAt", System.currentTimeMillis() + p.optInt("durationSeconds", 30) * 1000L)
                remainingSeconds = p.optInt("durationSeconds", 30)
                votedSide = null
                battleFinished = false
                activeSheet = PkSheetView.BATTLE_ACTIVE
            }
        }
        val score = Emitter.Listener { args ->
            val p = args.firstOrNull() as? JSONObject ?: return@Listener
            if (p.optString("roomId") != roomId || p.optString("battleId") != battleId) return@Listener
            blueScore = p.optInt("blueScore", blueScore)
            redScore = p.optInt("redScore", redScore)
            remainingSeconds = p.optInt("remainingSeconds", remainingSeconds)
        }
        val finished = Emitter.Listener { args ->
            val p = args.firstOrNull() as? JSONObject ?: return@Listener
            if (p.optString("roomId") != roomId || p.optString("battleId") != battleId) return@Listener
            blueScore = p.optInt("blueScore", blueScore)
            redScore = p.optInt("redScore", redScore)
            remainingSeconds = 0
            battleFinished = true
        }
        val failed = Emitter.Listener { args ->
            val p = args.firstOrNull() as? JSONObject ?: return@Listener
            if (p.optString("roomId") != roomId) return@Listener
            selectPromptText = when (p.optString("reason")) {
                "battle_running" -> "A PK is already running"
                "users_must_be_seated" -> "Select seated users"
                else -> "Select the user"
            }
            showSelectPrompt = true
        }
        val voteRejected = Emitter.Listener { args ->
            val p = args.firstOrNull() as? JSONObject ?: return@Listener
            if (p.optString("roomId") == roomId && p.optString("reason") == "already_voted") {
                selectPromptText = "1 person 1 vote"
                showSelectPrompt = true
            }
        }
        roomSocket?.on("pk_started", started)
        roomSocket?.on("pk_score_update", score)
        roomSocket?.on("pk_finished", finished)
        roomSocket?.on("pk_start_failed", failed)
        roomSocket?.on("pk_vote_rejected", voteRejected)
        roomSocket?.emit("pk_state_request", JSONObject().put("roomId", roomId))
        onDispose {
            roomSocket?.off("pk_started", started)
            roomSocket?.off("pk_score_update", score)
            roomSocket?.off("pk_finished", finished)
            roomSocket?.off("pk_start_failed", failed)
            roomSocket?.off("pk_vote_rejected", voteRejected)
        }
    }

    LaunchedEffect(battleId, battleEndsAt, activeSheet, battleFinished) {
        while (battleId.isNotBlank() && activeSheet == PkSheetView.BATTLE_ACTIVE && !battleFinished) {
            remainingSeconds = (((battleEndsAt - System.currentTimeMillis()).coerceAtLeast(0L)) / 1000L).toInt()
            if (remainingSeconds <= 0) break
            delay(250L)
        }
    }

    BackHandler {
        if (activeSheet != PkSheetView.MAIN) activeSheet = PkSheetView.MAIN else onClose()
    }

    val selectedBlue = roomSeats.firstOrNull { it.occupied && it.accountId == selectedBlueId }
    val selectedRed = roomSeats.firstOrNull { it.occupied && it.accountId == selectedRedId }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.52f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                if (activeSheet != PkSheetView.MAIN) activeSheet = PkSheetView.MAIN else onClose()
            }
    ) {
        if (activeSheet == PkSheetView.BATTLE_ACTIVE) {
            PkActiveBattleOverlay(
                remainingSeconds = remainingSeconds,
                blueScore = blueScore,
                redScore = redScore,
                votedSide = votedSide,
                blueUser = blueUser,
                redUser = redUser,
                scoreMode = battleMode,
                onBlueVote = {
                    if (battleMode == "Votes" && remainingSeconds > 0 && votedSide == null) {
                        roomSocket?.emit("pk_vote", JSONObject().put("roomId", roomId).put("side", "BLUE"))
                        votedSide = "BLUE"
                    }
                },
                onRedVote = {
                    if (battleMode == "Votes" && remainingSeconds > 0 && votedSide == null) {
                        roomSocket?.emit("pk_vote", JSONObject().put("roomId", roomId).put("side", "RED"))
                        votedSide = "RED"
                    }
                },
                onBlueSend = { blueUser?.let(onSendGift) },
                onRedSend = { redUser?.let(onSendGift) },
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            PkMainBattleSheet(
                selectedMode = selectedMode,
                onModeSelect = { selectedMode = it },
                selectedTime = selectedTime,
                onTimeSelect = { selectedTime = it },
                selectedBlueUser = selectedBlue?.let { PkUser(it.accountId, it.userName, it.userImage) },
                selectedRedUser = selectedRed?.let { PkUser(it.accountId, it.userName, it.userImage) },
                onBlueCardClick = { activeSheet = PkSheetView.SELECT_BLUE },
                onRedCardClick = { activeSheet = PkSheetView.SELECT_RED },
                onHelpClick = { activeSheet = PkSheetView.RULES },
                onStartClick = {
                    val blue = roomSeats.firstOrNull { it.occupied && it.accountId == selectedBlueId }
                    val red = roomSeats.firstOrNull { it.occupied && it.accountId == selectedRedId }
                    if (blue == null || red == null || blue.accountId == red.accountId) {
                        selectPromptText = "Select the user"
                        showSelectPrompt = true
                    } else {
                        roomSocket?.emit("pk_start", JSONObject()
                            .put("roomId", roomId).put("userId", accountId)
                            .put("userName", userName).put("userImage", userImage)
                            .put("blueUserId", blue.accountId).put("redUserId", red.accountId)
                            .put("mode", selectedMode).put("durationSeconds", parseDurationSeconds(selectedTime)))
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )

            if (activeSheet != PkSheetView.MAIN) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.42f))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { activeSheet = PkSheetView.MAIN })
            }
            when (activeSheet) {
                PkSheetView.SELECT_BLUE -> PkSelectPeopleBottomSheet(
                    bgImageUrl = SELECT_BLUE_BG_URL, fallbackBgColor = Color(0xFF0B2732),
                    plusTint = Color(0xFF3FA9D6), activeBorderColor = Color(0xFF60A5FA),
                    seats = roomSeats, selectedAccountId = selectedBlueId,
                    onSeatClick = { seat -> selectedBlueId = if (selectedBlueId == seat.accountId) "" else seat.accountId },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
                PkSheetView.SELECT_RED -> PkSelectPeopleBottomSheet(
                    bgImageUrl = SELECT_RED_BG_URL, fallbackBgColor = Color(0xFF2E0F18),
                    plusTint = Color(0xFFB9435F), activeBorderColor = Color(0xFFF87171),
                    seats = roomSeats, selectedAccountId = selectedRedId,
                    onSeatClick = { seat -> selectedRedId = if (selectedRedId == seat.accountId) "" else seat.accountId },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
                PkSheetView.RULES -> PkRulesBottomSheet(onBack = { activeSheet = PkSheetView.MAIN }, modifier = Modifier.align(Alignment.BottomCenter))
                else -> Unit
            }
        }

        if (showSelectPrompt) {
            Box(Modifier.fillMaxSize().background(Color.Transparent).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { showSelectPrompt = false }, contentAlignment = Alignment.Center) {
                Column(Modifier.clip(RoundedCornerShape(10.dp)).background(Color.Black.copy(alpha = 0.30f)).padding(horizontal = 22.dp, vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(selectPromptText, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
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
    selectedBlueUser: PkUser?,
    selectedRedUser: PkUser?,
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
                selectedUser = selectedBlueUser,
                modifier = Modifier.weight(1f),
                onClick = onBlueCardClick
            )
            PkSelectPersonCard(
                imageUrl = RED_CARD_URL,
                selectedUser = selectedRedUser,
                modifier = Modifier.weight(1f),
                onClick = onRedCardClick
            )
        }

        Spacer(Modifier.height(12.dp))

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
    blueUser: PkUser?,
    redUser: PkUser?,
    scoreMode: String,
    onBlueVote: () -> Unit,
    onRedVote: () -> Unit,
    onBlueSend: () -> Unit,
    onRedSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Draw hone par bhi dono LOSS ho jayenge (No Draw text)
    val isBattleFinished = remainingSeconds <= 0
    val leftResult = when {
        !isBattleFinished -> PkCardResult.NONE
        blueScore > redScore -> PkCardResult.WIN
        else -> PkCardResult.LOSS
    }
    val rightResult = when {
        !isBattleFinished -> PkCardResult.NONE
        redScore > blueScore -> PkCardResult.WIN
        else -> PkCardResult.LOSS
    }

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

    // Center white shining & sparkle animation
    val infiniteTransition = rememberInfiniteTransition(label = "pk_shine_transition")
    val shinePulse by infiniteTransition.animateFloat(
        initialValue = 0.78f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pk_shine_pulse"
    )
    val sparklePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pk_sparkle_phase"
    )

    val mins = remainingSeconds / 60
    val secs = remainingSeconds % 60
    val formattedTime = String.format(Locale.US, "%02d:%02d", mins, secs)

    Column(
        modifier = modifier
            .fillMaxWidth(0.90f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Loading Patti: 90% wide, Full Round (CircleShape), No side wing designs
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
                .clip(CircleShape)
                .border(
                    width = 1.6.dp,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFE9CA73),
                            Color(0xFFC29537),
                            Color(0xFFE9CA73)
                        )
                    ),
                    shape = CircleShape
                )
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val splitX = size.width * animatedBlueFraction
                val centerY = size.height / 2f

                // Left Blue/Teal Fill
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF0A3E45),
                            Color(0xFF0E6C7A),
                            Color(0xFF19A5B5)
                        ),
                        startX = 0f,
                        endX = splitX.coerceAtLeast(1f)
                    ),
                    topLeft = Offset.Zero,
                    size = Size(splitX, size.height)
                )

                // Right Red/Crimson Fill
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFD61737),
                            Color(0xFF9E0B24),
                            Color(0xFF5D0716)
                        ),
                        startX = splitX,
                        endX = size.width.coerceAtLeast(splitX + 1f)
                    ),
                    topLeft = Offset(splitX, 0f),
                    size = Size((size.width - splitX).coerceAtLeast(0f), size.height)
                )

                // Animated Outer White Shining Aura
                val glowHalfWidth = 26f * shinePulse
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFF9DF7FF).copy(alpha = 0.45f),
                            Color.White.copy(alpha = 0.96f),
                            Color(0xFFFFB2C2).copy(alpha = 0.45f),
                            Color.Transparent
                        ),
                        startX = (splitX - glowHalfWidth).coerceAtLeast(0f),
                        endX = (splitX + glowHalfWidth).coerceAtMost(size.width)
                    ),
                    topLeft = Offset((splitX - glowHalfWidth).coerceAtLeast(0f), 0f),
                    size = Size(glowHalfWidth * 2f, size.height)
                )

                // Animated Bright Radial White Flare in the middle
                val flareRadius = size.height * 0.72f * shinePulse
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White,
                            Color.White.copy(alpha = 0.85f),
                            Color(0xFFE0FFFF).copy(alpha = 0.30f),
                            Color.Transparent
                        ),
                        center = Offset(splitX, centerY),
                        radius = flareRadius
                    ),
                    radius = flareRadius,
                    center = Offset(splitX, centerY)
                )

                // Crisp Shining Center Vertical Spine
                drawLine(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.65f),
                            Color.White,
                            Color.White,
                            Color.White.copy(alpha = 0.65f)
                        )
                    ),
                    start = Offset(splitX, 0f),
                    end = Offset(splitX, size.height),
                    strokeWidth = 3.5f * (0.9f + 0.15f * shinePulse)
                )

                // Animated Twinkling Stars & Sparkle Dots (Left side)
                val leftParticles = listOf(
                    Triple(-10f, 0.24f, 0.0f),
                    Triple(-22f, 0.74f, 0.25f),
                    Triple(-34f, 0.32f, 0.50f),
                    Triple(-16f, 0.52f, 0.70f),
                    Triple(-28f, 0.82f, 0.15f),
                    Triple(-42f, 0.58f, 0.85f)
                )
                leftParticles.forEachIndexed { idx, (dx, ry, offsetPhase) ->
                    val phase = (sparklePhase + offsetPhase) % 1f
                    val twinkle = (0.30f + 0.70f * sin(phase * PI).toFloat()).coerceIn(0f, 1f)
                    val driftX = -phase * 8f
                    val px = splitX + dx + driftX
                    if (px > 48f) {
                        if (idx % 2 == 0) {
                            drawSparkleStar(
                                center = Offset(px, size.height * ry),
                                radius = 4.2f * (0.7f + 0.45f * twinkle),
                                color = Color(0xFFE8FEFF).copy(alpha = twinkle)
                            )
                        } else {
                            drawCircle(
                                color = Color(0xFFD6FCFF).copy(alpha = twinkle),
                                radius = 2.0f + 1.1f * twinkle,
                                center = Offset(px, size.height * ry)
                            )
                        }
                    }
                }

                // Animated Twinkling Stars & Sparkle Dots (Right side)
                val rightParticles = listOf(
                    Triple(10f, 0.26f, 0.12f),
                    Triple(22f, 0.76f, 0.38f),
                    Triple(32f, 0.34f, 0.64f),
                    Triple(15f, 0.54f, 0.82f),
                    Triple(26f, 0.80f, 0.28f),
                    Triple(38f, 0.50f, 0.92f)
                )
                rightParticles.forEachIndexed { idx, (dx, ry, offsetPhase) ->
                    val phase = (sparklePhase + offsetPhase) % 1f
                    val twinkle = (0.30f + 0.70f * sin(phase * PI).toFloat()).coerceIn(0f, 1f)
                    val driftX = phase * 8f
                    val px = splitX + dx + driftX
                    if (px < size.width - 48f) {
                        if (idx % 2 == 0) {
                            drawSparkleStar(
                                center = Offset(px, size.height * ry),
                                radius = 4.2f * (0.7f + 0.45f * twinkle),
                                color = Color(0xFFFFE8EE).copy(alpha = twinkle)
                            )
                        } else {
                            drawCircle(
                                color = Color(0xFFFFD6DF).copy(alpha = twinkle),
                                radius = 2.0f + 1.1f * twinkle,
                                center = Offset(px, size.height * ry)
                            )
                        }
                    }
                }
            }

            // Left Corner: Ticket Icon + Blue Score
            Row(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PkRemoteImage(
                    url = TICKET_ICON_URL,
                    contentDescription = "Left Ticket",
                    modifier = Modifier.size(19.dp),
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
                    .padding(end = 10.dp),
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
                    modifier = Modifier.size(19.dp),
                    contentScale = ContentScale.Fit,
                    fallbackColor = Color.Transparent
                )
            }
        }

        // Battle Cards Row (Exact Original Aspect Ratio - No Stretch / No Crop) + Center Timer
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Top
            ) {
                PkActiveBattleCard(
                    cardImageUrl = BATTLE_LEFT_CARD_URL,
                    voteButtonImageUrl = VOTE_BLUE_BTN_URL,
                    userName = blueUser?.name ?: "User",
                    avatarUrl = blueUser?.image.orEmpty(),
                    isLeftAvatar = true,
                    isVoted = votedSide == "BLUE",
                    scoreMode = scoreMode,
                    result = leftResult,
                    onVoteClick = onBlueVote,
                    onSendClick = onBlueSend,
                    modifier = Modifier.weight(1f)
                )

                PkActiveBattleCard(
                    cardImageUrl = BATTLE_RIGHT_CARD_URL,
                    voteButtonImageUrl = VOTE_RED_BTN_URL,
                    userName = redUser?.name ?: "User",
                    avatarUrl = redUser?.image.orEmpty(),
                    isLeftAvatar = false,
                    isVoted = votedSide == "RED",
                    scoreMode = scoreMode,
                    result = rightResult,
                    onVoteClick = onRedVote,
                    onSendClick = onRedSend,
                    modifier = Modifier.weight(1f)
                )
            }

            PkTimerTrapezoidBadge(
                formattedTime = formattedTime,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

private fun DrawScope.drawSparkleStar(
    center: Offset,
    radius: Float,
    color: Color
) {
    val inner = radius * 0.28f
    val starPath = Path().apply {
        moveTo(center.x, center.y - radius)
        lineTo(center.x + inner, center.y - inner)
        lineTo(center.x + radius, center.y)
        lineTo(center.x + inner, center.y + inner)
        lineTo(center.x, center.y + radius)
        lineTo(center.x - inner, center.y + inner)
        lineTo(center.x - radius, center.y)
        lineTo(center.x - inner, center.y - inner)
        close()
    }
    drawPath(path = starPath, color = color)
}

@Composable
private fun PkActiveBattleCard(
    cardImageUrl: String,
    voteButtonImageUrl: String,
    userName: String,
    avatarUrl: String,
    isLeftAvatar: Boolean,
    isVoted: Boolean,
    scoreMode: String,
    result: PkCardResult,
    onVoteClick: () -> Unit,
    onSendClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLoss = result == PkCardResult.LOSS
    val cardBitmap = rememberPkRemoteBitmap(cardImageUrl)
    val cardAspectRatio = remember(cardBitmap) {
        if (cardBitmap != null && cardBitmap.height > 0) {
            cardBitmap.width.toFloat() / cardBitmap.height.toFloat()
        } else {
            0.82f
        }
    }

    // Sirf thora sa dim card background & loss image par — avatar aur name bilkul dim nahi honge
    val cardBgDimFilter = if (isLoss) {
        ColorFilter.tint(Color.Black.copy(alpha = 0.24f), BlendMode.SrcAtop)
    } else {
        null
    }
    val lossImageDimFilter = ColorFilter.tint(Color.Black.copy(alpha = 0.16f), BlendMode.SrcAtop)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(cardAspectRatio)
    ) {
        val cardHeight = maxHeight
        val avatarSize = (cardHeight * 0.30f).coerceIn(46.dp, 68.dp)
        val topPad = (cardHeight * 0.14f).coerceIn(16.dp, 30.dp)
        val bottomPad = (cardHeight * 0.12f).coerceIn(12.dp, 24.dp)
        val voteBtnHeight = (cardHeight * 0.18f).coerceIn(30.dp, 40.dp)
        val resultImgHeight = (cardHeight * 0.24f).coerceIn(38.dp, 58.dp)

        if (cardBitmap != null) {
            Image(
                bitmap = cardBitmap,
                contentDescription = userName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
                colorFilter = cardBgDimFilter
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (isLeftAvatar) Color(0xFF0E3D43) else Color(0xFF4B0F1A))
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = topPad, bottom = bottomPad, start = 8.dp, end = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Real room participant avatar from the authoritative seat snapshot.
            PkRemoteImage(
                url = avatarUrl,
                contentDescription = userName,
                modifier = Modifier.size(avatarSize).clip(CircleShape).border(1.2.dp, Color.White.copy(alpha = 0.75f), CircleShape),
                contentScale = ContentScale.Crop,
                fallbackColor = Color(0xFF29343A)
            )

            Spacer(Modifier.height((cardHeight * 0.045f).coerceIn(6.dp, 12.dp)))

            // Fake User Name (Bilkul dim nahi hoga)
            Text(
                text = userName,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                maxLines = 1
            )

            when (result) {
                PkCardResult.WIN -> {
                    Spacer(Modifier.height((cardHeight * 0.035f).coerceIn(4.dp, 10.dp)))
                    PkRemoteImage(
                        url = WIN_IMAGE_URL,
                        contentDescription = "Win",
                        modifier = Modifier
                            .fillMaxWidth(0.74f)
                            .height(resultImgHeight),
                        contentScale = ContentScale.Fit,
                        fallbackColor = Color.Transparent
                    )
                }

                PkCardResult.LOSS -> {
                    Spacer(Modifier.height((cardHeight * 0.035f).coerceIn(4.dp, 10.dp)))
                    PkRemoteImage(
                        url = LOSS_IMAGE_URL,
                        contentDescription = "Loss",
                        modifier = Modifier
                            .fillMaxWidth(0.74f)
                            .height(resultImgHeight)
                            .alpha(0.84f),
                        contentScale = ContentScale.Fit,
                        fallbackColor = Color.Transparent,
                        colorFilter = lossImageDimFilter
                    )
                }

                PkCardResult.NONE -> {
                    Spacer(Modifier.weight(1f))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.84f)
                            .height(voteBtnHeight)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = if (scoreMode == "Coins") onSendClick else onVoteClick
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
                            text = if (scoreMode == "Coins") "Send" else if (isVoted) "Voted" else "Vote",
                            color = Color(0xFFFFE7A3),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
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
            .width(122.dp)
            .height(28.dp),
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
            PkStopwatchMiniIcon(modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(5.dp))
            Text(
                text = formattedTime,
                color = Color(0xFFFFF4D6),
                fontSize = 13.sp,
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

        drawLine(
            color = tint,
            start = Offset(w * 0.36f, h * 0.10f),
            end = Offset(w * 0.64f, h * 0.10f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.50f, h * 0.10f),
            end = Offset(w * 0.50f, h * 0.24f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawCircle(
            color = tint,
            radius = w * 0.34f,
            center = Offset(w * 0.50f, h * 0.58f),
            style = Stroke(width = stroke)
        )
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
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFCDE5F0), Color(0xFF9AC5A8))
                )
            )
            drawOval(
                color = Color(0xFF1D2228),
                topLeft = Offset(w * 0.14f, h * 0.68f),
                size = Size(w * 0.72f, h * 0.48f)
            )
            val shirtPath = Path().apply {
                moveTo(w * 0.42f, h * 0.68f)
                lineTo(w * 0.58f, h * 0.68f)
                lineTo(w * 0.50f, h * 0.92f)
                close()
            }
            drawPath(shirtPath, color = Color.White)
            drawRect(
                color = Color(0xFFD8A888),
                topLeft = Offset(w * 0.43f, h * 0.56f),
                size = Size(w * 0.14f, h * 0.15f)
            )
            drawOval(
                color = Color(0xFFE3B594),
                topLeft = Offset(w * 0.31f, h * 0.24f),
                size = Size(w * 0.38f, h * 0.40f)
            )
            drawOval(
                color = Color(0xFF16191D),
                topLeft = Offset(w * 0.26f, h * 0.10f),
                size = Size(w * 0.48f, h * 0.30f)
            )
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
            drawOval(
                color = Color(0xFF1E1E22),
                topLeft = Offset(w * 0.34f, h * 0.50f),
                size = Size(w * 0.32f, h * 0.16f)
            )
        } else {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFD6CEC2), Color(0xFFBDB3A5))
                )
            )
            drawOval(
                color = Color(0xFFF3F1EE),
                topLeft = Offset(w * 0.14f, h * 0.58f),
                size = Size(w * 0.76f, h * 0.54f)
            )
            drawLine(
                color = Color(0xFFD6CFC6),
                start = Offset(w * 0.46f, h * 0.66f),
                end = Offset(w * 0.40f, h * 0.96f),
                strokeWidth = 3f
            )
            drawRect(
                color = Color(0xFFE0B89E),
                topLeft = Offset(w * 0.41f, h * 0.48f),
                size = Size(w * 0.14f, h * 0.15f)
            )
            drawOval(
                color = Color(0xFFEAC5AC),
                topLeft = Offset(w * 0.30f, h * 0.20f),
                size = Size(w * 0.36f, h * 0.36f)
            )
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
    seats: List<NativeRoomSeat>,
    selectedAccountId: String,
    onSeatClick: (NativeRoomSeat) -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetShape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
    Box(
        modifier = modifier.fillMaxWidth().heightIn(min = 220.dp, max = 340.dp)
            .clip(sheetShape).background(fallbackBgColor)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
    ) {
        PkRemoteImage(bgImageUrl, null, Modifier.matchParentSize(), ContentScale.FillBounds, fallbackBgColor)
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding()
                .padding(start = 18.dp, end = 18.dp, top = 26.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            for (row in 0 until 3) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    for (col in 0 until 5) {
                        val seat = seats.getOrNull(row * 5 + col) ?: NativeRoomSeat(row * 5 + col + 1)
                        val selected = seat.occupied && seat.accountId.isNotBlank() && seat.accountId == selectedAccountId
                        Box(
                            Modifier.size(44.dp).clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.32f))
                                .then(if (selected) Modifier.border(1.6.dp, activeBorderColor, CircleShape) else Modifier)
                                .clickable(enabled = seat.occupied && seat.accountId.isNotBlank()) { onSeatClick(seat) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (seat.occupied && seat.userImage.isNotBlank()) {
                                PkRemoteImage(seat.userImage, seat.userName, Modifier.fillMaxSize().clip(CircleShape), ContentScale.Crop)
                                if (selected) {
                                    Box(Modifier.align(Alignment.BottomEnd).size(18.dp).clip(CircleShape)
                                        .background(Color.White).border(1.dp, activeBorderColor, CircleShape), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Check, contentDescription = "Selected", tint = Color(0xFF111827), modifier = Modifier.size(13.dp))
                                    }
                                }
                            } else {
                                Icon(Icons.Default.Add, contentDescription = "Empty seat", tint = plusTint, modifier = Modifier.size(20.dp))
                            }
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
    selectedUser: PkUser?,
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

        if (selectedUser == null) {
            Box(
                modifier = Modifier.size(42.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.28f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White, modifier = Modifier.size(24.dp))
            }
        } else {
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                PkRemoteImage(selectedUser.image, selectedUser.name, Modifier.size(44.dp).clip(CircleShape).border(1.5.dp, Color.White, CircleShape), ContentScale.Crop)
                Text(selectedUser.name, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
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
private fun rememberPkRemoteBitmap(url: String): ImageBitmap? {
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

    return bitmap
}

@Composable
private fun PkRemoteImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    fallbackColor: Color = Color.Transparent,
    colorFilter: ColorFilter? = null
) {
    val bitmap = rememberPkRemoteBitmap(url)

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
            colorFilter = colorFilter
        )
    } else {
        Box(modifier = modifier.background(fallbackColor))
    }
}

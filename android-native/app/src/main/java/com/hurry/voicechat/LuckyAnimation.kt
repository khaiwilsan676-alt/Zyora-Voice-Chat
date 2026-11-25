package com.hurry.voicechat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import org.json.JSONObject
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

private const val LUCKY_REWARD_BADGE_URL =
    "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/IMG_20261125_000330.png"

private const val LUCKY_COIN_ICON_URL =
    "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/file_00000000e56882119c217d508b6733dc.png"

// ============================================================
// LUCKY GIFT ANIMATION CONTROLS — keep all Lucky motion tuning here
// ============================================================
private const val LUCKY_SEAT_FLIGHT_DURATION_MS = 430
private const val LUCKY_SEAT_FLIGHT_ARC_DP = 42f
private const val LUCKY_SEAT_FLIGHT_IMAGE_SIZE_DP = 58
private const val LUCKY_COMBO_PULSE_DURATION_MS = 900

// Notification patti aur Combo button ka × count sync rakhne ke liye
private object LuckyComboSyncState {
    var displayMultiplier by mutableIntStateOf(1)
    var pendingComboClicks by mutableIntStateOf(0)
}

private fun luckyAnimationAssetUrl(path: String): String {
    if (path.startsWith("https://") || path.startsWith("http://") || path.startsWith("file:") || path.startsWith("content:")) return path
    return "https://zyora-voice-chat.vercel.app/" + path.trimStart('/')
}

// ============================================================
// LUCKY COINS RETURN LOGIC
// 1x: 10%, 2x: 8%, 4x: 8%, 5x: 4%, 10x: 3%, 15x: 2%, 20x: 2%, 50x: 0.8%, 100x: 0.8%
// ============================================================
data class LuckyCoinReturnResult(
    val multiplier: Int,
    val winAmount: Long,
)

fun calculateLuckyCoinReturn(giftCost: Long): LuckyCoinReturnResult {
    val roll = Random.nextDouble(0.0, 100.0)
    val multiplier = when {
        roll < 10.0 -> 1      // 10%
        roll < 18.0 -> 2      // 8%
        roll < 26.0 -> 4      // 8%
        roll < 30.0 -> 5      // 4%
        roll < 33.0 -> 10     // 3%
        roll < 35.0 -> 15     // 2%
        roll < 37.0 -> 20     // 2%
        roll < 37.8 -> 50     // 0.8%
        roll < 38.6 -> 100    // 0.8%
        else -> 0             // No return
    }
    return LuckyCoinReturnResult(
        multiplier = multiplier,
        winAmount = giftCost * multiplier,
    )
}


private const val LUCKY_NOTICE_VISIBLE_DURATION_MS = 5000L
private const val LUCKY_NOTICE_EXIT_DURATION_MS = 450L
const val LUCKY_SEAT_LANDING_EFFECT_DELAY_MS = 560L

/** Single owner for every Lucky animation state, seat target and combo timer. */
class LuckyAnimationController internal constructor(val roomId: String) {
    var origin: Offset? by mutableStateOf(null)
    val seatCenters = mutableStateMapOf<String, Offset>()
    var rootSize: IntSize by mutableStateOf(IntSize.Zero)
    val flights = mutableStateListOf<LuckyGiftFlight>()
    var notice: LuckyNotice? by mutableStateOf(null)
    var noticeGeneration by mutableIntStateOf(0)
    var showComboButton by mutableStateOf(false)
    var comboTimeLeft by mutableIntStateOf(5)
    var comboGeneration by mutableIntStateOf(0)
    var comboPending by mutableStateOf(false)
    var lastComboPayload: JSONObject? by mutableStateOf(null)

    fun recordOrigin(value: Offset) { origin = value }
    fun recordRootSize(value: IntSize) { rootSize = value }
    fun recordSeatCenter(id: String, center: Offset) { seatCenters[id] = center }
    fun finishFlight(id: String) {
        flights.removeAll { it.id == id }
    }
    private val launchedFlightIds = linkedSetOf<String>()

    fun launchToRecipients(
        id: String,
        image: String,
        recipientIds: Collection<String>,
        popupAtCenter: Boolean = false
    ) {
        if (id.isBlank() || image.isBlank() || id in launchedFlightIds) return
        val target = recipientIds.firstNotNullOfOrNull { seatCenters[it] } ?: return
        if (rootSize == IntSize.Zero) return
        // Start once on the sender's tap. The later server echo must not create
        // a second copy or restart the same flight.
        launchedFlightIds.add(id)
        if (launchedFlightIds.size > 200) launchedFlightIds.remove(launchedFlightIds.first())
        val center = Offset(rootSize.width / 2f, rootSize.height / 2f)
        flights.add(
            LuckyGiftFlight(
                id, image,
                if (popupAtCenter) center else origin ?: Offset(rootSize.width / 2f, rootSize.height * 0.88f),
                target,
                center,
                popupAtCenter
            )
        )
    }
    fun updateNotice(value: LuckyNotice?) { notice = value }
    fun renewNotice() { noticeGeneration++ }
    fun confirmCombo(payload: JSONObject) {
        lastComboPayload = JSONObject(payload.toString())
        comboPending = false
        showComboButton = true
        comboTimeLeft = 5
        comboGeneration++
    }
    fun beginCombo() { comboPending = true }
    fun failCombo() { comboPending = false }
    fun optimisticCombo() {
        notice = notice?.copy(multiplier = (notice?.multiplier ?: 1) + 1, isExiting = false)
        noticeGeneration++
    }
    fun tickCombo(value: Int) { comboTimeLeft = value.coerceIn(0, 5) }
    fun hideCombo() { showComboButton = false; comboTimeLeft = 0 }
}

@Composable
fun rememberLuckyAnimationController(roomId: String): LuckyAnimationController =
    remember(roomId) { LuckyAnimationController(roomId) }

@Composable
fun LuckyAnimationControllerEffects(controller: LuckyAnimationController) {
    LaunchedEffect(controller.roomId, controller.comboGeneration) {
        if (!controller.showComboButton) return@LaunchedEffect
        val generation = controller.comboGeneration
        for (second in 1..5) {
            delay(1000L)
            if (controller.comboGeneration != generation) return@LaunchedEffect
            controller.tickCombo(5 - second)
        }
        if (controller.comboGeneration == generation) controller.hideCombo()
    }
    LaunchedEffect(controller.roomId, controller.noticeGeneration) {
        val generation = controller.noticeGeneration
        val id = controller.notice?.id ?: return@LaunchedEffect
        delay(LUCKY_NOTICE_VISIBLE_DURATION_MS)
        if (controller.noticeGeneration != generation || controller.notice?.id != id) return@LaunchedEffect
        controller.updateNotice(controller.notice?.copy(isExiting = true))
        val exitGeneration = controller.noticeGeneration
        delay(LUCKY_NOTICE_EXIT_DURATION_MS)
        if (controller.noticeGeneration == exitGeneration && controller.notice?.id == id) controller.updateNotice(null)
    }
}


// ============================================================
// LUCKY GIFT NOTIFICATION SLIDER + FLY ANIMATION + REWARD BADGE
// ============================================================
data class LuckyNotice(
    val id: String,
    val senderId: String,
    val senderName: String,
    val recipientName: String,
    val senderImage: String,
    val giftImage: String,
    val multiplier: Int,
    val totalWinAmount: Long,
    val maxWinTimes: Int,
    val recipientMode: RecipientMode,
    val isExiting: Boolean = false,
)

private data class LuckyRewardPopupState(
    val id: String,
    val winAmount: Long,
)

@Composable
fun LuckyAnimationNotificationSlider(
    notice: LuckyNotice?,
    floatingWins: List<Pair<String, Long>> = emptyList(),
    targetAvatarOffset: Offset? = null,
) {
    // RoomPage owns the single seat-flight animation. This composable owns
    // only the notification slider and reward badge, avoiding duplicate flights.
    var lastAnimatedNoticeId by remember { mutableStateOf<String?>(null) }
    var lastAnimatedMultiplier by remember { mutableIntStateOf(0) }

    // Reward badge state (3 sec ke liye, naya reward aate hi purana hat ke naya upar se aayega)
    var activeReward by remember { mutableStateOf<LuckyRewardPopupState?>(null) }

    LaunchedEffect(notice?.id, notice?.multiplier) {
        if (notice == null) {
            lastAnimatedNoticeId = null
            lastAnimatedMultiplier = 0
            LuckyComboSyncState.pendingComboClicks = 0
            return@LaunchedEffect
        }

        if (notice.id != lastAnimatedNoticeId || notice.multiplier != lastAnimatedMultiplier) {
            lastAnimatedNoticeId = notice.id
            lastAnimatedMultiplier = notice.multiplier

            // Combo button aur Notification patti dono ka × bilkul connected rahega
            val syncedMultiplier = if (LuckyComboSyncState.pendingComboClicks > 0) {
                LuckyComboSyncState.pendingComboClicks =
                    (LuckyComboSyncState.pendingComboClicks - 1).coerceAtLeast(0)
                if (notice.multiplier > LuckyComboSyncState.displayMultiplier) {
                    notice.multiplier
                } else {
                    LuckyComboSyncState.displayMultiplier + 1
                }
            } else {
                notice.multiplier.coerceAtLeast(1)
            }
            LuckyComboSyncState.displayMultiplier = syncedMultiplier

            if (notice.totalWinAmount > 0L) {
                activeReward = LuckyRewardPopupState(
                    id = "${notice.id}_${syncedMultiplier}",
                    winAmount = notice.totalWinAmount,
                )
            }
        }
    }

    // 3 second auto-hide timer for reward badge (naya reward aate hi reset ho jayega)
    LaunchedEffect(activeReward?.id) {
        val currentId = activeReward?.id ?: return@LaunchedEffect
        delay(3000L)
        if (activeReward?.id == currentId) {
            activeReward = null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Notification patti (thori niche + thori wide + trailing fade)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 12.dp)
                .offset(y = 48.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            AnimatedVisibility(
                visible = notice != null && !notice.isExiting,
                enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(),
                modifier = Modifier.wrapContentWidth()
            ) {
                if (notice == null) return@AnimatedVisibility
                val isBroadcast = notice.recipientMode == RecipientMode.MIC ||
                        notice.recipientMode == RecipientMode.ROOM
                val topText = when (notice.recipientMode) {
                    RecipientMode.MIC -> "Sent to All Mic"
                    RecipientMode.ROOM -> "Sent to All in room"
                    else -> notice.senderName
                }
                val bottomText = if (isBroadcast) notice.senderName else "To ${notice.recipientName}"
                val shownMultiplier = LuckyComboSyncState.displayMultiplier.coerceAtLeast(1)

                Box(contentAlignment = Alignment.CenterStart) {
                    Row(
                        Modifier
                            .widthIn(min = 244.dp)
                            .clip(RoundedCornerShape(50.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFFFFC107),
                                        Color(0xFFFFC107),
                                        Color(0xD9FFC107),
                                        Color(0x73FFC107),
                                        Color(0x26FFC107),
                                        Color(0x00FFC107),
                                    ),
                                    startX = 0f,
                                    endX = Float.POSITIVE_INFINITY,
                                )
                            )
                            .padding(start = 6.dp, end = 82.dp, top = 2.dp, bottom = 2.dp)
                            .height(38.dp)
                            .wrapContentWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AsyncImage(
                            model = notice.senderImage,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape),
                        )
                        Spacer(Modifier.width(8.dp))

                        Column(
                            modifier = Modifier.widthIn(min = 96.dp, max = 140.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)
                        ) {
                            Text(
                                text = topText,
                                color = Color.White,
                                fontSize = 10.sp,
                                lineHeight = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = TextStyle(
                                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                                ),
                            )
                            Text(
                                text = bottomText,
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 9.sp,
                                lineHeight = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = TextStyle(
                                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                                ),
                            )
                        }
                        Spacer(Modifier.width(8.dp))

                        AsyncImage(
                            model = luckyAnimationAssetUrl(notice.giftImage),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(26.dp),
                        )
                        Text(
                            text = "×$shownMultiplier",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            fontStyle = FontStyle.Italic,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }

                    // Name ke right-side top corner par (20.dp niche + bigger winner image)
                    activeReward?.let { reward ->
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .offset(x = 142.dp, y = 26.dp)
                                .size(0.dp)
                                .wrapContentSize(align = Alignment.BottomCenter, unbounded = true)
                                .zIndex(2f),
                        ) {
                            key(reward.id) {
                                LuckyRewardTopBadge(winAmount = reward.winAmount)
                            }
                        }
                    }
                }
            }
        }

    }
}

@Composable
private fun LuckyRewardTopBadge(
    winAmount: Long,
    modifier: Modifier = Modifier,
) {
    val dropY = remember { Animatable(-34f) }
    val popScale = remember { Animatable(0.55f) }
    val alphaAnim = remember { Animatable(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "sunRays")
    val rayRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "rayRotation",
    )

    LaunchedEffect(Unit) {
        launch { alphaAnim.animateTo(1f, tween(110)) }
        launch {
            dropY.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
            )
        }
        launch {
            popScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
            )
        }
    }

    // Bigger winning image (96.dp) — Coins icon & value untouched at bottom
    Box(
        modifier = modifier
            .size(96.dp)
            .graphicsLayer {
                translationY = dropY.value
                scaleX = popScale.value
                scaleY = popScale.value
                alpha = alphaAnim.value
            },
        contentAlignment = Alignment.Center,
    ) {
        // Rotating Sun Rays Shining behind image (layout bounds inflate kiye bina)
        Canvas(modifier = Modifier.requiredSize(122.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerRadius = size.minDimension / 2f
            val rayCount = 12
            val rayAngleStep = 360f / rayCount
            val halfRaySpread = 9f

            // Soft golden glow center
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xCCFFE066),
                        Color(0x55FFD700),
                        Color.Transparent,
                    ),
                    center = center,
                    radius = outerRadius,
                ),
                radius = outerRadius,
                center = center,
            )

            // Rotating sun rays
            rotate(degrees = rayRotation, pivot = center) {
                for (i in 0 until rayCount) {
                    val angle1 = ((i * rayAngleStep) - halfRaySpread) * PI / 180.0
                    val angle2 = ((i * rayAngleStep) + halfRaySpread) * PI / 180.0
                    val path = Path().apply {
                        moveTo(center.x, center.y)
                        lineTo(
                            center.x + (outerRadius * cos(angle1)).toFloat(),
                            center.y + (outerRadius * sin(angle1)).toFloat(),
                        )
                        lineTo(
                            center.x + (outerRadius * cos(angle2)).toFloat(),
                            center.y + (outerRadius * sin(angle2)).toFloat(),
                        )
                        close()
                    }
                    drawPath(
                        path = path,
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xBBFFF176),
                                Color(0x00FFD700),
                            ),
                            center = center,
                            radius = outerRadius,
                        ),
                    )
                }
            }
        }

        // Main Reward Image (increased size: 96.dp)
        AsyncImage(
            model = LUCKY_REWARD_BADGE_URL,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )

        // Coin Icon + Yellow Coins Amount exact image ke bottom par overlap hokar (untouched)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-6).dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            AsyncImage(
                model = LUCKY_COIN_ICON_URL,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(2.dp))
            Text(
                text = winAmount.toString(),
                color = Color(0xFFFFEB3B),
                fontSize = 13.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color(0xCC000000),
                        offset = Offset(0f, 1.5f),
                        blurRadius = 4f,
                    ),
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                ),
            )
        }
    }
}

// ============================================================
// LUCKY COMBO BUTTON (Same to same image style + connected ×)
// ============================================================
@Composable
fun LuckyAnimationComboButton(
    visible: Boolean,
    progress: Float, // Smooth timer progress (0f to 1f)
    onClick: () -> Unit,
    comboCount: Int? = null,
) {
    LaunchedEffect(visible) {
        if (!visible) {
            LuckyComboSyncState.displayMultiplier = 1
            LuckyComboSyncState.pendingComboClicks = 0
        }
    }

    if (!visible) return

    val shownCount = (comboCount ?: LuckyComboSyncState.displayMultiplier).coerceAtLeast(1)
    val comboInteraction = remember { MutableInteractionSource() }
    val isComboPressed by comboInteraction.collectIsPressedAsState()
    // Respond on the first rendered press frame; no delayed press tween.
    val comboPressScale = if (isComboPressed) 0.94f else 1f

    val textShadow = remember {
        Shadow(
            color = Color(0x66B83200),
            offset = Offset(1f, 2f),
            blurRadius = 4f,
        )
    }

    Box(
        Modifier
            .fillMaxSize()
            .padding(end = 24.dp, bottom = 50.dp),
        contentAlignment = Alignment.BottomEnd,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(84.dp)
                .graphicsLayer {
                    scaleX = comboPressScale
                    scaleY = comboPressScale
                }
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0x888C5432),
                            Color(0x664A2F1F),
                        )
                    )
                )
                .clickable(
                    interactionSource = comboInteraction,
                    indication = null,
                ) {
                    LuckyComboSyncState.pendingComboClicks += 1
                    onClick()
                },
        ) {
            // Inner vibrant orange-red to golden-orange circle
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFFF4B24),
                                Color(0xFFFF681E),
                                Color(0xFFFF9F34),
                            )
                        )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "x$shownCount",
                        color = Color.White,
                        fontSize = 18.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontStyle = FontStyle.Italic,
                        style = TextStyle(
                            shadow = textShadow,
                            platformStyle = PlatformTextStyle(includeFontPadding = false),
                        ),
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Combo",
                        color = Color.White,
                        fontSize = 17.sp,
                        lineHeight = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontStyle = FontStyle.Italic,
                        style = TextStyle(
                            shadow = textShadow,
                            platformStyle = PlatformTextStyle(includeFontPadding = false),
                        ),
                    )
                }
            }

            // Rounded white circular progress arc around the orange circle
            Canvas(modifier = Modifier.fillMaxSize().padding(4.5.dp)) {
                val strokePx = 3.5.dp.toPx()
                val arcSize = Size(size.width - strokePx, size.height - strokePx)
                val topLeft = Offset(strokePx / 2f, strokePx / 2f)

                drawArc(
                    color = Color.White.copy(alpha = 0.16f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round),
                )

                val clampedProgress = progress.coerceIn(0f, 1f)
                if (clampedProgress > 0f) {
                    drawArc(
                        color = Color.White,
                        startAngle = -90f,
                        sweepAngle = 360f * clampedProgress,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round),
                    )
                }
            }
        }
    }
}

// ============================================================
// LUCKY GIFT SEAT FLIGHT — Bottom -> Center (1.5s) -> Hold (0.8s) -> Seat Avatar (same size)
// ============================================================
data class LuckyGiftFlight(
    val id: String,
    val image: String,
    val start: Offset,
    val target: Offset,
    val rootCenter: Offset,
    val popupAtCenter: Boolean = false
)

@Composable
fun LuckyGiftFlightOverlay(flight: LuckyGiftFlight, onFinished: () -> Unit) {
    val density = LocalDensity.current
    val imageSizeDp = LUCKY_SEAT_FLIGHT_IMAGE_SIZE_DP.dp
    val halfImagePx = with(density) { imageSizeDp.toPx() } / 2f

    val posX = remember(flight.id) { Animatable(flight.start.x) }
    val posY = remember(flight.id) { Animatable(flight.start.y) }
    val scaleAnim = remember(flight.id) { Animatable(0.35f) }
    val itemAlpha = remember(flight.id) { Animatable(0f) }

    LaunchedEffect(flight.id) {
        // Keep the original bottom -> center travel for every Lucky flight.
        val fadeInJob = launch { itemAlpha.snapTo(1f) }
        val moveXToCenterJob = launch {
            posX.animateTo(flight.rootCenter.x, animationSpec = tween(1500, easing = FastOutSlowInEasing))
        }
        val moveYToCenterJob = launch {
            posY.animateTo(flight.rootCenter.y, animationSpec = tween(1500, easing = FastOutSlowInEasing))
        }
        val growToOriginalJob = launch {
            scaleAnim.animateTo(1.0f, animationSpec = tween(1500, easing = FastOutSlowInEasing))
        }
        fadeInJob.join()
        moveXToCenterJob.join()
        moveYToCenterJob.join()
        growToOriginalJob.join()

        // Add only a small popup pulse after the image reaches center.
        if (flight.popupAtCenter) {
            scaleAnim.animateTo(1.12f, animationSpec = tween(180, easing = FastOutSlowInEasing))
            scaleAnim.animateTo(1.0f, animationSpec = tween(120, easing = FastOutSlowInEasing))
        }

        // Keep the requested 0.8s center hold, then fly to the seat.
        delay(800L)

        val flyXJob = launch {
            posX.animateTo(flight.target.x, animationSpec = tween(LUCKY_SEAT_FLIGHT_DURATION_MS, easing = FastOutSlowInEasing))
        }
        val flyYJob = launch {
            posY.animateTo(flight.target.y, animationSpec = tween(LUCKY_SEAT_FLIGHT_DURATION_MS, easing = FastOutSlowInEasing))
        }
        val fadeOutJob = launch {
            delay((LUCKY_SEAT_FLIGHT_DURATION_MS - 100).coerceAtLeast(0).toLong())
            itemAlpha.animateTo(0f, animationSpec = tween(100))
        }
        flyXJob.join()
        flyYJob.join()
        fadeOutJob.join()

        onFinished()
    }

    Box(Modifier.fillMaxSize().zIndex(260f), contentAlignment = Alignment.TopStart) {
        AsyncImage(
            model = luckyAnimationAssetUrl(flight.image),
            contentDescription = "Lucky gift animation",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(imageSizeDp)
                .graphicsLayer {
                    translationX = posX.value - halfImagePx
                    translationY = posY.value - halfImagePx
                    scaleX = scaleAnim.value
                    scaleY = scaleAnim.value
                    alpha = itemAlpha.value
                }
        )
    }
}
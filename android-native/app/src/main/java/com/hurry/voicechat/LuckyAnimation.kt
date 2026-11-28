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
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private const val LUCKY_REWARD_BADGE_URL =
    "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/IMG_20261125_000330.png"

private const val LUCKY_COIN_ICON_URL =
    "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/file_00000000e56882119c217d508b6733dc.png"

// ============================================================
// LUCKY GIFT ANIMATION CONTROLS — keep all Lucky motion tuning here
// ============================================================
private const val LUCKY_SEAT_POPUP_DURATION_MS = 85
private const val LUCKY_SEAT_FLIGHT_DURATION_MS = 210
private const val LUCKY_SEAT_FLIGHT_ARC_DP = 42f
private const val LUCKY_SEAT_FLIGHT_IMAGE_SIZE_DP = 58
private const val LUCKY_COMBO_PULSE_DURATION_MS = 900
private const val LUCKY_MAX_RECIPIENT_FLIGHTS = 15
private const val LUCKY_MAX_ACTIVE_FLIGHTS = 45

private fun luckyNoticeGiftKey(notice: LuckyNotice): String =
    "${notice.senderId}|${notice.giftImage.trim()}|${notice.recipientMode.name}"

private fun luckyAnimationAssetUrl(path: String): String {
    val clean = path.trim()
    if (clean.isEmpty()) return ""
    if (
        clean.startsWith("https://") ||
        clean.startsWith("http://") ||
        clean.startsWith("file:") ||
        clean.startsWith("content:")
    ) {
        return clean
    }
    return "https://zyora-voice-chat.vercel.app/" + clean.trimStart('/')
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
private const val LUCKY_NOTICE_EXIT_DURATION_MS = 350L
const val LUCKY_SEAT_LANDING_EFFECT_DELAY_MS = 320L

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
    var displayMultiplier by mutableIntStateOf(1)
    var pendingComboClicks by mutableIntStateOf(0)
    var activeGiftKey by mutableStateOf("")

    fun resetComboSyncState() {
        displayMultiplier = 1
        pendingComboClicks = 0
        activeGiftKey = ""
    }
    fun registerComboTap() { pendingComboClicks++ }
    fun clearPendingComboTaps() { pendingComboClicks = 0 }

    fun recordOrigin(value: Offset) { origin = value }
    fun recordRootSize(value: IntSize) {
        rootSize = value
        if (value != IntSize.Zero && pendingFlightRequests.isNotEmpty()) {
            val waiting = pendingFlightRequests.values.toList()
            pendingFlightRequests.clear()
            waiting.forEach { request ->
                launchToRecipients(request.id, request.image, request.recipientIds, request.popupAtCenter)
            }
        }
    }
    fun recordSeatCenter(id: String, center: Offset) { seatCenters[id] = center }

    fun finishFlight(id: String) {
        flights.removeAll { it.id == id }
    }

    private data class PendingFlightRequest(
        val id: String,
        val image: String,
        val recipientIds: List<String>,
        val popupAtCenter: Boolean,
    )

    private val launchedFlightIds = linkedSetOf<String>()
    private val pendingFlightRequests = linkedMapOf<String, PendingFlightRequest>()

    fun launchToRecipients(
        id: String,
        image: String,
        recipientIds: Collection<String>,
        popupAtCenter: Boolean = true
    ) {
        val cleanImage = image.trim()
        if (id.isBlank() || cleanImage.isBlank() || id in launchedFlightIds || id in pendingFlightRequests) return
        if (rootSize == IntSize.Zero) {
            // A fast send can arrive before Compose reports the room bounds.
            // Keep the event briefly instead of silently dropping its gift animation.
            pendingFlightRequests[id] = PendingFlightRequest(
                id = id,
                image = cleanImage,
                recipientIds = recipientIds.toList(),
                popupAtCenter = popupAtCenter,
            )
            while (pendingFlightRequests.size > 20) {
                pendingFlightRequests.remove(pendingFlightRequests.keys.first())
            }
            return
        }

        // Jitne recipients select hain (1 se max 15 tak) sabke liye alag image fly hogi
        val targets = recipientIds
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .mapNotNull { recipientId -> seatCenters[recipientId]?.let { recipientId to it } }
            .take(LUCKY_MAX_RECIPIENT_FLIGHTS)
            .toList()
        if (targets.isEmpty()) return

        // Start once on the sender's tap. The later server echo must not create
        // a second copy or restart the same flight batch.
        launchedFlightIds.add(id)
        if (launchedFlightIds.size > 200) launchedFlightIds.remove(launchedFlightIds.first())

        val center = Offset(rootSize.width / 2f, rootSize.height / 2f)

        // Rapid combo taps pe lag na ho isliye active flights bounded rakhenge
        val overflow = (flights.size + targets.size) - LUCKY_MAX_ACTIVE_FLIGHTS
        if (overflow > 0) {
            val removeCount = overflow.coerceAtMost(flights.size)
            repeat(removeCount) { flights.removeAt(0) }
        }

        targets.forEachIndexed { index, (recipientId, target) ->
            flights.add(
                LuckyGiftFlight(
                    id = "${id}_${recipientId}_$index",
                    image = cleanImage,
                    start = center,
                    target = target,
                    rootCenter = center,
                    popupAtCenter = popupAtCenter
                )
            )
        }
    }

    fun updateNotice(value: LuckyNotice?) {
        if (value == null) {
            clearNotice()
            return
        }

        val current = notice
        val incomingImage = value.giftImage.trim()
        val resolvedImage = if (incomingImage.isNotEmpty()) {
            incomingImage
        } else if (current != null && !current.isExiting && current.senderId == value.senderId) {
            current.giftImage.trim()
        } else {
            ""
        }

        val normalized = value.copy(giftImage = resolvedImage)
        val newGiftKey = luckyNoticeGiftKey(normalized)
        val wasHiddenOrExiting = current == null || current.isExiting
        val sameGiftChain = !wasHiddenOrExiting &&
            luckyNoticeGiftKey(current!!) == newGiftKey

        // Patti hatne ke baad ya naya gift aane par × bilkul shuru se (×1) reset hoke start hoga
        if (wasHiddenOrExiting || !sameGiftChain) {
            resetComboSyncState()
            activeGiftKey = newGiftKey
            displayMultiplier = 1
            notice = normalized.copy(
                multiplier = 1,
                isExiting = false,
            )
            noticeGeneration++
            return
        }

        val activeCurrent = current!!
        val mergedMultiplier = if (showComboButton) {
            if (pendingComboClicks > 0) {
                pendingComboClicks =
                    (pendingComboClicks - 1).coerceAtLeast(0)
                maxOf(
                    normalized.multiplier,
                    activeCurrent.multiplier + 1,
                    displayMultiplier + 1,
                )
            } else {
                maxOf(
                    normalized.multiplier,
                    activeCurrent.multiplier,
                    displayMultiplier,
                )
            }
        } else if (normalized.id != activeCurrent.id) {
            if (normalized.multiplier > activeCurrent.multiplier) {
                normalized.multiplier
            } else {
                maxOf(activeCurrent.multiplier, displayMultiplier) + 1
            }
        } else {
            maxOf(
                normalized.multiplier,
                activeCurrent.multiplier,
                displayMultiplier,
            )
        }

        activeGiftKey = newGiftKey
        displayMultiplier = mergedMultiplier
        notice = normalized.copy(
            multiplier = mergedMultiplier,
            isExiting = if (showComboButton) false else normalized.isExiting,
        )
        noticeGeneration++
    }

    internal fun setNoticeExiting(exiting: Boolean) {
        val current = notice ?: return
        if (current.isExiting == exiting) return
        notice = current.copy(isExiting = exiting)
        noticeGeneration++
    }

    internal fun clearNotice() {
        notice = null
        resetComboSyncState()
    }

    fun renewNotice() {
        val current = notice
        if (current != null && current.isExiting) {
            notice = current.copy(isExiting = false)
        }
        noticeGeneration++
    }

    fun confirmCombo(payload: JSONObject) {
        lastComboPayload = JSONObject(payload.toString())
        comboPending = false
        showComboButton = true
        comboTimeLeft = 5
        comboGeneration++
        val current = notice
        if (current != null && current.isExiting) {
            notice = current.copy(isExiting = false)
            noticeGeneration++
        }
    }

    fun beginCombo() { comboPending = true }
    fun failCombo() { comboPending = false }

    fun optimisticCombo() {
        if (pendingComboClicks > 0) {
            pendingComboClicks =
                (pendingComboClicks - 1).coerceAtLeast(0)
        }
        val current = notice
        val nextMultiplier = maxOf(
            current?.multiplier ?: 1,
            displayMultiplier,
        ) + 1
        displayMultiplier = nextMultiplier
        if (current != null) {
            activeGiftKey = luckyNoticeGiftKey(current)
            // Purana win amount copy nahi hoga -> sirf actual win pe hi reward badge aayega
            notice = current.copy(
                multiplier = nextMultiplier,
                totalWinAmount = 0L,
                maxWinTimes = 0,
                isExiting = false,
            )
            noticeGeneration++
        }
        if (showComboButton) {
            comboTimeLeft = 5
            comboGeneration++
        }
    }

    fun tickCombo(value: Int) { comboTimeLeft = value.coerceIn(0, 5) }

    fun hideCombo() {
        showComboButton = false
        comboTimeLeft = 0
        comboPending = false
        lastComboPayload = null
        pendingComboClicks = 0
        // Combo button hatne ke saath hi Notification patti bhi exit hogi (100% connected)
        val current = notice
        if (current != null && !current.isExiting) {
            notice = current.copy(isExiting = true)
            noticeGeneration++
        } else if (current == null) {
            resetComboSyncState()
        }
    }
}

@Composable
fun rememberLuckyAnimationController(roomId: String): LuckyAnimationController =
    remember(roomId) { LuckyAnimationController(roomId) }

@Composable
fun LuckyAnimationControllerEffects(controller: LuckyAnimationController) {
    LaunchedEffect(controller.roomId, controller.showComboButton, controller.comboGeneration) {
        if (!controller.showComboButton) return@LaunchedEffect
        val generation = controller.comboGeneration
        for (second in 1..5) {
            delay(1000L)
            if (controller.comboGeneration != generation || !controller.showComboButton) return@LaunchedEffect
            controller.tickCombo(5 - second)
        }
        if (controller.comboGeneration == generation && controller.showComboButton) {
            controller.hideCombo()
        }
    }

    LaunchedEffect(controller.roomId, controller.noticeGeneration, controller.showComboButton) {
        val generation = controller.noticeGeneration
        val currentNotice = controller.notice ?: return@LaunchedEffect
        val id = currentNotice.id

        // Jab tak Combo button visible hai, Notification patti nahi hategi
        if (controller.showComboButton) {
            if (currentNotice.isExiting) {
                controller.setNoticeExiting(false)
            }
            return@LaunchedEffect
        }

        // Combo hat gaya hai -> exit animation complete karke notice hata do & × reset karo
        if (currentNotice.isExiting) {
            delay(LUCKY_NOTICE_EXIT_DURATION_MS)
            if (
                controller.noticeGeneration == generation &&
                controller.notice?.id == id &&
                !controller.showComboButton
            ) {
                controller.clearNotice()
            }
            return@LaunchedEffect
        }

        // Viewers ke liye (jinke paas Combo button nahi hai): normal visible timer
        delay(LUCKY_NOTICE_VISIBLE_DURATION_MS)
        if (
            controller.noticeGeneration != generation ||
            controller.notice?.id != id ||
            controller.showComboButton
        ) {
            return@LaunchedEffect
        }
        controller.setNoticeExiting(true)
        val exitGeneration = controller.noticeGeneration
        delay(LUCKY_NOTICE_EXIT_DURATION_MS)
        if (
            controller.noticeGeneration == exitGeneration &&
            controller.notice?.id == id &&
            !controller.showComboButton
        ) {
            controller.clearNotice()
        }
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
    controller: LuckyAnimationController,
    floatingWins: List<Pair<String, Long>> = emptyList(),
    targetAvatarOffset: Offset? = null,
) {
    // RoomPage owns the single seat-flight animation. This composable owns
    // only the notification slider and reward badge, avoiding duplicate flights.
    var lastAnimatedNoticeId by remember { mutableStateOf<String?>(null) }
    var lastAnimatedMultiplier by remember { mutableIntStateOf(0) }
    var lastGiftKey by remember { mutableStateOf("") }
    var resetOnNextActive by remember { mutableStateOf(true) }

    // Sirf genuine win pe hi reward badge trigger hoga (har tap pe nahi)
    var lastRewardedNoticeId by remember { mutableStateOf<String?>(null) }
    var lastRewardedWinAmount by remember { mutableLongStateOf(0L) }

    // Reward badge state (3 sec ke liye, naya reward aate hi purana hat ke naya upar se aayega)
    var activeReward by remember { mutableStateOf<LuckyRewardPopupState?>(null) }
    val multiplierPopScale = remember { Animatable(1f) }

    LaunchedEffect(
        notice?.id,
        notice?.multiplier,
        notice?.totalWinAmount,
        notice?.giftImage,
        notice?.isExiting,
    ) {
        if (notice == null) {
            resetOnNextActive = true
            lastAnimatedNoticeId = null
            lastAnimatedMultiplier = 0
            lastGiftKey = ""
            lastRewardedNoticeId = null
            lastRewardedWinAmount = 0L
            activeReward = null
            controller.resetComboSyncState()
            return@LaunchedEffect
        }

        if (notice.isExiting) {
            resetOnNextActive = true
            activeReward = null
            return@LaunchedEffect
        }

        val currentGiftKey = luckyNoticeGiftKey(notice)
        val freshStart = resetOnNextActive || currentGiftKey != lastGiftKey
        val stateChanged = freshStart ||
            notice.id != lastAnimatedNoticeId ||
            notice.multiplier != lastAnimatedMultiplier ||
            notice.totalWinAmount != lastRewardedWinAmount

        if (stateChanged) {
            val previousMultiplier = controller.displayMultiplier
            val syncedMultiplier = if (freshStart) {
                resetOnNextActive = false
                controller.pendingComboClicks = 0
                1
            } else if (controller.pendingComboClicks > 0) {
                controller.pendingComboClicks =
                    (controller.pendingComboClicks - 1).coerceAtLeast(0)
                maxOf(notice.multiplier, previousMultiplier + 1)
            } else {
                maxOf(notice.multiplier, previousMultiplier).coerceAtLeast(1)
            }

            controller.activeGiftKey = currentGiftKey
            controller.displayMultiplier = syncedMultiplier

            if (!freshStart && syncedMultiplier > previousMultiplier) {
                launch {
                    multiplierPopScale.snapTo(1.22f)
                    multiplierPopScale.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 110, easing = FastOutSlowInEasing),
                    )
                }
            }

            // A loss resets the comparison so two consecutive equal-value wins
            // still count as separate server-confirmed win events.
            if (notice.totalWinAmount <= 0L) {
                lastRewardedWinAmount = 0L
            }
            // Win image & coins value only for a positive server-confirmed result.
            val isGenuineNewWin = notice.totalWinAmount > 0L && (
                notice.id != lastRewardedNoticeId ||
                    notice.totalWinAmount != lastRewardedWinAmount
            )
            if (isGenuineNewWin) {
                lastRewardedNoticeId = notice.id
                lastRewardedWinAmount = notice.totalWinAmount
                activeReward = LuckyRewardPopupState(
                    id = "${notice.id}_${notice.totalWinAmount}",
                    winAmount = notice.totalWinAmount,
                )
            }

            lastGiftKey = currentGiftKey
            lastAnimatedNoticeId = notice.id
            lastAnimatedMultiplier = syncedMultiplier
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
        // Notification patti (10.dp aur niche: 66.dp -> 76.dp)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 12.dp)
                .offset(y = 76.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            AnimatedVisibility(
                visible = notice != null && !notice.isExiting,
                enter = slideInHorizontally(
                    initialOffsetX = { -it },
                    animationSpec = tween(durationMillis = 90, easing = FastOutSlowInEasing),
                ) + fadeIn(animationSpec = tween(durationMillis = 90)),
                exit = slideOutHorizontally(
                    targetOffsetX = { -it },
                    animationSpec = tween(durationMillis = 90, easing = FastOutSlowInEasing),
                ) + fadeOut(animationSpec = tween(durationMillis = 90)),
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
                val shownMultiplier = if (controller.activeGiftKey == luckyNoticeGiftKey(notice)) {
                    maxOf(notice.multiplier, controller.displayMultiplier).coerceAtLeast(1)
                } else {
                    notice.multiplier.coerceAtLeast(1)
                }
                val senderImageUrl = remember(notice.senderImage) {
                    luckyAnimationAssetUrl(notice.senderImage)
                }
                val giftImageUrl = remember(notice.giftImage) {
                    luckyAnimationAssetUrl(notice.giftImage)
                }

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
                            .height(48.dp)
                            .wrapContentWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        key(senderImageUrl) {
                            AsyncImage(
                                model = senderImageUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape),
                            )
                        }
                        Spacer(Modifier.width(8.dp))

                        Column(
                            modifier = Modifier.widthIn(min = 96.dp, max = 140.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)
                        ) {
                            Text(
                                text = topText,
                                color = Color.White,
                                fontSize = 12.sp,
                                lineHeight = 14.sp,
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
                                fontSize = 11.sp,
                                lineHeight = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = TextStyle(
                                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                                ),
                            )
                        }
                        Spacer(Modifier.width(8.dp))

                        if (giftImageUrl.isNotEmpty()) {
                            key(giftImageUrl) {
                                AsyncImage(
                                    model = giftImageUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.size(26.dp),
                                )
                            }
                        }
                        Text(
                            text = "×$shownMultiplier",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            fontStyle = FontStyle.Italic,
                            modifier = Modifier
                                .padding(start = 4.dp)
                                .graphicsLayer {
                                    scaleX = multiplierPopScale.value
                                    scaleY = multiplierPopScale.value
                                },
                        )
                    }

                    // Win image badge exact 3.dp niche (26.dp -> 29.dp)
                    activeReward?.let { reward ->
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .offset(x = 142.dp, y = 31.dp)
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

    // Bigger winning image (114.dp), choti shining (84.dp), coins icon & value 5.dp aur upar (-21.dp -> -26.dp)
    Box(
        modifier = modifier
            .size(114.dp)
            .graphicsLayer {
                translationY = dropY.value
                scaleX = popScale.value
                scaleY = popScale.value
                alpha = alphaAnim.value
            },
        contentAlignment = Alignment.Center,
    ) {
        // Rotating Sun Rays Shining behind image (chota size: 84.dp)
        Canvas(modifier = Modifier.requiredSize(84.dp)) {
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

        // Main Reward Image (increased size: 114.dp)
        AsyncImage(
            model = LUCKY_REWARD_BADGE_URL,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )

        // Coin Icon + Yellow Coins Amount exact 5.dp aur upar (-21.dp -> -26.dp)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-28).dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            AsyncImage(
                model = LUCKY_COIN_ICON_URL,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(16.dp).offset(y = (-1).dp),
            )
            Spacer(Modifier.width(2.dp))
            Text(
                text = winAmount.toString(),
                color = Color(0xFFFFEB3B),
                fontSize = 13.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                modifier = Modifier.offset(y = (-3).dp),
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
// LUCKY COMBO BUTTON (Bigger size + Only "Combo" text, no ×)
// ============================================================
@Composable
fun LuckyAnimationComboButton(
    visible: Boolean,
    progress: Float, // Smooth timer progress (0f to 1f)
    onClick: () -> Unit,
    comboCount: Int? = null,
    enabled: Boolean = true,
) {
    if (!visible) return

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
                .size(100.dp)
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
                    enabled = enabled,
                    interactionSource = comboInteraction,
                    indication = null,
                ) {
                    onClick()
                },
        ) {
            // Inner vibrant orange-red to golden-orange circle (bigger size)
            Box(
                modifier = Modifier
                    .size(86.dp)
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
                Text(
                    text = "Combo",
                    color = Color.White,
                    fontSize = 21.sp,
                    lineHeight = 23.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontStyle = FontStyle.Italic,
                    style = TextStyle(
                        shadow = textShadow,
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                    ),
                )
            }

            // Rounded white circular progress arc around the orange circle
            Canvas(modifier = Modifier.fillMaxSize().padding(5.dp)) {
                val strokePx = 4.dp.toPx()
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
// LUCKY GIFT SEAT FLIGHT — Fast center popup -> simultaneous fan-out to seats
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
    val giftImageUrl = remember(flight.image) { luckyAnimationAssetUrl(flight.image) }
    val currentOnFinished by rememberUpdatedState(onFinished)

    if (giftImageUrl.isEmpty()) {
        LaunchedEffect(flight.id) { currentOnFinished() }
        return
    }

    val density = LocalDensity.current
    val imageSizeDp = LUCKY_SEAT_FLIGHT_IMAGE_SIZE_DP.dp
    val halfImagePx = with(density) { imageSizeDp.toPx() } / 2f

    // Single Animatable (0f..1f center popup, 1f..2f fast flight to seat + end fade)
    val motionProgress = remember(flight.id) { Animatable(0f) }

    LaunchedEffect(flight.id) {
        motionProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = LUCKY_SEAT_POPUP_DURATION_MS,
                easing = FastOutSlowInEasing,
            ),
        )
        motionProgress.animateTo(
            targetValue = 2f,
            animationSpec = tween(
                durationMillis = LUCKY_SEAT_FLIGHT_DURATION_MS,
                easing = FastOutSlowInEasing,
            ),
        )
        currentOnFinished()
    }

    val startX = flight.rootCenter.x
    val startY = flight.rootCenter.y
    val deltaX = flight.target.x - startX
    val deltaY = flight.target.y - startY

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(260f),
        contentAlignment = Alignment.TopStart,
    ) {
        key(flight.id, giftImageUrl) {
            AsyncImage(
                model = giftImageUrl,
                contentDescription = "Lucky gift animation",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(imageSizeDp)
                    .graphicsLayer {
                        val progress = motionProgress.value
                        if (progress <= 1f) {
                            val pop = progress.coerceIn(0f, 1f)
                            val scale = 0.35f + (0.65f * pop)
                            translationX = startX - halfImagePx
                            translationY = startY - halfImagePx
                            scaleX = scale
                            scaleY = scale
                            alpha = 1f
                        } else {
                            val fly = (progress - 1f).coerceIn(0f, 1f)
                            translationX = (startX + deltaX * fly) - halfImagePx
                            translationY = (startY + deltaY * fly) - halfImagePx
                            scaleX = 1f
                            scaleY = 1f
                            alpha = if (fly > 0.68f) {
                                (1f - ((fly - 0.68f) / 0.32f)).coerceIn(0f, 1f)
                            } else {
                                1f
                            }
                        }
                    },
            )
        }
    }
}
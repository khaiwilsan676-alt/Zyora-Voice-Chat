package com.hurry.voicechat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private const val LUCKY_REWARD_BADGE_URL =
    "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/IMG_20261125_000330.png"

private const val LUCKY_COIN_ICON_URL =
    "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/file_00000000e56882119c217d508b6733dc.png"

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

private data class LuckyFlyAnimItem(
    val id: String,
    val giftImage: String,
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
    // Sirf ek hi image fly karegi ek time pe
    var activeFlyItem by remember { mutableStateOf<LuckyFlyAnimItem?>(null) }
    var lastAnimatedNoticeId by remember { mutableStateOf<String?>(null) }

    // Reward badge state (3 sec ke liye, naya reward aate hi purana hat ke naya upar se aayega)
    var activeReward by remember { mutableStateOf<LuckyRewardPopupState?>(null) }

    LaunchedEffect(notice?.id) {
        if (notice != null && notice.id != lastAnimatedNoticeId) {
            lastAnimatedNoticeId = notice.id
            activeFlyItem = LuckyFlyAnimItem(id = notice.id, giftImage = notice.giftImage)

            if (notice.totalWinAmount > 0L) {
                activeReward = LuckyRewardPopupState(
                    id = notice.id,
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

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val screenWidthPx = with(density) { maxWidth.toPx() }
        val screenHeightPx = with(density) { maxHeight.toPx() }

        // Notification patti (20.dp niche + compact width + 69.dp trailing fade)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 12.dp)
                .offset(y = 20.dp),
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

                Box(contentAlignment = Alignment.CenterStart) {
                    // Patti ke theek top par (jahan x1 hai wahan chipak ke) Reward Image + Sun Rays + Coins Amount
                    activeReward?.let { reward ->
                        key(reward.id) {
                            LuckyRewardTopBadge(
                                winAmount = reward.winAmount,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(end = 44.dp)
                                    .offset(y = (-54).dp),
                            )
                        }
                    }

                    Row(
                        Modifier
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
                            .padding(start = 4.dp, end = 69.dp, top = 2.dp, bottom = 2.dp)
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
                        Spacer(Modifier.width(6.dp))

                        Column(
                            modifier = Modifier.widthIn(max = 110.dp),
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
                        Spacer(Modifier.width(6.dp))

                        AsyncImage(
                            model = luckyAnimationAssetUrl(notice.giftImage),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(26.dp),
                        )
                        Text(
                            text = "×${notice.multiplier}",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            fontStyle = FontStyle.Italic,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                }
            }
        }

        // Lucky gift flying animation (sirf ek image, sirf seat avatar pe, GPU smooth):
        activeFlyItem?.let { item ->
            key(item.id) {
                LuckyGiftFlyOverlayItem(
                    giftImage = item.giftImage,
                    screenWidthPx = screenWidthPx,
                    screenHeightPx = screenHeightPx,
                    targetAvatarOffset = targetAvatarOffset,
                    onFinished = {
                        if (activeFlyItem?.id == item.id) {
                            activeFlyItem = null
                        }
                    },
                )
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

    Box(
        modifier = modifier
            .graphicsLayer {
                translationY = dropY.value
                scaleX = popScale.value
                scaleY = popScale.value
                alpha = alphaAnim.value
            },
        contentAlignment = Alignment.Center,
    ) {
        // Rotating Sun Rays Shining behind image
        Canvas(modifier = Modifier.size(64.dp)) {
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

        // Main Reward Image
        AsyncImage(
            model = LUCKY_REWARD_BADGE_URL,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(50.dp),
        )

        // Bottom par Coin Icon + Yellow Coins Amount
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-2).dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            AsyncImage(
                model = LUCKY_COIN_ICON_URL,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(13.dp),
            )
            Spacer(Modifier.width(2.dp))
            Text(
                text = winAmount.toString(),
                color = Color(0xFFFFEB3B),
                fontSize = 11.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                style = TextStyle(
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                ),
            )
        }
    }
}

@Composable
private fun LuckyGiftFlyOverlayItem(
    giftImage: String,
    screenWidthPx: Float,
    screenHeightPx: Float,
    targetAvatarOffset: Offset?,
    onFinished: () -> Unit,
) {
    val density = LocalDensity.current
    val baseSizeDp = 52.dp
    val baseSizePx = with(density) { baseSizeDp.toPx() }
    val halfBasePx = baseSizePx / 2f

    val startX = screenWidthPx * 0.5f
    val startY = screenHeightPx - with(density) { 40.dp.toPx() }
    val centerY = screenHeightPx * 0.5f

    val posX = remember { Animatable(startX) }
    val posY = remember { Animatable(startY) }
    // Scale relative to 52.dp: 18dp = 0.35f, 64dp = 1.23f, 52dp = 1.0f, 24dp = 0.46f
    val scaleAnim = remember { Animatable(0.35f) }
    val itemAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // 1. Niche se choti (18dp) hoke 1.5 second (1500ms) mein screen ke center par aana
        val fadeInJob = launch {
            itemAlpha.animateTo(1f, animationSpec = tween(150))
        }
        val moveToCenterJob = launch {
            posY.animateTo(centerY, animationSpec = tween(1500, easing = FastOutSlowInEasing))
        }
        fadeInJob.join()
        moveToCenterJob.join()

        // 2. Center par aake original size (52dp) mein popup animation
        scaleAnim.animateTo(1.23f, animationSpec = tween(140, easing = FastOutSlowInEasing))
        scaleAnim.animateTo(1.0f, animationSpec = tween(110, easing = FastOutSlowInEasing))

        // 3. Center mein 0.8 second (800ms) rukna
        delay(800L)

        // 4. Sirf seat avatar par jaana (choti hoke, taaki avatar ke upar badi na dikhe)
        if (targetAvatarOffset != null) {
            val shrinkJob = launch {
                scaleAnim.animateTo(0.46f, animationSpec = tween(260, easing = FastOutSlowInEasing))
            }
            val flyXJob = launch {
                posX.animateTo(targetAvatarOffset.x, animationSpec = tween(420, easing = FastOutSlowInEasing))
            }
            val flyYJob = launch {
                posY.animateTo(targetAvatarOffset.y, animationSpec = tween(420, easing = FastOutSlowInEasing))
            }
            val fadeOutJob = launch {
                delay(280L)
                itemAlpha.animateTo(0f, animationSpec = tween(140))
            }
            shrinkJob.join()
            flyXJob.join()
            flyYJob.join()
            fadeOutJob.join()
        } else {
            itemAlpha.animateTo(0f, animationSpec = tween(180))
        }

        onFinished()
    }

    AsyncImage(
        model = luckyAnimationAssetUrl(giftImage),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .size(baseSizeDp)
            .graphicsLayer {
                translationX = posX.value - halfBasePx
                translationY = posY.value - halfBasePx
                scaleX = scaleAnim.value
                scaleY = scaleAnim.value
                alpha = itemAlpha.value
            },
    )
}

// ============================================================
// LUCKY COMBO BUTTON (FIXED: GPU-smooth, zero lag on rapid taps)
// ============================================================
@Composable
fun LuckyAnimationComboButton(
    visible: Boolean,
    progress: Float, // Smooth timer progress (0f to 1f)
    onClick: () -> Unit,
) {
    if (!visible) return
    val infiniteTransition = rememberInfiniteTransition(label = "comboPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            tween(900, easing = CubicBezierEasing(0.2f, 0.8f, 0.4f, 1f)),
            RepeatMode.Restart,
        ),
        label = "pulse",
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            tween(900, easing = CubicBezierEasing(0.2f, 0.8f, 0.4f, 1f)),
            RepeatMode.Restart,
        ),
        label = "alpha",
    )

    Box(
        Modifier
            .fillMaxSize()
            .padding(end = 24.dp, bottom = 50.dp),
        contentAlignment = Alignment.BottomEnd,
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(84.dp)) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = pulseScale
                        scaleY = pulseScale
                        alpha = pulseAlpha
                    }
                    .background(
                        Brush.radialGradient(
                            listOf(
                                Color(0x99FF3366),
                                Color(0x1AFF3366),
                            )
                        ),
                        CircleShape,
                    ),
            )

            Box(
                Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0xFFFF4D79), Color(0xFFFF1A4D))
                        )
                    )
                    .clickable { onClick() },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Combo",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                )
                Canvas(Modifier.fillMaxSize().padding(8.dp)) {
                    val stroke = Stroke(width = 3.5f)
                    drawCircle(
                        color = Color.White.copy(alpha = 0.35f),
                        radius = size.minDimension / 2 - stroke.width / 2,
                        style = stroke,
                    )
                }
            }
        }
    }
}

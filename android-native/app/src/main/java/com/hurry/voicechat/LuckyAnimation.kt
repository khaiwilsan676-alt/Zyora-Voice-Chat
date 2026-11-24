package com.hurry.voicechat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private fun luckyAnimationAssetUrl(path: String): String {
    if (path.startsWith("https://") || path.startsWith("http://") || path.startsWith("file:") || path.startsWith("content:")) return path
    return "https://zyora-voice-chat.vercel.app/" + path.trimStart('/')
}

// ============================================================
// LUCKY GIFT NOTIFICATION SLIDER + FLY ANIMATION
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

@Composable
fun LuckyAnimationNotificationSlider(
    notice: LuckyNotice?,
    floatingWins: List<Pair<String, Long>> = emptyList(),
    targetAvatarOffset: Offset? = null,
) {
    // Sirf ek hi image fly karegi ek time pe
    var activeFlyItem by remember { mutableStateOf<LuckyFlyAnimItem?>(null) }
    var lastAnimatedNoticeId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(notice?.id) {
        if (notice != null && notice.id != lastAnimatedNoticeId) {
            lastAnimatedNoticeId = notice.id
            activeFlyItem = LuckyFlyAnimItem(id = notice.id, giftImage = notice.giftImage)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val screenWidthPx = with(density) { maxWidth.toPx() }
        val screenHeightPx = with(density) { maxHeight.toPx() }

        // Notification patti (compact width + 69.dp trailing fade + multiplier right next to name)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 12.dp),
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

        // Lucky gift flying animation (sirf ek image):
        // Niche se choti (18dp) -> 1 sec mein Center -> Popup (64dp -> 52dp) -> 0.8 sec stay -> Choti (30dp) hoke Seat Avatar -> Disappear
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
private fun LuckyGiftFlyOverlayItem(
    giftImage: String,
    screenWidthPx: Float,
    screenHeightPx: Float,
    targetAvatarOffset: Offset?,
    onFinished: () -> Unit,
) {
    val density = LocalDensity.current
    val startX = screenWidthPx * 0.5f
    val startY = screenHeightPx - with(density) { 40.dp.toPx() }
    val centerX = screenWidthPx * 0.5f
    val centerY = screenHeightPx * 0.5f
    val avatarX = targetAvatarOffset?.x ?: with(density) { 32.dp.toPx() }
    val avatarY = targetAvatarOffset?.y ?: (screenHeightPx * 0.5f)

    val posX = remember { Animatable(startX) }
    val posY = remember { Animatable(startY) }
    val sizeDp = remember { Animatable(18f) }
    val itemAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // 1. Niche se choti (18dp) hoke 1 second (1000ms) mein screen ke center par aana
        val fadeInJob = launch {
            itemAlpha.animateTo(1f, animationSpec = tween(150))
        }
        val moveToCenterJob = launch {
            posY.animateTo(centerY, animationSpec = tween(1000, easing = FastOutSlowInEasing))
        }
        fadeInJob.join()
        moveToCenterJob.join()

        // 2. Center par aake popup animation (18dp -> 64dp bounce -> 52dp original size)
        sizeDp.animateTo(64f, animationSpec = tween(140, easing = FastOutSlowInEasing))
        sizeDp.animateTo(52f, animationSpec = tween(110, easing = FastOutSlowInEasing))

        // 3. Center mein 0.8 second (800ms) rukna
        delay(800L)

        // 4. Thodi si choti (30dp) hoke seat avatar par jaana aur gayab hona
        val shrinkJob = launch {
            sizeDp.animateTo(30f, animationSpec = tween(260, easing = FastOutSlowInEasing))
        }
        val flyXJob = launch {
            posX.animateTo(avatarX, animationSpec = tween(420, easing = FastOutSlowInEasing))
        }
        val flyYJob = launch {
            posY.animateTo(avatarY, animationSpec = tween(420, easing = FastOutSlowInEasing))
        }
        val fadeOutJob = launch {
            delay(300L)
            itemAlpha.animateTo(0f, animationSpec = tween(140))
        }
        shrinkJob.join()
        flyXJob.join()
        flyYJob.join()
        fadeOutJob.join()

        onFinished()
    }

    val halfSizePx = with(density) { (sizeDp.value / 2f).dp.toPx() }

    AsyncImage(
        model = luckyAnimationAssetUrl(giftImage),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .offset {
                IntOffset(
                    x = (posX.value - halfSizePx).roundToInt(),
                    y = (posY.value - halfSizePx).roundToInt(),
                )
            }
            .size(sizeDp.value.dp)
            .alpha(itemAlpha.value),
    )
}

// ============================================================
// LUCKY COMBO BUTTON (FIXED: Smooth progress support)
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
            tween(1800, easing = CubicBezierEasing(0.2f, 0.8f, 0.4f, 1f)),
            RepeatMode.Restart,
        ),
        label = "pulse",
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            tween(1800, easing = CubicBezierEasing(0.2f, 0.8f, 0.4f, 1f)),
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
                    .scale(pulseScale)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                Color(0x99FF3366),
                                Color(0x1AFF3366),
                            )
                        ),
                        CircleShape,
                    )
                    .alpha(pulseAlpha),
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

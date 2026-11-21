package com.hurry.voicechat

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val BASE_URL =
    "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/"

private const val DRAGON_BATTLE_BG =
    "${BASE_URL}public/IMG-20261121-WA0034.jpg"
private const val DRAGON_BATTLE_LOGO =
    "${BASE_URL}public/IMG_20261121_020906.png"

private const val CARD_IMAGE =
    "${BASE_URL}public/IMG_20261121_020825.png"
private const val TIMER_BG_IMAGE =
    "${BASE_URL}public/file_000000009cb882119b1349bc49e0d864.png"

private const val DRAGON_A_IMAGE =
    "${BASE_URL}public/IMG_20261121_020609.png"
private const val DRAGON_B_IMAGE =
    "${BASE_URL}public/IMG_20261121_020547.png"
private const val DRAGON_C_IMAGE =
    "${BASE_URL}public/IMG_20261121_020531.png"
private const val DRAGON_COIN_IMAGE =
    "${BASE_URL}public/file_00000000e56882119c217d508b6733dc.png"
private const val DRAGON_BOTTOM_LEFT_IMAGE =
    "${BASE_URL}public/file_00000000993c8208ae462545f9ccb0ba.png"

private const val DRAGON_WINNER_HEADING =
    "${BASE_URL}public/IMG_20260913_000423.png"
private const val DRAGON_WINNER_FRAME_2ND =
    "${BASE_URL}public/IMG_20260912_235156.png"
private const val DRAGON_WINNER_FRAME_1ST =
    "${BASE_URL}public/IMG_20260912_235215.png"
private const val DRAGON_WINNER_FRAME_3RD =
    "${BASE_URL}public/IMG_20260912_235230.png"

private data class BetChip(
    val label: String,
    val value: Long,
    val imageUrl: String
)

private enum class DragonSuit(val power: Int, val isRed: Boolean) {
    SPADES(4, false),
    HEARTS(3, true),
    CLUBS(2, false),
    DIAMONDS(1, true)
}

private data class DragonPlayingCard(
    val rankLabel: String,
    val rankValue: Int,
    val suit: DragonSuit
)

private val DRAGON_RANKS = listOf(
    "2" to 2, "3" to 3, "4" to 4, "5" to 5, "6" to 6,
    "7" to 7, "8" to 8, "9" to 9, "10" to 10,
    "J" to 11, "Q" to 12, "K" to 13, "A" to 14
)

private fun dealDragonRoundCards(): Triple<List<DragonPlayingCard>, List<DragonPlayingCard>, List<DragonPlayingCard>> {
    val deck = buildList {
        for (suit in DragonSuit.entries) {
            for ((label, value) in DRAGON_RANKS) {
                add(DragonPlayingCard(label, value, suit))
            }
        }
    }.shuffled()
    return Triple(deck.subList(0, 3), deck.subList(3, 6), deck.subList(6, 9))
}

private fun evaluateDragonHandScore(cards: List<DragonPlayingCard>): Long {
    val sorted = cards.sortedWith(compareByDescending<DragonPlayingCard> { it.rankValue }.thenByDescending { it.suit.power })
    val v0 = sorted[0].rankValue
    val v1 = sorted[1].rankValue
    val v2 = sorted[2].rankValue
    val isFlush = sorted[0].suit == sorted[1].suit && sorted[1].suit == sorted[2].suit
    val isNormalSeq = (v0 == v1 + 1 && v1 == v2 + 1)
    val isA23Seq = (v0 == 14 && v1 == 3 && v2 == 2)
    val isSeq = isNormalSeq || isA23Seq
    val isSet = (v0 == v1 && v1 == v2)
    val isPair = !isSet && (v0 == v1 || v1 == v2)

    val tier = when {
        isSet -> 6
        isFlush && isSeq -> 5
        isFlush -> 4
        isSeq -> 3
        isPair -> 2
        else -> 1
    }

    val (r0, r1, r2) = when {
        isA23Seq -> Triple(14, 3, 2)
        isPair && v1 == v2 -> Triple(v1, v2, v0)
        else -> Triple(v0, v1, v2)
    }
    val suitTie = sorted[0].suit.power * 25 + sorted[1].suit.power * 5 + sorted[2].suit.power
    return tier * 1_000_000L + r0 * 10_000L + r1 * 500L + r2 * 25L + suitTie
}

private fun pickDragonWinner(
    cardsA: List<DragonPlayingCard>,
    cardsB: List<DragonPlayingCard>,
    cardsC: List<DragonPlayingCard>
): String {
    val scoreA = evaluateDragonHandScore(cardsA)
    val scoreB = evaluateDragonHandScore(cardsB)
    val scoreC = evaluateDragonHandScore(cardsC)
    return when {
        scoreA >= scoreB && scoreA >= scoreC -> "A"
        scoreB >= scoreA && scoreB >= scoreC -> "B"
        else -> "C"
    }
}

private val BET_CHIPS = listOf(
    BetChip("5K", 5_000L, "${BASE_URL}public/IMG_20261121_020630.png"),
    BetChip("50K", 50_000L, "${BASE_URL}public/IMG_20261121_020651.png"),
    BetChip("500K", 500_000L, "${BASE_URL}public/IMG_20261121_020720.png"),
    BetChip("5M", 5_000_000L, "${BASE_URL}public/IMG_20261121_020746.png"),
    BetChip("10M", 10_000_000L, "${BASE_URL}public/IMG_20261121_020809.png")
)

private class DragonBetSoundManager(private val context: Context) {
    private val betFileName = "VID_20261121_003223_909_bsl.mp4"
    private val winnerFileName = "VID_20261121_003527_312_bsl.mp4"
    private val betPool = mutableListOf<MediaPlayer>()
    private var winnerPlayer: MediaPlayer? = null
    private var betCursor = 0
    @Volatile private var muted = false
    @Volatile private var released = false

    suspend fun preload() = withContext(Dispatchers.IO) {
        val betFile = ensureCached(betFileName)
        val winFile = ensureCached(winnerFileName)
        if (released) return@withContext
        synchronized(this@DragonBetSoundManager) {
            if (released) return@synchronized
            if (betFile != null && betPool.isEmpty()) {
                repeat(4) { createPreparedPlayer(betFile)?.let { betPool.add(it) } }
            }
            if (winFile != null && winnerPlayer == null) {
                winnerPlayer = createPreparedPlayer(winFile)
            }
            applyVolumeLocked(if (muted) 0f else 1f)
        }
    }

    private fun ensureCached(fileName: String): File? {
        val target = File(context.cacheDir, "wp_sfx_$fileName")
        if (target.exists() && target.length() > 0L) return target
        return runCatching {
            val tmp = File(context.cacheDir, "wp_sfx_${fileName}.tmp")
            val conn = (URL("${BASE_URL}public/$fileName").openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 10000
                instanceFollowRedirects = true
            }
            try {
                if (conn.responseCode in 200..299) {
                    conn.inputStream.use { input ->
                        tmp.outputStream().use { output -> input.copyTo(output) }
                    }
                    if (tmp.exists() && tmp.length() > 0L) {
                        if (target.exists()) target.delete()
                        tmp.renameTo(target)
                        target
                    } else null
                } else null
            } finally {
                conn.disconnect()
            }
        }.getOrNull()
    }

    private fun createPreparedPlayer(file: File): MediaPlayer? {
        return runCatching {
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(file.absolutePath)
                prepare()
            }
        }.getOrNull()
    }

    private fun applyVolumeLocked(vol: Float) {
        betPool.forEach { runCatching { it.setVolume(vol, vol) } }
        winnerPlayer?.let { runCatching { it.setVolume(vol, vol) } }
    }

    fun setMuted(isMuted: Boolean) {
        synchronized(this) {
            muted = isMuted
            if (released) return
            if (isMuted) {
                applyVolumeLocked(0f)
                betPool.forEach { runCatching { if (it.isPlaying) it.pause(); it.seekTo(0) } }
                winnerPlayer?.let { runCatching { if (it.isPlaying) it.pause(); it.seekTo(0) } }
            } else {
                applyVolumeLocked(1f)
            }
        }
    }

    fun playBetClick() {
        if (muted || released) return
        synchronized(this) {
            if (muted || released || betPool.isEmpty()) return
            val mp = betPool[betCursor % betPool.size]
            betCursor++
            runCatching {
                if (mp.isPlaying) mp.pause()
                mp.setVolume(1f, 1f)
                mp.seekTo(0)
                mp.start()
            }
        }
    }

    fun playWinner() {
        if (muted || released) return
        synchronized(this) {
            if (muted || released) return
            val mp = winnerPlayer ?: return
            runCatching {
                if (mp.isPlaying) mp.pause()
                mp.setVolume(1f, 1f)
                mp.seekTo(0)
                mp.start()
            }
        }
    }

    fun stopWinner() {
        synchronized(this) {
            if (released) return
            winnerPlayer?.let { mp ->
                runCatching {
                    if (mp.isPlaying) mp.pause()
                    mp.seekTo(0)
                }
            }
        }
    }

    fun release() {
        synchronized(this) {
            released = true
            betPool.forEach { runCatching { it.stop(); it.release() } }
            betPool.clear()
            winnerPlayer?.let { runCatching { it.stop(); it.release() } }
            winnerPlayer = null
        }
    }
}

@Composable
private fun DragonSuitVectorIcon(
    suit: DragonSuit,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            when (suit) {
                DragonSuit.DIAMONDS -> {
                    moveTo(w * 0.50f, h * 0.04f)
                    lineTo(w * 0.94f, h * 0.50f)
                    lineTo(w * 0.50f, h * 0.96f)
                    lineTo(w * 0.06f, h * 0.50f)
                    close()
                }
                DragonSuit.HEARTS -> {
                    moveTo(w * 0.50f, h * 0.92f)
                    cubicTo(w * 0.06f, h * 0.62f, w * 0.02f, h * 0.24f, w * 0.26f, h * 0.12f)
                    cubicTo(w * 0.39f, h * 0.06f, w * 0.48f, h * 0.17f, w * 0.50f, h * 0.28f)
                    cubicTo(w * 0.52f, h * 0.17f, w * 0.61f, h * 0.06f, w * 0.74f, h * 0.12f)
                    cubicTo(w * 0.98f, h * 0.24f, w * 0.94f, h * 0.62f, w * 0.50f, h * 0.92f)
                    close()
                }
                DragonSuit.SPADES -> {
                    moveTo(w * 0.50f, h * 0.05f)
                    cubicTo(w * 0.95f, h * 0.40f, w * 0.96f, h * 0.76f, w * 0.68f, h * 0.76f)
                    cubicTo(w * 0.59f, h * 0.76f, w * 0.54f, h * 0.72f, w * 0.53f, h * 0.69f)
                    lineTo(w * 0.63f, h * 0.95f)
                    lineTo(w * 0.37f, h * 0.95f)
                    lineTo(w * 0.47f, h * 0.69f)
                    cubicTo(w * 0.46f, h * 0.72f, w * 0.41f, h * 0.76f, w * 0.32f, h * 0.76f)
                    cubicTo(w * 0.04f, h * 0.76f, w * 0.05f, h * 0.40f, w * 0.50f, h * 0.05f)
                    close()
                }
                DragonSuit.CLUBS -> {
                    moveTo(w * 0.46f, h * 0.62f)
                    lineTo(w * 0.54f, h * 0.62f)
                    lineTo(w * 0.64f, h * 0.95f)
                    lineTo(w * 0.36f, h * 0.95f)
                    close()
                }
            }
        }
        drawPath(path = path, color = color, style = Fill)
        if (suit == DragonSuit.CLUBS) {
            drawCircle(color = color, radius = w * 0.23f, center = Offset(w * 0.50f, h * 0.28f))
            drawCircle(color = color, radius = w * 0.23f, center = Offset(w * 0.27f, h * 0.54f))
            drawCircle(color = color, radius = w * 0.23f, center = Offset(w * 0.73f, h * 0.54f))
            drawCircle(color = color, radius = w * 0.14f, center = Offset(w * 0.50f, h * 0.50f))
        }
    }
}

@Composable
private fun DragonPlayingCardFace(
    card: DragonPlayingCard,
    modifier: Modifier = Modifier
) {
    val suitColor = if (card.suit.isRed) Color(0xFFE53935) else Color(0xFF222222)
    val cardShape = RoundedCornerShape(4.dp)

    Box(
        modifier = modifier
            .clip(cardShape)
            .background(Color.White)
            .border(0.8.dp, Color(0xFF3A3A3A), cardShape)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 2.5.dp, top = 1.5.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = card.rankLabel,
                color = suitColor,
                fontSize = if (card.rankLabel.length > 1) 12.sp else 14.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                softWrap = false
            )
            DragonSuitVectorIcon(
                suit = card.suit,
                color = suitColor,
                modifier = Modifier.size(9.dp)
            )
        }

        DragonSuitVectorIcon(
            suit = card.suit,
            color = suitColor,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 3.dp, bottom = 3.dp)
                .size(22.dp)
        )
    }
}

@Composable
private fun FlippableDragonCard(
    card: DragonPlayingCard,
    isFlipped: Boolean,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "dragonCardFlip"
    )

    Box(
        modifier = modifier.graphicsLayer {
            rotationY = rotation
            cameraDistance = 12f * density
        },
        contentAlignment = Alignment.Center
    ) {
        if (rotation <= 90f) {
            AsyncImage(
                model = CARD_IMAGE,
                contentDescription = "Card Back",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Fit
            )
        } else {
            DragonPlayingCardFace(
                card = card,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationY = 180f }
            )
        }
    }
}

@Composable
private fun DragonWinnerAvatarPlaceholder(modifier: Modifier = Modifier) {
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
private fun DragonGlassTopButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit
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
                    colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.24f)),
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
private fun DragonGlassBackButton(
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
internal fun DragonBattleGameItem(onClick: () -> Unit) {
    Column(
        modifier = Modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        AsyncImage(
            model = DRAGON_BATTLE_LOGO,
            contentDescription = "Dragon Battle",
            modifier = Modifier.size(56.dp),
            contentScale = ContentScale.Fit
        )
        Text(
            text = "Dragon Battle",
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

/**
 * Screen ke ekdam BOTTOM par open hoga.
 * Height exact Wild Party ke barabar (maxHeight * 0.65f) rahegi.
 */
@Composable
fun DragonBattleGameHost(
    modifier: Modifier = Modifier,
    onClose: () -> Unit = {},
    onMinimize: () -> Unit = {}
) {
    var isLoaded by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .zIndex(100f)
            .background(Color.Transparent),
        contentAlignment = Alignment.BottomCenter
    ) {
        val sheetHeight = maxHeight * 0.65f

        // Full-screen dim backdrop (blocks clicks from reaching Room behind)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
        )

        if (!isLoaded) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(sheetHeight)
                    .background(Color(0xFF28180D))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    ),
                contentAlignment = Alignment.BottomCenter
            ) {
                AsyncImage(
                    model = DRAGON_BATTLE_BG,
                    contentDescription = "Dragon Battle Loading Background",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                DragonBattleLoadingOverlay(
                    modifier = Modifier.matchParentSize(),
                    onLoadingFinished = { isLoaded = true }
                )
            }
        } else {
            DragonBattleMainOverlay(
                sheetHeight = sheetHeight,
                modifier = Modifier.fillMaxSize(),
                onClose = onClose,
                onMinimize = onMinimize
            )
        }
    }
}

@Composable
private fun DragonBattleLoadingOverlay(
    modifier: Modifier = Modifier,
    onLoadingFinished: () -> Unit
) {
    var progress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        val totalSteps = 100
        for (i in 1..totalSteps) {
            delay(25L)
            progress = i / totalSteps.toFloat()
        }
        onLoadingFinished()
    }

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.24f))
        )

        Text(
            text = "Dragon Battle",
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp),
            style = TextStyle(
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Serif,
                shadow = Shadow(
                    color = Color(0xCC2B1200),
                    offset = Offset(2f, 3f),
                    blurRadius = 5f
                )
            )
        )

        AsyncImage(
            model = DRAGON_BATTLE_LOGO,
            contentDescription = "Dragon Battle Logo",
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 26.dp)
                .size(145.dp),
            contentScale = ContentScale.Fit
        )

        BlueGlossyLoadingBar(
            progress = progress,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 20.dp, vertical = 22.dp)
        )
    }
}

@Composable
private fun BlueGlossyLoadingBar(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val clampedProgress = progress.coerceIn(0f, 1f)
    val outerShape = RoundedCornerShape(50)
    val innerShape = RoundedCornerShape(50)

    val goldBorderBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFFF2A6),
            Color(0xFFE5A924),
            Color(0xFFA66A08),
            Color(0xFFF7D154)
        )
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
            .clip(outerShape)
            .background(Color(0xFF4E2605))
            .border(width = 2.5.dp, brush = goldBorderBrush, shape = outerShape)
            .padding(2.5.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (clampedProgress > 0f) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth(clampedProgress)
                    .fillMaxHeight()
                    .clip(innerShape)
            ) {
                val w = size.width
                val h = size.height

                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF5CE1FF),
                            Color(0xFF1589FF),
                            Color(0xFF004BB5)
                        )
                    )
                )

                val stripeWidth = h * 0.65f
                val step = stripeWidth * 2f
                var x = -h
                while (x < w + h) {
                    val path = Path().apply {
                        moveTo(x, h)
                        lineTo(x + stripeWidth, h)
                        lineTo(x + stripeWidth + h * 0.55f, 0f)
                        lineTo(x + h * 0.55f, 0f)
                        close()
                    }
                    drawPath(path = path, color = Color(0x4D9EEBFF))
                    x += step
                }

                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0x80FFFFFF), Color(0x00FFFFFF)),
                        startY = 0f,
                        endY = h * 0.45f
                    )
                )
            }
        }

        Text(
            text = String.format(Locale.US, "%.2f%%", clampedProgress * 100f),
            modifier = Modifier.align(Alignment.Center),
            style = TextStyle(
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                shadow = Shadow(
                    color = Color(0xCC001A4D),
                    offset = Offset(1f, 2f),
                    blurRadius = 3f
                )
            )
        )
    }
}

@Composable
private fun DragonBattleMainOverlay(
    sheetHeight: Dp,
    modifier: Modifier = Modifier,
    onClose: () -> Unit = {},
    onMinimize: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val soundManager = remember(context) { DragonBetSoundManager(context.applicationContext) }

    var timeLeft by remember { mutableIntStateOf(20) }
    var isRevealing by remember { mutableStateOf(false) }
    var revealedCount by remember { mutableIntStateOf(0) }
    var roundCards by remember { mutableStateOf(dealDragonRoundCards()) }

    var selectedChipIndex by remember { mutableIntStateOf(0) }
    var muted by remember { mutableStateOf(false) }
    var balance by remember { mutableLongStateOf(500_000L) }
    var rulesOpen by remember { mutableStateOf(false) }
    var historyOpen by remember { mutableStateOf(false) }
    var historyList by remember { mutableStateOf(listOf("C", "B", "A", "B", "A")) }

    // Winner page states (same as Wild Party)
    var winnerOpen by remember { mutableStateOf(false) }
    var winnerDragon by remember { mutableStateOf("A") }
    var winnerTimer by remember { mutableIntStateOf(5) }
    var roundBetAmount by remember { mutableLongStateOf(0L) }
    var roundWinAmount by remember { mutableLongStateOf(0L) }

    var potA by remember { mutableLongStateOf(0L) }
    var youA by remember { mutableLongStateOf(0L) }
    var potB by remember { mutableLongStateOf(0L) }
    var youB by remember { mutableLongStateOf(0L) }
    var potC by remember { mutableLongStateOf(0L) }
    var youC by remember { mutableLongStateOf(0L) }

    var lastBetTable by remember { mutableStateOf<String?>(null) }

    DisposableEffect(soundManager) {
        onDispose { soundManager.release() }
    }

    LaunchedEffect(Unit) {
        launch(Dispatchers.IO) { soundManager.preload() }
        while (true) {
            // 1. Betting countdown (20s -> 1s)
            isRevealing = false
            revealedCount = 0
            winnerOpen = false
            potA = 0L; youA = 0L
            potB = 0L; youB = 0L
            potC = 0L; youC = 0L

            for (sec in 20 downTo 1) {
                timeLeft = sec
                delay(1000L)
            }

            // 2. Round finished -> Deal random cards & flip 1 card every 1.5s: Group A -> Group B -> Group C
            timeLeft = 0
            isRevealing = true
            val dealt = dealDragonRoundCards()
            roundCards = dealt

            for (cardStep in 1..9) {
                revealedCount = cardStep
                delay(1500L)
            }

            // 3. Evaluate winner, credit reward, update history & show Winner Page (5s auto-close)
            val winner = pickDragonWinner(dealt.first, dealt.second, dealt.third)
            winnerDragon = winner
            historyList = (listOf(winner) + historyList).take(20)

            val totalBet = youA + youB + youC
            val winningBet = when (winner) {
                "A" -> youA
                "B" -> youB
                else -> youC
            }
            val prize = (winningBet * 2.95).toLong()
            roundBetAmount = totalBet
            roundWinAmount = prize
            if (prize > 0L) {
                balance += prize
            }

            delay(1000L)
            winnerTimer = 5
            winnerOpen = true
            launch(Dispatchers.IO) { soundManager.playWinner() }
            repeat(5) { step ->
                delay(1000L)
                winnerTimer = 4 - step
            }
            winnerOpen = false
            launch(Dispatchers.IO) { soundManager.stopWinner() }
        }
    }

    LaunchedEffect(muted) {
        withContext(Dispatchers.IO) {
            soundManager.setMuted(muted)
        }
    }

    fun placeBet(table: String) {
        if (isRevealing || winnerOpen) return
        val chipValue = BET_CHIPS[selectedChipIndex].value
        if (balance < chipValue) return
        scope.launch(Dispatchers.IO) { soundManager.playBetClick() }
        balance -= chipValue
        lastBetTable = table
        when (table) {
            "A" -> {
                potA += chipValue
                youA += chipValue
            }
            "B" -> {
                potB += chipValue
                youB += chipValue
            }
            "C" -> {
                potC += chipValue
                youC += chipValue
            }
        }
    }

    val (cardsA, cardsB, cardsC) = roundCards
    val flippedA = revealedCount.coerceIn(0, 3)
    val flippedB = (revealedCount - 3).coerceIn(0, 3)
    val flippedC = (revealedCount - 6).coerceIn(0, 3)

    Box(modifier = modifier, contentAlignment = Alignment.BottomCenter) {
        // Main Game Container (exact same height as Wild Party)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(sheetHeight)
                .background(Color(0xFF28180D))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
        ) {
            AsyncImage(
                model = DRAGON_BATTLE_BG,
                contentDescription = "Dragon Battle Background",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Wild Party-matching top controls and centered title.
                Row(
                    modifier = Modifier.fillMaxWidth().height(32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DragonGlassTopButton(onClick = { muted = !muted }) {
                            Canvas(Modifier.size(15.dp)) {
                                val speaker = Path().apply {
                                    moveTo(1.5.dp.toPx(), 5.dp.toPx())
                                    lineTo(5.dp.toPx(), 5.dp.toPx())
                                    lineTo(9.5.dp.toPx(), 1.8.dp.toPx())
                                    lineTo(9.5.dp.toPx(), 13.2.dp.toPx())
                                    lineTo(5.dp.toPx(), 10.dp.toPx())
                                    lineTo(1.5.dp.toPx(), 10.dp.toPx())
                                    close()
                                }
                                drawPath(speaker, Color.White.copy(alpha = if (muted) 0.55f else 1f), style = Fill)
                                drawPath(speaker, Color.White.copy(alpha = if (muted) 0.55f else 1f), style = Stroke(1.2.dp.toPx(), join = StrokeJoin.Round))
                                if (muted) {
                                    drawLine(Color.White, Offset(2.dp.toPx(), 2.dp.toPx()), Offset(13.dp.toPx(), 13.dp.toPx()), 2.2.dp.toPx(), cap = StrokeCap.Round)
                                } else {
                                    drawArc(Color.White, -52f, 104f, false, topLeft = Offset(7.2.dp.toPx(), 2.5.dp.toPx()), size = Size(7.2.dp.toPx(), 10.dp.toPx()), style = Stroke(1.9.dp.toPx(), cap = StrokeCap.Round))
                                }
                            }
                        }
                        DragonGlassTopButton(onClick = { rulesOpen = true; historyOpen = false }) {
                            Text(
                                "?",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                style = TextStyle(shadow = Shadow(Color.White.copy(alpha = 0.55f), Offset.Zero, 6f))
                            )
                        }
                    }

                    Text(
                        text = "Dragon Battle",
                        modifier = Modifier.weight(1.4f),
                        textAlign = TextAlign.Center,
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            shadow = Shadow(Color.Black.copy(alpha = 0.48f), Offset(0f, 1.6f), 2f)
                        )
                    )

                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DragonGlassTopButton(onClick = { historyOpen = true; rulesOpen = false }) {
                            Canvas(Modifier.size(14.dp)) {
                                val hBar = 2.4.dp.toPx()
                                val r = CornerRadius(hBar / 2f, hBar / 2f)
                                drawRoundRect(Color.White, Offset(0.5.dp.toPx(), 2.dp.toPx()), Size(13.dp.toPx(), hBar), r)
                                drawRoundRect(Color.White, Offset(0.5.dp.toPx(), 6.dp.toPx()), Size(13.dp.toPx(), hBar), r)
                                drawRoundRect(Color.White, Offset(0.5.dp.toPx(), 10.dp.toPx()), Size(8.5.dp.toPx(), hBar), r)
                                drawRoundRect(Color.White, Offset(10.5.dp.toPx(), 10.dp.toPx()), Size(3.dp.toPx(), hBar), r)
                            }
                        }
                        DragonGlassTopButton(onClick = onMinimize) {
                            Canvas(Modifier.size(14.dp)) {
                                val tri = Path().apply {
                                    moveTo(2.2.dp.toPx(), 4.2.dp.toPx())
                                    lineTo(11.8.dp.toPx(), 4.2.dp.toPx())
                                    lineTo(7.dp.toPx(), 10.8.dp.toPx())
                                    close()
                                }
                                drawPath(tri, Color.White.copy(alpha = 0.35f), style = Stroke(3.6.dp.toPx(), join = StrokeJoin.Round))
                                drawPath(tri, Brush.verticalGradient(listOf(Color(0xFFDDE3EE), Color.White)), style = Fill)
                                drawPath(tri, Brush.verticalGradient(listOf(Color(0xFFDDE3EE), Color.White)), style = Stroke(2.2.dp.toPx(), join = StrokeJoin.Round))
                            }
                        }
                        DragonGlassTopButton(onClick = onClose) {
                            Canvas(Modifier.size(14.dp)) {
                                val a = 3.1.dp.toPx()
                                val b = 10.9.dp.toPx()
                                drawLine(Color.White.copy(alpha = 0.35f), Offset(a, a), Offset(b, b), 4.6.dp.toPx(), cap = StrokeCap.Round)
                                drawLine(Color.White.copy(alpha = 0.35f), Offset(b, a), Offset(a, b), 4.6.dp.toPx(), cap = StrokeCap.Round)
                                drawLine(Color.White, Offset(a, a), Offset(b, b), 3.dp.toPx(), cap = StrokeCap.Round)
                                drawLine(Color.White, Offset(b, a), Offset(a, b), 3.dp.toPx(), cap = StrokeCap.Round)
                            }
                        }
                    }
                }

                // 2. TOP 3 CARD TABLES + CENTER 20s TIMER
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OverlappingThreeCardsTable(
                            multiplier = "2.92",
                            cards = cardsA,
                            flippedCount = flippedA
                        )
                        OverlappingThreeCardsTable(
                            multiplier = "2.92",
                            cards = cardsB,
                            flippedCount = flippedB
                        )
                        OverlappingThreeCardsTable(
                            multiplier = "2.92",
                            cards = cardsC,
                            flippedCount = flippedC
                        )
                    }

                    Box(
                        modifier = Modifier
                            .offset(y = (-7).dp)
                            .size(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = TIMER_BG_IMAGE,
                            contentDescription = "Countdown Timer Background",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                        Text(
                            text = timeLeft.toString(),
                            style = TextStyle(
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                shadow = Shadow(
                                    color = Color(0xCC000000),
                                    offset = Offset(1f, 2f),
                                    blurRadius = 3f
                                )
                            )
                        )
                    }
                }

                // 3. DRAGONS (A, B, C) + BETTING BOXES
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DragonBetColumn(
                        label = "A",
                        dragonImage = DRAGON_A_IMAGE,
                        pot = potA,
                        you = youA,
                        modifier = Modifier.weight(1f),
                        onClick = { placeBet("A") }
                    )
                    DragonBetColumn(
                        label = "B",
                        dragonImage = DRAGON_B_IMAGE,
                        pot = potB,
                        you = youB,
                        modifier = Modifier.weight(1f),
                        onClick = { placeBet("B") }
                    )
                    DragonBetColumn(
                        label = "C",
                        dragonImage = DRAGON_C_IMAGE,
                        pot = potC,
                        you = youC,
                        modifier = Modifier.weight(1f),
                        onClick = { placeBet("C") }
                    )
                }

                // Bottom-left artwork sitting neatly above the balance bar
                AsyncImage(
                    model = DRAGON_BOTTOM_LEFT_IMAGE,
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(start = 2.dp, bottom = 2.dp)
                        .size(38.dp),
                    contentScale = ContentScale.Fit
                )

                // Wild Party identical bottom balance bar + repeat + chips row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        )
                        .padding(bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DragonBalanceBar(balance = balance)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        DragonRepeatButton(
                            onClick = { lastBetTable?.let { placeBet(it) } }
                        )
                        BET_CHIPS.forEachIndexed { index, chip ->
                            val isSelected = selectedChipIndex == index
                            val chipRotation by animateFloatAsState(
                                targetValue = if (isSelected) 10f else 0f,
                                animationSpec = tween(durationMillis = 140),
                                label = "dragonChipRotation"
                            )
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .graphicsLayer {
                                        val scale = if (isSelected) 1.08f else 1f
                                        scaleX = scale
                                        scaleY = scale
                                        translationY = if (isSelected) -8.dp.toPx() else 0f
                                        rotationZ = chipRotation
                                    }
                                    .then(
                                        if (isSelected) Modifier.border(1.8.dp, Color(0xFFFFD83D), CircleShape)
                                        else Modifier
                                    )
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { selectedChipIndex = index },
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = chip.imageUrl,
                                    contentDescription = chip.label,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                    }
                }
            }

            // Exact same Wild Party height Rules Overlay (on ? click)
            if (rulesOpen) {
                DragonRulesOverlay(
                    modifier = Modifier.matchParentSize(),
                    onBack = { rulesOpen = false }
                )
            }

            // Exact same Wild Party height History Overlay (on 3-line icon click)
            if (historyOpen) {
                DragonHistoryOverlay(
                    history = historyList,
                    modifier = Modifier.matchParentSize(),
                    onBack = { historyOpen = false }
                )
            }
        }

        // Winner Page (Same design & behavior as Wild Party - auto-closes on countdown end)
        if (winnerOpen) {
            DragonWinnerOverlay(
                sheetHeight = sheetHeight,
                winnerDragon = winnerDragon,
                winnerTimer = winnerTimer,
                winAmount = roundWinAmount,
                betAmount = roundBetAmount
            )
        }
    }
}

@Composable
private fun DragonWinnerOverlay(
    sheetHeight: Dp,
    winnerDragon: String,
    winnerTimer: Int,
    winAmount: Long,
    betAmount: Long
) {
    val winnerSheetShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    val winnerImg = when (winnerDragon) {
        "A" -> DRAGON_A_IMAGE
        "B" -> DRAGON_B_IMAGE
        else -> DRAGON_C_IMAGE
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = sheetHeight * 0.86f)
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
            // Top Section: Countdown (top-right) + Winner Dragon (left) + Prizes & Your Bet (right)
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
                            model = winnerImg,
                            contentDescription = "Winner Dragon $winnerDragon",
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
                                modifier = Modifier.weight(0.70f),
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.weight(1.15f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                AsyncImage(
                                    model = DRAGON_COIN_IMAGE,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    contentScale = ContentScale.Fit
                                )
                                Text(
                                    text = formatShortAmount(winAmount),
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
                                modifier = Modifier.weight(0.70f),
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.weight(1.15f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                AsyncImage(
                                    model = DRAGON_COIN_IMAGE,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    contentScale = ContentScale.Fit
                                )
                                Text(
                                    text = formatShortAmount(betAmount),
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

            // Middle Heading Artwork (Top Winner image - exact same placement as Wild Party)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = DRAGON_WINNER_HEADING,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = 6.dp)
                        .requiredHeight(72.dp),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(Modifier.height(14.dp))

            // Bottom Podium: 2nd | 1st (slightly higher) | 3rd
            val frameItems = listOf(
                Triple("2nd", DRAGON_WINNER_FRAME_2ND, 0),
                Triple("1st", DRAGON_WINNER_FRAME_1ST, 1),
                Triple("3rd", DRAGON_WINNER_FRAME_3RD, 2)
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
                            DragonWinnerAvatarPlaceholder(
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
                                model = DRAGON_COIN_IMAGE,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                contentScale = ContentScale.Fit
                            )
                            Text(
                                text = if (showMe) formatShortAmount(winAmount) else "-",
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

@Composable
private fun DragonRulesOverlay(
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    Column(
        modifier = modifier
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            DragonGlassBackButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart)
            )
            Text(
                text = "Rules",
                modifier = Modifier.align(Alignment.Center),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            DragonRuleRow("1.", "At the start of each round , A,B and C will each be dealt a hand of 3 cards.")
            DragonRuleRow("2.", "If the dragon you bet on has the biggest hand, you will win a reward of ×2.95 of your bet amount.")
            DragonRuleRow("3.", "Hand Rankings:  Set > Pure Sequence > Colour > Sequence > Pair > High Card")
            DragonRuleRow("4.", "When the types are the same, compare the largest of the 3 cards.")
            DragonRuleRow("5.", "When the numbers are the same, compare the order of colour and size.")

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "6.",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.width(22.dp)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text("Spades", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text("♠", color = Color(0xFF8C8C96), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("> Hearts", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text("♥", color = Color(0xFFE53935), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("> Plums", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text("♣", color = Color(0xFF8C8C96), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("> Diamonds", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text("♦", color = Color(0xFFE53935), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun DragonRuleRow(number: String, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = number,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(22.dp)
        )
        Text(
            text = text,
            color = Color.White,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun DragonHistoryOverlay(
    history: List<String>,
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    Column(
        modifier = modifier
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            DragonGlassBackButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart)
            )
            Text(
                text = "History",
                modifier = Modifier.align(Alignment.Center),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF141414))
                .padding(start = 10.dp, end = 10.dp, top = 4.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.width(34.dp))
            DragonHistoryHeaderItem("A", DRAGON_A_IMAGE, Modifier.weight(1f))
            DragonHistoryHeaderItem("B", DRAGON_B_IMAGE, Modifier.weight(1f))
            DragonHistoryHeaderItem("C", DRAGON_C_IMAGE, Modifier.weight(1f))
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.Black)
        ) {
            itemsIndexed(history) { index, winner ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${index + 1}",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(34.dp)
                    )
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        DragonHistoryBadge(isWin = winner == "A")
                    }
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        DragonHistoryBadge(isWin = winner == "B")
                    }
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        DragonHistoryBadge(isWin = winner == "C")
                    }
                }
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    thickness = 0.7.dp,
                    color = Color.White.copy(alpha = 0.14f)
                )
            }
        }
    }
}

@Composable
private fun DragonHistoryHeaderItem(
    label: String,
    dragonImage: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.height(74.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 2.dp, top = 2.dp),
            style = TextStyle(
                color = Color(0xFFFFF3C4),
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Serif,
                shadow = Shadow(
                    color = Color(0xCC3B1800),
                    offset = Offset(2f, 3f),
                    blurRadius = 4f
                )
            )
        )
        AsyncImage(
            model = dragonImage,
            contentDescription = "Dragon $label",
            modifier = Modifier.size(70.dp),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun DragonHistoryBadge(isWin: Boolean) {
    val fillBrush = if (isWin) {
        Brush.verticalGradient(listOf(Color(0xFFFFD84D), Color(0xFFF3B116)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFF969696), Color(0xFF7D7D7D)))
    }
    val borderColor = if (isWin) Color(0xFFFFEC9E) else Color(0xFFD6D6D6)

    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(fillBrush)
            .border(2.dp, borderColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (isWin) "Win" else "Fail",
            color = Color.White,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun OverlappingThreeCardsTable(
    multiplier: String,
    cards: List<DragonPlayingCard>,
    flippedCount: Int
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .width(88.dp)
                .height(56.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            cards.take(3).forEachIndexed { idx, card ->
                FlippableDragonCard(
                    card = card,
                    isFlipped = idx < flippedCount,
                    modifier = Modifier
                        .offset(x = (idx * 22).dp)
                        .size(width = 44.dp, height = 56.dp)
                        .zIndex(idx.toFloat())
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = multiplier,
            color = Color(0xFFFFD699),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun DragonBetColumn(
    label: String,
    dragonImage: String,
    pot: Long,
    you: Long,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(82.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 2.dp, top = 2.dp),
                style = TextStyle(
                    color = Color(0xFFFFF3C4),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Serif,
                    shadow = Shadow(
                        color = Color(0xCC3B1800),
                        offset = Offset(2f, 3f),
                        blurRadius = 4f
                    )
                )
            )

            AsyncImage(
                model = dragonImage,
                contentDescription = "Dragon $label",
                modifier = Modifier.size(78.dp),
                contentScale = ContentScale.Fit
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(Color(0xD94B1A19))
                .border(1.dp, Color(0x55E5B34A), RoundedCornerShape(9.dp))
                .padding(bottom = 5.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Pot: ${formatShortAmount(pot)}",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "You: ${formatShortAmount(you)}",
                    color = Color(0xFFFFE082),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private fun formatShortAmount(amount: Long): String {
    return when {
        amount >= 1_000_000L -> {
            val v = amount / 1_000_000.0
            if (amount % 1_000_000L == 0L) "${amount / 1_000_000L}M"
            else String.format(Locale.US, "%.1fM", v)
        }
        amount >= 1_000L -> {
            val v = amount / 1_000.0
            if (amount % 1_000L == 0L) "${amount / 1_000L}K"
            else String.format(Locale.US, "%.1fK", v)
        }
        else -> amount.toString()
    }
}

@Composable
private fun DragonBalanceBar(balance: Long, modifier: Modifier = Modifier) {
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
                AsyncImage(
                    model = DRAGON_COIN_IMAGE,
                    contentDescription = "Coins",
                    modifier = Modifier.size(18.dp),
                    contentScale = ContentScale.Fit
                )
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
                text = if (balance >= 100_000_000L) formatShortAmount(balance) else balance.toString(),
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
private fun DragonRepeatButton(onClick: () -> Unit) {
    val outer = RoundedCornerShape(9.dp)
    val inner = RoundedCornerShape(7.dp)
    Box(
        Modifier.clip(outer).background(Color(0xFF472710).copy(alpha = .72f))
            .border(1.2.dp, Brush.verticalGradient(listOf(Color.White.copy(.94f), Color(0xFFEBDCD0).copy(.75f), Color.White.copy(.85f))), outer)
            .clickable(onClick = onClick).padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier.clip(inner)
                .background(Brush.verticalGradient(listOf(Color.White.copy(.52f), Color(0xFFCBB5A0).copy(.30f), Color(0xFF563218).copy(.68f), Color(0xFF6B4122).copy(.58f))))
                .border(.8.dp, Brush.verticalGradient(listOf(Color.White.copy(.85f), Color.White.copy(.28f), Color.White.copy(.52f))), inner)
                .padding(horizontal = 7.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Repeat",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                style = TextStyle(shadow = Shadow(Color.Black.copy(alpha = .55f), Offset(0f, 1.5f), 2.5f))
            )
        }
    }
}

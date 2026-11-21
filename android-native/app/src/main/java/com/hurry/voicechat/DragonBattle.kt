package com.hurry.voicechat

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.runtime.key
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
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
private const val DRAGON_WINNER_TAG_IMAGE =
    "${BASE_URL}public/winner_cutout.png"

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

private data class DragonBannerEvent(
    val id: Long,
    val text: String
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

private fun getDragonHandLabel(cards: List<DragonPlayingCard>): String {
    if (cards.size < 3) return "High Card"
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

    return when {
        isSet -> "Set"
        isFlush && isSeq -> "Pure Sequence"
        isFlush -> "Colour"
        isSeq -> "Sequence"
        isPair -> "Pair"
        else -> "High Card"
    }
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

    // Heavy punchy "DHAM" impact PCM buffer (22050 Hz mono 16-bit)
    private val dhamSampleRate = 22050
    private val dhamPcm: ShortArray by lazy {
        val count = (dhamSampleRate * 0.24).toInt()
        val data = ShortArray(count)
        var phase = 0.0
        for (i in 0 until count) {
            val t = i.toDouble() / dhamSampleRate
            val freq = 38.0 + 115.0 * exp(-t * 22.0)
            phase += 2.0 * PI * freq / dhamSampleRate
            val body = sin(phase) * exp(-t * 11.5)
            val snap = sin(2.0 * PI * 240.0 * t) * exp(-t * 55.0) * 0.45
            val sample = ((body + snap) * 0.92).coerceIn(-1.0, 1.0)
            data[i] = (sample * Short.MAX_VALUE).toInt().toShort()
        }
        data
    }

    suspend fun preload() = withContext(Dispatchers.IO) {
        dhamPcm
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

    fun playDham() {
        if (muted || released) return
        runCatching {
            val pcm = dhamPcm
            val byteSize = pcm.size * 2
            val track = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(dhamSampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(byteSize)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                AudioTrack(
                    android.media.AudioManager.STREAM_MUSIC,
                    dhamSampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    byteSize,
                    AudioTrack.MODE_STATIC
                )
            }
            track.write(pcm, 0, pcm.size)
            track.play()
            Thread.sleep(270L)
            runCatching { track.stop() }
            runCatching { track.release() }
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

/**
 * Stylized calligraphic "Dragon Battle" title matching the reference screenshot
 * (used on both Main Screen and Loading Page).
 */
@Composable
private fun DragonBattleTitle(
    height: Dp = 26.dp,
    modifier: Modifier = Modifier
) {
    val width = height * 4.56f
    Canvas(modifier = modifier.size(width = width, height = height)) {
        val sx = size.width / 310f
        val sy = size.height / 68f
        fun x(v: Float) = v * sx
        fun y(v: Float) = v * sy

        val titlePath = Path().apply {
            fillType = PathFillType.EvenOdd

            // D - outer (calligraphic swash)
            moveTo(x(4f), y(15f))
            quadraticTo(x(9f), y(9.5f), x(17f), y(9f))
            cubicTo(x(31f), y(8.5f), x(42f), y(16f), x(42f), y(28.5f))
            cubicTo(x(42f), y(41.5f), x(31f), y(48.5f), x(17f), y(48.5f))
            lineTo(x(7f), y(48.5f))
            quadraticTo(x(9.5f), y(44.5f), x(11.5f), y(41.5f))
            lineTo(x(13f), y(16.5f))
            quadraticTo(x(8f), y(17.5f), x(4f), y(15f))
            close()

            // D - counter
            moveTo(x(18.5f), y(16f))
            cubicTo(x(28.5f), y(16f), x(35f), y(20.5f), x(35f), y(28.5f))
            cubicTo(x(35f), y(37.5f), x(28f), y(42.5f), x(18f), y(42.5f))
            lineTo(x(17f), y(42.5f))
            close()

            // r
            moveTo(x(45f), y(27f))
            quadraticTo(x(48.5f), y(23f), x(52f), y(23f))
            lineTo(x(52f), y(29f))
            quadraticTo(x(56f), y(23f), x(61f), y(23f))
            quadraticTo(x(64.5f), y(24f), x(63.5f), y(28.5f))
            quadraticTo(x(62f), y(31f), x(59f), y(30.5f))
            quadraticTo(x(56f), y(29.5f), x(52.5f), y(33.5f))
            lineTo(x(52.5f), y(44f))
            quadraticTo(x(50f), y(48f), x(47f), y(47.5f))
            lineTo(x(47f), y(29f))
            close()

            // a - outer
            moveTo(x(74f), y(23f))
            quadraticTo(x(79f), y(22.5f), x(83f), y(26f))
            lineTo(x(83f), y(42.5f))
            lineTo(x(86.5f), y(44.5f))
            quadraticTo(x(83f), y(48f), x(79.5f), y(47.5f))
            lineTo(x(77.5f), y(44.5f))
            quadraticTo(x(72f), y(48.5f), x(67f), y(44.5f))
            cubicTo(x(64f), y(40.5f), x(64.5f), y(30f), x(68.5f), y(25.5f))
            quadraticTo(x(70.5f), y(23.5f), x(74f), y(23f))
            close()

            // a - counter
            moveTo(x(77.5f), y(29f))
            quadraticTo(x(72.5f), y(27.5f), x(70.8f), y(33f))
            cubicTo(x(69.8f), y(37f), x(70.8f), y(41.5f), x(74f), y(42.5f))
            quadraticTo(x(76.5f), y(42.5f), x(77.5f), y(40.5f))
            close()

            // g - outer (with curved descending tail)
            moveTo(x(98f), y(23f))
            quadraticTo(x(103f), y(22.5f), x(107f), y(26f))
            lineTo(x(107f), y(44f))
            cubicTo(x(107f), y(54.5f), x(100.5f), y(61f), x(90f), y(60.5f))
            quadraticTo(x(87f), y(59.5f), x(87.5f), y(56f))
            quadraticTo(x(89f), y(53.5f), x(92f), y(54.5f))
            cubicTo(x(97f), y(55.5f), x(101f), y(51.5f), x(101.5f), y(45f))
            quadraticTo(x(96f), y(48.5f), x(91.5f), y(44.5f))
            cubicTo(x(88.5f), y(40.5f), x(88.5f), y(30f), x(92.5f), y(25.5f))
            quadraticTo(x(94.5f), y(23.5f), x(98f), y(23f))
            close()

            // g - counter
            moveTo(x(101.5f), y(29f))
            quadraticTo(x(96.5f), y(27.5f), x(94.8f), y(33f))
            cubicTo(x(93.8f), y(37f), x(94.8f), y(41.5f), x(98f), y(42.5f))
            quadraticTo(x(100.5f), y(42.5f), x(101.5f), y(40.5f))
            close()

            // o - outer
            moveTo(x(122.5f), y(23f))
            cubicTo(x(129.5f), y(23f), x(133f), y(29f), x(133f), y(35.5f))
            cubicTo(x(133f), y(42.5f), x(129f), y(48f), x(122.5f), y(48f))
            cubicTo(x(115.5f), y(48f), x(112f), y(42.5f), x(112f), y(35.5f))
            cubicTo(x(112f), y(29f), x(115.5f), y(23f), x(122.5f), y(23f))
            close()

            // o - counter
            moveTo(x(122.5f), y(28.5f))
            cubicTo(x(119f), y(28.5f), x(117.5f), y(31.5f), x(117.5f), y(35.5f))
            cubicTo(x(117.5f), y(39.5f), x(119f), y(42.5f), x(122.5f), y(42.5f))
            cubicTo(x(126f), y(42.5f), x(127.5f), y(39.5f), x(127.5f), y(35.5f))
            cubicTo(x(127.5f), y(31.5f), x(126f), y(28.5f), x(122.5f), y(28.5f))
            close()

            // n
            moveTo(x(136f), y(27f))
            quadraticTo(x(139.5f), y(23f), x(143f), y(23f))
            lineTo(x(143f), y(28f))
            quadraticTo(x(147.5f), y(22.5f), x(153f), y(23.5f))
            cubicTo(x(157f), y(24.5f), x(158.5f), y(28f), x(158.5f), y(33.5f))
            lineTo(x(158.5f), y(43f))
            lineTo(x(161.5f), y(44.5f))
            quadraticTo(x(158f), y(48f), x(155f), y(47.5f))
            quadraticTo(x(153f), y(46f), x(153f), y(42.5f))
            lineTo(x(153f), y(33.5f))
            cubicTo(x(153f), y(30f), x(151.5f), y(28.5f), x(149f), y(28.5f))
            quadraticTo(x(145.5f), y(28.5f), x(143.5f), y(32f))
            lineTo(x(143.5f), y(44f))
            quadraticTo(x(141f), y(48f), x(138f), y(47.5f))
            lineTo(x(138f), y(29f))
            close()

            // B - outer
            moveTo(x(172f), y(15f))
            quadraticTo(x(176f), y(10f), x(185f), y(9.5f))
            cubicTo(x(195f), y(9.5f), x(200.5f), y(13.5f), x(200.5f), y(19.5f))
            cubicTo(x(200.5f), y(23.5f), x(198f), y(26.2f), x(194f), y(27.5f))
            cubicTo(x(199.5f), y(28.8f), x(203.5f), y(32.5f), x(203.5f), y(38f))
            cubicTo(x(203.5f), y(44.5f), x(197f), y(48f), x(187f), y(48f))
            lineTo(x(174f), y(48f))
            quadraticTo(x(176f), y(44.5f), x(177.5f), y(41.5f))
            lineTo(x(178.5f), y(16.5f))
            quadraticTo(x(175f), y(17f), x(172f), y(15f))
            close()

            // B - upper counter
            moveTo(x(184.5f), y(15f))
            cubicTo(x(191.5f), y(15f), x(194.2f), y(17f), x(194.2f), y(20.2f))
            cubicTo(x(194.2f), y(23.5f), x(191f), y(25.2f), x(184.2f), y(25.2f))
            close()

            // B - lower counter
            moveTo(x(184.2f), y(30.2f))
            cubicTo(x(192.5f), y(30.2f), x(196.8f), y(32.5f), x(196.8f), y(36.8f))
            cubicTo(x(196.8f), y(40.8f), x(192.5f), y(42.5f), x(184f), y(42.5f))
            close()

            // a - outer
            moveTo(x(216f), y(23f))
            quadraticTo(x(221f), y(22.5f), x(225f), y(26f))
            lineTo(x(225f), y(42.5f))
            lineTo(x(228.5f), y(44.5f))
            quadraticTo(x(225f), y(48f), x(221.5f), y(47.5f))
            lineTo(x(219.5f), y(44.5f))
            quadraticTo(x(214f), y(48.5f), x(209f), y(44.5f))
            cubicTo(x(206f), y(40.5f), x(206.5f), y(30f), x(210.5f), y(25.5f))
            quadraticTo(x(212.5f), y(23.5f), x(216f), y(23f))
            close()

            // a - counter
            moveTo(x(219.5f), y(29f))
            quadraticTo(x(214.5f), y(27.5f), x(212.8f), y(33f))
            cubicTo(x(211.8f), y(37f), x(212.8f), y(41.5f), x(216f), y(42.5f))
            quadraticTo(x(218.5f), y(42.5f), x(219.5f), y(40.5f))
            close()

            // t1 - stem
            moveTo(x(238f), y(14f))
            quadraticTo(x(240f), y(17f), x(240f), y(20.5f))
            lineTo(x(240f), y(41f))
            quadraticTo(x(242f), y(43f), x(246.5f), y(42f))
            quadraticTo(x(242.5f), y(48f), x(238f), y(48f))
            quadraticTo(x(234f), y(47f), x(234.5f), y(41.5f))
            lineTo(x(234.5f), y(20.5f))
            close()

            // t2 - stem
            moveTo(x(255f), y(14f))
            quadraticTo(x(257f), y(17f), x(257f), y(20.5f))
            lineTo(x(257f), y(41f))
            quadraticTo(x(259f), y(43f), x(263.5f), y(42f))
            quadraticTo(x(259.5f), y(48f), x(255f), y(48f))
            quadraticTo(x(251f), y(47f), x(251.5f), y(41.5f))
            lineTo(x(251.5f), y(20.5f))
            close()

            // l
            moveTo(x(266.5f), y(14.5f))
            quadraticTo(x(270.5f), y(11f), x(274.5f), y(10f))
            lineTo(x(274.5f), y(42f))
            lineTo(x(278.5f), y(43.5f))
            quadraticTo(x(274.5f), y(48f), x(271f), y(47.5f))
            quadraticTo(x(268.5f), y(45.5f), x(269f), y(42f))
            lineTo(x(269f), y(17f))
            close()

            // e - outer
            moveTo(x(292f), y(23f))
            cubicTo(x(298.5f), y(23f), x(302.5f), y(27.5f), x(302f), y(34f))
            quadraticTo(x(301.8f), y(36.2f), x(299.5f), y(36.8f))
            lineTo(x(286f), y(37.5f))
            cubicTo(x(286.5f), y(41.5f), x(289.5f), y(43.5f), x(293.5f), y(43f))
            quadraticTo(x(297.5f), y(42.5f), x(300.5f), y(39.5f))
            quadraticTo(x(301.5f), y(42f), x(299.5f), y(45f))
            cubicTo(x(296.5f), y(48f), x(291f), y(48.5f), x(286.5f), y(46.5f))
            cubicTo(x(281.5f), y(44f), x(280f), y(38.5f), x(280.5f), y(33.5f))
            cubicTo(x(281f), y(27f), x(285.5f), y(23f), x(292f), y(23f))
            close()

            // e - counter
            moveTo(x(291.5f), y(27.8f))
            cubicTo(x(288.2f), y(27.8f), x(286.3f), y(30.2f), x(286f), y(33.5f))
            lineTo(x(296.5f), y(33f))
            cubicTo(x(296.5f), y(29.8f), x(294.6f), y(27.8f), x(291.5f), y(27.8f))
            close()
        }

        // Single sweeping crossbar uniting both 't' stems cleanly without EvenOdd holes
        val crossbarPath = Path().apply {
            moveTo(x(229.5f), y(26.5f))
            lineTo(x(263.5f), y(22.5f))
            lineTo(x(263f), y(27f))
            lineTo(x(230f), y(30.5f))
            close()
        }

        val unifiedPath = Path().apply {
            op(titlePath, crossbarPath, PathOperation.Union)
        }

        val shadowOffsetPx = 1.8.dp.toPx()
        withTransform({ translate(left = 0f, top = shadowOffsetPx) }) {
            drawPath(path = unifiedPath, color = Color.Black.copy(alpha = 0.52f))
        }
        drawPath(path = unifiedPath, color = Color.White)
    }
}

@Composable
private fun DragonWinnerWhiteRays(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "winnerRays")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "winnerRaysRot"
    )
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "winnerRaysPulse"
    )

    Canvas(
        modifier = modifier.graphicsLayer {
            scaleX = pulse
            scaleY = pulse
        }
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension * 0.50f

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.92f),
                    Color(0xFFFFFCE0).copy(alpha = 0.55f),
                    Color.White.copy(alpha = 0.16f),
                    Color.Transparent
                ),
                center = center,
                radius = radius
            ),
            radius = radius,
            center = center
        )

        val rayCount = 12
        val halfSpreadDeg = 8.5f
        for (i in 0 until rayCount) {
            val baseAngle = rotation + i * (360f / rayCount)
            val a1 = Math.toRadians((baseAngle - halfSpreadDeg).toDouble())
            val a2 = Math.toRadians((baseAngle + halfSpreadDeg).toDouble())
            val rayPath = Path().apply {
                moveTo(center.x, center.y)
                lineTo(
                    center.x + (cos(a1) * radius).toFloat(),
                    center.y + (sin(a1) * radius).toFloat()
                )
                lineTo(
                    center.x + (cos(a2) * radius).toFloat(),
                    center.y + (sin(a2) * radius).toFloat()
                )
                close()
            }
            drawPath(
                path = rayPath,
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.85f),
                        Color.White.copy(alpha = 0.35f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius
                )
            )
        }
    }
}

@Composable
private fun DragonHandRankSlamBanner(
    label: String,
    modifier: Modifier = Modifier
) {
    val dropY = remember(label) { Animatable(-28f) }
    val scale = remember(label) { Animatable(1.65f) }
    val alpha = remember(label) { Animatable(0f) }

    LaunchedEffect(label) {
        launch { alpha.animateTo(1f, tween(90)) }
        launch { dropY.animateTo(0f, tween(170, easing = FastOutSlowInEasing)) }
        scale.animateTo(0.92f, tween(150, easing = FastOutSlowInEasing))
        scale.animateTo(1.04f, tween(80))
        scale.animateTo(1f, tween(70))
    }

    val ribbonBrush = Brush.horizontalGradient(
        colors = listOf(
            Color(0x20000000),
            Color(0xCC0D0A08),
            Color(0xD90D0A08),
            Color(0xCC0D0A08),
            Color(0x20000000)
        )
    )
    val goldEdgeBrush = Brush.horizontalGradient(
        colors = listOf(
            Color.Transparent,
            Color(0xCCFFE596),
            Color(0xFFFFF4C2),
            Color(0xCCFFE596),
            Color.Transparent
        )
    )
    val textGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFFFCE0),
            Color(0xFFFFE484),
            Color(0xFFE5A43B)
        )
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(22.dp)
            .graphicsLayer {
                translationY = dropY.value
                scaleX = scale.value
                scaleY = scale.value
                this.alpha = alpha.value
            }
            .background(ribbonBrush),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(0.9.dp)
                .background(goldEdgeBrush)
        )
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(0.9.dp)
                .background(goldEdgeBrush)
        )
        val fontSize = when {
            label.length >= 12 -> 10.sp
            label.length >= 8 -> 11.5.sp
            else -> 13.5.sp
        }
        Text(
            text = label,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            style = TextStyle(
                color = Color(0xCC2A1200),
                fontSize = fontSize,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
                shadow = Shadow(Color.Black, Offset(0f, 1.5f), 3f)
            ),
            modifier = Modifier.offset(y = 0.8.dp)
        )
        Text(
            text = label,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            style = TextStyle(
                brush = textGradient,
                fontSize = fontSize,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic
            )
        )
    }
}

@Composable
private fun DragonPhaseBanner(
    event: DragonBannerEvent,
    onFinished: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(62.dp)
            .clipToBounds(),
        contentAlignment = Alignment.Center
    ) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
        val slideOffset = remember(event.id) { Animatable(-widthPx) }

        LaunchedEffect(event.id) {
            slideOffset.snapTo(-widthPx)
            slideOffset.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
            )
            delay(1500L)
            slideOffset.animateTo(
                targetValue = widthPx,
                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
            )
            onFinished(event.id)
        }

        val ribbonBgBrush = Brush.horizontalGradient(
            colors = listOf(
                Color.Transparent,
                Color(0x904B1F04),
                Color(0xB8632C06),
                Color(0x904B1F04),
                Color.Transparent
            )
        )
        val borderGlowBrush = Brush.horizontalGradient(
            colors = listOf(
                Color.Transparent,
                Color(0xCCFFE5A0),
                Color(0xFFFFF5CC),
                Color(0xCCFFE5A0),
                Color.Transparent
            )
        )
        val textGradient = Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFFFBC8),
                Color(0xFFFFE14D),
                Color(0xFFFF9900),
                Color(0xFFE66A00)
            )
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .graphicsLayer {
                    translationX = slideOffset.value
                }
                .background(ribbonBgBrush),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(1.4.dp)
                    .background(borderGlowBrush)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(1.4.dp)
                    .background(borderGlowBrush)
            )

            Text(
                text = event.text,
                maxLines = 1,
                softWrap = false,
                style = TextStyle(
                    color = Color(0xCC4A1900),
                    fontSize = 31.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    letterSpacing = 0.5.sp,
                    shadow = Shadow(
                        color = Color(0xE6300E00),
                        offset = Offset(0f, 3.5f),
                        blurRadius = 6f
                    )
                ),
                modifier = Modifier.offset(y = 1.8.dp)
            )
            Text(
                text = event.text,
                maxLines = 1,
                softWrap = false,
                style = TextStyle(
                    brush = textGradient,
                    fontSize = 31.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    letterSpacing = 0.5.sp
                )
            )
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
    val suitColor = if (card.suit.isRed) Color(0xFFE53935) else Color(0xFF1F1F1F)
    val cardShape = RoundedCornerShape(3.dp)

    Box(
        modifier = modifier
            .clip(cardShape)
            .background(Color.White)
            .border(0.7.dp, Color(0xFFB5B5B5), cardShape)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 2.dp, top = 1.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = card.rankLabel,
                color = suitColor,
                fontSize = if (card.rankLabel.length > 1) 11.sp else 13.5.sp,
                lineHeight = 13.5.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                softWrap = false
            )
            DragonSuitVectorIcon(
                suit = card.suit,
                color = suitColor,
                modifier = Modifier.size(8.5.dp)
            )
        }

        DragonSuitVectorIcon(
            suit = card.suit,
            color = suitColor,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 1.5.dp, bottom = 2.dp)
                .size(20.dp)
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
        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
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
                    .clip(RoundedCornerShape(3.dp)),
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

        // Matching stylized Dragon Battle title on Loading Page
        DragonBattleTitle(
            height = 30.dp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp)
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

    var showRankA by remember { mutableStateOf(false) }
    var showRankB by remember { mutableStateOf(false) }
    var showRankC by remember { mutableStateOf(false) }
    var highlightedWinnerDragon by remember { mutableStateOf<String?>(null) }

    var activeBanner by remember { mutableStateOf<DragonBannerEvent?>(null) }
    var bannerSeq by remember { mutableLongStateOf(0L) }

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

    fun showBanner(text: String) {
        bannerSeq++
        activeBanner = DragonBannerEvent(bannerSeq, text)
    }

    DisposableEffect(soundManager) {
        onDispose { soundManager.release() }
    }

    LaunchedEffect(Unit) {
        launch(Dispatchers.IO) { soundManager.preload() }
        while (true) {
            // 1. Start Betting -> Show "Start Betting !" banner (Left -> Center 1.5s -> Right) & 20s countdown
            isRevealing = false
            revealedCount = 0
            showRankA = false
            showRankB = false
            showRankC = false
            highlightedWinnerDragon = null
            winnerOpen = false
            potA = 0L; youA = 0L
            potB = 0L; youB = 0L
            potC = 0L; youC = 0L

            showBanner("Start Betting !")

            for (sec in 20 downTo 1) {
                timeLeft = sec
                delay(1000L)
            }

            // 2. Time khatam -> "Stop Betting !" banner (Left -> Center 1.5s -> Right)
            timeLeft = 0
            isRevealing = true
            showBanner("Stop Betting !")
            delay(2150L)

            // 3. Cards show honge -> "Show !" banner + flip 1 card every 1.5s (Group A -> B -> C)
            val dealt = dealDragonRoundCards()
            roundCards = dealt
            showBanner("Show !")

            for (cardStep in 1..9) {
                revealedCount = cardStep
                if (cardStep == 3 || cardStep == 6 || cardStep == 9) {
                    delay(430L)
                    when (cardStep) {
                        3 -> showRankA = true
                        6 -> showRankB = true
                        9 -> showRankC = true
                    }
                    launch(Dispatchers.IO) { soundManager.playDham() }
                    delay(1070L)
                } else {
                    delay(1500L)
                }
            }

            // 4. Show Winning Tag (winner_cutout.png) on top card group + White Shine Rays behind Dragon + update History (max 20)
            val winner = pickDragonWinner(dealt.first, dealt.second, dealt.third)
            winnerDragon = winner
            highlightedWinnerDragon = winner
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

            delay(1800L)
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
    val handLabelA = remember(cardsA) { getDragonHandLabel(cardsA) }
    val handLabelB = remember(cardsB) { getDragonHandLabel(cardsB) }
    val handLabelC = remember(cardsC) { getDragonHandLabel(cardsC) }

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
                // Top controls and centered stylized Dragon Battle title
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                ) {
                    Row(
                        modifier = Modifier.align(Alignment.CenterStart),
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

                    DragonBattleTitle(
                        height = 28.dp,
                        modifier = Modifier.align(Alignment.Center)
                    )

                    Row(
                        modifier = Modifier.align(Alignment.CenterEnd),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
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

                // 2. TOP 3 CARD TABLES (+ winner_cutout.png tag on winning group) + CENTER TIMER
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(5f),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        OverlappingThreeCardsTable(
                            multiplier = "2.92",
                            cards = cardsA,
                            flippedCount = flippedA,
                            handRankLabel = handLabelA,
                            showHandRank = showRankA,
                            isWinner = highlightedWinnerDragon == "A",
                            modifier = Modifier
                                .weight(1f)
                                .zIndex(if (highlightedWinnerDragon == "A") 12f else 0f)
                        )
                        OverlappingThreeCardsTable(
                            multiplier = "2.92",
                            cards = cardsB,
                            flippedCount = flippedB,
                            handRankLabel = handLabelB,
                            showHandRank = showRankB,
                            isWinner = highlightedWinnerDragon == "B",
                            modifier = Modifier
                                .weight(1f)
                                .zIndex(if (highlightedWinnerDragon == "B") 12f else 0f)
                        )
                        OverlappingThreeCardsTable(
                            multiplier = "2.92",
                            cards = cardsC,
                            flippedCount = flippedC,
                            handRankLabel = handLabelC,
                            showHandRank = showRankC,
                            isWinner = highlightedWinnerDragon == "C",
                            modifier = Modifier
                                .weight(1f)
                                .zIndex(if (highlightedWinnerDragon == "C") 12f else 0f)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .offset(y = (-5).dp)
                            .size(30.dp)
                            .zIndex(15f),
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
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            style = TextStyle(
                                color = Color.White,
                                fontSize = 11.5.sp,
                                lineHeight = 11.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                shadow = Shadow(
                                    color = Color(0xCC000000),
                                    offset = Offset(0.8f, 1.2f),
                                    blurRadius = 2.5f
                                )
                            )
                        )
                    }
                }

                // 3. DRAGONS (A, B, C) + BETTING BOXES (Winning Dragon has bright White Shine Rays behind it)
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
                        isWinner = highlightedWinnerDragon == "A",
                        modifier = Modifier.weight(1f),
                        onClick = { placeBet("A") }
                    )
                    DragonBetColumn(
                        label = "B",
                        dragonImage = DRAGON_B_IMAGE,
                        pot = potB,
                        you = youB,
                        isWinner = highlightedWinnerDragon == "B",
                        modifier = Modifier.weight(1f),
                        onClick = { placeBet("B") }
                    )
                    DragonBetColumn(
                        label = "C",
                        dragonImage = DRAGON_C_IMAGE,
                        pot = potC,
                        you = youC,
                        isWinner = highlightedWinnerDragon == "C",
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

            // Center Phase Banner ("Start Betting !", "Stop Betting !", "Show !")
            activeBanner?.let { banner ->
                key(banner.id) {
                    DragonPhaseBanner(
                        event = banner,
                        onFinished = { finishedId ->
                            if (activeBanner?.id == finishedId) {
                                activeBanner = null
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.Center)
                            .zIndex(30f)
                    )
                }
            }

            // Exact same Wild Party height Rules Overlay (on ? click)
            if (rulesOpen) {
                DragonRulesOverlay(
                    modifier = Modifier.matchParentSize().zIndex(40f),
                    onBack = { rulesOpen = false }
                )
            }

            // Exact same Wild Party height History Overlay (on 3-line icon click)
            if (historyOpen) {
                DragonHistoryOverlay(
                    history = historyList.take(20),
                    modifier = Modifier.matchParentSize().zIndex(40f),
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
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.width(22.dp)
                )
                Text(
                    text = buildAnnotatedString {
                        append("Spades ")
                        withStyle(SpanStyle(color = Color(0xFF9E9EA8), fontWeight = FontWeight.Bold)) { append("♠") }
                        append(" > Hearts ")
                        withStyle(SpanStyle(color = Color(0xFFE53935), fontWeight = FontWeight.Bold)) { append("♥") }
                        append(" > Plums ")
                        withStyle(SpanStyle(color = Color(0xFF9E9EA8), fontWeight = FontWeight.Bold)) { append("♣") }
                        append(" > Diamonds ")
                        withStyle(SpanStyle(color = Color(0xFFE53935), fontWeight = FontWeight.Bold)) { append("♦") }
                    },
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.weight(1f)
                )
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
    val maxTwentyHistory = remember(history) { history.take(20) }
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
            itemsIndexed(maxTwentyHistory) { index, winner ->
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
    flippedCount: Int,
    handRankLabel: String,
    showHandRank: Boolean,
    isWinner: Boolean,
    modifier: Modifier = Modifier
) {
    val trayShape = RoundedCornerShape(6.dp)
    val tabShape = RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp)
    val trayBg = Color(0xE62E180B)
    val trayBorder = if (isWinner) Color(0xFFFFE58F) else Color(0xAA9E6B38)
    val tagScale by animateFloatAsState(
        targetValue = if (isWinner) 1f else 0.7f,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "winnerTagScale"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Outer unclipped Box so winner_cutout.png sits naturally over the top-right corner
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(trayShape)
                    .background(trayBg)
                    .border(if (isWinner) 1.4.dp else 1.1.dp, trayBorder, trayShape)
                    .padding(2.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(1.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    cards.take(3).forEachIndexed { idx, card ->
                        FlippableDragonCard(
                            card = card,
                            isFlipped = idx < flippedCount,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }

                if (showHandRank) {
                    DragonHandRankSlamBanner(
                        label = handRankLabel,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 3.dp)
                            .zIndex(5f)
                    )
                }
            }

            // Winning tag (winner_cutout.png) on top-right corner — NO rotation applied
            if (isWinner) {
                AsyncImage(
                    model = DRAGON_WINNER_TAG_IMAGE,
                    contentDescription = "Winner Tag",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 8.dp, y = (-12).dp)
                        .size(48.dp)
                        .zIndex(20f)
                        .graphicsLayer {
                            scaleX = tagScale
                            scaleY = tagScale
                        },
                    contentScale = ContentScale.Fit
                )
            }
        }

        // Attached dark brown 2.92 multiplier tab
        Box(
            modifier = Modifier
                .offset(y = (-1).dp)
                .clip(tabShape)
                .background(Color(0xE634190C))
                .border(0.8.dp, Color(0x888E5B2D), tabShape)
                .padding(horizontal = 12.dp, vertical = 1.5.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = multiplier,
                color = Color(0xFFF6AEAE),
                fontSize = 10.sp,
                lineHeight = 11.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun DragonBetColumn(
    label: String,
    dragonImage: String,
    pot: Long,
    you: Long,
    isWinner: Boolean,
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
                .height(84.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isWinner) {
                DragonWinnerWhiteRays(
                    modifier = Modifier
                        .size(110.dp)
                        .zIndex(0f)
                )
            }

            Text(
                text = label,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 2.dp, top = 2.dp)
                    .zIndex(2f),
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
                modifier = Modifier
                    .size(78.dp)
                    .zIndex(1f),
                contentScale = ContentScale.Fit
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(Color(0xD94B1A19))
                .border(
                    width = if (isWinner) 1.6.dp else 1.dp,
                    color = if (isWinner) Color(0xFFFFF0A6) else Color(0x55E5B34A),
                    shape = RoundedCornerShape(9.dp)
                )
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

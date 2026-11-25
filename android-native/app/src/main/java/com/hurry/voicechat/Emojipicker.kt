package com.hurry.voicechat

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import android.view.ViewGroup
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

private const val GITHUB_PUBLIC_BASE =
    "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/"
private const val SEAT_BANNER_BASE_IMAGE = "${GITHUB_PUBLIC_BASE}IMG_20261121_210809.png"
private const val WELCOME_BANNER_IMAGE = "${GITHUB_PUBLIC_BASE}IMG_20261121_210639.png"
private const val THANKS_BANNER_IMAGE = "${GITHUB_PUBLIC_BASE}IMG_20261121_210722.png"
private const val GIFT_ME_BANNER_IMAGE = "${GITHUB_PUBLIC_BASE}IMG_20261121_210747.png"

data class EmojiSticker(
    val id: String,
    val name: String,
    val src: String,
    val videoUrl: String? = null,
    // Picker-owned scale for the animation rendered over a room seat.
    val seatScale: Float = 1.1f
)

// Thread-safe, memory-bounded cache: emoji bitmaps must not grow without limit
// while the picker is opened repeatedly or URLs are loaded concurrently.
val staticBitmapCache = object : LruCache<String, Bitmap>(12 * 1024) {
    override fun sizeOf(key: String, value: Bitmap): Int = maxOf(1, value.byteCount / 1024)
}

// Yeh Custom Image loader hai jo 100% GIF ko play hone se rokega
@Composable
fun CustomStaticImage(url: String, contentDescription: String, modifier: Modifier) {
    var bitmap by remember { mutableStateOf(staticBitmapCache[url]) }

    LaunchedEffect(url) {
        if (bitmap == null) {
            val loadedBitmap = withContext(Dispatchers.IO) {
                try {
                    // Persist sticker bytes so reopening the picker does not download them again.
                    val cacheDir = File(context.cacheDir, "emoji-sticker-images").apply { mkdirs() }
                    val cacheName = java.security.MessageDigest.getInstance("SHA-256")
                        .digest(url.toByteArray(Charsets.UTF_8))
                        .joinToString("") { "%02x".format(it) }
                    val cachedFile = File(cacheDir, cacheName)
                    if (!cachedFile.isFile || cachedFile.length() == 0L) {
                        val tempFile = File(cacheDir, cacheName + ".tmp")
                        try {
                            val connection = URL(url).openConnection().apply {
                                connectTimeout = 7_000
                                readTimeout = 9_000
                                useCaches = true
                            }
                            connection.getInputStream().use { input ->
                                tempFile.outputStream().buffered().use { output -> input.copyTo(output) }
                            }
                            if (tempFile.length() > 0L) {
                                if (!tempFile.renameTo(cachedFile)) {
                                    tempFile.copyTo(cachedFile, overwrite = true)
                                    tempFile.delete()
                                }
                            }
                        } finally {
                            if (tempFile.exists()) tempFile.delete()
                        }
                    }
                    // Preserve the existing static-first-frame rendering for GIF stickers.
                    val decoded = if (cachedFile.isFile && cachedFile.length() > 0L) {
                        BitmapFactory.decodeFile(cachedFile.absolutePath)
                    } else null
                    if (decoded != null) staticBitmapCache.put(url, decoded)
                    decoded
                } catch (_: java.io.IOException) {
                    null
                } catch (_: SecurityException) {
                    null
                } catch (_: java.security.NoSuchAlgorithmException) {
                    null
                }
            }
            if (loadedBitmap != null) bitmap = loadedBitmap
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Fit
        )
    } else {
        Box(modifier = modifier)
    }
}

@Composable
fun EmojiPicker(onClose: () -> Unit = {}, onSelectEmoji: (EmojiSticker) -> Unit = {}) {
    var activeTab by remember { mutableStateOf("emojis") }

    val stickers = listOf(
        EmojiSticker("gif_512", "Emoji", "${GITHUB_PUBLIC_BASE}512.gif", null, 1.1f),
        EmojiSticker("gif_1", "Emoji 1", "${GITHUB_PUBLIC_BASE}512%20(1).gif", null, 1.1f),
        EmojiSticker("sad", "Sad", "${GITHUB_PUBLIC_BASE}512%20(6).gif", null, 1.1f),
        EmojiSticker("gif_2", "Emoji 2", "${GITHUB_PUBLIC_BASE}512%20(2).gif", null, 1.1f),
        EmojiSticker("gif_3", "Emoji 3", "${GITHUB_PUBLIC_BASE}512%20(3).gif", null, 1.1f),
        EmojiSticker("pleading", "Pleading", "${GITHUB_PUBLIC_BASE}512%20(4).gif", null, 1.1f),
        EmojiSticker("hug", "Hug", "${GITHUB_PUBLIC_BASE}512%20(8).gif", null, 1.1f),
        EmojiSticker("rolling", "Rolling", "${GITHUB_PUBLIC_BASE}512%20(10).gif", null, 1.1f),
        EmojiSticker("unamused", "Unamused", "${GITHUB_PUBLIC_BASE}512%20(11).gif", null, 1.1f),
        EmojiSticker("irritating", "Irritating", "${GITHUB_PUBLIC_BASE}512%20(12).gif", null, 1.1f),
        EmojiSticker("smart", "Smart", "${GITHUB_PUBLIC_BASE}512%20(13).gif", null, 1.1f),
        EmojiSticker("gif_14", "Emoji 14", "${GITHUB_PUBLIC_BASE}512%20(14).gif", null, 1.1f),
        EmojiSticker("loving", "Loving", "${GITHUB_PUBLIC_BASE}512%20(15).gif", null, 1.1f),
        EmojiSticker("party", "Party", "${GITHUB_PUBLIC_BASE}512%20(16).gif", null, 1.1f)
    )

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Box(Modifier.fillMaxSize().clickable(onClick = onClose))

        Box(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(.42f)
                .background(Color.Black, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
        ) {

            // Border waali photo normal hi rahegi
            AsyncImage(
                model = "${GITHUB_PUBLIC_BASE}file_0000000022f481fab7ddec2efb9449f9.png",
                contentDescription = "Top Border",
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-10).dp)
                    .wrapContentHeight(align = Alignment.Top)
                    .align(Alignment.TopCenter),
                contentScale = ContentScale.FillWidth
            )

            Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Tab("Emojis", activeTab == "emojis") { activeTab = "emojis" }
                    Spacer(Modifier.width(32.dp))
                    Tab("Premium", activeTab == "premium") { activeTab = "premium" }
                }

                if (activeTab == "emojis") {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(start = 2.dp, end = 2.dp, top = 12.dp, bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(stickers) { s ->
                            val isBigEmoji = s.id in listOf("welcome", "thanks", "gift_me")
                            val imageSize = if (isBigEmoji) 60.dp else 48.dp

                            Column(
                                Modifier.fillMaxWidth().clickable {
                                    onSelectEmoji(s)
                                    onClose()
                                },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CustomStaticImage(
                                    url = s.src,
                                    contentDescription = s.name,
                                    modifier = Modifier.size(imageSize)
                                )
                                Text(
                                    s.name.trim(),
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = .90f),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                } else {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("Premium Emojis Coming Soon", color = Color.White.copy(alpha = .70f), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun Tab(label: String, selected: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick)) {
        Text(label, color = if (selected) Color.White else Color.White.copy(alpha = .60f), fontSize = 14.sp)
        if (selected) {
            Box(
                Modifier
                    .padding(top = 4.dp)
                    .height(2.dp)
                    .width(42.dp)
                    .background(Color.White, RoundedCornerShape(2.dp))
            )
        }
    }
}

// =========================================================================================
// WELCOME / THANKS / GIFT ME SEAT BANNER OVERLAY + TUMHARI SEAT VIDEO LOGIC SAFE HAI
// =========================================================================================

private fun resolveSeatBannerUrl(src: String): String? = when {
    src.contains("IMG_20261028_183916.png") || src.contains("IMG_20261121_210639.png") -> WELCOME_BANNER_IMAGE
    src.contains("IMG_20261028_184100.png") || src.contains("IMG_20261121_210722.png") -> THANKS_BANNER_IMAGE
    src.contains("IMG_20261028_184037.png") || src.contains("IMG_20261121_210747.png") -> GIFT_ME_BANNER_IMAGE
    else -> null
}

@Composable
private fun AnimatedSeatBannerOverlay(
    baseImageUrl: String,
    bannerImageUrl: String,
    overlaySize: Dp
) {
    val density = LocalDensity.current
    val infiniteTransition = rememberInfiniteTransition(label = "seatBannerWave")

    // Slow & smooth: Left (-1f) se start -> halka upar hokar Right (+1f) -> fir same smooth reverse
    val progress by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bannerWaveProgress"
    )

    val maxShiftXPx = with(density) { (overlaySize * 0.085f).toPx() }
    val maxLiftYPx = with(density) { (overlaySize * 0.05f).toPx() }
    val baseDropYPx = with(density) { (overlaySize * 0.09f).toPx() }

    Box(
        modifier = Modifier
            .requiredSize(overlaySize)
            .aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        // Seat par base image
        AsyncImage(
            model = baseImageUrl,
            contentDescription = "Seat reaction base",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )

        // Base image ke bottom half se thoda niche + thoda bada + slow smooth hilta hua banner
        AsyncImage(
            model = bannerImageUrl,
            contentDescription = "Seat reaction banner",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(1f)
                .fillMaxHeight(0.56f)
                .graphicsLayer {
                    scaleX = 1.12f
                    scaleY = 1.12f
                    translationX = progress * maxShiftXPx
                    translationY = baseDropYPx - maxLiftYPx * (1f - progress * progress)
                    rotationZ = progress * 4.0f
                },
            contentScale = ContentScale.Fit
        )
    }
}

// Shared bounded disk cache for seat emoji MP4s. Reuses downloaded bytes
// across room re-entry and player recreation; evicts least-recently-used data.
internal object SeatEmojiVideoCache {
    @Volatile private var instance: SimpleCache? = null

    fun get(context: android.content.Context): SimpleCache =
        instance ?: synchronized(this) {
            instance ?: SimpleCache(
                File(context.cacheDir, "seat-emoji-video-cache"),
                LeastRecentlyUsedCacheEvictor(100L * 1024L * 1024L),
                StandaloneDatabaseProvider(context.applicationContext)
            ).also { instance = it }
        }
}

// The picker owns seat emoji sizing. Seat reactions render the banner overlay or original image/GIF source.
@Composable
fun SeatEmojiOverlay(
    src: String,
    seatSize: Dp,
    scale: Float = 1.1f,
    onVideoFinished: () -> Unit = {}
) {
    val overlaySize = (seatSize * scale.coerceIn(1.1f, 3.5f)).coerceAtLeast(24.dp)
    val bannerUrl = remember(src) { resolveSeatBannerUrl(src) }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when {
            bannerUrl != null -> {
                AnimatedSeatBannerOverlay(
                    baseImageUrl = SEAT_BANNER_BASE_IMAGE,
                    bannerImageUrl = bannerUrl,
                    overlaySize = overlaySize
                )
            }
            src.substringBefore("?").substringBefore("#").lowercase().endsWith(".mp4") -> {
                TransparentSeatVideoPlayer(
                    videoUrl = src,
                    modifier = Modifier
                        .requiredSize(overlaySize)
                        .aspectRatio(1f),
                    onVideoFinished = onVideoFinished
                )
            }
            else -> {
                AsyncImage(
                    model = src,
                    contentDescription = "Seat emoji",
                    modifier = Modifier
                        .requiredSize(overlaySize)
                        .aspectRatio(1f),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

@Composable
fun TransparentSeatVideoPlayer(
    videoUrl: String,
    modifier: Modifier = Modifier,
    onVideoFinished: () -> Unit = {}
) {
    val context = LocalContext.current
    val latestOnFinished by rememberUpdatedState(onVideoFinished)
    // Keep the original chroma-key renderer: green pixels are removed by
    // ChromaKeyVideoView's GLES shader. Only the video source is disk-cached.
    val player = remember(context, videoUrl) {
        val appContext = context.applicationContext
        val httpFactory = DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(10_000)
            .setReadTimeoutMs(15_000)
            .setAllowCrossProtocolRedirects(true)
        val upstreamFactory = DefaultDataSource.Factory(appContext, httpFactory)
        val cacheFactory = CacheDataSource.Factory()
            .setCache(SeatEmojiVideoCache.get(appContext))
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        ExoPlayer.Builder(appContext)
            .setMediaSourceFactory(DefaultMediaSourceFactory(cacheFactory))
            .build().apply {
                setMediaItem(MediaItem.fromUri(videoUrl))
                repeatMode = Player.REPEAT_MODE_OFF
                playWhenReady = true
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_ENDED) latestOnFinished()
                    }

                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    }
                })
                prepare()
            }
    }

    val chromaView = remember(context, player, videoUrl) {
        ChromaKeyVideoView(
            context,
            onVideoSurfaceReady = { surface -> player.setVideoSurface(surface) },
            onFirstFrameAvailable = {}
        ).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    // Keep the transparent GL surface attached at its real square bounds.
    // Moving/fading a SurfaceView before its first frame can leave a black surface.
    AndroidView(
        modifier = modifier,
        factory = { chromaView }
    )

    DisposableEffect(player, chromaView) {
        onDispose {
            player.clearVideoSurface()
            player.stop()
            player.clearMediaItems()
            player.release()
            chromaView.release()
        }
    }
}

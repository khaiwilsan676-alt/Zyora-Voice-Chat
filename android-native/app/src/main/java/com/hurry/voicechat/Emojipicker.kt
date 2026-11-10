package com.hurry.voicechat

import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.util.LruCache
import android.graphics.PixelFormat
import android.view.TextureView
import android.view.ViewGroup
import android.widget.FrameLayout
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

data class EmojiSticker(val id: String, val name: String, val src: String, val videoUrl: String? = null)

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
                    val connection = URL(url).openConnection().apply {
                        connectTimeout = 7_000
                        readTimeout = 9_000
                        useCaches = true
                    }
                    // Keep the existing static-first-frame behavior for GIF stickers.
                    val decoded = connection.getInputStream().use { input ->
                        BitmapFactory.decodeStream(input)
                    }
                    if (decoded != null) staticBitmapCache.put(url, decoded)
                    decoded
                } catch (_: java.io.IOException) {
                    null
                } catch (_: SecurityException) {
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
        EmojiSticker("welcome","Welcome","https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/IMG_20261028_183916.png", "https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/VID_20261028_105159_978_bsl.mp4"),
        EmojiSticker("thanks","Thanks","https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/IMG_20261028_184100.png", "https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/VID_20261028_105329_293_bsl.mp4"),
        EmojiSticker("gift_me","Gift me","https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/IMG_20261028_184037.png", "https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/VID_20261028_105304_745_bsl.mp4"),
        EmojiSticker("kiss_r","Kiss-R","https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/IMG_20261028_184130.png", "https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/VID_20261028_104846_084_bsl.mp4"),
        EmojiSticker("kiss_l","Kiss-L","https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/IMG_20261028_184200.png", "https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/VID_20261028_104823_659_bsl.mp4"),
        EmojiSticker("punch_r","Punch-R","https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/IMG_20261028_184240.png", "https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/VID_20261028_105414_962_bsl.mp4"),
        EmojiSticker("punch_l","Punch-L","https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/IMG_20261028_184300.png", "https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/VID_20261028_105348_731_bsl.mp4"),
        
        EmojiSticker("laugh","Laugh","https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/512.gif"),
        EmojiSticker("sad","Sad","https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/512%20(6).gif"),
        EmojiSticker("love","Sleep","https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/512%20(3).gif"),
        EmojiSticker("thinking","Thinking","https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/512%20(2).gif"),
        EmojiSticker("party","Party","https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/512%20(16).gif"),
        EmojiSticker("loving","Loving","https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/512%20(15).gif"),
        EmojiSticker("smart","Smart","https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/512%20(13).gif"),
        EmojiSticker("irritating","Irritating","https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/512%20(12).gif"),
        EmojiSticker("rolling","Rolling","https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/512%20(10).gif"),
        EmojiSticker("unamused","Unamused","https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/512%20(11).gif"),
        EmojiSticker("pleading","Pleading","https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/512%20(4).gif"),
        EmojiSticker("hug","Hug","https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/512%20(8).gif"),
        EmojiSticker("Angery"," Angery","https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/512%20(1).gif")
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
                model = "https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/file_0000000022f481fab7ddec2efb9449f9.png",
                contentDescription = "Top Border",
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-10).dp)
                    .wrapContentHeight(align = Alignment.Top)
                    .align(Alignment.TopCenter), 
                contentScale = ContentScale.FillWidth 
            )

            Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
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
                            val isBigEmoji = s.id in listOf("welcome", "thanks", "gift_me", "punch_l", "punch_r")
                            var imageSize = if (isBigEmoji) 60.dp else 48.dp 
                            var imageOffsetY = 0.dp
                            var textOffsetY = 0.dp

                            // YAHAN CONDITION LAGAI HAI ADJUSTMENTS KE LIYE:
                            when (s.id) {
                                "kiss_r" -> {
                                    imageOffsetY = 4.dp // 2% niche
                                    // BSS YAHAN CHANGE KIYA HAI TEXT KO NEECHE KARNE K LIYE TAAKI 'Gift me' SE ALIGN HO JAYE
                                    textOffsetY = 12.dp  
                                }
                                "punch_l", "punch_r" -> {
                                    imageSize = 58.dp      // 1% chhota
                                    imageOffsetY = (-2).dp // 1% upar
                                    // BSS YAHAN CHANGE KIYA HAI TEXT KO OUPER KARNE K LIYE
                                    textOffsetY = (-10).dp 
                                }
                            }

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
                                    modifier = Modifier
                                        .size(imageSize)
                                        .offset(y = imageOffsetY) // Image ki position wahi ki wahi rahegi
                                )
                                Text(
                                    s.name.trim(), 
                                    fontSize = 11.sp, 
                                    color = Color.White.copy(alpha = .90f), 
                                    maxLines = 1,
                                    modifier = Modifier.offset(y = textOffsetY) // Text upar/neeche yahan se ho raha hai bina image hilaye
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
            Box(Modifier.padding(top = 4.dp).height(2.dp).width(42.dp).background(Color.White, RoundedCornerShape(2.dp)))
        }
    }
}

// =========================================================================================
// TUMHARI SEAT VIDEO LOGIC EKDAM WAHI HAI, KOI CHANGE NAHI
// =========================================================================================

// Shared bounded disk cache for seat emoji MP4s. Reuses downloaded bytes
// across room re-entry and player recreation; evicts least-recently-used data.
private object SeatEmojiVideoCache {
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

@Composable
fun TransparentSeatVideoPlayer(
    videoUrl: String,
    modifier: Modifier = Modifier,
    onVideoFinished: () -> Unit = {}
) {
    val context = LocalContext.current
    val latestOnFinished by rememberUpdatedState(onVideoFinished)
    var firstFrameReady by remember(videoUrl) { mutableStateOf(false) }

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
                    override fun onRenderedFirstFrame() {
                        firstFrameReady = true
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_ENDED) latestOnFinished()
                    }

                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        // Never expose PlayerView's black shutter/error surface.
                        firstFrameReady = false
                    }
                })
                prepare()
            }
    }

    val playerView = remember(context, player) {
        PlayerView(context).apply {
            useController = false
            setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            setKeepContentOnPlayerReset(false)
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            // TextureView avoids the black SurfaceView layer/composition issue
            // seen in Compose room-seat overlays on affected Android devices.
            setVideoTextureView(TextureView(context))
            player = player
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .then(if (firstFrameReady) Modifier else Modifier.size(0.dp)),
            factory = { playerView },
            update = { it.player = player }
        )
    }

    DisposableEffect(player, playerView) {
        onDispose {
            playerView.player = null
            player.stop()
            player.clearMediaItems()
            player.release()
        }
    }
}

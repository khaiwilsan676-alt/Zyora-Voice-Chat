package com.hurry.voicechat

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.TextureView
import android.media.MediaPlayer
import android.graphics.SurfaceTexture
import android.view.Surface
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import coil3.compose.AsyncImage
import java.net.URL
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.net.HttpURLConnection
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

private val StoreBlue = Color(0xFF1D4ED8)
private val StoreBg = Color(0xFFF5F6F8)
private val StoreCyan = Color(0xFF00B4D8)
private val CardGrey = Color(0xFF242424) // Halka grey for cards
private const val StoreAssetBase = "https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/"
private const val ZyoraStoreAssetBase = "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/"
internal val nativeAvatarFrameRefreshVersions = mutableStateMapOf<String, Long>()
internal val nativeVoiceWaveRefreshVersions = mutableStateMapOf<String, Long>()

private const val NativeStoreCachePrefs = "zyora_store_inventory_cache"
private fun readNativeStoreInventoryCache(context: android.content.Context, userId: String): JSONArray {
    if (userId.isBlank()) return JSONArray()
    val raw = context.getSharedPreferences(NativeStoreCachePrefs, android.content.Context.MODE_PRIVATE)
        .getString("inventory_$userId", null) ?: return JSONArray()
    return runCatching { JSONArray(raw) }.getOrDefault(JSONArray())
}
private fun writeNativeStoreInventoryCache(context: android.content.Context, userId: String, items: JSONArray) {
    if (userId.isBlank()) return
    // The server inventory is an authoritative per-account snapshot. Never merge old
    // entries into it: doing so can resurrect an item's stale equipped=true flag after
    // another frame was equipped (or after the previous frame was removed).
    val snapshot = JSONArray()
    val now = System.currentTimeMillis()
    for (i in 0 until items.length()) {
        val source = items.optJSONObject(i) ?: continue
        val id = source.optString("itemId")
        if (id.isBlank() || source.optLong("expiresAt") <= now) continue
        snapshot.put(JSONObject(source.toString()))
    }
    context.getSharedPreferences(NativeStoreCachePrefs, android.content.Context.MODE_PRIVATE)
        .edit().putString("inventory_$userId", snapshot.toString()).apply()
}
private fun upsertNativeStoreCachedItem(context: android.content.Context, userId: String, item: JSONObject) {
    val id = item.optString("itemId")
    if (userId.isBlank() || id.isBlank()) return
    val cached = readNativeStoreInventoryCache(context, userId)
    val merged = JSONArray()
    var replaced = false
    for (i in 0 until cached.length()) {
        val existing = cached.optJSONObject(i) ?: continue
        if (existing.optString("itemId") == id) {
            merged.put(JSONObject(item.toString()))
            replaced = true
        } else merged.put(existing)
    }
    if (!replaced) merged.put(JSONObject(item.toString()))
    writeNativeStoreInventoryCache(context, userId, merged)
}
private fun updateNativeStoreCachedEquip(context: android.content.Context, userId: String, itemId: String, tab: String, equipped: Boolean) {
    val cached = readNativeStoreInventoryCache(context, userId)
    val updated = JSONArray()
    for (i in 0 until cached.length()) {
        val item = cached.optJSONObject(i) ?: continue
        if (item.optString("tab") == tab) {
            item.put("equipped", item.optString("itemId") == itemId && equipped)
        }
        updated.put(item)
    }
    writeNativeStoreInventoryCache(context, userId, updated)
}

private data class NativeEquippedChatBubble(val image: String, val expiresAt: Long)
private val nativeChatBubbleByUser = mutableStateMapOf<String, NativeEquippedChatBubble>()
private val nativeChatBubbleLoads = mutableSetOf<String>()

@Composable
internal fun NativeChatBubbleMessage(
    userId: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current.applicationContext
    val cached = remember(userId) {
        readNativeStoreInventoryCache(context, userId).let { items ->
            (0 until items.length()).mapNotNull { items.optJSONObject(it) }
                .firstOrNull { it.optString("tab") == "Chat Bubble" && it.optBoolean("equipped") && it.optLong("expiresAt") > System.currentTimeMillis() }
                ?.let { item ->
                    val catalogue = nativeStoreItems.firstOrNull { it.id == item.optString("itemId") }
                    NativeEquippedChatBubble(item.optString("image").ifBlank { catalogue?.image.orEmpty() }, item.optLong("expiresAt"))
                }
        }
    }
    val bubble = nativeChatBubbleByUser[userId] ?: cached
    LaunchedEffect(userId) {
        if (userId.isBlank()) return@LaunchedEffect
        val shouldLoad = synchronized(nativeChatBubbleLoads) { nativeChatBubbleLoads.add(userId) }
        if (shouldLoad) {
            try {
                val latest = withContext(Dispatchers.IO) {
                    runCatching {
                        val encoded = URLEncoder.encode(userId, "UTF-8")
                        val connection = (URL("https://zyora-server.onrender.com/api/store/inventory/$encoded").openConnection() as HttpURLConnection).apply {
                            connectTimeout = 8000
                            readTimeout = 8000
                            useCaches = false
                            setRequestProperty("Cache-Control", "no-cache, no-store")
                            setRequestProperty("Pragma", "no-cache")
                        }
                        val code = connection.responseCode
                        if (code !in 200..299) {
                            connection.disconnect()
                            error("Chat bubble inventory unavailable")
                        }
                        val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                        connection.disconnect()
                        val items = json.optJSONArray("items") ?: JSONArray()
                        writeNativeStoreInventoryCache(context, userId, items)
                        (0 until items.length()).mapNotNull { items.optJSONObject(it) }
                            .firstOrNull { it.optString("tab") == "Chat Bubble" && it.optBoolean("equipped") && it.optLong("expiresAt") > System.currentTimeMillis() }
                            ?.let { item ->
                                val catalogue = nativeStoreItems.firstOrNull { it.id == item.optString("itemId") }
                                NativeEquippedChatBubble(item.optString("image").ifBlank { catalogue?.image.orEmpty() }, item.optLong("expiresAt"))
                            }
                    }
                }
                latest.onSuccess { nativeChatBubbleByUser[userId] = it ?: NativeEquippedChatBubble("", 0L) }
            } finally {
                synchronized(nativeChatBubbleLoads) { nativeChatBubbleLoads.remove(userId) }
            }
        }
    }
    val activeBubble = bubble?.takeIf { it.image.isNotBlank() && it.expiresAt > System.currentTimeMillis() }
    Box(modifier) {
        if (activeBubble != null) {
            AsyncImage(
                model = StoreAssetBase + activeBubble.image,
                contentDescription = "Equipped chat bubble",
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Fit
            )
        } else {
            Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = .40f)))
        }
        content()
    }
}

private data class NativeStoreTab(val id: String, val label: String, val icon: String)
private data class NativeStoreItem(
    val id: String, val name: String, val image: String, val tab: String,
    val stars: Int, val price: Int, val duration: String, val video: String? = null,
    val ownedByDefault: Boolean = false
)
private fun nativeStoreAssetUrl(item: NativeStoreItem, path: String): String =
    (if (item.id == "a9") ZyoraStoreAssetBase else StoreAssetBase) + path

private val nativeStoreTabs = listOf(
    NativeStoreTab("Vehicle", "Vehicle", "IMG_20260913_090019.png"),
    NativeStoreTab("Avatar Frame", "Frame", "IMG_20260913_090057.png"),
    NativeStoreTab("Theme", "Theme", "IMG_20260913_090137.png"),
    NativeStoreTab("Voice", "Voice", "IMG_20261128_125432.png")
)

private val nativeStoreItems = listOf(
    NativeStoreItem("v1", "Leopard Roar", "IMG_20261023_124625.png", "Vehicle", 5, 1000000, "5D", "VID_20260905_095157_269_bsl.mp4"),
    NativeStoreItem("v10", "Shadow Beast", "file_0000000009a482118ab17c8d0629d193.png", "Vehicle", 5, 1200000, "5D", "Screenrecorder-2026-10-23-16-01-27-317~2.mp4"),
    NativeStoreItem("v2", "Speedster", "IMG_20261023_125302.png", "Vehicle", 5, 600000, "5D", "alioss_19_2025090317293759938~2.mp4"),
    NativeStoreItem("v3", "Blaze Runner", "IMG_20261023_125318.png", "Vehicle", 5, 549999, "5D", "alioss_19_2025120517555513292~2.mp4"),
    NativeStoreItem("v4", "Star Rider", "IMG_20261023_125335.png", "Vehicle", 5, 500000, "5D", "alioss_19_2025121720314422497~2.mp4"),
    NativeStoreItem("v5", "Sky Drift", "IMG_20261023_125357.png", "Vehicle", 5, 259000, "5D", "alioss_19_2026021110563339968~2.mp4"),
    NativeStoreItem("v6", "Neon Flash", "IMG_20261023_125133.png", "Vehicle", 5, 890999, "5D", "alioss_16_2025092202374953873~2.mp4"),
    NativeStoreItem("v7", "Royal Cruiser", "IMG_20261023_125152.png", "Vehicle", 5, 425000, "5D", "alioss_16_2025080802251364763~2.mp4"),
    NativeStoreItem("v8", "Shadow Wing", "IMG_20261023_125210.png", "Vehicle", 5, 525000, "5D", "alioss_16_2025092202291646319~2.mp4"),
    NativeStoreItem("v9", "Galaxy Surfer", "IMG_20261023_125416.png", "Vehicle", 5, 359999, "5D", "alioss_17_2025041211424496804~2.mp4"),

    NativeStoreItem("a1", "Crown Wings", "IMG_20261021_192009.png", "Avatar Frame", 5, 250000, "3D", video = "VID_20260905_024534_955_bsl.mp4"),
    NativeStoreItem("a2", "Host Wings", "IMG_20261021_192044.png", "Avatar Frame", 5, 500000, "3D", video = "VID_20260905_024726_660_bsl.mp4"),
    NativeStoreItem("a3", "Mystic Wings", "file_00000000c67c81fba9294a82cd14d3ca.png", "Avatar Frame", 5, 750000, "3D", video = "VID_20260905_083446_619_bsl.mp4"),
    NativeStoreItem("a4", "Ninja Neo", "file_00000000b8048211813166b471b0cdf6.png", "Avatar Frame", 5, 700000, "3D"),
    NativeStoreItem("a5", "Gemshine", "file_00000000a77c8211a913aef88c4d9a4d.png", "Avatar Frame", 5, 249000, "3D"),
    NativeStoreItem("a6", "Love Beat", "file_00000000445482119241b60988ca201a.png", "Avatar Frame", 5, 300000, "3D"),
    NativeStoreItem("a7", "Golden Aura", "file_00000000406482119e4e67fbbe323a54.png", "Avatar Frame", 5, 199000, "3D"),
    NativeStoreItem("a8", "Happy birthday", "file_00000000c21482118a018264cecee76c.png", "Avatar Frame", 5, 500000, "3D", video = "gemini_generated_video_c02dfa3d~2.mp4"),

    NativeStoreItem("t1", "Seafood", "IMG-20260904-WA0004.jpg", "Theme", 4, 2700000, "30D"),
    NativeStoreItem("t2", "Night Sky", "IMG-20260904-WA0005.jpg", "Theme", 5, 2400000, "30D"),
    NativeStoreItem("t3", "Seafood", "IMG-20260904-WA0006.jpg", "Theme", 4, 2700000, "30D"),
    NativeStoreItem("t4", "Night Sky", "IMG-20260904-WA0007.jpg", "Theme", 5, 2400000, "30D"),
    NativeStoreItem("t5", "Seafood", "IMG-20260904-WA0040.jpg", "Theme", 4, 2700000, "30D"),
    NativeStoreItem("t6", "Night Sky", "IMG-20260904-WA0041.jpg", "Theme", 5, 2400000, "30D"),
    NativeStoreItem("t7", "Painted Skies", "IMG-20261024-WA0021.jpg", "Theme", 5, 899999, "30D"),
    NativeStoreItem("t8", "Solace nature", "IMG-20261024-WA0022.jpg", "Theme", 5, 700000, "30D"),
    NativeStoreItem("t9", "Time light", "IMG-20261024-WA0023.jpg", "Theme", 5, 1000000, "30D"),

    NativeStoreItem("vw1", "Emerald Wave", "", "Voice", 5, 300000, "3D")
)

private data class VoiceStarSpec(
    val angleDeg: Float,
    val radiusFrac: Float,
    val sizeFrac: Float,
    val rotationDeg: Float,
    val phaseOffset: Float
)

private data class VoiceOrbSpec(
    val angleDeg: Float,
    val radiusFrac: Float,
    val sizeFrac: Float,
    val isYellow: Boolean,
    val phaseOffset: Float
)

private val emeraldVoiceStars = listOf(
    VoiceStarSpec(-65f, 0.80f, 0.056f, 12f, 0.0f),
    VoiceStarSpec(-42f, 0.72f, 0.036f, 25f, 0.9f),
    VoiceStarSpec(-18f, 0.77f, 0.052f, -10f, 1.8f),
    VoiceStarSpec(18f, 0.82f, 0.038f, 18f, 2.5f),
    VoiceStarSpec(52f, 0.74f, 0.034f, -15f, 3.2f),
    VoiceStarSpec(118f, 0.82f, 0.052f, 8f, 4.1f),
    VoiceStarSpec(155f, 0.86f, 0.034f, -20f, 4.9f),
    VoiceStarSpec(182f, 0.74f, 0.044f, 15f, 5.4f),
    VoiceStarSpec(-148f, 0.76f, 0.055f, -12f, 1.3f),
    VoiceStarSpec(-122f, 0.83f, 0.036f, 20f, 2.7f)
)

private val emeraldVoiceOrbs = listOf(
    VoiceOrbSpec(-86f, 0.77f, 0.022f, true, 0.4f),
    VoiceOrbSpec(-54f, 0.87f, 0.018f, false, 1.1f),
    VoiceOrbSpec(-28f, 0.88f, 0.024f, true, 2.0f),
    VoiceOrbSpec(-4f, 0.69f, 0.020f, false, 2.9f),
    VoiceOrbSpec(28f, 0.76f, 0.022f, true, 3.7f),
    VoiceOrbSpec(70f, 0.85f, 0.020f, false, 4.4f),
    VoiceOrbSpec(96f, 0.79f, 0.026f, true, 5.1f),
    VoiceOrbSpec(132f, 0.72f, 0.020f, false, 0.8f),
    VoiceOrbSpec(142f, 0.89f, 0.028f, true, 1.6f),
    VoiceOrbSpec(166f, 0.79f, 0.024f, true, 2.4f),
    VoiceOrbSpec(-172f, 0.86f, 0.026f, true, 3.3f),
    VoiceOrbSpec(-135f, 0.67f, 0.018f, false, 4.2f),
    VoiceOrbSpec(-112f, 0.74f, 0.022f, true, 5.0f),
    VoiceOrbSpec(-98f, 0.88f, 0.016f, false, 5.8f)
)

private fun buildVoiceWaveRingPath(
    cx: Float,
    cy: Float,
    baseRadius: Float,
    amp1: Float,
    freq1: Int,
    phase1: Float,
    amp2: Float,
    freq2: Int,
    phase2: Float,
    steps: Int = 96
): Path {
    val path = Path()
    val twoPi = (2.0 * PI).toFloat()
    for (i in 0..steps) {
        val theta = (i.toFloat() / steps) * twoPi
        val r = baseRadius + amp1 * sin(freq1 * theta + phase1) + amp2 * cos(freq2 * theta + phase2)
        val x = cx + r * cos(theta)
        val y = cy + r * sin(theta)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

private fun DrawScope.drawVoiceStar(
    center: Offset,
    outerRadius: Float,
    rotationDeg: Float,
    alpha: Float
) {
    val safeAlpha = alpha.coerceIn(0f, 1f)
    if (safeAlpha <= 0.01f || outerRadius <= 0.5f) return

    val glowRadius = outerRadius * 2.8f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFF176).copy(alpha = 0.65f * safeAlpha),
                Color(0xFF76FF03).copy(alpha = 0.25f * safeAlpha),
                Color.Transparent
            ),
            center = center,
            radius = glowRadius
        ),
        radius = glowRadius,
        center = center
    )

    val innerRadius = outerRadius * 0.43f
    val startRad = ((rotationDeg - 90f) * PI / 180.0).toFloat()
    val stepRad = (PI / 5.0).toFloat()
    val starPath = Path()
    for (i in 0 until 10) {
        val r = if (i % 2 == 0) outerRadius else innerRadius
        val angle = startRad + i * stepRad
        val x = center.x + r * cos(angle)
        val y = center.y + r * sin(angle)
        if (i == 0) starPath.moveTo(x, y) else starPath.lineTo(x, y)
    }
    starPath.close()

    drawPath(path = starPath, color = Color(0xFFFFEA00).copy(alpha = 0.95f * safeAlpha))
    drawCircle(
        color = Color(0xFFFFFFCC).copy(alpha = 0.85f * safeAlpha),
        radius = outerRadius * 0.24f,
        center = center
    )
}

@Composable
fun EmeraldVoiceWaveEffect(
    animated: Boolean,
    modifier: Modifier = Modifier
) {
    val phase: Float
    val pulseProgress: Float
    val breath: Float

    if (animated) {
        val transition = rememberInfiniteTransition(label = "emerald_voice_wave")
        phase = transition.animateFloat(
            initialValue = 0f,
            targetValue = (2.0 * PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 3600, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "wave_phase"
        ).value
        pulseProgress = transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "wave_pulse"
        ).value
        breath = transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1400, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "wave_breath"
        ).value
    } else {
        phase = 0.65f
        pulseProgress = 0.35f
        breath = 0.55f
    }

    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val center = Offset(cx, cy)
        val maxR = min(size.width, size.height) / 2f
        if (maxR <= 2f) return@Canvas

        val glowBoost = 0.88f + 0.22f * breath

        // 1. Ambient emerald radial ring halo
        drawCircle(
            brush = Brush.radialGradient(
                0.0f to Color.Transparent,
                0.44f to Color.Transparent,
                0.56f to Color(0xFF00E676).copy(alpha = 0.34f * glowBoost),
                0.70f to Color(0xFF059669).copy(alpha = 0.28f * glowBoost),
                0.86f to Color(0xFF065F46).copy(alpha = 0.16f * glowBoost),
                0.98f to Color(0xFF064E3B).copy(alpha = 0.05f),
                1.0f to Color.Transparent,
                center = center,
                radius = maxR
            ),
            radius = maxR,
            center = center
        )

        // 2. Inner silky glowing wavy emerald ribbons (exact overlapping energy look)
        val innerRibbon1 = buildVoiceWaveRingPath(
            cx = cx, cy = cy,
            baseRadius = maxR * (0.58f + (if (animated) 0.015f * sin(phase) else 0f)),
            amp1 = maxR * 0.032f, freq1 = 3, phase1 = phase,
            amp2 = maxR * 0.018f, freq2 = 2, phase2 = -phase * 0.8f
        )
        drawPath(
            path = innerRibbon1,
            color = Color(0xFF00E676).copy(alpha = 0.28f * glowBoost),
            style = Stroke(width = maxR * 0.11f, cap = StrokeCap.Round)
        )
        drawPath(
            path = innerRibbon1,
            color = Color(0xFF10B981).copy(alpha = 0.78f),
            style = Stroke(width = maxR * 0.044f, cap = StrokeCap.Round)
        )
        drawPath(
            path = innerRibbon1,
            color = Color(0xFF86EFAC).copy(alpha = 0.92f),
            style = Stroke(width = maxR * 0.016f, cap = StrokeCap.Round)
        )

        val innerRibbon2 = buildVoiceWaveRingPath(
            cx = cx, cy = cy,
            baseRadius = maxR * (0.635f + (if (animated) 0.016f * cos(phase) else 0f)),
            amp1 = maxR * 0.034f, freq1 = 2, phase1 = -phase * 1.15f + 1.1f,
            amp2 = maxR * 0.016f, freq2 = 4, phase2 = phase * 0.9f
        )
        drawPath(
            path = innerRibbon2,
            color = Color(0xFF00C853).copy(alpha = 0.26f * glowBoost),
            style = Stroke(width = maxR * 0.095f, cap = StrokeCap.Round)
        )
        drawPath(
            path = innerRibbon2,
            color = Color(0xFF34D399).copy(alpha = 0.72f),
            style = Stroke(width = maxR * 0.030f, cap = StrokeCap.Round)
        )
        drawPath(
            path = innerRibbon2,
            color = Color(0xFFA7F3D0).copy(alpha = 0.80f),
            style = Stroke(width = maxR * 0.012f, cap = StrokeCap.Round)
        )

        val midRibbon = buildVoiceWaveRingPath(
            cx = cx, cy = cy,
            baseRadius = maxR * 0.71f,
            amp1 = maxR * 0.028f, freq1 = 3, phase1 = phase * 0.75f + 2.3f,
            amp2 = maxR * 0.014f, freq2 = 5, phase2 = -phase
        )
        drawPath(
            path = midRibbon,
            color = Color(0xFF059669).copy(alpha = 0.30f),
            style = Stroke(width = maxR * 0.075f, cap = StrokeCap.Round)
        )
        drawPath(
            path = midRibbon,
            color = Color(0xFF22C55E).copy(alpha = 0.62f),
            style = Stroke(width = maxR * 0.022f, cap = StrokeCap.Round)
        )

        // 3. Outer concentric wave rings — static in Card, expanding live voice waves in Preview
        if (animated) {
            for (k in 0 until 3) {
                val p = (pulseProgress + k / 3f) % 1f
                val rippleR = maxR * (0.55f + 0.44f * p)
                val rippleAlpha = sin(p * PI.toFloat()).coerceIn(0f, 1f)
                val ripplePath = buildVoiceWaveRingPath(
                    cx = cx, cy = cy,
                    baseRadius = rippleR,
                    amp1 = maxR * 0.022f * (1f - 0.35f * p), freq1 = 3, phase1 = phase + k * 2.1f,
                    amp2 = maxR * 0.012f, freq2 = 2, phase2 = -phase * 0.7f + k
                )
                drawPath(
                    path = ripplePath,
                    color = Color(0xFF00E676).copy(alpha = 0.34f * rippleAlpha),
                    style = Stroke(width = maxR * (0.065f - 0.03f * p), cap = StrokeCap.Round)
                )
                drawPath(
                    path = ripplePath,
                    color = Color(0xFF69F0AE).copy(alpha = 0.85f * rippleAlpha),
                    style = Stroke(width = maxR * (0.022f - 0.01f * p), cap = StrokeCap.Round)
                )
            }
            // Delicate outer framing wave ring
            val outerGuide = buildVoiceWaveRingPath(
                cx = cx, cy = cy,
                baseRadius = maxR * 0.91f,
                amp1 = maxR * 0.018f, freq1 = 2, phase1 = -phase * 0.6f,
                amp2 = maxR * 0.010f, freq2 = 4, phase2 = phase * 0.5f
            )
            drawPath(
                path = outerGuide,
                color = Color(0xFF10B981).copy(alpha = 0.32f),
                style = Stroke(width = maxR * 0.018f, cap = StrokeCap.Round)
            )
        } else {
            val outerRing1 = buildVoiceWaveRingPath(
                cx = cx, cy = cy,
                baseRadius = maxR * 0.80f,
                amp1 = maxR * 0.022f, freq1 = 2, phase1 = 1.4f,
                amp2 = maxR * 0.012f, freq2 = 4, phase2 = 0.3f
            )
            drawPath(
                path = outerRing1,
                color = Color(0xFF059669).copy(alpha = 0.36f),
                style = Stroke(width = maxR * 0.055f, cap = StrokeCap.Round)
            )
            drawPath(
                path = outerRing1,
                color = Color(0xFF4ADE80).copy(alpha = 0.58f),
                style = Stroke(width = maxR * 0.016f, cap = StrokeCap.Round)
            )

            val outerRing2 = buildVoiceWaveRingPath(
                cx = cx, cy = cy,
                baseRadius = maxR * 0.90f,
                amp1 = maxR * 0.020f, freq1 = 3, phase1 = 2.7f,
                amp2 = maxR * 0.010f, freq2 = 2, phase2 = 1.1f
            )
            drawPath(
                path = outerRing2,
                color = Color(0xFF10B981).copy(alpha = 0.24f),
                style = Stroke(width = maxR * 0.045f, cap = StrokeCap.Round)
            )
            drawPath(
                path = outerRing2,
                color = Color(0xFF6EE7B7).copy(alpha = 0.42f),
                style = Stroke(width = maxR * 0.012f, cap = StrokeCap.Round)
            )

            val outerRing3 = buildVoiceWaveRingPath(
                cx = cx, cy = cy,
                baseRadius = maxR * 0.96f,
                amp1 = maxR * 0.012f, freq1 = 2, phase1 = 0.5f,
                amp2 = maxR * 0.008f, freq2 = 3, phase2 = 2.0f
            )
            drawPath(
                path = outerRing3,
                color = Color(0xFF34D399).copy(alpha = 0.24f),
                style = Stroke(width = maxR * 0.010f, cap = StrokeCap.Round)
            )
        }

        // 4. Luminous golden & lime bokeh orbs
        emeraldVoiceOrbs.forEach { orb ->
            val driftRad = if (animated) 0.03f * sin(phase + orb.phaseOffset) else 0f
            val angleRad = ((orb.angleDeg * PI / 180.0).toFloat()) + (if (animated) phase * 0.12f else 0f)
            val r = maxR * (orb.radiusFrac + driftRad)
            val pos = Offset(cx + r * cos(angleRad), cy + r * sin(angleRad))
            val orbR = maxR * orb.sizeFrac
            val twinkle = if (animated) 0.65f + 0.35f * sin(phase * 2f + orb.phaseOffset) else 0.92f
            val coreColor = if (orb.isYellow) Color(0xFFFFF59D) else Color(0xFFB9F6CA)
            val haloColor = if (orb.isYellow) Color(0xFFFFD600) else Color(0xFF00E676)

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        coreColor.copy(alpha = 0.95f * twinkle),
                        haloColor.copy(alpha = 0.45f * twinkle),
                        Color.Transparent
                    ),
                    center = pos,
                    radius = orbR * 3.0f
                ),
                radius = orbR * 3.0f,
                center = pos
            )
            drawCircle(
                color = coreColor.copy(alpha = 0.95f * twinkle),
                radius = orbR * 0.72f,
                center = pos
            )
        }

        // 5. Golden 5-point stars scattered along the rings
        emeraldVoiceStars.forEach { star ->
            val waveShift = if (animated) 0.025f * sin(phase * 1.5f + star.phaseOffset) else 0f
            val angleRad = ((star.angleDeg * PI / 180.0).toFloat()) + (if (animated) phase * 0.18f else 0f)
            val r = maxR * (star.radiusFrac + waveShift)
            val pos = Offset(cx + r * cos(angleRad), cy + r * sin(angleRad))
            val scalePulse = if (animated) 0.85f + 0.25f * sin(phase * 2f + star.phaseOffset) else 1f
            val starAlpha = if (animated) 0.75f + 0.25f * cos(phase * 2f + star.phaseOffset) else 0.96f
            drawVoiceStar(
                center = pos,
                outerRadius = maxR * star.sizeFrac * scalePulse,
                rotationDeg = star.rotationDeg + (if (animated) (phase * 18f) else 0f),
                alpha = starAlpha
            )
        }
    }
}

private fun isNativeVoiceWaveEquipped(item: JSONObject): Boolean {
    val isVoiceItem = item.optString("tab").equals("Voice", ignoreCase = true) || item.optString("itemId") == "vw1"
    val expiry = item.optLong("expiresAt", 0L)
    return isVoiceItem && item.optBoolean("equipped") && (expiry <= 0L || expiry > System.currentTimeMillis())
}

@Composable
internal fun NativeEquippedVoiceWaveOverlay(userId: String, modifier: Modifier = Modifier) {
    val appContext = LocalContext.current.applicationContext
    var equipped by remember(userId) {
        mutableStateOf(readNativeStoreInventoryCache(appContext, userId).let { cached ->
            (0 until cached.length()).mapNotNull { cached.optJSONObject(it) }
                .any { isNativeVoiceWaveEquipped(it) }
        })
    }
    val refreshVersion = nativeVoiceWaveRefreshVersions[userId] ?: 0L
    LaunchedEffect(userId, refreshVersion) {
        if (userId.isBlank()) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            runCatching {
                val encoded = URLEncoder.encode(userId, "UTF-8")
                val connection = (URL("https://zyora-server.onrender.com/api/store/inventory/$encoded").openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8000
                    readTimeout = 8000
                    useCaches = false
                    setRequestProperty("Cache-Control", "no-cache, no-store")
                    setRequestProperty("Pragma", "no-cache")
                }
                val code = connection.responseCode
                if (code !in 200..299) {
                    connection.disconnect()
                    error("Voice inventory unavailable")
                }
                val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                connection.disconnect()
                val items = json.optJSONArray("items") ?: JSONArray()
                writeNativeStoreInventoryCache(appContext, userId, items)
                (0 until items.length()).mapNotNull { items.optJSONObject(it) }
                    .any { isNativeVoiceWaveEquipped(it) }
            }.onSuccess { equipped = it }
        }
    }
    LaunchedEffect(userId, equipped) {
        if (!equipped) return@LaunchedEffect
        val cached = readNativeStoreInventoryCache(appContext, userId)
        val voice = (0 until cached.length()).mapNotNull { cached.optJSONObject(it) }
            .firstOrNull { isNativeVoiceWaveEquipped(it) }
            ?: return@LaunchedEffect
        val remaining = (voice.optLong("expiresAt") - System.currentTimeMillis()).coerceAtLeast(0L)
        if (remaining > 0L) delay(remaining)
        equipped = false
    }
    if (equipped) {
        EmeraldVoiceWaveEffect(animated = true, modifier = modifier)
    } else {
        DefaultWhiteVoiceWaveEffect(modifier = modifier)
    }
}

@Composable
private fun DefaultWhiteVoiceWaveEffect(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "default_white_voice_wave")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "default_white_voice_wave_phase"
    )
    Canvas(modifier = modifier) {
        val maxRadius = size.minDimension / 2f
        val stroke = (maxRadius * 0.035f).coerceAtLeast(1.5.dp.toPx())
        val outer = buildVoiceWaveRingPath(
            cx = size.width / 2f,
            cy = size.height / 2f,
            baseRadius = maxRadius * 0.76f,
            amp1 = maxRadius * 0.045f,
            freq1 = 5,
            phase1 = phase,
            amp2 = maxRadius * 0.022f,
            freq2 = 9,
            phase2 = -phase * 0.72f
        )
        val inner = buildVoiceWaveRingPath(
            cx = size.width / 2f,
            cy = size.height / 2f,
            baseRadius = maxRadius * 0.65f,
            amp1 = maxRadius * 0.028f,
            freq1 = 4,
            phase1 = -phase * 0.82f,
            amp2 = maxRadius * 0.014f,
            freq2 = 7,
            phase2 = phase * 0.55f
        )
        drawPath(outer, Color.White.copy(alpha = 0.78f), style = Stroke(width = stroke))
        drawPath(inner, Color.White.copy(alpha = 0.34f), style = Stroke(width = stroke * 0.72f))
    }
}

@Composable
fun NativeAvatarFrameOverlay(
    userId: String,
    modifier: Modifier = Modifier,
    fallbackUserIds: List<String> = emptyList(),
    gemshineOffsetX: Dp = 0.dp,
    gemshineOffsetY: Dp = 4.dp
) {
    val appContext = LocalContext.current.applicationContext
    val lookupIds = remember(userId, fallbackUserIds) { (listOf(userId) + fallbackUserIds).filter { it.isNotBlank() }.distinct() }
    var frame by remember(userId, fallbackUserIds) {
        mutableStateOf(lookupIds.firstNotNullOfOrNull { id ->
            readNativeStoreInventoryCache(appContext, id).let { cached ->
                (0 until cached.length()).mapNotNull { cached.optJSONObject(it) }
                    .firstOrNull { it.optString("tab") == "Avatar Frame" && it.optBoolean("equipped") && it.optLong("expiresAt") > System.currentTimeMillis() }
            }
        })
    }
    val refreshVersion = lookupIds.maxOfOrNull { nativeAvatarFrameRefreshVersions[it] ?: 0L } ?: 0L
    LaunchedEffect(lookupIds, refreshVersion) {
        frame = lookupIds.firstNotNullOfOrNull { lookupId ->
            val cached = readNativeStoreInventoryCache(appContext, lookupId)
            (0 until cached.length()).mapNotNull { cached.optJSONObject(it) }
                .firstOrNull { it.optString("tab") == "Avatar Frame" && it.optBoolean("equipped") && it.optLong("expiresAt") > System.currentTimeMillis() }
        }
        if (lookupIds.isNotEmpty()) {
            val equippedFrame = withContext(Dispatchers.IO) {
                lookupIds.firstNotNullOfOrNull { lookupId ->
                    runCatching {
                        val encoded = URLEncoder.encode(lookupId, "UTF-8")
                        val connection = (URL("https://zyora-server.onrender.com/api/store/inventory/$encoded").openConnection() as HttpURLConnection).apply {
                            connectTimeout = 8000
                            readTimeout = 8000
                            useCaches = false
                            setRequestProperty("Cache-Control", "no-cache, no-store")
                            setRequestProperty("Pragma", "no-cache")
                        }
                        val code = connection.responseCode
                        if (code !in 200..299) {
                            connection.disconnect()
                            return@runCatching null
                        }
                        val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                        connection.disconnect()
                        val items = json.optJSONArray("items") ?: JSONArray()
                        writeNativeStoreInventoryCache(appContext, lookupId, items)
                        (0 until items.length()).map { items.getJSONObject(it) }
                            .firstOrNull { it.optString("tab") == "Avatar Frame" && it.optBoolean("equipped") && it.optLong("expiresAt") > System.currentTimeMillis() }
                    }.getOrNull()
                }
            }
            frame = equippedFrame
        }
    }
    LaunchedEffect(frame) {
        val expiresAt = frame?.optLong("expiresAt") ?: return@LaunchedEffect
        val remaining = (expiresAt - System.currentTimeMillis()).coerceAtLeast(0L)
        if (remaining > 0L) delay(remaining)
        frame = null
    }
    val selected = frame
    if (selected != null) {
        val selectedItem = nativeStoreItems.firstOrNull { it.id == selected.optString("itemId") }
        val video = selected.optString("video", selectedItem?.video.orEmpty()).ifBlank { selectedItem?.video.orEmpty() }
        if (video.isNotBlank()) {
            val frameId = selected.optString("itemId", "")
            val frameMute = frameId == "a8"
            val videoUrl = if (video.startsWith("http://", true) || video.startsWith("https://", true)) video else nativeStoreAssetUrl(NativeStoreItem(frameId, "", "", "Avatar Frame", 0, 0, ""), video)
            StoreVideoPreview(
                url = videoUrl,
                modifier = modifier.graphicsLayer {
                    // Frame must visibly extend beyond the avatar, not sit inside its bounds.
                    scaleX = 1.28f
                    scaleY = 1.28f
                    translationY = -5.dp.toPx()
                },
                pause = false,
                topFade = 0f,
                bottomFade = 0f,
                removeGreenScreen = true,
                mute = frameMute,
                cacheVideo = true
            )
        } else {
            val image = selected.optString("image", selectedItem?.image.orEmpty()).ifBlank { selectedItem?.image.orEmpty() }
            if (image.isNotBlank()) AsyncImage(model = nativeStoreAssetUrl(NativeStoreItem(selected.optString("itemId"), "", "", "Avatar Frame", 0, 0, ""), image), contentDescription = "Equipped avatar frame", modifier = modifier.graphicsLayer { val frameId = selected.optString("itemId"); val isAdjustedFrame = frameId == "a5" || frameId == "a6"; val frameScale = if (isAdjustedFrame) 1.33f else if (frameId == "a4") 1.23f else 1.28f; scaleX = frameScale; scaleY = frameScale; if (isAdjustedFrame) { translationX = gemshineOffsetX.toPx(); translationY = -gemshineOffsetY.toPx() } }, contentScale = ContentScale.Fit)
        }
    }
}

@Composable
fun NativeOwnedFrameShowcase(userId: String, onDataLoaded: ((Boolean) -> Unit)? = null) {
    val appContext = LocalContext.current.applicationContext
    var frames by remember(userId) {
        mutableStateOf(readNativeStoreInventoryCache(appContext, userId).let { cached ->
            (0 until cached.length()).mapNotNull { cached.optJSONObject(it) }
                .filter { it.optString("tab") == "Avatar Frame" && it.optLong("expiresAt") > System.currentTimeMillis() }
        })
    }
    var showcaseClock by remember(userId) { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(userId) { while (true) { showcaseClock = System.currentTimeMillis(); delay(1000L) } }
    val refreshVersion = nativeAvatarFrameRefreshVersions[userId] ?: 0L
    LaunchedEffect(userId, refreshVersion) {
        if (userId.isNotBlank()) withContext(Dispatchers.IO) {
            runCatching {
                val encoded = URLEncoder.encode(userId, "UTF-8")
                val c = (URL("https://zyora-server.onrender.com/api/store/inventory/$encoded").openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8000
                    readTimeout = 8000
                }
                val code = c.responseCode
                val stream = if (code in 200..299) c.inputStream else c.errorStream
                val json = JSONObject(stream.bufferedReader().use { it.readText() })
                c.disconnect()
                if (code !in 200..299) throw IllegalStateException("Inventory unavailable")
                val items = json.optJSONArray("items") ?: JSONArray()
                writeNativeStoreInventoryCache(appContext, userId, items)
                (0 until items.length()).map { items.getJSONObject(it) }
                    .filter { it.optString("tab") == "Avatar Frame" && it.optLong("expiresAt") > System.currentTimeMillis() }
            }.onSuccess { frames = it; onDataLoaded?.invoke(it.isNotEmpty()) }
        }
    }
    LaunchedEffect(frames) {
        while (frames.isNotEmpty()) {
            val nextExpiry = frames.minOfOrNull { it.optLong("expiresAt") } ?: break
            val remaining = (nextExpiry - System.currentTimeMillis()).coerceAtLeast(0L)
            if (remaining > 0L) delay(remaining)
            val now = System.currentTimeMillis()
            frames = frames.filter { it.optLong("expiresAt") > now }
        }
    }
    val validFrames = frames.mapNotNull { owned ->
        val item = nativeStoreItems.firstOrNull { it.id == owned.optString("itemId") }
            ?: NativeStoreItem(
                owned.optString("itemId"),
                owned.optString("name", "Owned Frame"),
                owned.optString("image"),
                "Avatar Frame",
                owned.optInt("stars", 5).coerceIn(1, 5),
                0,
                owned.optString("duration", "30D"),
                owned.optString("video").takeIf { it.isNotBlank() }
            )
        owned to item
    }
    if (validFrames.isNotEmpty()) {
        Column(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            validFrames.chunked(3).forEach { rowFrames ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    rowFrames.forEach { (owned, item) ->
                        Column(
                            Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Catalogue still image only; equipped videos play only over avatars.
                            val frameImage = owned.optString("image").ifBlank { item.image }
                            AsyncImage(
                                model = nativeStoreAssetUrl(item, frameImage),
                                contentDescription = "Owned avatar frame",
                                modifier = Modifier.fillMaxWidth().height(92.dp),
                                contentScale = ContentScale.Fit
                            )
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                repeat(item.stars) {
                                    AsyncImage(
                                        model = StoreAssetBase + "file_0000000004e881fa84e61b8616f8eaf8.png",
                                        contentDescription = "Star",
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                            val remaining = ((owned.optLong("expiresAt") - showcaseClock).coerceAtLeast(0L) / 1000L)
                            Text(
                                text = "${remaining / 86400L}d ${(remaining % 86400L) / 3600L}h",
                                color = Color.LightGray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    repeat(3 - rowFrames.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}



@Composable
fun NativeOwnedMedalShowcase(userId: String, onDataLoaded: ((Boolean) -> Unit)? = null, showEmpty: Boolean = false) {
    val appContext = LocalContext.current.applicationContext
    var medals by remember(userId) {
        mutableStateOf(readNativeStoreInventoryCache(appContext, userId).let { cached ->
            (0 until cached.length()).mapNotNull { cached.optJSONObject(it) }
                .filter { it.optString("tab") == "Medal" && it.optLong("expiresAt") > System.currentTimeMillis() }
        })
    }
    var clock by remember(userId) { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(userId) { while (true) { clock = System.currentTimeMillis(); delay(1000L) } }
    LaunchedEffect(userId) {
        if (userId.isNotBlank()) withContext(Dispatchers.IO) {
            runCatching {
                val encoded = URLEncoder.encode(userId, "UTF-8")
                val connection = (URL("https://zyora-server.onrender.com/api/store/inventory/$encoded").openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8000; readTimeout = 8000; useCaches = false
                    setRequestProperty("Cache-Control", "no-cache, no-store")
                    setRequestProperty("Pragma", "no-cache")
                }
                try {
                    val code = connection.responseCode
                    if (code !in 200..299) error("Medal inventory unavailable")
                    val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                    val items = json.optJSONArray("items") ?: JSONArray()
                    writeNativeStoreInventoryCache(appContext, userId, items)
                    (0 until items.length()).mapNotNull { items.optJSONObject(it) }
                        .filter { it.optString("tab") == "Medal" && it.optLong("expiresAt") > System.currentTimeMillis() }
                } finally { connection.disconnect() }
            }.onSuccess { medals = it; onDataLoaded?.invoke(it.isNotEmpty()) }
                .onFailure { onDataLoaded?.invoke(medals.isNotEmpty()) }
        }
    }
    LaunchedEffect(medals) {
        while (medals.isNotEmpty()) {
            val expiry = medals.minOfOrNull { it.optLong("expiresAt") } ?: break
            val remaining = (expiry - System.currentTimeMillis()).coerceAtLeast(0L)
            if (remaining > 0L) delay(remaining)
            val now = System.currentTimeMillis()
            medals = medals.filter { it.optLong("expiresAt") > now }
            onDataLoaded?.invoke(medals.isNotEmpty())
        }
    }
    if (medals.isEmpty()) {
        if (showEmpty) {
            Column(Modifier.fillMaxWidth().fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                AsyncImage(model = "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/file_0000000047308211a02722299d1fda2e.png", contentDescription = "No Badge", contentScale = ContentScale.Fit, modifier = Modifier.size(135.dp))
                Spacer(Modifier.height(12.dp))
                Text("No Badge", color = Color(0xFF7D92B0), fontSize = 14.sp)
            }
        }
    } else {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            medals.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                    row.forEach { owned ->
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            val image = owned.optString("image")
                            val imageUrl = if (image.startsWith("http", true)) image else "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/" + image.removePrefix("/")
                            AsyncImage(model = imageUrl, contentDescription = owned.optString("name"), modifier = Modifier.fillMaxWidth().height(92.dp), contentScale = ContentScale.Fit)
                            Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                repeat(owned.optInt("stars", 1).coerceIn(1, 5)) {
                                    AsyncImage(model = "https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/file_0000000004e881fa84e61b8616f8eaf8.png", contentDescription = "Star", modifier = Modifier.size(12.dp))
                                }
                            }
                            val remaining = ((owned.optLong("expiresAt") - clock).coerceAtLeast(0L) / 1000L)
                            Text("${remaining / 86400L}d ${(remaining % 86400L) / 3600L}h", color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}
@Composable
fun NativeOwnedVehicleShowcase(userId: String, onDataLoaded: ((Boolean) -> Unit)? = null) {
    val appContext = LocalContext.current.applicationContext
    var vehicles by remember(userId) {
        mutableStateOf(readNativeStoreInventoryCache(appContext, userId).let { cached ->
            (0 until cached.length()).mapNotNull { cached.optJSONObject(it) }
                .filter { it.optString("tab") == "Vehicle" && it.optLong("expiresAt") > System.currentTimeMillis() }
        })
    }
    var showcaseClock by remember(userId) { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(userId) { while (true) { showcaseClock = System.currentTimeMillis(); delay(1000L) } }
    LaunchedEffect(userId) {
        if (userId.isNotBlank()) withContext(Dispatchers.IO) {
            runCatching {
                val encoded = URLEncoder.encode(userId, "UTF-8")
                val connection = (URL("https://zyora-server.onrender.com/api/store/inventory/$encoded").openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8000
                    readTimeout = 8000
                    useCaches = false
                    setRequestProperty("Cache-Control", "no-cache, no-store")
                }
                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val json = JSONObject(stream.bufferedReader().use { it.readText() })
                connection.disconnect()
                if (code !in 200..299) throw IllegalStateException("Inventory unavailable")
                val items = json.optJSONArray("items") ?: JSONArray()
                writeNativeStoreInventoryCache(appContext, userId, items)
                (0 until items.length()).map { items.getJSONObject(it) }
                    .filter { it.optString("tab") == "Vehicle" && it.optLong("expiresAt") > System.currentTimeMillis() }
            }.onSuccess { vehicles = it; onDataLoaded?.invoke(it.isNotEmpty()) }
        }
    }
    LaunchedEffect(vehicles) {
        while (vehicles.isNotEmpty()) {
            val nextExpiry = vehicles.minOfOrNull { it.optLong("expiresAt") } ?: break
            val remaining = (nextExpiry - System.currentTimeMillis()).coerceAtLeast(0L)
            if (remaining > 0L) delay(remaining)
            val now = System.currentTimeMillis()
            vehicles = vehicles.filter { it.optLong("expiresAt") > now }
        }
    }
    val validVehicles = vehicles.mapNotNull { owned ->
        val item = nativeStoreItems.firstOrNull { it.id == owned.optString("itemId") } ?: return@mapNotNull null
        owned to item
    }
    if (validVehicles.isNotEmpty()) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            validVehicles.chunked(3).forEach { rowVehicles ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowVehicles.forEach { (owned, item) ->
                        val image = owned.optString("image").ifBlank { item.image }
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            AsyncImage(
                                model = StoreAssetBase + image,
                                contentDescription = "Owned vehicle",
                                modifier = Modifier.fillMaxWidth().height(92.dp),
                                contentScale = ContentScale.Fit
                            )
                            Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                repeat(item.stars) {
                                    AsyncImage(
                                        model = StoreAssetBase + "file_0000000004e881fa84e61b8616f8eaf8.png",
                                        contentDescription = "Star",
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                            val remaining = ((owned.optLong("expiresAt") - showcaseClock).coerceAtLeast(0L) / 1000L)
                            Text(
                                text = "${remaining / 86400L}d ${(remaining % 86400L) / 3600L}h",
                                color = Color.LightGray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    repeat(3 - rowVehicles.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorePage(onBack: () -> Unit = {}, initialView: String = "store", userId: String = "") {
    var currentView by remember { mutableStateOf(if (initialView.equals("bag", true)) "bag" else "store") }
    var activeTab by remember { mutableStateOf(if (initialView.equals("bag", true)) "Avatar Frame" else "Vehicle") }
    val appContext = LocalContext.current
    val authPrefs = remember { appContext.getSharedPreferences("hurry_auth", android.content.Context.MODE_PRIVATE) }
    val walletCachePrefs = remember { appContext.getSharedPreferences("zyora_wallet_cache", android.content.Context.MODE_PRIVATE) }
    val storeInventoryCache = remember { appContext.getSharedPreferences(NativeStoreCachePrefs, android.content.Context.MODE_PRIVATE) }
    val previewAvatar = remember(userId) { authPrefs.getString("image", "").orEmpty().trim().takeIf { it.isNotBlank() && it != "null" && it != "undefined" } ?: ZYORA_DEFAULT_AVATAR_URL }
    val storeUserId = remember(userId) { authPrefs.getString("uid", null)?.takeIf { it.isNotBlank() } ?: userId.ifBlank { authPrefs.getString("accountId", "").orEmpty() } }
    val storeInventoryUserId = remember(userId) { userId.ifBlank { authPrefs.getString("accountId", "").orEmpty() } }
    var tryItem by remember { mutableStateOf<NativeStoreItem?>(null) }
    var sendItem by remember { mutableStateOf<NativeStoreItem?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    val giftSearchUsers = remember { mutableStateListOf<JSONObject>() }
    var giftSelectedUser by remember { mutableStateOf<JSONObject?>(null) }
    var giftBusy by remember { mutableStateOf(false) }
    var balance by remember(storeUserId) { mutableIntStateOf(walletCachePrefs.getLong("balance_$storeUserId", 0L).coerceAtLeast(0L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()) }
    val cachedInventory = remember(storeInventoryUserId) { readNativeStoreInventoryCache(appContext, storeInventoryUserId) }
    var serverInventory by remember(storeInventoryUserId) { mutableStateOf(cachedInventory) }
    val ownedIds = remember(storeInventoryUserId) {
        mutableStateListOf<String>().apply {
            for (i in 0 until cachedInventory.length()) cachedInventory.optJSONObject(i)?.let { item ->
                if (item.optLong("expiresAt") > System.currentTimeMillis()) add(item.optString("itemId"))
            }
        }
    }
    val equippedIds = remember(storeInventoryUserId) {
        mutableStateListOf<String>().apply {
            for (i in 0 until cachedInventory.length()) cachedInventory.optJSONObject(i)?.let { item ->
                if (item.optBoolean("equipped") && item.optLong("expiresAt") > System.currentTimeMillis()) add(item.optString("itemId"))
            }
        }
    }
    val expiryById = remember(storeInventoryUserId) {
        mutableStateMapOf<String, Long>().apply {
            for (i in 0 until cachedInventory.length()) cachedInventory.optJSONObject(i)?.let { item ->
                val id = item.optString("itemId"); val expiry = item.optLong("expiresAt")
                if (id.isNotBlank() && expiry > System.currentTimeMillis()) put(id, expiry)
            }
        }
    }
    var purchaseBusy by remember { mutableStateOf(false) }
    var purchaseItemId by remember { mutableStateOf<String?>(null) }
    var purchaseMessage by remember { mutableStateOf<String?>(null) }
    val storeScope = rememberCoroutineScope()
    var clockNow by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(sendItem, searchQuery) {
        giftSearchUsers.clear()
        giftSelectedUser = null
        val query = searchQuery.trim()
        if (sendItem != null && query.length >= 2) {
            delay(350)
            val found = withContext(Dispatchers.IO) { runCatching {
                val encoded = java.net.URLEncoder.encode(query, "UTF-8")
                val connection = (URL("https://zyora-server.onrender.com/api/users?search=" + encoded).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"; connectTimeout = 10000; readTimeout = 10000
                    setRequestProperty("Cache-Control", "no-cache")
                }
                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val response = JSONObject(stream.bufferedReader().use { it.readText() })
                connection.disconnect()
                if (code !in 200..299) emptyList() else {
                    val array = response.optJSONArray("users") ?: org.json.JSONArray()
                    (0 until array.length()).mapNotNull { index ->
                        array.optJSONObject(index)?.takeIf {
                            it.optString("accountId").isNotBlank() && it.optString("accountId") != storeInventoryUserId
                        }
                    }.distinctBy { it.optString("accountId") }.take(8)
                }
            }.getOrDefault(emptyList()) }
            if (sendItem != null && searchQuery.trim() == query) giftSearchUsers.addAll(found)
        }
    }
    // Refresh only the Bag countdown once per second; Store cards stay unchanged.
    LaunchedEffect(currentView) {
        if (currentView == "bag") {
            while (true) {
                clockNow = System.currentTimeMillis()
                delay(1000L)
            }
        }
    }
    // Remove expired items from the local Bag as soon as their expiry is reached.
    LaunchedEffect(expiryById.toMap()) {
        while (true) {
            val now = System.currentTimeMillis()
            val expiredIds = expiryById.filterValues { it <= now }.keys.toList()
            if (expiredIds.isNotEmpty()) {
                expiredIds.forEach { expiredId ->
                    ownedIds.remove(expiredId)
                    equippedIds.remove(expiredId)
                    expiryById.remove(expiredId)
                }
                continue
            }
            val nextExpiry = expiryById.values.minOrNull() ?: break
            delay((nextExpiry - now).coerceAtLeast(1L))
        }
    }
    LaunchedEffect(storeUserId) {
        if (storeUserId.isBlank()) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            runCatching {
                val encoded = URLEncoder.encode(storeInventoryUserId, "UTF-8")
                val c = (URL("https://zyora-server.onrender.com/api/store/inventory/$encoded").openConnection() as HttpURLConnection).apply { connectTimeout=15000; readTimeout=15000; useCaches=false; setRequestProperty("Cache-Control", "no-cache, no-store"); setRequestProperty("Pragma", "no-cache") }
                val code = c.responseCode
                val stream = if (code in 200..299) c.inputStream else c.errorStream
                val json = JSONObject(stream.bufferedReader().use { it.readText() }); c.disconnect()
                if (code !in 200..299) throw IllegalStateException(json.optString("error", "Wallet unavailable"))
                val owned = mutableListOf<String>(); val equipped = mutableListOf<String>(); val expiries = mutableMapOf<String,Long>()
                val arr = json.optJSONArray("items") ?: JSONArray()
                writeNativeStoreInventoryCache(appContext, storeInventoryUserId, arr)
                for (i in 0 until arr.length()) {
                    val item = arr.getJSONObject(i); val id = item.optString("itemId"); val expiry = item.optLong("expiresAt")
                    if (id.isNotBlank() && expiry > System.currentTimeMillis()) {
                        owned.add(id); expiries[id]=expiry
                        if (item.optBoolean("equipped")) equipped.add(id)
                    }
                }
                val walletEncoded = URLEncoder.encode(storeUserId, "UTF-8")
                val walletConn = (URL("https://zyora-server.onrender.com/api/lucky-bag/wallet/$walletEncoded").openConnection() as HttpURLConnection).apply { connectTimeout=12000; readTimeout=12000; useCaches=false; setRequestProperty("Cache-Control", "no-cache, no-store"); setRequestProperty("Pragma", "no-cache") }
                val walletCode = walletConn.responseCode
                val walletJson = JSONObject((if (walletCode in 200..299) walletConn.inputStream else walletConn.errorStream).bufferedReader().use { it.readText() })
                walletConn.disconnect()
                if (walletCode !in 200..299) throw IllegalStateException("Wallet unavailable")
                val serverCoins = walletJson.optLong("balance", 0L).coerceAtLeast(0L)
                Triple(serverCoins.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(), owned, Pair(equipped,expiries))
            }.onSuccess { result ->
                serverInventory = readNativeStoreInventoryCache(appContext, storeInventoryUserId)
                balance = result.first
                walletCachePrefs.edit().putLong("balance_$storeUserId", result.first.toLong()).apply()
                // Inventory GET may have started before a Buy request and finish after it.
                // Merge server-owned IDs instead of clearing a newer successful local purchase.
                result.second.forEach { id -> if (!ownedIds.contains(id)) ownedIds.add(id) }
                equippedIds.clear()
                equippedIds.addAll(result.third.first)
                result.third.second.forEach { (id, expiry) -> expiryById[id] = expiry }
            }.onFailure { purchaseMessage = it.message ?: "Wallet unavailable" }
        }
    }
    LaunchedEffect(storeUserId) {
        if (storeUserId.isBlank()) return@LaunchedEffect
        while (true) {
            delay(5000L)
            val latest = withContext(Dispatchers.IO) {
                runCatching {
                    val encoded = URLEncoder.encode(storeUserId, "UTF-8")
                    val c = (URL("https://zyora-server.onrender.com/api/lucky-bag/wallet/$encoded").openConnection() as HttpURLConnection).apply {
                        connectTimeout = 8000; readTimeout = 8000; useCaches = false
                        setRequestProperty("Cache-Control", "no-cache, no-store")
                    }
                    val code = c.responseCode
                    val stream = if (code in 200..299) c.inputStream else c.errorStream
                    val json = JSONObject(stream.bufferedReader().use { it.readText() })
                    c.disconnect()
                    if (code !in 200..299) error("Wallet unavailable")
                    json.optLong("balance", 0L).coerceAtLeast(0L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                }.getOrNull()
            }
            if (latest != null && latest != balance) {
                balance = latest
                walletCachePrefs.edit().putLong("balance_$storeUserId", latest.toLong()).apply()
            }
        }
    }
    val isBag = currentView == "bag"
    val catalogIds = nativeStoreItems.map { it.id }.toSet()
    val grantedInventoryItems = if (isBag) {
        (0 until serverInventory.length()).mapNotNull { index ->
            serverInventory.optJSONObject(index)?.takeIf { item ->
                item.optString("tab") == activeTab &&
                    item.optLong("expiresAt") > clockNow &&
                    item.optString("itemId").isNotBlank() &&
                    !catalogIds.contains(item.optString("itemId"))
            }?.let { item ->
                NativeStoreItem(
                    item.optString("itemId"),
                    item.optString("name", "Family Reward"),
                    item.optString("image"),
                    item.optString("tab"),
                    0,
                    0,
                    item.optString("duration", "7D"),
                    item.optString("video").takeIf { it.isNotBlank() }
                )
            }
        }
    } else emptyList()
    val visibleItems = nativeStoreItems.filter { it.tab == activeTab && (!isBag || ownedIds.contains(it.id)) } + grantedInventoryItems

    // Dark theme glossy blue background
    val darkGlossyBg = Brush.verticalGradient(
        0.0f to Color(0xFF1D4ED8), // Opaque top background; do not reveal the page behind
        0.3f to Color.Black, // Clear Black starting at 30%
        1.0f to Color.Black
    )

    Column(Modifier.fillMaxSize().background(darkGlossyBg)) {
        Column(Modifier.fillMaxWidth().statusBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(Modifier.size(36.dp).clip(CircleShape).clickable {
                    if (isBag) { currentView = "store"; activeTab = "Vehicle" } else onBack()
                }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = Color.White, modifier = Modifier.size(26.dp))
                }
                Text(if (isBag) "Bag" else "Store", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(
                    if (isBag) "Store" else "Bag", color = Color(0xFF60A5FA), fontSize = 15.sp,
                    fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(6.dp))
                        .clickable { if (isBag) { currentView = "store"; activeTab = "Vehicle" } else { currentView = "bag"; activeTab = "Avatar Frame" } }.padding(5.dp)
                )
            }
            Row(
                Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                nativeStoreTabs.forEach { tab ->
                    val active = activeTab == tab.id
                    var useVoiceFallbackIcon by remember(tab.id) { mutableStateOf(false) }
                    val tabIconUrl = if (tab.id == "Voice") {
                        if (useVoiceFallbackIcon) StoreAssetBase + tab.icon else ZyoraStoreAssetBase + tab.icon
                    } else {
                        StoreAssetBase + tab.icon
                    }
                    Box(
                        Modifier.weight(1f).height(90.dp).clip(RoundedCornerShape(6.dp))
                            .background(if (active) Color.Transparent else Color(0x33FFFFFF))
                            .clickable { activeTab = tab.id },
                        contentAlignment = Alignment.Center
                    ) {
                        if (active) {
                            AsyncImage(
                                model = StoreAssetBase + "IMG_20260913_090226.png",
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.FillBounds
                            )
                        }
                        Column(
                            Modifier.fillMaxSize().padding(top = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            AsyncImage(
                                model = tabIconUrl,
                                contentDescription = tab.label,
                                modifier = Modifier.size(40.dp),
                                onError = { if (tab.id == "Voice" && !useVoiceFallbackIcon) useVoiceFallbackIcon = true }
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(tab.label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                        }
                    }
                }
            }
        }

        if (visibleItems.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (isBag) AsyncImage(model = StoreAssetBase + "file_0000000047308211a02722299d1fda2e.png", contentDescription = "No data", modifier = Modifier.size(120.dp))
                    Text(if (isBag) "No data" else "No items found", color = Color(0xFF9CA3AF), fontSize = 14.sp)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize().animateContentSize(
                    animationSpec = androidx.compose.animation.core.tween(
                        durationMillis = 260,
                        easing = androidx.compose.animation.core.FastOutSlowInEasing
                    )
                ).pointerInput(activeTab) {
                    var horizontalDrag = 0f
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { _, dragAmount -> horizontalDrag += dragAmount },
                        onDragEnd = {
                            val currentIndex = nativeStoreTabs.indexOfFirst { it.id == activeTab }
                            // Finger moves right-to-left (negative drag) -> next Store tab.
                            // Finger moves left-to-right (positive drag) -> previous Store tab.
                            if (horizontalDrag < -45f && currentIndex < nativeStoreTabs.lastIndex) {
                                activeTab = nativeStoreTabs[currentIndex + 1].id
                            } else if (horizontalDrag > 45f && currentIndex > 0) {
                                activeTab = nativeStoreTabs[currentIndex - 1].id
                            }
                            horizontalDrag = 0f
                        },
                        onDragCancel = { horizontalDrag = 0f }
                    )
                },
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(visibleItems, key = { it.id }) { item ->
                    val theme = item.tab == "Theme"
                    val isVehicle = item.tab == "Vehicle"
                    val isFrame = item.tab == "Avatar Frame"
                    val isVoice = item.tab == "Voice"
                    val owned = ownedIds.contains(item.id) || item.ownedByDefault
                    val equipped = equippedIds.contains(item.id)

                    Box(
                        Modifier.fillMaxWidth().then(if (theme) Modifier.height(200.dp) else Modifier.wrapContentHeight())
                            .clip(RoundedCornerShape(5.dp))
                            .background(CardGrey) // Halka grey background
                    ) {
                        if (theme) {
                            AsyncImage(
                                model = nativeStoreAssetUrl(item, item.image),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Column(
                            Modifier.fillMaxWidth().padding(5.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (!theme) {
                                if (isVoice) {
                                    // Static Voice wave inside card only — no avatar, no animation
                                    Box(
                                        modifier = Modifier.fillMaxWidth().height(120.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        EmeraldVoiceWaveEffect(
                                            animated = false,
                                            modifier = Modifier.size(114.dp)
                                        )
                                    }
                                } else {
                                    // Bag mirrors the Store card preview. Vehicle MP4s are reserved
                                    // for the existing room-entry toast, never played inside Bag.
                                    val previewVideo = item.image
                                    if (previewVideo.endsWith(".mp4", ignoreCase = true)) {
                                        val isV1 = item.id == "v1"
                                        val removeGreen = if (isVehicle) isV1 else true
                                        val mediaModifier = if (isVehicle && !isV1) {
                                            Modifier.fillMaxWidth().wrapContentHeight()
                                        } else {
                                            Modifier.fillMaxWidth().height(if (isVehicle || isFrame) 120.dp else 80.dp)
                                        }
                                        val gridBottomFade = if (isVehicle) {
                                            when (item.id) {
                                                "v2", "v4", "v6" -> 0.15f
                                                else -> 0.1f
                                            }
                                        } else 0f
                                        StoreVideoPreview(
                                            url = nativeStoreAssetUrl(item, previewVideo),
                                            modifier = mediaModifier,
                                            pause = true,
                                            topFade = if (isVehicle) 0.1f else 0f,
                                            bottomFade = gridBottomFade,
                                            removeGreenScreen = removeGreen,
                                            mute = item.id == "a8",
                                            loop = !isVehicle,
                                            cacheVideo = isVehicle
                                        )
                                    } else {
                                        val mediaModifier = Modifier.fillMaxWidth().height(if (isVehicle || isFrame) 120.dp else 80.dp)
                                        AsyncImage(model = StoreAssetBase + item.image, contentDescription = item.name, modifier = mediaModifier)
                                    }
                                }
                            } else {
                                Spacer(Modifier.weight(1f).fillMaxWidth().height(80.dp))
                            }

                            // Image based stars
                            Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                                repeat(5) { index ->
                                    if (index < item.stars) {
                                        AsyncImage(model = StoreAssetBase + "file_0000000004e881fa84e61b8616f8eaf8.png", contentDescription = "Star", modifier = Modifier.size(14.dp))
                                    } else {
                                        AsyncImage(model = StoreAssetBase + "file_0000000004e881fa84e61b8616f8eaf8.png", contentDescription = "Star", modifier = Modifier.size(14.dp).alpha(0.3f))
                                    }
                                }
                            }
                            if (isBag) {
                                val expiry = expiryById[item.id] ?: 0L
                                val remaining = (expiry - clockNow).coerceAtLeast(0L) / 1000L
                                val days = remaining / 86400L
                                val hours = (remaining % 86400L) / 3600L
                                Text(
                                    if (expiry > clockNow) "${days}d ${hours}h" else "Expired",
                                    color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(top = 1.dp, bottom = 2.dp)
                                )
                            }

                            Row(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                AsyncImage(model = StoreAssetBase + "file_00000000e56882119c217d508b6733dc.png", contentDescription = "Coins", modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                // Text color updated for dark theme & Added Duration next to price
                                Text("%,d".format(item.price), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                                if (!isBag) Text(" / " + item.duration, color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                            Row(
                                Modifier.fillMaxWidth().height(30.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, StoreBlue, CircleShape)
                                    .background(CardGrey)
                            ) {
                                if (!isBag) {
                                    Box(
                                        Modifier.weight(1f).fillMaxHeight()
                                            .clickable { sendItem = item },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Send", color = Color(0xFF60A5FA), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Box(
                                    Modifier.weight(1f).fillMaxHeight()
                                        .background(StoreBlue)
                                        .clickable {
                                            if (owned) {
                                                if (!purchaseBusy && storeInventoryUserId.isNotBlank()) {
                                                    val nextEquipped = !equipped
                                                    val previousTabEquipped = equippedIds.filter { id ->
                                                        ((0 until serverInventory.length()).mapNotNull { serverInventory.optJSONObject(it) }
                                                            .firstOrNull { it.optString("itemId") == id }?.optString("tab")
                                                            ?: nativeStoreItems.firstOrNull { it.id == id }?.tab) == item.tab
                                                    }
                                                    // Optimistic UI: update Use/Remove immediately; restore only if server rejects it.
                                                    if (nextEquipped) {
                                                        equippedIds.removeAll { id -> nativeStoreItems.find { it.id == id }?.tab == item.tab }
                                                        equippedIds.add(item.id)
                                                    } else equippedIds.remove(item.id)
                                                    updateNativeStoreCachedEquip(appContext, storeInventoryUserId, item.id, item.tab, nextEquipped)
                                                    purchaseBusy = true
                                                    storeScope.launch {
                                                        val result = withContext(Dispatchers.IO) { runCatching {
                                                            val c = (URL("https://zyora-server.onrender.com/api/store/equip").openConnection() as HttpURLConnection).apply {
                                                                requestMethod="POST"; doOutput=true; connectTimeout=10000; readTimeout=10000
                                                                setRequestProperty("Content-Type", "application/json")
                                                            }
                                                            val body = JSONObject().put("userId", storeInventoryUserId).put("itemId", item.id).put("equipped", nextEquipped).toString()
                                                            c.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                                                            val code = c.responseCode
                                                            val stream = if (code in 200..299) c.inputStream else c.errorStream
                                                            val json = JSONObject(stream.bufferedReader().use { it.readText() }); c.disconnect()
                                                            if (code !in 200..299 || !json.optBoolean("success")) throw IllegalStateException(json.optString("error", "Could not use item"))
                                                        } }
                                                        result.onSuccess {
                                                            if (item.tab == "Avatar Frame") nativeAvatarFrameRefreshVersions[storeInventoryUserId] = (nativeAvatarFrameRefreshVersions[storeInventoryUserId] ?: 0L) + 1L
                                                            if (item.tab == "Voice") nativeVoiceWaveRefreshVersions[storeInventoryUserId] = (nativeVoiceWaveRefreshVersions[storeInventoryUserId] ?: 0L) + 1L
                                                        }.onFailure {
                                                            equippedIds.removeAll { id -> nativeStoreItems.find { it.id == id }?.tab == item.tab }
                                                            equippedIds.addAll(previousTabEquipped)
                                                            previousTabEquipped.forEach { previousId ->
                                                                updateNativeStoreCachedEquip(appContext, storeInventoryUserId, previousId, item.tab, true)
                                                            }
                                                            updateNativeStoreCachedEquip(appContext, storeInventoryUserId, item.id, item.tab, false)
                                                            purchaseMessage = if (nextEquipped) "Could not use item" else "Could not remove item"
                                                        }
                                                        purchaseBusy = false
                                                    }
                                                } else if (storeUserId.isBlank()) purchaseMessage = "Please sign in to use items"
                                            } else if (!purchaseBusy && storeInventoryUserId.isNotBlank()) {
                                                // Buy exactly this card immediately; no purchase sheet.
                                                // Show Owned immediately; roll back the optimistic state if payment fails.
                                                val wasAlreadyOwned = ownedIds.contains(item.id)
                                                ownedIds.add(item.id)
                                                purchaseBusy = true
                                                purchaseItemId = item.id
                                                storeScope.launch {
                                                    val result = withContext(Dispatchers.IO) {
                                                        runCatching {
                                                            val c = (URL("https://zyora-server.onrender.com/api/store/purchase").openConnection() as HttpURLConnection).apply {
                                                                requestMethod = "POST"; doOutput = true; connectTimeout = 12000; readTimeout = 12000
                                                                setRequestProperty("Content-Type", "application/json")
                                                            }
                                                            val body = JSONObject().put("userId", storeInventoryUserId).put("walletUserId", storeUserId).put("itemId", item.id)
                                                                .put("name", item.name).put("tab", item.tab).put("image", item.image)
                                                                .put("video", item.video ?: "").put("duration", item.duration).put("price", item.price).toString()
                                                            c.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                                                            val code = c.responseCode
                                                            val stream = if (code in 200..299) c.inputStream else c.errorStream
                                                            val json = JSONObject(stream.bufferedReader().use { it.readText() })
                                                            c.disconnect()
                                                            if (json.has("balance")) withContext(Dispatchers.Main) {
                                                                balance = json.optLong("balance", balance.toLong()).coerceAtLeast(0L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                                                                walletCachePrefs.edit().putLong("balance_$storeUserId", balance.toLong()).apply()
                                                            }
                                                            if (code !in 200..299 || !json.optBoolean("success")) throw IllegalStateException(json.optString("error", "Purchase failed"))
                                                            json
                                                        }
                                                    }
                                                    result.onSuccess { json ->
                                                        balance = json.optLong("balance", balance.toLong()).coerceAtLeast(0L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                                                        walletCachePrefs.edit().putLong("balance_$storeUserId", balance.toLong()).apply()
                                                        val bought = json.optJSONObject("item")
                                                        if (!ownedIds.contains(item.id)) ownedIds.add(item.id)
                                                        if (bought != null) {
                                                            expiryById[item.id] = bought.optLong("expiresAt")
                                                            upsertNativeStoreCachedItem(appContext, storeInventoryUserId, bought)
                                                        }
                                                        purchaseMessage = "Purchase successful"
                                                        if (item.tab == "Avatar Frame") nativeAvatarFrameRefreshVersions[storeUserId] = (nativeAvatarFrameRefreshVersions[storeUserId] ?: 0L) + 1L
                                                    }.onFailure {
                                                        if (!wasAlreadyOwned) ownedIds.remove(item.id)
                                                        purchaseMessage = if (it.message?.contains("insufficient", ignoreCase = true) == true) "Insufficient balance" else it.message ?: "Purchase failed"
                                                    }
                                                    purchaseBusy = false
                                                    purchaseItemId = null
                                                }
                                            } else if (storeUserId.isBlank()) purchaseMessage = "Please sign in to buy"
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        if (owned) {
                                            if (isBag) if (equipped) "Remove" else "Use"
                                            else "Owned"
                                        } else "Buy",
                                        color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Top right white play button inside black/10 circle
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.1f))
                                .clickable { tryItem = item },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    purchaseMessage?.let { message ->
        LaunchedEffect(message) { delay(2500); if (purchaseMessage == message) purchaseMessage = null }
        if (message == "Using" || message == "Removed" || message == "Gift Sended") {
            Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.BottomCenter) {
                Text(message, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.background(Color(0xFF242424), RoundedCornerShape(8.dp)).padding(horizontal = 14.dp, vertical = 9.dp))
            }
        } else {
            Dialog(onDismissRequest = { purchaseMessage = null }) {
                Box(
                    Modifier.fillMaxWidth().padding(horizontal = 48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.70f))
                        .padding(horizontal = 22.dp, vertical = 17.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(message, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                }
            }
        }
    }

    if (tryItem != null) {
        val item = tryItem!!
        Dialog(
            onDismissRequest = { tryItem = null },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Transparent) // Remove black drop background here
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { tryItem = null },
                contentAlignment = Alignment.Center
            ) {
                when (item.tab) {
                    "Theme" -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(enabled = false) {}) {
                            Box(Modifier.width(230.dp).height(480.dp).border(4.dp, Color.Yellow, RoundedCornerShape(24.dp)).clip(RoundedCornerShape(24.dp)).background(Color.Transparent)) {
                                AsyncImage(model = StoreAssetBase + item.image, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            }

                            // Image based stars for dialog
                            Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                repeat(5) { index ->
                                    if (index < item.stars) {
                                        AsyncImage(model = StoreAssetBase + "file_0000000004e881fa84e61b8616f8eaf8.png", contentDescription = "Star", modifier = Modifier.size(20.dp))
                                    } else {
                                        AsyncImage(model = StoreAssetBase + "file_0000000004e881fa84e61b8616f8eaf8.png", contentDescription = "Star", modifier = Modifier.size(20.dp).alpha(0.3f))
                                    }
                                }
                            }

                            Text(item.name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                    "Avatar Frame" -> {
                        Box(
                            Modifier.width(300.dp).height(300.dp).background(CardGrey, RoundedCornerShape(16.dp)).clickable(enabled = false) {},
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Spacer(Modifier.height(40.dp))
                                Text(item.name, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                                    Text("◷", color = Color.LightGray, fontSize = 18.sp)
                                    Spacer(Modifier.width(4.dp))
                                    Text(item.duration, color = Color.LightGray, fontSize = 13.sp)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)) {
                                    AsyncImage(model = StoreAssetBase + "file_00000000e56882119c217d508b6733dc.png", contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("%,d".format(item.price), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                                val isOwned = ownedIds.contains(item.id)
                                val isEquipped = equippedIds.contains(item.id)
                                Button(
                                    onClick = {
                                        // Route preview actions through the same server-backed
                                        // purchase/equip sheet as the Store card. Never mutate
                                        // wallet or inventory locally from the preview.
                                        tryItem = null
                                    },
                                    modifier = Modifier.fillMaxWidth().height(42.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = StoreBlue)
                                ) {
                                    Text(if (isOwned) if (isEquipped) "Equipped" else "Equip" else "Buy", fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = { tryItem = null },
                                    modifier = Modifier.fillMaxWidth().height(42.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.LightGray),
                                    border = BorderStroke(1.dp, Color.Gray)
                                ) {
                                    Text("Cancel", fontWeight = FontWeight.Bold)
                                }
                            }

                            val frameOffsetY = when (item.id) {
                                "a3" -> (-72).dp // Moved slightly up from -65.dp
                                "a5" -> (-60).dp
                                "a6" -> (-62).dp
                                else -> (-55).dp
                            }

                            val frameSize = when (item.id) {
                                "a3" -> 130.dp
                                "a6" -> 120.dp
                                else -> 110.dp
                            }

                            Box(Modifier.align(Alignment.TopCenter).offset(y = frameOffsetY).size(frameSize), contentAlignment = Alignment.Center) {
                                AsyncImage(model = previewAvatar, contentDescription = "Your profile avatar", modifier = Modifier.size(80.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                                val source = item.video ?: item.image
                                if (source.endsWith(".mp4", true)) {
                                    StoreVideoPreview(
                                        StoreAssetBase + source,
                                        Modifier.fillMaxSize(),
                                        pause = false,
                                        removeGreenScreen = true,
                                        mute = item.id == "a8",
                                        loop = item.tab != "Vehicle",
                                        cacheVideo = item.tab == "Vehicle"
                                    )
                                } else {
                                    AsyncImage(model = StoreAssetBase + source, contentDescription = null, modifier = Modifier.fillMaxSize())
                                }
                            }
                        }
                    }
                    "Voice" -> {
                        Box(
                            Modifier.width(300.dp).height(300.dp).background(CardGrey, RoundedCornerShape(16.dp)).clickable(enabled = false) {},
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Spacer(Modifier.height(40.dp))
                                Text(item.name, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                                    Text("◷", color = Color.LightGray, fontSize = 18.sp)
                                    Spacer(Modifier.width(4.dp))
                                    Text(item.duration, color = Color.LightGray, fontSize = 13.sp)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)) {
                                    AsyncImage(model = StoreAssetBase + "file_00000000e56882119c217d508b6733dc.png", contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("%,d".format(item.price), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                                val isOwned = ownedIds.contains(item.id)
                                val isEquipped = equippedIds.contains(item.id)
                                Button(
                                    onClick = { tryItem = null },
                                    modifier = Modifier.fillMaxWidth().height(42.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = StoreBlue)
                                ) {
                                    Text(if (isOwned) if (isEquipped) "Equipped" else "Equip" else "Buy", fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = { tryItem = null },
                                    modifier = Modifier.fillMaxWidth().height(42.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.LightGray),
                                    border = BorderStroke(1.dp, Color.Gray)
                                ) {
                                    Text("Cancel", fontWeight = FontWeight.Bold)
                                }
                            }

                            // Animated voice wave radiating from BEHIND the user avatar
                            Box(
                                Modifier.align(Alignment.TopCenter).offset(y = (-72).dp).size(144.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                EmeraldVoiceWaveEffect(
                                    animated = true,
                                    modifier = Modifier.fillMaxSize()
                                )
                                AsyncImage(model = previewAvatar, contentDescription = "Your profile avatar", modifier = Modifier.size(76.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                            }
                        }
                    }
                    else -> {
                        val alignBottom = item.id == "v9" || item.id == "v7" || item.id == "v2" || item.id == "v10"

                        val topFadeVal = when (item.id) {
                            "v10" -> 0.05f
                            "v9", "v7", "v6" -> 0.15f
                            else -> 0.10f
                        }
                        val bottomFadeVal = when (item.id) {
                            "v10" -> 0.05f
                            "v2", "v4", "v6" -> 0.15f
                            "v9", "v7" -> 0.0f
                            else -> 0.10f
                        }

                        Box(
                            Modifier
                                .fillMaxSize()
                                .clickable(enabled = false) {},
                            contentAlignment = if (alignBottom) Alignment.BottomCenter else Alignment.Center
                        ) {
                            val isV1 = item.id == "v1"
                            val source = item.video ?: item.image
                            if (source.endsWith(".mp4", true)) {
                                StoreVideoPreview(
                                    url = nativeStoreAssetUrl(item, source),
                                    modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                                    pause = false,
                                    topFade = topFadeVal,
                                    bottomFade = bottomFadeVal,
                                    removeGreenScreen = isV1,
                                    mute = item.id == "a8",
                                    loop = item.tab != "Vehicle",
                                    cacheVideo = item.tab == "Vehicle"
                                )
                            } else {
                                AsyncImage(model = StoreAssetBase + source, contentDescription = item.name, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
            }
        }
    }

    if (sendItem != null) {
        ModalBottomSheet(
            onDismissRequest = { sendItem = null; searchQuery = "" },
            containerColor = Color(0xFF1E1E1E),
            dragHandle = {}, // Top line (drag handle) ko hata diya
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp) // Corner md (kam) kar diya
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.5f) // 50Vh set kar diya
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Top Left Back Icon & Middle Heading "Gift"
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .clickable { sendItem = null; searchQuery = "" }
                            .padding(4.dp)
                    )
                    Text("Gift", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Input bar (Contact text remove kar diya and corner md (kam) kar diya)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp)) // pehle CircleShape tha, ab thoda kam round hai
                        .background(Color(0xFF2A2A2A))
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⌕", fontSize = 20.sp, color = Color.Gray)
                    Spacer(Modifier.width(8.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = Color.White),
                        decorationBox = { inner ->
                            if (searchQuery.isEmpty()) Text("Search friends...", fontSize = 14.sp, color = Color(0xFF9CA3AF))
                            inner()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                giftSearchUsers.forEach { target ->
                    val targetName = target.optString("name", "User")
                    val targetImage = target.optString("image")
                    Row(Modifier.fillMaxWidth().clickable { giftSelectedUser = target }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(model = targetImage, contentDescription = targetName, modifier = Modifier.size(44.dp).clip(CircleShape))
                        Column(Modifier.padding(start = 12.dp).weight(1f)) {
                            Text(targetName, fontSize = 14.sp, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("ID: ${target.optString("accountId")}", fontSize = 11.sp, color = Color.Gray)
                        }
                        if (giftSelectedUser?.optString("accountId") == target.optString("accountId")) Text("Selected", color = Color(0xFF60A5FA), fontSize = 12.sp)
                    }
                }
                giftSelectedUser?.let { target ->
                    Spacer(Modifier.height(10.dp))
                    Box(Modifier.fillMaxWidth().height(42.dp).clip(RoundedCornerShape(10.dp)).background(StoreBlue).clickable(enabled = !giftBusy) {
                        val gift = sendItem ?: return@clickable
                        giftBusy = true
                        storeScope.launch {
                            val sent = withContext(Dispatchers.IO) { runCatching {
                                val connection = (URL("https://zyora-server.onrender.com/api/store/gift").openConnection() as HttpURLConnection).apply {
                                    requestMethod = "POST"; doOutput = true; connectTimeout = 15000; readTimeout = 15000
                                    setRequestProperty("Content-Type", "application/json")
                                }
                                val payload = JSONObject()
                                    .put("senderId", storeInventoryUserId).put("walletUserId", storeUserId)
                                    .put("recipientAccountId", target.optString("accountId"))
                                    .put("itemId", gift.id).put("name", gift.name).put("tab", gift.tab)
                                    .put("image", gift.image).put("video", gift.video ?: "")
                                    .put("duration", gift.duration).put("price", gift.price).toString()
                                connection.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
                                val status = connection.responseCode
                                val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                                val response = JSONObject(stream.bufferedReader().use { it.readText() })
                                connection.disconnect()
                                if (response.has("balance")) withContext(Dispatchers.Main) {
                                    balance = response.optLong("balance", balance.toLong()).coerceAtLeast(0L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                                    walletCachePrefs.edit().putLong("balance_$storeUserId", balance.toLong()).apply()
                                }
                                if (status !in 200..299 || !response.optBoolean("success")) throw IllegalStateException(response.optString("error", "Gift failed"))
                                response
                            } }
                            sent.onSuccess {
                                purchaseMessage = "Gift Sended"
                                sendItem = null; searchQuery = ""; giftSelectedUser = null
                            }.onFailure { purchaseMessage = if (it.message?.contains("insufficient", true) == true) "Insufficient balance" else it.message ?: "Gift failed" }
                            giftBusy = false
                        }
                    }, contentAlignment = Alignment.Center) {
                        Text(if (giftBusy) "Sending..." else "Send", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(18.dp))
            }
        }
    }
}

@Composable
private fun StoreChromaImage(
    url: String,
    description: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit
) {
    var bitmap by remember(url) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(url) {
        bitmap = withContext(Dispatchers.IO) {
            runCatching {
                val connection = (URL(url).openConnection() as java.net.HttpURLConnection).apply {
                    connectTimeout = 7_000
                    readTimeout = 9_000
                    useCaches = true
                }
                val original = try {
                    connection.inputStream.use { BitmapFactory.decodeStream(it) }
                } finally {
                    connection.disconnect()
                }
                if (original == null) null else {
                    val pixels = IntArray(original.width * original.height)
                    original.getPixels(pixels, 0, original.width, 0, 0, original.width, original.height)
                    for (i in pixels.indices) {
                        val color = pixels[i]
                        val a = (color ushr 24) and 0xFF
                        val r = (color ushr 16) and 0xFF
                        val g = (color ushr 8) and 0xFF
                        val b = color and 0xFF

                        val maxRB = max(r, b)
                        val greenness = g - maxRB

                        val edge0 = 10.2f
                        val edge1 = 38.25f
                        val t = ((greenness.toFloat() - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
                        val blend = t * t * (3f - 2f * t)

                        val despilledG = min(g, maxRB + 13)

                        val finalAlpha = (a * (1f - blend)).roundToInt().coerceIn(0, 255)
                        pixels[i] = (finalAlpha shl 24) or (r shl 16) or (despilledG shl 8) or b
                    }
                    Bitmap.createBitmap(pixels, original.width, original.height, Bitmap.Config.ARGB_8888)
                }
            }.getOrNull()
        }
    }
    val imageBitmap = bitmap
    if (imageBitmap != null) {
        Image(bitmap = imageBitmap.asImageBitmap(), contentDescription = description, modifier = modifier, contentScale = contentScale)
    } else {
        Box(modifier, contentAlignment = Alignment.Center) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = StoreBlue)
        }
    }
}

@Composable
fun StoreVideoPreview(
    url: String,
    modifier: Modifier = Modifier,
    pause: Boolean = false,
    topFade: Float = 0f,
    bottomFade: Float = 0f,
    removeGreenScreen: Boolean = false,
    removeBlackBackground: Boolean = false,
    mute: Boolean = false, // New Parameter for mute
    loop: Boolean = true,
    onComplete: (() -> Unit)? = null,
    cacheVideo: Boolean = false
) {
    val context = LocalContext.current.applicationContext
    val playbackUrl by produceState(initialValue = if (cacheVideo) "" else url, url, cacheVideo) {
        if (!cacheVideo) {
            value = url
        } else {
            value = withContext(Dispatchers.IO) {
                runCatching {
                    val digest = MessageDigest.getInstance("SHA-256")
                        .digest(url.toByteArray())
                        .joinToString("") { "%02x".format(it) }
                    val cachedFile = File(context.filesDir, "store-video-$digest.mp4")
                    if (!cachedFile.isFile || cachedFile.length() <= 0L) {
                        // Use a unique temporary file: Store grid, detail and room previews
                        // can request the same asset at the same time.
                        val tempFile = File.createTempFile("store-video-$digest-", ".tmp", context.filesDir)
                        try {
                            val connection = URL(url).openConnection().apply {
                                connectTimeout = 15000
                                readTimeout = 30000
                            }
                            try {
                                connection.getInputStream().use { input ->
                                    tempFile.outputStream().use { output -> input.copyTo(output) }
                                }
                            } finally {
                                (connection as? HttpURLConnection)?.disconnect()
                            }
                            check(tempFile.isFile && tempFile.length() > 0L) { "Downloaded vehicle video is empty" }
                            if (!tempFile.renameTo(cachedFile)) {
                                tempFile.copyTo(cachedFile, overwrite = true)
                            }
                        } finally {
                            tempFile.delete()
                        }
                    }
                    check(cachedFile.isFile && cachedFile.length() > 0L) { "Cached vehicle video is unavailable" }
                    cachedFile.toURI().toString()
                }.getOrElse { url }
            }
        }
    }
    var isVideoReady by remember(url) { mutableStateOf(false) }
    var videoRatio by remember { mutableStateOf<Float?>(null) }

    val chromaMatrix = remember {
        ColorMatrix(floatArrayOf(
            1f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 0f,
            2.5f, -4f, 2.5f, 1f, 0f
        ))
    }

    // Black-background removal for Family avatar-frame videos.
    // RGB stays unchanged; only alpha is derived from brightness so black
    // becomes transparent while the frame animation remains visible.
    val blackBackgroundMatrix = remember {
        ColorMatrix(floatArrayOf(
            1f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 0f,
            1.5f, 1.5f, 1.5f, 0f, -0.08f
        ))
    }

    val fadeModifier = if (topFade > 0f || bottomFade > 0f) {
        Modifier
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()

                val stops = mutableListOf<Pair<Float, Color>>()
                if (topFade > 0f) {
                    stops.add(0.0f to Color.Transparent)
                    stops.add(topFade to Color.Black)
                } else {
                    stops.add(0.0f to Color.Black)
                }

                if (bottomFade > 0f) {
                    stops.add(1f - bottomFade to Color.Black)
                    stops.add(1.0f to Color.Transparent)
                } else {
                    stops.add(1.0f to Color.Black)
                }

                drawRect(
                    brush = Brush.verticalGradient(*stops.toTypedArray()),
                    blendMode = BlendMode.DstIn
                )
            }
    } else Modifier

    val ratioModifier = videoRatio?.let { Modifier.aspectRatio(it) } ?: Modifier

    Box(
        modifier = modifier
            .then(fadeModifier)
            .then(ratioModifier)
            .alpha(if (isVideoReady) 1f else 0f)
            .graphicsLayer {
                if (removeGreenScreen || removeBlackBackground) {
                    compositingStrategy = CompositingStrategy.Offscreen
                    colorFilter = ColorFilter.colorMatrix(
                        if (removeBlackBackground) blackBackgroundMatrix else chromaMatrix
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (playbackUrl.isNotBlank()) key(playbackUrl) {
            AndroidView(
                factory = { ctx ->
                    val textureView = TextureView(ctx)
                    textureView.isOpaque = false

                    val mediaPlayer = MediaPlayer()
                    val released = AtomicBoolean(false)
                    var outputSurface: Surface? = null
                    fun releasePlayer() {
                        if (released.compareAndSet(false, true)) {
                            runCatching { mediaPlayer.setSurface(null) }
                            runCatching { mediaPlayer.release() }
                            outputSurface?.release()
                            outputSurface = null
                        }
                    }
                    textureView.tag = ::releasePlayer

                    textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                            try {
                                outputSurface = Surface(surface)
                                mediaPlayer.setSurface(outputSurface)
                                mediaPlayer.setDataSource(ctx, Uri.parse(playbackUrl))
                                mediaPlayer.isLooping = loop
                                if (!loop && onComplete != null) {
                                    mediaPlayer.setOnCompletionListener { if (!released.get()) onComplete() }
                                }
                                mediaPlayer.setOnVideoSizeChangedListener { _, w, h ->
                                    if (w > 0 && h > 0) videoRatio = w.toFloat() / h.toFloat()
                                }
                                mediaPlayer.setOnInfoListener { _, what, _ ->
                                    if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                                        isVideoReady = true
                                        true
                                    } else false
                                }
                                mediaPlayer.setOnPreparedListener {
                                    if (released.get()) return@setOnPreparedListener
                                    if (mute) it.setVolume(0f, 0f)
                                    if (!pause) it.start() else {
                                        it.seekTo(100)
                                        isVideoReady = true
                                    }
                                }
                                mediaPlayer.setOnErrorListener { _, _, _ ->
                                    isVideoReady = false
                                    true
                                }
                                mediaPlayer.prepareAsync()
                            } catch (e: Exception) {
                                e.printStackTrace()
                                releasePlayer()
                            }
                        }
                        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
                        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                            releasePlayer()
                            return true
                        }
                        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
                    }
                    textureView
                },
                modifier = Modifier.fillMaxSize(),
                onRelease = { view ->
                    (view.tag as? (() -> Unit))?.invoke()
                }
            )
        }
    }
}

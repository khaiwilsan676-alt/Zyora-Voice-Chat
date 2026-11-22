package com.hurry.voicechat

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil3.compose.AsyncImage
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

private object LeaderboardChromaCache {
    val images = object : LruCache<String, ImageBitmap>(16 * 1024) {
        override fun sizeOf(key: String, value: ImageBitmap): Int =
            ((value.width.toLong() * value.height.toLong() * 4L) / 1024L)
                .coerceAtLeast(1L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }
}

// BLACK CARD ORNATE GOLDEN CORNER BORDER (Same as Image)
@Composable
private fun OrnateCardCornerDecoration(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val goldLight = Color(0xFFF3DFA2)
        val goldMain = Color(0xFFD4AF37)
        val goldDark = Color(0xFF8C6B23)
        val strokeThin = Stroke(width = 2.6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val strokeMed = Stroke(width = 3.6f, cap = StrokeCap.Round, join = StrokeJoin.Round)

        // Inner top horizontal subtle golden line
        drawLine(
            brush = Brush.horizontalGradient(
                0f to Color.Transparent,
                0.15f to goldMain.copy(alpha = 0.7f),
                0.5f to goldLight.copy(alpha = 0.85f),
                0.85f to goldMain.copy(alpha = 0.7f),
                1f to Color.Transparent
            ),
            start = Offset(w * 0.14f, 12f),
            end = Offset(w * 0.86f, 12f),
            strokeWidth = 1.8f
        )

        // Helper to draw symmetrical corner flourish (dir = 1f for left, -1f for right)
        fun drawCorner(isRight: Boolean) {
            fun px(x: Float) = if (isRight) w - x else x

            // Outer sweeping top-to-side curl
            val mainCurl = Path().apply {
                moveTo(px(14f), 20f)
                cubicTo(px(34f), 10f, px(64f), 8f, px(108f), 14f)
            }
            drawPath(mainCurl, color = goldLight, style = strokeMed)

            // Secondary top leaf curve
            val topLeaf = Path().apply {
                moveTo(px(18f), 16f)
                cubicTo(px(42f), 28f, px(66f), 22f, px(84f), 12f)
            }
            drawPath(topLeaf, color = goldMain, style = strokeThin)

            // Inner small top scroll
            val innerTopScroll = Path().apply {
                moveTo(px(48f), 16f)
                cubicTo(px(58f), 6f, px(72f), 6f, px(70f), 16f)
            }
            drawPath(innerTopScroll, color = goldLight, style = strokeThin)

            // Outer vertical sweeping curve going down the side
            val sideSweep = Path().apply {
                moveTo(px(14f), 20f)
                cubicTo(px(10f), 60f, px(16f), 110f, px(12f), 175f)
            }
            drawPath(
                sideSweep,
                brush = Brush.verticalGradient(
                    0f to goldLight,
                    0.7f to goldMain,
                    1f to Color.Transparent
                ),
                style = strokeMed
            )

            // Inner side leaf 1 (upper)
            val sideLeaf1 = Path().apply {
                moveTo(px(15f), 24f)
                cubicTo(px(36f), 36f, px(44f), 62f, px(26f), 86f)
                cubicTo(px(20f), 70f, px(24f), 46f, px(15f), 24f)
            }
            drawPath(sideLeaf1, color = goldLight, style = strokeThin)

            // Inner side leaf 2 (middle)
            val sideLeaf2 = Path().apply {
                moveTo(px(14f), 78f)
                cubicTo(px(32f), 92f, px(34f), 122f, px(16f), 148f)
            }
            drawPath(sideLeaf2, color = goldMain, style = strokeThin)

            // Inner side leaf 3 (lower taper)
            val sideLeaf3 = Path().apply {
                moveTo(px(14f), 118f)
                cubicTo(px(26f), 134f, px(24f), 156f, px(13f), 178f)
            }
            drawPath(
                sideLeaf3,
                brush = Brush.verticalGradient(
                    0f to goldMain,
                    1f to Color.Transparent
                ),
                style = strokeThin
            )

            // Corner accent dot
            drawCircle(
                color = goldDark,
                radius = 3.2f,
                center = Offset(px(20f), 20f)
            )
            drawCircle(
                color = goldLight,
                radius = 1.8f,
                center = Offset(px(20f), 20f)
            )
        }

        drawCorner(isRight = false)
        drawCorner(isRight = true)
    }
}

// COUNTDOWN KE UPAR WALA ORNATE GOLDEN DESIGN (Same as Image)
@Composable
private fun CountdownTopOrnament(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val goldLight = Color(0xFFF3DFA2)
        val goldMain = Color(0xFFD4AF37)

        val ornamentHalfWidth = 54f
        val dotsOffset1 = ornamentHalfWidth + 10f
        val dotsOffset2 = ornamentHalfWidth + 20f
        val lineStartOffset = ornamentHalfWidth + 30f

        // Left fading golden line
        drawLine(
            brush = Brush.horizontalGradient(
                0f to Color.Transparent,
                0.45f to goldMain.copy(alpha = 0.65f),
                1f to goldLight
            ),
            start = Offset(w * 0.08f, cy),
            end = Offset(cx - lineStartOffset, cy),
            strokeWidth = 2.2f,
            cap = StrokeCap.Round
        )

        // Right fading golden line
        drawLine(
            brush = Brush.horizontalGradient(
                0f to goldLight,
                0.55f to goldMain.copy(alpha = 0.65f),
                1f to Color.Transparent
            ),
            start = Offset(cx + lineStartOffset, cy),
            end = Offset(w * 0.92f, cy),
            strokeWidth = 2.2f,
            cap = StrokeCap.Round
        )

        // Dots on left & right
        drawCircle(color = goldLight, radius = 3.2f, center = Offset(cx - dotsOffset1, cy))
        drawCircle(color = goldMain, radius = 2.2f, center = Offset(cx - dotsOffset2, cy))
        drawCircle(color = goldLight, radius = 3.2f, center = Offset(cx + dotsOffset1, cy))
        drawCircle(color = goldMain, radius = 2.2f, center = Offset(cx + dotsOffset2, cy))

        // Center symmetrical filigree swirl
        val swirlStroke = Stroke(width = 3.0f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val swirlThin = Stroke(width = 2.0f, cap = StrokeCap.Round, join = StrokeJoin.Round)

        // Left swirl
        val leftSwirl = Path().apply {
            moveTo(cx - 2f, cy)
            cubicTo(cx - 10f, cy - 14f, cx - 28f, cy - 14f, cx - 28f, cy)
            cubicTo(cx - 28f, cy + 12f, cx - 12f, cy + 12f, cx - 14f, cy - 1f)
            cubicTo(cx - 16f, cy - 7f, cx - 22f, cy - 5f, cx - 21f, cy + 2f)
        }
        drawPath(leftSwirl, color = goldLight, style = swirlStroke)

        // Left outer wing loop
        val leftWing = Path().apply {
            moveTo(cx - 28f, cy)
            cubicTo(cx - 34f, cy - 9f, cx - 46f, cy - 7f, cx - 50f, cy)
            cubicTo(cx - 46f, cy + 7f, cx - 34f, cy + 9f, cx - 28f, cy)
        }
        drawPath(leftWing, color = goldMain, style = swirlThin)

        // Right swirl (mirror)
        val rightSwirl = Path().apply {
            moveTo(cx + 2f, cy)
            cubicTo(cx + 10f, cy - 14f, cx + 28f, cy - 14f, cx + 28f, cy)
            cubicTo(cx + 28f, cy + 12f, cx + 12f, cy + 12f, cx + 14f, cy - 1f)
            cubicTo(cx + 16f, cy - 7f, cx + 22f, cy - 5f, cx + 21f, cy + 2f)
        }
        drawPath(rightSwirl, color = goldLight, style = swirlStroke)

        // Right outer wing loop (mirror)
        val rightWing = Path().apply {
            moveTo(cx + 28f, cy)
            cubicTo(cx + 34f, cy - 9f, cx + 46f, cy - 7f, cx + 50f, cy)
            cubicTo(cx + 46f, cy + 7f, cx + 34f, cy + 9f, cx + 28f, cy)
        }
        drawPath(rightWing, color = goldMain, style = swirlThin)

        // Center diamond / jewel accent
        val centerGem = Path().apply {
            moveTo(cx, cy - 5.5f)
            lineTo(cx + 4.5f, cy)
            lineTo(cx, cy + 5.5f)
            lineTo(cx - 4.5f, cy)
            close()
        }
        drawPath(centerGem, color = goldLight)
    }
}

// DEFAULT USER ICON AVATAR (Jab Top 1, 2, 3 me koi user na ho)
@Composable
private fun DefaultUserAvatarIcon(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0xFF2B1316)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize(0.62f)) {
            val iconColor = Color(0xFFB89B72)
            val headRadius = size.minDimension * 0.22f
            drawCircle(
                color = iconColor,
                radius = headRadius,
                center = Offset(size.width / 2f, size.height * 0.34f)
            )
            drawArc(
                color = iconColor,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = Offset(size.width * 0.14f, size.height * 0.58f),
                size = Size(size.width * 0.72f, size.height * 0.60f)
            )
        }
    }
}

// VIDEO PLAYER
@Composable
fun ChromaVideoPlayer(url: String, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            android.webkit.WebView(context).apply {
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                settings.javaScriptEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false

                val htmlData = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                    <style>
                        body { margin: 0; overflow: hidden; background-color: transparent !important; }
                        canvas { width: 100%; height: 100%; object-fit: contain; background: transparent; }
                        video { display: none; }
                    </style>
                    </head>
                    <body style="background:transparent;">
                    <video id="v" src="$url" autoplay loop muted playsinline crossorigin="anonymous"></video>
                    <canvas id="c"></canvas>
                    <script>
                        const v = document.getElementById('v');
                        const c = document.getElementById('c');
                        const ctx = c.getContext('2d', { willReadFrequently: true });
                        
                        v.addEventListener('loadedmetadata', function() {
                            c.width = v.videoWidth || 400;
                            c.height = v.videoHeight || 400;
                        });
                        
                        v.addEventListener('error', function() {
                            ctx.clearRect(0, 0, c.width, c.height);
                        });

                        v.addEventListener('play', function() {
                            function step() {
                                if(v.paused || v.ended) return;
                                try {
                                    ctx.drawImage(v, 0, 0, c.width, c.height);
                                } catch(e) {
                                    ctx.clearRect(0, 0, c.width, c.height);
                                }
                                requestAnimationFrame(step);
                            }
                            requestAnimationFrame(step);
                        });
                    </script>
                    </body>
                    </html>
                """.trimIndent()
                loadDataWithBaseURL(null, htmlData, "text/html", "UTF-8", null)
            }
        }
    )
}

// IMAGE LOADER 
@Composable
private fun LeaderboardChromaImage(url: String, contentDescription: String?, modifier: Modifier, contentScale: ContentScale = ContentScale.Fit) {
    var bitmap by remember(url) { mutableStateOf(LeaderboardChromaCache.images[url]) }
    LaunchedEffect(url) {
        if (bitmap == null) {
            bitmap = withContext(Dispatchers.IO) {
                runCatching {
                    LeaderboardChromaCache.images.get(url) ?: run {
                        val connection = URL(url).openConnection().apply {
                            connectTimeout = 7_000
                            readTimeout = 9_000
                            useCaches = true
                        }
                        val source = connection.getInputStream().use { BitmapFactory.decodeStream(it) }
                        if (source == null) null else {
                            source.asImageBitmap().also { LeaderboardChromaCache.images.put(url, it) }
                        }
                    }
                }.getOrNull()
            }
        }
    }
    bitmap?.let { Image(bitmap=it, contentDescription=contentDescription, modifier=modifier, contentScale=contentScale) }
}

private data class ZyoraRank(
    val id: String, val name: String, val image: String, val coins: Long,
    val country: String = "", val gender: String = "", val level: Int = 1
)

private data class ZyoraLeaderboardData(val rows: List<ZyoraRank>, val endsAt: Long)

private suspend fun loadZyoraLeaderboard(tab: String, period: String): ZyoraLeaderboardData =
    withContext(Dispatchers.IO) {
        val encodedTab = URLEncoder.encode(tab, "UTF-8")
        val encodedPeriod = URLEncoder.encode(period, "UTF-8")
        val connection = (URL("https://zyora-server.onrender.com/api/leaderboard?tab=$encodedTab&period=$encodedPeriod").openConnection() as HttpURLConnection).apply {
            connectTimeout = 10000
            readTimeout = 12000
        }
        try {
            if (connection.responseCode !in 200..299) return@withContext ZyoraLeaderboardData(emptyList(), 0L)
            val root = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            val array = root.optJSONArray("leaderboard")
            val rows = (0 until (array?.length() ?: 0)).mapNotNull { index ->
                val item = array?.optJSONObject(index) ?: return@mapNotNull null
                ZyoraRank(
                    id = item.optString(if (tab == "room") "roomId" else "userId"),
                    name = item.optString("name", "User"),
                    image = item.optString("image"),
                    coins = item.optLong("coins"),
                    country = item.optString("country"),
                    gender = item.optString("gender").ifBlank { "Male" },
                    level = item.optInt("level", 1)
                )
            }
            ZyoraLeaderboardData(rows, root.optLong("endsAt"))
        } finally {
            connection.disconnect()
        }
    }

private fun leaderboardShortName(name: String): String =
    if (name.length > 9) name.take(9) + "..." else name

private fun leaderboardCoinText(value: Long): String = when {
    value >= 1_000_000_000 -> String.format(Locale.US, "%.1fB", value / 1_000_000_000.0)
    value >= 1_000_000 -> String.format(Locale.US, "%.1fM", value / 1_000_000.0)
    value >= 1_000 -> String.format(Locale.US, "%.1fK", value / 1_000.0)
    else -> value.toString()
}

private fun leaderboardFlag(country: String): String = when (country.trim().lowercase(Locale.ROOT)) {
    "india" -> "🇮🇳"
    "singapore" -> "🇸🇬"
    "saudi arabia", "saudi" -> "🇸🇦"
    "united states", "usa", "us" -> "🇺🇸"
    "united kingdom", "uk" -> "🇬🇧"
    "pakistan" -> "🇵🇰"
    "bangladesh" -> "🇧🇩"
    "nepal" -> "🇳🇵"
    "indonesia" -> "🇮🇩"
    "philippines" -> "🇵🇭"
    "thailand" -> "🇹🇭"
    "malaysia" -> "🇲🇾"
    "uae", "united arab emirates" -> "🇦🇪"
    else -> ""
}

@Composable
fun Leaderboard(onBack:()->Unit={}, initialTab:String="honour", onOpenProfile:(RoomUser)->Unit = {}) {
    var tab by remember { mutableStateOf(initialTab) }
    var period by remember { mutableStateOf("daily") }
    var showRules by remember { mutableStateOf(false) }
    var leaderboardRows by remember { mutableStateOf<List<ZyoraRank>>(emptyList()) }
    var leaderboardEndsAt by remember { mutableLongStateOf(0L) }
    var leaderboardNow by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val tabs=listOf("honour" to "Honour","charm" to "Charm","room" to "Room")
    val periods=listOf("daily" to "Daily","weekly" to "Weekly","monthly" to "Monthly")
    val raw = "https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/"

    val topTitleGradient = Brush.horizontalGradient(
        colors = listOf(Color.White, Color(0xFFFCE28B), Color(0xFFE5B83B))
    )

    val topImage = when (tab) {
        "charm" -> "IMG_20261105_230141.png"
        "room" -> "IMG_20261105_230309.png"
        else -> "IMG_20261105_230210.png"
    }

    val context = LocalContext.current
    val authPrefs = remember { context.getSharedPreferences("hurry_auth", android.content.Context.MODE_PRIVATE) }
    val ownUserId = authPrefs.getString("userId", authPrefs.getString("id", "")).orEmpty()
    val ownName = authPrefs.getString("name", authPrefs.getString("displayName", "You")).orEmpty().ifBlank { "You" }
    val ownGender = authPrefs.getString("gender", "Male").orEmpty().ifBlank { "Male" }
    val ownAvatar = authPrefs.getString("avatar", authPrefs.getString("image", "/default-avatar.png")).orEmpty().let { if (it.startsWith("http", ignoreCase = true)) it else raw + it.removePrefix("/") }

    val ownIndex = leaderboardRows.indexOfFirst { (ownUserId.isNotBlank() && it.id == ownUserId) || it.name.equals(ownName, ignoreCase = true) }
    val ownRankText = if (ownIndex >= 0) "${ownIndex + 1}" else "-"
    val ownCoins = if (ownIndex >= 0) leaderboardRows[ownIndex].coins else 0L

    LaunchedEffect(tab, period) {
        while (true) {
            runCatching { loadZyoraLeaderboard(if (tab == "honour") "rich" else tab, period) }
                .onSuccess { leaderboardRows = it.rows; leaderboardEndsAt = it.endsAt }
                .onFailure { /* keep the last good snapshot during transient network failures */ }
            delay(10000)
        }
    }
    LaunchedEffect(Unit) {
        while (true) { leaderboardNow = System.currentTimeMillis(); delay(1000) }
    }
    val remainingSeconds = ((leaderboardEndsAt - leaderboardNow) / 1000L).coerceAtLeast(0L)
    val remainingDays = remainingSeconds / 86400L
    val leaderboardCountdown = if (period == "daily") {
        String.format(Locale.US, "%02d:%02d:%02d",
            remainingSeconds / 3600L, (remainingSeconds % 3600L) / 60L, remainingSeconds % 60L)
    } else {
        String.format(Locale.US, "%dd %02d:%02d:%02d",
            remainingDays, (remainingSeconds % 86400L) / 3600L,
            (remainingSeconds % 3600L) / 60L, remainingSeconds % 60L)
    }

    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val screenWidthDp = LocalConfiguration.current.screenWidthDp

    val podiumTopWidth = (screenWidthDp * 0.55f).coerceAtMost(240f).dp

    Box(Modifier.fillMaxSize().background(Color(0xFF1A0204))) {
        Box(Modifier.fillMaxWidth().height(screenHeight * 0.60f).align(Alignment.TopCenter)) {
            AsyncImage(
                model = raw + topImage,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.45f to Color(0x331A0204),
                        1f to Color(0xFF1A0204)
                    )
                )
            )
        }

        Column(Modifier.fillMaxSize().statusBarsPadding()) {

            // TOP ROW
            Row(Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 12.dp), verticalAlignment=Alignment.CenterVertically) {
                AsyncImage(
                    model = raw + "file_0000000051d881f5af4f9cf84a56dcd3.png",
                    contentDescription = "Back",
                    modifier = Modifier.size(32.dp).clickable(onClick=onBack),
                    contentScale = ContentScale.Fit
                )

                Row(
                    Modifier.weight(1f),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEach { (id, label) ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { tab = id }.padding(horizontal = 8.dp)
                        ) {
                            if (tab == id) {
                                Text(
                                    text = label,
                                    style = TextStyle(
                                        brush = topTitleGradient,
                                        fontSize = 16.sp,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                    )
                                )
                            } else {
                                Text(
                                    text = label,
                                    color = Color(0xFFAAAAAA),
                                    fontSize = 16.sp,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                                )
                            }
                            if (tab == id) {
                                Canvas(modifier = Modifier.offset(y = (-2).dp).size(width = 24.dp, height = 6.dp)) {
                                    drawArc(
                                        brush = topTitleGradient,
                                        startAngle = 0f,
                                        sweepAngle = 180f,
                                        useCenter = false,
                                        style = Stroke(width = 5f, cap = StrokeCap.Round)
                                    )
                                }
                            } else {
                                Spacer(Modifier.height(6.dp))
                            }
                        }
                    }
                }

                AsyncImage(
                    model = raw + "file_0000000073ec820b832b6dafb168dabe.png",
                    contentDescription = "Leaderboard rules",
                    modifier = Modifier.size(32.dp).clickable { showRules = true },
                    contentScale = ContentScale.Fit
                )
            }

            // SECOND ROW (Left Corner)
            Row(Modifier.fillMaxWidth().offset(y = (-2).dp).padding(horizontal = 12.dp, vertical = 2.dp), horizontalArrangement = Arrangement.Start) {
                Row(
                    Modifier.width(220.dp).height(30.dp)
                        .border(1.dp, Color(0xFFD4AF37), RoundedCornerShape(24.dp))
                        .background(Color(0xCC110A07), RoundedCornerShape(24.dp))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    periods.forEach { (id, label) ->
                        Box(
                            Modifier.weight(1f).fillMaxHeight()
                                .clip(RoundedCornerShape(22.dp))
                                .background(
                                    if (period == id) Brush.verticalGradient(
                                        listOf(Color(0xFFE7B865), Color(0xFFBA7627), Color(0xFF743410))
                                    ) else Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
                                )
                                .clickable { period = id },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, color = if(period == id) Color.White else Color(0xFF8A857D),
                                fontSize = 13.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                        }
                    }
                }
            }

            LazyColumn(Modifier.weight(1f).fillMaxWidth(),contentPadding=PaddingValues(bottom=0.dp),verticalArrangement=Arrangement.spacedBy(0.dp)) {
                // Podium Section
                item {
                    Column(Modifier.fillMaxWidth().padding(top=18.dp, bottom=0.dp),horizontalAlignment=Alignment.CenterHorizontally) {

                        // TOP 1 BOX 
                        val rank1 = leaderboardRows.getOrNull(0)
                        Box(
                            modifier = Modifier.width(podiumTopWidth).height(260.dp).offset(y = (-35).dp)
                                .clickable(enabled = tab != "room" && rank1 != null) { rank1?.let { onOpenProfile(RoomUser(id = it.id, uid = it.id, accountId = it.id, name = it.name, image = it.image, gender = it.gender, country = it.country)) } }, 
                            contentAlignment = Alignment.Center
                        ) {
                            val rank1Img = rank1?.image?.takeIf { it.isNotBlank() }
                            if (rank1 != null && rank1Img != null) {
                                AsyncImage(
                                    model = rank1Img,
                                    contentDescription = "Rank 1 Avatar",
                                    modifier = Modifier
                                        .offset(y = (-9).dp)
                                        .size(100.dp)
                                        .clip(RoundedCornerShape(50)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                DefaultUserAvatarIcon(
                                    modifier = Modifier
                                        .offset(y = (-9).dp)
                                        .size(100.dp)
                                )
                            }
                            LeaderboardChromaImage(
                                url = raw + "file_000000006bc882118206787e86f026f0.png",
                                contentDescription = "Rank 1 Frame",
                                modifier = Modifier.offset(y = 2.dp).requiredSize(300.dp)
                            )
                        }

                        // TOP 1 INFO
                        if (rank1 != null) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.offset(y = (-43).dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(leaderboardShortName(rank1.name), color = Color.White, fontSize = 15.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    if (tab == "room") {
                                        Text(leaderboardFlag(rank1.country), fontSize = 14.sp)
                                    } else {
                                        if (rank1.gender.isNotBlank()) {
                                            Box(Modifier.size(20.dp).clip(RoundedCornerShape(50)).background(if (rank1.gender.equals("female", true)) Color(0xFFFF5874) else Color(0xFF3B82F6)), contentAlignment = Alignment.Center) {
                                                CustomGenderIcon(rank1.gender.equals("female", true), Modifier.size(10.dp))
                                            }
                                        }
                                        Box(contentAlignment = Alignment.CenterEnd, modifier = Modifier.height(16.dp).width(34.dp)) {
                                            AsyncImage(model = raw + "IMG_20261028_004355.png", contentDescription = "Level", contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                                            Text("Lv.1", color = Color.White, fontSize = 7.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Normal, modifier = Modifier.padding(end = 4.dp).offset(x = 1.dp, y = (-2).dp))
                                        }
                                    }
                                }
                                Spacer(Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AsyncImage(model = raw + "file_00000000e56882119c217d508b6733dc.png", contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(text = leaderboardCoinText(rank1.coins), color = Color(0xFFE7B865), fontSize = 13.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(Modifier.height(5.dp)) 

                        // Rank 2 and 3 Section (Edge-to-Edge, zero left/right gap)
                        Row(
                            Modifier.fillMaxWidth().offset(y = (-42).dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            // Rank 2 Column (Avatar + Frame overlapping, Info below frame same as Top 1)
                            val rank2 = leaderboardRows.getOrNull(1)
                            Column(
                                modifier = Modifier.weight(1f).clickable(enabled = tab != "room" && rank2 != null) { rank2?.let { onOpenProfile(RoomUser(id = it.id, uid = it.id, accountId = it.id, name = it.name, image = it.image, gender = it.gender, country = it.country)) },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(modifier = Modifier.fillMaxWidth().height(175.dp), contentAlignment = Alignment.Center) {
                                    val rank2Img = rank2?.image?.takeIf { it.isNotBlank() }
                                    if (rank2 != null && rank2Img != null) {
                                        AsyncImage(
                                            model = rank2Img,
                                            contentDescription = "Rank 2 Avatar",
                                            modifier = Modifier
                                                .offset(y = (-6).dp)
                                                .size(65.dp)
                                                .clip(RoundedCornerShape(50)),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        DefaultUserAvatarIcon(
                                            modifier = Modifier
                                                .offset(y = (-6).dp)
                                                .size(65.dp)
                                        )
                                    }
                                    LeaderboardChromaImage(
                                        url = raw + "IMG_20261119_021124.png",
                                        contentDescription = "Rank 2 frame",
                                        modifier = Modifier.fillMaxWidth().requiredHeight(225.dp),
                                        contentScale = ContentScale.FillWidth
                                    )
                                }

                                // TOP 2 INFO (Frame ke niche)
                                if (rank2 != null) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.offset(y = (-14).dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                            Text(leaderboardShortName(rank2.name), color = Color.White, fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            if (tab == "room") {
                                                Text(leaderboardFlag(rank2.country), fontSize = 12.sp)
                                            } else {
                                                if (rank2.gender.isNotBlank()) {
                                                    Box(Modifier.size(20.dp).clip(RoundedCornerShape(50)).background(if (rank2.gender.equals("female", true)) Color(0xFFFF5874) else Color(0xFF3B82F6)), contentAlignment = Alignment.Center) {
                                                        CustomGenderIcon(rank2.gender.equals("female", true), Modifier.size(10.dp))
                                                    }
                                                }
                                                Box(contentAlignment = Alignment.CenterEnd, modifier = Modifier.height(16.dp).width(34.dp)) {
                                                    AsyncImage(model = raw + "IMG_20261028_004355.png", contentDescription = "Level", contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                                                    Text("Lv.1", color = Color.White, fontSize = 7.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Normal, modifier = Modifier.padding(end = 4.dp).offset(x = 1.dp, y = (-2).dp))
                                                }
                                            }
                                        }
                                        Spacer(Modifier.height(2.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            AsyncImage(model = raw + "file_00000000e56882119c217d508b6733dc.png", contentDescription = null, modifier = Modifier.size(12.dp))
                                            Spacer(Modifier.width(2.dp))
                                            Text(text = leaderboardCoinText(rank2.coins), color = Color(0xFFE7B865), fontSize = 11.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            // Rank 3 Column (Avatar + Frame overlapping, Info below frame same as Top 1)
                            val rank3 = leaderboardRows.getOrNull(2)
                            Column(
                                modifier = Modifier.weight(1f).clickable(enabled = tab != "room" && rank3 != null) { rank3?.let { onOpenProfile(RoomUser(id = it.id, uid = it.id, accountId = it.id, name = it.name, image = it.image, gender = it.gender, country = it.country)) },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(modifier = Modifier.fillMaxWidth().height(175.dp), contentAlignment = Alignment.Center) {
                                    val rank3Img = rank3?.image?.takeIf { it.isNotBlank() }
                                    if (rank3 != null && rank3Img != null) {
                                        AsyncImage(
                                            model = rank3Img,
                                            contentDescription = "Rank 3 Avatar",
                                            modifier = Modifier
                                                .offset(y = (-6).dp)
                                                .size(65.dp) 
                                                .clip(RoundedCornerShape(50)),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        DefaultUserAvatarIcon(
                                            modifier = Modifier
                                                .offset(y = (-6).dp)
                                                .size(65.dp)
                                        )
                                    }
                                    LeaderboardChromaImage(
                                        url = raw + "file_000000006f108211a4a5d64cb1ecf702.png",
                                        contentDescription = "Rank 3 frame",
                                        modifier = Modifier.fillMaxWidth().requiredHeight(215.dp),
                                        contentScale = ContentScale.FillWidth
                                    )
                                }

                                // TOP 3 INFO (Frame ke niche)
                                if (rank3 != null) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.offset(y = (-14).dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                            Text(leaderboardShortName(rank3.name), color = Color.White, fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            if (tab == "room") {
                                                Text(leaderboardFlag(rank3.country), fontSize = 12.sp)
                                            } else {
                                                if (rank3.gender.isNotBlank()) {
                                                    Box(Modifier.size(20.dp).clip(RoundedCornerShape(50)).background(if (rank3.gender.equals("female", true)) Color(0xFFFF5874) else Color(0xFF3B82F6)), contentAlignment = Alignment.Center) {
                                                        CustomGenderIcon(rank3.gender.equals("female", true), Modifier.size(10.dp))
                                                    }
                                                }
                                                Box(contentAlignment = Alignment.CenterEnd, modifier = Modifier.height(16.dp).width(34.dp)) {
                                                    AsyncImage(model = raw + "IMG_20261028_004355.png", contentDescription = "Level", contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                                                    Text("Lv.1", color = Color.White, fontSize = 7.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Normal, modifier = Modifier.padding(end = 4.dp).offset(x = 1.dp, y = (-2).dp))
                                                }
                                            }
                                        }
                                        Spacer(Modifier.height(2.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            AsyncImage(model = raw + "file_00000000e56882119c217d508b6733dc.png", contentDescription = null, modifier = Modifier.size(12.dp))
                                            Spacer(Modifier.width(2.dp))
                                            Text(text = leaderboardCoinText(rank3.coins), color = Color(0xFFE7B865), fontSize = 11.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Card and Timer Block (4 to 50 rank list)
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .offset(y = (-16).dp)
                            .defaultMinSize(minHeight = 450.dp)
                            .background(Color(0xE60A0505), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                            .border(1.dp, Color(0xFFD4AF37), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    ) {
                        // Ornate Golden Corner Border Overlay
                        OrnateCardCornerDecoration(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .align(Alignment.TopCenter)
                        )

                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Countdown se pehle wala Ornate Design
                            Spacer(Modifier.height(14.dp))
                            CountdownTopOrnament(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(18.dp)
                                    .padding(horizontal = 24.dp)
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp, bottom = 12.dp)
                                    .background(
                                        Brush.horizontalGradient(
                                            0.0f to Color.Transparent,
                                            0.2f to Color(0xFF4A2511), 
                                            0.8f to Color(0xFF4A2511), 
                                            1.0f to Color.Transparent
                                        )
                                    )
                                    .padding(vertical = 4.dp), 
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "⏳$leaderboardCountdown (GMT 5:30+)",
                                    color = Color(0xFFD4AF37),
                                    fontSize = 12.sp,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                                )
                            }

                            val remainingUsers = leaderboardRows.drop(3).take(47)

                            // EMPTY STATE CHECK (Agar 4 se leke 50 tak koi na ho)
                            if (remainingUsers.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(250.dp), 
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/file_00000000777481f588df50d28908ce63.png",
                                        contentDescription = "No Users",
                                        modifier = Modifier.size(100.dp), 
                                        alpha = 0.5f, 
                                        colorFilter = androidx.compose.ui.graphics.ColorFilter.colorMatrix(
                                            androidx.compose.ui.graphics.ColorMatrix().apply { setToSaturation(0f) } 
                                        ),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            } else {
                                // DATA STATE (Normal List)
                                remainingUsers.forEachIndexed { index, user ->
                                    val i = index
                                    val resolvedAvatarUrl = user.image.takeIf { it.startsWith("http", ignoreCase = true) }
                                        ?: (raw + user.image.removePrefix("/").ifBlank { "default_avatar.png" })

                                    Row(
                                        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 14.dp).clickable(enabled = tab != "room") { onOpenProfile(RoomUser(id = user.id, uid = user.id, accountId = user.id, name = user.name, image = user.image, gender = user.gender, country = user.country)) },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${i + 4}",
                                            color = Color(0xFFE7B865),
                                            fontSize = 18.sp,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                            modifier = Modifier.width(32.dp),
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        AsyncImage(
                                            model = resolvedAvatarUrl,
                                            contentDescription = "Avatar",
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(50))
                                                .border(1.5.dp, Color(0x33FFFFFF), RoundedCornerShape(50)),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Column(Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = leaderboardShortName(user.name),
                                                    color = Color.White,
                                                    fontSize = 14.sp,
                                                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(Modifier.width(6.dp))
                                                if (tab == "room") {
                                                    Text(leaderboardFlag(user.country), fontSize = 14.sp)
                                                } else if (user.gender.isNotBlank()) {
                                                    Box(Modifier.size(20.dp).clip(RoundedCornerShape(50)).background(if (user.gender.equals("female", true)) Color(0xFFFF5874) else Color(0xFF3B82F6)), contentAlignment = Alignment.Center) {
                                                        CustomGenderIcon(user.gender.equals("female", true), Modifier.size(10.dp))
                                                    }
                                                }
                                            }
                                            if (tab != "room") {
                                                Spacer(Modifier.height(4.dp))
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(contentAlignment = Alignment.CenterEnd, modifier = Modifier.height(16.dp).width(34.dp)) {
                                                        AsyncImage(model = raw + "IMG_20261028_004355.png", contentDescription = "Level Image", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                                                        Text("Lv.1", color = Color.White, fontSize = 7.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Normal, modifier = Modifier.padding(end = 4.dp).offset(x = 1.dp, y = (-2).dp))
                                                    }
                                                }
                                            }
                                        }
                                        Spacer(Modifier.width(8.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = leaderboardCoinText(user.coins),
                                                color = Color(0xFFE7B865),
                                                fontSize = 14.sp,
                                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            AsyncImage(
                                                model = raw + "file_00000000e56882119c217d508b6733dc.png",
                                                contentDescription = "Coins Icon",
                                                modifier = Modifier.size(16.dp),
                                                contentScale = ContentScale.Fit
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(180.dp)) 
                        }
                    }
                }
            }
        }

        // BOTTOM BAR
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .offset(y = 24.dp) 
                .height(150.dp) 
        ) {
            AsyncImage(
                model = raw + "IMG_20261106_021443.png",
                contentDescription = "Bottom Bar Background",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .padding(top = 30.dp), 
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = ownRankText,
                    color = Color(0xFFE7B865),
                    fontSize = 16.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    modifier = Modifier.width(28.dp),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.width(6.dp))
                AsyncImage(
                    model = ownAvatar,
                    contentDescription = "Your avatar",
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(50)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = leaderboardShortName(ownName), 
                            color = Color.White, 
                            fontSize = 15.sp, 
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, 
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (tab != "room") {
                            Spacer(Modifier.width(6.dp))
                            Box(
                                Modifier.size(20.dp).clip(RoundedCornerShape(50)).background(if (ownGender.equals("female", true)) Color(0xFFFF5874) else Color(0xFF3B82F6)),
                                contentAlignment = Alignment.Center
                            ) {
                                CustomGenderIcon(ownGender.equals("female", true), Modifier.size(10.dp))
                            }
                        }
                    }
                    if (tab != "room") {
                        Spacer(Modifier.height(4.dp))
                        Box(contentAlignment = Alignment.CenterEnd, modifier = Modifier.height(16.dp).width(34.dp)) {
                            AsyncImage(model = raw + "IMG_20261028_004355.png", contentDescription = "Level Image", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                            Text("Lv.1", color = Color.White, fontSize = 7.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Normal, modifier = Modifier.padding(end = 4.dp).offset(x = 1.dp, y = (-2).dp))
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = raw + "file_00000000e56882119c217d508b6733dc.png",
                        contentDescription = "Coins Icon",
                        modifier = Modifier.size(16.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = leaderboardCoinText(ownCoins), 
                        color = Color(0xFFE7B865), 
                        fontSize = 15.sp, 
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                }
            }
        }

        if (showRules) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { showRules = false },
                properties = androidx.compose.ui.window.DialogProperties(
                    usePlatformDefaultWidth = false,
                    decorFitsSystemWindows = false
                )
            ) {
                val cardShape = RoundedCornerShape(14.dp)
                val buttonShape = RoundedCornerShape(12.dp)
                val goldenBorderBrush = Brush.verticalGradient(
                    listOf(Color(0xFFF3D578), Color(0xFFC59B38), Color(0xFFE5C158))
                )

                val ruleTitle = when (tab) {
                    "charm" -> "Charm"
                    "room" -> "Room"
                    else -> "Rich"
                }
                val ruleBody = when (tab) {
                    "charm" -> "Charm Raking is determined by the number of diamonds You receive Lucky gift Only count 10% of diamond value"
                    "room" -> "Room Raking is determined by the number of Coins You Received in the room Lucky gift Only count 10% of Coins value"
                    else -> "Rich Raking is determined by the number of Coins You Send Lucky gift Only count 10% of Coins value"
                }
                val cardBgColors = when (tab) {
                    "charm" -> listOf(Color(0xFF9E1B5E), Color(0xFF6E0D3E), Color(0xFF450625))
                    "room" -> listOf(Color(0xFF0B4182), Color(0xFF072957), Color(0xFF041A3B))
                    else -> listOf(Color(0xFF6B3A18), Color(0xFF4A250E), Color(0xFF2E1506))
                }
                val buttonBgColors = when (tab) {
                    "charm" -> listOf(Color(0xFFC42577), Color(0xFF8A1450))
                    "room" -> listOf(Color(0xFF1055A3), Color(0xFF0A3973))
                    else -> listOf(Color(0xFF8C4E22), Color(0xFF5E3111))
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.65f))
                        .clickable { showRules = false },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.86f)
                            .clip(cardShape)
                            .background(Brush.verticalGradient(colors = cardBgColors))
                            .border(1.5.dp, goldenBorderBrush, cardShape)
                            .clickable(enabled = false) {}
                            .padding(horizontal = 22.dp, vertical = 26.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = ruleTitle,
                                style = TextStyle(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(Color.White, Color(0xFFFCE28B), Color(0xFFE5B83B))
                                    ),
                                    fontSize = 21.sp,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                ),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(bottom = 22.dp)
                            )

                            Text(
                                text = ruleBody,
                                color = Color.White.copy(alpha = 0.95f),
                                fontSize = 14.sp,
                                lineHeight = 22.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(28.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clip(buttonShape)
                                    .background(Brush.verticalGradient(colors = buttonBgColors))
                                    .border(1.5.dp, Color(0xFFE5C158), buttonShape)
                                    .clickable { showRules = false },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Got it",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

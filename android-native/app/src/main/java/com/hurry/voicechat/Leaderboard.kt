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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
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
                    gender = item.optString("gender"),
                    level = item.optInt("level", 1)
                )
            }
            ZyoraLeaderboardData(rows, root.optLong("endsAt"))
        } finally {
            connection.disconnect()
        }
    }

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
fun Leaderboard(onBack:()->Unit={}, initialTab:String="honour") {
    var tab by remember { mutableStateOf(initialTab) }
    var period by remember { mutableStateOf("daily") }
    var showRules by remember { mutableStateOf(false) }
    var leaderboardRows by remember { mutableStateOf<List<ZyoraRank>>(emptyList()) }
    var leaderboardEndsAt by remember { mutableLongStateOf(0L) }
    var leaderboardNow by remember { mutableLongStateOf(System.currentTimeMillis()) }
    
    val tabs=listOf("honour" to "Honour","charm" to "Charm","room" to "Room")
    val periods=listOf("daily" to "Daily","weekly" to "Weekly","monthly" to "Monthly")
    val raw = "https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/"
    
    val topImage = when (tab) {
        "charm" -> "IMG_20261105_230141.png"
        "room" -> "IMG_20261105_230309.png"
        else -> "IMG_20261105_230210.png"
    }
    
    val context = LocalContext.current
    val authPrefs = remember { context.getSharedPreferences("hurry_auth", android.content.Context.MODE_PRIVATE) }
    val ownName = authPrefs.getString("name", authPrefs.getString("displayName", "You")).orEmpty().ifBlank { "You" }
    val ownAvatar = authPrefs.getString("avatar", authPrefs.getString("image", "/default-avatar.png")).orEmpty().let { if (it.startsWith("http", ignoreCase = true)) it else raw + it.removePrefix("/") }

    LaunchedEffect(tab, period) {
        while (true) {
            runCatching { loadZyoraLeaderboard(if (tab == "honour") "rich" else tab, period) }
                .onSuccess { leaderboardRows = it.rows; leaderboardEndsAt = it.endsAt }
                .onFailure { leaderboardRows = emptyList() }
            delay(15000)
        }
    }
    LaunchedEffect(Unit) {
        while (true) { leaderboardNow = System.currentTimeMillis(); delay(1000) }
    }
    val remainingSeconds = ((leaderboardEndsAt - leaderboardNow) / 1000L).coerceAtLeast(0L)
    val leaderboardCountdown = String.format(Locale.US, "%02d:%02d:%02d",
        (remainingSeconds % 86400) / 3600, (remainingSeconds % 3600) / 60, remainingSeconds % 60)
    
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    
    val podiumTopWidth = (screenWidthDp * 0.55f).coerceAtMost(240f).dp
    val podiumSideWidth = (screenWidthDp * 0.45f).coerceAtMost(180f).dp

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
                            Text(
                                text = label,
                                color = if (tab == id) Color(0xFFE7B865) else Color(0xFFAAAAAA),
                                fontSize = 16.sp,
                                fontWeight = if (tab == id) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.SemiBold
                            )
                            if (tab == id) {
                                Spacer(Modifier.height(4.dp))
                                Canvas(modifier = Modifier.size(width = 24.dp, height = 6.dp)) {
                                    drawArc(
                                        color = Color(0xFFE7B865),
                                        startAngle = 0f,
                                        sweepAngle = 180f,
                                        useCenter = false,
                                        style = Stroke(width = 5f, cap = StrokeCap.Round)
                                    )
                                }
                            } else {
                                Spacer(Modifier.height(10.dp))
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
            
            // SECOND ROW
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.Start) {
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
                        Box(
                            modifier = Modifier.width(podiumTopWidth).height(260.dp).offset(y = (-35).dp), 
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = leaderboardRows.getOrNull(0)?.image?.takeIf { it.isNotBlank() } ?: (raw + "default_avatar.png"),
                                contentDescription = "Rank 1 Avatar",
                                modifier = Modifier
                                    .offset(y = (-15).dp)
                                    .size(95.dp)
                                    .clip(RoundedCornerShape(50)),
                                contentScale = ContentScale.Crop
                            )
                            LeaderboardChromaImage(
                                url = raw + "file_00000000a0e48207ade38b36ab946ecb.png",
                                contentDescription = "Rank 1 Frame",
                                modifier = Modifier.requiredSize(300.dp)
                            )
                        }
                        
                        // TOP 1 INFO
                        val rank1 = leaderboardRows.getOrNull(0)
                        if (rank1 != null) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.offset(y = (-35).dp)
                            ) {
                                Text(
                                    text = rank1.name, 
                                    color = Color.White, 
                                    fontSize = 15.sp, 
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, 
                                    maxLines = 1, 
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AsyncImage(model = raw + "file_00000000e56882119c217d508b6733dc.png", contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(text = leaderboardCoinText(rank1.coins), color = Color(0xFFE7B865), fontSize = 13.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                }
                            }
                        }
                        
                        Spacer(Modifier.height(5.dp)) 
                        
                        // Rank 2 and 3 Section (Yahan Arrangement.Center kar diya, gap ekdam khatam!)
                        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp),horizontalArrangement=Arrangement.Center,verticalAlignment=Alignment.CenterVertically) {
                            // Rank 2 Box
                            Box(modifier = Modifier.width(podiumSideWidth).height(175.dp), contentAlignment = Alignment.Center) {
                                AsyncImage(
                                    model = leaderboardRows.getOrNull(1)?.image?.takeIf { it.isNotBlank() } ?: (raw + "default_avatar.png"),
                                    contentDescription = "Rank 2 Avatar",
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .offset(y = 20.dp)
                                        .size(65.dp)
                                        .clip(RoundedCornerShape(50)),
                                    contentScale = ContentScale.Crop
                                )
                                LeaderboardChromaImage(url=raw+"file_0000000069b88207b9fdd01576b71ee9.png",contentDescription="Rank 2 frame",modifier=Modifier.requiredSize(200.dp),contentScale=ContentScale.Fit)
                                
                                // TOP 2 INFO
                                val rank2 = leaderboardRows.getOrNull(1)
                                if (rank2 != null) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.align(Alignment.TopCenter).offset(y = 90.dp) 
                                    ) {
                                        Text(
                                            text = rank2.name,
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.widthIn(max = 90.dp)
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            AsyncImage(model = raw + "file_00000000e56882119c217d508b6733dc.png", contentDescription = null, modifier = Modifier.size(12.dp))
                                            Spacer(Modifier.width(2.dp))
                                            Text(text = leaderboardCoinText(rank2.coins), color = Color(0xFFE7B865), fontSize = 11.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                            
                            // Rank 3 Box
                            Box(modifier = Modifier.width(podiumSideWidth).height(175.dp), contentAlignment = Alignment.Center) {
                                AsyncImage(
                                    model = leaderboardRows.getOrNull(2)?.image?.takeIf { it.isNotBlank() } ?: (raw + "default_avatar.png"),
                                    contentDescription = "Rank 3 Avatar",
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .offset(y = 20.dp) 
                                        .size(65.dp) 
                                        .clip(RoundedCornerShape(50)),
                                    contentScale = ContentScale.Crop
                                )
                                LeaderboardChromaImage(url=raw+"file_00000000ecc882079f34a9356b53dc8e.png",contentDescription="Rank 3 frame",modifier=Modifier.requiredSize(190.dp),contentScale=ContentScale.Fit)
                                
                                // TOP 3 INFO
                                val rank3 = leaderboardRows.getOrNull(2)
                                if (rank3 != null) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.align(Alignment.TopCenter).offset(y = 90.dp) 
                                    ) {
                                        Text(
                                            text = rank3.name,
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.widthIn(max = 90.dp)
                                        )
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
                
                item {
                    Spacer(Modifier.height(10.dp))
                }

                // Card and Timer Block (4 to 50 rank list)
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .padding(top = 35.dp) 
                            .defaultMinSize(minHeight = 450.dp) 
                            .background(Color(0xE60A0505), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)) 
                            .border(1.dp, Color(0xFFD4AF37), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    ) {
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
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
                            Text("— ∞ ", color = Color(0xFFD4AF37), fontSize = 14.sp)
                            Text(
                                text = "⏳ $leaderboardCountdown (GMT+5.5)",
                                color = Color(0xFFD4AF37),
                                fontSize = 14.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                            )
                            Text(" ∞ —", color = Color(0xFFD4AF37), fontSize = 14.sp)
                        }

                        val remainingUsers = leaderboardRows.drop(3).take(47)
                        
                        // EMPTY STATE CHECK 
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
                                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 14.dp),
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
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                            Text(
                                                text = user.name,
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            if (user.gender.isNotBlank()) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(14.dp)
                                                        .background(if (user.gender.equals("female", true)) Color(0xFFFF69B4) else Color(0xFF4A90E2), RoundedCornerShape(50)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = if (user.gender.equals("female", true)) "♀" else "♂",
                                                        color = Color.White,
                                                        fontSize = 9.sp,
                                                        style = androidx.compose.ui.text.TextStyle(
                                                            platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)
                                                        ),
                                                        modifier = Modifier.offset(y = (-1).dp)
                                                    )
                                                }
                                            }
                                            Spacer(Modifier.weight(1f))
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
                                        Spacer(Modifier.height(4.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(leaderboardFlag(user.country), fontSize = 14.sp)
                                            Spacer(Modifier.width(6.dp))
                                            Box {
                                                AsyncImage(
                                                    model = raw + "IMG_20261028_004355.png",
                                                    contentDescription = "Level Image",
                                                    modifier = Modifier.height(18.dp),
                                                    contentScale = ContentScale.Fit
                                                )
                                                Text(
                                                    text = "Lv.${user.level}",
                                                    color = Color.White,
                                                    fontSize = 9.sp,
                                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(180.dp)) 
                    }
                }
            }
        }

        // BOTTOM BAR
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .offset(y = 10.dp) 
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
                AsyncImage(
                    model = ownAvatar,
                    contentDescription = "Your avatar",
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(50)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = ownName, 
                    color = Color.White, 
                    fontSize = 15.sp, 
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, 
                    modifier = Modifier.weight(1f), 
                    maxLines = 1
                )
                Text(
                    text = "+100", 
                    color = Color(0xFFE7B865), 
                    fontSize = 18.sp, 
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
            }
        }

        if (showRules) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { showRules = false },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(Modifier.fillMaxSize().background(Color(0x99000000)).clickable { showRules = false },contentAlignment=Alignment.BottomCenter) {
                    Column(
                        Modifier.fillMaxWidth()
                            .fillMaxHeight(0.60f)
                            .clip(RoundedCornerShape(topStart=16.dp,topEnd=16.dp))
                            .background(Color(0xFF1A0204))
                            .clickable { }
                            .padding(20.dp)
                    ) {
                        Text("Rules",color=Color(0xFFE7B865),fontSize=18.sp,fontWeight=androidx.compose.ui.text.font.FontWeight.Bold,modifier=Modifier.align(Alignment.CenterHorizontally))
                        Spacer(Modifier.height(16.dp))
                        
                        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                            Text("Contribution Leaderboard: Your ranking on this leaderboard is determined by the total number of Coins you have spent in the room. The higher the amount spent, the higher your position on the leaderboard.",color=Color.White, fontSize = 12.sp)
                            Text("Charm Leaderboard: Your ranking on this leaderboard is determined by the total number of Coins you Receive. The higher the amount spent, the higher your position on the leaderboard.",color=Color.White, fontSize = 12.sp)
                            Text("Room Leaderboard: Your ranking on this leaderboard is determined by the total number of Coins you have spent in the room. The higher the amount spent, the higher your position on the leaderboard.",color=Color.White, fontSize = 12.sp)
                            
                            Spacer(Modifier.height(4.dp))
                            Text("Data Periods",color=Color(0xFFE7B865),fontSize = 14.sp, fontWeight=androidx.compose.ui.text.font.FontWeight.SemiBold)
                            Text("1. Daily leaderboard data is finalized every day at (5:30+GMT).\n2. Weekly leaderboard data is finalized every Monday at (5:30+GMT).\n3. Monthly leaderboard data is finalized on the first day of every month at (5:30+GMT).",color=Color.White, fontSize = 12.sp)
                            
                            Spacer(Modifier.height(4.dp))
                            Text("Note: Users who violate platform rules are ineligible to participate in the rankings.",color=Color.White, fontSize = 12.sp)
                            Text("The platform reserves the right to make the final decision.",color=Color.White, fontSize = 12.sp)
                            
                            Spacer(Modifier.height(12.dp))
                            Text("Regard Zyora team",color=Color(0xFFE7B865), fontSize = 12.sp, modifier=Modifier.fillMaxWidth(), textAlign = TextAlign.End)
                        }
                    }
                }
            }
        }
    }
}

package com.hurry.voicechat

import android.graphics.BitmapFactory
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
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

private object LeaderboardChromaCache {
    val images = ConcurrentHashMap<String, ImageBitmap>()
}

@Composable
private fun LeaderboardChromaImage(url: String, contentDescription: String?, modifier: Modifier, contentScale: ContentScale = ContentScale.Fit) {
    var bitmap by remember(url) { mutableStateOf(LeaderboardChromaCache.images[url]) }
    LaunchedEffect(url) {
        if (bitmap == null) {
            bitmap = withContext(Dispatchers.IO) {
                runCatching {
                    LeaderboardChromaCache.images[url] ?: run {
                        val source = URL(url).openStream().use { BitmapFactory.decodeStream(it) }
                        if (source == null) null else {
                            val result = source.copy(android.graphics.Bitmap.Config.ARGB_8888, true)
                            for (y in 0 until result.height) for (x in 0 until result.width) {
                                val pixel = result.getPixel(x, y)
                                val red = android.graphics.Color.red(pixel)
                                val green = android.graphics.Color.green(pixel)
                                val blue = android.graphics.Color.blue(pixel)
                                if (green > 50 && green > red * 1.15 && green > blue * 1.15) {
                                    result.setPixel(x, y, android.graphics.Color.TRANSPARENT)
                                }
                            }
                            source.recycle()
                            result.asImageBitmap().also { LeaderboardChromaCache.images[url] = it }
                        }
                    }
                }.getOrNull()
            }
        }
    }
    bitmap?.let { Image(bitmap=it, contentDescription=contentDescription, modifier=modifier, contentScale=contentScale) }
}

@Composable
fun Leaderboard(onBack:()->Unit={}, initialTab:String="honour") {
    var tab by remember { mutableStateOf(initialTab) }
    var period by remember { mutableStateOf("daily") }
    var showRules by remember { mutableStateOf(false) }
    
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
    val ownAvatar = authPrefs.getString("avatar", authPrefs.getString("image", "/default-avatar.png")).orEmpty().let { if (it.startsWith("http")) it else raw + it.removePrefix("/") }
    
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    
    // Top 1,2,3 Frames enlarged considerably
    val podiumTopWidth = (screenWidthDp * 0.55f).coerceAtMost(240f).dp
    val podiumSideWidth = (screenWidthDp * 0.50f).coerceAtMost(220f).dp

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
            
            // TOP ROW: Back Button + Simple Text Tabs with Smile + Rules Button
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
                                // Golden Smile (Curve) Indicator
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
            
            // SECOND ROW: Left Aligned Pill for Daily / Weekly / Monthly
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
                item {
                    Column(Modifier.fillMaxWidth().padding(top=18.dp,bottom=24.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                        // Increased height and width of Top Frames
                        LeaderboardChromaImage(url=raw+"file_00000000a0e48207ade38b36ab946ecb.png",contentDescription="Rank 1 frame",modifier=Modifier.width(podiumTopWidth).height(500.dp),contentScale=ContentScale.Fit)
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                            LeaderboardChromaImage(url=raw+"file_0000000069b88207b9fdd01576b71ee9.png",contentDescription="Rank 2 frame",modifier=Modifier.width(podiumSideWidth).height(400.dp),contentScale=ContentScale.Fit)
                            LeaderboardChromaImage(url=raw+"file_00000000ecc882079f34a9356b53dc8e.png",contentDescription="Rank 3 frame",modifier=Modifier.width(podiumSideWidth).height(400.dp),contentScale=ContentScale.Fit)
                        }
                    }
                }
                
                // Ranks 4 to 50
                items(47) { i ->
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Rank Number
                        Text(
                            text = "${i + 4}",
                            color = Color(0xFFE7B865),
                            fontSize = 18.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            modifier = Modifier.width(32.dp),
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(Modifier.width(6.dp))
                        
                        // Protocol User Avatar
                        AsyncImage(
                            model = raw + "default_avatar.png",
                            contentDescription = "Avatar",
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(50))
                                .border(1.5.dp, Color(0x33FFFFFF), RoundedCornerShape(50)),
                            contentScale = ContentScale.Crop
                        )
                        
                        Spacer(Modifier.width(12.dp))
                        
                        // Middle Section - Name, Flag, Gender (Restored original style)
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "User Name ${i+4}",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.width(6.dp))
                                
                                // Flag
                                Text(if (i % 2 == 0) "🇮🇳" else "🇸🇦", fontSize = 14.sp) 
                                Spacer(Modifier.width(4.dp))
                                
                                // Gender Icon
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(if (i % 2 == 0) Color(0xFF4A90E2) else Color(0xFFFF69B4), RoundedCornerShape(50)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(if (i % 2 == 0) "♂" else "♀", color = Color.White, fontSize = 10.sp)
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            
                            // Restored Original Level Style
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF3B82F6), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                            ) {
                                Text("Lv.${10 + i}", color = Color.White, fontSize = 9.sp)
                            }
                        }
                        
                        // Right Side - Coins Value and Icon
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${796 - (i * 12)}.9K",
                                color = Color(0xFFE7B865),
                                fontSize = 16.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                            Spacer(Modifier.width(6.dp))
                            // Gold Coin Icon
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .background(Brush.radialGradient(listOf(Color(0xFFFFE066), Color(0xFFD4AF37))), RoundedCornerShape(50)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("S", color = Color(0xFF5C3A00), fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            
            // Bottom user row - Edge to Edge Background Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
            ) {
                // Full Width/Height Image Background (Edge to Edge)
                AsyncImage(
                    model = raw + "IMG_20261106_021443.png",
                    contentDescription = "Bottom Bar Background",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                
                // User Details directly overlapping on the image
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp), // Thoda inner space text aur avatar ke liye
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
}

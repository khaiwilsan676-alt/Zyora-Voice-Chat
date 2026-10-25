package com.hurry.voicechat

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import android.content.Context
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil3.compose.AsyncImage
import coil3.compose.rememberAsyncImagePainter
import kotlinx.coroutines.delay

private const val LEVEL_ASSET_BASE = "https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/"
private fun levelAsset(file: String) = LEVEL_ASSET_BASE + file

private data class NativeLevelTier(
    val range: String,
    val badge: String,
    val rewards: List<Pair<Int, String>>
)

private data class NativeLevelMedalItem(
    val levelText: String,
    val imageFile: String,
    val videoFile: String
)

private data class NativeLevelFrameItem(
    val levelText: String,
    val imageFile: String
)

// Naya Vehicle Data Class
private data class NativeLevelVehicleItem(
    val levelText: String,
    val imageFile: String,
    val videoFile: String
)

private val levelTiers = listOf(
    NativeLevelTier("Lv.1 - Lv.10","IMG_20260917_220530.png",listOf(2 to "16,000",4 to "25,000",6 to "35,000",8 to "46,000",10 to "58,000")),
    NativeLevelTier("Lv.11 - Lv.20","IMG_20260917_220613.png",listOf(12 to "70,000",14 to "84,000",16 to "98,000",18 to "114,000",20 to "130,000")),
    NativeLevelTier("Lv.21 - Lv.30","IMG_20260917_220641.png",listOf(22 to "146,000",24 to "164,000",26 to "280,000",28 to "430,000",30 to "676,000")),
    NativeLevelTier("Lv.31 - Lv.40","IMG_20260917_220710.png",listOf(32 to "1,000,000",34 to "1,600,000",36 to "11,200,000",38 to "11,400,000",40 to "16,000,000")),
    NativeLevelTier("Lv.41 - Lv.50","IMG_20260917_220733.png",listOf(42 to "22,000,000",44 to "29,000,000",46 to "37,000,000",48 to "47,000,000",50 to "58,000,000")),
    NativeLevelTier("Lv.51 - Lv.60","IMG_20260917_220753.png",listOf(52 to "70,000,000",54 to "84,000,000",56 to "100,000,000",58 to "120,000,000",60 to "140,000,000")),
    NativeLevelTier("Lv.61 - Lv.70","IMG_20260917_220815.png",listOf(62 to "160,000,000",64 to "180,000,000",66 to "200,000,000",68 to "230,000,000",70 to "260,000,000")),
    NativeLevelTier("Lv.71 - Lv.80","IMG_20260917_220839.png",listOf(72 to "290,000,000",74 to "320,000,000",76 to "360,000,000",78 to "400,000,000",80 to "440,000,000")),
    NativeLevelTier("Lv.81 - Lv.90","IMG_20260917_220900.png",listOf(82 to "480,000,000",84 to "520,000,000",86 to "570,000,000",88 to "620,000,000",90 to "680,000,000")),
    NativeLevelTier("Lv.91 - Lv.100","IMG_20260917_220922.png",listOf(92 to "740,000,000",94 to "800,000,000",96 to "860,000,000",98 to "930,000,000",100 to "1,000,000,000"))
)

private val medalsList = listOf(
    NativeLevelMedalItem("Lv.5", "IMG_20261024_153426.png", "1000220021-background.mp4"),
    NativeLevelMedalItem("Lv.15", "file_00000000bd988211840a0dcbe09101ee.png", "1000220022-background.mp4"),
    NativeLevelMedalItem("Lv.30", "IMG_20261024_153407.png", "1000220023-background.mp4"),
    NativeLevelMedalItem("Lv.50", "IMG_20261024_153322.png", "1000220024-background.mp4"),
    NativeLevelMedalItem("Lv.65", "IMG_20261024_153346.png", "1000220025-background.mp4"),
    NativeLevelMedalItem("Lv.75", "IMG_20261024_153248.png", "1000220026-background.mp4"),
    NativeLevelMedalItem("Lv.90", "IMG_20261024_153322.png", "1000220027-background.mp4")
)

private val framesList = listOf(
    NativeLevelFrameItem("Lv.5", "IMG_20261025_135250.png"),
    NativeLevelFrameItem("Lv.10", "IMG_20261025_135305.png"),
    NativeLevelFrameItem("Lv.25", "IMG_20261025_135331.png"),
    NativeLevelFrameItem("Lv.40", "IMG_20261025_135358.png"),
    NativeLevelFrameItem("Lv.55", "IMG_20261025_135714.png"),
    NativeLevelFrameItem("Lv.80", "IMG_20261025_135736.png"),
    NativeLevelFrameItem("Lv.100", "IMG_20261025_140657.png")
)

// Nayi Vehicles List
private val vehiclesList = listOf(
    NativeLevelVehicleItem("Lv.25", "IMG_20261025_124343.png", "alioss_16_2025080802181154988~2.mp4"), // 75 wali 25 m daal di
    NativeLevelVehicleItem("Lv.45", "IMG_20261025_124403.png", "alioss_17_2025110614082840769~2.mp4"), // 90 wali 45 m daal di
    NativeLevelVehicleItem("Lv.60", "IMG_20261025_124423.png", "alioss_17_2025111717234047534~2.mp4"), // 25 wali 60 m daal di
    NativeLevelVehicleItem("Lv.75", "IMG_20261025_124548.png", "alioss_17_2025110916541444937~2.mp4"), // 45 wali 75 m daal di
    NativeLevelVehicleItem("Lv.90", "IMG_20261025_124608.png", "alioss_17_2025103016391722282~2.mp4")  // 60 wali 90 m daal di
)

private val profileBgImages = listOf(
    "file_00000000edf082119f3c51da31cca16b.png",
    "file_00000000acd48211ba29883fe47a0215.png"
)

@Composable
private fun StarRow(starCount: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(0.dp)) {
        repeat(starCount) {
            AsyncImage(
                model = levelAsset("file_0000000004e881fa84e61b8616f8eaf8.png"),
                contentDescription = "Star",
                modifier = Modifier.size(14.dp) 
            )
        }
    }
}

@Composable
private fun SectionHeading(title: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f).height(1.5.dp).background(Brush.horizontalGradient(listOf(Color.Transparent, Color.White))))
        Spacer(Modifier.width(4.dp))
        Box(modifier = Modifier.size(3.dp).clip(CircleShape).background(Color(0xFF4DD0E1))) 
        Spacer(Modifier.width(3.dp))
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFFFFEB3B))) 
        
        Spacer(Modifier.width(12.dp))
        
        Text(
            text = title,
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        
        Spacer(Modifier.width(12.dp))
        
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFFFFEB3B))) 
        Spacer(Modifier.width(3.dp))
        Box(modifier = Modifier.size(3.dp).clip(CircleShape).background(Color(0xFF4DD0E1))) 
        Spacer(Modifier.width(4.dp))
        Box(modifier = Modifier.weight(1f).height(1.5.dp).background(Brush.horizontalGradient(listOf(Color.White, Color.Transparent))))
    }
}

// BSS YE WALA COMPONENT MODIFIED HAI BLACK REMOVE KARNE KE LIYE
@Composable
private fun FullScreenVideoDialog(videoUrl: String, isMedal: Boolean, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val dialogWindowProvider = androidx.compose.ui.platform.LocalView.current.parent as? androidx.compose.ui.window.DialogWindowProvider
        dialogWindowProvider?.window?.setDimAmount(0f)
        dialogWindowProvider?.window?.setBackgroundDrawableResource(android.R.color.transparent)

        Box(
            modifier = Modifier
                .fillMaxSize()
                // Yahan halka sa black dark background add kiya gaya hai sirf Medal ke liye
                .background(if (isMedal) Color.Black.copy(alpha = 0.6f) else Color.Transparent)
                .clickable { onDismiss() }, 
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { ctx ->
                    // VideoView ki jagah TextureView jisme transparent video perfectly blend hogi
                    android.view.TextureView(ctx).apply {
                        isOpaque = false // Transparent background ke liye jaruri
                        surfaceTextureListener = object : android.view.TextureView.SurfaceTextureListener {
                            var mediaPlayer: android.media.MediaPlayer? = null

                            override fun onSurfaceTextureAvailable(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {
                                mediaPlayer = android.media.MediaPlayer().apply {
                                    try {
                                        setDataSource(videoUrl)
                                        setSurface(android.view.Surface(surface))
                                        setOnPreparedListener { mp ->
                                            mp.isLooping = false
                                            start()
                                        }
                                        setOnCompletionListener {
                                            onDismiss() 
                                        }
                                        prepareAsync()
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }

                            override fun onSurfaceTextureSizeChanged(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {}
                            
                            override fun onSurfaceTextureDestroyed(surface: android.graphics.SurfaceTexture): Boolean {
                                mediaPlayer?.release()
                                mediaPlayer = null
                                return true
                            }
                            
                            override fun onSurfaceTextureUpdated(surface: android.graphics.SurfaceTexture) {}
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = 0.99f 
                        if (isMedal) {
                            // Jaduwi filter jo Video se purely Black color nikal ke transparent kar dega
                            val removeBlackMatrix = ColorMatrix(floatArrayOf(
                                1f, 0f, 0f, 0f, 0f,  // R
                                0f, 1f, 0f, 0f, 0f,  // G
                                0f, 0f, 1f, 0f, 0f,  // B
                                1.2f, 1.2f, 1.2f, 0f, 0f // Alpha channel ko RGB brightness se map karna
                            ))
                            colorFilter = ColorFilter.colorMatrix(removeBlackMatrix)
                        }
                    }
                    .drawWithContent {
                        drawContent()
                        if (!isMedal) {
                            // ENTRY VIDEO KE LIYE PERFECT 10% MIXING/FADE EFFECT
                            // Top s 10% blend
                            drawRect(
                                brush = Brush.verticalGradient(
                                    0.0f to Color(0xFF04060A), // Same as app background color
                                    0.1f to Color.Transparent
                                )
                            )
                            // Bottom s 10% blend
                            drawRect(
                                brush = Brush.verticalGradient(
                                    0.9f to Color.Transparent,
                                    1.0f to Color(0xFF04060A) // Same as app background color
                                )
                            )
                        }
                    }
            )
        }
    }
}

@Composable
private fun LevelUpgradeDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).widthIn(max = 340.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB)),
            shadowElevation = 20.dp
        ) {
            Column(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("◆", color = Color(0xFFF59E0B), fontSize = 18.sp)
                    Text("  How to upgrade  ", color = Color(0xFF111827), fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp)
                    Text("◆", color = Color(0xFFF59E0B), fontSize = 18.sp)
                }
                Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
                    val methods = listOf(
                        Triple("Send Gifts", "1 coins=1 EXP", 0),
                        Triple("Join rooms", "2000 EXP/Day", 1),
                        Triple("Share Rooms", "2000 EXP/Day", 2),
                        Triple("Take the mic and unmute in your ov", "1 min = 500 exp, 10000 exp/day", 3),
                        Triple("Take the mic and unmute in others'", "1 min = 500 exp, 20000 exp/day", 4)
                    )
                    methods.forEachIndexed { index, item ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                                when (item.third) {
                                    0 -> Icon(Icons.Default.CardGiftcard, null, tint = Color(0xFF3B82F6), modifier = Modifier.size(40.dp))
                                    1 -> Icon(Icons.Default.Home, null, tint = Color(0xFF3B82F6), modifier = Modifier.size(40.dp))
                                    2 -> Icon(Icons.Default.Share, null, tint = Color(0xFF3B82F6), modifier = Modifier.size(40.dp))
                                    else -> Icon(Icons.Default.Mic, null, tint = Color(0xFF3B82F6), modifier = Modifier.size(40.dp))
                                }
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text(item.first, color = Color(0xFF111827), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, lineHeight = 17.sp, maxLines = 1, overflow = TextOverflow.Clip)
                                Text(item.second, color = Color(0xFF6B7280), fontSize = 13.sp, lineHeight = 16.sp)
                            }
                        }
                        if (index != methods.lastIndex) HorizontalDivider(color = Color(0xFFF3F4F6))
                    }
                }
                Box(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 16.dp)) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                    ) {
                        Text("Confirm", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp)
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun Level(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp

    val userPrefs = remember { context.getSharedPreferences("hurry_user", Context.MODE_PRIVATE) }
    val storedName = userPrefs.getString("name", "")?.trim().orEmpty()
    val userName = storedName.takeIf { it.isNotBlank() && it.lowercase() !in listOf("guest", "user", "null", "undefined") }
    val userUid = userPrefs.getString("uid", "")?.trim().orEmpty().ifBlank {
        userPrefs.getString("phone", "")?.trim().orEmpty()
    }
    val userPhoto = userPrefs.getString("photo", "")?.trim().orEmpty()
    
    val profilePainter = rememberAsyncImagePainter(model = levelAsset("file_0000000074a4821189d05fde51ca98f9.png"))
    
    val profileIntrinsic = profilePainter.intrinsicSize
    val profileAspect = if (profileIntrinsic.width.isFinite() && profileIntrinsic.height.isFinite() && profileIntrinsic.width > 0f && profileIntrinsic.height > 0f) profileIntrinsic.width / profileIntrinsic.height else 3.25f
    val profileFrameWidth = configuration.screenWidthDp.dp
    val profileFrameHeight = profileFrameWidth / profileAspect
    val displayName = userName ?: userUid.take(8).ifBlank { "Guest" }
    
    var showUpgrade by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    var showAllLevels by remember { mutableStateOf(false) }
    var showAllRewards by remember { mutableStateOf(false) }
    
    // Naye View More states
    var showAllFrames by remember { mutableStateOf(false) }
    var showAllMedals by remember { mutableStateOf(false) }
    var showAllVehicles by remember { mutableStateOf(false) }
    
    var selectedVideoData by remember { mutableStateOf<Pair<String, Boolean>?>(null) }

    val pagerState = rememberPagerState { profileBgImages.size }

    val topBannerSpace = (screenHeight * 0.07).dp
    val vh3 = (screenHeight * 0.03).dp
    val newSectionTopPadding = (48.dp - vh3).coerceAtLeast(8.dp)

    LaunchedEffect(pagerState) {
        while (true) {
            delay(5000) 
            if (profileBgImages.isNotEmpty()) {
                val nextPage = (pagerState.currentPage + 1) % profileBgImages.size
                pagerState.animateScrollToPage(nextPage)
            }
        }
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF04060A))) {
        AsyncImage(model = levelAsset("file_00000000e02481f4bb2153e2714aca47.png"), contentDescription = null, Modifier.fillMaxWidth().height(280.dp).align(Alignment.TopCenter), contentScale = ContentScale.Crop)
        Box(
            Modifier.align(Alignment.TopStart).offset(x = (-40).dp, y = (-40).dp).size(260.dp)
                .blur(90.dp).clip(CircleShape).background(Color(0x4D1D4ED8))
        )
        Box(Modifier.fillMaxWidth().height(280.dp).background(Brush.verticalGradient(listOf(Color.Transparent, Color(0x6604060A), Color(0xFF04060A)))))

        Column(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxWidth().statusBarsPadding()) {
                Box(Modifier.fillMaxWidth().height(56.dp).padding(top = 8.dp), contentAlignment = Alignment.Center) {
                    IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart).padding(start = 2.dp)) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White, modifier = Modifier.size(26.dp))
                    }
                    Text("Level", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    IconButton(onClick = { showUpgrade = true }, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 2.dp)) {
                        Icon(Icons.Default.HelpOutline, "How to upgrade", tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                }

                Spacer(Modifier.height(topBannerSpace))

                Box(Modifier.fillMaxWidth().height((profileFrameHeight - 48.dp).coerceAtLeast(1.dp))) {
                    Box(Modifier.width(profileFrameWidth).height(profileFrameHeight).offset(x = 0.dp, y = (-40).dp)) {
                        Image(
                            painter = profilePainter,
                            contentDescription = "Top Card Frame",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.FillBounds
                        )
                        Row(Modifier.fillMaxSize().padding(horizontal = 32.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(48.dp).clip(CircleShape).background(Color(0xFF626A75)), contentAlignment = Alignment.Center) {
                                if (userPhoto.isNotBlank()) {
                                    AsyncImage(
                                        model = userPhoto,
                                        contentDescription = "User",
                                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Text(displayName.take(1).uppercase(), color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(displayName, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Spacer(Modifier.width(8.dp))
                                    AsyncImage(model = levelAsset(levelTiers[0].badge), contentDescription = null, modifier = Modifier.height(28.dp).width(58.dp))
                                }
                                Spacer(Modifier.height(3.dp)) 
                                Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha=.25f))) {
                                    Box(Modifier.fillMaxWidth(.38f).fillMaxHeight().clip(RoundedCornerShape(20.dp)).background(Brush.horizontalGradient(listOf(Color(0xFFFFE072), Color(0xFFF4B63F))))) {
                                        Box(Modifier.align(Alignment.CenterEnd).offset(x = 6.dp).size(12.dp).border(2.dp, Color(0xFFF4B63F), CircleShape).clip(CircleShape).background(Color.White))
                                    }
                                }
                                Text("4.5k/20.2k remaining to reach Level 5 >", color = Color.White.copy(alpha=.9f), fontSize = 10.5.sp, modifier = Modifier.padding(top = 2.dp), maxLines = 1)
                            }
                        }
                    }
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = newSectionTopPadding, bottom = 40.dp)
            ) {
                // ==========================================
                // 1. LEVEL SECTION
                // ==========================================
                item {
                    SectionHeading("Level") 
                    Spacer(Modifier.height(8.dp))
                }

                val visibleLevels = if (showAllLevels) levelTiers else levelTiers.take(9)
                val chunkedLevels = visibleLevels.chunked(3)
                
                items(chunkedLevels) { rowTiers ->
                    Row(
                        modifier = Modifier.fillMaxWidth(), 
                        horizontalArrangement = Arrangement.spacedBy(10.dp) 
                    ) {
                        for (tier in rowTiers) {
                            Box(
                                Modifier
                                    .weight(1f)
                                    .aspectRatio(0.85f)
                                    .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                AsyncImage(
                                    model = levelAsset(tier.badge),
                                    contentDescription = null,
                                    modifier = Modifier.height(38.dp).align(Alignment.Center) 
                                )
                                
                                val formattedRange = tier.range.replace(" - Lv.", " - ")
                                Text(
                                    text = formattedRange, 
                                    color = Color.White, 
                                    fontSize = 11.sp, 
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.align(Alignment.BottomCenter) 
                                )
                            }
                        }
                        repeat(3 - rowTiers.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }

                if (levelTiers.size > 9) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 12.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (showAllLevels) "View Less <" else "View More >",
                                color = Color.White,
                                fontSize = 11.sp, 
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { showAllLevels = !showAllLevels } 
                                    .border(1.dp, Color(0xFF3B82F6), RoundedCornerShape(8.dp)) 
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(24.dp))
                }

                // ==============================================
                // 2. REWARDS SECTION
                // ==============================================
                item {
                    SectionHeading("Rewards")
                    Spacer(Modifier.height(8.dp))
                }

                val allRewards = levelTiers.flatMap { it.rewards }
                val visibleRewards = if (showAllRewards) allRewards else allRewards.take(9)
                val chunkedRewards = visibleRewards.chunked(3)

                items(chunkedRewards) { rowItems ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        for (reward in rowItems) {
                            Box(
                                Modifier
                                    .weight(1f)
                                    .aspectRatio(0.85f)
                                    .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                    .clip(RoundedCornerShape(8.dp))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .background(
                                            color = Color.Black.copy(alpha = 0.4f),
                                            shape = RoundedCornerShape(bottomEnd = 8.dp) 
                                        )
                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "Lv.${reward.first}", 
                                        color = Color.White, 
                                        fontSize = 11.sp, 
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                
                                AsyncImage(
                                    model = levelAsset("file_00000000b2d481fd8cd233482dbeb9ef.png"),
                                    contentDescription = null,
                                    modifier = Modifier.size(56.dp).align(Alignment.Center) 
                                )
                                
                                Text(
                                    text = reward.second, 
                                    color = Color.White, 
                                    fontSize = 12.sp, 
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp)
                                )
                            }
                        }
                        repeat(3 - rowItems.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }

                if (allRewards.size > 9) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 12.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (showAllRewards) "View Less <" else "View More >",
                                color = Color.White,
                                fontSize = 11.sp, 
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { showAllRewards = !showAllRewards } 
                                    .border(1.dp, Color(0xFF3B82F6), RoundedCornerShape(8.dp)) 
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(24.dp))
                }

                // ==============================================
                // NEW FRAME SECTION
                // ==============================================
                item {
                    SectionHeading("Frame")
                    Spacer(Modifier.height(8.dp))
                }

                val visibleFrames = if (showAllFrames) framesList else framesList.take(4)
                val chunkedFrames = visibleFrames.chunked(2)
                
                items(chunkedFrames) { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        for (frame in rowItems) {
                            Box(
                                Modifier
                                    .weight(1f)
                                    .aspectRatio(0.85f)
                                    .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                    .clip(RoundedCornerShape(8.dp)) 
                            ) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .background(
                                            color = Color.Black.copy(alpha = 0.4f),
                                            shape = RoundedCornerShape(bottomEnd = 8.dp) 
                                        )
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = frame.levelText,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                AsyncImage(
                                    model = levelAsset(frame.imageFile),
                                    contentDescription = null,
                                    modifier = Modifier.size(110.dp).align(Alignment.Center) 
                                )
                                
                                val starCount = (frame.levelText.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0) % 3 + 3
                                Column(
                                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    StarRow(starCount = starCount)
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = "${frame.levelText} Frame",
                                        color = Color.LightGray,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        repeat(2 - rowItems.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }

                if (framesList.size > 4) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 12.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (showAllFrames) "View Less <" else "View More >",
                                color = Color.White,
                                fontSize = 11.sp, 
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { showAllFrames = !showAllFrames } 
                                    .border(1.dp, Color(0xFF3B82F6), RoundedCornerShape(8.dp)) 
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(24.dp))
                }

                // ==============================================
                // 3. MEDAL SECTION 
                // ==============================================
                item {
                    SectionHeading("Medal")
                    Spacer(Modifier.height(8.dp))
                }

                val visibleMedals = if (showAllMedals) medalsList else medalsList.take(4)
                val chunkedMedals = visibleMedals.chunked(2) 
                
                items(chunkedMedals) { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(), 
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        for (medal in rowItems) {
                            Box(
                                Modifier
                                    .weight(1f)
                                    .aspectRatio(0.85f)
                                    .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { 
                                        selectedVideoData = levelAsset(medal.videoFile) to true 
                                    }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .background(
                                            color = Color.Black.copy(alpha = 0.4f),
                                            shape = RoundedCornerShape(bottomEnd = 8.dp) 
                                        )
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = medal.levelText,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                AsyncImage(
                                    model = levelAsset(medal.imageFile),
                                    contentDescription = null,
                                    modifier = Modifier.size(90.dp).align(Alignment.Center) 
                                )
                                
                                val starCount = (medal.levelText.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0) % 3 + 3
                                Column(
                                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    StarRow(starCount = starCount)
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = "${medal.levelText} Medal", 
                                        color = Color.LightGray, 
                                        fontSize = 12.sp, 
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        repeat(2 - rowItems.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }

                if (medalsList.size > 4) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 12.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (showAllMedals) "View Less <" else "View More >",
                                color = Color.White,
                                fontSize = 11.sp, 
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { showAllMedals = !showAllMedals } 
                                    .border(1.dp, Color(0xFF3B82F6), RoundedCornerShape(8.dp)) 
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(24.dp))
                }

                // ==============================================
                // VEHICLE/ENTRY SECTION
                // ==============================================
                item {
                    SectionHeading("Entry")
                    Spacer(Modifier.height(8.dp))
                }

                val visibleVehicles = if (showAllVehicles) vehiclesList else vehiclesList.take(4)
                val chunkedVehicles = visibleVehicles.chunked(2)
                
                items(chunkedVehicles) { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        for (vehicle in rowItems) {
                            Box(
                                Modifier
                                    .weight(1f)
                                    .aspectRatio(0.85f)
                                    .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                    .clip(RoundedCornerShape(8.dp)) 
                                    .clickable { 
                                        selectedVideoData = levelAsset(vehicle.videoFile) to false 
                                    }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .background(
                                            color = Color.Black.copy(alpha = 0.4f), 
                                            shape = RoundedCornerShape(bottomEnd = 8.dp) 
                                        )
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = vehicle.levelText,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                AsyncImage(
                                    model = levelAsset(vehicle.imageFile),
                                    contentDescription = null,
                                    modifier = Modifier.size(110.dp).align(Alignment.Center) 
                                )

                                val starCount = (vehicle.levelText.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0) % 3 + 3
                                Column(
                                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    StarRow(starCount = starCount)
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = "${vehicle.levelText} Entry",
                                        color = Color.LightGray, 
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        repeat(2 - rowItems.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }

                if (vehiclesList.size > 4) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 12.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (showAllVehicles) "View Less <" else "View More >",
                                color = Color.White,
                                fontSize = 11.sp, 
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { showAllVehicles = !showAllVehicles } 
                                    .border(1.dp, Color(0xFF3B82F6), RoundedCornerShape(8.dp)) 
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(24.dp))
                }

                // ==============================================
                // 5. CUSTOMIZE PROFILE BACKGROUND SECTION 
                // ==============================================
                item {
                    SectionHeading("Customize Profile Background") 
                    Spacer(Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            AsyncImage(
                                model = levelAsset(profileBgImages[page]),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop 
                            )
                        }

                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .background(
                                    color = Color.Black.copy(alpha = 0.4f), 
                                    shape = RoundedCornerShape(bottomEnd = 8.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Lv.25",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            repeat(profileBgImages.size) { index ->
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (pagerState.currentPage == index) Color.White else Color.Black
                                        )
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }

    if (showUpgrade) LevelUpgradeDialog { showUpgrade = false }

    selectedVideoData?.let { (videoUrl, isMedal) ->
        FullScreenVideoDialog(videoUrl = videoUrl, isMedal = isMedal) {
            selectedVideoData = null
        }
    }
}

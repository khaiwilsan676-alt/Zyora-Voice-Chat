package com.hurry.voicechat

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage

private const val RAW_URL = "https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/"
private val String.fullUrl: String get() = if (this.startsWith("http")) this else RAW_URL + this.removePrefix("/")

data class MedalItem(
    val id: String,
    val name: String,
    val video: String, // Thumbnail image for Grid
    val previewVideo: String, // MP4 Video for Dialog Preview
    val stars: Int,
    val category: String, // 'achievement' | 'gift' | 'activity' | 'dummy'
    val variant: String = "black", // 'black' | 'green'
    val cardVideoSize: Int,
    val cardVideoTop: Float, // percentage as float 0.0 to 1.0
    val sheetVideoSize: Int,
    val sheetVideoTop: Float, // percentage as float
    val tierGroup: String? = null,
    val giftText: String
)

val TIER_TAB_IMAGES = listOf(
    "/IMG_20260924_132022.png",
    "/IMG_20260924_132038.png",
    "/IMG_20260924_132051.png"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedalScreen(onBack: () -> Unit) {
    var activeTab by remember { mutableStateOf("achievement") }
    var selectedMedal by remember { mutableStateOf<MedalItem?>(null) }
    var activeTier by remember { mutableIntStateOf(0) }

    var showObtained by remember { mutableStateOf(false) }
    var showUnequipped by remember { mutableStateOf(false) }
    var showRules by remember { mutableStateOf(false) }

    val medals = remember { getMedalsData() }
    val filteredMedals = medals.filter { it.category == activeTab }

    fun getTierMedals(medal: MedalItem?): List<MedalItem> {
        if (medal == null) return emptyList()
        if (medal.tierGroup == null) return listOf(medal)
        return medals.filter { it.tierGroup == medal.tierGroup }
    }

    val tierMedals = getTierMedals(selectedMedal)
    val displayMedal = tierMedals.getOrNull(activeTier) ?: selectedMedal

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF051329)) // Deep Blue fallback background
    ) {
        // 1. Fixed Top Background Image
        AsyncImage(
            model = "file_000000008f6c82109aa9c53c85841c5b.png".fullUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.45f)
                .align(Alignment.TopCenter)
        )

        // 2. Main Fixed Layout (No page scroll)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            // Fixed Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                }
                Text(
                    text = "My Badges",
                    color = Color.White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { showRules = true }) {
                    Icon(Icons.Default.HelpOutline, "Rules", tint = Color.White.copy(alpha = 0.9f))
                }
            }

            // Fixed 10 Slots Grid (Row 1: 5, Row 2: 5)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    repeat(5) {
                        MedalSlot { showUnequipped = true }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    repeat(5) {
                        MedalSlot { showUnequipped = true }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Fixed Tabs (Blue Theme + Golden Border)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "achievement" to "Achievements",
                    "activity" to "Activities",
                    "gift" to "Gift"
                ).forEach { (key, label) ->
                    val isSelected = activeTab == key
                    val tabShape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(tabShape)
                            .background(
                                if (isSelected) {
                                    Brush.verticalGradient(
                                        listOf(Color(0xFF0E5BB5), Color(0xFF072F6B))
                                    )
                                } else {
                                    Brush.verticalGradient(
                                        listOf(Color(0xFF08224A).copy(alpha = 0.88f), Color(0xFF04132C).copy(alpha = 0.92f))
                                    )
                                }
                            )
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) Color(0xFFE5C158) else Color(0xFF28558F).copy(alpha = 0.65f),
                                shape = tabShape
                            )
                            .clickable { activeTab = key },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.White else Color(0xFFB5CBE8),
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Fixed Bottom Curved Container with Golden Top Border Only
            val bottomShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF9C7A2F),
                                Color(0xFFF3D578),
                                Color(0xFFE5C158),
                                Color(0xFFF3D578),
                                Color(0xFF9C7A2F)
                            )
                        ),
                        shape = bottomShape
                    )
                    .padding(top = 2.dp) // Sirf top curved golden border
                    .clip(bottomShape)
                    .clipToBounds()
            ) {
                // Fixed Bottom Background Image
                AsyncImage(
                    model = "file_00000000a8e08210a8a24219461e3ce6.png".fullUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // ONLY Medals Scroll Here (Strictly Clipped Inside Bottom Border)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 16.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .clipToBounds()
                ) {
                    items(filteredMedals.size) { index ->
                        val medal = filteredMedals[index]
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val group = getTierMedals(medal)
                                    val idx = group.indexOfFirst { it.id == medal.id }
                                    activeTier = if (idx >= 0) idx else 0
                                    selectedMedal = medal
                                },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Colorless Image display in Grid
                            AsyncImage(
                                model = medal.video.fullUrl,
                                contentDescription = medal.name,
                                contentScale = ContentScale.Fit,
                                colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }),
                                modifier = Modifier.size(92.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = medal.name,
                                color = Color(0xFFE5EEFA),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Full Screen Overlay (Medal Detail) with Video Player
        if (selectedMedal != null && displayMedal != null) {
            Dialog(
                onDismissRequest = { selectedMedal = null },
                properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    // Top Header Area
                    Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.3f)) {
                        AsyncImage(
                            model = "IMG_20260924_132006.png".fullUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .windowInsetsPadding(WindowInsets.statusBars)
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(onClick = { selectedMedal = null }) {
                                Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                            }
                            IconButton(onClick = { showRules = true }) {
                                Icon(Icons.Default.HelpOutline, null, tint = Color.White.copy(alpha = 0.8f))
                            }
                        }

                        // VIDEO PLAYER FOR PREVIEW
                        MedalVideoPlayer(
                            videoUrl = displayMedal.previewVideo.fullUrl,
                            modifier = Modifier
                                .size(displayMedal.sheetVideoSize.dp)
                                .align(Alignment.Center)
                                .offset(y = 20.dp)
                        )
                    }

                    // Bottom Content Area
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .fillMaxHeight(0.7f)
                            .padding(horizontal = 24.dp, vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        AsyncImage(
                            model = "IMG_20260924_132112.png".fullUrl,
                            contentDescription = "frame",
                            modifier = Modifier.size(256.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Stars
                        Row(horizontalArrangement = Arrangement.Center) {
                            repeat(displayMedal.stars) {
                                Text("★", color = Color(0xFFEAB308), fontSize = 16.sp)
                            }
                        }

                        Text(
                            text = displayMedal.name,
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Text(
                            text = displayMedal.giftText,
                            color = Color(0xFF6B7280),
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        if (tierMedals.size > 1) {
                            Spacer(modifier = Modifier.height(24.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                tierMedals.indices.forEach { i ->
                                    if (i > 0) Text(" > ", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(horizontal = 8.dp))
                                    // 3 Tabs loop
                                    AsyncImage(
                                        model = TIER_TAB_IMAGES.getOrNull(i)?.fullUrl ?: TIER_TAB_IMAGES.last().fullUrl,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(
                                                width = if (activeTier == i) 2.dp else 0.dp,
                                                color = if (activeTier == i) Color(0xFFFACC15) else Color.Transparent,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable { activeTier = i }
                                            .graphicsLayer { alpha = if (activeTier == i) 1f else 0.7f }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(32.dp))
                            Box(
                                modifier = Modifier
                                    .width(320.dp)
                                    .height(48.dp)
                                    .clip(CircleShape)
                                    .background(Brush.verticalGradient(listOf(Color(0xFFFDE08B), Color(0xFFE2B344))))
                                    .clickable { /* action */ },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Not Obtained", color = Color(0xFF4A2B0C), fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // BOTTOM SHEETS & DIALOGS
    if (showObtained) {
        val list = listOf(
            "CP Level Badge" to "IMG_20260924_132022.png", "Millionaire I" to "IMG_20260924_132038.png", "Charm Star I" to "IMG_20260924_132051.png",
            "Fruit King I" to "IMG_20260924_132022.png", "Millionaire II" to "IMG_20260924_132038.png", "Fruit King II" to "IMG_20260924_132051.png"
        )
        MedalGridBottomSheet("Obtained Medals", list, onDismiss = { showObtained = false })
    }

    if (showUnequipped) {
        val list = listOf(
            "CP Spokesperson" to "IMG_20260924_132022.png", "Level Medal" to "IMG_20260924_132038.png", "Weird Star" to "IMG_20260924_132051.png",
            "SVIP Badge" to "IMG_20260924_132022.png", "Money Gun Talent" to "IMG_20260924_132038.png", "First Recharge" to "IMG_20260924_132051.png"
        )
        MedalGridBottomSheet("Unequipped Medals", list, onDismiss = { showUnequipped = false })
    }

    if (showRules) {
        RulesBottomSheet(onDismiss = { showRules = false })
    }
}

// VIDEO PLAYER COMPONENT
@Composable
fun MedalVideoPlayer(videoUrl: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUrl))
            repeatMode = Player.REPEAT_MODE_ONE // Loop
            playWhenReady = true
            prepare()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = {
            PlayerView(context).apply {
                player = exoPlayer
                useController = false
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
            }
        },
        modifier = modifier
    )
}

// HELPER UI COMPONENTS - Slot Card with Image ONLY (No + Icon)
@Composable
fun MedalSlot(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = "IMG_20261120_123855.png".fullUrl,
            contentDescription = "Badge Slot",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedalGridBottomSheet(title: String, items: List<Pair<String, String>>, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF2A1B5E),
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.6f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                textAlign = TextAlign.Center
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items.size) { index ->
                    val (name, img) = items[index]
                    Column(
                        modifier = Modifier.aspectRatio(1f).clip(RoundedCornerShape(12.dp)).background(Color(0xFF1E1245)).padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        AsyncImage(model = img.fullUrl, contentDescription = null, modifier = Modifier.weight(1f).fillMaxWidth(), contentScale = ContentScale.Fit)
                        Text(text = name, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
    }
}

// RULES / INTRODUCTION CARD (Same as screenshot, Blue Theme + Golden Borders)
@Composable
fun RulesBottomSheet(onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val cardShape = RoundedCornerShape(14.dp)
        val buttonShape = RoundedCornerShape(12.dp)
        val goldenBorderBrush = Brush.verticalGradient(
            listOf(Color(0xFFF3D578), Color(0xFFC59B38), Color(0xFFE5C158))
        )

        Box(
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .clip(cardShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0B4182), // Rich Royal Blue top
                            Color(0xFF072957), // Deep Blue middle
                            Color(0xFF041A3B)  // Dark Navy Blue bottom
                        )
                    )
                )
                .border(1.5.dp, goldenBorderBrush, cardShape)
                .padding(horizontal = 22.dp, vertical = 26.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title: Introduction (White-to-Gold gradient like screenshot)
                Text(
                    text = "Introduction",
                    style = TextStyle(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color.White, Color(0xFFFCE28B), Color(0xFFE5B83B))
                        ),
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 22.dp)
                )

                // Numbered Rules List
                RuleItemRow(
                    number = "1.",
                    text = "Badges are divided into Event Badges and Honor Badges. You can tap the badges on the Medal Wall to know how to get them."
                )
                Spacer(modifier = Modifier.height(14.dp))
                RuleItemRow(
                    number = "2.",
                    text = "You can wear up to 10 badges at the same time. They will be displayed in your Info Card and Room Chat Area."
                )
                Spacer(modifier = Modifier.height(14.dp))
                RuleItemRow(
                    number = "3.",
                    text = "Some badges are time-limited and expired ones cannot be worn."
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Got it Button (Blue fill + Golden border)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(buttonShape)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF1055A3), Color(0xFF0A3973))
                            )
                        )
                        .border(1.5.dp, Color(0xFFE5C158), buttonShape)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Got it",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun RuleItemRow(number: String, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = number,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 22.sp,
            modifier = Modifier.width(24.dp)
        )
        Text(
            text = text,
            color = Color.White.copy(alpha = 0.95f),
            fontSize = 14.sp,
            lineHeight = 22.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

// REAL DATA MAPPED WITH NEW PREVIEW VIDEO MP4s
fun getMedalsData(): List<MedalItem> = listOf(
    // Rich
    MedalItem("rich_1", "Rich I", "/IMG_20261023_110513.png", "/VID_20260924_124439.mp4", 1, "achievement", "black", 120, 0.5f, 200, 0.5f, "rich", "Send gifts to earn"),
    MedalItem("rich_2", "Rich II", "/IMG_20261023_110533.png", "/1000200509-background.mp4", 2, "achievement", "black", 120, 0.5f, 200, 0.5f, "rich", "Send gifts to earn"),
    MedalItem("rich_3", "Rich III", "/IMG_20261023_110559.png", "/1000200510-background.mp4", 3, "achievement", "black", 120, 0.5f, 200, 0.5f, "rich", "Send gifts to earn"),

    // Room
    MedalItem("room_1", "Room Top I", "/IMG_20261023_110628.png", "/1000200514-background.mp4", 1, "achievement", "black", 120, 0.5f, 200, 0.5f, "room", "Room achievements"),
    MedalItem("room_2", "Room Top II", "/IMG_20261023_110644.png", "/1000200515-background.mp4", 2, "achievement", "black", 120, 0.5f, 200, 0.5f, "room", "Room achievements"),
    MedalItem("room_3", "Room Top III", "/IMG_20261023_110705.png", "/1000200516-background.mp4", 3, "achievement", "black", 120, 0.5f, 200, 0.5f, "room", "Room achievements"),

    // Millionaire
    MedalItem("mill_1", "Millionaire I", "/IMG_20261023_110734.png", "/1000200518-background.mp4", 1, "achievement", "black", 120, 0.5f, 200, 0.5f, "millionaire", "Recharge to earn"),
    MedalItem("mill_2", "Millionaire II", "/IMG_20261023_110756.png", "/1000200521-background.mp4", 2, "achievement", "black", 120, 0.5f, 200, 0.5f, "millionaire", "Recharge to earn"),
    MedalItem("mill_3", "Millionaire III", "/IMG_20261023_110825.png", "/1000200522-background.mp4", 3, "achievement", "black", 120, 0.5f, 200, 0.5f, "millionaire", "Recharge to earn"),

    // Super Gamer
    MedalItem("gamer_1", "Super Gamer 1", "/IMG_20261023_110852.png", "/1000201032-background.mp4", 1, "achievement", "black", 120, 0.5f, 200, 0.5f, "gamer", "Play games to earn"),
    MedalItem("gamer_2", "Super Gamer 2", "/IMG_20261023_110852.png", "/1000201032-background.mp4", 2, "achievement", "black", 120, 0.5f, 200, 0.5f, "gamer", "Play games to earn"),
    MedalItem("gamer_3", "Super Gamer 3", "/IMG_20261023_110852.png", "/1000201032-background.mp4", 3, "achievement", "black", 120, 0.5f, 200, 0.5f, "gamer", "Play games to earn"),

    // VIP & Pure Love (Activity/Gifts)
    MedalItem("vip_1", "VIP 1", "/IMG_20261023_110912.png", "/1000200506-background.mp4", 1, "activity", "black", 120, 0.5f, 200, 0.5f, "vip", "VIP Event"),
    MedalItem("vip_2", "VIP 2", "/IMG_20261023_110930.png", "/1000200505-background.mp4", 2, "activity", "black", 120, 0.5f, 200, 0.5f, "vip", "VIP Event"),
    MedalItem("love_1", "Pure Love", "/IMG_20261023_111000.png", "/1000200507-background.mp4", 1, "gift", "black", 120, 0.5f, 200, 0.5f, "love", "Gift rewards"),

    // Medals 11, 12, 13
    MedalItem("medal_11", "Medal 11", "/IMG_20261023_111027.png", "/1000200513-background.mp4", 1, "activity", "black", 120, 0.5f, 200, 0.5f, "medals_x", "Special event rewards"),
    MedalItem("medal_12", "Medal 12", "/IMG_20261023_111053.png", "/1000200512-background.mp4", 2, "activity", "black", 120, 0.5f, 200, 0.5f, "medals_x", "Special event rewards"),
    MedalItem("medal_13", "Medal 13", "/IMG_20261023_111115.png", "/1000200511-background.mp4", 3, "activity", "black", 120, 0.5f, 200, 0.5f, "medals_x", "Special event rewards")
)

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import kotlin.math.sin

private const val RAW_URL = "https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/"
private val String.fullUrl: String get() = if (this.startsWith("http")) this else RAW_URL + this.removePrefix("/")

data class MedalItem(
    val id: String,
    val name: String,
    val video: String,
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
            .background(Color(0xFF02050E))
    ) {
        // 1. WebGL/Starry Background equivalent
        StarryBackground()

        // 2. Top Gradient Mask Image
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.48f)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xFF02050E).copy(alpha = 0.3f), Color(0xFF02050E)),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        ) {
            AsyncImage(
                model = "file_00000000f1dc821196bf96f688c3b2f6.png".fullUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = 0.6f }
            )
        }

        // 3. Main Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            // Header
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
                    text = "Medal",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { showRules = true }) {
                    Icon(Icons.Default.HelpOutline, "Rules", tint = Color.White.copy(alpha = 0.8f))
                }
            }

            // The Medal I Wear Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 12.dp)) {
                    Box(modifier = Modifier.width(40.dp).height(1.dp).background(Color(0xFFA89BBF).copy(alpha = 0.6f)))
                    Text(
                        text = "The Medal I Wear",
                        color = Color(0xFFF3F4F6),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    Box(modifier = Modifier.width(40.dp).height(1.dp).background(Color(0xFFA89BBF).copy(alpha = 0.6f)))
                }

                // 10 Slots Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    repeat(5) {
                        MedalSlot { showUnequipped = true }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    repeat(5) {
                        MedalSlot { showUnequipped = true }
                    }
                }

                Row(
                    modifier = Modifier.padding(top = 24.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Obtained Medal(s): ", color = Color(0xFFE5E7EB), fontSize = 15.sp)
                    Text("3", color = Color(0xFFFACC15), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Check>",
                        color = Color(0xFFFACC15),
                        fontSize = 15.sp,
                        modifier = Modifier.clickable { showObtained = true }
                    )
                }
            }

            // Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("achievement" to "Achievements", "activity" to "Activities", "gift" to "Gift").forEach { (key, label) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { activeTab = key }
                    ) {
                        Text(
                            text = label,
                            color = if (activeTab == key) Color.White else Color(0xFF8B79B5),
                            fontSize = 15.sp,
                            fontWeight = if (activeTab == key) FontWeight.Bold else FontWeight.SemiBold
                        )
                        if (activeTab == key) {
                            Box(modifier = Modifier.padding(top = 4.dp).width(8.dp).height(2.dp).clip(CircleShape).background(Color(0xFFFACC15)))
                        }
                    }
                }
            }

            // Medal Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredMedals.size) { index ->
                    val medal = filteredMedals[index]
                    Box(
                        modifier = Modifier
                            .height(190.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Brush.verticalGradient(listOf(Color(0xFF1A1230), Color(0xFF0D0820))))
                            .clickable {
                                val group = getTierMedals(medal)
                                val idx = group.indexOfFirst { it.id == medal.id }
                                activeTier = if (idx >= 0) idx else 0
                                selectedMedal = medal
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Dummy Image/Video Placeholder (Grayscale)
                        AsyncImage(
                            model = medal.video.fullUrl, // Ideally a thumbnail of the video
                            contentDescription = null,
                            colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }),
                            modifier = Modifier.size(medal.cardVideoSize.dp).align(Alignment.Center)
                        )
                        Text(
                            text = medal.name,
                            color = Color(0xFFE5E7EB),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp)
                        )
                    }
                }
            }
        }

        // Full Screen Overlay (Medal Detail)
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
                    // Top Header Image
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

                        // Video playing area (Colorful)
                        AsyncImage(
                            model = displayMedal.video.fullUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(displayMedal.sheetVideoSize.dp)
                                .align(Alignment.Center)
                                .offset(y = 20.dp) // adjust based on sheetVideoTop percentage
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
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        if (tierMedals.size > 1) {
                            Spacer(modifier = Modifier.height(24.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                tierMedals.indices.forEach { i ->
                                    if (i > 0) Text(" > ", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(horizontal = 8.dp))
                                    AsyncImage(
                                        model = TIER_TAB_IMAGES[i].fullUrl,
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
                            // Styled Button
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

    // BOTTOM SHEETS

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

// Helper UI Components

@Composable
fun MedalSlot(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF281B54).copy(alpha = 0.6f))
            .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.Add, contentDescription = "Add", tint = Color(0xFFE2D5FF), modifier = Modifier.size(24.dp))
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulesBottomSheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF2A1B5E),
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.75f)) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp).drawBehind {
                    drawLine(color = Color.White.copy(alpha = 0.1f), start = Offset(0f, size.height), end = Offset(size.width, size.height), strokeWidth = 1.dp.toPx())
                },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Rules", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium)
            }
            
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text("*Rules*", color = Color(0xFFFACC15), fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
                
                RuleSection("1. Important Information:", "(a) Before participating, please carefully review the event rules...\n(b) Your participation confirms that you have read...")
                RuleSection("2. What are Medals?", "(a) Medals are special rewards...\n(b) Medals are divided by achievement...")
                RuleSection("3. How to earn medals?", "=>. Each medal type has its own unique earning criteria...")
                RuleSection("4. Medal Role:", "=>. Medals showcase your achievements in various areas...")
                RuleSection("5. Medal Benefits Expiration:", "(a) Medals generally have no expiration date...\n(b) Some medals are time-limited...\n(c) You can display a maximum of 10 medals at a time.\n(d) These will appear in your info card...\n(e) To check your medal status...")
                RuleSection("6. Improving Medal Levels", "(=>) Your medal level is determined by your current task...")
                RuleSection("7. Additional Notes:", "(a) We strictly prohibit any form of abuse...\n(b) We are constantly working to improve...\n(c) Medal rewards cannot be exchanged...")
            }
        }
    }
}

@Composable
fun RuleSection(title: String, desc: String) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Text(text = "*$title*", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
        Text(text = desc, color = Color.White, fontSize = 13.sp, lineHeight = 20.sp)
    }
}

@Composable
fun StarryBackground() {
    // Basic animated background mimicking WebGL shader
    val infiniteTransition = rememberInfiniteTransition()
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(tween(10000, easing = LinearEasing), RepeatMode.Restart)
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(Color(0xFF010205))
        // Simple pulsing stars simulation
        for (i in 0..50) {
            val x = (i * 123.4f) % size.width
            val y = (i * 321.4f) % size.height
            val alpha = (sin(time + i) + 1f) / 2f
            drawCircle(Color.White.copy(alpha = alpha * 0.5f), radius = 2f, center = Offset(x, y))
        }
    }
}

// Dummy Data Function
fun getMedalsData(): List<MedalItem> = listOf(
    MedalItem("1", "Rich", "/VID_20260924_124439.mp4", 5, "achievement", "green", 500, 0.5f, 700, 0.85f, "rich", "0/5000000000000 Coins Of gifts Send"),
    MedalItem("2", "Rich", "/1000200509-background.mp4", 4, "achievement", "black", 500, 0.5f, 700, 0.85f, "rich", "0/400000000000 Coins Of gifts Send"),
    MedalItem("new-1", "Super Gamer", "/1000201032-background.mp4", 4, "achievement", "black", 400, 0.5f, 660, 0.85f, "supergamer", "0/1000000000000 Coins You won from game"),
    MedalItem("new-1-b", "Super Gamer 2", "/1000201032-background.mp4", 4, "dummy", "black", 400, 0.5f, 660, 0.85f, "supergamer", "0/2000000000000 Coins You won from game"),
    // Add rest of the items exactly as they were in React...
)

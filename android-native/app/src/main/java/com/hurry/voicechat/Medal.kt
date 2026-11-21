package com.hurry.voicechat

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import kotlin.math.cos
import kotlin.math.sin

private const val RAW_URL = "https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/"
private val String.fullUrl: String get() = if (this.startsWith("http")) this else RAW_URL + this.removePrefix("/")

data class MedalItem(
    val id: String,
    val name: String,
    val video: String, // Medal Image
    val previewVideo: String,
    val stars: Int,
    val category: String, // 'achievement' | 'gift' | 'activity' | 'dummy'
    val variant: String = "black",
    val cardVideoSize: Int,
    val cardVideoTop: Float,
    val sheetVideoSize: Int,
    val sheetVideoTop: Float,
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
    val context = LocalContext.current
    val medalUserId = remember {
        val prefs = context.getSharedPreferences("hurry_auth", android.content.Context.MODE_PRIVATE)
        prefs.getString("accountId", null)?.takeIf { it.isNotBlank() } ?: prefs.getString("uid", "").orEmpty()
    }
    var activeTab by remember { mutableStateOf("event") }
    var selectedMedal by remember { mutableStateOf<MedalItem?>(null) }
    var activeTier by remember { mutableIntStateOf(0) }

    var showObtained by remember { mutableStateOf(false) }
    var showUnequipped by remember { mutableStateOf(false) }
    var showRules by remember { mutableStateOf(false) }

    val medals = remember { getMedalsData() }
    val filteredMedals = remember(activeTab, medals) {
        if (activeTab == "honor") {
            medals.filter { it.category == "achievement" }
        } else {
            medals.filter { it.category == "activity" || it.category == "gift" }
        }
    }

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
            .background(Color(0xFF030B17))
    ) {
        // 1. Fixed Top Background Image
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.45f)
                .align(Alignment.TopCenter)
        ) {
            AsyncImage(
                model = "file_000000008f6c82109aa9c53c85841c5b.png".fullUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = 0.55f }
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF030B17).copy(alpha = 0.55f),
                                Color(0xFF030B17).copy(alpha = 0.35f),
                                Color(0xFF030B17).copy(alpha = 0.75f)
                            )
                        )
                    )
            )
        }

        // 2. Main Fixed Layout
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
                    text = "My Badges",
                    color = Color.White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { showRules = true }) {
                    Icon(Icons.Default.HelpOutline, "Rules", tint = Color.White.copy(alpha = 0.9f))
                }
            }

            // 10 Slots Grid
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 10.dp),
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
                Spacer(modifier = Modifier.height(14.dp))
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

            // 2 Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf(
                    "honor" to "Honor Badge·0",
                    "event" to "Event Badge·0"
                ).forEach { (key, label) ->
                    val isSelected = activeTab == key
                    val tabShape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(tabShape)
                            .background(
                                if (isSelected) {
                                    Brush.verticalGradient(
                                        listOf(Color(0xFF0E5BB5), Color(0xFF072F6B))
                                    )
                                } else {
                                    Brush.verticalGradient(
                                        listOf(
                                            Color(0xFF071E42).copy(alpha = 0.85f),
                                            Color(0xFF041229).copy(alpha = 0.92f)
                                        )
                                    )
                                }
                            )
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) Color(0xFFE5C158) else Color(0xFF28558F).copy(alpha = 0.55f),
                                shape = tabShape
                            )
                            .clickable { activeTab = key },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.White else Color(0xFFAEC5E3),
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Bottom Curved Container
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
                    .padding(top = 2.dp)
                    .clip(bottomShape)
                    .background(Color(0xFF041126))
                    .clipToBounds()
            ) {
                AsyncImage(
                    model = "file_00000000a8e08210a8a24219461e3ce6.png".fullUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = 0.45f }
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF030E21).copy(alpha = 0.5f))
                )

                Column(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp)) {
                    LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 16.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clipToBounds()
                ) {
                    if (activeTab == "event") {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            NativeOwnedMedalShowcase(userId = medalUserId)
                        }
                    } else {
                        items(medalsList, key = { it.levelText }) { medal ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF1E2740), RoundedCornerShape(12.dp))
                                    .clickable { },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                AsyncImage(
                                    model = RAW_URL + medal.imageFile,
                                    contentDescription = "${medal.levelText} Medal",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(108.dp)
                                        .padding(8.dp)
                                )
                                Text(
                                    text = "${medal.levelText} Medal",
                                    color = Color(0xFFE5EEFA),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                        }
                    }
            }
        }
        }

        // FULL SCREEN MEDAL DETAIL DIALOG (IMAGE ONLY - SAME AS SCREENSHOT)
        if (selectedMedal != null && displayMedal != null) {
            MedalDetailDialog(
                displayMedal = displayMedal,
                tierMedals = tierMedals,
                activeTier = activeTier,
                onSelectTier = { activeTier = it },
                onDismiss = { selectedMedal = null }
            )
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
        WearBadgeBottomSheet(userId = medalUserId, onDismiss = { showUnequipped = false })
    }

    if (showRules) {
        RulesBottomSheet(onDismiss = { showRules = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WearBadgeBottomSheet(userId: String, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val buttonShape = RoundedCornerShape(12.dp)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF04142B),
        scrimColor = Color.Black.copy(alpha = 0.45f),
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = "Wear Badge",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                NativeOwnedMedalShowcase(userId = userId, showEmpty = true)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 22.dp)
                    .height(48.dp)
                    .clip(buttonShape)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF0E5BB5),
                                Color(0xFF072F6B)
                            )
                        )
                    )
                    .border(1.5.dp, Color(0xFFE5C158), buttonShape)
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Save",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun MedalDetailDialog(
    displayMedal: MedalItem,
    tierMedals: List<MedalItem>,
    activeTier: Int,
    onSelectTier: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.94f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBars)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(64.dp))

                // Medal Image with Rotating Golden Sunburst Rays & Confetti
                Box(
                    modifier = Modifier
                        .size(310.dp),
                    contentAlignment = Alignment.Center
                ) {
                    GoldenSunburstEffect(modifier = Modifier.fillMaxSize())

                    AsyncImage(
                        model = displayMedal.video.fullUrl,
                        contentDescription = displayMedal.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(215.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Medal Name
                Text(
                    text = displayMedal.name,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Subtitle / Gift Text
                Text(
                    text = displayMedal.giftText,
                    color = Color(0xFFB8BCC4),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center
                )

                // 1 > 2 > 3 Tier Images Row (Same TIER_TAB_IMAGES)
                if (tierMedals.size > 1) {
                    Spacer(modifier = Modifier.height(22.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        )
                    ) {
                        tierMedals.indices.forEach { i ->
                            if (i > 0) {
                                Text(
                                    text = ">",
                                    color = Color(0xFF9CA3AF),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 14.dp)
                                )
                            }
                            val isSelected = activeTier == i
                            val boxShape = RoundedCornerShape(10.dp)
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(boxShape)
                                    .background(
                                        if (isSelected) Color(0xFF1A1610) else Color.Transparent
                                    )
                                    .border(
                                        width = if (isSelected) 1.3.dp else 0.dp,
                                        color = if (isSelected) Color(0xFFE5C158) else Color.Transparent,
                                        shape = boxShape
                                    )
                                    .clickable { onSelectTier(i) }
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = TIER_TAB_IMAGES.getOrNull(i)?.fullUrl ?: TIER_TAB_IMAGES.last().fullUrl,
                                    contentDescription = "Tier ${i + 1}",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer { alpha = if (isSelected) 1f else 0.85f }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // "Not Obtained" Grey Pill Button (Same as Screenshot)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF3E3E40))
                        .padding(horizontal = 18.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Not Obtained",
                        color = Color(0xFFD4D4D8),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Bottom Text
                Text(
                    text = "The event has ended",
                    color = Color(0xFF8E8E93),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.padding(bottom = 26.dp)
                )
            }
        }
    }
}

@Composable
fun GoldenSunburstEffect(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "sunburst")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 28000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sunburstRotation"
    )

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension * 0.80f
        val rayCount = 18

        // Soft Golden Radial Glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFDF7A).copy(alpha = 0.45f),
                    Color(0xFFD9A12B).copy(alpha = 0.20f),
                    Color.Transparent
                ),
                center = center,
                radius = radius * 0.85f
            ),
            radius = radius * 0.85f,
            center = center
        )

        // Rotating Golden Rays
        rotate(degrees = rotation, pivot = center) {
            val stepAngle = 360f / rayCount
            for (i in 0 until rayCount) {
                val startDeg = i * stepAngle
                val halfSpread = stepAngle * 0.28f
                val rad1 = Math.toRadians((startDeg - halfSpread).toDouble())
                val rad2 = Math.toRadians((startDeg + halfSpread).toDouble())

                val path = Path().apply {
                    moveTo(center.x, center.y)
                    lineTo(
                        center.x + (radius * cos(rad1)).toFloat(),
                        center.y + (radius * sin(rad1)).toFloat()
                    )
                    lineTo(
                        center.x + (radius * cos(rad2)).toFloat(),
                        center.y + (radius * sin(rad2)).toFloat()
                    )
                    close()
                }
                drawPath(
                    path = path,
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFE694).copy(alpha = 0.58f),
                            Color(0xFFE0A82E).copy(alpha = 0.28f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = radius
                    )
                )
            }
        }
    }
}

// HELPER UI COMPONENTS
@Composable
fun MedalSlot(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
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
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1E1245))
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        AsyncImage(
                            model = img.fullUrl,
                            contentDescription = null,
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentScale = ContentScale.Fit
                        )
                        Text(
                            text = name,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RulesBottomSheet(onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        val cardShape = RoundedCornerShape(14.dp)
        val buttonShape = RoundedCornerShape(12.dp)
        val goldenBorderBrush = Brush.verticalGradient(
            listOf(Color(0xFFF3D578), Color(0xFFC59B38), Color(0xFFE5C158))
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.86f)
                    .clip(cardShape)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF0B4182),
                                Color(0xFF072957),
                                Color(0xFF041A3B)
                            )
                        )
                    )
                    .border(1.5.dp, goldenBorderBrush, cardShape)
                    .clickable(enabled = false) {}
                    .padding(horizontal = 22.dp, vertical = 26.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
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

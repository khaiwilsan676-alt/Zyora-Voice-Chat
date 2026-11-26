package com.hurry.voicechat

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

private const val BASE_IMAGE_URL =
    "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/"

private const val BATTLE_ICON_URL = "${BASE_IMAGE_URL}public/IMG_20261028_182422.png"
private const val BLUE_CARD_URL = "${BASE_IMAGE_URL}public/IMG_20261126_011922.png"
private const val RED_CARD_URL = "${BASE_IMAGE_URL}public/IMG_20261126_011946.png"

private const val SELECT_BLUE_BG_URL =
    "${BASE_IMAGE_URL}public/file_00000000c6848211b4e0d9153986df4d.png"
private const val SELECT_RED_BG_URL =
    "${BASE_IMAGE_URL}public/file_00000000b5d882119d8f0569b18d4e79.png"
private const val RULES_BG_URL =
    "${BASE_IMAGE_URL}public/file_00000000d178821186730c503cab3ecf.png"

// Image ke hisaab se Black Sheet + Green & Gold Theme Colors
private val PkSheetBg = Color.Black
private val PkGreenSelectedBg = Color(0xFF0C6E52)
private val PkGreenSelectedBorder = Color(0xFF28B87D)
private val PkUnselectedBg = Color(0xFF191D1B)
private val PkUnselectedBorder = Color(0xFF3E4643)
private val PkStartGradientStart = Color(0xFF19584C)
private val PkStartGradientEnd = Color(0xFF103F36)
private val PkStartGoldBorder = Color(0xFFC8A64B)
private val PkHelpGoldBorder = Color(0xFFE2C265)

private val pkImageCache = ConcurrentHashMap<String, ImageBitmap>()

private enum class PkSheetView {
    MAIN,
    SELECT_BLUE,
    SELECT_RED,
    RULES
}

@Composable
fun PKBattlePage(onClose: () -> Unit) {
    var selectedMode by remember { mutableStateOf("Votes") }
    var selectedTime by remember { mutableStateOf("30 s") }
    var activeSheet by remember { mutableStateOf(PkSheetView.MAIN) }
    var selectedBlueIndex by remember { mutableStateOf<Int?>(null) }
    var selectedRedIndex by remember { mutableStateOf<Int?>(null) }

    BackHandler {
        if (activeSheet != PkSheetView.MAIN) {
            activeSheet = PkSheetView.MAIN
        } else {
            onClose()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.52f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (activeSheet != PkSheetView.MAIN) {
                    activeSheet = PkSheetView.MAIN
                } else {
                    onClose()
                }
            }
    ) {
        // Main 60vh Battle Sheet hamesha neeche rahegi
        PkMainBattleSheet(
            selectedMode = selectedMode,
            onModeSelect = { selectedMode = it },
            selectedTime = selectedTime,
            onTimeSelect = { selectedTime = it },
            onBlueCardClick = { activeSheet = PkSheetView.SELECT_BLUE },
            onRedCardClick = { activeSheet = PkSheetView.SELECT_RED },
            onHelpClick = { activeSheet = PkSheetView.RULES },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // Overlay sheets (+15 Blue, +15 Red, Rules) seedha Main sheet ke upar bottom par open hongi
        if (activeSheet != PkSheetView.MAIN) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.42f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        activeSheet = PkSheetView.MAIN
                    }
            )
        }

        when (activeSheet) {
            PkSheetView.SELECT_BLUE -> {
                PkSelectPeopleBottomSheet(
                    bgImageUrl = SELECT_BLUE_BG_URL,
                    fallbackBgColor = Color(0xFF0B2732),
                    plusTint = Color(0xFF3FA9D6),
                    activeBorderColor = Color(0xFF60A5FA),
                    selectedSlotIndex = selectedBlueIndex,
                    onSlotClick = { index ->
                        selectedBlueIndex = if (selectedBlueIndex == index) null else index
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }

            PkSheetView.SELECT_RED -> {
                PkSelectPeopleBottomSheet(
                    bgImageUrl = SELECT_RED_BG_URL,
                    fallbackBgColor = Color(0xFF2E0F18),
                    plusTint = Color(0xFFB9435F),
                    activeBorderColor = Color(0xFFF87171),
                    selectedSlotIndex = selectedRedIndex,
                    onSlotClick = { index ->
                        selectedRedIndex = if (selectedRedIndex == index) null else index
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }

            PkSheetView.RULES -> {
                PkRulesBottomSheet(
                    onBack = { activeSheet = PkSheetView.MAIN },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }

            PkSheetView.MAIN -> Unit
        }
    }
}

@Composable
private fun PkMainBattleSheet(
    selectedMode: String,
    onModeSelect: (String) -> Unit,
    selectedTime: String,
    onTimeSelect: (String) -> Unit,
    onBlueCardClick: () -> Unit,
    onRedCardClick: () -> Unit,
    onHelpClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.60f)
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(PkSheetBg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { }
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Top Header: Square Battle Icon (No Crop/Cut) + Title + Gold Help (?)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PkRemoteImage(
                url = BATTLE_ICON_URL,
                contentDescription = "Battle",
                modifier = Modifier.size(34.dp),
                contentScale = ContentScale.Fit,
                fallbackColor = Color.Transparent
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "Battle",
                style = TextStyle(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.White, Color(0xFFF3CA52))
                    ),
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, PkHelpGoldBorder, CircleShape)
                    .clickable(onClick = onHelpClick),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "?",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Select People Section
        Text(
            text = "Select People",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PkSelectPersonCard(
                imageUrl = BLUE_CARD_URL,
                modifier = Modifier.weight(1f),
                onClick = onBlueCardClick
            )
            PkSelectPersonCard(
                imageUrl = RED_CARD_URL,
                modifier = Modifier.weight(1f),
                onClick = onRedCardClick
            )
        }

        Spacer(Modifier.height(12.dp))

        // Mode Section
        Text(
            text = "Mode",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PkOptionChip(
                label = "Votes",
                selected = selectedMode == "Votes",
                modifier = Modifier.weight(1f),
                onClick = { onModeSelect("Votes") }
            )
            PkOptionChip(
                label = "Coins",
                selected = selectedMode == "Coins",
                modifier = Modifier.weight(1f),
                onClick = { onModeSelect("Coins") }
            )
            Spacer(Modifier.weight(1f))
        }

        Spacer(Modifier.height(12.dp))

        // Time Section
        Text(
            text = "Time",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            listOf("30 s", "1 min", "5 min").forEach { time ->
                PkOptionChip(
                    label = time,
                    selected = selectedTime == time,
                    modifier = Modifier.weight(1f),
                    onClick = { onTimeSelect(time) }
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            listOf("10 min", "15 min").forEach { time ->
                PkOptionChip(
                    label = time,
                    selected = selectedTime == time,
                    modifier = Modifier.weight(1f),
                    onClick = { onTimeSelect(time) }
                )
            }
            Spacer(Modifier.weight(1f))
        }

        Spacer(Modifier.height(14.dp))

        // Start Button (Green Gradient + Gold Border same as Image)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(PkStartGradientStart, PkStartGradientEnd)
                    )
                )
                .border(1.5.dp, PkStartGoldBorder, RoundedCornerShape(12.dp))
                .clickable { },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Start",
                color = Color(0xFFE6F4EF),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun PkSelectPeopleBottomSheet(
    bgImageUrl: String,
    fallbackBgColor: Color,
    plusTint: Color,
    activeBorderColor: Color,
    selectedSlotIndex: Int?,
    onSlotClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetShape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(sheetShape)
            .background(fallbackBgColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { }
    ) {
        // Background image ekdam fit (No Zoom, No Crop)
        PkRemoteImage(
            url = bgImageUrl,
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.FillBounds,
            fallbackColor = fallbackBgColor
        )

        // Top heading aur icon hata diya hai — sirf 3x5 (15 slots) + grid
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            for (row in 0 until 3) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (col in 0 until 5) {
                        val index = row * 5 + col
                        val isSelected = selectedSlotIndex == index
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.30f))
                                .then(
                                    if (isSelected) {
                                        Modifier.border(1.5.dp, activeBorderColor, CircleShape)
                                    } else {
                                        Modifier
                                    }
                                )
                                .clickable { onSlotClick(index) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Person",
                                tint = if (isSelected) Color.White else plusTint,
                                modifier = Modifier.size(25.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PkRulesBottomSheet(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetShape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 270.dp)
            .clip(sheetShape)
            .background(Color(0xFF143127))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { }
    ) {
        PkRemoteImage(
            url = RULES_BG_URL,
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.FillBounds,
            fallbackColor = Color(0xFF143127)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            // Top Bar: Back arrow + Centered Rules Title
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = "Rules",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(26.dp))

            // Plain text only (No icons/emojis)
            Text(
                text = "Number of votes",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = "PK according to number of votes received, 1 person 1 vote.",
                color = Color.White.copy(alpha = 0.88f),
                fontSize = 14.sp,
                lineHeight = 20.sp
            )

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Number of coins",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = "PK according to number of coins received.",
                color = Color.White.copy(alpha = 0.88f),
                fontSize = 14.sp,
                lineHeight = 20.sp
            )

            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun PkSelectPersonCard(
    imageUrl: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    // No border, No crop — image ekdam Fit show hogi
    Box(
        modifier = modifier
            .height(98.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        PkRemoteImage(
            url = imageUrl,
            contentDescription = "Select Person",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
            fallbackColor = Color.Transparent
        )

        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.28f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun PkOptionChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) PkGreenSelectedBg else PkUnselectedBg)
            .border(
                width = if (selected) 1.2.dp else 1.dp,
                color = if (selected) PkGreenSelectedBorder else PkUnselectedBorder,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}

@Composable
private fun PkRemoteImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    fallbackColor: Color = Color.Transparent
) {
    var bitmap by remember(url) { mutableStateOf(pkImageCache[url]) }

    LaunchedEffect(url) {
        if (bitmap == null) {
            val loaded = withContext(Dispatchers.IO) {
                try {
                    val conn = URL(url).openConnection() as HttpURLConnection
                    conn.connectTimeout = 8000
                    conn.readTimeout = 8000
                    conn.doInput = true
                    conn.connect()
                    conn.inputStream.use { input ->
                        BitmapFactory.decodeStream(input)?.asImageBitmap()
                    }
                } catch (_: Exception) {
                    null
                }
            }
            if (loaded != null) {
                pkImageCache[url] = loaded
                bitmap = loaded
            }
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
    } else {
        Box(modifier = modifier.background(fallbackColor))
    }
}

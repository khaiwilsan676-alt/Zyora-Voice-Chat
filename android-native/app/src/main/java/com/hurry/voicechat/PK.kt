package com.hurry.voicechat

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
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

private val PkSheetBg = Color(0xFF0E1626)
private val PkBlueSelectedBg = Color(0xFF155EEF)
private val PkBlueSelectedBorder = Color(0xFF60A5FA)
private val PkBlueUnselectedBg = Color(0xFF172238)
private val PkBlueUnselectedBorder = Color(0xFF283A5A)
private val PkStartGradientStart = Color(0xFF1D6BFF)
private val PkStartGradientEnd = Color(0xFF0F44B8)
private val PkStartBorder = Color(0xFF6CA8FF)

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
        when (activeSheet) {
            PkSheetView.MAIN -> {
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
            }

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
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(PkSheetBg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { }
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 20.dp)
    ) {
        // Top Header: Battle Icon + Title + Help (?)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PkRemoteImage(
                url = BATTLE_ICON_URL,
                contentDescription = "Battle",
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
                fallbackColor = Color(0xFF1E3A8A)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "Battle",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, Color(0xFF60A5FA), CircleShape)
                    .clickable(onClick = onHelpClick),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "?",
                    color = Color(0xFF93C5FD),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(22.dp))

        // Select People Section
        Text(
            text = "Select People",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            PkSelectPersonCard(
                imageUrl = BLUE_CARD_URL,
                fallbackColor = Color(0xFF103B66),
                borderColor = Color(0xFF60A5FA).copy(alpha = 0.7f),
                modifier = Modifier.weight(1f),
                onClick = onBlueCardClick
            )
            PkSelectPersonCard(
                imageUrl = RED_CARD_URL,
                fallbackColor = Color(0xFF5A1624),
                borderColor = Color(0xFFF87171).copy(alpha = 0.7f),
                modifier = Modifier.weight(1f),
                onClick = onRedCardClick
            )
        }

        Spacer(Modifier.height(22.dp))

        // Mode Section
        Text(
            text = "Mode",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
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

        Spacer(Modifier.height(22.dp))

        // Time Section
        Text(
            text = "Time",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
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

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
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

        Spacer(Modifier.height(26.dp))

        // Start Button (Full Blue Theme)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(PkStartGradientStart, PkStartGradientEnd)
                    )
                )
                .border(1.2.dp, PkStartBorder, RoundedCornerShape(12.dp))
                .clickable { },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Start",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(6.dp))
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
        PkRemoteImage(
            url = bgImageUrl,
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop,
            fallbackColor = fallbackBgColor
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 18.dp)
        ) {
            // Top Icon + Select People Text
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 2.dp)
            ) {
                PkGoldShieldIcon(modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.Bold)) {
                            append("Select ")
                        }
                        withStyle(SpanStyle(color = Color(0xFFF2C94C), fontWeight = FontWeight.Bold)) {
                            append("People")
                        }
                    },
                    fontSize = 20.sp
                )
            }

            Spacer(Modifier.height(22.dp))

            // 3x5 Grid (15 circular slots with + icon instead of seat icon)
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
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
                                    .size(56.dp)
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
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
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
            contentScale = ContentScale.Crop,
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
private fun PkGoldShieldIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val shieldPath = Path().apply {
            moveTo(w * 0.50f, h * 0.04f)
            cubicTo(w * 0.72f, h * 0.08f, w * 0.90f, h * 0.24f, w * 0.90f, h * 0.50f)
            cubicTo(w * 0.90f, h * 0.74f, w * 0.72f, h * 0.88f, w * 0.50f, h * 0.96f)
            cubicTo(w * 0.28f, h * 0.88f, w * 0.10f, h * 0.74f, w * 0.10f, h * 0.50f)
            cubicTo(w * 0.10f, h * 0.24f, w * 0.28f, h * 0.08f, w * 0.50f, h * 0.04f)
            close()
        }

        drawPath(
            path = shieldPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFFFEE9C), Color(0xFFF3C64F), Color(0xFFD69823))
            )
        )

        val visorPath = Path().apply {
            moveTo(w * 0.30f, h * 0.45f)
            lineTo(w * 0.70f, h * 0.45f)
            cubicTo(w * 0.70f, h * 0.58f, w * 0.60f, h * 0.64f, w * 0.50f, h * 0.64f)
            cubicTo(w * 0.40f, h * 0.64f, w * 0.30f, h * 0.58f, w * 0.30f, h * 0.45f)
            close()
        }

        drawPath(
            path = visorPath,
            color = Color(0xFF1A1E24),
            style = Stroke(
                width = w * 0.09f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        drawLine(
            color = Color(0xFF1A1E24),
            start = Offset(w * 0.50f, h * 0.64f),
            end = Offset(w * 0.50f, h * 0.92f),
            strokeWidth = w * 0.09f,
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun PkSelectPersonCard(
    imageUrl: String,
    fallbackColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(114.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(fallbackColor)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        PkRemoteImage(
            url = imageUrl,
            contentDescription = "Select Person",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            fallbackColor = fallbackColor
        )

        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.32f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add",
                tint = Color.White,
                modifier = Modifier.size(26.dp)
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
            .height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) PkBlueSelectedBg else PkBlueUnselectedBg)
            .border(
                width = 1.dp,
                color = if (selected) PkBlueSelectedBorder else PkBlueUnselectedBorder,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else Color.White.copy(alpha = 0.88f),
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
    contentScale: ContentScale = ContentScale.Crop,
    fallbackColor: Color = Color(0xFF172238)
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

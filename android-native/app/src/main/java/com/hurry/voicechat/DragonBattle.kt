package com.hurry.voicechat

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import java.util.Locale
import kotlinx.coroutines.delay

private const val BASE_URL =
    "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/"

private const val DRAGON_BATTLE_BG =
    "${BASE_URL}public/file_00000000f3248211a67433d26cac032b.png"
private const val DRAGON_BATTLE_LOGO =
    "${BASE_URL}public/IMG_20261121_020906.png"

private const val CARD_IMAGE =
    "${BASE_URL}public/IMG_20261121_020825.png"
private const val TIMER_BG_IMAGE =
    "${BASE_URL}public/file_000000009cb882119b1349bc49e0d864.png"

private const val DRAGON_A_IMAGE =
    "${BASE_URL}public/IMG_20261121_020609.png"
private const val DRAGON_B_IMAGE =
    "${BASE_URL}public/IMG_20261121_020547.png"
private const val DRAGON_C_IMAGE =
    "${BASE_URL}public/IMG_20261121_020609.png"
private const val DRAGON_COIN_IMAGE =
    "${BASE_URL}public/file_00000000e56882119c217d508b6733dc.png"
private const val DRAGON_BOTTOM_LEFT_IMAGE =
    "${BASE_URL}public/file_00000000993c8208ae462545f9ccb0ba.png"

private data class BetChip(
    val label: String,
    val value: Long,
    val imageUrl: String
)

private val BET_CHIPS = listOf(
    BetChip("5K", 5_000L, "${BASE_URL}public/IMG_20261121_020630.png"),
    BetChip("50K", 50_000L, "${BASE_URL}public/IMG_20261121_020651.png"),
    BetChip("500K", 500_000L, "${BASE_URL}public/IMG_20261121_020720.png"),
    BetChip("5M", 5_000_000L, "${BASE_URL}public/IMG_20261121_020746.png"),
    BetChip("10M", 10_000_000L, "${BASE_URL}public/IMG_20261121_020809.png")
)

@Composable
internal fun DragonBattleGameItem(onClick: () -> Unit) {
    Column(
        modifier = Modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        AsyncImage(
            model = DRAGON_BATTLE_LOGO,
            contentDescription = "Dragon Battle",
            modifier = Modifier.size(56.dp),
            contentScale = ContentScale.Fit
        )
        Text(
            text = "Dragon Battle",
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

/**
 * Screen ke ekdam BOTTOM par open hoga.
 * Height sirf Background Image ki exact height ke barabar rahegi.
 */
@Composable
fun DragonBattleGameHost(
    modifier: Modifier = Modifier,
    onClose: () -> Unit = {},
    onMinimize: () -> Unit = {}
) {
    var isLoaded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(480.dp)
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
            contentAlignment = Alignment.BottomCenter
        ) {
            // Sirf is background image ki height hi game ki total height decide karegi
            AsyncImage(
                model = DRAGON_BATTLE_BG,
                contentDescription = "Dragon Battle Background",
                modifier = Modifier
                    .fillMaxSize(),
                contentScale = ContentScale.FillWidth
            )

            // Loading aur Main UI dono isi exact image height ke andar lock rahenge
            if (!isLoaded) {
                DragonBattleLoadingOverlay(
                    modifier = Modifier.matchParentSize(),
                    onLoadingFinished = { isLoaded = true }
                )
            } else {
                DragonBattleMainOverlay(
                    modifier = Modifier.matchParentSize(),
                    onClose = onClose,
                    onMinimize = onMinimize
                )
            }
        }
    }
}

@Composable
private fun DragonBattleLoadingOverlay(
    modifier: Modifier = Modifier,
    onLoadingFinished: () -> Unit
) {
    var progress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        val totalSteps = 100
        for (i in 1..totalSteps) {
            delay(25L)
            progress = i / totalSteps.toFloat()
        }
        onLoadingFinished()
    }

    Box(modifier = modifier) {
        Text(
            text = "Dragon Battle",
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp),
            style = TextStyle(
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Serif,
                shadow = Shadow(
                    color = Color(0xCC2B1200),
                    offset = Offset(2f, 3f),
                    blurRadius = 5f
                )
            )
        )

        AsyncImage(
            model = DRAGON_BATTLE_LOGO,
            contentDescription = "Dragon Battle Logo",
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 26.dp)
                .size(145.dp),
            contentScale = ContentScale.Fit
        )

        BlueGlossyLoadingBar(
            progress = progress,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 20.dp, vertical = 22.dp)
        )
    }
}

@Composable
private fun BlueGlossyLoadingBar(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val clampedProgress = progress.coerceIn(0f, 1f)
    val outerShape = RoundedCornerShape(50)
    val innerShape = RoundedCornerShape(50)

    val goldBorderBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFFF2A6),
            Color(0xFFE5A924),
            Color(0xFFA66A08),
            Color(0xFFF7D154)
        )
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
            .clip(outerShape)
            .background(Color(0xFF4E2605))
            .border(width = 2.5.dp, brush = goldBorderBrush, shape = outerShape)
            .padding(2.5.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (clampedProgress > 0f) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth(clampedProgress)
                    .fillMaxHeight()
                    .clip(innerShape)
            ) {
                val w = size.width
                val h = size.height

                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF5CE1FF),
                            Color(0xFF1589FF),
                            Color(0xFF004BB5)
                        )
                    )
                )

                val stripeWidth = h * 0.65f
                val step = stripeWidth * 2f
                var x = -h
                while (x < w + h) {
                    val path = Path().apply {
                        moveTo(x, h)
                        lineTo(x + stripeWidth, h)
                        lineTo(x + stripeWidth + h * 0.55f, 0f)
                        lineTo(x + h * 0.55f, 0f)
                        close()
                    }
                    drawPath(path = path, color = Color(0x4D9EEBFF))
                    x += step
                }

                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0x80FFFFFF), Color(0x00FFFFFF)),
                        startY = 0f,
                        endY = h * 0.45f
                    )
                )
            }
        }

        Text(
            text = String.format(Locale.US, "%.2f%%", clampedProgress * 100f),
            modifier = Modifier.align(Alignment.Center),
            style = TextStyle(
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                shadow = Shadow(
                    color = Color(0xCC001A4D),
                    offset = Offset(1f, 2f),
                    blurRadius = 3f
                )
            )
        )
    }
}

@Composable
private fun DragonBattleMainOverlay(
    modifier: Modifier = Modifier,
    onClose: () -> Unit = {},
    onMinimize: () -> Unit = {}
) {
    var timeLeft by remember { mutableIntStateOf(20) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            timeLeft = if (timeLeft > 1) timeLeft - 1 else 20
        }
    }

    var selectedChipIndex by remember { mutableIntStateOf(0) }
    var balance by remember { mutableLongStateOf(500_000L) }

    var potA by remember { mutableLongStateOf(0L) }
    var youA by remember { mutableLongStateOf(0L) }
    var potB by remember { mutableLongStateOf(0L) }
    var youB by remember { mutableLongStateOf(0L) }
    var potC by remember { mutableLongStateOf(0L) }
    var youC by remember { mutableLongStateOf(0L) }

    var lastBetTable by remember { mutableStateOf<String?>(null) }

    fun placeBet(table: String) {
        val chipValue = BET_CHIPS[selectedChipIndex].value
        if (balance < chipValue) return
        balance -= chipValue
        lastBetTable = table
        when (table) {
            "A" -> {
                potA += chipValue
                youA += chipValue
            }
            "B" -> {
                potB += chipValue
                youB += chipValue
            }
            "C" -> {
                potC += chipValue
                youC += chipValue
            }
        }
    }

    Column(
        modifier = modifier.padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. TOP HEADER BAR
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                HeaderCircleIcon(Icons.Default.OpenWith) {}
                HeaderCircleIcon(Icons.AutoMirrored.Filled.VolumeUp) {}
                HeaderCircleIcon(Icons.Default.HelpOutline) {}
                HeaderCircleIcon(Icons.Default.EmojiEvents) {}
            }

            Text(
                text = "Dragon Battle",
                style = TextStyle(
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Serif,
                    shadow = Shadow(
                        color = Color(0xCC2B1200),
                        offset = Offset(1f, 3f),
                        blurRadius = 4f
                    )
                )
            )

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                HeaderCircleIcon(Icons.Default.Menu) {}
                HeaderCircleIcon(Icons.Default.KeyboardArrowDown, onMinimize)
                HeaderCircleIcon(Icons.Default.Close, onClose)
            }
        }

        // 2. TOP 3 CARD TABLES + CENTER 20s TIMER
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OverlappingThreeCardsTable(multiplier = "2.92")
                OverlappingThreeCardsTable(multiplier = "2.92")
                OverlappingThreeCardsTable(multiplier = "2.92")
            }

            Box(
                modifier = Modifier
                    .offset(y = (-7).dp)
                    .size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = TIMER_BG_IMAGE,
                    contentDescription = "Countdown Timer Background",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                Text(
                    text = timeLeft.toString(),
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        shadow = Shadow(
                            color = Color(0xCC000000),
                            offset = Offset(1f, 2f),
                            blurRadius = 3f
                        )
                    )
                )
            }
        }

        // 3. DRAGONS (A, B, C) + BETTING BOXES
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DragonBetColumn(
                label = "A",
                dragonImage = DRAGON_A_IMAGE,
                pot = potA,
                you = youA,
                modifier = Modifier.weight(1f),
                onClick = { placeBet("A") }
            )
            DragonBetColumn(
                label = "B",
                dragonImage = DRAGON_B_IMAGE,
                pot = potB,
                you = youB,
                modifier = Modifier.weight(1f),
                onClick = { placeBet("B") }
            )
            DragonBetColumn(
                label = "C",
                dragonImage = DRAGON_C_IMAGE,
                pot = potC,
                you = youC,
                modifier = Modifier.weight(1f),
                onClick = { placeBet("C") }
            )
        }

        AsyncImage(\n            model = DRAGON_BOTTOM_LEFT_IMAGE,\n            contentDescription = null,\n            modifier = Modifier.align(Alignment.BottomStart).padding(start = 8.dp, bottom = 60.dp).size(44.dp),\n            contentScale = ContentScale.Fit\n        )\n\n        // 4. PLAYERS ICON
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            Box(
                modifier = Modifier.size(30.dp).clip(RoundedCornerShape(7.dp))
                    .background(Color(0xAA4A2508))
                    .border(1.2.dp, Color(0xFFE5B34A), RoundedCornerShape(7.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Groups, contentDescription = "Players",
                    tint = Color(0xFFFFE082), modifier = Modifier.size(18.dp))
            }
        }

        // Wild Party identical bottom-left artwork and balance / repeat styling.
        Box(Modifier.fillMaxWidth().height(0.dp)) 
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            DragonBalanceBar(
                balance = balance,
                modifier = Modifier.weight(1f, fill = false)
            )
            DragonRepeatButton(
                onClick = { lastBetTable?.let { placeBet(it) } }
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BET_CHIPS.forEachIndexed { index, chip ->
                    val isSelected = selectedChipIndex == index
                    Box(
                        modifier = Modifier.size(if (isSelected) 40.dp else 34.dp)
                            .scale(if (isSelected) 1.05f else 1f)
                            .clip(CircleShape)
                            .then(if (isSelected) Modifier.border(2.dp, Color(0xFFFFE047), CircleShape) else Modifier)
                            .clickable { selectedChipIndex = index },
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(model = chip.imageUrl, contentDescription = chip.label,
                            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderCircleIcon(
    icon: ImageVector,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(Color(0x993D2314))
            .border(1.dp, Color(0x88FFE0A3), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
private fun OverlappingThreeCardsTable(multiplier: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .width(88.dp)
                .height(58.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            AsyncImage(
                model = CARD_IMAGE,
                contentDescription = "Card 1",
                modifier = Modifier
                    .offset(x = 0.dp)
                    .size(width = 44.dp, height = 58.dp)
                    .clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Crop
            )
            AsyncImage(
                model = CARD_IMAGE,
                contentDescription = "Card 2",
                modifier = Modifier
                    .offset(x = 22.dp)
                    .size(width = 44.dp, height = 58.dp)
                    .clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Crop
            )
            AsyncImage(
                model = CARD_IMAGE,
                contentDescription = "Card 3",
                modifier = Modifier
                    .offset(x = 44.dp)
                    .size(width = 44.dp, height = 58.dp)
                    .clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = multiplier,
            color = Color(0xFFFFD699),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun DragonBetColumn(
    label: String,
    dragonImage: String,
    pot: Long,
    you: Long,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(82.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 2.dp, top = 2.dp),
                style = TextStyle(
                    color = Color(0xFFFFF3C4),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Serif,
                    shadow = Shadow(
                        color = Color(0xCC3B1800),
                        offset = Offset(2f, 3f),
                        blurRadius = 4f
                    )
                )
            )

            AsyncImage(
                model = dragonImage,
                contentDescription = "Dragon $label",
                modifier = Modifier.size(78.dp),
                contentScale = ContentScale.Fit
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(Color(0xD94B1A19))
                .border(1.dp, Color(0x55E5B34A), RoundedCornerShape(9.dp))
                .padding(bottom = 5.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Pot: ${formatShortAmount(pot)}",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "You: ${formatShortAmount(you)}",
                    color = Color(0xFFFFE082),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private fun formatShortAmount(amount: Long): String {
    return when {
        amount >= 1_000_000L -> {
            val v = amount / 1_000_000.0
            if (amount % 1_000_000L == 0L) "${amount / 1_000_000L}M"
            else String.format(Locale.US, "%.1fM", v)
        }
        amount >= 1_000L -> {
            val v = amount / 1_000.0
            if (amount % 1_000L == 0L) "${amount / 1_000L}K"
            else String.format(Locale.US, "%.1fK", v)
        }
        else -> amount.toString()
    }
}
@Composable
private fun DragonBalanceBar(balance: Long, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(7.dp)
    Row(modifier = modifier.clip(shape)
        .background(Brush.verticalGradient(listOf(Color.White.copy(.58f), Color(0xFFD5BFA6).copy(.34f), Color(0xFF5A3418).copy(.50f), Color(0xFF3E200C).copy(.55f))))
        .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(.90f), Color.White.copy(.38f), Color(0xFFE6C8A0).copy(.45f))), shape)
        .padding(horizontal = 4.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        AsyncImage(model = DRAGON_COIN_IMAGE, contentDescription = "Coins", modifier = Modifier.size(18.dp), contentScale = ContentScale.Fit)
        Text("›", color = Color(0xFFFFD84D), fontSize = 15.sp, fontWeight = FontWeight.Black)
        Text(formatShortAmount(balance), color = Color(0xFFFFD84D), fontSize = 13.sp, fontWeight = FontWeight.Black,
            style = TextStyle(shadow = Shadow(Color.Black.copy(alpha = .65f), Offset(0f, 1.5f), 3f)))
    }
}

@Composable
private fun DragonRepeatButton(onClick: () -> Unit) {
    val outer = RoundedCornerShape(9.dp)
    val inner = RoundedCornerShape(7.dp)
    Box(Modifier.clip(outer).background(Color(0xFF472710).copy(alpha = .72f))
        .border(1.2.dp, Brush.verticalGradient(listOf(Color.White.copy(.94f), Color(0xFFEBDCD0).copy(.75f), Color.White.copy(.85f))), outer)
        .clickable(onClick = onClick).padding(2.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.clip(inner)
            .background(Brush.verticalGradient(listOf(Color.White.copy(.52f), Color(0xFFCBB5A0).copy(.30f), Color(0xFF563218).copy(.68f), Color(0xFF6B4122).copy(.58f))))
            .border(.8.dp, Brush.verticalGradient(listOf(Color.White.copy(.85f), Color.White.copy(.28f), Color.White.copy(.52f))), inner)
            .padding(horizontal = 7.dp, vertical = 4.dp), contentAlignment = Alignment.Center) {
            Text("Repeat", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black,
                style = TextStyle(shadow = Shadow(Color.Black.copy(alpha = .55f), Offset(0f, 1.5f), 2.5f)))
        }
    }
}



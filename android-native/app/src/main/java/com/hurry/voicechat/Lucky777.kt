package com.hurry.voicechat

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay

private const val LUCKY777_BACKGROUND =
    "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/file_000000001d488211b1d4b391060d3c39.png"
private const val LUCKY777_LOGO =
    "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/IMG_20261127_231318.png"

@Composable
fun Lucky777(
    onClose: () -> Unit = {},
    onMinimize: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var progress by remember { mutableFloatStateOf(0f) }
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1800, easing = LinearEasing),
        label = "lucky777Loading"
    )

    LaunchedEffect(Unit) {
        delay(120)
        progress = 1f
    }

    val barShape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF17100A))
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
    ) {
        AsyncImage(
            model = LUCKY777_BACKGROUND,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.24f)))

        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 8.dp, end = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(34.dp).clip(RoundedCornerShape(18.dp))
                    .background(Color.Black.copy(alpha = 0.38f))
                    .clickable { onMinimize() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Minimize", tint = Color.White)
            }
            Box(
                Modifier.size(34.dp).clip(RoundedCornerShape(18.dp))
                    .background(Color.Black.copy(alpha = 0.38f))
                    .clickable { onClose() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }
        }

        Text(
            text = "Lucky 777",
            modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 14.dp),
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold
        )

        AsyncImage(
            model = LUCKY777_LOGO,
            contentDescription = "Lucky 777 Logo",
            modifier = Modifier.align(Alignment.Center).offset(y = (-14).dp).size(126.dp),
            contentScale = ContentScale.Fit
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 22.dp, end = 22.dp, bottom = 26.dp)
                .height(25.dp)
                .shadow(6.dp, barShape)
                .clip(barShape)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF562C05), Color(0xFF7E460A), Color(0xFF693706))
                    )
                )
                .border(
                    2.8.dp,
                    Brush.verticalGradient(
                        listOf(Color(0xFFFFF8B8), Color(0xFFFFDA3E), Color(0xFFE09200), Color(0xFFFFE56D))
                    ),
                    barShape
                )
                .padding(2.8.dp)
                .clip(barShape)
                .background(Color(0xFF17202B))
        ) {
            Box(
                Modifier.fillMaxHeight().fillMaxWidth(animatedProgress.coerceIn(0f, 1f))
                    .clip(barShape)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF8BE8FF), Color(0xFF21A6FF), Color(0xFF006EE6), Color(0xFF0047AB))
                        )
                    )
            )
        }
    }
}

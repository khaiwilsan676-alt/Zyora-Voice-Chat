package com.hurry.voicechat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.key
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay

data class RoomBroadcastNotice(
    val id: String,
    val video: String,
    val avatar: String,
    val name: String,
    val detail: String,
    val giftImage: String,
    val giftName: String
)

@Composable
fun RoomBroadcastNotification(notice: RoomBroadcastNotice, onDismiss: () -> Unit) {
    var secondsLeft by remember(notice.id) { mutableIntStateOf(5) }
    LaunchedEffect(notice.id) {
        repeat(5) {
            delay(1000L)
            secondsLeft = (secondsLeft - 1).coerceAtLeast(0)
        }
        onDismiss()
    }
    Box(Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 4.dp)) {
            // The banner lifetime is controlled by the five-second countdown below.
            // A shorter source video must not dismiss the notification early.
            key(notice.id) {
                // Recreate ExoPlayer for each notification, even when consecutive
                // notifications use the same MP4 URL.
                RoomGiftVideoOverlay(src = notice.video, onFinished = {})
            }
            Row(
                Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)).background(Color(0x22000000)).padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = notice.avatar,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(42.dp).clip(CircleShape)
                )
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                    Text(notice.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text("$secondsLeft", color = Color.White, fontSize = 11.sp)
                    Text(notice.detail, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                }
                if (notice.giftImage.isNotBlank()) {
                    AsyncImage(
                        model = notice.giftImage,
                        contentDescription = notice.giftName,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }
    }
}
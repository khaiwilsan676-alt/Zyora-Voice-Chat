package com.hurry.voicechat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import kotlinx.coroutines.delay

private const val LUCKY_BAG_SOCKET_URL = "https://hurry-voice-chat-lz75.onrender.com"
private const val LUCKY_BAG_BANNER_IMAGE = "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/file_000000001abc8211b1623dee45d87328.png"

private data class LuckyBagBannerData(
    val bagId: String,
    val roomId: String,
    val roomName: String,
    val senderId: String,
    val senderName: String,
    val senderImage: String,
    val amount: Long,
    val recipients: Int
)

/** Global banner layer: mounted above the active page so it can appear on every tab/screen. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LuckyBagNotificationSlider(currentUserId: String, onGoToRoom: (String, String) -> Unit) {
    var banner by remember { mutableStateOf<LuckyBagBannerData?>(null) }
    var visible by remember { mutableStateOf(false) }
    var seconds by remember { mutableIntStateOf(5) }

    DisposableEffect(currentUserId) {
        var socket: Socket? = null
        val listener = io.socket.emitter.Emitter.Listener { args ->
            val json = args.firstOrNull() as? JSONObject ?: return@Listener
            val data = LuckyBagBannerData(
                bagId = json.optString("bagId"),
                roomId = json.optString("roomId"),
                roomName = json.optString("roomName", "Room"),
                senderId = json.optString("senderId"),
                senderName = json.optString("senderName", "User"),
                senderImage = json.optString("senderImage"),
                amount = json.optLong("amount"),
                recipients = json.optInt("recipients", 5)
            )
            if (data.bagId.isNotBlank() && data.roomId.isNotBlank()) {
                banner = data
                seconds = 5
                visible = true
            }
        }
        runCatching {
            socket = IO.socket(LUCKY_BAG_SOCKET_URL)
            socket?.on("lucky_bag_banner", listener)
            socket?.connect()
        }
        onDispose {
            socket?.off("lucky_bag_banner", listener)
            socket?.disconnect()
            socket?.close()
        }
    }

    LaunchedEffect(banner?.bagId) {
        if (banner != null) {
            visible = true
            for (s in 5 downTo 1) {
                seconds = s
                delay(1000)
            }
            visible = false
            delay(350)
            banner = null
        }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        AnimatedVisibility(
            visible = visible && banner != null,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(),
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 2.dp)
                .padding(horizontal = 8.dp)
        ) {
            val item = banner
            if (item != null) {
                Box(
                    Modifier.fillMaxWidth().height(76.dp).clip(RoundedCornerShape(13.dp))
                ) {
                    AsyncImage(
                        model = LUCKY_BAG_BANNER_IMAGE,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Row(
                        Modifier.fillMaxSize().background(Color(0xA91B1730)).padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = item.senderImage.takeIf { it.isNotBlank() } ?: "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/default-avatar.png",
                            contentDescription = "Sender avatar",
                            modifier = Modifier.size(43.dp).clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF55466B)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    item.senderName,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Spacer(Modifier.width(5.dp))
                                Box(
                                    Modifier.size(23.dp).border(1.5.dp, Color(0xFFFFD36A), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(seconds.toString(), color = Color(0xFFFFE6A0), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Text(
                                "Sent ${item.amount} Coins Lucky Bag",
                                color = Color(0xFFFFE5A1),
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Clip,
                                modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                            )
                        }
                        Spacer(Modifier.width(7.dp))
                        Box(
                            Modifier.height(34.dp).width(56.dp).clip(RoundedCornerShape(50))
                                .background(Color(0xFFFFC84B))
                                .clickable {
                                    onGoToRoom(item.roomId, item.roomName)
                                    visible = false
                                    banner = null
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "GO",
                                color = Color(0xFF5B3210),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier
                            )
                        }
                    }
                    // Keep the hit target on top of the decorative banner.
                    Box(
                        Modifier.align(Alignment.CenterEnd).width(64.dp).fillMaxHeight()
                            .clip(RoundedCornerShape(topEnd = 13.dp, bottomEnd = 13.dp))
                            .background(Color.Transparent)
                    )
                }
            }
        }
    }
}

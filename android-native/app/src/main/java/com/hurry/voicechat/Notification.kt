package com.hurry.voicechat

import android.os.Handler
import android.os.Looper
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
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
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import kotlinx.coroutines.delay
import org.json.JSONObject

private const val ZYORA_NOTIFICATION_SOCKET_URL = "https://zyora-server.onrender.com"
private const val ZYORA_GIFT_NOTIFICATION_VIDEO = "gemini_generated_video_9da434d9~2.mp4"
private const val ZYORA_GAME_NOTIFICATION_VIDEO = "gemini_generated_video_87dc00f7~2.mp4"
private const val ZYORA_NOTIFICATION_PUBLIC = "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/"

data class RoomBroadcastNotice(
    val id: String,
    val video: String,
    val avatar: String,
    val name: String,
    val detail: String,
    val giftImage: String,
    val giftName: String
)

/** Single app-wide controller for Gift and Game broadcast notifications. */
@Composable
fun NotificationController() {
    val notices = remember { mutableStateListOf<RoomBroadcastNotice>() }
    val seenIds = remember { linkedSetOf<String>() }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    DisposableEffect(Unit) {
        var socket: Socket? = null

        fun addNotice(notice: RoomBroadcastNotice) {
            if (notice.id.isBlank()) return
            mainHandler.post {
                if (seenIds.add(notice.id)) {
                    if (seenIds.size > 1200) {
                        val iterator = seenIds.iterator()
                        repeat(400) { if (iterator.hasNext()) { iterator.next(); iterator.remove() } }
                    }
                    notices.add(notice)
                    while (notices.size > 20) notices.removeAt(0)
                }
            }
        }

        val giftListener = Emitter.Listener { args ->
            val payload = args.firstOrNull() as? JSONObject ?: return@Listener
            val rawImage = payload.optString("giftImage").trim()
            val giftImage = when {
                rawImage.isBlank() -> ""
                rawImage.startsWith("http", true) -> rawImage
                else -> ZYORA_NOTIFICATION_PUBLIC + rawImage.trimStart('/')
            }
            addNotice(
                RoomBroadcastNotice(
                    id = payload.optString("id"),
                    video = payload.optString("video").ifBlank { ZYORA_GIFT_NOTIFICATION_VIDEO },
                    avatar = payload.optString("senderImage").ifBlank { ZYORA_NOTIFICATION_PUBLIC + "default-avatar.png" },
                    name = payload.optString("senderName").ifBlank { "User" },
                    detail = "Sended to " + payload.optString("recipientName").ifBlank { "User" },
                    giftImage = giftImage,
                    giftName = payload.optString("giftName").ifBlank { "Gift" }
                )
            )
        }

        val gameListener = Emitter.Listener { args ->
            val payload = args.firstOrNull() as? JSONObject ?: return@Listener
            val id = payload.optString("id")
            val amount = payload.optLong("win", 0L)
            val game = payload.optString("game").lowercase()
            if (id.isBlank() || amount < 5_000_000L || (game != "wildparty" && game != "dragonbattle")) return@Listener
            val dragonBattle = game == "dragonbattle"
            addNotice(
                RoomBroadcastNotice(
                    id = id,
                    video = payload.optString("video").ifBlank { ZYORA_GAME_NOTIFICATION_VIDEO },
                    avatar = payload.optString("avatar").ifBlank { ZYORA_NOTIFICATION_PUBLIC + "default-avatar.png" },
                    name = payload.optString("name").ifBlank { "User" },
                    detail = "Win " + amount.toString() + " • " + if (dragonBattle) "Dragon Battle" else "Wild Party",
                    giftImage = ZYORA_NOTIFICATION_PUBLIC + if (dragonBattle) {
                        "IMG_20261121_020906.png"
                    } else {
                        "file_000000009d808211b8ffb7c2183b4ef5.png"
                    },
                    giftName = if (dragonBattle) "Dragon Battle" else "Wild Party"
                )
            )
        }

        runCatching {
            socket = IO.socket(ZYORA_NOTIFICATION_SOCKET_URL)
            socket?.on("zyora_luxury_gift_notification", giftListener)
            socket?.on("zyora_game_big_win_notification", gameListener)
            socket?.connect()
        }

        onDispose {
            socket?.off("zyora_luxury_gift_notification", giftListener)
            socket?.off("zyora_game_big_win_notification", gameListener)
            socket?.disconnect()
            socket?.close()
            mainHandler.removeCallbacksAndMessages(null)
        }
    }

    val activeNotice = notices.firstOrNull()
    var secondsLeft by remember(activeNotice?.id) { mutableIntStateOf(5) }
    LaunchedEffect(activeNotice?.id) {
        if (activeNotice == null) return@LaunchedEffect
        repeat(5) {
            delay(1000L)
            secondsLeft = (secondsLeft - 1).coerceAtLeast(0)
        }
        notices.remove(activeNotice)
    }

    Box(Modifier.fillMaxSize().zIndex(270f), contentAlignment = Alignment.TopCenter) {
        AnimatedVisibility(
            visible = activeNotice != null,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { -it }),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .statusBarsPadding()
                .padding(top = 6.dp)
        ) {
            activeNotice?.let { notice ->
                Box(Modifier.fillMaxWidth().height(124.dp)) {
                    key(notice.id) {
                        RoomGiftVideoOverlay(src = notice.video, onFinished = {})
                    }
                    Row(
                        Modifier.fillMaxSize()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0x22000000))
                            .padding(horizontal = 10.dp),
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
                            Text(notice.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("$secondsLeft", color = Color.White, fontSize = 11.sp)
                            Text(notice.detail, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
    }
}

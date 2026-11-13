package com.hurry.voicechat

import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.zIndex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
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

private const val LUCKY_BAG_SOCKET_URL = "https://zyora-server.onrender.com"
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
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { -it }),
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 2.dp)
        ) {
            val item = banner
            if (item != null) {
                // Modified Box: FillWidth ke liye default height hataya aur matchParentSize ka use kiya
                Box(Modifier.fillMaxWidth().defaultMinSize(minHeight = 76.dp)) {
                    AsyncImage(
                        model = LUCKY_BAG_BANNER_IMAGE,
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth(),
                        // Edge-to-edge fix: no crop, no stretch
                        contentScale = ContentScale.FillWidth 
                    )
                    Row(
                        Modifier.matchParentSize().padding(start = 8.dp, end = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = item.senderImage.takeIf { it.isNotBlank() } ?: "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/default-avatar.png",
                            contentDescription = "Sender avatar",
                            modifier = Modifier.size(43.dp).clip(RoundedCornerShape(8.dp)),
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
                                Box(Modifier.size(23.dp), contentAlignment = Alignment.Center) {
                                    Text(seconds.toString(), color = Color(0xFFFFE6A0), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Text(
                                "Sent ${item.amount} Coins Lucky Bag · ${item.roomName}",
                                color = Color(0xFFFFE5A1),
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Clip,
                                modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                            )
                        }
                        Spacer(Modifier.width(5.dp))
                        Box(
                            Modifier
                                .offset(x = 5.dp)
                                .padding(end = 2.dp)
                                .size(width = 54.dp, height = 32.dp)
                                // GO button 3D Golden Color bina border ke
                                .background(Brush.verticalGradient(listOf(Color(0xFFFFF2A2), Color(0xFFFFD28A), Color(0xFFC78426))), RoundedCornerShape(50))
                                .clickable {
                                    onGoToRoom(item.roomId, item.roomName)
                                    visible = false
                                    banner = null
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "GO",
                                color = Color(0xFF80502F),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }
    }
}


/** Owns the RoomPage Lucky Bag Open and Confirm overlays and their API actions. */
@Composable
fun LuckyBagOpenConfirmCards(
    showLuckyBagCenterReveal: Boolean,
    showLuckyBagClaimedCard: Boolean,
    luckyBagSenderName: String,
    luckyBagSenderImage: String,
    luckyBagId: String,
    luckyBagClaiming: Boolean,
    luckyBagClaimError: String?,
    luckyBagClaimAmount: Long,
    canonicalWalletUserId: String,
    roomWalletBalance: Long,
    showFloatingLuckyBag: Boolean,
    onShowLuckyBagCenterRevealChange: (Boolean) -> Unit,
    onShowLuckyBagClaimedCardChange: (Boolean) -> Unit,
    onLuckyBagClaimingChange: (Boolean) -> Unit,
    onLuckyBagClaimErrorChange: (String?) -> Unit,
    onLuckyBagClaimAmountChange: (Long) -> Unit,
    onShowFloatingLuckyBagChange: (Boolean) -> Unit,
    onRoomWalletBalanceChange: (Long) -> Unit
) {
        if (showLuckyBagCenterReveal) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable { onShowLuckyBagCenterRevealChange(false); onShowFloatingLuckyBagChange(true) },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(0.9f),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                        AsyncImage(
                            model = "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/IMG_20261021_170648.png",
                            contentDescription = "Lucky Bag Claim Reveal",
                            modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp).clickable(enabled = false) {},
                            contentScale = ContentScale.Fit
                        )
                        Column(
                            modifier = Modifier.align(Alignment.TopCenter).padding(top = 34.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            AsyncImage(
                                model = luckyBagSenderImage.ifBlank { "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/default-avatar.png" },
                                contentDescription = "Lucky Bag sender avatar",
                                modifier = Modifier.size(58.dp).clip(CircleShape).border(2.dp, Color(0xFFFFE4A0), CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Text(
                                text = "Lucky Bag from ${luckyBagSenderName.ifBlank { "User" }}",
                                color = Color(0xFF8B3B26), fontSize = 14.sp, fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center, modifier = Modifier.padding(top = 5.dp)
                            )
                            luckyBagClaimError?.let {
                                Text(it, color = Color(0xFFB91C1C), fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp).padding(horizontal = 12.dp))
                            }
                        }
                    }
                    
                    Box(
                        modifier = Modifier
                             // Open Button 15dp aur upar shift kar diya (-17.dp total)
                             .offset(y = (-17).dp)
                            .width(190.dp)
                            .height(72.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Brush.verticalGradient(listOf(Color(0xFFFFF2A2), Color(0xFFFFE28A), Color(0xFFF4C46B), Color(0xFFFFF0A0))))
                            .border(3.dp, Color(0xFFFFEDB0), RoundedCornerShape(50))
                            .padding(6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Brush.verticalGradient(listOf(Color(0xFFFFE7A0), Color(0xFFF5C66E), Color(0xFFFFEDB0))))
                            .border(3.dp, Color(0xFFF5D27D), RoundedCornerShape(50))
                            .padding(6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Brush.verticalGradient(listOf(Color(0xFFFFF0C9), Color(0xFFFFDDA0), Color(0xFFFFCC85))))
                            .border(3.dp, Color(0xFFFFF0C4), RoundedCornerShape(50))
                            .clickable(enabled = !luckyBagClaiming) {
                                if (luckyBagId.isBlank()) {
                                    onLuckyBagClaimErrorChange("Lucky Bag is unavailable")
                                } else {
                                    onLuckyBagClaimingChange(true)
                                    onLuckyBagClaimErrorChange(null)
                                    CoroutineScope(Dispatchers.IO).launch {
                                        val result = runCatching {
                                            val payload = JSONObject().put("bagId", luckyBagId).put("userId", canonicalWalletUserId)
                                            val connection = (java.net.URL("https://zyora-server.onrender.com/api/lucky-bag/claim").openConnection() as java.net.HttpURLConnection).apply {
                                                requestMethod = "POST"
                                                connectTimeout = 12000
                                                readTimeout = 15000
                                                doOutput = true
                                                setRequestProperty("Content-Type", "application/json")
                                            }
                                            connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
                                            val status = connection.responseCode
                                            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                                            val body = stream.bufferedReader().use { it.readText() }
                                            connection.disconnect()
                                            val json = JSONObject(body)
                                            if (status !in 200..299 || !json.optBoolean("success")) throw IllegalStateException(json.optString("error", "Claim failed"))
                                            json
                                        }
                                        withContext(Dispatchers.Main) {
                                            onLuckyBagClaimingChange(false)
                                            result.onSuccess { json ->
                                                onLuckyBagClaimAmountChange(json.optLong("amount"))
                                                onShowLuckyBagCenterRevealChange(false)
                                                onShowLuckyBagClaimedCardChange(true)
                                                onShowFloatingLuckyBagChange(false)
                                            }.onFailure { err -> onLuckyBagClaimErrorChange(err.message ?: "Claim failed") }
                                        }
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (luckyBagClaiming) "Open" else "Open",
                            color = Color(0xFFDF182B),
                            fontSize = 23.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
        
        if (showLuckyBagClaimedCard) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=0.6f)).clickable { }, contentAlignment = Alignment.Center) {
                // Confirm Card parent box: Ekdum niche se exactly 15dp curve fix kar diya hai clip lagake
                Box(Modifier.fillMaxWidth(0.85f).aspectRatio(0.59f).clip(RoundedCornerShape(topStart=32.dp, topEnd=32.dp, bottomStart=15.dp, bottomEnd=15.dp)).clickable(enabled=false){}) {
                    
                    Box(Modifier.fillMaxSize().padding(top = 40.dp).clip(RoundedCornerShape(topStart=32.dp, topEnd=32.dp, bottomStart=15.dp, bottomEnd=15.dp)).background(Brush.linearGradient(listOf(Color(0xFFFF6449), Color(0xFFFF263F), Color(0xFFE90037)))))
                    
                    Column(
                        modifier = Modifier.align(Alignment.TopCenter).zIndex(2f).offset(y = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AsyncImage(
                            model = luckyBagSenderImage.ifBlank { "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/default-avatar.png" },
                            contentDescription = "Lucky Bag sender avatar",
                            modifier = Modifier.size(62.dp).clip(CircleShape).border(3.dp, Color(0xFFFFD875), CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Text(
                            text = "Lucky Bag from ${luckyBagSenderName.ifBlank { "User" }}",
                            color = Color(0xFF8B3B26), fontSize = 14.sp, fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center, modifier = Modifier.padding(top = 5.dp)
                        )
                    }

                    Box(Modifier.align(Alignment.TopCenter).fillMaxWidth(0.9f).fillMaxHeight(0.72f).clip(RoundedCornerShape(24.dp)).background(Brush.verticalGradient(listOf(Color(0xFFFFF7E5), Color(0xFFFFF0D4))))) {
                        Column(Modifier.align(Alignment.BottomCenter).padding(bottom = 66.dp).fillMaxWidth()) {
                            Box(Modifier.fillMaxWidth().height(2.dp).padding(horizontal = 24.dp).background(Color(0xFFF5EBD8)))
                            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 18.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Reward: ${luckyBagClaimAmount} Coins", color = Color(0xFFD29A72), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Details", color = Color(0xFFD29A72), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Icon(Icons.Default.ChevronRight, null, tint = Color(0xFFD29A72), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    Canvas(Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(0.35f)) {
                        val w = size.width
                        val h = size.height
                        
                        val goldBrush = Brush.verticalGradient(listOf(Color(0xFFFFF8C9), Color(0xFFFFD76D), Color(0xFFFFAD36), Color(0xFFFFF0A9)))
                        val goldPath = Path().apply {
                            moveTo(0f, h * 0.15f)
                            quadraticBezierTo(w / 2f, -h * 0.05f, w, h * 0.15f)
                            lineTo(w, h * 0.28f)
                            quadraticBezierTo(w / 2f, h * 0.08f, 0f, h * 0.28f)
                            close()
                        }
                        drawPath(goldPath, goldBrush)
                        
                        val redBrush = Brush.verticalGradient(listOf(Color(0xFFFF6449), Color(0xFFE90037)))
                        val redPath = Path().apply {
                            moveTo(0f, h * 0.26f)
                            quadraticBezierTo(w / 2f, h * 0.06f, w, h * 0.26f)
                            lineTo(w, h)
                            lineTo(0f, h)
                            close()
                        }
                        drawPath(redPath, redBrush)
                    }

                    luckyBagClaimError?.let { error ->
                        Text(
                            error,
                            color = Color(0xFF8B1E1E),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 88.dp, start = 16.dp, end = 16.dp)
                        )
                    }
                    Box(
                        Modifier.align(Alignment.BottomCenter)
                            .padding(bottom = 24.dp)
                            .fillMaxWidth(0.85f)
                            .height(62.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFFFF304D))
                            .border(3.dp, Color(0xFFFFDC73), RoundedCornerShape(50))
                            .padding(5.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Brush.verticalGradient(listOf(Color(0xFFFFF2B9), Color(0xFFFFD28A), Color(0xFFFFAC45), Color(0xFFFFE8A0))))
                            .border(3.dp, Color(0xFFFFF0AD), RoundedCornerShape(50))
                            .clickable(enabled = !luckyBagClaiming) {
                                if (luckyBagId.isBlank() || canonicalWalletUserId.isBlank()) {
                                    onLuckyBagClaimErrorChange("Lucky Bag confirmation unavailable")
                                } else {
                                    onLuckyBagClaimingChange(true)
                                    onLuckyBagClaimErrorChange(null)
                                    CoroutineScope(Dispatchers.IO).launch {
                                        val result = runCatching {
                                            val payload = JSONObject()
                                                .put("bagId", luckyBagId)
                                                .put("userId", canonicalWalletUserId)
                                            val connection = (java.net.URL("https://zyora-server.onrender.com/api/lucky-bag/confirm").openConnection() as java.net.HttpURLConnection).apply {
                                                requestMethod = "POST"
                                                connectTimeout = 12000
                                                readTimeout = 15000
                                                doOutput = true
                                                setRequestProperty("Content-Type", "application/json")
                                            }
                                            connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
                                            val status = connection.responseCode
                                            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                                            val body = stream.bufferedReader().use { it.readText() }
                                            connection.disconnect()
                                            val json = JSONObject(body)
                                            if (status !in 200..299 || !json.optBoolean("success")) {
                                                throw IllegalStateException(json.optString("error", "Confirmation failed"))
                                            }
                                            json
                                        }
                                        withContext(Dispatchers.Main) {
                                            onLuckyBagClaimingChange(false)
                                            result.onSuccess { json ->
                                                onRoomWalletBalanceChange(json.optLong("balance", roomWalletBalance).coerceAtLeast(0L))
                                                onShowLuckyBagClaimedCardChange(false)
                                                onLuckyBagClaimAmountChange(0L)
                                            }.onFailure { err ->
                                                onLuckyBagClaimErrorChange(err.message ?: "Confirmation failed")
                                            }
                                        }
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (luckyBagClaiming) "Confirming..." else "Confirm",
                            color = Color(0xFF9A4F1C),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }

}

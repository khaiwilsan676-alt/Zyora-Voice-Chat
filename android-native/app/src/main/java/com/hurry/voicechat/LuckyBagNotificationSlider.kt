package com.hurry.voicechat

import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithContent
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
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
private const val LUCKY_BAG_CARD_PNG = "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/IMG_20261028_182446.png"
private const val LUCKY_BAG_COIN_PNG = "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/file_00000000e56882119c217d508b6733dc.png"

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
@Composable
fun LuckyBagNotificationSlider(currentUserId: String, onGoToRoom: (String, String) -> Unit) {
    var banner by remember { mutableStateOf<LuckyBagBannerData?>(null) }
    var visible by remember { mutableStateOf(false) }
    var countdownSeconds by remember { mutableIntStateOf(4) }

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
            countdownSeconds = 4
            for (sec in 4 downTo 1) {
                countdownSeconds = sec
                delay(1000)
            }
            delay(1000)
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
                .fillMaxWidth(0.94f)
                .statusBarsPadding()
                .padding(top = 6.dp)
        ) {
            val item = banner
            if (item != null) {
                // Parent Box (Patli si patti)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    // 1. CUSTOM PATTI BACKGROUND WITH LEFT-RIGHT FADE LAYER
                    Canvas(
                        modifier = Modifier
                            .matchParentSize()
                            .graphicsLayer { alpha = 0.99f }
                            .drawWithContent {
                                drawContent()
                                drawRect(
                                    brush = Brush.horizontalGradient(
                                        0.0f to Color.Transparent,
                                        0.16f to Color.Black,
                                        0.84f to Color.Black,
                                        1.0f to Color.Transparent
                                    ),
                                    blendMode = BlendMode.DstIn
                                )
                            }
                    ) {
                        val w = size.width
                        val h = size.height

                        // Main Coral-Red Gradient Patti
                        drawRect(
                            brush = Brush.horizontalGradient(
                                0.0f to Color(0xFFB72B3E),
                                0.25f to Color(0xFFE03F4E),
                                0.55f to Color(0xFFE55052),
                                0.80f to Color(0xFFD94A4B),
                                1.0f to Color(0xFFB13040)
                            )
                        )

                        // Soft Warm Inner Glow
                        drawRect(
                            brush = Brush.verticalGradient(
                                0.0f to Color(0x28FFF0D4),
                                0.45f to Color.Transparent,
                                1.0f to Color(0x22FF9E7A)
                            )
                        )

                        // Top & Bottom Thin Golden-Peach Border Lines
                        val borderBrush = Brush.horizontalGradient(
                            0.0f to Color.Transparent,
                            0.20f to Color(0xD9FFE1C6),
                            0.50f to Color(0xFFFFF1E0),
                            0.80f to Color(0xD9FFE1C6),
                            1.0f to Color.Transparent
                        )
                        drawLine(
                            brush = borderBrush,
                            start = Offset(0f, 1.dp.toPx()),
                            end = Offset(w, 1.dp.toPx()),
                            strokeWidth = 1.4.dp.toPx()
                        )
                        drawLine(
                            brush = borderBrush,
                            start = Offset(0f, h - 1.dp.toPx()),
                            end = Offset(w, h - 1.dp.toPx()),
                            strokeWidth = 1.4.dp.toPx()
                        )

                        // Subtle Sparkle Dots (Image jaisa glow feel)
                        val sparkles = listOf(
                            Offset(w * 0.26f, h * 0.92f) to 2.2.dp.toPx(),
                            Offset(w * 0.62f, h * 0.35f) to 1.6.dp.toPx(),
                            Offset(w * 0.80f, h * 0.86f) to 1.8.dp.toPx(),
                            Offset(w * 0.90f, h * 0.22f) to 2.0.dp.toPx()
                        )
                        sparkles.forEach { (center, radius) ->
                            drawCircle(
                                color = Color(0xCCFFF8E7),
                                radius = radius,
                                center = center
                            )
                        }
                    }

                    // 2. CONTENT LAYER (Bina fade ke ekdum crisp & clear)
                    Row(
                        modifier = Modifier
                            .matchParentSize()
                            .padding(start = 18.dp, end = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // User Avatar
                        AsyncImage(
                            model = item.senderImage.takeIf { it.isNotBlank() }
                                ?: "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/default-avatar.png",
                            contentDescription = "Sender avatar",
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, Color(0xFFFFE5BE), CircleShape),
                            contentScale = ContentScale.Crop
                        )

                        Spacer(Modifier.width(8.dp))

                        // Name + 4s Theme Countdown Circle & Short Text
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.senderName,
                                    color = Color(0xFFFFD86B),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    lineHeight = 14.sp,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Spacer(Modifier.width(5.dp))
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFB72B3E))
                                        .border(1.dp, Color(0xFFFFD86B), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = countdownSeconds.toString(),
                                        color = Color(0xFFFFF2EE),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        lineHeight = 9.sp
                                    )
                                }
                            }
                            Text(
                                text = "sent you a lucky bag",
                                color = Color(0xFFFFF2EE),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 13.sp
                            )
                        }

                        Spacer(Modifier.width(4.dp))

                        // Thodi badi Lucky Bag Card PNG (Open button se pehle)
                        AsyncImage(
                            model = LUCKY_BAG_CARD_PNG,
                            contentDescription = "Lucky Bag Card",
                            modifier = Modifier.requiredSize(51.dp),
                            contentScale = ContentScale.Fit
                        )

                        Spacer(Modifier.width(6.dp))

                        // Open Button (Exact image jaisa pill button)
                        Box(
                            modifier = Modifier
                                .size(width = 56.dp, height = 27.dp)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color(0xFFF9DC82),
                                            Color(0xFFEEC060)
                                        )
                                    )
                                )
                                .clickable {
                                    onGoToRoom(item.roomId, item.roomName)
                                    visible = false
                                    banner = null
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "open",
                                color = Color(0xFFC8281A),
                                fontSize = 12.sp,
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
    claimantName: String,
    claimantImage: String,
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
                             .offset(y = (-30).dp)
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
                                                .put("userName", claimantName).put("userImage", claimantImage)
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
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable { },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(0.85f)
                        .aspectRatio(0.59f)
                        .clickable(enabled = false) {}
                ) {
                    // Red/Coral Outer Back Frame
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(top = 34.dp)
                            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 24.dp, bottomEnd = 24.dp))
                            .border(2.dp, Color(0xFFFFD88A), RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 24.dp, bottomEnd = 24.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFFFF6449), Color(0xFFFF263F), Color(0xFFE90037))))
                    )

                    // Inner Cream/Ivory Card (Exact screenshot style)
                    Box(
                        Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 34.dp)
                            .fillMaxWidth(0.93f)
                            .fillMaxHeight(0.75f)
                            .clip(RoundedCornerShape(22.dp))
                            .background(Brush.verticalGradient(listOf(Color(0xFFFFFAF0), Color(0xFFFFF3DF))))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 42.dp, start = 16.dp, end = 16.dp, bottom = 36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Lucky Packet from ${luckyBagSenderName.ifBlank { "User" }}",
                                color = Color(0xFFC59362),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(Modifier.height(14.dp))

                            // Big Amount + Coin Image
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "%,d".format(luckyBagClaimAmount),
                                    color = Color(0xFFE74A5A),
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.width(6.dp))
                                AsyncImage(
                                    model = LUCKY_BAG_COIN_PNG,
                                    contentDescription = "Coin",
                                    modifier = Modifier.size(30.dp),
                                    contentScale = ContentScale.Fit
                                )
                            }

                            Spacer(Modifier.height(6.dp))

                            Text(
                                text = "Gold Coins have put into your wallet",
                                color = Color(0xFFC59362),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal,
                                textAlign = TextAlign.Center
                            )

                            Spacer(Modifier.height(16.dp))

                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(Color(0xFFF2E5D2))
                            )

                            Spacer(Modifier.height(10.dp))

                            // Received / Details Header Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Received",
                                    color = Color(0xFFC59362),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Details",
                                        color = Color(0xFFC59362),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Icon(
                                        Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = Color(0xFFC59362),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            // Claimant Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = claimantImage.ifBlank { "https://zyora-server.onrender.com/default-avatar.png" },
                                    contentDescription = "Your avatar",
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                                Text(
                                    text = claimantName.ifBlank { "User" },
                                    color = Color(0xFF7B5738),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier
                                        .padding(start = 10.dp, end = 8.dp)
                                        .weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "%,d".format(luckyBagClaimAmount),
                                        color = Color(0xFFE55B65),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    AsyncImage(
                                        model = LUCKY_BAG_COIN_PNG,
                                        contentDescription = "Coin",
                                        modifier = Modifier.size(16.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            }
                        }
                    }

                    // Top Overlapping Sender Avatar
                    AsyncImage(
                        model = luckyBagSenderImage.ifBlank { "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/default-avatar.png" },
                        contentDescription = "Lucky Bag sender avatar",
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .zIndex(3f)
                            .size(68.dp)
                            .clip(CircleShape)
                            .border(3.dp, Color(0xFFFFE5A5), CircleShape),
                        contentScale = ContentScale.Crop
                    )

                    // Bottom Curved Red & Gold Pocket
                    Canvas(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .fillMaxHeight(0.32f)
                            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                    ) {
                        val w = size.width
                        val h = size.height

                        val goldBrush = Brush.verticalGradient(listOf(Color(0xFFFFF8C9), Color(0xFFFFD76D), Color(0xFFFFAD36), Color(0xFFFFF0A9)))
                        val goldPath = Path().apply {
                            moveTo(0f, h * 0.06f)
                            quadraticBezierTo(w / 2f, h * 0.28f, w, h * 0.06f)
                            lineTo(w, h * 0.17f)
                            quadraticBezierTo(w / 2f, h * 0.39f, 0f, h * 0.17f)
                            close()
                        }
                        drawPath(goldPath, goldBrush)

                        val redBrush = Brush.verticalGradient(listOf(Color(0xFFFF5A4E), Color(0xFFE91B3A)))
                        val redPath = Path().apply {
                            moveTo(0f, h * 0.15f)
                            quadraticBezierTo(w / 2f, h * 0.37f, w, h * 0.15f)
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
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 92.dp, start = 16.dp, end = 16.dp)
                        )
                    }

                    // Bottom Golden Confirm Button
                    Box(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 22.dp)
                            .fillMaxWidth(0.84f)
                            .height(60.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFFFF304D))
                            .border(3.dp, Color(0xFFFFDC73), RoundedCornerShape(50))
                            .padding(5.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Brush.verticalGradient(listOf(Color(0xFFFFF2B9), Color(0xFFFFD28A), Color(0xFFFFAC45), Color(0xFFFFE8A0))))
                            .border(3.dp, Color(0xFFFFF0AD), RoundedCornerShape(50))
                            .clickable {
                                if (luckyBagId.isBlank() || canonicalWalletUserId.isBlank()) {
                                    onLuckyBagClaimErrorChange("Lucky Bag confirmation unavailable")
                                } else {
                                    onShowLuckyBagClaimedCardChange(false)
                                    onShowFloatingLuckyBagChange(true)
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
                                            result.onSuccess { json ->
                                                onRoomWalletBalanceChange(json.optLong("balance", roomWalletBalance).coerceAtLeast(0L))
                                            }
                                        }
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Confirm",
                            color = Color(0xFF9A4F1C),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
}

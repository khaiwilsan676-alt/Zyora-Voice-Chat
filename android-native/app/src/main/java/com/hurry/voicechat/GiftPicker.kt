package com.hurry.voicechat

import android.net.Uri
import android.widget.VideoView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.roundToInt

// ============================================================
// DATA MODELS
// ============================================================
data class GiftPickerGift(
    val id: Int,
    val name: String,
    val coins: Int,
    val image: String,
    val video: String? = null,
    val videoStyle: String? = null,
    val noMask: Boolean = false,
    val sideFade: Boolean = false,
)

data class GiftPickerSeat(
    val number: Int,
    val isOccupied: Boolean,
    val user: GiftPickerUser? = null,
)

data class GiftPickerUser(
    val name: String,
    val image: String,
    val accountId: String,
)

data class GiftPickerRoomUser(
    val accountId: String,
    val name: String,
    val image: String,
)

enum class RecipientMode { MIC, ROOM, SINGLE }

// ============================================================
// GIFTS
// ============================================================
private val hotGifts = listOf(
    GiftPickerGift(1, "Teddy", 70000, "/IMG_20260922_142150.jpg",
        video = "/VID_20260921_011932.mp4", videoStyle = "fade"),
    GiftPickerGift(2, "Autumn's Embrace", 54900, "/IMG_20260922_182259.png",
        video = "/gemini_generated_video_89e836bd~2.mp4", videoStyle = "pure", noMask = true),
    GiftPickerGift(3, "Arab King", 500000, "/image_d9df9625~2.jpg",
        video = "/gemini_generated_video_17e19680~2.mp4", videoStyle = "fade", sideFade = true),
)

private val luckyGifts = listOf(
    GiftPickerGift(101, "Tiara", 3000, "/IMG_20260927_213855.png", noMask = true),
    GiftPickerGift(102, "Lucky Clover", 1499, "/IMG_20260927_213917.png", noMask = true),
    GiftPickerGift(103, "Hi", 999, "/IMG_20260927_213946.png", noMask = true),
    GiftPickerGift(104, "Rose", 3999, "/IMG_20260927_214121.png", noMask = true),
    GiftPickerGift(105, "Kiss", 1600, "/IMG_20260927_214139.png", noMask = true),
    GiftPickerGift(106, "Balloon", 4000, "/IMG_20260927_214220.png", noMask = true),
    GiftPickerGift(107, "Dragon", 7000, "/IMG_20260927_221521.png", noMask = true),
    GiftPickerGift(108, "Nine Hands", 10999, "/IMG_20260927_221544.png", noMask = true),
    GiftPickerGift(109, "Coffin", 8999, "/IMG_20260927_221559.png", noMask = true),
    GiftPickerGift(110, "Sword", 9999, "/IMG_20260927_221615.png", noMask = true),
    GiftPickerGift(111, "Love lock", 5000, "/IMG_20260927_221637.png", noMask = true),
    GiftPickerGift(112, "Lantern", 6999, "/IMG_20260927_221654.png", noMask = true),
    GiftPickerGift(113, "Ring", 5999, "/IMG_20260927_221707.png", noMask = true),
    GiftPickerGift(114, "Dancing Girl", 12000, "/IMG_20260927_221722.png", noMask = true),
    GiftPickerGift(115, "Whale", 7899, "/IMG_20260927_221742.png", noMask = true),
    GiftPickerGift(116, "Star", 9800, "/file_0000000066f482118f772ed6fab4ad1f.png", noMask = true),
    GiftPickerGift(117, "Fire Bird", 13000, "/file_00000000fe088211b7be0110e2d3f878.png", noMask = true),
)

// Resolve web-app public assets to absolute URLs so Coil can load gift artwork in native Android.
private fun giftAssetUrl(path: String): String {
    if (path.startsWith("https://") || path.startsWith("http://") || path.startsWith("file:") || path.startsWith("content:")) return path
    return "https://zyora-voice-chat.vercel.app/" + path.trimStart('/')
}

private val tabs = listOf("Hot", "Lucky", "Luxury", "Event")
private val multipliers = listOf("1×", "10×", "299×", "599×", "999×")

// Blue palette
private val Blue500 = Color(0xFF3B82F6)
private val Blue600 = Color(0xFF2563EB)

fun rollLuckyWin(): Int {
    val luck = Math.random()
    if (luck > 0.999) return 30
    if (luck > 0.980) return 15
    if (luck > 0.966) return 10
    if (luck > 0.900) return 5
    if (luck > 0.910) return 2
    return 0
}

// ============================================================
// MAIN COMPOSABLE
// ============================================================
@Composable
fun GiftPicker(
    open: Boolean = true,
    onClose: () -> Unit = {},
    seats: List<NativeRoomSeat> = emptyList(),
    onSend: (Int) -> Unit = {},
    roomId: String = "",
    currentUserAccountId: String = "",
    currentUserName: String = "User",
    currentUserImage: String = "/default-avatar.png",
    roomUsers: List<ActiveUserNative> = emptyList(),
    walletBalance: Long = 0L,
    onDebitWallet: suspend (Long) -> Long? = { null },
    onCreditDiamonds: suspend (Int) -> Unit = {},
    onSendCoinTransfer: (Map<String, Any?>) -> Unit = {},
    onSendGiftVideoPlay: (Map<String, Any?>) -> Unit = {},
    onRecordGiftTransaction: suspend (String, Long) -> Unit = { _, _ -> },
    onRecordDiamondTransaction: suspend (String, Int) -> Unit = { _, _ -> },
    walletContent: @Composable () -> Unit = {},
) {
    var activeTab by remember { mutableStateOf("Hot") }
    var selectedMultiplier by remember { mutableStateOf("1×") }
    var showMultipliers by remember { mutableStateOf(false) }
    var selectedGift by remember { mutableStateOf<Int?>(null) }
    var sending by remember { mutableStateOf(false) }
    var isWalletOpen by remember { mutableStateOf(false) }

    var showInsufficient by remember { mutableStateOf(false) }
    var insufficientText by remember { mutableStateOf("Insufficient Balance") }

    var selectedTargets by remember { mutableStateOf<List<String>>(emptyList()) }
    var recipientMode by remember { mutableStateOf(RecipientMode.MIC) }

    var playingVideo by remember { mutableStateOf<Pair<String, String>?>(null) }

    val scope = rememberCoroutineScope()

    val currentGifts = when (activeTab) {
        "Lucky" -> luckyGifts
        "Hot" -> hotGifts
        else -> emptyList()
    }

    val parseMultiplier: (String) -> Int = { m -> m.replace("×", "").toIntOrNull() ?: 1 }
    val selectedGiftObj = currentGifts.find { it.id == selectedGift }
    val totalCost = selectedGiftObj?.let { it.coins * parseMultiplier(selectedMultiplier) } ?: 0

    val recipientIds = remember(selectedTargets, roomUsers, seats) {
        when {
            selectedTargets.isNotEmpty() -> selectedTargets
            roomUsers.isNotEmpty() -> roomUsers.map { it.accountId }
            else -> seats.filter { it.occupied && it.accountId.isNotBlank() }.map { it.accountId }
        }
    }
    val recipientCount = recipientIds.size
    val recipientUsers = recipientIds.mapNotNull { id ->
        roomUsers.find { it.accountId == id }?.let {
            GiftPickerRoomUser(it.accountId, it.name, it.image)
        } ?: seats.find { it.accountId == id }?.let {
            GiftPickerRoomUser(it.accountId, it.userName, it.userImage)
        }
    }
    val firstRecipient = recipientUsers.firstOrNull()
    val totalSendCost = totalCost.toLong() * max(1, recipientCount)
    val canAfford = totalCost > 0 && recipientCount > 0 && totalSendCost <= walletBalance
    val isLuckyGiftTab = activeTab == "Lucky"

    fun triggerInsufficient(message: String = "Insufficient Balance", openWalletAfter: Boolean = true) {
        insufficientText = message
        showInsufficient = true
        scope.launch {
            delay(2000)
            showInsufficient = false
            if (openWalletAfter) { delay(80); isWalletOpen = true }
        }
    }

    fun handleSend() {
        if (isWalletOpen) return
        if (selectedGiftObj == null || sending) return
        if (recipientCount == 0) { triggerInsufficient("Select Recipient", false); return }
        if (totalSendCost > walletBalance) { triggerInsufficient("Insufficient Balance", true); return }
        if (!canAfford) return

        sending = true
        scope.launch {
            var luckyReturnAmount = 0L
            var winTimes = 0
            var netDeductionCost = totalSendCost

            if (isLuckyGiftTab) {
                winTimes = rollLuckyWin()
                luckyReturnAmount = if (winTimes > 0) totalSendCost * winTimes else 0L
                netDeductionCost = totalSendCost - luckyReturnAmount
            }

            val updatedBalance = onDebitWallet(netDeductionCost)
            if (updatedBalance == null) {
                sending = false
                triggerInsufficient("Insufficient Balance", true)
                return@launch
            }
            onRecordGiftTransaction(selectedGiftObj.name, -netDeductionCost)

            if (roomId.isNotEmpty() && currentUserAccountId.isNotEmpty() && recipientIds.isNotEmpty()) {
                val uniqueTransferId = "tx-$currentUserAccountId-${System.currentTimeMillis()}-${(0..999999).random()}"

                onSendCoinTransfer(mapOf(
                    "roomId" to roomId,
                    "senderId" to currentUserAccountId,
                    "recipientIds" to recipientIds,
                    "amount" to totalCost,
                    "giftName" to selectedGiftObj.name,
                    "giftImage" to selectedGiftObj.image,
                    "giftType" to activeTab,
                    "senderName" to currentUserName,
                    "senderImage" to currentUserImage,
                    "recipientName" to (firstRecipient?.name ?: "User"),
                    "recipientImage" to (firstRecipient?.image ?: "/default-avatar.png"),
                    "multiplier" to parseMultiplier(selectedMultiplier),
                    "luckyGift" to isLuckyGiftTab,
                    "luckyReturnAmount" to luckyReturnAmount,
                    "luckyReturnPercent" to winTimes,
                    "luckyImage" to selectedGiftObj.image,
                    "winTimes" to winTimes,
                    "transferId" to uniqueTransferId,
                    "eventId" to uniqueTransferId,
                    "timestamp" to System.currentTimeMillis(),
                    "recipientMode" to recipientMode.name.lowercase(),
                ))

                if (recipientIds.contains(currentUserAccountId) && isLuckyGiftTab) {
                    val diamondCredit = (totalCost * 0.1).toInt()
                    if (diamondCredit > 0) {
                        onCreditDiamonds(diamondCredit)
                        onRecordDiamondTransaction(
                            "Diamonds received — ${selectedGiftObj.name}", diamondCredit
                        )
                    }
                }

                if (selectedGiftObj.video != null && !isLuckyGiftTab) {
                    onSendGiftVideoPlay(mapOf(
                        "roomId" to roomId,
                        "senderId" to currentUserAccountId,
                        "giftName" to selectedGiftObj.name,
                        "video" to selectedGiftObj.video,
                        "videoStyle" to (selectedGiftObj.videoStyle ?: "fade"),
                        "timestamp" to System.currentTimeMillis(),
                    ))
                }
            }

            onSend(totalCost)

            if (isLuckyGiftTab) {
                sending = false
                onClose()
                return@launch
            }

            if (selectedGiftObj.video != null) {
                playingVideo = selectedGiftObj.video to (selectedGiftObj.videoStyle ?: "fade")
            } else {
                sending = false
                onClose()
            }
        }
    }

    // ============================================================
    // VIDEO OVERLAY
    // ============================================================
    playingVideo?.let { (src, style) ->
        VideoEffectOverlay(
            src = src,
            style = style,
            onFinished = {
                playingVideo = null
                sending = false
                onClose()
            },
            onStarted = { sending = false },
        )
        return
    }

    // ============================================================
    // SHEET UI
    // ============================================================
    if (open) {
        Dialog(
            onDismissRequest = onClose,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
            ),
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                BoxWithConstraints(
                    Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.46f)
                        .background(Color(0xEB000000)) // rgba(0,0,0,0.92)
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)) // ⬅️ 6.dp curved
                        .border(
                            1.dp,
                            Color.White.copy(alpha = 0.10f),
                            RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp),
                        )
                        .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
                ) {
                    Column(Modifier.fillMaxSize()) {

                        // ── MIC / HOUSE buttons (absolute top-right) ──
                        Box(Modifier.fillMaxWidth()) {
                            Row(
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(y = (-8).dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                RecipientButton(
                                    isActive = recipientMode == RecipientMode.MIC,
                                    onClick = {
                                        selectedTargets = seats.filter { it.occupied && it.accountId.isNotBlank() }
                                            .map { it.accountId }
                                        recipientMode = RecipientMode.MIC
                                    },
                                    iconType = RecipientIconType.MIC,
                                )
                                RecipientButton(
                                    isActive = recipientMode == RecipientMode.ROOM,
                                    onClick = {
                                        selectedTargets = emptyList()
                                        recipientMode = RecipientMode.ROOM
                                    },
                                    iconType = RecipientIconType.HOUSE,
                                )
                            }
                        }

                        // ── Seat avatar row ──
                        LazyRow(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp, bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(seats.filter { it.occupied && it.accountId.isNotBlank() },
                                key = { it.accountId }) { seat ->
                                val uid = seat.accountId
                                val selected = selectedTargets.contains(uid)
                                AsyncImage(
                                    model = seat.userImage,
                                    contentDescription = seat.userName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .border(
                                            width = 2.5.dp,
                                            color = if (selected) Blue500 else Color.Transparent,
                                            shape = CircleShape,
                                        )
                                        .clickable {
                                            selectedTargets = if (selected)
                                                selectedTargets.filter { it != uid }
                                            else selectedTargets + uid
                                            recipientMode = RecipientMode.SINGLE
                                        },
                                )
                            }
                        }

                        // ── divider ──
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color.White.copy(alpha = 0.10f)),
                        )

                        // ── Tabs ──
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            tabs.forEach { tab ->
                                val isActive = activeTab == tab
                                Text(
                                    text = tab,
                                    color = if (isActive) Color.White else Color(0xFF9CA3AF),
                                    fontSize = 14.sp,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                                    modifier = Modifier
                                        .scale(if (isActive) 1.05f else 1f)
                                        .clickable { activeTab = tab },
                                )
                            }
                        }

                        // ── GiftPickerGift grid (swipeable) ──
                        var dragX by remember { mutableStateOf(0f) }
                        val density = LocalDensity.current
                        val threshold = with(density) { 60.dp.toPx() }

                        Box(
                            Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .pointerInput(activeTab) {
                                    detectHorizontalDragGestures(
                                        onDragEnd = {
                                            if (dragX < -threshold) {
                                                val i = tabs.indexOf(activeTab)
                                                activeTab = tabs[(i + 1) % tabs.size]
                                            } else if (dragX > threshold) {
                                                val i = tabs.indexOf(activeTab)
                                                activeTab = tabs[(i - 1 + tabs.size) % tabs.size]
                                            }
                                            dragX = 0f
                                        },
                                        onHorizontalDrag = { _, amount -> dragX += amount },
                                    )
                                },
                        ) {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(4),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                items(currentGifts, key = { it.id }) { gift ->
                                    GiftCard(
                                        gift = gift,
                                        isSelected = selectedGift == gift.id,
                                        activeTab = activeTab,
                                        onClick = { selectedGift = gift.id },
                                    )
                                }
                            }
                        }

                        // ── Bottom bar ──
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            // Wallet balance
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { isWalletOpen = true },
                            ) {
                                AsyncImage(
                                    model = giftAssetUrl("/file_00000000e56882119c217d508b6733dc.png"),
                                    contentDescription = "Coins",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = walletBalance.toString(),
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                )
                                Icon(
                                    imageVector = Icons.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp),
                                )
                            }

                            // Multiplier + Send
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Box {
                                    // Dropdown
                                    if (showMultipliers) {
                                        Surface(
                                            color = Color(0xFF0C1418),
                                            shape = RoundedCornerShape(6.dp),
                                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                                            shadowElevation = 12.dp,
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .offset(y = (-44).dp)
                                                .widthIn(min = 50.dp),
                                        ) {
                                            Column(Modifier.padding(4.dp)) {
                                                multipliers.forEach { m ->
                                                    val active = selectedMultiplier == m
                                                    Box(
                                                        Modifier
                                                            .fillMaxWidth()
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(
                                                                if (active) Blue600 else Color.Transparent
                                                            )
                                                            .clickable {
                                                                selectedMultiplier = m
                                                                showMultipliers = false
                                                            }
                                                            .padding(horizontal = 10.dp, vertical = 2.dp),
                                                        contentAlignment = Alignment.Center,
                                                    ) {
                                                        Text(
                                                            text = m,
                                                            color = if (active) Color.White
                                                            else Color(0xFFD1D5DB),
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Medium,
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Row(
                                        Modifier
                                            .clip(RoundedCornerShape(50))
                                            .border(1.dp, Blue500, RoundedCornerShape(50)),
                                    ) {
                                        Row(
                                            Modifier
                                                .clickable { showMultipliers = !showMultipliers }
                                                .padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        ) {
                                            Text(
                                                text = selectedMultiplier,
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                            )
                                            Icon(
                                                imageVector = Icons.Filled.KeyboardArrowUp,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .rotate(if (showMultipliers) 180f else 0f),
                                            )
                                        }

                                        Box(
                                            Modifier
                                                .clip(RoundedCornerShape(topEnd = 50.dp, bottomEnd = 50.dp))
                                                .background(
                                                    Brush.linearGradient(
                                                        listOf(Blue500, Blue600)
                                                    )
                                                )
                                                .clickable(enabled = selectedGift != null && !sending) {
                                                    handleSend()
                                                }
                                                .padding(horizontal = 24.dp, vertical = 6.dp)
                                                .then(
                                                    if (selectedGift == null || sending)
                                                        Modifier.alpha(0.55f) else Modifier
                                                ),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                text = if (sending) "send" else "Send",
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Insufficient overlay
            if (showInsufficient) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(50),
                    ) {
                        Text(
                            text = insufficientText,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }
    }
}

// ============================================================
// GIFT CARD
// ============================================================
@Composable
private fun GiftCard(
    gift: GiftPickerGift,
    isSelected: Boolean,
    activeTab: String,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) Blue500.copy(alpha = 0.05f) else Color.Transparent
            )
            .border(
                2.dp,
                if (isSelected) Blue500 else Color.Transparent,
                RoundedCornerShape(8.dp),
            )
            .clickable { onClick() }
            .padding(horizontal = 2.dp, vertical = 6.dp),
    ) {
        // Lucky badge
        if (activeTab == "Lucky") {
            AsyncImage(
                model = giftAssetUrl("/IMG_20260928_172454.png"),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(16.dp),
            )
        }
        // Hot badge (only Arab King)
        if (activeTab == "Hot" && gift.id == 3) {
            AsyncImage(
                model = giftAssetUrl("/IMG_20260928_172516.png"),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(16.dp),
            )
        }

        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val size = if (gift.noMask) 60.dp else 64.dp
            Box(
                Modifier
                    .size(size)
                    .clip(
                        if (gift.sideFade) RoundedCornerShape(12.dp) else RoundedCornerShape(0.dp)
                    ),
            ) {
                AsyncImage(
                    model = giftAssetUrl(gift.image),
                    contentDescription = gift.name,
                    contentScale = if (gift.noMask) ContentScale.Fit else ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            Text(
                text = gift.name,
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(2.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                AsyncImage(
                    model = "/file_00000000e56882119c217d508b6733dc.png",
                    contentDescription = "Coins",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape),
                )
                Spacer(Modifier.width(2.dp))
                Text(
                    text = gift.coins.toString(),
                    color = Color(0xFF9CA3AF),
                    fontSize = 9.sp,
                )
            }
        }
    }
}

// ============================================================
// RECIPIENT BUTTON (MIC / HOUSE)
// ============================================================
private enum class RecipientIconType { MIC, HOUSE }

@Composable
private fun RecipientButton(
    isActive: Boolean,
    onClick: () -> Unit,
    iconType: RecipientIconType,
) {
    val iconColor = if (isActive) Blue500 else Color(0xFF9CA3AF)
    Box(
        Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(if (isActive) Blue500.copy(alpha = 0.20f) else Color(0xFF282D32))
            .border(2.dp, if (isActive) Blue500 else Color(0xFF6B7280), CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            Modifier.size(if (iconType == RecipientIconType.MIC) 18.dp else 20.dp)
        ) {
            val iconPath = androidx.compose.ui.graphics.Path()
            if (iconType == RecipientIconType.MIC) {
                // Exact solid microphone silhouette used by the reference GiftPicker.tsx.
                val sx = size.width / 24f
                val sy = size.height / 24f
                iconPath.moveTo(12f * sx, 14f * sy)
                iconPath.cubicTo(13.66f * sx, 14f * sy, 15f * sx, 12.66f * sy, 15f * sx, 11f * sy)
                iconPath.lineTo(15f * sx, 5f * sy)
                iconPath.cubicTo(15f * sx, 3.34f * sy, 13.66f * sx, 2f * sy, 12f * sx, 2f * sy)
                iconPath.cubicTo(10.34f * sx, 2f * sy, 9f * sx, 3.34f * sy, 9f * sx, 5f * sy)
                iconPath.lineTo(9f * sx, 11f * sy)
                iconPath.cubicTo(9f * sx, 12.66f * sy, 10.34f * sx, 14f * sy, 12f * sx, 14f * sy)
                iconPath.close()
                iconPath.moveTo(17.91f * sx, 11f * sy)
                iconPath.cubicTo(17.42f * sx, 11f * sy, 17.01f * sx, 11.39f * sy, 17.01f * sx, 11.88f * sy)
                iconPath.cubicTo(17.01f * sx, 14.64f * sy, 14.77f * sx, 16.88f * sy, 12.01f * sx, 16.88f * sy)
                iconPath.cubicTo(9.25f * sx, 16.88f * sy, 7.01f * sx, 14.64f * sy, 7.01f * sx, 11.88f * sy)
                iconPath.cubicTo(7.01f * sx, 11.39f * sy, 6.6f * sx, 11f * sy, 6.11f * sx, 11f * sy)
                iconPath.cubicTo(5.62f * sx, 11f * sy, 5.21f * sx, 11.39f * sy, 5.21f * sx, 11.88f * sy)
                iconPath.cubicTo(5.21f * sx, 15.54f * sy, 8.06f * sx, 18.54f * sy, 11.61f * sx, 18.96f * sy)
                iconPath.lineTo(11.61f * sx, 21.88f * sy)
                iconPath.lineTo(13.41f * sx, 21.88f * sy)
                iconPath.lineTo(13.41f * sx, 18.96f * sy)
                iconPath.cubicTo(16.96f * sx, 18.54f * sy, 19.81f * sx, 15.54f * sy, 19.81f * sx, 11.88f * sy)
                iconPath.cubicTo(19.81f * sx, 11.39f * sy, 19.4f * sx, 11f * sy, 18.91f * sx, 11f * sy)
                iconPath.close()
            } else {
                // Exact solid house/hound silhouette used by the reference GiftPicker.tsx.
                val sx = size.width / 512f
                val sy = size.height / 512f
                iconPath.moveTo(80f * sx, 235f * sy)
                iconPath.lineTo(222f * sx, 105f * sy)
                iconPath.quadraticTo(256f * sx, 75f * sy, 290f * sx, 105f * sy)
                iconPath.lineTo(432f * sx, 235f * sy)
                iconPath.quadraticTo(445f * sx, 247f * sy, 432f * sx, 260f * sy)
                iconPath.lineTo(405f * sx, 290f * sy)
                iconPath.lineTo(405f * sx, 405f * sy)
                iconPath.quadraticTo(405f * sx, 425f * sy, 385f * sx, 425f * sy)
                iconPath.lineTo(127f * sx, 425f * sy)
                iconPath.quadraticTo(107f * sx, 425f * sy, 107f * sx, 405f * sy)
                iconPath.lineTo(107f * sx, 290f * sy)
                iconPath.lineTo(80f * sx, 260f * sy)
                iconPath.quadraticTo(67f * sx, 247f * sy, 80f * sx, 235f * sy)
                iconPath.close()
                iconPath.moveTo(210f * sx, 425f * sy)
                iconPath.lineTo(210f * sx, 300f * sy)
                iconPath.quadraticTo(210f * sx, 280f * sy, 230f * sx, 280f * sy)
                iconPath.lineTo(282f * sx, 280f * sy)
                iconPath.quadraticTo(302f * sx, 280f * sy, 302f * sx, 300f * sy)
                iconPath.lineTo(302f * sx, 425f * sy)
                iconPath.close()
            }
            drawPath(path = iconPath, color = iconColor)
        }
        Surface(
            color = if (isActive) Blue500 else Color(0xFF9CA3AF),
            shape = RoundedCornerShape(50),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 0.dp, y = 0.dp),
        ) {
            Text(
                text = "All",
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
            )
        }
    }
}

// ============================================================
// VIDEO OVERLAY
// ============================================================
@Composable
private fun VideoEffectOverlay(
    src: String,
    style: String,
    onFinished: () -> Unit,
    onStarted: () -> Unit,
) {
    val isFade = style == "fade"
    var show by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        onStarted()
        delay(10_000)
        show = false
        onFinished()
    }

    if (!show) return
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Transparent),
        contentAlignment = Alignment.Center,
    ) {
        AndroidView(
            factory = { ctx ->
                VideoView(ctx).apply {
                    setVideoURI(Uri.parse(src))
                    setOnPreparedListener { mp ->
                        mp.isLooping = true
                        mp.setVolume(0f, 0f)
                        start()
                    }
                    setOnCompletionListener { onFinished() }
                }
            },
            modifier = if (isFade) Modifier.fillMaxSize()
            else Modifier
                .fillMaxWidth(0.72f)
                .fillMaxHeight(0.72f),
        )
    }
}

// ============================================================
// LUCKY GIFT NOTIFICATION SLIDER
// ============================================================
data class LuckyNotice(
    val id: String,
    val senderId: String,
    val senderName: String,
    val recipientName: String,
    val senderImage: String,
    val giftImage: String,
    val multiplier: Int,
    val totalWinAmount: Long,
    val maxWinTimes: Int,
    val recipientMode: RecipientMode,
    val isExiting: Boolean = false,
)

@Composable
fun LuckyGiftNotificationSlider(
    notice: LuckyNotice?,
    floatingWins: List<Pair<String, Long>> = emptyList(),
) {
    AnimatedVisibility(
        visible = notice != null && !notice.isExiting,
        enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 100.dp),
    ) {
        if (notice == null) return@AnimatedVisibility
        val isBroadcast = notice.recipientMode == RecipientMode.MIC ||
                notice.recipientMode == RecipientMode.ROOM
        val topText = when (notice.recipientMode) {
            RecipientMode.MIC -> "Sent to All Mic"
            RecipientMode.ROOM -> "Sent to All in room"
            else -> notice.senderName
        }
        val bottomText = if (isBroadcast) notice.senderName else "To ${notice.recipientName}"

        Row(
            Modifier
                .clip(RoundedCornerShape(topEnd = 50.dp, bottomEnd = 50.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFFFC107),
                            Color(0xFFFFC107),
                            Color(0xD9FFC107),
                            Color(0x73FFC107),
                            Color(0x26FFC107),
                            Color(0x00FFC107),
                        ),
                        startX = 0f,
                        endX = Float.POSITIVE_INFINITY,
                    )
                )
                .padding(start = 4.dp, end = 60.dp, top = 4.dp, bottom = 4.dp)
                .widthIn(min = 280.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = notice.senderImage,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape),
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.widthIn(max = 110.dp)) {
                Text(
                    text = topText,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = bottomText,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            AsyncImage(
                model = notice.giftImage,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(38.dp),
            )
            Text(
                text = "×${notice.multiplier}",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}

// ============================================================
// LUCKY COMBO BUTTON
// ============================================================
@Composable
fun LuckyComboButton(
    visible: Boolean,
    timeLeft: Int,
    maxTime: Int = 5,
    onClick: () -> Unit,
) {
    if (!visible) return
    val infiniteTransition = rememberInfiniteTransition(label = "comboPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            tween(1800, easing = CubicBezierEasing(0.2f, 0.8f, 0.4f, 1f)),
            RepeatMode.Restart,
        ),
        label = "pulse",
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            tween(1800, easing = CubicBezierEasing(0.2f, 0.8f, 0.4f, 1f)),
            RepeatMode.Restart,
        ),
        label = "alpha",
    )

    Box(
        Modifier
            .fillMaxSize()
            .padding(end = 24.dp, bottom = 48.dp),
        contentAlignment = Alignment.BottomEnd,
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(104.dp)) {
            // pulse rings
            Box(
                Modifier
                    .fillMaxSize()
                    .scale(pulseScale)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                Color(0x99FF3366),
                                Color(0x1AFF3366),
                            )
                        ),
                        CircleShape,
                    )
                    .alpha(pulseAlpha),
            )

            // main button
            Box(
                Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0xFFFF4D79), Color(0xFFFF1A4D))
                        )
                    )
                    .clickable { onClick() },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Combo",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                )
                // Countdown ring
                Canvas(Modifier.fillMaxSize().padding(8.dp)) {
                    val stroke = Stroke(width = 4.5f)
                    // background ring
                    drawCircle(
                        color = Color.White.copy(alpha = 0.35f),
                        radius = size.minDimension / 2 - stroke.width / 2,
                        style = stroke,
                    )
                    // progress arc
                    drawArc(
                        color = Color.White,
                        startAngle = -90f,
                        sweepAngle = 360f * (timeLeft.toFloat() / maxTime),
                        useCenter = false,
                        style = stroke,
                    )
                }
            }
        }
    }
}


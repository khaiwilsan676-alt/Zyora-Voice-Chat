package com.hurry.voicechat

import android.net.Uri
import android.widget.VideoView
import android.view.WindowManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
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
import androidx.compose.ui.window.DialogWindowProvider
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
    GiftPickerGift(3, "Arab King", 500000, "/image_d9df9625~2.jpg",
        video = "/gemini_generated_video_17e19680~2.mp4", videoStyle = "fade", sideFade = true),
    GiftPickerGift(2001, "Mysalo Princess", 499999, "/IMG_20261124_165000.png",
        video = "/alioss_29_2026031617445981861~2.mp4", videoStyle = "fade", sideFade = true),
    GiftPickerGift(2002, "Love Twinkle", 200000, "/IMG_20261124_165025.png",
        video = "/alioss_30_2026031022451975617~2.mp4", videoStyle = "fade", sideFade = true),
    GiftPickerGift(2003, "Castle Night", 170000, "/IMG_20261124_165043.png",
        video = "/alioss_29_2026032620350482882~2.mp4", videoStyle = "fade", sideFade = true),
    GiftPickerGift(2004, "Christmas Deer", 24900, "/IMG_20261125_014144.png",
        video = "/alioss_4_wm_20261119100204_45937~2.mp4", videoStyle = "fade", sideFade = true),
    GiftPickerGift(2005, "Liffy lipstick", 12900, "/IMG_20261125_014203.png",
        video = "/alioss_29_2025112718560649595~2.mp4", videoStyle = "fade", sideFade = true),
    GiftPickerGift(2006, "Gift", 9999, "/IMG_20261125_014233.png",
        video = "/alioss_28_2026032011584793951~2.mp4", videoStyle = "fade", sideFade = true),
    GiftPickerGift(2007, "Heart lamp", 45000, "/IMG_20261125_014348.png",
        video = "/alioss_28_2025102210290741641~2.mp4", videoStyle = "fade", sideFade = true),
    GiftPickerGift(2008, "Heartbeat", 120999, "/IMG_20261125_014410.png",
        video = "/alioss_28_2025081318180787557~2.mp4", videoStyle = "fade", sideFade = true),
    GiftPickerGift(2009, "Desert Aura", 55000, "/IMG_20261125_014422.png",
        video = "/alioss_20_2026111817155739325~2.mp4", videoStyle = "fade", sideFade = true),
)

private val luxuryGifts = listOf(
    GiftPickerGift(3001, "Golden tiger", 1_000_000, "/file_000000008a8c821186f5941d97193ee7.png",
        video = "/alioss_28_2025121214091492750~2.mp4", videoStyle = "fade", sideFade = true),
)

private val standardGifts = listOf(
    GiftPickerGift(2101, "Apple", 999, "/IMG_20261124_162823.png", noMask = true),
    GiftPickerGift(2102, "Cherry", 1299, "/IMG_20261124_162846.png", noMask = true),
    GiftPickerGift(2103, "Strawberry", 1500, "/IMG_20261124_162906.png", noMask = true),
    GiftPickerGift(2104, "Banana", 1599, "/IMG_20261124_162922.png", noMask = true),
    GiftPickerGift(2105, "Avocado", 2500, "/IMG_20261124_162944.png", noMask = true),
    GiftPickerGift(2106, "Mango", 2599, "/IMG_20261124_163008.png", noMask = true),
    GiftPickerGift(2107, "Papaya", 1400, "/IMG_20261124_163037.png", noMask = true),
    GiftPickerGift(2108, "Watermelon", 3000, "/IMG_20261124_163056.png", noMask = true),
    GiftPickerGift(2109, "Lucky Star", 1250, "/IMG_20261124_163119.png", noMask = true),
    GiftPickerGift(2110, "Bell", 4999, "/IMG_20261124_163139.png", noMask = true),
    GiftPickerGift(2111, "Love Lock", 3000, "/IMG_20261124_163202.png", noMask = true),
    GiftPickerGift(2112, "Crystal", 5999, "/IMG_20261124_163228.png", noMask = true),
    GiftPickerGift(2113, "Tea Pot", 2000, "/IMG_20261124_163256.png", noMask = true),
    GiftPickerGift(2114, "Shisha", 7000, "/IMG_20261124_163315.png", noMask = true),
    GiftPickerGift(2115, "Sword", 7250, "/IMG_20261124_163337.png", noMask = true),
)


private fun giftAssetUrl(path: String): String {
    if (path.startsWith("https://") || path.startsWith("http://") || path.startsWith("file:") || path.startsWith("content:")) return path
    // Resolve native gift media directly from the committed public assets.
    // The previous Vercel host could serve a stale deployment for newly added files.
    return "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/" +
        path.trimStart('/')
}

private val tabs = listOf("Hot", "Lucky", "Luxury", "Event")

private val multipliers = listOf("1", "10", "299", "599", "999")

// Blue palette
private val Blue500 = Color(0xFF3B82F6)
private val Blue600 = Color(0xFF2563EB)

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
    roomOwnerId: String = "",
    roomAdminIds: List<String> = emptyList(),
    walletBalance: Long = 0L,
    onDebitWallet: suspend (Long) -> Long? = { null },
    onCreditDiamonds: suspend (Int) -> Unit = {},
    onSendCoinTransfer: (Map<String, Any?>) -> Unit = {},
    onSendGiftVideoPlay: (Map<String, Any?>) -> Unit = {},
    onRecordGiftTransaction: suspend (String, Long) -> Unit = { _, _ -> },
    onRecordDiamondTransaction: suspend (String, Int) -> Unit = { _, _ -> },
    walletContent: @Composable () -> Unit = {},
    directMode: Boolean = false,
    directRecipient: GiftPickerRoomUser? = null,
) {
    var activeTab by remember { mutableStateOf("Hot") }
    var selectedMultiplier by remember { mutableStateOf("1") }
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
        "Lucky" -> standardGifts
        "Hot" -> hotGifts
        "Luxury" -> luxuryGifts
        else -> emptyList()
    }

    val parseMultiplier: (String) -> Int = { m -> m.toIntOrNull() ?: 1 }
    val selectedGiftObj = currentGifts.find { it.id == selectedGift }
    val totalCost = selectedGiftObj?.let { it.coins * parseMultiplier(selectedMultiplier) } ?: 0

    // Build one-pass lookups instead of repeatedly scanning the full user/seat
    // lists for every recipient. These are rebuilt from the current snapshots so
    // in-place room seat updates cannot leave stale cached recipients on screen.
    val roomUsersById = roomUsers.associateBy { it.accountId }
    val seatsById = seats.associateBy { it.accountId }
    val recipientIds = when {
        directMode && directRecipient != null -> listOf(directRecipient.accountId)
        selectedTargets.isNotEmpty() -> selectedTargets
        roomUsers.isNotEmpty() -> roomUsers.map { it.accountId }
        else -> seats.filter { it.occupied && it.accountId.isNotBlank() }.map { it.accountId }
    }.asSequence().filter { it.isNotBlank() }.distinct().toList()
    val recipientCount = recipientIds.size
    val firstRecipient = if (directMode) directRecipient else recipientIds.firstNotNullOfOrNull { id ->
        roomUsersById[id]?.let {
            GiftPickerRoomUser(it.accountId, it.name, it.image)
        } ?: seatsById[id]?.let {
            GiftPickerRoomUser(it.accountId, it.userName, it.userImage)
        }
    }
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

        // Freeze the chosen gift and recipients for this transaction. UI state may
        // recompose while the send coroutine is running.
        val giftToSend = selectedGiftObj ?: return
        val tabToSend = activeTab
        val multiplierToSend = parseMultiplier(selectedMultiplier)
        val targetsToSend = recipientIds.toList()
        val firstRecipientToSend = firstRecipient
        val totalCostToSend = totalCost
        val totalSendCostToSend = totalSendCost
        val isLuckyToSend = isLuckyGiftTab

        sending = true
        scope.launch {
            // The server is the single source of truth for Lucky Gift rewards.
            // Debit the gross send amount here; client-side random multipliers could
            // produce a negative debit and reject valid sends before the server ran.
            val updatedBalance = onDebitWallet(totalSendCostToSend)
            if (updatedBalance == null) {
                sending = false
                triggerInsufficient("Insufficient Balance", true)
                return@launch
            }
            onRecordGiftTransaction(giftToSend.name, -totalSendCostToSend)

            if ((roomId.isNotEmpty() || directMode) && currentUserAccountId.isNotEmpty() && targetsToSend.isNotEmpty()) {
                val uniqueTransferId = "tx-$currentUserAccountId-${System.currentTimeMillis()}-${(0..999999).random()}"

                onSendCoinTransfer(mapOf(
                    "roomId" to roomId,
                    "senderId" to currentUserAccountId,
                    "recipientIds" to targetsToSend,
                    "amount" to totalSendCostToSend,
                    "giftName" to giftToSend.name,
                    "giftImage" to giftToSend.image,
                    "giftType" to tabToSend,
                    "senderName" to currentUserName,
                    "senderImage" to currentUserImage,
                    "recipientName" to (firstRecipientToSend?.name ?: "User"),
                    "recipientImage" to (firstRecipientToSend?.image ?: "/default-avatar.png"),
                    "multiplier" to multiplierToSend,
                    "luckyGift" to isLuckyToSend,
                    "luckyImage" to giftToSend.image,
                    "transferId" to uniqueTransferId,
                    "eventId" to uniqueTransferId,
                    "timestamp" to System.currentTimeMillis(),
                    "recipientMode" to recipientMode.name.lowercase(),
                ))

                if (giftToSend.video != null && !isLuckyToSend) {
                    onSendGiftVideoPlay(mapOf(
                        "roomId" to roomId,
                        "senderId" to currentUserAccountId,
                        "giftName" to giftToSend.name,
                        "video" to giftToSend.video,
                        "videoStyle" to (giftToSend.videoStyle ?: "fade"),
                        "timestamp" to System.currentTimeMillis(),
                    ))
                }
            }

            onSend(totalCostToSend)

            if (isLuckyToSend) {
                sending = false
                onClose()
                return@launch
            }

            if (giftToSend.video != null) {
                playingVideo = giftToSend.video to (giftToSend.videoStyle ?: "fade")
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
            val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
            SideEffect {
                dialogWindow?.setBackgroundDrawableResource(android.R.color.transparent)
                dialogWindow?.decorView?.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                dialogWindow?.setDimAmount(0f)
                dialogWindow?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            }
            
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter,
            ) {
                val giftSheetHeight = maxHeight * 0.52f
                // Drag dismiss variable
                var offsetY by remember { mutableStateOf(0f) }
                
                BoxWithConstraints(
                    Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.52f)
                        // Applying Offset based on Drag
                        .offset { IntOffset(0, offsetY.roundToInt()) }
                        // Pointer input to detect drag from top to bottom
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragEnd = {
                                    if (offsetY > 150f) { // threshold to close
                                        onClose()
                                    } else {
                                        offsetY = 0f // snap back if not dragged enough
                                    }
                                },
                                onVerticalDrag = { change, dragAmount ->
                                    change.consume()
                                    if (offsetY + dragAmount > 0) { // only allow dragging down
                                        offsetY += dragAmount
                                    }
                                }
                            )
                        }
                        .background(
                            color = Color(0xEB000000),
                            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp) 
                        )
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)) 
                        .border(
                            1.dp,
                            Color.White.copy(alpha = 0.10f),
                            RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                        )
                        .navigationBarsPadding()
                        .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
                ) {
                    Column(Modifier.fillMaxSize()) {

                        Row(
                            Modifier.fillMaxWidth().padding(bottom = 2.dp),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (directMode && directRecipient != null) {
                                AsyncImage(
                                    model = directRecipient.image,
                                    contentDescription = directRecipient.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(38.dp).clip(CircleShape)
                                        .border(2.5.dp, Blue500, CircleShape)
                                )
                            } else {
                                LazyRow(
                                    Modifier.weight(1f).padding(end = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    items(seats.filter { it.occupied && it.accountId.isNotBlank() }, key = { it.accountId }) { seat ->
                                        val uid = seat.accountId
                                        val selected = selectedTargets.contains(uid)
                                        val roleLabel = when {
                                            uid.isNotBlank() && uid == roomOwnerId -> "Owner"
                                            uid.isNotBlank() && uid in roomAdminIds -> "Admin"
                                            else -> null
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Box(modifier = Modifier.size(38.dp)) {
                                                AsyncImage(
                                                    model = seat.userImage,
                                                    contentDescription = seat.userName,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.size(38.dp).clip(CircleShape)
                                                        .border(2.5.dp, if (selected) Blue500 else Color.Transparent, CircleShape)
                                                        .clickable {
                                                            selectedTargets = if (selected) selectedTargets.filter { it != uid } else selectedTargets + uid
                                                            recipientMode = RecipientMode.SINGLE
                                                        },
                                                )
                                                if (roleLabel != null) {
                                                    Surface(
                                                        modifier = Modifier.align(Alignment.BottomCenter).offset(y = 7.dp),
                                                        color = Blue500,
                                                        shape = RoundedCornerShape(50),
                                                        border = BorderStroke(0.75.dp, Color.White),
                                                    ) {
                                                        Box(
                                                            contentAlignment = Alignment.Center,
                                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                                        ) {
                                                            Text(
                                                                text = roleLabel,
                                                                color = Color.White,
                                                                fontSize = 7.sp,
                                                                fontWeight = FontWeight.Medium,
                                                                textAlign = TextAlign.Center,
                                                                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                            if (roleLabel != null) Spacer(Modifier.height(7.dp))
                                        }
                                    }
                                }
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(start = 2.dp)
                                ) {
                                    RecipientButton(isActive = recipientMode == RecipientMode.MIC, onClick = {
                                        selectedTargets = seats.filter { it.occupied && it.accountId.isNotBlank() }.map { it.accountId }
                                        recipientMode = RecipientMode.MIC
                                    }, iconType = RecipientIconType.MIC)
                                    RecipientButton(isActive = recipientMode == RecipientMode.ROOM, onClick = {
                                        selectedTargets = emptyList()
                                        recipientMode = RecipientMode.ROOM
                                    }, iconType = RecipientIconType.HOUSE)
                                }
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
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (activeTab == "Hot") {
                                Modifier
                                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                                    .drawWithContent {
                                        drawContent()
                                        val edgeFade = 0.01f
                                        drawRect(
                                            brush = Brush.horizontalGradient(
                                                0f to Color.Transparent,
                                                edgeFade to Color.White,
                                                (1f - edgeFade) to Color.White,
                                                1f to Color.Transparent,
                                            ),
                                            blendMode = BlendMode.DstIn,
                                        )
                                        drawRect(
                                            brush = Brush.verticalGradient(
                                                0f to Color.Transparent,
                                                edgeFade to Color.White,
                                                (1f - edgeFade) to Color.White,
                                                1f to Color.Transparent,
                                            ),
                                            blendMode = BlendMode.DstIn,
                                        )
                                    }
                            } else Modifier
                        ),
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
                                    if (showMultipliers) {
                                        Layout(
                                            content = {
                                                Surface(
                                                    color = Color(0xFF0C1418),
                                                    shape = RoundedCornerShape(12.dp), 
                                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                                                    shadowElevation = 8.dp,
                                                    modifier = Modifier.width(50.dp)
                                                ) {
                                                    Column(Modifier.padding(0.dp)) {
                                                        multipliers.forEach { m ->
                                                            val active = selectedMultiplier == m
                                                            Box(
                                                                Modifier
                                                                    .fillMaxWidth()
                                                                    .background(Color.Transparent) 
                                                                    .clickable {
                                                                        selectedMultiplier = m
                                                                        showMultipliers = false
                                                                    }
                                                                    .padding(vertical = 4.dp), 
                                                                contentAlignment = Alignment.Center,
                                                            ) {
                                                                Text(
                                                                    text = m,
                                                                    color = if (active) Color.White else Color(0xFFD1D5DB),
                                                                    fontSize = 10.sp, 
                                                                    fontWeight = FontWeight.Medium,
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        ) { measurables, constraints ->
                                            val placeable = measurables.first().measure(constraints)
                                            layout(0, 0) {
                                                placeable.place(0, -placeable.height - 15)
                                            }
                                        }
                                    }

                                    // Main Button Container
                                    Row(
                                        Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(Color.Transparent) 
                                            .border(1.dp, Blue500.copy(alpha = 0.6f), RoundedCornerShape(50)),
                                    ) {
                                        // 1x Multiplier Part
                                        Row(
                                            Modifier
                                                .clickable { showMultipliers = !showMultipliers }
                                                .padding(horizontal = 14.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        ) {
                                            Text(
                                                text = selectedMultiplier,
                                                color = Color.White,
                                                fontSize = 13.sp,
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

                                        // Send Part
                                        Box(
                                            Modifier
                                                .alpha(1f)
                                                .width(70.dp)
                                                .clip(RoundedCornerShape(topEnd = 50.dp, bottomEnd = 50.dp))
                                                .background(
                                                    Brush.verticalGradient(
                                                        colors = listOf(Color(0xFF60A5FA), Color(0xFF1D4ED8)) 
                                                    )
                                                )
                                                .border(
                                                    width = 0.5.dp,
                                                    color = Color.White.copy(alpha = 0.4f), 
                                                    shape = RoundedCornerShape(topEnd = 50.dp, bottomEnd = 50.dp)
                                                )
                                                .clickable(enabled = selectedGift != null && !sending) {
                                                    handleSend()
                                                }
                                                .padding(vertical = 6.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                text = if (sending) "send" else "Send",
                                                color = Color.White,
                                                fontSize = 13.sp,
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
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (isSelected) Color.Gray.copy(alpha = 0.35f) else Color.Transparent 
            )
            .clickable { onClick() }
            .padding(start = 2.dp, end = 2.dp, top = 2.dp, bottom = 1.dp),
    ) {
        if (activeTab == "Lucky") {
            AsyncImage(
                model = giftAssetUrl("/IMG_20260928_172454.png"),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(12.dp),
            )
        }

        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val size = if (gift.noMask) 48.dp else 64.dp
            val isHotCircle = activeTab == "Hot" && (gift.id == 1 || gift.id == 3) 
            val isHotEdgeFade = activeTab == "Hot" && (gift.id == 2001 || gift.id == 2002 || gift.id == 2003 || gift.id in 2004..2009)

            Box(
                Modifier
                    .size(size)
                    .clip(
                        when {
                            isHotCircle -> CircleShape 
                            gift.sideFade -> RoundedCornerShape(12.dp)
                            else -> RoundedCornerShape(0.dp)
                        }
                    )
                    .then(
                        if (isHotEdgeFade) {
                            Modifier
                                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                                .drawWithContent {
                                    drawContent()
                                    val edgeFade = 0.05f
                                    drawRect(
                                        brush = Brush.horizontalGradient(
                                            0f to Color.Transparent,
                                            edgeFade to Color.White,
                                            (1f - edgeFade) to Color.White,
                                            1f to Color.Transparent,
                                        ),
                                        blendMode = BlendMode.DstIn,
                                    )
                                    drawRect(
                                        brush = Brush.verticalGradient(
                                            0f to Color.Transparent,
                                            edgeFade to Color.White,
                                            (1f - edgeFade) to Color.White,
                                            1f to Color.Transparent,
                                        ),
                                        blendMode = BlendMode.DstIn,
                                    )
                                }
                        } else Modifier
                    ),
            ) {
                AsyncImage(
                    model = giftAssetUrl(gift.image),
                    contentDescription = gift.name,
                    contentScale = if (gift.noMask) ContentScale.Fit else ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )

                if (isHotCircle) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    0.90f to Color.Transparent, 
                                    1.0f to Color.Black.copy(alpha = 0.6f) 
                                )
                            )
                    )
                }

                if (activeTab == "Hot") {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(16.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color(0x99000000))
                                )
                            )
                    )
                }
            }

            Text(
                text = gift.name,
                color = Color.White,
                fontSize = 10.sp,
                lineHeight = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                style = TextStyle(
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 1.dp),
            )

            Row(
                modifier = Modifier.padding(top = 1.dp, bottom = 1.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                AsyncImage(
                    model = giftAssetUrl("/file_00000000e56882119c217d508b6733dc.png"),
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
                    lineHeight = 10.sp,
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    ),
                )
            }
        }
    }
}

// ============================================================
// RECIPIENT BUTTON
// ============================================================
private enum class RecipientIconType { MIC, HOUSE }

@Composable
private fun RecipientButton(
    isActive: Boolean,
    onClick: () -> Unit,
    iconType: RecipientIconType,
) {
    val iconColor = if (isActive) Blue500 else Color(0xFF9CA3AF)
    Box(Modifier.width(44.dp).height(46.dp), contentAlignment = Alignment.TopCenter) {
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
                    drawPath(path = iconPath, color = iconColor)
                } else {
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

                    val doorPath = androidx.compose.ui.graphics.Path()
                    doorPath.moveTo(226f * sx, 425f * sy)
                    doorPath.lineTo(226f * sx, 340f * sy)
                    doorPath.quadraticTo(256f * sx, 290f * sy, 286f * sx, 340f * sy)
                    doorPath.lineTo(286f * sx, 425f * sy)
                    doorPath.close()

                    val finalPath = androidx.compose.ui.graphics.Path()
                    finalPath.op(iconPath, doorPath, PathOperation.Difference)
                    drawPath(path = finalPath, color = iconColor)
                }
            }
        }
        
        Surface(
            color = if (isActive) Blue500 else Color(0xFF9CA3AF),
            shape = RoundedCornerShape(50),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-2).dp, y = (-2).dp)
                .height(13.dp), 
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 4.dp)) {
                Text(
                    text = "All",
                    color = Color.White,
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
            }
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
    
    var videoRatio by remember { mutableStateOf<Float?>(null) }

    LaunchedEffect(src) { onStarted() }

    if (!show) return

    val isKing = src.contains("17e19680~2")
    val isMysaloPrincess = src.contains("2026031617445981861~2")
    val isCastleNight = src.contains("2026032620350482882~2")

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Transparent),
        contentAlignment = Alignment.Center,
    ) {
        AndroidView(
            factory = { ctx ->
                android.view.TextureView(ctx).apply {
                    isOpaque = false 
                    surfaceTextureListener = object : android.view.TextureView.SurfaceTextureListener {
                        var mediaPlayer: android.media.MediaPlayer? = null

                        override fun onSurfaceTextureAvailable(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {
                            try {
                                mediaPlayer = android.media.MediaPlayer().apply {
                                    setDataSource(ctx, Uri.parse(giftAssetUrl(src)))
                                    setSurface(android.view.Surface(surface))
                                    setVolume(1f, 1f)
                                    setOnVideoSizeChangedListener { _, w, h ->
                                        if (w > 0 && h > 0) {
                                            videoRatio = w.toFloat() / h.toFloat()
                                        }
                                    }
                                    setOnPreparedListener { mp ->
                                        mp.isLooping = false
                                        mp.start() 
                                    }
                                    setOnCompletionListener { onFinished() }
                                    setOnErrorListener { _, _, _ ->
                                        onFinished()
                                        true
                                    }
                                    prepareAsync()
                                }
                            } catch (e: Exception) {
                                onFinished()
                            }
                        }

                        override fun onSurfaceTextureSizeChanged(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {}
                        
                        override fun onSurfaceTextureDestroyed(surface: android.graphics.SurfaceTexture): Boolean {
                            mediaPlayer?.release()
                            mediaPlayer = null
                            return true
                        }
                        
                        override fun onSurfaceTextureUpdated(surface: android.graphics.SurfaceTexture) {}
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .then(if (videoRatio != null) Modifier.aspectRatio(videoRatio!!) else Modifier.wrapContentHeight())
                .graphicsLayer { 
                    compositingStrategy = CompositingStrategy.Offscreen 
                    if (style == "pure") {
                        blendMode = BlendMode.Plus 
                    }
                }
                .drawWithContent {
                    drawContent()
                    if (isFade) {
                        val topFadeStop = if (isCastleNight) 0.13f else 0.1f
                        val bottomFadeStop = when {
                            isKing -> 0.85f
                            isMysaloPrincess -> 0.87f
                            isCastleNight -> 0.87f
                            else -> 0.9f
                        }
                        drawRect(
                            brush = Brush.verticalGradient(
                                0.0f to Color.White.copy(alpha = 0f), 
                                topFadeStop to Color.White, 
                                bottomFadeStop to Color.White, 
                                1.0f to Color.White.copy(alpha = 0f) 
                            ),
                            blendMode = BlendMode.DstIn 
                        )
                    }
                }
        )
    }
}

@Composable
fun RoomGiftVideoOverlay(src: String, onFinished: () -> Unit) {
    VideoEffectOverlay(src = src, style = "fade", onFinished = onFinished, onStarted = {})
}

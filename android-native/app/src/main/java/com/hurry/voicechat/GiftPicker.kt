package com.hurry.voicechat

import android.graphics.SurfaceTexture
import android.net.Uri
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.view.Surface
import android.view.TextureView
import android.view.WindowManager
import android.widget.VideoView
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
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
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import coil3.compose.AsyncImage
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
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
    val fitNoFade: Boolean = false,
    val greenRemove: Boolean = false,
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

private const val GITHUB_PUBLIC_BASE_URL =
    "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/"

private fun giftAssetUrl(path: String): String {
    if (path.startsWith("https://") || path.startsWith("http://") || path.startsWith("file:") || path.startsWith("content:")) return path
    // Resolve native gift media directly from the committed public assets.
    // The previous Vercel host could serve a stale deployment for newly added files.
    return GITHUB_PUBLIC_BASE_URL + path.trimStart('/')
}

// ============================================================
// GIFTS
// ============================================================
private val hotGifts = listOf(
    GiftPickerGift(2004, "Christmas Deer", 24900, "${GITHUB_PUBLIC_BASE_URL}IMG_20261125_014144.png",
        video = "${GITHUB_PUBLIC_BASE_URL}alioss_29_2025112718560649595~2.mp4", videoStyle = "fade", sideFade = true),
    GiftPickerGift(2005, "Liffy lipstick", 12900, "${GITHUB_PUBLIC_BASE_URL}IMG_20261125_014203.png",
        video = "${GITHUB_PUBLIC_BASE_URL}alioss_20_2026111817155739325~2.mp4", videoStyle = "fade", sideFade = true),
    GiftPickerGift(2006, "Kitty Cat", 9999, "${GITHUB_PUBLIC_BASE_URL}IMG_20261125_014233.png",
        video = "${GITHUB_PUBLIC_BASE_URL}alioss_4_wm_20261119100204_45937~2.mp4", videoStyle = "fade", sideFade = true),
    GiftPickerGift(2007, "Heart lamp", 45000, "${GITHUB_PUBLIC_BASE_URL}IMG_20261125_014348.png",
        video = "${GITHUB_PUBLIC_BASE_URL}alioss_28_2026032011584793951~2.mp4", videoStyle = "fade", sideFade = true),
    GiftPickerGift(3, "Arab King", 500000, "${GITHUB_PUBLIC_BASE_URL}image_d9df9625~2.jpg",
        video = "${GITHUB_PUBLIC_BASE_URL}gemini_generated_video_17e19680~2.mp4", videoStyle = "fade", sideFade = true),
    GiftPickerGift(2021, "Chiken", 65000, "${GITHUB_PUBLIC_BASE_URL}IMG_20261126_174325.png",
        video = "${GITHUB_PUBLIC_BASE_URL}gemini_generated_video_b14f3d64~2.mp4", videoStyle = "fade", sideFade = false, fitNoFade = true, greenRemove = true),
    GiftPickerGift(2002, "Love Twinkle", 200000, "${GITHUB_PUBLIC_BASE_URL}IMG_20261124_165025.png",
        video = "${GITHUB_PUBLIC_BASE_URL}alioss_30_2026031022451975617~2.mp4", videoStyle = "fade", sideFade = true),
    GiftPickerGift(2008, "Heartbeat", 120999, "${GITHUB_PUBLIC_BASE_URL}IMG_20261125_014410.png",
        video = "${GITHUB_PUBLIC_BASE_URL}alioss_28_2025081318180787557~2.mp4", videoStyle = "fade", sideFade = true),
    GiftPickerGift(2009, "Desert Aura", 55000, "${GITHUB_PUBLIC_BASE_URL}IMG_20261125_014422.png",
        video = "${GITHUB_PUBLIC_BASE_URL}alioss_28_2025102210290741641~2.mp4", videoStyle = "fade", sideFade = true),
    GiftPickerGift(2010, "Golden Santa", 80000, "${GITHUB_PUBLIC_BASE_URL}IMG_20261126_131208.png",
        video = "${GITHUB_PUBLIC_BASE_URL}alioss_19_2025112417523580912~2.mp4", videoStyle = "fade", sideFade = true),
    GiftPickerGift(2011, "Marry Christmas", 60500, "${GITHUB_PUBLIC_BASE_URL}IMG_20261126_131036.png",
        video = "${GITHUB_PUBLIC_BASE_URL}alioss_19_2025120215032990087~2.mp4", videoStyle = "fade", sideFade = true),
    GiftPickerGift(2012, "Telephone", 20000, "${GITHUB_PUBLIC_BASE_URL}IMG_20261126_130742.png",
        video = "${GITHUB_PUBLIC_BASE_URL}gemini_generated_video_53f32fc7~3.mp4", videoStyle = "fade", sideFade = false, fitNoFade = true, greenRemove = true),
    GiftPickerGift(2013, "Pastery", 10000, "${GITHUB_PUBLIC_BASE_URL}IMG_20261126_130758.png",
        video = "${GITHUB_PUBLIC_BASE_URL}gemini_generated_video_33912b4f~2.mp4", videoStyle = "fade", sideFade = false, fitNoFade = true, greenRemove = true),
    GiftPickerGift(2014, "Hand Bag", 15500, "${GITHUB_PUBLIC_BASE_URL}IMG_20261126_130829.png",
        video = "${GITHUB_PUBLIC_BASE_URL}gemini_generated_video_e1f4d16e~2.mp4", videoStyle = "fade", sideFade = false, fitNoFade = true, greenRemove = true),
    GiftPickerGift(2022, "Sugar Sweet", 50000, "${GITHUB_PUBLIC_BASE_URL}IMG_20261126_174344.png",
        video = "${GITHUB_PUBLIC_BASE_URL}gemini_generated_video_60f28193~2.mp4", videoStyle = "fade", sideFade = false, fitNoFade = true, greenRemove = true),
    GiftPickerGift(2017, "Crystal Eagle", 55000, "${GITHUB_PUBLIC_BASE_URL}IMG_20261126_131120.png",
        video = "${GITHUB_PUBLIC_BASE_URL}alioss_19_2025092420113736699~2.mp4", videoStyle = "fade", sideFade = true),
    GiftPickerGift(2018, "Ring Perposal", 90000, "${GITHUB_PUBLIC_BASE_URL}IMG_20261126_131146.png",
        video = "${GITHUB_PUBLIC_BASE_URL}alioss_32_2026030812294130790~2.mp4", videoStyle = "fade", sideFade = true),
    GiftPickerGift(2019, "Perfume", 80500, "${GITHUB_PUBLIC_BASE_URL}IMG_20261126_131228.png",
        video = "${GITHUB_PUBLIC_BASE_URL}alioss_32_2026030801230537257~2.mp4", videoStyle = "fade", sideFade = true),
    GiftPickerGift(2020, "Kitty Gloves", 35000, "${GITHUB_PUBLIC_BASE_URL}IMG_20261126_174313.png",
        video = "${GITHUB_PUBLIC_BASE_URL}gemini_generated_video_ab36b2de~2.mp4", videoStyle = "fade", sideFade = false, fitNoFade = true, greenRemove = true),
    GiftPickerGift(2001, "Mysalo Princess", 499999, "${GITHUB_PUBLIC_BASE_URL}IMG_20261124_165000.png",
        video = "${GITHUB_PUBLIC_BASE_URL}alioss_29_2026031617445981861~2.mp4", videoStyle = "fade", sideFade = true),
    GiftPickerGift(2016, "Dragon Flame", 520000, "${GITHUB_PUBLIC_BASE_URL}IMG_20261126_131057.png",
        video = "${GITHUB_PUBLIC_BASE_URL}alioss_19_2026031609505116054~2.mp4", videoStyle = "fade", sideFade = true),
)

private val luxuryGifts = listOf(
    GiftPickerGift(3001, "Golden tiger", 40_000_000, "${GITHUB_PUBLIC_BASE_URL}file_000000008a8c821186f5941d97193ee7.png",
        video = "${GITHUB_PUBLIC_BASE_URL}alioss_28_2025121214091492750~2.mp4", videoStyle = "fade", sideFade = true),
    GiftPickerGift(3002, "Fairy Fox", 7_000_000, "${GITHUB_PUBLIC_BASE_URL}IMG_20261126_131020.png",
        video = "${GITHUB_PUBLIC_BASE_URL}alioss_19_2026030523213229566~2.mp4", videoStyle = "fade", sideFade = true),
    GiftPickerGift(2015, "Double Gun", 65_000_000, "${GITHUB_PUBLIC_BASE_URL}IMG_20261126_131003.png",
        video = "${GITHUB_PUBLIC_BASE_URL}alioss_19_2026030523341815130~2.mp4", videoStyle = "fade", sideFade = true),
    GiftPickerGift(2003, "Castle Night", 85_000_000, "${GITHUB_PUBLIC_BASE_URL}IMG_20261124_165043.png",
        video = "${GITHUB_PUBLIC_BASE_URL}alioss_29_2026032620350482882~2.mp4", videoStyle = "fade", sideFade = true),
)

private val standardGifts = listOf(
    GiftPickerGift(2101, "Apple", 999, "${GITHUB_PUBLIC_BASE_URL}IMG_20261124_162823.png", noMask = true),
    GiftPickerGift(2102, "Cherry", 1299, "${GITHUB_PUBLIC_BASE_URL}IMG_20261124_162846.png", noMask = true),
    GiftPickerGift(2103, "Strawberry", 1500, "${GITHUB_PUBLIC_BASE_URL}IMG_20261124_162906.png", noMask = true),
    GiftPickerGift(2104, "Banana", 1599, "${GITHUB_PUBLIC_BASE_URL}IMG_20261124_162922.png", noMask = true),
    GiftPickerGift(2105, "Avocado", 2500, "${GITHUB_PUBLIC_BASE_URL}IMG_20261124_162944.png", noMask = true),
    GiftPickerGift(2106, "Mango", 2599, "${GITHUB_PUBLIC_BASE_URL}IMG_20261124_163008.png", noMask = true),
    GiftPickerGift(2107, "Papaya", 1400, "${GITHUB_PUBLIC_BASE_URL}IMG_20261124_163037.png", noMask = true),
    GiftPickerGift(2108, "Watermelon", 3000, "${GITHUB_PUBLIC_BASE_URL}IMG_20261124_163056.png", noMask = true),
    GiftPickerGift(2109, "Lucky Star", 1250, "${GITHUB_PUBLIC_BASE_URL}IMG_20261124_163119.png", noMask = true),
    GiftPickerGift(2110, "Bell", 4999, "${GITHUB_PUBLIC_BASE_URL}IMG_20261124_163139.png", noMask = true),
    GiftPickerGift(2111, "Love Lock", 3000, "${GITHUB_PUBLIC_BASE_URL}IMG_20261124_163202.png", noMask = true),
    GiftPickerGift(2112, "Crystal", 5999, "${GITHUB_PUBLIC_BASE_URL}IMG_20261124_163228.png", noMask = true),
    GiftPickerGift(2113, "Tea Pot", 2000, "${GITHUB_PUBLIC_BASE_URL}IMG_20261124_163256.png", noMask = true),
    GiftPickerGift(2114, "Shisha", 7000, "${GITHUB_PUBLIC_BASE_URL}IMG_20261124_163315.png", noMask = true),
    GiftPickerGift(2115, "Sword", 7250, "${GITHUB_PUBLIC_BASE_URL}IMG_20261124_163337.png", noMask = true),
)

private val tabs = listOf("Hot", "Luxury", "Lucky", "Event")

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
    var selectedGiftTab by remember { mutableStateOf<String?>(null) }
    var sending by remember { mutableStateOf(false) }

    // A selection belongs to the tab where it was made. Clear it immediately
    // when switching tabs so an old gift can never remain sendable/displayed.
    LaunchedEffect(activeTab) {
        selectedGift = null
        selectedGiftTab = null
    }
    var isWalletOpen by remember { mutableStateOf(false) }

    var showInsufficient by remember { mutableStateOf(false) }
    var insufficientText by remember { mutableStateOf("Insufficient Balance") }

    var selectedTargets by remember { mutableStateOf<List<String>>(emptyList()) }
    var recipientSelectionTouched by remember(directMode, directRecipient) { mutableStateOf(false) }
    var recipientMode by remember(directMode, directRecipient) {
        mutableStateOf(if (directMode && directRecipient != null) RecipientMode.SINGLE else RecipientMode.MIC)
    }

    LaunchedEffect(open, seats, directMode, directRecipient) {
        if (open && !directMode && !recipientSelectionTouched) {
            selectedTargets = seats.filter { it.occupied && it.accountId.isNotBlank() }
                .map { it.accountId }.distinct()
            recipientMode = RecipientMode.MIC
        }
    }

    var playingVideo by remember { mutableStateOf<Triple<String, String, Boolean>?>(null) }

    val scope = rememberCoroutineScope()

    val currentGifts = when (activeTab) {
        "Lucky" -> standardGifts
        "Hot" -> hotGifts
        "Luxury" -> luxuryGifts
        else -> emptyList()
    }

    val parseMultiplier: (String) -> Int = { m -> m.toIntOrNull() ?: 1 }
    // Keep the live gift amount derived directly from selection state. The
    // recipient snapshots are memoized separately so changing the multiplier
    // does not repeatedly rebuild room/user lookups on every rapid tap.
    val selectedGiftObj = remember(currentGifts, selectedGift) {
        currentGifts.firstOrNull { it.id == selectedGift }
    }
    val multiplierValue = remember(selectedMultiplier) { parseMultiplier(selectedMultiplier) }
    val totalCost = remember(selectedGiftObj, multiplierValue) {
        selectedGiftObj?.let { it.coins * multiplierValue } ?: 0
    }

    val roomUsersById = remember(roomUsers) { roomUsers.associateBy { it.accountId } }
    val seatsById = remember(seats) { seats.associateBy { it.accountId } }
    val recipientIds = remember(directMode, directRecipient, recipientMode, selectedTargets, roomUsers, seats) {
        when {
            directMode && directRecipient != null -> listOf(directRecipient.accountId)
            recipientMode == RecipientMode.ROOM ->
                roomUsers.map { it.accountId } + seats.filter { it.occupied }.map { it.accountId }
            else -> selectedTargets
        }.asSequence().filter { it.isNotBlank() }.distinct().toList()
    }
    val recipientCount = recipientIds.size
    val firstRecipient = remember(directMode, directRecipient, recipientIds, roomUsersById, seatsById) {
        if (directMode) directRecipient else recipientIds.firstNotNullOfOrNull { id ->
            roomUsersById[id]?.let {
                GiftPickerRoomUser(it.accountId, it.name, it.image)
            } ?: seatsById[id]?.let {
                GiftPickerRoomUser(it.accountId, it.userName, it.userImage)
            }
        }
    }
    val totalSendCost = remember(totalCost, recipientCount) {
        totalCost.toLong() * max(1, recipientCount)
    }
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
        if (isWalletOpen || sending) return
        // Require a fresh, explicit selection from the currently visible tab.
        if (selectedGiftTab != activeTab || selectedGiftObj == null) return
        if (recipientCount == 0) { triggerInsufficient("Select the user", false); return }
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
        val resolvedGiftImage = giftAssetUrl(giftToSend.image)
        val resolvedGiftVideo = giftToSend.video?.let { giftAssetUrl(it) }

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
                    "giftImage" to resolvedGiftImage,
                    "giftType" to tabToSend,
                    "senderName" to currentUserName,
                    "senderImage" to currentUserImage,
                    "recipientName" to (firstRecipientToSend?.name ?: "User"),
                    "recipientImage" to (firstRecipientToSend?.image ?: "/default-avatar.png"),
                    "multiplier" to multiplierToSend,
                    "luckyGift" to isLuckyToSend,
                    "luckyImage" to resolvedGiftImage,
                    "transferId" to uniqueTransferId,
                    "eventId" to uniqueTransferId,
                    "timestamp" to System.currentTimeMillis(),
                    "recipientMode" to recipientMode.name.lowercase(),
                ))

                if (resolvedGiftVideo != null && !isLuckyToSend) {
                    onSendGiftVideoPlay(mapOf(
                        "roomId" to roomId,
                        "senderId" to currentUserAccountId,
                        "giftName" to giftToSend.name,
                        "video" to resolvedGiftVideo,
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

            if (resolvedGiftVideo != null) {
                playingVideo = Triple(resolvedGiftVideo, giftToSend.videoStyle ?: "fade", giftToSend.greenRemove)
            } else {
                sending = false
                onClose()
            }
        }
    }

    // ============================================================
    // VIDEO OVERLAY
    // ============================================================
    playingVideo?.let { (src, style, greenRemove) ->
        VideoEffectOverlay(
            src = src,
            style = style,
            greenRemove = greenRemove,
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
                                                            recipientSelectionTouched = true
                                                            val nextTargets = when {
                                                                recipientMode == RecipientMode.MIC && selectedTargets.size > 1 -> listOf(uid)
                                                                selected -> selectedTargets.filter { it != uid }
                                                                else -> selectedTargets + uid
                                                            }
                                                            selectedTargets = nextTargets
                                                            recipientMode = if (nextTargets.size > 1) RecipientMode.MIC else RecipientMode.SINGLE
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
                                        recipientSelectionTouched = true
                                        selectedTargets = seats.filter { it.occupied && it.accountId.isNotBlank() }.map { it.accountId }.distinct()
                                        recipientMode = RecipientMode.MIC
                                    }, iconType = RecipientIconType.MIC)
                                    RecipientButton(isActive = recipientMode == RecipientMode.ROOM, onClick = {
                                        recipientSelectionTouched = true
                                        selectedTargets = (roomUsers.map { it.accountId } + seats.filter { it.occupied }.map { it.accountId })
                                            .filter { it.isNotBlank() }.distinct()
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
                                verticalArrangement = Arrangement.spacedBy(10.dp),
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
                                        onClick = {
                                            selectedGift = gift.id
                                            selectedGiftTab = activeTab
                                        },
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
                                modifier = Modifier.clickable { isWalletOpen = true }.offset(y = (-2).dp),
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
                                                .clickable(enabled = selectedGift != null && selectedGiftTab == activeTab && selectedGiftObj != null && !sending) {
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
                        color = Color.Black.copy(alpha = if (insufficientText == "Select the user") 0.30f else 0.75f),
                        shape = RoundedCornerShape(50),
                    ) {
                        Text(
                            text = insufficientText,
                            color = Color.White,
                            fontSize = if (insufficientText == "Select the user") 11.sp else 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
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
            .padding(start = 2.dp, end = 2.dp, top = 5.dp, bottom = 5.dp),
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
            val size = if (gift.noMask) 45.dp else 60.dp
            val isHotCircle = activeTab == "Hot" && (gift.id == 1 || gift.id == 3)
            val isFitNoFade = gift.id == 2005 || gift.fitNoFade
            val isKittyCat = gift.id == 2006
            val isDesertAura = gift.id == 2009
            val isImageEdgeFade = (activeTab == "Hot" || activeTab == "Luxury") && !isFitNoFade && !isKittyCat

            Box(
                Modifier
                    .size(size)
                    .clip(
                        when {
                            isHotCircle -> CircleShape
                            isFitNoFade -> RoundedCornerShape(0.dp)
                            gift.sideFade -> RoundedCornerShape(12.dp)
                            else -> RoundedCornerShape(0.dp)
                        }
                    )
                    .then(
                        if (isImageEdgeFade) {
                            Modifier
                                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                                .drawWithContent {
                                    drawContent()
                                    if (isHotCircle) {
                                        drawRect(
                                            brush = Brush.radialGradient(
                                                0.82f to Color.White,
                                                1.0f to Color.Transparent,
                                            ),
                                            blendMode = BlendMode.DstIn,
                                        )
                                    } else {
                                        val edgeFade = if (isDesertAura) 0.05f else 0.12f
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
                                }
                        } else Modifier
                    ),
            ) {
                AsyncImage(
                    model = giftAssetUrl(gift.image),
                    contentDescription = gift.name,
                    contentScale = if (gift.noMask || isFitNoFade) ContentScale.Fit else ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
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
                    .padding(top = 4.dp),
            )

            Row(
                modifier = Modifier.padding(top = 2.dp, bottom = 1.dp),
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

        if (activeTab == "Luxury" || (activeTab == "Hot" && (gift.id == 2001 || gift.id == 2016))) {
            Column(
                modifier = Modifier.align(Alignment.TopStart),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AsyncImage(
                    model = giftAssetUrl("/IMG_20261126_214358.png"),
                    contentDescription = "Mic",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(12.dp),
                )
                AsyncImage(
                    model = giftAssetUrl("/IMG_20261126_214430.png"),
                    contentDescription = "Effect",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(12.dp),
                )
                AsyncImage(
                    model = giftAssetUrl("/IMG_20261126_214516.png"),
                    contentDescription = "3D",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(12.dp),
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
// GREEN SCREEN CHROMA KEY VIDEO RENDERER
// ============================================================
private fun isGreenScreenGiftVideo(url: String): Boolean {
    return url.contains("gemini_generated_video_33912b4f~2") ||
        url.contains("gemini_generated_video_53f32fc7~3") ||
        url.contains("gemini_generated_video_e1f4d16e~2") ||
        url.contains("gemini_generated_video_b14f3d64~2") ||
        url.contains("gemini_generated_video_ab36b2de~2") ||
        url.contains("gemini_generated_video_60f28193~2") ||
        url.contains("gemini_generated_video_9da434d9~2") ||
        url.contains("gemini_generated_video_87dc00f7~2")
}

private class ChromaKeyVideoRenderer(
    private val onVideoSurfaceReady: (Surface) -> Unit,
) : TextureView.SurfaceTextureListener {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var glThread: HandlerThread? = null
    private var glHandler: Handler? = null

    private var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
    private var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE

    private var program = 0
    private var oesTextureId = 0
    private var aPositionLoc = -1
    private var aTexCoordLoc = -1
    private var uTexMatrixLoc = -1
    private var uTextureLoc = -1

    private var videoSurfaceTexture: SurfaceTexture? = null
    private var videoSurface: Surface? = null

    private var viewportWidth = 0
    private var viewportHeight = 0
    private val stMatrix = FloatArray(16)

    @Volatile
    private var released = false

    private val vertexBuffer: FloatBuffer = ByteBuffer
        .allocateDirect(8 * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .apply {
            put(
                floatArrayOf(
                    -1f, -1f,
                    1f, -1f,
                    -1f, 1f,
                    1f, 1f,
                )
            )
            position(0)
        }

    private val texCoordBuffer: FloatBuffer = ByteBuffer
        .allocateDirect(8 * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .apply {
            put(
                floatArrayOf(
                    0f, 0f,
                    1f, 0f,
                    0f, 1f,
                    1f, 1f,
                )
            )
            position(0)
        }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        if (released) return
        viewportWidth = width
        viewportHeight = height
        val thread = HandlerThread("GiftChromaKeyGL").apply { start() }
        glThread = thread
        val handler = Handler(thread.looper)
        glHandler = handler
        handler.post {
            if (!released) {
                initGl(surface)
            }
        }
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        glHandler?.post {
            viewportWidth = width
            viewportHeight = height
            if (eglDisplay != EGL14.EGL_NO_DISPLAY && eglSurface != EGL14.EGL_NO_SURFACE) {
                GLES20.glViewport(0, 0, viewportWidth, viewportHeight)
            }
        }
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        release()
        return true
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit

    private fun initGl(outputSurfaceTexture: SurfaceTexture) {
        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (eglDisplay == EGL14.EGL_NO_DISPLAY) return
        val version = IntArray(2)
        if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) return

        val configAttribs = intArrayOf(
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,
            EGL14.EGL_NONE,
        )
        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)
        if (!EGL14.eglChooseConfig(eglDisplay, configAttribs, 0, configs, 0, 1, numConfigs, 0) ||
            numConfigs[0] <= 0 || configs[0] == null
        ) return

        val contextAttribs = intArrayOf(
            EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
            EGL14.EGL_NONE,
        )
        eglContext = EGL14.eglCreateContext(eglDisplay, configs[0], EGL14.EGL_NO_CONTEXT, contextAttribs, 0)
        if (eglContext == EGL14.EGL_NO_CONTEXT) return

        val surfaceAttribs = intArrayOf(EGL14.EGL_NONE)
        eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, configs[0], outputSurfaceTexture, surfaceAttribs, 0)
        if (eglSurface == EGL14.EGL_NO_SURFACE) return

        if (!EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) return

        program = createProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        if (program == 0) return
        aPositionLoc = GLES20.glGetAttribLocation(program, "aPosition")
        aTexCoordLoc = GLES20.glGetAttribLocation(program, "aTexCoord")
        uTexMatrixLoc = GLES20.glGetUniformLocation(program, "uTexMatrix")
        uTextureLoc = GLES20.glGetUniformLocation(program, "uTexture")

        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        oesTextureId = textures[0]
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oesTextureId)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

        GLES20.glViewport(0, 0, viewportWidth, viewportHeight)
        GLES20.glClearColor(0f, 0f, 0f, 0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        EGL14.eglSwapBuffers(eglDisplay, eglSurface)

        val st = SurfaceTexture(oesTextureId)
        st.setOnFrameAvailableListener({ renderFrame() }, glHandler)
        videoSurfaceTexture = st
        val surface = Surface(st)
        videoSurface = surface

        mainHandler.post {
            if (!released) {
                onVideoSurfaceReady(surface)
            }
        }
    }

    private fun renderFrame() {
        if (released) return
        val st = videoSurfaceTexture ?: return
        if (eglDisplay == EGL14.EGL_NO_DISPLAY || eglSurface == EGL14.EGL_NO_SURFACE) return

        try {
            st.updateTexImage()
            st.getTransformMatrix(stMatrix)
        } catch (_: Exception) {
            return
        }

        GLES20.glViewport(0, 0, viewportWidth, viewportHeight)
        GLES20.glDisable(GLES20.GL_BLEND)
        GLES20.glClearColor(0f, 0f, 0f, 0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        GLES20.glUseProgram(program)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oesTextureId)
        GLES20.glUniform1i(uTextureLoc, 0)
        GLES20.glUniformMatrix4fv(uTexMatrixLoc, 1, false, stMatrix, 0)

        vertexBuffer.position(0)
        GLES20.glEnableVertexAttribArray(aPositionLoc)
        GLES20.glVertexAttribPointer(aPositionLoc, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)

        texCoordBuffer.position(0)
        GLES20.glEnableVertexAttribArray(aTexCoordLoc)
        GLES20.glVertexAttribPointer(aTexCoordLoc, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(aPositionLoc)
        GLES20.glDisableVertexAttribArray(aTexCoordLoc)

        EGL14.eglSwapBuffers(eglDisplay, eglSurface)
    }

    fun release() {
        if (released) return
        released = true
        val handler = glHandler
        val thread = glThread
        glHandler = null
        glThread = null
        if (handler != null) {
            handler.post {
                releaseGl()
                thread?.quitSafely()
            }
        } else {
            thread?.quitSafely()
        }
    }

    private fun releaseGl() {
        try {
            videoSurfaceTexture?.setOnFrameAvailableListener(null)
            videoSurface?.release()
            videoSurface = null
            videoSurfaceTexture?.release()
            videoSurfaceTexture = null

            if (oesTextureId != 0) {
                GLES20.glDeleteTextures(1, intArrayOf(oesTextureId), 0)
                oesTextureId = 0
            }
            if (program != 0) {
                GLES20.glDeleteProgram(program)
                program = 0
            }
            if (eglDisplay != EGL14.EGL_NO_DISPLAY) {
                EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
                if (eglSurface != EGL14.EGL_NO_SURFACE) {
                    EGL14.eglDestroySurface(eglDisplay, eglSurface)
                    eglSurface = EGL14.EGL_NO_SURFACE
                }
                if (eglContext != EGL14.EGL_NO_CONTEXT) {
                    EGL14.eglDestroyContext(eglDisplay, eglContext)
                    eglContext = EGL14.EGL_NO_CONTEXT
                }
                EGL14.eglTerminate(eglDisplay)
                eglDisplay = EGL14.EGL_NO_DISPLAY
            }
        } catch (_: Exception) {
        }
    }

    private fun createProgram(vertexSource: String, fragmentSource: String): Int {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource)
        if (vertexShader == 0) return 0
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
        if (fragmentShader == 0) {
            GLES20.glDeleteShader(vertexShader)
            return 0
        }
        val prog = GLES20.glCreateProgram()
        GLES20.glAttachShader(prog, vertexShader)
        GLES20.glAttachShader(prog, fragmentShader)
        GLES20.glLinkProgram(prog)
        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(prog, GLES20.GL_LINK_STATUS, linkStatus, 0)
        GLES20.glDeleteShader(vertexShader)
        GLES20.glDeleteShader(fragmentShader)
        if (linkStatus[0] != GLES20.GL_TRUE) {
            GLES20.glDeleteProgram(prog)
            return 0
        }
        return prog
    }

    private fun loadShader(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        val compiled = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == 0) {
            GLES20.glDeleteShader(shader)
            return 0
        }
        return shader
    }

    companion object {
        private const val VERTEX_SHADER = """
            attribute vec4 aPosition;
            attribute vec4 aTexCoord;
            uniform mat4 uTexMatrix;
            varying vec2 vTexCoord;
            void main() {
                gl_Position = aPosition;
                vTexCoord = (uTexMatrix * aTexCoord).xy;
            }
        """

        private const val FRAGMENT_SHADER = """
            #extension GL_OES_EGL_image_external : require
            precision mediump float;
            uniform samplerExternalOES uTexture;
            varying vec2 vTexCoord;
            void main() {
                vec4 color = texture2D(uTexture, vTexCoord);
                float maxRB = max(color.r, color.b);
                float greenExcess = color.g - maxRB;
                float alpha = 1.0 - smoothstep(0.06, 0.18, greenExcess);
                float spill = smoothstep(0.02, 0.14, greenExcess);
                vec3 rgb = vec3(
                    color.r,
                    mix(color.g, min(color.g, maxRB * 0.96 + 0.02), spill),
                    color.b
                );
                gl_FragColor = vec4(rgb * alpha, alpha);
            }
        """
    }
}

// ============================================================
// VIDEO OVERLAY
// ============================================================
@Composable
internal fun VideoEffectOverlay(
    src: String,
    style: String,
    onFinished: () -> Unit,
    onStarted: () -> Unit,
    greenRemove: Boolean = false,
    forceNoGreenRemove: Boolean = false,
) {
    val context = LocalContext.current
    val resolvedUrl = giftAssetUrl(src)
    val isFade = style == "fade"
    val isGreenRemove = !forceNoGreenRemove && (greenRemove || isGreenScreenGiftVideo(resolvedUrl))
    var videoRatio by remember(resolvedUrl) { mutableStateOf<Float?>(null) }
    val latestOnFinished by rememberUpdatedState(onFinished)

    val player = remember(context, resolvedUrl) {
        val appContext = context.applicationContext
        val httpFactory = DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(30_000)
            .setAllowCrossProtocolRedirects(true)
        val cacheFactory = CacheDataSource.Factory()
            .setCache(SeatEmojiVideoCache.get(appContext))
            .setUpstreamDataSourceFactory(DefaultDataSource.Factory(appContext, httpFactory))
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        var playbackRetryCount = 0
        ExoPlayer.Builder(appContext)
            .setMediaSourceFactory(DefaultMediaSourceFactory(cacheFactory))
            .build()
            .apply {
                setMediaItem(MediaItem.fromUri(resolvedUrl))
                repeatMode = Player.REPEAT_MODE_OFF
                playWhenReady = true
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_ENDED) latestOnFinished()
                    }
                    override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                        if (videoSize.width > 0 && videoSize.height > 0) {
                            videoRatio = videoSize.width.toFloat() / videoSize.height.toFloat()
                        }
                    }
                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        if (playbackRetryCount < 1) {
                            playbackRetryCount++
                            seekTo(0)
                            prepare()
                            playWhenReady = true
                        } else {
                            latestOnFinished()
                        }
                    }
                })
                prepare()
            }
    }

    val chromaRenderer = remember(player, isGreenRemove) {
        if (isGreenRemove) {
            ChromaKeyVideoRenderer { surface ->
                player.setVideoSurface(surface)
            }
        } else {
            null
        }
    }

    val textureView = remember(player, chromaRenderer) {
        TextureView(context).apply {
            isOpaque = false
            if (chromaRenderer != null) {
                surfaceTextureListener = chromaRenderer
            } else {
                player.setVideoTextureView(this)
            }
        }
    }

    LaunchedEffect(resolvedUrl) { onStarted() }

    val isKing = resolvedUrl.contains("17e19680~2")
    val isMysaloPrincess = resolvedUrl.contains("2026031617445981861~2")
    val isCastleNight = resolvedUrl.contains("2026032620350482882~2")
    val isHeartLamp = resolvedUrl.contains("2026032011584793951~2")
    val isDesertAura = resolvedUrl.contains("2025102210290741641~2")
    val isLuxuryGift = luxuryGifts.any { it.video != null && resolvedUrl == giftAssetUrl(it.video) } ||
        resolvedUrl.contains("2025121214091492750~2")
    val isBottomAligned = isHeartLamp || isDesertAura || isLuxuryGift

    Box(
        Modifier.fillMaxSize().background(Color.Transparent),
        contentAlignment = if (isBottomAligned) Alignment.BottomCenter else Alignment.Center,
    ) {
        AndroidView(
            factory = { textureView },
            modifier = Modifier
                .fillMaxWidth()
                .then(if (videoRatio != null) Modifier.aspectRatio(videoRatio!!) else Modifier.fillMaxSize())
                .graphicsLayer {
                    // Offscreen compositing can hide TextureView-backed ExoPlayer output
                    // inside small room notification banners. Only use it for effects
                    // that actually need Compose blending/masking.
                    if (isFade || style == "pure") {
                        compositingStrategy = CompositingStrategy.Offscreen
                    }
                    if (style == "pure") blendMode = BlendMode.Plus
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
                                0.0f to Color.Transparent,
                                topFadeStop to Color.White,
                                bottomFadeStop to Color.White,
                                1.0f to Color.Transparent
                            ),
                            blendMode = BlendMode.DstIn
                        )
                    }
                }
        )
    }

    DisposableEffect(player, textureView, chromaRenderer) {
        onDispose {
            if (chromaRenderer != null) {
                player.clearVideoSurface()
                textureView.surfaceTextureListener = null
                chromaRenderer.release()
            } else {
                player.clearVideoTextureView(textureView)
            }
            player.stop()
            player.clearMediaItems()
            player.release()
        }
    }
}

@Composable
fun RoomGiftVideoOverlay(src: String, onFinished: () -> Unit) {
    val resolved = giftAssetUrl(src)
    VideoEffectOverlay(
        src = resolved,
        // Room banners need a direct TextureView surface; the fade-mask offscreen
        // layer can prevent the video surface from appearing in this overlay.
        style = "plain",
        // Keep notification playback on ExoPlayer's direct TextureView path.
        // The custom GL chroma SurfaceTexture renderer is not reliable in this
        // short-lived room banner and can leave the video completely invisible.
        greenRemove = isGreenScreenGiftVideo(resolved),
        onFinished = onFinished,
        onStarted = {},
    )
}

package com.hurry.voicechat

import android.content.Intent
import android.net.Uri
import android.media.MediaMetadataRetriever
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.withContext
import androidx.compose.ui.graphics.asImageBitmap
import android.os.Handler
import android.os.Looper
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import org.json.JSONArray
import org.json.JSONObject
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.basicMarquee
import coil3.compose.AsyncImage
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

// Copy only after a deliberate two-second hold; release/cancel before then does nothing.
internal fun Modifier.twoSecondLongPress(onLongPress: () -> Unit): Modifier = pointerInput(onLongPress) {
    detectTapGestures(
        onPress = {
            val released = withTimeoutOrNull(1_000L) { tryAwaitRelease() }
            if (released == null) onLongPress()
        }
    )
}

// Room Admin Tag image kept in code for future use
const val ROOM_ADMIN_TAG = "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/IMG_20261107_154649.png"

// Optimized Shader-like CPU logic to prevent room hanging
@Composable
fun TransparentGreenImage(url: String, modifier: Modifier = Modifier) {
    var bitmap by remember(url) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(url) {
        bitmap = withContext(Dispatchers.IO) {
            runCatching {
                val connection = (java.net.URL(url).openConnection() as java.net.HttpURLConnection).apply {
                    connectTimeout = 7_000
                    readTimeout = 9_000
                    useCaches = true
                }
                val source = try {
                    connection.inputStream.use { android.graphics.BitmapFactory.decodeStream(it) }
                } finally {
                    connection.disconnect()
                } ?: return@runCatching null
                val result = source.copy(android.graphics.Bitmap.Config.ARGB_8888, true)
                val width = result.width
                val height = result.height
                val pixels = IntArray(width * height)
                result.getPixels(pixels, 0, width, 0, 0, width, height)
                for (i in pixels.indices) {
                    val color = pixels[i]
                    val r = (color shr 16) and 0xFF
                    val g = (color shr 8) and 0xFF
                    val b = color and 0xFF
                    if (g > 90 && g > r * 1.1 && g > b * 1.1) pixels[i] = android.graphics.Color.TRANSPARENT
                }
                result.setPixels(pixels, 0, width, 0, 0, width, height)
                result
            }.getOrNull()
        }
    }
    bitmap?.let { Image(bitmap = it.asImageBitmap(), contentDescription = null, modifier = modifier, contentScale = ContentScale.Fit) }
}

@Composable
fun SystemBellIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val scaleX = size.width / 512f
        val scaleY = size.height / 512f
        drawCircle(color = Color(0xFF1685FF), radius = 65f * scaleX, center = Offset(256f * scaleX, 256f * scaleY))
        val bellPath = Path().apply {
            moveTo(256f * scaleX, 229f * scaleY)
            cubicTo(246f * scaleX, 229f * scaleY, 241f * scaleX, 238f * scaleY, 241f * scaleX, 249f * scaleY)
            lineTo(241f * scaleX, 258f * scaleY)
            cubicTo(241f * scaleX, 267f * scaleY, 237f * scaleX, 273f * scaleY, 232f * scaleX, 279f * scaleY)
            quadraticBezierTo(230f * scaleX, 282f * scaleY, 235f * scaleX, 282f * scaleY)
            lineTo(277f * scaleX, 282f * scaleY)
            quadraticBezierTo(282f * scaleX, 282f * scaleY, 280f * scaleX, 279f * scaleY)
            cubicTo(275f * scaleX, 273f * scaleY, 271f * scaleX, 267f * scaleY, 271f * scaleX, 258f * scaleY)
            lineTo(271f * scaleX, 249f * scaleY)
            cubicTo(271f * scaleX, 238f * scaleY, 266f * scaleX, 229f * scaleY, 256f * scaleX, 229f * scaleY)
            close()
        }
        drawPath(bellPath, color = Color.White)
        drawOval(color = Color.White, topLeft = Offset((256f - 5f) * scaleX, (288f - 3.5f) * scaleY), size = androidx.compose.ui.geometry.Size(10f * scaleX, 7f * scaleY))
    }
}

@Composable
private fun ThickBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 6.dp,
    activeColor: Color = Color(0xFF3B82F6),
    inactiveColor: Color = Color.White.copy(alpha = 0.15f),
    onProgressChange: ((Float) -> Unit)? = null
) {
    var barWidth by remember { mutableIntStateOf(0) }
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(inactiveColor)
            .onSizeChanged { barWidth = it.width }
            .then(
                if (onProgressChange != null) {
                    Modifier
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                if (barWidth > 0) onProgressChange((offset.x / barWidth).coerceIn(0f, 1f))
                            }
                        }
                        .pointerInput(Unit) {
                            detectDragGestures { change, _ ->
                                if (barWidth > 0) onProgressChange((change.position.x / barWidth).coerceIn(0f, 1f))
                            }
                        }
                } else Modifier
            )
    ) {
        Box(
            Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(RoundedCornerShape(height / 2))
                .background(activeColor)
        )
    }
}

data class RoomPageOwner(val id:String="",val uid:String="",val accountId:String="",val name:String="Room",val image:String="",val country:String="")
data class RoomPageCurrentUser(val id:String="",val uid:String="",val accountId:String="",val name:String="User",val image:String="")
data class NativeRoomSeat(val number:Int,val occupied:Boolean=false,val locked:Boolean=false,val muted:Boolean=false,val selfMuted:Boolean=false,val speaking:Boolean=false,val userName:String="",val userImage:String="",val accountId:String="",val gifSrc:String="",val gifTimestamp:Long=0L,val gifDurationMs:Long=5000L,val gifScale:Float=0.8f)
data class NativeMention(val accountId: String, val name: String)
data class NativeRoomMessage(val id:String,val text:String,val sender:String,val senderImage:String,val senderAccountId:String="",val timestamp:Long=System.currentTimeMillis(),val type:String="message",val mentions: List<NativeMention> = emptyList(),val imageUrl:String="")
data class NativeEntryToast(val id:String,val name:String,val image:String,val vehicleVideo:String="",val accountId:String="",val vehicleItemId:String="")
data class NativeRoomKickNotice(val id:String,val targetName:String,val targetImage:String,val actorName:String,val actorImage:String,val actorRole:String,val timestamp:Long=System.currentTimeMillis())

@Composable
fun RoomPage(
    roomOwner: RoomPageOwner,
    currentUser: RoomPageCurrentUser,
    onClose:()->Unit={},
    onOpenPublicProfile:(RoomProfileUser)->Unit={},
    onBack:()->Unit=onClose,
    onKeepRoom:(String,String,String)->Unit={_,_,_->},
    onFollowToggle:(String,Boolean)->Unit={_,_->},
    onRoomSupport:()->Unit={},
    onJoinRoom:(String)->Unit={},
    onKickNotice:(JSONObject)->Unit={},
    isCovered: Boolean = false
) {
    val roomId=roomOwner.id.ifBlank{roomOwner.accountId.ifBlank{roomOwner.uid.ifBlank{"default-room"}}}
    val roomOwnerAccountId = roomOwner.accountId.ifBlank { roomOwner.id }
    val isLogoRoom = roomOwnerAccountId in setOf("100002", "100003", "500002")
    val accountId=currentUser.accountId.ifBlank{currentUser.uid.ifBlank{currentUser.id.ifBlank{"guest"}}}
    val clipboard = LocalClipboardManager.current
    val appContext = LocalContext.current
    // Keep only the last selected room song across process/app restarts.
    // The room session itself is deliberately not restored.
    val lastMusicPrefs = remember { appContext.getSharedPreferences("zyora_last_room_music", android.content.Context.MODE_PRIVATE) }
    val lastMusicKey = "last_track_$accountId"
    val savedRoomTrack = remember(accountId) {
        runCatching {
            val saved = lastMusicPrefs.getString(lastMusicKey, null) ?: return@runCatching null
            val json = JSONObject(saved)
            NativeMusicTrack(
                id = json.optString("id"),
                name = json.optString("name"),
                url = json.optString("url"),
                duration = json.optLong("duration", 0L)
            ).takeIf { it.url.isNotBlank() && it.name.isNotBlank() }
        }.getOrNull()
    }
    fun persistLastRoomTrack(track: NativeMusicTrack?) {
        if (track == null) {
            lastMusicPrefs.edit().remove(lastMusicKey).apply()
        } else {
            lastMusicPrefs.edit().putString(lastMusicKey, JSONObject()
                .put("id", track.id)
                .put("name", track.name)
                .put("url", track.url)
                .put("duration", track.duration)
                .toString()).apply()
        }
    }
    var roomWalletBalance by remember(accountId) { mutableLongStateOf(0L) }
    // Use exactly the same wallet identity priority as CoinsNativePage.
    // The wallet page keys MongoDB balances by Firebase UID first, then accountId.
    // Using accountId here created a separate zero-balance wallet, so Lucky Bag
    // incorrectly rejected sends even while Mine > Wallet showed sufficient coins.
    val canonicalWalletUserId = appContext.getSharedPreferences("hurry_auth", android.content.Context.MODE_PRIVATE)
        .getString("uid", null)?.takeIf { it.isNotBlank() }
        ?: accountId.ifBlank { currentUser.uid.ifBlank { currentUser.id } }

    LaunchedEffect(canonicalWalletUserId) {
        if (canonicalWalletUserId.isBlank() || canonicalWalletUserId == "guest") return@LaunchedEffect
        val serverBalance = withContext(Dispatchers.IO) {
            runCatching {
                val encoded = java.net.URLEncoder.encode(canonicalWalletUserId, "UTF-8")
                val conn = (java.net.URL("https://zyora-server.onrender.com/api/lucky-bag/wallet/" + encoded).openConnection() as java.net.HttpURLConnection).apply {
                    connectTimeout = 10000
                    readTimeout = 10000
                }
                try {
                    if (conn.responseCode !in 200..299) error("Wallet unavailable")
                    JSONObject(conn.inputStream.bufferedReader().use { it.readText() }).optLong("balance", 0L).coerceAtLeast(0L)
                } finally { conn.disconnect() }
            }.getOrNull()
        }
        if (serverBalance != null) roomWalletBalance = serverBalance
    }
    val currentProfileKeys = listOf(currentUser.uid, currentUser.id, currentUser.accountId, accountId).filter { it.isNotBlank() }.distinct()
    var cachedCurrentProfile by remember(currentProfileKeys) { mutableStateOf<JSONObject?>(null) }
    LaunchedEffect(currentProfileKeys) {
        val cached = withContext(Dispatchers.IO) {
            val profileDb = NativeIndexDatabase(appContext)
            currentProfileKeys.firstNotNullOfOrNull { key ->
                runCatching { profileDb.loadProfile(key) }.getOrNull()
            }
        }
        // Render the local snapshot immediately, then reconcile it with the canonical
        // MongoDB profile. Never let a placeholder "User" cache mask a real profile name.
        cachedCurrentProfile = cached
        val remote = runCatching {
            ProfilePersistence.load(
                TargetUser(
                    id = currentUser.id.ifBlank { accountId },
                    uid = currentUser.uid.ifBlank { currentUser.id.ifBlank { accountId } },
                    accountId = accountId,
                    displayAccountNumber = accountId
                )
            )
        }.getOrNull()
        if (remote != null) {
            val remoteName = sequenceOf("name", "displayName", "userName", "nickname")
                .map { remote.optString(it, "").trim() }
                .firstOrNull { it.isNotBlank() && it != "null" && !it.equals("User", ignoreCase = true) }
            val remoteImage = sequenceOf("image", "avatar", "profileImageUrl", "dp", "photoURL")
                .map { remote.optString(it, "").trim() }
                .firstOrNull { it.isNotBlank() && it != "null" && it != "undefined" }
            val merged = JSONObject(cached?.toString() ?: "{}")
            if (remoteName != null) merged.put("name", remoteName)
            if (remoteImage != null) merged.put("image", remoteImage)
            if (remoteName != null || remoteImage != null) {
                withContext(Dispatchers.IO) {
                    val profileDb = NativeIndexDatabase(appContext)
                    currentProfileKeys.forEach { key -> profileDb.saveProfile(key, merged) }
                }
                cachedCurrentProfile = merged
            }
        }
    }
    val roomDisplayName = cachedCurrentProfile?.let { profile ->
        sequenceOf("name", "displayName", "userName", "nickname")
            .map { profile.optString(it, "").trim() }
            .firstOrNull { it.isNotBlank() && it != "null" && !it.equals("User", ignoreCase = true) }
    } ?: currentUser.name.trim().takeIf { it.isNotBlank() && !it.equals("User", ignoreCase = true) } ?: "User"
    val roomDisplayImage = cachedCurrentProfile?.let { profile ->
        sequenceOf("image", "dp", "photo", "photoURL", "photoUrl", "avatar", "avatarUrl", "profileImage", "profileImageUrl", "profile_image", "imageUrl", "picture")
            .map { profile.optString(it, "").trim() }
            .firstOrNull { it.isNotBlank() && it != "null" && it != "undefined" && !it.endsWith("/default-avatar.png", true) }
    } ?: currentUser.image.trim().takeIf { it.isNotBlank() && it != "null" && it != "undefined" && !it.endsWith("/default-avatar.png", true) }
        ?: ZYORA_DEFAULT_AVATAR_URL
    var copiedId by remember { mutableStateOf("") }
    // Room-info-only data is loaded when the sheet is opened, not during room entry.
    // This keeps non-critical member/admin lookups off the room join critical path.
    var showInfo by remember { mutableStateOf(false) }
    var ownerVipId by remember(roomOwnerAccountId) { mutableStateOf("") }
    var ownerVipBadge by remember(roomOwnerAccountId) { mutableStateOf(false) }
    LaunchedEffect(roomOwnerAccountId) {
        val profile = withContext(Dispatchers.IO) {
            runCatching {
                ProfilePersistence.load(TargetUser(
                    id = roomOwnerAccountId, uid = roomOwner.uid.ifBlank { roomOwner.id },
                    accountId = roomOwnerAccountId, name = roomOwner.name, image = roomOwner.image
                ))
            }.getOrNull()
        }
        val candidate = profile?.optString("vipId", "").orEmpty().trim()
        val expiry = profile?.optLong("vipIdExpiresAt", 0L) ?: 0L
        if (candidate.matches(Regex("\\d{1,20}")) && (expiry == 0L || expiry > System.currentTimeMillis()) &&
            profile?.optBoolean("vipIdBadge", false) == true) {
            ownerVipId = candidate
            ownerVipBadge = true
        } else {
            ownerVipId = ""
            ownerVipBadge = false
        }
    }
    var roomName by remember{mutableStateOf(roomOwner.name.ifBlank{"Room"})}
    var roomImage by remember{mutableStateOf(roomOwner.image)}
    var announcement by remember{mutableStateOf("")}
    // Chat visibility is transient; the Room Settings announcement remains persisted.
    var showRoomSettingsAnnouncementInChat by remember(roomId) { mutableStateOf(false) }
    var roomAdminIds by remember(roomId) { mutableStateOf<List<String>>(emptyList()) }
    // Some seat snapshots identify the signed-in user by UID while admin lists use accountId.
    val currentUserSeatIds = setOf(accountId, currentUser.uid, currentUser.id).filter { it.isNotBlank() }.toSet()
    val seatAdminIds = remember(roomAdminIds, currentUserSeatIds) {
        if (roomAdminIds.any { it in currentUserSeatIds }) roomAdminIds + currentUserSeatIds else roomAdminIds
    }
    var locked by remember(roomId) { mutableStateOf(false) }
    var publicMsgOff by remember(roomId) { mutableStateOf(false) }
    var roomPassword by remember(roomId) { mutableStateOf("") }
    var roomAdminUsers by remember(roomId) { mutableStateOf<List<RoomInfoFollower>>(emptyList()) }
    var roomMicMode by remember(roomId) { mutableIntStateOf(appContext.getSharedPreferences("room_settings", android.content.Context.MODE_PRIVATE).getInt("micMode_$roomId", 10)) }
    var roomTheme by remember(roomId) { mutableStateOf(appContext.getSharedPreferences("room_settings", android.content.Context.MODE_PRIVATE).getString("theme_$roomId", "forest-night") ?: "forest-night") }
    var purchasedRoomThemeImage by remember(roomId) { mutableStateOf("") }
    var purchasedRoomThemeExpiresAt by remember(roomId) { mutableLongStateOf(0L) }
    var roomCustomThemeUrl by remember(roomId) {
        mutableStateOf(appContext.getSharedPreferences("room_settings", android.content.Context.MODE_PRIVATE).getString("customTheme_$roomId", "").orEmpty())
    }

    // Built-in themes must never be checked against the paid Store inventory.
    // Previously every ID except Forest Night, Moon Light and Custom was treated
    // as a purchased theme. That made Lantern Tree / Northern Light / Sword Ruins
    // look "unowned" and silently overwrite the saved choice with Forest Night.
    val builtInRoomThemeIds = remember {
        setOf(
            "forest-night", "mood-light", "lantern-tree", "northern-light",
            "sword-ruins", "cornfield-portal", "deer-moonlight",
            "moon-light-new", "dark-night-flower", "custom"
        )
    }

    LaunchedEffect(roomId, roomTheme) {
        purchasedRoomThemeImage = ""
        purchasedRoomThemeExpiresAt = 0L
        if (roomTheme.isNotBlank() && roomTheme !in builtInRoomThemeIds) {
            val selectedThemeId = roomTheme
            // Distinguish a confirmed missing/expired purchase from a network or
            // server error. A temporary inventory failure must not erase a theme.
            val inventoryLookup = withContext(Dispatchers.IO) {
                runCatching {
                    val encoded = java.net.URLEncoder.encode(roomOwner.accountId.ifBlank { roomOwner.id.ifBlank { roomOwner.uid } }, "UTF-8")
                    val conn = (java.net.URL("https://zyora-server.onrender.com/api/store/inventory/$encoded").openConnection() as java.net.HttpURLConnection).apply {
                        connectTimeout = 8000
                        readTimeout = 8000
                        useCaches = false
                        setRequestProperty("Cache-Control", "no-cache, no-store")
                        setRequestProperty("Pragma", "no-cache")
                    }
                    try {
                        if (conn.responseCode !in 200..299) error("Theme inventory unavailable")
                        val root = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                        val items = root.optJSONArray("items") ?: JSONArray()
                        (0 until items.length()).map { items.getJSONObject(it) }
                            .firstOrNull { it.optString("itemId") == selectedThemeId && it.optString("tab") == "Theme" && it.optLong("expiresAt") > System.currentTimeMillis() }
                            ?.let { it.optString("image", "") to it.optLong("expiresAt") }
                    } finally { conn.disconnect() }
                }
            }
            if (inventoryLookup.isSuccess) {
                val purchased = inventoryLookup.getOrNull()
                if (purchased == null || purchased.first.isBlank()) {
                    if (roomTheme == selectedThemeId) {
                        roomTheme = "forest-night"
                        appContext.getSharedPreferences("room_settings", android.content.Context.MODE_PRIVATE)
                            .edit().putString("theme_$roomId", "forest-night").apply()
                    }
                } else {
                    purchasedRoomThemeImage = purchased.first
                    purchasedRoomThemeExpiresAt = purchased.second
                    delay((purchased.second - System.currentTimeMillis()).coerceAtLeast(0L))
                    if (roomTheme == selectedThemeId && purchasedRoomThemeExpiresAt == purchased.second) {
                        roomTheme = "forest-night"
                        purchasedRoomThemeImage = ""
                        purchasedRoomThemeExpiresAt = 0L
                        appContext.getSharedPreferences("room_settings", android.content.Context.MODE_PRIVATE)
                            .edit().putString("theme_$roomId", "forest-night").apply()
                    }
                }
            }
        }
    }
    
    var luckyGiftNotice by remember(roomId) { mutableStateOf<LuckyNotice?>(null) }
    var showLuckyComboButton by remember(roomId) { mutableStateOf(false) }
    var luckyComboTimeLeft by remember(roomId) { mutableIntStateOf(5) }
    var luckyComboGeneration by remember(roomId) { mutableIntStateOf(0) }
    var lastLuckyComboPayload by remember(roomId) { mutableStateOf<JSONObject?>(null) }
    // A single room-scoped Compose timer owns the combo window. Re-keying it on
    // every new gift or combo tap cancels the previous countdown instead of
    // leaving several Handler callbacks racing to hide the button.
    LaunchedEffect(roomId, luckyComboGeneration) {
        if (!showLuckyComboButton) return@LaunchedEffect
        val activeGeneration = luckyComboGeneration
        for (second in 1..5) {
            kotlinx.coroutines.delay(1000L)
            if (luckyComboGeneration != activeGeneration) return@LaunchedEffect
            luckyComboTimeLeft = 5 - second
        }
        if (luckyComboGeneration == activeGeneration) {
            showLuckyComboButton = false
            luckyComboTimeLeft = 0
        }
    }
    val seenLuckyGiftTransferIds = remember(roomId) { java.util.Collections.synchronizedSet(mutableSetOf<String>()) }
    var showFloatingLuckyBag by remember { mutableStateOf(false) }
    var showLuckyBagCenterReveal by remember { mutableStateOf(false) }
    var showLuckyBagClaimedCard by remember { mutableStateOf(false) }
    var luckyBagSenderName by remember { mutableStateOf("") }
    var luckyBagSenderImage by remember { mutableStateOf("") }
    var luckyBagId by remember { mutableStateOf("") }
    var luckyBagExpiresAt by remember { mutableLongStateOf(0L) }
    var luckyBagSecondsLeft by remember { mutableIntStateOf(0) }
    var luckyBagClaiming by remember { mutableStateOf(false) }
    var luckyBagClaimError by remember { mutableStateOf<String?>(null) }
    var luckyBagClaimAmount by remember { mutableLongStateOf(0L) }

    LaunchedEffect(luckyBagExpiresAt) {
        while (luckyBagExpiresAt > 0L) {
            val remaining = ((luckyBagExpiresAt - System.currentTimeMillis() + 999L) / 1000L).coerceAtLeast(0L)
            luckyBagSecondsLeft = remaining.toInt()
            if (remaining <= 0L) {
                showFloatingLuckyBag = false
                break
            }
            kotlinx.coroutines.delay(1000L)
        }
    }

    LaunchedEffect(roomId, accountId) {
        withContext(Dispatchers.IO) {
            runCatching {
                val encodedRoom = java.net.URLEncoder.encode(roomId, "UTF-8")
                val connection = (java.net.URL("https://zyora-server.onrender.com/api/lucky-bag/room/$encodedRoom").openConnection() as java.net.HttpURLConnection).apply {
                    connectTimeout = 8000
                    readTimeout = 8000
                }
                try {
                    if (connection.responseCode in 200..299) {
                        val root = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                        val bag = root.optJSONObject("bag")
                        if (bag != null) {
                            withContext(Dispatchers.Main) {
                                luckyBagId = bag.optString("bagId")
                                luckyBagExpiresAt = bag.optLong("expiresAt", 0L)
                                luckyBagSenderName = bag.optString("senderName", "User")
                                luckyBagSenderImage = bag.optString("senderImage", "")
                                showFloatingLuckyBag = true
                            }
                        }
                    }
                } finally { connection.disconnect() }
            }
        }
    }

    LaunchedEffect(roomAdminIds, showInfo) {
        if (!showInfo) return@LaunchedEffect
        if (roomAdminIds.isEmpty()) {
            roomAdminUsers = emptyList()
        } else {
            val found = withContext(Dispatchers.IO) {
                roomAdminIds.mapNotNull { adminId ->
                    runCatching {
                        val encoded = java.net.URLEncoder.encode(adminId, "UTF-8")
                        val conn = (java.net.URL("https://zyora-server.onrender.com/api/users?search=$encoded&accountId=$encoded&q=$encoded").openConnection() as java.net.HttpURLConnection).apply {
                            connectTimeout = 2500
                            readTimeout = 2500
                        }
                        try {
                            if (conn.responseCode !in 200..299) return@runCatching null
                            val rootText = conn.inputStream.bufferedReader().use { it.readText() }
                            val root = rootText.trim()
                            val arr = if (root.startsWith("[")) JSONArray(root) else {
                                val obj = JSONObject(root)
                                obj.optJSONArray("users") ?: obj.optJSONArray("results")
                                    ?: JSONArray().apply { obj.optJSONObject("user")?.let { put(it) } }
                            }
                            val user = (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }.firstOrNull { u ->
                                u.optString("accountId", u.optString("accountNumber", u.optString("displayUserNumber", ""))) == adminId ||
                                    u.optString("uid") == adminId || u.optString("id") == adminId
                            }
                            user?.let { u ->
                                RoomInfoFollower(
                                    adminId,
                                    u.optString("name").takeUnless { it == "null" }.orEmpty()
                                        .ifBlank { u.optString("displayName").ifBlank { u.optString("userName", "User") } },
                                    u.optString("image").takeUnless { it == "null" }.orEmpty()
                                        .ifBlank { u.optString("avatar").ifBlank { u.optString("profileImageUrl", "") } }
                                )
                            }
                        } finally { conn.disconnect() }
                    }.getOrNull()
                }
            }
            roomAdminUsers = found
        }
    }

    LaunchedEffect(roomId, accountId) {
        val cachedLock = withContext(Dispatchers.IO) { runCatching { NativeIndexDatabase(appContext).loadRoomLock(roomId) }.getOrNull() }
        if (cachedLock != null) { locked = cachedLock.first; if (accountId == roomOwnerAccountId) roomPassword = cachedLock.second }
        withContext(Dispatchers.IO) {
            runCatching {
                val encoded = java.net.URLEncoder.encode(roomId, "UTF-8")
                val conn = (java.net.URL("https://zyora-server.onrender.com/api/rooms?roomId=$encoded").openConnection() as java.net.HttpURLConnection).apply { connectTimeout=10000; readTimeout=10000 }
                try {
                    if (conn.responseCode in 200..299) {
                        val root = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                        val saved = root.optJSONObject("room") ?: root
                        val name = saved.optString("name", saved.optString("roomName", "")).takeUnless { it == "null" }.orEmpty()
                        val rawRoomImage = sequenceOf("dp", "image", "roomDp", "Room dp", "roomImage", "imageUrl", "avatar", "photo").map { saved.optString(it, "").trim() }.firstOrNull { it.isNotBlank() && it != "null" && it != "undefined" && it != "/default-avatar.png" }.orEmpty()
                        val image = when {
                            rawRoomImage.startsWith("data:image/", true) ->
                                "https://zyora-server.onrender.com/api/rooms/image?roomId=$encoded"
                            rawRoomImage.isBlank() -> ""
                            rawRoomImage.startsWith("/") -> "https://zyora-server.onrender.com$rawRoomImage"
                            else -> rawRoomImage
                        }
                        val message = saved.optString("message", saved.optString("announcement", "")).takeUnless { it == "null" }.orEmpty()
                        val savedMicMode = saved.optInt("micMode", roomMicMode).let { if (it in listOf(5, 10, 15)) it else roomMicMode }
                        val savedTheme = sequenceOf("theme", "roomTheme", "Room Theme").map { saved.optString(it, "").trim() }.firstOrNull { it.isNotBlank() && it != "null" } ?: roomTheme
                        val savedCustomTheme = sequenceOf("customTheme", "themeImage").map { saved.optString(it, "").trim() }.firstOrNull { it.isNotBlank() && it != "null" }.orEmpty()
                        val savedLocked = saved.optBoolean("isLocked", locked)
                        val savedPublicMsgOff = saved.optBoolean("publicMsgOff", false)
                        val savedRoomPassword = saved.optString("roomPassword", roomPassword).takeUnless { it == "null" }.orEmpty()
                        val savedAdminsJson = saved.optJSONArray("admin") ?: saved.optJSONArray("admins")
                        val savedAdmins = savedAdminsJson?.let { adminsJson ->
                            (0 until adminsJson.length()).flatMap { i ->
                                val entry = adminsJson.opt(i)
                                when (entry) {
                                    is JSONObject -> listOf("accountId", "userId", "id", "uid")
                                        .mapNotNull { key -> entry.optString(key).trim().takeIf { it.isNotBlank() && it != "null" } }
                                    is String -> listOf(entry.trim()).filter { it.isNotBlank() && it != "null" }
                                    else -> emptyList()
                                }
                            }.distinct()
                        }
                        launch(Dispatchers.Main) {
                            if (name.isNotBlank()) roomName = name
                            if (!isLogoRoom && image.isNotBlank()) roomImage = image
                            announcement = message
                            showRoomSettingsAnnouncementInChat = message.isNotBlank() && saved.optBoolean("announcementChatVisible", true)
                            publicMsgOff = savedPublicMsgOff
                            roomMicMode = savedMicMode
                            roomTheme = savedTheme
                            if (savedAdmins != null) roomAdminIds = savedAdmins
                            locked = savedLocked
                            roomPassword = if (accountId == roomOwnerAccountId) savedRoomPassword else ""
                            locked = savedLocked
                            roomCustomThemeUrl = savedCustomTheme
                            appContext.getSharedPreferences("room_settings", android.content.Context.MODE_PRIVATE).edit().putBoolean("room_locked_$roomId", savedLocked).putString("room_password_$roomId", if (accountId == roomOwnerAccountId) savedRoomPassword else "").apply()
                            NativeIndexDatabase(appContext).saveRoomLock(roomId, savedLocked, if (accountId == roomOwnerAccountId) savedRoomPassword else "")
                            appContext.getSharedPreferences("room_settings", android.content.Context.MODE_PRIVATE).edit().putInt("micMode_$roomId", savedMicMode).putString("theme_$roomId", savedTheme).putString("customTheme_$roomId", savedCustomTheme).apply()
                        }
                    }
                } finally { conn.disconnect() }
            }
        }
    }
    var followed by remember{mutableStateOf(false)}
    var followedRoomUsers by remember(roomId) { mutableStateOf<List<RoomInfoFollower>>(emptyList()) }
    LaunchedEffect(roomId, showInfo) {
        if (!showInfo) return@LaunchedEffect
        followedRoomUsers = withContext(Dispatchers.IO) {
            runCatching {
                val encodedRoom = java.net.URLEncoder.encode(roomId, "UTF-8")
                val connection = (java.net.URL("https://zyora-server.onrender.com/api/room-follows?roomId=$encodedRoom").openConnection() as java.net.HttpURLConnection).apply {
                    connectTimeout = 10000
                    readTimeout = 10000
                    setRequestProperty("Accept", "application/json")
                }
                try {
                    if (connection.responseCode !in 200..299) error("Room members unavailable")
                    val root = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                    val array = root.optJSONArray("followers") ?: JSONArray()
                    (0 until array.length()).mapNotNull { index ->
                        val item = array.optJSONObject(index) ?: return@mapNotNull null
                        val id = item.optString("userId").trim()
                        if (id.isBlank()) return@mapNotNull null
                        RoomInfoFollower(id, item.optString("followerName").ifBlank { "User" }, item.optString("followerImage"), item.optString("followerCountry"))
                    }.distinctBy { it.accountId }
                } finally { connection.disconnect() }
            }.getOrDefault(emptyList())
        }
    }
    LaunchedEffect(roomId, accountId) {
        if (accountId.isBlank() || accountId == "guest") return@LaunchedEffect
        followed = withContext(Dispatchers.IO) {
            runCatching {
                val url = java.net.URL("https://zyora-server.onrender.com/api/room-follows?userId=" + java.net.URLEncoder.encode(accountId, "UTF-8"))
                val conn = (url.openConnection() as java.net.HttpURLConnection).apply { connectTimeout = 8000; readTimeout = 8000 }
                try {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val follows = JSONObject(body).optJSONArray("follows") ?: JSONArray()
                    (0 until follows.length()).any { follows.optJSONObject(it)?.optString("roomId") == roomId }
                } finally { conn.disconnect() }
            }.getOrDefault(false)
        }
    }
    val roomEffectsPrefs = remember { appContext.getSharedPreferences("hurry_room_tools", android.content.Context.MODE_PRIVATE) }
    val entryEffectPrefKey = "entry_effect_$accountId"
    val giftEffectPrefKey = "gift_effect_$accountId"
    val speakerPrefKey = "speaker_enabled_$accountId"
    // These are device-local preferences keyed by the signed-in account.
    // Never inherit another room member's/room owner's previous room-level toggle.
    var entryEffectEnabled by remember(accountId) { mutableStateOf(roomEffectsPrefs.getBoolean(entryEffectPrefKey, true)) }
    var giftEffectEnabled by remember(accountId) { mutableStateOf(roomEffectsPrefs.getBoolean(giftEffectPrefKey, true)) }
    var speaker by remember(accountId) { mutableStateOf(roomEffectsPrefs.getBoolean(speakerPrefKey, true)) }
    var localVoiceSpeaking by remember { mutableStateOf(false) }
    // Public room messages stay ON unless the owner explicitly disables them.
    var showPublicMessageClosedToast by remember { mutableStateOf(false) }
    var roomToastText by remember { mutableStateOf("") }
    LaunchedEffect(roomToastText) {
        if (roomToastText.isNotBlank()) {
            kotlinx.coroutines.delay(1600)
            roomToastText = ""
        }
    }
    var showRoomOwnerBlockedToast by remember { mutableStateOf(false) }
    val isRoomOwner = accountId == roomOwnerAccountId || roomAdminIds.any { it.trim() in currentUserSeatIds }
    val canSendRoomPublicMessage = isRoomOwner || roomAdminIds.any { it == accountId }
    LaunchedEffect(showPublicMessageClosedToast) {
        if (showPublicMessageClosedToast) {
            kotlinx.coroutines.delay(1800)
            showPublicMessageClosedToast = false
        }
    }
    var showChat by remember{mutableStateOf(false)}
    val latestShowChat by rememberUpdatedState(showChat)
    var showMessageSheet by remember{mutableStateOf(false)}
    val roomUnreadPrefs = remember { appContext.getSharedPreferences("zyora_unread_messages", android.content.Context.MODE_PRIVATE) }
    var roomHasUnreadMessages by remember(accountId) {
        mutableStateOf(roomUnreadPrefs.getInt("unread_$accountId", 0) > 0)
    }
    DisposableEffect(accountId, roomUnreadPrefs) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            if (key?.startsWith("unread_${accountId}_") == true) {
                val prefix = "unread_${accountId}_"
                val total = prefs.all.entries.asSequence()
                    .filter { it.key.startsWith(prefix) }
                    .sumOf { (it.value as? Int ?: 0).coerceAtLeast(0) }
                prefs.edit().putInt("unread_$accountId", total).apply()
                roomHasUnreadMessages = total > 0
            }
        }
        roomUnreadPrefs.registerOnSharedPreferenceChangeListener(listener)
        val prefix = "unread_${accountId}_"
        val total = roomUnreadPrefs.all.entries.asSequence()
            .filter { it.key.startsWith(prefix) }
            .sumOf { (it.value as? Int ?: 0).coerceAtLeast(0) }
        roomUnreadPrefs.edit().putInt("unread_$accountId", total).apply()
        roomHasUnreadMessages = total > 0
        onDispose { roomUnreadPrefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    var selectedOfficialChat by remember { mutableStateOf<Triple<String, String, String>?>(null) }
    var pendingRoomInvitationJson by remember { mutableStateOf<String?>(null) }
    var showMenu by remember{mutableStateOf(false)}
    var showStore by remember { mutableStateOf(false) }
    var storeInitialView by remember { mutableStateOf("store") }
    var showExitMenu by remember{mutableStateOf(false)}
    // The room remains composed while minimized; no socket-only keep/dispose handoff is used.
    var showRoomCup by remember { mutableStateOf(false) }
    var showRoomCupOnline by remember { mutableStateOf(false) }
    var showSettings by remember{mutableStateOf(false)}
    var showGift by remember{mutableStateOf(false)}
    var showEmoji by remember{mutableStateOf(false)}
    var showGames by remember{mutableStateOf(false)}
    var showRoomTask by remember{mutableStateOf(false)}
    var showWildParty by remember{mutableStateOf(false)}
    var wildPartyMinimized by remember { mutableStateOf(false) }
    var showDragonBattle by remember{mutableStateOf(false)}
    var dragonBattleMinimized by remember { mutableStateOf(false) }
    var showSeatSheet by remember{mutableStateOf(false)}
    var selectedChatProfile by remember { mutableStateOf<NativeRoomMessage?>(null) }

    var miniMusicTrack by remember(accountId) { mutableStateOf(savedRoomTrack) }
    // Restore the previous selection into shared controller state without auto-playing it.
    LaunchedEffect(accountId, savedRoomTrack) {
        // Room music selection is account-scoped. Never let a previous room/account's
        // process-global track override this user's saved selection on re-entry.
        MusicPlaybackState.currentTrack = savedRoomTrack
        MusicPlaybackState.currentPlayingUri = null
        MusicPlaybackState.isPlaying = false
    }
    // Controller UI is opt-in for each room visit. Restoring a saved track must not show it.
    var musicControllerVisible by remember { mutableStateOf(false) }
    var musicControllerMinimized by remember { mutableStateOf(false) }
    val musicCardDragOffset = remember { Animatable(0f) }
    val musicDragScope = rememberCoroutineScope()
    var isMusicPlaying by remember { mutableStateOf(false) }
    var musicVolume by remember { mutableFloatStateOf(1f) }
    var repeatOneEnabled by remember { mutableStateOf(false) }
    var showMusicVolumeView by remember { mutableStateOf(false) }
    var openMusicPageFromController by remember { mutableStateOf(false) }
    var musicPlaylist by remember { mutableStateOf<List<NativeMusicTrack>>(emptyList()) }
    var currentTrackIndex by remember { mutableIntStateOf(0) }
    var musicCurrentTime by remember { mutableFloatStateOf(0f) }
    var musicDuration by remember { mutableFloatStateOf(0f) }

    var inputValue by remember { mutableStateOf(TextFieldValue("")) }
    val input = inputValue.text
    var pendingMentions by remember { mutableStateOf<List<NativeMention>>(emptyList()) }
    val chatFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    var keyboardShownForChat by remember { mutableStateOf(false) }

    // Resolve the real duration for tracks whose playlist metadata is missing.
    // Never substitute a hard-coded 3:00 value in the room mini-player.
    LaunchedEffect(miniMusicTrack?.url, miniMusicTrack?.duration) {
        val track = miniMusicTrack ?: return@LaunchedEffect
        if (track.duration > 0L) {
            musicDuration = track.duration / 1000f
        } else {
            musicDuration = 0f
            musicCurrentTime = 0f
            val resolvedDuration = withContext(Dispatchers.IO) {
                runCatching {
                    val retriever = MediaMetadataRetriever()
                    try {
                        val uri = Uri.parse(track.url)
                        if (uri.scheme == "http" || uri.scheme == "https") {
                            retriever.setDataSource(track.url, emptyMap<String, String>())
                        } else {
                            retriever.setDataSource(appContext, uri)
                        }
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                            ?.toLongOrNull() ?: 0L
                    } finally {
                        runCatching { retriever.release() }
                    }
                }.getOrDefault(0L)
            }
            if (miniMusicTrack?.url == track.url && resolvedDuration > 0L) {
                musicDuration = resolvedDuration / 1000f
            }
        }
    }

    LaunchedEffect(isMusicPlaying, miniMusicTrack) {
        while (isMusicPlaying && miniMusicTrack != null) {
            kotlinx.coroutines.delay(1000)
            if (musicDuration > 0f) {
                if (musicCurrentTime < musicDuration) {
                    musicCurrentTime = (musicCurrentTime + 1f).coerceAtMost(musicDuration)
                } else if (repeatOneEnabled) {
                    // MediaPlayer is already looping the same track; keep the UI timer on that track.
                    musicCurrentTime = 0f
                } else if (musicPlaylist.isNotEmpty()) {
                    currentTrackIndex = (currentTrackIndex + 1) % musicPlaylist.size
                    val nextTrack = musicPlaylist[currentTrackIndex]
                    miniMusicTrack = nextTrack
                    persistLastRoomTrack(nextTrack)
                    musicCurrentTime = 0f
                    MusicPlaybackState.currentPlayingUri = nextTrack.url
                    MusicPlaybackState.currentTrack = nextTrack
                    runCatching {
                        val intent = Intent(appContext, RoomMusicService::class.java)
                            .setAction(RoomMusicService.ACTION_PLAY)
                            .putExtra(RoomMusicService.EXTRA_URI, nextTrack.url)
                            .putExtra(RoomMusicService.EXTRA_TITLE, nextTrack.name)
                        ContextCompat.startForegroundService(appContext, intent)
                    }
                } else {
                    isMusicPlaying = false
                    MusicPlaybackState.isPlaying = false
                }
            }
        }
    }

    fun musicPlayPause() {
        val track = miniMusicTrack ?: return
        runCatching {
            if (isMusicPlaying) {
                appContext.startService(Intent(appContext, RoomMusicService::class.java).setAction(RoomMusicService.ACTION_PAUSE))
                isMusicPlaying = false
                MusicPlaybackState.isPlaying = false
            } else {
                val intent = Intent(appContext, RoomMusicService::class.java)
                    .setAction(RoomMusicService.ACTION_PLAY)
                    .putExtra(RoomMusicService.EXTRA_URI, track.url)
                    .putExtra(RoomMusicService.EXTRA_TITLE, track.name)
                ContextCompat.startForegroundService(appContext, intent)
                isMusicPlaying = true
                MusicPlaybackState.currentPlayingUri = track.url
                MusicPlaybackState.currentTrack = track
                MusicPlaybackState.isPlaying = true
            }
        }
    }

    fun musicNext() {
        val tracks = musicPlaylist.ifEmpty { MusicPlaybackState.playlist }
        if (tracks.isEmpty()) return
        val activeUri = miniMusicTrack?.url ?: MusicPlaybackState.currentPlayingUri
        val activeIndex = tracks.indexOfFirst { it.url == activeUri }.let { if (it >= 0) it else currentTrackIndex }
        currentTrackIndex = (activeIndex + 1).mod(tracks.size)
        val nextTrack = tracks[currentTrackIndex]
        miniMusicTrack = nextTrack
        musicCurrentTime = 0f
        musicDuration = if (nextTrack.duration > 0L) nextTrack.duration / 1000f else 0f
        MusicPlaybackState.currentPlayingUri = nextTrack.url
        MusicPlaybackState.currentTrack = nextTrack
        MusicPlaybackState.isPlaying = true
        runCatching {
            ContextCompat.startForegroundService(appContext, Intent(appContext, RoomMusicService::class.java)
                .setAction(RoomMusicService.ACTION_PLAY)
                .putExtra(RoomMusicService.EXTRA_URI, nextTrack.url)
                .putExtra(RoomMusicService.EXTRA_TITLE, nextTrack.name))
        }
        isMusicPlaying = true
    }

    fun musicPrev() {
        val tracks = musicPlaylist.ifEmpty { MusicPlaybackState.playlist }
        if (tracks.isEmpty()) return
        val activeUri = miniMusicTrack?.url ?: MusicPlaybackState.currentPlayingUri
        val activeIndex = tracks.indexOfFirst { it.url == activeUri }.let { if (it >= 0) it else currentTrackIndex }
        currentTrackIndex = (activeIndex - 1 + tracks.size).mod(tracks.size)
        val prevTrack = tracks[currentTrackIndex]
        miniMusicTrack = prevTrack
        musicCurrentTime = 0f
        musicDuration = if (prevTrack.duration > 0L) prevTrack.duration / 1000f else 180f
        MusicPlaybackState.currentPlayingUri = prevTrack.url
        MusicPlaybackState.currentTrack = prevTrack
        MusicPlaybackState.isPlaying = true
        runCatching {
            ContextCompat.startForegroundService(appContext, Intent(appContext, RoomMusicService::class.java)
                .setAction(RoomMusicService.ACTION_PLAY)
                .putExtra(RoomMusicService.EXTRA_URI, prevTrack.url)
                .putExtra(RoomMusicService.EXTRA_TITLE, prevTrack.name))
        }
        isMusicPlaying = true
    }

    fun musicReplayOne() {
        val track = miniMusicTrack ?: return
        repeatOneEnabled = !repeatOneEnabled
        runCatching {
            appContext.startService(Intent(appContext, RoomMusicService::class.java)
                .setAction(RoomMusicService.ACTION_REPEAT_ONE)
                .putExtra(RoomMusicService.EXTRA_REPEAT_ONE, repeatOneEnabled))
            appContext.startService(Intent(appContext, RoomMusicService::class.java)
                .setAction(RoomMusicService.ACTION_SEEK)
                .putExtra(RoomMusicService.EXTRA_POSITION_MS, 0))
            musicCurrentTime = 0f
            if (!isMusicPlaying) {
                ContextCompat.startForegroundService(appContext, Intent(appContext, RoomMusicService::class.java)
                    .setAction(RoomMusicService.ACTION_PLAY)
                    .putExtra(RoomMusicService.EXTRA_URI, track.url)
                    .putExtra(RoomMusicService.EXTRA_TITLE, track.name))
                isMusicPlaying = true
                MusicPlaybackState.currentPlayingUri = track.url
                MusicPlaybackState.currentTrack = track
                MusicPlaybackState.isPlaying = true
            }
        }
    }

    fun musicClose() {
        runCatching {
            appContext.startService(Intent(appContext, RoomMusicService::class.java).setAction(RoomMusicService.ACTION_STOP))
        }
        miniMusicTrack = null
        musicControllerVisible = false
        musicControllerMinimized = false
        persistLastRoomTrack(null)
        isMusicPlaying = false
        MusicPlaybackState.currentPlayingUri = null
        MusicPlaybackState.currentTrack = null
        MusicPlaybackState.isPlaying = false
        musicPlaylist = emptyList()
        musicCurrentTime = 0f
        musicDuration = 0f
    }

    // Leaving a room must stop the audio immediately, but keep the last song
    // selected so the next room can restore its controller without reopening the picker.
    fun stopMusicForRoomExit() {
        runCatching {
            appContext.startService(Intent(appContext, RoomMusicService::class.java).setAction(RoomMusicService.ACTION_STOP))
        }
        isMusicPlaying = false
        musicControllerVisible = false
        musicControllerMinimized = false
        MusicPlaybackState.isPlaying = false
        MusicPlaybackState.currentPlayingUri = null
        miniMusicTrack?.let {
            MusicPlaybackState.currentTrack = it
            persistLastRoomTrack(it)
        }
    }

    BackHandler(enabled = !isCovered) {
        when {
            showLuckyBagClaimedCard -> showLuckyBagClaimedCard = false
            showLuckyBagCenterReveal -> showLuckyBagCenterReveal = false
            showExitMenu -> showExitMenu = false
            showChat -> { showChat = false; keyboardController?.hide() }
            selectedOfficialChat != null -> selectedOfficialChat = null
            showMessageSheet -> showMessageSheet = false
            showMenu -> showMenu = false
            showStore -> showStore = false
            showInfo -> showInfo = false
            showSettings -> showSettings = false
            showGift -> showGift = false
            showEmoji -> showEmoji = false
            showGames -> showGames = false
            showRoomTask -> showRoomTask = false
            showWildParty -> showWildParty = false
            showSeatSheet -> showSeatSheet = false
            else -> showExitMenu = true
        }
    }
    
    var showOfficialAnnouncement by remember { mutableStateOf(true) }
    var clearedBy by remember { mutableStateOf("") }
    // A live kick owns the acknowledgement card; do not replace it with the
    // compact restriction popup if a delayed join-block event arrives.
    val liveKickReceived = remember(roomId, accountId) { java.util.concurrent.atomic.AtomicBoolean(false) }
    val kickNotices = remember { mutableStateListOf<NativeRoomKickNotice>() }
    
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidthPx = remember(configuration, density) { with(density) { configuration.screenWidthDp.dp.roundToPx() } }
    
    // Keep room chat open while the keyboard switches between letters and emoji.
    // The IME can briefly report hidden during that transition; only the close
    // button should dismiss the chat composer.
    val roomSocketRef=remember{mutableStateOf<io.socket.client.Socket?>(null)}

    LaunchedEffect(showChat, roomSocketRef.value) {
        roomSocketRef.value?.takeIf { it.connected() }?.emit(
            "room_chat_view", JSONObject().put("roomId", roomId).put("userId", accountId).put("active", showChat)
        )
        if (showChat) {
            kotlinx.coroutines.delay(120)
            runCatching { chatFocusRequester.requestFocus() }
            keyboardController?.show()
        } else keyboardController?.hide()
    }
    
    var selectedSeat by remember{mutableIntStateOf(0)}
    var showInviteSheet by remember(roomId) { mutableStateOf(false) }
    var showSeatInviteUsers by remember(roomId) { mutableStateOf(false) }
    var incomingSeatInvite by remember(roomId) { mutableStateOf<JSONObject?>(null) }
     LaunchedEffect(incomingSeatInvite?.optString("inviteId")) {
        val activeInviteId = incomingSeatInvite?.optString("inviteId").orEmpty()
        if (activeInviteId.isNotBlank()) {
            kotlinx.coroutines.delay(60_000)
            if (incomingSeatInvite?.optString("inviteId") == activeInviteId) {
                incomingSeatInvite = null
            }
        }
    }

    val messages=remember{mutableStateListOf<NativeRoomMessage>()}
            val chatListState = androidx.compose.foundation.lazy.rememberLazyListState()
            LaunchedEffect(messages.size, imeVisible) { if (messages.isNotEmpty()) chatListState.animateScrollToItem(messages.lastIndex, scrollOffset = 0) }
    val roomImagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val messageId = accountId + "-" + System.currentTimeMillis()
            CoroutineScope(Dispatchers.Main).launch {
                val imageUrl = runCatching { uploadChatImage(appContext, uri, messageId, "room-chat-image") }.getOrNull()
                val activeRoomSocket = roomSocketRef.value
                if (!imageUrl.isNullOrBlank() && activeRoomSocket?.connected() == true) {
                    val createdAt = System.currentTimeMillis()
                    val imageMessage = NativeRoomMessage(
                        id = messageId, text = "📷 Image", sender = roomDisplayName,
                        senderImage = roomDisplayImage, senderAccountId = accountId,
                        timestamp = createdAt, type = "image", imageUrl = imageUrl
                    )
                    activeRoomSocket.emit(
                        "room_message",
                        JSONObject().put("id", messageId).put("roomId", roomId)
                            .put("senderId", accountId).put("senderAccountId", accountId)
                            .put("senderName", roomDisplayName).put("senderAvatar", roomDisplayImage)
                            .put("senderImage", roomDisplayImage).put("text", "📷 Image")
                            .put("type", "image").put("imageUrl", imageUrl).put("createdAt", createdAt)
                    )
                    messages.add(imageMessage)
                } else {
                    android.widget.Toast.makeText(appContext, "Image upload failed. Try an image under 5 MB.", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    val entryToasts=remember{mutableStateListOf<NativeEntryToast>()}
    // Keep the vehicle video as an independent room-screen preview, not inside the entry banner.
    var roomVehiclePreview by remember(roomId) { mutableStateOf("") }
    var roomVehiclePreviewItemId by remember(roomId) { mutableStateOf("") }
    // Deduplicate by the server's entry ID, not by room: every member entry
    // must play on every viewer's device.
    val seenRoomVehicleEntryIds = remember(roomId) { mutableStateListOf<String>() }
    val pendingRoomVehicleEntries = remember(roomId) { mutableStateListOf<Pair<String, String>>() }
    var roomGiftVideoPreview by remember(roomId) { mutableStateOf("") }
    // Turning an effect OFF also dismisses an already-running local preview.
    // This is local-only and does not broadcast a setting to other room members.
    LaunchedEffect(entryEffectEnabled) {
        if (!entryEffectEnabled) {
            roomVehiclePreview = ""
            roomVehiclePreviewItemId = ""
            pendingRoomVehicleEntries.clear()
        }
    }
    LaunchedEffect(giftEffectEnabled) {
        if (!giftEffectEnabled) roomGiftVideoPreview = ""
    }
    val seats=remember{mutableStateListOf<NativeRoomSeat>().apply{for(i in 1..15)add(NativeRoomSeat(i))}}
    // The owner gets one automatic attempt on the first authoritative seat snapshot only.
    val ownerAutoSeatAttempted = remember(roomId, accountId) { java.util.concurrent.atomic.AtomicBoolean(false) }
    // Preserve live speaker state across room_seats snapshots.
    val activeVoiceSpeakerIds=remember(roomId){mutableStateSetOf<String>()}
    val activeMusicSpeakerIds=remember(roomId){mutableStateSetOf<String>()}
    val backendRoomUsers=remember{mutableStateListOf<ActiveUserNative>()}
    val activeRoomUsers = backendRoomUsers.toList()
    // Keep the room socket stable while asynchronously loaded profile/settings state updates.
    // Reading a cached profile must not tear down and reconnect the live room connection.
    val latestRoomDisplayName by rememberUpdatedState(roomDisplayName)
    val latestRoomDisplayImage by rememberUpdatedState(roomDisplayImage)
    val latestRoomName by rememberUpdatedState(roomName)
    val latestRoomImage by rememberUpdatedState(roomImage)
    val latestEntryEffectEnabled by rememberUpdatedState(entryEffectEnabled)
    val latestGiftEffectEnabled by rememberUpdatedState(giftEffectEnabled)

    DisposableEffect(roomId,accountId,roomOwner.id,roomOwner.accountId) {
        val roomSocketOptions = IO.Options().apply {
            transports = arrayOf("websocket")
            reconnection = true
            reconnectionAttempts = Int.MAX_VALUE
            reconnectionDelay = 1000
            reconnectionDelayMax = 30000
            timeout = 20000
        }
        // RoomPage stays composed while minimized, so its original socket is never
        // torn down or re-created merely because another page is shown.
        val roomSocket = IO.socket("https://zyora-server.onrender.com", roomSocketOptions)
        roomSocketRef.value=roomSocket
        val mainHandler = Handler(Looper.getMainLooper())
        // Own room-scoped background work so requests cannot outlive this room instance.
        val roomIoScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + Dispatchers.IO)
        val presenceListener = Emitter.Listener { args ->
            val payload = args.firstOrNull() as? JSONObject
            if (payload != null && payload.optString("roomId") == roomId) {
                val usersJson = payload.optJSONArray("users") ?: JSONArray()
                val parsed = ArrayList<ActiveUserNative>()
                for (index in 0 until usersJson.length()) {
                    val user = usersJson.optJSONObject(index) ?: continue
                    val userId = user.optString("accountId", user.optString("userId"))
                    if (userId.isNotBlank()) {
                        val roomCountry = sequenceOf("flag", "countryName", "country", "countryCode", "country_code")
                            .map { user.optString(it, "").trim() }
                            .firstOrNull { it.isNotBlank() && it != "null" }.orEmpty()
                        parsed.add(ActiveUserNative(
                            accountId=userId,
                            name=user.optString("name","User").ifBlank{"User"},
                            image=sequenceOf("image", "dp", "photo", "photoURL", "photoUrl", "avatar", "avatarUrl", "profileImage", "profileImageUrl", "profile_image", "imageUrl", "picture").map { user.optString(it, "").trim() }.firstOrNull { it.isNotBlank() && it != "null" && it != "undefined" }.orEmpty(),
                            country=roomCountry
                        ))
                    }
                }
                mainHandler.post { backendRoomUsers.clear(); backendRoomUsers.addAll(parsed) }
            }
        }
        val messageListener = Emitter.Listener { args ->
            val payload = args.firstOrNull() as? JSONObject
            if (payload != null && payload.optString("roomId") == roomId) {
                val id=payload.optString("id", "${payload.optString("senderId","user")}-${payload.optLong("createdAt",System.currentTimeMillis())}")
                val type=payload.optString("type","message")
                val senderId = payload.optString("senderAccountId",payload.optString("senderId",""))
                // Never perform synchronous IndexDB/profile disk I/O on the Socket.IO
                // callback thread. Parse and publish the message immediately; persist
                // profile metadata in the room's owned IO scope in the background.
                val remoteSender = payload.optString("senderName","").takeUnless { it == "null" }.orEmpty()
                val remoteImage = payload.optString("senderAvatar",payload.optString("senderImage","")).takeUnless { it == "null" }.orEmpty()
                val sender = remoteSender.ifBlank { senderId.ifBlank { "Unknown" } }
                val image = remoteImage
                if (senderId.isNotBlank() && (remoteSender.isNotBlank() || remoteImage.isNotBlank())) {
                    roomIoScope.launch {
                        runCatching {
                            val profileDb = NativeIndexDatabase(appContext)
                            val updatedProfile = profileDb.loadProfile(senderId) ?: JSONObject()
                            if (remoteSender.isNotBlank()) updatedProfile.put("name", remoteSender)
                            if (remoteImage.isNotBlank()) updatedProfile.put("image", remoteImage)
                            profileDb.saveProfile(senderId, updatedProfile)
                        }
                    }
                }
                val mentionJson = payload.optJSONArray("mentions") ?: JSONArray()
                val parsedMentions = (0 until mentionJson.length()).mapNotNull { index -> mentionJson.optJSONObject(index)?.let { mention -> val targetId = mention.optString("accountId").trim(); val targetName = mention.optString("name").trim(); if (targetId.isNotBlank() && targetName.isNotBlank()) NativeMention(targetId, targetName) else null } }
                val message=NativeRoomMessage(id,payload.optString("text",""),sender,image,senderId,payload.optLong("createdAt",System.currentTimeMillis()),type,parsedMentions,payload.optString("imageUrl"))
                mainHandler.post {
                    if(messages.none{it.id==message.id}) messages.add(message)
                    if(type=="join" && entryToasts.none{it.id==id}) {
                        entryToasts.add(NativeEntryToast(id,sender,image,accountId=senderId))
                        mainHandler.postDelayed({entryToasts.removeAll{it.id==id}},4000)
                    }
                }
            }
        }
        val giftVideoListener = Emitter.Listener { args ->
            val payload = args.firstOrNull() as? JSONObject ?: return@Listener
            if (payload.optString("roomId") != roomId || !latestGiftEffectEnabled) return@Listener
            val video = payload.optString("video").trim()
            if (video.isBlank()) return@Listener
            val videoUrl = if (video.startsWith("http://", true) || video.startsWith("https://", true)) video
                else "https://zyora-voice-chat.vercel.app/" + video.trimStart('/')
            mainHandler.post { roomGiftVideoPreview = videoUrl }
        }
        val vehicleEntryListener = Emitter.Listener { args ->
            val payload = args.firstOrNull() as? JSONObject ?: return@Listener
            if (payload.optString("roomId") != roomId || !latestEntryEffectEnabled) return@Listener
            val user = payload.optString("userId").trim()
            val vehicle = payload.optJSONObject("vehicle") ?: return@Listener
            val video = vehicle.optString("video").trim()
            if (user.isBlank() || video.isBlank() || vehicle.optLong("expiresAt") <= System.currentTimeMillis()) return@Listener
            val entryId = payload.optString("entryId").trim()
            if (entryId.isBlank()) return@Listener
            val videoUrl = if (video.startsWith("http", true)) video else "https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/" + video
            val itemId = vehicle.optString("itemId").trim()
            mainHandler.post {
                if (!seenRoomVehicleEntryIds.contains(entryId)) {
                    seenRoomVehicleEntryIds.add(entryId)
                    if (seenRoomVehicleEntryIds.size > 100) seenRoomVehicleEntryIds.removeAt(0)
                    if (roomVehiclePreview.isBlank()) {
                        roomVehiclePreview = videoUrl
                        roomVehiclePreviewItemId = itemId
                    } else if (pendingRoomVehicleEntries.size < 20) {
                        pendingRoomVehicleEntries.add(videoUrl to itemId)
                    }
                }
            }
        }
        val avatarFrameUpdatedListener = Emitter.Listener { args ->
            val payload = args.firstOrNull() as? JSONObject ?: return@Listener
            val updatedUserId = payload.optString("userId").trim()
            if (updatedUserId.isNotBlank()) mainHandler.post {
                nativeAvatarFrameRefreshVersions[updatedUserId] = (nativeAvatarFrameRefreshVersions[updatedUserId] ?: 0L) + 1L
            }
        }
        val clearedListener = Emitter.Listener { args ->
            val payload=args.firstOrNull() as? JSONObject
            if(payload!=null && payload.optString("roomId")==roomId) {
                mainHandler.post {
                    messages.clear()
                    kickNotices.clear()
                    showRoomSettingsAnnouncementInChat = false
                    appContext.getSharedPreferences("room_settings", android.content.Context.MODE_PRIVATE).edit().putBoolean("announcement_chat_visible_$roomId", false).apply()
                    showOfficialAnnouncement = false
                    // Clearing public chat must never clear the persistent Room Settings announcement.
                    clearedBy = payload.optString("userName").ifBlank { backendRoomUsers.firstOrNull { it.accountId == payload.optString("userId") }?.name ?: "Admin" }
                }
            }
        }
        val kickAnnouncementListener = Emitter.Listener { args ->
            val payload = args.firstOrNull() as? JSONObject ?: return@Listener
            if (payload.optString("roomId") != roomId) return@Listener
            val targetId = payload.optString("targetId")
            val notice = NativeRoomKickNotice(
                id = "kick-$targetId-${payload.optLong("timestamp", System.currentTimeMillis())}",
                targetName = payload.optString("targetName").ifBlank { "User" },
                targetImage = payload.optString("targetImage"),
                actorName = payload.optString("actorName").ifBlank { "Admin" },
                actorImage = payload.optString("actorImage"),
                actorRole = if (payload.optString("actorRole") == "admin") "Room Admin" else "Room Owner",
                timestamp = payload.optLong("timestamp", System.currentTimeMillis())
            )
            // Add the system notice to the same visible chat timeline.
            mainHandler.post {
                if (kickNotices.none { it.id == notice.id }) kickNotices.add(notice)
            }
        }
        val seatInviteResultListener = Emitter.Listener { args ->
            val payload = args.firstOrNull() as? JSONObject ?: return@Listener
            if (payload.optString("roomId") == roomId &&
                payload.optString("inviterId") == accountId &&
                payload.optString("status") == "accepted") {
                roomSocket.emit("room_seat_invite_apply", JSONObject()
                    .put("roomId", roomId).put("inviterId", accountId)
                    .put("targetId", payload.optString("targetId"))
                    .put("inviteId", payload.optString("inviteId"))
                    .put("seatNumber", payload.optInt("seatNumber"))
                    .put("userName", payload.optString("userName", "User"))
                    .put("userImage", payload.optString("userImage", "")))
            }
        }
        val seatInviteListener = Emitter.Listener { args ->
            val payload = args.firstOrNull() as? JSONObject ?: return@Listener
            if (payload.optString("roomId") == roomId && payload.optString("targetId") == accountId) {
                mainHandler.post { incomingSeatInvite = payload }
            }
        }
        val roomKickedListener = Emitter.Listener { args ->
            val payload = args.firstOrNull() as? JSONObject ?: return@Listener
            if (payload.optString("roomId") == roomId && payload.optString("targetId") == accountId) {
                // Set before posting to main: a subsequent blocked event must not win this race.
                liveKickReceived.set(true)
                mainHandler.post {
                    incomingSeatInvite = null
                    onKickNotice(payload)
                    onClose()
                }
            }
        }
        val roomKickBlockedListener = Emitter.Listener { args ->
            val payload = args.firstOrNull() as? JSONObject ?: return@Listener
            if (payload.optString("roomId") == roomId &&
                payload.optString("userId") == accountId && liveKickReceived.compareAndSet(false, true)) {
                // This event is sent to the user being removed from the live room.
                // Show the same acknowledgement card as room_kicked; only later
                // re-entry checks use the compact "can't join until" popup.
                mainHandler.post {
                    incomingSeatInvite = null
                    onKickNotice(JSONObject()
                        .put("roomId", roomId)
                        .put("targetId", accountId)
                        .put("expiresAt", payload.optLong("expiresAt", 0L))
                        .put("ownerId", payload.optString("ownerId", roomId))
                        .put("ownerName", payload.optString("ownerName", "Room Owner"))
                        .put("ownerImage", payload.optString("ownerImage", ""))
                        .put("blocked", false))
                    onClose()
                }
            }
        }
        val walletBalanceListener = Emitter.Listener { args ->
            val payload = args.firstOrNull() as? JSONObject ?: return@Listener
            val eventUserId = payload.optString("userId")
            val ownWalletIds = setOf(canonicalWalletUserId, accountId, currentUser.uid, currentUser.id)
                .filter { it.isNotBlank() }.toSet()
            // Gift transfers may identify the sender by permanent account ID,
            // while the initial wallet read uses Firebase UID. Accept both aliases.
            if (eventUserId in ownWalletIds) {
                val updated = payload.optLong("balance", -1L)
                if (updated >= 0L) mainHandler.post { roomWalletBalance = updated }
            }
        }
        val luckyBagTransactionListener = Emitter.Listener { args ->
            val payload = args.firstOrNull() as? JSONObject ?: return@Listener
            val transactionUserId = payload.optString("userId")
            if (transactionUserId.isNotBlank() &&
                transactionUserId in setOf(canonicalWalletUserId, accountId, currentUser.uid, currentUser.id)) {
                val transactionId = payload.optString("id").ifBlank {
                    "${payload.optString("type")}:${payload.optString("bagId")}:$transactionUserId"
                }
                val type = payload.optString("type")
                val amount = payload.optLong("amount", 0L)
                val bagId = payload.optString("bagId")
                val createdAt = payload.optLong("createdAt", System.currentTimeMillis())
                mainHandler.post {
                    runCatching {
                        NativeIndexDatabase(appContext).saveLuckyBagTransaction(
                            transactionId, transactionUserId, type, amount, bagId, createdAt
                        )
                    }
                }
            }
        }
        val luckyBagBannerListener = Emitter.Listener { args ->
            val payload = args.firstOrNull() as? JSONObject ?: return@Listener
            if (payload.optString("roomId") == roomId && payload.optString("senderId") !in setOf(canonicalWalletUserId, accountId, currentUser.uid, currentUser.id).filter { it.isNotBlank() }) {
                mainHandler.post {
                    luckyBagId = payload.optString("bagId")
                    luckyBagSenderName = payload.optString("senderName", "User")
                    luckyBagSenderImage = payload.optString("senderImage", "")
                    luckyBagClaimError = null
                    showFloatingLuckyBag = true
                }
            }
        }
        val luckyBagClaimReservedListener = Emitter.Listener { args ->
            val payload = args.firstOrNull() as? JSONObject ?: return@Listener
            val ownIds = setOf(canonicalWalletUserId, accountId, currentUser.uid, currentUser.id).filter { it.isNotBlank() }.toSet()
            if (payload.optString("roomId") == roomId && payload.optString("senderId") in ownIds) {
                mainHandler.post {
                    runCatching {
                        NativeIndexDatabase(appContext).saveLuckyBagClaim(
                            payload.optString("bagId"), payload.optString("roomId"),
                            payload.optString("userId"), JSONObject(payload.toString())
                        )
                    }
                }
            }
        }
        val luckyGiftReceivedListener = Emitter.Listener { args ->
            val payload = args.firstOrNull() as? JSONObject ?: return@Listener
            if (payload.optString("roomId") != roomId || !payload.optBoolean("luckyGift", false)) return@Listener
            val senderId = payload.optString("senderId")
            val senderName = payload.optString("senderName", "User")
            val senderImageRaw = payload.optString("senderImage")
            val giftImageRaw = payload.optString("giftImage").ifBlank { payload.optString("luckyImage") }
            val giftImage = when {
                giftImageRaw.isBlank() -> ""
                giftImageRaw.startsWith("http", true) -> giftImageRaw
                else -> "https://zyora-voice-chat.vercel.app/" + giftImageRaw.trimStart('/')
            }
            val recipientIdsJson = payload.optJSONArray("recipientIds") ?: JSONArray()
            val recipientIds = (0 until recipientIdsJson.length()).map { recipientIdsJson.optString(it) }.toSet()
            val multiplier = payload.optInt("multiplier", 1).coerceAtLeast(1)
            val isComboReplay = payload.optBoolean("comboReplay", false)
            val recipientName = payload.optString("recipientName", "User")
            val recipientMode = when (payload.optString("recipientMode").lowercase()) {
                "mic" -> RecipientMode.MIC
                "room" -> RecipientMode.ROOM
                else -> RecipientMode.SINGLE
            }
            val timestamp = payload.optLong("timestamp", System.currentTimeMillis())
            val noticeId = payload.optString("transferId").ifBlank { "lucky-$timestamp" }
            if (!seenLuckyGiftTransferIds.add(noticeId)) return@Listener
            if (seenLuckyGiftTransferIds.size > 200) {
                synchronized(seenLuckyGiftTransferIds) {
                    while (seenLuckyGiftTransferIds.size > 150) seenLuckyGiftTransferIds.remove(seenLuckyGiftTransferIds.first())
                }
            }
            mainHandler.post {
                luckyGiftNotice = LuckyNotice(
                    id = noticeId,
                    senderId = senderId,
                    senderName = senderName,
                    recipientName = recipientName,
                    senderImage = senderImageRaw.ifBlank { ZYORA_DEFAULT_AVATAR_URL },
                    giftImage = giftImage,
                    multiplier = if (isComboReplay && luckyGiftNotice?.senderId == senderId) {
                        (luckyGiftNotice?.multiplier ?: multiplier) + 1
                    } else multiplier,
                    totalWinAmount = payload.optLong("luckyReturnAmount", 0L),
                    maxWinTimes = payload.optInt("luckyReturnPercent", 0),
                    recipientMode = recipientMode
                )
                if (senderId == accountId) {
                    // Both the original send and its combo replay renew the same
                    // five-second window. Never hide the button on a combo tap.
                    showLuckyComboButton = true
                    luckyComboTimeLeft = 5
                    luckyComboGeneration++
                }
                if (giftImage.isNotBlank()) {
                    seats.indices.filter { seats[it].occupied && seats[it].accountId in recipientIds }.forEach { index ->
                        val seat = seats[index]
                        seats[index] = seat.copy(gifSrc = giftImage, gifTimestamp = timestamp, gifDurationMs = 1800L)
                        val seatNumber = seat.number
                        mainHandler.postDelayed({
                            val currentIndex = seats.indexOfFirst { it.number == seatNumber }
                            if (currentIndex >= 0 && seats[currentIndex].gifTimestamp == timestamp) {
                                seats[currentIndex] = seats[currentIndex].copy(gifSrc = "", gifTimestamp = 0L, gifDurationMs = 5000L)
                            }
                        }, 1800L)
                    }
                }
                // Each new send or combo replay renews the same visible notice.
                // The notice only exits after five seconds with no further combo.
                mainHandler.postDelayed({
                    if (luckyGiftNotice?.id == noticeId) {
                        luckyGiftNotice = luckyGiftNotice?.copy(isExiting = true)
                        mainHandler.postDelayed({ if (luckyGiftNotice?.id == noticeId) luckyGiftNotice = null }, 450)
                    }
                }, 5000L)
            }
        }
        val seatListener = Emitter.Listener { args ->
            val payload = args.firstOrNull() as? JSONObject
            if (payload != null && payload.optString("roomId") == roomId) {
                val seatsJson = payload.optJSONArray("seats") ?: JSONArray()
                val parsed = ArrayList<NativeRoomSeat>()
                for (index in 0 until seatsJson.length()) {
                    val seat = seatsJson.optJSONObject(index) ?: continue
                    val user = seat.optJSONObject("user")
                    val gif = seat.optJSONObject("gif")
                    parsed.add(NativeRoomSeat(number=seat.optInt("number",index+1),occupied=seat.optBoolean("isOccupied",user != null),locked=seat.optBoolean("isLocked",false),muted=seat.optBoolean("isMuted",false),selfMuted=seat.optBoolean("isSelfMuted",false),speaking=seat.optBoolean("isSpeaking",false),userName=if (user == null) "" else user.optString("name","User"),userImage=if (user == null) "" else if (user.optString("accountId",user.optString("userId","")) in setOf("100002", "100003")) "https://zyora-voice-chat.vercel.app/logo.png" else sequenceOf("image", "dp", "photo", "photoURL", "photoUrl", "avatar", "avatarUrl", "profileImage", "profileImageUrl", "profile_image", "imageUrl", "picture").map { user.optString(it, "").trim() }.firstOrNull { it.isNotBlank() && it != "null" && it != "undefined" }.orEmpty(),accountId=if (user == null) "" else user.optString("accountId",user.optString("userId","")),gifSrc=gif?.optString("src","") ?: "",gifTimestamp=gif?.optLong("timestamp",0L) ?: 0L,gifDurationMs=gif?.optLong("duration",5000L) ?: 5000L,gifScale=((gif?.optDouble("scale",0.8) ?: 0.8).toFloat())))
                }
                mainHandler.post {
                    // A fast seat transfer can briefly produce a snapshot containing
                    // the same account in both its old and new seats. Keep only the
                    // newest occurrence in the payload so Compose never draws two avatars.
                    val seenOccupiedAccounts = mutableSetOf<String>()
                    val normalizedParsed = parsed.asReversed().filter { seat ->
                        !seat.occupied || seat.accountId.isBlank() || seenOccupiedAccounts.add(seat.accountId)
                    }.asReversed()
                    val byNumber = normalizedParsed.associateBy { it.number }
                    for (index in seats.indices) {
                        val number = seats[index].number
                        val incoming = byNumber[number]
                        seats[index] = incoming?.copy(
                            speaking = !incoming.muted && !incoming.selfMuted && (incoming.speaking || incoming.accountId in activeVoiceSpeakerIds)
                        ) ?: NativeRoomSeat(number=number)
                    }
                    normalizedParsed.filter { it.number !in seats.map(NativeRoomSeat::number) }.forEach { incoming ->
                        seats.add(incoming.copy(
                            speaking = !incoming.muted && (incoming.speaking || incoming.accountId in activeVoiceSpeakerIds)
                        ))
                    }
                    val ownerIds = setOf(roomOwner.accountId, roomOwner.id, roomOwner.uid, roomId).filter { it.isNotBlank() }.toSet()
                    val firstSeat = normalizedParsed.firstOrNull { it.number == 1 }
                    if (accountId in ownerIds && firstSeat != null && ownerAutoSeatAttempted.compareAndSet(false, true)) {
                        val ownerAlreadySeated = normalizedParsed.any { it.occupied && it.accountId in ownerIds }
                        if (!firstSeat.occupied && !firstSeat.locked && !ownerAlreadySeated) {
                            val ownerUser = JSONObject()
                                .put("name", latestRoomDisplayName.ifBlank { "User" })
                                .put("image", latestRoomDisplayImage.ifBlank { ZYORA_DEFAULT_AVATAR_URL })
                                .put("accountId", accountId)
                            roomSocket.emit(
                                "room_seat_action",
                                JSONObject().put("roomId", roomId).put("userId", accountId)
                                    .put("action", "take").put("seatNumber", 1).put("user", ownerUser)
                            )
                        }
                    }
                }
            }
        }
        val voiceSpeakerListener = Emitter.Listener { args ->
            val payload = args.firstOrNull() as? JSONObject
            if (payload != null && payload.optString("roomId") == roomId) {
                val speakingUserId = payload.optString("userId").trim()
                if (speakingUserId.isNotBlank()) {
                    val isSpeakingNow = payload.optBoolean("speaking", false)
                    mainHandler.post {
                        if (isSpeakingNow) activeVoiceSpeakerIds.add(speakingUserId)
                        else activeVoiceSpeakerIds.remove(speakingUserId)
                        for (index in seats.indices) {
                            val seat = seats[index]
                            if (seat.accountId == speakingUserId) {
                                seats[index] = seat.copy(speaking = isSpeakingNow && !seat.muted && !seat.selfMuted)
                            }
                        }
                    }
                }
            }
        }
        val roomFollowListener = Emitter.Listener { args ->
            val payload = args.firstOrNull() as? JSONObject
            if (payload?.optString("roomId") == roomId) {
                roomIoScope.launch {
                    val updated = runCatching {
                        val encodedRoom = java.net.URLEncoder.encode(roomId, "UTF-8")
                        val connection = (java.net.URL("https://zyora-server.onrender.com/api/room-follows?roomId=$encodedRoom").openConnection() as java.net.HttpURLConnection).apply {
                            connectTimeout = 10000
                            readTimeout = 10000
                        }
                        try {
                            if (connection.responseCode !in 200..299) error("Room members unavailable")
                            val array = JSONObject(connection.inputStream.bufferedReader().use { it.readText() }).optJSONArray("followers") ?: JSONArray()
                            (0 until array.length()).mapNotNull { index ->
                                val item = array.optJSONObject(index) ?: return@mapNotNull null
                                val id = item.optString("userId").trim()
                                if (id.isBlank()) return@mapNotNull null
                                RoomInfoFollower(id, item.optString("followerName").ifBlank { "User" }, item.optString("followerImage"), item.optString("followerCountry"))
                            }.distinctBy { it.accountId }
                        } finally { connection.disconnect() }
                    }.getOrDefault(emptyList())
                    mainHandler.post { followedRoomUsers = updated }
                }
            }
        }
        val roomJoinBlockedListener = Emitter.Listener { args ->
            val payload = args.firstOrNull() as? JSONObject
            if (payload != null && payload.optString("roomId") == roomId) {
                mainHandler.post { showRoomOwnerBlockedToast = true }
                mainHandler.postDelayed({ showRoomOwnerBlockedToast = false; onClose() }, 2200)
            }
        }
        val settingsListener = Emitter.Listener { args ->
            val payload = args.firstOrNull() as? JSONObject
            if (payload != null && payload.optString("roomId") == roomId) {
                val updatedMicMode = payload.optInt("micMode", roomMicMode)
                val hasAnnouncement = payload.has("announcement") || payload.has("message")
                val updatedAnnouncement = if (payload.has("announcement")) payload.optString("announcement", "")
                    else payload.optString("message", "")
                mainHandler.post {
                    if (updatedMicMode in listOf(5, 10, 15)) {
                        roomMicMode = updatedMicMode
                        appContext.getSharedPreferences("room_settings", android.content.Context.MODE_PRIVATE)
                            .edit().putInt("micMode_$roomId", updatedMicMode).apply()
                    }
                    if (payload.has("theme")) {
                        val updatedTheme = payload.optString("theme").trim()
                        if (updatedTheme.isNotBlank() && updatedTheme != "null") {
                            roomTheme = updatedTheme
                            appContext.getSharedPreferences("room_settings", android.content.Context.MODE_PRIVATE)
                                .edit().putString("theme_$roomId", updatedTheme).apply()
                        }
                    }
                    if (payload.has("customTheme")) {
                        val updatedCustomTheme = payload.optString("customTheme").trim()
                        roomCustomThemeUrl = updatedCustomTheme
                        appContext.getSharedPreferences("room_settings", android.content.Context.MODE_PRIVATE)
                            .edit().putString("customTheme_$roomId", updatedCustomTheme).apply()
                    }
                    if (hasAnnouncement) {
                        announcement = updatedAnnouncement
                        showRoomSettingsAnnouncementInChat = if (payload.has("announcementChatVisible")) payload.optBoolean("announcementChatVisible") else updatedAnnouncement.isNotBlank()
                        appContext.getSharedPreferences("room_settings", android.content.Context.MODE_PRIVATE).edit().putBoolean("announcement_chat_visible_$roomId", showRoomSettingsAnnouncementInChat).apply()
                    }
                    if (payload.has("publicMsgOff")) publicMsgOff = payload.optBoolean("publicMsgOff", true)
                    if (payload.has("roomName")) roomName = payload.optString("roomName", latestRoomName)
                    if (payload.has("addedAdminId")) {
                        val addedAdminId = payload.optString("addedAdminId").trim()
                        val addedAdminName = payload.optString("addedAdminName").trim().ifBlank { "User" }
                        if (addedAdminId.isNotBlank()) {
                            val noticeId = "admin-added-$roomId-$addedAdminId-${payload.optLong("updatedAt")}"
                            if (messages.none { it.id == noticeId }) {
                                val ownerName = payload.optString("actorName").trim().ifBlank { latestRoomDisplayName.ifBlank { "Room Owner" } }
                                val ownerImage = payload.optString("actorImage").trim().ifBlank { latestRoomDisplayImage }
                                val ownerId = payload.optString("actorId").trim().ifBlank { roomOwner.accountId.ifBlank { roomOwner.id.ifBlank { roomId } } }
                                messages.add(NativeRoomMessage(noticeId, "Set $addedAdminName as Admin", ownerName, ownerImage, ownerId, payload.optLong("updatedAt", System.currentTimeMillis()), "message"))
                            }
                        }
                    }
                    if (payload.has("admin") && payload.optJSONArray("admin") != null) {
                        val adminsJson = payload.optJSONArray("admin")!!
                        roomAdminIds = (0 until adminsJson.length()).flatMap { index ->
                            val entry = adminsJson.opt(index)
                            when (entry) {
                                is JSONObject -> listOf("accountId", "userId", "id", "uid")
                                    .mapNotNull { key -> entry.optString(key).trim().takeIf { it.isNotBlank() && it != "null" } }
                                is String -> listOf(entry.trim()).filter { it.isNotBlank() && it != "null" }
                                else -> emptyList()
                            }
                        }.distinct()
                    }
                }
            }
        }
        var joinMessageSent = false
        // Room pages can reuse an already-connected room socket. In that path the
        // server does not emit a new room_vehicle_entry event, so resolve the current
        // user's equipped Bag vehicle directly as a reliable playback fallback.
        fun loadEquippedRoomVehicle() {
            val vehicleLookupIds = listOf(
                currentUser.uid,
                currentUser.id,
                currentUser.accountId,
                accountId,
                appContext.getSharedPreferences("hurry_auth", android.content.Context.MODE_PRIVATE)
                    .getString("uid", "").orEmpty()
            ).filter { it.isNotBlank() && it != "guest" }.distinct()
            if (vehicleLookupIds.isEmpty()) return
            roomIoScope.launch {
                val equipped = vehicleLookupIds.firstNotNullOfOrNull { lookupId ->
                    runCatching {
                        val encoded = java.net.URLEncoder.encode(lookupId, "UTF-8")
                        val connection = (java.net.URL("https://zyora-server.onrender.com/api/store/inventory/" + encoded).openConnection() as java.net.HttpURLConnection).apply {
                            connectTimeout = 10000
                            readTimeout = 12000
                            useCaches = false
                            setRequestProperty("Cache-Control", "no-cache, no-store")
                            setRequestProperty("Pragma", "no-cache")
                        }
                        try {
                            if (connection.responseCode !in 200..299) return@runCatching null
                            val root = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                            val items = root.optJSONArray("items") ?: JSONArray()
                            (0 until items.length()).mapNotNull { items.optJSONObject(it) }
                                .firstOrNull { item ->
                                    item.optString("tab") == "Vehicle" &&
                                        item.optBoolean("equipped") &&
                                        item.optLong("expiresAt") > System.currentTimeMillis() &&
                                        item.optString("video").isNotBlank()
                                }
                        } finally {
                            connection.disconnect()
                        }
                    }.getOrNull()
                }
                if (equipped != null && latestEntryEffectEnabled) {
                    val video = equipped.optString("video").trim()
                    val videoUrl = if (video.startsWith("http://", true) || video.startsWith("https://", true)) video
                        else "https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/" + video
                    val itemId = equipped.optString("itemId").trim()
                    mainHandler.post {
                        // The server excludes the joining socket from room_vehicle_entry.
                        if (roomVehiclePreview.isBlank()) {
                            roomVehiclePreview = videoUrl
                            roomVehiclePreviewItemId = itemId
                        } else if (pendingRoomVehicleEntries.size < 20) {
                            pendingRoomVehicleEntries.add(videoUrl to itemId)
                        }
                    }
                }
            }
        }
        val connectListener = Emitter.Listener {
            // Join the room immediately after the WebSocket handshake. Profile cache
            // persistence is useful metadata work, but must never sit on the critical
            // room-entry path and delay the authoritative seat/presence snapshots.
            val joinPayload = JSONObject().put("roomId",roomId).put("userId",currentUser.uid.ifBlank{currentUser.id.ifBlank{accountId}}).put("accountId",accountId).put("roomOwnerId",roomOwner.uid.ifBlank{roomOwner.id.ifBlank{roomOwner.accountId.ifBlank{roomId}}}).put("name",latestRoomDisplayName).put("dp",latestRoomDisplayImage).put("roomName",latestRoomName).put("roomImage",latestRoomImage).put("suppressEntry", joinMessageSent)
            roomSocket.emit("room_join",joinPayload)
            roomIoScope.launch {
                runCatching {
                    val profileDb = NativeIndexDatabase(appContext)
                    val currentProfile = currentProfileKeys.firstNotNullOfOrNull { profileDb.loadProfile(it) } ?: JSONObject()
                    if (!currentProfile.has("name") && latestRoomDisplayName.isNotBlank()) currentProfile.put("name", latestRoomDisplayName)
                    if (!currentProfile.has("image") && latestRoomDisplayImage.isNotBlank()) currentProfile.put("image", latestRoomDisplayImage)
                    currentProfileKeys.forEach { profileDb.saveProfile(it, currentProfile) }
                }
            }
            // A kept-room reconnect restores the existing room session. Do not replay
            // the entry vehicle animation when silently restoring that session.
            if (!joinMessageSent && latestEntryEffectEnabled) loadEquippedRoomVehicle()
            if (latestShowChat) roomSocket.emit("room_chat_view", JSONObject().put("roomId", roomId).put("userId", accountId).put("active", true))
            if (!joinMessageSent && accountId != "guest") {
                joinMessageSent = true
                val joinCreatedAt = System.currentTimeMillis()
                val joinId = "$accountId-$joinCreatedAt"
                val joinName = latestRoomDisplayName.ifBlank { "User" }
                val joinImage = latestRoomDisplayImage.ifBlank { ZYORA_DEFAULT_AVATAR_URL }
                val nativeJoin = NativeRoomMessage(joinId,"Enter the Room",joinName,joinImage,accountId,joinCreatedAt,"join")
                mainHandler.post {
                    if (messages.none { it.id == joinId }) messages.add(nativeJoin)
                    if (entryToasts.none { it.id == joinId }) {
                        entryToasts.add(NativeEntryToast(joinId,joinName,joinImage,accountId=accountId))
                        mainHandler.postDelayed({ entryToasts.removeAll { it.id == joinId } },4000)
                    }
                }
            }
            roomSocket.emit("room_presence_request",JSONObject().put("roomId",roomId))
            roomSocket.emit("room_seats_request",JSONObject().put("roomId",roomId))
        }
        roomSocket.on("room_presence",presenceListener)
        roomSocket.on("room_vehicle_entry", vehicleEntryListener)
        roomSocket.on("gift_video_play", giftVideoListener)
        roomSocket.on("avatar_frame_updated", avatarFrameUpdatedListener)
        roomSocket.on("room_seat_invite", seatInviteListener)
        roomSocket.on("room_kicked", roomKickedListener)
        roomSocket.on("room_kick_blocked", roomKickBlockedListener)
        roomSocket.on("room_seat_invite_result", seatInviteResultListener)
        roomSocket.on("room_message",messageListener)
        roomSocket.on("room_chat_message",messageListener)
        roomSocket.on("room_chat_cleared",clearedListener)
        roomSocket.on("room_kick_announcement",kickAnnouncementListener)
        roomSocket.on("room_seats",seatListener)
        roomSocket.on("coin_transfer_received", luckyGiftReceivedListener)
        roomSocket.on("room_voice_speaker",voiceSpeakerListener)
        val roomPublicMessageBlockedListener = Emitter.Listener { args ->
            val payload = args.firstOrNull() as? JSONObject
            if (payload != null && payload.optString("roomId") == roomId) {
                mainHandler.post { showPublicMessageClosedToast = true }
                mainHandler.postDelayed({ showPublicMessageClosedToast = false }, 1800)
            }
        }
        roomSocket.on("room_settings_updated",settingsListener)
        roomSocket.on("room_public_message_blocked", roomPublicMessageBlockedListener)
        roomSocket.on("room_join_blocked", roomJoinBlockedListener)
        roomSocket.on("room_follow_updated", roomFollowListener)
        roomSocket.on("lucky_bag_banner",luckyBagBannerListener)
        roomSocket.on("lucky_bag_transaction",luckyBagTransactionListener)
        roomSocket.on("lucky_bag_claim_reserved", luckyBagClaimReservedListener)
        roomSocket.on("wallet_balance_updated",walletBalanceListener)
        roomSocket.on(Socket.EVENT_CONNECT,connectListener)
        if (roomSocket.connected()) {
            // The retained socket is already a room member. Only refresh snapshots;
            // do not emit room_join or generate another "Enter the Room" event.
            roomSocket.emit("room_presence_request", JSONObject().put("roomId", roomId))
            roomSocket.emit("room_seats_request", JSONObject().put("roomId", roomId))
            if (latestShowChat) roomSocket.emit("room_chat_view", JSONObject().put("roomId", roomId).put("userId", accountId).put("active", true))
        } else {
            roomSocket.connect()
        }
        onDispose {
            // A real room exit must stop the foreground music service even when navigation
            // bypasses the Exit-menu button. Keep the saved track metadata for the next visit.
            runCatching {
                appContext.startService(Intent(appContext, RoomMusicService::class.java)
                    .setAction(RoomMusicService.ACTION_STOP))
            }
            miniMusicTrack?.let { track ->
                persistLastRoomTrack(track)
                MusicPlaybackState.currentTrack = track
            }
            isMusicPlaying = false
            MusicPlaybackState.isPlaying = false
            MusicPlaybackState.currentPlayingUri = null
            // This effect is disposed only on a real room switch/exit. A minimized room
            // stays composed, so its Socket.IO listeners and voice state remain alive.
            roomIoScope.cancel()
            mainHandler.removeCallbacksAndMessages(null)
            roomSocket.emit("room_chat_view", JSONObject().put("roomId", roomId).put("userId", accountId).put("active", false))
            roomSocket.emit("room_leave",JSONObject().put("roomId",roomId).put("userId",accountId).put("accountId",accountId))
            roomSocket.off("room_presence",presenceListener)
            roomSocket.off("room_vehicle_entry", vehicleEntryListener)
            roomSocket.off("gift_video_play", giftVideoListener)
            roomSocket.off("avatar_frame_updated", avatarFrameUpdatedListener)
            roomSocket.off("room_seat_invite", seatInviteListener)
            roomSocket.off("room_kicked", roomKickedListener)
            roomSocket.off("room_kick_blocked", roomKickBlockedListener)
            roomSocket.off("room_seat_invite_result", seatInviteResultListener)
            roomSocket.off("room_message",messageListener)
            roomSocket.off("room_chat_message",messageListener)
            roomSocket.off("room_chat_cleared",clearedListener)
            roomSocket.off("room_kick_announcement",kickAnnouncementListener)
            roomSocket.off("room_seats",seatListener)
            roomSocket.off("coin_transfer_received", luckyGiftReceivedListener)
            roomSocket.off("room_voice_speaker",voiceSpeakerListener)
            mainHandler.post { activeVoiceSpeakerIds.clear() }
            roomSocket.off("room_settings_updated",settingsListener)
            roomSocket.off("room_public_message_blocked", roomPublicMessageBlockedListener)
            roomSocket.off("room_join_blocked", roomJoinBlockedListener)
            roomSocket.off("room_follow_updated", roomFollowListener)
            roomSocket.off("lucky_bag_banner",luckyBagBannerListener)
            roomSocket.off("lucky_bag_transaction",luckyBagTransactionListener)
            roomSocket.off("lucky_bag_claim_reserved", luckyBagClaimReservedListener)
            roomSocket.off("wallet_balance_updated",walletBalanceListener)
            roomSocket.off(Socket.EVENT_CONNECT,connectListener)
            roomSocket.disconnect()
            roomSocket.close()
            roomSocketRef.value=null
            mainHandler.post { backendRoomUsers.clear() }
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)){
        if (roomToastText.isNotBlank()) {
            Text(
                text = roomToastText,
                color = Color.White,
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.Center).zIndex(102f)
                    .clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(alpha = 0.70f))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }
        if (showRoomOwnerBlockedToast) {
            Text(
                text = "You're blocked by Room owner",
                color = Color.White,
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.Center).zIndex(101f)
                    .clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(alpha = 0.70f))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }
        if (showPublicMessageClosedToast) {
            Text(
                text = "Public Message are close",
                color = Color.White,
                fontSize = 12.sp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .zIndex(100f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.70f))
                    .padding(horizontal = 14.dp, vertical = 9.dp)
            )
        }
        JitsiRoomAudio(roomId=roomId,userName=roomDisplayName,localAccountId=accountId,canSpeak=seats.any { it.number <= roomMicMode && it.occupied && it.accountId == accountId && !it.muted && !it.selfMuted },canShareMusic=seats.any { it.occupied && it.accountId == accountId },playbackVoiceEnabled=speaker,roomSocket=roomSocketRef.value,musicUri=miniMusicTrack?.url ?: MusicPlaybackState.currentPlayingUri,musicPlaying=isMusicPlaying,onSpeakingChanged={localVoiceSpeaking=it},onMusicActivityChanged={publisherId,active -> if(active) activeMusicSpeakerIds.add(publisherId) else activeMusicSpeakerIds.remove(publisherId)})
        AsyncImage(model=when(roomTheme) {
            "forest-night" -> "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/1784875884052~2.jpg"
            "mood-light" -> "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/1784533036732~2.jpg"
            "lantern-tree" -> "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/file_000000001db88211a17274a0cb143387.png"
            "northern-light" -> "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/file_000000004f388211b534832cd6b1f1a2.png"
            "sword-ruins" -> "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/file_0000000085a48211ae91d22ca50879e2.png"
            "cornfield-portal" -> "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/file_00000000baec8211b803a01c54381503.png"
            "deer-moonlight" -> "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/file_00000000dec082119e076cff4bc01164.png"
            "moon-light-new" -> "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/file_0000000050288211aecacc1c105240c7.png"
            "dark-night-flower" -> "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/file_000000006ec4821182b483795db2d94c.png"
            "custom" -> roomCustomThemeUrl.takeIf { it.startsWith("https://") } ?: "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/1784875884052~2.jpg"
            else -> if (purchasedRoomThemeImage.isNotBlank()) "https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/$purchasedRoomThemeImage" else "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/1784875884052~2.jpg"
        },contentDescription="Room theme background",modifier=Modifier.fillMaxSize(),contentScale=ContentScale.Crop)

        // CUP AND MUSIC PILLS REVERTED TO ORIGINAL TOP LEFT OVERLAY POSITION
        Row(modifier = Modifier.align(Alignment.TopStart).padding(top = 90.dp, start = 4.dp),verticalAlignment = Alignment.CenterVertically,horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.height(22.dp).clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = 0.20f)).clickable { showRoomCupOnline = false; showRoomCup = true }.padding(horizontal = 10.dp),verticalAlignment = Alignment.CenterVertically,horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Default.EmojiEvents, contentDescription = "Top Cup", tint = Color(0xFFFFD54F), modifier = Modifier.size(15.dp))
                RoomDailyCupTotal(roomId)
            }
            if (musicControllerVisible && miniMusicTrack != null) {
                Row(modifier = Modifier.height(26.dp).clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = 0.30f)).clickable { musicControllerMinimized = false }.padding(start = 2.dp, end = 10.dp),verticalAlignment = Alignment.CenterVertically,horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AsyncImage(model = "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/file_00000000ee70820b9fd0dfe71739874e.png",contentDescription = "Music",modifier = Modifier.size(20.dp),contentScale = ContentScale.Fit)
                    Text(text = miniMusicTrack?.name.orEmpty(),color = Color.White,fontSize = 11.sp,fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,maxLines = 1,overflow = TextOverflow.Clip,modifier = Modifier.widthIn(max = 60.dp).basicMarquee(iterations = Int.MAX_VALUE))
                }
            }
        }

        // FULL ROOM CONTENT LAYOUT
        RoomPageBlock {
Column(Modifier.fillMaxSize()) { // REMOVED imePadding() FROM HERE TO PREVENT TOP UI FROM JUMPING
            
            // --- TOP SECTION (FIXED, NO IME PADDING) ---
            Box(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth()) {
                    // Top Room DP card and Top Right Options
                    Row(Modifier.fillMaxWidth().padding(top=40.dp,start=0.dp,end=8.dp),verticalAlignment=Alignment.CenterVertically){ 
                        Row(modifier = Modifier.height(44.dp).clip(RoundedCornerShape(topEnd = 50.dp, bottomEnd = 50.dp)).background(Color.Black.copy(alpha = 0.3f)).clickable { showInfo = true }.padding(start = 4.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                            FallbackAsyncImage(model = roomImage,contentDescription = "Room profile",modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)),contentScale = ContentScale.Crop)
                            Column(Modifier.padding(start = 8.dp), verticalArrangement = Arrangement.Center) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(text = roomName,color = Color.White,fontSize = 13.sp,fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,maxLines = 1,modifier = Modifier.widthIn(max = 85.dp).basicMarquee(iterations = Int.MAX_VALUE),overflow = TextOverflow.Clip)
                                    CountryFlagImage(roomOwner.country, Modifier.size(18.dp, 12.dp))
                                    if (accountId != roomOwner.accountId.ifBlank { roomOwner.id }) {
                                        if (!followed) {
                                            AsyncImage(
                                                model = "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/IMG_20261107_003419.png",
                                                contentDescription = "Follow room",
                                                modifier = Modifier.size(26.dp).clickable {
                                            val next = !followed
                                            followed = next
                                            onFollowToggle(roomId, next)
                                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                                runCatching {
                                                    val encodedUser = java.net.URLEncoder.encode(accountId, "UTF-8")
                                                    val encodedRoom = java.net.URLEncoder.encode(roomId, "UTF-8")
                                                    val url = if (next) java.net.URL("https://zyora-server.onrender.com/api/room-follows") else java.net.URL("https://zyora-server.onrender.com/api/room-follows?userId=$encodedUser&roomId=$encodedRoom")
                                                    val conn = (url.openConnection() as java.net.HttpURLConnection).apply {
                                                        requestMethod = if (next) "POST" else "DELETE"
                                                        connectTimeout = 8000
                                                        readTimeout = 8000
                                                        if (next) {
                                                            doOutput = true
                                                            setRequestProperty("Content-Type", "application/json")
                                                        }
                                                    }
                                                    if (next) {
                                                        val payload = JSONObject()
                                                            .put("userId", accountId)
                                                            .put("roomId", roomId)
                                                            .put("roomName", roomName)
                                                            .put("roomImage", roomImage)
                                                            .put("roomCountry", roomOwner.country)
                                                            .put("followerName", roomDisplayName)
                                                            .put("followerImage", roomDisplayImage)
                                                            .put("followerCountry", cachedCurrentProfile?.optString("country").orEmpty().ifBlank { appContext.getSharedPreferences("hurry_auth", android.content.Context.MODE_PRIVATE).getString("country", "").orEmpty() })
                                                        conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
                                                    }
                                                    conn.responseCode
                                                    conn.disconnect()
                                                }
                                            }
                                        
                                                },
                                                contentScale = ContentScale.Fit
                                            )
                                        }
                                    }
                                }
                                val roomOwnerAccountId = roomOwner.accountId.ifBlank { roomOwner.id.ifBlank { roomId } }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.offset(y = (-3).dp)) {
                                    if (ownerVipBadge && ownerVipId.isNotBlank()) {
                                        AsyncImage(
                                            model = "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/IMG_20261101_125212.png",
                                            contentDescription = "VIP ID",
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier.height(13.dp).widthIn(min = 12.dp, max = 18.dp)
                                        )
                                        Text(ownerVipId, color = Color.White.copy(alpha = 0.9f), fontSize = 10.sp)
                                    } else when (roomOwnerAccountId) {
                                        "100002", "100003", "500001", "500002", "500003", "500004", "500005" -> {
                                            AsyncImage(
                                                model = "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/IMG_20261101_125212.png",
                                                contentDescription = "Purple ID Badge",
                                                contentScale = ContentScale.Fit,
                                                modifier = Modifier.height(13.dp).widthIn(min = 12.dp, max = 18.dp)
                                            )
                                            Text(roomOwnerAccountId, color = Color.White.copy(alpha = 0.9f), fontSize = 10.sp)
                                        }
                                        else -> Text("ID: $roomOwnerAccountId", color = Color.White.copy(alpha = 0.9f), fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        // Right aligned Top Icons
                        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)){
                            Row(modifier=Modifier.height(32.dp).clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha=.30f)).clickable{showRoomCupOnline=true;showRoomCup=true}.padding(horizontal=12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)){
                                Image(painterResource(R.drawable.ic_hurry_people),contentDescription="Active users",modifier=Modifier.size(16.dp))
                                Text(backendRoomUsers.size.toString(),color=Color.White,fontSize=12.sp,fontWeight=androidx.compose.ui.text.font.FontWeight.SemiBold)
                            }
                            val isRoomOwner = accountId.isNotBlank() && accountId == roomOwner.accountId.ifBlank { roomOwner.id }
                            if (isRoomOwner) {
                                Box(Modifier.size(32.dp).clip(CircleShape).background(Color.Black.copy(alpha=.30f)).clickable { showSettings = true },contentAlignment=Alignment.Center){Image(painterResource(R.drawable.ic_hurry_settings),contentDescription="Room settings",modifier=Modifier.size(18.dp))}
                            }
                            Box(Modifier.size(32.dp).clip(CircleShape).background(Color.Black.copy(alpha=.30f)).clickable{showExitMenu=true},contentAlignment=Alignment.Center){Image(painterResource(R.drawable.ic_hurry_power),contentDescription="Exit room",modifier=Modifier.size(18.dp))}
                        }
                    }
                    
                    // FIXED HEIGHT ROW TO PREVENT JUMPING
                    Spacer(Modifier.height(0.2.dp))
                    Row(modifier = Modifier.padding(start = 4.dp).height(26.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {}
                    
                    // Seat tap policy: only owner/admin opens the seat action sheet; regular users take an available seat directly.
                    val seatActorIds = setOf(accountId, currentUser.uid, currentUser.id, currentUser.accountId)
                        .map { it.trim() }.filter { it.isNotBlank() }.toSet()
                    val canManageRoomSeats = seatActorIds.isNotEmpty() && (
                        seatActorIds.any { it in setOf(roomOwner.accountId, roomOwner.id, roomOwner.uid, roomId).map(String::trim).filter(String::isNotBlank).toSet() } ||
                        roomAdminIds.any { it.trim() in seatActorIds }
                    )
                    val handleSeatTap: (NativeRoomSeat) -> Unit = { tappedSeat ->
                        selectedSeat = tappedSeat.number
                        if (tappedSeat.occupied) {
                            showSeatSheet = false
                        } else if (canManageRoomSeats) {
                            showSeatSheet = true
                        } else if (!tappedSeat.locked && accountId.isNotBlank() && accountId != "guest") {
                            val displayName = roomDisplayName.ifBlank { "User" }
                            val displayImage = roomDisplayImage.ifBlank { ZYORA_DEFAULT_AVATAR_URL }
                            val user = JSONObject().put("name", displayName).put("image", displayImage).put("accountId", accountId)
                            val targetIndex = seats.indexOfFirst { it.number == tappedSeat.number }
                            if (targetIndex >= 0 && !seats[targetIndex].occupied && !seats[targetIndex].locked) {
                                // Move this account off its previous seat immediately.
                                // Previously only the destination was painted optimistically,
                                // leaving the old avatar visible until the server snapshot.
                                seats.indices.filter { index ->
                                    index != targetIndex && seats[index].occupied &&
                                        (seats[index].accountId.trim() in seatActorIds ||
                                            seats[index].accountId.trim() == accountId.trim())
                                }.forEach { index ->
                                    val previousSeat = seats[index]
                                    seats[index] = NativeRoomSeat(
                                        number = previousSeat.number,
                                        locked = previousSeat.locked
                                    )
                                }
                                seats[targetIndex] = seats[targetIndex].copy(
                                    occupied = true, userName = displayName, userImage = displayImage,
                                    accountId = accountId, muted = false
                                )
                                roomSocketRef.value?.emit(
                                    "room_seat_action",
                                    JSONObject().put("roomId", roomId).put("userId", accountId)
                                        .put("action", "take").put("seatNumber", tappedSeat.number).put("user", user)
                                )
                            }
                            showSeatSheet = false
                            selectedSeat = 0
                        } else {
                            showSeatSheet = false
                            selectedSeat = 0
                        }
                    }
                    // SEAT LAYOUT WITH REDUCED TOP PADDING TO SHIFT UP (5% ouper shift kiya gaya hai)
                    val topPadding = when (roomMicMode) { 5 -> 20.dp; 10 -> 25.dp; 15 -> 27.dp; else -> 25.dp }
                    Column(Modifier.fillMaxWidth().padding(horizontal=10.dp).padding(top=topPadding, bottom=8.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                        val micSize = when (roomMicMode) { 15 -> 57.dp; 5 -> 68.dp; else -> 66.dp } // 5-mic seats 68dp; 15-mic seats 57dp
                        
                        if (roomMicMode == 5) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                seats.take(1).forEach { seat -> NativeSeat(seat, roomOwner.accountId.ifBlank{roomOwner.id}, roomAdminIds = seatAdminIds, isSpeaking = seat.occupied && (seat.accountId in activeMusicSpeakerIds || (!seat.muted && !seat.selfMuted && (seat.accountId in activeVoiceSpeakerIds || (seat.accountId == accountId && localVoiceSpeaking))) || (seat.accountId == accountId && isMusicPlaying)), seatSize = micSize) { handleSeatTap(seat) } }
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                seats.drop(1).take(4).forEach { seat -> NativeSeat(seat, roomOwner.accountId.ifBlank{roomOwner.id}, roomAdminIds = seatAdminIds, isSpeaking = seat.occupied && (seat.accountId in activeMusicSpeakerIds || (!seat.muted && !seat.selfMuted && (seat.accountId in activeVoiceSpeakerIds || (seat.accountId == accountId && localVoiceSpeaking))) || (seat.accountId == accountId && isMusicPlaying)), seatSize = micSize) { handleSeatTap(seat) } }
                            }
                        } else {
                            val rowSizes = when (roomMicMode) {
                                10 -> listOf(2, 4, 4)
                                15 -> listOf(5, 5, 5) // 15 mic mode: Total 3 rows, har mein 5 seats
                                else -> listOf(1, 4, 4, 4)
                            }
                            var seatOffset = 0
                            rowSizes.forEach { rowSize ->
                                val rowSeats = seats.drop(seatOffset).take(rowSize)
                                seatOffset += rowSize
                                val arrangement = if (rowSize == 1) Arrangement.Center else Arrangement.SpaceBetween
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = arrangement) {
                                    if (roomMicMode == 10 && rowSize == 2) {
                                        Spacer(modifier = Modifier.width(micSize)) 
                                        rowSeats.forEach { seat -> NativeSeat(seat, roomOwner.accountId.ifBlank{roomOwner.id}, roomAdminIds = seatAdminIds, isSpeaking = seat.occupied && (seat.accountId in activeMusicSpeakerIds || (!seat.muted && !seat.selfMuted && (seat.accountId in activeVoiceSpeakerIds || (seat.accountId == accountId && localVoiceSpeaking))) || (seat.accountId == accountId && isMusicPlaying)), seatSize = micSize) { handleSeatTap(seat) } }
                                        Spacer(modifier = Modifier.width(micSize))
                                    } else {
                                        rowSeats.forEach { seat -> NativeSeat(seat, roomOwner.accountId.ifBlank{roomOwner.id}, roomAdminIds = seatAdminIds, isSpeaking = seat.occupied && (seat.accountId in activeMusicSpeakerIds || (!seat.muted && !seat.selfMuted && (seat.accountId in activeVoiceSpeakerIds || (seat.accountId == accountId && localVoiceSpeaking))) || (seat.accountId == accountId && isMusicPlaying)), seatSize = micSize) { handleSeatTap(seat) } }
                                    }
                                }
                            }
                        }
                    }
                }
                

                // Chat page is rendered once at the root so seat-mode height changes
                // cannot resize or clip its scrolling region.

            }
            // --- END TOP SECTION ---

            
            // --- BOTTOM SECTION (MESSAGES + INPUT/FOOTER, WITH IME PADDING) ---
            Column(Modifier.fillMaxWidth().weight(1f).imePadding()) {
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    
                    if (!showChat) {
                        RoomChatMessages(
                            state = chatListState,
                            messages = messages,
                            kickNotices = kickNotices,
                            showOfficialAnnouncement = showOfficialAnnouncement,
                            clearedBy = clearedBy,
                            announcement = announcement,
                            showRoomSettingsAnnouncementInChat = showRoomSettingsAnnouncementInChat,
                            roomOwnerId = roomOwner.accountId.ifBlank { roomOwner.id },
                            roomAdminIds = roomAdminIds,
                            isCovered = isCovered,
                            onSelectMessage = { selectedChatProfile = it },
                            onSelectMention = { mention -> selectedChatProfile = NativeRoomMessage("mention-profile-${mention.accountId}", "", mention.name, "", mention.accountId) },
                            onCopyMessage = { text ->
                                clipboard.setText(AnnotatedString(text))
                                roomToastText = "Copy to clipboard"
                            }
                        )
                    }
                }
                
                if(showChat) {
                    Row(
                        Modifier.fillMaxWidth()
                            .background(Color.White)
                            .navigationBarsPadding()
                            .padding(horizontal=16.dp, vertical=12.dp),
                        verticalAlignment=Alignment.CenterVertically,
                        horizontalArrangement=Arrangement.spacedBy(10.dp)
                    ) {
                        IconButton(
                            onClick={roomImagePickerLauncher.launch("image/*")},
                            modifier=Modifier.size(28.dp)
                        ) {
                            Image(
                                painterResource(R.drawable.ic_hurry_chat_image),
                                contentDescription="Image",
                                modifier=Modifier.size(26.dp)
                            )
                        }
                        Row(
                            Modifier.weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color(0xFFF3F4F6))
                                .padding(start=16.dp, end=6.dp),
                            verticalAlignment=Alignment.CenterVertically
                        ) {
                            androidx.compose.foundation.text.BasicTextField(
                                value=inputValue,
                                onValueChange={ inputValue=it },
                                singleLine=true,
                                keyboardOptions=KeyboardOptions(
                                    capitalization=KeyboardCapitalization.None,
                                    keyboardType=KeyboardType.Text,
                                    autoCorrect=false
                                ),
                                textStyle=androidx.compose.ui.text.TextStyle(color=Color.Black,fontSize=15.sp),
                                cursorBrush=SolidColor(Color(0xFF3B82F6)),
                                modifier=Modifier.weight(1f).fillMaxHeight().focusRequester(chatFocusRequester),
                                decorationBox={innerTextField ->
                                    Box(Modifier.fillMaxSize(), contentAlignment=Alignment.CenterStart) {
                                        if(input.isEmpty()) Text("Type a message...",color=Color(0xFF8A8F98),fontSize=15.sp)
                                        innerTextField()
                                    }
                                }
                            )
                            val isRoomInputBlank = input.isBlank()
                            Box(
                                Modifier.size(36.dp)
                                    .clip(CircleShape)
                                    .background(if(isRoomInputBlank) Color(0xFF9CA3AF) else Color(0xFF3B82F6))
                                    .clickable(enabled=!isRoomInputBlank) {
                                        if (publicMsgOff && !canSendRoomPublicMessage) {
                                            showPublicMessageClosedToast = true
                                        } else {
                                        val text=input.trim()
                                        val createdAt=System.currentTimeMillis()
                                        val messageId="$accountId-$createdAt"
                                        val mentions=pendingMentions.filter { text.contains("@${it.name}") }
                                        val outgoing=NativeRoomMessage(messageId,text,roomDisplayName,roomDisplayImage,accountId,createdAt,"message",mentions)
                                        messages.add(outgoing)
                                        val mentionsJson=JSONArray().apply { mentions.forEach { put(JSONObject().put("accountId",it.accountId).put("name",it.name)) } }
                                        roomSocketRef.value?.takeIf{it.connected()}?.emit(
                                            "room_message",
                                            JSONObject().put("id",messageId).put("roomId",roomId).put("senderId",accountId)
                                                .put("senderAccountId",accountId).put("senderName",roomDisplayName)
                                                .put("senderAvatar",roomDisplayImage).put("senderImage",roomDisplayImage)
                                                .put("text",text).put("mentions",mentionsJson).put("type","message").put("createdAt",createdAt)
                                        )
                                        inputValue=TextFieldValue("")
                                        pendingMentions=emptyList()
                                        }
                                    },
                                contentAlignment=Alignment.Center
                            ) {
                                Icon(Icons.Default.Send,null,tint=Color.White,modifier=Modifier.size(18.dp).padding(start=2.dp))
                            }
                        }
                    }
                } else {
                    Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal=8.dp, vertical=6.dp).height(44.dp),verticalAlignment=Alignment.CenterVertically){
                        Row(Modifier.weight(1f),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)){
                            RoomFooterButton(onClick={showChat=true},description="Say Hi Chat",icon=R.drawable.ic_hurry_chat)
                            val selfSeatIndex=seats.indexOfFirst{it.occupied && it.accountId==accountId}
                            if(selfSeatIndex>=0) {
                                val seat = seats[selfSeatIndex]
                                Box(Modifier.size(42.dp).clip(CircleShape).background(Color.Black.copy(alpha=.30f)).clickable(enabled = !seat.muted){ val nextMuted = !seat.selfMuted; seats[selfSeatIndex]=seat.copy(selfMuted=nextMuted); roomSocketRef.value?.emit("room_seat_action", JSONObject().put("roomId", roomId).put("userId", accountId).put("action", "self_mute").put("seatNumber", seat.number).put("isMuted", nextMuted)) },contentAlignment=Alignment.Center) {
                                    Icon(imageVector = if (seat.muted || seat.selfMuted) Icons.Default.MicOff else Icons.Default.Mic,contentDescription = "Microphone",tint = if (seat.muted) Color.LightGray else Color.White,modifier = Modifier.size(28.dp))
                                }
                            }
                            if(selfSeatIndex>=0) RoomFooterButton(onClick={showEmoji=true},description="Emoji",icon=R.drawable.ic_hurry_emoji)
                        }
                        AsyncImage(
                            model="https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/file_0000000019c4821180028eebae10dbfc.png",
                            contentDescription="Gift",
                            modifier=Modifier.size(56.dp).clickable{showGift=true}.scale(1.1f),
                            contentScale=ContentScale.Fit
                        )
                        Row(Modifier.weight(1f),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp,Alignment.End)){
                            RoomFooterButton(onClick={showMessageSheet=true},description="Message Box Menu",icon=R.drawable.ic_hurry_mail,hasUnread=roomHasUnreadMessages)
                            RoomFooterButton(onClick={showMenu=true},description="Apps Menu",icon=R.drawable.ic_hurry_apps)
                        }
                    }
                }
            }
        }
        }

        // Full-page Black/50 chat layer. It is independent of the seat layout:
        // arrow stays at the top, messages use all space down to the composer,
        // and the transparent reserved composer slot leaves the original input
        // row (48dp field + original padding/width) completely untouched.
        if (showChat) {
            Column(
                Modifier.fillMaxSize().imePadding().zIndex(50f)
            ) {
                Column(
                    Modifier.weight(1f).fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.50f))
                        .clickable { showChat = false; keyboardController?.hide() }
                ) {
                    Box(Modifier.fillMaxWidth().height(80.dp)) {
                        IconButton(
                            onClick = { showChat = false; keyboardController?.hide() },
                            modifier = Modifier.align(Alignment.TopEnd).padding(top = 40.dp, end = 12.dp).size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = "Close room chat",
                                tint = Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        RoomChatMessages(
                            state = chatListState,
                            messages = messages,
                            kickNotices = kickNotices,
                            showOfficialAnnouncement = showOfficialAnnouncement,
                            clearedBy = clearedBy,
                            announcement = announcement,
                            showRoomSettingsAnnouncementInChat = showRoomSettingsAnnouncementInChat,
                            roomOwnerId = roomOwner.accountId.ifBlank { roomOwner.id },
                            roomAdminIds = roomAdminIds,
                            isCovered = isCovered,
                            onSelectMessage = { selectedChatProfile = it },
                            onSelectMention = { mention -> selectedChatProfile = NativeRoomMessage("mention-profile-${mention.accountId}", "", mention.name, "", mention.accountId) },
                            onCopyMessage = { text ->
                                clipboard.setText(AnnotatedString(text))
                                roomToastText = "Copy to clipboard"
                            }
                        )
                    }
                }
                // Keep the room chat composer above the black/50 chat layer.
                Row(
                    Modifier.fillMaxWidth().navigationBarsPadding()
                        .background(Color.White)
                        .padding(horizontal=16.dp, vertical=12.dp)
                        .zIndex(51f),
                    verticalAlignment=Alignment.CenterVertically,
                    horizontalArrangement=Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(onClick={roomImagePickerLauncher.launch("image/*")}, modifier=Modifier.size(28.dp)) {
                        Image(painterResource(R.drawable.ic_hurry_chat_image), contentDescription="Image", modifier=Modifier.size(26.dp))
                    }
                    Row(
                        Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFFF3F4F6)).padding(start=16.dp, end=6.dp),
                        verticalAlignment=Alignment.CenterVertically
                    ) {
                        androidx.compose.foundation.text.BasicTextField(
                            value=inputValue,
                                onValueChange={ inputValue=it },
                            singleLine=true,
                            keyboardOptions=KeyboardOptions(
                                capitalization=KeyboardCapitalization.None,
                                    keyboardType=KeyboardType.Text,
                                    autoCorrect=false
                            ),
                            textStyle=androidx.compose.ui.text.TextStyle(color=Color.Black,fontSize=15.sp),
                            cursorBrush=SolidColor(Color(0xFF3B82F6)),
                            modifier=Modifier.weight(1f).fillMaxHeight().focusRequester(chatFocusRequester),
                            decorationBox={innerTextField ->
                                Box(Modifier.fillMaxSize(), contentAlignment=Alignment.CenterStart) {
                                    if(input.isEmpty()) Text("Type a message...",color=Color(0xFF8A8F98),fontSize=15.sp)
                                    innerTextField()
                                }
                            }
                        )
                        val isRoomInputBlank = input.isBlank()
                        Box(
                            Modifier.size(36.dp).clip(CircleShape)
                                .background(if(isRoomInputBlank) Color(0xFF9CA3AF) else Color(0xFF3B82F6))
                                .clickable(enabled=!isRoomInputBlank) {
                                    if (publicMsgOff && !canSendRoomPublicMessage) {
                                        showPublicMessageClosedToast = true
                                    } else {
                                        val text=input.trim()
                                        val createdAt=System.currentTimeMillis()
                                        val messageId="$accountId-$createdAt"
                                        val mentions=pendingMentions.filter { text.contains("@${it.name}") }
                                        val outgoing=NativeRoomMessage(messageId,text,roomDisplayName,roomDisplayImage,accountId,createdAt,"message",mentions)
                                        messages.add(outgoing)
                                        val mentionsJson=JSONArray().apply { mentions.forEach { put(JSONObject().put("accountId",it.accountId).put("name",it.name)) } }
                                        roomSocketRef.value?.takeIf{it.connected()}?.emit(
                                            "room_message",
                                            JSONObject().put("id",messageId).put("roomId",roomId).put("senderId",accountId)
                                                .put("senderAccountId",accountId).put("senderName",roomDisplayName)
                                                .put("senderAvatar",roomDisplayImage).put("senderImage",roomDisplayImage)
                                                .put("text",text).put("mentions",mentionsJson).put("type","message").put("createdAt",createdAt)
                                        )
                                        inputValue=TextFieldValue("")
                                        pendingMentions=emptyList()
                                    }
                                },
                            contentAlignment=Alignment.Center
                        ) {
                            Icon(Icons.Default.Send,null,tint=Color.White,modifier=Modifier.size(18.dp).padding(start=2.dp))
                        }
                    }
                }
            }
        }

        val sideBannerImages = listOf(
            "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/IMG_20261018_165235.png",
            "https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/1788339681679~2.jpg"
        )
        val sideBannerPager = androidx.compose.foundation.pager.rememberPagerState(pageCount = { sideBannerImages.size })
        LaunchedEffect(Unit) { while (true) { kotlinx.coroutines.delay(3000); if (!sideBannerPager.isScrollInProgress) sideBannerPager.animateScrollToPage((sideBannerPager.currentPage + 1) % sideBannerImages.size) } }

        // BOTTOM RIGHT CORNER (Task Image Reverted back here)
        Column(modifier=Modifier.align(Alignment.BottomEnd).padding(end=10.dp, bottom=68.dp).heightIn(max=380.dp).verticalScroll(rememberScrollState()),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(1.dp)) {
            // Lucky Bag sits in its own row immediately BEFORE the room banner.
            if (showFloatingLuckyBag) {
                Row(
                    modifier = Modifier
                        .padding(bottom = 4.dp)
                        .clickable {
                            // A previous claim on this device reopens the same confirmation card.
                            luckyBagClaimError = null
                            val savedClaim = appContext.getSharedPreferences("lucky_bag_claims", android.content.Context.MODE_PRIVATE)
                                .getLong("claim_" + luckyBagId + "_" + canonicalWalletUserId, 0L)
                            if (savedClaim > 0L) {
                                luckyBagClaimAmount = savedClaim
                                showLuckyBagClaimedCard = true
                            } else {
                                showLuckyBagCenterReveal = true
                            }
                            showFloatingLuckyBag = false
                        },
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.width(56.dp).height(62.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        AsyncImage(
                            model = "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/IMG_20261021_170707.png",
                            contentDescription = "Lucky Bag Sent",
                            modifier = Modifier.size(54.dp).graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                                .drawWithCache {
                                    val edgeFade = Brush.horizontalGradient(
                                        0f to Color.Transparent, 0.12f to Color.White,
                                        0.88f to Color.White, 1f to Color.Transparent
                                    )
                                    onDrawWithContent { drawContent(); drawRect(edgeFade, blendMode = BlendMode.DstIn) }
                                },
                            contentScale = ContentScale.Fit
                        )
                        Text(
                            text = "%d:%02d".format(luckyBagSecondsLeft / 60, luckyBagSecondsLeft % 60),
                            color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 1
                        )
                    }
                }
            }

            // Minimized Wild Party stays composed and keeps its live round/state.
            // Tapping its logo resumes the exact same running game without reloading.
            if (wildPartyMinimized) {
                AsyncImage(
                    model = "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/file_000000009d808211b8ffb7c2183b4ef5.png",
                    contentDescription = "Resume Wild Party",
                    modifier = Modifier.padding(bottom = 2.dp).size(48.dp).clickable {
                        wildPartyMinimized = false
                        showWildParty = true
                    },
                    contentScale = ContentScale.Fit
                )
            }
            // Keep Dragon Battle composed while minimized so its live round resumes in place.
            if (dragonBattleMinimized) {
                AsyncImage(
                    model = "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/IMG_20261121_020906.png",
                    contentDescription = "Resume Dragon Battle",
                    modifier = Modifier.padding(bottom = 4.dp).size(54.dp).clickable {
                        dragonBattleMinimized = false
                        showDragonBattle = true
                    },
                    contentScale = ContentScale.Fit
                )
            }
            Column(horizontalAlignment=Alignment.CenterHorizontally, modifier = Modifier.offset(y = 6.dp)) {
                androidx.compose.foundation.pager.HorizontalPager(state = sideBannerPager,modifier = Modifier.size(52.dp, 72.dp).clip(RoundedCornerShape(8.dp))) { page ->
                    AsyncImage(model = sideBannerImages[page],contentDescription = "Room side banner",modifier = Modifier.fillMaxSize().clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) { if (page == 0) onRoomSupport() },contentScale = ContentScale.Crop)
                }
                Row(horizontalArrangement=Arrangement.spacedBy(2.dp), verticalAlignment=Alignment.CenterVertically, modifier=Modifier.padding(top=2.dp)) {
                    repeat(sideBannerImages.size) { i ->
                        val isActive = sideBannerPager.currentPage == i
                        Box(Modifier.size(if (isActive) 6.dp else 4.dp).clip(CircleShape).background(if (isActive) Color.White else Color.White.copy(alpha=.4f)))
                    }
                }
            }
            
            // Task Image reverted back to bottom right above games
            TransparentGreenImage(url="https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/IMG-20260902-WA0066.jpg",modifier=Modifier.offset(y = 12.dp).padding(top=1.dp).size(52.dp).clickable { showRoomTask=true })
            AsyncImage(model="https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/file_000000002a8c81fab8232bebeeeb45cc.png",contentDescription="Games",modifier=Modifier.size(50.dp).clickable { showGames=true },contentScale=ContentScale.Fit)
        }
        
        RoomPageBlock {
Column(Modifier.align(Alignment.BottomStart).padding(bottom=LocalConfiguration.current.screenHeightDp.dp*0.33f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
            entryToasts.toList().forEach { toast ->
                key(toast.id) {
                    val visibleState = remember { androidx.compose.animation.core.MutableTransitionState(false).apply { targetState = true } }
                    AnimatedVisibility(
                        visibleState = visibleState, 
                        enter=slideInHorizontally(initialOffsetX={ _ -> screenWidthPx }, animationSpec = tween(durationMillis = 800, easing = LinearOutSlowInEasing)) + fadeIn(animationSpec = tween(800))
                    ) {
                        // Blue banner - thoda transparent mix aur full username dikhane ke liye weight(1f) add kiya hai
                        Row(Modifier.fillMaxWidth(0.80f).clip(RoundedCornerShape(topStart=50.dp,bottomStart=50.dp,topEnd=0.dp,bottomEnd=0.dp)).background(Brush.horizontalGradient(listOf(Color(0xCC2563EB),Color(0x802563EB),Color.Transparent))).padding(start=10.dp,end=10.dp,top=2.dp,bottom=2.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                            FallbackAsyncImage(model=toast.image,contentDescription=toast.name,modifier=Modifier.size(24.dp).clip(CircleShape),contentScale=ContentScale.Crop)
                            Text(toast.name,color=Color.White,fontSize=13.sp,fontWeight=androidx.compose.ui.text.font.FontWeight.SemiBold,maxLines=1,modifier=Modifier.weight(1f, fill=false))
                            Text("Enter the Room",color=Color.White,fontSize=12.sp,fontWeight=FontWeight.Bold,maxLines=1)
                        }}
                }
            }
        }
        }

        RoomPageBlock {
            if (roomVehiclePreview.isNotBlank()) {
                // Keep the Room playback presentation identical to the Store full-screen
                // Vehicle preview: same full width, vertical alignment, fades and V1 keying.
                val previewItemId = roomVehiclePreviewItemId
                val alignBottom = previewItemId == "v9" || previewItemId == "v7" || previewItemId == "v2" || previewItemId == "v10"
                val topFade = when (previewItemId) {
                    "v10" -> 0.05f
                    "v9", "v7", "v6" -> 0.15f
                    else -> 0.10f
                }
                val bottomFade = when (previewItemId) {
                    "v10" -> 0.05f
                    "v2", "v4", "v6" -> 0.15f
                    "v9", "v7" -> 0.0f
                    else -> 0.10f
                }
                StoreVideoPreview(
                    url = roomVehiclePreview,
                    modifier = Modifier
                        .align(if (alignBottom) Alignment.BottomCenter else Alignment.Center)
                        .zIndex(50f)
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    pause = false,
                    topFade = topFade,
                    bottomFade = bottomFade,
                    removeGreenScreen = previewItemId == "v1",
                    mute = false,
                    loop = false,
                    cacheVideo = true,
                    onComplete = {
                        if (pendingRoomVehicleEntries.isNotEmpty()) {
                            val nextEntry = pendingRoomVehicleEntries.removeAt(0)
                            roomVehiclePreview = nextEntry.first
                            roomVehiclePreviewItemId = nextEntry.second
                        } else {
                            roomVehiclePreview = ""
                            roomVehiclePreviewItemId = ""
                        }
                    }
                )
            }
        }

        RoomPageBlock {
if (musicControllerVisible && miniMusicTrack != null && !musicControllerMinimized && !MusicPlaybackState.isMusicPageOpen) {
            val track = miniMusicTrack!!
            Box(
                Modifier.align(Alignment.BottomCenter)
                    .zIndex(10f)
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 60.dp)
                    .fillMaxWidth()
                    .height(160.dp)
                    // Transform the entire card layer, including its black surface and rounded clip.
                    .graphicsLayer { translationY = musicCardDragOffset.value }
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragEnd = {
                                val shouldMinimize = musicCardDragOffset.value > 90.dp.toPx()
                                musicDragScope.launch {
                                    if (shouldMinimize) {
                                        musicCardDragOffset.animateTo(
                                            targetValue = size.height.toFloat(),
                                            animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
                                        )
                                        musicControllerMinimized = true
                                        musicCardDragOffset.snapTo(0f)
                                    } else {
                                        musicCardDragOffset.animateTo(
                                            targetValue = 0f,
                                            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
                                        )
                                    }
                                }
                            },
                            onDragCancel = {
                                musicDragScope.launch {
                                    musicCardDragOffset.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
                                    )
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                musicDragScope.launch { musicCardDragOffset.snapTo((musicCardDragOffset.value + dragAmount.y).coerceIn(0f, size.height.toFloat())) }
                            }
                        )
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Column(Modifier.fillMaxSize()) {
                    if (showMusicVolumeView) {
                        Box(Modifier.fillMaxWidth().height(30.dp), contentAlignment = Alignment.Center) {
                            IconButton(onClick = { showMusicVolumeView = false }, modifier = Modifier.align(Alignment.CenterStart).size(30.dp)) {
                                Icon(Icons.Default.ArrowBack, "Back", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                            Text("Volume", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Normal)
                        }
                        Spacer(Modifier.height(28.dp))
                        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VolumeMute, "Open music page", tint = Color.White,
                                modifier = Modifier.size(21.dp).clickable {
                                    showMusicVolumeView = false
                                    musicControllerMinimized = true
                                    MusicPlaybackState.isMusicPageOpen = true
                                })
                            Spacer(Modifier.width(12.dp))
                            ThickBar(progress = musicVolume, height = 6.dp, activeColor = Color.White,
                                inactiveColor = Color.White.copy(alpha = 0.42f), modifier = Modifier.weight(1f),
                                onProgressChange = { value ->
                                    musicVolume = value
                                    runCatching { appContext.startService(Intent(appContext, RoomMusicService::class.java)
                                        .setAction(RoomMusicService.ACTION_VOLUME).putExtra(RoomMusicService.EXTRA_VOLUME, value)) }
                                })
                            Spacer(Modifier.width(12.dp))
                            Icon(Icons.Default.VolumeUp, "Volume up", tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.height(28.dp))
                    } else {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { musicClose() }, modifier = Modifier.size(34.dp)) {
                                Icon(Icons.Default.PowerSettingsNew, "Stop music", tint = Color.White, modifier = Modifier.size(25.dp))
                            }
                            IconButton(onClick = { musicControllerMinimized = true }, modifier = Modifier.size(34.dp)) {
                                Icon(Icons.Default.CloseFullscreen, "Minimize", tint = Color.White, modifier = Modifier.size(22.dp))
                            }
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(track.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Normal, maxLines = 1,
                            overflow = TextOverflow.Clip, modifier = Modifier.fillMaxWidth().basicMarquee(iterations = Int.MAX_VALUE))
                        Spacer(Modifier.height(5.dp))
                        val progressFraction = if (musicDuration > 0f) (musicCurrentTime / musicDuration).coerceIn(0f, 1f) else 0f
                        ThickBar(progress = progressFraction, height = 5.dp, activeColor = Color.White,
                            inactiveColor = Color.White.copy(alpha = 0.48f), modifier = Modifier.fillMaxWidth(),
                            onProgressChange = { value ->
                                if (musicDuration > 0f) {
                                    musicCurrentTime = value * musicDuration
                                    runCatching { appContext.startService(Intent(appContext, RoomMusicService::class.java)
                                        .setAction(RoomMusicService.ACTION_SEEK)
                                        .putExtra(RoomMusicService.EXTRA_POSITION_MS, (musicCurrentTime * 1000f).toInt())) }
                                }
                            })
                        Row(Modifier.fillMaxWidth().padding(top = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(formatMusicTime(musicCurrentTime), color = Color.White.copy(alpha = .7f), fontSize = 9.sp, fontWeight = FontWeight.Normal)
                            Text(formatMusicTime(musicDuration), color = Color.White.copy(alpha = .7f), fontSize = 9.sp, fontWeight = FontWeight.Normal)
                        }
                        Spacer(Modifier.height(5.dp))
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.VolumeUp, "Volume", tint = Color.White, modifier = Modifier.size(21.dp))
                            ThickBar(
                                progress = musicVolume,
                                height = 8.dp,
                                activeColor = Color(0xFF3B82F6),
                                inactiveColor = Color.White.copy(alpha = 0.20f),
                                modifier = Modifier.weight(1f),
                                onProgressChange = { value ->
                                    musicVolume = value
                                    runCatching {
                                        appContext.startService(Intent(appContext, RoomMusicService::class.java)
                                            .setAction(RoomMusicService.ACTION_VOLUME)
                                            .putExtra(RoomMusicService.EXTRA_VOLUME, value))
                                    }
                                }
                            )
                            Box(Modifier.size(42.dp).clip(CircleShape).background(Color.White).clickable { musicPlayPause() }, contentAlignment = Alignment.Center) {
                                Icon(if (isMusicPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    if (isMusicPlaying) "Pause" else "Play", tint = Color(0xFF202938), modifier = Modifier.size(25.dp))
                            }
                            IconButton(onClick = { musicNext() }, modifier = Modifier.size(34.dp)) {
                                Icon(Icons.Default.SkipNext, "Next", tint = Color.White, modifier = Modifier.size(27.dp))
                            }
                            Box(
                                Modifier.size(30.dp).clickable { musicReplayOne() },
                                contentAlignment = Alignment.Center
                            ) {
                                // Show the plain repeat icon until enabled; show Repeat One only after tapping.
                                Icon(
                                    if (repeatOneEnabled) Icons.Default.RepeatOne else Icons.Default.Repeat,
                                    contentDescription = if (repeatOneEnabled) "Repeat current song" else "Repeat",
                                    tint = Color.White,
                                    modifier = Modifier.size(25.dp)
                                )
                            }
                            Icon(Icons.Default.QueueMusic, "Open Music page", tint = Color.White,
                                modifier = Modifier.size(25.dp).clickable {
                                    // Open the song list from the controller queue icon.
                                    musicControllerMinimized = true
                                    openMusicPageFromController = true
                                    showMenu = true
                                })
                        }
                    }
                }
            }
        }
        }
        if(showExitMenu) RoomExitMenu(
            onKeep={showExitMenu=false;onKeepRoom(roomName,roomImage,roomId)},
            onExit={showExitMenu=false;stopMusicForRoomExit();onClose()},
            onDismiss={showExitMenu=false}
        )
        if(showMenu) Fourgride(
            roomId = roomId,
            openMusicPageRequest = openMusicPageFromController,
            onClose={showMenu=false},
            onClearChat={
                messages.clear()
                kickNotices.clear()
                showRoomSettingsAnnouncementInChat = false
                appContext.getSharedPreferences("room_settings", android.content.Context.MODE_PRIVATE).edit().putBoolean("announcement_chat_visible_$roomId", false).apply()
                roomToastText = "Public message are cleared"
                showOfficialAnnouncement = false
                // Keep the Room Settings announcement; this action only clears chat messages.
                val clearerName = roomDisplayName.ifBlank { "User" }
                clearedBy = clearerName
                roomSocketRef.value?.emit("room_clear_chat",JSONObject().put("roomId",roomId).put("userId",accountId).put("userName",clearerName).put("timestamp",System.currentTimeMillis()))
                showMenu=false
            },
            publicMsgOff=publicMsgOff,
            isRoomOwner=isRoomOwner || currentUserSeatIds.any { it in roomAdminIds },
            onTogglePublicMsg={
                if (isRoomOwner || currentUserSeatIds.any { it in roomAdminIds }) {
                    publicMsgOff = !publicMsgOff
                    roomSocketRef.value?.takeIf { it.connected() }?.emit("room_settings_update", JSONObject()
                        .put("roomId", roomId).put("userId", accountId).put("publicMsgOff", publicMsgOff))
                }
            },
            speaker=speaker,
            onToggleSpeaker={
                speaker = !speaker
                roomEffectsPrefs.edit().putBoolean(speakerPrefKey, speaker).apply()
            },
            entryEffect=entryEffectEnabled,
            giftEffect=giftEffectEnabled,
            onToggleEntryEffect={
                entryEffectEnabled = !entryEffectEnabled
                roomEffectsPrefs.edit().putBoolean(entryEffectPrefKey, entryEffectEnabled).apply()
            },
            onToggleGiftEffect={
                giftEffectEnabled = !giftEffectEnabled
                roomEffectsPrefs.edit().putBoolean(giftEffectPrefKey, giftEffectEnabled).apply()
            },
            onMusicPlaylistChanged={ tracks ->
                musicPlaylist = tracks
                MusicPlaybackState.playlist = tracks
                val selectedUri = miniMusicTrack?.url ?: MusicPlaybackState.currentPlayingUri
                val selectedIndex = tracks.indexOfFirst { it.url == selectedUri }
                if (selectedIndex >= 0) currentTrackIndex = selectedIndex
            },
            onMusicPlay={ track ->
                musicControllerVisible = true
                miniMusicTrack = track
                persistLastRoomTrack(track)
                MusicPlaybackState.wasPlayedOnMusicPage = true
                MusicPlaybackState.musicPageBackPressed = false
                if (musicPlaylist.none { it.url == track.url }) musicPlaylist = listOf(track)
                currentTrackIndex = musicPlaylist.indexOfFirst { it.url == track.url }.coerceAtLeast(0)
                musicCurrentTime = 0f
                musicDuration = if (track.duration > 0) track.duration / 1000f else 180f
                isMusicPlaying = true
                MusicPlaybackState.currentPlayingUri = track.url
                MusicPlaybackState.currentTrack = track
                MusicPlaybackState.isPlaying = true
                musicControllerMinimized = false
            },
            onMusicPlaybackChanged={ track, playing ->
                miniMusicTrack = track
                persistLastRoomTrack(track)
                isMusicPlaying = playing
                MusicPlaybackState.currentPlayingUri = track.url
                MusicPlaybackState.currentTrack = track
                MusicPlaybackState.isPlaying = playing
            },
            onMusicMinimize={ track ->
                openMusicPageFromController = false
                musicControllerVisible = track != null
                if (track != null) {
                    miniMusicTrack = track
                    persistLastRoomTrack(track)
                    MusicPlaybackState.musicPageBackPressed = MusicPlaybackState.wasPlayedOnMusicPage && MusicPlaybackState.isPlaying
                    MusicPlaybackState.isMusicPageOpen = false
                    musicControllerMinimized = false
                } else {
                    MusicPlaybackState.musicPageBackPressed = false
                    musicControllerMinimized = false
                }
                showMenu = false
            },
            onOpenStore={ view ->
                storeInitialView = if (view.equals("bag", true)) "bag" else "store"
                showStore = true
                showMenu = false
            },
            luckyBagSenderId = canonicalWalletUserId,
            luckyBagSenderName = roomDisplayName.ifBlank { "User" },
            luckyBagSenderImage = roomDisplayImage,
            luckyBagRoomId = roomId,
            luckyBagRoomName = roomName,
            onLuckyBagSent={ name, image, amount, recipients, bagId, expiresAt ->
                luckyBagSenderName = name
                luckyBagSenderImage = image
                luckyBagId = bagId
                luckyBagExpiresAt = expiresAt
                luckyBagClaimAmount = amount
                luckyBagClaimError = null
                // Show the same room Lucky Packet image to its sender too.
                showFloatingLuckyBag = true
            }
        )
        
        // Block all room controls while a full-screen Store/Bag/Games/Lucky Bag
        // overlay is open. Keep this blocker below the actual overlay content.
        if (showStore || showGames || showLuckyBagCenterReveal || showLuckyBagClaimedCard) {
            Box(
                Modifier.fillMaxSize()
                    .clickable(
                        indication = null,
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    ) { }
            )
        }

        val roomFollowers = followedRoomUsers.filter { it.accountId != roomOwner.accountId && it.accountId != roomOwner.id }
        if(showStore) StorePage(onBack={showStore=false}, initialView=storeInitialView, userId=accountId.ifBlank{roomOwner.accountId.ifBlank{roomOwner.id}})
        if(showInfo) RoomInfo(
            isOpen=true,
            onClose={showInfo=false},
            isRoomOwner=accountId==roomOwner.accountId.ifBlank{roomOwner.id},
            roomOwner=RoomInfoOwner(id=roomOwner.id,uid=roomOwner.uid,accountId=roomOwner.accountId,name=roomOwner.name,image=roomOwner.image,vipId=ownerVipId,vipIdBadge=ownerVipBadge),
            roomData=RoomInfoData(roomName,roomImage,announcement,roomId),
            roomFollowers=roomFollowers,
            roomAdmins=roomAdminUsers,
            onOpenProfile={ follower -> selectedChatProfile=NativeRoomMessage("member-profile-" + follower.accountId,"",follower.name,follower.image,follower.accountId);showInfo=false },
            onOpenOwnerChat={ owner ->
                val ownerChatId = owner.accountId.trim()
                if (ownerChatId.isNotBlank() && ownerChatId != accountId) {
                    selectedOfficialChat = Triple(ownerChatId, owner.name.ifBlank { "Room Owner" }, owner.image)
                    showInfo = false
                    showMessageSheet = false
                    showMenu = false
                }
            },
            onCopyId={ id -> clipboard.setText(AnnotatedString(id));copiedId=id },
            copied=copiedId==roomId,
            currentUserId=accountId,
            isRoomFollowed=followed,
            onToggleRoomFollow={ next ->
                if (accountId.isNotBlank() && accountId != "guest" && accountId != roomId) {
                    val previousFollowState = followed
                    followed = next
                    onFollowToggle(roomId,next)
                    CoroutineScope(Dispatchers.IO).launch {
                        val result = runCatching {
                            val encodedUser=java.net.URLEncoder.encode(accountId,"UTF-8")
                            val encodedRoom=java.net.URLEncoder.encode(roomId,"UTF-8")
                            val url=if(next) java.net.URL("https://zyora-server.onrender.com/api/room-follows") else java.net.URL("https://zyora-server.onrender.com/api/room-follows?userId=$encodedUser&roomId=$encodedRoom")
                            val conn=(url.openConnection() as java.net.HttpURLConnection).apply {
                                requestMethod=if(next) "POST" else "DELETE"
                                connectTimeout=8000; readTimeout=8000
                                if(next) { doOutput=true; setRequestProperty("Content-Type","application/json") }
                            }
                            try {
                                if(next) {
                                    val payload=JSONObject().put("userId",accountId).put("roomId",roomId)
                                        .put("roomName",roomName).put("roomImage",roomImage).put("roomCountry",roomOwner.country)
                                        .put("followerName",roomDisplayName).put("followerImage",roomDisplayImage)
                                        .put("followerCountry",cachedCurrentProfile?.optString("country").orEmpty())
                                    conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
                                }
                                if(conn.responseCode !in 200..299) error("Room follow update failed")
                            } finally { conn.disconnect() }
                        }
                        if (result.isFailure) {
                            android.util.Log.e("ZyoraRoomFollow","Room follow update failed",result.exceptionOrNull())
                            withContext(Dispatchers.Main) {
                                followed = previousFollowState
                                android.widget.Toast.makeText(appContext, "Room follow update failed. Try again.", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            },
            onShareToUser={ fan ->
                pendingRoomInvitationJson=JSONObject().put("roomId",roomId).put("roomName",roomName)
                    .put("roomDp",roomImage).put("roomOwnerId",roomOwner.accountId.ifBlank{roomOwner.id})
                    .put("roomIdText",roomId).put("inviteId",java.util.UUID.randomUUID().toString()).toString()
                selectedOfficialChat=Triple(fan.accountId,fan.name,fan.image)
                showInfo=false
            }
        )
        if (showRoomCup) RoomCupSheet(roomId, accountId, roomDisplayName, roomDisplayImage, { showRoomCup = false; showRoomCupOnline = false }, showRoomCupOnline, activeRoomUsers, roomOwner.accountId.ifBlank{roomOwner.id}, roomAdminIds, { user -> selectedChatProfile=NativeRoomMessage("active-profile-" + user.accountId,"",user.name,user.image,user.accountId); showRoomCup=false; showRoomCupOnline=false }, { id -> clipboard.setText(AnnotatedString(id)); copiedId=id })
        if(showSeatInviteUsers) ActiveUsers(
            isOpen=true,
            onClose={showSeatInviteUsers=false},
            roomUsers=activeRoomUsers.filter { it.accountId != accountId },
            excludedAccountIds=seats.asSequence().filter { it.occupied }.map { it.accountId }.filter { it.isNotBlank() }.toSet(),
            roomOwnerId=roomOwner.accountId.ifBlank{roomOwner.id},
            roomAdminIds=roomAdminIds,
            onOpenProfile={},
            onCopyUserId={},
            inviteMode=true,
            onInvite={ user ->
                val inviteSeat=seats.firstOrNull { !it.occupied && !it.locked }
                val socket=roomSocketRef.value
                if(user.accountId.isNotBlank() && user.accountId != accountId && inviteSeat != null && socket?.connected()==true) {
                    socket.emit("room_seat_invite_send", JSONObject()
                        .put("roomId",roomId)
                        .put("inviterId",accountId)
                        .put("inviterName",roomDisplayName.ifBlank{"User"})
                        .put("inviterImage",roomDisplayImage)
                        .put("targetId",user.accountId)
                        .put("seatNumber",inviteSeat.number))
                    showSeatInviteUsers=false
                } else if(inviteSeat==null) {
                    android.widget.Toast.makeText(appContext,"No available unlocked seat",android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    android.widget.Toast.makeText(appContext,"Unable to send invitation",android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        )
        if(showSettings && accountId.isNotBlank() && accountId == roomOwner.accountId.ifBlank { roomOwner.id }) RoomSettingPage(initial = RoomSettingsDataNative(roomName=roomName,roomDp=roomImage,announcement=announcement,micMode=roomMicMode,theme=roomTheme,isLocked=locked,roomPassword=roomPassword,admin=roomAdminIds,customTheme=appContext.getSharedPreferences("room_settings", android.content.Context.MODE_PRIVATE).getString("customTheme_$roomId", "").orEmpty()),roomOwnerId=roomId,currentUserId=accountId,roomMembers=followedRoomUsers,onBack={showSettings=false},onSave={saved->
            val newlyAddedAdminId = saved.admin.firstOrNull { it !in roomAdminIds }
            val newlyAddedAdminName = newlyAddedAdminId?.let { id ->
                followedRoomUsers.firstOrNull { it.accountId == id }?.name
                    ?: roomAdminUsers.firstOrNull { it.accountId == id }?.name
                    ?: activeRoomUsers.firstOrNull { it.accountId == id }?.name
                    ?: id
            }.orEmpty()
            roomName=saved.roomName;roomImage=saved.roomDp;announcement=saved.announcement;showRoomSettingsAnnouncementInChat=saved.announcement.isNotBlank();appContext.getSharedPreferences("room_settings", android.content.Context.MODE_PRIVATE).edit().putBoolean("announcement_chat_visible_$roomId", showRoomSettingsAnnouncementInChat).apply();roomMicMode=saved.micMode;roomTheme=saved.theme;roomCustomThemeUrl=saved.customTheme;roomAdminIds=saved.admin;locked=saved.isLocked;roomPassword=saved.roomPassword
            appContext.getSharedPreferences("room_settings", android.content.Context.MODE_PRIVATE).edit().putInt("micMode_$roomId", saved.micMode).putString("theme_$roomId", saved.theme).putString("customTheme_$roomId", saved.customTheme).putBoolean("room_locked_$roomId", saved.isLocked).putString("room_password_$roomId", saved.roomPassword).apply()
            CoroutineScope(Dispatchers.IO).launch { NativeIndexDatabase(appContext).saveRoomLock(roomId, saved.isLocked, saved.roomPassword) }
            roomSocketRef.value?.takeIf { it.connected() }?.emit("room_settings_update", JSONObject()
                .put("roomId", roomId).put("userId", accountId).put("roomOwnerId", roomId)
                .put("micMode", saved.micMode).put("roomName", saved.roomName).put("roomDp", saved.roomDp)
                .put("announcement", saved.announcement).put("theme", saved.theme)
                 .put("customTheme", saved.customTheme)
                .put("admin", JSONArray(saved.admin))
                .put("addedAdminId", newlyAddedAdminId ?: "")
                .put("addedAdminName", newlyAddedAdminName)
                .put("actorId", accountId)
                .put("actorName", latestRoomDisplayName.ifBlank { "Room Owner" })
                .put("actorImage", latestRoomDisplayImage)
                .put("isLocked", saved.isLocked)
                .put("updatedAt", System.currentTimeMillis()))
        })
        luckyGiftNotice?.let { notice ->
            Box(Modifier.align(Alignment.TopStart).fillMaxWidth().offset(y = 12.dp).zIndex(250f)) {
                LuckyGiftNotificationSlider(notice = notice)
            }
        }
        if (showLuckyComboButton) {
            Box(Modifier.fillMaxSize().zIndex(251f)) {
                LuckyComboButton(
                    visible = true,
                    progress = (luckyComboTimeLeft / 5f).coerceIn(0f, 1f),
                    onClick = {
                        val previousGift = lastLuckyComboPayload
                        // Keep the combo control mounted and restart its
                        // five-second window while the replay is sent.
                        showLuckyComboButton = true
                        luckyComboTimeLeft = 5
                        luckyComboGeneration++
                        if (previousGift != null) {
                            val nextTransferId = "combo-$accountId-${System.currentTimeMillis()}-${System.nanoTime()}"
                            val comboTransfer = JSONObject(previousGift.toString())
                                .put("transferId", nextTransferId)
                                .put("eventId", nextTransferId)
                                .put("comboReplay", true)
                                .put("timestamp", System.currentTimeMillis())
                            roomSocketRef.value?.emit("coin_transfer", comboTransfer)
                        }
                    },
                )
            }
        }
        if(showGift) GiftPicker(
            open = true,
            onClose = { showGift = false },
            seats = seats,
            roomId = roomId,
            currentUserAccountId = accountId,
            currentUserName = roomDisplayName,
            currentUserImage = roomDisplayImage,
            roomUsers = activeRoomUsers,
            walletBalance = roomWalletBalance,
            onDebitWallet = { requested -> if (requested <= roomWalletBalance) roomWalletBalance else null },
            onSendCoinTransfer = { transfer ->
                val outgoingTransfer = JSONObject(transfer)
                if (outgoingTransfer.optBoolean("luckyGift", false)) {
                    lastLuckyComboPayload = JSONObject(outgoingTransfer.toString())

                    // Optimistically play the sender's own Lucky Gift immediately.
                    // The transfer ID is recorded first, so the server echo is ignored
                    // by luckyGiftReceivedListener instead of rendering the same gift twice.
                    val localTransferId = outgoingTransfer.optString("transferId")
                    if (localTransferId.isNotBlank() && seenLuckyGiftTransferIds.add(localTransferId)) {
                        if (seenLuckyGiftTransferIds.size > 200) {
                            synchronized(seenLuckyGiftTransferIds) {
                                while (seenLuckyGiftTransferIds.size > 150) {
                                    seenLuckyGiftTransferIds.remove(seenLuckyGiftTransferIds.first())
                                }
                            }
                        }
                        val localSenderId = outgoingTransfer.optString("senderId")
                        val localSenderName = outgoingTransfer.optString("senderName", "User")
                        val localSenderImage = outgoingTransfer.optString("senderImage")
                        val localGiftImageRaw = outgoingTransfer.optString("giftImage")
                            .ifBlank { outgoingTransfer.optString("luckyImage") }
                        val localGiftImage = when {
                            localGiftImageRaw.isBlank() -> ""
                            localGiftImageRaw.startsWith("http", true) -> localGiftImageRaw
                            else -> "https://zyora-voice-chat.vercel.app/" + localGiftImageRaw.trimStart('/')
                        }
                        val localRecipientsJson = outgoingTransfer.optJSONArray("recipientIds") ?: JSONArray()
                        val localRecipientIds = (0 until localRecipientsJson.length())
                            .map { localRecipientsJson.optString(it) }.toSet()
                        val localRecipientMode = when (outgoingTransfer.optString("recipientMode").lowercase()) {
                            "mic" -> RecipientMode.MIC
                            "room" -> RecipientMode.ROOM
                            else -> RecipientMode.SINGLE
                        }
                        val localIsCombo = outgoingTransfer.optBoolean("comboReplay", false)
                        val localMultiplier = outgoingTransfer.optInt("multiplier", 1).coerceAtLeast(1)
                        val localTimestamp = outgoingTransfer.optLong("timestamp", System.currentTimeMillis())
                        luckyGiftNotice = LuckyNotice(
                            id = localTransferId,
                            senderId = localSenderId,
                            senderName = localSenderName,
                            recipientName = outgoingTransfer.optString("recipientName", "User"),
                            senderImage = localSenderImage.ifBlank { ZYORA_DEFAULT_AVATAR_URL },
                            giftImage = localGiftImage,
                            multiplier = if (localIsCombo && luckyGiftNotice?.senderId == localSenderId) {
                                (luckyGiftNotice?.multiplier ?: localMultiplier) + 1
                            } else localMultiplier,
                            totalWinAmount = outgoingTransfer.optLong("luckyReturnAmount", 0L),
                            maxWinTimes = outgoingTransfer.optInt("luckyReturnPercent", 0),
                            recipientMode = localRecipientMode
                        )
                        showLuckyComboButton = true
                        luckyComboTimeLeft = 5
                        luckyComboGeneration++

                        if (localGiftImage.isNotBlank()) {
                            for (index in seats.indices) {
                                val seat = seats[index]
                                if (seat.occupied && seat.accountId in localRecipientIds) {
                                    seats[index] = seat.copy(
                                        gifSrc = localGiftImage,
                                        gifTimestamp = localTimestamp,
                                        gifDurationMs = 1800L
                                    )
                                    val seatNumber = seat.number
                                    Handler(Looper.getMainLooper()).postDelayed({
                                        val currentIndex = seats.indexOfFirst { it.number == seatNumber }
                                        if (currentIndex >= 0 && seats[currentIndex].gifTimestamp == localTimestamp) {
                                            seats[currentIndex] = seats[currentIndex].copy(
                                                gifSrc = "", gifTimestamp = 0L, gifDurationMs = 5000L
                                            )
                                        }
                                    }, 1800L)
                                }
                            }
                        }
                        Handler(Looper.getMainLooper()).postDelayed({
                            if (luckyGiftNotice?.id == localTransferId) {
                                luckyGiftNotice = luckyGiftNotice?.copy(isExiting = true)
                                Handler(Looper.getMainLooper()).postDelayed({
                                    if (luckyGiftNotice?.id == localTransferId) luckyGiftNotice = null
                                }, 450L)
                            }
                        }, 5000L)
                    }
                }
                roomSocketRef.value?.emit("coin_transfer", outgoingTransfer)
            },
            onSendGiftVideoPlay = { payload -> roomSocketRef.value?.emit("gift_video_play", JSONObject(payload)) }
        )
        if (roomGiftVideoPreview.isNotBlank()) {
            RoomGiftVideoOverlay(src = roomGiftVideoPreview) { roomGiftVideoPreview = "" }
        }
        if(showGames) GamesSheet(onClose={showGames=false},onWildParty={showGames=false;showWildParty=true},onDragonBattle={showGames=false;showDragonBattle=true})
        if(showRoomTask) Roomtask(onBack={showRoomTask=false})
        if (showWildParty || wildPartyMinimized) Wildparty(
            onClose = { showWildParty = false; wildPartyMinimized = false },
            onMinimize = { showWildParty = false; wildPartyMinimized = true },
            modifier = if (showWildParty) Modifier.fillMaxSize() else Modifier.size(1.dp).graphicsLayer { alpha = 0f }
        )
        BackHandler(enabled=showDragonBattle) { showDragonBattle=false; dragonBattleMinimized=false }
        if (showDragonBattle || dragonBattleMinimized) {
            DragonBattleGameHost(
                onClose = { showDragonBattle = false; dragonBattleMinimized = false },
                onMinimize = { showDragonBattle = false; dragonBattleMinimized = true },
                modifier = if (showDragonBattle) Modifier.fillMaxSize() else Modifier.size(1.dp).graphicsLayer { alpha = 0f }
            )
        }
        if(showEmoji) EmojiPicker(onClose={showEmoji=false},onSelectEmoji={ sticker ->
            val seatIndex=seats.indexOfFirst{it.occupied && it.accountId==accountId}
            if(seatIndex>=0){
                val timestamp=System.currentTimeMillis()
                val selected=seats[seatIndex]
                // Restore the original seat media selection: MP4 when supplied, otherwise the original image/GIF.
                val seatMediaUrl = sticker.videoUrl?.takeIf { it.isNotBlank() } ?: sticker.src
                seats[seatIndex]=selected.copy(
                    gifSrc=seatMediaUrl,
                    gifTimestamp=timestamp,
                    gifScale=sticker.seatScale
                )
                roomSocketRef.value?.emit("room_seat_action",JSONObject().put("roomId",roomId).put("userId",accountId).put("action","emoji").put("seatNumber",selected.number).put("user",JSONObject().put("name",roomDisplayName.ifBlank{"User"}).put("image",roomDisplayImage.ifBlank{ZYORA_DEFAULT_AVATAR_URL}).put("accountId",accountId)).put("src",seatMediaUrl).put("scale",sticker.seatScale).put("timestamp",timestamp))
                
                Handler(Looper.getMainLooper()).postDelayed({ if(seats.getOrNull(seatIndex)?.gifTimestamp==timestamp) seats[seatIndex]=seats[seatIndex].copy(gifSrc="",gifTimestamp=0L) },5000)
            }
        })
        selectedChatProfile?.let { profileMessage ->
            val profileId = profileMessage.senderAccountId
            RoomProfile(
                user = RoomUser(
                    id = profileId, uid = profileId, accountId = profileId,
                    name = profileMessage.sender.ifBlank { "User" }, image = profileMessage.senderImage,
                    isInSeat = seats.any { it.occupied && it.accountId == profileId },
                    isMuted = seats.firstOrNull { it.occupied && it.accountId == profileId }?.muted ?: false,
                    isLocked = seats.firstOrNull { it.occupied && it.accountId == profileId }?.locked ?: false
                ),
                isCurrentUser = profileId == accountId,
                isRoomOwner = profileId == roomOwner.accountId.ifBlank { roomOwner.id },
                canModerate = currentUserSeatIds.any { it == roomId || it == roomOwnerAccountId || it in roomAdminIds },
                onClose = { selectedChatProfile = null },
                onOpenPublicProfile = { profile -> selectedChatProfile = null; selectedSeat = 0; showSeatSheet = false; onOpenPublicProfile(profile) },
                onCopyId = { if (profileId.isNotBlank()) clipboard.setText(AnnotatedString(profileId)) },
                onMention = { name, targetId ->
                    val cleanName = name.trim().ifBlank { "User" }
                    run {
                        val cursor = inputValue.selection.start.coerceIn(0, input.length)
                        val insertion = (if (cursor > 0 && !input[cursor - 1].isWhitespace()) " " else "") + "@$cleanName "
                        val updated = input.substring(0, cursor) + insertion + input.substring(cursor)
                        inputValue = TextFieldValue(updated, selection=TextRange(cursor + insertion.length))
                    }
                    if (targetId.isNotBlank()) pendingMentions = (pendingMentions.filterNot { it.accountId == targetId } + NativeMention(targetId, cleanName))
                    showChat = true
                    selectedChatProfile = null
                },
                onFollow = { if (profileId.isNotBlank()) onFollowToggle(profileId, true) },
                onMessage = {
                    if (profileId.isNotBlank() && profileId != accountId) {
                        selectedOfficialChat = Triple(profileId, profileMessage.sender, profileMessage.senderImage)
                        selectedChatProfile = null
                    }
                },
                onInvite = {
                    val inviteSeat = seats.firstOrNull { !it.occupied && !it.locked }
                    val socket = roomSocketRef.value
                    if (profileId.isNotBlank() && profileId != accountId && inviteSeat != null && socket?.connected() == true) {
                        socket.emit(
                            "room_seat_invite_send",
                            JSONObject()
                                .put("roomId", roomId)
                                .put("inviterId", accountId)
                                .put("inviterName", roomDisplayName.ifBlank { "User" })
                                .put("inviterImage", roomDisplayImage)
                                .put("targetId", profileId)
                                .put("seatNumber", inviteSeat.number)
                        )
                        selectedChatProfile = null
                    }
                },
                onLeaveSeat = {
                    val index = seats.indexOfFirst { it.occupied && it.accountId == profileId }
                    if (index >= 0) {
                        val seatNumber = seats[index].number
                        seats[index] = NativeRoomSeat(number = seatNumber)
                        roomSocketRef.value?.emit("room_seat_action", JSONObject().put("roomId", roomId).put("userId", accountId).put("targetUserId", profileId).put("action", "leave").put("seatNumber", seatNumber))
                    }
                    selectedChatProfile = null
                },
                onMute = {
                    val index = seats.indexOfFirst { it.occupied && it.accountId == profileId }
                    if (index >= 0 && (accountId == roomOwner.accountId || accountId == roomOwner.id || accountId == roomOwner.uid || accountId == roomId || roomAdminIds.any { it.trim() in currentUserSeatIds })) {
                        val nextMuted = !seats[index].muted
                        seats[index] = seats[index].copy(muted = nextMuted)
                        roomSocketRef.value?.emit("room_seat_action", JSONObject().put("roomId", roomId).put("userId", accountId).put("action", "owner_mute").put("seatNumber", seats[index].number).put("isMuted", nextMuted))
                    }
                },
                onLock = {
                    val index = seats.indexOfFirst { it.occupied && it.accountId == profileId }
                    if (index >= 0 && (accountId == roomOwner.accountId || accountId == roomOwner.id || accountId == roomOwner.uid || accountId == roomId || roomAdminIds.any { it.trim() in currentUserSeatIds })) {
                        val seatNumber = seats[index].number
                        val nextLocked = !seats[index].locked
                        seats[index] = if (nextLocked) NativeRoomSeat(number = seatNumber, locked = true)
                            else seats[index].copy(locked = false)
                        roomSocketRef.value?.emit("room_seat_action", JSONObject().put("roomId", roomId).put("userId", accountId).put("action", "lock").put("seatNumber", seatNumber).put("isLocked", nextLocked))
                    }
                },
                onKickOut = { selectedTime ->
                    val durationMs = when (selectedTime) {
                        "15 minutes" -> 15L * 60_000L
                        "30 minutes" -> 30L * 60_000L
                        "2 hours" -> 2L * 60L * 60_000L
                        "24 hours" -> 24L * 60L * 60_000L
                        else -> 30L * 60_000L
                    }
                    roomSocketRef.value?.emit("room_seat_action", JSONObject().put("roomId", roomId)
                        .put("userId", accountId).put("action", "kick").put("targetUserId", profileId)
                        .put("durationMs", durationMs).put("targetName", profileMessage.sender)
                        .put("targetImage", profileMessage.senderImage)
                        .put("actorName", roomDisplayName).put("actorImage", roomDisplayImage)
                        .put("actorRole", if (currentUserSeatIds.any { it in roomAdminIds }) "admin" else "owner")
                        .put("ownerName", roomDisplayName).put("ownerImage", roomDisplayImage))
                    selectedChatProfile = null
                }
            )
        }
        if(showSeatSheet && selectedSeat>0) {
            val seatIndex = seats.indexOfFirst { it.number == selectedSeat }
            val seat = seats.getOrNull(seatIndex)
            val isRoomOwner = accountId.isNotBlank() &&
                currentUserSeatIds.any { it in setOf(roomOwner.accountId, roomOwner.id, roomOwner.uid, roomId).map(String::trim) }
            val isRoomAdmin = accountId.isNotBlank() &&
                roomAdminIds.any { it.trim() in currentUserSeatIds }
            if(seat!=null && (isRoomOwner || isRoomAdmin)) SeatActionSheet(
                seat=seat,
                isOwner=isRoomOwner || isRoomAdmin,
                isMySeat=seat.occupied && seat.accountId==accountId,
                onDismiss={showSeatSheet=false;selectedSeat=0},
                onTake={
                    if(!seat.occupied && !seat.locked) {
                        val user = JSONObject().put("name",roomDisplayName.ifBlank{"User"}).put("image",roomDisplayImage.ifBlank{ZYORA_DEFAULT_AVATAR_URL}).put("accountId",accountId)
                        // A seat move is one local state transition: clear every previous
                        // seat for this account BEFORE painting the destination. Otherwise
                        // the old avatar remains visible until the server's next snapshot.
                        seats.indices.filter { index ->
                            seats[index].occupied && seats[index].accountId == accountId && seats[index].number != seat.number
                        }.forEach { index ->
                            val previous = seats[index]
                            seats[index] = NativeRoomSeat(number = previous.number, locked = previous.locked)
                        }
                        if(seatIndex>=0) seats[seatIndex]=seat.copy(occupied=true,userName=roomDisplayName.ifBlank{"User"},userImage=roomDisplayImage.ifBlank { ZYORA_DEFAULT_AVATAR_URL },accountId=accountId,muted=false,selfMuted=false,speaking=false,locked=false)
                        roomSocketRef.value?.emit("room_seat_action",JSONObject().put("roomId",roomId).put("userId",accountId).put("action","take").put("seatNumber",seat.number).put("user",user))
                        showSeatSheet=false;selectedSeat=0
                    }
                },
                onLeave={
                    if(seatIndex>=0) seats[seatIndex]=NativeRoomSeat(number=seat.number)
                    roomSocketRef.value?.emit("room_seat_action",JSONObject().put("roomId",roomId).put("userId",accountId).put("action","leave").put("seatNumber",seat.number))
                    showSeatSheet=false;selectedSeat=0
                },
                onMute={
                    val nextMuted=!seat.muted
                    if(seatIndex>=0) seats[seatIndex]=seat.copy(muted=nextMuted)
                    roomSocketRef.value?.emit("room_seat_action",JSONObject().put("roomId",roomId).put("userId",accountId).put("action","owner_mute").put("seatNumber",seat.number).put("isMuted",nextMuted))
                    showSeatSheet=false;selectedSeat=0
                },
                onLock={
                    if(isRoomOwner || isRoomAdmin) {
                        // Read the latest seat state at tap time; the sheet's captured
                        // seat can be stale after a room_seats broadcast.
                        val currentSeat = seats.getOrNull(seatIndex)
                        if (currentSeat != null) {
                            val nextLocked = !currentSeat.locked
                            // Paint lock/unlock immediately; do not wait for the room snapshot.
                            seats[seatIndex] = if (nextLocked) {
                                NativeRoomSeat(number = currentSeat.number, locked = true, muted = currentSeat.muted)
                            } else {
                                currentSeat.copy(locked = false)
                            }
                            roomSocketRef.value?.takeIf { it.connected() }?.emit(
                                "room_seat_action",
                                JSONObject().put("roomId", roomId).put("userId", accountId)
                                    .put("action", "lock").put("seatNumber", currentSeat.number)
                                    .put("isLocked", nextLocked)
                            )
                            showSeatSheet = false
                            selectedSeat = 0
                        }
                    }
                },
                onInvite={
                    showSeatSheet=false
                    showSeatInviteUsers=true
                }
            )
        }
        if(selectedSeat>0 && seats.firstOrNull{it.number==selectedSeat}?.occupied==true && !showSeatSheet) {
            val profileSeat = seats.first { it.number == selectedSeat }
            RoomProfile(
                user = RoomUser(
                    id = profileSeat.accountId,
                    uid = profileSeat.accountId,
                    accountId = profileSeat.accountId,
                    name = profileSeat.userName.ifBlank { "User" },
                    image = profileSeat.userImage,
                    isInSeat = true,
                    isMuted = profileSeat.muted,
                    isLocked = profileSeat.locked
                ),
                isCurrentUser = profileSeat.accountId == accountId,
                isRoomOwner = profileSeat.accountId == roomOwner.accountId.ifBlank { roomOwner.id },
                canModerate = currentUserSeatIds.any { it == roomId || it == roomOwnerAccountId || it in roomAdminIds },
                onClose = { selectedSeat = 0 },
                onOpenPublicProfile = { profile -> selectedSeat = 0; showSeatSheet = false; onOpenPublicProfile(profile) },
                onMention = { name, targetId ->
                    val cleanName = name.trim().ifBlank { "User" }
                    run {
                        val cursor = inputValue.selection.start.coerceIn(0, input.length)
                        val insertion = (if (cursor > 0 && !input[cursor - 1].isWhitespace()) " " else "") + "@$cleanName "
                        val updated = input.substring(0, cursor) + insertion + input.substring(cursor)
                        inputValue = TextFieldValue(updated, selection=TextRange(cursor + insertion.length))
                    }
                    if (targetId.isNotBlank()) pendingMentions = (pendingMentions.filterNot { it.accountId == targetId } + NativeMention(targetId, cleanName))
                    showChat = true
                    selectedSeat = 0
                },
                onCopyId = {
                    clipboard.setText(AnnotatedString(profileSeat.accountId))
                    copiedId = profileSeat.accountId
                },
                onLeaveSeat = {
                    seats[seats.indexOfFirst { it.number == selectedSeat }.takeIf { it >= 0 } ?: 0] =
                        NativeRoomSeat(number = selectedSeat)
                    roomSocketRef.value?.emit(
                        "room_seat_action",
                        JSONObject().put("roomId", roomId).put("userId", accountId)
                            .put("targetUserId", profileSeat.accountId).put("action", "leave").put("seatNumber", selectedSeat)
                    )
                    selectedSeat = 0
                },
                onFollow = { onFollowToggle(profileSeat.accountId, true) },
                onMessage = {
                    val receiverId = profileSeat.accountId.trim()
                    if (receiverId.isNotBlank() && receiverId != accountId) {
                        showSeatSheet = false
                        selectedSeat = 0
                        selectedOfficialChat = Triple(
                            receiverId,
                            profileSeat.userName.ifBlank { "User" },
                            profileSeat.userImage
                        )
                    }
                },
                onMute = {
                    val idx = seats.indexOfFirst { it.number == selectedSeat }
                    if (idx >= 0) {
                        val nextMuted = !seats[idx].muted
                        seats[idx] = seats[idx].copy(muted = nextMuted)
                        roomSocketRef.value?.emit("room_seat_action", JSONObject().put("roomId", roomId)
                            .put("userId", accountId).put("action", "owner_mute").put("seatNumber", selectedSeat).put("isMuted", nextMuted))
                    }
                },
                onLock = {
                    val idx = seats.indexOfFirst { it.number == selectedSeat }
                    if (idx >= 0 && (accountId == roomOwner.accountId || accountId == roomOwner.id || accountId == roomOwner.uid || accountId == roomId || roomAdminIds.any { it.trim() in currentUserSeatIds })) {
                        val nextLocked = !seats[idx].locked
                        val lockedSeat = seats[idx]
                        seats[idx] = if (nextLocked) NativeRoomSeat(number = selectedSeat, locked = true, muted = lockedSeat.muted)
                            else lockedSeat.copy(locked = false)
                        roomSocketRef.value?.emit("room_seat_action", JSONObject().put("roomId", roomId)
                            .put("userId", accountId).put("action", "lock").put("seatNumber", selectedSeat).put("isLocked", nextLocked))
                        if (nextLocked) selectedSeat = 0
                    }
                },
                onKickOut = { selectedTime ->
                    val durationMs = when (selectedTime) {
                        "15 minutes" -> 15L * 60_000L
                        "30 minutes" -> 30L * 60_000L
                        "2 hours" -> 2L * 60L * 60_000L
                        "24 hours" -> 24L * 60L * 60_000L
                        else -> 30L * 60_000L
                    }
                    roomSocketRef.value?.emit("room_seat_action", JSONObject().put("roomId", roomId)
                        .put("userId", accountId).put("action", "kick").put("targetUserId", profileSeat.accountId)
                        .put("seatNumber", selectedSeat).put("durationMs", durationMs)
                        .put("targetName", profileSeat.userName).put("targetImage", profileSeat.userImage)
                        .put("actorName", roomDisplayName).put("actorImage", roomDisplayImage)
                        .put("actorRole", if (currentUserSeatIds.any { it in roomAdminIds }) "admin" else "owner")
                        .put("ownerName", roomDisplayName).put("ownerImage", roomDisplayImage))
                    val idx = seats.indexOfFirst { it.number == selectedSeat }
                    if (idx >= 0) seats[idx] = NativeRoomSeat(number = selectedSeat)
                    selectedSeat = 0
                }
            )
        }
        if(showMessageSheet) {
            val screenHeight = LocalConfiguration.current.screenHeightDp
            val sheetShape = RoundedCornerShape(topStart = 15.dp, topEnd = 15.dp)
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.30f)).clickable{showMessageSheet=false},contentAlignment=Alignment.BottomCenter) {
                Surface(modifier=Modifier.fillMaxWidth().height((screenHeight * .60f).dp).clip(sheetShape).clickable(enabled=false) {},color=Color.White,contentColor=Color.Black,shape=sheetShape,shadowElevation=16.dp) {
                    Column(Modifier.fillMaxSize().clip(sheetShape).background(Color.White)) {
                        MessageScreen(
                            onChat={ uid, name, image ->
                                selectedOfficialChat = Triple(uid, name, image)
                                showMessageSheet = false
                            },
                            embeddedSheet=true,
                            onBack={ showMessageSheet=false },
                            onJoinRoom=onJoinRoom
                        )
                    }
                }
            }
        }

        LuckyBagOpenConfirmCards(
            showLuckyBagCenterReveal = showLuckyBagCenterReveal,
            showLuckyBagClaimedCard = showLuckyBagClaimedCard,
            luckyBagSenderName = luckyBagSenderName,
            luckyBagSenderImage = luckyBagSenderImage,
            luckyBagId = luckyBagId,
            luckyBagClaiming = luckyBagClaiming,
            luckyBagClaimError = luckyBagClaimError,
            luckyBagClaimAmount = luckyBagClaimAmount,
            claimantName = roomDisplayName,
            claimantImage = roomDisplayImage,
            canonicalWalletUserId = canonicalWalletUserId,
            roomWalletBalance = roomWalletBalance,
            showFloatingLuckyBag = showFloatingLuckyBag,
            onShowLuckyBagCenterRevealChange = { showLuckyBagCenterReveal = it },
            onShowLuckyBagClaimedCardChange = { showLuckyBagClaimedCard = it },
            onLuckyBagClaimingChange = { luckyBagClaiming = it },
            onLuckyBagClaimErrorChange = { luckyBagClaimError = it },
            onLuckyBagClaimAmountChange = {
                luckyBagClaimAmount = it
                if (it > 0L) appContext.getSharedPreferences("lucky_bag_claims", android.content.Context.MODE_PRIVATE)
                    .edit().putLong("claim_" + luckyBagId + "_" + canonicalWalletUserId, it).apply()
            },
            onShowFloatingLuckyBagChange = { showFloatingLuckyBag = it },
            onRoomWalletBalanceChange = { roomWalletBalance = it }
        )
        
        if (showInviteSheet) RoomInviteSheet(
            roomUsers = activeRoomUsers,
            currentUserId = accountId,
            roomOwnerId = roomOwner.accountId.ifBlank { roomOwner.id },
            onClose = { showInviteSheet = false },
            onInvite = { target ->
                val socket = roomSocketRef.value
                if (socket?.connected() == true && selectedSeat > 0) {
                    socket.emit(
                        "room_seat_invite_send",
                        JSONObject()
                            .put("roomId", roomId)
                            .put("inviterId", accountId)
                            .put("inviterName", roomDisplayName.ifBlank { "User" })
                            .put("inviterImage", roomDisplayImage)
                            .put("targetId", target.accountId)
                            .put("seatNumber", selectedSeat)
                    )
                    showInviteSheet = false
                    selectedSeat = 0
                }
            }
        )
        incomingSeatInvite?.let { invite ->
            RoomSeatInviteDialog(
                inviterName = invite.optString("inviterName", "User"),
                inviterImage = invite.optString("inviterImage", ""),
                onRefuse = {
                    val socket = roomSocketRef.value
                    if (socket?.connected() == true) {
                        socket.emit(
                            "room_seat_invite_response",
                            JSONObject().put("roomId", roomId).put("targetId", accountId)
                                .put("inviterId", invite.optString("inviterId"))
                                .put("inviteId", invite.optString("inviteId"))
                                .put("seatNumber", invite.optInt("seatNumber")).put("accepted", false)
                        )
                        incomingSeatInvite = null
                    }
                },
                onAccept = {
                    val socket = roomSocketRef.value
                    if (socket?.connected() == true) {
                        socket.emit(
                            "room_seat_invite_response",
                            JSONObject().put("roomId", roomId).put("targetId", accountId)
                                .put("inviterId", invite.optString("inviterId"))
                                .put("inviteId", invite.optString("inviteId"))
                                .put("seatNumber", invite.optInt("seatNumber")).put("accepted", true)
                                .put("userName", roomDisplayName.ifBlank { "User" })
                                .put("userImage", roomDisplayImage)
                        )
                        incomingSeatInvite = null
                    }
                }
            )
        }
        // Keep direct chats above every room overlay so the owner chat is visible immediately.
        selectedOfficialChat?.let { chat ->
            key(chat.first) {
                Box(
                    Modifier.fillMaxSize()
                        .background(Color.White)
                        .zIndex(500f)
                ) {
                    NativeChatScreen(
                        name = chat.second,
                        image = chat.third,
                        receiverId = chat.first,
                        currentUserId = currentUser.uid.ifBlank { currentUser.id },
                        currentAccountIdOverride = currentUser.accountId.ifBlank { currentUser.id },
                        onBack = { selectedOfficialChat = null; pendingRoomInvitationJson = null },
                        onJoinRoom = onJoinRoom,
                        initialRoomInvitation = pendingRoomInvitationJson
                    )
                }
            }
        }
        

    }
}


@Composable
private fun androidx.compose.foundation.layout.BoxScope.RoomPageBlock(content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit) {
    content()
}


@Composable
private fun RoomChatMessages(
    state: androidx.compose.foundation.lazy.LazyListState,
    messages: List<NativeRoomMessage>,
    kickNotices: List<NativeRoomKickNotice>,
    showOfficialAnnouncement: Boolean,
    clearedBy: String,
    announcement: String,
    showRoomSettingsAnnouncementInChat: Boolean,
    roomOwnerId: String,
    roomAdminIds: List<String> = emptyList(),
    isCovered: Boolean = false,
    onSelectMessage: (NativeRoomMessage) -> Unit,
    onSelectMention: (NativeMention) -> Unit = {},
    onCopyMessage: (String) -> Unit = {}
) {
    LazyColumn(
        state = state,
                            modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (showOfficialAnnouncement) {
                                item {
                                    Column(Modifier.fillMaxWidth().padding(start=4.dp, top=8.dp, end=82.dp, bottom=2.dp)) {
                                        Column(Modifier.fillMaxWidth(.92f).clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(alpha=.30f)).padding(horizontal=14.dp,vertical=12.dp)) {
                                            Text("Announcement: Welcome to Zyora. Any content related to porn, fraud, or fake official accounts will result in a ban!",color=Color(0xFFE2C67D),fontSize=13.sp,lineHeight=16.sp,fontWeight=androidx.compose.ui.text.font.FontWeight.Medium)
                                        }
                                    }
                                }
                            } else if (clearedBy.isNotBlank()) {
                                // CLEARED CHAT MESSAGE UI WITH SMALLER FONT
                                item {
                                    Column(Modifier.fillMaxWidth().padding(start=4.dp, top=8.dp, end=82.dp, bottom=2.dp)) {
                                        Column(Modifier.wrapContentWidth().clip(RoundedCornerShape(12.dp)).background(Color.Black.copy(alpha=.30f)).padding(horizontal=12.dp, vertical=8.dp), verticalArrangement=Arrangement.spacedBy(4.dp)) {
                                            Row(verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                                                Box(Modifier.size(24.dp).clip(CircleShape).background(Color(0xFF00C853)), contentAlignment=Alignment.Center) {
                                                    Icon(Icons.Default.Notifications, contentDescription="System", tint=Color.White, modifier=Modifier.size(16.dp))
                                                }
                                                Text("System", color=Color(0xFFD1D1D1), fontSize=15.sp, fontWeight=FontWeight.Medium)
                                            }
                                            Row(verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                                                Text(clearedBy, color=Color(0xFFE2C67D), fontSize=13.sp, fontWeight=FontWeight.Bold, maxLines=1, softWrap=false, overflow=TextOverflow.Ellipsis)
                                                Text("cleared the chat message", color=Color.White, fontSize=13.sp, fontWeight=FontWeight.Normal, maxLines=1, softWrap=false)
                                            }
                                        }
                                    }
                                }
                            }
                            if(showRoomSettingsAnnouncementInChat && announcement.isNotBlank()) {
                                item {
                                    Column(Modifier.fillMaxWidth().padding(start=4.dp, end=82.dp, bottom=2.dp)) {
                                        Column(Modifier.fillMaxWidth(.92f).clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(alpha=.30f)).padding(horizontal=14.dp,vertical=12.dp)) {
                                            Text("ANNOUNCEMENT: $announcement",color=Color(0xFFE2C67D),fontSize=13.sp,lineHeight=16.sp,fontWeight=androidx.compose.ui.text.font.FontWeight.Medium)
                                        }
                                    }
                                }
                            }
                            items(kickNotices, key = { it.id }, contentType = { "room_kick_notice" }) { notice ->
                                Column(Modifier.fillMaxWidth().padding(start=4.dp, top=0.dp, end=82.dp, bottom=2.dp)) {
                                    Column(Modifier.wrapContentWidth().clip(RoundedCornerShape(12.dp)).background(Color.Black.copy(alpha=.30f)).padding(horizontal=12.dp, vertical=8.dp), verticalArrangement=Arrangement.spacedBy(4.dp)) {
                                        Row(verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                                            Box(Modifier.size(24.dp).clip(CircleShape).background(Color(0xFF00C853)), contentAlignment=Alignment.Center) {
                                                Icon(Icons.Default.Notifications, contentDescription="System", tint=Color.White, modifier=Modifier.size(16.dp))
                                            }
                                            Text("System", color=Color(0xFFD1D1D1), fontSize=15.sp, fontWeight=FontWeight.Medium)
                                        }
                                        Row(verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                                            FallbackAsyncImage(model=notice.targetImage, contentDescription=notice.targetName, modifier=Modifier.size(22.dp).clip(CircleShape), contentScale=ContentScale.Crop)
                                            Text(notice.targetName, color=Color(0xFFE2C67D), fontSize=13.sp, fontWeight=FontWeight.Bold, maxLines=1, softWrap=false, overflow=TextOverflow.Ellipsis)
                                            Text("are kicked out of Room by", color=Color.White, fontSize=13.sp, maxLines=1, softWrap=false)
                                        }
                                        Row(verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                                            FallbackAsyncImage(model=notice.actorImage, contentDescription=notice.actorName, modifier=Modifier.size(22.dp).clip(CircleShape), contentScale=ContentScale.Crop)
                                            Text(notice.actorName, color=Color(0xFFE2C67D), fontSize=13.sp, fontWeight=FontWeight.Bold, maxLines=1, softWrap=false, overflow=TextOverflow.Ellipsis)
                                            Text("(${notice.actorRole})", color=Color.White, fontSize=13.sp, fontWeight=FontWeight.Normal)
                                        }
                                    }
                                }
                            }
                            items(messages, key = { it.id }, contentType = { message -> if (message.type == "join") "room_join_message" else "room_chat_message" }) { message ->
                                if(message.type=="join") {
                                    Column(Modifier.padding(vertical=2.dp).wrapContentWidth().clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(alpha=.30f)).padding(horizontal=12.dp,vertical=4.dp),horizontalAlignment=Alignment.Start,verticalArrangement=Arrangement.spacedBy(2.dp)){
                                        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp), modifier=Modifier.clickable { onSelectMessage(message) }){
                                            FallbackAsyncImage(model=message.senderImage,contentDescription=message.sender,modifier=Modifier.size(24.dp).clip(CircleShape),contentScale=ContentScale.Crop)
                                            Text(message.sender,color=Color.White.copy(alpha=.95f),fontSize=12.sp,fontWeight=androidx.compose.ui.text.font.FontWeight.SemiBold)
                                            if (message.senderAccountId == roomOwnerId) {
                                                AsyncImage(model="https://zyora-voice-chat.vercel.app/IMG_20261027_235537.png", contentDescription="Owner", contentScale=ContentScale.Fit, modifier=Modifier.height(16.dp))
                                            } else if (message.senderAccountId in roomAdminIds) {
                                                AsyncImage(model=ROOM_ADMIN_TAG, contentDescription="Room Admin", contentScale=ContentScale.Fit, modifier=Modifier.height(16.dp))
                                            }
                                            

                                            val hasPublicProfileIdImage = message.senderAccountId in setOf("100002", "100003", "500001", "500002", "500003", "500004", "500005", "500005", "500005")
                                            if (hasPublicProfileIdImage) {
                                                AsyncImage(model = "https://zyora-voice-chat.vercel.app/IMG_20261101_125212.png", contentDescription = "Purple ID", contentScale = ContentScale.Fit, modifier = Modifier.height(16.dp).widthIn(min = 14.dp, max = 22.dp))
                                            }
                                            
                                            Box(modifier = Modifier.height(16.dp).width(36.dp), contentAlignment = Alignment.CenterEnd) {
                                                AsyncImage(model = "https://zyora-voice-chat.vercel.app/IMG_20261028_004355.png", contentDescription = "Level", contentScale = ContentScale.FillBounds, modifier = Modifier.fillMaxSize())
                                                Text("Lv.1", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterEnd).offset(x = 2.dp, y = (-3).dp).padding(end = 5.dp))
                                            }
                                        }
                                        Text("Enter the room",color=Color.White,fontSize=12.sp,fontWeight=FontWeight.Bold, modifier=Modifier.padding(start=2.dp).clickable { onSelectMessage(message) })
                                    }
                                } else NativeMessage(message, roomOwnerId = roomOwnerId, roomAdminIds = roomAdminIds, isCovered = isCovered, onAvatarClick = { onSelectMessage(message) }, onMentionClick = onSelectMention, onCopyMessage = onCopyMessage)
                            }
                        }
}

private fun formatMusicTime(seconds: Float): String {
    if (seconds.isNaN() || seconds < 0f) return "0:00"
    val t = seconds.toInt()
    return "${t / 60}:${(t % 60).toString().padStart(2, '0')}"
}


@Composable
private fun RoomInviteSheet(
    roomUsers: List<ActiveUserNative>,
    currentUserId: String,
    roomOwnerId: String,
    onClose: () -> Unit,
    onInvite: (ActiveUserNative) -> Unit
) {
    val context = LocalContext.current
    var countryByUser by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    LaunchedEffect(roomUsers) {
        countryByUser = withContext(Dispatchers.IO) {
            roomUsers.mapNotNull { user ->
                if (user.accountId.isBlank()) return@mapNotNull null
                val country = runCatching {
                    loadRoomProfileCountry(context, user.accountId, user.name, user.image, user.country)
                }.getOrDefault("")
                user.accountId to country
            }.toMap()
        }
    }
    // Reuse the invite list until live room users or the owner actually change.
    // This avoids filtering, de-duplicating and sorting the full list on unrelated
    // room recompositions (voice waves, music progress, chat overlays, etc.).
    val visibleUsers = remember(roomUsers, currentUserId, roomOwnerId) {
        roomUsers.asSequence()
            .filter { it.accountId.isNotBlank() && it.accountId != currentUserId }
            .distinctBy { it.accountId }
            .sortedWith(compareBy<ActiveUserNative> { if (it.accountId == roomOwnerId) 0 else 1 }.thenBy { it.name.lowercase() })
            .toList()
    }
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .30f)).clickable { onClose() }, contentAlignment = Alignment.BottomCenter) {
        Column(
            Modifier.fillMaxWidth().fillMaxHeight(.5f)
                .clip(RoundedCornerShape(topStart = 11.dp, topEnd = 11.dp))
                .background(Color(0xF5000000))
                .clickable(enabled = false) {},
        ) {
            Text("Invite", modifier = Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 10.dp),
                color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            LazyColumn(
                Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(start = 13.dp, end = 12.dp, top = 3.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(visibleUsers, key = { it.accountId }) { user ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        FallbackAsyncImage(
                            model = user.image,
                            contentDescription = user.name,
                            modifier = Modifier.size(38.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Column(Modifier.weight(1f).padding(start = 9.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text(user.name.ifBlank { "User" }, color = Color.White, fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                                if (user.accountId == roomOwnerId) {
                                    AsyncImage(model = "https://zyora-voice-chat.vercel.app/IMG_20261027_235537.png",
                                        contentDescription = "Owner", contentScale = ContentScale.Fit, modifier = Modifier.height(18.dp).width(40.dp))
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(Modifier.width(18.dp).height(18.dp), contentAlignment = Alignment.Center) {
                                    val flagCountry = countryByUser[user.accountId].orEmpty()
                                    if (flagCountry.isNotBlank()) CountryFlagImage(flagCountry, Modifier.size(18.dp, 12.dp))
                                }
                                Box(Modifier.height(16.dp).width(36.dp), contentAlignment = Alignment.CenterEnd) {
                                    AsyncImage(model = "https://zyora-voice-chat.vercel.app/IMG_20261028_004355.png",
                                        contentDescription = "Level", contentScale = ContentScale.FillBounds, modifier = Modifier.fillMaxSize())
                                    Text("Lv.1", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.offset(x = 2.dp, y = (-3).dp).padding(end = 5.dp))
                                }
                            }
                        }
                        AsyncImage(
                            model = "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/IMG_20261107_153857.png",
                            contentDescription = "Invite",
                            modifier = Modifier.padding(start = 8.dp).width(66.dp).height(30.dp).clickable { onInvite(user) },
                            contentScale = ContentScale.Fit
                        )
                    }
                }
                if (visibleUsers.isEmpty()) {
                    item { Text("No active users", modifier = Modifier.fillMaxWidth().padding(18.dp),
                        color = Color.White.copy(alpha = .65f), fontSize = 12.sp, textAlign = TextAlign.Center) }
                }
            }
        }
    }
}

@Composable
private fun RoomSeatInviteDialog(
    inviterName: String,
    inviterImage: String,
    onRefuse: () -> Unit,
    onAccept: () -> Unit
) {
    val raw = "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/"
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .68f)), contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxWidth(.84f).aspectRatio(1475f / 792f), contentAlignment = Alignment.Center) {
            AsyncImage(
                model = raw + "IMG_20261106_001208.png",
                contentDescription = "Mic invitation",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).align(Alignment.TopCenter).padding(top = 34.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(13.dp))
                Text(inviterName.ifBlank { "User" }, color = Color.White, fontSize = 17.sp,
                    fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(9.dp))
                Row(Modifier.offset(y = (-11).dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Text("Invite you to join mic", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.width(5.dp))
                    AsyncImage(model = raw + "IMG_20261106_001226.png", contentDescription = "Mic",
                        modifier = Modifier.size(23.dp), contentScale = ContentScale.Fit)
                }
            }
            FallbackAsyncImage(
                model = inviterImage,
                contentDescription = "Inviter avatar",
                modifier = Modifier.align(Alignment.TopCenter).offset(y = (-27).dp).size(68.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Row(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 22.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(Modifier.weight(1f).height(43.dp).clip(RoundedCornerShape(9.dp))
                    .background(Color.White.copy(alpha = .10f)).clickable { onRefuse() }, contentAlignment = Alignment.Center) {
                    Text("Refuse", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Box(Modifier.weight(1f).height(43.dp).clip(RoundedCornerShape(9.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFFFFE49A), Color(0xFFD69A36))))
                    .clickable { onAccept() }, contentAlignment = Alignment.Center) {
                    Text("Accept", color = Color(0xFF4B2D08), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable private fun SeatActionSheet(seat:NativeRoomSeat,isOwner:Boolean,isMySeat:Boolean,onDismiss:()->Unit,onTake:()->Unit,onLeave:()->Unit,onMute:()->Unit,onLock:()->Unit,onInvite:()->Unit){
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.30f)).clickable{onDismiss()},contentAlignment=Alignment.BottomCenter){
        Surface(Modifier.fillMaxWidth(),color=Color.White.copy(alpha=.96f),shape=RoundedCornerShape(topStart=12.dp,topEnd=12.dp)){
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal=16.dp,vertical=12.dp),verticalArrangement=Arrangement.spacedBy(2.dp)){
                if(!seat.occupied && !isMySeat) SeatSheetAction("Take Mic",enabled=!seat.locked,onClick=onTake)
                if(isMySeat) SeatSheetAction("Leave Seat",onClick=onLeave)
                if(isOwner || isMySeat || seat.occupied) SeatSheetAction(if(seat.muted)"Unmute Seat" else "Mute Seat",onClick=onMute)
                if(isOwner) SeatSheetAction(if(seat.locked)"Unlock Mic" else "Lock Mic",onClick=onLock)
                SeatSheetAction("Invite",onClick=onInvite)
            }
        }
    }
}

@Composable private fun SeatSheetAction(label:String,enabled:Boolean=true,onClick:()->Unit){
    TextButton(onClick=onClick,enabled=enabled,modifier=Modifier.fillMaxWidth().height(44.dp),shape=RoundedCornerShape(10.dp)){ Text(label,color=if(enabled)Color(0xFF171717) else Color.Gray,fontSize=14.sp) }
}

@Composable private fun RoomFooterButton(onClick:()->Unit,description:String,icon:Int,hasUnread:Boolean=false){
    Box(Modifier.size(42.dp).clip(CircleShape).background(Color.Black.copy(alpha=.30f)).clickable(onClick=onClick),contentAlignment=Alignment.Center){
        Image(painterResource(icon),contentDescription=description,modifier=Modifier.size(when(description){"Say Hi Chat"->30.dp;"Emoji"->26.dp;"Message Box Menu"->28.dp;"Apps Menu"->26.dp;else->26.dp}))
        if (hasUnread && description == "Message Box Menu") {
            // Anchor the unread indicator to the mail glyph, not its touch target.
            Box(
                Modifier.align(Alignment.TopEnd)
                    .padding(top = 7.dp, end = 7.dp)
                    .size(8.dp).background(Color(0xFFFF3030), CircleShape)
            )
        }
    }
}

@Composable private fun NativeSeat(seat:NativeRoomSeat,ownerId:String,roomAdminIds:List<String> = emptyList(),isSpeaking:Boolean=seat.speaking,seatSize:Dp=62.dp,onClick:()->Unit){
    val waveProgress = if (isSpeaking) {
        val voicePulse = rememberInfiniteTransition(label = "voice-wave")
        voicePulse.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1900, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "voice-wave-progress"
        ).value
    } else 0f
    val raw="https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/"
    
    var showGif by remember(seat.gifTimestamp) { mutableStateOf(seat.gifSrc.isNotBlank()) }
    LaunchedEffect(seat.gifTimestamp) {
        if (seat.gifTimestamp != 0L && seat.gifSrc.isNotBlank()) {
            showGif = true
            kotlinx.coroutines.delay(seat.gifDurationMs.coerceIn(500L, 5000L))
            showGif = false
        } else {
            showGif = false
        }
    }

    Column(horizontalAlignment=Alignment.CenterHorizontally,modifier=Modifier.width(seatSize).clickable(onClick=onClick)){
        Box(Modifier.size(seatSize),contentAlignment=Alignment.Center){ 
            Canvas(Modifier.fillMaxSize()) {
                val blue = !seat.locked
                val ringBrush = Brush.linearGradient(colors = if(blue) listOf(Color(0xFFD9F7FF),Color(0xFF91E1FF),Color(0xFF4DBFEF),Color(0xFF299DD6),Color(0xFF167DB9)) else listOf(Color(0xFFF3F4F6),Color(0xFFD1D5DB),Color(0xFF9CA3AF),Color(0xFF6B7280),Color(0xFF4B5563)),start=Offset(size.width*100f/512f,size.height*55f/512f),end=Offset(size.width*420f/512f,size.height*460f/512f))
                val radius=size.minDimension*225f/512f
                val center=Offset(size.width/2f,size.height/2f)
                drawCircle(color=if(blue) Color(0xFF55C6F3).copy(alpha=.25f) else Color(0xFF9CA3AF).copy(alpha=.25f),radius=radius+size.minDimension*7.5f/512f,center=center,style=Stroke(width=size.minDimension*15f/512f))
                drawCircle(brush=ringBrush,radius=radius,center=center,style=Stroke(width=size.minDimension*10f/512f))
                val shine = Brush.horizontalGradient(colors=listOf(Color.White.copy(alpha=0f),Color.White.copy(alpha=.75f),Color.White.copy(alpha=.95f),Color.White.copy(alpha=0f)),startX=0f,endX=size.width)
                val topHighlight=Path().apply { moveTo(size.width*98f/512f,size.height*134f/512f); cubicTo(size.width*113f/512f,size.height*108f/512f,size.width*132f/512f,size.height*92f/512f,size.width*153f/512f,size.height*79f/512f); cubicTo(size.width*174f/512f,size.height*67f/512f,size.width*196f/512f,size.height*59f/512f,size.width*218f/512f,size.height*56f/512f) }
                drawPath(topHighlight,shine,style=Stroke(width=size.minDimension*7.5f/512f,cap=StrokeCap.Round))
                val bottomHighlight=Path().apply { moveTo(size.width*207f/512f,size.height*452f/512f); cubicTo(size.width*258f/512f,size.height*466f/512f,size.width*316f/512f,size.height*456f/512f,size.width*361f/512f,size.height*428f/512f); cubicTo(size.width*378f/512f,size.height*417f/512f,size.width*392f/512f,size.height*404f/512f,size.width*404f/512f,size.height*388f/512f) }
                drawPath(bottomHighlight,shine,style=Stroke(width=size.minDimension*7f/512f,cap=StrokeCap.Round))
            }
            if (isSpeaking) {
                // Three staggered solid white 30% circles expand behind the avatar.
                // The next circle starts before the previous circle finishes fading.
                Canvas(Modifier.fillMaxSize().graphicsLayer { scaleX = 1.5f; scaleY = 1.5f }.align(Alignment.Center)) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val maxRadius = size.minDimension * 0.42f
                    val phases = listOf(
                        waveProgress,
                        (waveProgress + 1f / 3f) % 1f,
                        (waveProgress + 2f / 3f) % 1f
                    )
                    phases.forEach { phase ->
                        val fade = if (phase < 0.82f) 1f else ((1f - phase) / 0.18f).coerceIn(0f, 1f)
                        drawCircle(
                            color = Color.White.copy(alpha = 0.50f * fade),
                            radius = maxRadius * phase,
                            center = center
                        )
                    }
                }
            }
            Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){
                if(seat.locked) AsyncImage(model=raw+"file_0000000015a48211b000ee447a786f7c.png",contentDescription="Locked seat",modifier=Modifier.fillMaxSize(.62f),contentScale=ContentScale.Fit)
                else if(seat.occupied) {
                    FallbackAsyncImage(model=seat.userImage,contentDescription=seat.userName,modifier=Modifier.requiredSize(seatSize + 1.dp).clip(CircleShape),contentScale=ContentScale.Crop)
                    NativeAvatarFrameOverlay(userId=seat.accountId,modifier=Modifier.requiredSize(seatSize + 1.dp))
                } else AsyncImage(model=raw+"file_000000003e7482309b7f6e7f2a922160.png",contentDescription="Empty seat",modifier=Modifier.fillMaxSize(.65f),contentScale=ContentScale.Fit)
            }
            
            if(showGif && seat.gifSrc.isNotBlank()) {
                SeatEmojiOverlay(
                    src = seat.gifSrc,
                    seatSize = seatSize,
                    scale = seat.gifScale,
                    onVideoFinished = { showGif = false }
                )
            }
            
            if(seat.muted) Box(Modifier.align(Alignment.BottomEnd).clip(CircleShape).background(Color.White).padding(2.dp)) { Icon(Icons.Default.MicOff,null,tint=Color.Black,modifier=Modifier.size(12.dp)) }
        }
        
        if (seat.occupied && (seat.accountId == ownerId || seat.accountId in roomAdminIds)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                AsyncImage(
                    model = if (seat.accountId == ownerId) "https://zyora-voice-chat.vercel.app/IMG_20261027_235537.png" else ROOM_ADMIN_TAG,
                    contentDescription = if (seat.accountId == ownerId) "Owner" else "Room Admin",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.height(14.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(seat.userName, color = Color.White.copy(.9f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
            }
        } else {
            Text(if (seat.occupied) seat.userName else seat.number.toString(), color = Color.White.copy(.9f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

private object NativeRoomMessageProfileRefresh {
    private val lastRefreshByUser = mutableMapOf<String, Long>()
    fun shouldRefresh(accountId: String, now: Long = System.currentTimeMillis()): Boolean {
        if (accountId.isBlank()) return false
        return synchronized(lastRefreshByUser) {
            val last = lastRefreshByUser[accountId] ?: 0L
            if (now - last >= 60_000L) {
                lastRefreshByUser[accountId] = now
                if (lastRefreshByUser.size > 2000) {
                    val cutoff = now - 10 * 60_000L
                    lastRefreshByUser.entries.removeAll { it.value < cutoff }
                }
                true
            } else false
        }
    }
}

@Composable private fun NativeMessage(m:NativeRoomMessage, roomOwnerId: String, roomAdminIds: List<String> = emptyList(), isCovered: Boolean = false, onAvatarClick: () -> Unit = {}, onMentionClick: (NativeMention) -> Unit = {}, onCopyMessage: (String) -> Unit = {}){
    val context = LocalContext.current
    var showRoomImagePreview by remember(m.id) { mutableStateOf(false) }
    val profileDb = remember(context) { NativeIndexDatabase(context) }
    var profileTags by remember(m.senderAccountId) { mutableStateOf<JSONObject?>(null) }
    LaunchedEffect(m.senderAccountId) {
        if (m.senderAccountId.isNotBlank()) {
            // SQLite reads/writes and remote profile refreshes must never run on the
            // Compose main thread while the room chat is being laid out.
            val cached = withContext(Dispatchers.IO) {
                runCatching { profileDb.loadProfile(m.senderAccountId) }.getOrNull()
            }
            profileTags = cached
            // Room chat can render dozens of messages from the same people. Refresh each
            // sender profile at most once per minute; render the SQLite snapshot immediately
            // instead of issuing one HTTP request per message.
            val remote = if (NativeRoomMessageProfileRefresh.shouldRefresh(m.senderAccountId)) {
                runCatching {
                    ProfilePersistence.load(
                        TargetUser(uid = m.senderAccountId, id = m.senderAccountId, accountId = m.senderAccountId, name = m.sender, image = m.senderImage)
                    )
                }.getOrNull()
            } else null
            if (remote != null) {
                val merged = JSONObject(cached?.toString() ?: "{}")
                listOf("officialHeadTag", "officialTag", "adminTag", "serviceTag", "coinSellerTag", "coinSeller", "sellerTag", "csTag", "hostTag", "isHost").forEach { key ->
                    if (remote.has(key)) merged.put(key, remote.opt(key))
                }
                val remoteName = sequenceOf("name", "displayName", "userName").map { remote.optString(it, "") }.firstOrNull { it.isNotBlank() && it != "null" } ?: m.sender
                val remoteImage = sequenceOf("image", "dp", "photo", "photoURL", "photoUrl", "avatar", "avatarUrl", "profileImage", "profileImageUrl", "profile_image", "imageUrl", "picture").map { remote.optString(it, "") }.firstOrNull { it.isNotBlank() && it != "null" } ?: ZYORA_DEFAULT_AVATAR_URL
                if (remoteName.isNotBlank()) merged.put("name", remoteName)
                if (remoteImage.isNotBlank()) merged.put("image", remoteImage)
                withContext(Dispatchers.IO) { profileDb.saveProfile(m.senderAccountId, merged) }
                profileTags = merged
            }
        }
    }

    val isRoomOwnerMessage = m.senderAccountId == roomOwnerId
    val isCoreOfficial = m.senderAccountId == "100002" || m.senderAccountId == "100003"
    val isRedOfficial = m.senderAccountId == "500001"
    val is500002Official = m.senderAccountId in setOf("500002", "500003", "500004", "500005", "500005")
    
    val officialHeadTag = !is500002Official && (isCoreOfficial || profileTags?.optBoolean("officialHeadTag", false) == true)
    val officialTag = isCoreOfficial || isRedOfficial || is500002Official || profileTags?.optBoolean("officialTag", false) == true
    val adminTag = !is500002Official && (isCoreOfficial || profileTags?.optBoolean("adminTag", false) == true)
    val serviceTag = !is500002Official && (isCoreOfficial || profileTags?.optBoolean("serviceTag", false) == true || profileTags?.optBoolean("csTag", false) == true)
    val hostTag = !is500002Official && (isCoreOfficial || profileTags?.optBoolean("hostTag", false) == true || profileTags?.optBoolean("isHost", false) == true)
    val coinSellerTag = !is500002Official && (isCoreOfficial || profileTags?.optBoolean("coinSellerTag", false) == true || profileTags?.optBoolean("coinSeller", false) == true || profileTags?.optBoolean("sellerTag", false) == true)
    val showPurpleIdImage = isCoreOfficial || isRedOfficial || is500002Official
    
    Row(Modifier.fillMaxWidth().padding(vertical=2.dp),verticalAlignment=Alignment.Top,horizontalArrangement=Arrangement.Start){
        val cachedName = profileTags?.let { profile -> sequenceOf("name", "displayName", "userName").map { profile.optString(it, "") }.firstOrNull { it.isNotBlank() && it != "null" } }.orEmpty()
        val cachedImage = profileTags?.let { profile -> sequenceOf("image", "dp", "photo", "photoURL", "photoUrl", "avatar", "avatarUrl", "profileImage", "profileImageUrl", "profile_image", "imageUrl", "picture").map { profile.optString(it, "") }.firstOrNull { it.isNotBlank() && it != "null" } }.orEmpty()
        FallbackAsyncImage(model=cachedImage.ifBlank { m.senderImage.ifBlank { ZYORA_DEFAULT_AVATAR_URL } },contentDescription=cachedName.ifBlank { m.sender },modifier=Modifier.padding(top=2.dp).size(28.dp).clip(CircleShape).border(1.dp, Color.White.copy(alpha=.1f), CircleShape).clickable(onClick = onAvatarClick),contentScale=ContentScale.Crop)
        
        Column(Modifier.padding(start=8.dp).fillMaxWidth(.75f),verticalArrangement=Arrangement.spacedBy(2.dp)){
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(3.dp)){
                Text(cachedName.ifBlank { m.sender },color=Color.White.copy(alpha=.9f),fontSize=12.sp,lineHeight=13.sp,fontWeight=androidx.compose.ui.text.font.FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis)
                
                if (isRoomOwnerMessage || m.senderAccountId in roomAdminIds) {
                    AsyncImage(model = if (m.senderAccountId in roomAdminIds) ROOM_ADMIN_TAG else "https://zyora-voice-chat.vercel.app/IMG_20261027_235537.png", contentDescription = if (m.senderAccountId in roomAdminIds) "Room Admin" else "Owner", contentScale = ContentScale.Fit, modifier = Modifier.height(16.dp))
                }
                if (showPurpleIdImage) {
                    AsyncImage(model = "https://zyora-voice-chat.vercel.app/IMG_20261101_125212.png", contentDescription = "Purple ID", contentScale = ContentScale.Fit, modifier = Modifier.height(16.dp).widthIn(min = 14.dp, max = 22.dp))
                }
                Box(modifier = Modifier.height(16.dp).width(36.dp), contentAlignment = Alignment.CenterEnd) {
                    AsyncImage(model = "https://zyora-voice-chat.vercel.app/IMG_20261028_004355.png", contentDescription = "Level", contentScale = ContentScale.FillBounds, modifier = Modifier.fillMaxSize())
                    Text("Lv.1", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.offset(x = 2.dp, y = (-3).dp).padding(end = 5.dp))
                }
            }
            if (officialHeadTag || officialTag || adminTag) {
                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(3.dp)){
                    if(officialHeadTag) AsyncImage(model="https://zyora-voice-chat.vercel.app/file_000000005c388246a5d95dba880d7eb9.png",contentDescription="Official Head",contentScale=ContentScale.Fit,modifier=Modifier.height(20.dp).widthIn(max=68.dp))
                    if(officialTag) AsyncImage(model="https://zyora-voice-chat.vercel.app/IMG_20261027_102250.png",contentDescription="Official",contentScale=ContentScale.Fit,modifier=Modifier.height(18.dp).widthIn(max=60.dp))
                    if(adminTag) AsyncImage(model="https://zyora-voice-chat.vercel.app/IMG_20261027_114117.png",contentDescription="Admin",contentScale=ContentScale.Fit,modifier=Modifier.height(16.dp).widthIn(max=56.dp))
                }
            }
            if (serviceTag || hostTag || coinSellerTag) {
                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(3.dp)){
                    if(serviceTag) AsyncImage(model="https://zyora-voice-chat.vercel.app/IMG_20261027_102207.png",contentDescription="Service / CS",contentScale=ContentScale.Fit,modifier=Modifier.height(16.dp).widthIn(max=56.dp))
                    if(hostTag) AsyncImage(model="https://zyora-voice-chat.vercel.app/IMG_20261028_154914.png",contentDescription="Host",contentScale=ContentScale.Fit,modifier=Modifier.height(16.dp).widthIn(max=56.dp))
                    if(coinSellerTag) AsyncImage(model="https://zyora-voice-chat.vercel.app/IMG_20261028_110222.png",contentDescription="Coin Seller",contentScale=ContentScale.Fit,modifier=Modifier.height(16.dp).widthIn(max=56.dp))
                }
            }
            if (m.type == "image" && m.imageUrl.isNotBlank()) {
                AsyncImage(model = m.imageUrl, contentDescription = "Room shared image",
                    modifier = Modifier.widthIn(max = 140.dp).heightIn(max = 140.dp)
                        .clip(RoundedCornerShape(12.dp)).clickable { showRoomImagePreview = true },
                    contentScale = ContentScale.Crop)
                LaunchedEffect(isCovered) {
                    if (isCovered) showRoomImagePreview = false
                }
                if (!isCovered && showRoomImagePreview) {
                    Dialog(onDismissRequest = { showRoomImagePreview = false },
                        properties = DialogProperties(usePlatformDefaultWidth = false)) {
                        Box(Modifier.fillMaxSize().background(Color.Black)) {
                            AsyncImage(model = m.imageUrl, contentDescription = "Image preview",
                                modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                            IconButton(onClick = { showRoomImagePreview = false },
                                modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(start = 8.dp, top = 6.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Close image", tint = Color.White)
                            }
                        }
                    }
                }
            } else NativeChatBubbleMessage(
                userId = m.senderAccountId,
                modifier = Modifier.align(Alignment.Start)
                    .twoSecondLongPress { if (m.text.isNotBlank()) onCopyMessage(m.text) }
                    .clip(RoundedCornerShape(topStart=2.dp,topEnd=12.dp,bottomStart=12.dp,bottomEnd=12.dp))
                    .border(0.5.dp, Color.White.copy(alpha=.15f), RoundedCornerShape(topStart=2.dp,topEnd=12.dp,bottomStart=12.dp,bottomEnd=12.dp))
            ) {
                Box(Modifier.padding(horizontal=10.dp, vertical=8.dp)) {
                    if (m.mentions.isEmpty()) {
                        Text(m.text,color=Color.White,fontSize=13.sp,lineHeight=18.sp,softWrap=true)
                    } else {
                        val annotated = androidx.compose.ui.text.buildAnnotatedString {
                            var cursor = 0
                            val matches = m.mentions.mapNotNull { mention ->
                                val start = m.text.indexOf("@${mention.name}")
                                if (start >= 0) Triple(start, start + mention.name.length + 1, mention) else null
                            }.sortedBy { it.first }
                            matches.forEach { (start, end, mention) ->
                                if (start < cursor) return@forEach
                                append(m.text.substring(cursor, start))
                                pushStringAnnotation(tag = "room-mention", annotation = mention.accountId)
                                pushStyle(androidx.compose.ui.text.SpanStyle(color = Color(0xFFE2C67D), fontWeight = FontWeight.SemiBold))
                                append(m.text.substring(start, end))
                                pop()
                                pop()
                                cursor = end
                            }
                            if (cursor < m.text.length) append(m.text.substring(cursor))
                        }
                        androidx.compose.foundation.text.ClickableText(
                            text = annotated,
                            style = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 13.sp, lineHeight = 18.sp),
                            onClick = { offset ->
                                val account = annotated.getStringAnnotations("room-mention", offset, offset).firstOrNull()?.item
                                m.mentions.firstOrNull { it.accountId == account }?.let(onMentionClick)
                            }
                        )
                    }
                }
            }
        }
    }
}
@Composable private fun RoomExitMenu(onKeep:()->Unit,onExit:()->Unit,onDismiss:()->Unit){
    val raw="https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/"
    Box(Modifier.fillMaxSize().background(Color.Transparent).clickable{onDismiss()},contentAlignment=Alignment.TopCenter){
        Box(modifier = Modifier.fillMaxWidth().height(48.percentHeight).background(Brush.verticalGradient(listOf(Color.Black,Color.Black,Color.Black.copy(alpha=.5f),Color.Transparent))).clickable(enabled=false){})
        Column(modifier = Modifier.fillMaxWidth().height(32.percentHeight),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){
            Row(Modifier.fillMaxWidth().padding(horizontal=42.dp).offset(y=(-4).dp),horizontalArrangement=Arrangement.spacedBy(64.dp),verticalAlignment=Alignment.CenterVertically){
                Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.weight(1f)){
                    AsyncImage(model=raw+"IMG_20261002_113229.png",contentDescription="Keep",modifier=Modifier.size(67.dp).clickable{onKeep()},contentScale=ContentScale.Fit)
                    Text("Keep",color=Color.White,fontSize=14.sp,fontWeight=androidx.compose.ui.text.font.FontWeight.SemiBold)
                }
                Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.weight(1f)){
                    AsyncImage(model=raw+"IMG_20261002_113213.png",contentDescription="Exit",modifier=Modifier.size(67.dp).clickable{onExit()},contentScale=ContentScale.Fit)
                    Text("Exit",color=Color.White,fontSize=14.sp,fontWeight=androidx.compose.ui.text.font.FontWeight.SemiBold)
                }
            }
        }
    }
}

private val Int.percentHeight: androidx.compose.ui.unit.Dp
    @Composable get() = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp * this / 100

@Composable private fun MemberSheet(seat:NativeRoomSeat,onClose:()->Unit)=SimpleSheet("Member",onClose){
    FallbackAsyncImage(model=seat.userImage,contentDescription=null,modifier=Modifier.size(76.dp).clip(CircleShape),contentScale=ContentScale.Crop)
    Text(seat.userName,color=Color.White,fontSize=17.sp);if (seat.accountId in setOf("500002", "500003", "500004", "500005", "500005")) { Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(3.dp)) { AsyncImage(model="https://zyora-voice-chat.vercel.app/IMG_20261101_125212.png",contentDescription="Purple ID Badge",contentScale=ContentScale.Fit,modifier=Modifier.height(18.dp).widthIn(max=28.dp)); Text(seat.accountId,color=Color.White.copy(.65f),fontSize=12.sp) } } else Text("ID: "+seat.accountId,color=Color.White.copy(.65f),fontSize=12.sp)
}

@Composable private fun GamesSheet(onClose:()->Unit,onWildParty:()->Unit,onDragonBattle:()->Unit){
    val wildIconUrl="https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/file_000000009d808211b8ffb7c2183b4ef5.png"
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.30f)).clickable{onClose()},contentAlignment=Alignment.BottomCenter){
        Surface(Modifier.fillMaxWidth().clickable(enabled=false){},color=Color.Black.copy(alpha=0.92f),shape=RoundedCornerShape(topStart=15.dp,topEnd=15.dp)){
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(start=16.dp,end=16.dp,top=12.dp,bottom=24.dp)){
                Text("Games",color=Color.White,fontSize=16.sp,fontWeight=FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                Row(modifier=Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalAlignment=Alignment.Top){
                    // Column 1: Wild Party
                    Column(Modifier.weight(1f).clickable{onWildParty()},horizontalAlignment=Alignment.CenterHorizontally){
                        AsyncImage(model=wildIconUrl,contentDescription="Wild party",modifier=Modifier.size(56.dp),contentScale=ContentScale.Fit)
                        Spacer(Modifier.height(2.dp))
                        Text("Wild party",color=Color.White,fontSize=10.sp,fontWeight=FontWeight.Bold,maxLines=1,softWrap=false)
                    }
                    // Column 2: Dragon Battle
                    Box(Modifier.weight(1f),contentAlignment=Alignment.TopCenter){ DragonBattleGameItem(onClick=onDragonBattle) }
                    Spacer(Modifier.weight(2f))
                }
            }
        }
    }
}

@Composable private fun SimpleSheet(title:String,onClose:()->Unit,content: @Composable () -> Unit){
    Box(Modifier.fillMaxSize().background(Color.Black.copy(.42f)).clickable{onClose()},contentAlignment=Alignment.BottomCenter){
        Surface(Modifier.fillMaxWidth().clickable(enabled=false){},color=Color(0xFF171717),shape=RoundedCornerShape(topStart=22.dp,topEnd=22.dp)){
            Column(Modifier.padding(18.dp).navigationBarsPadding()){
                Row(verticalAlignment=Alignment.CenterVertically){Text(title,color=Color.White,fontSize=18.sp,modifier=Modifier.weight(1f));IconButton(onClick=onClose){Icon(Icons.Default.Close,null,tint=Color.White)}}
                content();Spacer(Modifier.height(18.dp))
            }
        }
    }
}

private fun roomCountryFlag(country: String): String {
    val value = country.trim()
    return when {
        value.contains("india", true) || value.contains("🇮🇳") -> "🇮🇳"
        value.contains("singapore", true) || value.contains("🇸🇬") -> "🇸🇬"
        value.contains("pakistan", true) || value.contains("🇵🇰") -> "🇵🇰"
        value.contains("usa", true) || value.contains("united states", true) || value.contains("🇺🇸") -> "🇺🇸"
        value.contains("south africa", true) || value.contains("🇿🇦") -> "🇿🇦"
        value.contains("turkey", true) || value.contains("türkiye", true) || value.contains("🇹🇷") -> "🇹🇷"
        value.isBlank() -> ""
        else -> countryFlag(value)
    }
}
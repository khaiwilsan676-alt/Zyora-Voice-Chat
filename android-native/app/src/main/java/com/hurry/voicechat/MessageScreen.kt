package com.hurry.voicechat

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.android.gms.tasks.Tasks
import android.os.Build
import android.os.Handler
import android.os.Looper
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import org.json.JSONObject
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Image
import androidx.compose.runtime.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ExperimentalGraphicsApi
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val RAW_MSG = "https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/"


private fun containsProhibitedChatText(value: String): Boolean {
    val words = value.lowercase(Locale.ROOT)
        .split(Regex("[^\\p{L}\\p{N}]+"))
        .map { token -> token.filter { ch -> ch.isLetterOrDigit() } }
        .filter { it.isNotBlank() }
    val prohibited = setOf(
        "madarchod", "madarchot", "madarchood", "maderchod", "mc", "bhenchod",
        "behenchod", "bhench0d", "behench0d", "bhench*d", "bc", "bhosdike",
        "bhosdi", "bhosda", "bhosdiwale", "bhosad", "bhosadike", "bhosrika",
        "chutiya", "chutia", "chutiye", "chootiya", "chutiaa", "chut", "gaand",
        "gandu", "gaanduu", "gaandmara", "gaandfat", "lund", "lodu", "lode",
        "lavde", "laude", "lawde", "lawda", "randi", "rand", "harami",
        "haramzada", "haraamzada", "kutta", "kutti", "kamina", "kamine",
        "saali", "saala", "sala", "bitch", "bitches", "bastard", "asshole",
        "fuck", "fucking", "fucker", "motherfucker", "shit", "shitty", "bullshit",
        "dick", "dickhead", "pussy", "slut", "whore", "sonofabitch", "dumbass",
        "idiot", "moron", "piss", "wanker", "twat",
        // Additional common Hindi/Hinglish spellings and abbreviations.
        "bsdk", "bhosdike", "bhosdika", "bhosdiki", "bhosd", "bhosra",
        "behnchod", "bhench0d", "bhenchood", "bhenchut", "bhenkelode",
        "mkc", "mkl", "bkl", "lodu", "chodu", "ch00tiya", "ch00t",
        "gand", "gandu", "gandfat", "gandmara", "gaandichut", "jhaatu",
        "jhatu", "jhaant", "jhant", "tatte", "tatta", "suar", "suwar",
        "rakhail", "raand", "randwa", "haramkhor", "haramkhoron",
        "kamjat", "kameena", "kameeni", "kutte", "kuttiya", "kutti",
        "besharam", "nalayak", "chamaar", "chamar", "neech", "gadha",
        "gadhi", "pig", "cunt", "cocksucker", "prick", "jerkoff",
        "chhinal", "chinal", "chhinar", "bhosdiwala", "bhosdiwali",
        "bhosdikee", "bhosadchod", "bhosdichod", "bhosdichut",
        "gaandchatu", "gandchatu", "gaandphatu", "gandphatu",
        "chutmar", "chutmaar", "chutiyapa", "chutiyapan",
        "madarch00d", "madarch0d", "m4darchod", "bhench0dd",
        "bhosd1ke", "bh0sdike", "chut1ya", "g4ndu", "l0du",
        "fuk", "fuker", "fuking", "fck", "fcking", "sh1t",
        "a55hole", "b1tch", "d1ck",
        "लंड", "लौड़ा", "लौडे", "चूतिया", "चूत", "भोसड़ी",
        "भोसड़ीके", "मादरचोद", "बहनचोद", "गांडू", "रंडी",
        // Other voice-chat and social-room application names.
        "sama", "wafa", "yoyo", "sugo", "sugolite", "xena", "timo",
        "achat", "funplay", "soulchill", "yaahlan", "layla", "hala",
        "yoho", "tikko", "saada", "amar", "baat", "yalla", "friendchat",
        "nivi", "starchat", "yooy", "clubhouse", "yubo", "nahki",
        "lamalite", "fofochat", "amo", "yari", "opalive", "falla",
        "funni", "ume", "habi", "oichat", "ahlan", "songora",
        "mico", "weplay", "hago", "wakie", "zello", "guilded",
        "teamspeak", "beela", "paltalk", "frnd", "chamet",
        "connecto", "zeep", "livetalk", "dostt", "vibly",
        "voiceclub", "videovibe", "discord", "telegram", "whatsapp",
        "viber", "xspaces", "spoon", "tandem", "hellotalk",
        "roblox", "vrchat", "recroom", "guilded", "steam",
        "xbox", "playstation", "pal talk", "free4talk",
        // Additional voice-chat, social-audio and stranger-chat app names.
        "litmatch", "vibely", "dostify", "mychatclub", "helloyo",
        "halame", "wafaludo", "yallaludo", "hawachat", "starmaker",
        "airtalk", "talkwithstranger", "maum", "mindfuse", "whisperly",
        "hear me", "hearme", "dostify", "dostt", "connecto",
        "voiceclub", "vibely", "litmatch", "beela chat", "beelachat",
        "star chat", "starchat", "x spaces", "xspaces", "club house",
        "hello yo", "hello you", "yalla ludo", "wafa ludo",
        "nintendo gamechat", "gamechat", "messenger rooms", "facebook messenger",
        "instagram live", "tiktok live", "bigo live", "bigo",
        "livu", "azeus", "tumile", " omi", "ome tv", "ometv",
        "chatous", "monkey", "azarlive", "azar", "chatrandom",
        "chatroulette", "camsurf", "tinychat", "paltalk", "camfrog",
        "skout", "meetme", "tagged", "hily", "litmatch"
    )
    val compactInput = value.lowercase(Locale.ROOT)
        .filter { it.isLetterOrDigit() }
        .replace('0', 'o').replace('1', 'i').replace('3', 'e')
        .replace('4', 'a').replace('5', 's').replace('8', 'b')
    return words.any { word ->
        val normalized = word.replace('0', 'o').replace('1', 'i').replace('3', 'e').replace('4', 'a').replace('5', 's').replace('8', 'b')
        normalized in prohibited || (normalized.length >= 5 && prohibited.any { it.length >= 5 && normalized.contains(it) })
    } || prohibited.any { entry ->
        val compactEntry = entry.filter { it.isLetterOrDigit() }
            .replace('0', 'o').replace('1', 'i').replace('3', 'e')
            .replace('4', 'a').replace('5', 's').replace('8', 'b')
        compactEntry.length >= 5 && compactInput.contains(compactEntry)
    }
}

data class ZyoraIncomingMessageBanner(
    val messageId: String,
    val senderId: String,
    val senderName: String,
    val senderAvatar: String,
    val messageText: String
)

object ZyoraMessageBannerState {
    var current by mutableStateOf<ZyoraIncomingMessageBanner?>(null)
}

// Added unreadCount to track new messages
private data class MessagePreview(val uid: String, val name: String, val image: String, val message: String, val time: String = "", val unreadCount: Int = 0, val isInRoom: Boolean = false, val roomId: String = "")

private fun refreshTotalUnreadCount(context: Context, accountId: String) {
    if (accountId.isBlank()) return
    val unreadPrefs = context.getSharedPreferences("zyora_unread_messages", Context.MODE_PRIVATE)
    val prefix = "unread_${accountId}_"
    val total = unreadPrefs.all.entries
        .asSequence()
        .filter { it.key.startsWith(prefix) }
        .sumOf { (it.value as? Int ?: 0).coerceAtLeast(0) }
    unreadPrefs.edit().putInt("unread_${accountId}", total).apply()
}

private fun resolveChatAvatar(value: String): String {
    val image = value.trim()
    if (image.isBlank() || image == "null" || image == "/default-avatar.png") {
        return RAW_MSG + "default-avatar.png"
    }
    if (image.startsWith("http://") || image.startsWith("https://") ||
        image.startsWith("data:") || image.startsWith("content:") || image.startsWith("file:")) return image
    return "https://zyora-server.onrender.com/" + image.trimStart('/')
}

@OptIn(ExperimentalGraphicsApi::class)
@Composable
fun rememberVerifiedChatIcon(): ImageVector {
    return remember {
        ImageVector.Builder(
            name = "VerifiedChat",
            defaultWidth = 48.dp,
            defaultHeight = 48.dp,
            viewportWidth = 48f,
            viewportHeight = 48f
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 3.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(24f, 5f)
                curveTo(13.5f, 5f, 5f, 13.5f, 5f, 24f)
                curveTo(5f, 34.5f, 13.5f, 43f, 24f, 43f)
                lineTo(38f, 43f)
                lineTo(38f, 35.5f)
                curveTo(38f, 33.5f, 43f, 29f, 43f, 24f)
                curveTo(43f, 13.5f, 34.5f, 5f, 24f, 5f)
                close()
            }
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 4f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(15.5f, 23.5f)
                lineTo(22f, 29.5f)
                lineTo(33f, 18f)
            }
        }.build()
    }
}

@OptIn(ExperimentalGraphicsApi::class)
@Composable
fun rememberVoiceWaveIcon(): ImageVector {
    return remember {
        ImageVector.Builder("VoiceWave", 24.dp, 24.dp, 24f, 24f).apply {
            path(stroke = SolidColor(Color.Black), strokeLineWidth = 2.5f, strokeLineCap = StrokeCap.Round) {
                moveTo(5f, 10f); lineTo(5f, 14f)
                moveTo(9f, 7f); lineTo(9f, 17f)
                moveTo(13f, 4f); lineTo(13f, 20f)
                moveTo(17f, 7f); lineTo(17f, 17f)
                moveTo(21f, 10f); lineTo(21f, 14f)
            }
        }.build()
    }
}

@OptIn(ExperimentalGraphicsApi::class)
@Composable
fun rememberKeyboardIcon(): ImageVector {
    return remember {
        ImageVector.Builder("Keyboard", 24.dp, 24.dp, 24f, 24f).apply {
            path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(3f, 7f); lineTo(21f, 7f); lineTo(21f, 17f); lineTo(3f, 17f); close()
                moveTo(7f, 11f); lineTo(8f, 11f)
                moveTo(11f, 11f); lineTo(12f, 11f)
                moveTo(15f, 11f); lineTo(16f, 11f)
                moveTo(7f, 14f); lineTo(17f, 14f)
            }
        }.build()
    }
}

// Chota Music Track Animation component Pill icon ke liye
@Composable
fun SmallMusicEqualizer() {
    val infiniteTransition = rememberInfiniteTransition(label = "small_eq")
    val bar1 by infiniteTransition.animateFloat(initialValue = 3f, targetValue = 7f, animationSpec = infiniteRepeatable(tween(400), RepeatMode.Reverse), label = "b1")
    val bar2 by infiniteTransition.animateFloat(initialValue = 7f, targetValue = 3f, animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "b2")
    val bar3 by infiniteTransition.animateFloat(initialValue = 4f, targetValue = 6f, animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "b3")

    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(1.5.dp)) {
        Box(Modifier.width(1.5.dp).height(bar1.dp).background(Color.White, CircleShape))
        Box(Modifier.width(1.5.dp).height(bar2.dp).background(Color.White, CircleShape))
        Box(Modifier.width(1.5.dp).height(bar3.dp).background(Color.White, CircleShape))
    }
}

// Swipe-to-Delete logic
@Composable
fun SwipeRevealItem(
    isOfficial: Boolean,
    onDelete: () -> Unit,
    content: @Composable () -> Unit
) {
    if (isOfficial) {
        content()
        return
    }

    val coroutineScope = rememberCoroutineScope()
    val offsetX = remember { androidx.compose.animation.core.Animatable(0f) }
    val density = LocalDensity.current
    val actionWidth = 90.dp
    val actionWidthPx = with(density) { actionWidth.toPx() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFEF4444)) // Red Delete Background
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(actionWidth)
                .fillMaxHeight()
                .clickable { onDelete() },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                Spacer(Modifier.height(4.dp))
                Text("Delete", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.toInt(), 0) }
                .background(Color(0xFFF1F1F1))
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            coroutineScope.launch {
                                if (offsetX.value < -actionWidthPx / 2) {
                                    offsetX.animateTo(-actionWidthPx)
                                } else {
                                    offsetX.animateTo(0f)
                                }
                            }
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        coroutineScope.launch {
                            offsetX.snapTo((offsetX.value + dragAmount).coerceIn(-actionWidthPx, 0f))
                        }
                    }
                }
        ) {
            content()
        }
    }
}

@Composable
fun PrivateMessageInboxSync() {
    val context = LocalContext.current
    val authPrefs = remember { context.getSharedPreferences("hurry_auth", Context.MODE_PRIVATE) }
    val userPrefs = remember { context.getSharedPreferences("hurry_user", Context.MODE_PRIVATE) }
    val authUid = authPrefs.getString("uid", "").orEmpty().ifBlank { userPrefs.getString("uid", "").orEmpty() }
    val accountId = authPrefs.getString("accountId", "").orEmpty()
        .ifBlank { userPrefs.getString("accountId", userPrefs.getString("userId", authUid)).orEmpty() }
        .ifBlank { authUid }
    val indexDb = remember { NativeIndexDatabase(context) }
    DisposableEffect(authUid, accountId) {
        if (authUid.isBlank() || accountId.isBlank()) return@DisposableEffect onDispose { }
        val options = IO.Options().apply { transports = arrayOf("websocket"); reconnection = true; reconnectionAttempts = Int.MAX_VALUE }
        val socket = IO.socket("https://zyora-server.onrender.com", options)
        val mainHandler = Handler(Looper.getMainLooper())
        val incoming = Emitter.Listener { args ->
            val payload = args.firstOrNull() as? JSONObject ?: return@Listener
            val receiverIds = setOf(authUid, accountId)
            if (payload.optString("receiverId") !in receiverIds && payload.optString("receiverAccountId") !in receiverIds) return@Listener
            val peerId = payload.optString("senderAccountId").ifBlank { payload.optString("senderId") }
            if (peerId.isBlank()) return@Listener
            val messageId = payload.optString("id")
            val isVoice = payload.optString("type") == "voice"
            val body = if (isVoice) VoiceMessageStore.saveIncoming(context, payload) ?: ("VOICE::" + payload.optLong("durationSec", 1))
                else when (payload.optString("type")) {
                    "image" -> "IMAGE::" + payload.optString("imageUrl")
                    "room_invitation" -> "ROOM_INVITE::" + (payload.optJSONObject("roomData")?.toString() ?: "{}")
                    else -> payload.optString("text")
                }
            val preview = when {
                isVoice -> "🎤 Voice message"
                payload.optString("type") == "image" -> "📷 Image"
                payload.optString("type") == "room_invitation" -> "Room invitation"
                else -> body
            }
            mainHandler.post {
                runCatching {
                    val chatKey = "chat_" + accountId + "_" + peerId
                    val history = org.json.JSONArray(indexDb.loadChat(chatKey) ?: "[]")
                    val alreadySaved = (0 until history.length()).any { i ->
                        messageId.isNotBlank() && history.optJSONObject(i)?.optString("id") == messageId
                    }
                    if (!alreadySaved) {
                        history.put(JSONObject().put("id", messageId).put("mine", false).put("text", body))
                        indexDb.saveChat(chatKey, history.toString())
                        // Publish only after the message is safely persisted; duplicate socket deliveries
                        // therefore cannot create repeated top banners.
                        ZyoraMessageBannerState.current = ZyoraIncomingMessageBanner(
                            messageId = messageId,
                            senderId = peerId,
                            senderName = payload.optString("senderName", "User").ifBlank { "User" },
                            senderAvatar = payload.optString("senderPhoto", ""),
                            messageText = preview.ifBlank { "New message" }
                        )
                        
                        // User-specific unread count logic update
                        val unreadPrefs = context.getSharedPreferences("zyora_unread_messages", Context.MODE_PRIVATE)
                        val unreadKey = "unread_" + accountId + "_" + peerId
                        unreadPrefs.edit().putInt(unreadKey, unreadPrefs.getInt(unreadKey, 0) + 1).apply()
                        refreshTotalUnreadCount(context, accountId)
                    }
                    val timestamp = payload.optLong("timestamp", System.currentTimeMillis())
                    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
                    val saved = JSONObject().put("peerId", peerId).put("peerName", payload.optString("senderName", "User"))
                        .put("peerPhoto", payload.optString("senderPhoto", "")).put("lastMessage", preview)
                        .put("timestamp", timestamp).put("time", time)
                    indexDb.saveConversation("conversation_" + accountId + "_" + peerId, peerId, saved)
                    if (messageId.isNotBlank()) socket.emit("private_message_received", JSONObject().put("id", messageId))
                }.onFailure { android.util.Log.e("ZyoraInboxSync", "Could not persist incoming message", it) }
            }
        }
        val sent = Emitter.Listener { args ->
            val payload = args.firstOrNull() as? JSONObject ?: return@Listener
            val peerId = payload.optString("receiverAccountId").ifBlank { payload.optString("receiverId") }
            if (peerId.isBlank()) return@Listener
            val body = payload.optString("previewText").ifBlank { payload.optString("text") }
            val preview = if (payload.optString("type") == "voice") "🎤 Voice message"
                else body.ifBlank { if (payload.optString("type") == "image") "📷 Image" else "" }
            mainHandler.post {
                val timestamp = payload.optLong("timestamp", System.currentTimeMillis())
                val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
                val saved = JSONObject().put("peerId", peerId).put("peerName", payload.optString("receiverName", "User"))
                    .put("peerPhoto", payload.optString("receiverPhoto", "")).put("lastMessage", preview)
                    .put("timestamp", timestamp).put("time", time)
                indexDb.saveConversation("conversation_" + accountId + "_" + peerId, peerId, saved)
            }
        }
        socket.on("private_message", incoming)
        socket.on("private_message_sent", sent)
        socket.on(Socket.EVENT_CONNECT) { socket.emit("register", JSONObject().put("userId", authUid).put("accountId", accountId)) }
        socket.connect()
        onDispose {
            socket.off("private_message", incoming); socket.off("private_message_sent", sent)
            socket.disconnect(); socket.close()
        }
    }
}

@Composable
fun MessageScreen(onChat: (String, String, String) -> Unit, embeddedSheet: Boolean = false, onBack: () -> Unit = {}, onJoinRoom: (String) -> Unit = {}) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("hurry_user", Context.MODE_PRIVATE) }
    val authPrefs = remember { context.getSharedPreferences("hurry_auth", Context.MODE_PRIVATE) }
    val unreadPrefs = remember { context.getSharedPreferences("zyora_unread_messages", Context.MODE_PRIVATE) }
    
    val currentAuthUid = authPrefs.getString("uid", "").orEmpty().ifBlank { prefs.getString("uid", "").orEmpty() }
    val currentUserId = authPrefs.getString("accountId", "").orEmpty().ifBlank { prefs.getString("accountId", prefs.getString("userId", currentAuthUid)).orEmpty() }.ifBlank { currentAuthUid }
    val indexDb = remember { NativeIndexDatabase(context) }
    val chats = remember {
        mutableStateListOf<MessagePreview>().apply {
            add(MessagePreview("hurry_team_official", "Zyora Team", RAW_MSG + "logo.png", ""))
            add(MessagePreview("hurry_system_official", "Zyora System", RAW_MSG + "file_00000000a66881f8aa9e15d2fe2b9a0c.png", ""))
            if (currentUserId.isNotBlank()) indexDb.loadConversations("conversation_${currentUserId}_").forEach { saved ->
                val peerId = saved.optString("peerId")
                val unread = unreadPrefs.getInt("unread_${currentUserId}_${peerId}", 0) // Fetch unread messages
                if (peerId.isNotBlank() && none { it.uid == peerId }) add(MessagePreview(peerId, saved.optString("peerName", "User"), saved.optString("peerPhoto", ""), saved.optString("lastMessage", ""), saved.optString("time", ""), unread))
            }
        }
    }
    DisposableEffect(currentUserId, chats.map { it.uid }) {
        if (currentUserId.isBlank()) return@DisposableEffect onDispose { }
        val presenceSocket = IO.socket("https://zyora-server.onrender.com", IO.Options().apply {
            transports = arrayOf("websocket")
            reconnection = true
            reconnectionAttempts = Int.MAX_VALUE
        })
        fun applyRoomStatus(payload: JSONObject?) {
            if (payload == null) return
            val peerId = payload.optString("accountId", payload.optString("userId")).trim()
            if (peerId.isBlank()) return
            val loginId = payload.optString("userId").trim()
            val index = chats.indexOfFirst { it.uid == peerId || (loginId.isNotBlank() && it.uid == loginId) }
            if (index >= 0) {
                val old = chats[index]
                val inRoom = payload.optBoolean("inRoom", false) && payload.optString("roomId").isNotBlank()
                chats[index] = old.copy(isInRoom = inRoom, roomId = if (inRoom) payload.optString("roomId") else "")
            }
        }
        val statusListener = Emitter.Listener { args -> applyRoomStatus(args.firstOrNull() as? JSONObject) }
        val updateListener = Emitter.Listener { args -> applyRoomStatus(args.firstOrNull() as? JSONObject) }
        presenceSocket.on("user_room_status", statusListener)
        presenceSocket.on("user_room_status_updated", updateListener)
        presenceSocket.on(Socket.EVENT_CONNECT) {
            chats.filter { it.uid.isNotBlank() && !it.uid.startsWith("hurry_") }.forEach { chat ->
                presenceSocket.emit("user_room_status_request", JSONObject().put("userId", chat.uid).put("accountId", chat.uid))
            }
        }
        presenceSocket.connect()
        onDispose {
            presenceSocket.off("user_room_status", statusListener)
            presenceSocket.off("user_room_status_updated", updateListener)
            presenceSocket.disconnect()
            presenceSocket.close()
        }
    }

    fun updateConversationPreview(peerId: String, peerName: String, peerPhoto: String, lastMessage: String, timestamp: Long) {
        if (peerId.isBlank() || peerId == currentUserId) return
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
        val saved = JSONObject().put("peerId", peerId).put("peerName", peerName).put("peerPhoto", peerPhoto).put("lastMessage", lastMessage).put("timestamp", timestamp).put("time", time)
        indexDb.saveConversation("conversation_${currentUserId}_$peerId", peerId, saved)
        
        val unread = unreadPrefs.getInt("unread_${currentUserId}_${peerId}", 0)
        
        val existing = chats.indexOfFirst { it.uid == peerId }
        if (existing >= 0) chats.removeAt(existing)
        chats.add(2.coerceAtMost(chats.size), MessagePreview(peerId, peerName.ifBlank { "User" }, peerPhoto, lastMessage, time, unread))
    }

    Column(Modifier.fillMaxSize().background(Color(0xFFF1F1F1))) {
        if (embeddedSheet) {
            Box(Modifier.fillMaxWidth().height(54.dp).background(Color(0xFFF1F1F1))) {
                IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart).size(48.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF1F2937))
                }
                Text("Message", modifier = Modifier.align(Alignment.Center), color = Color(0xFF1F2937), fontSize = 19.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            Box(
                Modifier.fillMaxWidth().background(
                    Brush.verticalGradient(
                        0.0f to Color(0xFF3B82F6),
                        0.3f to Color(0xFF3B82F6),
                        0.75f to Color(0xFFF1F1F1),
                        1.0f to Color(0xFFF1F1F1)
                    )
                ).statusBarsPadding().padding(top = 12.dp, bottom = 12.dp)
            ) {
                Text(
                    "Message",
                    Modifier.align(Alignment.Center),
                    color = Color.Black, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold
                )
                
                Icon(
                    imageVector = rememberVerifiedChatIcon(),
                    contentDescription = "Custom Icon",
                    tint = Color.Unspecified,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 16.dp)
                        .size(30.dp)
                )
            }
        }

        LazyColumn(Modifier.fillMaxSize().background(Color(0xFFF1F1F1)).padding(bottom = 8.dp)) {
            items(chats, key = { it.uid }) { chat ->
                SwipeRevealItem(
                    isOfficial = (chat.uid == "hurry_team_official" || chat.uid == "hurry_system_official"),
                    onDelete = { chats.remove(chat) }
                ) {
                    MessageRow(chat, onClick = { onChat(chat.uid, chat.name, chat.image) }, onJoinRoom = onJoinRoom)
                }
            }
        }
    }
}

@Composable
private fun MessageRow(chat: MessagePreview, onClick: () -> Unit, onJoinRoom: (String) -> Unit) {
    // Require a confirmed live room ID; stale booleans must not keep room UI visible.
    val isOfficial = chat.uid == "hurry_team_official" || chat.uid == "hurry_system_official"
    val showRoomPresence = !isOfficial && chat.isInRoom && chat.roomId.isNotBlank()

    Row(
        Modifier.fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        
        // Avatar + Blue Border + Bottom Music Pill Icon (Sirf normal user ke liye)
        Box(contentAlignment = Alignment.BottomCenter, modifier = Modifier.clickable(enabled = showRoomPresence) { if (showRoomPresence) onJoinRoom(chat.roomId) }) {
            AsyncImage(
                model = resolveChatAvatar(chat.image.ifBlank { RAW_MSG + "default-avatar.png" }), contentDescription = chat.name,
                modifier = Modifier
                    .size(56.dp)
                    // Conditional border (Agar official nahi hai, tabhi border apply hoga)
                    .then(if (showRoomPresence) Modifier.border(2.dp, Color(0xFF3B82F6), CircleShape) else Modifier)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            
            // Music Pill Icon at bottom center (Sirf normal user ke liye)
            if (showRoomPresence) {
                Box(
                    modifier = Modifier
                        .offset(y = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF3B82F6))
                        .clickable { onJoinRoom(chat.roomId) }
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    SmallMusicEqualizer() 
                }
            }
        }
        
        Spacer(Modifier.width(12.dp)) 
        
        Column(Modifier.weight(1f)) {
            Text(chat.name, color = Color(0xFF1F2937), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            if (chat.message.isNotEmpty()) {
                Spacer(Modifier.height(3.dp))
                Text(chat.message, color = Color(0xFF6B7280), fontSize = 14.sp, maxLines = 1)
            }
        }
        
        // Time & Unread Badge (Red circle, time se theek pehle)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (chat.unreadCount > 0) {
                Box(
                    modifier = Modifier
                        .defaultMinSize(minWidth = 18.dp)
                        .height(18.dp)
                        .background(Color.Red, CircleShape)
                        .padding(horizontal = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (chat.unreadCount > 99) "99+" else chat.unreadCount.toString(),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        style = androidx.compose.ui.text.TextStyle(
                            platformStyle = androidx.compose.ui.text.PlatformTextStyle(
                                includeFontPadding = false
                            )
                        )
                    )
                }
                Spacer(Modifier.width(6.dp))
            }
            if (chat.time.isNotEmpty()) {
                Text(chat.time, color = Color(0xFF999999), fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun NativeChatScreen(
    name: String,
    image: String,
    receiverId: String = "",
    currentUserId: String = "",
    currentAccountIdOverride: String = "",
    onBack: () -> Unit,
    onJoinRoom: (String) -> Unit = {},
    onOpenPublicProfile: () -> Unit = {},
    initialRoomInvitation: String? = null
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("hurry_user", Context.MODE_PRIVATE) }
    val authPrefs = remember { context.getSharedPreferences("hurry_auth", Context.MODE_PRIVATE) }
    val currentPhoto = sequenceOf(
        prefs.getString("photo", "").orEmpty(),
        prefs.getString("image", "").orEmpty(),
        authPrefs.getString("image", "").orEmpty()
    ).firstOrNull { it.isNotBlank() && it != "null" }.orEmpty()
    val currentName = sequenceOf(
        prefs.getString("name", "").orEmpty(),
        authPrefs.getString("name", "").orEmpty()
    ).firstOrNull { it.isNotBlank() && it != "null" } ?: "Zyora User"
    val currentAccountId = currentAccountIdOverride.ifBlank { authPrefs.getString("accountId", prefs.getString("accountId", currentUserId)).orEmpty() }.ifBlank { currentUserId }
    val isOfficial = name == "Zyora Team" || name == "Zyora System"
    var directSocket by remember(receiverId, currentUserId) { mutableStateOf<Socket?>(null) }
    var chatRegistered by remember(receiverId, currentUserId) { mutableStateOf(false) }
    var peerOnline by remember(receiverId) { mutableStateOf(false) }
    var peerRoomId by remember(receiverId) { mutableStateOf("") }

    var text by remember { mutableStateOf("") }
    var showOptions by remember { mutableStateOf(false) }
    var showReportPage by remember { mutableStateOf(false) }
    var isUserBlocked by remember(receiverId) { mutableStateOf(false) }
    var blockedByPeer by remember(receiverId) { mutableStateOf(false) }
    var blockStateReady by remember(receiverId) { mutableStateOf(false) }
    var blockPopupText by remember { mutableStateOf("") }
    var copyPopupText by remember { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current
    LaunchedEffect(copyPopupText) {
        if (copyPopupText.isNotBlank()) {
            kotlinx.coroutines.delay(1600)
            copyPopupText = ""
        }
    }
    var replyText by remember { mutableStateOf<String?>(null) }
    var previewImageUrl by remember { mutableStateOf<String?>(null) }
    var roomInvitationSent by remember(receiverId, initialRoomInvitation) { mutableStateOf(false) }
    
    LaunchedEffect(currentAccountId, receiverId) {
        if (currentAccountId.isNotBlank() && receiverId.isNotBlank()) {
            val state = withContext(Dispatchers.IO) { fetchZyoraUserBlockState(currentAccountId, receiverId) }
            isUserBlocked = state.first
            blockedByPeer = state.second
            blockStateReady = true
        }
    }
    LaunchedEffect(blockPopupText) {
        if (blockPopupText.isNotBlank()) {
            kotlinx.coroutines.delay(1800)
            blockPopupText = ""
        }
    }

    // Scam banner hide/show
    var showScamBanner by remember { mutableStateOf(true) }
    
    // Voice Mode States
    var isVoiceMode by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var recordStartTime by remember { mutableLongStateOf(0L) }
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recordedVoiceFile by remember { mutableStateOf<File?>(null) }
    var voicePlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    val audioPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    // Equalizer Animation State
    val infiniteTransition = rememberInfiniteTransition(label = "equalizer")
    val bar1Height by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "bar1"
    )
    val bar2Height by infiniteTransition.animateFloat(
        initialValue = 12f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "bar2"
    )
    val bar3Height by infiniteTransition.animateFloat(
        initialValue = 6f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "bar3"
    )

    val messageListState = androidx.compose.foundation.lazy.rememberLazyListState()
    val officialIdForCache = when (name) {
        "Zyora Team" -> "hurry_team_official"
        "Zyora System" -> "hurry_system_official"
        else -> null
    }
    val cacheKey = "official_chat_cache_${officialIdForCache ?: name}"
    val indexDb = remember { NativeIndexDatabase(context) }
    val localConversationKey = "chat_${currentAccountId}_${receiverId.ifBlank { officialIdForCache ?: name }}"
    
    // Yaha unread count reset karne ki ninja technique lagai gayi hai (Jaise hi tum chat khologe count gayaab)
    LaunchedEffect(receiverId, currentAccountId) {
        if (receiverId.isNotBlank() && currentAccountId.isNotBlank()) {
            val unreadPrefs = context.getSharedPreferences("zyora_unread_messages", Context.MODE_PRIVATE)
            unreadPrefs.edit().putInt("unread_${currentAccountId}_${receiverId}", 0).apply()
            refreshTotalUnreadCount(context, currentAccountId)
        }
    }

    fun saveConversationPreview(peerId: String, peerName: String, peerPhoto: String, lastMessage: String, timestamp: Long) {
        if (peerId.isBlank() || currentAccountId.isBlank() || peerId == currentAccountId) return
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
        val preview = JSONObject().put("peerId", peerId).put("peerName", peerName.ifBlank { "User" }).put("peerPhoto", peerPhoto).put("lastMessage", lastMessage).put("timestamp", timestamp).put("time", time)
        indexDb.saveConversation("conversation_${currentAccountId}_$peerId", peerId, preview)
    }
    
    val messages = remember(name, receiverId, currentUserId) {
        mutableStateListOf<Pair<Boolean, String>>().apply {
            val dbCache = indexDb.loadChat(localConversationKey)
            val restored = runCatching {
                val array = org.json.JSONArray(dbCache ?: "[]")
                (0 until array.length()).mapNotNull { index ->
                    val entry = array.optJSONObject(index) ?: return@mapNotNull null
                    val value = entry.optString("text")
                    if (value.isNotBlank()) (entry.optBoolean("mine") to value) else null
                }
            }.getOrDefault(emptyList())
            if (restored.isNotEmpty()) addAll(restored)
            else if (officialIdForCache != null) {
                val cached = prefs.getString(cacheKey, "[]") ?: "[]"
                runCatching {
                    val array = org.json.JSONArray(cached)
                    for (index in 0 until array.length()) {
                        val entry = array.optJSONObject(index) ?: continue
                        val value = entry.optString("text")
                        if (value.isNotBlank()) add(false to value)
                    }
                }
            }
        }
    }
    val messageIds = remember(name, receiverId, currentUserId) {
        mutableStateListOf<String>().apply {
            val saved = runCatching { org.json.JSONArray(indexDb.loadChat(localConversationKey) ?: "[]") }.getOrDefault(org.json.JSONArray())
            for (index in 0 until saved.length()) add(saved.optJSONObject(index)?.optString("id").orEmpty())
            while (size < messages.size) add("")
        }
    }
    LaunchedEffect(messages.size, messageIds.size, localConversationKey) {
        val array = org.json.JSONArray()
        messages.forEachIndexed { index, (mine, value) ->
            array.put(JSONObject().put("id", messageIds.getOrNull(index).orEmpty()).put("mine", mine).put("text", value))
        }
        indexDb.saveChat(localConversationKey, array.toString())
    }
    val chatScope = rememberCoroutineScope()
    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val messageId = currentUserId + "_" + System.currentTimeMillis()
            chatScope.launch {
                val containsOtherAppUi = runCatching {
                    withContext(Dispatchers.IO) {
                        val image = InputImage.fromFilePath(context, uri)
                        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                        try {
                            val recognizedText = Tasks.await(recognizer.process(image)).text
                            containsProhibitedChatText(recognizedText)
                        } finally {
                            recognizer.close()
                        }
                    }
                }.getOrDefault(false)
                if (containsOtherAppUi) {
                    blockPopupText = "Can't Send Image-prohibited"
                    return@launch
                }
                val imageUrl = runCatching { uploadChatImage(context, uri, messageId, "chat-image") }.getOrNull()
                if (!imageUrl.isNullOrBlank()) {
                    val localImage = "IMAGE::" + imageUrl
                    messages.add(true to localImage)
                    messageIds.add(messageId)
                    saveConversationPreview(receiverId, name, image, "📷 Image", System.currentTimeMillis())
                    val payload = JSONObject().put("id", messageId).put("senderId", currentUserId)
                        .put("senderAccountId", currentAccountId).put("receiverId", receiverId)
                        .put("receiverAccountId", receiverId).put("senderName", currentName)
                        .put("senderPhoto", currentPhoto).put("receiverName", name).put("receiverPhoto", image)
                        .put("text", "📷 Image").put("type", "image").put("imageUrl", imageUrl)
                        .put("timestamp", System.currentTimeMillis())
                    if (chatRegistered && directSocket?.connected() == true) directSocket?.emit("private_message", payload)
                } else {
                    android.widget.Toast.makeText(context, "Image upload failed. Try an image under 5 MB.", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    LaunchedEffect(initialRoomInvitation, chatRegistered, receiverId) {
        if (!roomInvitationSent && !initialRoomInvitation.isNullOrBlank() && chatRegistered &&
            directSocket?.connected() == true && receiverId.isNotBlank()) {
            val roomData = runCatching { JSONObject(initialRoomInvitation) }.getOrNull()
            if (roomData != null) {
                // One invitation may be emitted only once. Persist the guard before
                // touching the socket so reconnects/recomposition cannot resend it.
                val inviteId = roomData.optString("inviteId").ifBlank {
                    currentUserId + "_" + receiverId + "_" + roomData.optString("roomId")
                }
                val sentKey = "room_invitation_sent_${currentUserId}_${receiverId}_${inviteId}"
                val alreadySent = prefs.getBoolean(sentKey, false)
                if (!alreadySent) {
                    prefs.edit().putBoolean(sentKey, true).commit()
                    roomInvitationSent = true
                    val messageId = inviteId
                    val sentAt = System.currentTimeMillis()
                    val localInvite = "ROOM_INVITE::" + roomData.toString()
                    messages.add(true to localInvite)
                    messageIds.add(messageId)
                    saveConversationPreview(receiverId, name, image, "Room invitation", sentAt)
                    directSocket?.emit("private_message", JSONObject()
                        .put("id", messageId).put("senderId", currentUserId).put("senderAccountId", currentAccountId)
                        .put("receiverId", receiverId).put("receiverAccountId", receiverId)
                        .put("senderName", currentName).put("senderPhoto", currentPhoto)
                        .put("receiverName", name).put("receiverPhoto", image)
                        .put("text", "Room invitation").put("type", "room_invitation")
                        .put("roomData", roomData).put("timestamp", sentAt))
                } else {
                    roomInvitationSent = true
                }
            }
        }
    }

    fun cacheOfficialMessage(payload: JSONObject, officialId: String) {
        if (payload.optString("senderId") != officialId) return
        val value = payload.optString("text").ifBlank {
            if (payload.optString("type") == "image") "📷 Image" else ""
        }
        if (value.isBlank()) return
        val timestamp = payload.optLong("timestamp", System.currentTimeMillis())
        val id = payload.optString("id", payload.optString("messageId", ""))
        val cached = runCatching { org.json.JSONArray(prefs.getString(cacheKey, "[]") ?: "[]") }.getOrDefault(org.json.JSONArray())
        val duplicate = (0 until cached.length()).any { index ->
            val entry = cached.optJSONObject(index) ?: return@any false
            val oldId = entry.optString("id", "")
            if (id.isNotBlank() && oldId.isNotBlank()) id == oldId
            else entry.optLong("timestamp") == timestamp && entry.optString("text") == value
        }
        if (!duplicate) {
            cached.put(JSONObject().put("id", id).put("timestamp", timestamp).put("text", value))
            prefs.edit().putString(cacheKey, cached.toString())
                .putLong("${cacheKey}_last_timestamp", maxOf(prefs.getLong("${cacheKey}_last_timestamp", 0L), timestamp))
                .apply()
            messages.add(false to value)
        }
    }

    DisposableEffect(receiverId, currentUserId, name) {
        if (isOfficial || receiverId.isBlank() || currentUserId.isBlank()) return@DisposableEffect onDispose { }
        val opts = IO.Options().apply {
            transports = arrayOf("websocket")
            reconnection = true
            reconnectionAttempts = Int.MAX_VALUE
            reconnectionDelay = 1000
            reconnectionDelayMax = 5000
        }
        val chatSocket = IO.socket("https://zyora-server.onrender.com", opts)
        directSocket = chatSocket
        val mainHandler = Handler(Looper.getMainLooper())
        val peerPresenceListener = Emitter.Listener { args ->
            val payload = args.firstOrNull() as? JSONObject ?: return@Listener
            if (payload.optString("userId") == receiverId || payload.optString("accountId") == receiverId) {
                mainHandler.post { peerOnline = payload.optBoolean("online", false) }
            }
        }
        val peerOnlineListener = Emitter.Listener { args ->
            if (args.firstOrNull()?.toString() == receiverId) mainHandler.post { peerOnline = true }
        }
        val peerOfflineListener = Emitter.Listener { args ->
            if (args.firstOrNull()?.toString() == receiverId) mainHandler.post { peerOnline = false }
        }
        val roomStatusListener = Emitter.Listener { args ->
            val payload = args.firstOrNull() as? JSONObject ?: return@Listener
            val statusAccountId = payload.optString("accountId").trim()
            val statusUserId = payload.optString("userId").trim()
            if (receiverId == statusAccountId || receiverId == statusUserId) {
                val confirmedRoomId = payload.optString("roomId").trim()
                    .takeIf { payload.optBoolean("inRoom", false) && it.isNotBlank() }
                    .orEmpty()
                mainHandler.post { peerRoomId = confirmedRoomId }
            }
        }
        chatSocket.on("presence_status", peerPresenceListener)
        chatSocket.on("user_online", peerOnlineListener)
        chatSocket.on("user_offline", peerOfflineListener)
        chatSocket.on("user_room_status", roomStatusListener)
        chatSocket.on("user_room_status_updated", roomStatusListener)
        chatSocket.emit("check_presence", receiverId)
        chatSocket.emit("user_room_status_request", JSONObject().put("userId", receiverId).put("accountId", receiverId))
        val incoming = Emitter.Listener { args ->
            val payload = args.firstOrNull() as? JSONObject ?: return@Listener
            val sender = payload.optString("senderId")
            val senderAccountId = payload.optString("senderAccountId").ifBlank { sender }
            val receiver = payload.optString("receiverId")
            val receiverAccount = payload.optString("receiverAccountId")
            val addressedToMe = receiver == currentAccountId || receiver == currentUserId ||
                receiverAccount == currentAccountId || receiverAccount == currentUserId
            val fromPeer = senderAccountId == receiverId || sender == receiverId
            if (fromPeer && addressedToMe) {
                val isVoice = payload.optString("type") == "voice"
                val value = if (isVoice) {
                    VoiceMessageStore.saveIncoming(context, payload) ?: ("VOICE::" + payload.optLong("durationSec", 1))
                } else when (payload.optString("type")) {
                    "image" -> "IMAGE::" + payload.optString("imageUrl")
                    "room_invitation" -> "ROOM_INVITE::" + (payload.optJSONObject("roomData")?.toString() ?: "{}")
                    else -> payload.optString("text")
                }
                if (value.isNotBlank()) mainHandler.post {
                    val mine = sender == currentUserId || senderAccountId == currentAccountId
                    val messageId = payload.optString("id")
                    if (messageId.isNotBlank() && messageIds.contains(messageId)) return@post
                    messages.add(mine to value)
                    messageIds.add(messageId)
                    val peerId = if (mine) receiverId else senderAccountId
                    val peerName = if (mine) name else payload.optString("senderName", name)
                    val peerPhoto = if (mine) image else payload.optString("senderPhoto", image)
                    val preview = if (isVoice) "🎤 Voice message" else value
                    saveConversationPreview(peerId, peerName, peerPhoto, preview, payload.optLong("timestamp", System.currentTimeMillis()))
                    
                    // Since we are inside the chat, keep unread zero
                    val unreadPrefs = context.getSharedPreferences("zyora_unread_messages", Context.MODE_PRIVATE)
                    unreadPrefs.edit().putInt("unread_${currentAccountId}_${peerId}", 0).apply()
                    refreshTotalUnreadCount(context, currentAccountId)
                }
            }
        }
        val history = Emitter.Listener { args ->
            val data = args.firstOrNull() as? JSONObject ?: return@Listener
            val arr = data.optJSONArray("messages") ?: return@Listener
            val loaded = mutableListOf<Pair<Boolean, String>>()
            val loadedIds = mutableListOf<String>()
            for (index in 0 until arr.length()) {
                val msg = arr.optJSONObject(index) ?: continue
                val isVoice = msg.optString("type") == "voice"
                val value = if (isVoice) {
                    VoiceMessageStore.saveIncoming(context, msg)
                        ?: ("VOICE::" + msg.optLong("durationSec", 1))
                } else when (msg.optString("type")) {
                    "image" -> "IMAGE::" + msg.optString("imageUrl")
                    "room_invitation" -> "ROOM_INVITE::" + (msg.optJSONObject("roomData")?.toString() ?: "{}")
                    else -> msg.optString("text")
                }
                if (value.isNotBlank()) {
                    loaded.add((msg.optString("senderId") == currentUserId || msg.optString("senderAccountId") == currentAccountId) to value)
                    loadedIds.add(msg.optString("id"))
                }
            }
            if (loaded.isNotEmpty()) {
                mainHandler.post {
                    messages.clear()
                    messages.addAll(loaded)
                    messageIds.clear()
                    messageIds.addAll(loadedIds)
                }
            }
        }
        val registrationReady = Emitter.Listener {
            mainHandler.post { chatRegistered = true }
            chatSocket.emit("private_message_history_request", JSONObject()
                .put("userId", currentUserId)
                .put("accountId", currentAccountId)
                .put("otherUserId", receiverId)
                .put("otherAccountId", receiverId)
                .put("chatId", listOf(currentAccountId, receiverId).sorted().joinToString("_")))
        }
        chatSocket.on("private_message", incoming)
        chatSocket.on("private_message_history", history)
        chatSocket.on("registration_ready", registrationReady)
        chatSocket.on(Socket.EVENT_CONNECT) {
            mainHandler.post { chatRegistered = false }
            chatSocket.emit("register", JSONObject().put("userId", currentUserId).put("accountId", currentAccountId))
            chatSocket.emit("check_presence", receiverId)
            chatSocket.emit("user_room_status_request", JSONObject().put("userId", receiverId).put("accountId", receiverId))
        }
        chatSocket.connect()
        onDispose {
            chatSocket.off("presence_status", peerPresenceListener)
            chatSocket.off("user_online", peerOnlineListener)
            chatSocket.off("user_offline", peerOfflineListener)
            chatSocket.off("user_room_status", roomStatusListener)
            chatSocket.off("user_room_status_updated", roomStatusListener)
            chatSocket.off("private_message", incoming)
            chatSocket.off("private_message_history", history)
            chatSocket.off("registration_ready", registrationReady)
            chatRegistered = false
            directSocket = null
            chatSocket.disconnect()
            chatSocket.close()
        }
    }

    fun configureVoiceAudioRoute() {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val bluetoothDevice = audioManager.availableCommunicationDevices.firstOrNull {
                    it.type == android.media.AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                        it.type == android.media.AudioDeviceInfo.TYPE_BLE_HEADSET
                }
                if (bluetoothDevice != null) audioManager.setCommunicationDevice(bluetoothDevice)
            } else {
                @Suppress("DEPRECATION")
                if (audioManager.isBluetoothScoAvailableOffCall) {
                    audioManager.startBluetoothSco()
                    audioManager.isBluetoothScoOn = true
                }
            }
        }
    }

    fun resetVoiceAudioRoute() {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) audioManager.clearCommunicationDevice()
            else {
                @Suppress("DEPRECATION")
                audioManager.stopBluetoothSco()
            }
            audioManager.mode = AudioManager.MODE_NORMAL
        }
    }

    @Suppress("DEPRECATION")
    fun createVoiceRecorder(): MediaRecorder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()

    fun startVoiceRecording(): Boolean {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) return false
        var createdRecorder: MediaRecorder? = null
        return runCatching {
            val directory = File(context.filesDir, "voice_notes").apply { mkdirs() }
            val file = File(directory, "voice_${System.currentTimeMillis()}.m4a")
            configureVoiceAudioRoute()
            val recorder = createVoiceRecorder()
            createdRecorder = recorder
            recorder.setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setAudioEncodingBitRate(32000)
            recorder.setAudioSamplingRate(22050)
            recorder.setMaxDuration(90000)
            recorder.setMaxFileSize(700000)
            recorder.setOutputFile(file.absolutePath)
            recorder.prepare()
            recorder.start()
            mediaRecorder = recorder
            recordedVoiceFile = file
            recordStartTime = System.currentTimeMillis()
            isRecording = true
            true
        }.getOrElse {
            runCatching { createdRecorder?.release() }
            mediaRecorder = null
            isRecording = false
            resetVoiceAudioRoute()
            false
        }
    }

    fun finishVoiceRecording(): Pair<File, Long>? {
        val recorder = mediaRecorder ?: return null
        val file = recordedVoiceFile
        val duration = ((System.currentTimeMillis() - recordStartTime) / 1000L).coerceAtLeast(1L)
        val saved = runCatching { recorder.stop(); true }.getOrDefault(false)
        runCatching { recorder.reset(); recorder.release() }
        mediaRecorder = null
        recordedVoiceFile = null
        isRecording = false
        resetVoiceAudioRoute()
        return if (saved && file != null && file.exists() && file.length() > 512L) file to duration else {
            file?.delete()
            null
        }
    }

    fun playVoiceNote(path: String) {
        runCatching {
            voicePlayer?.stop()
            voicePlayer?.release()
            voicePlayer = null
            // Voice-note playback uses the normal media stream/speaker volume, never call volume.
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.mode = AudioManager.MODE_NORMAL
            val player = MediaPlayer()
            voicePlayer = player
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            player.setDataSource(path)
            player.setOnCompletionListener {
                it.release()
                if (voicePlayer === it) voicePlayer = null
                resetVoiceAudioRoute()
            }
            player.prepare()
            player.start()
        }.onFailure {
            runCatching { voicePlayer?.release() }
            voicePlayer = null
            resetVoiceAudioRoute()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { mediaRecorder?.stop() }
            runCatching { mediaRecorder?.release() }
            runCatching { voicePlayer?.release() }
            resetVoiceAudioRoute()
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) messageListState.animateScrollToItem(messages.lastIndex)
    }

    DisposableEffect(name) {
        val officialId = when (name) {
            "Zyora Team" -> "hurry_team_official"
            "Zyora System" -> "hurry_system_official"
            else -> null
        }
        if (officialId == null) return@DisposableEffect onDispose { }

        val mainHandler = Handler(Looper.getMainLooper())
        val socketOptions = IO.Options()
        socketOptions.transports = arrayOf("websocket")
        socketOptions.reconnection = true
        socketOptions.reconnectionAttempts = Int.MAX_VALUE
        socketOptions.reconnectionDelay = 1000
        socketOptions.reconnectionDelayMax = 5000
        val officialSocket: Socket = IO.socket(
            "https://zyora-server.onrender.com",
            socketOptions
        )

        val appendOfficial: (JSONObject) -> Unit = { payload ->
            mainHandler.post { cacheOfficialMessage(payload, officialId) }
        }

        val liveListener = Emitter.Listener { args ->
            (args.firstOrNull() as? JSONObject)?.let(appendOfficial)
        }
        val historyListener = Emitter.Listener { args ->
            val payload = args.firstOrNull() as? JSONObject ?: return@Listener
            val history = payload.optJSONArray("messages") ?: return@Listener
            val items = mutableListOf<JSONObject>()
            for (index in 0 until history.length()) {
                val message = history.optJSONObject(index) ?: continue
                if (message.optString("senderId") == officialId) items.add(message)
            }
            mainHandler.post {
                items.sortedBy { it.optLong("timestamp") }.forEach { cacheOfficialMessage(it, officialId) }
            }
        }

        officialSocket.on("official_broadcast_message", liveListener)
        officialSocket.on("official_message_history_response", historyListener)
        officialSocket.on(Socket.EVENT_CONNECT) {
            // Targeted Owner Panel messages are emitted to user:<Firebase UID> and
            // user:<Zyora account ID> rooms. This dedicated official-chat socket
            // must register both identities too; otherwise it connects successfully
            // but can never receive a private official message addressed to this user.
            officialSocket.emit(
                "register",
                JSONObject()
                    .put("userId", currentUserId)
                    .put("accountId", currentAccountId)
            )
            val lastTimestamp = prefs.getLong("${cacheKey}_last_timestamp", 0L)
            officialSocket.emit(
                "official_message_history_request",
                JSONObject().put("sinceTimestamp", lastTimestamp)
            )
        }
        officialSocket.connect()

        onDispose {
            officialSocket.off("official_broadcast_message", liveListener)
            officialSocket.off("official_message_history_response", historyListener)
            officialSocket.disconnect()
            officialSocket.close()
        }
    }

    Box(Modifier.fillMaxSize().background(
        Brush.verticalGradient(
            0.0f to Color(0xFF3B82F6),
            0.03f to Color(0xFF3B82F6), 
            0.08f to Color(0xFFF0F2F5), 
            1.0f to Color(0xFFF0F2F5)
        )
    )) {
        if (isOfficial) {
            AsyncImage(
                model = RAW_MSG + "file_00000000777481f588df50d28908ce63.png",
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .align(Alignment.BottomCenter)
                    .alpha(0.05f),
                contentScale = ContentScale.FillWidth
            )
        }

        Column(Modifier.fillMaxSize()) {
            
            Box(
                Modifier.fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 8.dp, bottom = 4.dp) 
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 4.dp).size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.Black, 
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally 
                ) {
                    Text(
                        name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black, 
                        maxLines = 1,
                        modifier = Modifier.clickable { onOpenPublicProfile() }
                    )
                    if (!isOfficial) {
                        Text(
                            if (peerOnline) "Online" else "Offline", 
                            fontSize = 11.sp, 
                            color = if (peerOnline) Color(0xFF16A34A) else Color.DarkGray, 
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                if (!isOfficial) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 12.dp) 
                    ) {
                        Text(
                            "⋮",
                            fontSize = 28.sp,
                            color = Color.Black, 
                            modifier = Modifier
                                .clickable { showOptions = true }
                                .padding(horizontal = 8.dp)
                        )
                        

                    }
                }
            }

            if (!isOfficial) {
                if (showScamBanner) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFDBEAFE)) 
                            // Padding reduced for thinner height
                            .padding(horizontal = 16.dp, vertical = 1.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info, 
                            contentDescription = "Warning",
                            tint = Color(0xFF1D4ED8), 
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Protect your info. Beware of scams.",
                            color = Color(0xFF1D4ED8), 
                            fontSize = 11.sp, // Text chota kar diya
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF1D4ED8),
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { showScamBanner = false }
                        )
                    }
                }

                if (!isOfficial && peerRoomId.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 11.dp, vertical = 2.dp) // Outer padding thori kam kar di
                        .clip(RoundedCornerShape(25.dp))
                        .background(Color(0xFF3B82F6))
                        .clickable { onJoinRoom(peerRoomId) }
                        .padding(horizontal = 12.dp, vertical = 4.dp), // Banner height decreased
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Added user avatar in left corner without border
                    AsyncImage(
                        model = image, // Peer (samne wale) user ka image
                        contentDescription = name,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "I'm chatting and playing games in voice room, join me~",
                        color = Color.White,
                        fontSize = 12.sp, // Text chota kar diya
                        fontWeight = FontWeight.Medium,
                        lineHeight = 16.sp, // Line height adjust kar diya
                        maxLines = 2, // Sirf 2 line tak allow karega
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, // Agar text lamba hua toh 3rd line ki jagah ... dikha dega
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .clickable { onJoinRoom(peerRoomId) }
                            .padding(horizontal = 10.dp, vertical = 4.dp), // Pill height decreased
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.padding(end = 4.dp),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Box(Modifier.width(2.dp).height(bar1Height.dp).background(Color(0xFF3B82F6), CircleShape))
                            Box(Modifier.width(2.dp).height(bar2Height.dp).background(Color(0xFF3B82F6), CircleShape))
                            Box(Modifier.width(2.dp).height(bar3Height.dp).background(Color(0xFF3B82F6), CircleShape))
                        }
                        Text(
                            text = "In Room",
                            color = Color(0xFF3B82F6),
                            fontSize = 11.sp, // Thora chota kar diya taaki proportional lage
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                }
            }

            LazyColumn(
                state = messageListState,
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp),
                reverseLayout = false,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(top = 14.dp, bottom = 12.dp)
            ) {
                items(messages) { item ->
                    val mine = item.first
                    
                    val isVoiceNote = VoiceMessageStore.isVoice(item.second)
                    val isImageMessage = item.second.startsWith("IMAGE::")
                    val isRoomInvitation = item.second.startsWith("ROOM_INVITE::")
                    val imageMessageUrl = if (isImageMessage) item.second.removePrefix("IMAGE::") else ""
                    val roomInvitationData = if (isRoomInvitation) runCatching { JSONObject(item.second.removePrefix("ROOM_INVITE::")) }.getOrNull() else null
                    val messageText = if (isVoiceNote) VoiceMessageStore.displayDuration(item.second) else item.second

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        if (!mine) {
                            AsyncImage(
                                model = image,
                                contentDescription = name,
                                modifier = Modifier.size(28.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(Modifier.width(8.dp))
                        }

                        Column(horizontalAlignment = if (mine) Alignment.End else Alignment.Start) {
                            Surface(
                                modifier = if (!isImageMessage && !isRoomInvitation) Modifier.twoSecondLongPress {
                                    clipboard.setText(AnnotatedString(messageText))
                                    copyPopupText = "Copy to clipboard"
                                } else Modifier,
                                color = if (mine) Color(0xFF374151) else Color.White,
                                shape = if (mine)
                                    RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp)
                                else
                                    RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp),
                                shadowElevation = 0.dp
                            ) {
                                if (isVoiceNote) {
                                    Row(
                                        modifier = Modifier
                                            .widthIn(min = 100.dp, max = 220.dp)
                                            .clickable { VoiceMessageStore.playableFile(context, item.second)?.absolutePath?.let(::playVoiceNote) }
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = rememberVoiceWaveIcon(),
                                            contentDescription = "Play Voice Note",
                                            tint = if (mine) Color.White else Color(0xFF1F2937),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        
                                        Row(
                                            Modifier.weight(1f),
                                            horizontalArrangement = Arrangement.SpaceEvenly,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            listOf(0.4f, 0.8f, 1f, 0.6f, 0.4f).forEach { heightFraction ->
                                                Box(
                                                    modifier = Modifier
                                                        .width(2.5.dp)
                                                        .height(14.dp * heightFraction)
                                                        .clip(CircleShape)
                                                        .background(if (mine) Color.White.copy(alpha = 0.7f) else Color.Gray.copy(alpha = 0.7f))
                                                )
                                            }
                                        }
                                        
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = "$messageText\"",
                                            color = if (mine) Color.White else Color(0xFF1F2937),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                } else if (isImageMessage) {
                                    AsyncImage(
                                        model = imageMessageUrl,
                                        contentDescription = "Shared image",
                                        modifier = Modifier.widthIn(max = 230.dp).heightIn(max = 230.dp)
                                            .clip(RoundedCornerShape(12.dp)).clickable { previewImageUrl = imageMessageUrl },
                                        contentScale = ContentScale.Crop
                                    )
                                } else if (isRoomInvitation && roomInvitationData != null) {
                                    val sharedRoomId = roomInvitationData.optString("roomId")
                                    Column(
                                        Modifier.width(238.dp).clip(RoundedCornerShape(12.dp)).background(Color.White)
                                            .clickable { if (sharedRoomId.isNotBlank()) onJoinRoom(sharedRoomId) }
                                            .padding(12.dp)
                                    ) {
                                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                            Text("Invitation", Modifier.weight(1f), color = Color(0xFF111827), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                            Text("›", color = Color(0xFF111827), fontSize = 24.sp)
                                        }
                                        Spacer(Modifier.height(9.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            AsyncImage(
                                                model = roomInvitationData.optString("roomDp"),
                                                contentDescription = roomInvitationData.optString("roomName"),
                                                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(7.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                            Column(Modifier.padding(start = 9.dp).weight(1f)) {
                                                Text(roomInvitationData.optString("roomName").ifBlank { "Room" },
                                                    color = Color(0xFF111827), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                                Spacer(Modifier.height(3.dp))
                                                if (sharedRoomId in setOf("100002","100003","500002","500003","500004")) {
                                                    AsyncImage(model = "https://zyora-voice-chat.vercel.app/IMG_20261101_125212.png",
                                                        contentDescription = "Account ID", modifier = Modifier.height(16.dp).widthIn(min = 22.dp, max = 30.dp), contentScale = ContentScale.Fit)
                                                } else {
                                                    Text("ID: " + sharedRoomId, color = Color(0xFF6B7280), fontSize = 10.sp)
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Column(Modifier.widthIn(max = 290.dp).padding(horizontal = 13.dp, vertical = 9.dp)) {
                                        if (replyText != null && mine) {
                                            Text(replyText!!, color = Color(0xFFCBD5E1), fontSize = 10.sp, maxLines = 1)
                                            Spacer(Modifier.height(3.dp))
                                        }
                                        Text(messageText, color = if (mine) Color.White else Color(0xFF1F2937), fontSize = 14.sp)
                                    }
                                }
                            }
                        }

                        if (mine) {
                            Spacer(Modifier.width(8.dp))
                            if (currentPhoto.isNotBlank()) {
                                AsyncImage(
                                    model = currentPhoto,
                                    contentDescription = currentName,
                                    modifier = Modifier.size(28.dp).clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    Modifier.size(28.dp).clip(CircleShape).background(Color(0xFF9CA3AF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(currentName.take(1).uppercase(), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Normal)
                                }
                            }
                        }
                    }
                }
            }

            if (!isOfficial) {
                Column(
                    Modifier.fillMaxWidth()
                        .background(Color.White)
                        .navigationBarsPadding()
                        .imePadding()
                ) {
                    if (replyText != null) {
                        Row(
                            Modifier.fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(Color(0xFFF3F4F6))
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.width(3.dp).height(28.dp).background(Color(0xFF3B82F6)))
                            Spacer(Modifier.width(8.dp))
                            Text(replyText!!, Modifier.weight(1f), fontSize = 11.sp, color = Color(0xFF6B7280), maxLines = 1)
                            Text("×", fontSize = 20.sp, color = Color(0xFF6B7280), modifier = Modifier.clickable { replyText = null })
                        }
                    }

                    Row(
                        Modifier.fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        
                        if (isVoiceMode) {
                            IconButton(onClick = { isVoiceMode = false }, modifier = Modifier.size(28.dp)) {
                                Icon(rememberKeyboardIcon(), contentDescription = "Keyboard", tint = Color(0xFF6B7280), modifier = Modifier.size(26.dp))
                            }
                        } else {
                            IconButton(onClick = { isVoiceMode = true }, modifier = Modifier.size(28.dp)) {
                                Icon(rememberVoiceWaveIcon(), contentDescription = "Voice", tint = Color(0xFF6B7280), modifier = Modifier.size(26.dp))
                            }
                        }
                        
                        Spacer(Modifier.width(10.dp))
                        
                        IconButton(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            enabled = !isOfficial,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Outlined.Image, contentDescription = "Image", tint = Color(0xFF6B7280), modifier = Modifier.size(26.dp))
                        }
                        Spacer(Modifier.width(10.dp))

                        if (isVoiceMode) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(if (isRecording) Color(0xFFD1D5DB) else Color(0xFFF3F4F6))
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onPress = {
                                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                                                    audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                                    return@detectTapGestures
                                                }
                                                if (!startVoiceRecording()) return@detectTapGestures
                                                val released = tryAwaitRelease()
                                                val recorded = finishVoiceRecording()
                                                if (released && recorded != null) {
                                                    val activeSocket = directSocket
                                                    if (!chatRegistered || activeSocket == null || !activeSocket.connected()) {
                                                        activeSocket?.connect()
                                                        recorded.first.delete()
                                                        return@detectTapGestures
                                                    }
                                                    val (audioFile, durationSec) = recorded
                                                    val messageId = "${currentUserId}_${System.currentTimeMillis()}"
                                                    val localVoice = VoiceMessageStore.localMarker(messageId, durationSec)
                                                    val audioBase64 = VoiceMessageStore.persistOutgoing(context, messageId, receiverId, audioFile, durationSec)
                                                    messages.add(true to localVoice)
                                                    messageIds.add(messageId)
                                                    saveConversationPreview(receiverId, name, image, "🎤 Voice message", System.currentTimeMillis())
                                                    val outgoing = JSONObject()
                                                        .put("id", messageId)
                                                        .put("senderId", currentUserId)
                                                        .put("senderAccountId", currentAccountId)
                                                        .put("receiverId", receiverId)
                                                    .put("receiverAccountId", receiverId)
                                                        .put("senderName", currentName)
                                                        .put("senderPhoto", currentPhoto)
                                                        .put("receiverName", name)
                                                        .put("receiverPhoto", image)
                                                        .put("text", "VOICE::$durationSec")
                                                        .put("type", "voice")
                                                        .put("audioMimeType", "audio/mp4")
                                                        .put("audioBase64", audioBase64)
                                                        .put("durationSec", durationSec)
                                                        .put("timestamp", System.currentTimeMillis())
                                                    if (!isOfficial && receiverId.isNotBlank() && currentUserId.isNotBlank()) {
                                                        directSocket?.emit("private_message", outgoing)
                                                    }
                                                } else {
                                                    recorded?.first?.delete()
                                                }
                                            }
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isRecording) "Release to Send" else "Hold to Talk",
                                    color = Color(0xFF1F2937),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                        } else {
                            Box(Modifier.weight(1f)) {
                                Row(
                                    Modifier.fillMaxWidth()
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(Color(0xFFF3F4F6))
                                        .padding(start = 16.dp, end = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    androidx.compose.foundation.text.BasicTextField(
                                        value = text,
                                        onValueChange = { text = it },
                                        singleLine = true,
                                        textStyle = androidx.compose.ui.text.TextStyle(
                                            color = Color.Black,
                                            fontSize = 15.sp
                                        ),
                                        modifier = Modifier.weight(1f).fillMaxHeight(),
                                        decorationBox = { inner ->
                                            Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.fillMaxSize()) {
                                                if (text.isEmpty()) Text("Type a message...", color = Color(0xFF8A8F98), fontSize = 15.sp)
                                                inner()
                                            }
                                        }
                                    )
                                    
                                    val isTextBlank = text.isBlank()
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(if (isTextBlank) Color(0xFF9CA3AF) else Color(0xFF3B82F6))
                                            .clickable(enabled = !isTextBlank) {
                                                if (blockStateReady && (isUserBlocked || blockedByPeer)) {
                                                    blockPopupText = if (isUserBlocked) "You blocked this person" else "You are blocked"
                                                    return@clickable
                                                }
                                                if (!blockStateReady) return@clickable
                                                val activeSocket = directSocket
                                                if (!chatRegistered || activeSocket == null || !activeSocket.connected()) {
                                                    activeSocket?.connect()
                                                    return@clickable
                                                }
                                                val outgoingText = text.trim()
                                                if (containsProhibitedChatText(outgoingText)) {
                                                    blockPopupText = "Can't send text-prohibited"
                                                    return@clickable
                                                }
                                                val outgoingId = "${currentUserId}_${System.currentTimeMillis()}"
                                                val outgoing = JSONObject()
                                                    .put("id", outgoingId)
                                                    .put("senderId", currentUserId)
                                                    .put("senderAccountId", currentAccountId)
                                                    .put("receiverId", receiverId)
                                                    .put("receiverAccountId", receiverId)
                                                    .put("senderName", currentName)
                                                    .put("senderPhoto", currentPhoto)
                                                    .put("receiverName", name)
                                                    .put("receiverPhoto", image)
                                                    .put("text", outgoingText)
                                                    .put("type", "message")
                                                    .put("timestamp", System.currentTimeMillis())
                                                if (!isOfficial && receiverId.isNotBlank() && currentUserId.isNotBlank()) {
                                                    directSocket?.emit("private_message", outgoing)
                                                }
                                                text = ""
                                                replyText = null
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Send,
                                            contentDescription = "Send",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp).padding(start = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    DisposableEffect(directSocket, receiverId) {
        val socket = directSocket
        val sentListener = Emitter.Listener { args ->
            val payload = args.firstOrNull() as? JSONObject ?: return@Listener
            val id = payload.optString("id")
            val peerId = payload.optString("receiverAccountId").ifBlank { payload.optString("receiverId") }
            if (peerId != receiverId) return@Listener
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                if (id.isNotBlank() && messageIds.contains(id)) return@post
                val value = when (payload.optString("type")) {
                    "image" -> "IMAGE::" + payload.optString("imageUrl")
                    "room_invitation" -> "ROOM_INVITE::" + (payload.optJSONObject("roomData")?.toString() ?: "{}")
                    "voice" -> "VOICE::" + payload.optLong("durationSec", 1)
                    else -> payload.optString("text")
                }
                if (value.isNotBlank()) {
                    messages.add(true to value)
                    messageIds.add(id)
                    saveConversationPreview(receiverId, name, image, payload.optString("previewText").ifBlank { value }, payload.optLong("timestamp", System.currentTimeMillis()))
                }
            }
        }
        val blockedListener = Emitter.Listener { args ->
            val payload = args.firstOrNull() as? JSONObject ?: return@Listener
            val code = payload.optString("code")
            if (code == "BLOCKED_BY_ME" || code == "YOU_ARE_BLOCKED") {
                val messageId = payload.optString("id")
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    if (messageId.isNotBlank()) {
                        val index = messageIds.indexOf(messageId)
                        if (index >= 0) {
                            if (index < messages.size) messages.removeAt(index)
                            messageIds.removeAt(index)
                        }
                    }
                    blockPopupText = if (code == "BLOCKED_BY_ME") "You blocked this person" else "You are blocked"
                }
            }
        }
        socket?.on("private_message_sent", sentListener)
        socket?.on("private_message_error", blockedListener)
        onDispose {
            socket?.off("private_message_sent", sentListener)
            socket?.off("private_message_error", blockedListener)
        }
    }
    if (copyPopupText.isNotBlank()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(copyPopupText, color = Color.White, fontSize = 12.sp,
                modifier = Modifier.clip(RoundedCornerShape(7.dp)).background(Color.Black.copy(alpha = 0.70f))
                    .padding(horizontal = 14.dp, vertical = 8.dp))
        }
    }
    if (blockPopupText.isNotBlank()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(blockPopupText, color = Color.White, fontSize = 12.sp,
                modifier = Modifier.clip(RoundedCornerShape(7.dp)).background(Color.Black.copy(alpha = 0.70f))
                    .padding(horizontal = 14.dp, vertical = 8.dp))
        }
    }
    UserDangerActionMenu(
        expanded = showOptions,
        isBlocked = isUserBlocked,
        onDismiss = { showOptions = false },
        onBlockedChange = { shouldBlock ->
            isUserBlocked = shouldBlock
            kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                val saved = updateZyoraUserBlock(context, currentAccountId, receiverId, shouldBlock)
                if (!saved) android.os.Handler(android.os.Looper.getMainLooper()).post { isUserBlocked = !shouldBlock }
            }
        },
        onReport = { showReportPage = true }
    )

    if (showReportPage) {
        UserReport(
            targetName = name,
            targetImage = image,
            targetId = receiverId,
            onDismiss = { showReportPage = false }
        )
    }
    if (!previewImageUrl.isNullOrBlank()) {
        Dialog(onDismissRequest = { previewImageUrl = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Box(Modifier.fillMaxSize().background(Color.Black)) {
                AsyncImage(model = previewImageUrl, contentDescription = "Image preview",
                    modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                IconButton(onClick = { previewImageUrl = null },
                    modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(start = 8.dp, top = 6.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close image", tint = Color.White)
                }
            }
        }
    }
}

package com.hurry.voicechat

import android.content.Context
import android.os.Handler
import android.os.Looper
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import org.json.JSONObject
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SentimentSatisfiedAlt
import androidx.compose.runtime.*
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage

private const val RAW_MSG = "https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/"

private data class MessagePreview(val uid: String, val name: String, val image: String, val message: String, val time: String = "")

// Yeh function tumhare SVG ko Compose Icon mein convert karne ke liye hai
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

@Composable
fun MessageScreen(onChat: (String, String, String) -> Unit, embeddedSheet: Boolean = false, onBack: () -> Unit = {}) {
    val chats = remember {
        listOf(
            MessagePreview("hurry_team_official", "Zyora Team", RAW_MSG + "logo.png", ""),
            MessagePreview("hurry_system_official", "Zyora System", RAW_MSG + "file_00000000a66881f8aa9e15d2fe2b9a0c.png", "")
        )
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
                        0.3f to Color(0xFF3B82F6), // Blue color thora aur increase kiya yahan
                        0.75f to Color(0xFFF1F1F1),
                        1.0f to Color(0xFFF1F1F1)
                    )
                ).statusBarsPadding().padding(top = 12.dp, bottom = 12.dp)
            ) {
                // Heading ko ekdam Middle mein set kiya
                Text(
                    "Message",
                    Modifier.align(Alignment.Center),
                    color = Color.Black, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold
                )
                
                // Right side corner mein custom SVG icon add kiya
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
            items(chats) { chat ->
                MessageRow(chat) { onChat(chat.uid, chat.name, chat.image) }
            }
        }
    }
}

@Composable
private fun MessageRow(chat: MessagePreview, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = chat.image, contentDescription = chat.name,
            modifier = Modifier.size(56.dp).clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(chat.name, color = Color(0xFF1F2937), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            if (chat.message.isNotEmpty()) {
                Spacer(Modifier.height(3.dp))
                Text(chat.message, color = HurryMuted, fontSize = 14.sp, maxLines = 1)
            }
        }
        if (chat.time.isNotEmpty()) Text(chat.time, color = Color(0xFF999999), fontSize = 12.sp)
    }
}

@Composable
fun NativeChatScreen(
    name: String,
    image: String,
    receiverId: String = "",
    currentUserId: String = "",
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("hurry_user", Context.MODE_PRIVATE) }
    val currentPhoto = prefs.getString("photo", prefs.getString("image", "")) ?: ""
    val currentName = prefs.getString("name", "")?.takeIf { it.isNotBlank() } ?: "Zyora User"
    val currentAccountId = prefs.getString("accountId", currentUserId).orEmpty().ifBlank { currentUserId }
    val isOfficial = name == "Zyora Team" || name == "Zyora System"
    var directSocket by remember(receiverId, currentUserId) { mutableStateOf<Socket?>(null) }

    var text by remember { mutableStateOf("") }
    var showOptions by remember { mutableStateOf(false) }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var replyText by remember { mutableStateOf<String?>(null) }
    val officialIdForCache = when (name) {
        "Zyora Team" -> "hurry_team_official"
        "Zyora System" -> "hurry_system_official"
        else -> null
    }
    val cacheKey = "official_chat_cache_${officialIdForCache ?: name}"
    val indexDb = remember { NativeIndexDatabase(context) }
    val localConversationKey = "chat_${currentUserId}_${receiverId.ifBlank { officialIdForCache ?: name }}"
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
    LaunchedEffect(messages.size, localConversationKey) {
        val array = org.json.JSONArray()
        messages.forEach { (mine, value) ->
            array.put(JSONObject().put("mine", mine).put("text", value))
        }
        indexDb.saveChat(localConversationKey, array.toString())
    }

    // Persist each official message locally; cached history renders immediately on reopen.
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

    // Real-time one-to-one chat: register identity, load conversation history, and receive new messages.
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
        val incoming = Emitter.Listener { args ->
            val payload = args.firstOrNull() as? JSONObject ?: return@Listener
            val sender = payload.optString("senderId")
            val receiver = payload.optString("receiverId")
            if ((sender == receiverId && receiver == currentUserId) ||
                (sender == currentUserId && receiver == receiverId)) {
                val value = payload.optString("text")
                if (value.isNotBlank()) mainHandler.post {
                    val mine = sender == currentUserId
                    messages.add(mine to value)
                }
            }
        }
        val history = Emitter.Listener { args ->
            val data = args.firstOrNull() as? JSONObject ?: return@Listener
            val arr = data.optJSONArray("messages") ?: return@Listener
            val loaded = mutableListOf<Pair<Boolean, String>>()
            for (index in 0 until arr.length()) {
                val msg = arr.optJSONObject(index) ?: continue
                val value = msg.optString("text")
                if (value.isNotBlank()) loaded.add((msg.optString("senderId") == currentUserId) to value)
            }
            if (loaded.isNotEmpty()) {
                mainHandler.post {
                    messages.clear()
                    messages.addAll(loaded)
                }
            }
        }
        chatSocket.on("private_message", incoming)
        chatSocket.on("private_message_history", history)
        chatSocket.on(Socket.EVENT_CONNECT) {
            chatSocket.emit("register", JSONObject().put("userId", currentUserId).put("accountId", currentAccountId))
            chatSocket.emit("private_message_history_request", JSONObject()
                .put("userId", currentUserId)
                .put("otherUserId", receiverId)
                .put("chatId", listOf(currentUserId, receiverId).sorted().joinToString("_")))
        }
        chatSocket.connect()
        onDispose {
            chatSocket.off("private_message", incoming)
            chatSocket.off("private_message_history", history)
            directSocket = null
            chatSocket.disconnect()
            chatSocket.close()
        }
    }

    // Receive owner-panel Zyora Team/System broadcasts live; request only messages newer than local cache.
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

    Box(Modifier.fillMaxSize().background(Color(0xFFF0F2F5))) {
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
            // Same structure as the real Hurry chat header:
            // blue-to-chat-background gradient, 12dp safe-area top, back + avatar + title.
            Row(
                Modifier.fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color(0xFF3B82F6), Color(0xFFF0F2F5))))
                    .padding(top = (5f * LocalConfiguration.current.screenHeightDp / 100f).dp, start = 8.dp, end = 8.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF1F2937),
                        modifier = Modifier.size(24.dp)
                    )
                }
                AsyncImage(
                    model = image,
                    contentDescription = name,
                    modifier = Modifier.size(40.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                    Text(
                        name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937),
                        maxLines = 1
                    )
                    if (!isOfficial) {
                        Text("Offline", fontSize = 11.sp, color = Color(0xFF4B5563), fontWeight = FontWeight.Medium)
                    }
                }
                if (!isOfficial) {
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

            // Real-chat style message surface: no card around the whole list.
            LazyColumn(
                Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp),
                reverseLayout = false,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(top = 14.dp, bottom = 12.dp)
            ) {
                items(messages) { item ->
                    val mine = item.first
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
                                color = if (mine) Color(0xFF374151) else Color.White,
                                shape = if (mine)
                                    RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp)
                                else
                                    RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp),
                                shadowElevation = 0.dp
                            ) {
                                Column(Modifier.widthIn(max = 290.dp).padding(horizontal = 13.dp, vertical = 9.dp)) {
                                    if (replyText != null && mine) {
                                        Text(
                                            replyText!!,
                                            color = Color(0xFFCBD5E1),
                                            fontSize = 10.sp,
                                            maxLines = 1
                                        )
                                        Spacer(Modifier.height(3.dp))
                                    }
                                    Text(
                                        item.second,
                                        color = if (mine) Color.White else Color(0xFF1F2937),
                                        fontSize = 14.sp
                                    )
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

            // Real app input bar: image + emoji + rounded input + send icon.
            if (!isOfficial) {
                Column(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        .background(Color.White)
                        .navigationBarsPadding()
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
                        IconButton(
                            onClick = { },
                            enabled = false,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Image, contentDescription = "Image", tint = Color(0xFF6B7280), modifier = Modifier.size(24.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        IconButton(
                            onClick = { showEmojiPicker = !showEmojiPicker },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.SentimentSatisfiedAlt,
                                contentDescription = "Emoji",
                                tint = if (showEmojiPicker) Color(0xFF3B82F6) else Color(0xFF6B7280),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(Modifier.width(8.dp))

                        Box(Modifier.weight(1f)) {
                            Row(
                                Modifier.fillMaxWidth()
                                    .clip(CircleShape)
                                    .background(Color(0xFFF3F4F6))
                                    .padding(start = 16.dp, end = 9.dp, top = 2.dp, bottom = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                androidx.compose.foundation.text.BasicTextField(
                                    value = text,
                                    onValueChange = { text = it },
                                    singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        color = Color.Black,
                                        fontSize = 14.sp
                                    ),
                                    modifier = Modifier.weight(1f).padding(vertical = 9.dp),
                                    decorationBox = { inner ->
                                        if (text.isEmpty()) Text("Type a message...", color = Color(0xFF8A8F98), fontSize = 14.sp)
                                        inner()
                                    }
                                )
                                IconButton(
                                    onClick = {
                                        if (text.isNotBlank()) {
                                            val outgoingText = text.trim()
                                            messages.add(true to outgoingText)
                                            val outgoing = JSONObject()
                                                .put("id", "${currentUserId}_${System.currentTimeMillis()}")
                                                .put("senderId", currentUserId)
                                                .put("receiverId", receiverId)
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
                                            showEmojiPicker = false
                                            replyText = null
                                        }
                                    },
                                    enabled = text.isNotBlank(),
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Send,
                                        contentDescription = "Send",
                                        tint = if (text.isBlank()) Color(0xFFD1D5DB) else Color(0xFF3B82F6),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (showEmojiPicker) {
                        val emojis = listOf("😀","😂","🤣","🥺","😍","🥰","😘","😊","😉","😎","🤩","🥳","😒","😞","😢","😭","😡","🤬","😱","😴","🤔","🤭","❤️","🔥","✨","💯","👍","👏","🙌","💪")
                        Column(
                            Modifier.fillMaxWidth()
                                .height(230.dp)
                                .background(Color(0xFFF1F3F5))
                                .padding(8.dp)
                        ) {
                            emojis.chunked(8).forEach { row ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                    row.forEach { emoji ->
                                        Text(emoji, fontSize = 25.sp, modifier = Modifier.clickable { text += emoji }.padding(5.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showOptions) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = .45f)).clickable { showOptions = false },
                contentAlignment = Alignment.BottomCenter
            ) {
                Column(
                    Modifier.fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                        .background(Color.Black)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .clickable(enabled = false) {},
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Report", color = Color.White, fontSize = 16.sp, modifier = Modifier.padding(vertical = 12.dp).clickable { showOptions = false })
                    Text("Clear Chat", color = Color.White, fontSize = 16.sp, modifier = Modifier.padding(vertical = 12.dp).clickable {
                        messages.clear(); showOptions = false
                    })
                    Text("Delete Messages", color = Color.White, fontSize = 16.sp, modifier = Modifier.padding(vertical = 12.dp).clickable { showOptions = false })
                    Text("Block", color = Color.White, fontSize = 16.sp, modifier = Modifier.padding(vertical = 12.dp).clickable { showOptions = false })
                    Spacer(Modifier.weight(1f))
                    Box(
                        Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF3B82F6)).clickable { showOptions = false },
                        contentAlignment = Alignment.Center
                    ) { Text("Cancel", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Normal) }
                }
            }
        }
    }
}

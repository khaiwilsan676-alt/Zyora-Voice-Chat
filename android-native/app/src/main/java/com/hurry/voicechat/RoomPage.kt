package com.hurry.voicechat

import android.content.Intent
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.graphics.Brush
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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

// Optimized Shader-like CPU logic to prevent room hanging
@Composable
fun TransparentGreenImage(url: String, modifier: Modifier = Modifier) {
    var bitmap by remember(url) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(url) {
        bitmap = withContext(Dispatchers.IO) {
            runCatching {
                val source = java.net.URL(url).openStream().use { android.graphics.BitmapFactory.decodeStream(it) } ?: return@runCatching null
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

data class RoomPageOwner(val id:String="",val uid:String="",val accountId:String="",val name:String="Room",val image:String="",val country:String="🇮🇳")
data class RoomPageCurrentUser(val id:String="",val uid:String="",val accountId:String="",val name:String="User",val image:String="")
data class NativeRoomSeat(val number:Int,val occupied:Boolean=false,val locked:Boolean=false,val muted:Boolean=false,val speaking:Boolean=false,val userName:String="",val userImage:String="",val accountId:String="",val gifSrc:String="",val gifTimestamp:Long=0L)
data class NativeRoomMessage(val id:String,val text:String,val sender:String,val senderImage:String,val senderAccountId:String="",val timestamp:Long=System.currentTimeMillis(),val type:String="message")
data class NativeEntryToast(val id:String,val name:String,val image:String)

@Composable
fun RoomPage(
    roomOwner: RoomPageOwner,
    currentUser: RoomPageCurrentUser,
    onClose:()->Unit={},
    onBack:()->Unit=onClose,
    onKeepRoom:(String,String,String)->Unit={_,_,_->},
    onFollowToggle:(String,Boolean)->Unit={_,_->}
) {
    val roomId=roomOwner.id.ifBlank{roomOwner.accountId.ifBlank{roomOwner.uid.ifBlank{"default-room"}}}
    val accountId=currentUser.accountId.ifBlank{currentUser.uid.ifBlank{currentUser.id.ifBlank{"guest"}}}
    val clipboard = LocalClipboardManager.current
    val appContext = LocalContext.current
    var copiedId by remember { mutableStateOf("") }
    var roomName by remember{mutableStateOf(roomOwner.name.ifBlank{"Room"})}
    var roomImage by remember{mutableStateOf(roomOwner.image)}
    var announcement by remember{mutableStateOf("")}
    LaunchedEffect(roomId) {
        withContext(Dispatchers.IO) {
            runCatching {
                val encoded = java.net.URLEncoder.encode(roomId, "UTF-8")
                val conn = (java.net.URL("https://hurry-voice-chat-lz75.onrender.com/api/rooms?roomId=$encoded").openConnection() as java.net.HttpURLConnection).apply { connectTimeout=10000; readTimeout=10000 }
                try {
                    if (conn.responseCode in 200..299) {
                        val root = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                        val saved = root.optJSONObject("room") ?: root
                        val name = saved.optString("name", saved.optString("roomName", "")).takeUnless { it == "null" }.orEmpty()
                        val image = saved.optString("dp", saved.optString("image", saved.optString("roomDp", ""))).takeUnless { it == "null" }.orEmpty()
                        val message = saved.optString("message", saved.optString("announcement", "")).takeUnless { it == "null" }.orEmpty()
                        launch(Dispatchers.Main) {
                            if (name.isNotBlank()) roomName = name
                            if (image.isNotBlank()) roomImage = image
                            announcement = message
                        }
                    }
                } finally { conn.disconnect() }
            }
        }
    }
    var followed by remember{mutableStateOf(false)}
    LaunchedEffect(roomId, accountId) {
        if (accountId.isBlank() || accountId == "guest") return@LaunchedEffect
        followed = withContext(Dispatchers.IO) {
            runCatching {
                val url = java.net.URL("https://hurry-voice-chat-lz75.onrender.com/api/room-follows?userId=" + java.net.URLEncoder.encode(accountId, "UTF-8"))
                val conn = (url.openConnection() as java.net.HttpURLConnection).apply { connectTimeout = 8000; readTimeout = 8000 }
                try {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val follows = JSONObject(body).optJSONArray("follows") ?: JSONArray()
                    (0 until follows.length()).any { follows.optJSONObject(it)?.optString("roomId") == roomId }
                } finally { conn.disconnect() }
            }.getOrDefault(false)
        }
    }
    var speaker by remember{mutableStateOf(true)}
    var localVoiceSpeaking by remember { mutableStateOf(false) }
    var publicMsgOff by remember{mutableStateOf(false)}
    var locked by remember{mutableStateOf(false)}
    var showChat by remember{mutableStateOf(false)}
    var showMessageSheet by remember{mutableStateOf(false)}
    var showMenu by remember{mutableStateOf(false)}
    var showStore by remember { mutableStateOf(false) }
    var storeInitialView by remember { mutableStateOf("store") }
    var showExitMenu by remember{mutableStateOf(false)}
    var keepingRoomForMini by remember { mutableStateOf(false) }
    var showInfo by remember{mutableStateOf(false)}
    var showActiveUsers by remember{mutableStateOf(false)}
    var showSettings by remember{mutableStateOf(false)}
    var showGift by remember{mutableStateOf(false)}
    var showEmoji by remember{mutableStateOf(false)}
    var showGames by remember{mutableStateOf(false)}
    var showRoomTask by remember{mutableStateOf(false)}
    var showWildParty by remember{mutableStateOf(false)}
    var showFruitParty by remember{mutableStateOf(false)}
    var showSeatSheet by remember{mutableStateOf(false)}

    // MUSIC STATE
    var miniMusicTrack by remember { mutableStateOf<NativeMusicTrack?>(null) }
    var musicControllerMinimized by remember { mutableStateOf(false) }
    var isMusicPlaying by remember { mutableStateOf(false) }
    var musicVolume by remember { mutableFloatStateOf(1f) }
    var musicPlaylist by remember { mutableStateOf<List<NativeMusicTrack>>(emptyList()) }
    var currentTrackIndex by remember { mutableIntStateOf(0) }
    var musicCurrentTime by remember { mutableFloatStateOf(0f) }
    var musicDuration by remember { mutableFloatStateOf(0f) }

    var input by remember{mutableStateOf("")}
    val chatFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    var keyboardShownForChat by remember { mutableStateOf(false) }

    LaunchedEffect(isMusicPlaying, miniMusicTrack) {
        while (isMusicPlaying && miniMusicTrack != null) {
            kotlinx.coroutines.delay(1000)
            if (musicDuration > 0f) {
                if (musicCurrentTime < musicDuration) {
                    musicCurrentTime = (musicCurrentTime + 1f).coerceAtMost(musicDuration)
                } else if (musicPlaylist.isNotEmpty()) {
                    currentTrackIndex = (currentTrackIndex + 1) % musicPlaylist.size
                    val nextTrack = musicPlaylist[currentTrackIndex]
                    miniMusicTrack = nextTrack
                    musicCurrentTime = 0f
                    runCatching {
                        val intent = Intent(appContext, RoomMusicService::class.java)
                            .setAction(RoomMusicService.ACTION_PLAY)
                            .putExtra(RoomMusicService.EXTRA_URI, nextTrack.url)
                            .putExtra(RoomMusicService.EXTRA_TITLE, nextTrack.name)
                        ContextCompat.startForegroundService(appContext, intent)
                    }
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
        if (musicPlaylist.isEmpty()) return
        currentTrackIndex = (currentTrackIndex + 1) % musicPlaylist.size
        val nextTrack = musicPlaylist[currentTrackIndex]
        miniMusicTrack = nextTrack
        musicCurrentTime = 0f
        runCatching {
            val intent = Intent(appContext, RoomMusicService::class.java)
                .setAction(RoomMusicService.ACTION_PLAY)
                .putExtra(RoomMusicService.EXTRA_URI, nextTrack.url)
                .putExtra(RoomMusicService.EXTRA_TITLE, nextTrack.name)
            ContextCompat.startForegroundService(appContext, intent)
        }
        isMusicPlaying = true
    }

    fun musicPrev() {
        if (musicPlaylist.isEmpty()) return
        currentTrackIndex = (currentTrackIndex - 1 + musicPlaylist.size) % musicPlaylist.size
        val prevTrack = musicPlaylist[currentTrackIndex]
        miniMusicTrack = prevTrack
        musicCurrentTime = 0f
        runCatching {
            val intent = Intent(appContext, RoomMusicService::class.java)
                .setAction(RoomMusicService.ACTION_PLAY)
                .putExtra(RoomMusicService.EXTRA_URI, prevTrack.url)
                .putExtra(RoomMusicService.EXTRA_TITLE, prevTrack.name)
            ContextCompat.startForegroundService(appContext, intent)
        }
        isMusicPlaying = true
    }

    fun musicClose() {
        runCatching {
            appContext.startService(Intent(appContext, RoomMusicService::class.java).setAction(RoomMusicService.ACTION_STOP))
        }
        miniMusicTrack = null
        isMusicPlaying = false
        MusicPlaybackState.currentPlayingUri = null
        MusicPlaybackState.currentTrack = null
        MusicPlaybackState.isPlaying = false
        musicPlaylist = emptyList()
        musicCurrentTime = 0f
        musicDuration = 0f
    }

    BackHandler(enabled = true) {
        when {
            showExitMenu -> showExitMenu = false
            showChat -> { showChat = false; keyboardController?.hide() }
            showMessageSheet -> showMessageSheet = false
            showMenu -> showMenu = false
            showStore -> showStore = false
            showInfo -> showInfo = false
            showActiveUsers -> showActiveUsers = false
            showSettings -> showSettings = false
            showGift -> showGift = false
            showEmoji -> showEmoji = false
            showGames -> showGames = false
            showRoomTask -> showRoomTask = false
            showWildParty -> showWildParty = false
            showFruitParty -> showFruitParty = false
            showSeatSheet -> showSeatSheet = false
            else -> showExitMenu = true
        }
    }
    
    var showOfficialAnnouncement by remember { mutableStateOf(true) }
    var clearedBy by remember { mutableStateOf("") }
    
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidthPx = remember(configuration, density) { with(density) { configuration.screenWidthDp.dp.roundToPx() } }
    
    LaunchedEffect(showChat, imeVisible) {
        if (!showChat) keyboardShownForChat = false
        else if (imeVisible) keyboardShownForChat = true
        else if (keyboardShownForChat) { showChat = false; keyboardShownForChat = false }
    }
    LaunchedEffect(showChat) {
        if (showChat) {
            kotlinx.coroutines.delay(120)
            runCatching { chatFocusRequester.requestFocus() }
            keyboardController?.show()
        } else keyboardController?.hide()
    }
    
    var selectedSeat by remember{mutableIntStateOf(0)}

    val messages=remember{mutableStateListOf<NativeRoomMessage>()}
    val entryToasts=remember{mutableStateListOf<NativeEntryToast>()}
    val roomSocketRef=remember{mutableStateOf<io.socket.client.Socket?>(null)}
    val seats=remember{mutableStateListOf<NativeRoomSeat>().apply{for(i in 1..15)add(NativeRoomSeat(i))}}
    val backendRoomUsers=remember{mutableStateListOf<ActiveUserNative>()}

    DisposableEffect(roomId,accountId,currentUser.name,currentUser.image,roomOwner.id,roomOwner.accountId) {
        val roomSocket = IO.socket("https://hurry-voice-chat-lz75.onrender.com")
        roomSocketRef.value=roomSocket
        val mainHandler = Handler(Looper.getMainLooper())
        val presenceListener = Emitter.Listener { args ->
            val payload = args.firstOrNull() as? JSONObject
            if (payload != null && payload.optString("roomId") == roomId) {
                val usersJson = payload.optJSONArray("users") ?: JSONArray()
                val parsed = ArrayList<ActiveUserNative>()
                for (index in 0 until usersJson.length()) {
                    val user = usersJson.optJSONObject(index) ?: continue
                    val userId = user.optString("accountId", user.optString("userId"))
                    if (userId.isNotBlank()) parsed.add(ActiveUserNative(accountId=userId,name=user.optString("name","User").ifBlank{"User"},image=user.optString("image","")))
                }
                mainHandler.post { backendRoomUsers.clear(); backendRoomUsers.addAll(parsed) }
            }
        }
        val messageListener = Emitter.Listener { args ->
            val payload = args.firstOrNull() as? JSONObject
            if (payload != null && payload.optString("roomId") == roomId) {
                val id=payload.optString("id", "${payload.optString("senderId","user")}-${payload.optLong("createdAt",System.currentTimeMillis())}")
                val type=payload.optString("type","message")
                val sender=payload.optString("senderName","Unknown")
                val image=payload.optString("senderAvatar",payload.optString("senderImage",""))
                val message=NativeRoomMessage(id,payload.optString("text",""),sender,image,payload.optString("senderAccountId",payload.optString("senderId","")),payload.optLong("createdAt",System.currentTimeMillis()),type)
                mainHandler.post {
                    if(messages.none{it.id==message.id}) messages.add(message)
                    if(type=="join" && entryToasts.none{it.id==id}) {
                        entryToasts.add(NativeEntryToast(id,sender,image))
                        mainHandler.postDelayed({entryToasts.removeAll{it.id==id}},4000)
                    }
                }
            }
        }
        val clearedListener = Emitter.Listener { args ->
            val payload=args.firstOrNull() as? JSONObject
            if(payload!=null && payload.optString("roomId")==roomId) {
                mainHandler.post {
                    messages.clear()
                    showOfficialAnnouncement = false
                    clearedBy = payload.optString("userName").ifBlank { backendRoomUsers.firstOrNull { it.accountId == payload.optString("userId") }?.name ?: "Admin" }
                }
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
                    parsed.add(NativeRoomSeat(number=seat.optInt("number",index+1),occupied=seat.optBoolean("isOccupied",user != null),locked=seat.optBoolean("isLocked",false),muted=seat.optBoolean("isMuted",false),speaking=seat.optBoolean("isSpeaking",false),userName=if (user == null) "" else user.optString("name","User"),userImage=if (user == null) "" else user.optString("image","").takeIf { it.isNotBlank() && it != "null" } ?: user.optString("dp","").takeIf { it.isNotBlank() && it != "null" } ?: user.optString("avatarUrl","").takeIf { it.isNotBlank() && it != "null" } ?: user.optString("profileImage","").takeIf { it.isNotBlank() && it != "null" } ?: "",accountId=if (user == null) "" else user.optString("accountId",user.optString("userId","")),gifSrc=gif?.optString("src","") ?: "",gifTimestamp=gif?.optLong("timestamp",0L) ?: 0L))
                }
                mainHandler.post {
                    val byNumber = parsed.associateBy { it.number }
                    for (index in seats.indices) { val number = seats[index].number; seats[index] = byNumber[number] ?: NativeRoomSeat(number=number) }
                    parsed.filter { it.number !in seats.map(NativeRoomSeat::number) }.forEach { seats.add(it) }
                }
            }
        }
        var joinMessageSent = false
        val connectListener = Emitter.Listener {
            val joinPayload = JSONObject().put("roomId",roomId).put("userId",currentUser.uid.ifBlank{currentUser.id.ifBlank{accountId}}).put("accountId",accountId).put("roomOwnerId",roomOwner.uid.ifBlank{roomOwner.id.ifBlank{roomOwner.accountId.ifBlank{roomId}}}).put("name",currentUser.name).put("dp",currentUser.image)
            roomSocket.emit("room_join",joinPayload)
            if (!joinMessageSent && accountId != "guest") {
                joinMessageSent = true
                val joinCreatedAt = System.currentTimeMillis()
                val joinId = "$accountId-$joinCreatedAt"
                val joinName = currentUser.name.ifBlank { "User" }
                val joinImage = currentUser.image.ifBlank { "https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/default-avatar.png" }
                val joinMessage = JSONObject().put("id",joinId).put("roomId",roomId).put("senderId",accountId).put("senderAccountId",accountId).put("senderName",joinName).put("senderAvatar",joinImage).put("text","Enter the Room").put("type","join").put("createdAt",joinCreatedAt)
                val nativeJoin = NativeRoomMessage(joinId,"Enter the Room",joinName,joinImage,accountId,joinCreatedAt,"join")
                mainHandler.post {
                    if (messages.none { it.id == joinId }) messages.add(nativeJoin)
                    if (entryToasts.none { it.id == joinId }) { entryToasts.add(NativeEntryToast(joinId,joinName,joinImage)); mainHandler.postDelayed({ entryToasts.removeAll { it.id == joinId } },4000) }
                }
                roomSocket.emit("room_message",joinMessage)
            }
            roomSocket.emit("room_presence_request",JSONObject().put("roomId",roomId))
            roomSocket.emit("room_seats_request",JSONObject().put("roomId",roomId))
        }
        roomSocket.on("room_presence",presenceListener)
        roomSocket.on("room_message",messageListener)
        roomSocket.on("room_chat_cleared",clearedListener)
        roomSocket.on("room_seats",seatListener)
        roomSocket.on(Socket.EVENT_CONNECT,connectListener)
        roomSocket.connect()
        onDispose {
            if (!keepingRoomForMini) roomSocket.emit("room_leave",JSONObject().put("roomId",roomId).put("userId",accountId).put("accountId",accountId))
            roomSocket.off("room_presence",presenceListener)
            roomSocket.off("room_message",messageListener)
            roomSocket.off("room_chat_cleared",clearedListener)
            roomSocket.off("room_seats",seatListener)
            roomSocket.off(Socket.EVENT_CONNECT,connectListener)
            roomSocket.disconnect()
            roomSocket.close()
            roomSocketRef.value=null
            mainHandler.post { backendRoomUsers.clear() }
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)){
        JitsiRoomAudio(roomId=roomId,userName=currentUser.name,canSpeak=seats.any { it.occupied && it.accountId == accountId && !it.muted },onSpeakingChanged={localVoiceSpeaking=it})
        AsyncImage(model="https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/1784875884052~2.jpg",contentDescription="Room theme background",modifier=Modifier.fillMaxSize(),contentScale=ContentScale.Crop)

        // TOP ROW: Trophy + Music Pill
        Row(modifier = Modifier.align(Alignment.TopStart).padding(top = 84.dp, start = 4.dp),verticalAlignment = Alignment.CenterVertically,horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.height(22.dp).background(Brush.horizontalGradient(listOf(Color(0xE6242B35),Color(0x99242B35),Color.Transparent))).clickable { showInfo = true }.padding(start = 12.dp, end = 12.dp),verticalAlignment = Alignment.CenterVertically) {
                TransparentGreenImage(url = "https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/1788258883971~2.jpg",modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("0", color = Color(0xFFEEF3A3), fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, letterSpacing = 0.1.sp)
                Icon(Icons.Default.ChevronRight, null, tint = Color(0xFFEEF3A3), modifier = Modifier.padding(start = 4.dp).size(10.dp))
            }
            if (musicControllerMinimized && miniMusicTrack != null) {
                Row(modifier = Modifier.height(26.dp).clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = 0.30f)).clickable { musicControllerMinimized = false }.padding(start = 2.dp, end = 10.dp),verticalAlignment = Alignment.CenterVertically,horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AsyncImage(model = "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/file_00000000ee70820b9fd0dfe71739874e.png",contentDescription = "Music",modifier = Modifier.size(22.dp),contentScale = ContentScale.Fit)
                    Text(text = miniMusicTrack?.name.orEmpty(),color = Color.White,fontSize = 11.sp,fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,maxLines = 1,overflow = TextOverflow.Clip,modifier = Modifier.widthIn(max = 68.dp).basicMarquee(iterations = Int.MAX_VALUE))
                }
            }
        }

        Column(Modifier.fillMaxSize().imePadding()){
            Row(Modifier.fillMaxWidth().padding(top=40.dp,start=0.dp,end=8.dp),verticalAlignment=Alignment.CenterVertically){ 
                Row(modifier = Modifier.height(44.dp).clip(RoundedCornerShape(topEnd = 50.dp, bottomEnd = 50.dp)).background(Color.Black.copy(alpha = 0.3f)).clickable { showInfo = true }.padding(start = 4.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(model = roomImage,contentDescription = "Room profile",modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)),contentScale = ContentScale.Crop)
                    Column(Modifier.padding(start = 8.dp), verticalArrangement = Arrangement.Center) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(text = roomName,color = Color.White,fontSize = 13.sp,fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,maxLines = 1,modifier = Modifier.widthIn(max = 85.dp).basicMarquee(iterations = Int.MAX_VALUE),overflow = TextOverflow.Clip)
                            Text(roomCountryFlag(roomOwner.country), color = Color.White, fontSize = 13.sp)
                            if (accountId != roomOwner.accountId.ifBlank { roomOwner.id }) {
                                Box(Modifier.size(20.dp).clip(CircleShape).background(if (followed) Color(0xFF9CA3AF) else Color(0xFF3B82F6)).clickable {
                                    val next = !followed
                                    followed = next
                                    onFollowToggle(roomId, next)
                                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                        runCatching {
                                            val encodedUser = java.net.URLEncoder.encode(accountId, "UTF-8")
                                            val encodedRoom = java.net.URLEncoder.encode(roomId, "UTF-8")
                                            val url = if (next) java.net.URL("https://hurry-voice-chat-lz75.onrender.com/api/room-follows") else java.net.URL("https://hurry-voice-chat-lz75.onrender.com/api/room-follows?userId=$encodedUser&roomId=$encodedRoom")
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
                                                    .put("followerName", currentUser.name)
                                                    .put("followerImage", currentUser.image)
                                                conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
                                            }
                                            conn.responseCode
                                            conn.disconnect()
                                        }
                                    }
                                }, contentAlignment = Alignment.Center) {
                                    Icon(imageVector = if(followed) Icons.Default.Check else Icons.Default.Add,contentDescription = if(followed) "Following" else "Follow",tint = Color.White,modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                        Text("ID:" + roomOwner.accountId.ifBlank { roomOwner.id.ifBlank { roomId } }, color = Color.White.copy(alpha = 0.9f), fontSize = 10.sp, modifier = Modifier.offset(y = (-3).dp))
                    }
                }
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)){
                    Row(modifier=Modifier.height(32.dp).clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha=.30f)).clickable{showActiveUsers=true}.padding(horizontal=12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)){
                        Image(painterResource(R.drawable.ic_hurry_people),contentDescription="Active users",modifier=Modifier.size(16.dp))
                        Text(backendRoomUsers.size.toString(),color=Color.White,fontSize=12.sp,fontWeight=androidx.compose.ui.text.font.FontWeight.SemiBold)
                    }
                    if(accountId==roomOwner.accountId.ifBlank{roomOwner.id}) {
                        Box(Modifier.size(32.dp).clip(CircleShape).background(Color.Black.copy(alpha=.30f)).clickable{showSettings=true},contentAlignment=Alignment.Center){Image(painterResource(R.drawable.ic_hurry_settings),contentDescription="Room settings",modifier=Modifier.size(18.dp))}
                    }
                    Box(Modifier.size(32.dp).clip(CircleShape).background(Color.Black.copy(alpha=.30f)).clickable{showExitMenu=true},contentAlignment=Alignment.Center){Image(painterResource(R.drawable.ic_hurry_power),contentDescription="Exit room",modifier=Modifier.size(18.dp))}
                }
            }
            
            Column(Modifier.fillMaxWidth().padding(horizontal=10.dp).padding(top=44.dp, bottom=8.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                seats.chunked(5).forEach { rowSeats ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween) {
                        rowSeats.forEach { seat -> NativeSeat(seat, roomOwner.accountId.ifBlank{roomOwner.id}, isSpeaking = seat.speaking || (seat.accountId == accountId && localVoiceSpeaking)) { selectedSeat = seat.number; showSeatSheet = true } }
                    }
                }
            }

            val chatListState = androidx.compose.foundation.lazy.rememberLazyListState()
            LaunchedEffect(messages.size, imeVisible) { if (messages.isNotEmpty()) chatListState.animateScrollToItem(messages.lastIndex) }
            
            LazyColumn(state=chatListState,modifier=Modifier.fillMaxWidth().weight(1f).padding(horizontal=10.dp),contentPadding=PaddingValues(bottom=8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                if (showOfficialAnnouncement) {
                    item {
                        Column(Modifier.fillMaxWidth().padding(start=4.dp, top=8.dp, end=82.dp, bottom=2.dp)) {
                            Column(Modifier.fillMaxWidth(.92f).clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(alpha=.30f)).padding(horizontal=14.dp,vertical=12.dp)) {
                                Text("Announcement: Welcome to Zyora. Any content related to porn, fraud, or fake official accounts will result in a ban!",color=Color(0xFFE2C67D),fontSize=13.sp,lineHeight=16.sp,fontWeight=androidx.compose.ui.text.font.FontWeight.Medium)
                            }
                        }
                    }
                } else if (clearedBy.isNotBlank()) {
                    item {
                        Column(Modifier.fillMaxWidth().padding(start=4.dp, top=8.dp, end=82.dp, bottom=2.dp)) {
                            Column(Modifier.fillMaxWidth(.92f).clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(alpha=.30f)).padding(horizontal=14.dp,vertical=12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) { SystemBellIcon(modifier = Modifier.size(44.dp)); Text("System", color = Color.Gray, fontSize = 12.sp) }
                                Spacer(Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(clearedBy, color = Color(0xFFE2C67D), fontSize = 13.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
                                    Text(" Cleared Chat messages", color = Color.White, fontSize = 13.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
                if(announcement.isNotBlank()) {
                    item {
                        Column(Modifier.fillMaxWidth().padding(start=4.dp, end=82.dp, bottom=2.dp)) {
                            Column(Modifier.fillMaxWidth(.92f).clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(alpha=.30f)).padding(horizontal=14.dp,vertical=12.dp)) {
                                Text("ANNOUNCEMENT: $announcement",color=Color(0xFFE2C67D),fontSize=13.sp,lineHeight=16.sp,fontWeight=androidx.compose.ui.text.font.FontWeight.Medium)
                            }
                        }
                    }
                }
                items(messages,key={it.id}){message ->
                    if(message.type=="join") {
                        Column(Modifier.padding(vertical=2.dp).wrapContentWidth().clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(alpha=.30f)).padding(horizontal=12.dp,vertical=4.dp),horizontalAlignment=Alignment.Start,verticalArrangement=Arrangement.spacedBy(2.dp)){
                            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
                                AsyncImage(model=message.senderImage.ifBlank{"https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/default-avatar.png"},contentDescription=message.sender,modifier=Modifier.size(24.dp).clip(CircleShape),contentScale=ContentScale.Crop)
                                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)){
                                    Text(message.sender,color=Color.White.copy(alpha=.95f),fontSize=12.sp,fontWeight=androidx.compose.ui.text.font.FontWeight.SemiBold)
                                    if (message.senderAccountId == roomOwner.accountId.ifBlank { roomOwner.id }) {
                                        androidx.compose.material3.Icon(imageVector=Icons.Filled.Home,contentDescription=null,modifier=Modifier.size(12.dp),tint=Color.White)
                                    }
                                }
                            }
                            Text("Enter the Room",color=Color.White,fontSize=12.sp,fontWeight=androidx.compose.ui.text.font.FontWeight.Medium, modifier=Modifier.padding(start=2.dp))
                        }
                    } else NativeMessage(message)
                }
            }
            
            if(showChat) {
                Row(Modifier.fillMaxWidth().background(Color.White).padding(horizontal=10.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    IconButton(onClick={showEmoji=!showEmoji},modifier=Modifier.size(38.dp)){Image(painterResource(R.drawable.ic_hurry_chat_image),contentDescription="Attach image",modifier=Modifier.size(26.dp))}
                    androidx.compose.foundation.text.BasicTextField(value=input,onValueChange={input=it},modifier=Modifier.weight(1f).focusRequester(chatFocusRequester).padding(vertical=10.dp),singleLine=true,textStyle=androidx.compose.ui.text.TextStyle(color=Color.Black,fontSize=15.sp),cursorBrush=SolidColor(Color.Blue),decorationBox={innerTextField -> Box { if(input.isEmpty()) Text("Type a message...",color=Color.Gray,fontSize=15.sp); innerTextField() }})
                    IconButton(onClick={if(input.isNotBlank()){messages.add(NativeRoomMessage(System.currentTimeMillis().toString(),input,currentUser.name,currentUser.image,accountId));input=""}},modifier=Modifier.size(38.dp)){Icon(Icons.Default.Send,null,tint=Color(0xFF3B82F6),modifier=Modifier.size(26.dp))}
                }
            } else {
                Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal=8.dp, vertical=6.dp).height(44.dp),verticalAlignment=Alignment.CenterVertically){
                    Row(Modifier.weight(1f),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)){
                        RoomFooterButton(onClick={showChat=true},description="Say Hi Chat",icon=R.drawable.ic_hurry_chat)
                        val selfSeatIndex=seats.indexOfFirst{it.occupied && it.accountId==accountId}
                        if(selfSeatIndex>=0) {
                            val seat = seats[selfSeatIndex]
                            Box(Modifier.size(42.dp).clip(CircleShape).background(Color.Black.copy(alpha=.30f)).clickable{ val nextMuted = !seat.muted; seats[selfSeatIndex]=seat.copy(muted=nextMuted); roomSocketRef.value?.emit("room_seat_action", JSONObject().put("roomId", roomId).put("userId", accountId).put("action", "mute").put("seatNumber", seat.number).put("isMuted", nextMuted)) },contentAlignment=Alignment.Center) {
                                Icon(imageVector = if (seat.muted) Icons.Default.MicOff else Icons.Default.Mic,contentDescription = "Microphone",tint = Color.White,modifier = Modifier.size(28.dp))
                            }
                        }
                        if(selfSeatIndex>=0) RoomFooterButton(onClick={showEmoji=true},description="Emoji",icon=R.drawable.ic_hurry_emoji)
                    }
                    AsyncImage(
                        model="https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/file_0000000019c4821180028eebae10dbfc.png",
                        contentDescription="Gift",
                        modifier=Modifier.size(56.dp).clickable{showGift=true},
                        contentScale=ContentScale.Fit
                    )
                    Row(Modifier.weight(1f),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp,Alignment.End)){
                        RoomFooterButton(onClick={showMessageSheet=true},description="Message Box Menu",icon=R.drawable.ic_hurry_mail)
                        RoomFooterButton(onClick={showMenu=true},description="Apps Menu",icon=R.drawable.ic_hurry_apps)
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

        Column(modifier=Modifier.align(Alignment.BottomEnd).padding(end=10.dp, bottom=68.dp).heightIn(max=330.dp).verticalScroll(rememberScrollState()),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(2.dp)) {
            Column(horizontalAlignment=Alignment.CenterHorizontally) {
                androidx.compose.foundation.pager.HorizontalPager(state = sideBannerPager,modifier = Modifier.size(60.dp, 84.dp).clip(RoundedCornerShape(8.dp))) { page ->
                    AsyncImage(model = sideBannerImages[page],contentDescription = "Room side banner",modifier = Modifier.fillMaxSize(),contentScale = ContentScale.Crop)
                }
                Row(horizontalArrangement=Arrangement.spacedBy(2.dp), verticalAlignment=Alignment.CenterVertically, modifier=Modifier.padding(top=4.dp)) {
                    repeat(sideBannerImages.size) { i ->
                        val isActive = sideBannerPager.currentPage == i
                        Box(Modifier.size(if (isActive) 6.dp else 4.dp).clip(CircleShape).background(if (isActive) Color.White else Color.White.copy(alpha=.4f)))
                    }
                }
            }
            TransparentGreenImage(url="https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/IMG-20260902-WA0066.jpg",modifier=Modifier.size(63.dp).clickable { showRoomTask=true })
            AsyncImage(model="https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/file_000000002a8c81fab8232bebeeeb45cc.png",contentDescription="Games",modifier=Modifier.size(56.dp).clickable { showGames=true },contentScale=ContentScale.Fit)
        }
        
        Column(Modifier.align(Alignment.BottomStart).padding(bottom=LocalConfiguration.current.screenHeightDp.dp*0.33f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
            entryToasts.toList().forEach { toast ->
                key(toast.id) {
                    val visibleState = remember { androidx.compose.animation.core.MutableTransitionState(false).apply { targetState = true } }
                    AnimatedVisibility(
                        visibleState = visibleState, 
                        enter=slideInHorizontally(initialOffsetX={ _ -> screenWidthPx }, animationSpec = tween(durationMillis = 800, easing = LinearOutSlowInEasing)) + fadeIn(animationSpec = tween(800))
                    ) {
                        Row(Modifier.fillMaxWidth(0.75f).clip(RoundedCornerShape(topStart=50.dp,bottomStart=50.dp,topEnd=0.dp,bottomEnd=0.dp)).background(Brush.horizontalGradient(listOf(Color(0xF52563EB),Color(0x002563EB)))).padding(start=10.dp,end=10.dp,top=2.dp,bottom=2.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                            AsyncImage(model=toast.image.ifBlank{"https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/default-avatar.png"},contentDescription=toast.name,modifier=Modifier.size(24.dp).clip(CircleShape),contentScale=ContentScale.Crop)
                            Text(toast.name,color=Color.White,fontSize=13.sp,fontWeight=androidx.compose.ui.text.font.FontWeight.SemiBold,maxLines=1,modifier=Modifier.widthIn(max=95.dp))
                            Text("Enter the Room",color=Color.White,fontSize=12.sp,maxLines=1)
                        }
                    }
                }
            }
        }

        // ==================== MUSIC CONTROLLER CARD ====================
        if (miniMusicTrack != null && !musicControllerMinimized && !MusicPlaybackState.isMusicPageOpen) {
            val track = miniMusicTrack!!
            Box(
                modifier = Modifier.align(Alignment.BottomCenter)
                    .zIndex(10f)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = (configuration.screenHeightDp * 0.08f).dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xE6000000))
                    .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Column(Modifier.fillMaxWidth()) {
                    Box(Modifier.fillMaxWidth()) {
                        Box(
                            Modifier.align(Alignment.TopStart)
                                .size(24.dp).clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.10f))
                                .clickable { musicClose() },
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Default.Close, "Close", tint = Color.White, modifier = Modifier.size(14.dp)) }

                        Text(
                            text = track.name,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.align(Alignment.Center).fillMaxWidth().padding(horizontal = 32.dp)
                        )

                        Box(
                            Modifier.align(Alignment.TopEnd)
                                .size(24.dp).clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.10f))
                                .clickable { musicControllerMinimized = true },
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Default.KeyboardArrowDown, "Minimize", tint = Color.White, modifier = Modifier.size(16.dp)) }
                    }

                    Spacer(Modifier.height(8.dp))

                    val progressFraction = if (musicDuration > 0f) (musicCurrentTime / musicDuration).coerceIn(0f, 1f) else 0f
                    ThickBar(
                        progress = progressFraction,
                        height = 6.dp,
                        activeColor = Color(0xFF3B82F6),
                        inactiveColor = Color.White.copy(alpha = 0.15f),
                        modifier = Modifier.fillMaxWidth(),
                        onProgressChange = { newProgress ->
                            if (musicDuration > 0f) {
                                musicCurrentTime = newProgress * musicDuration
                                runCatching {
                                    appContext.startService(
                                        Intent(appContext, RoomMusicService::class.java)
                                            .setAction(RoomMusicService.ACTION_SEEK)
                                            .putExtra(RoomMusicService.EXTRA_POSITION_MS, (musicCurrentTime * 1000f).toInt())
                                    )
                                }
                            }
                        }
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(formatMusicTime(musicCurrentTime), color = Color.White.copy(alpha = 0.6f), fontSize = 9.sp)
                        Text(formatMusicTime(musicDuration), color = Color.White.copy(alpha = 0.6f), fontSize = 9.sp)
                    }

                    Spacer(Modifier.height(4.dp))

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { musicPrev() }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.SkipPrevious, "Prev", tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(16.dp))
                        Box(
                            Modifier.size(44.dp).clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.10f))
                                .clickable { musicPlayPause() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isMusicPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isMusicPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        IconButton(onClick = { musicNext() }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.SkipNext, "Next", tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VolumeUp, "Volume", tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        ThickBar(
                            progress = musicVolume,
                            height = 6.dp,
                            activeColor = Color(0xFF3B82F6),
                            inactiveColor = Color.White.copy(alpha = 0.15f),
                            modifier = Modifier.weight(1f),
                            onProgressChange = { newVolume ->
                                musicVolume = newVolume
                                runCatching {
                                    appContext.startService(
                                        Intent(appContext, RoomMusicService::class.java)
                                            .setAction(RoomMusicService.ACTION_VOLUME)
                                            .putExtra(RoomMusicService.EXTRA_VOLUME, newVolume)
                                    )
                                }
                            }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "${(musicVolume * 100).toInt()}%",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                            textAlign = TextAlign.End,
                            modifier = Modifier.width(30.dp)
                        )
                    }
                }
            }
        }
        
        if(showExitMenu) RoomExitMenu(
            onKeep={keepingRoomForMini=true;showExitMenu=false;onKeepRoom(roomName,roomImage,roomId)},
            onExit={showExitMenu=false;onClose()},
            onDismiss={showExitMenu=false}
        )
        if(showMenu) Fourgride(
            onClose={showMenu=false},
            onClearChat={
                messages.clear()
                showOfficialAnnouncement = false
                val clearerName = currentUser.name.ifBlank { "User" }
                clearedBy = clearerName
                roomSocketRef.value?.emit("room_chat_clear",JSONObject().put("roomId",roomId).put("userId",accountId).put("userName",clearerName).put("timestamp",System.currentTimeMillis()))
                showMenu=false
            },
            publicMsgOff=publicMsgOff,
            onTogglePublicMsg={publicMsgOff=!publicMsgOff},
            speaker=speaker,
            onToggleSpeaker={speaker=!speaker},
            onMusicPlaylistChanged={ tracks ->
                musicPlaylist = tracks
                val selectedUri = miniMusicTrack?.url ?: MusicPlaybackState.currentPlayingUri
                val selectedIndex = tracks.indexOfFirst { it.url == selectedUri }
                if (selectedIndex >= 0) currentTrackIndex = selectedIndex
            },
            onMusicPlay={ track ->
                miniMusicTrack = track
                musicPlaylist = listOf(track)
                currentTrackIndex = 0
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
                isMusicPlaying = playing
                MusicPlaybackState.currentPlayingUri = track.url
                MusicPlaybackState.currentTrack = track
                MusicPlaybackState.isPlaying = playing
            },
            onMusicMinimize={ track ->
                if (track != null) {
                    miniMusicTrack = track
                    musicControllerMinimized = false
                } else {
                    miniMusicTrack = null
                    musicControllerMinimized = false
                }
                showMenu = false
            },
            onOpenStore={ view ->
                storeInitialView = if (view.equals("bag", true)) "bag" else "store"
                showStore = true
                showMenu = false
            }
        )
        
        val activeRoomUsers = backendRoomUsers.toList()
        val roomFollowers = seats.filter { it.occupied && it.accountId.isNotBlank() && it.accountId != roomOwner.accountId && it.accountId != roomOwner.id }.map { RoomInfoFollower(it.accountId,it.userName,it.userImage) }
        if(showStore) StorePage(onBack={showStore=false}, initialView=storeInitialView)
        if(showInfo) RoomInfo(isOpen=true,onClose={showInfo=false},isRoomOwner=accountId==roomOwner.accountId.ifBlank{roomOwner.id},roomOwner=RoomInfoOwner(id=roomOwner.id,uid=roomOwner.uid,accountId=roomOwner.accountId.ifBlank{roomOwner.id},name=roomOwner.name,image=roomOwner.image),roomData=RoomInfoData(roomName,roomImage,announcement,roomId),roomFollowers=roomFollowers,onOpenProfile={ follower -> seats.firstOrNull{it.accountId==follower.accountId}?.let{selectedSeat=it.number};showInfo=false },onCopyId={ id -> clipboard.setText(AnnotatedString(id));copiedId=id },copied=copiedId==roomId)
        if(showActiveUsers) ActiveUsers(isOpen=true,onClose={showActiveUsers=false},roomUsers=activeRoomUsers,currentUser=ActiveUserNative(accountId,currentUser.name,currentUser.image),roomOwnerId=roomOwner.accountId.ifBlank{roomOwner.id},onOpenProfile={ user -> seats.firstOrNull{it.accountId==user.accountId}?.let{selectedSeat=it.number};showActiveUsers=false },onCopyUserId={ id -> clipboard.setText(AnnotatedString(id));copiedId=id })
        if(showSettings) RoomSettingPage(initial = RoomSettingsDataNative(roomName=roomName,roomDp=roomImage,announcement=announcement,isLocked=locked),roomOwnerId=roomId,currentUserId=accountId,onBack={showSettings=false},onSave={saved->
            roomName=saved.roomName;roomImage=saved.roomDp;announcement=saved.announcement;locked=saved.isLocked;showSettings=false
            CoroutineScope(Dispatchers.IO).launch {
                runCatching {
                    val conn = (java.net.URL("https://hurry-voice-chat-lz75.onrender.com/api/rooms").openConnection() as java.net.HttpURLConnection).apply {
                        requestMethod="PUT"; connectTimeout=10000; readTimeout=10000; doOutput=true
                        setRequestProperty("Content-Type","application/json")
                    }
                    val payload = JSONObject().put("accountId", roomId).put("id", roomId).put("name", saved.roomName).put("dp", saved.roomDp).put("image", saved.roomDp).put("message", saved.announcement).put("announcement", saved.announcement).put("isLocked", saved.isLocked).put("country", roomOwner.country)
                    conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
                    val status=conn.responseCode
                    if (status !in 200..299) throw java.io.IOException("Room save failed: HTTP $status")
                    conn.disconnect()
                }
            }
        })
        if(showGift) GiftPicker(open=true,onClose={showGift=false},seats=seats.toList(),roomId=roomId,currentUserAccountId=accountId,currentUserName=currentUser.name,currentUserImage=currentUser.image,roomUsers=activeRoomUsers)
        if(showGames) GamesSheet(onClose={showGames=false},onWildParty={showGames=false;showWildParty=true},onFruitParty={showGames=false;showFruitParty=true})
        if(showRoomTask) Roomtask(onBack={showRoomTask=false})
        if(showWildParty) Wildparty(onClose={showWildParty=false},onMinimize={showWildParty=false})
        if(showFruitParty) Fruitparty(onClose={showFruitParty=false},onMinimize={showFruitParty=false})
        if(showEmoji) EmojiPicker(onClose={showEmoji=false},onSelectEmoji={ sticker ->
            val seatIndex=seats.indexOfFirst{it.occupied && it.accountId==accountId}
            if(seatIndex>=0){
                val timestamp=System.currentTimeMillis()
                val selected=seats[seatIndex]
                seats[seatIndex]=selected.copy(gifSrc=sticker.src,gifTimestamp=timestamp)
                roomSocketRef.value?.emit("room_seat_action",JSONObject().put("roomId",roomId).put("userId",accountId).put("action","emoji").put("seatNumber",selected.number).put("user",JSONObject().put("name",currentUser.name.ifBlank{"User"}).put("image",currentUser.image.ifBlank{"https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/default-avatar.png"}).put("accountId",accountId)).put("src",sticker.src).put("timestamp",timestamp))
                
                Handler(Looper.getMainLooper()).postDelayed({ if(seats.getOrNull(seatIndex)?.gifTimestamp==timestamp) seats[seatIndex]=seats[seatIndex].copy(gifSrc="",gifTimestamp=0L) },3000)
            }
        })
        if(showSeatSheet && selectedSeat>0) {
            val seatIndex = seats.indexOfFirst { it.number == selectedSeat }
            val seat = seats.getOrNull(seatIndex)
            val isRoomOwner = accountId == roomOwner.accountId || accountId == roomOwner.id || accountId == roomId
            if(seat!=null) SeatActionSheet(
                seat=seat,
                isOwner=isRoomOwner,
                isMySeat=seat.occupied && seat.accountId==accountId,
                onDismiss={showSeatSheet=false;selectedSeat=0},
                onTake={
                    if(!seat.occupied && !seat.locked) {
                        val user = JSONObject().put("name",currentUser.name.ifBlank{"User"}).put("image",currentUser.image.ifBlank{"https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/default-avatar.png"}).put("accountId",accountId)
                        if(seatIndex>=0) seats[seatIndex]=seat.copy(occupied=true,userName=currentUser.name.ifBlank{"User"},userImage=currentUser.image.ifBlank { "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/default-avatar.png" },accountId=accountId,muted=false,locked=false)
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
                    roomSocketRef.value?.emit("room_seat_action",JSONObject().put("roomId",roomId).put("userId",accountId).put("action","mute").put("seatNumber",seat.number).put("isMuted",nextMuted))
                    showSeatSheet=false;selectedSeat=0
                },
                onLock={
                    if(isRoomOwner) {
                        val nextLocked=!seat.locked
                        if(seatIndex>=0) seats[seatIndex]=seat.copy(locked=nextLocked)
                        roomSocketRef.value?.emit("room_seat_action",JSONObject().put("roomId",roomId).put("userId",accountId).put("action","lock").put("seatNumber",seat.number).put("isLocked",nextLocked))
                        showSeatSheet=false;selectedSeat=0
                    }
                },
                onInvite={clipboard.setText(AnnotatedString("Join room $roomName — seat $selectedSeat (ID: $roomId)"));showSeatSheet=false;selectedSeat=0}
            )
        }
        if(selectedSeat>0 && seats.firstOrNull{it.number==selectedSeat}?.occupied==true && !showSeatSheet) MemberSheet(seats.first{it.number==selectedSeat}){selectedSeat=0}
        if(showMessageSheet) {
            val screenHeight = LocalConfiguration.current.screenHeightDp
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.30f)).clickable{showMessageSheet=false},contentAlignment=Alignment.BottomCenter) {
                Surface(modifier=Modifier.fillMaxWidth().height((screenHeight * .60f).dp).background(Color.White).clickable(enabled=false) {},color=Color.White,contentColor=Color.Black,shape=RoundedCornerShape(12.dp),shadowElevation=16.dp) {
                    Column(Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)).background(Color.White)) { MessageScreen(onChat={ _, _, _ -> showMessageSheet=false }, embeddedSheet=true, onBack={ showMessageSheet=false }) }
                }
            }
        }
    }
}

private fun formatMusicTime(seconds: Float): String {
    if (seconds.isNaN() || seconds < 0f) return "0:00"
    val t = seconds.toInt()
    return "${t / 60}:${(t % 60).toString().padStart(2, '0')}"
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

@Composable private fun RoomFooterButton(onClick:()->Unit,description:String,icon:Int){
    Box(Modifier.size(42.dp).clip(CircleShape).background(Color.Black.copy(alpha=.30f)).clickable(onClick=onClick),contentAlignment=Alignment.Center){
        Image(painterResource(icon),contentDescription=description,modifier=Modifier.size(when(description){"Say Hi Chat"->30.dp;"Emoji"->26.dp;"Message Box Menu"->28.dp;"Apps Menu"->26.dp;else->26.dp}))
    }
}

@Composable private fun NativeSeat(seat:NativeRoomSeat,ownerId:String,isSpeaking:Boolean=seat.speaking,onClick:()->Unit){
    val voicePulse = rememberInfiniteTransition(label = "voice-wave")
    val waveProgress by voicePulse.animateFloat(initialValue = 0.15f, targetValue = 1f, animationSpec = infiniteRepeatable(animation = tween(850, easing = LinearEasing), repeatMode = RepeatMode.Restart), label = "voice-wave-progress")
    val raw="https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/"
    
    var showGif by remember(seat.gifTimestamp) { mutableStateOf(seat.gifSrc.isNotBlank()) }
    LaunchedEffect(seat.gifTimestamp) {
        if (seat.gifTimestamp != 0L && seat.gifSrc.isNotBlank()) {
            showGif = true
            kotlinx.coroutines.delay(3000)
            showGif = false
        } else {
            showGif = false
        }
    }

    Column(horizontalAlignment=Alignment.CenterHorizontally,modifier=Modifier.width(62.dp).clickable(onClick=onClick)){
        Box(Modifier.size(62.dp),contentAlignment=Alignment.Center){ 
            Canvas(Modifier.fillMaxSize()) {
                val blue = !seat.locked
                val ringBrush = Brush.linearGradient(colors = if(blue) listOf(Color(0xFFD9F7FF),Color(0xFF91E1FF),Color(0xFF4DBFEF),Color(0xFF299DD6),Color(0xFF167DB9)) else listOf(Color(0xFFF3F4F6),Color(0xFFD1D5DB),Color(0xFF9CA3AF),Color(0xFF6B7280),Color(0xFF4B5563)),start=Offset(size.width*100f/512f,size.height*55f/512f),end=Offset(size.width*420f/512f,size.height*460f/512f))
                val radius=size.minDimension*225f/512f
                val center=Offset(size.width/2f,size.height/2f)
                drawCircle(color=if(blue) Color(0xFF55C6F3).copy(alpha=.25f) else Color(0xFF9CA3AF).copy(alpha=.25f),radius=radius+size.minDimension*7.5f/512f,center=center,style=Stroke(width=size.minDimension*15f/512f))
                if (isSpeaking) {
                    val pulseRadius = size.minDimension * (0.45f + waveProgress * 0.42f)
                    drawCircle(color = Color(0xFF168CFF).copy(alpha = 0.65f * (1f - waveProgress)), radius = pulseRadius, center = center, style = Stroke(width = size.minDimension * 0.055f))
                    drawCircle(color = Color(0xFF45B7FF).copy(alpha = 0.42f * (1f - waveProgress)), radius = pulseRadius * 0.78f, center = center, style = Stroke(width = size.minDimension * 0.035f))
                }
                drawCircle(brush=ringBrush,radius=radius,center=center,style=Stroke(width=size.minDimension*10f/512f))
                val shine = Brush.horizontalGradient(colors=listOf(Color.White.copy(alpha=0f),Color.White.copy(alpha=.75f),Color.White.copy(alpha=.95f),Color.White.copy(alpha=0f)),startX=0f,endX=size.width)
                val topHighlight=Path().apply { moveTo(size.width*98f/512f,size.height*134f/512f); cubicTo(size.width*113f/512f,size.height*108f/512f,size.width*132f/512f,size.height*92f/512f,size.width*153f/512f,size.height*79f/512f); cubicTo(size.width*174f/512f,size.height*67f/512f,size.width*196f/512f,size.height*59f/512f,size.width*218f/512f,size.height*56f/512f) }
                drawPath(topHighlight,shine,style=Stroke(width=size.minDimension*7.5f/512f,cap=StrokeCap.Round))
                val bottomHighlight=Path().apply { moveTo(size.width*207f/512f,size.height*452f/512f); cubicTo(size.width*258f/512f,size.height*466f/512f,size.width*316f/512f,size.height*456f/512f,size.width*361f/512f,size.height*428f/512f); cubicTo(size.width*378f/512f,size.height*417f/512f,size.width*392f/512f,size.height*404f/512f,size.width*404f/512f,size.height*388f/512f) }
                drawPath(bottomHighlight,shine,style=Stroke(width=size.minDimension*7f/512f,cap=StrokeCap.Round))
            }
            Box(Modifier.fillMaxSize(.91f).clip(CircleShape),contentAlignment=Alignment.Center){
                if(seat.locked) AsyncImage(model=raw+"file_0000000015a48211b000ee447a786f7c.png",contentDescription="Locked seat",modifier=Modifier.fillMaxSize(.62f),contentScale=ContentScale.Fit)
                else if(seat.occupied&&seat.userImage.isNotBlank()) AsyncImage(model=seat.userImage,contentDescription=seat.userName,modifier=Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
                else AsyncImage(model=raw+"file_000000003e7482309b7f6e7f2a922160.png",contentDescription="Empty seat",modifier=Modifier.fillMaxSize(.65f),contentScale=ContentScale.Fit)
            }
            
            if(showGif && seat.gifSrc.isNotBlank()) AsyncImage(model=seat.gifSrc,contentDescription="Emoji reaction",modifier=Modifier.fillMaxSize(),contentScale=ContentScale.Fit)
            
            if(seat.muted) Box(Modifier.align(Alignment.BottomEnd).clip(CircleShape).background(Color.Red).padding(2.dp)) { Icon(Icons.Default.MicOff,null,tint=Color.White,modifier=Modifier.size(12.dp)) }
        }
        
        if (seat.occupied && seat.accountId == ownerId) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Box(Modifier.size(14.dp).clip(CircleShape).background(Color(0xFF3B82F6)), contentAlignment = Alignment.Center) {
                    androidx.compose.material3.Icon(imageVector = Icons.Filled.Home, contentDescription = "Owner", tint = Color.White, modifier = Modifier.size(9.dp))
                }
                Spacer(Modifier.width(4.dp))
                Text(seat.userName, color = Color.White.copy(.9f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
            }
        } else {
            Text(if (seat.occupied) seat.userName else seat.number.toString(), color = Color.White.copy(.9f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable private fun NativeMessage(m:NativeRoomMessage){
    Row(Modifier.fillMaxWidth().padding(vertical=2.dp),verticalAlignment=Alignment.Top,horizontalArrangement=Arrangement.Start){
        AsyncImage(model=m.senderImage.ifBlank{"https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/default-avatar.png"},contentDescription=m.sender,modifier=Modifier.padding(top=2.dp).size(28.dp).clip(CircleShape).border(1.dp, Color.White.copy(alpha=.1f), CircleShape),contentScale=ContentScale.Crop)
        Column(Modifier.padding(start=8.dp).fillMaxWidth(.75f),verticalArrangement=Arrangement.spacedBy(2.dp)){
            Text(m.sender,color=Color.White.copy(alpha=.9f),fontSize=12.sp,lineHeight=13.sp,fontWeight=androidx.compose.ui.text.font.FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis)
            Box(Modifier.align(Alignment.Start).clip(RoundedCornerShape(topStart=2.dp,topEnd=12.dp,bottomStart=12.dp,bottomEnd=12.dp)).background(Color.Black.copy(alpha=.40f)).border(0.5.dp,Color.White.copy(alpha=.15f),RoundedCornerShape(topStart=2.dp,topEnd=12.dp,bottomStart=12.dp,bottomEnd=12.dp)).padding(horizontal=10.dp,vertical=8.dp)){
                Text(m.text,color=Color.White,fontSize=13.sp,lineHeight=18.sp,softWrap=true)
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
    AsyncImage(model=seat.userImage,contentDescription=null,modifier=Modifier.size(76.dp).clip(CircleShape),contentScale=ContentScale.Crop)
    Text(seat.userName,color=Color.White,fontSize=17.sp);Text("ID: "+seat.accountId,color=Color.White.copy(.65f),fontSize=12.sp)
}

@Composable private fun GamesSheet(onClose:()->Unit,onWildParty:()->Unit,onFruitParty:()->Unit){
    val raw="https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/"
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.30f)).clickable{onClose()},contentAlignment=Alignment.BottomCenter){
        Surface(Modifier.fillMaxWidth().clickable(enabled=false){},color=Color.Black.copy(alpha=0.92f),shape=RoundedCornerShape(topStart=15.dp,topEnd=15.dp)){
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(start=16.dp,end=16.dp,top=12.dp,bottom=24.dp)){
                Text("Games",color=Color.White,fontSize=16.sp,fontWeight=androidx.compose.ui.text.font.FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement=Arrangement.spacedBy(20.dp),verticalAlignment=Alignment.Top){
                    Column(Modifier.clickable{onWildParty()}, horizontalAlignment=Alignment.CenterHorizontally){
                        AsyncImage(model=raw+"file_000000009d808211b8ffb7c2183b4ef5.png",contentDescription="Wild party",modifier=Modifier.size(56.dp),contentScale=ContentScale.Fit)
                        Spacer(Modifier.height(2.dp))
                        Text("Wild party",color=Color.White,fontSize=10.sp,maxLines=1,softWrap=false)
                    }
                    Column(Modifier.clickable{onFruitParty()}, horizontalAlignment=Alignment.CenterHorizontally){
                        AsyncImage(model=raw+"file_00000000c9e08211ab3e8c4d511b7965.png",contentDescription="Fruit party",modifier=Modifier.size(54.dp).clip(RoundedCornerShape(6.dp)),contentScale=ContentScale.Fit)
                        Spacer(Modifier.height(2.dp))
                        Text("Fruit party",color=Color.White,fontSize=10.sp,maxLines=1,softWrap=false)
                    }
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
        value.isBlank() -> "🇮🇳"
        else -> country
    }
}

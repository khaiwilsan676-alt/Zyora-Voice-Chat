package com.hurry.voicechat

import androidx.compose.animation.core.animateFloat

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.geometry.Offset
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import coil3.compose.AsyncImage
import androidx.compose.ui.Alignment
import androidx.core.content.FileProvider
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Keep the foreground app awake so Android does not dim/timeout the display while in use.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()
        window.isNavigationBarContrastEnforced = false
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        setContent {
            var showLaunch by remember { mutableStateOf(true) }
            var authenticated by remember {
                mutableStateOf(
                    getSharedPreferences("hurry_auth", android.content.Context.MODE_PRIVATE)
                        .getString("uid", null).isNullOrBlank().not()
                )
            }
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(1200)
                showLaunch = false
                checkForHurryUpdate()
            }
            if (showLaunch) {
                HurryLaunchScreen()
            } else if (!authenticated) {
                NativeLoginScreen(onLoginSuccess = { authenticated = true })
            } else {
                HurryNativeRoot(onLogout = { authenticated = false })
            }
        }
    }

    private suspend fun checkForHurryUpdate() {
        withContext(Dispatchers.IO) {
            runCatching {
                val api = URL("https://api.github.com/repos/khaiwilsan676-alt/Hurry-Voice-Chat/releases/latest")
                val connection = (api.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 8000
                    readTimeout = 8000
                    setRequestProperty("Accept", "application/vnd.github+json")
                    setRequestProperty("User-Agent", "Hurry-Native")
                }
                val json = connection.inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()

                val tag = JSONObject(json).optString("tag_name")
                val remoteCode = tag.substringAfterLast("-").toIntOrNull() ?: return@runCatching
                val localCode = packageManager.getPackageInfo(packageName, 0).longVersionCode.toInt()
                if (remoteCode <= localCode) return@runCatching

                val assets = JSONObject(json).optJSONArray("assets") ?: return@runCatching
                val asset = (0 until assets.length()).map { assets.getJSONObject(it) }
                    .firstOrNull { it.optString("name") == "hurry-native.apk" }
                    ?: return@runCatching
                val downloadUrl = asset.optString("browser_download_url")
                if (downloadUrl.isBlank()) return@runCatching

                val apk = File(cacheDir, "hurry-native-update.apk")
                val dl = (URL(downloadUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 30000
                    setRequestProperty("User-Agent", "Hurry-Native")
                }
                dl.inputStream.use { input -> apk.outputStream().use { output -> input.copyTo(output) } }
                dl.disconnect()

                runOnUiThread {
                    val uri: Uri = FileProvider.getUriForFile(this@MainActivity, "$packageName.fileprovider", apk)
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/vnd.android.package-archive")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(intent)
                }
            }
        }
    }
}

@Composable
private fun HurryLaunchScreen() {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AsyncImage(
            model = "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/file_000000003b34820ba9a4e048344ec207.png",
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = screenHeight * 0.35f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/logo.png",
                contentDescription = "Zyora logo",
                modifier = Modifier
                    .width(68.dp)
                    .height(68.dp)
                    .clip(RoundedCornerShape(20.dp)),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Zyora",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}
@Composable
private fun HurryNativeRoot(onLogout: () -> Unit) {
    val context = LocalContext.current
    val roomCreateScope = rememberCoroutineScope()
    val authPrefs = remember { context.getSharedPreferences("hurry_auth", android.content.Context.MODE_PRIVATE) }
    val languagePrefs = remember { context.getSharedPreferences("zyora_settings", android.content.Context.MODE_PRIVATE) }
    var languageCode by remember { mutableStateOf(languagePrefs.getString("appLanguage", "en") ?: "en") }
    val userId = remember { authPrefs.getString("uid", "").orEmpty() }
    var mineCreatedRoom by remember {
        mutableStateOf(
            if (authPrefs.getBoolean("mine_room_created", false)) HurryRoom(
                id = authPrefs.getString("mine_room_id", userId).orEmpty(),
                name = authPrefs.getString("mine_room_name", "Room").orEmpty(),
                image = authPrefs.getString("mine_room_image", "").orEmpty(),
                country = authPrefs.getString("mine_room_country", "🇮🇳").orEmpty(),
                accountId = authPrefs.getString("mine_room_account_id", userId).orEmpty()
            ) else null
        )
    }
    // Restore this account's existing room from the server after app reinstall/data refresh.
    LaunchedEffect(userId) {
        val stableOwnerId = authPrefs.getString("accountId", userId).orEmpty().ifBlank { userId }
        if (stableOwnerId.isNotBlank()) {
            withContext(Dispatchers.IO) {
                runCatching {
                    val encoded = java.net.URLEncoder.encode(stableOwnerId, "UTF-8")
                    val connection = (URL("https://zyora-voice-chat.onrender.com/api/rooms?roomId=$encoded").openConnection() as HttpURLConnection).apply {
                        connectTimeout = 10000
                        readTimeout = 10000
                    }
                    try {
                        if (connection.responseCode in 200..299) {
                            val root = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                            val saved = root.optJSONObject("room") ?: root
                            val serverRoom = HurryRoom(
                                id = saved.optString("accountId", saved.optString("roomId", saved.optString("id", stableOwnerId))),
                                name = saved.optString("name", saved.optString("roomName", "Room")).ifBlank { "Room" },
                                image = saved.optString("dp", saved.optString("image", saved.optString("roomDp", ""))),
                                announcement = saved.optString("message", saved.optString("announcement", "")),
                                country = saved.optString("country", "🇮🇳"),
                                locked = saved.optBoolean("isLocked", false),
                                accountId = saved.optString("accountId", stableOwnerId)
                            )
                            withContext(Dispatchers.Main) {
                                authPrefs.edit()
                                    .putBoolean("mine_room_created", true)
                                    .putString("mine_room_id", serverRoom.id)
                                    .putString("mine_room_name", serverRoom.name)
                                    .putString("mine_room_image", serverRoom.image)
                                    .putString("mine_room_country", serverRoom.country)
                                    .putString("mine_room_account_id", serverRoom.accountId)
                                    .apply()
                                mineCreatedRoom = serverRoom
                            }
                        } else if (mineCreatedRoom != null) {
                            // Migrate a previously created local room to the permanent server record.
                            val local = mineCreatedRoom!!
                            val payload = JSONObject()
                                .put("accountId", local.accountId.ifBlank { stableOwnerId })
                                .put("roomId", local.id)
                                .put("id", local.id)
                                .put("name", local.name)
                                .put("roomName", local.name)
                                .put("dp", local.image)
                                .put("roomDp", local.image)
                                .put("country", local.country)
                                .put("message", local.announcement)
                                .put("announcement", local.announcement)
                            val save = (URL("https://zyora-voice-chat.onrender.com/api/rooms").openConnection() as HttpURLConnection).apply {
                                requestMethod = "PUT"
                                connectTimeout = 10000
                                readTimeout = 10000
                                doOutput = true
                                setRequestProperty("Content-Type", "application/json")
                            }
                            save.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
                            runCatching { save.inputStream.close() }
                            save.disconnect()
                        }
                    } finally { connection.disconnect() }
                }
            }
        }
    }
    var tab by remember { mutableStateOf(HurryTab.HOME) }
    var homeMine by remember { mutableStateOf(false) }
    var keptRoom by remember {
        authPrefs.edit().remove("kept_room_data").apply()
        mutableStateOf<HurryRoom?>(null)
    }
    var openedRoom by remember { mutableStateOf<HurryRoom?>(null) }
    var openedChat by remember { mutableStateOf<Triple<String, String, String>?>(null) }
    var openedMePage by remember { mutableStateOf<String?>(null) }
    var openedLeaderboard by remember { mutableStateOf<String?>(null) }
    var openedInvite by remember { mutableStateOf(false) }
    var openedSearch by remember { mutableStateOf(false) }
    var openedRoomSupport by remember { mutableStateOf(false) }
    var openedPublicProfile by remember { mutableStateOf<RoomProfileUser?>(null) }

    BackHandler(enabled = true) {
        when {
            openedPublicProfile != null -> openedPublicProfile = null
            openedRoomSupport -> openedRoomSupport = false
            openedSearch -> openedSearch = false
            openedRoom != null -> openedRoom = null
            openedLeaderboard != null -> openedLeaderboard = null
            openedInvite -> openedInvite = false
            openedChat != null -> openedChat = null
            openedMePage != null -> openedMePage = null
            tab != HurryTab.HOME -> tab = HurryTab.HOME
            homeMine -> homeMine = false
            // Stay on the current root screen rather than closing the app.
            else -> Unit
        }
    }

    HurryTheme {
        Box(Modifier.fillMaxSize().navigationBarsPadding()) {
            if (openedPublicProfile != null) {
                PublicProfile(user=openedPublicProfile!!,onBack={openedPublicProfile=null},onMessage={ val profile = openedPublicProfile!!; val receiverId = profile.uid?.takeIf { it.isNotBlank() } ?: profile.id?.takeIf { it.isNotBlank() } ?: profile.accountId.orEmpty(); if (receiverId.isNotBlank() && receiverId != userId) { openedChat = Triple(receiverId, profile.name, profile.image); openedPublicProfile = null } },currentUserId=userId)
            } else if (openedRoomSupport) {
                RoomSupport(onBack = { openedRoomSupport = false })
            } else if (openedSearch) {
                SearchPage(onBack={openedSearch=false},onOpenProfile={openedPublicProfile=it;openedSearch=false},onOpenRoom={openedRoom=it;openedSearch=false})
            } else if (openedRoom != null) {
                RoomPage(
                    roomOwner = RoomPageOwner(
                        id = openedRoom!!.id,
                        accountId = openedRoom!!.accountId.ifBlank { openedRoom!!.id },
                        name = openedRoom!!.name,
                        image = openedRoom!!.image,
                        country = openedRoom!!.country
                    ),
                    currentUser = RoomPageCurrentUser(id = userId, uid = userId, accountId = authPrefs.getString("accountId", userId).orEmpty(), name = authPrefs.getString("name", "User").orEmpty(), image = authPrefs.getString("image", "").orEmpty()),
                    onBack = { openedRoom = null },
                    onRoomSupport = { openedRoomSupport = true },
                    onClose = {
                        if (keptRoom?.id == openedRoom?.id) {
                            keptRoom = null
                            authPrefs.edit().remove("kept_room_data").apply()
                        }
                        openedRoom = null
                    },
                    onKeepRoom = { roomName, roomImage, roomId ->
                        val active = openedRoom
                        val miniRoom = HurryRoom(
                            id = roomId.ifBlank { active?.id.orEmpty() },
                            name = roomName.ifBlank { active?.name ?: "Room" },
                            image = roomImage.ifBlank { active?.image.orEmpty() },
                            country = active?.country ?: "🇮🇳",
                            accountId = active?.accountId?.ifBlank { active.id } ?: roomId
                        )
                        keptRoom = miniRoom
                        // Keep is session-only: the mini-room bubble must not survive app close.
                        authPrefs.edit().remove("kept_room_data").apply()
                        openedRoom = null
                    }
                )
            } else if (openedLeaderboard != null) {
                Leaderboard(onBack = { openedLeaderboard = null }, initialTab = openedLeaderboard!!)
            } else if (openedInvite) {
                InviteFriends(onBack = { openedInvite = false })
            } else if (openedChat != null) {
                val chat = openedChat!!
                NativeChatScreen(name = chat.second, image = chat.third, receiverId = chat.first, currentUserId = userId, onBack = { openedChat = null })
            } else {
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1f)) {
                        when (tab) {
                            HurryTab.HOME -> HomeScreen(
                                languageCode = languageCode,
                                onRoom = { openedRoom = it },
                                onMine = { homeMine = true },
                                onPopular = { homeMine = false },
                                onLeaderboard = { openedLeaderboard = it },
                                onInviteFriends = { openedInvite = true },
                                onSearch = { openedSearch = true },
                                onRoomSupport = { openedRoomSupport = true },
                                onCreateMineRoom = {
                                    // A room is keyed to the user's stable account ID, not to each tap.
                                    // Once saved locally, every tap only reopens this same room.
                                    val stableOwnerId = authPrefs.getString("accountId", userId).orEmpty().ifBlank { userId }
                                    val room = mineCreatedRoom ?: HurryRoom(
                                        id = stableOwnerId.ifBlank { "mine-room" },
                                        name = "Room",
                                        image = authPrefs.getString("image", "").orEmpty().ifBlank { "https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/default-avatar.png" },
                                        country = authPrefs.getString("country", "🇮🇳").orEmpty(),
                                        accountId = stableOwnerId.ifBlank { userId }
                                    ).also {
                                        authPrefs.edit()
                                            .putBoolean("mine_room_created", true)
                                            .putString("mine_room_id", it.id)
                                            .putString("mine_room_name", it.name)
                                            .putString("mine_room_image", it.image)
                                            .putString("mine_room_country", it.country)
                                            .putString("mine_room_account_id", it.accountId)
                                            .apply()
                                        mineCreatedRoom = it
                                        roomCreateScope.launch {
                                            withContext(Dispatchers.IO) {
                                                runCatching {
                                                    val payload = JSONObject()
                                                        .put("accountId", it.accountId)
                                                        .put("roomId", it.id)
                                                        .put("id", it.id)
                                                        .put("name", it.name)
                                                        .put("roomName", it.name)
                                                        .put("dp", it.image)
                                                        .put("roomDp", it.image)
                                                        .put("country", it.country)
                                                        .put("roomAdmin", it.accountId)
                                                        .put("message", it.name + "'s Room Notice")
                                                        .put("theme", "default")
                                                    val connection = (URL("https://zyora-voice-chat.onrender.com/api/rooms").openConnection() as HttpURLConnection).apply {
                                                        requestMethod = "PUT"
                                                        connectTimeout = 10000
                                                        readTimeout = 10000
                                                        doOutput = true
                                                        setRequestProperty("Content-Type", "application/json")
                                                    }
                                                    connection.outputStream.use { output -> output.write(payload.toString().toByteArray(Charsets.UTF_8)) }
                                                    runCatching { connection.inputStream.close() }
                                                    connection.disconnect()
                                                }
                                            }
                                        }
                                    }
                                    openedRoom = room
                                },
                                mineCreatedRoom = mineCreatedRoom,
                                mineSelected = homeMine
                            )
                            HurryTab.MESSAGE -> MessageScreen(onChat = { uid, name, image ->
                                openedChat = Triple(uid, name, image)
                            })
                            HurryTab.ME -> when (openedMePage) {
                                null -> MeScreen(languageCode = languageCode,onOpen = { openedMePage = it }, onPublicProfile = { openedPublicProfile = it })
                                "Coins" -> CoinsNativePage(onBack = { openedMePage = null })
                                "Diamonds" -> DiamondsNativePage(onBack = { openedMePage = null })
                                "Invite Friends" -> InviteFriends(onBack = { openedMePage = null })
                                "Family" -> Family(onBack = { openedMePage = null })
                                "Level" -> Level(onBack = { openedMePage = null })
                                "Medal" -> MedalScreen(onBack = { openedMePage = null })
                                "Store" -> StorePage(onBack = { openedMePage = null })
                                "Bag" -> StorePage(onBack = { openedMePage = null }, initialView = "bag")
                                "Seller Center" -> SellerCenter(onBack = { openedMePage = null })
                                "SVIP" -> SvipPage(onBack = { openedMePage = null })
                                "Language Setting" -> LanguagePage(selectedLanguage = languageCode, onLanguageSelected = { code -> languageCode = code; languagePrefs.edit().putString("appLanguage", code).apply() }, onBack = { openedMePage = null })
                                "Settings" -> SettingsPage(languageCode = languageCode, onBack = { openedMePage = null }, onLogout = {
                                    authPrefs.edit()
                                        .remove("uid")
                                        .remove("name")
                                        .remove("phone")
                                        .remove("image")
                                        .remove("accountId")
                                        .apply()
                                    openedMePage = null
                                    onLogout()
                                })
                                "Customer Service" -> HurrySupport(onBack = { openedMePage = null })
                                "Help & Feedback" -> HelpFeedbackPage(onBack = { openedMePage = null })
                                else -> MeNativeSubPage(openedMePage!!, onBack = { openedMePage = null })
                            }
                        }
                    }
                    if (openedMePage == null) {
                        HurryBottomNav(tab, languageCode = languageCode, onTab = { tab = it })
                    }
                }
            }
            if (
                openedRoom == null &&
                openedChat == null &&
                openedMePage == null &&
                openedPublicProfile == null &&
                openedLeaderboard == null &&
                !openedInvite &&
                !openedSearch &&
                !openedRoomSupport &&
                tab in setOf(HurryTab.HOME, HurryTab.MESSAGE, HurryTab.ME) &&
                keptRoom != null
            ) {
                KeptRoomBubble(
                    room = keptRoom!!,
                    onOpen = { openedRoom = keptRoom },
                    onExit = {
                        keptRoom = null
                        authPrefs.edit().remove("kept_room_data").apply()
                    }
                )
            }
            LuckyBagNotificationSlider(
                currentUserId = userId,
                onGoToRoom = { roomId, roomName ->
                    openedRoom = HurryRoom(
                        id = roomId,
                        name = roomName.ifBlank { "Room" },
                        image = "",
                        country = "🇮🇳",
                        accountId = roomId
                    )
                }
            )
        }
    }
}

@Composable
private fun KeptRoomBubble(room: HurryRoom, onOpen: () -> Unit, onExit: () -> Unit) {
    var drag by remember(room.id) { mutableStateOf(Offset.Zero) }
    var dragging by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        if (dragging) {
            val exitWave = androidx.compose.animation.core.rememberInfiniteTransition(label = "keep_exit_wave")
            val pulse by exitWave.animateFloat(
                initialValue = 0.72f,
                targetValue = 1f,
                animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                    animation = androidx.compose.animation.core.tween(620, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                    repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                ),
                label = "keep_exit_pulse"
            )
            Box(
                Modifier.align(Alignment.BottomEnd)
                    .size(58.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0x33F44336)),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                    val arcSize = androidx.compose.ui.geometry.Size(size.width * pulse, size.height * pulse)
                    val inset = (size.width - arcSize.width) / 2f
                    drawArc(
                        color = Color(0x99F44336),
                        startAngle = 42f,
                        sweepAngle = 276f,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 4.dp.toPx(),
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    )
                    drawArc(
                        color = Color(0x55F44336),
                        startAngle = 42f,
                        sweepAngle = 276f,
                        useCenter = false,
                        topLeft = Offset(5.dp.toPx(), 5.dp.toPx()),
                        size = androidx.compose.ui.geometry.Size(size.width - 10.dp.toPx(), size.height - 10.dp.toPx()),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                    )
                }
            }
        }
        Box(
            Modifier.align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 176.dp)
                .offset { IntOffset(drag.x.roundToInt(), drag.y.roundToInt()) }
                .size(56.dp).clip(RoundedCornerShape(50))
                .background(Color.White)
                .clickable(enabled = !dragging) { onOpen() }
                .pointerInput(room.id) {
                    detectDragGestures(
                        onDragStart = { dragging = true },
                        onDragEnd = {
                            val shouldExit = drag.y > 24f.dpToPxSafe()
                            dragging = false
                            if (shouldExit) onExit()
                            drag = Offset.Zero
                        },
                        onDragCancel = { dragging = false; drag = Offset.Zero },
                        onDrag = { change, amount ->
                            change.consume()
                            drag += amount
                        }
                    )
                }
        ) {
            AsyncImage(
                model = if (room.image.isBlank()) "https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/default-avatar.png" else room.image,
                contentDescription = "Kept room. Tap to reopen or drag to exit",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            val wave = androidx.compose.animation.core.rememberInfiniteTransition(label = "kept_room_wave")
            val wave1 by wave.animateFloat(initialValue = 0.25f, targetValue = 1f, animationSpec = androidx.compose.animation.core.infiniteRepeatable(animation = androidx.compose.animation.core.tween(380, easing = androidx.compose.animation.core.LinearEasing), repeatMode = androidx.compose.animation.core.RepeatMode.Reverse), label = "kept_wave_1")
            val wave2 by wave.animateFloat(initialValue = 0.9f, targetValue = 0.2f, animationSpec = androidx.compose.animation.core.infiniteRepeatable(animation = androidx.compose.animation.core.tween(470, easing = androidx.compose.animation.core.LinearEasing), repeatMode = androidx.compose.animation.core.RepeatMode.Reverse), label = "kept_wave_2")
            val wave3 by wave.animateFloat(initialValue = 0.35f, targetValue = 1f, animationSpec = androidx.compose.animation.core.infiniteRepeatable(animation = androidx.compose.animation.core.tween(330, easing = androidx.compose.animation.core.LinearEasing), repeatMode = androidx.compose.animation.core.RepeatMode.Reverse), label = "kept_wave_3")
            Row(
                Modifier.align(Alignment.Center).height(22.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                listOf(wave1, wave2, wave3).forEach { level ->
                    Box(
                        Modifier.width(3.dp).height((5f + level * 15f).dp)
                            .clip(RoundedCornerShape(3.dp)).background(Color.White)
                    )
                }
            }
        }
    }
}

private fun Float.dpToPxSafe(): Float = this * 3f

@Composable
private fun MeNativeSubPage(title: String, onBack: () -> Unit) {
    val socialPage = title in setOf("Friends", "Fans", "Following", "Visitors")
    Column(Modifier.fillMaxSize().background(Color.White).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("‹", fontSize = 36.sp, color = Color(0xFF222222), modifier = Modifier.clickable { onBack() })
            Spacer(Modifier.width(12.dp))
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.Normal, color = Color(0xFF111827))
        }
        if (socialPage) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(bottom = 48.dp)) {
                    androidx.compose.foundation.Canvas(Modifier.size(76.dp)) {
                        val ink = Color(0xFFD8DCE3)
                        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.4.dp.toPx())
                        val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height * 0.31f)
                        drawCircle(ink, radius = size.minDimension * 0.13f, center = center, style = stroke)
                        drawArc(ink, startAngle = 200f, sweepAngle = 140f, useCenter = false,
                            topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.17f, size.height * 0.36f),
                            size = androidx.compose.ui.geometry.Size(size.width * 0.66f, size.height * 0.53f), style = stroke)
                        drawCircle(ink, radius = size.minDimension * 0.09f, center = androidx.compose.ui.geometry.Offset(size.width * 0.75f, size.height * 0.40f), style = stroke)
                        drawArc(ink, startAngle = 205f, sweepAngle = 130f, useCenter = false,
                            topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.58f, size.height * 0.46f),
                            size = androidx.compose.ui.geometry.Size(size.width * 0.34f, size.height * 0.36f), style = stroke)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("No $title yet", fontSize = 14.sp, color = Color(0xFF9CA3AF))
                }
            }
        }
    }
}
package com.hurry.voicechat

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.StrokeCap

import android.content.Intent
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import coil3.compose.AsyncImage
import androidx.compose.ui.Alignment
import androidx.core.content.FileProvider
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import org.json.JSONObject
import com.google.firebase.auth.FirebaseAuth
import io.socket.client.IO
import io.socket.client.Socket
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
            var profileSetupComplete by remember {
                mutableStateOf(
                    getSharedPreferences("hurry_auth", android.content.Context.MODE_PRIVATE).let { prefs ->
                        // Never let a stale local completion flag hide the new-user page.
                        !prefs.getBoolean("needsProfileSetup", false) &&
                            prefs.getBoolean("profileSetupComplete", false)
                    }
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
                NativeLoginScreen(onLoginSuccess = {
                    val authPrefs = getSharedPreferences("hurry_auth", android.content.Context.MODE_PRIVATE)
                    val needsSetup = authPrefs.getBoolean("needsProfileSetup", false)
                    if (needsSetup) {
                        authPrefs.edit().putBoolean("profileSetupComplete", false).apply()
                        profileSetupComplete = false
                    } else {
                        profileSetupComplete = authPrefs.getBoolean("profileSetupComplete", true)
                    }
                    authenticated = true
                })
            } else if (getSharedPreferences("hurry_auth", android.content.Context.MODE_PRIVATE)
                    .getBoolean("needsProfileSetup", false) && !profileSetupComplete) {
                Newuserafterlogin(
                    onBack = { authenticated = false },
                    onComplete = {
                        getSharedPreferences("hurry_auth", android.content.Context.MODE_PRIVATE).edit()
                            .putBoolean("needsProfileSetup", false)
                            .putBoolean("profileSetupComplete", true)
                            .apply()
                        profileSetupComplete = true
                        authenticated = true
                    }
                )
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

    // Ask for the app's media, camera and voice permissions together only after
    // the authenticated user reaches the Home root (after first-time profile setup).
    val homePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }
    LaunchedEffect(Unit) {
        val requiredPermissions = buildList {
            add(Manifest.permission.CAMERA)
            add(Manifest.permission.RECORD_AUDIO)
            add(
                if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
                else Manifest.permission.READ_EXTERNAL_STORAGE
            )
            if (Build.VERSION.SDK_INT >= 31) add(Manifest.permission.BLUETOOTH_CONNECT)
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val missingPermissions = requiredPermissions.filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        }
        if (missingPermissions.isNotEmpty()) {
            homePermissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }
    val roomCreateScope = rememberCoroutineScope()
    val authPrefs = remember { context.getSharedPreferences("hurry_auth", android.content.Context.MODE_PRIVATE) }
    val languagePrefs = remember { context.getSharedPreferences("zyora_settings", android.content.Context.MODE_PRIVATE) }
    var languageCode by remember { mutableStateOf(languagePrefs.getString("appLanguage", "en") ?: "en") }
    val userId = remember { authPrefs.getString("uid", "").orEmpty() }
    LaunchedEffect(userId) {
        if (userId.isNotBlank()) {
            val serviceIntent = Intent(context, ZyoraMessageListenerService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(serviceIntent)
            else context.startService(serviceIntent)
        }
    }
    // Isolate the local Mine-room cache by Firebase UID when multiple Google accounts use one device.
    val mineRoomKey = "mine_room_${userId}"
    var mineCreatedRoom by remember {
        mutableStateOf(
            if (authPrefs.getBoolean("${mineRoomKey}_created", false)) HurryRoom(
                id = authPrefs.getString("${mineRoomKey}_id", userId).orEmpty(),
                name = authPrefs.getString("${mineRoomKey}_name", "Room").orEmpty(),
                image = authPrefs.getString("${mineRoomKey}_image", "").orEmpty(),
                announcement = authPrefs.getString("${mineRoomKey}_announcement", "").orEmpty(),
                country = authPrefs.getString("${mineRoomKey}_country", "").orEmpty(),
                accountId = authPrefs.getString("${mineRoomKey}_account_id", userId).orEmpty()
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
                    val connection = (URL("https://zyora-server.onrender.com/api/rooms?roomId=$encoded").openConnection() as HttpURLConnection).apply {
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
                                country = sequenceOf("countryName", "country", "countryCode").map { saved.optString(it, "").trim() }.firstOrNull { it.isNotBlank() && it != "null" }.orEmpty(),
                                locked = saved.optBoolean("isLocked", false),
                                accountId = saved.optString("accountId", stableOwnerId)
                            )
                            withContext(Dispatchers.Main) {
                                authPrefs.edit()
                                    .putBoolean("${mineRoomKey}_created", true)
                                    .putString("${mineRoomKey}_id", serverRoom.id)
                                    .putString("${mineRoomKey}_name", serverRoom.name)
                                    .putString("${mineRoomKey}_image", serverRoom.image)
                                    .putString("${mineRoomKey}_country", serverRoom.country)
                                    .putString("${mineRoomKey}_account_id", serverRoom.accountId)
                                    .putString("${mineRoomKey}_announcement", serverRoom.announcement)
                                    .putString("${mineRoomKey}_dp", serverRoom.image)
                                    .putString("room_theme_${serverRoom.id}", saved.optString("theme", saved.optString("roomTheme", "forest-night")).ifBlank { "forest-night" })
                                    .putString("room_custom_theme_${serverRoom.id}", saved.optString("customTheme", saved.optString("themeImage", "")))
                                    .putInt("room_mic_mode_${serverRoom.id}", saved.optInt("micMode", 10))
                                    .putBoolean("room_locked_${serverRoom.id}", saved.optBoolean("isLocked", false))
                                    .putString("room_password_${serverRoom.id}", saved.optString("roomPassword", ""))
                                    .apply()
                                mineCreatedRoom = serverRoom
                            }
                        } else if (connection.responseCode == 404) {
                            // A 404 from MongoDB is authoritative: this room was deleted.
                            // Clear its device cache and never migrate/re-upload it.
                            authPrefs.edit()
                                .putBoolean("${mineRoomKey}_created", false)
                                .remove("${mineRoomKey}_id")
                                .remove("${mineRoomKey}_name")
                                .remove("${mineRoomKey}_image")
                                .remove("${mineRoomKey}_country")
                                .remove("${mineRoomKey}_account_id")
                                .remove("${mineRoomKey}_announcement")
                                .remove("${mineRoomKey}_dp")
                                .apply()
                            withContext(Dispatchers.Main) { mineCreatedRoom = null }
                        }
                    } finally { connection.disconnect() }
                }
            }
        }
    }
    var tab by remember { mutableStateOf(HurryTab.HOME) }
    val messageIdentity = remember(userId) {
        authPrefs.getString("accountId", "").orEmpty().ifBlank {
            context.getSharedPreferences("hurry_user", android.content.Context.MODE_PRIVATE).let { messagePrefs ->
                messagePrefs.getString("accountId", messagePrefs.getString("userId", messagePrefs.getString("uid", userId))).orEmpty()
            }
        }.ifBlank { userId }
    }
    val unreadPrefs = remember { context.getSharedPreferences("zyora_unread_messages", android.content.Context.MODE_PRIVATE) }
    var unreadMessageCount by remember(messageIdentity) { mutableIntStateOf(unreadPrefs.getInt("unread_$messageIdentity", 0).coerceAtLeast(0)) }
    DisposableEffect(messageIdentity) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "unread_$messageIdentity") {
                unreadMessageCount = unreadPrefs.getInt(key, 0).coerceAtLeast(0)
            }
        }
        unreadPrefs.registerOnSharedPreferenceChangeListener(listener)
        unreadMessageCount = unreadPrefs.getInt("unread_$messageIdentity", 0).coerceAtLeast(0)
        onDispose { unreadPrefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
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
                            country = active?.country.orEmpty(),
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
                                selectedTab = tab,
                                unreadMessageCount = unreadMessageCount,
                                onTabSelected = { selected ->
                                    if (selected == HurryTab.MESSAGE) {
                                        unreadMessageCount = 0
                                        unreadPrefs.edit().putInt("unread_$messageIdentity", 0).apply()
                                    }
                                    tab = selected
                                },
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
                                        country = authPrefs.getString("country", "").orEmpty(),
                                        accountId = stableOwnerId.ifBlank { userId }
                                    ).also {
                                        authPrefs.edit()
                                            .putBoolean("${mineRoomKey}_created", true)
                                            .putString("${mineRoomKey}_id", it.id)
                                            .putString("${mineRoomKey}_name", it.name)
                                            .putString("${mineRoomKey}_image", it.image)
                                            .putString("${mineRoomKey}_country", it.country)
                                            .putString("${mineRoomKey}_account_id", it.accountId)
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
                                                    val connection = (URL("https://zyora-server.onrender.com/api/rooms").openConnection() as HttpURLConnection).apply {
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
                                mineSelected = homeMine,
                                keptRoom = keptRoom,
                                onOpenKeptRoom = { keptRoom?.let { openedRoom = it } },
                                onExitKeptRoom = {
                                    keptRoom = null
                                    authPrefs.edit().remove("kept_room_data").apply()
                                }
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
                                "Store" -> StorePage(onBack = { openedMePage = null }, userId = authPrefs.getString("accountId", userId).orEmpty().ifBlank { userId })
                                "Bag" -> StorePage(onBack = { openedMePage = null }, initialView = "bag", userId = authPrefs.getString("accountId", userId).orEmpty().ifBlank { userId })
                                "Seller Center" -> SellerCenter(onBack = { openedMePage = null })
                                "SVIP" -> SvipPage(onBack = { openedMePage = null })
                                "Language Setting" -> LanguagePage(selectedLanguage = languageCode, onLanguageSelected = { code -> languageCode = code; languagePrefs.edit().putString("appLanguage", code).apply() }, onBack = { openedMePage = null })
                                "Settings" -> SettingsPage(languageCode = languageCode, onBack = { openedMePage = null }, onLogout = {
                                    // Fully end the previous Firebase session and clear cached auth identity.
                                    context.stopService(Intent(context, ZyoraMessageListenerService::class.java))
                                    FirebaseAuth.getInstance().signOut()
                                    authPrefs.edit()
                                        .remove("uid")
                                        .remove("name")
                                        .remove("phone")
                                        .remove("image")
                                        .remove("accountId")
                                        .remove("email")
                                        .remove("idToken")
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
                }
                if (tab != HurryTab.HOME && openedMePage == null) {
                    Box(Modifier.fillMaxSize()) {
                        Box(Modifier.align(Alignment.BottomCenter)) {
                            HurryBottomNav(tab, languageCode = languageCode, unreadMessageCount = unreadMessageCount, onTab = { selected ->
                                if (selected == HurryTab.MESSAGE) { unreadMessageCount = 0; unreadPrefs.edit().putInt("unread_$messageIdentity", 0).apply() }
                                tab = selected
                                if (selected != HurryTab.ME) openedMePage = null
                                if (selected == HurryTab.HOME) homeMine = false
                            })
                        }
                    }
                }
            }
            LuckyBagNotificationSlider(
                currentUserId = userId,
                onGoToRoom = { roomId, roomName ->
                    openedRoom = HurryRoom(
                        id = roomId,
                        name = roomName.ifBlank { "Room" },
                        image = "",
                        country = "",
                        accountId = roomId
                    )
                }
            )
        }
    }
}

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

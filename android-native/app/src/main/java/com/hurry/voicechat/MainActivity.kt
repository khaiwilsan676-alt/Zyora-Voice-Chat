package com.hurry.voicechat

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color
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
        enableEdgeToEdge()
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
    var tab by remember { mutableStateOf(HurryTab.HOME) }
    var homeMine by remember { mutableStateOf(false) }
    var openedRoom by remember { mutableStateOf<HurryRoom?>(null) }
    var openedChat by remember { mutableStateOf<Triple<String, String, String>?>(null) }
    var openedMePage by remember { mutableStateOf<String?>(null) }
    var openedLeaderboard by remember { mutableStateOf<String?>(null) }
    var openedInvite by remember { mutableStateOf(false) }
    var openedSearch by remember { mutableStateOf(false) }
    var openedPublicProfile by remember { mutableStateOf<RoomProfileUser?>(null) }

    BackHandler(enabled = openedRoom != null || openedChat != null || openedMePage != null || openedSearch || openedPublicProfile != null) {
        when {
            openedPublicProfile != null -> openedPublicProfile = null
            openedSearch -> openedSearch = false
            openedChat != null -> openedChat = null
            openedRoom != null -> openedRoom = null
            openedMePage != null -> openedMePage = null
        }
    }

    HurryTheme {
        Box(Modifier.fillMaxSize()) {
            if (openedPublicProfile != null) {
                PublicProfile(user=openedPublicProfile!!,onBack={openedPublicProfile=null})
            } else if (openedSearch) {
                SearchPage(onBack={openedSearch=false},onOpenProfile={openedPublicProfile=it;openedSearch=false})
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
                    onClose = { openedRoom = null }
                )
            } else if (openedLeaderboard != null) {
                Leaderboard(onBack = { openedLeaderboard = null }, initialTab = openedLeaderboard!!)
            } else if (openedInvite) {
                InviteFriends(onBack = { openedInvite = false })
            } else if (openedChat != null) {
                val chat = openedChat!!
                NativeChatScreen(chat.second, chat.third, onBack = { openedChat = null })
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
                                                    val connection = (URL("https://hurry-voice-chat-lz75.onrender.com/api/rooms").openConnection() as HttpURLConnection).apply {
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
                                "Medal" -> Medal(onBack = { openedMePage = null })
                                "Store" -> StorePage(onBack = { openedMePage = null })
                                "Bag" -> StorePage(onBack = { openedMePage = null }, initialView = "bag")
                                "Seller Center" -> SellerCenter(onBack = { openedMePage = null })
                                "SVIP" -> Svip(onBack = { openedMePage = null })
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
        }
    }
}

@Composable
private fun MeNativeSubPage(title: String, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Color.White).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("‹", fontSize = 36.sp, color = Color(0xFF222222), modifier = Modifier.clickable { onBack() })
            Spacer(Modifier.width(12.dp))
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.Normal, color = Color(0xFF111827))
        }
    }
}
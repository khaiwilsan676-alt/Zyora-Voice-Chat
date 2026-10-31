package com.hurry.voicechat

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import io.socket.client.IO
import io.socket.emitter.Emitter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class RoomSettingsDataNative(
    val roomName: String = "Room",
    val announcement: String = "",
    val micMode: Int = 10,
    val theme: String = "forest-night",
    val isLocked: Boolean = false,
    val roomPassword: String = "",
    val roomDp: String = "",
    val admin: List<String> = emptyList(),
    val customTheme: String = ""
)

private data class RoomThemeChoice(val id: String, val name: String, val image: String)
private data class NativeSettingMember(val accountId: String, val name: String, val image: String)

private const val SETTINGS_API = "https://zyora-server.onrender.com"
private const val RAW_SETTINGS = "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/"

private val roomThemeChoices = listOf(
    RoomThemeChoice("forest-night", "Forest Night", RAW_SETTINGS + "1784875884052~2.jpg"),
    RoomThemeChoice("mood-light", "Moon Light", RAW_SETTINGS + "1784533036732~2.jpg"),
    RoomThemeChoice("lantern-tree", "Lantern tree", RAW_SETTINGS + "file_000000001db88211a17274a0cb143387.png"),
    RoomThemeChoice("northern-light", "Northern Light", RAW_SETTINGS + "file_000000004f388211b534832cd6b1f1a2.png"),
    RoomThemeChoice("sword-ruins", "Sword Ruins", RAW_SETTINGS + "file_0000000085a48211ae91d22ca50879e2.png"),
    RoomThemeChoice("cornfield-portal", "Cornfield Portal", RAW_SETTINGS + "file_00000000baec8211b803a01c54381503.png"),
    RoomThemeChoice("deer-moonlight", "Deer Moonlight", RAW_SETTINGS + "file_00000000dec082119e076cff4bc01164.png"),
    RoomThemeChoice("moon-light-new", "Moon light", RAW_SETTINGS + "file_0000000050288211aecacc1c105240c7.png"),
    RoomThemeChoice("dark-night-flower", "Dark night flower", RAW_SETTINGS + "file_000000006ec4821182b483795db2d94c.png")
)

// Tailwind palette
private val blue500 = Color(0xFF3B82F6)
private val blue400 = Color(0xFF60A5FA)
private val gray800 = Color(0xFF1F2937)
private val gray700 = Color(0xFF374151)
private val gray600 = Color(0xFF4B5563)
private val gray500 = Color(0xFF6B7280)
private val gray400 = Color(0xFF9CA3AF)
private val gray300 = Color(0xFFD1D5DB)
private val gray200 = Color(0xFFE5E7EB)
private val gray100 = Color(0xFFF3F4F6)
private val gray50 = Color(0xFFF9FAFB)
private val red500 = Color(0xFFEF4444)
private val yellowCard1 = Color(0xFFFDF3D0)
private val yellowCard2 = Color(0xFFFBEEC4)
private val yellowBorder = Color(0xFFFDE68A)
private val yellowInkDark = Color(0xFF8A6A1F)
private val yellowInk = Color(0xFF7A5F1C)
private val yellowBtn = Color(0xFFF0C24B)

private suspend fun cropImageToDataUrl(context: Context, uri: android.net.Uri): String = withContext(Dispatchers.IO) {
    val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        var total = 0
        while (true) {
            val n = input.read(buffer)
            if (n <= 0) break
            total += n
            require(total <= 10 * 1024 * 1024) { "Image must be smaller than 10MB" }
            out.write(buffer, 0, n)
        }
        out.toByteArray()
    } ?: error("Cannot read selected image")
    val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: error("Invalid image")
    val side = minOf(original.width, original.height)
    val x = (original.width - side) / 2
    val y = (original.height - side) / 2
    val square = Bitmap.createBitmap(original, x, y, side, side)
    val scaled = Bitmap.createScaledBitmap(square, 640, 640, true)
    val output = ByteArrayOutputStream()
    scaled.compress(Bitmap.CompressFormat.JPEG, 90, output)
    if (scaled !== square) scaled.recycle()
    if (square !== original) square.recycle()
    original.recycle()
    "data:image/jpeg;base64," + Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
}

private suspend fun saveRoomSettingsRemote(roomId: String, userId: String, data: RoomSettingsDataNative) = withContext(Dispatchers.IO) {
    if (roomId.isBlank()) return@withContext
    val payload = JSONObject()
        .put("accountId", roomId).put("roomId", roomId).put("id", roomId)
        .put("roomName", data.roomName).put("name", data.roomName).put("Room Name", data.roomName)
        .put("roomDp", data.roomDp).put("image", data.roomDp).put("Room dp", data.roomDp)
        .put("announcement", data.announcement).put("message", data.announcement)
        .put("theme", data.theme).put("customTheme", data.customTheme).put("themeImage", data.customTheme)
        .put("admin", JSONArray(data.admin))
        .put("isLocked", data.isLocked).put("roomPassword", data.roomPassword)
        .put("micMode", data.micMode)
    val connection = (URL("$SETTINGS_API/api/rooms").openConnection() as HttpURLConnection).apply {
        requestMethod = "PUT"
        connectTimeout = 20000
        readTimeout = 25000
        doOutput = true
        setRequestProperty("Content-Type", "application/json; charset=utf-8")
    }
    try {
        connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
        val code = connection.responseCode
        if (code !in 200..299) {
            val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            kotlin.error("Save failed: HTTP $code ${errorBody.take(180)}")
        }
    } finally { connection.disconnect() }

    runCatching {
        val socket = IO.socket(SETTINGS_API)
        socket.connect()
        val event = JSONObject()
            .put("roomId", roomId).put("userId", userId).put("roomOwnerId", roomId)
            .put("roomName", data.roomName).put("roomDp", data.roomDp)
            .put("announcement", data.announcement).put("micMode", data.micMode)
            .put("theme", data.theme).put("customTheme", data.customTheme).put("themeImage", data.customTheme)
            .put("isLocked", data.isLocked).put("roomPassword", data.roomPassword).put("updatedAt", System.currentTimeMillis())
        socket.emit("room_settings_update", event)
        kotlinx.coroutines.delay(350)
        socket.disconnect()
        socket.close()
    }
}

@Composable
fun RoomSettingPage(
    initial: RoomSettingsDataNative = RoomSettingsDataNative(),
    roomOwnerId: String = "",
    currentUserId: String = "",
    onBack: () -> Unit = {},
    onSave: (RoomSettingsDataNative) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var state by remember(initial) { mutableStateOf(initial) }
    var sheet by remember { mutableStateOf("") }
    var draft by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var tempMicMode by remember { mutableIntStateOf(initial.micMode) }
    var adminSearch by remember { mutableStateOf("") }
    var admins by remember(initial.admin) { mutableStateOf(initial.admin) }
    var customTheme by remember(roomOwnerId) { mutableStateOf(context.getSharedPreferences("room_settings", Context.MODE_PRIVATE).getString("customTheme_$roomOwnerId", "").orEmpty()) }
    var pendingPicker by remember { mutableStateOf("photo") }
    var saving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf("") }
    val members = remember { mutableStateListOf<NativeSettingMember>() }
    
    // DP Cropping Uri state
    var croppingUri by remember { mutableStateOf<android.net.Uri?>(null) }

    // 🔥 COMMON SAVE FUNCTION 🔥 
    val performSave = { newState: RoomSettingsDataNative ->
        scope.launch {
            saving = true
            saveError = ""
            val ready = newState.copy(
                roomName = newState.roomName.trim().ifBlank { "Room" },
                admin = admins,
                customTheme = newState.customTheme.ifBlank { customTheme }
            )
            runCatching {
                val stableRoomId = roomOwnerId.ifBlank { currentUserId }
                val permanentDp = if (ready.roomDp.startsWith("data:image/")) {
                    ProfilePersistence.uploadDpToGitHub(ready.roomDp, "$stableRoomId-room", "room-dp")
                        ?: error("Room DP upload failed. Please try again.")
                } else ready.roomDp
                val permanentTheme = if (ready.customTheme.startsWith("data:image/")) {
                    ProfilePersistence.uploadDpToGitHub(ready.customTheme, "$stableRoomId-theme", "room-theme")
                        ?: error("Room theme upload failed. Please try again.")
                } else ready.customTheme
                val permanent = ready.copy(roomDp = permanentDp, customTheme = permanentTheme)
                saveRoomSettingsRemote(stableRoomId, currentUserId, permanent)
                context.getSharedPreferences("room_settings", Context.MODE_PRIVATE).edit()
                    .putString("roomDp_$roomOwnerId", permanent.roomDp)
                    .putInt("micMode_$roomOwnerId", permanent.micMode)
                    .putString("theme_$roomOwnerId", permanent.theme)
                    .putString("customTheme_$roomOwnerId", permanent.customTheme)
                    .apply()
                permanent
            }.onSuccess { permanent ->
                state = permanent
                onSave(permanent)
            }.onFailure { saveError = it.message ?: "Network error. Try again." }
            saving = false
        }
    }

    LaunchedEffect(adminSearch, sheet) {
        val q = adminSearch.trim()
        if (sheet != "admin" || q.isBlank()) return@LaunchedEffect
        delay(180)
        withContext(Dispatchers.IO) {
            runCatching {
                val encoded = java.net.URLEncoder.encode(q, "UTF-8")
                val conn = (URL("$SETTINGS_API/api/users?search=$encoded&accountId=$encoded&q=$encoded").openConnection() as HttpURLConnection).apply {
                    connectTimeout = 2500
                    readTimeout = 2500
                }
                try {
                    if (conn.responseCode !in 200..299) return@runCatching emptyList<NativeSettingMember>()
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val root = body.trim()
                    val arr = if (root.startsWith("[")) JSONArray(root) else {
                        val obj = JSONObject(root)
                        obj.optJSONArray("users") ?: obj.optJSONArray("results")
                            ?: JSONArray().apply { (obj.optJSONObject("user"))?.let { put(it) } }
                    }
                    (0 until arr.length()).mapNotNull { i ->
                        val u = arr.optJSONObject(i) ?: return@mapNotNull null
                        val id = u.optString("accountId").takeUnless { it == "null" }.orEmpty()
                            .ifBlank { u.optString("accountNumber").takeUnless { it == "null" }.orEmpty() }
                            .ifBlank { u.optString("displayUserNumber").takeUnless { it == "null" }.orEmpty() }
                            .ifBlank { u.optString("uid").takeUnless { it == "null" }.orEmpty() }
                            .ifBlank { u.optString("id").takeUnless { it == "null" }.orEmpty() }
                        if (id.isBlank()) null else NativeSettingMember(
                            id,
                            u.optString("name").takeUnless { it == "null" }.orEmpty()
                                .ifBlank { u.optString("displayName").ifBlank { u.optString("userName", "User") } },
                            u.optString("image").takeUnless { it == "null" }.orEmpty()
                                .ifBlank { u.optString("photoURL").ifBlank { u.optString("avatar", "") } }
                        )
                    }
                } finally { conn.disconnect() }
            }.getOrDefault(emptyList())
        }.let { found ->
            if (sheet == "admin" && adminSearch.trim() == q) {
                found.forEach { result ->
                    val index = members.indexOfFirst { it.accountId == result.accountId }
                    if (index >= 0) members[index] = result else members.add(0, result)
                }
            }
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            if (pendingPicker == "photo") {
                croppingUri = uri
            } else {
                scope.launch {
                    runCatching { cropImageToDataUrl(context, uri) }
                        .onSuccess { dataUrl ->
                            customTheme = dataUrl
                            performSave(state.copy(theme = "custom", customTheme = dataUrl))
                            sheet = ""
                        }
                        .onFailure { saveError = it.message ?: "Could not load image" }
                }
            }
        }
    }

    val storeRoomThemes = remember { mutableStateListOf<RoomThemeChoice>() }
    LaunchedEffect(currentUserId) {
        if (currentUserId.isBlank()) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            runCatching {
                val encoded = java.net.URLEncoder.encode(currentUserId, "UTF-8")
                val conn = (URL("$SETTINGS_API/api/store/inventory/$encoded").openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8000
                    readTimeout = 8000
                }
                try {
                    if (conn.responseCode !in 200..299) emptyList()
                    else {
                        val root = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                        val arr = root.optJSONArray("items") ?: JSONArray()
                        (0 until arr.length()).mapNotNull { index ->
                            val item = arr.optJSONObject(index) ?: return@mapNotNull null
                            val id = item.optString("itemId")
                            val expiry = item.optLong("expiresAt")
                            if (item.optString("tab") == "Theme" && item.optBoolean("equipped") && id.isNotBlank() && expiry > System.currentTimeMillis()) {
                                val image = item.optString("image")
                                if (image.isNotBlank()) RoomThemeChoice(id, item.optString("name", id), RAW_SETTINGS + image) else null
                            } else null
                        }
                    }
                } finally { conn.disconnect() }
            }.getOrDefault(emptyList())
        }.let { found ->
            storeRoomThemes.clear()
            storeRoomThemes.addAll(found)
        }
    }

    val themeItems = remember(customTheme, storeRoomThemes.toList()) {
        (if (customTheme.isNotBlank()) listOf(RoomThemeChoice("custom", "Custom", customTheme)) else emptyList()) +
            roomThemeChoices + storeRoomThemes.toList()
    }

    DisposableEffect(roomOwnerId) {
        if (roomOwnerId.isBlank()) return@DisposableEffect onDispose { }
        val socket = runCatching { IO.socket(SETTINGS_API) }.getOrNull() ?: return@DisposableEffect onDispose { }
        val listener = Emitter.Listener { args ->
            val payload = args.firstOrNull() as? JSONObject ?: return@Listener
            if (payload.optString("roomId") != roomOwnerId) return@Listener
            val users = payload.optJSONArray("users") ?: JSONArray()
            val parsed = (0 until users.length()).mapNotNull { i ->
                val item = users.optJSONObject(i) ?: return@mapNotNull null
                val id = item.optString("accountId", item.optString("userId", item.optString("appLongId", "")))
                if (id.isBlank()) null else NativeSettingMember(
                    id,
                    item.optString("name", item.optString("userName", "User")),
                    item.optString("image", item.optString("dp", item.optString("avatar", "")))
                )
            }
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                members.clear(); members.addAll(parsed)
            }
        }
        socket.on("room_members_list", listener)
        socket.connect()
        socket.emit("get_room_members", JSONObject().put("roomId", roomOwnerId))
        onDispose {
            socket.off("room_members_list", listener)
            socket.disconnect(); socket.close()
        }
    }

    Box(Modifier.fillMaxSize().background(Color.White)) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().background(Color.White)
                    .statusBarsPadding()
                    .padding(start = 8.dp, end = 8.dp, top = 16.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.ArrowBack, "Back", tint = gray800, modifier = Modifier.size(24.dp)) }
                
                Text(
                    "Room Setting",
                    Modifier.weight(1f),
                    fontSize = 18.sp, fontWeight = FontWeight.Bold, color = gray800, textAlign = TextAlign.Center
                )
                
                Spacer(Modifier.size(40.dp))
            }

            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 24.dp)
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        Modifier.size(96.dp).clip(RoundedCornerShape(16.dp))
                            .border(2.dp, gray200, RoundedCornerShape(16.dp))
                            .background(gray100),
                        contentAlignment = Alignment.Center
                    ) {
                        if (state.roomDp.isNotBlank()) AsyncImage(
                            model = state.roomDp, contentDescription = "Room photo",
                            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
                        ) else Text("Room", color = gray400, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier.clip(CircleShape)
                            .border(1.dp, gray300, CircleShape)
                            .background(Color.White)
                            .clickable { pendingPicker = "photo"; picker.launch("image/*") }
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Edit, null, tint = gray700, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Edit Room photo", color = gray700, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }

                SettingRow("Room Name", state.roomName) { draft = state.roomName; sheet = "name" }
                SettingRow("Room Announcement", state.announcement) { draft = state.announcement; sheet = "announcement" }
                SettingRow("Theme", themeItems.firstOrNull { it.id == state.theme }?.name ?: "") { sheet = "theme" }
                SettingRow("Admin", "${admins.size} Selected") { adminSearch = ""; sheet = "admin" }
                SettingRow("Lock Room", if (state.isLocked) "Locked" else "", red = state.isLocked) {
                    password = if (state.isLocked) state.roomPassword else ""; sheet = "lock"
                }
                
                Box(Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Mic Mode", Modifier.weight(1f),
                            fontSize = 14.sp, fontWeight = FontWeight.Medium, color = gray600
                        )
                        Row(
                            Modifier.clip(RoundedCornerShape(8.dp))
                                .clickable { tempMicMode = state.micMode; sheet = "mic" }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Mic ${state.micMode}",
                                fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = gray800
                            )
                            Icon(Icons.Default.ChevronRight, null, tint = gray400, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                if (saveError.isNotBlank()) {
                    Text(saveError, color = red500, fontSize = 12.sp, modifier = Modifier.padding(vertical = 8.dp), textAlign = TextAlign.Center)
                }
            }
        }
    }

    when (sheet) {
        "name" -> CenteredModal(scrimAlpha = 0.5f, onDismiss = { sheet = "" }) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                    .clip(RoundedCornerShape(16.dp)).background(Color.White).padding(vertical = 16.dp)
            ) {
                ModalHeader("Room name", onBack = { sheet = "" }, horizontal = 12.dp)
                Spacer(Modifier.height(12.dp))
                RoundedInput(
                    value = draft,
                    onChange = { draft = it.take(30) },
                    placeholder = "Please enter room name",
                    counter = "${draft.length}/30",
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                Spacer(Modifier.height(20.dp))
                PillButton("Confirm") { performSave(state.copy(roomName = draft.trim().ifBlank { "Room" })); sheet = "" }
            }
        }
        "announcement" -> CenteredModal(scrimAlpha = 0.5f, onDismiss = { sheet = "" }) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                    .clip(RoundedCornerShape(16.dp)).background(Color.White).padding(vertical = 16.dp)
            ) {
                ModalHeader("Room announcement", onBack = { sheet = "" }, horizontal = 12.dp)
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(gray100)
                ) {
                    BasicTextField(
                        value = draft,
                        onValueChange = { draft = it.take(200) },
                        modifier = Modifier.fillMaxWidth()
                            .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 32.dp)
                            .heightIn(min = 180.dp),
                        textStyle = TextStyle(fontSize = 14.sp, color = gray800),
                        decorationBox = { inner ->
                            Box {
                                if (draft.isEmpty()) Text(
                                    "Please enter room announcement",
                                    color = gray400, fontSize = 14.sp
                                )
                                inner()
                            }
                        }
                    )
                    Text(
                        "${draft.length}/200",
                        color = gray500, fontSize = 12.sp,
                        modifier = Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 8.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))
                PillButton("Confirm") { performSave(state.copy(announcement = draft)); sheet = "" }
            }
        }
        "lock" -> CenteredModal(scrimAlpha = 0.3f, onDismiss = { sheet = "" }) {
            Box(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp) 
                    .clip(RoundedCornerShape(16.dp)).background(Color.White).padding(24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Set Room Password", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = gray800)
                    Spacer(Modifier.height(24.dp))
                    PasswordInputBoxes(password) { password = it }
                    Spacer(Modifier.height(24.dp))
                    if (state.isLocked && password == state.roomPassword) {
                        PillButton(
                            "Unlock Room",
                            color = red500,
                            onClick = {
                                performSave(state.copy(isLocked = false, roomPassword = ""))
                                password = ""; sheet = ""
                            }
                        )
                    } else {
                        PillButton(
                            if (state.isLocked) "Update Password" else "Set Password",
                            enabled = password.length == 4,
                            onClick = {
                                if (password.length == 4) {
                                    performSave(state.copy(isLocked = true, roomPassword = password))
                                    sheet = ""
                                }
                            }
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Box(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .clickable { sheet = ""; password = "" }.padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) { Text("Cancel", color = gray500, fontSize = 14.sp, fontWeight = FontWeight.Medium) }
                }
            }
        }
        "theme" -> ThemeFullScreen(
            current = state.theme,
            items = themeItems,
            hasCustom = customTheme.isNotBlank(),
            onBack = { sheet = "" },
            onPick = { id -> performSave(state.copy(theme = id)); sheet = "" },
            onUploadCustom = { pendingPicker = "theme"; picker.launch("image/*") }
        )
        
        // 👉 MIC SHEET HEIGHT 40% SE 50% KAR DI HAI (10% INCREASE) 👈
        "mic" -> BottomSheetModal(scrimAlpha = 0.3f, heightFraction = 0.50f, onDismiss = { sheet = "" }) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(36.dp).clip(CircleShape).clickable { sheet = "" },
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Default.ArrowBack, null, tint = gray800, modifier = Modifier.size(20.dp)) }
                    Text(
                        "Select Mic Mode", Modifier.weight(1f),
                        fontSize = 16.sp, fontWeight = FontWeight.Bold, color = gray800, textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.width(36.dp))
                }
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(5, 10, 15).forEach { mode ->
                            val asset = when (mode) {
                                5 -> RAW_SETTINGS + "Screenshot_2026-10-26-13-33-28-361_com.hawa.app.jpg"
                                10 -> RAW_SETTINGS + "Screenshot_2026-10-26-13-33-19-325_com.hawa.app.jpg"
                                else -> RAW_SETTINGS + "Screenshot_2026-10-26-13-33-08-252_com.hawa.app.jpg"
                            }
                            val selected = tempMicMode == mode
                            Column(
                                Modifier.weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .then(
                                        if (selected) Modifier.border(2.dp, blue500, RoundedCornerShape(12.dp)) else Modifier
                                    )
                                    .clickable { tempMicMode = mode }
                                    .padding(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                AsyncImage(
                                    model = asset, contentDescription = "Mic $mode",
                                    modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                                    contentScale = ContentScale.Fit
                                )
                                Text(
                                    "Mic $mode", fontSize = 14.sp,
                                    color = if (selected) blue500 else gray700,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                )
                            }
                        }
                    }
                }
                Box(
                    Modifier.fillMaxWidth()
                        .drawBehind {
                            drawLine(
                                color = gray100, start = androidx.compose.ui.geometry.Offset(0f, 0f),
                                end = androidx.compose.ui.geometry.Offset(size.width, 0f),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .navigationBarsPadding()
                ) {
                    PillButton("Confirm", onClick = { performSave(state.copy(micMode = tempMicMode)); sheet = "" })
                }
            }
        }
        "admin" -> BottomSheetModal(scrimAlpha = 0.35f, heightFraction = 0.5f, dark = false, onDismiss = { 
            performSave(state.copy(admin = admins))
            sheet = "" 
        }) {
            Column(Modifier.fillMaxSize().navigationBarsPadding().background(Color.White)) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 12.dp, end = 16.dp, top = 8.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(38.dp).clip(CircleShape).clickable { 
                            performSave(state.copy(admin = admins))
                            sheet = "" 
                        },
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Default.ArrowBack, "Back", tint = gray800, modifier = Modifier.size(22.dp)) }
                    Text(
                        "Admin", Modifier.weight(1f),
                        fontSize = 16.sp, fontWeight = FontWeight.Bold, color = gray800, textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.size(38.dp))
                }
                Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
                    Box(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                            .background(gray100).padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        BasicTextField(
                            value = adminSearch,
                            onValueChange = { adminSearch = it.filter { ch -> ch.isDigit() } },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(fontSize = 13.sp, color = gray800),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            decorationBox = { inner ->
                                Box {
                                    if (adminSearch.isEmpty()) Text("Search user by ID", color = gray400, fontSize = 13.sp)
                                    inner()
                                }
                            }
                        )
                    }
                }
                val filtered = members.filter {
                    adminSearch.isBlank() || it.accountId.contains(adminSearch, true)
                }
                LazyColumn(
                    Modifier.weight(1f).padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered, key = { it.accountId }) { member ->
                        val isAdmin = admins.contains(member.accountId)
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = member.image.ifBlank { RAW_SETTINGS + "default-avatar.png" },
                                contentDescription = member.name,
                                modifier = Modifier.size(38.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                                Text(member.name, color = gray800, fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("ID: ${member.accountId}", color = gray500, fontSize = 10.sp)
                            }
                            Box(
                                Modifier.clip(RoundedCornerShape(7.dp))
                                    .background(if (isAdmin) gray200 else blue500)
                                    .clickable {
                                        admins = if (isAdmin) admins.filterNot { it == member.accountId }
                                        else if (admins.size < 20) admins + member.accountId else admins
                                    }
                                    .padding(horizontal = 15.dp, vertical = 7.dp)
                            ) {
                                Text(if (isAdmin) "Added" else "Add",
                                    color = if (isAdmin) gray700 else Color.White,
                                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    if (filtered.isEmpty()) item {
                        Box(Modifier.fillMaxWidth().padding(22.dp), contentAlignment = Alignment.Center) {
                            Text(if (adminSearch.isBlank()) "Search by ID to add an admin" else "No user found",
                                color = gray500, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
        "admin-help" -> CenteredModal(scrimAlpha = 0.6f, onDismiss = { sheet = "admin" }) {
            Box(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp).widthIn(max = 320.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFFFFDF3))
                    .border(1.dp, yellowBorder, RoundedCornerShape(16.dp))
            ) {
                Column {
                    Box(Modifier.fillMaxWidth().background(yellowCard1)) {
                        Text(
                            "Room Admin",
                            Modifier.fillMaxWidth().padding(vertical = 14.dp),
                            color = yellowInkDark, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                    Column(Modifier.background(yellowCard2).padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text(
                            "Admin permissions include:",
                            color = yellowInkDark, fontSize = 14.sp, fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "locking the mic, muting the mic, kicking someone out, inviting someone, removing someone from a seat, and playing music.",
                            color = yellowInk, fontSize = 12.sp, lineHeight = 18.sp
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Each room has 5 admins. You can increase the admin limit in the following ways:",
                            color = yellowInk, fontSize = 12.sp, lineHeight = 18.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "1. By leveling up your ID, you can add 5 more admins; you can increase the total number of admins up to 20.",
                            color = yellowInk, fontSize = 12.sp, lineHeight = 18.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "2. If you wish to appoint more admins, follow the rules below to add admins for free.",
                            color = yellowInk, fontSize = 12.sp, lineHeight = 18.sp
                        )
                        Spacer(Modifier.height(12.dp))
                        Column(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                                .border(1.dp, yellowBorder, RoundedCornerShape(8.dp))
                        ) {
                            listOf(
                                "Level 25" to "+5 admins",
                                "Level 30" to "+5 admins",
                                "Level 35" to "+5 admins"
                            ).forEachIndexed { idx, (lvl, cnt) ->
                                Row(
                                    Modifier.fillMaxWidth()
                                        .background(if (idx % 2 == 0) Color(0xFFFAEEC2) else yellowCard1)
                                ) {
                                    Text(
                                        lvl, Modifier.weight(1f).padding(8.dp),
                                        color = yellowInk, fontSize = 11.sp
                                    )
                                    Text(
                                        cnt, Modifier.weight(1f).padding(8.dp),
                                        color = yellowInk, fontSize = 11.sp, textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        PillButton(
                            "Close",
                            color = yellowBtn,
                            textColor = yellowInk,
                            onClick = { sheet = "admin" }
                        )
                    }
                }
            }
        }
    }

    // 🔥 NEW CROP IMAGE DIALOG WITH CANCEL & SAVE BUTTONS AND 10x10 CROP SQUARE 🔥
    if (croppingUri != null) {
        Dialog(
            onDismissRequest = { croppingUri = null },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            var scale by remember { mutableFloatStateOf(1f) }
            var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
            var bitmap by remember { mutableStateOf<Bitmap?>(null) }

            LaunchedEffect(croppingUri) {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(croppingUri!!)?.use {
                        bitmap = BitmapFactory.decodeStream(it)
                    }
                }
            }

            Box(Modifier.fillMaxSize().background(Color.Black)) {
                if (bitmap != null) {
                    val transformableState = rememberTransformableState { zoomChange, offsetChange, _ ->
                        scale = (scale * zoomChange).coerceIn(0.5f, 5f)
                        offset += offsetChange
                    }

                    Box(Modifier.fillMaxSize().transformable(transformableState), contentAlignment = Alignment.Center) {
                        Image(
                            bitmap = bitmap!!.asImageBitmap(),
                            contentDescription = "Crop Image",
                            modifier = Modifier.graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = offset.x
                                translationY = offset.y
                            },
                            contentScale = ContentScale.Fit
                        )

                        Box(
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .border(2.dp, Color.White)
                        )
                    }
                }

                Row(
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Cancel",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { croppingUri = null }
                            .padding(8.dp)
                    )
                    Text(
                        text = "Save",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable {
                                if (bitmap != null) {
                                    scope.launch {
                                        val base64 = withContext(Dispatchers.IO) {
                                            try {
                                                val b = bitmap!!
                                                val size = minOf(b.width, b.height)
                                                val cropSize = (size / scale).toInt().coerceIn(1, size)
                                                val xOffsetScaled = (offset.x).toInt()
                                                val yOffsetScaled = (offset.y).toInt()

                                                val startX = ((b.width - cropSize) / 2 - xOffsetScaled).coerceIn(0, b.width - cropSize)
                                                val startY = ((b.height - cropSize) / 2 - yOffsetScaled).coerceIn(0, b.height - cropSize)

                                                val cropped = Bitmap.createBitmap(b, startX, startY, cropSize, cropSize)
                                                val scaled = Bitmap.createScaledBitmap(cropped, 640, 640, true)
                                                val out = ByteArrayOutputStream()
                                                scaled.compress(Bitmap.CompressFormat.JPEG, 90, out)
                                                "data:image/jpeg;base64," + Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
                                            } catch (e: Exception) {
                                                cropImageToDataUrl(context, croppingUri!!)
                                            }
                                        }
                                        performSave(state.copy(roomDp = base64))
                                        croppingUri = null
                                    }
                                }
                            }
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}

// ═══════════════════════════ HELPERS ═══════════════════════════

@Composable
private fun SettingRow(
    title: String,
    value: String,
    red: Boolean = false,
    onClick: () -> Unit
) {
    Box(Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
        Row(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, Modifier.weight(1f), fontSize = 14.sp, fontWeight = FontWeight.Medium, color = gray600)
            if (value.isNotBlank()) {
                Text(
                    value, fontSize = 12.sp,
                    color = if (red) red500 else gray400,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 140.dp)
                )
            }
            Icon(
                Icons.Default.ChevronRight, null, tint = gray400,
                modifier = Modifier.padding(start = 8.dp).size(16.dp)
            )
        }
    }
}

@Composable
private fun ModalHeader(title: String, onBack: () -> Unit, horizontal: Dp = 8.dp) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = horizontal, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(36.dp).clip(CircleShape).clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Default.ArrowBack, null, tint = gray800, modifier = Modifier.size(20.dp)) }
        Text(
            title, Modifier.weight(1f),
            fontSize = 16.sp, fontWeight = FontWeight.Bold, color = gray800, textAlign = TextAlign.Center
        )
        Spacer(Modifier.width(36.dp))
    }
}

@Composable
private fun RoundedInput(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String = "",
    counter: String = "",
    modifier: Modifier = Modifier
) {
    Box(modifier.fillMaxWidth().clip(CircleShape).background(gray100)) {
        BasicTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth().padding(
                start = 20.dp,
                end = if (counter.isNotEmpty()) 64.dp else 20.dp,
                top = 16.dp,
                bottom = 16.dp
            ),
            textStyle = TextStyle(fontSize = 16.sp, color = gray800),
            singleLine = true,
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty() && placeholder.isNotEmpty())
                        Text(placeholder, color = gray400, fontSize = 16.sp)
                    inner()
                }
            }
        )
        if (counter.isNotEmpty()) Text(
            counter, color = gray500, fontSize = 12.sp,
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 20.dp)
        )
    }
}

@Composable
private fun PillButton(
    label: String,
    color: Color = blue500,
    textColor: Color = Color.White,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp).height(48.dp)
            .clip(CircleShape)
            .background(if (enabled) color else gray300)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
            color = if (enabled) textColor else gray400
        )
    }
}

@Composable
private fun PasswordInputBoxes(value: String, onChange: (String) -> Unit) {
    val focusReqs = remember { List(4) { FocusRequester() } }
    var focusedIdx by remember { mutableIntStateOf(-1) }
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
    ) {
        (0..3).forEach { index ->
            val char = value.getOrNull(index)?.toString() ?: ""
            val isFocused = focusedIdx == index
            Box(
                Modifier.size(56.dp)
                    .border(
                        2.dp,
                        if (isFocused) blue500 else gray300,
                        RoundedCornerShape(8.dp)
                    )
                    .background(Color.White, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                BasicTextField(
                    value = char,
                    onValueChange = { input ->
                        val digits = input.filter(Char::isDigit)
                        when {
                            digits.isEmpty() -> {
                                if (index < value.length) onChange(value.removeRange(index, index + 1))
                                else if (index > 0 && value.isNotEmpty()) {
                                    onChange(value.dropLast(1))
                                    focusReqs[index - 1].requestFocus()
                                }
                            }
                            digits.length > 1 -> {
                                val newValue = (value.take(index) + digits).take(4)
                                onChange(newValue)
                                focusReqs[minOf(newValue.length, 3)].requestFocus()
                            }
                            else -> {
                                val sb = StringBuilder(value)
                                while (sb.length < index) sb.append(' ')
                                if (index < sb.length) sb.setCharAt(index, digits[0]) else sb.append(digits[0])
                                onChange(sb.toString().trimEnd().take(4))
                                if (index < 3) focusReqs[index + 1].requestFocus()
                            }
                        }
                    },
                    modifier = Modifier.width(40.dp)
                        .focusRequester(focusReqs[index])
                        .onFocusChanged {
                            if (it.isFocused) focusedIdx = index
                            else if (focusedIdx == index) focusedIdx = -1
                        },
                    textStyle = TextStyle(
                        textAlign = TextAlign.Center, fontSize = 24.sp,
                        fontWeight = FontWeight.Bold, color = gray800
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true
                )
            }
        }
    }
}

@Composable
private fun CenteredModal(
    scrimAlpha: Float = 0.5f,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = scrimAlpha))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { }
            ) { content() }
        }
    }
}

@Composable
private fun BottomSheetModal(
    scrimAlpha: Float = 0.5f,
    heightFraction: Float = 0.85f,
    dark: Boolean = false,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    val screenH = LocalConfiguration.current.screenHeightDp.dp
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = scrimAlpha))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                Modifier.fillMaxWidth().heightIn(max = screenH * heightFraction)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(if (dark) Color.Black else Color.White)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { }
            ) { content() }
        }
    }
}

@Composable
private fun ThemeFullScreen(
    current: String,
    items: List<RoomThemeChoice>,
    hasCustom: Boolean,
    onBack: () -> Unit,
    onPick: (String) -> Unit,
    onUploadCustom: () -> Unit
) {
    var selectedTheme by remember(current) { mutableStateOf(current) }
    
    Dialog(
        onDismissRequest = onBack,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Column(Modifier.fillMaxSize().background(Color.White)) {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.ArrowBack, null, tint = gray800, modifier = Modifier.size(24.dp)) }
                
                Text(
                    "Room Theme", Modifier.weight(1f),
                    fontSize = 18.sp, fontWeight = FontWeight.Bold, color = gray800, textAlign = TextAlign.Center
                )
                
                Box(
                    Modifier.defaultMinSize(minWidth = 40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onPick(selectedTheme) }
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Save",
                        color = Color.Black,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 16.dp)
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 640.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!hasCustom) {
                        item {
                            Box(
                                Modifier.fillMaxWidth().aspectRatio(1f)
                                    .clip(RoundedCornerShape(13.dp))
                                    .dashedBorder(gray300, 1.dp, 13.dp)
                                    .background(gray50)
                                    .clickable(onClick = onUploadCustom),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text("+", color = gray400, fontSize = 24.sp)
                                    Text(
                                        "Custom", color = gray500, fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                    gridItems(items) { theme ->
                        Column(
                            Modifier.fillMaxWidth().clickable { selectedTheme = theme.id }
                        ) {
                            Box(
                                Modifier.fillMaxWidth()
                                    .clip(RoundedCornerShape(13.dp))
                                    .border(2.dp, if (selectedTheme == theme.id) blue400 else Color.Transparent, RoundedCornerShape(13.dp))
                            ) {
                                AsyncImage(
                                    model = theme.image, contentDescription = theme.name,
                                    modifier = Modifier.fillMaxWidth().wrapContentHeight().clip(RoundedCornerShape(13.dp)), 
                                    contentScale = ContentScale.FillWidth 
                                )
                                
                                if (selectedTheme == theme.id) {
                                    Box(
                                        Modifier
                                            .matchParentSize()
                                            .clip(RoundedCornerShape(13.dp))
                                            .background(Color.Black.copy(alpha = 0.1f))
                                    )
                                }

                                if (theme.id == "custom") {
                                    Box(
                                        Modifier.align(Alignment.TopStart).padding(6.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color.Black.copy(alpha = 0.6f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) { Text("Custom", color = Color.White, fontSize = 10.sp) }
                                }
                            }
                            Text(
                                theme.name, fontSize = 12.sp, color = gray700,
                                fontWeight = FontWeight.Medium, textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 4.dp),
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

// Dashed border helper
private fun Modifier.dashedBorder(color: Color, width: Dp, cornerRadius: Dp): Modifier = this.drawBehind {
    val strokeWidth = width.toPx()
    val radiusPx = cornerRadius.toPx()
    val path = Path().apply {
        addRoundRect(
            RoundRect(
                left = strokeWidth / 2, top = strokeWidth / 2,
                right = size.width - strokeWidth / 2, bottom = size.height - strokeWidth / 2,
                cornerRadius = CornerRadius(radiusPx, radiusPx)
            )
        )
    }
    drawPath(
        path = path, color = color,
        style = Stroke(
            width = strokeWidth,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
        )
    )
}

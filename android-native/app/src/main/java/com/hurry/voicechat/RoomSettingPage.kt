package com.hurry.voicechat

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.google.firebase.auth.FirebaseAuth
import com.google.android.gms.tasks.Tasks
import java.util.concurrent.TimeUnit
import io.socket.client.IO
import io.socket.emitter.Emitter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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

private data class RoomThemeChoice(val id: String, val name: String, val image: String, val expiresAt: Long = 0L)
private data class NativeSettingMember(val accountId: String, val name: String, val image: String)

private const val SETTINGS_API = "https://zyora-server.onrender.com"
private const val RAW_SETTINGS = "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/"
private const val STORE_THEME_ASSET_BASE = "https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/"

private val roomThemeChoices = listOf(
    RoomThemeChoice("forest-night", "Forest Night", RAW_SETTINGS + "1784875884052~2.jpg"),
    RoomThemeChoice("mood-light", "Moon Light", RAW_SETTINGS + "1784533036732~2.jpg"),
    RoomThemeChoice("lantern-tree", "Lantern tree", RAW_SETTINGS + "file_000000001db88211a17274a0cb143387.png"),
    RoomThemeChoice("northern-light", "Northern Light", RAW_SETTINGS + "file_000000004f388211b534832cd6b1f1a2.png"),
    RoomThemeChoice("sword-ruins", "Sword Ruins", RAW_SETTINGS + "file_0000000085a48211ae91d22ca50879e2.png"),
    RoomThemeChoice("cornfield-portal", "Cornfield Portal", RAW_SETTINGS + "file_00000000baec8211b803a01c54381503.png"),
    RoomThemeChoice("deer-moonlight", "Deer Moonlight", RAW_SETTINGS + "file_00000000dec082119e076cff4bc01164.png")
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
    scaled.compress(Bitmap.CompressFormat.JPEG, 78, output)
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
            val error = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            error("Save failed: HTTP $code ${error.take(180)}")
        }
    } finally { connection.disconnect() }
}

@Composable
fun RoomSettingPage(
    initial: RoomSettingsDataNative = RoomSettingsDataNative(),
    roomOwnerId: String = "",
    currentUserId: String = "",
    roomMembers: List<RoomInfoFollower> = emptyList(),
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
    var pendingCustomThemeImage by remember { mutableStateOf("") }
    var pendingCustomThemeName by remember { mutableStateOf("") }
    var customThemeUploading by remember { mutableStateOf(false) }
    var pendingPicker by remember { mutableStateOf("photo") }
    var saving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf("") }
    var members by remember { mutableStateOf<List<NativeSettingMember>>(emptyList()) }

    LaunchedEffect(roomMembers, roomOwnerId, currentUserId) {
        members = roomMembers
            .asSequence()
            .filter { it.accountId.isNotBlank() && it.accountId != roomOwnerId && it.accountId != currentUserId }
            .distinctBy { it.accountId }
            .map { NativeSettingMember(it.accountId, it.name, it.image) }
            .toList()
    }

    LaunchedEffect(sheet, roomOwnerId, members.isEmpty()) {
        if (sheet != "admin-members" || roomOwnerId.isBlank() || members.isNotEmpty()) return@LaunchedEffect
        val refreshed = withContext(Dispatchers.IO) {
            runCatching {
                val encodedRoom = java.net.URLEncoder.encode(roomOwnerId, "UTF-8")
                val connection = (URL("$SETTINGS_API/api/room-follows?roomId=$encodedRoom").openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8000
                    readTimeout = 8000
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("Cache-Control", "no-cache")
                }
                try {
                    if (connection.responseCode !in 200..299) error("Room members unavailable")
                    val root = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                    val array = root.optJSONArray("followers") ?: JSONArray()
                    (0 until array.length()).mapNotNull { index ->
                        val item = array.optJSONObject(index) ?: return@mapNotNull null
                        val id = item.optString("userId").trim()
                        if (id.isBlank() || id == roomOwnerId || id == currentUserId) return@mapNotNull null
                        NativeSettingMember(
                            id,
                            item.optString("followerName").ifBlank { "User" },
                            item.optString("followerImage")
                        )
                    }.distinctBy { it.accountId }
                } finally {
                    connection.disconnect()
                }
            }.getOrDefault(emptyList())
        }
        if (sheet == "admin-members" && refreshed.isNotEmpty()) {
            val merged = LinkedHashMap<String, NativeSettingMember>()
            members.forEach { merged[it.accountId] = it }
            refreshed.forEach { merged[it.accountId] = it }
            members = merged.values.toList()
        }
    }

    // 🔥 COMMON SAVE FUNCTION 🔥
    val performSave = save@{ newState: RoomSettingsDataNative ->
        if (saving) return@save
        saving = true
        saveError = ""
        val ready = newState.copy(
            roomName = newState.roomName.trim().ifBlank { "Room" },
            admin = admins,
            customTheme = newState.customTheme.ifBlank { customTheme }
        )
        val stableRoomId = roomOwnerId.ifBlank { currentUserId }
        // Scalar settings are applied locally and broadcast immediately. Image data URLs
        // wait for permanent upload so a large base64 payload is never sent over Socket.IO.
        val canApplyOptimistically = !ready.roomDp.startsWith("data:image/") &&
            !ready.customTheme.startsWith("data:image/")
        if (canApplyOptimistically) {
            state = ready
            context.getSharedPreferences("room_settings", Context.MODE_PRIVATE).edit()
                .putString("roomDp_$stableRoomId", ready.roomDp)
                .putInt("micMode_$stableRoomId", ready.micMode)
                .putString("theme_$stableRoomId", ready.theme)
                .putString("customTheme_$stableRoomId", ready.customTheme)
                .putBoolean("room_locked_$stableRoomId", ready.isLocked)
                .putString("room_password_$stableRoomId", ready.roomPassword)
                .apply()
            onSave(ready)
        }
        scope.launch {
            runCatching {
                // Upload room DP and custom theme concurrently; each upload already runs on IO.
                val (permanentDp, permanentTheme) = coroutineScope {
                    val dpJob = async {
                        if (ready.roomDp.startsWith("data:image/")) {
                            ProfilePersistence.uploadImageToMongoDB(ready.roomDp, "$stableRoomId-room", "room-dp")
                                ?: error("Room DP upload failed. Check MongoDB image storage and try again.")
                        } else ready.roomDp
                    }
                    val themeJob = async {
                        if (ready.customTheme.startsWith("data:image/")) {
                            ProfilePersistence.uploadImageToMongoDB(ready.customTheme, "$stableRoomId-theme", "room-theme")
                                ?: error("Room theme upload failed. Check MongoDB image storage and try again.")
                        } else ready.customTheme
                    }
                    dpJob.await() to themeJob.await()
                }
                val permanent = ready.copy(roomDp = permanentDp, customTheme = permanentTheme)
                saveRoomSettingsRemote(stableRoomId, currentUserId, permanent)
                context.getSharedPreferences("room_settings", Context.MODE_PRIVATE).edit()
                    .putString("roomDp_$stableRoomId", permanent.roomDp)
                    .putInt("micMode_$stableRoomId", permanent.micMode)
                    .putString("theme_$stableRoomId", permanent.theme)
                    .putString("customTheme_$stableRoomId", permanent.customTheme)
                    .putBoolean("room_locked_$stableRoomId", permanent.isLocked)
                    .putString("room_password_$stableRoomId", permanent.roomPassword)
                    .apply()
                withContext(Dispatchers.IO) {
                    NativeIndexDatabase(context).saveRoomLock(stableRoomId, permanent.isLocked, permanent.roomPassword)
                }
                permanent
            }.onSuccess { permanent ->
                state = permanent
                // For image changes, publish only after URLs are permanent. For scalar
                // changes, the optimistic callback already updated the room and sockets.
                if (!canApplyOptimistically) onSave(permanent)
            }.onFailure { saveError = it.message ?: "Network error. Try again." }
            saving = false
        }
    }

    LaunchedEffect(adminSearch, sheet) {
        val q = adminSearch.trim()
        if (sheet != "admin-members" || q.isBlank()) return@LaunchedEffect
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
                        if (id.isBlank() || (q.all { it.isDigit() } && id != q)) null else NativeSettingMember(
                            id,
                            u.optString("name").takeUnless { it == "null" }.orEmpty()
                                .ifBlank { u.optString("displayName").ifBlank { u.optString("userName", "User") } },
                            u.optString("image").takeUnless { it == "null" }.orEmpty()
                                .ifBlank { u.optString("avatar").ifBlank { u.optString("profileImageUrl", "") } }
                        )
                    }
                } finally { conn.disconnect() }
            }.getOrDefault(emptyList())
        }.let { found ->
            if (sheet == "admin-members" && adminSearch.trim() == q && found.isNotEmpty()) {
                val merged = LinkedHashMap<String, NativeSettingMember>()
                found.forEach { merged[it.accountId] = it }
                members.forEach { if (it.accountId !in merged) merged[it.accountId] = it }
                members = merged.values.toList()
            }
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch {
            runCatching { cropImageToDataUrl(context, uri) }
                .onSuccess { dataUrl ->
                    if (pendingPicker == "photo") {
                        performSave(state.copy(roomDp = dataUrl))
                    } else {
                        pendingCustomThemeImage = dataUrl
                    }
                }
                .onFailure { saveError = it.message ?: "Could not load image" }
        }
    }

    val settingsCachePrefs = remember(roomOwnerId) {
        context.getSharedPreferences("room_settings_inventory_cache", Context.MODE_PRIVATE)
    }
    var storeRoomThemes by remember(roomOwnerId) {
        mutableStateOf(runCatching {
            val cached = JSONArray(settingsCachePrefs.getString("themes_$currentUserId", "[]").orEmpty())
            (0 until cached.length()).mapNotNull { index ->
                val item = cached.optJSONObject(index) ?: return@mapNotNull null
                val expiry = item.optLong("expiresAt")
                val id = item.optString("id")
                val image = item.optString("image")
                if (id.isNotBlank() && image.isNotBlank() && (expiry == 0L || expiry > System.currentTimeMillis()))
                    RoomThemeChoice(id, item.optString("name", id), image, expiry) else null
            }
        }.getOrDefault(emptyList()))
    }
    LaunchedEffect(currentUserId, sheet == "theme") {
        if (currentUserId.isBlank() || sheet != "theme") return@LaunchedEffect
        val refreshed = withContext(Dispatchers.IO) {
            runCatching {
                val encoded = java.net.URLEncoder.encode(currentUserId, "UTF-8")
                val conn = (URL("$SETTINGS_API/api/store/inventory/$encoded").openConnection() as HttpURLConnection).apply {
                    connectTimeout = 6000
                    readTimeout = 8000
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("Cache-Control", "no-cache, no-store")
                    setRequestProperty("Pragma", "no-cache")
                }
                try {
                    if (conn.responseCode !in 200..299) error("Inventory unavailable")
                    val root = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                    val arr = root.optJSONArray("items") ?: JSONArray()
                    (0 until arr.length()).mapNotNull { index ->
                        val item = arr.optJSONObject(index) ?: return@mapNotNull null
                        val id = item.optString("itemId")
                        val expiry = item.optLong("expiresAt")
                        if (item.optString("tab") == "Theme" && id.isNotBlank() && expiry > System.currentTimeMillis()) {
                            val image = item.optString("image")
                            val resolvedImage = when {
                                image.startsWith("data:image/") || image.startsWith("https://") || image.startsWith("http://") -> image
                                image.isNotBlank() -> STORE_THEME_ASSET_BASE + image
                                else -> ""
                            }
                            if (resolvedImage.isNotBlank()) RoomThemeChoice(id, item.optString("name", id), resolvedImage, expiry) else null
                        } else null
                    }
                } finally { conn.disconnect() }
            }.getOrNull()
        }
        // Show cached owned themes immediately; replace them only after a valid server response.
        if (refreshed != null) {
            storeRoomThemes = refreshed
            val cache = JSONArray()
            refreshed.forEach { theme ->
                cache.put(JSONObject().put("id", theme.id).put("name", theme.name)
                    .put("image", theme.image).put("expiresAt", theme.expiresAt))
            }
            settingsCachePrefs.edit().putString("themes_$currentUserId", cache.toString()).apply()
        }
    }

    val themeItems = remember(storeRoomThemes) {
        roomThemeChoices + storeRoomThemes
    }

    LaunchedEffect(storeRoomThemes) {
        while (storeRoomThemes.isNotEmpty()) {
            val nextExpiry = storeRoomThemes.minOfOrNull { it.expiresAt } ?: break
            val waitMs = (nextExpiry - System.currentTimeMillis()).coerceAtLeast(0L)
            if (waitMs > 0L) delay(waitMs)
            val now = System.currentTimeMillis()
            val expiredThemes = storeRoomThemes.filter { it.expiresAt > 0L && it.expiresAt <= now }
            if (expiredThemes.isNotEmpty()) storeRoomThemes = storeRoomThemes.filterNot { it in expiredThemes }
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
                SettingRow("Admin", "${admins.size}/5") { adminSearch = ""; sheet = "admin" }
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
            hasCustom = false,
            onBack = { sheet = "" },
            onPick = { id -> performSave(state.copy(theme = id)); sheet = "" },
            onUploadCustom = {
                pendingCustomThemeImage = ""
                pendingCustomThemeName = ""
                sheet = "customized-theme"
            }
        )

        "customized-theme" -> Dialog(
            onDismissRequest = { sheet = "theme" },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            val isUploadActive = pendingCustomThemeImage.isNotBlank()
            Column(
                Modifier.fillMaxSize()
                    .background(Color.White)
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                Row(
                    Modifier.fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).clickable { sheet = "theme" },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = gray800, modifier = Modifier.size(24.dp))
                    }
                    Text(
                        "Room Theme",
                        Modifier.weight(1f),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = gray800,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.size(40.dp))
                }

                Column(
                    Modifier.weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(10.dp))

                    Box(
                        Modifier.fillMaxWidth(0.62f)
                            .aspectRatio(0.58f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(gray100)
                            .border(1.dp, gray300, RoundedCornerShape(12.dp))
                            .clickable {
                                pendingPicker = "theme"
                                picker.launch("image/*")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (pendingCustomThemeImage.isNotBlank()) {
                            AsyncImage(
                                model = pendingCustomThemeImage,
                                contentDescription = "Selected Room Theme",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                "+",
                                color = gray600,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Light
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Text(
                        "750*1600px",
                        color = gray500,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(18.dp))

                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("*", color = red500, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(
                            " Theme name",
                            color = gray800,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    Box(
                        Modifier.fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(gray50)
                            .border(1.dp, gray200, RoundedCornerShape(10.dp))
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        BasicTextField(
                            value = pendingCustomThemeName,
                            onValueChange = { pendingCustomThemeName = it.take(30) },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(fontSize = 14.sp, color = gray800),
                            singleLine = true,
                            decorationBox = { inner ->
                                Box {
                                    if (pendingCustomThemeName.isEmpty()) {
                                        Text("Please enter", color = gray400, fontSize = 14.sp)
                                    }
                                    inner()
                                }
                            }
                        )
                    }

                    Spacer(Modifier.height(28.dp))

                    Text(
                        "After uploading the room background, you need to wait for official review. If you have any questions contact the official customer service.If fails, the coins will be returned",
                        color = gray500,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                    )

                    Spacer(Modifier.height(24.dp))
                }

                Box(
                    Modifier.fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .height(50.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isUploadActive) blue500 else blue500.copy(alpha = 0.45f))
                        .clickable(enabled = isUploadActive && !customThemeUploading) {
                            if (currentUserId.isBlank() || pendingCustomThemeName.isBlank()) {
                                saveError = "Theme name and account are required"
                            } else {
                                customThemeUploading = true
                                saveError = ""
                                scope.launch {
                                    val result = withContext(Dispatchers.IO) {
                                        runCatching {
                                            val firebaseUser = FirebaseAuth.getInstance().currentUser ?: error("Please sign in again")
                                            val token = Tasks.await(firebaseUser.getIdToken(false), 30, TimeUnit.SECONDS).token
                                                ?: error("Authentication token unavailable")
                                            val connection = (URL("$SETTINGS_API/api/room/custom-themes").openConnection() as HttpURLConnection).apply {
                                                requestMethod = "POST"
                                                connectTimeout = 15000
                                                readTimeout = 30000
                                                doOutput = true
                                                setRequestProperty("Authorization", "Bearer $token")
                                                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                                                setRequestProperty("Accept", "application/json")
                                            }
                                            try {
                                                val body = JSONObject()
                                                    .put("userId", currentUserId)
                                                    .put("name", pendingCustomThemeName.trim())
                                                    .put("image", pendingCustomThemeImage)
                                                    .toString()
                                                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                                                val code = connection.responseCode
                                                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                                                val response = JSONObject(stream?.bufferedReader()?.use { it.readText() }.orEmpty())
                                                if (code !in 200..299 || !response.optBoolean("success")) {
                                                    error(response.optString("error").ifBlank { "Theme upload failed" })
                                                }
                                                response
                                            } finally {
                                                connection.disconnect()
                                            }
                                        }
                                    }
                                    customThemeUploading = false
                                    result.onSuccess {
                                        pendingCustomThemeImage = ""
                                        pendingCustomThemeName = ""
                                        sheet = ""
                                        saveError = "Theme submitted for official approval"
                                    }.onFailure { saveError = it.message ?: "Theme upload failed" }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (customThemeUploading) "Uploading..." else "Upload",
                            color = if (isUploadActive) Color.White else Color.White.copy(alpha = 0.85f),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.width(6.dp))
                        AsyncImage(
                            model = RAW_SETTINGS + "file_00000000e56882119c217d508b6733dc.png",
                            contentDescription = "Coins",
                            modifier = Modifier.size(18.dp),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            "5,00,000 Coins",
                            color = if (isUploadActive) Color.White else Color.White.copy(alpha = 0.85f),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Mic Mode Sheet height increased
        "mic" -> BottomSheetModal(scrimAlpha = 0.3f, heightFraction = 0.62f, onDismiss = { sheet = "" }) {
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
                                5 -> RAW_SETTINGS + "IMG_20261102_124233.jpg"
                                10 -> RAW_SETTINGS + "IMG_20261102_124251.jpg"
                                else -> RAW_SETTINGS + "IMG_20261102_124300.jpg"
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
                                    modifier = Modifier.fillMaxWidth().wrapContentHeight().clip(RoundedCornerShape(8.dp)),
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

        // Admin management uses the same centered-card presentation as the Leaderboard info popup.
        "admin" -> Dialog(
            onDismissRequest = { sheet = "" },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            val adminCardShape = RoundedCornerShape(14.dp)
            val adminBorder = Brush.verticalGradient(
                listOf(Color(0xFFF3D578), Color(0xFFC59B38), Color(0xFFE5C158))
            )
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.65f))
                    .clickable { sheet = "" },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    Modifier.fillMaxWidth(0.86f)
                        .height((LocalConfiguration.current.screenHeightDp * 0.62f).dp)
                        .clip(adminCardShape)
                        .background(Brush.verticalGradient(listOf(Color(0xFF0B4182), Color(0xFF072957), Color(0xFF041A3B))))
                        .border(1.5.dp, adminBorder, adminCardShape)
                        .clickable(enabled = false) {}
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Spacer(Modifier.width(18.dp))
                        Text(
                            "Room Admins", Modifier.weight(1f),
                            color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Box(
                            Modifier.size(18.dp)
                                .clip(CircleShape)
                                .border(0.7.dp, Color.White.copy(alpha = 0.85f), CircleShape)
                                .clickable { sheet = "admin-help" },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("?", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(
                        "Room Admins: ${admins.size}/5",
                        color = Color.White.copy(alpha = 0.72f), fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
                    )
                    LazyColumn(
                        Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(admins, key = { it }) { adminId ->
                            val member = members.firstOrNull { it.accountId == adminId }
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                AsyncImage(
                                    model = member?.image?.ifBlank { RAW_SETTINGS + "default-avatar.png" } ?: RAW_SETTINGS + "default-avatar.png",
                                    contentDescription = "Admin",
                                    modifier = Modifier.size(40.dp).clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                                Column(Modifier.weight(1f).padding(start = 10.dp)) {
                                    Text(member?.name ?: adminId, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    AsyncImage(
                                        model = RAW_SETTINGS + "IMG_20261107_153823.png",
                                        contentDescription = "Room admin tag",
                                        modifier = Modifier.height(16.dp).width(52.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                                Text(
                                    "Remove", color = Color(0xFFFF8B8B), fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable {
                                        admins = admins.filterNot { it == adminId }
                                        performSave(state.copy(admin = admins))
                                    }.padding(horizontal = 8.dp, vertical = 7.dp)
                                )
                            }
                        }
                        if (admins.isEmpty()) {
                            item {
                                Text(
                                    "No room admins", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Box(
                        Modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(12.dp))
                            .background(Brush.verticalGradient(listOf(Color(0xFF1055A3), Color(0xFF0A3973))))
                            .border(1.2.dp, Color(0xFFE5C158), RoundedCornerShape(12.dp))
                            .clickable { adminSearch = ""; sheet = "admin-members" },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Add", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Room admin members sheet height = 0.65f (same size as admin) + pill 3D blue Add / Added same size button
        "admin-members" -> BottomSheetModal(scrimAlpha = 0.45f, heightFraction = 0.65f, dark = true, cornerRadius = 12.dp, onDismiss = { sheet = "admin" }) {
            Column(Modifier.fillMaxSize().navigationBarsPadding().background(Color.Black).padding(horizontal = 14.dp)) {
                Text("Room members", Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 12.dp), color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                val visibleMembers = remember(roomMembers, members, roomOwnerId, currentUserId) {
                    (roomMembers.asSequence()
                        .map { NativeSettingMember(it.accountId, it.name, it.image) } + members.asSequence())
                        .filter { it.accountId.isNotBlank() && it.accountId != roomOwnerId && it.accountId != currentUserId }
                        .distinctBy { it.accountId }
                        .toList()
                }
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(visibleMembers, key = { it.accountId }) { member ->
                        val added = member.accountId in admins
                        val canAdd = !added && admins.size < 5
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(model = member.image.ifBlank { RAW_SETTINGS + "default-avatar.png" }, contentDescription = member.name, modifier = Modifier.size(38.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                            Column(Modifier.weight(1f).padding(start = 9.dp)) {
                                Text(member.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("ID: ${member.accountId}", color = Color(0xFF9CA3AF), fontSize = 10.sp)
                            }
                            Box(
                                Modifier
                                    .width(72.dp)
                                    .height(30.dp)
                                    .shadow(elevation = if (added) 0.dp else 4.dp, shape = CircleShape)
                                    .clip(CircleShape)
                                    .background(
                                        if (added) Brush.verticalGradient(listOf(Color(0xFF1D4ED8), Color(0xFF1E3A8A)))
                                        else Brush.verticalGradient(listOf(Color(0xFF60A5FA), Color(0xFF2563EB), Color(0xFF1D4ED8)))
                                    )
                                    .border(
                                        width = 1.dp,
                                        brush = Brush.verticalGradient(
                                            listOf(Color.White.copy(alpha = if (added) 0.25f else 0.65f), Color(0xFF1E40AF))
                                        ),
                                        shape = CircleShape
                                    )
                                    .clickable(enabled = canAdd) {
                                        admins = (admins + member.accountId).distinct()
                                        performSave(state.copy(admin = admins))
                                        sheet = "admin"
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (added) "Added" else "Add",
                                    color = if (added) Color.White.copy(alpha = 0.85f) else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    if (visibleMembers.isEmpty()) item { Text("No room members available", color = Color(0xFF9CA3AF), modifier = Modifier.fillMaxWidth().padding(20.dp), textAlign = TextAlign.Center) }
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
fun PasswordInputBoxes(value: String, onChange: (String) -> Unit) {
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
    cornerRadius: Dp = 16.dp,
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
                    .clip(RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius))
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
    val expiringThemes = remember(items) { items.filter { it.expiresAt > 0L } }
    var clockNow by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(expiringThemes) {
        if (expiringThemes.isEmpty()) return@LaunchedEffect
        while (true) {
            val now = System.currentTimeMillis()
            clockNow = now
            val nextExpiry = expiringThemes.minOfOrNull { it.expiresAt } ?: break
            if (nextExpiry <= now) break
            delay(minOf(1000L, nextExpiry - now).coerceAtLeast(1L))
        }
    }
    val visibleThemes = remember(items, clockNow) {
        items.filter { it.expiresAt <= 0L || it.expiresAt > clockNow }
    }

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

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f).fillMaxWidth().padding(start = 12.dp, top = 16.dp, end = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                if (!hasCustom) {
                    item {
                        Box(
                            Modifier.fillMaxWidth().aspectRatio(0.86f)
                                .clip(RoundedCornerShape(8.dp))
                                .dashedBorder(gray300, 1.dp, 8.dp)
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
                gridItems(visibleThemes) { theme ->
                    Column(
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .then(
                                if (selectedTheme == theme.id) Modifier
                                    .padding(2.dp)
                                    .border(2.dp, blue400, RoundedCornerShape(8.dp)) else Modifier
                            )
                            .clickable { selectedTheme = theme.id }
                    ) {
                        Box(Modifier.fillMaxWidth().aspectRatio(0.86f).clip(RoundedCornerShape(8.dp))) {
                            AsyncImage(
                                model = theme.image, contentDescription = theme.name,
                                modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
                            )
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
                        if (theme.expiresAt > 0L) {
                            val seconds = ((theme.expiresAt - clockNow).coerceAtLeast(0L) / 1000L)
                            Text(
                                "%dd %02dh %02dm %02ds".format(seconds / 86400L, (seconds % 86400L) / 3600L, (seconds % 3600L) / 60L, seconds % 60L),
                                fontSize = 10.sp, color = gray500, textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
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

@Composable
fun RoomPasswordPrompt(
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var enteredPassword by remember { mutableStateOf("") }
    var incorrect by remember { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }
    LaunchedEffect(incorrect) {
        if (incorrect) {
            delay(1500)
            incorrect = false
        }
    }
    CenteredModal(scrimAlpha = 0.3f, onDismiss = onDismiss) {
        Box(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(16.dp)).background(Color.White).padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Set Room Password", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = gray800)
                Spacer(Modifier.height(24.dp))
                PasswordInputBoxes(enteredPassword) { enteredPassword = it; incorrect = false }
                Spacer(Modifier.height(24.dp))
                PillButton(
                    "Enter the room",
                    enabled = enteredPassword.length == 4 && !submitting,
                    onClick = {
                        if (enteredPassword.length == 4 && !submitting) {
                            submitting = true
                            onSubmit(enteredPassword)
                            submitting = false
                        }
                    }
                )
                Spacer(Modifier.height(12.dp))
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .clickable { onDismiss() }.padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) { Text("Cancel", color = gray500, fontSize = 14.sp, fontWeight = FontWeight.Medium) }
            }
        }
    }
    if (incorrect) {
        Box(Modifier.fillMaxSize().background(Color.Transparent), contentAlignment = Alignment.Center) {
            Text(
                "Password incorrect",
                color = Color.White,
                fontSize = 13.sp,
                modifier = Modifier.clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.70f))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            )
        }
    }
}

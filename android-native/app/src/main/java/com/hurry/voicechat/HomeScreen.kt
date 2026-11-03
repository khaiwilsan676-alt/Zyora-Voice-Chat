package com.hurry.voicechat

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.basicMarquee

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import androidx.room.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ═══════════════════════════════════════════════════════
// CONSTANTS
// ═══════════════════════════════════════════════════════
private const val RAW = "https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/"
private const val HONOUR_BG  = RAW + "IMG_20260912_144404.png"
private const val CHARM_BG   = RAW + "IMG_20260912_144324.png"
private const val ROOM_BG    = RAW + "IMG_20260912_144347.png"
private const val CARD_FRAME = RAW + "file_00000000048882118276c7215012963f.png"

// MePage wala exact gradient
private val MePageTopGradient = androidx.compose.ui.graphics.Brush.verticalGradient(
    0.0f  to Color(0xFF2563EB),
    0.97f to Color(0xFFEFF6FF),
    1.0f  to Color(0xFFF9FAFB)
)

// ═══════════════════════════════════════════════════════
// ROOM DB — INDEXEDDB EQUIVALENT (same file)
// ═══════════════════════════════════════════════════════
@Entity(tableName = "rooms")
data class RoomEntity(
    @PrimaryKey val id: String,
    val name: String,
    val country: String,
    val image: String,
    val accountId: String,
    val createdAt: Long,
    val isLocked: Boolean = false,
    val roomPassword: String? = null,
    val cachedAt: Long = System.currentTimeMillis()
)

@Dao
interface RoomDao {
    @Query("SELECT * FROM rooms ORDER BY createdAt DESC")
    fun getAll(): List<RoomEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(rooms: List<RoomEntity>): List<Long>

    @Query("DELETE FROM rooms")
    fun clearAll(): Int
}

@Database(entities = [RoomEntity::class], version = 2, exportSchema = false)
abstract class HurryDatabase : RoomDatabase() {
    abstract fun roomDao(): RoomDao

    companion object {
        @Volatile private var INSTANCE: HurryDatabase? = null
        fun get(context: Context): HurryDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    HurryDatabase::class.java,
                    "hurry_cache.db"
                ).fallbackToDestructiveMigration()
                 .build().also { INSTANCE = it }
            }
    }
}

// Process-wide fast cache: survives HomeScreen being removed during tab changes.
private object NativeRoomMemoryCache {
    var rooms by mutableStateOf<List<HurryRoom>?>(null)
    var lastRefreshAt: Long = 0L
    var refreshInFlight: Boolean = false
}

// ═══════════════════════════════════════════════════════
// COUNTRY PILL DATA
// ═══════════════════════════════════════════════════════
private data class CountryPill(
    val flag: String?,
    val label: String,
    val value: String
)

private val countryPills = listOf(
    CountryPill(null, "All",          "all"),
    CountryPill("🇮🇳", "India",        "India"),
    CountryPill("🇸🇬", "Singapore",    "Singapore"),
    CountryPill("🇵🇰", "Pakistan",     "Pakistan"),
    CountryPill("🇺🇸", "USA",          "USA"),
    CountryPill("🇿🇦", "South Africa", "South Africa"),
    CountryPill("🇹🇷", "Turkey",       "Turkey")
)

fun countryFlag(country: String): String {
    val value = country.trim()
    if (value.isBlank()) return ""
    if (value.codePointCount(0, value.length) >= 2 && value.any { it in '\uD83C'..'\uD83C' }) {
        if (value.contains("🇮🇳") || value.contains("🇸🇬") || value.contains("🇵🇰") || value.contains("🇺🇸") || value.contains("🇿🇦") || value.contains("🇹🇷")) return value.take(2)
    }
    val isoCodes = java.util.Locale.getISOCountries()
    val code = if (value.length == 2 && value.uppercase() in isoCodes) {
        value.uppercase()
    } else {
        isoCodes.firstOrNull { code ->
            java.util.Locale("", code).displayCountry.equals(value, ignoreCase = true)
        }
    } ?: return ""
    return buildString {
        code.uppercase().forEach { ch -> appendCodePoint(0x1F1E6 + (ch - 'A')) }
    }
}

private fun countryMatches(roomCountry: String, selected: String): Boolean {
    if (selected == "all") return true
    return countryFlag(roomCountry) == countryFlag(selected)
}

// ═══════════════════════════════════════════════════════
// HOME SCREEN
// ═══════════════════════════════════════════════════════
@kotlin.OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onRoom: (HurryRoom) -> Unit,
    onMine: () -> Unit = {},
    onPopular: () -> Unit = {},
    onLeaderboard: (String) -> Unit = {},
    onInviteFriends: () -> Unit = {},
    onSearch: () -> Unit = {},
    onRoomSupport: () -> Unit = {},
    onCreateMineRoom: () -> Unit = {},
    mineCreatedRoom: HurryRoom? = null,
    mineSelected: Boolean = false,
    languageCode: String = "en",
    keptRoom: HurryRoom? = null,
    onOpenKeptRoom: () -> Unit = {},
    onExitKeptRoom: () -> Unit = {},
    selectedTab: HurryTab = HurryTab.HOME,
    onTabSelected: (HurryTab) -> Unit = {}
) {
    val mine = mineSelected
    val context = LocalContext.current
    val dao = remember {
        runCatching { HurryDatabase.get(context).roomDao() }.getOrNull()
    }
    val roomSyncPrefs = remember { context.getSharedPreferences("native_room_sync", Context.MODE_PRIVATE) }

    var rooms by remember { mutableStateOf(NativeRoomMemoryCache.rooms) }
    var selectedCountry by remember { mutableStateOf("all") }
    var showDailyCheckIn by remember { mutableStateOf(false) }

    val authPrefs = remember { context.getSharedPreferences("hurry_auth", Context.MODE_PRIVATE) }
    val checkInUserId = remember {
        authPrefs.getString("accountId", null)?.takeIf { it.isNotBlank() }
            ?: authPrefs.getString("uid", "").orEmpty()
    }
    var followingRooms by remember { mutableStateOf<List<HurryRoom>>(emptyList()) }
    LaunchedEffect(checkInUserId, mine) {
        if (!mine || checkInUserId.isBlank()) return@LaunchedEffect
        followingRooms = withContext(Dispatchers.IO) {
            runCatching {
                val encoded = java.net.URLEncoder.encode(checkInUserId, "UTF-8")
                val connection = (java.net.URL("https://zyora-server.onrender.com/api/room-follows?userId=$encoded").openConnection() as java.net.HttpURLConnection).apply {
                    connectTimeout = 10000
                    readTimeout = 10000
                    setRequestProperty("Accept", "application/json")
                }
                try {
                    if (connection.responseCode !in 200..299) error("Following rooms unavailable")
                    val array = org.json.JSONObject(connection.inputStream.bufferedReader().use { it.readText() }).optJSONArray("follows") ?: org.json.JSONArray()
                    (0 until array.length()).mapNotNull { index ->
                        val item = array.optJSONObject(index) ?: return@mapNotNull null
                        val id = item.optString("roomId").trim()
                        if (id.isBlank()) return@mapNotNull null
                        HurryRoom(id, item.optString("roomName").ifBlank { "Room" }, item.optString("roomImage"), country = item.optString("roomCountry").ifBlank { "🇮🇳" }, accountId = id)
                    }.distinctBy { it.id }
                } finally { connection.disconnect() }
            }.getOrDefault(emptyList())
        }
    }
    val checkInScope = rememberCoroutineScope()
    var checkInDay by remember { mutableIntStateOf(1) }
    var claimedToday by remember { mutableStateOf(false) }
    var checkInSubmitting by remember { mutableStateOf(false) }

    LaunchedEffect(checkInUserId) {
        if (checkInUserId.isBlank()) return@LaunchedEffect
        runCatching {
            withContext(Dispatchers.IO) {
                val encoded = java.net.URLEncoder.encode(checkInUserId, "UTF-8")
                val connection = (java.net.URL("https://zyora-server.onrender.com/api/daily-checkin?userId=$encoded").openConnection() as java.net.HttpURLConnection).apply {
                    connectTimeout = 10000
                    readTimeout = 10000
                }
                try {
                    if (connection.responseCode !in 200..299) error("Daily check-in unavailable")
                    org.json.JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                } finally { connection.disconnect() }
            }
        }.onSuccess { data ->
            checkInDay = data.optInt("currentDay", 1).coerceIn(1, 7)
            claimedToday = data.optBoolean("claimedToday", false)
        }
    }


    LaunchedEffect(Unit) {
        if (NativeRoomMemoryCache.rooms == null) {
            val cached = if (dao != null) {
                runCatching { withContext(Dispatchers.IO) { dao.getAll() } }.getOrDefault(emptyList())
            } else emptyList()
            if (cached.isNotEmpty()) {
                val cachedRooms = cached.map {
                    HurryRoom(
                        id = it.id,
                        name = it.name,
                        country = it.country,
                        image = it.image,
                        accountId = it.accountId
                    )
                }
                NativeRoomMemoryCache.rooms = cachedRooms
                rooms = cachedRooms
            }
        } else {
            rooms = NativeRoomMemoryCache.rooms
        }

        val now = System.currentTimeMillis()
        if (!NativeRoomMemoryCache.refreshInFlight &&
            (NativeRoomMemoryCache.lastRefreshAt == 0L || now - NativeRoomMemoryCache.lastRefreshAt >= 30_000L)
        ) {
            NativeRoomMemoryCache.refreshInFlight = true
            try {
                val since = roomSyncPrefs.getLong("last_server_updated_at", 0L)
                val update = runCatching { HurryApi.roomsUpdatedSince(since) }.getOrNull()
                NativeRoomMemoryCache.lastRefreshAt = System.currentTimeMillis()
                if (update != null) {
                    val fresh = update.rooms
                    val previous = NativeRoomMemoryCache.rooms ?: rooms ?: emptyList()
                    val merged = if (since == 0L) fresh else (fresh + previous).distinctBy { it.id }
                    if (fresh.isNotEmpty() || since == 0L) {
                        NativeRoomMemoryCache.rooms = merged
                        rooms = merged
                        if (dao != null) {
                            runCatching {
                                withContext(Dispatchers.IO) {
                                    dao.clearAll()
                                    dao.insertAll(
                                        merged.map {
                                            RoomEntity(
                                                id = it.id,
                                                name = it.name,
                                                country = it.country,
                                                image = it.image,
                                                accountId = it.accountId,
                                                createdAt = System.currentTimeMillis()
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                    if (update.serverTime > 0L) {
                        roomSyncPrefs.edit().putLong("last_server_updated_at", update.serverTime).apply()
                    }
                } else if (rooms == null) {
                    rooms = emptyList()
                }
            } finally {
                NativeRoomMemoryCache.refreshInFlight = false
            }
        }
    }

    if (mine) {
        Box(Modifier.fillMaxSize()) {
            MineNativePage(onBack = onPopular, createdRoom = mineCreatedRoom, followingRooms = followingRooms, onOpenRoom = onRoom, onCreateRoom = onCreateMineRoom, onSearch = onSearch, languageCode = languageCode)
            Box(Modifier.align(Alignment.BottomCenter)) {
                HurryBottomNav(selectedTab, languageCode = languageCode, onTab = onTabSelected)
            }
        }
        return
    }

    val banners = listOf(
        RAW + "file_00000000efe88211874bfd6b0392fd41.png", 
        RAW + "file_0000000033348246b73ea85588488c24.png", 
        RAW + "IMG-20260818-WA0001.jpg"
    )
    val pager = rememberPagerState(pageCount = { banners.size })
    val listState = rememberLazyListState()
    val countrySticky by remember { derivedStateOf { listState.firstVisibleItemIndex >= 3 } }

    val filteredRooms = remember(rooms, selectedCountry) {
        val list = rooms ?: emptyList()
        if (selectedCountry == "all") list
        else list.filter { countryMatches(it.country, selectedCountry) }
    }

    val roomRows = remember(filteredRooms) { filteredRooms.chunked(2) }

    // 🔥 Outermost Box
    Box(Modifier.fillMaxSize()) {
        
        // Dono states yahan outermost Box me declare kiye hain taaki Red Zone aur Draggable DP dono sync rahe
        var keptRoomDrag by remember(keptRoom?.id) { mutableStateOf(Offset.Zero) }
        var keptRoomDragging by remember(keptRoom?.id) { mutableStateOf(false) }
        val density = androidx.compose.ui.platform.LocalDensity.current

        Column(
            Modifier.fillMaxSize()
                .background(Color(0xFFF5F5F5))
        ) {
            // ═══════════ FIXED TOP HEADER ═══════════
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(MePageTopGradient)
                    .padding(top = (WindowInsets.statusBars.asPaddingValues().calculateTopPadding() - 16.dp).coerceAtLeast(0.dp))
                    .padding(top = 0.dp, start = 12.dp, end = 0.dp, bottom = 0.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onPopular() }
                                .padding(horizontal = 4.dp, vertical = 3.dp)
                        ) {
                            Text(
                                nativeText(languageCode, "popular"),
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E1E1E)
                            )
                        }
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onMine() }
                                .padding(horizontal = 4.dp, vertical = 3.dp)
                        ) {
                            Text(
                                nativeText(languageCode, "mine"),
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color(0xFF6B7280)
                            )
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp) 
                    ) {
                        Box(Modifier.clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) { onSearch() }) { 
                            HurrySearchIcon() 
                        }
                        
                        Box(Modifier.clickable { onCreateMineRoom() }) {
                            HurryHouseIcon()
                        }
                    }
                }
            }

            // ═══════════ SCROLLABLE AREA ═══════════
            Box(Modifier.fillMaxSize()) {
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.foundation.LocalOverscrollConfiguration provides null
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().offset(y = (-8).dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        item {
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF5F5F5))
                                    .padding(top = 0.dp, start = 12.dp, end = 12.dp, bottom = 4.dp)
                            ) {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .height((13.5f * LocalConfiguration.current.screenHeightDp / 100f).dp)
                                        .clip(RoundedCornerShape(6.dp))
                                ) {
                                    HorizontalPager(
                                        state = pager,
                                        modifier = Modifier.fillMaxSize(),
                                        userScrollEnabled = true,
                                        pageSpacing = 0.dp
                                    ) { page ->
                                        AsyncImage(
                                            model = banners[page],
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize().clickable(
                                                indication = null, 
                                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                                            ) { 
                                                if (page == 0) onInviteFriends() 
                                                else if (page == 1) onRoomSupport() 
                                                else onInviteFriends()
                                            },
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    Row(
                                        Modifier
                                            .align(Alignment.BottomCenter)
                                            .padding(bottom = 5.dp),
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        repeat(banners.size) { i ->
                                            Box(
                                                Modifier
                                                    .padding(horizontal = 1.5.dp)
                                                    .size(5.dp)
                                                    .clip(RoundedCornerShape(50))
                                                    .background(
                                                        if (i == pager.currentPage) Color.White
                                                        else Color.White.copy(alpha = 0.75f)
                                                    )
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp)
                                    .graphicsLayer { translationY = -4.dp.toPx() },
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                HurryCategoryCard("Honour", HONOUR_BG, Modifier.weight(1f)) { onLeaderboard("honour") }
                                HurryCategoryCard("Charm",  CHARM_BG,  Modifier.weight(1f)) { onLeaderboard("charm") }
                                HurryCategoryCard("Room",   ROOM_BG,   Modifier.weight(1f)) { onLeaderboard("room") }
                            }
                        }

                        item {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF5F5F5))
                            ) {
                                CountryPillRow(
                                    selected = selectedCountry,
                                    onSelect = { selectedCountry = it },
                                    languageCode = languageCode
                                )
                            }
                        }

                        if (rooms == null) {
                            item {
                                Box(
                                    Modifier.fillMaxWidth().height(180.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = Color(0xFF2563EB), strokeWidth = 2.dp)
                                }
                            }
                        } else if (filteredRooms.isEmpty()) {
                            item {
                                Box(
                                    Modifier.fillMaxWidth().height(120.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "No rooms in this country",
                                        fontSize = 14.sp,
                                        color = Color(0xFF6B7280)
                                    )
                                }
                            }
                        } else {
                            items(
                                items = roomRows,
                                key = { row -> row.first().id }
                            ) { row ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(start = 12.dp, end = 12.dp, bottom = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    row.forEach { room ->
                                        RoomListCard(room, onRoom, Modifier.weight(1f))
                                    }
                                    if (row.size == 1) Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
                
                if (countrySticky) {
                    Box(
                        Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = (-8).dp) 
                            .fillMaxWidth()
                            .background(Color(0xFFF5F5F5))
                    ) {
                        CountryPillRow(
                            selected = selectedCountry,
                            onSelect = { selectedCountry = it },
                            languageCode = languageCode
                        )
                    }
                }
            }
        } // End of Main Column

        // 🟢 BOTTOM NAV - Ab iske BAAD red zone aayega taaki overlay ho!
        Box(Modifier.align(Alignment.BottomCenter)) {
            HurryBottomNav(selectedTab, languageCode = languageCode, onTab = onTabSelected)
        }

        // 🔴 RED DROP ZONE - EXACTLY AT THE BOTTOM-RIGHT CORNER (Overlapping the Bottom Bar perfectly)
        if (keptRoom != null && keptRoomDragging) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(190.dp) // Perfect circular corner 
                    .offset(x = 95.dp, y = 95.dp) // Half of size ensures it creates exactly a Quarter-Circle at the corner
                    .background(Color(0xFFE53935), CircleShape),
                contentAlignment = Alignment.TopStart
            ) {
                // Target icon properly positioned within the visible quadrant
                Canvas(Modifier.padding(start = 38.dp, top = 38.dp).size(42.dp)) {
                    drawCircle(Color.White, radius = size.width * 0.18f)
                    drawCircle(Color.White, radius = size.width * 0.42f, style = Stroke(width = 3.dp.toPx()))
                }
            }
        }

        // Keep room dp & Bottom Image UI (Ise bhi yahan rakha hai taaki drag karte time yeh upar hi rahe)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 150.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.2.dp)
        ) {
            if (keptRoom != null) {
                val keptRoomMusic = rememberInfiniteTransition(label = "home-kept-room-music")
                val keptRoomMusicPhase by keptRoomMusic.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 900, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "home-kept-room-wave"
                )
                Box(
                    Modifier
                        .offset { IntOffset(keptRoomDrag.x.roundToInt(), keptRoomDrag.y.roundToInt()) }
                        .size(60.dp)
                        .border(2.dp, Color.White, CircleShape)
                        .clip(CircleShape)
                        .pointerInput(keptRoom.id) {
                            detectDragGestures(
                                onDragStart = { keptRoomDragging = true },
                                onDragEnd = {
                                    val exitThresholdY = with(density) { 50.dp.toPx() } 
                                    val exitThresholdX = with(density) { 20.dp.toPx() }
                                    val shouldExit = keptRoomDrag.y > exitThresholdY || (keptRoomDrag.y > 20f && keptRoomDrag.x > exitThresholdX)
                                    
                                    keptRoomDragging = false
                                    if (shouldExit) onExitKeptRoom()
                                    keptRoomDrag = Offset.Zero
                                },
                                onDragCancel = { keptRoomDragging = false; keptRoomDrag = Offset.Zero },
                                onDrag = { change, amount ->
                                    change.consume()
                                    keptRoomDrag += amount
                                }
                            )
                        }
                        .clickable(enabled = !keptRoomDragging) { onOpenKeptRoom() },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = keptRoom.image,
                        contentDescription = "Kept room",
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Canvas(Modifier.fillMaxSize()) {
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val heights = listOf(0.30f, 0.48f, 0.36f)
                        val offsets = listOf(-7f, 0f, 7f)
                        heights.forEachIndexed { index, base ->
                            val pulse = kotlin.math.abs(kotlin.math.sin((keptRoomMusicPhase * 2f * Math.PI) + index * 1.35f)).toFloat()
                            val halfHeight = size.height * (base * (0.55f + pulse * 0.65f)) / 2f
                            drawLine(
                                color = Color.White,
                                start = Offset(centerX + offsets[index].dp.toPx(), centerY - halfHeight),
                                end = Offset(centerX + offsets[index].dp.toPx(), centerY + halfHeight),
                                strokeWidth = 2.6.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }
            }

            AsyncImage(
                model = RAW + "IMG_20260916_002115.png",
                contentDescription = "Right Corner Decoration",
                modifier = Modifier
                    .size(70.dp)
                    .clickable(
                        indication = null,
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    ) { showDailyCheckIn = true },
                contentScale = ContentScale.Fit
            )
        }

        // DailyCheckIn Modal bhi yahan aayega top overlay ke liye
        DailyCheckInModal(
            isOpen = showDailyCheckIn,
            onClose = { showDailyCheckIn = false },
            currentDay = checkInDay,
            onSignIn = {
                if (!checkInSubmitting && !claimedToday && checkInUserId.isNotBlank()) {
                    checkInSubmitting = true
                    checkInScope.launch {
                        val result = runCatching {
                            withContext(Dispatchers.IO) {
                                val connection = (java.net.URL("https://zyora-server.onrender.com/api/daily-checkin").openConnection() as java.net.HttpURLConnection).apply {
                                    requestMethod = "POST"
                                    doOutput = true
                                    connectTimeout = 12000
                                    readTimeout = 12000
                                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                                }
                                try {
                                    val payload = org.json.JSONObject().put("userId", checkInUserId).toString()
                                    connection.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
                                    val code = connection.responseCode
                                    val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                                    val responseText = stream.bufferedReader().use { it.readText() }
                                    val response = org.json.JSONObject(responseText)
                                    if (code !in 200..299 || !response.optBoolean("success")) {
                                        error(response.optString("error", "Daily reward could not be claimed"))
                                    }

                                    val balance = response.optLong("balance", -1L)
                                    if (balance >= 0L) runCatching {
                                        val walletDao = WalletDatabase.get(context).walletDao()
                                        val old = walletDao.getUserData() ?: UserDataEntity()
                                        walletDao.saveUserData(old.copy(balance = balance))
                                        val coins = response.optLong("coinsAwarded", 0L)
                                        if (coins > 0L) walletDao.insertTransaction(
                                            TransactionEntity(
                                                title = "Daily Check-in",
                                                amount = coins,
                                                date = java.text.SimpleDateFormat("yyyy.MM.dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date()),
                                                timestamp = System.currentTimeMillis(),
                                                type = "daily_checkin"
                                            )
                                        )
                                    }

                                    response
                                } finally { connection.disconnect() }
                            }
                        }
                        result.onSuccess { response ->
                            checkInDay = response.optInt("day", checkInDay).coerceIn(1, 7)
                            claimedToday = true
                        }.onFailure {
                            claimedToday = false
                        }
                        checkInSubmitting = false
                    }
                }
            },
            claimedToday = claimedToday || checkInSubmitting
        )
    } // 🔥 Outermost Box Ends Here
}

// ═══════════════════════════════════════════════════════
// COUNTRY PILL ROW (Horizontal scroll — LazyRow)
// ═══════════════════════════════════════════════════════
@Composable
private fun CountryPillRow(
    selected: String,
    onSelect: (String) -> Unit,
    languageCode: String
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .graphicsLayer { translationY = -3.dp.toPx() }
            .height(40.dp),
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        userScrollEnabled = true
    ) {
        items(countryPills.size) { index ->
            val pill = countryPills[index]
            val active = selected == pill.value

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (active) Color(0xFF2563EB) else Color.White)
                    .clickable { onSelect(pill.value) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (pill.flag == null) {
                    androidx.compose.foundation.Canvas(Modifier.size(24.dp)) {
                        val stroke = Stroke(
                            width = 2.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                        val c = if (active) Color.White else Color(0xFF1E1E1E)
                        drawCircle(
                            color = c,
                            radius = size.minDimension * 0.42f,
                            center = Offset(size.width / 2f, size.height / 2f),
                            style = stroke
                        )
                        drawLine(
                            color = c,
                            start = Offset(size.width * 0.08f, size.height / 2f),
                            end = Offset(size.width * 0.92f, size.height / 2f),
                            strokeWidth = stroke.width,
                            cap = StrokeCap.Round
                        )
                        drawOval(
                            color = c,
                            topLeft = Offset(size.width * 0.33f, size.height * 0.08f),
                            size = Size(size.width * 0.34f, size.height * 0.84f),
                            style = stroke
                        )
                    }
                } else {
                    Text(pill.flag, fontSize = 14.sp)
                }

                Text(
                    text = if (pill.value == "all") nativeText(languageCode, "all") else pill.label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (active) Color.White else Color(0xFF374151),
                    maxLines = 1
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// ROOM CARD
// ═══════════════════════════════════════════════════════
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun RoomListCard(
    room: HurryRoom,
    onRoom: (HurryRoom) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth().clickable { onRoom(room) }) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFE5E7EB))
        ) {
            FallbackAsyncImage(
                model = if (room.image.startsWith("http")) room.image
                    else if (room.image.isBlank() ||
                        room.image.equals("undefined", ignoreCase = true) ||
                        room.image.equals("null", ignoreCase = true) ||
                        room.image.endsWith("/default-avatar.png") ||
                        room.image.equals("default-avatar.png", ignoreCase = true)) ""
                    else RAW + room.image.trimStart('/'),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            var liveCount by remember(room.id) { mutableIntStateOf((1000..4999).random()) }
            val barTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "room_card_equalizer")
            val bar1 by barTransition.animateFloat(initialValue = 0.25f, targetValue = 1f, animationSpec = androidx.compose.animation.core.infiniteRepeatable(animation = androidx.compose.animation.core.tween(360, easing = androidx.compose.animation.core.LinearEasing), repeatMode = androidx.compose.animation.core.RepeatMode.Reverse), label = "room_bar_1")
            val bar2 by barTransition.animateFloat(initialValue = 1f, targetValue = 0.2f, animationSpec = androidx.compose.animation.core.infiniteRepeatable(animation = androidx.compose.animation.core.tween(470, easing = androidx.compose.animation.core.LinearEasing), repeatMode = androidx.compose.animation.core.RepeatMode.Reverse), label = "room_bar_2")
            val bar3 by barTransition.animateFloat(initialValue = 0.35f, targetValue = 1f, animationSpec = androidx.compose.animation.core.infiniteRepeatable(animation = androidx.compose.animation.core.tween(330, easing = androidx.compose.animation.core.LinearEasing), repeatMode = androidx.compose.animation.core.RepeatMode.Reverse), label = "room_bar_3")

            LaunchedEffect(room.id) {
                while (true) {
                    kotlinx.coroutines.delay((500L..1200L).random())
                    liveCount = (liveCount + (-3..5).random()).coerceAtLeast(1000)
                }
            }
            
            Row(
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 7.dp, bottom = 2.dp) 
                    .padding(horizontal = 5.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    liveCount.toString(),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1
                )
                Row(
                    modifier = Modifier.height(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    listOf(bar1, bar2, bar3).forEach { level ->
                        Box(
                            Modifier.width(2.dp).height((3f + level * 9f).dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White)
                        )
                    }
                }
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 1.dp, vertical = 0.5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(countryFlag(room.country), fontSize = 16.sp, maxLines = 1)
            
            val roomName = room.name.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) && !it.equals("undefined", ignoreCase = true) } ?: "Zyora_User"
            
            Text(
                text = roomName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF202124),
                maxLines = 1,
                modifier = Modifier.weight(1f).then(
                    if (roomName.length > 16) {
                        Modifier.basicMarquee(iterations = Int.MAX_VALUE, velocity = 25.dp)
                    } else {
                        Modifier
                    }
                )
            )
        }
    }
}

// ═══════════════════════════════════════════════════════
// HOUSE ICON 
// ═══════════════════════════════════════════════════════
@Composable
fun HurryHouseIcon(selected: Boolean = false) {
    AsyncImage(
        model = RAW + "file_000000009aac81faa8564416680aba4e.png",
        contentDescription = null,
        modifier = Modifier.size(68.dp),
        contentScale = ContentScale.Fit
    )
}

// ═══════════════════════════════════════════════════════
// CATEGORY CARD
// ═══════════════════════════════════════════════════════
@Composable
private fun HurryCategoryCard(
    label: String,
    bg: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Box(modifier.height(92.dp).clip(RoundedCornerShape(16.dp)).clickable { onClick() }) {
        AsyncImage(
            model = bg,
            contentDescription = label,
            modifier = Modifier
                .fillMaxSize()
                .offset(y = 1.dp)
                .graphicsLayer {
                    val s = if (label.equals("Honour", true)) 1.08f else 1.02f
                    scaleX = s; scaleY = s
                },
            contentScale = ContentScale.Fit
        )
        Text(
            text = label.uppercase(),
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            color = Color.White
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 3.dp, end = 3.dp, bottom = 2.dp)
        ) {
            Row(
                Modifier
                    .matchParentSize() 
                    .offset(y = (-1).dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = RAW + "logo.png",
                    contentDescription = null,
                    modifier = Modifier.size(24.dp).clip(RoundedCornerShape(50)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(2.dp))
                AsyncImage(
                    model = RAW + "logo.png",
                    contentDescription = null,
                    modifier = Modifier.size(29.dp).clip(RoundedCornerShape(50)).padding(1.dp),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(2.dp))
                AsyncImage(
                    model = RAW + "logo.png",
                    contentDescription = null,
                    modifier = Modifier.size(24.dp).clip(RoundedCornerShape(50)),
                    contentScale = ContentScale.Crop
                )
            }

            AsyncImage(
                model = CARD_FRAME,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                contentScale = ContentScale.FillWidth
            )
        }
    }
}

@Composable
fun HurryBottomNav(tab: HurryTab, languageCode: String = "en", onTab: (HurryTab) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color.White)
            .navigationBarsPadding()
            .height(64.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavItem(HurryTab.HOME, tab, onTab, nativeText(languageCode, "home"))
        NavItem(HurryTab.MESSAGE, tab, onTab, nativeText(languageCode, "message"))
        NavItem(HurryTab.ME, tab, onTab, nativeText(languageCode, "me"))
    }
}

@Composable
private fun NavItem(
    which: HurryTab,
    selected: HurryTab,
    onTab: (HurryTab) -> Unit,
    label: String
) {
    Column(
        Modifier
            .width(72.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onTab(which) }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        if (which == HurryTab.MESSAGE) {
            HurryMessageIcon(active = which == selected, modifier = Modifier.size(30.dp))
        } else Canvas(Modifier.size(30.dp)) {
            val s = size.minDimension / 36f
            val active = which == selected
            val fill = if (active) Color(0xFF3B82F6) else Color.White
            val stroke = Color(0xFF1D1D1F)
            fun path(block: Path.() -> Unit) = Path().apply(block)

            when (which) {
                HurryTab.HOME -> {
                    val q = path {
                        moveTo(18*s,2.8f*s)
                        cubicTo(20.2f*s,2.8f*s,30.2f*s,8.2f*s,30.2f*s,12.6f*s)
                        lineTo(30.2f*s,23.2f*s)
                        cubicTo(30.2f*s,27.8f*s,28f*s,31f*s,18f*s,31f*s)
                        cubicTo(8f*s,31f*s,5.8f*s,27.8f*s,5.8f*s,23.2f*s)
                        lineTo(5.8f*s,12.6f*s)
                        cubicTo(5.8f*s,8.2f*s,15.8f*s,2.8f*s,18f*s,2.8f*s)
                        close()
                    }
                    drawPath(path = q, color = fill)
                    drawPath(path = q, color = stroke, style = Stroke(2.4f*s, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    drawPath(path = path {
                        moveTo(12.2f*s,14.2f*s)
                        cubicTo(13.3f*s,12.6f*s,14.9f*s,12.1f*s,16.8f*s,13.4f*s)
                    }, stroke, style = Stroke(1.8f*s, cap = StrokeCap.Round))
                    drawPath(path = path {
                        moveTo(11.2f*s,20.8f*s)
                        cubicTo(12.5f*s,24.2f*s,21f*s,25.6f*s,24.3f*s,20.2f*s)
                    }, stroke, style = Stroke(1.8f*s, cap = StrokeCap.Round))
                }
                HurryTab.MESSAGE -> {
                    val q = path {
                        moveTo(6f*s,10.5f*s)
                        cubicTo(6f*s,7f*s,8.3f*s,5f*s,12.2f*s,5f*s)
                        lineTo(23.8f*s,5f*s)
                        cubicTo(27.7f*s,5f*s,30f*s,7f*s,30f*s,10.5f*s)
                        lineTo(30f*s,16.5f*s)
                        cubicTo(30f*s,20f*s,27.7f*s,22f*s,23.8f*s,22f*s)
                        lineTo(21f*s,22f*s)
                        lineTo(17.5f*s,27.2f*s)
                        cubicTo(17f*s,28f*s,15.8f*s,28f*s,15.2f*s,27.2f*s)
                        lineTo(12.2f*s,22f*s)
                        cubicTo(8.3f*s,22f*s,6f*s,20f*s,6f*s,16.5f*s)
                        close()
                    }
                    drawPath(path = q, color = fill)
                    drawPath(path = q, color = stroke, style = Stroke(2.4f*s))
                    drawPath(path = path {
                        moveTo(12f*s,14.5f*s)
                        cubicTo(13.5f*s,12.5f*s,15.5f*s,14.5f*s,19.5f*s,12.5f*s)
                        cubicTo(21.5f*s,14.5f*s,24f*s,14.5f*s,24f*s,14.5f*s)
                    }, stroke, style = Stroke(1.8f*s, cap = StrokeCap.Round))
                }
                HurryTab.ME -> {
                    val q = path {
                        moveTo(18f*s,4.5f*s)
                        cubicTo(23.5f*s,4.5f*s,28f*s,8.5f*s,27.2f*s,13.8f*s)
                        lineTo(26.2f*s,19.8f*s)
                        cubicTo(26f*s,21.2f*s,27.2f*s,22.5f*s,28.6f*s,23.1f*s)
                        cubicTo(30.6f*s,24f*s,31f*s,26.2f*s,29f*s,27.5f*s)
                        cubicTo(27.5f*s,28.5f*s,25f*s,28.8f*s,22f*s,28.8f*s)
                        lineTo(14f*s,28.8f*s)
                        cubicTo(11f*s,28.8f*s,8.5f*s,28.5f*s,7f*s,27.5f*s)
                        cubicTo(5f*s,26.2f*s,5.4f*s,24f*s,7.4f*s,23.1f*s)
                        cubicTo(8.8f*s,22.5f*s,10f*s,21.2f*s,9.8f*s,19.8f*s)
                        lineTo(8.8f*s,13.8f*s)
                        cubicTo(8f*s,8.5f*s,12.5f*s,4.5f*s,18f*s,4.5f*s)
                        close()
                    }
                    drawPath(path = q, color = fill)
                    drawPath(path = q, color = stroke, style = Stroke(2.4f*s))
                    drawCircle(color = stroke, radius = 1.6f*s, center = androidx.compose.ui.geometry.Offset(14f*s,15f*s))
                    drawCircle(color = stroke, radius = 1.6f*s, center = androidx.compose.ui.geometry.Offset(22f*s,15f*s))
                }
            }
        }
        Text(
            label,
            fontSize = 12.sp,
            color = if (which == selected) Color.Black else Color(0xFF6B7280)
        )
    }
}

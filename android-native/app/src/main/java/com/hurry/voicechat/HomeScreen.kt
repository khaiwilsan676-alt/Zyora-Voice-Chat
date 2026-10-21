package com.hurry.voicechat

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
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

private fun countryFlag(country: String): String {
    val value = country.trim()
    return when {
        value.contains("india", ignoreCase = true) || value.contains("🇮🇳") -> "🇮🇳"
        value.contains("singapore", ignoreCase = true) || value.contains("🇸🇬") -> "🇸🇬"
        value.contains("pakistan", ignoreCase = true) || value.contains("🇵🇰") -> "🇵🇰"
        value.contains("usa", ignoreCase = true) || value.contains("united states", ignoreCase = true) || value.contains("🇺🇸") -> "🇺🇸"
        value.contains("south africa", ignoreCase = true) || value.contains("🇿🇦") -> "🇿🇦"
        value.contains("turkey", ignoreCase = true) || value.contains("türkiye", ignoreCase = true) || value.contains("🇹🇷") -> "🇹🇷"
        else -> "🌎"
    }
}

private fun countryMatches(roomCountry: String, selected: String): Boolean {
    if (selected == "all") return true
    return countryFlag(roomCountry) == countryFlag(selected)
}

// ═══════════════════════════════════════════════════════
// HOME SCREEN
// ═══════════════════════════════════════════════════════
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
    languageCode: String = "en"
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
        MineNativePage(onBack = onPopular, createdRoom = mineCreatedRoom, onCreateRoom = onCreateMineRoom, onSearch = onSearch, languageCode = languageCode)
        return
    }

    // 🔴 2ND BANNER REMOVED, AB SIRF 3 HAIN
    val banners = listOf(
        RAW + "file_00000000efe88211874bfd6b0392fd41.png", 
        RAW + "file_0000000033348246b73ea85588488c24.png", 
        RAW + "IMG-20260818-WA0001.jpg"
    )
    val pager = rememberPagerState(pageCount = { banners.size })
    val listState = rememberLazyListState()
    val countrySticky by remember { derivedStateOf { listState.firstVisibleItemIndex >= 3 } }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(5000)
            if (!pager.isScrollInProgress) {
                pager.animateScrollToPage((pager.currentPage + 1) % banners.size)
            }
        }
    }

    val filteredRooms = remember(rooms, selectedCountry) {
        val list = rooms ?: emptyList()
        if (selectedCountry == "all") list
        else list.filter { countryMatches(it.country, selectedCountry) }
    }

    // Build rows once per room-list change. LazyColumn renders only visible rows;
    // avoid repeatedly copying the growing list and triggering a full recomposition every 40ms.
    val roomRows = remember(filteredRooms) { filteredRooms.chunked(2) }

    Column(
        Modifier.fillMaxSize().background(Color(0xFFF5F5F5))
    ) {
        // ═══════════ FIXED TOP HEADER (does not scroll) ═══════════
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
                
                // 🔴 SEARCH OR HOUSE ICON KE BEECH KA GAP EXACT 3.DP HAI
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
        // ═══════════ END FIXED TOP HEADER ═══════════

        // ═══════════ SCROLLABLE AREA ═══════════
        Box(Modifier.fillMaxSize()) {
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
                                    // 🔴 1ST BANNER = INVITE FRIENDS, 2ND = ROOM SUPPORT
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
            
            // 🔴 STICKY HEADER TRANSPARENT GAP FIX (Added offset -8.dp to match LazyColumn)
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

            AsyncImage(
                model = RAW + "IMG_20260916_002115.png",
                contentDescription = "Right Corner Decoration",
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 38.dp) 
                    .size(70.dp)
                    .clickable(
                        indication = null,
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    ) { showDailyCheckIn = true },
                contentScale = ContentScale.Fit
            )

            DailyCheckInModal(
                isOpen = showDailyCheckIn,
                onClose = { showDailyCheckIn = false },
                currentDay = 1,
                onSignIn = { },
                claimedToday = false
            )
        }
    }
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
@Composable
private fun RoomListCard(
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
            var bar1 by remember(room.id) { mutableStateOf(4.dp) }
            var bar2 by remember(room.id) { mutableStateOf(9.dp) }
            var bar3 by remember(room.id) { mutableStateOf(6.dp) }
            
            LaunchedEffect(room.id) {
                while (true) {
                    kotlinx.coroutines.delay((500L..1200L).random())
                    liveCount = (liveCount + (-3..5).random()).coerceAtLeast(1000)
                    bar1 = (3..10).random().dp
                    bar2 = (4..12).random().dp
                    bar3 = (3..10).random().dp
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
                    listOf(bar1, bar2, bar3).forEach { barHeight ->
                        val animatedHeight by androidx.compose.animation.core.animateDpAsState(
                            targetValue = barHeight,
                            label = "equalizer_animation"
                        )
                        Box(
                            Modifier.width(2.dp).height(animatedHeight)
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
            Text(
                room.name.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) && !it.equals("undefined", ignoreCase = true) } ?: "Zyora_User",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF202124),
                maxLines = 1,
                modifier = Modifier.weight(1f)
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

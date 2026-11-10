package com.hurry.voicechat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import org.json.JSONArray
import org.json.JSONObject
import coil3.compose.AsyncImage

/** Recent rooms are persisted per account, newest first, with a rolling two-hour lifetime. */
object RecentRoomHistory {
    private const val PREFS = "zyora_recent_rooms"
    private const val MAX_ROOMS = 15
    private const val MAX_AGE_MS = 2L * 60L * 60L * 1000L
    var version by mutableIntStateOf(0)
        private set

    private fun key(accountId: String) = "rooms_${accountId.ifBlank { "guest" }}"

    @Synchronized
    fun record(context: android.content.Context, accountId: String, room: HurryRoom) {
        val prefs = context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val existing = readArray(prefs.getString(key(accountId), "[]").orEmpty())
        val kept = existing.filter { now - it.second < MAX_AGE_MS && it.first.id != room.id }
        save(prefs, key(accountId), (listOf(room to now) + kept).take(MAX_ROOMS))
        version++
    }

    @Synchronized
    fun load(context: android.content.Context, accountId: String): List<HurryRoom> {
        val prefs = context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val storageKey = key(accountId)
        val existing = readArray(prefs.getString(storageKey, "[]").orEmpty())
        val valid = existing.filter { now - it.second < MAX_AGE_MS }
            .distinctBy { it.first.id }.take(MAX_ROOMS)
        if (valid.size != existing.size) save(prefs, storageKey, valid)
        return valid.map { it.first }
    }

    private fun readArray(raw: String): List<Pair<HurryRoom, Long>> = runCatching {
        val array = JSONArray(raw)
        (0 until array.length()).mapNotNull { index ->
            val item = array.optJSONObject(index) ?: return@mapNotNull null
            val id = item.optString("id").trim()
            if (id.isBlank()) return@mapNotNull null
            HurryRoom(
                id = id,
                name = item.optString("name", "Room"),
                image = item.optString("image", ""),
                country = item.optString("country", ""),
                accountId = item.optString("accountId", "")
            ) to item.optLong("visitedAt", 0L)
        }
    }.getOrDefault(emptyList())

    private fun save(prefs: android.content.SharedPreferences, key: String, rooms: List<Pair<HurryRoom, Long>>) {
        val array = JSONArray()
        rooms.forEach { (room, visitedAt) ->
            array.put(JSONObject().put("id", room.id).put("name", room.name)
                .put("image", room.image).put("country", room.country)
                .put("accountId", room.accountId).put("visitedAt", visitedAt))
        }
        prefs.edit().putString(key, array.toString()).apply()
    }
}

@Composable
fun MineNativePage(onBack: () -> Unit, createdRoom: HurryRoom? = null, followingRooms: List<HurryRoom> = emptyList(), onOpenRoom: (HurryRoom) -> Unit = {}, onCreateRoom: () -> Unit = {}, onSearch: () -> Unit = {}, languageCode: String = "en") {
    var recent by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val authPrefs = remember { context.getSharedPreferences("hurry_auth", android.content.Context.MODE_PRIVATE) }
    val recentAccountId = authPrefs.getString("accountId", null)?.takeIf { it.isNotBlank() }
        ?: authPrefs.getString("uid", "").orEmpty()
    val recentRevision = RecentRoomHistory.version
    val recentRooms = remember(recentRevision, recentAccountId) { RecentRoomHistory.load(context, recentAccountId) }
    val card = Modifier.fillMaxWidth().height(112.dp)
    
    LazyColumn(
        Modifier.fillMaxSize().background(Color(0xFFEFEFEF)),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // ═══════════ FIXED TOP HEADER ═══════════
        item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color(0xFF2563EB),
                            0.97f to Color(0xFFEFF6FF),
                            1.0f to Color(0xFFF9FAFB)
                        )
                    )
                    // Top Safe Area decrease kiya
                    .padding(top = (WindowInsets.statusBars.asPaddingValues().calculateTopPadding() - 16.dp).coerceAtLeast(0.dp))
                    // Right padding 0.dp taaki corner me chipke
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
                                .clickable { onBack() }
                                .padding(horizontal = 4.dp, vertical = 3.dp)
                        ) {
                            Text(
                                nativeText(languageCode, "popular"),
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Normal,
                                color = HurryMuted
                            )
                        }
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { }
                                .padding(horizontal = 4.dp, vertical = 3.dp)
                        ) {
                            Text(
                                nativeText(languageCode, "mine"),
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Bold,
                                color = HurryText
                            )
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    
                    // Gap bilkul HomeScreen jaisa (8.dp) kar diya hai
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Box(
                            modifier = Modifier.clickable(
                                indication = null,
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                            ) { onSearch() }
                        ) {
                            HurrySearchIcon()
                        }
                        
                        Box(Modifier.clickable { onCreateRoom() }) {
                            HurryHouseIcon()
                        }
                    }
                }
            }
        }
        // ═══════════ END FIXED TOP HEADER ═══════════

        item {
            Spacer(Modifier.height(6.dp))
            Box(card.padding(horizontal = 12.dp).clip(RoundedCornerShape(6.dp)).background(
                Brush.linearGradient(listOf(Color(0xFF667EEA), Color(0xFF764BA2)))
            ).clickable { onCreateRoom() }) {
                Row(Modifier.fillMaxSize().padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(56.dp).clip(RoundedCornerShape(50)).background(Color.White.copy(alpha=.2f)), contentAlignment=Alignment.Center) {
                        if (createdRoom == null) {
                            Text("+", color=Color.White, fontSize=32.sp)
                        } else {
                            FallbackAsyncImage(model=createdRoom.image, contentDescription=null, modifier=Modifier.fillMaxSize().clip(RoundedCornerShape(50)))
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(createdRoom?.name ?: nativeText(languageCode, "create_room"), color=Color.White, fontSize=16.sp, maxLines=1, softWrap=false)
                        Text(nativeText(languageCode, if (createdRoom == null) "tap_create" else "tap_enter"), color=Color.White.copy(alpha=.8f), fontSize=14.sp)
                    }
                }
            }
            Row(Modifier.padding(horizontal = 12.dp).padding(top=12.dp), horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                Column(Modifier.clickable { recent=false }, horizontalAlignment=Alignment.Start) {
                    Text("Following", fontSize=14.sp, color=if(!recent) Color(0xFF111827) else Color(0xFF9CA3AF))
                }
                Column(Modifier.clickable { recent=true }, horizontalAlignment=Alignment.Start) {
                    Text("Recent", fontSize=14.sp, color=if(recent) Color(0xFF111827) else Color(0xFF9CA3AF))
                }
            }
        }
        val visibleRooms = if (recent) recentRooms else followingRooms
        if (visibleRooms.isNotEmpty()) {
            items(visibleRooms.chunked(2)) { pair ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    RoomListCard(pair[0], onOpenRoom, Modifier.weight(1f))
                    if (pair.size > 1) RoomListCard(pair[1], onOpenRoom, Modifier.weight(1f))
                    else Spacer(Modifier.weight(1f))
                }
            }
        } else {
            item {
                Column(Modifier.fillMaxWidth().padding(top=24.dp), horizontalAlignment=Alignment.CenterHorizontally) {
                    AsyncImage("https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/file_0000000047308211a02722299d1fda2e.png", null, Modifier.size(50.dp))
                    Text(nativeText(languageCode, "no_data"), color=Color(0xFF9CA3AF), fontSize=14.sp)
                }
            }
        }
    }
}

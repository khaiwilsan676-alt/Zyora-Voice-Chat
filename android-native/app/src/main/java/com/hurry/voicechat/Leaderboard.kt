package com.hurry.voicechat

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

private data class ZyoraRank(
    val id: String, val name: String, val image: String, val coins: Long,
    val country: String = "", val gender: String = "", val level: Int = 1, val rank: Int = 0
)

private data class ZyoraLeaderboardData(val rows: List<ZyoraRank>, val endsAt: Long)

private suspend fun loadZyoraLeaderboard(tab: String, period: String): ZyoraLeaderboardData =
    withContext(Dispatchers.IO) {
        val url = URL("https://zyora-server.onrender.com/api/leaderboard?tab=${URLEncoder.encode(tab, "UTF-8")}&period=${URLEncoder.encode(period, "UTF-8")}")
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 10000
        connection.readTimeout = 12000
        try {
            if (connection.responseCode !in 200..299) return@withContext ZyoraLeaderboardData(emptyList(), 0L)
            val root = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            val array = root.optJSONArray("leaderboard")
            val rows = (0 until (array?.length() ?: 0)).mapNotNull { index ->
                val item = array?.optJSONObject(index) ?: return@mapNotNull null
                ZyoraRank(
                    id = item.optString(if (tab == "room") "roomId" else "userId"),
                    name = item.optString("name", "User"),
                    image = item.optString("image"),
                    coins = item.optLong("coins"),
                    country = item.optString("country"),
                    gender = item.optString("gender"),
                    level = item.optInt("level", 1),
                    rank = item.optInt("rank", index + 1)
                )
            }
            ZyoraLeaderboardData(rows, root.optLong("endsAt"))
        } finally { connection.disconnect() }
    }

private fun zyoraFlagCode(country: String): String {
    val value = country.trim().lowercase(Locale.ROOT)
    return when (value) {
        "india" -> "in"; "singapore" -> "sg"; "saudi arabia", "saudi" -> "sa"
        "united states", "usa", "us" -> "us"; "united kingdom", "uk" -> "gb"
        "pakistan" -> "pk"; "bangladesh" -> "bd"; "nepal" -> "np"
        "indonesia" -> "id"; "philippines" -> "ph"; "thailand" -> "th"
        "malaysia" -> "my"; "uae", "united arab emirates" -> "ae"
        else -> value.takeIf { it.length == 2 } ?: ""
    }
}

private fun zyoraCoinText(value: Long): String = when {
    value >= 1_000_000_000 -> String.format(Locale.US, "%.1fB", value / 1_000_000_000.0)
    value >= 1_000_000 -> String.format(Locale.US, "%.1fM", value / 1_000_000.0)
    value >= 1_000 -> String.format(Locale.US, "%.1fK", value / 1_000.0)
    else -> value.toString()
}

@Composable
fun Leaderboard(onBack: () -> Unit = {}, initialTab: String = "rich") {
    var tab by remember { mutableStateOf(if (initialTab == "honour") "rich" else initialTab) }
    var period by remember { mutableStateOf("daily") }
    var rows by remember { mutableStateOf<List<ZyoraRank>>(emptyList()) }
    var endsAt by remember { mutableLongStateOf(0L) }
    var loading by remember { mutableStateOf(true) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val context = LocalContext.current
    val auth = remember { context.getSharedPreferences("hurry_auth", Context.MODE_PRIVATE) }
    val ownName = auth.getString("name", auth.getString("displayName", "You")).orEmpty().ifBlank { "You" }
    val ownAvatar = auth.getString("avatar", auth.getString("image", "")).orEmpty()
    val base = "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/"
    val tabs = listOf("rich" to "Rich", "charm" to "Charm", "room" to "Room")
    val periods = listOf("daily" to "Daily", "weekly" to "Weekly", "monthly" to "Monthly")

    LaunchedEffect(tab, period) {
        loading = true
        while (true) {
            runCatching { loadZyoraLeaderboard(tab, period) }.onSuccess {
                rows = it.rows
                endsAt = it.endsAt
            }.onFailure { rows = emptyList() }
            loading = false
            delay(15000)
        }
    }
    LaunchedEffect(Unit) {
        while (true) { now = System.currentTimeMillis(); delay(1000) }
    }
    val remaining = ((endsAt - now).coerceAtLeast(0L) / 1000L)
    val countdown = String.format(Locale.US, "%02d:%02d:%02d:%02d",
        remaining / 86400, (remaining % 86400) / 3600, (remaining % 3600) / 60, remaining % 60)

    Box(Modifier.fillMaxSize().background(Color(0xFF1A0204))) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(model = base + "file_0000000051d881f5af4f9cf84a56dcd3.png", contentDescription = "Back",
                    modifier = Modifier.size(32.dp).clickable(onClick = onBack))
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    tabs.forEach { (key, label) ->
                        Column(Modifier.clickable { tab = key }.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(label, color = if (tab == key) Color(0xFFE7B865) else Color.LightGray, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            if (tab == key) Spacer(Modifier.padding(top = 3.dp).width(24.dp).height(3.dp).clip(CircleShape).background(Color(0xFFE7B865)))
                        }
                    }
                }
                AsyncImage(model = base + "file_0000000073ec820b832b6dafb168dabe.png", contentDescription = "Rules", modifier = Modifier.size(30.dp))
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp).widthIn(max = 250.dp)
                .clip(CircleShape).background(Color(0xCC110A07)).border(1.dp, Color(0xFFD4AF37), CircleShape).padding(3.dp)) {
                periods.forEach { (key, label) ->
                    Box(Modifier.weight(1f).height(29.dp).clip(CircleShape)
                        .background(if (period == key) Brush.verticalGradient(listOf(Color(0xFFE7B865), Color(0xFF743410))) else Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent)))
                        .clickable { period = key }, contentAlignment = Alignment.Center) {
                        Text(label, color = if (period == key) Color.White else Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Text("⏳ $countdown", color = Color(0xFFE7B865), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            if (loading && rows.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("Loading...", color = Color.Gray) }
            } else if (rows.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("No data", color = Color.Gray, fontSize = 13.sp) }
            } else if (tab == "room") {
                LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    itemsIndexed(rows.take(50), key = { _, item -> item.id }) { index, item ->
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color.Black).padding(horizontal = 12.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("${index + 1}", Modifier.width(30.dp), color = Color(0xFFE7B865), fontSize = 17.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                            AsyncImage(model = item.image.ifBlank { base + "default_avatar.png" }, contentDescription = item.name, modifier = Modifier.padding(start = 8.dp).size(46.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                                Text(item.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("Room", color = Color.Gray, fontSize = 10.sp)
                            }
                            Text(zyoraCoinText(item.coins), color = Color(0xFFE7B865), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            AsyncImage(model = base + "file_00000000e56882119c217d508b6733dc.png", contentDescription = "Coins", modifier = Modifier.padding(start = 5.dp).size(18.dp))
                        }
                    }
                }
            } else {
                LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    item {
                        Row(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 14.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
                            if (rows.size > 1) PodiumUser(rows[1], 2, base, 60, 175)
                            if (rows.isNotEmpty()) PodiumUser(rows[0], 1, base, 72, 230)
                            if (rows.size > 2) PodiumUser(rows[2], 3, base, 60, 175)
                        }
                    }
                    itemsIndexed(rows.drop(3).take(47), key = { _, item -> item.id }) { index, item ->
                        Row(Modifier.fillMaxWidth().padding(horizontal = 9.dp, vertical = 5.dp).clip(RoundedCornerShape(9.dp)).background(Color.Black).padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("${index + 4}", Modifier.width(30.dp), color = Color(0xFFE7B865), fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                            AsyncImage(model = item.image.ifBlank { base + "default_avatar.png" }, contentDescription = item.name, modifier = Modifier.padding(start = 7.dp).size(43.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                                Text(item.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(item.country, color = Color.LightGray, fontSize = 10.sp)
                                    if (item.gender.isNotBlank()) Text(if (item.gender.equals("female", true)) " ♀ " else " ♂ ", color = if (item.gender.equals("female", true)) Color(0xFFFF77B7) else Color(0xFF66B8FF), fontSize = 11.sp)
                                    Text("Lv.${item.level}", color = Color.Gray, fontSize = 10.sp)
                                }
                            }
                            Text(zyoraCoinText(item.coins), color = Color(0xFFE7B865), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            AsyncImage(model = base + "file_00000000e56882119c217d508b6733dc.png", contentDescription = "Coins", modifier = Modifier.padding(start = 5.dp).size(17.dp))
                        }
                    }
                }
            }
        }
        Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(12.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF263B54)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = ownAvatar.ifBlank { base + "default_avatar.png" }, contentDescription = ownName, modifier = Modifier.size(38.dp).clip(CircleShape), contentScale = ContentScale.Crop)
            Text(ownName, Modifier.weight(1f).padding(start = 9.dp), color = Color.White, fontSize = 13.sp, maxLines = 1)
        }
    }
}

@Composable
private fun PodiumUser(user: ZyoraRank, rank: Int, base: String, avatarSize: Int, frameSize: Int) {
    val frame = when (rank) {
        1 -> "file_00000000a0e48207ade38b36ab946ecb.png"
        2 -> "file_0000000069b88207b9fdd01576b71ee9.png"
        else -> "file_00000000ecc882079f34a9356b53dc8e.png"
    }
    Column(Modifier.width(if (rank == 1) 150.dp else 112.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(frameSize.dp), contentAlignment = Alignment.Center) {
            AsyncImage(model = user.image.ifBlank { base + "default_avatar.png" }, contentDescription = user.name,
                modifier = Modifier.size(avatarSize.dp).clip(CircleShape), contentScale = ContentScale.Crop)
            AsyncImage(model = base + frame, contentDescription = "Rank $rank frame",
                modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        }
        Text(user.name, color = Color.White, fontSize = if (rank == 1) 13.sp else 11.sp, fontWeight = FontWeight.Bold,
            maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            val code = zyoraFlagCode(user.country)
            if (code.isNotBlank()) AsyncImage(model = "https://flagcdn.com/w40/$code.png", contentDescription = "Country flag", modifier = Modifier.size(width = 18.dp, height = 12.dp), contentScale = ContentScale.Crop)
            if (user.gender.isNotBlank()) Text(if (user.gender.equals("female", true)) "♀" else "♂", color = if (user.gender.equals("female", true)) Color(0xFFFF77B7) else Color(0xFF66B8FF), fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp))
        }
        AsyncImage(model = base + "IMG_20261028_004355.png", contentDescription = "Level",
            modifier = Modifier.width(36.dp).height(16.dp), contentScale = ContentScale.FillBounds)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(zyoraCoinText(user.coins), color = Color(0xFFE7B865), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            AsyncImage(model = base + "file_00000000e56882119c217d508b6733dc.png", contentDescription = "Coins", modifier = Modifier.padding(start = 3.dp).size(15.dp))
        }
    }
}

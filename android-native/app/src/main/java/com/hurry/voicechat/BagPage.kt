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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

private data class BagRewardItem(val id: String, val name: String, val tab: String, val image: String)

@Composable
fun BagPage(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val authPrefs = remember { context.getSharedPreferences("hurry_auth", android.content.Context.MODE_PRIVATE) }
    val userId = remember {
        authPrefs.getString("accountId", null)?.takeIf { it.isNotBlank() }
            ?: authPrefs.getString("uid", "").orEmpty()
    }
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf("Avatar Frame") }
    var rewards by remember { mutableStateOf<List<BagRewardItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(userId) {
        if (userId.isBlank()) { loading = false; return@LaunchedEffect }
        val result = runCatching {
            withContext(Dispatchers.IO) {
                val encoded = URLEncoder.encode(userId, "UTF-8")
                val connection = (URL("https://zyora-server.onrender.com/api/store/inventory/$encoded").openConnection() as HttpURLConnection).apply {
                    connectTimeout = 10000
                    readTimeout = 10000
                }
                try {
                    if (connection.responseCode !in 200..299) error("Bag inventory unavailable")
                    val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                    val array = json.optJSONArray("items") ?: org.json.JSONArray()
                    buildList {
                        for (i in 0 until array.length()) {
                            val item = array.optJSONObject(i) ?: continue
                            val tab = item.optString("tab")
                            if (tab == "Avatar Frame" || tab == "Theme") {
                                add(BagRewardItem(
                                    item.optString("itemId"),
                                    item.optString("name", "Reward"),
                                    tab,
                                    item.optString("image")
                                ))
                            }
                        }
                    }
                } finally { connection.disconnect() }
            }
        }
        rewards = result.getOrDefault(emptyList())
        loading = false
    }

    Column(Modifier.fillMaxSize().background(Color(0xFFF5F6FA))) {
        Row(
            Modifier.fillMaxWidth().background(Color.White).statusBarsPadding().padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("‹", fontSize = 34.sp, color = Color(0xFF222222), modifier = Modifier.clickable(onClick = onBack))
            Spacer(Modifier.width(12.dp))
            Text("Bag", fontSize = 20.sp, color = Color(0xFF111827), fontWeight = FontWeight.Bold)
        }
        Row(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            listOf("Avatar Frame", "Theme").forEach { tab ->
                Column(Modifier.clickable { selectedTab = tab }, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (tab == "Avatar Frame") "Frame" else "Theme", color = if (selectedTab == tab) Color(0xFF2563EB) else Color(0xFF6B7280), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(Modifier.height(5.dp))
                    Box(Modifier.width(42.dp).height(3.dp).clip(RoundedCornerShape(3.dp)).background(if (selectedTab == tab) Color(0xFF2563EB) else Color.Transparent))
                }
            }
        }
        val visibleItems = rewards.filter { it.tab == selectedTab }
        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Loading...", color = Color.Gray) }
        } else if (visibleItems.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(if (selectedTab == "Avatar Frame") "No frames yet" else "No themes yet", color = Color(0xFF9CA3AF), fontSize = 14.sp)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(visibleItems, key = { it.id }) { item ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = if (item.image.startsWith("http")) item.image else "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/" + item.image.removePrefix("/"),
                            contentDescription = item.name,
                            modifier = Modifier.size(64.dp),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.name, color = Color(0xFF1F2937), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("Permanent reward", color = Color(0xFF9CA3AF), fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

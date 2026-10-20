package com.hurry.voicechat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import io.socket.client.IO
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage

private const val PROFILE_ASSETS = "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/"

@Composable
fun PublicProfile(
    user: RoomProfileUser,
    onBack: () -> Unit = {},
    onFollow: () -> Unit = {},
    onMessage: () -> Unit = {},
    currentUserId: String = ""
) {
    val blue = Color(0xFF268CFA)
    val avatar = user.image.ifBlank { PROFILE_ASSETS + "default-avatar.png" }
    val targetUid = user.uid.ifBlank { user.id }.ifBlank { user.accountId }
    var coverPhoto by remember(targetUid) { mutableStateOf("") }
    var followerCount by remember(targetUid) { mutableIntStateOf(user.followers) }
    var isFollowing by remember(targetUid) { mutableStateOf(user.isFollowing) }
    var followBusy by remember(targetUid) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(targetUid, currentUserId) {
        if (targetUid.isBlank()) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            runCatching {
                val url = URL("https://hurry-voice-chat-lz75.onrender.com/api/follows?targetUid=" +
                    java.net.URLEncoder.encode(targetUid, "UTF-8") + "&followerId=" +
                    java.net.URLEncoder.encode(currentUserId, "UTF-8"))
                val conn = (url.openConnection() as HttpURLConnection).apply { connectTimeout = 10000; readTimeout = 10000 }
                try {
                    if (conn.responseCode in 200..299) {
                        val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                        val count = json.optInt("followers", user.followers)
                        val following = json.optBoolean("following", user.isFollowing)
                        withContext(Dispatchers.Main) {
                            followerCount = count
                            isFollowing = following
                        }
                    }
                } finally { conn.disconnect() }
            }
            runCatching {
                val url = URL("https://hurry-voice-chat-lz75.onrender.com/api/users?uid=" +
                    java.net.URLEncoder.encode(targetUid, "UTF-8"))
                val conn = (url.openConnection() as HttpURLConnection).apply { connectTimeout = 10000; readTimeout = 10000 }
                try {
                    if (conn.responseCode in 200..299) {
                        val root = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                        val profile = root.optJSONObject("user") ?: root.optJSONArray("users")?.optJSONObject(0)
                        val cover = profile?.optString("coverPhoto", profile.optString("coverImage", "")).orEmpty()
                        withContext(Dispatchers.Main) { coverPhoto = cover }
                    }
                } finally { conn.disconnect() }
            }
        }
    }

    DisposableEffect(targetUid, currentUserId) {
        val socket = runCatching { IO.socket("https://hurry-voice-chat-lz75.onrender.com") }.getOrNull()
        val listener = io.socket.emitter.Emitter.Listener { args ->
            val data = args.firstOrNull() as? JSONObject ?: return@Listener
            if (data.optString("targetUid") == targetUid) {
                followerCount = data.optInt("followers", followerCount)
                if (data.optString("followerId") == currentUserId) {
                    isFollowing = data.optBoolean("following", data.optString("action") == "follow")
                }
            }
        }
        socket?.on("follow_updated", listener)
        socket?.connect()
        onDispose { socket?.off("follow_updated", listener); socket?.disconnect(); socket?.close() }
    }

    val toggleFollow: () -> Unit = {
        if (currentUserId.isNotBlank() && targetUid.isNotBlank() && currentUserId != targetUid && !followBusy) {
            val nextFollowing = !isFollowing
            isFollowing = nextFollowing
            followerCount = (followerCount + if (nextFollowing) 1 else -1).coerceAtLeast(0)
            followBusy = true
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        val conn = (URL("https://hurry-voice-chat-lz75.onrender.com/api/follows").openConnection() as HttpURLConnection).apply {
                            requestMethod = "POST"
                            connectTimeout = 10000
                            readTimeout = 10000
                            doOutput = true
                            setRequestProperty("Content-Type", "application/json")
                        }
                        try {
                            val body = JSONObject().put("followerId", currentUserId).put("targetUid", targetUid)
                                .put("action", if (nextFollowing) "follow" else "unfollow").toString()
                            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                            if (conn.responseCode !in 200..299) error("Follow update failed")
                            val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                            Triple(json.optBoolean("success"), json.optInt("followers", followerCount), json.optBoolean("following", nextFollowing))
                        } finally { conn.disconnect() }
                    }.getOrNull()
                }
                if (result?.first == true) {
                    followerCount = result.second
                    isFollowing = result.third
                    onFollow()
                } else {
                    isFollowing = !nextFollowing
                    followerCount = (followerCount + if (nextFollowing) -1 else 1).coerceAtLeast(0)
                }
                followBusy = false
            }
        }
    }
    Column(Modifier.fillMaxSize().background(Color.White)) {
        Box(Modifier.fillMaxWidth().height(350.dp).background(Color(0xFF263445))) {
            AsyncImage(
                model = coverPhoto.ifBlank { avatar },
                contentDescription = "Profile cover",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x55000000), Color.Transparent, Color(0xAA000000)))))
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("‹", color = Color.White, fontSize = 38.sp, modifier = Modifier.clickable(onClick = onBack))
                Text("⋯", color = Color.White, fontSize = 30.sp)
            }
            Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(horizontal = 18.dp, vertical = 18.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    AsyncImage(
                        model = avatar,
                        contentDescription = user.name,
                        modifier = Modifier.size(82.dp).clip(CircleShape).background(Color.LightGray),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f).padding(bottom = 4.dp)) {
                        Text(user.name.ifBlank { "Zyora User" }, color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        Text("ID: ${user.accountId.ifBlank { user.id }}", color = Color.White, fontSize = 12.sp)
                    }
                    Text("Lv.1", color = Color.White, fontSize = 12.sp, modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xAA222222)).padding(horizontal = 7.dp, vertical = 4.dp))
                }
            }
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProfileStat(followerCount.toString(), "Fans")
                ProfileStat("0", "Following")
                ProfileStat("0", "Visitors")
                ProfileStat("0", "Friends")
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                Text(user.flag.ifBlank { "🇮🇳" }, fontSize = 18.sp)
                Spacer(Modifier.width(7.dp))
                Text(user.country.ifBlank { "India" }, color = Color(0xFF555B65), fontSize = 13.sp)
                Spacer(Modifier.width(10.dp))
                Text("•  ${user.gender}  •  ${user.age}", color = Color(0xFF777D86), fontSize = 12.sp)
            }
            Text(
                user.bio.ifBlank { "No bio added yet" },
                color = if (user.bio.isBlank()) Color(0xFF9CA3AF) else Color(0xFF555B65),
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 12.dp, bottom = 18.dp)
            )
            HorizontalDivider(color = Color(0xFFF0F0F0))
            Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.weight(1f).height(42.dp).clip(RoundedCornerShape(22.dp)).background(if (isFollowing) Color(0xFFEAF2FC) else blue).clickable(onClick = toggleFollow), contentAlignment = Alignment.Center) {
                    Text(if (isFollowing) "Following" else "+ Follow", color = if (isFollowing) Color(0xFF2676D2) else Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
                Box(Modifier.weight(1f).height(42.dp).clip(RoundedCornerShape(22.dp)).background(Color(0xFFF1F7FF)).clickable(onClick = onMessage), contentAlignment = Alignment.Center) {
                    Text("Message", color = blue, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(26.dp)) {
                Text("Profile", color = blue, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text("Album", color = Color(0xFF777D86), fontSize = 15.sp)
                Text("Room", color = Color(0xFF777D86), fontSize = 15.sp)
            }
            Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                Text("No posts yet", color = Color(0xFFB0B5BC), fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun ProfileStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Color(0xFF20242A), fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
        Text(label, color = Color(0xFF8B919A), fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp))
    }
}

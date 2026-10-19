package com.hurry.voicechat

import android.content.Intent

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage

data class NativeMusicTrack(val id: String, val name: String, val url: String)

@Composable
fun Fourgride(
    onClose: () -> Unit,
    onClearChat: () -> Unit,
    publicMsgOff: Boolean,
    onTogglePublicMsg: () -> Unit,
    speaker: Boolean,
    onToggleSpeaker: () -> Unit,
    onMusicPlay: ((NativeMusicTrack) -> Unit)? = null,
    onMusicMinimize: ((NativeMusicTrack?) -> Unit)? = null,
    onOpenStore: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("hurry_room_tools", android.content.Context.MODE_PRIVATE) }
    val musicPrefs = remember { context.getSharedPreferences("zyora_phone_music", android.content.Context.MODE_PRIVATE) }
    var musicTracks by remember { mutableStateOf(emptyList<NativeMusicTrack>()) }
    var playingUri by remember { mutableStateOf<String?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        musicTracks = musicPrefs.getStringSet("uris", emptySet()).orEmpty().mapNotNull { raw ->
            runCatching {
                val uri = Uri.parse(raw)
                val name = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                } ?: uri.lastPathSegment ?: "Music"
                NativeMusicTrack(raw, name, raw)
            }.getOrNull()
        }
    }
    val musicPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) {
            val added = uris.mapNotNull { uri ->
                runCatching {
                    context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    val name = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) cursor.getString(0) else null
                    } ?: uri.lastPathSegment ?: "Music"
                    NativeMusicTrack(uri.toString(), name, uri.toString())
                }.getOrNull()
            }
            val updatedTracks = (musicTracks + added).distinctBy { it.id }
            musicTracks = updatedTracks
            musicPrefs.edit().putStringSet("uris", updatedTracks.map { it.id }.toSet()).apply()
        }
    }
    var entryEffect by remember { mutableStateOf(prefs.getBoolean("entry_effect", false)) }
    var giftEffect by remember { mutableStateOf(prefs.getBoolean("gift_effect", false)) }
    var showMusic by remember { mutableStateOf(false) }
    var showLuckyBag by remember { mutableStateOf(false) }

    val base = "https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/"
    val tools = listOf(
        ToolItem("Clear-Chat", "IMG_20260814_110525.png") { onClearChat() },
        ToolItem("Public msg", "IMG_20260814_110608.png") { onTogglePublicMsg() },
        ToolItem("Entry Effect", "IMG_20260814_110709.png") {
            entryEffect = !entryEffect
            prefs.edit().putBoolean("entry_effect", entryEffect).apply()
        },
        ToolItem("Gift Effect", "IMG_20260814_110727.png") {
            giftEffect = !giftEffect
            prefs.edit().putBoolean("gift_effect", giftEffect).apply()
        },
        ToolItem("Music", "IMG_20260814_144255.png") { showMusic = true },
        ToolItem("Speaker", "IMG_20260814_110628.png") { onToggleSpeaker() },
        ToolItem("Store", "IMG_20260814_110501.png") { onOpenStore?.invoke("store") },
        ToolItem("My-Iteam", "IMG_20260814_110545.png") { onOpenStore?.invoke("bag") }
    )
    val playTools = listOf(
        ToolItem("Lucky bag", "1790602570756~2.jpg") { showLuckyBag = true },
        ToolItem("Pk Battle", "IMG_20260814_110802.png") { }
    )

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Box(Modifier.fillMaxSize().clickable { onClose() })
        Column(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                .background(Color.White)
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp)
                .heightIn(max = LocalConfiguration.current.screenHeightDp.dp * 0.70f)
        ) {
            Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Tools", modifier = Modifier.weight(1f), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
            }

            Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ToolCell(Modifier.weight(1f), "Clear-Chat", base + "IMG_20260814_110525.png", onClick = onClearChat)
                ToolCell(Modifier.weight(1f), "Public msg", base + "IMG_20260814_110608.png", toggle = !publicMsgOff, onClick = onTogglePublicMsg)
                ToolCell(Modifier.weight(1f), "Entry Effect", base + "IMG_20260814_110709.png", toggle = entryEffect, onClick = {
                    entryEffect = !entryEffect
                    prefs.edit().putBoolean("entry_effect", entryEffect).apply()
                })
                ToolCell(Modifier.weight(1f), "Gift Effect", base + "IMG_20260814_110727.png", toggle = giftEffect, onClick = {
                    giftEffect = !giftEffect
                    prefs.edit().putBoolean("gift_effect", giftEffect).apply()
                })
            }

            Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ToolCell(Modifier.weight(1f), "Music", base + "IMG_20260814_144255.png", onClick = { showMusic = true })
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    AsyncImage(model = base + "IMG_20260814_110628.png", contentDescription = "Speaker", modifier = Modifier.size(48.dp), contentScale = ContentScale.Fit)
                    Row(Modifier.padding(top = 4.dp).clickable { onToggleSpeaker() }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Speaker", fontSize = 10.sp, color = Color(0xFF374151), maxLines = 1, softWrap = false)
                        ToolToggle(speaker)
                    }
                }
                ToolCell(Modifier.weight(1f), "Store", base + "IMG_20260814_110501.png", onClick = { onOpenStore?.invoke("store") })
                ToolCell(Modifier.weight(1f), "My-Iteam", base + "IMG_20260814_110545.png", onClick = { onOpenStore?.invoke("bag") })
            }

            Text("Play Tools", modifier = Modifier.padding(top = 8.dp, bottom = 8.dp), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1F2937))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ToolCell(Modifier.weight(1f), "Lucky bag", base + "1790602570756~2.jpg", onClick = { showLuckyBag = true })
                ToolCell(Modifier.weight(1f), "Pk Battle", base + "IMG_20260814_110802.png", onClick = {})
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.weight(1f))
            }
        }
    }

    if (showMusic) {
        BackHandler(enabled = true) { showMusic = false }
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), contentAlignment = Alignment.Center) {
                    IconButton(onClick = { showMusic = false }, modifier = Modifier.align(Alignment.CenterStart)) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Text("Music", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    IconButton(
                        onClick = {
                            onMusicMinimize?.invoke(musicTracks.firstOrNull { it.url == playingUri })
                            showMusic = false
                            onClose()
                        },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Minimize music", tint = Color.White)
                    }
                }
                if (musicTracks.isEmpty()) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No music added yet. Tap below to add music from your phone.", color = Color.White.copy(alpha = .45f), fontSize = 13.sp)
                    }
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp)) {
                        items(musicTracks, key = { it.id }) { track ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(track.name, Modifier.weight(1f), color = Color.White, fontSize = 14.sp, maxLines = 1)
                                IconButton(onClick = {
                                    if (playingUri == track.url && isPlaying) {
                                        context.startService(Intent(context, RoomMusicService::class.java).setAction(RoomMusicService.ACTION_PAUSE))
                                        isPlaying = false
                                    } else {
                                        val playIntent = Intent(context, RoomMusicService::class.java)
                                            .setAction(RoomMusicService.ACTION_PLAY)
                                            .putExtra(RoomMusicService.EXTRA_URI, track.url)
                                            .putExtra(RoomMusicService.EXTRA_TITLE, track.name)
                                        androidx.core.content.ContextCompat.startForegroundService(context, playIntent)
                                        playingUri = track.url
                                        isPlaying = true
                                        onMusicPlay?.invoke(track)
                                    }
                                }) {
                                    Icon(if (playingUri == track.url && isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = "Play music", tint = Color.White)
                                }
                                IconButton(onClick = {
                                    if (playingUri == track.url) {
                                        context.startService(Intent(context, RoomMusicService::class.java).setAction(RoomMusicService.ACTION_STOP))
                                        playingUri = null
                                        isPlaying = false
                                    }
                                    musicTracks = musicTracks.filterNot { it.id == track.id }
                                    musicPrefs.edit().putStringSet("uris", musicTracks.map { it.id }.toSet()).apply()
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove music", tint = Color.White.copy(alpha = .8f))
                                }
                            }
                        }
                    }
                }
                Button(
                    onClick = { musicPicker.launch(arrayOf("audio/*")) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Add Music From Phone", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
    if (showLuckyBag) {
        LuckyBag(onClose = { showLuckyBag = false })
    }
}

private data class ToolItem(val title: String, val image: String, val action: () -> Unit)

@Composable
private fun ToolToggle(enabled: Boolean) {
    Box(
        Modifier.padding(1.dp).size(width = 24.dp, height = 16.dp)
            .clip(RoundedCornerShape(50)).background(if (enabled) Color(0xFF3B82F6) else Color(0xFFD1D5DB))
            .padding(2.dp),
        contentAlignment = if (enabled) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(Modifier.size(12.dp).clip(RoundedCornerShape(50)).background(Color.White))
    }
}

@Composable
private fun ToolCell(
    modifier: Modifier,
    title: String,
    imageUrl: String,
    toggle: Boolean? = null,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            AsyncImage(model = imageUrl, contentDescription = title, modifier = Modifier.fillMaxSize().then(if (toggle == null) Modifier.clickable(onClick = onClick) else Modifier), contentScale = ContentScale.Fit)
            if (toggle != null) Box(Modifier.align(Alignment.BottomEnd).clickable(onClick = onClick)) { ToolToggle(toggle) }
        }
        Text(title, modifier = Modifier.padding(top = 4.dp), fontSize = 10.sp, color = Color(0xFF374151), maxLines = 1, softWrap = false)
    }
}

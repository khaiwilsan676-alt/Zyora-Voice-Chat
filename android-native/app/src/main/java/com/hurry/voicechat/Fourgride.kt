package com.hurry.voicechat

import android.Manifest
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ✅ GLOBAL STATE — sheet close hone ke baad bhi survive karta hai
object MusicPlaybackState {
    var currentPlayingUri: String? = null
    var isPlaying: Boolean = false
}

data class NativeMusicTrack(val id: String, val name: String, val url: String, val duration: Long = 0L)
data class DeviceMusicItem(val id: String, val name: String, val uri: String, val duration: Long)

private fun formatMusicDuration(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%d:%02d".format(min, sec)
}

private fun queryDeviceMusic(context: android.content.Context): List<DeviceMusicItem> {
    val list = mutableListOf<DeviceMusicItem>()
    val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.DISPLAY_NAME,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.DURATION
    )
    val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
    val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"
    runCatching {
        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val title = cursor.getString(titleCol)?.takeIf { it.isNotBlank() }
                    ?: cursor.getString(nameCol)?.takeIf { it.isNotBlank() }
                    ?: "Music"
                val duration = cursor.getLong(durationCol)
                val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                list.add(DeviceMusicItem(id.toString(), title, uri.toString(), duration))
            }
        }
    }
    return list
}

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

    // ✅ INIT from global state (survives sheet close)
    var playingUri by remember { mutableStateOf(MusicPlaybackState.currentPlayingUri) }
    var isPlaying by remember { mutableStateOf(MusicPlaybackState.isPlaying) }

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
                    context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
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

    var showDeviceMusic by remember { mutableStateOf(false) }
    var deviceMusicTracks by remember { mutableStateOf<List<DeviceMusicItem>>(emptyList()) }
    var addedDeviceIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var deviceMusicLoading by remember { mutableStateOf(false) }
    var musicDurations by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }

    val audioPermission = if (Build.VERSION.SDK_INT >= 33)
        Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            showDeviceMusic = true
        } else {
            musicPicker.launch(arrayOf("audio/*"))
        }
    }

    LaunchedEffect(showMusic, musicTracks.size) {
        if (showMusic && musicTracks.isNotEmpty()) {
            val durations = withContext(Dispatchers.IO) {
                musicTracks.associate { track ->
                    val existing = track.duration
                    val dur = if (existing > 0L) existing else runCatching {
                        val mmr = MediaMetadataRetriever()
                        try {
                            mmr.setDataSource(context, Uri.parse(track.url))
                            mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                        } finally {
                            runCatching { mmr.release() }
                        }
                    }.getOrDefault(0L)
                    track.id to dur
                }
            }
            musicDurations = durations
        }
    }

    LaunchedEffect(showDeviceMusic) {
        if (showDeviceMusic) {
            deviceMusicLoading = true
            deviceMusicTracks = withContext(Dispatchers.IO) { queryDeviceMusic(context) }
            val existingUris = musicTracks.map { it.url }.toSet()
            addedDeviceIds = deviceMusicTracks.filter { it.uri in existingUris }.map { it.id }.toSet()
            deviceMusicLoading = false
        }
    }

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
        BackHandler(enabled = true) {
            val currentTrack = musicTracks.firstOrNull { it.url == playingUri }
            if (currentTrack != null) onMusicPlay?.invoke(currentTrack)
            showMusic = false
            onClose()
        }
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), contentAlignment = Alignment.Center) {
                    IconButton(
                        onClick = {
                            val currentTrack = musicTracks.firstOrNull { it.url == playingUri }
                            if (currentTrack != null) onMusicPlay?.invoke(currentTrack)
                            showMusic = false
                            onClose()
                        },
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Text("Music", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                if (musicTracks.isEmpty()) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No music added yet. Tap below to add music from your phone.", color = Color.White.copy(alpha = .45f), fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp)) {
                        items(musicTracks, key = { it.id }) { track ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(track.name, color = Color.White, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        formatMusicDuration(musicDurations[track.id] ?: track.duration),
                                        color = Color.White.copy(alpha = .55f),
                                        fontSize = 11.sp
                                    )
                                }
                                IconButton(onClick = {
                                    if (playingUri == track.url && isPlaying) {
                                        context.startService(Intent(context, RoomMusicService::class.java).setAction(RoomMusicService.ACTION_PAUSE))
                                        isPlaying = false
                                        MusicPlaybackState.isPlaying = false
                                        MusicPlaybackState.currentPlayingUri = playingUri
                                    } else {
                                        val playIntent = Intent(context, RoomMusicService::class.java)
                                            .setAction(RoomMusicService.ACTION_PLAY)
                                            .putExtra(RoomMusicService.EXTRA_URI, track.url)
                                            .putExtra(RoomMusicService.EXTRA_TITLE, track.name)
                                        ContextCompat.startForegroundService(context, playIntent)
                                        playingUri = track.url
                                        isPlaying = true
                                        MusicPlaybackState.currentPlayingUri = track.url
                                        MusicPlaybackState.isPlaying = true
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
                                        MusicPlaybackState.currentPlayingUri = null
                                        MusicPlaybackState.isPlaying = false
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
                    onClick = {
                        if (ContextCompat.checkSelfPermission(context, audioPermission) == PackageManager.PERMISSION_GRANTED) {
                            showDeviceMusic = true
                        } else {
                            permissionLauncher.launch(audioPermission)
                        }
                    },
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

    if (showDeviceMusic) {
        BackHandler(enabled = true) { showDeviceMusic = false }
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), contentAlignment = Alignment.Center) {
                    IconButton(onClick = { showDeviceMusic = false }, modifier = Modifier.align(Alignment.CenterStart)) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Text("Add Music", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }

                if (deviceMusicLoading) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF3B82F6))
                    }
                } else if (deviceMusicTracks.isEmpty()) {
                    Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                        Text("No music found on device.", color = Color.White.copy(alpha = .45f), fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp)) {
                        items(deviceMusicTracks, key = { it.id }) { device ->
                            val isAdded = device.id in addedDeviceIds
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(device.name, color = Color.White, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        formatMusicDuration(device.duration),
                                        color = Color.White.copy(alpha = .55f),
                                        fontSize = 11.sp
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Button(
                                    onClick = {
                                        if (!isAdded) {
                                            val newTrack = NativeMusicTrack(device.uri, device.name, device.uri, device.duration)
                                            val updated = (musicTracks + newTrack).distinctBy { it.id }
                                            musicTracks = updated
                                            musicPrefs.edit().putStringSet("uris", updated.map { it.id }.toSet()).apply()
                                            addedDeviceIds = addedDeviceIds + device.id
                                        }
                                    },
                                    shape = RoundedCornerShape(50),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isAdded) Color(0xFF6B7280) else Color(0xFF3B82F6)
                                    ),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                    enabled = !isAdded
                                ) {
                                    Text(if (isAdded) "Added" else "Add", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }

                val allAdded = deviceMusicTracks.isNotEmpty() && deviceMusicTracks.all { it.id in addedDeviceIds }
                Button(
                    onClick = {
                        val newTracks = deviceMusicTracks.filter { it.id !in addedDeviceIds }.map {
                            NativeMusicTrack(it.uri, it.name, it.uri, it.duration)
                        }
                        if (newTracks.isNotEmpty()) {
                            val updated = (musicTracks + newTracks).distinctBy { it.id }
                            musicTracks = updated
                            musicPrefs.edit().putStringSet("uris", updated.map { it.id }.toSet()).apply()
                            addedDeviceIds = addedDeviceIds + newTracks.map { it.id }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (allAdded) Color(0xFF6B7280) else Color(0xFF3B82F6)
                    ),
                    enabled = !allAdded
                ) {
                    Text(if (allAdded) "All Added" else "Add All", color = Color.White, fontWeight = FontWeight.SemiBold)
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

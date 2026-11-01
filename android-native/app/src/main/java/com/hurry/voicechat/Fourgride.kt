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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloseFullscreen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ✅ GLOBAL STATE
object MusicPlaybackState {
    var currentPlayingUri: String? by mutableStateOf(null)
    var isPlaying: Boolean by mutableStateOf(false)
    var currentTrack: NativeMusicTrack? by mutableStateOf(null) 
    var isMusicPageOpen: Boolean by mutableStateOf(false) 
    var wasPlayedOnMusicPage: Boolean by mutableStateOf(false)
    var musicPageBackPressed: Boolean by mutableStateOf(false)
    var playlist: List<NativeMusicTrack> by mutableStateOf(emptyList())
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
    onMusicPlaybackChanged: ((NativeMusicTrack, Boolean) -> Unit)? = null,
    onMusicMinimize: ((NativeMusicTrack?) -> Unit)? = null,
    onMusicPlaylistChanged: ((List<NativeMusicTrack>) -> Unit)? = null,
    onOpenStore: ((String) -> Unit)? = null,
    luckyBagSenderId: String = "",
    luckyBagSenderName: String = "User",
    luckyBagSenderImage: String = "",
    luckyBagRoomId: String = "",
    luckyBagRoomName: String = "Room",
    onLuckyBagSent: ((String, String, Long, Int, String) -> Unit)? = null
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("hurry_room_tools", android.content.Context.MODE_PRIVATE) }
    val musicPrefs = remember { context.getSharedPreferences("zyora_phone_music", android.content.Context.MODE_PRIVATE) }
    var musicTracks by remember { mutableStateOf(emptyList<NativeMusicTrack>()) }

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

    var entryEffect by remember { mutableStateOf(prefs.getBoolean("entry_effect", false)) }
    var giftEffect by remember { mutableStateOf(prefs.getBoolean("gift_effect", false)) }
    var showMusic by remember { mutableStateOf(false) }
    var showLuckyBag by remember { mutableStateOf(false) }
    var hideToolsAfterLucky by remember { mutableStateOf(false) }

    var showDeviceMusic by remember { mutableStateOf(false) }
    var deviceMusicTracks by remember { mutableStateOf<List<DeviceMusicItem>>(emptyList()) }
    var addedDeviceIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var deviceMusicLoading by remember { mutableStateOf(false) }
    var musicDurations by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }

    val audioPermission = if (Build.VERSION.SDK_INT >= 33)
        Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        showDeviceMusic = true
    }
    
    LaunchedEffect(showMusic) {
        MusicPlaybackState.isMusicPageOpen = showMusic
    }

    LaunchedEffect(musicTracks) {
        MusicPlaybackState.playlist = musicTracks
        onMusicPlaylistChanged?.invoke(musicTracks)
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
        ToolItem("Clear-Chat", "IMG_20261028_182124.png") { onClearChat() },
        ToolItem("Public msg", "IMG_20261028_182153.png") { onTogglePublicMsg() },
        ToolItem("Entry Effect", "IMG_20261028_182218.png") {
            entryEffect = !entryEffect
            prefs.edit().putBoolean("entry_effect", entryEffect).apply()
        },
        ToolItem("Gift Effect", "IMG_20261028_182239.png") {
            giftEffect = !giftEffect
            prefs.edit().putBoolean("gift_effect", giftEffect).apply()
        },
        ToolItem("Music", "IMG_20261028_182305.png") { MusicPlaybackState.wasPlayedOnMusicPage = false; MusicPlaybackState.musicPageBackPressed = false; showMusic = true },
        ToolItem("Speaker", "IMG_20261028_182329.png") { onToggleSpeaker() },
        ToolItem("Store", "IMG_20261028_234208.png") { onOpenStore?.invoke("store") },
        ToolItem("My-Iteam", "IMG_20261028_182358.png") { onOpenStore?.invoke("bag") }
    )
    val playTools = listOf(
        ToolItem("Lucky bag", "IMG_20261028_182446.png") { hideToolsAfterLucky = true; showLuckyBag = true },
        ToolItem("Pk Battle", "IMG_20261028_182422.png") { }
    )

    if (!hideToolsAfterLucky) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Box(Modifier.fillMaxSize().clickable { onClose() })
        Column(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                .background(Color.Black.copy(alpha = 0.85f))
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp)
                .heightIn(max = LocalConfiguration.current.screenHeightDp.dp * 0.70f)
        ) {
            Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Tools", modifier = Modifier.weight(1f), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ToolCell(Modifier.weight(1f), "Clear-Chat", base + "IMG_20261028_182124.png", onClick = onClearChat)
                ToolCell(Modifier.weight(1f), "Public msg", base + "IMG_20261028_182153.png", toggle = !publicMsgOff, onClick = onTogglePublicMsg)
                ToolCell(Modifier.weight(1f), "Entry Effect", base + "IMG_20261028_182218.png", toggle = entryEffect, onClick = {
                    entryEffect = !entryEffect
                    prefs.edit().putBoolean("entry_effect", entryEffect).apply()
                })
                ToolCell(Modifier.weight(1f), "Gift Effect", base + "IMG_20261028_182239.png", toggle = giftEffect, onClick = {
                    giftEffect = !giftEffect
                    prefs.edit().putBoolean("gift_effect", giftEffect).apply()
                })
            }

            Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ToolCell(Modifier.weight(1f), "Music", base + "IMG_20261028_182305.png", onClick = { MusicPlaybackState.wasPlayedOnMusicPage = false; MusicPlaybackState.musicPageBackPressed = false; showMusic = true })
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    AsyncImage(model = base + "IMG_20261028_182329.png", contentDescription = "Speaker", modifier = Modifier.size(48.dp), contentScale = ContentScale.Fit)
                    Row(Modifier.padding(top = 4.dp).clickable { onToggleSpeaker() }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Speaker", fontSize = 10.sp, color = Color.White.copy(alpha = 0.9f), maxLines = 1, softWrap = false)
                        ToolToggle(speaker)
                    }
                }
                ToolCell(Modifier.weight(1f), "Store", base + "IMG_20261028_234208.png", onClick = { onOpenStore?.invoke("store") })
                ToolCell(Modifier.weight(1f), "My-Iteam", base + "IMG_20261028_182358.png", onClick = { onOpenStore?.invoke("bag") })
            }

            Text("Play Tools", modifier = Modifier.padding(top = 8.dp, bottom = 8.dp), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ToolCell(Modifier.weight(1f), "Lucky bag", base + "IMG_20261028_182446.png", onClick = { hideToolsAfterLucky = true; showLuckyBag = true })
                ToolCell(Modifier.weight(1f), "Pk Battle", base + "IMG_20261028_182422.png", onClick = {})
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.weight(1f))
            }
        }
    }

    }

    if (showMusic) {
        BackHandler(enabled = true) {
            val currentTrack = musicTracks.firstOrNull { it.url == MusicPlaybackState.currentPlayingUri } ?: MusicPlaybackState.currentTrack
            if (MusicPlaybackState.currentPlayingUri != null && currentTrack != null) {
                MusicPlaybackState.musicPageBackPressed = MusicPlaybackState.wasPlayedOnMusicPage && MusicPlaybackState.isPlaying
                onMusicMinimize?.invoke(currentTrack) 
            } else {
                MusicPlaybackState.musicPageBackPressed = false
                onMusicMinimize?.invoke(null) 
            }
            showMusic = false
            MusicPlaybackState.isMusicPageOpen = false
            onClose()
        }
        Box(Modifier.fillMaxSize().background(Color(0xFF141414))) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                Box(Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 8.dp), contentAlignment = Alignment.Center) {
                    IconButton(
                        onClick = {
                            val currentTrack = musicTracks.firstOrNull { it.url == MusicPlaybackState.currentPlayingUri } ?: MusicPlaybackState.currentTrack
                            if (MusicPlaybackState.currentPlayingUri != null && currentTrack != null) {
                                MusicPlaybackState.musicPageBackPressed = MusicPlaybackState.wasPlayedOnMusicPage && MusicPlaybackState.isPlaying
                                onMusicMinimize?.invoke(currentTrack)
                            } else {
                                MusicPlaybackState.musicPageBackPressed = false
                                onMusicMinimize?.invoke(null)
                            }
                            showMusic = false
                            MusicPlaybackState.isMusicPageOpen = false
                            onClose()
                        },
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Text("Music", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                if (musicTracks.isEmpty()) {
                    Column(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        AsyncImage(
                            model = base + "IMG_20261101_193716.png",
                            contentDescription = "No Music",
                            modifier = Modifier.size(100.dp),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("You have no music now", color = Color.White.copy(alpha = .45f), fontSize = 13.sp)
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
                                    if (MusicPlaybackState.currentPlayingUri == track.url && MusicPlaybackState.isPlaying) {
                                        context.startService(Intent(context, RoomMusicService::class.java).setAction(RoomMusicService.ACTION_PAUSE))
                                        MusicPlaybackState.isPlaying = false
                                        MusicPlaybackState.currentTrack = track
                                        onMusicPlaybackChanged?.invoke(track, false)
                                    } else {
                                        val playIntent = Intent(context, RoomMusicService::class.java)
                                            .setAction(RoomMusicService.ACTION_PLAY)
                                            .putExtra(RoomMusicService.EXTRA_URI, track.url)
                                            .putExtra(RoomMusicService.EXTRA_TITLE, track.name)
                                        ContextCompat.startForegroundService(context, playIntent)
                                        MusicPlaybackState.currentPlayingUri = track.url
                                        MusicPlaybackState.isPlaying = true
                                        MusicPlaybackState.currentTrack = track
                                        onMusicPlay?.invoke(track)
                                        onMusicPlaybackChanged?.invoke(track, true)
                                    }
                                }) {
                                    Icon(if (MusicPlaybackState.currentPlayingUri == track.url && MusicPlaybackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = "Play music", tint = Color.White)
                                }
                                IconButton(onClick = {
                                    if (MusicPlaybackState.currentPlayingUri == track.url) {
                                        context.startService(Intent(context, RoomMusicService::class.java).setAction(RoomMusicService.ACTION_STOP))
                                        MusicPlaybackState.currentPlayingUri = null
                                        MusicPlaybackState.isPlaying = false
                                        MusicPlaybackState.currentTrack = null
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
        Box(Modifier.fillMaxSize().background(Color(0xFF141414))) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                Box(Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 8.dp), contentAlignment = Alignment.Center) {
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
                                        containerColor = if (isAdded) Color.White.copy(alpha = 0.1f) else Color(0xFF3B82F6)
                                    ),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                    enabled = !isAdded
                                ) {
                                    Text(if (isAdded) "Added" else "Add", color = if (isAdded) Color.White.copy(alpha = 0.5f) else Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
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
                        containerColor = if (allAdded) Color.White.copy(alpha = 0.1f) else Color(0xFF3B82F6)
                    ),
                    enabled = !allAdded
                ) {
                    Text(if (allAdded) "All Added" else "Add All", color = if (allAdded) Color.White.copy(alpha = 0.5f) else Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    if (showLuckyBag) {
        LuckyBag(senderId = luckyBagSenderId, senderName = luckyBagSenderName, senderImage = luckyBagSenderImage, roomId = luckyBagRoomId, roomName = luckyBagRoomName, onClose = { showLuckyBag = false; onClose() }, onSent = { name, image, amount, recipients, bagId -> onLuckyBagSent?.invoke(name, image, amount, recipients, bagId) })
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
        Text(title, modifier = Modifier.padding(top = 4.dp), fontSize = 10.sp, color = Color.White.copy(alpha = 0.9f), maxLines = 1, softWrap = false)
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MiniMusicControllerCard(
    modifier: Modifier = Modifier,
    onOpenMusicPage: () -> Unit = {} 
) {
    val context = LocalContext.current
    val playingUri = MusicPlaybackState.currentPlayingUri
    val isPlaying = MusicPlaybackState.isPlaying
    val track = MusicPlaybackState.currentTrack
    val isMusicPageOpen = MusicPlaybackState.isMusicPageOpen

    // ✅ State for toggling volume view and volume level
    var showVolumeView by remember { mutableStateOf(false) }
    var volumeLevel by remember { mutableFloatStateOf(0.5f) } 

    if (!isMusicPageOpen && playingUri != null && track != null) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            if (showVolumeView) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = { showVolumeView = false },
                            modifier = Modifier.align(Alignment.CenterStart)
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        Text(
                            text = "Volume",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.VolumeMute, contentDescription = "Low Volume", tint = Color.White)
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(8.dp) 
                                .clip(RoundedCornerShape(50))
                                .background(Color.Gray.copy(alpha = 0.5f)) 
                                .pointerInput(Unit) {
                                    detectDragGestures { change, _ ->
                                        volumeLevel = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                                    }
                                }
                                .pointerInput(Unit) {
                                    detectTapGestures { offset ->
                                        volumeLevel = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                                    }
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(fraction = volumeLevel.coerceAtLeast(0.001f))
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.White) // ✅ Pura Pink hatakar direct White kar diya
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        Icon(Icons.Default.VolumeUp, contentDescription = "High Volume", tint = Color.White)
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                }
            } 
            else {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                MusicPlaybackState.isMusicPageOpen = true
                                onOpenMusicPage()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.CloseFullscreen, contentDescription = "Minimize", tint = Color.White)
                        }
                        IconButton(
                            onClick = {
                                context.startService(Intent(context, RoomMusicService::class.java).setAction(RoomMusicService.ACTION_STOP))
                                MusicPlaybackState.currentPlayingUri = null
                                MusicPlaybackState.isPlaying = false
                                MusicPlaybackState.currentTrack = null
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.PowerSettingsNew, contentDescription = "Stop", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = track.name,
                        color = Color.White,
                        fontSize = 16.sp,
                        maxLines = 1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .basicMarquee(iterations = Int.MAX_VALUE), 
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "00:00", 
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                        Slider(
                            value = 0f, 
                            onValueChange = {},
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                                .height(24.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color.White,
                                inactiveTrackColor = Color.Gray
                            )
                        )
                        Text(
                            text = formatMusicDuration(track.duration), 
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.QueueMusic, 
                            contentDescription = "Playlist", 
                            tint = Color.White,
                            modifier = Modifier.clickable { /* TBD */ }.padding(4.dp)
                        )
                        
                        Icon(
                            imageVector = Icons.Default.SkipPrevious, 
                            contentDescription = "Previous", 
                            tint = Color.White,
                            modifier = Modifier.clickable {
                                val playlist = MusicPlaybackState.playlist
                                val currentIndex = playlist.indexOfFirst { it.url == track.url }
                                if (currentIndex > 0) {
                                    val prevTrack = playlist[currentIndex - 1]
                                    val playIntent = Intent(context, RoomMusicService::class.java)
                                        .setAction(RoomMusicService.ACTION_PLAY)
                                        .putExtra(RoomMusicService.EXTRA_URI, prevTrack.url)
                                        .putExtra(RoomMusicService.EXTRA_TITLE, prevTrack.name)
                                    ContextCompat.startForegroundService(context, playIntent)
                                    MusicPlaybackState.currentPlayingUri = prevTrack.url
                                    MusicPlaybackState.isPlaying = true
                                    MusicPlaybackState.currentTrack = prevTrack
                                }
                            }.padding(4.dp)
                        )
                        
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .clickable {
                                    if (isPlaying) {
                                        context.startService(Intent(context, RoomMusicService::class.java).setAction(RoomMusicService.ACTION_PAUSE))
                                        MusicPlaybackState.isPlaying = false
                                    } else {
                                        val playIntent = Intent(context, RoomMusicService::class.java)
                                            .setAction(RoomMusicService.ACTION_PLAY)
                                            .putExtra(RoomMusicService.EXTRA_URI, track.url)
                                            .putExtra(RoomMusicService.EXTRA_TITLE, track.name)
                                        ContextCompat.startForegroundService(context, playIntent)
                                        MusicPlaybackState.isPlaying = true
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = Color.Black,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.SkipNext, 
                            contentDescription = "Next", 
                            tint = Color.White,
                            modifier = Modifier.clickable {
                                val playlist = MusicPlaybackState.playlist
                                val currentIndex = playlist.indexOfFirst { it.url == track.url }
                                if (currentIndex != -1 && currentIndex < playlist.size - 1) {
                                    val nextTrack = playlist[currentIndex + 1]
                                    val playIntent = Intent(context, RoomMusicService::class.java)
                                        .setAction(RoomMusicService.ACTION_PLAY)
                                        .putExtra(RoomMusicService.EXTRA_URI, nextTrack.url)
                                        .putExtra(RoomMusicService.EXTRA_TITLE, nextTrack.name)
                                    ContextCompat.startForegroundService(context, playIntent)
                                    MusicPlaybackState.currentPlayingUri = nextTrack.url
                                    MusicPlaybackState.isPlaying = true
                                    MusicPlaybackState.currentTrack = nextTrack
                                }
                            }.padding(4.dp)
                        )

                        Icon(
                            imageVector = Icons.Default.Repeat, 
                            contentDescription = "Repeat", 
                            tint = Color.White,
                            modifier = Modifier.clickable { /* TBD */ }.padding(4.dp)
                        )

                        Icon(
                            imageVector = Icons.Default.VolumeUp, 
                            contentDescription = "Volume", 
                            tint = Color.White,
                            modifier = Modifier.clickable { showVolumeView = true }.padding(4.dp) 
                        )
                    }
                }
            }
        }
    }
}

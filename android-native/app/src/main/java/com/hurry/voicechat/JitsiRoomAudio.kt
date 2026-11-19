package com.hurry.voicechat

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.audiofx.AcousticEchoCanceler
import android.media.AudioFormat
import android.os.Handler
import android.os.Looper
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import java.nio.ByteOrder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import io.socket.client.Socket
import io.socket.emitter.Emitter
import org.json.JSONObject
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

private const val ROOM_VOICE_SAMPLE_RATE = 16000
private const val ROOM_VOICE_FRAME_BYTES = 640 // 20 ms, mono, 16-bit PCM
private const val ROOM_MUSIC_SAMPLE_RATE = 16000
private const val ROOM_MUSIC_FRAME_BYTES = 640 // 20 ms, mono, 16-bit PCM

/** Room-only live audio relay over Zyora's existing authenticated room Socket.IO connection. */
@Composable
fun JitsiRoomAudio(
    roomId: String,
    userName: String,
    localAccountId: String = "",
    canSpeak: Boolean,
    canShareMusic: Boolean = canSpeak,
    playbackVoiceEnabled: Boolean = true,
    roomSocket: Socket?,
    musicUri: String? = null,
    musicPlaying: Boolean = false,
    onSpeakingChanged: (Boolean) -> Unit = {},
    onMusicActivityChanged: (String, Boolean) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val latestMusicUri by rememberUpdatedState(musicUri)
    val latestCanSpeak by rememberUpdatedState(canSpeak)
    val latestCanShareMusic by rememberUpdatedState(canShareMusic)
    val latestPlaybackVoiceEnabled by rememberUpdatedState(playbackVoiceEnabled)
    val latestMusicPlaying by rememberUpdatedState(musicPlaying)
    val latestOnMusicActivityChanged by rememberUpdatedState(onMusicActivityChanged)
    var hasMicPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasMicPermission = granted || ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }
    LaunchedEffect(canSpeak, hasMicPermission) {
        if (canSpeak && !hasMicPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    DisposableEffect(context, roomId, roomSocket, hasMicPermission) {
        if (roomSocket == null) return@DisposableEffect onDispose { }
        // Room playback is media audio, not a phone call. Do not force SCO or
        // the built-in speaker: those communication routes bypass classic A2DP
        // earbuds and switch volume to the call stream. Android routes the
        // USAGE_MEDIA AudioTrack to the user's selected speaker/Bluetooth device.
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val previousAudioMode = audioManager.mode
        audioManager.mode = AudioManager.MODE_NORMAL

        val alive = AtomicBoolean(true)
        val recording = AtomicBoolean(false)
        val voiceAllowed = AtomicBoolean(false)
        val retryPending = AtomicBoolean(false)
        val voicePermissionRetries = AtomicInteger(0)
        val retryHandler = Handler(Looper.getMainLooper())
        fun requestVoicePermission() {
            if (alive.get() && latestCanSpeak && roomSocket.connected()) {
                roomSocket.emit("room_voice_start", JSONObject().put("roomId", roomId))
            }
        }
        fun scheduleVoiceRetry(delayMs: Long = 700L) {
            // A denied seat/mute permission is not a transient error. Limit retries
            // so a stale canSpeak flag cannot spam the server every 500 ms forever.
            if (!alive.get() || !latestCanSpeak || voicePermissionRetries.get() >= 3 ||
                !retryPending.compareAndSet(false, true)
            ) return
            retryHandler.postDelayed({
                retryPending.set(false)
                if (alive.get() && latestCanSpeak && !voiceAllowed.get()) {
                    voicePermissionRetries.incrementAndGet()
                    requestVoicePermission()
                }
            }, delayMs)
        }
        var recorder: AudioRecord? = null
        var echoCanceler: AcousticEchoCanceler? = null
        var voicePlayer: AudioTrack? = null
        var musicPlayer: AudioTrack? = null
        val playerLock = Any()
        var voicePlaybackThread: Thread? = null
        var musicPlaybackThread: Thread? = null
        // Short jitter buffers absorb brief Socket.IO/device scheduling delays.
        // Independent per-speaker queues prevent concurrent voices being concatenated.
        val voiceSpeakerQueues = ConcurrentHashMap<String, ArrayBlockingQueue<ByteArray>>()
        val musicPlaybackQueue = ArrayBlockingQueue<ByteArray>(20)
        val musicActivityHandler = Handler(Looper.getMainLooper())
        val musicActivityTimeouts = ConcurrentHashMap<String, Runnable>()
        val activeMusicPublishers = ConcurrentHashMap.newKeySet<String>()
        fun clearMusicPublisherActivity() {
            musicActivityTimeouts.values.forEach { musicActivityHandler.removeCallbacks(it) }
            musicActivityTimeouts.clear()
            val publishers = activeMusicPublishers.toList()
            activeMusicPublishers.clear()
            publishers.forEach { publisherId -> musicActivityHandler.post { latestOnMusicActivityChanged(publisherId, false) } }
        }
        var captureThread: Thread? = null
        var musicRelayThread: Thread? = null
        var musicWatcherThread: Thread? = null
        val musicRelayRunning = AtomicBoolean(false)
        var activeMusicUri: String? = null
        var finishedMusicUri: String? = null

        fun stopMusicRelay() {
            musicRelayRunning.set(false)
            musicRelayThread?.interrupt()
            musicRelayThread?.join(300)
            musicRelayThread = null
            activeMusicUri = null
        }

        fun startMusicRelay(uri: String) {
            if (!alive.get() || !latestCanShareMusic || !roomSocket.connected() || uri.isBlank() || musicRelayRunning.get()) return
            musicRelayRunning.set(true)
            activeMusicUri = uri
            musicRelayThread = thread(name = "ZyoraRoomMusicRelay", isDaemon = true) {
                try {
                    relayDecodedMusic(context, uri, roomSocket, roomId, musicRelayRunning)
                } finally {
                    // Mark a track finished only after natural completion. A socket
                    // reconnect, pause, or route interruption cancels the relay and
                    // must allow the same selected track to resume when transport returns.
                    if (musicRelayRunning.get()) finishedMusicUri = uri
                    musicRelayRunning.set(false)
                    activeMusicUri = null
                }
            }
        }

        fun stopCapture() {
            recording.set(false)
            runCatching { recorder?.stop() }
            captureThread?.join(350)
            captureThread = null
            runCatching { echoCanceler?.release() }
            echoCanceler = null
            runCatching { recorder?.release() }
            recorder = null
            onSpeakingChanged(false)
        }

        fun failCaptureStart() {
            recording.set(false)
            onSpeakingChanged(false)
            if (roomSocket.connected()) {
                roomSocket.emit("room_voice_stop", JSONObject().put("roomId", roomId))
            }
            // Recover from transient AudioRecord initialization/route failures.
            scheduleVoiceRetry(900L)
        }

        fun startCapture() {
            if (!alive.get() || !hasMicPermission || !latestCanSpeak || recording.get()) return
            val minBuffer = AudioRecord.getMinBufferSize(
                ROOM_VOICE_SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            if (minBuffer <= 0) {
                failCaptureStart()
                return
            }
            val created = runCatching {
                AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    ROOM_VOICE_SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    maxOf(minBuffer * 2, ROOM_VOICE_FRAME_BYTES * 4)
                )
            }.getOrNull() ?: run {
                failCaptureStart()
                return
            }
            if (created.state != AudioRecord.STATE_INITIALIZED) {
                created.release()
                failCaptureStart()
                return
            }
            recorder = created
            // Prefer a connected Bluetooth headset microphone when Android exposes
            // one as an input device. Do not force SCO/communication mode: room
            // playback remains USAGE_MEDIA so A2DP/LE media output stays available.
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                val bluetoothInput = runCatching {
                    audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS).firstOrNull { device ->
                        device.type == android.media.AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                            (android.os.Build.VERSION.SDK_INT >= 31 &&
                                device.type == android.media.AudioDeviceInfo.TYPE_BLE_HEADSET)
                    }
                }.getOrNull()
                if (bluetoothInput != null) {
                    runCatching { created.setPreferredDevice(bluetoothInput) }
                }
            }
            if (AcousticEchoCanceler.isAvailable()) {
                echoCanceler = runCatching {
                    AcousticEchoCanceler.create(created.audioSessionId)?.also { it.enabled = true }
                }.getOrNull()
            }
            recording.set(true)
            runCatching { created.startRecording() }.onFailure {
                recording.set(false)
                created.release()
                recorder = null
                failCaptureStart()
                return
            }
            onSpeakingChanged(false)
            captureThread = thread(name = "ZyoraRoomMic", isDaemon = true) {
                val frame = ByteArray(ROOM_VOICE_FRAME_BYTES)
                var filled = 0
                var speechFrames = 0
                var silenceFrames = 0
                var voiceActive = false
                while (alive.get() && recording.get()) {
                    val count = runCatching { created.read(frame, filled, frame.size - filled) }.getOrDefault(-1)
                    if (count > 0) {
                        filled += count
                        if (filled == frame.size) {
                            if (recording.get()) {
                                var squareSum = 0L
                                var sampleIndex = 0
                                while (sampleIndex < frame.size) {
                                    val sample = ((frame[sampleIndex + 1].toInt() shl 8) or
                                        (frame[sampleIndex].toInt() and 0xFF)).toShort().toInt()
                                    squareSum += sample.toLong() * sample.toLong()
                                    sampleIndex += 2
                                }
                                val meanSquare = squareSum / (frame.size / 2)
                                val voiceDetected = meanSquare > 400L * 400L
                                if (voiceDetected) {
                                    speechFrames += 1
                                    silenceFrames = 0
                                    if (!voiceActive && speechFrames >= 2) {
                                        voiceActive = true
                                        if (roomSocket.connected()) {
                                            roomSocket.emit("room_voice_activity", JSONObject().put("roomId", roomId).put("speaking", true))
                                        }
                                        Handler(Looper.getMainLooper()).post { onSpeakingChanged(true) }
                                    }
                                } else {
                                    speechFrames = 0
                                    if (voiceActive) {
                                        silenceFrames += 1
                                        if (silenceFrames >= 12) {
                                            voiceActive = false
                                            silenceFrames = 0
                                            if (roomSocket.connected()) {
                                                roomSocket.emit("room_voice_activity", JSONObject().put("roomId", roomId).put("speaking", false))
                                            }
                                            Handler(Looper.getMainLooper()).post { onSpeakingChanged(false) }
                                        }
                                    }
                                }
                                if (roomSocket.connected()) {
                                    runCatching {
                                        roomSocket.emit(
                                            "room_voice_chunk",
                                            JSONObject().put("roomId", roomId),
                                            frame.copyOf()
                                        )
                                    }
                                }
                            }
                            filled = 0
                        }
                    } else if (count < 0) {
                        if (recording.compareAndSet(true, false)) {
                            // AudioRecord died. The stop event also revokes the server-side
                            // publisher session, so a local capture restart alone is invalid.
                            voiceAllowed.set(false)
                            if (roomSocket.connected()) {
                                roomSocket.emit("room_voice_stop", JSONObject().put("roomId", roomId))
                            }
                            Handler(Looper.getMainLooper()).post { onSpeakingChanged(false) }
                            runCatching { created.stop() }
                            runCatching { created.release() }
                            if (recorder === created) recorder = null
                            runCatching { echoCanceler?.release() }
                            echoCanceler = null
                            // Re-authorize with the server first; permissionListener starts
                            // AudioRecord only after room_voice_permission(allowed=true).
                            Handler(Looper.getMainLooper()).postDelayed({
                                if (alive.get() && latestCanSpeak && hasMicPermission &&
                                    !recording.get() && roomSocket.connected()
                                ) requestVoicePermission()
                            }, 1000)
                        }
                        break
                    } else {
                        // AudioRecord can briefly return 0 while the input route settles.
                        // Yielding alone can spin this dedicated thread at full CPU; a tiny
                        // backoff avoids idle CPU churn without changing the 20 ms frame format.
                        try { Thread.sleep(2) } catch (_: InterruptedException) {
                            if (!alive.get() || !recording.get()) break
                        }
                    }
                }
            }
        }

        val moderationMuteListener = Emitter.Listener { args ->
            val data = args.firstOrNull() as? JSONObject ?: return@Listener
            if (data.optString("roomId") != roomId) return@Listener
            if (data.optBoolean("muted", false)) {
                voiceAllowed.set(false)
                stopCapture()
                if (roomSocket.connected()) roomSocket.emit("room_voice_stop", JSONObject().put("roomId", roomId))
            }
        }

        val permissionListener = Emitter.Listener { args ->
            val data = args.firstOrNull() as? JSONObject ?: return@Listener
            if (data.optString("roomId") != roomId) return@Listener
            val allowed = data.optBoolean("allowed", false) && latestCanSpeak
            voiceAllowed.set(allowed)
            if (allowed) {
                retryPending.set(false)
                voicePermissionRetries.set(0)
                startCapture()
            } else {
                stopCapture()
                // Retry transient seat-state/permission races while still unmuted.
                scheduleVoiceRetry(500L)
            }
        }
        fun createOutputTrack(isVoicePacket: Boolean): AudioTrack? {
            val sampleRate = if (isVoicePacket) ROOM_VOICE_SAMPLE_RATE else ROOM_MUSIC_SAMPLE_RATE
            val channelMask = AudioFormat.CHANNEL_OUT_MONO
            val frameBytes = if (isVoicePacket) ROOM_VOICE_FRAME_BYTES else ROOM_MUSIC_FRAME_BYTES
            val minOut = AudioTrack.getMinBufferSize(sampleRate, channelMask, AudioFormat.ENCODING_PCM_16BIT)
            if (minOut <= 0) return null
            return runCatching {
                AudioTrack.Builder()
                    .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                    .setAudioFormat(AudioFormat.Builder().setSampleRate(sampleRate).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(channelMask).build())
                    .setBufferSizeInBytes(maxOf(minOut * 2, frameBytes * 8))
                    .setTransferMode(AudioTrack.MODE_STREAM).build()
                    .takeIf { it.state == AudioTrack.STATE_INITIALIZED }
            }.getOrNull()
        }

        fun ensurePlaybackWorker(isVoicePacket: Boolean) {
            synchronized(playerLock) {
                val current = if (isVoicePacket) voicePlaybackThread else musicPlaybackThread
                if (current?.isAlive == true) return
                val worker = thread(name = if (isVoicePacket) "ZyoraRoomVoicePlayback" else "ZyoraRoomMusicPlayback", isDaemon = true) {
                    while (alive.get()) {
                        val packet = if (isVoicePacket) {
                            if (!latestPlaybackVoiceEnabled) {
                                voiceSpeakerQueues.values.forEach { it.clear() }
                                try { Thread.sleep(20) } catch (_: InterruptedException) { if (!alive.get()) break }
                                continue
                            }
                            // Socket.IO delivers each publisher independently. Give packets a
                            // short collection window so two speakers' frames arriving a few ms
                            // apart are mixed in the same output frame instead of alternating.
                            if (voiceSpeakerQueues.values.any { it.isNotEmpty() }) {
                                try { Thread.sleep(8) } catch (_: InterruptedException) { if (!alive.get()) break }
                            }
                            val mixed = LongArray(ROOM_VOICE_FRAME_BYTES / 2)
                            var activeSpeakers = 0
                            voiceSpeakerQueues.values.forEach { speakerQueue ->
                                val frame = speakerQueue.poll() ?: return@forEach
                                if (frame.size != ROOM_VOICE_FRAME_BYTES) return@forEach
                                activeSpeakers++
                                var i = 0
                                while (i + 1 < ROOM_VOICE_FRAME_BYTES) {
                                    val sample = ((frame[i + 1].toInt() shl 8) or (frame[i].toInt() and 0xFF)).toShort().toInt()
                                    mixed[i / 2] += sample.toLong()
                                    i += 2
                                }
                            }
                            if (activeSpeakers == 0) {
                                try { Thread.sleep(5) } catch (_: InterruptedException) { if (!alive.get()) break }
                                continue
                            }
                            // Average simultaneous speakers instead of hard limiting their sum.
                            // The old nonlinear limiter distorted peaks as soon as two people spoke.
                            ByteArray(ROOM_VOICE_FRAME_BYTES).also { output ->
                                mixed.forEachIndexed { index, sum ->
                                    val sample = (sum / activeSpeakers).toInt()
                                        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                                    output[index * 2] = (sample and 0xFF).toByte()
                                    output[index * 2 + 1] = ((sample shr 8) and 0xFF).toByte()
                                }
                            }
                        } else {
                            try { musicPlaybackQueue.poll(100, TimeUnit.MILLISECONDS) }
                            catch (_: InterruptedException) { if (!alive.get()) break else null } ?: continue
                        }
                        if (isVoicePacket && !latestPlaybackVoiceEnabled) continue
                        val track = synchronized(playerLock) { if (isVoicePacket) voicePlayer else musicPlayer } ?: continue
                        var offset = 0
                        var failed = false
                        try {
                            if (track.playState != AudioTrack.PLAYSTATE_PLAYING) track.play()
                            while (offset < packet.size && alive.get() && (!isVoicePacket || latestPlaybackVoiceEnabled)) {
                                val written = track.write(packet, offset, packet.size - offset, AudioTrack.WRITE_BLOCKING)
                                if (written <= 0) { failed = true; break }
                                offset += written
                            }
                        } catch (_: Throwable) { failed = true }
                        if (failed) synchronized(playerLock) {
                            if (isVoicePacket && voicePlayer === track) {
                                voicePlayer = null
                                runCatching { track.pause(); track.flush(); track.stop(); track.release() }
                            } else if (!isVoicePacket && musicPlayer === track) {
                                musicPlayer = null
                                runCatching { track.pause(); track.flush(); track.stop(); track.release() }
                            }
                            if (isVoicePacket) voiceSpeakerQueues.values.forEach { it.clear() } else musicPlaybackQueue.clear()
                        }
                    }
                }
                if (isVoicePacket) voicePlaybackThread = worker else musicPlaybackThread = worker
            }
        }

        fun createChunkListener(isVoicePacket: Boolean) = Emitter.Listener { args ->
            val data = args.firstOrNull() as? JSONObject ?: return@Listener
            if (data.optString("roomId") != roomId || !alive.get()) return@Listener
            if (localAccountId.isNotBlank()) {
                val packetOwners = listOf(data.optString("userId"), data.optString("accountId"), data.optString("senderId"), data.optString("senderAccountId"), data.optString("publisherId"))
                if (packetOwners.any { it.isNotBlank() && it == localAccountId }) return@Listener
            }
            val rawAudio = args.getOrNull(1) ?: data.opt("audio")
            val bytes = when (rawAudio) {
                is ByteArray -> rawAudio
                is org.json.JSONArray -> ByteArray(rawAudio.length()) { index -> rawAudio.optInt(index, 0).coerceIn(0, 255).toByte() }
                else -> return@Listener
            }
            if (bytes.isEmpty() || bytes.size > 4096 || bytes.size % 2 != 0) return@Listener
            val ready = synchronized(playerLock) {
                if (isVoicePacket) {
                    if (voicePlayer?.state != AudioTrack.STATE_INITIALIZED) {
                        runCatching { voicePlayer?.release() }
                        voicePlayer = createOutputTrack(true)
                    }
                    voicePlayer != null
                } else {
                    if (musicPlayer?.state != AudioTrack.STATE_INITIALIZED) {
                        runCatching { musicPlayer?.release() }
                        musicPlayer = createOutputTrack(false)
                    }
                    musicPlayer != null
                }
            }
            if (!ready) return@Listener
            ensurePlaybackWorker(isVoicePacket)
            if (isVoicePacket) {
                val speakerId = sequenceOf("userId", "accountId", "senderId", "senderAccountId", "publisherId")
                    .map { data.optString(it).trim() }
                    .firstOrNull { it.isNotBlank() } ?: return@Listener
                if (speakerId == localAccountId) return@Listener
                // Server voice frames are fixed 20 ms / 640-byte PCM packets.
                if (bytes.size != ROOM_VOICE_FRAME_BYTES) return@Listener
                val queue = voiceSpeakerQueues.computeIfAbsent(speakerId) { ArrayBlockingQueue(4) }
                if (!queue.offer(bytes.copyOf())) { queue.poll(); queue.offer(bytes.copyOf()) }
            } else {
                if (!musicPlaybackQueue.offer(bytes.copyOf())) { musicPlaybackQueue.poll(); musicPlaybackQueue.offer(bytes.copyOf()) }
            }
        }

        val voiceChunkListener = createChunkListener(true)
        val musicChunkListener = createChunkListener(false)
        // Music packets carry the publishing accountId. Keep that user's avatar wave
        // active while packets arrive, then clear it shortly after the stream stops.
        val musicActivityListener = Emitter.Listener { args ->
            val payload = args.firstOrNull() as? JSONObject ?: return@Listener
            if (payload.optString("roomId") != roomId || !alive.get()) return@Listener
            val publisherId = sequenceOf("userId", "accountId", "senderId", "senderAccountId", "publisherId")
                .map { payload.optString(it).trim() }
                .firstOrNull { it.isNotBlank() } ?: return@Listener
            if (publisherId == localAccountId) return@Listener
            if (activeMusicPublishers.add(publisherId)) {
                musicActivityHandler.post { if (alive.get()) latestOnMusicActivityChanged(publisherId, true) }
            }
            val timeout = Runnable {
                musicActivityTimeouts.remove(publisherId)
                if (activeMusicPublishers.remove(publisherId) && alive.get()) {
                    latestOnMusicActivityChanged(publisherId, false)
                }
            }
            musicActivityTimeouts.put(publisherId, timeout)?.let(musicActivityHandler::removeCallbacks)
            musicActivityHandler.postDelayed(timeout, 600L)
        }
        roomSocket.on("room_moderation_mute", moderationMuteListener)
        roomSocket.on("room_voice_permission", permissionListener)
        roomSocket.on("room_voice_chunk", voiceChunkListener)
        roomSocket.on("room_music_chunk", musicChunkListener)
        roomSocket.on("room_music_chunk", musicActivityListener)
        val disconnectListener = Emitter.Listener {
            clearMusicPublisherActivity()
            voiceAllowed.set(false)
            stopMusicRelay()
            stopCapture()
            voiceSpeakerQueues.values.forEach { it.clear() }
            runCatching { voicePlayer?.pause(); voicePlayer?.flush() }
            runCatching { musicPlayer?.pause(); musicPlayer?.flush() }
        }
        roomSocket.on(Socket.EVENT_DISCONNECT, disconnectListener)

        var lastPlaybackVoiceEnabled = latestPlaybackVoiceEnabled
        val playbackVoiceMuteWatcherThread = thread(name = "ZyoraRoomSpeakerWatch", isDaemon = true) {
            while (alive.get()) {
                val voicePlaybackEnabled = latestPlaybackVoiceEnabled
                if (!voicePlaybackEnabled) {
                    voiceSpeakerQueues.values.forEach { it.clear() }
                    // Flush the partially buffered pre-mute frame once. Without this,
                    // stale AudioTrack data can overlap the first packets after unmute.
                    if (lastPlaybackVoiceEnabled) {
                        synchronized(playerLock) {
                            runCatching { voicePlayer?.pause(); voicePlayer?.flush() }
                        }
                    }
                }
                lastPlaybackVoiceEnabled = voicePlaybackEnabled
                try { Thread.sleep(50) } catch (_: InterruptedException) { }
            }
        }

        val connectListener = Emitter.Listener {
            // Server authorization is seat-based. Do not gate the room audio session
            // on RECORD_AUDIO: local music sharing must still work if mic permission
            // is denied; startCapture() independently checks that permission.
            if (latestCanSpeak && alive.get()) {
                requestVoicePermission()
            }
        }
        roomSocket.on(Socket.EVENT_CONNECT, connectListener)

        // Keep the room audio session and Socket.IO listeners alive across mute toggles.
        // Only stop publishing mic frames while muted; request permission again on unmute.
        var lastMicEnabled = latestCanSpeak
        val micMuteWatcherThread = thread(name = "ZyoraRoomMicMuteWatch", isDaemon = true) {
            while (alive.get()) {
                val micEnabled = latestCanSpeak
                if (micEnabled != lastMicEnabled) {
                    lastMicEnabled = micEnabled
                    if (!micEnabled) {
                        voicePermissionRetries.set(0)
                        retryPending.set(false)
                        retryHandler.removeCallbacksAndMessages(null)
                        voiceAllowed.set(false)
                        stopCapture()
                        if (roomSocket.connected()) {
                            roomSocket.emit("room_voice_stop", JSONObject().put("roomId", roomId))
                        }
                    } else if (roomSocket.connected()) {
                        voicePermissionRetries.set(0)
                        requestVoicePermission()
                    }
                }
                try { Thread.sleep(50) } catch (_: InterruptedException) { }
            }
        }

        musicWatcherThread = thread(name = "ZyoraRoomMusicWatch", isDaemon = true) {
            while (alive.get()) {
                val uri = latestMusicUri?.takeIf { it.isNotBlank() }
                val shouldRelay = latestCanShareMusic && roomSocket.connected() && latestMusicPlaying && uri != null
                if (!shouldRelay) {
                    if (musicRelayRunning.get()) stopMusicRelay()
                    if (!latestMusicPlaying || uri == null) finishedMusicUri = null
                } else if (uri != activeMusicUri && uri != finishedMusicUri) {
                    if (musicRelayRunning.get()) stopMusicRelay()
                    startMusicRelay(uri!!)
                }
                try { Thread.sleep(100) } catch (_: InterruptedException) { }
            }
            stopMusicRelay()
        }
        requestVoicePermission()

        onDispose {
            // Detach listeners and stop producers immediately, but never join audio
            // workers on Compose's main thread. The old synchronous joins could block
            // navigation/room exit for more than a second on slower devices.
            alive.set(false)
            voiceAllowed.set(false)
            recording.set(false)
            musicRelayRunning.set(false)
            retryHandler.removeCallbacksAndMessages(null)
            retryPending.set(false)
            voicePermissionRetries.set(0)
            micMuteWatcherThread.interrupt()
            playbackVoiceMuteWatcherThread.interrupt()
            musicWatcherThread?.interrupt()
            voiceSpeakerQueues.values.forEach { it.clear() }
            voiceSpeakerQueues.clear()
            musicPlaybackQueue.clear()
            voicePlaybackThread?.interrupt()
            musicPlaybackThread?.interrupt()
            runCatching { recorder?.stop() }
            captureThread?.interrupt()
            roomSocket.emit("room_voice_stop", JSONObject().put("roomId", roomId))
            roomSocket.off("room_moderation_mute", moderationMuteListener)
            roomSocket.off("room_voice_permission", permissionListener)
            roomSocket.off("room_voice_chunk", voiceChunkListener)
            roomSocket.off("room_music_chunk", musicChunkListener)
            roomSocket.off("room_music_chunk", musicActivityListener)
            clearMusicPublisherActivity()
            roomSocket.off(Socket.EVENT_DISCONNECT, disconnectListener)
            roomSocket.off(Socket.EVENT_CONNECT, connectListener)
            onSpeakingChanged(false)

            val cleanupRecorder = recorder
            val cleanupEchoCanceler = echoCanceler
            recorder = null
            echoCanceler = null
            val cleanupVoicePlayerThread = voicePlaybackThread
            val cleanupMusicPlayerThread = musicPlaybackThread
            voicePlaybackThread = null
            musicPlaybackThread = null
            val cleanupMusicThread = musicRelayThread
            val cleanupMusicWatcher = musicWatcherThread
            thread(name = "ZyoraRoomAudioCleanup", isDaemon = true) {
                runCatching { micMuteWatcherThread.join(350) }
                runCatching { playbackVoiceMuteWatcherThread.join(350) }
                runCatching { cleanupMusicWatcher?.join(350) }
                runCatching { cleanupMusicThread?.interrupt(); cleanupMusicThread?.join(350) }
                runCatching { captureThread?.join(350) }
                runCatching { cleanupRecorder?.release() }
                runCatching { cleanupEchoCanceler?.release() }
                runCatching { cleanupVoicePlayerThread?.join(500) }
                runCatching { cleanupMusicPlayerThread?.join(500) }
                synchronized(playerLock) {
                    runCatching { voicePlayer?.pause(); voicePlayer?.flush(); voicePlayer?.stop(); voicePlayer?.release() }
                    runCatching { musicPlayer?.pause(); musicPlayer?.flush(); musicPlayer?.stop(); musicPlayer?.release() }
                    voicePlayer = null
                    musicPlayer = null
                }
                runCatching {
                    // Restore only the global mode changed by this room session.
                    audioManager.mode = previousAudioMode
                }
            }
        }
    }

}

/** Decode selected music to 16 kHz mono PCM with a restrained bass lift and peak guard.
 * Voice capture and voice packet logic remain unchanged.
 */
private fun relayDecodedMusic(
    context: Context,
    uri: String,
    socket: Socket,
    roomId: String,
    running: AtomicBoolean
) {
    val extractor = MediaExtractor()
    var codec: MediaCodec? = null
    try {
        extractor.setDataSource(context, Uri.parse(uri), null)
        var trackIndex = -1
        var format: MediaFormat? = null
        for (i in 0 until extractor.trackCount) {
            val candidate = extractor.getTrackFormat(i)
            if (candidate.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                trackIndex = i
                format = candidate
                break
            }
        }
        if (trackIndex < 0 || format == null) return
        extractor.selectTrack(trackIndex)
        // Request 16-bit PCM explicitly; music uses its own 16 kHz mono packet format.
        format.setInteger(MediaFormat.KEY_PCM_ENCODING, AudioFormat.ENCODING_PCM_16BIT)
        codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
        codec.configure(format, null, null, 0)
        codec.start()

        var inputDone = false
        var outputDone = false
        var sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        var channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceAtLeast(1)
        val packet = ByteArray(ROOM_MUSIC_FRAME_BYTES)
        var packetSize = 0
        var resamplePosition = 0.0
        var bassLowPass = 0.0
        var nextFrameAt = System.nanoTime()

        while (running.get() && !outputDone) {
            if (!inputDone) {
                val inputIndex = codec.dequeueInputBuffer(10_000)
                if (inputIndex >= 0) {
                    val input = codec.getInputBuffer(inputIndex) ?: continue
                    val sampleSize = extractor.readSampleData(input, 0)
                    if (sampleSize < 0) {
                        codec.queueInputBuffer(inputIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        inputDone = true
                    } else {
                        codec.queueInputBuffer(inputIndex, 0, sampleSize, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }
            }

            val info = MediaCodec.BufferInfo()
            val outputIndex = codec.dequeueOutputBuffer(info, 10_000)
            if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                val outputFormat = codec.outputFormat
                sampleRate = outputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                channels = outputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceAtLeast(1)
            } else if (outputIndex >= 0) {
                val output = codec.getOutputBuffer(outputIndex)
                if (output != null && info.size > 0 && (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                    output.position(info.offset)
                    output.limit(info.offset + info.size)
                    output.order(ByteOrder.LITTLE_ENDIAN)
                    val sourceFrames = info.size / (channels * 2)
                    val step = sampleRate.coerceAtLeast(1).toDouble() / ROOM_MUSIC_SAMPLE_RATE.toDouble()
                    while (resamplePosition < sourceFrames && running.get()) {
                        // Downmix stereo to mono at 16 kHz. A gentle low-frequency lift
                        // adds kick/bass weight; clamping prevents boosted peaks clipping.
                        val intervalEnd = minOf(resamplePosition + step, sourceFrames.toDouble())
                        var cursor = resamplePosition
                        var monoSum = 0.0
                        var totalWeight = 0.0
                        while (cursor < intervalEnd) {
                            val sourceFrame = cursor.toInt().coerceIn(0, sourceFrames - 1)
                            val nextBoundary = minOf(sourceFrame + 1.0, intervalEnd)
                            val weight = nextBoundary - cursor
                            val base = info.offset + sourceFrame * channels * 2
                            val left = output.getShort(base).toInt()
                            val right = if (channels == 1) left else output.getShort(base + 2).toInt()
                            monoSum += ((left + right) * 0.5) * weight
                            totalWeight += weight
                            cursor = nextBoundary
                        }
                        val mono = if (totalWeight > 0.0) monoSum / totalWeight else 0.0
                        bassLowPass += (mono - bassLowPass) * 0.07
                        val enhanced = (mono + bassLowPass * 0.38)
                            .toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                        packet[packetSize++] = (enhanced and 0xFF).toByte()
                        packet[packetSize++] = ((enhanced shr 8) and 0xFF).toByte()
                        resamplePosition += step
                        if (packetSize == packet.size) {
                            if (socket.connected() && running.get()) {
                                socket.emit("room_music_chunk", JSONObject().put("roomId", roomId), packet.copyOf())
                            }
                            nextFrameAt += 20_000_000L
                            val waitNanos = nextFrameAt - System.nanoTime()
                            if (waitNanos > 0) {
                                try { Thread.sleep(waitNanos / 1_000_000L, (waitNanos % 1_000_000L).toInt()) }
                                catch (_: InterruptedException) { }
                            } else nextFrameAt = System.nanoTime()
                            packetSize = 0
                        }
                    }
                    // Carry fractional resampling position into the next decoded buffer
                    // to prevent gradual timing drift on 44.1/48 kHz source tracks.
                    resamplePosition -= sourceFrames
                }
                outputDone = (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0
                codec.releaseOutputBuffer(outputIndex, false)
            }
        }
    } catch (_: Exception) {
        // Local playback is unaffected if this device cannot decode/share the selected file.
    } finally {
        runCatching { codec?.stop() }
        runCatching { codec?.release() }
        runCatching { extractor.release() }
    }
}

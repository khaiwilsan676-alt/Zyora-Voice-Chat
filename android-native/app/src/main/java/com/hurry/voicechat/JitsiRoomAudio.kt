package com.hurry.voicechat

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.audiofx.AcousticEchoCanceler
import android.media.AudioFormat
import android.media.AudioManager
import android.os.Build
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
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

private const val ROOM_VOICE_SAMPLE_RATE = 16000
private const val ROOM_VOICE_FRAME_BYTES = 640 // 20 ms, mono, 16-bit PCM

/** Room-only live audio relay over Zyora's existing authenticated room Socket.IO connection. */
@Composable
fun JitsiRoomAudio(
    roomId: String,
    userName: String,
    canSpeak: Boolean,
    roomSocket: Socket?,
    musicUri: String? = null,
    musicPlaying: Boolean = false,
    onSpeakingChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val latestMusicUri by rememberUpdatedState(musicUri)
    val latestCanSpeak by rememberUpdatedState(canSpeak)
    val latestMusicPlaying by rememberUpdatedState(musicPlaying)
    var hasMicPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasMicPermission = granted || ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }
    var hasBluetoothPermission by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        )
    }
    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasBluetoothPermission = granted || Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
    }

    LaunchedEffect(canSpeak, hasMicPermission, hasBluetoothPermission) {
        if (canSpeak && !hasMicPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else if (canSpeak && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !hasBluetoothPermission) {
            bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        }
    }

    DisposableEffect(context, roomId, roomSocket, hasMicPermission, hasBluetoothPermission) {
        if (roomSocket == null) return@DisposableEffect onDispose { }
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val oldMode = audioManager.mode
        // Room audio uses the normal media/speaker route, never call mode.
        audioManager.mode = AudioManager.MODE_NORMAL

        val alive = AtomicBoolean(true)
        val recording = AtomicBoolean(false)
        val voiceAllowed = AtomicBoolean(false)
        val retryPending = AtomicBoolean(false)
        val retryHandler = Handler(Looper.getMainLooper())
        fun requestVoicePermission() {
            if (alive.get() && latestCanSpeak && roomSocket.connected()) {
                roomSocket.emit("room_voice_start", JSONObject().put("roomId", roomId))
            }
        }
        fun scheduleVoiceRetry(delayMs: Long = 700L) {
            if (!alive.get() || !latestCanSpeak || !retryPending.compareAndSet(false, true)) return
            retryHandler.postDelayed({
                retryPending.set(false)
                if (alive.get() && latestCanSpeak && !voiceAllowed.get()) requestVoicePermission()
            }, delayMs)
        }
        var recorder: AudioRecord? = null
        var echoCanceler: AcousticEchoCanceler? = null
        var player: AudioTrack? = null
        val playerLock = Any()
        var playbackThread: Thread? = null
        // 20 ms PCM frames: six packets cap stale playback to about 120 ms
        // instead of allowing nearly one second of delayed voice to accumulate.
        val playbackQueue = ArrayBlockingQueue<ByteArray>(6)
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
            if (!alive.get() || !voiceAllowed.get() || !latestCanSpeak || uri.isBlank() || musicRelayRunning.get()) return
            musicRelayRunning.set(true)
            activeMusicUri = uri
            musicRelayThread = thread(name = "ZyoraRoomMusicRelay", isDaemon = true) {
                try {
                    relayDecodedMusic(context, uri, roomSocket, roomId, musicRelayRunning)
                } finally {
                    musicRelayRunning.set(false)
                    finishedMusicUri = uri
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
                    MediaRecorder.AudioSource.VOICE_COMMUNICATION,
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
                        Thread.yield()
                    }
                }
            }
        }

        val permissionListener = Emitter.Listener { args ->
            val data = args.firstOrNull() as? JSONObject ?: return@Listener
            if (data.optString("roomId") != roomId) return@Listener
            val allowed = data.optBoolean("allowed", false) && latestCanSpeak
            voiceAllowed.set(allowed)
            if (allowed) {
                retryPending.set(false)
                startCapture()
            } else {
                stopMusicRelay()
                stopCapture()
                // Retry transient seat-state/permission races while still unmuted.
                scheduleVoiceRetry(500L)
            }
        }
        val chunkListener = Emitter.Listener { args ->
            val data = args.firstOrNull() as? JSONObject ?: return@Listener
            if (data.optString("roomId") != roomId || !alive.get()) return@Listener
            // JSONObject serializes Kotlin ByteArray payloads as JSON number arrays.
            // Accept both Socket.IO binary buffers and JSON arrays so relayed mic/music
            // packets are decoded consistently on every Android client.
            val rawAudio = args.getOrNull(1) as? ByteArray ?: data.opt("audio")
            val bytes = when (rawAudio) {
                is ByteArray -> rawAudio
                is org.json.JSONArray -> ByteArray(rawAudio.length()) { index ->
                    rawAudio.optInt(index, 0).coerceIn(0, 255).toByte()
                }
                else -> return@Listener
            }
            if (bytes.isEmpty() || bytes.size > 4096 || bytes.size % 2 != 0) return@Listener
            val output = synchronized(playerLock) {
                player?.takeIf { it.state == AudioTrack.STATE_INITIALIZED } ?: run {
                    val minOut = AudioTrack.getMinBufferSize(
                        ROOM_VOICE_SAMPLE_RATE,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT
                    )
                    if (minOut <= 0) return@Listener
                    runCatching {
                        AudioTrack.Builder()
                            .setAudioAttributes(
                                AudioAttributes.Builder()
                                    .setUsage(AudioAttributes.USAGE_MEDIA)
                                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                    .build()
                            )
                            .setAudioFormat(
                                AudioFormat.Builder()
                                    .setSampleRate(ROOM_VOICE_SAMPLE_RATE)
                                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                    .build()
                            )
                            .setBufferSizeInBytes(maxOf(minOut * 3, ROOM_VOICE_FRAME_BYTES * 12))
                            .setTransferMode(AudioTrack.MODE_STREAM)
                            .build()
                            .takeIf { it.state == AudioTrack.STATE_INITIALIZED }
                            ?.also { player = it }
                    }.getOrNull() ?: return@Listener
                }
            }
            // Never block Socket.IO's event thread on AudioTrack.write(). A dedicated
            // consumer drains a bounded jitter queue so network callbacks stay responsive.
            if (playbackThread?.isAlive != true) {
                playbackThread = thread(name = "ZyoraRoomPlayback", isDaemon = true) {
                    while (alive.get()) {
                        val packet = try {
                            playbackQueue.poll(100, TimeUnit.MILLISECONDS)
                        } catch (_: InterruptedException) {
                            if (!alive.get()) break else null
                        } ?: continue
                        val track = synchronized(playerLock) { player } ?: continue
                        var offset = 0
                        var playbackFailed = false
                        try {
                            if (track.playState != AudioTrack.PLAYSTATE_PLAYING) track.play()
                            while (offset < packet.size && alive.get()) {
                                val written = track.write(
                                    packet, offset, packet.size - offset, AudioTrack.WRITE_BLOCKING
                                )
                                if (written <= 0) {
                                    playbackFailed = true
                                    break
                                }
                                offset += written
                            }
                        } catch (_: Throwable) {
                            playbackFailed = true
                        }
                        if (playbackFailed) {
                            synchronized(playerLock) {
                                if (player === track) {
                                    player = null
                                    runCatching { track.pause(); track.flush(); track.stop(); track.release() }
                                }
                            }
                            // Drop stale buffered audio; the next incoming packet recreates AudioTrack.
                            playbackQueue.clear()
                        }
                    }
                }
            }
            if (!playbackQueue.offer(bytes.copyOf())) {
                playbackQueue.poll()
                playbackQueue.offer(bytes.copyOf())
            }
        }

        roomSocket.on("room_voice_permission", permissionListener)
        roomSocket.on("room_voice_chunk", chunkListener)
        val disconnectListener = Emitter.Listener {
            voiceAllowed.set(false)
            stopMusicRelay()
            stopCapture()
            playbackQueue.clear()
            runCatching { player?.pause(); player?.flush() }
        }
        roomSocket.on(Socket.EVENT_DISCONNECT, disconnectListener)

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
                        voiceAllowed.set(false)
                        stopCapture()
                        if (roomSocket.connected()) {
                            roomSocket.emit("room_voice_stop", JSONObject().put("roomId", roomId))
                        }
                    } else if (roomSocket.connected()) {
                        requestVoicePermission()
                    }
                }
                try { Thread.sleep(50) } catch (_: InterruptedException) { }
            }
        }

        musicWatcherThread = thread(name = "ZyoraRoomMusicWatch", isDaemon = true) {
            while (alive.get()) {
                val uri = latestMusicUri?.takeIf { it.isNotBlank() }
                val shouldRelay = voiceAllowed.get() && latestCanSpeak && latestMusicPlaying && uri != null
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
            alive.set(false)
            voiceAllowed.set(false)
            retryHandler.removeCallbacksAndMessages(null)
            retryPending.set(false)
            micMuteWatcherThread.interrupt()
            micMuteWatcherThread.join(300)
            musicWatcherThread?.interrupt()
            musicWatcherThread?.join(300)
            stopMusicRelay()
            roomSocket.emit("room_voice_stop", JSONObject().put("roomId", roomId))
            roomSocket.off("room_voice_permission", permissionListener)
            roomSocket.off("room_voice_chunk", chunkListener)
            roomSocket.off(Socket.EVENT_DISCONNECT, disconnectListener)
            roomSocket.off(Socket.EVENT_CONNECT, connectListener)
            stopCapture()
            playbackQueue.clear()
            playbackThread?.interrupt()
            playbackThread?.join(500)
            playbackThread = null
            synchronized(playerLock) {
                runCatching { player?.pause(); player?.flush(); player?.stop(); player?.release() }
                player = null
            }

            audioManager.mode = oldMode
        }
    }

}

/** Decode the selected local track to real-time 16 kHz mono PCM and send it through
 * the same room_voice_chunk relay. Local MediaPlayer playback remains independent.
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
        // Request the exact PCM format expected by the room's 16-bit mono packet
        // protocol, rather than relying on a device decoder's default PCM encoding.
        format.setInteger(MediaFormat.KEY_PCM_ENCODING, AudioFormat.ENCODING_PCM_16BIT)
        codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
        codec.configure(format, null, null, 0)
        codec.start()

        var inputDone = false
        var outputDone = false
        var sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        var channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceAtLeast(1)
        val packet = ByteArray(ROOM_VOICE_FRAME_BYTES)
        var packetSize = 0
        var resamplePosition = 0.0
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
                    val step = sampleRate.coerceAtLeast(1).toDouble() / 16000.0
                    while (resamplePosition < sourceFrames && running.get()) {
                        val sourceFrame = resamplePosition.toInt().coerceIn(0, sourceFrames - 1)
                        var mixed = 0
                        for (channel in 0 until channels) {
                            mixed += output.getShort(info.offset + sourceFrame * channels * 2 + channel * 2).toInt() / channels
                        }
                        val sample = mixed.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort().toInt()
                        packet[packetSize++] = (sample and 0xFF).toByte()
                        packet[packetSize++] = ((sample shr 8) and 0xFF).toByte()
                        resamplePosition += step
                        if (packetSize == packet.size) {
                            if (socket.connected() && running.get()) {
                                socket.emit("room_voice_chunk", JSONObject().put("roomId", roomId), packet.copyOf())
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

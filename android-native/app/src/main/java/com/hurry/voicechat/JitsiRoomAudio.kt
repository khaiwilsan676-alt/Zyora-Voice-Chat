package com.hurry.voicechat

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import io.socket.client.Socket
import io.socket.emitter.Emitter
import org.json.JSONObject
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
    onSpeakingChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    var hasMicPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasMicPermission = granted || ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    LaunchedEffect(canSpeak, hasMicPermission) {
        if (canSpeak && !hasMicPermission) permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    DisposableEffect(context, roomId, roomSocket, canSpeak, hasMicPermission) {
        if (roomSocket == null) return@DisposableEffect onDispose { }
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val oldMode = audioManager.mode
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION

        val alive = AtomicBoolean(true)
        val recording = AtomicBoolean(false)
        var recorder: AudioRecord? = null
        var player: AudioTrack? = null
        var captureThread: Thread? = null

        fun stopCapture() {
            recording.set(false)
            runCatching { recorder?.stop() }
            captureThread?.join(350)
            captureThread = null
            runCatching { recorder?.release() }
            recorder = null
            onSpeakingChanged(false)
        }

        fun startCapture() {
            if (!alive.get() || !hasMicPermission || !canSpeak || recording.get()) return
            val minBuffer = AudioRecord.getMinBufferSize(
                ROOM_VOICE_SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            if (minBuffer <= 0) return
            val created = runCatching {
                AudioRecord(
                    MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                    ROOM_VOICE_SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    maxOf(minBuffer * 2, ROOM_VOICE_FRAME_BYTES * 4)
                )
            }.getOrNull() ?: return
            if (created.state != AudioRecord.STATE_INITIALIZED) {
                created.release()
                return
            }
            recorder = created
            recording.set(true)
            runCatching { created.startRecording() }.onFailure {
                recording.set(false)
                created.release()
                recorder = null
                return
            }
            onSpeakingChanged(true)
            captureThread = thread(name = "ZyoraRoomMic", isDaemon = true) {
                val frame = ByteArray(ROOM_VOICE_FRAME_BYTES)
                var filled = 0
                while (alive.get() && recording.get()) {
                    val count = runCatching { created.read(frame, filled, frame.size - filled) }.getOrDefault(-1)
                    if (count > 0) {
                        filled += count
                        if (filled == frame.size) {
                            if (recording.get() && roomSocket.connected()) {
                                runCatching {
                                    roomSocket.emit(
                                        "room_voice_chunk",
                                        JSONObject().put("roomId", roomId).put("audio", frame.copyOf())
                                    )
                                }
                            }
                            filled = 0
                        }
                    } else if (count < 0) break
                }
            }
        }

        val permissionListener = Emitter.Listener { args ->
            val data = args.firstOrNull() as? JSONObject ?: return@Listener
            if (data.optString("roomId") != roomId) return@Listener
            if (data.optBoolean("allowed", false)) startCapture() else stopCapture()
        }
        val chunkListener = Emitter.Listener { args ->
            val data = args.firstOrNull() as? JSONObject ?: return@Listener
            if (data.optString("roomId") != roomId || !alive.get()) return@Listener
            val bytes = data.opt("audio") as? ByteArray ?: return@Listener
            if (bytes.isEmpty() || bytes.size > 4096) return@Listener
            val output = player ?: run {
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
                                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
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
                        .setBufferSizeInBytes(maxOf(minOut * 2, ROOM_VOICE_FRAME_BYTES * 8))
                        .setTransferMode(AudioTrack.MODE_STREAM)
                        .build()
                        .also { it.play(); player = it }
                }.getOrNull() ?: return@Listener
            }
            runCatching { output.write(bytes, 0, bytes.size) }
        }

        roomSocket.on("room_voice_permission", permissionListener)
        roomSocket.on("room_voice_chunk", chunkListener)
        val disconnectListener = Emitter.Listener {
            stopCapture()
            runCatching { player?.pause(); player?.flush() }
        }
        roomSocket.on(Socket.EVENT_DISCONNECT, disconnectListener)

        val connectListener = Emitter.Listener {
            if (canSpeak && hasMicPermission && alive.get()) {
                roomSocket.emit("room_voice_start", JSONObject().put("roomId", roomId))
            }
        }
        roomSocket.on(Socket.EVENT_CONNECT, connectListener)
        if (canSpeak && hasMicPermission && roomSocket.connected()) {
            roomSocket.emit("room_voice_start", JSONObject().put("roomId", roomId))
        }

        onDispose {
            alive.set(false)
            roomSocket.emit("room_voice_stop", JSONObject().put("roomId", roomId))
            roomSocket.off("room_voice_permission", permissionListener)
            roomSocket.off("room_voice_chunk", chunkListener)
            roomSocket.off(Socket.EVENT_DISCONNECT, disconnectListener)
            roomSocket.off(Socket.EVENT_CONNECT, connectListener)
            stopCapture()
            runCatching { player?.stop(); player?.release() }
            player = null
            audioManager.mode = oldMode
        }
    }

}

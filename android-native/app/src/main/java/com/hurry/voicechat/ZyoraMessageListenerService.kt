package com.hurry.voicechat

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class ZyoraMessageListenerService : Service() {
    private var socket: Socket? = null
    // Socket callbacks must never decode audio, parse message history, or write SQLite on the UI thread.
    private val messageIo = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "zyora-message-io").apply { isDaemon = true }
    }
    private var activeUid = ""
    private var activeAccountId = ""

    override fun onCreate() {
        super.onCreate()
        createChannels()
        startAsForeground()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val auth = getSharedPreferences("hurry_auth", Context.MODE_PRIVATE)
        val user = getSharedPreferences("hurry_user", Context.MODE_PRIVATE)
        activeUid = auth.getString("uid", "").orEmpty()
        activeAccountId = auth.getString("accountId", "").orEmpty().ifBlank {
            user.getString("accountId", "").orEmpty().ifBlank {
                user.getString("userId", activeUid).orEmpty().ifBlank { activeUid }
            }
        }
        if (activeUid.isBlank() && activeAccountId.isBlank()) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (socket == null) connectSocket()
        return START_STICKY
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(
                NotificationChannel("zyora_message_listener", "Zyora connection", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Keeps Zyora message delivery connected in the background"
                    setShowBadge(false)
                }
            )
        }
    }

    private fun startAsForeground() {
        val openApp = PendingIntent.getActivity(
            this, 7000, Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification: Notification = NotificationCompat.Builder(this, "zyora_message_listener")
            .setSmallIcon(R.drawable.ic_stat_zyora)
            .setContentTitle("Zyora")
            .setContentText("Zyora is running")
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setContentIntent(openApp)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(7000, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING)
        } else {
            startForeground(7000, notification)
        }
    }

    private fun connectSocket() {
        val options = IO.Options().apply {
            transports = arrayOf("websocket")
            reconnection = true
            reconnectionAttempts = Int.MAX_VALUE
            reconnectionDelay = 1000
            reconnectionDelayMax = 30000
            timeout = 20000
        }
        val client = IO.socket("https://zyora-server.onrender.com", options)
        socket = client
        client.on(Socket.EVENT_CONNECT) {
            client.emit("register", JSONObject()
                .put("userId", activeUid.ifBlank { activeAccountId })
                .put("accountId", activeAccountId.ifBlank { activeUid }))
        }
        client.on("private_message") { args ->
            val payload = args.firstOrNull() as? JSONObject ?: return@on
            val receiverIds = setOf(activeUid, activeAccountId).filter { it.isNotBlank() }.toSet()
            if (payload.optString("receiverId") !in receiverIds &&
                payload.optString("receiverAccountId") !in receiverIds) return@on
            messageIo.execute { handleIncomingMessage(payload) }
        }
        client.connect()
    }

    private fun handleIncomingMessage(payload: JSONObject) {
        val peerId = payload.optString("senderAccountId").ifBlank { payload.optString("senderId") }
        if (peerId.isBlank()) return
        val messageId = payload.optString("id").ifBlank { "incoming_${System.currentTimeMillis()}" }
        val isVoice = payload.optString("type") == "voice"
        val storedText = if (isVoice) {
            VoiceMessageStore.saveIncoming(this, payload) ?: "VOICE::${payload.optLong("durationSec", 1)}"
        } else payload.optString("text").ifBlank {
            if (payload.optString("type") == "image") "📷 Image" else " "
        }
        val identity = activeAccountId.ifBlank { activeUid }
        val database = NativeIndexDatabase(this)
        val chatKey = "chat_${identity}_$peerId"
        val history = runCatching { JSONArray(database.loadChat(chatKey) ?: "[]") }.getOrDefault(JSONArray())
        val duplicate = (0 until history.length()).any { history.optJSONObject(it)?.optString("id") == messageId }
        if (!duplicate) {
            history.put(JSONObject().put("id", messageId).put("mine", false).put("text", storedText))
            database.saveChat(chatKey, history.toString())
            val unreadPrefs = getSharedPreferences("zyora_unread_messages", Context.MODE_PRIVATE)
            val key = "unread_$identity"
            unreadPrefs.edit().putInt(key, unreadPrefs.getInt(key, 0) + 1).apply()
            // The foreground listener owns incoming messages while Home, Room and Me are visible.
            // Publish the banner here as well as in the open-chat listener so it works outside MessageScreen.
            ZyoraMessageBannerState.current = ZyoraIncomingMessageBanner(
                messageId = messageId,
                senderId = peerId,
                senderName = payload.optString("senderName", "User").ifBlank { "User" },
                senderAvatar = payload.optString("senderPhoto", ""),
                messageText = when {
                    isVoice -> "🎤 Voice message"
                    payload.optString("type") == "image" -> "📷 Image"
                    else -> storedText
                }
            )
        }
        val timestamp = payload.optLong("timestamp", System.currentTimeMillis())
        val preview = JSONObject()
            .put("peerId", peerId)
            .put("peerName", payload.optString("senderName", "User"))
            .put("peerPhoto", payload.optString("senderPhoto", ""))
            .put("lastMessage", if (isVoice) "🎤 Voice message" else storedText)
            .put("timestamp", timestamp)
            .put("time", SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp)))
        database.saveConversation("conversation_${identity}_$peerId", peerId, preview)

    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Swiping Zyora away is an explicit app-close action: release the
        // background Socket.IO connection so the server marks this account offline.
        socket?.let { client ->
            client.off()
            client.disconnect()
            client.close()
        }
        socket = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        socket?.let { it.off(); it.disconnect(); it.close() }
        socket = null
        messageIo.shutdownNow()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

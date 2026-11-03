package com.hurry.voicechat

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private const val ZYORA_MESSAGE_CHANNEL = "zyora_messages"

internal fun registerZyoraPushToken(context: Context, accountId: String) {
    if (accountId.isBlank()) return
    FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
        Thread {
            runCatching {
                val connection = (URL("https://zyora-server.onrender.com/api/push-token").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 10000
                    readTimeout = 10000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                }
                try {
                    val body = JSONObject().put("accountId", accountId).put("token", token).toString()
                    connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                    connection.responseCode
                } finally {
                    connection.disconnect()
                }
            }
        }.start()
    }
}

class ZyoraFirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val prefs = getSharedPreferences("hurry_user", Context.MODE_PRIVATE)
        val accountId = prefs.getString("accountId", prefs.getString("userId", prefs.getString("uid", ""))).orEmpty()
        if (accountId.isNotBlank()) registerZyoraPushToken(this, accountId)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        showMessageNotification()
    }

    private fun showMessageNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    ZYORA_MESSAGE_CHANNEL,
                    "Zyora messages",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications for new Zyora messages"
                    enableVibration(true)
                }
            )
        }
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            7001,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, ZYORA_MESSAGE_CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_zyora)
            .setContentTitle("Zyora")
            .setContentText("You have message from Zyora")
            .setStyle(NotificationCompat.BigTextStyle().bigText("You have message from Zyora"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        runCatching { NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), notification) }
    }
}

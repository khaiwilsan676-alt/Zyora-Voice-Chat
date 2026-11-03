package com.hurry.voicechat

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

private const val ZYORA_LOCAL_MESSAGE_CHANNEL = "zyora_local_messages"

internal fun showZyoraLocalMessageNotification(context: Context) {
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        manager.createNotificationChannel(
            NotificationChannel(
                ZYORA_LOCAL_MESSAGE_CHANNEL,
                "Zyora messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for new Zyora messages"
                enableVibration(true)
            }
        )
    }

    val launchIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
    val pendingIntent = PendingIntent.getActivity(
        context,
        7001,
        launchIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val appIcon = runCatching {
        val drawable = context.packageManager.getApplicationIcon(context.packageName)
        val width = drawable.intrinsicWidth.coerceAtLeast(1)
        val height = drawable.intrinsicHeight.coerceAtLeast(1)
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
        }
    }.getOrNull()

    val notification = NotificationCompat.Builder(context, ZYORA_LOCAL_MESSAGE_CHANNEL)
        .setSmallIcon(R.drawable.ic_stat_zyora)
        .setLargeIcon(appIcon)
        .setContentTitle("Zyora")
        .setContentText("You have message from Zyora")
        .setStyle(NotificationCompat.BigTextStyle().bigText("You have message from Zyora"))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setCategory(NotificationCompat.CATEGORY_MESSAGE)
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)
        .build()

    runCatching {
        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            NotificationManagerCompat.from(context).notify(
                (System.currentTimeMillis() and 0x7fffffff).toInt(),
                notification
            )
        }
    }
}

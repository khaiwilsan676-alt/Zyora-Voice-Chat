package com.hurry.voicechat

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

/**
 * Keeps room music playback alive when the room sheet is minimized or Zyora
 * is moved to the background. This service only handles the user's selected
 * local audio track; it does not change room UI or voice-chat permissions.
 */
class RoomMusicService : Service() {
    private var player: MediaPlayer? = null
    private var currentUri: String? = null

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Room music",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Music playing in a Zyora room" }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> {
                val uri = intent.getStringExtra(EXTRA_URI).orEmpty()
                val title = intent.getStringExtra(EXTRA_TITLE).orEmpty().ifBlank { "Room music" }
                if (uri.isBlank()) return START_NOT_STICKY
                startForeground(NOTIFICATION_ID, buildNotification(title))
                if (currentUri != uri || player == null) {
                    releasePlayer()
                    currentUri = uri
                    runCatching {
                        player = MediaPlayer().apply {
                            setAudioAttributes(
                                AudioAttributes.Builder()
                                    .setUsage(AudioAttributes.USAGE_MEDIA)
                                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                    .build()
                            )
                            setDataSource(this@RoomMusicService, Uri.parse(uri))
                            setOnCompletionListener {
                                releasePlayer()
                                stopForeground(STOP_FOREGROUND_REMOVE)
                                stopSelf()
                            }
                            setOnErrorListener { _, _, _ ->
                                releasePlayer()
                                stopForeground(STOP_FOREGROUND_REMOVE)
                                stopSelf()
                                true
                            }
                            prepare()
                            start()
                        }
                    }.onFailure {
                        releasePlayer()
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }
                } else if (player?.isPlaying == false) {
                    runCatching { player?.start() }
                }
            }
            ACTION_PAUSE -> runCatching { if (player?.isPlaying == true) player?.pause() }
            ACTION_STOP -> {
                releasePlayer()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            else -> return START_NOT_STICKY
        }
        return START_NOT_STICKY
    }

    private fun buildNotification(title: String): Notification {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = launchIntent?.let {
            PendingIntent.getActivity(
                this, 0, it,
                PendingIntent.FLAG_UPDATE_CURRENT or
                    (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Zyora · Room music")
            .setContentText(title)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun releasePlayer() {
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
        currentUri = null
    }

    override fun onDestroy() {
        releasePlayer()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_PLAY = "com.hurry.voicechat.roommusic.PLAY"
        const val ACTION_PAUSE = "com.hurry.voicechat.roommusic.PAUSE"
        const val ACTION_STOP = "com.hurry.voicechat.roommusic.STOP"
        const val EXTRA_URI = "track_uri"
        const val EXTRA_TITLE = "track_title"
        private const val CHANNEL_ID = "zyora_room_music"
        private const val NOTIFICATION_ID = 7301
    }
}

package com.hurry.voicechat

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class ZyoraMessageBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val auth = context.getSharedPreferences("hurry_auth", Context.MODE_PRIVATE)
        val user = context.getSharedPreferences("hurry_user", Context.MODE_PRIVATE)
        val uid = auth.getString("uid", "").orEmpty()
        val accountId = auth.getString("accountId", "").orEmpty().ifBlank {
            user.getString("accountId", "").orEmpty().ifBlank {
                user.getString("userId", uid).orEmpty().ifBlank { uid }
            }
        }
        if (uid.isBlank() && accountId.isBlank()) return
        val service = Intent(context, ZyoraMessageListenerService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(service)
        else context.startService(service)
    }
}

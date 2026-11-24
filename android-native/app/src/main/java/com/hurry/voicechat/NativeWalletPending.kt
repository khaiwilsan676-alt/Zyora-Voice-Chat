package com.hurry.voicechat

import android.content.Context
import org.json.JSONArray

/**
 * All native wallet surfaces must project the same in-flight game transactions.
 * Game-specific queues remain durable for retry/idempotency, but they are not
 * separate balances: every screen sums and acknowledges both queues.
 */
internal object NativeWalletPending {
    private val preferenceFiles = listOf(
        "wild_party_wallet_pending",
        "dragon_battle_wallet_pending"
    )

    fun delta(context: Context, userId: String): Long {
        if (userId.isBlank()) return 0L
        return preferenceFiles.sumOf { preferenceName ->
            val prefs = context.getSharedPreferences(preferenceName, Context.MODE_PRIVATE)
            synchronized(prefs) {
                val queue = runCatching {
                    JSONArray(prefs.getString("queue_$userId", "[]") ?: "[]")
                }.getOrDefault(JSONArray())
                (0 until queue.length()).sumOf { index ->
                    queue.optJSONObject(index)?.optLong("delta") ?: 0L
                }
            }
        }
    }

    fun acknowledge(context: Context, userId: String, transactionId: String) {
        if (userId.isBlank() || transactionId.isBlank()) return
        preferenceFiles.forEach { preferenceName ->
            val prefs = context.getSharedPreferences(preferenceName, Context.MODE_PRIVATE)
            synchronized(prefs) {
                val key = "queue_$userId"
                val queue = runCatching {
                    JSONArray(prefs.getString(key, "[]") ?: "[]")
                }.getOrDefault(JSONArray())
                val remaining = JSONArray()
                for (index in 0 until queue.length()) {
                    val item = queue.optJSONObject(index) ?: continue
                    if (item.optString("id") != transactionId) remaining.put(item)
                }
                prefs.edit().putString(key, remaining.toString()).commit()
            }
        }
    }
}

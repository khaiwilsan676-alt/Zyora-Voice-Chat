package com.hurry.voicechat

data class HurryRoom(
    val id: String,
    val name: String,
    val image: String,
    val announcement: String = "",
    val country: String = "🇮🇳",
    val locked: Boolean = false,
    val accountId: String = ""
)

data class HurryUser(
    val id: String = "",
    val name: String = "Hurry User",
    val image: String = ""
)

enum class HurryTab { HOME, MESSAGE, ME }

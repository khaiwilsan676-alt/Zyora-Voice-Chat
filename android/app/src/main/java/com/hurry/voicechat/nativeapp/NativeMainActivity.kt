package com.hurry.voicechat.nativeapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.hurry.voicechat.*

private enum class NativeRoute { HOME, MESSAGE, ME, SEARCH, WALLET, SETTING, LANGUAGE, LEADERBOARD, LEVEL, STORE, SVIP, ROOM }

class NativeMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContent {
            MaterialTheme {
                var route by rememberSaveable { mutableStateOf(NativeRoute.HOME) }
                val back: () -> Unit = {
                    route = when (route) {
                        NativeRoute.HOME -> NativeRoute.HOME
                        NativeRoute.MESSAGE, NativeRoute.ME, NativeRoute.SEARCH -> NativeRoute.HOME
                        NativeRoute.WALLET, NativeRoute.SETTING, NativeRoute.LANGUAGE,
                        NativeRoute.LEADERBOARD, NativeRoute.LEVEL, NativeRoute.STORE,
                        NativeRoute.SVIP -> NativeRoute.ME
                        NativeRoute.ROOM -> NativeRoute.HOME
                    }
                }
                when (route) {
                    NativeRoute.HOME -> HomePage(
                        onOpenMessage = { route = NativeRoute.MESSAGE },
                        onOpenMe = { route = NativeRoute.ME },
                        onSearch = { route = NativeRoute.SEARCH },
                        onOpenRoom = { route = NativeRoute.ROOM }
                    )
                    NativeRoute.MESSAGE -> MessagePage(onChatOpen = {})
                    NativeRoute.ME -> MePage(
                        onBack = { route = NativeRoute.HOME },
                        onWallet = { route = NativeRoute.WALLET },
                        onSetting = { route = NativeRoute.SETTING }
                    )
                    NativeRoute.SEARCH -> SearchPage(onBack = back)
                    NativeRoute.WALLET -> Wallet(onBack = back)
                    NativeRoute.SETTING -> SettingPage(onBack = back)
                    NativeRoute.LANGUAGE -> LanguagePage(onBack = back)
                    NativeRoute.LEADERBOARD -> Leaderboard(onBack = back)
                    NativeRoute.LEVEL -> Level(onBack = back)
                    NativeRoute.STORE -> StorePage(onBack = back)
                    NativeRoute.SVIP -> Svip(onBack = back)
                    NativeRoute.ROOM -> RoomPage(
                        roomOwner = RoomPageOwner(),
                        currentUser = RoomPageCurrentUser(),
                        onClose = back,
                        onBack = back
                    )
                }
            }
        }
    }
}

package com.hurry.voicechat.nativeapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

                Box(modifier = Modifier.fillMaxSize()) {
                    
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

                    if (route == NativeRoute.HOME || route == NativeRoute.MESSAGE || route == NativeRoute.ME) {
                        MineFloatingButtonUI(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = 50.dp, y = 50.dp), 
                            onClick = { route = NativeRoute.ME }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MineFloatingButtonUI(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .size(180.dp)
            .clip(CircleShape)
            .background(Color(0xFFEF4D35))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.offset(x = (-30).dp, y = (-30).dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.Transparent)
                        .border(4.dp, Color.White, CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color.Transparent)
                        .border(4.dp, Color.White, CircleShape)
                )
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Text(
                text = "Mine",
                color = Color(0xFFB03322), 
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

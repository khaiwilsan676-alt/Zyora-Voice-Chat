package com.hurry.voicechat

import android.content.Context

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext

private val SettingsInk = Color(0xFF0F172A)
private val SettingsBody = Color(0xFF1E293B)
private val SettingsMuted = Color(0xFF94A3B8)
private val SettingsDivider = Color(0xFFF1F5F9)

@Composable
fun SettingsPage(
    onBack: () -> Unit = {},
    onLogout: () -> Unit = {},
    languageCode: String = "en"
) {
    val settingsContext = LocalContext.current
    val settingsPrefs = remember { settingsContext.getSharedPreferences("zyora_settings", Context.MODE_PRIVATE) }
    var notificationsEnabled by remember { mutableStateOf(settingsPrefs.getBoolean("appNotifications", true)) }
    var showBlocklist by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }

    if (showBlocklist) {
        Column(Modifier.fillMaxSize().background(Color.White)) {
            SettingsSubHeader(nativeText(languageCode, "blocklist")) { showBlocklist = false }
            Box(
                Modifier.fillMaxWidth().weight(1f).padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No data", fontSize = 17.sp, fontWeight = FontWeight.Medium, color = Color(0xFF9CA3AF), letterSpacing = 0.3.sp)
            }
        }
        return
    }

    if (showAbout) {
        Column(Modifier.fillMaxSize().background(Color.White)) {
            SettingsSubHeader(nativeText(languageCode, "about")) { showAbout = false }
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 32.dp)
            ) {
                Text(
                    "About Us",
                    modifier = Modifier.fillMaxWidth().padding(bottom = 28.dp),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    letterSpacing = (-0.4).sp
                )
                Text(
                    "Zyora is a real-time group voice chat application for people from different places to connect. You can find new friends who share the same interests, chat with them, and enjoy the fun of the party!",
                    fontSize = 15.sp, lineHeight = 25.sp, color = Color(0xFF333333), letterSpacing = 0.2.sp
                )
                Spacer(Modifier.height(24.dp))
                Text("We have the following functions:", fontSize = 15.sp, lineHeight = 25.sp, color = Color(0xFF333333))
                Spacer(Modifier.height(12.dp))
                Text("• Free", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF333333))
                Text("You can have free, high-quality and stable voice chat through the Internet.", fontSize = 15.sp, lineHeight = 25.sp, color = Color(0xFF333333))
                Spacer(Modifier.height(16.dp))
                Text("• Online party", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF333333))
                Text("Join different online parties and topic rooms, including birthday parties, wedding parties, games, and more.", fontSize = 15.sp, lineHeight = 25.sp, color = Color(0xFF333333))
                Spacer(Modifier.height(16.dp))
                Text("• Gorgeous gifts", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF333333))
                Text("While enjoying chats with friends, you can send them gifts with special effects and have more fun together.", fontSize = 15.sp, lineHeight = 25.sp, color = Color(0xFF333333))
            }
        }
        return
    }

    Column(Modifier.fillMaxSize().background(Color.White)) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding()
                .padding(start = 12.dp, end = 12.dp, top = 16.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(40.dp).clickable { onBack() }.padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Canvas(Modifier.size(24.dp)) {
                    val stroke = 2.4.dp.toPx()
                    drawLine(SettingsInk, androidx.compose.ui.geometry.Offset(size.width * 0.86f, size.height / 2), androidx.compose.ui.geometry.Offset(size.width * 0.16f, size.height / 2), strokeWidth = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    drawLine(SettingsInk, androidx.compose.ui.geometry.Offset(size.width * 0.16f, size.height / 2), androidx.compose.ui.geometry.Offset(size.width * 0.45f, size.height * 0.20f), strokeWidth = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    drawLine(SettingsInk, androidx.compose.ui.geometry.Offset(size.width * 0.16f, size.height / 2), androidx.compose.ui.geometry.Offset(size.width * 0.45f, size.height * 0.80f), strokeWidth = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                }
            }
            Text(
                nativeText(languageCode, "settings"),
                modifier = Modifier.weight(1f),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = SettingsInk,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.width(40.dp))
        }

        Spacer(Modifier.height(16.dp))
        Column(Modifier.fillMaxWidth().background(Color.White)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(nativeText(languageCode, "notifications"), Modifier.weight(1f), fontSize = 16.sp, color = SettingsBody)
                SettingsToggle(checked = notificationsEnabled) {
                    notificationsEnabled = it
                    settingsPrefs.edit().putBoolean("appNotifications", it).apply()
                }
            }
            SettingsDividerLine()
            SettingsNavigationRow(nativeText(languageCode, "blocklist")) { showBlocklist = true }
            SettingsDividerLine()
            SettingsNavigationRow(nativeText(languageCode, "about")) { showAbout = true }
        }

        Spacer(Modifier.height(40.dp))
        Box(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                .height(50.dp).border(1.dp, Color(0xFFCBD5E1), CircleShape)
                .background(Color.White, CircleShape)
                .clickable { onLogout() },
            contentAlignment = Alignment.Center
        ) {
            Text(nativeText(languageCode, "logout"), fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color(0xFF334155))
        }
    }
}

@Composable
private fun SettingsSubHeader(title: String, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(32.dp).clickable { onBack() }, contentAlignment = Alignment.Center) {
            androidx.compose.foundation.Canvas(Modifier.size(24.dp)) {
                val stroke = 2.4.dp.toPx()
                drawLine(SettingsInk, androidx.compose.ui.geometry.Offset(size.width * 0.86f, size.height / 2), androidx.compose.ui.geometry.Offset(size.width * 0.16f, size.height / 2), strokeWidth = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                drawLine(SettingsInk, androidx.compose.ui.geometry.Offset(size.width * 0.16f, size.height / 2), androidx.compose.ui.geometry.Offset(size.width * 0.45f, size.height * 0.20f), strokeWidth = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                drawLine(SettingsInk, androidx.compose.ui.geometry.Offset(size.width * 0.16f, size.height / 2), androidx.compose.ui.geometry.Offset(size.width * 0.45f, size.height * 0.80f), strokeWidth = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            }
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = SettingsInk)
        }
        Spacer(Modifier.width(32.dp))
    }
}

@Composable
private fun SettingsNavigationRow(title: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, Modifier.weight(1f), fontSize = 16.sp, color = SettingsBody)
        androidx.compose.foundation.Canvas(Modifier.size(20.dp)) {
            val stroke = 1.8.dp.toPx()
            drawLine(SettingsMuted, androidx.compose.ui.geometry.Offset(size.width * 0.38f, size.height * 0.2f), androidx.compose.ui.geometry.Offset(size.width * 0.68f, size.height * 0.5f), strokeWidth = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            drawLine(SettingsMuted, androidx.compose.ui.geometry.Offset(size.width * 0.68f, size.height * 0.5f), androidx.compose.ui.geometry.Offset(size.width * 0.38f, size.height * 0.8f), strokeWidth = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        }
    }
}

@Composable
private fun SettingsDividerLine() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(SettingsDivider))
}

@Composable
private fun SettingsToggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Box(
        Modifier.width(48.dp).height(28.dp)
            .background(if (checked) Color(0xFF22D3EE) else Color(0xFFCBD5E1), RoundedCornerShape(50))
            .clickable { onCheckedChange(!checked) }
            .padding(3.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(Modifier.size(22.dp).background(Color.White, CircleShape))
    }
}

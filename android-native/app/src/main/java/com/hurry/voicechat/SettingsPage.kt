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
    onLogout: () -> Unit = {}
) {
    val settingsContext = LocalContext.current
    val settingsPrefs = remember { settingsContext.getSharedPreferences("zyora_settings", Context.MODE_PRIVATE) }
    var notificationsEnabled by remember { mutableStateOf(settingsPrefs.getBoolean("appNotifications", true)) }
    var showBlocklist by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }

    if (showBlocklist) {
        Column(Modifier.fillMaxSize().background(Color.White)) {
            SettingsSubHeader("Blocklist") { showBlocklist = false }
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
            SettingsSubHeader("About Us") { showAbout = false }
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
                    "Welcome to Zyora",
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2563EB),
                    textAlign = TextAlign.Center
                )
                Text(
                    "Zyora is a social voice-chat application made to bring people together. Join live voice rooms, meet new people, make friends, and enjoy conversations with a community that shares your interests.",
                    fontSize = 15.sp, lineHeight = 25.sp, color = Color(0xFF333333), letterSpacing = 0.2.sp
                )
                Spacer(Modifier.height(24.dp))
                Text("What you can do on Zyora", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF111827))
                Spacer(Modifier.height(14.dp))
                Text("• Live Voice Rooms", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF333333))
                Text("Join rooms and talk with other people in real time.", fontSize = 15.sp, lineHeight = 25.sp, color = Color(0xFF333333))
                Spacer(Modifier.height(16.dp))
                Text("• Meet New Friends", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF333333))
                Text("Discover people, connect through conversations, and build new friendships.", fontSize = 15.sp, lineHeight = 25.sp, color = Color(0xFF333333))
                Spacer(Modifier.height(16.dp))
                Text("• Social Features", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF333333))
                Text("Personalize your profile, interact with others, and enjoy the features available in the app.", fontSize = 15.sp, lineHeight = 25.sp, color = Color(0xFF333333))
                Spacer(Modifier.height(24.dp))
                Text("Our Goal", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF111827))
                Spacer(Modifier.height(8.dp))
                Text("We want Zyora to be a fun, welcoming, and respectful place for people to connect. Please respect other users and help keep every room friendly and safe.", fontSize = 15.sp, lineHeight = 25.sp, color = Color(0xFF333333))
                Spacer(Modifier.height(24.dp))
                Text("Your Privacy", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF111827))
                Spacer(Modifier.height(8.dp))
                Text("You can clear Zyora's locally stored app data at any time from Android Settings. Clearing app storage may sign you out and remove information saved on your phone.", fontSize = 15.sp, lineHeight = 25.sp, color = Color(0xFF333333))
                Spacer(Modifier.height(24.dp))
                Text("Thank you for being part of Zyora!", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2563EB))
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
                    val stroke = 2.2.dp.toPx()
                    drawLine(SettingsInk, androidx.compose.ui.geometry.Offset(size.width * 0.88f, size.height / 2), androidx.compose.ui.geometry.Offset(size.width * 0.15f, size.height / 2), strokeWidth = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    drawLine(SettingsInk, androidx.compose.ui.geometry.Offset(size.width * 0.15f, size.height / 2), androidx.compose.ui.geometry.Offset(size.width * 0.48f, size.height * 0.18f), strokeWidth = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    drawLine(SettingsInk, androidx.compose.ui.geometry.Offset(size.width * 0.15f, size.height / 2), androidx.compose.ui.geometry.Offset(size.width * 0.48f, size.height * 0.82f), strokeWidth = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                }
            }
            Text(
                "Settings",
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
                Text("Message Notifications", Modifier.weight(1f), fontSize = 16.sp, color = SettingsBody)
                SettingsToggle(checked = notificationsEnabled) {
                    notificationsEnabled = it
                    settingsPrefs.edit().putBoolean("appNotifications", it).apply()
                }
            }
            SettingsDividerLine()
            SettingsNavigationRow("Blocklist") { showBlocklist = true }
            SettingsDividerLine()
            SettingsNavigationRow("About Us") { showAbout = true }
        }

        Spacer(Modifier.height(40.dp))
        Box(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                .height(50.dp).border(1.dp, Color(0xFFCBD5E1), CircleShape)
                .background(Color.White, CircleShape)
                .clickable { onLogout() },
            contentAlignment = Alignment.Center
        ) {
            Text("Log out", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color(0xFF334155))
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
                val stroke = 2.2.dp.toPx()
                drawLine(SettingsInk, androidx.compose.ui.geometry.Offset(size.width * 0.88f, size.height / 2), androidx.compose.ui.geometry.Offset(size.width * 0.15f, size.height / 2), strokeWidth = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                drawLine(SettingsInk, androidx.compose.ui.geometry.Offset(size.width * 0.15f, size.height / 2), androidx.compose.ui.geometry.Offset(size.width * 0.48f, size.height * 0.18f), strokeWidth = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                drawLine(SettingsInk, androidx.compose.ui.geometry.Offset(size.width * 0.15f, size.height / 2), androidx.compose.ui.geometry.Offset(size.width * 0.48f, size.height * 0.82f), strokeWidth = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
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

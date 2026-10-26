package com.hurry.voicechat

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.window.Dialog

private val SettingsInk = Color(0xFF0F172A)
private val SettingsBody = Color(0xFF1E293B)
private val SettingsMuted = Color(0xFF94A3B8)
private val SettingsDivider = Color(0xFFF1F5F9)
private val PageBackgroundGrey = Color(0xFFF5F5F5) 
private val LogoutRed = Color(0xFFE53935)
private val DeleteOrangeIcon = Color(0xFFF05133)
// GradientBlue is kept just in case you need it later, but removed from Delete Account UI
private val GradientBlue = Color(0xFF0055FF) 

@Composable
fun SettingsPage(
    onBack: () -> Unit = {},
    onLogout: () -> Unit = {},
    languageCode: String = "en"
) {
    val settingsContext = LocalContext.current
    var showAbout by remember { mutableStateOf(false) }
    var showPrivacyPolicy by remember { mutableStateOf(false) }
    var showDeleteAccount by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Dialog logic - Center Popup
    if (showDeleteConfirmDialog) {
        Dialog(onDismissRequest = { showDeleteConfirmDialog = false }) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Account Delete",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Are you sure? Do you want to delete your account Permanent",
                        fontSize = 15.sp,
                        color = Color(0xFF4B5563),
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Cancel Button (Grey)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .background(Color(0xFFE5E7EB), RoundedCornerShape(8.dp))
                                .clickable { showDeleteConfirmDialog = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Cancel", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color(0xFF4B5563))
                        }
                        
                        // Confirm Button (Red)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .background(LogoutRed, RoundedCornerShape(8.dp))
                                .clickable { 
                                    showDeleteConfirmDialog = false
                                    // Add logic for account deletion here
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Confirm", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // Privacy Policy
    if (showPrivacyPolicy) {
        Column(
            Modifier.fillMaxSize().background(Color.White)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(top = 12.dp, bottom = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Fixed Left Arrow & Safe Area
                Box(Modifier.size(32.dp).clickable { showPrivacyPolicy = false }, contentAlignment = Alignment.Center) {
                    androidx.compose.foundation.Canvas(Modifier.size(24.dp)) {
                        val stroke = 2.4.dp.toPx()
                        drawLine(SettingsInk, androidx.compose.ui.geometry.Offset(size.width * 0.86f, size.height / 2), androidx.compose.ui.geometry.Offset(size.width * 0.16f, size.height / 2), strokeWidth = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                        drawLine(SettingsInk, androidx.compose.ui.geometry.Offset(size.width * 0.16f, size.height / 2), androidx.compose.ui.geometry.Offset(size.width * 0.45f, size.height * 0.20f), strokeWidth = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                        drawLine(SettingsInk, androidx.compose.ui.geometry.Offset(size.width * 0.16f, size.height / 2), androidx.compose.ui.geometry.Offset(size.width * 0.45f, size.height * 0.80f), strokeWidth = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    }
                }
                Text(
                    "Privacy Policy",
                    color = Color(0xFF111827),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
            Text("Privacy Policy", color = Color(0xFF111827), fontSize = 25.sp, fontWeight = FontWeight.Bold)
            Text("Last updated: October 14, 2026", color = Color(0xFF6B7280), fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp, bottom = 22.dp))
            SettingsPrivacySection("1. About Zyora", "Zyora is a social voice-chat application where people can create profiles, join voice rooms, meet others, chat, and enjoy social features together.")
            SettingsPrivacySection("2. Information We Collect", "When you use Zyora, we may handle the details you choose to share, such as your profile name, photo, account details, messages, and activity in the app. Microphone access is used when you participate in live voice rooms. We only describe information that is needed to provide and support the app’s features.")
            SettingsPrivacySection("3. How We Use Information", "Your information helps you access your account, maintain your profile, connect with other users, participate in rooms and chats, improve your experience, and help keep Zyora safe. Zyora does not sell your personal information.")
            SettingsPrivacySection("4. Your Account and Data", "Your account information and content associated with your account are retained to help keep your Zyora experience consistent over time, including when you return to the app. Deleting the app from your phone does not automatically delete information associated with your account. If you want to request access, correction, or deletion of your account information, contact Zyora Support. Some information may need to be retained where required for safety, security, or legal reasons.")
            SettingsPrivacySection("5. App Permissions", "Zyora may request internet access to provide online features and microphone access when you use live voice chat. You can manage app permissions through your device settings.")
            SettingsPrivacySection("6. Keeping Your Information Safe", "We take reasonable steps to protect accounts and information and to support a safer community. Keep your sign-in details private and be careful about sharing sensitive information in public rooms. No online service can promise absolute security.")
            SettingsPrivacySection("7. Sharing Information", "We do not sell your personal information. Information may be used when necessary to provide Zyora’s features, protect users and the service, respond to support requests, or meet applicable legal obligations.")
            SettingsPrivacySection("8. Policy Updates and Contact", "We may update this policy as Zyora evolves. If you have questions or a privacy-related request, reach out to the Zyora team through the support option in the app.")
            Spacer(Modifier.height(30.dp))
        }
        return
    }

    // Delete Account Page Logic
    if (showDeleteAccount) {
        Box(
            Modifier
                .fillMaxSize()
                // Pura grey karr diya, blue gradient hata diya
                .background(PageBackgroundGrey)
        ) {
            Column(Modifier.fillMaxSize()) {
                SettingsSubHeader("Delete Account") { showDeleteAccount = false }
                
                // White Info Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                        .background(Color.White, RoundedCornerShape(16.dp))
                        .padding(24.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Warning Icon
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(DeleteOrangeIcon, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("!", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "Are you sure you want to\ndelete this account (" + (settingsContext.getSharedPreferences("hurry_auth", Context.MODE_PRIVATE).getString("name", "User") ?: "User") + " ID: " + (settingsContext.getSharedPreferences("hurry_auth", Context.MODE_PRIVATE).getString("accountId", null)?.takeIf { it.isNotBlank() } ?: settingsContext.getSharedPreferences("hurry_auth", Context.MODE_PRIVATE).getString("uid", "").orEmpty()) + ")?",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            textAlign = TextAlign.Center,
                            lineHeight = 26.sp
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // List items
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            DeleteAccountListItem("1. All account-related information, including friends, followers, and chat records, will be permanently deleted.")
                            DeleteAccountListItem("2. Your rank, account balance, and all personal data will be cleared.")
                            DeleteAccountListItem("3. Once deleted, the account cannot be recovered, and you will no longer be able to log in.")
                            DeleteAccountListItem("4. The account deletion process takes 7 days, during which you can request recovery.")
                        }
                    }
                }
            }
            
            // Bottom Action Buttons
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Cancel Button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .background(Color.White, RoundedCornerShape(8.dp))
                        .clickable { showDeleteAccount = false },
                    contentAlignment = Alignment.Center
                ) {
                    Text("Cancel", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color(0xFF6B7280))
                }
                
                // Delete Button (Opens Dialog)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .background(LogoutRed, RoundedCornerShape(8.dp))
                        .clickable { showDeleteConfirmDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Text("Delete", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color.White)
                }
            }
        }
        return
    }

    // About Page Logic
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
                    "Welcome to Zyora — a place to connect, make friends, and share good moments. Join lively voice rooms, meet people with similar interests, chat together, express yourself, and enjoy a welcoming social community wherever you are.",
                    fontSize = 15.sp, lineHeight = 25.sp, color = Color(0xFF333333), letterSpacing = 0.2.sp
                )
                Spacer(Modifier.height(24.dp))
                Text("Discover what you can enjoy on Zyora:", fontSize = 15.sp, lineHeight = 25.sp, color = Color(0xFF333333))
                Spacer(Modifier.height(12.dp))
                Text("• Voice Chat", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF333333))
                Text("Join voice rooms, talk with friends in real time, and enjoy conversations with people from different places.", fontSize = 15.sp, lineHeight = 25.sp, color = Color(0xFF333333))
                Spacer(Modifier.height(16.dp))
                Text("• Rooms & Community", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF333333))
                Text("Explore rooms for different interests, join group conversations, and be part of shared moments and online parties.", fontSize = 15.sp, lineHeight = 25.sp, color = Color(0xFF333333))
                Spacer(Modifier.height(16.dp))
                Text("• Gifts & Expression", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF333333))
                Text("Send virtual gifts, celebrate your friends, and make your time together more memorable.", fontSize = 15.sp, lineHeight = 25.sp, color = Color(0xFF333333))
            }
        }
        return
    }

    // Main Settings Page
    Column(Modifier.fillMaxSize().background(PageBackgroundGrey)) { 
        
        // Top Section
        Column(Modifier.fillMaxWidth().background(Color.White)) {
            // Header
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
                    text = "Setting",
                    modifier = Modifier.weight(1f),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SettingsInk,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.width(40.dp))
            }

            SettingsDividerLine()
            
            // Options - Underscores removed, direct string used
            SettingsNavigationRow("Privacy Policy") { showPrivacyPolicy = true }
            SettingsDividerLine()
            SettingsNavigationRow("About Us") { showAbout = true }
            SettingsDividerLine()
            SettingsNavigationRow("Delete Account") { showDeleteAccount = true }
        }

        Spacer(Modifier.height(16.dp))

        // Logout Section
        Box(
            Modifier
                .fillMaxWidth()
                .background(Color.White)
                .clickable { onLogout() }
                .padding(vertical = 18.dp), 
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Log Out", 
                fontSize = 16.sp, 
                fontWeight = FontWeight.Medium, 
                color = LogoutRed 
            )
        }
    }
}

@Composable
private fun SettingsPrivacySection(title: String, body: String) {
    Column(Modifier.fillMaxWidth().padding(bottom = 18.dp)) {
        Text(title, color = Color(0xFF111827), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(body, color = Color(0xFF374151), fontSize = 14.sp, lineHeight = 22.sp)
    }
}

@Composable
private fun DeleteAccountListItem(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        color = Color(0xFF4B5563),
        lineHeight = 20.sp,
        textAlign = TextAlign.Start,
        modifier = Modifier.fillMaxWidth()
    )
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
            .padding(horizontal = 20.dp, vertical = 18.dp),
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

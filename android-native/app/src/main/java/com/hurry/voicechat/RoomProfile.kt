package com.hurry.voicechat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// --- Helper Data Classes & Functions ---

typealias RoomProfileUser = RoomUser

data class RoomUser(
    val id: String? = null,
    val uid: String? = null,
    val accountId: String? = null,
    val name: String = "User",
    val image: String = "",
    val gender: String = "♂",
    val age: Int = 24,
    val country: String = "India",
    val flag: String = "🇮🇳",
    val followers: Int = 0,
    val isFollowing: Boolean = false,
    val isOnline: Boolean = true,
    val isInSeat: Boolean = false,
    val isMuted: Boolean = false,
    val isLocked: Boolean = false
)

fun generateStableId(input: String): String {
    return input // Placeholder for your actual hash/id generation logic
}

// --- Shader Placeholders ---
// Note: Actual WebGL shaders require GLSurfaceView or AGSL (RuntimeShader) in Android.
// We use these placeholders where you can integrate ExoPlayer/SVGA or ColorMatrix filters later.

@Composable
fun GreenColorRemovalShader(imageSrc: String, modifier: Modifier = Modifier) {
    AsyncImage(
        model = imageSrc,
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.Fit
    )
}

@Composable
fun WhiteColorRemovalShader(imageSrc: String, modifier: Modifier = Modifier) {
    AsyncImage(
        model = imageSrc,
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.Fit
    )
}

// --- Main RoomProfile Component ---

@Composable
fun RoomProfile(
    user: RoomUser,
    isCurrentUser: Boolean = false,
    isRoomOwner: Boolean = false,
    onClose: () -> Unit,
    onCopyId: () -> Unit = {},
    onLeaveSeat: () -> Unit = {},
    onMention: (String) -> Unit = {},
    onFollow: () -> Unit = {},
    onMessage: () -> Unit = {},
    onThirdAction: () -> Unit = {},
    onMute: () -> Unit = {},
    onLock: () -> Unit = {},
    onKickOut: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Tag States
    var adminTag by remember { mutableStateOf(false) }
    var officialTag by remember { mutableStateOf(false) }
    var vipTag by remember { mutableStateOf(false) }
    var premiumTag by remember { mutableStateOf(false) }
    var showReports by remember { mutableStateOf(false) }

    val hasAnyTag = adminTag || officialTag || vipTag || premiumTag
    val accountId = generateStableId(user.accountId ?: user.id ?: user.uid ?: "User")
    
    val genderColor = if (user.gender.equals("female", true) || user.gender.equals("f", true) || user.gender == "♀") {
        Color(0xFFEC4899) // pink-500
    } else {
        Color(0xFF3B82F6) // blue-500
    }

    val showLeaveSeat = isCurrentUser && user.isInSeat
    val showModerationRow = isRoomOwner && !isCurrentUser
    val showActions = !isCurrentUser

    // Fetch Tags
    LaunchedEffect(user.uid, user.id, user.accountId) {
        val uid = user.uid ?: user.id ?: user.accountId
        if (uid != null && uid != "N/A" && uid != "User") {
            try {
                // Placeholder for actual API call
                // val response = fetch("your_api/users?uid=$uid")
                // Parsing logic...
                // adminTag = data.adminTag; etc...
            } catch (e: Exception) {
                // Handle error quietly
            }
        }
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
                .clickable { onClose() }
        ) {
            // Sheet Container (Aligns to bottom)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .clickable(enabled = false) {} // Consume clicks so it doesn't close
            ) {
                // White Background Box (Starts 48dp below to leave room for avatar overlap)
                Column(
                    modifier = Modifier
                        .padding(top = 44.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .background(Color.White)
                        .padding(top = 56.dp, bottom = 24.dp, start = 16.dp, end = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    
                    // Row 1: Name + Gender Tag
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Text(
                            text = user.name.ifBlank { "User" },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF111827)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(genderColor)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("${user.gender} ${user.age}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }

                    // Row 2: Badges / Tags
                    if (hasAnyTag) {
                        Row(
                            modifier = Modifier.padding(top = 6.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (adminTag) GreenColorRemovalShader(imageSrc = "1788021461820~2.jpg".fullUrl(), modifier = Modifier.height(32.dp))
                            if (officialTag) GreenColorRemovalShader(imageSrc = "1788021468845~2.jpg".fullUrl(), modifier = Modifier.height(32.dp))
                            if (vipTag) AsyncImage(model = "1785469775751.png".fullUrl(), contentDescription = "VIP", modifier = Modifier.height(24.dp))
                            if (premiumTag) AsyncImage(model = "1785469365805.png".fullUrl(), contentDescription = "Premium", modifier = Modifier.height(24.dp))
                        }
                    }

                    // Row 3: Level Badge
                    Row(
                        modifier = Modifier.padding(top = 6.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            AsyncImage(model = "IMG_20260917_220530.png".fullUrl(), contentDescription = "Level", modifier = Modifier.height(32.dp))
                            Text("Lv.1", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 16.dp, top = 4.dp))
                        }
                        Spacer(modifier = Modifier.width(2.dp))
                        AsyncImage(model = "1785486414756.png".fullUrl(), contentDescription = null, modifier = Modifier.height(24.dp))
                    }

                    // Row 4: ID | Fans | Flag
                    Row(
                        modifier = Modifier.padding(top = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("ID:", color = Color.Gray, fontSize = 14.sp)
                        Text(accountId, color = Color.DarkGray, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 4.dp))
                        if (accountId != "N/A" && accountId != "User") {
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("ID", accountId))
                                    Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
                                    onCopyId()
                                },
                                modifier = Modifier.size(24.dp).padding(start = 4.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color.Gray, modifier = Modifier.size(14.dp))
                            }
                        }
                        
                        Text(" | ", color = Color.LightGray, modifier = Modifier.padding(horizontal = 4.dp))
                        
                        Text("${user.followers}", color = Color.DarkGray, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text(" Fans", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(start = 4.dp))
                        
                        Text(" | ", color = Color.LightGray, modifier = Modifier.padding(horizontal = 4.dp))
                        
                        Text(user.flag, fontSize = 16.sp)
                    }

                    // Action Buttons (Leave Seat OR Social Actions)
                    if (showLeaveSeat) {
                        Button(
                            onClick = onLeaveSeat,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                            shape = CircleShape
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Leave", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (showActions) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Follow
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onFollow() }) {
                                Icon(Icons.Default.Favorite, null, tint = Color(0xFFEC4899), modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (user.isFollowing) "Following" else "Follow", color = Color(0xFFEC4899), fontWeight = FontWeight.Medium)
                            }
                            
                            // Chat
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onMessage() }) {
                                Icon(Icons.Default.MailOutline, null, tint = Color.DarkGray, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Chat", color = Color.DarkGray, fontWeight = FontWeight.Medium)
                            }
                            
                            // 3rd Action Icon
                            AsyncImage(
                                model = "file_000000008e508208b1353ae33e2abef9.png".fullUrl(),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .clickable { onThirdAction() }
                            )
                        }

                        // Moderation Row (Mute, Leave, Lock, Kick)
                        if (showModerationRow) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(if (user.isMuted) "Unmute" else "Mute", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.clickable { onMute() })
                                Text("|", color = Color.LightGray)
                                Text("Leave", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.clickable { onLeaveSeat() })
                                Text("|", color = Color.LightGray)
                                Text(if (user.isLocked) "Unlock" else "Lock", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.clickable { onLock() })
                                Text("|", color = Color.LightGray)
                                Text("Kick out", color = Color.Red, fontSize = 14.sp, modifier = Modifier.clickable { onKickOut() })
                            }
                        }
                    }
                }

                // Overlapping Avatar (Placed outside the white column to break boundaries)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .size(88.dp) // Total 22 units equivalent (88dp)
                ) {
                    AsyncImage(
                        model = user.image.ifBlank { "default-avatar.png".fullUrl() },
                        contentDescription = user.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color.LightGray)
                    )
                    
                    // White Color Removal Shader Overlay Placeholder
                    WhiteColorRemovalShader(
                        imageSrc = "1786867564769.png".fullUrl(),
                        modifier = Modifier
                            .size(132.dp) // 150% of 88dp
                            .align(Alignment.Center)
                    )
                }

                // Corner Action Icons (Top Left & Top Right above white sheet)
                if (!isCurrentUser) {
                    IconButton(
                        onClick = { showReports = true },
                        modifier = Modifier.align(Alignment.TopStart).padding(top = 52.dp, start = 8.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = "Report", tint = Color.DarkGray)
                    }

                    IconButton(
                        onClick = { onMention(user.name) },
                        modifier = Modifier.align(Alignment.TopEnd).padding(top = 52.dp, end = 8.dp)
                    ) {
                        Icon(Icons.Default.Face, contentDescription = "Mention", tint = Color.DarkGray) // Placeholder for @ AtSign
                    }
                }
            }
        }
    }
    
    // User Report Screen Placeholder
    if (showReports) {
        // UserReportScreen(targetUser = user, onClose = { showReports = false })
    }
}

// Extension to simulate fullUrl resolving
private fun String.fullUrl(): String {
    val baseUrl = "https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/"
    return if (this.startsWith("http")) this else baseUrl + this.removePrefix("/")
}

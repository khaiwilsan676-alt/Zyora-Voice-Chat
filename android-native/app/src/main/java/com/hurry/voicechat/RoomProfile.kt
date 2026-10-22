package com.hurry.voicechat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage

data class RoomProfileUser(
    val id: String = "", val uid: String = "", val accountId: String = "",
    val name: String = "User", val image: String = "", val gender: String = "♂",
    val age: Int = 24, val country: String = "India", val flag: String = "🇮🇳",
    val followers: Int = 0, val bio: String = "", val isFollowing: Boolean = false,
    val isOnline: Boolean = true, val isInSeat: Boolean = false,
    val isMuted: Boolean = false, val isLocked: Boolean = false
)

private val ProfileBlue = Color(0xFF278CF5)
private val ProfilePink = Color(0xFFFA4D83)

@Composable
fun RoomProfile(
    user: RoomProfileUser,
    isCurrentUser: Boolean = false,
    isRoomOwner: Boolean = false,
    onClose: () -> Unit = {},
    onCopyId: () -> Unit = {},
    onLeaveSeat: () -> Unit = {},
    onMention: () -> Unit = {},
    onFollow: () -> Unit = {},
    onMessage: () -> Unit = {},
    onThirdAction: () -> Unit = {},
    onMute: () -> Unit = {},
    onLock: () -> Unit = {},
    onKickOut: () -> Unit = {},
    onReport: () -> Unit = {}
) {
    val config = LocalConfiguration.current
    val sheetHeight = when {
        !isCurrentUser && isRoomOwner -> 0.40f
        !isCurrentUser -> 0.32f
        user.isInSeat -> 0.34f
        else -> 0.21f
    }
    val clipboard = LocalClipboardManager.current
    val id = user.accountId.ifBlank { user.id.ifBlank { user.uid } }
    val genderColor = when (user.gender.lowercase()) {
        "female", "f", "♀" -> Color(0xFFEC4899)
        "male", "m", "♂" -> Color(0xFF3B82F6)
        else -> Color(0xFF6B7280)
    }
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().clickable(onClick = onClose))
        Box(Modifier.fillMaxWidth().height((config.screenHeightDp * sheetHeight).dp).align(Alignment.BottomCenter)) {
            Column(
                Modifier.fillMaxSize().clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(Color.White).padding(horizontal = 16.dp)
            ) {
                Spacer(Modifier.height(48.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    if (!isCurrentUser) {
                        IconButton(onClick = onReport, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.Default.Report, contentDescription = "Report user", tint = Color(0xFF4B5563), modifier = Modifier.size(22.dp))
                        }
                    } else Spacer(Modifier.size(34.dp))
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(user.name.ifBlank { "User" }, color = Color(0xFF16181D), fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(user.gender + " " + user.age, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium,
                                modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(genderColor).padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                        Spacer(Modifier.height(5.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Text("Lv.1", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(ProfileBlue).padding(horizontal = 7.dp, vertical = 3.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("ID: " + id, color = Color(0xFF656B75), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            IconButton(onClick = {
                                if (id.isNotBlank()) clipboard.setText(AnnotatedString(id))
                                onCopyId()
                            }, modifier = Modifier.size(25.dp)) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy ID", tint = Color(0xFF9CA3AF), modifier = Modifier.size(13.dp))
                            }
                        }
                        Spacer(Modifier.height(3.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Text(user.followers.toString() + " Fans", color = Color(0xFF777D86), fontSize = 12.sp)
                            Spacer(Modifier.width(9.dp))
                            Text(user.flag.ifBlank { "🇮🇳" } + " " + user.country.ifBlank { "India" }, color = Color(0xFF777D86), fontSize = 12.sp)
                            Spacer(Modifier.width(7.dp))
                            Box(Modifier.size(6.dp).clip(CircleShape).background(if (user.isOnline) Color(0xFF22C55E) else Color.LightGray))
                            Spacer(Modifier.width(4.dp))
                            Text(if (user.isOnline) "Online" else "Offline", color = Color(0xFF777D86), fontSize = 11.sp)
                        }
                    }
                    if (!isCurrentUser) {
                        IconButton(onClick = onMention, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.Default.AlternateEmail, contentDescription = "Mention user", tint = Color(0xFF4B5563), modifier = Modifier.size(22.dp))
                        }
                    } else Spacer(Modifier.size(34.dp))
                }
                if (user.bio.isNotBlank()) {
                    Text(user.bio, color = Color(0xFF777D86), fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth().padding(top = 5.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
                Spacer(Modifier.height(12.dp))
                if (isCurrentUser) {
                    if (user.isInSeat) {
                        Button(onClick = onLeaveSeat, modifier = Modifier.fillMaxWidth().height(44.dp),
                            shape = RoundedCornerShape(24.dp), colors = ButtonDefaults.buttonColors(containerColor = ProfileBlue)) {
                            Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp)); Text("Leave", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f).clickable(onClick = onFollow), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Favorite, contentDescription = null, tint = ProfilePink, modifier = Modifier.size(23.dp))
                            Spacer(Modifier.height(3.dp))
                            Text(if (user.isFollowing) "Following" else "Follow", color = ProfilePink, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                        Column(Modifier.weight(1f).clickable(onClick = onMessage), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Message, contentDescription = null, tint = Color(0xFF343A40), modifier = Modifier.size(23.dp))
                            Spacer(Modifier.height(3.dp))
                            Text("Chat", color = Color(0xFF343A40), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                        Column(Modifier.weight(1f).clickable(onClick = onThirdAction), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("♡", color = ProfilePink, fontSize = 26.sp, lineHeight = 26.sp)
                            Text("Gift", color = Color(0xFF343A40), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    if (isRoomOwner) {
                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider(color = Color(0xFFF0F0F0))
                        Row(Modifier.fillMaxWidth().padding(top = 7.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                            Text(if (user.isMuted) "Unmute" else "Mute", color = Color(0xFF59616B), fontSize = 12.sp, modifier = Modifier.clickable(onClick = onMute).padding(5.dp))
                            Text("Leave", color = Color(0xFF59616B), fontSize = 12.sp, modifier = Modifier.clickable(onClick = onLeaveSeat).padding(5.dp))
                            Text(if (user.isLocked) "Unlock" else "Lock", color = Color(0xFF59616B), fontSize = 12.sp, modifier = Modifier.clickable(onClick = onLock).padding(5.dp))
                            Text("Kick out", color = Color(0xFFDC2626), fontSize = 12.sp, modifier = Modifier.clickable(onClick = onKickOut).padding(5.dp))
                        }
                    }
                }
            }
            AsyncImage(
                model = user.image.ifBlank { "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/default-avatar.png" },
                contentDescription = user.name,
                modifier = Modifier.align(Alignment.TopCenter).offset(y = (-42).dp).size(84.dp).clip(CircleShape).background(Color(0xFFF3F4F6)),
                contentScale = ContentScale.Crop
            )
        }
    }
}

package com.hurry.voicechat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage

data class RoomInfoOwner(val id:String="",val uid:String="",val accountId:String="",val name:String,val image:String)
data class RoomInfoData(val roomName:String,val roomDp:String,val roomAnnouncement:String,val roomId:String)
data class RoomInfoFollower(val accountId:String,val name:String,val image:String,val country:String="")

@Composable
fun RoomInfo(
    isOpen:Boolean,
    onClose:()->Unit,
    isRoomOwner:Boolean,
    roomOwner:RoomInfoOwner,
    roomData:RoomInfoData,
    roomFollowers:List<RoomInfoFollower>,
    roomAdmins:List<RoomInfoFollower> = emptyList(),
    onOpenProfile:(RoomInfoFollower)->Unit,
    onCopyId:(String)->Unit,
    copied:Boolean=false
) {
    if (!isOpen) return
    val context = LocalContext.current
    var tab by remember { mutableStateOf(0) }
    
    // Naya state bottom menu aur share sheet ke liye
    var showActionMenu by remember { mutableStateOf(false) }
    var showShareSheet by remember { mutableStateOf(false) } // SHARE SHEET STATE
    var isFollowingUser by remember { mutableStateOf(false) }

    var ownerDisplayName by remember(roomOwner.id, roomOwner.uid, roomOwner.accountId) {
        mutableStateOf(if (roomOwner.accountId == "100002" || roomOwner.id == "100002") "Zyora" else "User")
    }
    val isLogoOwner = roomOwner.accountId == "100002" || roomOwner.id == "100002"
    var ownerDisplayImage by remember(roomOwner.id, roomOwner.uid, roomOwner.accountId, roomOwner.image) {
        mutableStateOf(if (isLogoOwner) "https://zyora-voice-chat.vercel.app/logo.png" else roomOwner.image)
    }
    var ownerCountry by remember(roomOwner.accountId) { mutableStateOf("") }
    var memberProfiles by remember { mutableStateOf>(emptyMap()) }

    LaunchedEffect(roomFollowers.map { it.accountId }, roomAdmins.map { it.accountId }) {
        val all = (roomFollowers + roomAdmins).distinctBy { it.accountId }.filter { it.accountId.isNotBlank() }
        val loaded = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            all.associate { member ->
                val roomProfileCountry = runCatching {
                    loadRoomProfileCountry(context, member.accountId, member.name, member.image)
                }.getOrDefault("")
                member.accountId to member.copy(country = roomProfileCountry)
            }
        }
        memberProfiles = loaded
    }

    LaunchedEffect(roomOwner.id, roomOwner.uid, roomOwner.accountId, roomOwner.name, roomOwner.image) {
        val ownerId = roomOwner.accountId.ifBlank { roomOwner.id.ifBlank { roomOwner.uid } }
        if (ownerId.isBlank()) return@LaunchedEffect
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val profile = runCatching {
                ProfilePersistence.load(TargetUser(
                    id = ownerId, uid = ownerId, accountId = ownerId,
                    name = roomOwner.name, image = roomOwner.image
                ))
            }.getOrNull()
            val country = runCatching {
                loadRoomProfileCountry(context, ownerId, roomOwner.name, roomOwner.image)
            }.getOrDefault("")
            val name = profile?.let { p ->
                sequenceOf("name", "displayName", "userName", "nickname")
                    .map { p.optString(it, "").trim() }
                    .firstOrNull { it.isNotBlank() && it != "null" && !it.equals("User", true) }
            }.orEmpty()
            val image = profile?.let { p ->
                sequenceOf("image", "avatar", "profileImageUrl")
                    .map { p.optString(it, "").trim() }
                    .firstOrNull { it.isNotBlank() && it != "null" }
            }.orEmpty()
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                if (!isLogoOwner) {
                    if (name.isNotBlank()) ownerDisplayName = name
                    if (image.isNotBlank()) ownerDisplayImage = image
                }
                ownerCountry = country
            }
        }
    }

    Box(Modifier.fillMaxSize(), contentAlignment=Alignment.BottomCenter) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.50f)).clickable(onClick=onClose))
        
        Box(Modifier.fillMaxWidth().fillMaxHeight(.65f)) {
            
            Column(
                Modifier.fillMaxSize()
                    .padding(top = 40.dp)
                    .clip(RoundedCornerShape(topStart=24.dp, topEnd=24.dp))
                    .background(Color(0xFF1E1E1E))
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier.size(36.dp).background(Color(0xFF2A2A2A), CircleShape).clickable { 
                                showActionMenu = true
                            },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.MoreHoriz, contentDescription="More", tint=Color.White)
                        }
                        Box(
                            // YAHAN SHARE BUTTON PE CLICK LAGAYA HAI
                            modifier = Modifier.size(36.dp).background(Color(0xFF2A2A2A), CircleShape).clickable {
                                showShareSheet = true 
                            },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Share, contentDescription="Share", tint=Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                    
                    Row(
                        modifier = Modifier.background(Color(0xFF2A2A2A), RoundedCornerShape(18.dp)).padding(horizontal = 14.dp, vertical = 8.dp).clickable { },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.FavoriteBorder, contentDescription="Follow", tint=Color.White, modifier=Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Follow", color=Color.White, fontSize=14.sp, fontWeight=FontWeight.Medium)
                    }
                }

                Column(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        roomData.roomName.ifBlank{"Room"}, 
                        fontSize=18.sp, 
                        fontWeight=FontWeight.Bold, 
                        color=Color.White
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        Text("ID:${roomData.roomId}", fontSize=13.sp, color=Color.White.copy(alpha=.70f))
                        Spacer(Modifier.width(4.dp))
                        IconButton(onClick={onCopyId(roomData.roomId)}, modifier=Modifier.size(24.dp)) {
                            Icon(Icons.Default.ContentCopy, contentDescription="Copy", tint=Color.White.copy(alpha=.70f), modifier=Modifier.size(14.dp))
                        }
                    }
                    if(copied) Text("Copied!", fontSize=12.sp, color=Color(0xFF4ADE80))
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    Modifier.fillMaxWidth().padding(horizontal=16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("Profile", "Member").forEachIndexed { index, label ->
                        Column(
                            Modifier.weight(1f).clickable { tab=index }.padding(vertical=8.dp),
                            horizontalAlignment=Alignment.CenterHorizontally
                        ) {
                            Text(
                                label,
                                fontSize=16.sp,
                                fontWeight=if(tab==index) FontWeight.Bold else FontWeight.Medium,
                                color=if(tab==index) Color.White else Color.Gray
                            )
                            if (tab==index) {
                                Spacer(Modifier.height(4.dp))
                                Box(Modifier.width(20.dp).height(3.dp).background(Color.White, RoundedCornerShape(1.5.dp)))
                            } else {
                                Spacer(Modifier.height(7.dp))
                            }
                        }
                    }
                }
                
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha=0.05f)))

                if (tab==0) {
                    Column(
                        Modifier.fillMaxWidth().weight(1f).padding(horizontal=16.dp, vertical=20.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth().background(Color(0xFF2A2A2A), RoundedCornerShape(16.dp)).padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = if (isLogoOwner) "https://zyora-voice-chat.vercel.app/logo.png" else ownerDisplayImage,
                                contentDescription = "Owner",
                                modifier = Modifier.size(48.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(ownerDisplayName.ifBlank{"Unknown"}, fontSize=16.sp, fontWeight=FontWeight.SemiBold, color=Color.White, maxLines=1)
                                Spacer(Modifier.height(2.dp))
                                Text("Room owner", fontSize=12.sp, color=Color.Gray)
                            }
                            
                            AsyncImage(
                                model = "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/IMG_20261105_125330.png",
                                contentDescription = "Chat",
                                modifier = Modifier
                                    .size(40.dp)
                                    .clickable {
                                        onOpenProfile(RoomInfoFollower(roomOwner.accountId, ownerDisplayName, ownerDisplayImage, ownerCountry))
                                    },
                                contentScale = ContentScale.Fit
                            )
                        }

                        Spacer(Modifier.height(24.dp))

                        Text("Announcement", fontSize=13.sp, color=Color.Gray, fontWeight=FontWeight.Medium)
                        Spacer(Modifier.height(8.dp))
                        Text(roomData.roomAnnouncement.ifBlank{"No Announcement"}, fontSize=14.sp, color=Color.White)
                    }
                } else {
                    LazyColumn(
                        Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        item {
                            MemberRow(ownerDisplayName, if (isLogoOwner) "https://zyora-voice-chat.vercel.app/logo.png" else ownerDisplayImage, roomOwner.accountId, "Owner", ownerCountry) {
                                onOpenProfile(RoomInfoFollower(roomOwner.accountId, ownerDisplayName, ownerDisplayImage, ownerCountry))
                            }
                        }
                        items(roomFollowers.filter { it.accountId.isNotBlank() && it.accountId != roomOwner.accountId }, key = { "member-${it.accountId}" }) { f ->
                            val profile = memberProfiles[f.accountId] ?: f.copy(country = "")
                            MemberRow(profile.name, profile.image, profile.accountId, "", profile.country) { onOpenProfile(profile) }
                        }
                        if (roomFollowers.isEmpty() && roomAdmins.isEmpty()) {
                            item {
                                Text("No members yet", modifier = Modifier.fillMaxWidth().padding(20.dp),
                                    textAlign = TextAlign.Center, fontSize = 13.sp, color = Color(0xFF9CA3AF))
                            }
                        }
                    }
                }
            }
            
            AsyncImage(
                model = roomData.roomDp.ifBlank { "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/default-avatar.png" },
                contentDescription = "Room Avatar",
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .size(80.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .border(3.dp, Color(0xFF1E1E1E), RoundedCornerShape(18.dp)),
                contentScale = ContentScale.Crop
            )
        }
        
        // --- 3 DOTS (ACTION MENU) ---
        if (showActionMenu) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable { showActionMenu = false },
                contentAlignment = Alignment.BottomCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF2A2B2E)) 
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    isFollowingUser = !isFollowingUser
                                    showActionMenu = false 
                                }
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isFollowingUser) "Unfollow" else "Follow",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        Divider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showActionMenu = false }
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Report", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }
                        
                        Divider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showActionMenu = false }
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Block", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // YAHAN CANCEL BUTTON KO BLUE KIYA HAI
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF2196F3)) // BLUE COLOR BSS
                            .clickable { showActionMenu = false }
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Cancel",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // --- SHARE BOTTOM SHEET 40VH BLACK BSS ---
        if (showShareSheet) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable { showShareSheet = false },
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.40f) // 40Vh
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .background(Color.Black) // Black Background
                        .clickable(enabled = false) {} // Taki sheet click hone pe khud se close na ho
                ) {
                    Text(
                        text = "Share in zyora", // Top left heading
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(top = 20.dp, start = 20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MemberRow(
    name: String,
    image: String,
    id: String,
    role: String,
    country: String,
    onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 2.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = if (image.isBlank()) "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/default-avatar.png" else image,
            contentDescription = name,
            modifier = Modifier.size(38.dp).clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Column(Modifier.padding(start = 11.dp).weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name.ifBlank { "User" },
                    fontSize = 13.sp, fontWeight = FontWeight.Medium,
                    color = Color.White, maxLines = 2, softWrap = true)
                if (role == "Owner") {
                    AsyncImage(model="https://zyora-voice-chat.vercel.app/IMG_20261027_235537.png",
                        contentDescription="Owner", contentScale=ContentScale.Fit,
                        modifier=Modifier.padding(start=5.dp).height(14.dp).widthIn(min=30.dp, max=42.dp))
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Box(Modifier.size(width = 18.dp, height = 16.dp), contentAlignment = Alignment.Center) {
                    if (country.isNotBlank()) Text(countryFlagEmoji(country), fontSize = 13.sp)
                }
                Box(Modifier.height(16.dp).width(36.dp), contentAlignment = Alignment.Center) {
                    AsyncImage(model="https://zyora-voice-chat.vercel.app/IMG_20261028_004355.png",
                        contentDescription="Level", contentScale=ContentScale.Fit, modifier=Modifier.fillMaxSize())
                    Text("Lv.1", color=Color.White, fontSize=8.sp, fontWeight=FontWeight.Bold)
                }
            }
        }
    }
}

private fun countryFlagEmoji(country: String): String {
    val raw = country.trim()
    if (raw.isNotEmpty() && raw.codePoints().toArray().all { it in 0x1F1E6..0x1F1FF }) return raw
    val code = when (raw.lowercase()) {
        "india", "indian" -> "IN"
        "united states", "usa", "us" -> "US"
        "united kingdom", "uk", "britain" -> "GB"
        "pakistan" -> "PK"
        "bangladesh" -> "BD"
        "nepal" -> "NP"
        "sri lanka" -> "LK"
        "uae", "united arab emirates" -> "AE"
        "saudi arabia" -> "SA"
        "canada" -> "CA"
        "australia" -> "AU"
        else -> country.trim().uppercase().takeIf { it.length == 2 } ?: return ""
    }
    return code.map { String(Character.toChars(0x1F1E6 + it.code - 65)) }.joinToString("")
}

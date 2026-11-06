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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.Canvas
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
    copied:Boolean=false,
    currentUserId:String="",
    isRoomFollowed:Boolean=false,
    onToggleRoomFollow:(Boolean)->Unit={},
    onShareToUser:(RoomInfoFollower)->Unit={}
) {
    if (!isOpen) return
    val context = LocalContext.current
    var tab by remember { mutableStateOf(0) }
    
    // Naya state share sheet handle karne ke liye
    var showShareSheet by remember { mutableStateOf(false) }
    var showDangerMenu by remember { mutableStateOf(false) }
    var showRoomReport by remember { mutableStateOf(false) }
    var roomFans by remember(currentUserId, roomOwner.accountId, roomOwner.id) { mutableStateOf<List<RoomInfoFollower>>(emptyList()) }
    LaunchedEffect(currentUserId, roomOwner.accountId, roomOwner.id, showShareSheet) {
        if (showShareSheet) {
            val ownerId = currentUserId.ifBlank { roomOwner.accountId.ifBlank { roomOwner.id.ifBlank { roomOwner.uid } } }
            roomFans = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                runCatching {
                    SocialFollowApi.list(context, ownerId, "fans").map {
                        RoomInfoFollower(it.accountId.ifBlank { it.uid.ifBlank { it.id } }, it.name, it.image, it.country)
                    }.filter { it.accountId.isNotBlank() && it.accountId != currentUserId }.distinctBy { it.accountId }
                }.getOrDefault(emptyList())
            }
        }
    }
    
    var ownerDisplayName by remember(roomOwner.id, roomOwner.uid, roomOwner.accountId) {
        mutableStateOf(if (roomOwner.accountId == "100002" || roomOwner.id == "100002") "Zyora" else "User")
    }
    val isLogoOwner = roomOwner.accountId == "100002" || roomOwner.id == "100002"
    var ownerDisplayImage by remember(roomOwner.id, roomOwner.uid, roomOwner.accountId, roomOwner.image) {
        mutableStateOf(if (isLogoOwner) "https://zyora-voice-chat.vercel.app/logo.png" else roomOwner.image)
    }
    var ownerCountry by remember(roomOwner.accountId) { mutableStateOf("") }
    var memberProfiles by remember { mutableStateOf<Map<String, RoomInfoFollower>>(emptyMap()) }

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
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.30f)).clickable(onClick=onClose))
        Column(
            Modifier.fillMaxWidth().fillMaxHeight(.60f)
                .clip(RoundedCornerShape(topStart=15.dp,topEnd=15.dp))
                .background(Color(0xEB000000))
        ) {
            
            // Top Box Layout - 3 Dot (Left), Title (Center), Share (Right)
            Box(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 4.dp)
            ) {
                // 3 Dots Icon (Ekdam Left)
                if (!isRoomOwner) Box(
                    modifier = Modifier.align(Alignment.CenterStart).size(34.dp).clip(CircleShape).background(Color.White.copy(alpha=0.15f)).clickable { showDangerMenu = true },
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(Modifier.size(16.dp)) {
                        drawCircle(Color.White, radius = 1.8.dp.toPx(), center = androidx.compose.ui.geometry.Offset(size.width * 0.15f, size.height / 2))
                        drawCircle(Color.White, radius = 1.8.dp.toPx(), center = androidx.compose.ui.geometry.Offset(size.width * 0.5f, size.height / 2))
                        drawCircle(Color.White, radius = 1.8.dp.toPx(), center = androidx.compose.ui.geometry.Offset(size.width * 0.85f, size.height / 2))
                    }
                }

                // Heading Top Middle (Center)
                Text(
                    text = "Room Information",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )

                // Share/Forward Arrow Icon (Ekdam Right)
                Box(
                    modifier = Modifier.align(Alignment.CenterEnd).size(34.dp).clip(CircleShape).background(Color.White.copy(alpha=0.15f))
                        .clickable { showShareSheet = true }, // Yahan click lagaya hai
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(Modifier.size(16.dp)) {
                        val path = Path().apply {
                            moveTo(size.width * 0.15f, size.height * 0.85f)
                            quadraticTo(size.width * 0.15f, size.height * 0.35f, size.width * 0.5f, size.height * 0.35f)
                            lineTo(size.width * 0.5f, size.height * 0.15f)
                            lineTo(size.width * 0.95f, size.height * 0.5f)
                            lineTo(size.width * 0.5f, size.height * 0.85f)
                            lineTo(size.width * 0.5f, size.height * 0.65f)
                            quadraticTo(size.width * 0.3f, size.height * 0.65f, size.width * 0.15f, size.height * 0.85f)
                        }
                        drawPath(path, Color.White, style = Stroke(width = 1.8.dp.toPx(), join = StrokeJoin.Round, cap = StrokeCap.Round))
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=8.dp),
                horizontalArrangement=Arrangement.spacedBy(7.dp)
            ) {
                listOf("Room info","Members").forEachIndexed { index, label ->
                    Column(
                        Modifier.weight(1f).clickable { tab=index }.padding(vertical=4.dp),
                        horizontalAlignment=Alignment.CenterHorizontally
                    ) {
                        Text(label,
                            fontSize=14.sp, fontWeight=FontWeight.SemiBold,
                            color=if(tab==index) Color.White else Color.White.copy(alpha=.50f))
                        if (tab==index) {
                            Box(
                                Modifier.padding(top=4.dp).size(width=20.dp, height=2.dp)
                                    .background(Color.White, RoundedCornerShape(1.dp))
                            )
                        }
                    }
                }
            }

            if (tab==0) {
                Column(
                    Modifier.fillMaxWidth().weight(1f).padding(horizontal=16.dp,vertical=12.dp),
                    verticalArrangement=Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        AsyncImage(
                            model=roomData.roomDp.ifBlank { "/default-avatar.png" },
                            contentDescription="Room",
                            modifier=Modifier.size(80.dp).clip(RoundedCornerShape(6.dp)).border(1.dp,Color.White.copy(alpha=.15f),RoundedCornerShape(6.dp)),
                            contentScale=ContentScale.Crop
                        )
                        Column(Modifier.padding(start=8.dp)) {
                            Text(roomData.roomName.ifBlank{"Room"},fontSize=14.sp,
                                fontWeight=FontWeight.SemiBold,color=Color.White)
                            Row(verticalAlignment=Alignment.CenterVertically) {
                                if (roomData.roomId in setOf("100002","100003","500002","500003","500004")) {
                                    AsyncImage(model="https://zyora-voice-chat.vercel.app/IMG_20261101_125212.png", contentDescription="Purple ID Badge", modifier=Modifier.height(16.dp).widthIn(min=22.dp,max=30.dp), contentScale=ContentScale.Fit)
                                    Text(roomData.roomId,fontSize=10.sp,color=Color.White.copy(alpha=.60f))
                                } else Text("ID: "+roomData.roomId,fontSize=10.sp,color=Color.White.copy(alpha=.60f))
                                IconButton(onClick={onCopyId(roomData.roomId)},modifier=Modifier.size(26.dp)) {
                                    Icon(Icons.Default.ContentCopy,contentDescription="Copy room ID",
                                        tint=Color.White.copy(alpha=.70f),modifier=Modifier.size(15.dp))
                                }
                                if(copied) Text("Copied!",fontSize=12.sp,color=Color(0xFF4ADE80))
                            }
                        }
                    }
                    
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            onOpenProfile(RoomInfoFollower(roomOwner.accountId, ownerDisplayName, ownerDisplayImage, ownerCountry))
                        }.padding(vertical=4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(
                                model = if (isLogoOwner) "https://zyora-voice-chat.vercel.app/logo.png" else ownerDisplayImage.ifBlank { "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/default-avatar.png" },
                                contentDescription = "Host Avatar",
                                modifier = Modifier.size(38.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Column(Modifier.padding(start = 10.dp)) {
                                Text("Host",fontSize=10.sp,fontWeight=FontWeight.Medium,color=Color.White.copy(alpha=.50f))
                                Text(ownerDisplayName.ifBlank{"Unknown"},
                                    fontSize=12.sp,fontWeight=FontWeight.Medium,color=Color.White,
                                    maxLines=2, softWrap=true)
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = "Go to Profile",
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text("Announcement:",fontSize=12.sp,fontWeight=FontWeight.Medium,color=Color(0xFF9CA3AF))
                        Text(roomData.roomAnnouncement.ifBlank{"—"},Modifier.padding(top=2.dp),
                            fontSize=12.sp,color=Color.White.copy(alpha=.90f))
                    }
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

        // --- NEW SHARE BOTTOM SHEET LOGIC ---
        if (showShareSheet) {
            // Dark overlay background for share sheet to dismiss it when clicked outside
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=0.4f)).clickable { showShareSheet = false })
            
            // Share Bottom Sheet Container (50Vh, Black, 12dp curve)
            Column(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.50f)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .background(Color.Black)
            ) {
                // Top Left Title
                Text(
                    text = "Share in Zyora",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp)
                )
                
                LazyColumn(
                    Modifier.fillMaxWidth().weight(1f).padding(horizontal = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (roomFans.isEmpty()) {
                        item {
                            Text("No fans yet", Modifier.fillMaxWidth().padding(vertical = 24.dp),
                                color = Color(0xFF9CA3AF), fontSize = 13.sp, textAlign = TextAlign.Center)
                        }
                    }
                    items(roomFans, key = { "room-share-" + it.accountId }) { fan ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(
                                model = fan.image.ifBlank { "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/default-avatar.png" },
                                contentDescription = fan.name,
                                modifier = Modifier.size(42.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Text(fan.name.ifBlank { "User" }, Modifier.weight(1f).padding(start = 10.dp),
                                color = Color.White, fontSize = 14.sp, maxLines = 1)
                            Box(
                                Modifier.clip(RoundedCornerShape(18.dp)).background(Color(0xFF2878E8))
                                    .clickable {
                                        onShareToUser(fan)
                                        showShareSheet = false
                                    }.padding(horizontal = 17.dp, vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Share", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
        // ------------------------------------
        if (showDangerMenu) {
            RoomDangerActionMenu(
                isFollowing = isRoomFollowed,
                onDismiss = { showDangerMenu = false },
                onFollow = {
                    onToggleRoomFollow(!isRoomFollowed)
                    showDangerMenu = false
                },
                onReport = {
                    showDangerMenu = false
                    showRoomReport = true
                }
            )
        }
        if (showRoomReport) {
            UserReport(
                targetName = roomData.roomName,
                targetImage = roomData.roomDp,
                targetId = roomData.roomId,
                onDismiss = { showRoomReport = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoomDangerActionMenu(
    isFollowing: Boolean,
    onDismiss: () -> Unit,
    onFollow: () -> Unit,
    onReport: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.Transparent,
        contentColor = Color.White,
        scrimColor = Color.Black.copy(alpha = 0.5f),
        dragHandle = null
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 32.dp)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFF1C1C1E))) {
                Text("Report", Modifier.fillMaxWidth().clickable(onClick = onReport).padding(vertical = 18.dp),
                    color = Color.White, fontSize = 18.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Medium)
                Divider(color = Color.DarkGray.copy(alpha = 0.5f), thickness = 0.5.dp)
                Text(if (isFollowing) "Followed" else "Follow",
                    Modifier.fillMaxWidth().clickable(onClick = onFollow).padding(vertical = 18.dp),
                    color = Color.White, fontSize = 18.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = onDismiss, Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                shape = RoundedCornerShape(16.dp)) {
                Text("Cancel", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
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
                    if (country.isNotBlank()) Text(roomProfileCountryFlag(country), fontSize = 13.sp)
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

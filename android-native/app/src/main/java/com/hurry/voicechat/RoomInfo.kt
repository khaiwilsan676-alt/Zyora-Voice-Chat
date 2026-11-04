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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
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
    copied:Boolean=false
) {
    if (!isOpen) return
    val context = LocalContext.current
    var tab by remember { mutableStateOf(0) }
    // roomOwner.name may be the ROOM title from discovery/kept-room data.
    // Never use it as the host profile name; resolve the host by stable account ID.
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
                    // Read the same Room Profile country field used by the Room Profile sheet.
                    loadRoomProfileCountry(context, member.accountId, member.name, member.image)
                }.getOrDefault("")
                member.accountId to member.copy(country = roomProfileCountry)
            }
        }
        memberProfiles = loaded
    }

    // Resolve host display details by stable ID; country always comes from Room Profile.
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
            Modifier.fillMaxWidth().fillMaxHeight(.50f)
                .clip(RoundedCornerShape(topStart=15.dp,topEnd=15.dp))
                .background(Color(0xEB000000))
        ) {
            Row(
                Modifier.fillMaxWidth().padding(start=16.dp,end=16.dp,top=16.dp,bottom=8.dp),
                horizontalArrangement=Arrangement.spacedBy(12.dp)
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
                            Canvas(Modifier.padding(top=2.dp).size(width=20.dp,height=6.dp)) {
                                val path = Path().apply {
                                    moveTo(size.width * .05f, size.height * .17f)
                                    quadraticTo(size.width * .5f, size.height * 1.15f, size.width * .95f, size.height * .17f)
                                }
                                drawPath(path,Color.White,style=Stroke(width=1.5.dp.toPx()))
                            }
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
                    Column {
                        Text("Host",fontSize=10.sp,fontWeight=FontWeight.Medium,color=Color.White.copy(alpha=.50f))
                        Text(ownerDisplayName.ifBlank{"Unknown"},Modifier.fillMaxWidth().padding(top=4.dp),
                            fontSize=12.sp,fontWeight=FontWeight.Medium,color=Color.White,
                            maxLines=2, softWrap=true)
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
                        // Owner is a simple member row, not a full-width card.
                        MemberRow(ownerDisplayName, if (isLogoOwner) "https://zyora-voice-chat.vercel.app/logo.png" else ownerDisplayImage, roomOwner.accountId, "Owner", ownerCountry) {
                            onOpenProfile(RoomInfoFollower(roomOwner.accountId, ownerDisplayName, ownerDisplayImage, ownerCountry))
                        }
                    }
                    // Only room followers appear in the Members list.
                    items(roomFollowers.filter { it.accountId.isNotBlank() && it.accountId != roomOwner.accountId }, key = { "member-${it.accountId}" }) { f ->
                        val profile = memberProfiles[f.accountId] ?: f
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

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
import coil3.compose.AsyncImage

data class RoomInfoOwner(val id:String="",val uid:String="",val accountId:String="",val name:String,val image:String)
data class RoomInfoData(val roomName:String,val roomDp:String,val roomAnnouncement:String,val roomId:String)
data class RoomInfoFollower(val accountId:String,val name:String,val image:String)

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
    var tab by remember { mutableStateOf(0) }
    var ownerDisplayName by remember(roomOwner.id, roomOwner.uid, roomOwner.accountId, roomOwner.name) {
        mutableStateOf(roomOwner.name.ifBlank { "User" })
    }
    var ownerDisplayImage by remember(roomOwner.id, roomOwner.uid, roomOwner.accountId, roomOwner.image) {
        mutableStateOf(roomOwner.image)
    }

    LaunchedEffect(roomOwner.id, roomOwner.uid, roomOwner.accountId) {
        val profile = ProfilePersistence.load(
            TargetUser(
                id = roomOwner.id.ifBlank { null },
                uid = roomOwner.uid.ifBlank { null },
                accountId = roomOwner.accountId.ifBlank { null },
                name = roomOwner.name,
                photo = roomOwner.image,
                image = roomOwner.image
            )
        ) ?: return@LaunchedEffect
        profile.optString("name").takeIf { it.isNotBlank() && it != "null" }?.let {
            ownerDisplayName = it
        }
        sequenceOf("image", "photo", "photoURL", "avatar", "picture")
            .map { profile.optString(it, "") }
            .firstOrNull { it.isNotBlank() && it != "null" }
            ?.let { ownerDisplayImage = it }
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
                                Text("ID: "+roomData.roomId,fontSize=10.sp,color=Color.White.copy(alpha=.60f))
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
                        Text(roomOwner.name.ifBlank{"Unknown"},Modifier.padding(top=4.dp),
                            fontSize=12.sp,fontWeight=FontWeight.Medium,color=Color.White)
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
                        MemberRow(roomOwner.name, roomOwner.image, roomOwner.accountId, "Owner", Color(0xFF278CF5)) {
                            onOpenProfile(RoomInfoFollower(roomOwner.accountId, roomOwner.name, roomOwner.image))
                        }
                    }
                    items(roomAdmins.filter { admin ->
                        admin.accountId.isNotBlank() &&
                            admin.accountId != roomOwner.accountId &&
                            roomFollowers.none { it.accountId == admin.accountId }
                    }, key = { "admin-${it.accountId}" }) { admin ->
                        MemberRow(admin.name, admin.image, admin.accountId, "House", Color(0xFF22A447)) {
                            onOpenProfile(admin)
                        }
                    }
                    items(roomFollowers.filter { it.accountId != roomOwner.accountId && roomAdmins.none { a -> a.accountId == it.accountId } }, key = { "member-${it.accountId}" }) { f ->
                        MemberRow(f.name, f.image, f.accountId, "", Color.Transparent) { onOpenProfile(f) }
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
    roleColor: Color,
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
        Text(name.ifBlank { "User" }, Modifier.weight(1f).padding(start = 11.dp),
            fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.White, maxLines = 1)
        if (role == "Owner") {
            Box(
                Modifier.padding(start = 8.dp).size(24.dp).clip(CircleShape).background(Color(0xFF3B82F6)),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Icon(
                    androidx.compose.material.icons.Icons.Default.Home,
                    contentDescription = "Owner", tint = Color.White, modifier = Modifier.size(14.dp)
                )
            }
        }
        if (role.isNotBlank() && role != "Owner") {
            Row(
                Modifier.clip(RoundedCornerShape(5.dp)).background(roleColor).padding(horizontal = 7.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                if (role == "House") androidx.compose.material3.Icon(
                    androidx.compose.material.icons.Icons.Default.Home,
                    contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp)
                )
                Text(role, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }
    }
}

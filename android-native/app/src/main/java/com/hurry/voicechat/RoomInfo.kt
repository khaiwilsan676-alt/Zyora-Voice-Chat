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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

data class RoomInfoOwner(val id:String="",val uid:String="",val accountId:String="",val name:String,val image:String,val vipId:String="",val vipIdBadge:Boolean=false)
data class RoomInfoData(val roomName:String,val roomDp:String,val roomAnnouncement:String,val roomId:String)
data class RoomInfoFollower(val accountId:String,val name:String,val image:String,val country:String="",val gender:String="")

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
    onShareToUser:(RoomInfoFollower)->Unit={},
    onOpenOwnerChat:(RoomInfoFollower)->Unit={}
) {
    if (!isOpen) return
    val context = LocalContext.current
    var tab by remember { mutableStateOf(0) }
    
    var showShareSheet by remember { mutableStateOf(false) }
    var showDangerMenu by remember { mutableStateOf(false) }
    var showRoomReport by remember { mutableStateOf(false) }
    var roomFans by remember(currentUserId, roomOwner.accountId, roomOwner.id) { mutableStateOf<List<RoomInfoFollower>>(emptyList()) }
    
    LaunchedEffect(currentUserId, showShareSheet) {
        if (showShareSheet && currentUserId.isNotBlank()) {
            roomFans = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                // Show the union of the signed-in user's Fans and Following lists.
                // SocialFollowApi persists each list locally, so cached rows remain
                // available if the network is unavailable or the app is reopened.
                val fans = SocialFollowApi.list(context, currentUserId, "fans")
                val following = SocialFollowApi.list(context, currentUserId, "following")
                (fans + following).map {
                    RoomInfoFollower(
                        it.accountId.ifBlank { it.uid.ifBlank { it.id } },
                        it.name, it.image, it.country
                    )
                }.filter { it.accountId.isNotBlank() && it.accountId != currentUserId }
                    .distinctBy { it.accountId }
            }
        }
    }
    
    var ownerVipId by remember(roomOwner.id, roomOwner.uid, roomOwner.accountId, roomOwner.vipId) { mutableStateOf(roomOwner.vipId.trim()) }
    var ownerVipBadge by remember(roomOwner.id, roomOwner.uid, roomOwner.accountId, roomOwner.vipIdBadge) { mutableStateOf(roomOwner.vipIdBadge) }
    LaunchedEffect(isOpen, roomOwner.id, roomOwner.uid, roomOwner.accountId) {
        if (!isOpen) return@LaunchedEffect
        val ownerId = roomOwner.accountId.ifBlank { roomOwner.id.ifBlank { roomOwner.uid } }
        if (ownerId.isBlank()) return@LaunchedEffect
        val profile = withContext(kotlinx.coroutines.Dispatchers.IO) {
            val remote = runCatching {
                ProfilePersistence.load(TargetUser(
                    id = roomOwner.id.ifBlank { ownerId },
                    uid = roomOwner.uid.ifBlank { ownerId },
                    accountId = roomOwner.accountId.ifBlank { ownerId }
                ))
            }.getOrNull()
            val db = NativeIndexDatabase(context)
            val cached = sequenceOf(roomOwner.uid, roomOwner.id, roomOwner.accountId, ownerId)
                .filter { it.isNotBlank() }.mapNotNull { key -> runCatching { db.loadProfile(key) }.getOrNull() }
                .firstOrNull { it.has("vipId") }
            remote to cached
        }
        val remote = profile.first
        val cached = profile.second
        val candidate = sequenceOf(remote?.optString("vipId", "").orEmpty(), cached?.optString("vipId", "").orEmpty())
            .map { it.trim() }.firstOrNull { it.matches(Regex("\\d{1,20}")) }.orEmpty()
        val expiry = remote?.optLong("vipIdExpiresAt", cached?.optLong("vipIdExpiresAt", 0L) ?: 0L)
            ?: cached?.optLong("vipIdExpiresAt", 0L) ?: 0L
        val badge = (remote?.optBoolean("vipIdBadge", false) == true) ||
            remote?.optString("vipIdBadge", "").equals("true", true) ||
            (cached?.optBoolean("vipIdBadge", false) == true) ||
            cached?.optString("vipIdBadge", "").equals("true", true)
        val active = candidate.isNotBlank() && (expiry == 0L || expiry > System.currentTimeMillis())
        ownerVipId = if (active) candidate else ""
        ownerVipBadge = active && (badge || candidate.isNotBlank())
    }
    var ownerDisplayName by remember(roomOwner.id, roomOwner.uid, roomOwner.accountId) {
        mutableStateOf(if (roomOwner.accountId == "100002" || roomOwner.id == "100002") "Zyora" else roomOwner.name.trim().takeIf { it.isNotBlank() && !it.equals("User", true) } ?: "User")
    }
    val isLogoOwner = roomOwner.accountId == "100002" || roomOwner.id == "100002"
    var ownerDisplayImage by remember(roomOwner.id, roomOwner.uid, roomOwner.accountId, roomOwner.image) {
        mutableStateOf(if (isLogoOwner) "https://zyora-voice-chat.vercel.app/logo.png" else roomOwner.image)
    }
    var ownerCountry by remember(roomOwner.accountId) { mutableStateOf("") }
    var ownerGender by remember(roomOwner.accountId) { mutableStateOf("") }
    var memberProfiles by remember { mutableStateOf<Map<String, RoomInfoFollower>>(emptyMap()) }

    LaunchedEffect(roomFollowers.map { it.accountId }, roomAdmins.map { it.accountId }) {
        val all = (roomFollowers + roomAdmins).distinctBy { it.accountId }.filter { it.accountId.isNotBlank() }
        if (all.isEmpty()) {
            memberProfiles = emptyMap()
            return@LaunchedEffect
        }

        // Render room/socket data immediately, merged with the persistent profile
        // cache. Never hold the Members list behind network profile requests.
        val cached = withContext(kotlinx.coroutines.Dispatchers.IO) {
            val db = NativeIndexDatabase(context)
            all.mapNotNull { member ->
                runCatching {
                    db.loadProfile("room_info_member:${member.accountId}")?.let { saved ->
                        member.accountId to member.copy(
                            name = member.name.ifBlank { saved.optString("name") },
                            image = member.image.ifBlank { saved.optString("image") },
                            country = member.country.ifBlank { saved.optString("country") },
                            gender = member.gender.ifBlank { saved.optString("gender") }
                        )
                    }
                }.getOrNull()
            }.toMap()
        }
        val initial = all.associate { member ->
            val saved = cached[member.accountId]
            member.accountId to member.copy(
                name = member.name.ifBlank { saved?.name.orEmpty() },
                image = member.image.ifBlank { saved?.image.orEmpty() },
                country = member.country.ifBlank { saved?.country.orEmpty() },
                gender = member.gender.ifBlank { saved?.gender.orEmpty() }
            )
        }
        memberProfiles = initial

        // Refresh in the background and publish each completed member separately;
        // one slow profile/server response can no longer delay the remaining rows.
        val limiter = kotlinx.coroutines.sync.Semaphore(6)
        kotlinx.coroutines.coroutineScope {
            all.forEach { member ->
                launch(kotlinx.coroutines.Dispatchers.IO) {
                    limiter.withPermit {
                        val base = initial[member.accountId] ?: member
                        val remoteProfile = runCatching {
                            ProfilePersistence.load(TargetUser(
                                id = member.accountId, uid = member.accountId, accountId = member.accountId,
                                name = base.name, image = base.image, gender = base.gender, country = base.country
                            ))
                        }.getOrNull()
                        val roomProfileCountry = if (base.country.isBlank()) {
                            runCatching {
                                loadRoomProfileCountry(context, member.accountId, base.name, base.image, base.country)
                            }.getOrNull()?.takeIf { it.isNotBlank() }.orEmpty()
                        } else base.country
                        val roomProfileGender = sequenceOf(
                            remoteProfile?.optString("gender", "").orEmpty(),
                            base.gender
                        ).map { it.trim() }.firstOrNull {
                            it.isNotBlank() && it != "null" && !it.equals("undefined", true)
                        }.orEmpty()
                        val resolved = base.copy(
                            country = roomProfileCountry.ifBlank { base.country },
                            gender = roomProfileGender
                        )
                        runCatching {
                            val db = NativeIndexDatabase(context)
                            val saved = db.loadProfile("room_info_member:${member.accountId}") ?: org.json.JSONObject()
                            db.saveProfile(
                                "room_info_member:${member.accountId}",
                                saved.put("name", resolved.name.ifBlank { saved.optString("name") })
                                    .put("image", resolved.image.ifBlank { saved.optString("image") })
                                    .put("country", resolved.country.ifBlank { saved.optString("country") })
                                    .put("gender", resolved.gender.ifBlank { saved.optString("gender") })
                                    .put("updatedAt", System.currentTimeMillis())
                            )
                        }
                        withContext(kotlinx.coroutines.Dispatchers.Main) {
                            memberProfiles = memberProfiles + (member.accountId to resolved)
                        }
                    }
                }
            }
        }
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
            val gender = sequenceOf(
                profile?.optString("gender", "").orEmpty(),
                runCatching { NativeIndexDatabase(context).loadProfile(ownerId)?.optString("gender", "").orEmpty() }.getOrDefault("")
            ).map { it.trim() }.firstOrNull { it.isNotBlank() && it != "null" }.orEmpty()
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
                ownerGender = gender
            }
        }
    }

    Box(Modifier.fillMaxSize(), contentAlignment=Alignment.BottomCenter) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.30f)).clickable(onClick=onClose))
        Column(
            Modifier.fillMaxWidth().fillMaxHeight(.55f)
                .background(Color(0xEB000000), RoundedCornerShape(topStart=15.dp,topEnd=15.dp))
        ) {
            
            Box(Modifier.fillMaxWidth().height(48.dp).padding(start = 14.dp, top = 10.dp)) {
                Row(
                    Modifier.align(Alignment.TopStart),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isRoomOwner) Box(
                        modifier = Modifier.size(34.dp).clip(CircleShape)
                            .background(Color.White.copy(alpha=0.15f))
                            .clickable { showDangerMenu = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(Modifier.size(16.dp)) {
                            drawCircle(Color.White, 1.8.dp.toPx(), androidx.compose.ui.geometry.Offset(size.width * .15f, size.height / 2))
                            drawCircle(Color.White, 1.8.dp.toPx(), androidx.compose.ui.geometry.Offset(size.width * .5f, size.height / 2))
                            drawCircle(Color.White, 1.8.dp.toPx(), androidx.compose.ui.geometry.Offset(size.width * .85f, size.height / 2))
                        }
                    }
                    Box(
                        Modifier.size(34.dp).clip(CircleShape)
                            .background(Color.White.copy(alpha=0.15f))
                            .clickable { showShareSheet = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(Modifier.size(16.dp)) {
                            val path = Path().apply {
                                moveTo(size.width * .15f, size.height * .85f)
                                quadraticTo(size.width * .15f, size.height * .35f, size.width * .5f, size.height * .35f)
                                lineTo(size.width * .5f, size.height * .15f)
                                lineTo(size.width * .95f, size.height * .5f)
                                lineTo(size.width * .5f, size.height * .85f)
                                lineTo(size.width * .5f, size.height * .65f)
                                quadraticTo(size.width * .3f, size.height * .65f, size.width * .15f, size.height * .85f)
                            }
                            drawPath(path, Color.White, style = Stroke(width = 1.8.dp.toPx(), join = StrokeJoin.Round, cap = StrokeCap.Round))
                        }
                    }
                }
                FallbackAsyncImage(
                    model = roomData.roomDp.trim().takeIf { it.isNotBlank() && it != "null" && it != "undefined" }
                        ?: ownerDisplayImage.trim().takeIf { it.isNotBlank() && it != "null" && it != "undefined" }
                        ?: ZYORA_DEFAULT_AVATAR_URL,
                    contentDescription = "Room",
                    modifier = Modifier.align(Alignment.TopCenter).offset(y = (-40).dp)
                        .requiredSize(80.dp).clip(RoundedCornerShape(12.dp))
                        .border(1.dp, Color.White.copy(alpha=.18f), RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            Column(Modifier.fillMaxWidth().padding(top = 0.dp, bottom = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(roomData.roomName.ifBlank { "Room" }, fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold, color = Color.White, maxLines = 1)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (roomData.roomId in setOf("100002","100003","500002","500003","500004","500005","500005")) {
                            AsyncImage(model="https://zyora-voice-chat.vercel.app/IMG_20261101_125212.png",
                                contentDescription="Purple ID Badge",
                                modifier=Modifier.height(16.dp).widthIn(min=22.dp,max=30.dp), contentScale=ContentScale.Fit)
                            Text(roomData.roomId,fontSize=10.sp,color=Color.White.copy(alpha=.60f))
                        } else Text("ID: "+roomData.roomId,fontSize=10.sp,color=Color.White.copy(alpha=.60f))
                        IconButton(onClick={onCopyId(roomData.roomId)},modifier=Modifier.size(24.dp)) {
                            Icon(Icons.Default.ContentCopy,contentDescription="Copy room ID",
                                tint=Color.White.copy(alpha=.70f),modifier=Modifier.size(14.dp))
                        }
                    }
                }

            Row(
                Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=6.dp),
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
                    Modifier.fillMaxWidth().weight(1f).padding(horizontal=16.dp,vertical=8.dp),
                    verticalArrangement=Arrangement.spacedBy(9.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha=.10f)).padding(horizontal=11.dp, vertical=9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            Modifier.weight(1f).clickable {
                                onOpenProfile(RoomInfoFollower(roomOwner.accountId, ownerDisplayName, ownerDisplayImage, ownerCountry))
                            },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FallbackAsyncImage(
                                model = if (isLogoOwner) "https://zyora-voice-chat.vercel.app/logo.png" else ownerDisplayImage,
                                contentDescription = "Room owner avatar",
                                modifier = Modifier.size(38.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Column(Modifier.padding(start=10.dp), verticalArrangement=Arrangement.spacedBy(0.dp)) {
                                Text(ownerDisplayName.ifBlank{"Unknown"}, fontSize=13.sp,
                                    fontWeight=FontWeight.SemiBold, color=Color.White, maxLines=1)
                                Text("Room owner", fontSize=10.sp, color=Color.White.copy(alpha=.55f))
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                    val displayOwnerId = ownerVipId.takeIf { ownerVipBadge && it.matches(Regex("\\d{1,20}")) }
                                        ?: roomOwner.accountId.ifBlank { roomOwner.id.ifBlank { roomOwner.uid } }
                                    if (ownerVipBadge && ownerVipId.isNotBlank()) {
                                        AsyncImage(
                                            model = "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/IMG_20261101_125212.png",
                                            contentDescription = "VIP ID",
                                            modifier = Modifier.height(11.dp).widthIn(min = 12.dp, max = 16.dp),
                                            contentScale = ContentScale.Fit
                                        )
                                        Text(displayOwnerId, fontSize=10.sp, color=Color.White.copy(alpha=.70f))
                                    } else {
                                        Text("ID: $displayOwnerId", fontSize=10.sp, color=Color.White.copy(alpha=.70f))
                                    }
                                }
                            }
                        }
                        AsyncImage(
                            model = "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/IMG_20261105_125330.png",
                            contentDescription = "Chat",
                            modifier = Modifier.size(36.dp).clip(CircleShape).clickable { onOpenOwnerChat(RoomInfoFollower(roomOwner.accountId.ifBlank { roomOwner.uid }.ifBlank { roomOwner.id }, ownerDisplayName, ownerDisplayImage, ownerCountry)) },
                            contentScale = ContentScale.Crop
                        )
                    }

                    Column {
                        Text("Announcement",fontSize=12.sp,fontWeight=FontWeight.Medium,color=Color(0xFF9CA3AF))
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
                        MemberRow(ownerDisplayName, if (isLogoOwner) "https://zyora-voice-chat.vercel.app/logo.png" else ownerDisplayImage, roomOwner.accountId.ifBlank { roomOwner.id.ifBlank { roomOwner.uid } }, "Owner", ownerCountry, ownerGender) {
                            onOpenProfile(RoomInfoFollower(roomOwner.accountId.ifBlank { roomOwner.id.ifBlank { roomOwner.uid } }, ownerDisplayName, ownerDisplayImage, ownerCountry, ownerGender))
                        }
                    }
                    val adminIds = roomAdmins.map { it.accountId }.toSet()
                    val allMembers = (roomFollowers + roomAdmins)
                        .filter { it.accountId.isNotBlank() && it.accountId != roomOwner.accountId }
                        .distinctBy { it.accountId }
                    items(allMembers, key = { "member-${it.accountId}" }) { f ->
                        val profile = memberProfiles[f.accountId] ?: f.copy(country = "")
                        MemberRow(profile.name, profile.image, profile.accountId, if (profile.accountId in adminIds) "Admin" else "", profile.country, profile.gender) { onOpenProfile(profile) }
                    }
                }
            }
        }

        if (showShareSheet) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=0.4f)).clickable { showShareSheet = false })
            Column(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.50f)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .background(Color.Black)
            ) {
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
                    // Yahan se "No fans yet" wala block hata diya hai
                    
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
                            
                            // Ye rahi teri nayi image wali button bss 
                            AsyncImage(
                                model = "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/IMG_20261107_205456.png",
                                contentDescription = "Share",
                                modifier = Modifier
                                    .height(28.dp)
                                    .width(64.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .clickable {
                                        onShareToUser(fan)
                                        showShareSheet = false
                                    },
                                contentScale = ContentScale.FillBounds
                            )
                        }
                    }
                }
            }
        }
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
        scrimColor = Color.Transparent,
        dragHandle = null
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 24.dp)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.Black)) {
                Text("Report", Modifier.fillMaxWidth().clickable(onClick = onReport).padding(vertical = 18.dp),
                    color = Color.White, fontSize = 18.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Medium)
                Divider(color = Color.DarkGray.copy(alpha = 0.5f), thickness = 0.5.dp)
                Text(if (isFollowing) "Followed" else "Follow",
                    Modifier.fillMaxWidth().clickable(onClick = onFollow).padding(vertical = 18.dp),
                    color = Color.White, fontSize = 18.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = onDismiss, Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                shape = RoundedCornerShape(12.dp)) {
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
    gender: String = "",
    onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 2.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = if (image.isBlank()) "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/default-avatar.png" else image,
            contentDescription = name,
            modifier = Modifier.size(44.dp).clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Column(Modifier.padding(start = 11.dp).weight(1f), verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Row(
                modifier = Modifier.offset(y = 2.dp), // Yahan thora niche kiya LV.1 ki taraf
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(name.ifBlank { "User" },
                    modifier = Modifier.weight(1f, fill = false), // fill = false lagaya taki tag name ke theek baad aaye, right mein na bhage
                    fontSize = 13.sp, fontWeight = FontWeight.Medium,
                    color = Color.White, maxLines = 2, softWrap = true)
                
                if (role == "Owner") {
                    AsyncImage(model="https://zyora-voice-chat.vercel.app/IMG_20261027_235537.png",
                        contentDescription="Owner", contentScale=ContentScale.Fit,
                        modifier=Modifier.padding(start=2.dp).height(14.dp).widthIn(min=30.dp, max=42.dp))
                } else if (role == "Admin") {
                    AsyncImage(model=ROOM_ADMIN_TAG, contentDescription="Room Admin",
                        contentScale=ContentScale.Fit, 
                        modifier=Modifier.padding(start=2.dp).height(14.dp).widthIn(min=30.dp, max=42.dp))
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                if (gender.isNotBlank()) {
                    val isFemale = gender.equals("female", true) || gender.equals("f", true) || gender == "♀"
                    Box(
                        Modifier.size(20.dp).clip(CircleShape)
                            .background(if (isFemale) Color(0xFFEC4899) else Color(0xFF3B82F6)),
                        contentAlignment = Alignment.Center
                    ) {
                        CustomGenderIcon(isFemale = isFemale, modifier = Modifier.size(8.dp))
                    }
                }
                Box(Modifier.height(16.dp).width(36.dp)) {
                    AsyncImage(model="https://zyora-voice-chat.vercel.app/IMG_20261028_004355.png",
                        contentDescription="Level", 
                        contentScale=ContentScale.FillBounds, modifier=Modifier.fillMaxSize())
                    
                    // Yahan LV.1 kar diya aur offset(1.dp, -3.dp) lagaya (1dp right, 3dp up)
                    Text("LV.1", color=Color.White, fontSize=8.sp, fontWeight=FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 4.dp)
                            .offset(x = 1.dp, y = (-3).dp)
                    )
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

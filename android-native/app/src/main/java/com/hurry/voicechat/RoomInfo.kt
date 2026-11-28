package com.hurry.voicechat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
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
    onOpenOwnerChat:(RoomInfoFollower)->Unit={},
    openShareRequest:Boolean=false,
    onShareSheetDismissed:()->Unit={}
) {
    if (!isOpen && !openShareRequest) return
    val context = LocalContext.current
    var tab by remember { mutableStateOf(0) }
    
    var showShareSheet by remember { mutableStateOf(false) }
    LaunchedEffect(openShareRequest) { showShareSheet = openShareRequest }
    var showDangerMenu by remember { mutableStateOf(false) }
    var showRoomReport by remember { mutableStateOf(false) }
    var roomFans by remember(currentUserId, roomOwner.accountId, roomOwner.id) { mutableStateOf<List<RoomInfoFollower>>(emptyList()) }
    
    LaunchedEffect(currentUserId, showShareSheet) {
        if (showShareSheet && currentUserId.isNotBlank()) {
            val cachedRows = withContext(kotlinx.coroutines.Dispatchers.IO) {
                val db = NativeIndexDatabase(context)
                (SocialFollowApi.cachedList(context, currentUserId, "fans") + SocialFollowApi.cachedList(context, currentUserId, "following"))
                    .mapNotNull { row ->
                        val id = row.accountId.ifBlank { row.uid.ifBlank { row.id } }
                        if (id.isBlank() || id == currentUserId) return@mapNotNull null
                        val p = runCatching { db.loadProfile("room_info_member:$id") ?: db.loadProfile(id) }.getOrNull()
                        fun valid(v:String)=v.trim().takeIf { it.isNotBlank() && it!="null" && !it.equals("undefined",true) && !it.equals("User",true) && !it.endsWith("/default-avatar.png",true) }.orEmpty()
                        RoomInfoFollower(id, valid(row.name).ifBlank { valid(p?.optString("name").orEmpty()) },
                            valid(row.image).ifBlank { valid(p?.optString("image").orEmpty()).ifBlank { valid(p?.optString("avatar").orEmpty()) } },
                            row.country.ifBlank { p?.optString("country").orEmpty() }, row.gender.ifBlank { p?.optString("gender").orEmpty() })
                    }.distinctBy { it.accountId }
            }
            roomFans = cachedRows.filter { it.name.isNotBlank() || it.image.isNotBlank() }
            val (fans, following) = kotlinx.coroutines.coroutineScope {
                val a=async(kotlinx.coroutines.Dispatchers.IO) { SocialFollowApi.list(context,currentUserId,"fans") }
                val b=async(kotlinx.coroutines.Dispatchers.IO) { SocialFollowApi.list(context,currentUserId,"following") }
                a.await() to b.await()
            }
            val rows=(fans+following).mapNotNull { row ->
                val id=row.accountId.ifBlank { row.uid.ifBlank { row.id } }
                if(id.isBlank()||id==currentUserId) null else RoomInfoFollower(id,row.name,row.image,row.country,row.gender)
            }.distinctBy { it.accountId }
            val limiter=kotlinx.coroutines.sync.Semaphore(6)
            rows.forEach { row -> launch(kotlinx.coroutines.Dispatchers.IO) {
                limiter.withPermit {
                    val db=NativeIndexDatabase(context)
                    val p=runCatching { db.loadProfile("room_info_member:${row.accountId}") ?: db.loadProfile(row.accountId) }.getOrNull()
                    fun valid(v:String)=v.trim().takeIf { it.isNotBlank() && it!="null" && !it.equals("undefined",true) && !it.equals("User",true) && !it.endsWith("/default-avatar.png",true) }.orEmpty()
                    val cn=valid(p?.optString("name").orEmpty())
                    val ci=valid(p?.optString("image").orEmpty()).ifBlank { valid(p?.optString("avatar").orEmpty()) }
                    val remote=if(valid(row.name).isBlank()||valid(row.image).isBlank()) runCatching {
                        ProfilePersistence.load(TargetUser(id=row.accountId,uid=row.accountId,accountId=row.accountId,name=cn,image=ci))
                    }.getOrNull() else null
                    fun rv(vararg keys:String)=keys.asSequence().map { valid(remote?.optString(it,"").orEmpty()) }.firstOrNull { it.isNotBlank() }.orEmpty()
                    val resolved=row.copy(
                        name=valid(row.name).ifBlank { cn }.ifBlank { rv("name","displayName","userName","nickname") },
                        image=valid(row.image).ifBlank { ci }.ifBlank { rv("image","avatar","profileImageUrl","dp","photo","photoURL","avatarUrl") },
                        country=row.country.ifBlank { p?.optString("country").orEmpty() }.ifBlank { remote?.optString("country").orEmpty() },
                        gender=row.gender.ifBlank { p?.optString("gender").orEmpty() }.ifBlank { remote?.optString("gender").orEmpty() })
                    if(resolved.name.isNotBlank()||resolved.image.isNotBlank()) {
                        runCatching {
                            val saved=db.loadProfile("room_info_member:${row.accountId}")?:org.json.JSONObject()
                            db.saveProfile("room_info_member:${row.accountId}",saved.put("name",resolved.name).put("image",resolved.image).put("country",resolved.country).put("gender",resolved.gender).put("updatedAt",System.currentTimeMillis()))
                        }
                        withContext(kotlinx.coroutines.Dispatchers.Main) { roomFans=roomFans.filterNot { it.accountId==resolved.accountId }+resolved }
                    }
                }
            } }
        }
    }

    var ownerVipId by remember(roomOwner.id, roomOwner.uid, roomOwner.accountId, roomOwner.vipId) { mutableStateOf(roomOwner.vipId.trim()) }
    var ownerVipBadge by remember(roomOwner.id, roomOwner.uid, roomOwner.accountId, roomOwner.vipIdBadge) { mutableStateOf(roomOwner.vipIdBadge) }
    // Use the exact VIP ID state already rendered in the room's top owner card.
    // Do not reload/overwrite it here with a second, potentially stale profile lookup.

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

        // Hydrate the owner row from local profile cache first. The previous flow
        // waited for two network lookups before setting gender/country, so the
        // owner row showed fallback data on every sheet open.
        val db = NativeIndexDatabase(context)
        val cachedProfiles = withContext(kotlinx.coroutines.Dispatchers.IO) {
            listOf(
                runCatching { db.loadProfile("room_info_member:$ownerId") }.getOrNull(),
                runCatching { db.loadProfile(ownerId) }.getOrNull()
            )
        }
        fun cachedValue(keys: List<String>): String = cachedProfiles.asSequence()
            .filterNotNull()
            .flatMap { p -> keys.asSequence().map { p.optString(it, "").trim() } }
            .firstOrNull { it.isNotBlank() && it != "null" && !it.equals("undefined", true) }
            .orEmpty()
        val cachedName = cachedValue(listOf("name", "displayName", "userName", "nickname"))
            .takeIf { it.isNotBlank() && !it.equals("User", true) }.orEmpty()
        val cachedImage = cachedValue(listOf("image", "avatar", "profileImageUrl", "dp", "photo", "photoURL", "avatarUrl"))
            .takeIf { it.isNotBlank() && !it.endsWith("/default-avatar.png", true) }.orEmpty()
        val cachedCountry = cachedValue(listOf("country", "countryName", "nationality"))
        val cachedGender = cachedValue(listOf("gender", "sex"))

        withContext(kotlinx.coroutines.Dispatchers.Main) {
            if (!isLogoOwner) {
                if (cachedName.isNotBlank()) ownerDisplayName = cachedName
                if (cachedImage.isNotBlank()) ownerDisplayImage = cachedImage
            }
            if (cachedCountry.isNotBlank()) ownerCountry = cachedCountry
            if (cachedGender.isNotBlank()) ownerGender = cachedGender
        }

        // Refresh profile and room-country independently; neither request blocks
        // the other or the already-rendered cached owner row.
        val (profile, country) = kotlinx.coroutines.coroutineScope {
            val profileTask = async(kotlinx.coroutines.Dispatchers.IO) {
                runCatching {
                    ProfilePersistence.load(TargetUser(
                        id = ownerId, uid = ownerId, accountId = ownerId,
                        name = roomOwner.name, image = roomOwner.image
                    ))
                }.getOrNull()
            }
            val countryTask = async(kotlinx.coroutines.Dispatchers.IO) {
                runCatching {
                    loadRoomProfileCountry(context, ownerId, roomOwner.name, roomOwner.image, cachedCountry)
                }.getOrDefault("")
            }
            profileTask.await() to countryTask.await()
        }
        val name = profile?.let { p ->
            sequenceOf("name", "displayName", "userName", "nickname")
                .map { p.optString(it, "").trim() }
                .firstOrNull { it.isNotBlank() && it != "null" && !it.equals("User", true) }
        }.orEmpty()
        val image = profile?.let { p ->
            sequenceOf("image", "avatar", "profileImageUrl", "dp", "photo", "photoURL", "avatarUrl")
                .map { p.optString(it, "").trim() }
                .firstOrNull { it.isNotBlank() && it != "null" && !it.endsWith("/default-avatar.png", true) }
        }.orEmpty()
        val gender = sequenceOf(profile?.optString("gender", "").orEmpty(), cachedGender)
            .map { it.trim() }
            .firstOrNull { it.isNotBlank() && it != "null" && !it.equals("undefined", true) }
            .orEmpty()
        val resolvedCountry = country.ifBlank { cachedCountry }

        withContext(kotlinx.coroutines.Dispatchers.Main) {
            if (!isLogoOwner) {
                if (name.isNotBlank()) ownerDisplayName = name
                if (image.isNotBlank()) ownerDisplayImage = image
            }
            if (resolvedCountry.isNotBlank()) ownerCountry = resolvedCountry
            if (gender.isNotBlank()) ownerGender = gender
        }

        withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                val saved = db.loadProfile("room_info_member:$ownerId") ?: org.json.JSONObject()
                db.saveProfile(
                    "room_info_member:$ownerId",
                    saved.put("name", name.ifBlank { cachedName.ifBlank { roomOwner.name } })
                        .put("image", image.ifBlank { cachedImage.ifBlank { roomOwner.image } })
                        .put("country", resolvedCountry)
                        .put("gender", gender)
                        .put("updatedAt", System.currentTimeMillis())
                )
            }
        }
    }

    // A share request is a standalone overlay: never render the Room Info panel behind it.
    if (isOpen && !openShareRequest) {
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
                }
                FallbackAsyncImage(
                    model = roomData.roomDp.trim().takeIf {
                        it.isNotBlank() && it != "null" && it != "undefined" &&
                            !it.equals("/default-avatar.png", true) &&
                            !it.endsWith("/default-avatar.png", true)
                    } ?: ZYORA_DEFAULT_AVATAR_URL,
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
                        val topVipId = ownerVipId.takeIf { ownerVipBadge && it.matches(Regex("\\d{1,20}")) }
                        val topDisplayId = topVipId ?: roomData.roomId
                        if (topVipId != null) {
                            AsyncImage(model="https://zyora-voice-chat.vercel.app/IMG_20261101_125212.png",
                                contentDescription="VIP ID Badge",
                                modifier=Modifier.height(16.dp).widthIn(min=22.dp,max=30.dp), contentScale=ContentScale.Fit)
                            Text(topVipId,fontSize=10.sp,color=Color.White.copy(alpha=.60f))
                        } else if (roomData.roomId in setOf("100002","100003","500002","500003","500004","500005")) {
                            AsyncImage(model="https://zyora-voice-chat.vercel.app/IMG_20261101_125212.png",
                                contentDescription="Purple ID Badge",
                                modifier=Modifier.height(16.dp).widthIn(min=22.dp,max=30.dp), contentScale=ContentScale.Fit)
                            Text(roomData.roomId,fontSize=10.sp,color=Color.White.copy(alpha=.60f))
                        } else Text("ID: "+roomData.roomId,fontSize=10.sp,color=Color.White.copy(alpha=.60f))
                        IconButton(onClick={onCopyId(topDisplayId)},modifier=Modifier.size(24.dp)) {
                            Icon(Icons.Default.ContentCopy,contentDescription="Copy ID",
                                tint=Color.White.copy(alpha=.70f),modifier=Modifier.size(14.dp))
                        }
                    }
                }

            Row(
                Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=6.dp)
                    .pointerInput(tab) {
                        var drag=0f
                        detectHorizontalDragGestures(
                            onHorizontalDrag={ _, amount -> drag+=amount },
                            onDragEnd={ if(drag < -55f && tab==0) tab=1 else if(drag > 55f && tab==1) tab=0; drag=0f },
                            onDragCancel={ drag=0f }
                        )
                    },
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
                        MemberRow(
                            ownerDisplayName,
                            if (isLogoOwner) "https://zyora-voice-chat.vercel.app/logo.png" else ownerDisplayImage,
                            ownerVipId.takeIf { ownerVipBadge && it.isNotBlank() }
                                ?: roomOwner.accountId.ifBlank { roomOwner.id.ifBlank { roomOwner.uid } },
                            "Owner", ownerCountry, ownerGender,
                            vipId = ownerVipId,
                            showVipIdBadge = ownerVipBadge && ownerVipId.isNotBlank()
                        ) {
                            onOpenProfile(RoomInfoFollower(roomOwner.accountId.ifBlank { roomOwner.id.ifBlank { roomOwner.uid } }, ownerDisplayName, ownerDisplayImage, ownerCountry, ownerGender))
                        }
                    }
                    val ownerId = roomOwner.accountId.ifBlank { roomOwner.id.ifBlank { roomOwner.uid } }
                    val adminIds = roomAdmins.map { it.accountId }.filter { it.isNotBlank() && it != ownerId }.toSet()
                    // Keep the owner first, then admins in their assigned order, then regular members.
                    val orderedAdmins = roomAdmins
                        .filter { it.accountId.isNotBlank() && it.accountId != ownerId }
                        .distinctBy { it.accountId }
                    val regularMembers = roomFollowers
                        .filter { it.accountId.isNotBlank() && it.accountId != ownerId && it.accountId !in adminIds }
                        .distinctBy { it.accountId }
                    val allMembers = orderedAdmins + regularMembers
                    items(allMembers, key = { "member-${it.accountId}" }) { f ->
                        val profile = memberProfiles[f.accountId] ?: f.copy(country = "")
                        MemberRow(profile.name, profile.image, profile.accountId, if (profile.accountId in adminIds) "Admin" else "", profile.country, profile.gender) { onOpenProfile(profile) }
                    }
                }
            }
        }

    }
        if (showShareSheet) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=0.4f)).clickable { showShareSheet = false; onShareSheetDismissed() })
                Column(
                    Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.60f)
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
                            if (fan.image.isNotBlank()) {
                                AsyncImage(model=fan.image,contentDescription=fan.name,modifier=Modifier.size(42.dp).clip(CircleShape),contentScale=ContentScale.Crop)
                            } else {
                                Box(Modifier.size(42.dp).clip(CircleShape).background(Color.White.copy(alpha=.10f)))
                            }
                            Text(fan.name.takeIf { it.isNotBlank() } ?: " ", Modifier.weight(1f).padding(start = 10.dp),
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
                                        onShareSheetDismissed()
                                    },
                                contentScale = ContentScale.FillBounds
                            )
                        }
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
    vipId: String = "",
    showVipIdBadge: Boolean = false,
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

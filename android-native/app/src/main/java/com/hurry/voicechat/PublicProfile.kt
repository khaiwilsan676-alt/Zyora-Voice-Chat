package com.hurry.voicechat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import kotlin.math.abs

// --- DATA CLASSES ---

data class TargetUser(
    val id: String? = null,
    val uid: String? = null,
    val name: String? = null,
    val displayAccountNumber: String? = null,
    val accountId: String? = null,
    val photo: String? = null,
    val image: String? = null,
    val coverPhoto: String? = null,
    val gender: String? = null,
    val age: Int? = null,
    val followers: Int? = null,
    val bio: String? = null,
    val location: String? = null,
    val country: String? = null,
    val countryCode: String? = null,
    val flag: String? = null,
    val officialHeadTag: Boolean? = null,
    val officialTag: Boolean? = null,
    val adminTag: Boolean? = null,
    val serviceTag: Boolean? = null,
    val coinSellerTag: Boolean? = null,
    val vipTag: Boolean? = null,
    val premiumTag: Boolean? = null
)

// Dummy constants for Country and Accounts
val COUNTRIES = listOf(
    Pair("IN", "🇮🇳 India"), Pair("US", "🇺🇸 United States")
)
val SPECIAL_ACCOUNTS = mapOf("HUSxSvQnabgU029dWYt1TUV04hd2" to "100002")

// --- MAIN SCREEN ---

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PublicProfileScreen(
    onBack: () -> Unit,
    onJoinRoom: (String) -> Unit = {},
    isOtherUser: Boolean = false,
    targetUser: TargetUser? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val profileCacheKey = targetUser?.uid?.takeIf { it.isNotBlank() } ?: targetUser?.id?.takeIf { it.isNotBlank() } ?: targetUser?.accountId.orEmpty()
    val cachedProfile = remember(profileCacheKey) { NativeIndexDatabase(context).loadProfile(profileCacheKey) }

    // Vercel serves files inside the public/ directory from the URL root.
    val profileAsset: (String) -> String = { path ->
        if (path.isBlank()) ""
        else if (path.startsWith("http://") || path.startsWith("https://") || path.startsWith("data:") || path.startsWith("content:") || path.startsWith("file:")) path
        else {
            val cleanPath = path.trimStart('/').removePrefix("public/")
            "https://zyora-voice-chat.vercel.app/$cleanPath"
        }
    }

    // State Variables
    var coverPhotos by remember(profileCacheKey, cachedProfile, targetUser?.coverPhoto) {
        mutableStateOf(ProfilePersistence.stringList(cachedProfile?.optString("coverPhotos", "")).ifEmpty { cachedProfile?.optString("coverPhoto")?.takeIf { it.isNotBlank() && it != "null" }?.let { listOf(it) } ?: targetUser?.coverPhoto?.takeIf { it.isNotBlank() }?.let { listOf(it) } ?: emptyList() }.map { profileAsset(it) })
    }
    var albumImages by remember(profileCacheKey, cachedProfile) { mutableStateOf(ProfilePersistence.stringList(cachedProfile?.optString("albumImages", ""))) }
    var selectedAvatarUri by remember(targetUser?.photo, targetUser?.image, cachedProfile) {
        mutableStateOf<String?>(sequenceOf("image", "photo", "photoURL", "avatar", "picture").map { cachedProfile?.optString(it, "").orEmpty() }.firstOrNull { it.isNotBlank() && it != "null" } ?: targetUser?.photo?.takeIf { it.isNotBlank() } ?: targetUser?.image?.takeIf { it.isNotBlank() })
    }
    var ownedFrames by remember { mutableStateOf(listOf<String>()) } // Dummy for UI
    var receivedGifts by remember { mutableStateOf(listOf<String>()) } // Dummy for UI

    var showEditSheet by remember { mutableStateOf(false) }
    var showNameScreen by remember { mutableStateOf(false) }
    var showAgeScreen by remember { mutableStateOf(false) }
    var showBioScreen by remember { mutableStateOf(false) }
    var showActionSheet by remember { mutableStateOf(false) }
    var showUserReport by remember { mutableStateOf(false) }
    var showChat by remember { mutableStateOf(false) }

    var fullImageIndex by remember { mutableStateOf<Int?>(null) }
    var isFollowing by remember { mutableStateOf(false) }

    // User Data State
    var userName by remember(profileCacheKey, cachedProfile) { mutableStateOf(cachedProfile?.optString("name")?.takeIf { it.isNotBlank() && it != "null" } ?: targetUser?.name?.takeIf { it.isNotBlank() } ?: "User") }
    var userBio by remember(profileCacheKey, cachedProfile, targetUser?.bio) { mutableStateOf(cachedProfile?.optString("bio")?.takeIf { it != "null" } ?: targetUser?.bio ?: "") }
    var userAge by remember(profileCacheKey, cachedProfile, targetUser?.age) { mutableIntStateOf(if (cachedProfile?.has("age") == true) cachedProfile.optInt("age", targetUser?.age ?: 0) else targetUser?.age ?: 0) }
    
    // Keep profile details in index.db; MongoDB remains the source for the latest name and DP.
    var userGender by remember(profileCacheKey, cachedProfile, targetUser?.gender) { mutableStateOf(cachedProfile?.optString("gender")?.takeIf { it.isNotBlank() && it != "null" } ?: targetUser?.gender?.takeIf { it.isNotBlank() } ?: "Male") }
    
    val displayAccNum = targetUser?.displayAccountNumber?.takeIf { it.isNotBlank() } ?: targetUser?.accountId?.takeIf { it.isNotBlank() } ?: "—"
    
    val defaultAvatar = "/file_000000009aac81faa8564416680aba4e.png"
    val userPhotoRaw = selectedAvatarUri ?: targetUser?.photo?.takeIf { it.isNotBlank() } ?: targetUser?.image?.takeIf { it.isNotBlank() } ?: defaultAvatar
    val userPhoto = profileAsset(userPhotoRaw)

    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { picked -> val saved = ProfilePersistence.imageDataUri(context, picked) ?: picked.toString(); selectedAvatarUri = saved; if (!isOtherUser) coroutineScope.launch { val fields = mapOf("image" to saved, "photo" to saved); ProfilePersistence.save(targetUser, fields); if (profileCacheKey.isNotBlank()) { val db = NativeIndexDatabase(context); val cached = db.loadProfile(profileCacheKey) ?: org.json.JSONObject(); fields.forEach { (key, value) -> cached.put(key, value) }; db.saveProfile(profileCacheKey, cached) } } }
    }
    val albumPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { picked -> if (albumImages.size < 4) { val saved = ProfilePersistence.imageDataUri(context, picked) ?: picked.toString(); albumImages = albumImages + saved; if (!isOtherUser) coroutineScope.launch { val fields = mapOf("albumImages" to org.json.JSONArray(albumImages).toString()); ProfilePersistence.save(targetUser, fields); if (profileCacheKey.isNotBlank()) { val db = NativeIndexDatabase(context); val cached = db.loadProfile(profileCacheKey) ?: org.json.JSONObject(); fields.forEach { (key, value) -> cached.put(key, value) }; db.saveProfile(profileCacheKey, cached) } } } }
    }
    val backgroundPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { picked -> if (coverPhotos.size < 4) { val saved = ProfilePersistence.imageDataUri(context, picked) ?: picked.toString(); coverPhotos = coverPhotos + saved; if (!isOtherUser) coroutineScope.launch { val fields = mapOf("coverPhotos" to org.json.JSONArray(coverPhotos).toString(), "coverPhoto" to saved); ProfilePersistence.save(targetUser, fields); if (profileCacheKey.isNotBlank()) { val db = NativeIndexDatabase(context); val cached = db.loadProfile(profileCacheKey) ?: org.json.JSONObject(); fields.forEach { (key, value) -> cached.put(key, value) }; db.saveProfile(profileCacheKey, cached) } } } }
    }
    
    val followers = targetUser?.followers ?: 0
    val flag = targetUser?.flag ?: "🇮🇳"
    val location = targetUser?.location ?: "India"

    LaunchedEffect(targetUser?.uid, targetUser?.id, targetUser?.accountId, profileCacheKey) {
        val remote = ProfilePersistence.load(targetUser)
        val cache = NativeIndexDatabase(context)
        val cached = if (profileCacheKey.isNotBlank()) cache.loadProfile(profileCacheKey) else null
        if (remote == null && cached == null) return@LaunchedEffect

        // First visit seeds index.db from MongoDB. Later visits keep non-name/DP fields
        // from index.db and refresh only name/avatar from MongoDB.
        val merged = org.json.JSONObject(cached?.toString() ?: "{}")
        if (cached == null && remote != null) {
            val keys = remote.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                if (key != "name" && key != "displayName" && key != "userName" && key != "image" && key != "photo" && key != "photoURL" && key != "avatar" && key != "picture") merged.put(key, remote.opt(key))
            }
        }
        remote?.let { latest ->
            sequenceOf("name", "displayName", "userName", "image", "photo", "photoURL", "avatar", "picture").forEach { key ->
                val value = latest.optString(key, "")
                if (value.isNotBlank() && value != "null") merged.put(key, value)
            }
        }
        if (profileCacheKey.isNotBlank()) cache.saveProfile(profileCacheKey, merged)

        remote?.optString("name")?.takeIf { it.isNotBlank() && it != "null" }?.let { userName = it }
        val savedAvatar = remote?.let { latest ->
            sequenceOf("image", "photo", "photoURL", "avatar", "picture").map { latest.optString(it, "") }
                .firstOrNull { it.isNotBlank() && it != "null" }
        }
        if (!savedAvatar.isNullOrBlank() && savedAvatar != selectedAvatarUri) selectedAvatarUri = savedAvatar

        if (cached == null && remote != null) {
            remote.optString("bio").takeIf { it != "null" }?.let { userBio = it }
            if (remote.has("age")) userAge = remote.optInt("age", userAge)
            remote.optString("gender").takeIf { it.isNotBlank() && it != "null" }?.let { userGender = it }
            val albums = ProfilePersistence.stringList(remote.optString("albumImages", ""))
            if (albums.isNotEmpty()) albumImages = albums
            val covers = ProfilePersistence.stringList(remote.optString("coverPhotos", ""))
            if (covers.isNotEmpty()) coverPhotos = covers.map { profileAsset(it) }
            else remote.optString("coverPhoto").takeIf { it.isNotBlank() && it != "null" }?.let { coverPhotos = listOf(profileAsset(it)) }
        } else if (cached != null) {
            cached.optString("bio").takeIf { it != "null" }?.let { userBio = it }
            if (cached.has("age")) userAge = cached.optInt("age", userAge)
            cached.optString("gender").takeIf { it.isNotBlank() && it != "null" }?.let { userGender = it }
            val albums = ProfilePersistence.stringList(cached.optString("albumImages", ""))
            if (albums.isNotEmpty()) albumImages = albums
            val covers = ProfilePersistence.stringList(cached.optString("coverPhotos", ""))
            if (covers.isNotEmpty()) coverPhotos = covers.map { profileAsset(it) }
            else cached.optString("coverPhoto").takeIf { it.isNotBlank() && it != "null" }?.let { coverPhotos = listOf(profileAsset(it)) }
        }
    }

    val avatarLetter = userName.take(1).uppercase()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = if (isOtherUser) 100.dp else 40.dp)
        ) {
            // 1. Cover & Header Section
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                        .background(Color(0xFF1F2937)) // gray-800
                ) {
                    if (coverPhotos.isNotEmpty()) {
                        val pagerState = rememberPagerState(pageCount = { coverPhotos.size })
                        LaunchedEffect(pagerState, coverPhotos.size) {
                            if (coverPhotos.size > 1) {
                                while (true) {
                                    kotlinx.coroutines.delay(5_000)
                                    pagerState.animateScrollToPage((pagerState.currentPage + 1) % coverPhotos.size)
                                }
                            }
                        }
                        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                            AsyncImage(
                                model = coverPhotos[page],
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    } else if (userPhoto.isNotBlank()) {
                        AsyncImage(model = userPhoto, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(avatarLetter, color = Color.White, fontSize = 64.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Top Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.statusBars)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                        }
                        if (isOtherUser) {
                            IconButton(onClick = { showActionSheet = true }) {
                                Icon(Icons.Outlined.Warning, "Report", tint = Color.White)
                            }
                        } else {
                            IconButton(onClick = { showEditSheet = true }) {
                                Icon(Icons.Outlined.Edit, "Edit", tint = Color.White)
                            }
                        }
                    }

                    // Floating Avatar
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.height(24.dp).width(48.dp)) {
                        AsyncImage(model = profileAsset("/IMG_20260911_230430.png"), contentDescription = "Level", contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                        Text("1", color = Color(0xFFF5DDA0), fontSize = 9.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    }
                }
            }

            // 2. Profile Info Section
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                        .padding(top = 4.dp)
                ) {
                    val genderStr = userGender.trim().ifEmpty { "Male" }
                    val isFemale = genderStr.equals("female", ignoreCase = true)
                    
                    // Name format logic: Max 8 characters
                    val displayName = if (userName.length > 8) userName.take(8) + "..." else userName

                    // Custom Layout for Name, Gender and Tags (Row 1 & Row 2)
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // ROW 1: Name, Gender Tag, Official Head (bada), Official (chhota)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(displayName, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            
                            // GENDER BOX (Fully Rounded & Smaller)
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape) // Fully round kar diya
                                    .background(if (isFemale) Color(0xFFFF5874) else Color(0xFF3B82F6))
                                    .padding(horizontal = 5.dp, vertical = 2.dp) // Thoda chhota kar diya
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    CustomGenderIcon(isFemale = isFemale, modifier = Modifier.size(8.dp)) // Icon chhota
                                    Text(text = userAge.toString(), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold) // Text chhota
                                }
                            }

                            // TAG 1: Official Head (Size utna hi rakha hai - 30.dp)
                            AsyncImage(model = profileAsset("/file_00000000ef2c82118b2266596d0e992d.png"), contentDescription = "Official Head", contentScale = ContentScale.Fit, modifier = Modifier.height(31.dp).widthIn(max = 80.dp))
                            
                            // TAG 2: Official (Size chhota kar diya - 22.dp)
                            AsyncImage(model = profileAsset("/IMG_20261027_102250.png"), contentDescription = "Official", contentScale = ContentScale.Fit, modifier = Modifier.height(22.dp).widthIn(max = 60.dp))
                        }

                        // ROW 2: Baaki 3 Tags (Size chhota kar diya - 22.dp)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AsyncImage(model = profileAsset("/IMG_20261027_114117.png"), contentDescription = "Admin", contentScale = ContentScale.Fit, modifier = Modifier.height(22.dp).widthIn(max = 60.dp))
                            AsyncImage(model = profileAsset("/IMG_20261027_102207.png"), contentDescription = "Service / CS", contentScale = ContentScale.Fit, modifier = Modifier.height(22.dp).widthIn(max = 60.dp))
                            AsyncImage(model = profileAsset("/IMG_20261027_105359.png"), contentDescription = "Coin Seller", contentScale = ContentScale.Fit, modifier = Modifier.height(22.dp).widthIn(max = 60.dp))
                        }
                    }

                    // ID & Followers
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        AsyncImage(
                            model = profileAsset("/file_0000000063b481fa89d3c7729265058b.png"),
                            contentDescription = "Purple ID Badge",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.height(30.dp).widthIn(min = 22.dp, max = 35.dp).padding(end = 4.dp)
                        )
                        Text(displayAccNum, fontSize = 12.sp, color = Color.Gray)

                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("ID", displayAccNum))
                                Toast.makeText(context, "ID Copied!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color.Gray, modifier = Modifier.size(14.dp))
                        }
                        Text(" | ", color = Color.LightGray)
                        Text("$followers Fans", fontSize = 12.sp, color = Color.Gray)
                    }

                    Row(modifier = Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        AsyncImage(model = profileAsset("/1785486414756.png"), contentDescription = "Profile badge", contentScale = ContentScale.Fit, modifier = Modifier.height(24.dp).widthIn(max = 52.dp))
                    }

                    // Location & Bio
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp)) {
                        Icon(Icons.Outlined.LocationOn, "Location", tint = Color.Gray, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("$flag $location", fontSize = 14.sp, color = Color.DarkGray)
                    }

                    Row(modifier = Modifier.padding(top = 8.dp), verticalAlignment = Alignment.Top) {
                        if (!isOtherUser) {
                            Icon(Icons.Outlined.Edit, "Edit Bio", tint = Color.Gray, modifier = Modifier.size(16.dp).clickable { showEditSheet = true })
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = userBio.ifBlank { if (isOtherUser) "No bio added yet" else "Add bio..." },
                            fontSize = 12.sp,
                            color = Color.Gray,
                            fontStyle = FontStyle.Italic
                        )
                    }

                    // Profile Tab Header
                    Box(
                        modifier = Modifier
                            .padding(top = 24.dp)
                            .padding(bottom = 8.dp) 
                    ) {
                        Text("Profile", color = Color(0xFF3B82F6), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Box(modifier = Modifier.align(Alignment.BottomCenter).offset(y = 10.dp).width(16.dp).height(4.dp).clip(CircleShape).background(Color(0xFF3B82F6)))
                    }
                }
            }

            // 3. Grids Section (Albums, Vehicles, Medals, Frames, Gifts)
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    
                    // Albums
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionHeader("Albums", "${albumImages.size}/4")
                        if (albumImages.isNotEmpty()) {
                            LazyVerticalGrid(columns = GridCells.Fixed(4), modifier = Modifier.height(80.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(albumImages.size) { idx ->
                                    AsyncImage(
                                        model = profileAsset(albumImages[idx]),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.aspectRatio(1f).clip(RoundedCornerShape(12.dp)).background(Color.LightGray).clickable { fullImageIndex = idx }
                                    )
                                }
                            }
                        } else {
                            AsyncImage(model = profileAsset("/IMG_20260726_225835.jpg"), contentDescription = "Album", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(112.dp).clip(RoundedCornerShape(16.dp)))
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp)) 

                    // Vehicle
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionHeader("Vehicle")
                        AsyncImage(model = profileAsset("/1785091443553.png"), contentDescription = "Vehicle", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(112.dp).clip(RoundedCornerShape(16.dp)))
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Medal
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionHeader("Medal")
                        AsyncImage(model = profileAsset("/1785091431545.png"), contentDescription = "Medal", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(112.dp).clip(RoundedCornerShape(16.dp)))
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Frame
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionHeader("Frame")
                        if (ownedFrames.isNotEmpty()) {
                            LazyVerticalGrid(columns = GridCells.Fixed(4), modifier = Modifier.height(88.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(ownedFrames.size) { idx ->
                                    AsyncImage(model = profileAsset(ownedFrames[idx]), contentDescription = "Frame", contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth().aspectRatio(1f))
                                }
                            }
                        } else {
                            AsyncImage(model = profileAsset("/1785091457562.png"), contentDescription = "Frame", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(112.dp).clip(RoundedCornerShape(16.dp)))
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Gift
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionHeader("Gift")
                        if (receivedGifts.isNotEmpty()) {
                            LazyVerticalGrid(columns = GridCells.Fixed(4), modifier = Modifier.height(88.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(receivedGifts.size) { idx ->
                                    AsyncImage(model = profileAsset(receivedGifts[idx]), contentDescription = "Gift", contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth().aspectRatio(1f))
                                }
                            }
                        } else {
                            AsyncImage(model = profileAsset("/1785091520912.png"), contentDescription = "Gift", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(112.dp).clip(RoundedCornerShape(16.dp)))
                        }
                    }
                }
            }
        }

        // 4. Bottom Action Bar (For Other Users)
        if (isOtherUser) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.95f))
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .windowInsetsPadding(WindowInsets.navigationBars),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = { isFollowing = !isFollowing },
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5874)),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp) 
                ) {
                    Icon(Icons.Default.Favorite, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isFollowing) "Following" else "Follow", 
                        fontSize = 16.sp, 
                        fontWeight = FontWeight.Medium,
                        maxLines = 1, 
                        softWrap = false, 
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Button(
                    onClick = { showChat = true },
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DC4E9)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    RealProfileChatIcon()
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Chat", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }

    // --- OVERLAYS & BOTTOM SHEETS ---

    if (isOtherUser && showActionSheet) {
        ModalBottomSheet(
            onDismissRequest = { showActionSheet = false },
            containerColor = Color.Black,
            contentColor = Color.White,
            scrimColor = Color.Black.copy(alpha = 0.28f),
            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
            dragHandle = null
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp)
            ) {
                Text(
                    "Report",
                    modifier = Modifier.fillMaxWidth().clickable {
                        showActionSheet = false
                        showUserReport = true
                    }.padding(vertical = 16.dp),
                    color = Color.White,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "Block",
                    modifier = Modifier.fillMaxWidth().clickable {
                        showActionSheet = false
                        Toast.makeText(context, "Block user", Toast.LENGTH_SHORT).show()
                    }.padding(vertical = 16.dp),
                    color = Color.White,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { showActionSheet = false },
                    modifier = Modifier
                        .width(280.dp)
                        .height(55.dp)
                        .align(Alignment.CenterHorizontally),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Cancel", color = Color.White) }
            }
        }
    }

    if (isOtherUser && showUserReport) {
        Dialog(
            onDismissRequest = { showUserReport = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            var reportCategory by remember { mutableStateOf("") }
            var reportDescription by remember { mutableStateOf("") }
            var reportSubmitted by remember { mutableStateOf(false) }
            Column(
                modifier = Modifier.fillMaxSize().background(Color.White).windowInsetsPadding(WindowInsets.statusBars).padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { showUserReport = false }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF374151))
                    }
                    Spacer(Modifier.weight(1f))
                    Text("Report", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827))
                    Spacer(Modifier.weight(1f))
                    Spacer(Modifier.width(48.dp))
                }
                if (reportSubmitted) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Thank You!", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827))
                        Text("Your report has been submitted successfully.", color = Color(0xFF6B7280), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
                        Button(onClick = { showUserReport = false }, modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) { Text("OK") }
                    }
                } else {
                    Text("Select a reason", fontSize = 14.sp, color = Color(0xFF374151), modifier = Modifier.padding(top = 20.dp, bottom = 12.dp))
                    val reportCategories = listOf("Violence", "Abusing", "Illegal", "Other's")
                    reportCategories.chunked(2).forEach { rowCategories ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowCategories.forEach { category ->
                                FilterChip(
                                    selected = reportCategory == category,
                                    onClick = { reportCategory = category },
                                    label = { Text(category) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = reportDescription,
                        onValueChange = { reportDescription = it },
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(140.dp),
                        label = { Text("Description") }
                    )
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = {
                            if (reportCategory.isNotBlank()) {
                                reportSubmitted = true
                            } else {
                                Toast.makeText(context, "Select a reason", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DC4E9))
                    ) { Text("Submit", color = Color.White) }
                }
            }
        }
    }

    if (!isOtherUser && showEditSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        
        ModalBottomSheet(
            onDismissRequest = { showEditSheet = false },
            sheetState = sheetState,
            containerColor = Color.White,
            scrimColor = Color.Black.copy(alpha = 0.5f),
            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
            dragHandle = null
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.66f) 
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { showEditSheet = false }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF374151))
                    }
                    Text("Edit Information", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827))
                    Spacer(modifier = Modifier.width(24.dp))
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Avatar", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF374151))
                        Box(
                            modifier = Modifier.size(56.dp).clip(CircleShape).background(Color(0xFFE5E7EB)).clickable { avatarPicker.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            if (userPhoto.isNotBlank()) {
                                AsyncImage(model = userPhoto, contentDescription = "Avatar", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            } else {
                                Text(avatarLetter, fontSize = 20.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    EditRowItem("Nickname", { showNameScreen = true }) {
                        Text(userName.ifBlank { "Enter name" }, color = Color(0xFF6B7280), fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Icon(Icons.Default.ChevronRight, null, tint = Color(0xFF9CA3AF), modifier = Modifier.size(16.dp))
                    }

                    EditRowItem("Age", { showAgeScreen = true }) {
                        Text(userAge.toString(), color = Color(0xFF6B7280), fontSize = 14.sp)
                        Icon(Icons.Default.ChevronRight, null, tint = Color(0xFF9CA3AF), modifier = Modifier.size(16.dp))
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Background (${coverPhotos.size}/4)", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF374151))
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (coverPhotos.size < 4) {
                                Box(
                                    modifier = Modifier.requiredSize(64.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFF3F4F6)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(Modifier.fillMaxSize().clickable { backgroundPicker.launch("image/*") }, contentAlignment = Alignment.Center) {
                                        Text("+", fontSize = 30.sp, color = Color(0xFFD1D5DB), fontWeight = FontWeight.Light)
                                    }
                                }
                            }
                            coverPhotos.take(4).forEach { photo ->
                                AsyncImage(model = photo, contentDescription = "Background", contentScale = ContentScale.Crop, modifier = Modifier.requiredSize(64.dp).clip(RoundedCornerShape(6.dp)))
                            }
                        }
                    }

                    EditRowItem("Bio", { showBioScreen = true }) {
                        Text(userBio.ifBlank { "Add bio..." }, color = Color(0xFF6B7280), fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 180.dp))
                        Icon(Icons.Default.ChevronRight, null, tint = Color(0xFF9CA3AF), modifier = Modifier.size(16.dp))
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Album Photos (${albumImages.size}/4)", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF374151))
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (albumImages.size < 4) {
                                Box(
                                    modifier = Modifier.requiredSize(64.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFF3F4F6)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(Modifier.fillMaxSize().clickable { albumPicker.launch("image/*") }, contentAlignment = Alignment.Center) {
                                        Text("+", fontSize = 30.sp, color = Color(0xFFD1D5DB), fontWeight = FontWeight.Light)
                                    }
                                }
                            }
                            albumImages.take(4).forEachIndexed { index, image ->
                                Box(modifier = Modifier.requiredSize(64.dp).clip(RoundedCornerShape(6.dp)).clickable { fullImageIndex = index }) {
                                    AsyncImage(model = profileAsset(image), contentDescription = "Album photo", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNameScreen) {
        ModalBottomSheet(
            onDismissRequest = { showNameScreen = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
            dragHandle = null
        ) {
            var tempName by remember { mutableStateOf(userName) }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { showNameScreen = false }, modifier = Modifier.size(24.dp)) { 
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF374151)) 
                    }
                    Text("Nickname", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827))
                    Spacer(modifier = Modifier.width(24.dp))
                }
                
                TextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF3F4F6),
                        unfocusedContainerColor = Color(0xFFF3F4F6),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    ),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { userName = tempName; showNameScreen = false; if (!isOtherUser) coroutineScope.launch { val fields = mapOf("name" to tempName, "displayName" to tempName, "userName" to tempName); ProfilePersistence.save(targetUser, fields); if (profileCacheKey.isNotBlank()) { val db = NativeIndexDatabase(context); val cached = db.loadProfile(profileCacheKey) ?: org.json.JSONObject(); fields.forEach { (key, value) -> cached.put(key, value) }; db.saveProfile(profileCacheKey, cached) } } },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DC4E9)),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Save", fontSize = 16.sp, color = Color.White) }
            }
        }
    }

    if (showBioScreen) {
        ModalBottomSheet(
            onDismissRequest = { showBioScreen = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
            dragHandle = null
        ) {
            var tempBio by remember { mutableStateOf(userBio) }
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Bio", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827))
                }
                
                TextField(
                    value = tempBio,
                    onValueChange = { tempBio = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(150.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF3F4F6),
                        unfocusedContainerColor = Color(0xFFF3F4F6),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    ),
                    placeholder = { Text("Write your bio here...", color = Color.Gray) },
                    shape = RoundedCornerShape(8.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = { userBio = tempBio; showBioScreen = false; if (!isOtherUser) coroutineScope.launch { val fields = mapOf("bio" to tempBio); ProfilePersistence.save(targetUser, fields); if (profileCacheKey.isNotBlank()) { val db = NativeIndexDatabase(context); val cached = db.loadProfile(profileCacheKey) ?: org.json.JSONObject(); fields.forEach { (key, value) -> cached.put(key, value) }; db.saveProfile(profileCacheKey, cached) } } },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DC4E9)),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Confirm", fontSize = 16.sp, color = Color.White) }
            }
        }
    }

    if (showAgeScreen) {
        ModalBottomSheet(
            onDismissRequest = { showAgeScreen = false }, 
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
            dragHandle = null
        ) {
            var selectedDay by remember { mutableStateOf(1) }
            var selectedMonth by remember { mutableStateOf(1) }
            var selectedYear by remember { mutableStateOf(2000) }

            val days = remember { (1..31).map { it.toString().padStart(2, '0') } }
            val months = remember { (1..12).map { it.toString().padStart(2, '0') } }
            val years = remember { (1900..2026).map { it.toString() } }

            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), 
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Cancel", color = Color.Gray, modifier = Modifier.clickable { showAgeScreen = false })
                    Text("Birthday", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(
                        "Confirm",
                        color = Color(0xFF1DC4E9),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            val today = java.time.LocalDate.now()
                            userAge = (today.year - selectedYear - if (today.monthValue < selectedMonth || (today.monthValue == selectedMonth && today.dayOfMonth < selectedDay)) 1 else 0).coerceIn(0, 120)
                            if (!isOtherUser) coroutineScope.launch { val fields = mapOf("age" to userAge.toString()); ProfilePersistence.save(targetUser, fields); if (profileCacheKey.isNotBlank()) { val db = NativeIndexDatabase(context); val cached = db.loadProfile(profileCacheKey) ?: org.json.JSONObject(); fields.forEach { (key, value) -> cached.put(key, value) }; db.saveProfile(profileCacheKey, cached) } }
                            showAgeScreen = false
                        }
                    )
                }
                
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .background(Color(0xFFF3F4F6))
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(), 
                        horizontalArrangement = Arrangement.SpaceEvenly, 
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        WheelPicker(
                            items = days,
                            modifier = Modifier.weight(1f),
                            startIndex = days.indexOf("01"),
                            onItemSelected = { _, item -> selectedDay = item.toInt() }
                        )
                        WheelPicker(
                            items = months,
                            modifier = Modifier.weight(1f),
                            startIndex = months.indexOf("01"),
                            onItemSelected = { _, item -> selectedMonth = item.toInt() }
                        )
                        WheelPicker(
                            items = years,
                            modifier = Modifier.weight(1f),
                            startIndex = years.indexOf("2000"),
                            onItemSelected = { _, item -> selectedYear = item.toInt() }
                        )
                    }
                }
            }
        }
    }

    if (fullImageIndex != null) {
        Dialog(onDismissRequest = { fullImageIndex = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            var scale by remember { mutableFloatStateOf(1f) }
            var offset by remember { mutableStateOf(Offset.Zero) }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            if (scale > 1f) {
                                offset += pan
                            } else {
                                offset = Offset.Zero
                            }
                        }
                    }
            ) {
                IconButton(onClick = { fullImageIndex = null }, modifier = Modifier.align(Alignment.TopStart).padding(16.dp).windowInsetsPadding(WindowInsets.statusBars)) {
                    Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                }

                AsyncImage(
                    model = profileAsset(albumImages[fullImageIndex!!]),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

// --- HELPER COMPOSABLES ---

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T> WheelPicker(
    items: List<T>,
    modifier: Modifier = Modifier,
    itemHeight: androidx.compose.ui.unit.Dp = 40.dp,
    visibleItemsCount: Int = 5,
    startIndex: Int = 0,
    onItemSelected: (index: Int, item: T) -> Unit
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = startIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val halfCount = visibleItemsCount / 2

    val paddedItems = remember(items) {
        val pad = List(halfCount) { null }
        pad + items + pad
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }.collect { index ->
            if (index in items.indices) {
                onItemSelected(index, items[index])
            }
        }
    }

    Box(modifier = modifier.height(itemHeight * visibleItemsCount)) {
        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            modifier = Modifier.fillMaxSize()
        ) {
            items(paddedItems.size) { index ->
                val item = paddedItems[index]
                Box(
                    modifier = Modifier
                        .height(itemHeight)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (item != null) {
                        val isSelected = index == listState.firstVisibleItemIndex + halfCount
                        Text(
                            text = item.toString(),
                            fontSize = if (isSelected) 20.sp else 16.sp,
                            color = if (isSelected) Color(0xFF111827) else Color.Gray,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CustomGenderIcon(isFemale: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val strokeWidth = 1.5.dp.toPx()
        if (isFemale) {
            val radius = size.width * 0.3f
            val cx = size.width / 2
            val cy = size.height * 0.35f
            drawCircle(color = Color.White, radius = radius, center = Offset(cx, cy), style = Stroke(width = strokeWidth))
            drawLine(color = Color.White, start = Offset(cx, cy + radius), end = Offset(cx, size.height), strokeWidth = strokeWidth)
            drawLine(color = Color.White, start = Offset(cx - radius * 0.8f, size.height * 0.75f), end = Offset(cx + radius * 0.8f, size.height * 0.75f), strokeWidth = strokeWidth)
        } else {
            val radius = size.width * 0.3f
            val cx = size.width * 0.35f
            val cy = size.height * 0.65f
            drawCircle(color = Color.White, radius = radius, center = Offset(cx, cy), style = Stroke(width = strokeWidth))
            
            val arrowStartX = cx + radius * 0.707f
            val arrowStartY = cy - radius * 0.707f
            val arrowEndX = size.width - strokeWidth/2
            val arrowEndY = strokeWidth/2
            
            drawLine(color = Color.White, start = Offset(arrowStartX, arrowStartY), end = Offset(arrowEndX, arrowEndY), strokeWidth = strokeWidth)
            drawLine(color = Color.White, start = Offset(arrowEndX, arrowEndY), end = Offset(arrowEndX - size.width * 0.4f, arrowEndY), strokeWidth = strokeWidth)
            drawLine(color = Color.White, start = Offset(arrowEndX, arrowEndY), end = Offset(arrowEndX, arrowEndY + size.height * 0.4f), strokeWidth = strokeWidth)
        }
    }
}

@Composable
private fun RealProfileChatIcon() {
    Canvas(modifier = Modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        val bubble = Path().apply {
            moveTo(w * 0.5f, h * 0.08f)
            cubicTo(w * 0.25f, h * 0.08f, w * 0.07f, h * 0.27f, w * 0.07f, h * 0.51f)
            cubicTo(w * 0.07f, h * 0.75f, w * 0.25f, h * 0.93f, w * 0.5f, h * 0.93f)
            cubicTo(w * 0.59f, h * 0.93f, w * 0.68f, h * 0.90f, w * 0.75f, h * 0.85f)
            lineTo(w * 0.93f, h * 0.94f)
            lineTo(w * 0.89f, h * 0.75f)
            cubicTo(w * 0.96f, h * 0.67f, w * 0.98f, h * 0.59f, w * 0.98f, h * 0.5f)
            cubicTo(w * 0.98f, h * 0.26f, w * 0.77f, h * 0.08f, w * 0.5f, h * 0.08f)
            close()
        }
        drawPath(path = bubble, color = Color.White)

        // Middle mein 3 transparent dots (Background color use karke fake cutout)
        val dotColor = Color(0xFF1DC4E9)
        val dotRadius = w * 0.08f
        val dotY = h * 0.48f
        val spacing = w * 0.22f

        drawCircle(color = dotColor, radius = dotRadius, center = Offset(w * 0.5f - spacing, dotY))
        drawCircle(color = dotColor, radius = dotRadius, center = Offset(w * 0.5f, dotY))
        drawCircle(color = dotColor, radius = dotRadius, center = Offset(w * 0.5f + spacing, dotY))
    }
}

@Composable
fun SectionHeader(title: String, subtitle: String = "") {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
        if (subtitle.isNotEmpty()) {
            Text(subtitle, fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
fun EditRowItem(title: String, onClick: () -> Unit, content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp), 
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.DarkGray)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            content()
        }
    }
}

@Composable
fun PublicProfile(
    user: RoomProfileUser,
    onBack: () -> Unit,
    onMessage: () -> Unit = {},
    currentUserId: String = ""
) {
    // The previous empty wrapper caused the public-profile route to render nothing.
    // Convert the room/user model into the screen model and render the actual UI.
    val target = remember(user) {
        TargetUser(
            id = user.id,
            uid = user.uid,
            name = user.name,
            displayAccountNumber = user.accountId?.takeIf { it.isNotBlank() } ?: user.id,
            accountId = user.accountId,
            photo = user.image,
            coverPhoto = user.coverPhoto,
            gender = user.gender,
            age = user.age,
            followers = user.followers,
            bio = user.bio,
            location = user.country,
            country = user.country,
            flag = user.flag,
            officialHeadTag = user.officialHeadTag,
            officialTag = user.officialTag,
            adminTag = user.adminTag,
            serviceTag = user.serviceTag,
            coinSellerTag = user.coinSellerTag,
            vipTag = user.vipTag,
            premiumTag = user.premiumTag
        )
    }
    PublicProfileScreen(
        onBack = onBack,
        isOtherUser = !(
            !currentUserId.isBlank() &&
            (user.uid == currentUserId || user.id == currentUserId || user.accountId == currentUserId)
        ),
        targetUser = target
    )
}

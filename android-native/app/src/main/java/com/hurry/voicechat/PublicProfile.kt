package com.hurry.voicechat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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

    // Vercel serves files inside the public/ directory from the URL root.
    // Using /public/<filename> returns 404, which made every profile badge,
    // gender-related asset, medal, and frame appear missing in the native app.
    val profileAsset: (String) -> String = { path ->
        if (path.isBlank()) ""
        else if (path.startsWith("http://") || path.startsWith("https://") || path.startsWith("data:")) path
        else {
            val cleanPath = path.trimStart('/').removePrefix("public/")
            "https://zyora-voice-chat.vercel.app/$cleanPath"
        }
    }

    // State Variables
    var coverPhotos by remember(targetUser?.coverPhoto) { 
        mutableStateOf(targetUser?.coverPhoto?.takeIf { it.isNotBlank() }?.let { listOf(profileAsset(it)) } ?: emptyList()) 
    }
    var albumImages by remember { mutableStateOf(listOf<String>()) }
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
    var userName by remember { mutableStateOf(targetUser?.name?.takeIf { it.isNotBlank() } ?: "User") }
    var userBio by remember { mutableStateOf(targetUser?.bio ?: "") }
    var userAge by remember { mutableStateOf(targetUser?.age ?: 0) }
    var userGender by remember { mutableStateOf(targetUser?.gender ?: "") }
    val displayAccNum = targetUser?.displayAccountNumber?.takeIf { it.isNotBlank() } ?: targetUser?.accountId?.takeIf { it.isNotBlank() } ?: "—"
    
    val userPhotoRaw = targetUser?.photo?.takeIf { it.isNotBlank() } ?: targetUser?.image?.takeIf { it.isNotBlank() } ?: ""
    val userPhoto = profileAsset(userPhotoRaw)
    
    val followers = targetUser?.followers ?: 0
    val flag = targetUser?.flag ?: "🇮🇳"
    val location = targetUser?.location ?: "India"

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
                        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                            AsyncImage(
                                model = coverPhotos[page],
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        // Pager Indicator
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 32.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            repeat(coverPhotos.size) { iteration ->
                                val color = if (pagerState.currentPage == iteration) Color.White else Color.White.copy(alpha = 0.5f)
                                val width = if (pagerState.currentPage == iteration) 16.dp else 6.dp
                                Box(
                                    modifier = Modifier
                                        .size(width = width, height = 6.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                )
                            }
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
                                Icon(Icons.Default.Warning, "Report", tint = Color.White)
                            }
                        } else {
                            IconButton(onClick = { showEditSheet = true }) {
                                Icon(Icons.Outlined.Edit, "Edit", tint = Color.White)
                            }
                        }
                    }

                    // Floating Avatar
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 24.dp)
                            .offset(y = (-16).dp) 
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF374151)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (userPhoto.isNotBlank()) {
                            AsyncImage(model = userPhoto, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        } else {
                            Text(avatarLetter, color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 2. Profile Info Section
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                        .background(Color.White)
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                        .padding(top = 4.dp)
                ) {
                    // Name & Badges using FlowRow (max 3 tags per row as requested)
                    FlowRow(
                        maxItemsInEachRow = 3,
                        verticalArrangement = Arrangement.Center,
                        horizontalArrangement = Arrangement.spacedBy(4.dp), 
                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                    ) {
                        Text(userName, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black, modifier = Modifier.align(Alignment.CenterVertically))
                        
                        val genderStr = userGender.trim()
                        val ageStr = if (userAge > 0) userAge.toString() else ""
                        val tagText = "$genderStr $ageStr".trim()

                        if (tagText.isNotEmpty()) {
                            val isFemale = genderStr.equals("female", ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterVertically)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isFemale) Color(0xFFFF5874) else Color(0xFF3B82F6))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(text = tagText, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Badges with requested sizes (30.dp for Official Head, 27.dp for others)
                        AsyncImage(model = profileAsset("/file_0000000096ec8211a137f37ef1cf1cd0.png"), contentDescription = "Official Head", contentScale = ContentScale.Fit, modifier = Modifier.height(30.dp).widthIn(max = 80.dp).align(Alignment.CenterVertically))
                        AsyncImage(model = profileAsset("/IMG_20261026_205516.png"), contentDescription = "Official", contentScale = ContentScale.Fit, modifier = Modifier.height(27.dp).widthIn(max = 80.dp).align(Alignment.CenterVertically))
                        AsyncImage(model = profileAsset("/file_00000000fc0c81f5ac4a79c1f7415756.png"), contentDescription = "Admin", contentScale = ContentScale.Fit, modifier = Modifier.height(27.dp).widthIn(max = 80.dp).align(Alignment.CenterVertically))
                        AsyncImage(model = profileAsset("/IMG_20261026_205203.png"), contentDescription = "Service / CS", contentScale = ContentScale.Fit, modifier = Modifier.height(27.dp).widthIn(max = 80.dp).align(Alignment.CenterVertically))
                        AsyncImage(model = profileAsset("/IMG_20261026_205346.png"), contentDescription = "Coin Seller", contentScale = ContentScale.Fit, modifier = Modifier.height(27.dp).widthIn(max = 80.dp).align(Alignment.CenterVertically))
                    }

                    // ID & Followers (ID image height updated to 25.dp)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        AsyncImage(
                            model = profileAsset("/file_0000000063b481fa89d3c7729265058b.png"),
                            contentDescription = "Purple ID Badge",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.height(25.dp).widthIn(min = 22.dp, max = 28.dp).padding(end = 4.dp)
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
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.height(24.dp).width(48.dp)) {
                            AsyncImage(model = profileAsset("/IMG_20260917_220530.png"), contentDescription = "Level", contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                            Text("Lv.1", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
                        }
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
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Albums
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

                    SectionHeader("Vehicle")
                    AsyncImage(model = profileAsset("/1785091443553.png"), contentDescription = "Vehicle", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(112.dp).clip(RoundedCornerShape(16.dp)))

                    SectionHeader("Medal")
                    AsyncImage(model = profileAsset("/1785091431545.png"), contentDescription = "Medal", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(112.dp).clip(RoundedCornerShape(16.dp)))

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
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Favorite, null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isFollowing) "Following" else "Follow", fontSize = 16.sp, fontWeight = FontWeight.Medium)
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
            shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp),
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
                    fontSize = 18.sp,
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
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { showActionSheet = false },
                    modifier = Modifier.fillMaxWidth(),
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
                    .fillMaxHeight(0.60f) 
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
                            modifier = Modifier.size(56.dp).clip(CircleShape).background(Color(0xFFE5E7EB)),
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
                                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFF3F4F6)),
                                    contentAlignment = Alignment.Center
                                ) { Text("+", fontSize = 30.sp, color = Color(0xFFD1D5DB), fontWeight = FontWeight.Light) }
                            }
                            coverPhotos.take(4).forEach { photo ->
                                AsyncImage(model = photo, contentDescription = "Background", contentScale = ContentScale.Crop, modifier = Modifier.size(64.dp).clip(RoundedCornerShape(6.dp)))
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
                                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFF3F4F6)),
                                    contentAlignment = Alignment.Center
                                ) { Text("+", fontSize = 30.sp, color = Color(0xFFD1D5DB), fontWeight = FontWeight.Light) }
                            }
                            albumImages.take(4).forEachIndexed { index, image ->
                                Box(modifier = Modifier.size(64.dp).clip(RoundedCornerShape(6.dp)).clickable { fullImageIndex = index }) {
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
                    onClick = { userName = tempName; showNameScreen = false },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DC4E9)),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Save", fontSize = 16.sp, color = Color.White) }
            }
        }
    }

    if (showAgeScreen) {
        ModalBottomSheet(onDismissRequest = { showAgeScreen = false }, containerColor = Color.White) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Cancel", color = Color.Gray, modifier = Modifier.clickable { showAgeScreen = false })
                    Text("Birthday", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Confirm", color = Color(0xFF1DC4E9), fontWeight = FontWeight.Bold, modifier = Modifier.clickable { showAgeScreen = false })
                }
                
                Row(modifier = Modifier.fillMaxWidth().height(200.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    Text("DD", fontSize = 24.sp, color = Color.Gray)
                    Text("MM", fontSize = 24.sp, color = Color.Gray)
                    Text("YYYY", fontSize = 24.sp, color = Color.Gray)
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
    val targetIdentities = listOfNotNull(user.uid, user.id, user.accountId).filter { it.isNotBlank() }
    val viewingOwnProfile = currentUserId.isNotBlank() && targetIdentities.any { it == currentUserId }

    PublicProfileScreen(
        onBack = onBack,
        isOtherUser = !viewingOwnProfile,
        targetUser = TargetUser(
            id = user.id,
            uid = user.uid,
            name = user.name,
            displayAccountNumber = user.accountId,
            accountId = user.accountId,
            photo = user.image,
            image = user.image,
            gender = user.gender,
            age = user.age,
            followers = user.followers,
            location = user.country,
            country = user.country,
            flag = user.flag,
            
            bio = user.bio,
            coverPhoto = user.coverPhoto,
            officialHeadTag = user.officialHeadTag,
            officialTag = user.officialTag,
            adminTag = user.adminTag,
            serviceTag = user.serviceTag,
            coinSellerTag = user.coinSellerTag,
            vipTag = user.vipTag,
            premiumTag = user.premiumTag
        )
    )
}

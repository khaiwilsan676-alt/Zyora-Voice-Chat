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
    val officialTag: Boolean? = null,
    val adminTag: Boolean? = null,
    val vipTag: Boolean? = null,
    val premiumTag: Boolean? = null
)

// Dummy constants for Country and Accounts
val COUNTRIES = listOf(
    Pair("IN", "🇮🇳 India"), Pair("US", "🇺🇸 United States")
)
val SPECIAL_ACCOUNTS = mapOf("HUSxSvQnabgU029dWYt1TUV04hd2" to "100002")

// --- MAIN SCREEN ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicProfileScreen(
    onBack: () -> Unit,
    onJoinRoom: (String) -> Unit = {},
    isOtherUser: Boolean = false,
    targetUser: TargetUser? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // State Variables
    var coverPhotos by remember { mutableStateOf(listOf<String>()) }
    var albumImages by remember { mutableStateOf(listOf<String>()) }
    var ownedFrames by remember { mutableStateOf(listOf<String>()) } // Dummy for UI
    var receivedGifts by remember { mutableStateOf(listOf<String>()) } // Dummy for UI

    var showEditSheet by remember { mutableStateOf(false) }
    var showNameScreen by remember { mutableStateOf(false) }
    var showAgeScreen by remember { mutableStateOf(false) }
    var showBioScreen by remember { mutableStateOf(false) }
    var showActionSheet by remember { mutableStateOf(false) }
    var showChat by remember { mutableStateOf(false) }

    var fullImageIndex by remember { mutableStateOf<Int?>(null) }
    var isFollowing by remember { mutableStateOf(false) }

    // User Data State (Mocked for UI)
    var userName by remember { mutableStateOf(targetUser?.name ?: "Zyora User") }
    var userBio by remember { mutableStateOf(targetUser?.bio ?: "") }
    var userAge by remember { mutableStateOf(targetUser?.age ?: 22) }
    var userGender by remember { mutableStateOf(targetUser?.gender ?: "♂") }
    val displayAccNum = targetUser?.displayAccountNumber ?: "100379620"
    val userPhoto = targetUser?.photo ?: ""
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
                                Icon(Icons.Default.Edit, "Edit", tint = Color.White)
                            }
                        }
                    }

                    // Floating Avatar Overlap
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 24.dp)
                            .offset(y = 48.dp) // Shifts avatar downwards to overlap
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF374151)) // gray-700
                            .border(4.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (userPhoto.isNotBlank()) {
                            AsyncImage(model = userPhoto, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        } else {
                            Text(avatarLetter, color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        // Placeholder for WhiteColorRemovalShader (Frame overlap)
                        // AsyncImage(model = "frame_url_here", modifier = Modifier.fillMaxSize().scale(1.5f))
                    }
                }
            }

            // 2. Profile Info Section
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .background(Color.White)
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .padding(top = 24.dp) // Space for overlapping avatar
                ) {
                    // Name & Badges
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(userName, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF3B82F6))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("$userGender $userAge", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // ID & Followers
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        Text("ID:$displayAccNum", fontSize = 12.sp, color = Color.Gray)
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

                    // Location & Bio
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp)) {
                        Icon(Icons.Default.LocationOn, "Location", tint = Color.Gray, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("$flag $location", fontSize = 14.sp, color = Color.DarkGray)
                    }

                    Row(modifier = Modifier.padding(top = 8.dp), verticalAlignment = Alignment.Top) {
                        if (!isOtherUser) {
                            Icon(Icons.Default.Edit, "Edit Bio", tint = Color.Gray, modifier = Modifier.size(16.dp).clickable { showEditSheet = true })
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
                            .border(bottom = BorderStroke(1.dp, Color(0xFFE5E7EB)), shape = RectangleShape)
                            .padding(bottom = 8.dp)
                    ) {
                        Text("Profile", color = Color(0xFF3B82F6), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Box(modifier = Modifier.align(Alignment.BottomCenter).offset(y = 9.dp).width(16.dp).height(4.dp).clip(CircleShape).background(Color(0xFF3B82F6)))
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
                                    model = albumImages[idx],
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.aspectRatio(1f).clip(RoundedCornerShape(12.dp)).background(Color.LightGray).clickable { fullImageIndex = idx }
                                )
                            }
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFFF3F4F6)))
                    }

                    // Single Image Sections
                    SectionHeader("Vehicle")
                    Box(modifier = Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFFF3F4F6)))

                    SectionHeader("Medal")
                    Box(modifier = Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFFF3F4F6)))
                    
                    SectionHeader("Frame")
                    Box(modifier = Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFFF3F4F6)))
                    
                    SectionHeader("Gift")
                    Box(modifier = Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFFF3F4F6)))
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
                    Icon(Icons.Default.MailOutline, null, tint = Color.White) // Chat icon
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Chat", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }

    // --- OVERLAYS & BOTTOM SHEETS ---

    // Edit Profile Sheet
    if (!isOtherUser && showEditSheet) {
        ModalBottomSheet(
            onDismissRequest = { showEditSheet = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp).padding(bottom = 32.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { showEditSheet = false }) { Icon(Icons.Default.ArrowBack, null) }
                    Text("Edit Information", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(48.dp))
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Avatar Edit
                EditRowItem("Avatar", { /* Trigger Image Picker */ }) {
                    Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(Color.Gray))
                }
                
                // Nickname Edit
                EditRowItem("Nickname", { showNameScreen = true }) {
                    Text(userName, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Icon(Icons.Default.KeyboardArrowRight, null, tint = Color.Gray)
                }
                
                // Age Edit
                EditRowItem("Age", { showAgeScreen = true }) {
                    Text(userAge.toString(), color = Color.Gray)
                    Icon(Icons.Default.KeyboardArrowRight, null, tint = Color.Gray)
                }
                
                // Bio Edit
                EditRowItem("Bio", { showBioScreen = true }) {
                    Text(userBio.ifBlank { "Add bio..." }, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 120.dp))
                    Icon(Icons.Default.KeyboardArrowRight, null, tint = Color.Gray)
                }

                // Background (Covers)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Background (${coverPhotos.size}/4)", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Row(modifier = Modifier.padding(top = 8.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFF3F4F6)).clickable { /* pick image */ }, contentAlignment = Alignment.Center) {
                        Text("+", fontSize = 24.sp, color = Color.Gray)
                    }
                    coverPhotos.forEach { photo ->
                        AsyncImage(model = photo, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)))
                    }
                }
            }
        }
    }

    // Name Edit Screen (Dialog for full screen illusion)
    if (showNameScreen) {
        Dialog(onDismissRequest = { showNameScreen = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            var tempName by remember { mutableStateOf(userName) }
            Column(modifier = Modifier.fillMaxSize().background(Color.White).windowInsetsPadding(WindowInsets.statusBars)) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    IconButton(onClick = { showNameScreen = false }) { Icon(Icons.Default.ArrowBack, null) }
                    Text("Nickname", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterVertically))
                    Spacer(modifier = Modifier.width(48.dp))
                }
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(12.dp)
                )
                Button(
                    onClick = { userName = tempName; showNameScreen = false },
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DC4E9))
                ) { Text("Save", fontSize = 16.sp) }
            }
        }
    }

    // Age / Birthday Wheel Picker Bottom Sheet
    if (showAgeScreen) {
        ModalBottomSheet(onDismissRequest = { showAgeScreen = false }, containerColor = Color.White) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Cancel", color = Color.Gray, modifier = Modifier.clickable { showAgeScreen = false })
                    Text("Birthday", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Confirm", color = Color(0xFF1DC4E9), fontWeight = FontWeight.Bold, modifier = Modifier.clickable { showAgeScreen = false })
                }
                
                // Simple Wheel Picker placeholder layout
                Row(modifier = Modifier.fillMaxWidth().height(200.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    Text("DD", fontSize = 24.sp, color = Color.Gray)
                    Text("MM", fontSize = 24.sp, color = Color.Gray)
                    Text("YYYY", fontSize = 24.sp, color = Color.Gray)
                }
            }
        }
    }

    // Full Image Viewer
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
                    model = albumImages[fullImageIndex!!],
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
            .padding(vertical = 12.dp),
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
    PublicProfileScreen(
        onBack = onBack,
        isOtherUser = true,
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
            flag = user.flag
        )
    )
}

package com.hurry.voicechat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// --- Helper Data Classes & Functions ---

typealias RoomProfileUser = RoomUser

data class RoomUser(
    val id: String? = null,
    val uid: String? = null,
    val accountId: String? = null,
    val name: String = "User",
    val image: String = "",
    val gender: String = "",
    val age: Int = 0,
    val country: String = "India",
    val flag: String = "🇮🇳",
    val bio: String = "",
    val followers: Int = 0,
    val isFollowing: Boolean = false,
    val isOnline: Boolean = true,
    val isInSeat: Boolean = false,
    val isMuted: Boolean = false,
    val isLocked: Boolean = false,
    // Public-profile assets and badges; defaults preserve existing room-profile callers.
    val coverPhoto: String? = null,
    val officialHeadTag: Boolean? = null,
    val officialTag: Boolean? = null,
    val adminTag: Boolean? = null,
    val serviceTag: Boolean? = null,
    val coinSellerTag: Boolean? = null,
    val vipTag: Boolean? = null,
    val premiumTag: Boolean? = null
)

fun generateStableId(input: String): String {
    return input // Placeholder for your actual hash/id generation logic
}

// --- Shader Placeholders ---
// Note: Actual WebGL shaders require GLSurfaceView or AGSL (RuntimeShader) in Android.
// We use these placeholders where you can integrate ExoPlayer/SVGA or ColorMatrix filters later.

@Composable
fun GreenColorRemovalShader(imageSrc: String, modifier: Modifier = Modifier) {
    AsyncImage(
        model = imageSrc,
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.Fit
    )
}

@Composable
fun WhiteColorRemovalShader(imageSrc: String, modifier: Modifier = Modifier) {
    AsyncImage(
        model = imageSrc,
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.Fit
    )
}

// --- Main RoomProfile Component ---

@Composable
fun RoomProfile(
    user: RoomUser,
    isCurrentUser: Boolean = false,
    isRoomOwner: Boolean = false,
    onClose: () -> Unit,
    onCopyId: () -> Unit = {},
    onLeaveSeat: () -> Unit = {},
    onMention: (String) -> Unit = {},
    onFollow: () -> Unit = {},
    onMessage: () -> Unit = {},
    onThirdAction: () -> Unit = {},
    onMute: () -> Unit = {},
    onLock: () -> Unit = {},
    onKickOut: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val profileCacheKeys = listOfNotNull(user.uid, user.id, user.accountId).filter { it.isNotBlank() }.distinct()
    val profileCacheKey = profileCacheKeys.firstOrNull().orEmpty()
    val cachedProfile = remember(profileCacheKeys) { val db = NativeIndexDatabase(context); profileCacheKeys.firstNotNullOfOrNull { db.loadProfile(it) } }

    var displayName by remember(user.uid, user.id, user.accountId, user.name, cachedProfile) { mutableStateOf(cachedProfile?.optString("name")?.takeIf { it.isNotBlank() && it != "null" } ?: user.name.ifBlank { "User" }) }
    var displayImage by remember(user.uid, user.id, user.accountId, user.image, cachedProfile) { mutableStateOf(sequenceOf("image", "photo", "photoURL", "avatar", "picture").map { cachedProfile?.optString(it, "").orEmpty() }.firstOrNull { it.isNotBlank() && it != "null" } ?: user.image) }
    var displayGender by remember(user.uid, user.id, user.accountId, user.gender, cachedProfile) { mutableStateOf(cachedProfile?.optString("gender")?.takeIf { it.isNotBlank() && it != "null" } ?: user.gender) }
    var displayAge by remember(user.uid, user.id, user.accountId, user.age, cachedProfile) { mutableIntStateOf(if (cachedProfile?.has("age") == true) cachedProfile.optInt("age", user.age) else user.age) }
    var displayBio by remember(user.uid, user.id, user.accountId, user.bio, cachedProfile) { mutableStateOf(cachedProfile?.optString("bio")?.takeIf { it != "null" } ?: user.bio) }

    LaunchedEffect(user.uid, user.id, user.accountId, profileCacheKeys) {
        val profileDb = NativeIndexDatabase(context)
        val cached = profileCacheKeys.firstNotNullOfOrNull { profileDb.loadProfile(it) }
        val remote = ProfilePersistence.load(TargetUser(
            id = user.id, uid = user.uid, accountId = user.accountId,
            name = user.name, photo = user.image, image = user.image,
            gender = user.gender, age = user.age, bio = user.bio
        ))
        if (cached == null && remote == null) return@LaunchedEffect

        // Cache all non-name/DP fields once. Always refresh only name and avatar from MongoDB.
        val merged = org.json.JSONObject(cached?.toString() ?: "{}")
        if (cached == null && remote != null) {
            val keys = remote.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                if (key !in setOf("name", "displayName", "userName", "image", "photo", "photoURL", "avatar", "picture")) merged.put(key, remote.opt(key))
            }
        }
        remote?.let { latest ->
            sequenceOf("name", "displayName", "userName", "image", "photo", "photoURL", "avatar", "picture").forEach { key ->
                val value = latest.optString(key, "")
                if (value.isNotBlank() && value != "null") merged.put(key, value)
            }
        }
        profileCacheKeys.forEach { profileDb.saveProfile(it, merged) }
        remote?.optString("name")?.takeIf { it.isNotBlank() && it != "null" }?.let { displayName = it }
        remote?.let { latest ->
            sequenceOf("image", "photo", "photoURL", "avatar", "picture").map { latest.optString(it, "") }
                .firstOrNull { it.isNotBlank() && it != "null" }?.takeIf { it != displayImage }?.let { displayImage = it }
        }
        val stableDetails = cached ?: remote
        stableDetails?.optString("gender")?.takeIf { it.isNotBlank() && it != "null" }?.let { displayGender = it }
        if (stableDetails?.has("age") == true) displayAge = stableDetails.optInt("age", displayAge)
        stableDetails?.optString("bio")?.takeIf { it != "null" }?.let { displayBio = it }
    }

    // Tag States
    var adminTag by remember(user.adminTag, cachedProfile) { mutableStateOf(user.adminTag == true || cachedProfile?.optBoolean("adminTag", false) == true) }
    var officialTag by remember(user.officialTag, cachedProfile) { mutableStateOf(user.officialTag == true || cachedProfile?.optBoolean("officialTag", false) == true) }
    var officialHeadTag by remember(user.officialHeadTag, cachedProfile) { mutableStateOf(user.officialHeadTag == true || cachedProfile?.optBoolean("officialHeadTag", false) == true) }
    var serviceTag by remember(user.serviceTag) { mutableStateOf(user.serviceTag == true) }
    var coinSellerTag by remember(user.coinSellerTag) { mutableStateOf(user.coinSellerTag == true) }
    var vipTag by remember(user.vipTag) { mutableStateOf(user.vipTag == true) }
    var premiumTag by remember(user.premiumTag) { mutableStateOf(user.premiumTag == true) }
    var showReports by remember { mutableStateOf(false) }

    LaunchedEffect(user.uid, user.id, user.accountId, cachedProfile) {
        val profile = ProfilePersistence.load(
            TargetUser(id = user.id, uid = user.uid, accountId = user.accountId, name = user.name, photo = user.image, image = user.image)
        ) ?: cachedProfile ?: return@LaunchedEffect
        if (profile.has("officialHeadTag")) officialHeadTag = profile.optBoolean("officialHeadTag")
        if (profile.has("officialTag")) officialTag = profile.optBoolean("officialTag")
        if (profile.has("adminTag")) adminTag = profile.optBoolean("adminTag")
        if (profile.has("serviceTag")) serviceTag = profile.optBoolean("serviceTag")
        if (profile.has("coinSellerTag")) coinSellerTag = profile.optBoolean("coinSellerTag")
        if (profile.has("vipTag")) vipTag = profile.optBoolean("vipTag")
        if (profile.has("premiumTag")) premiumTag = profile.optBoolean("premiumTag")
    }



    val hasAnyTag = officialHeadTag || officialTag || adminTag || serviceTag || coinSellerTag || vipTag || premiumTag
    val accountId = generateStableId(user.accountId ?: user.id ?: user.uid ?: "User")
    
    val genderColor = if (displayGender.equals("female", true) || displayGender.equals("f", true) || displayGender == "♀") {
        Color(0xFFEC4899) // pink-500
    } else {
        Color(0xFF3B82F6) // blue-500
    }

    val showLeaveSeat = isCurrentUser && user.isInSeat
    val showModerationRow = isRoomOwner && !isCurrentUser
    val showActions = !isCurrentUser

    // Fetch Tags
    LaunchedEffect(user.uid, user.id, user.accountId) {
        val uid = user.uid ?: user.id ?: user.accountId
        if (uid != null && uid != "N/A" && uid != "User") {
            try {
                // Placeholder for actual API call
                // val response = fetch("your_api/users?uid=$uid")
                // Parsing logic...
                // adminTag = data.adminTag; etc...
            } catch (e: Exception) {
                // Handle error quietly
            }
        }
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
                .clickable { onClose() }
        ) {
            // Sheet Container (Aligns to bottom)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .clickable(enabled = false) {} // Consume clicks so it doesn't close
            ) {
                // White Background Box (Starts 48dp below to leave room for avatar overlap)
                Column(
                    modifier = Modifier
                        .padding(top = 44.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .background(Color.White)
                        .padding(top = 56.dp, bottom = 24.dp, start = 16.dp, end = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    
                    // Row 1: Name + Gender Tag
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Text(
                            text = displayName.ifBlank { "User" },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF111827)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(genderColor)
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                CustomGenderIcon(isFemale = displayGender.equals("female", true) || displayGender.equals("f", true) || displayGender == "♀", modifier = Modifier.size(8.dp))
                                Text(displayAge.toString(), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Row 2: Public Profile tags (keep each badge's established size)
                    if (hasAnyTag) {
                        Row(
                            modifier = Modifier.padding(top = 6.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (officialHeadTag) AsyncImage(model = "file_00000000ef2c82118b2266596d0e992d.png".fullUrl(), contentDescription = "Official Head", contentScale = ContentScale.Fit, modifier = Modifier.height(31.dp).widthIn(max = 80.dp))
                            if (officialTag) AsyncImage(model = "IMG_20261027_102250.png".fullUrl(), contentDescription = "Official", contentScale = ContentScale.Fit, modifier = Modifier.height(22.dp).widthIn(max = 60.dp))
                            if (adminTag) AsyncImage(model = "IMG_20261027_114117.png".fullUrl(), contentDescription = "Admin", contentScale = ContentScale.Fit, modifier = Modifier.height(22.dp).widthIn(max = 60.dp))
                            if (serviceTag) AsyncImage(model = "IMG_20261027_102207.png".fullUrl(), contentDescription = "Service / CS", contentScale = ContentScale.Fit, modifier = Modifier.height(22.dp).widthIn(max = 60.dp))
                            if (coinSellerTag) AsyncImage(model = "IMG_20261027_105359.png".fullUrl(), contentDescription = "Coin Seller", contentScale = ContentScale.Fit, modifier = Modifier.height(22.dp).widthIn(max = 60.dp))
                        }
                    }

                    // Row 3: Level Badge
                    Row(
                        modifier = Modifier.padding(top = 6.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            AsyncImage(model = "IMG_20261028_004355.png".fullUrl(), contentDescription = "Level", modifier = Modifier.height(32.dp))
                            Text("1", color = Color(0xFFF5DDA0), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center))
                        }
                        Spacer(modifier = Modifier.width(2.dp))
                        AsyncImage(model = "1785486414756.png".fullUrl(), contentDescription = null, modifier = Modifier.height(24.dp))
                    }

                    // Row 4: ID | Fans | Flag
                    Row(
                        modifier = Modifier.padding(top = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        AsyncImage(
                            model = "file_0000000063b481fa89d3c7729265058b.png".fullUrl(),
                            contentDescription = "Purple ID Badge",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.height(30.dp).widthIn(min = 22.dp, max = 35.dp).padding(end = 4.dp)
                        )
                        Text(accountId, color = Color.DarkGray, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 4.dp))
                        if (accountId != "N/A" && accountId != "User") {
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("ID", accountId))
                                    Toast.makeText(context, "ID Copied!", Toast.LENGTH_SHORT).show()
                                    onCopyId()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color.Gray, modifier = Modifier.size(14.dp))
                            }
                        }
                        
                        Text(" | ", color = Color.LightGray, modifier = Modifier.padding(horizontal = 4.dp))
                        
                        Text("${user.followers}", color = Color.DarkGray, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text(" Fans", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(start = 4.dp))
                        
                        Text(" | ", color = Color.LightGray, modifier = Modifier.padding(horizontal = 4.dp))
                        
                        Text(user.flag, fontSize = 16.sp)
                    }

                    // Action Buttons (Leave Seat OR Social Actions)
                    if (showLeaveSeat) {
                        Button(
                            onClick = onLeaveSeat,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                            shape = CircleShape
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Leave", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (showActions) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Follow
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onFollow() }) {
                                Icon(Icons.Default.Favorite, null, tint = Color(0xFFEC4899), modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (user.isFollowing) "Following" else "Follow", color = Color(0xFFEC4899), fontWeight = FontWeight.Medium)
                            }
                            
                            // Chat
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onMessage() }) {
                                Icon(Icons.Default.MailOutline, null, tint = Color.DarkGray, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Chat", color = Color.DarkGray, fontWeight = FontWeight.Medium)
                            }
                            
                            // 3rd Action Icon
                            AsyncImage(
                                model = "file_000000008e508208b1353ae33e2abef9.png".fullUrl(),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .clickable { onThirdAction() }
                            )
                        }

                        // Moderation Row (Mute, Leave, Lock, Kick)
                        if (showModerationRow) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(if (user.isMuted) "Unmute" else "Mute", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.clickable { onMute() })
                                Text("|", color = Color.LightGray)
                                Text("Leave", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.clickable { onLeaveSeat() })
                                Text("|", color = Color.LightGray)
                                Text(if (user.isLocked) "Unlock" else "Lock", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.clickable { onLock() })
                                Text("|", color = Color.LightGray)
                                Text("Kick out", color = Color.Red, fontSize = 14.sp, modifier = Modifier.clickable { onKickOut() })
                            }
                        }
                    }
                }

                // Overlapping Avatar (Placed outside the white column to break boundaries)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .size(88.dp) // Total 22 units equivalent (88dp)
                ) {
                    AsyncImage(
                        model = displayImage.ifBlank { "default-avatar.png".fullUrl() },
                        contentDescription = displayName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color.LightGray)
                    )
                    
                }

                // Corner Action Icons (Top Left & Top Right above white sheet)
                if (!isCurrentUser) {
                    IconButton(
                        onClick = { showReports = true },
                        modifier = Modifier.align(Alignment.TopStart).padding(top = 52.dp, start = 8.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = "Report", tint = Color.DarkGray)
                    }

                    IconButton(
                        onClick = { onMention(user.name) },
                        modifier = Modifier.align(Alignment.TopEnd).padding(top = 52.dp, end = 8.dp)
                    ) {
                        Icon(Icons.Default.AlternateEmail, contentDescription = "Mention", tint = Color.DarkGray)
                    }
                }
            }
        }
    }
    
    if (showReports) {
        var selectedReason by remember { mutableStateOf("Harassment") }
        AlertDialog(
            onDismissRequest = { showReports = false },
            title = { Text("Report ${user.name}", color = Color(0xFF111827)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Choose a reason", color = Color.Gray, fontSize = 13.sp)
                    listOf("Harassment", "Inappropriate content", "Fake account", "Other").forEach { reason ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { selectedReason = reason }.padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = selectedReason == reason, onClick = { selectedReason = reason })
                            Text(reason, color = Color(0xFF374151), fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    Toast.makeText(context, "Report submitted", Toast.LENGTH_SHORT).show()
                    showReports = false
                }) { Text("Submit", color = Color(0xFF3B82F6)) }
            },
            dismissButton = {
                TextButton(onClick = { showReports = false }) { Text("Cancel") }
            }
        )
    }
}

// Extension to simulate fullUrl resolving
private fun String.fullUrl(): String {
    val baseUrl = "https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/"
    return if (this.startsWith("http")) this else baseUrl + this.removePrefix("/")
}

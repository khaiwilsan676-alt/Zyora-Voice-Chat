package com.hurry.voicechat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
    val country: String = "",
    val countryCode: String = "",
    val flag: String = "",
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


internal suspend fun loadRoomProfileCountry(
    context: Context,
    accountId: String,
    name: String,
    image: String,
    fallbackCountry: String = ""
): String {
    if (accountId.isBlank()) return fallbackCountry
    val cached = runCatching { NativeIndexDatabase(context).loadProfile(accountId) }.getOrNull()
    val remote = runCatching {
        ProfilePersistence.load(TargetUser(
            id = accountId, uid = accountId, accountId = accountId,
            name = name, image = image, country = fallbackCountry
        ))
    }.getOrNull()
    val merged = org.json.JSONObject(cached?.toString() ?: "{}")
    remote?.let { profile ->
        sequenceOf("flag", "countryName", "country", "countryCode", "country_code", "nationality", "location").forEach { key ->
            val value = profile.optString(key, "").trim()
            if (value.isNotBlank() && value != "null") merged.put(key, value)
        }
    }
    return sequenceOf(
        merged.optString("country", ""),
        merged.optString("countryName", ""),
        merged.optString("countryCode", ""),
        merged.optString("country_code", ""),
        merged.optString("flag", ""),
        merged.optString("nationality", ""),
        merged.optString("location", ""),
        fallbackCountry
    ).map { it.trim() }.firstOrNull { it.isNotBlank() && it != "null" }.orEmpty()
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
    onMention: (String, String) -> Unit = { _, _ -> },
    onFollow: () -> Unit = {},
    onMessage: () -> Unit = {},
    onThirdAction: () -> Unit = {},
    onOpenPublicProfile: (RoomUser) -> Unit = {},
    onMute: () -> Unit = {},
    onLock: () -> Unit = {},
    onKickOut: () -> Unit = {},
    initialReportOpen: Boolean = false
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val profileCacheKeys = listOfNotNull(user.uid, user.id, user.accountId).filter { it.isNotBlank() }.distinct()
    val profileCacheKey = profileCacheKeys.firstOrNull().orEmpty()
    val cachedProfile = remember(profileCacheKeys) { val db = NativeIndexDatabase(context); profileCacheKeys.firstNotNullOfOrNull { db.loadProfile(it) } }
    var loadedProfile by remember(profileCacheKeys) { mutableStateOf(cachedProfile) }

    var displayName by remember(user.uid, user.id, user.accountId, user.name, cachedProfile) { mutableStateOf(cachedProfile?.optString("name")?.takeIf { it.isNotBlank() && it != "null" } ?: user.name.ifBlank { "User" }) }
    var displayImage by remember(user.uid, user.id, user.accountId, user.image, cachedProfile) { mutableStateOf(sequenceOf("image", "avatar", "profileImageUrl").map { cachedProfile?.optString(it, "").orEmpty() }.firstOrNull { it.isNotBlank() && it != "null" } ?: user.image) }
    val displayProfileImage = displayImage
    var displayGender by remember(user.uid, user.id, user.accountId, user.gender, cachedProfile) { mutableStateOf(cachedProfile?.optString("gender")?.takeIf { it.isNotBlank() && it != "null" } ?: user.gender) }
    var displayAge by remember(user.uid, user.id, user.accountId, user.age, cachedProfile) { mutableIntStateOf(if (cachedProfile?.has("age") == true) cachedProfile.optInt("age", user.age) else user.age) }
    var displayBio by remember(user.uid, user.id, user.accountId, user.bio, cachedProfile) { mutableStateOf(cachedProfile?.optString("bio")?.takeIf { it != "null" } ?: user.bio) }
    var displayCountry by remember(user.uid, user.id, user.accountId, user.country, cachedProfile) { mutableStateOf(sequenceOf(cachedProfile?.optString("country", ""), cachedProfile?.optString("countryName", ""), cachedProfile?.optString("countryCode", ""), cachedProfile?.optString("country_code", ""), cachedProfile?.optString("flag", ""), user.country, user.countryCode, user.flag).map { it?.trim().orEmpty() }.firstOrNull { it.isNotBlank() && it != "null" }.orEmpty()) }

    LaunchedEffect(user.uid, user.id, user.accountId, profileCacheKeys) {
        val profileDb = NativeIndexDatabase(context)
        val cached = profileCacheKeys.firstNotNullOfOrNull { profileDb.loadProfile(it) }
        val remote = ProfilePersistence.load(TargetUser(
            id = user.id, uid = user.uid, accountId = user.accountId,
            name = user.name, photo = user.image, image = user.image,
            gender = user.gender, age = user.age, bio = user.bio, country = user.country, countryCode = user.countryCode
        ))
        if (cached == null && remote == null) return@LaunchedEffect

        val merged = org.json.JSONObject(cached?.toString() ?: "{}")
        if (cached == null && remote != null) {
            val keys = remote.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                if (key !in setOf("name", "displayName", "userName", "image", "avatar", "profileImageUrl")) merged.put(key, remote.opt(key))
            }
        }
        remote?.let { latest ->
            sequenceOf("name", "displayName", "userName", "nickname", "image", "avatar", "profileImageUrl", "flag", "country", "countryName", "countryCode", "country_code", "nationality", "location").forEach { key ->
                val value = latest.optString(key, "").trim()
                val isPlaceholderName = key in setOf("name", "displayName", "userName", "nickname") && value.equals("User", ignoreCase = true)
                val isPlaceholderAvatar = key in setOf("image", "avatar", "profileImageUrl") &&
                    (value.endsWith("/default-avatar.png") || value == "default-avatar.png")
                if (value.isNotBlank() && value != "null" && !isPlaceholderName && (!isPlaceholderAvatar || key == "image")) merged.put(key, value)
            }
        }
        profileCacheKeys.forEach { profileDb.saveProfile(it, merged) }
        loadedProfile = merged
        remote?.let { latest -> sequenceOf("name", "displayName", "userName", "nickname").map { latest.optString(it, "").trim() }.firstOrNull { it.isNotBlank() && it != "null" && !it.equals("User", ignoreCase = true) }?.let { displayName = it } }
        remote?.let { latest ->
            sequenceOf("image", "avatar", "profileImageUrl").map { latest.optString(it, "").trim() }
                .firstOrNull { it.isNotBlank() && it != "null" }?.takeIf { it != displayImage }?.let { displayImage = it }
        }
        sequenceOf("country", "countryName", "countryCode", "country_code", "nationality", "location").map { merged.optString(it, "").trim() }.firstOrNull { it.isNotBlank() && it != "null" }?.let { displayCountry = it }
        val stableDetails = merged
        stableDetails?.optString("gender")?.takeIf { it.isNotBlank() && it != "null" }?.let { displayGender = it }
        if (stableDetails?.has("age") == true) displayAge = stableDetails.optInt("age", displayAge)
        stableDetails?.optString("bio")?.takeIf { it != "null" }?.let { displayBio = it }
    }

    val isCoreOfficialAccount = user.accountId == "100002" || user.accountId == "100003" || user.id == "100002" || user.id == "100003"
    val isRedOfficialAccount = user.accountId == "500001" || user.id == "500001"
    val is500002OfficialAccount = user.accountId in setOf("500002", "500003", "500004") || user.id in setOf("500002", "500003", "500004")
    var adminTag by remember(user.adminTag, cachedProfile, isCoreOfficialAccount) { mutableStateOf(isCoreOfficialAccount || user.adminTag == true || cachedProfile?.optBoolean("adminTag", false) == true) }
    var officialTag by remember(user.officialTag, cachedProfile, isCoreOfficialAccount, isRedOfficialAccount, is500002OfficialAccount) { mutableStateOf(isCoreOfficialAccount || isRedOfficialAccount || is500002OfficialAccount || user.officialTag == true || cachedProfile?.optBoolean("officialTag", false) == true) }
    var officialHeadTag by remember(user.officialHeadTag, cachedProfile, isCoreOfficialAccount) { mutableStateOf(isCoreOfficialAccount || user.officialHeadTag == true || cachedProfile?.optBoolean("officialHeadTag", false) == true) }
    var serviceTag by remember(user.serviceTag, cachedProfile, isCoreOfficialAccount) { mutableStateOf(isCoreOfficialAccount || user.serviceTag == true || cachedProfile?.optBoolean("serviceTag", false) == true) }
    var coinSellerTag by remember(user.coinSellerTag, cachedProfile, isCoreOfficialAccount) { mutableStateOf(isCoreOfficialAccount || user.coinSellerTag == true || cachedProfile?.optBoolean("coinSellerTag", false) == true) }
    var hostTag by remember(cachedProfile, isCoreOfficialAccount) { mutableStateOf(isCoreOfficialAccount || cachedProfile?.optBoolean("hostTag", false) == true || cachedProfile?.optBoolean("isHost", false) == true) }
    var vipTag by remember(user.vipTag) { mutableStateOf(user.vipTag == true) }
    var premiumTag by remember(user.premiumTag) { mutableStateOf(user.premiumTag == true) }
    var showReports by remember(initialReportOpen) { mutableStateOf(initialReportOpen) }
    
    // New State for the KickOut dialog
    var showKickDialog by remember { mutableStateOf(false) }

    LaunchedEffect(user.uid, user.id, user.accountId, loadedProfile) {
        val profile = loadedProfile ?: return@LaunchedEffect
        if (profile.has("officialHeadTag")) officialHeadTag = isCoreOfficialAccount || profile.optBoolean("officialHeadTag")
        if (profile.has("officialTag")) officialTag = isCoreOfficialAccount || isRedOfficialAccount || profile.optBoolean("officialTag")
        if (profile.has("adminTag")) adminTag = isCoreOfficialAccount || profile.optBoolean("adminTag")
        if (profile.has("serviceTag")) serviceTag = isCoreOfficialAccount || profile.optBoolean("serviceTag")
        if (profile.has("coinSellerTag")) coinSellerTag = isCoreOfficialAccount || profile.optBoolean("coinSellerTag")
        hostTag = isCoreOfficialAccount || profile.optBoolean("hostTag", false) || profile.optBoolean("isHost", false)
        if (profile.has("vipTag")) vipTag = profile.optBoolean("vipTag")
        if (profile.has("premiumTag")) premiumTag = profile.optBoolean("premiumTag")
    }

    val hasAnyTag = officialHeadTag || officialTag || adminTag || serviceTag || hostTag || coinSellerTag || vipTag || premiumTag
    val accountId = generateStableId(user.accountId ?: user.id ?: user.uid ?: "User")
    
    val genderColor = if (displayGender.equals("female", true) || displayGender.equals("f", true) || displayGender == "♀") {
        Color(0xFFEC4899)
    } else {
        Color(0xFF3B82F6)
    }

    val showLeaveSeat = isCurrentUser && user.isInSeat
    val showModerationRow = isRoomOwner && !isCurrentUser
    val showActions = !isCurrentUser

    var isFollowing by remember(user.uid, user.id, user.accountId) { mutableStateOf(user.isFollowing) }
    LaunchedEffect(user.isFollowing) { isFollowing = user.isFollowing }

    LaunchedEffect(user.uid, user.id, user.accountId) {
        val uid = user.uid ?: user.id ?: user.accountId
        if (uid != null && uid != "N/A" && uid != "User") {
            try {
                // Placeholder API Call
            } catch (e: Exception) {
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
                .clickable { if (!showKickDialog) onClose() else showKickDialog = false }
        ) {
            // Agar kick dialog nahi dikh raha hai toh main Room Profile sheet dikhegi
            if (!showKickDialog) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .clickable(enabled = false) {}
                ) {
                    Column(
                        modifier = Modifier
                            .padding(top = 44.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                            .background(Color.White)
                            .padding(top = 56.dp, bottom = 24.dp, start = 16.dp, end = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        
                        // 1st: Owner tag -> Name -> Gender tag -> Country flag
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            
                            // Owner tag image
                            if (isRoomOwner) {
                                AsyncImage(
                                    model = "IMG_20261027_235537.png".fullUrl(),
                                    contentDescription = "Room owner",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.height(16.dp).widthIn(max = 64.dp).padding(end = 4.dp)
                                )
                            }
                            
                            // Name
                            Text(
                                text = displayName.ifBlank { "User" },
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF111827)
                            )
                            
                            // Gender & Age (Chota kar diya)
                            Box(
                                modifier = Modifier
                                    .padding(start = 6.dp)
                                    .clip(CircleShape)
                                    .background(genderColor)
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    CustomGenderIcon(isFemale = displayGender.equals("female", true) || displayGender.equals("f", true) || displayGender == "♀", modifier = Modifier.size(6.dp))
                                    Text(displayAge.toString(), color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            
                            // Country Flag
                            if (displayCountry.isNotBlank()) {
                                Text(roomProfileCountryFlag(displayCountry), fontSize = 14.sp, modifier = Modifier.padding(start = 4.dp))
                            }
                        }

                        // 2nd: ID | Fans
                        Row(
                            modifier = Modifier.padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (isCoreOfficialAccount || is500002OfficialAccount || isRedOfficialAccount) {
                                AsyncImage(model = "IMG_20261101_125212.png".fullUrl(), contentDescription = "Purple ID Badge", contentScale = ContentScale.Fit, modifier = Modifier.height(30.dp).widthIn(min = 22.dp, max = 35.dp).padding(end = 4.dp))
                                Text(accountId, color = Color.DarkGray, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 4.dp))
                            } else {
                                Text("ID: $accountId", color = Color.DarkGray, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            }
                            
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
                        }

                        // 3rd: Lv.1 image, Officialhead, official, Admin tag
                        Row(
                            modifier = Modifier.padding(top = 10.dp).horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            // Lv.1 Image (choti ki aur text normal kiya, aur side wali image hata di)
                            Box(
                                contentAlignment = Alignment.CenterEnd,
                                modifier = Modifier.height(16.dp) 
                            ) {
                                AsyncImage(
                                    model = "IMG_20261028_004355.png".fullUrl(), 
                                    contentDescription = "Level", 
                                    modifier = Modifier.height(16.dp) 
                                )
                                Text(
                                    text = "Lv.1", 
                                    color = Color.White,
                                    fontSize = 10.sp, 
                                    fontWeight = FontWeight.Normal,
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                            }

                            // Officialhead
                            if (officialHeadTag) AsyncImage(model = "file_00000000ef2c82118b2266596d0e992d.png".fullUrl(), contentDescription = "Official Head", contentScale = ContentScale.Fit, modifier = Modifier.height(27.dp).widthIn(max = 80.dp))
                            
                            // Official Tag
                            if (officialTag) AsyncImage(model = "IMG_20261027_102250.png".fullUrl(), contentDescription = "Official", contentScale = ContentScale.Fit, modifier = Modifier.height(19.dp).widthIn(max = 60.dp))
                            
                            // Admin Tag
                            if (adminTag) AsyncImage(model = "IMG_20261027_114117.png".fullUrl(), contentDescription = "Admin", contentScale = ContentScale.Fit, modifier = Modifier.height(19.dp).widthIn(max = 60.dp))
                        }

                        // 4th: Baaki other tags (Service, Host, Coin Seller) agar hain toh
                        if (serviceTag || hostTag || coinSellerTag) {
                            Row(
                                modifier = Modifier.padding(top = 8.dp).horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (serviceTag) AsyncImage(model = "IMG_20261027_102207.png".fullUrl(), contentDescription = "Service / CS", contentScale = ContentScale.Fit, modifier = Modifier.height(19.dp).widthIn(max = 60.dp))
                                if (hostTag) AsyncImage(model = "IMG_20261028_154914.png".fullUrl(), contentDescription = "Host", contentScale = ContentScale.Fit, modifier = Modifier.height(17.dp).widthIn(max = 60.dp))
                                if (coinSellerTag) AsyncImage(model = "IMG_20261027_105359.png".fullUrl(), contentDescription = "Coin Seller", contentScale = ContentScale.Fit, modifier = Modifier.height(19.dp).widthIn(max = 60.dp))
                            }
                        }

                        if (showLeaveSeat) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.clickable { onLeaveSeat() }
                                ) {
                                    Box(modifier = Modifier.size(26.dp)) {
                                        Icon(
                                            imageVector = Icons.Default.Person, 
                                            contentDescription = "Leave",
                                            tint = Color.Black,
                                            modifier = Modifier.size(22.dp).align(Alignment.TopStart)
                                        )
                                        Icon(
                                            imageVector = Icons.Default.ArrowDownward,
                                            contentDescription = null,
                                            tint = Color.Red, // <--- Red done here
                                            modifier = Modifier
                                                .size(12.dp)
                                                .align(Alignment.BottomEnd)
                                                .background(Color.White, CircleShape) 
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Leave", color = Color.Black, fontSize = 12.sp)
                                }
                            }
                        }

                        if (showActions) {
                            // Modertaion Row (Mute, Leave, Lock, Kick)
                            if (showModerationRow) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // MUTE ICON 
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.clickable { onMute() }
                                    ) {
                                        Icon(
                                            imageVector = if (user.isMuted) Icons.Default.Mic else Icons.Default.MicOff,
                                            contentDescription = "Mute Toggle",
                                            tint = Color.Black,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(if (user.isMuted) "Unmute" else "Mute", color = Color.Black, fontSize = 12.sp)
                                    }
                                    
                                    Text("|", color = Color.LightGray)
                                    
                                    // LEAVE ICON 
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.clickable { onLeaveSeat() }
                                    ) {
                                        Box(modifier = Modifier.size(26.dp)) {
                                            Icon(
                                                imageVector = Icons.Default.Person, 
                                                contentDescription = "Leave",
                                                tint = Color.Black,
                                                modifier = Modifier.size(22.dp).align(Alignment.TopStart)
                                            )
                                            Icon(
                                                imageVector = Icons.Default.ArrowDownward,
                                                contentDescription = null,
                                                tint = Color.Red, // <--- Red done here
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .align(Alignment.BottomEnd)
                                                    .background(Color.White, CircleShape) 
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("Leave", color = Color.Black, fontSize = 12.sp)
                                    }
                                    
                                    Text("|", color = Color.LightGray)

                                    // KICK OUT ICON
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.clickable { showKickDialog = true } 
                                    ) {
                                        Box(modifier = Modifier.size(26.dp)) {
                                            Icon(
                                                imageVector = Icons.Default.Person, 
                                                contentDescription = "Kick out",
                                                tint = Color.Black,
                                                modifier = Modifier.size(22.dp).align(Alignment.TopStart)
                                            )
                                            Icon(
                                                imageVector = Icons.Default.Close, 
                                                contentDescription = null,
                                                tint = Color.Red, // <--- Red done here (Cross icon)
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .align(Alignment.BottomEnd)
                                                    .background(Color.White, CircleShape) 
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("Kick", color = Color.Black, fontSize = 12.sp)
                                    }
                                }
                            }
                            
                            // Follow, Chat, Gift Row
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Follow Column
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .weight(1f) 
                                        .clickable {
                                            isFollowing = !isFollowing
                                            onFollow()
                                        }
                                ) {
                                    AsyncImage(
                                        model = (if (isFollowing) "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/IMG_20261105_125204.png" else "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/IMG_20261105_125259.png"),
                                        contentDescription = if (isFollowing) "Followed" else "Follow",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.size(50.dp) 
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = if (isFollowing) "Followed" else "Follow", 
                                        color = Color.Black,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }

                                // Chat Column
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .weight(1f) 
                                        .clickable { onMessage() }
                                ) {
                                    AsyncImage(
                                        model = "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/IMG_20261105_125330.png",
                                        contentDescription = "Chat",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.size(50.dp) 
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Chat",
                                        color = Color.Black,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                                
                                // Send Gift Column
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .weight(1f) 
                                        .clickable { onThirdAction() }
                                ) {
                                    AsyncImage(
                                        model = "file_00000000b9f481fa90d96c422ae8dd32.png".fullUrl(), 
                                        contentDescription = "Gift",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.size(50.dp).clip(CircleShape) 
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Send Gift", 
                                        color = Color.Black,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }

                    // Overlapping Avatar
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .size(88.dp)
                            .clickable { onOpenPublicProfile(user.copy(name = displayName, image = displayProfileImage, country = displayCountry)) }
                    ) {
                        AsyncImage(
                            model = displayProfileImage.ifBlank { "default-avatar.png".fullUrl() },
                            contentDescription = displayName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(Color.LightGray)
                        )
                        NativeAvatarFrameOverlay(
                            userId = user.accountId.orEmpty().ifBlank { user.uid.orEmpty().ifBlank { user.id.orEmpty() } },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Corner Action Icons
                    if (!isCurrentUser) {
                        IconButton(
                            onClick = { showReports = true },
                            modifier = Modifier.align(Alignment.TopStart).padding(top = 52.dp, start = 8.dp)
                        ) {
                            LucidePublicProfileIcon(warning = true, tint = Color.Black, modifier = Modifier.size(24.dp))
                        }

                        IconButton(
                            onClick = { onMention(displayName.ifBlank { user.name }, sequenceOf(user.accountId, user.id, user.uid).firstOrNull { !it.isNullOrBlank() }.orEmpty()) },
                            modifier = Modifier.align(Alignment.TopEnd).padding(top = 52.dp, end = 8.dp)
                        ) {
                            Icon(Icons.Default.AlternateEmail, contentDescription = "Mention", tint = Color.Black)
                        }
                    }
                }
            } else {
                // Yaha pe apka new Kick Out Card Show Hoga (RoomProfile hide ho chuka hai)
                KickOutDialogCard(
                    userName = displayName.ifBlank { user.name },
                    onDismiss = { showKickDialog = false },
                    onConfirm = { time ->
                        showKickDialog = false
                        onKickOut() // Parent ki pass ki gayi callback call karega
                        onClose()   // Full screen Dialog close karne ke liye
                    }
                )
            }
        }
    }
    
    if (showReports) {
        UserReport(
            targetName = displayName,
            targetImage = displayProfileImage,
            targetId = user.accountId.orEmpty().ifBlank { user.uid.orEmpty().ifBlank { user.id.orEmpty() } },
            idBadgeImage = if (user.accountId.orEmpty().ifBlank { user.uid.orEmpty().ifBlank { user.id.orEmpty() } } in setOf("500002", "500003", "500004")) "IMG_20261101_125212.png".fullUrl() else null,
            onDismiss = { showReports = false }
        )
    }
}

@Composable
fun KickOutDialogCard(
    userName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var selectedTime by remember { mutableStateOf("30 minutes") }
    val timeOptions = listOf("15 minutes", "30 minutes", "2 hours", "24 hours")

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Click outside the card to dismiss it
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                // Consume clicks inside the card so it doesn't dismiss
                .clickable(enabled = false) {}
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Are you sure to Kick out",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Text(
                text = "$userName?",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Text(
                text = "Select kick-out time",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Grid Layout for time selection
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    TimeOptionItem(text = timeOptions[0], isSelected = selectedTime == timeOptions[0], modifier = Modifier.weight(1f)) { selectedTime = timeOptions[0] }
                    TimeOptionItem(text = timeOptions[1], isSelected = selectedTime == timeOptions[1], modifier = Modifier.weight(1f)) { selectedTime = timeOptions[1] }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    TimeOptionItem(text = timeOptions[2], isSelected = selectedTime == timeOptions[2], modifier = Modifier.weight(1f)) { selectedTime = timeOptions[2] }
                    TimeOptionItem(text = timeOptions[3], isSelected = selectedTime == timeOptions[3], modifier = Modifier.weight(1f)) { selectedTime = timeOptions[3] }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray),
                    border = BorderStroke(1.dp, Color.LightGray)
                ) {
                    Text("Cancel", fontSize = 16.sp)
                }

                Button(
                    onClick = { onConfirm(selectedTime) },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF05133)) // Exact Reddish Orange from Screenshot
                ) {
                    Text("Confirm", fontSize = 16.sp, color = Color.White)
                }
            }
        }
    }
}

// Button item design for Kickout Timer
@Composable
fun TimeOptionItem(text: String, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val backgroundColor = if (isSelected) Color(0xFFFDECE9) else Color(0xFFF5F5F5)
    val borderColor = if (isSelected) Color(0xFFF05133) else Color.Transparent
    val textColor = if (isSelected) Color(0xFFF05133) else Color.Gray

    Box(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text, 
            color = textColor, 
            fontSize = 14.sp, 
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
        )
    }
}

private fun String.fullUrl(): String {
    val baseUrl = "https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/"
    return if (this.startsWith("http")) this else baseUrl + this.removePrefix("/")
}

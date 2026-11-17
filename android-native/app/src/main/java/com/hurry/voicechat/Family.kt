package com.hurry.voicechat

import android.content.Context
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

// GITHUB LINK AUTO-ADDER
private const val FAMILY_ASSET_BASE = "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/"
private fun familyAsset(file: String): String {
    return if (file.startsWith("http")) file else FAMILY_ASSET_BASE + file.removePrefix("/")
}

// 10K, 1M, 1B FORMATTER FOR COINS
private fun formatCoins(value: Number): String {
    val v = value.toLong()
    return when {
        v >= 1_000_000_000 -> String.format(java.util.Locale.US, "%.1fB", v / 1_000_000_000.0).replace(".0B", "B")
        v >= 1_000_000 -> String.format(java.util.Locale.US, "%.1fM", v / 1_000_000.0).replace(".0M", "M")
        v >= 1_000 -> String.format(java.util.Locale.US, "%.1fK", v / 1_000.0).replace(".0K", "K")
        else -> v.toString()
    }
}

private val familyGold = Color(0xFFFFD700)
private val familyDark = Color(0xFF1A0D06)
private val familyBrown = Color(0xFF2A1610)
private val familyCardBg = Color(0xFF3B0C06)

private val familyRules = listOf(
    "1. The top 10 families in the weekly family rankings will receive various rewards. Rewards are distributed every Sunday at 5:30 AM (GMT+5:30).",
    "2. There is no need to wait for official approval.",
    "3. A family can have a maximum of 50 members.",
    "4. Individuals can apply to join a family. Once the family leader approves the request, the applicant can join the family immediately.",
    "5. Each user can join only one family. Regular family members can leave the family at any time without the leader's approval.",
    "6. The family leader has the authority to remove other members from the family at any time without the removed member's consent. The family leader can also disband the family freely without official approval.",
    "7. Upon the disbanding of the family, all members will immediately leave the original family.",
    "8. Family rankings reflect the family's strength. Zaho determines family rankings based on the total spending of all family members.",
    "9. Any benefits or relationships formed within the family are the result of voluntary actions taken by the members themselves and have no connection to Zaho."
)

private data class RewardItemData(
    val title: String,
    val thumbnail: String,
    val type: String,
    val videoUrl: String = "",
    val isWinnerReward: Boolean = false
)

private data class RewardModalData(
    val url: String,
    val title: String,
    val isWinnerReward: Boolean,
    val type: String
)

@Composable
fun Family(onBack: () -> Unit, onOpenFamily: (String) -> Unit = {}) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("hurry_user", Context.MODE_PRIVATE) }
    val authPrefs = remember { context.getSharedPreferences("hurry_auth", Context.MODE_PRIVATE) }
    val familyCreatorId = authPrefs.getString("accountId", "").orEmpty().ifBlank { authPrefs.getString("uid", "").orEmpty() }.ifBlank { authPrefs.getString("id", "").orEmpty() }
    val familyAuthUid = authPrefs.getString("uid", "").orEmpty().ifBlank { familyCreatorId }
    val familyAuthId = authPrefs.getString("id", "").orEmpty().ifBlank { familyAuthUid }
    var view by remember { mutableStateOf("main") }
    var showRules by remember { mutableStateOf(false) }
    var familyName by remember { mutableStateOf(prefs.getString("familyName", "") ?: "") }
    var announcement by remember { mutableStateOf(prefs.getString("familyAnnouncement", "") ?: "") }
    var familyImage by remember { mutableStateOf(prefs.getString("familyImage", "") ?: "") }
    var createBusy by remember { mutableStateOf(false) }
    var createError by remember { mutableStateOf<String?>(null) }

    var activeModal by remember { mutableStateOf<RewardModalData?>(null) }

    val scope = rememberCoroutineScope()
    val walletDao = remember { WalletDatabase.get(context).walletDao() }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) runCatching { context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        familyImage = uri?.toString().orEmpty()
    }
    var activeRank by remember { mutableIntStateOf(0) }
    var seconds by remember { mutableIntStateOf(0) }
    var topFamilies by remember { mutableStateOf<List<FamilyRecord>>(emptyList()) }
    var myFamilyRecord by remember { mutableStateOf<FamilyRecord?>(null) }
    LaunchedEffect(Unit) { while (true) { runCatching { FamilyProfileApi.leaderboard() }.onSuccess { topFamilies = it }; delay(30000) } }

    // Resolve the signed-in user's family from the server profile first, then local prefs.
    // This covers both the family creator and users who joined an existing family.
    LaunchedEffect(familyAuthUid, familyAuthId, familyCreatorId) {
        val profileTarget = TargetUser(
            id = familyAuthId,
            uid = familyAuthUid,
            accountId = familyCreatorId,
            displayAccountNumber = familyCreatorId
        )
        val remoteProfile = runCatching { ProfilePersistence.load(profileTarget) }.getOrNull()
        val remoteFamilyCode = remoteProfile?.optString("familyCode").orEmpty()
            .takeIf { it.isNotBlank() && it != "null" }
            ?: remoteProfile?.optString("familyId").orEmpty()
                .takeIf { it.isNotBlank() && it != "null" }
        val localFamilyCode = prefs.getString("familyCode", "").orEmpty()
            .ifBlank { prefs.getString("familyId", "").orEmpty() }
        val resolvedFamilyCode = remoteFamilyCode ?: localFamilyCode

        if (resolvedFamilyCode.isNotBlank()) {
            prefs.edit().putBoolean("familyCreated", true)
                .putString("familyCode", resolvedFamilyCode).apply()
            remoteProfile?.optString("familyName")?.takeIf { it.isNotBlank() && it != "null" }?.let { familyName = it }
            remoteProfile?.optString("familyImage")?.takeIf { it.isNotBlank() && it != "null" }?.let { familyImage = it }
            remoteProfile?.optString("familyAnnouncement")?.takeIf { it.isNotBlank() && it != "null" }?.let { announcement = it }

            runCatching { FamilyProfileApi.load(resolvedFamilyCode) }.onSuccess { family ->
                myFamilyRecord = family
                familyName = family.name
                familyImage = family.image
                announcement = family.announcement
                prefs.edit()
                    .putBoolean("familyCreated", true)
                    .putString("familyCode", resolvedFamilyCode)
                    .putString("familyName", family.name)
                    .putString("familyImage", family.image)
                    .putString("familyAnnouncement", family.announcement)
                    .putString("familyCreatorId", family.ownerAccountId)
                    .apply()
            }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            val now = Calendar.getInstance()
            val days = (Calendar.SUNDAY - now.get(Calendar.DAY_OF_WEEK) + 7) % 7
            val end = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, if (days == 0) 7 else days); set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59) }
            seconds = ((end.timeInMillis - now.timeInMillis).coerceAtLeast(0L) / 1000L).toInt()
            delay(1000)
        }
    }

    val bgColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (view == "create") Color(0xFFF9F9F9) else if (view == "top") familyBrown else familyDark,
        label = "bgColorTransition"
    )

    Box(Modifier.fillMaxSize().background(bgColor)) {
        androidx.compose.animation.Crossfade(targetState = view, label = "viewFade") { currentView ->
            when (currentView) {
                "create" -> FamilyCreate(onBack = { view = "main" }, name = familyName, onName = { familyName = it }, announcement = announcement, onAnnouncement = { announcement = it }, image = familyImage, onImage = { imagePicker.launch(arrayOf("image/*")) }, busy = createBusy, error = createError, onCreate = {
                    if (createBusy) return@FamilyCreate
                    if (familyName.isBlank()) {
                        createError = "Please enter a Family Name."
                    } else {
                        createBusy = true
                        createError = null
                        scope.launch {
                            var createdFamilyImage = ""
                            var createdFamilyCode = ""
                            val result = withContext(Dispatchers.IO) {
                                runCatching {
                                    val current = walletDao.getUserData() ?: UserDataEntity()
                                    if (current.balance < 30_000_000L) {
                                        "You need 30,000,000 coins to create a family."
                                    } else {
                                        walletDao.saveUserData(current.copy(balance = current.balance - 30_000_000L))
                                        val now = System.currentTimeMillis()
                                        walletDao.insertTransaction(TransactionEntity(
                                            title = "Create Family", amount = -30_000_000L,
                                            date = java.text.SimpleDateFormat("yyyy.MM.dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(now)),
                                            timestamp = now, type = "coins"
                                        ))
                                        val familyCode = prefs.getString("familyCode", "").orEmpty().ifBlank { "FAM" + now.toString().takeLast(6) }
                                        createdFamilyCode = familyCode
                                        val selectedImage = familyImage.takeIf { it.startsWith("http") || it.startsWith("data:image/") }
                                            ?: familyImage.takeIf { it.startsWith("content://") }?.let { ProfilePersistence.imageDataUri(context, Uri.parse(it)) }.orEmpty()
                                        val familyPhoto = if (selectedImage.startsWith("data:image/")) {
                                            ProfilePersistence.uploadImageToMongoDB(selectedImage, "$familyCreatorId-family", "family-dp")
                                                ?: throw IllegalStateException("Family image upload failed.")
                                        } else selectedImage
                                        createdFamilyImage = familyPhoto
                                        val saved = ProfilePersistence.save(
                                            TargetUser(id = familyAuthId, uid = familyAuthUid, accountId = familyCreatorId, displayAccountNumber = familyCreatorId),
                                            mapOf("familyCreated" to "true", "familyName" to familyName.trim(),
                                                "familyImage" to familyPhoto, "familyCreatorId" to familyCreatorId,
                                                "familyCode" to familyCode, "familyAnnouncement" to announcement)
                                        )
                                        if (!saved) {
                                            walletDao.saveUserData(current)
                                            throw IllegalStateException("Family could not be published. Please try again.")
                                        }
                                        try {
                                            FamilyProfileApi.create(familyCode, familyName.trim(), familyPhoto, announcement)
                                        } catch (familyError: Throwable) {
                                            walletDao.saveUserData(current)
                                            ProfilePersistence.save(
                                                TargetUser(id = familyAuthId, uid = familyAuthUid, accountId = familyCreatorId, displayAccountNumber = familyCreatorId),
                                                mapOf("familyCreated" to "false", "familyName" to "", "familyImage" to "", "familyCreatorId" to "", "familyCode" to "", "familyAnnouncement" to "")
                                            )
                                            throw familyError
                                        }
                                        null
                                    }
                                }.getOrElse { "Could not create family. Please try again." }
                            }
                            createBusy = false
                            if (result == null) {
                                prefs.edit().putString("familyName", familyName.trim())
                                    .putString("familyAnnouncement", announcement)
                                     .putString("familyImage", createdFamilyImage)
                                    .putString("familyCreatorId", familyCreatorId)
                                    .putString("familyCode", createdFamilyCode)
                                    .putBoolean("familyCreated", true).apply()
                                myFamilyRecord = runCatching { FamilyProfileApi.load(createdFamilyCode) }.getOrNull()
                                view = "main"
                                Toast.makeText(context, "Family created successfully", Toast.LENGTH_SHORT).show()
                            } else createError = result
                        }
                    }
                })
                "top" -> FamilyTop(
                    onBack = { 
                        view = "main" 
                        activeRank = 0
                    }, 
                    onRules = { showRules = true }, 
                    activeRank = activeRank, 
                    onRank = { 
                        activeRank = it
                        if (it == 0 || it == 1) {
                            view = "main"
                        }
                    }, 
                    onOpenModal = { url, title, isWinner, type -> activeModal = RewardModalData(url, title, isWinner, type) },
                    topFamilies = topFamilies
                )
                else -> FamilyHome(
                    onBack = onBack, 
                    onRules = { showRules = true }, 
                    seconds = seconds, 
                    activeRank = activeRank, 
                    onRank = { 
                        activeRank = it
                        if (it == 2) {
                            view = "top" 
                        }
                    }, 
                    onCreate = { view = "create" },
                    familyCreated = myFamilyRecord != null || prefs.getBoolean("familyCreated", false),
                    familyName = myFamilyRecord?.name?.takeIf { it.isNotBlank() } ?: familyName,
                    familyImage = myFamilyRecord?.image?.takeIf { it.isNotBlank() } ?: familyImage,
                    familyCreatorId = myFamilyRecord?.ownerAccountId?.takeIf { it.isNotBlank() } ?: familyCreatorId,
                    topFamilies = topFamilies,
                    myFamily = myFamilyRecord,
                    familyCode = myFamilyRecord?.id?.takeIf { it.isNotBlank() }
                        ?: prefs.getString("familyCode", "").orEmpty().ifBlank { prefs.getString("familyId", "").orEmpty() },
                    onOpenFamily = onOpenFamily
                )
            }
        }

        if (activeModal != null) {
            FamilyRewardModal(
                url = activeModal!!.url,
                title = activeModal!!.title,
                isWinnerReward = activeModal!!.isWinnerReward,
                type = activeModal!!.type,
                onClose = { activeModal = null }
            )
        }

        if (showRules) FamilyRules(onClose = { showRules = false })
    }
}

// --------------------------------------------------------
// MAIN HOME SCREEN
// --------------------------------------------------------
@Composable
private fun FamilyHome(onBack: () -> Unit, onRules: () -> Unit, seconds: Int, activeRank: Int, onRank: (Int) -> Unit, onCreate: () -> Unit, familyCreated: Boolean, familyName: String, familyImage: String, familyCreatorId: String, topFamilies: List<FamilyRecord>, myFamily: FamilyRecord?, familyCode: String, onOpenFamily: (String) -> Unit) {
    val scrollState = rememberScrollState()
    val familyMemberCountText = myFamily?.members?.size?.coerceAtMost(50)?.toString() ?: "—"
    val activeFilter = null
    val inactiveFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

    Box(Modifier.fillMaxSize().background(Color.Transparent)) {
        Box(Modifier.fillMaxSize().verticalScroll(scrollState)) {
            Box(Modifier.fillMaxWidth().height(400.dp)) {
                AsyncImage(
                    model = familyAsset("IMG_20261115_203110-isme-jo-background-theme-hai-naa-bss-wohi-dee-sale-isme-jo-ek-tune-middle-m-divi.jpg"),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.0f to Color.Transparent, 0.95f to Color.Transparent, 1.0f to familyDark)))
            }

            Column(Modifier.fillMaxSize().padding(bottom = 140.dp)) {
                Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 8.dp, end = 16.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = Color.White, modifier = Modifier.size(28.dp)) }
                    Spacer(Modifier.weight(1f))
                    // DEFAULT TOP RIGHT QUESTION MARK BACK!
                    FamilyQuestion(Modifier.size(20.dp).clickable(onClick = onRules))
                }

                // 1 frame Middle m 1 row, 2nd row m 2,3 frame
                Column(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    FamilyPodiumCard(family = topFamilies.getOrNull(0), frame = "IMG_20261115_135339.png", size = 175.dp, modifier = Modifier.zIndex(2f).offset(y = (-20).dp))
                    Row(Modifier.fillMaxWidth().offset(y = (-35).dp).padding(horizontal = 25.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        FamilyPodiumCard(family = topFamilies.getOrNull(1), frame = "IMG_20261115_135356.png", size = 130.dp)
                        FamilyPodiumCard(family = topFamilies.getOrNull(2), frame = "IMG_20261115_135419.png", size = 130.dp)
                    }
                }
                
                Spacer(Modifier.height(13.dp))

                // 3 tab image
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(0.dp), verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(model = familyAsset("IMG_20261115_133007.png"), contentDescription = null, modifier = Modifier.weight(1f).wrapContentHeight().clickable { onRank(0) }, contentScale = ContentScale.FillWidth, colorFilter = if (activeRank == 0) activeFilter else inactiveFilter)
                    AsyncImage(model = familyAsset("IMG_20261115_132909.png"), contentDescription = null, modifier = Modifier.weight(1f).wrapContentHeight().clickable { onRank(1) }, contentScale = ContentScale.FillWidth, colorFilter = if (activeRank == 1) activeFilter else inactiveFilter)
                    AsyncImage(model = familyAsset("IMG_20261115_132937.png"), contentDescription = null, modifier = Modifier.weight(1f).wrapContentHeight().clickable { onRank(2) }, contentScale = ContentScale.FillWidth, colorFilter = if (activeRank == 2) activeFilter else inactiveFilter)
                }

                Spacer(Modifier.height(10.dp))

                // Countdown
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp).background(Brush.horizontalGradient(listOf(Color.Transparent, familyGold.copy(alpha = .12f), Color.Transparent))).border(1.dp, familyGold.copy(alpha = .18f)), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Text("Countdown", color = Color(0xFFFDF6E3), fontSize = 14.sp, modifier = Modifier.padding(end = 6.dp))
                    Text("%02d".format(seconds / 86400), color = Color.White, modifier = Modifier.background(Color(0xFF1A0F02), RoundedCornerShape(4.dp)).padding(4.dp), fontWeight = FontWeight.Bold)
                    Text(" Days ", color = Color(0xFFFDF6E3), fontSize = 12.sp)
                    Text("%02d".format((seconds / 3600) % 24), color = Color.White, modifier = Modifier.background(Color(0xFF1A0F02), RoundedCornerShape(4.dp)).padding(4.dp), fontWeight = FontWeight.Bold)
                    Text(":", color = Color.White)
                    Text("%02d".format((seconds / 60) % 60), color = Color.White, modifier = Modifier.background(Color(0xFF1A0F02), RoundedCornerShape(4.dp)).padding(4.dp), fontWeight = FontWeight.Bold)
                    Text(":", color = Color.White)
                    Text("%02d".format(seconds % 60), color = Color.White, modifier = Modifier.background(Color(0xFF1A0F02), RoundedCornerShape(4.dp)).padding(4.dp), fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(10.dp))

                // 1, 2, 3 Cards
                val topOffsets = listOf(0.dp, (-10).dp, (-19).dp)
                topFamilies.take(3).forEachIndexed { index, family ->
                    Box(Modifier.fillMaxWidth().padding(horizontal = 4.dp).wrapContentHeight().offset(y = topOffsets[index]), contentAlignment = Alignment.CenterStart) {
                        val bgImage = when(index) {
                            0 -> "IMG_20261115_134723.png"
                            1 -> "IMG_20261115_134543.png"
                            else -> "IMG_20261115_134632.png"
                        }
                        AsyncImage(model = familyAsset(bgImage), contentDescription = null, modifier = Modifier.fillMaxWidth(), contentScale = ContentScale.FillWidth)
                        
                        Text("Top ${index + 1}", color = familyGold, fontSize = 16.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(start = 14.dp))
                        
                        Row(Modifier.align(Alignment.CenterStart).padding(start = 60.dp, end = 26.dp), verticalAlignment = Alignment.CenterVertically) {
                            
                            // LIST AVATARS TOP 1, 2, 3 (CIRCLE AVATAR WITH FRAME EXACTLY LIKE PODIUM!)
                            val listFrame = when(index) {
                                0 -> "IMG_20261115_135339.png"
                                1 -> "IMG_20261115_135356.png"
                                else -> "IMG_20261115_135419.png"
                            }
                            FamilyPodiumCard(family = family, frame = listFrame, size = 65.dp)
                            
                            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                                Text(family.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AsyncImage(model = familyAsset("IMG_20261116_162146.png"), contentDescription = "Members", modifier = Modifier.size(10.dp))
                                    Spacer(Modifier.width(2.dp))
                                    Text("${family.members.size.coerceAtMost(50)}/50", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                            // RIGHT CORNER COINS
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                AsyncImage(model = familyAsset("file_00000000e56882119c217d508b6733dc.png"), contentDescription = "Coins", modifier = Modifier.size(16.dp))
                                Spacer(Modifier.height(2.dp))
                                Text(formatCoins(family.totalCoinsSent), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // 4 se leke 50 Cards
                topFamilies.drop(3).take(47).forEachIndexed { index, family ->
                    Box(Modifier.fillMaxWidth().padding(horizontal = 4.dp).wrapContentHeight().offset(y = (-25).dp), contentAlignment = Alignment.CenterStart) {
                        AsyncImage(model = familyAsset("IMG_20261115_134839.png"), contentDescription = null, modifier = Modifier.fillMaxWidth(), contentScale = ContentScale.FillWidth)
                        Text((index + 4).toString(), color = familyGold, fontSize = 22.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(start = 24.dp))
                        
                        Row(Modifier.align(Alignment.CenterStart).padding(start = 65.dp, end = 26.dp), verticalAlignment = Alignment.CenterVertically) {
                            
                            // SQUARE AVATARS FOR 4 TO 50 (SIZE BADI KAR DI HAI: 48.dp)
                            AsyncImage(model = family.image, contentDescription = family.name, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                            
                            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                                Text(family.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AsyncImage(model = familyAsset("IMG_20261116_162146.png"), contentDescription = "Members", modifier = Modifier.size(10.dp))
                                    Spacer(Modifier.width(2.dp))
                                    Text("${family.members.size.coerceAtMost(50)}/50", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                            // RIGHT CORNER COINS
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                AsyncImage(model = familyAsset("file_00000000e56882119c217d508b6733dc.png"), contentDescription = "Coins", modifier = Modifier.size(16.dp))
                                Spacer(Modifier.height(2.dp))
                                Text(formatCoins(family.totalCoinsSent), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().offset(y = 25.dp)) {
            AsyncImage(model = familyAsset("file_000000009d1081f59878648feb821b5e.png"), contentDescription = null, modifier = Modifier.fillMaxWidth().scale(1.15f), contentScale = ContentScale.FillWidth)
            if (familyCreated) {
                Row(
                    Modifier.align(Alignment.Center).fillMaxWidth().padding(horizontal = 28.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = familyImage.takeIf { it.isNotBlank() } ?: familyAsset("IMG_20261116_162146.png"),
                        contentDescription = "Family DP",
                        modifier = Modifier.size(54.dp).clip(RoundedCornerShape(15.dp))
                            .clickable(enabled = familyCode.isNotBlank()) { onOpenFamily(familyCode) },
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(familyName, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(model = familyAsset("IMG_20261116_162146.png"), contentDescription = "Members", modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("$familyMemberCountText/50", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    
                    // RANK POSITION SHOW (Right Corner in Bottom Bar)
                    val myRankIndex = topFamilies.indexOfFirst { it.name == familyName }
                    if (myRankIndex != -1) {
                        Text("Top ${myRankIndex + 1}", color = familyGold, fontSize = 14.sp, fontWeight = FontWeight.Black)
                    } else {
                        Text("Unranked", color = Color.Gray.copy(alpha = 0.8f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            } else {
                Box(Modifier.matchParentSize(), contentAlignment = Alignment.Center) {
                    AsyncImage(model = familyAsset("file_0000000056508230808da89fb794a97f.png"), contentDescription = "Create Family", modifier = Modifier.fillMaxWidth(0.5f).clickable(onClick = onCreate), contentScale = ContentScale.Fit)
                }
            }
        }
    }
}

// AVATAR IN CIRCLE BEHIND THE FRAME (Perfectly used for Top Podium & List 1,2,3 Cards too)
@Composable
private fun FamilyPodiumCard(family: FamilyRecord?, frame: String, size: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        // AVATAR RENDERED FIRST SO IT IS BEHIND THE FRAME (SIZE INCREASED & CENTERED!)
        val avatarSize = size * 0.85f // Frame ko full cover karne ke liye size bada kar diya
        Box(Modifier.align(Alignment.Center)) { // Yahan ekdam exact Center align kar diya
            if (family != null) {
                AsyncImage(
                    model = family.image, 
                    contentDescription = null, 
                    modifier = Modifier.size(avatarSize).clip(CircleShape), 
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.size(avatarSize).clip(CircleShape).background(Color(0xFF5C1A06)), 
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = "Default Avatar", tint = Color.White, modifier = Modifier.size(avatarSize * 0.6f))
                }
            }
        }
        
        // FRAME RENDERED ON TOP
        AsyncImage(model = familyAsset(frame), contentDescription = "Top family frame", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
    }
}

// --------------------------------------------------------
// TOP RANKINGS REWARDS CARDS
// --------------------------------------------------------
@Composable
private fun FamilyTop(onBack: () -> Unit, onRules: () -> Unit, activeRank: Int, onRank: (Int) -> Unit, onOpenModal: (String, String, Boolean, String) -> Unit, topFamilies: List<FamilyRecord>) {
    val scrollState = rememberScrollState()
    val activeFilter = null
    val inactiveFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

    Box(Modifier.fillMaxSize().background(Color.Transparent).verticalScroll(scrollState)) {
        Box(Modifier.fillMaxWidth().height(400.dp)) {
            AsyncImage(model = familyAsset("IMG_20261115_203110-isme-jo-background-theme-hai-naa-bss-wohi-dee-sale-isme-jo-ek-tune-middle-m-divi.jpg"), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.0f to Color.Transparent, 0.95f to Color.Transparent, 1.0f to familyBrown)))
        }

        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(top = 8.dp, start = 8.dp, end = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = Color.White, modifier = Modifier.size(28.dp)) }
                Spacer(Modifier.weight(1f))
                // QUESTION MARK ICON WAPAS PEHLE JAISA ADD KAR DIYA
                FamilyQuestion(Modifier.size(20.dp).clickable(onClick = onRules))
            }

            Column(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                FamilyPodiumCard(family = topFamilies.getOrNull(0), frame = "IMG_20261115_135339.png", size = 175.dp, modifier = Modifier.zIndex(2f).offset(y = (-15).dp))
                Row(Modifier.fillMaxWidth().offset(y = (-35).dp).padding(horizontal = 25.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    FamilyPodiumCard(family = topFamilies.getOrNull(1), frame = "IMG_20261115_135356.png", size = 130.dp)
                    FamilyPodiumCard(family = topFamilies.getOrNull(2), frame = "IMG_20261115_135419.png", size = 130.dp)
                }
            }

            Spacer(Modifier.height(15.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(0.dp), verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(model = familyAsset("IMG_20261115_133007.png"), contentDescription = null, modifier = Modifier.weight(1f).wrapContentHeight().clickable { onRank(0) }, contentScale = ContentScale.FillWidth, colorFilter = if (activeRank == 0) activeFilter else inactiveFilter)
                AsyncImage(model = familyAsset("IMG_20261115_132909.png"), contentDescription = null, modifier = Modifier.weight(1f).wrapContentHeight().clickable { onRank(1) }, contentScale = ContentScale.FillWidth, colorFilter = if (activeRank == 1) activeFilter else inactiveFilter)
                AsyncImage(model = familyAsset("IMG_20261115_132937.png"), contentDescription = null, modifier = Modifier.weight(1f).wrapContentHeight().clickable { onRank(2) }, contentScale = ContentScale.FillWidth, colorFilter = if (activeRank == 2) activeFilter else inactiveFilter)
            }
            
            Spacer(Modifier.height(20.dp))

            RewardCard(title = "TOP 1 Reward", item1 = RewardItemData("Medal *7 days", "1000196572-background (1).mp4", "black"), item2 = RewardItemData("Top1 Tag *7 days", "", "tag"), item3 = RewardItemData("Vehicle *7 days", "IMG_20260918_141104.jpg", "vehicle", videoUrl = "gemini_generated_video_e407ad86~2.mp4"), frame = RewardItemData("Frames *7 days", "gemini_generated_video_0d259062.mp4", "green", isWinnerReward = true), familyFrame = RewardItemData("Family Frame *7 d", "", "tag"), roomTheme = RewardItemData("Room Theme *7 d", "IMG-20260914-WA0043.jpg", "tall"), onClick = onOpenModal, label = "TOP1 Family")
            Spacer(Modifier.height(24.dp))
            RewardCard(title = "TOP 2 Reward", item1 = RewardItemData("Medal *7 days", "1000196573-background (1).mp4", "black"), item2 = RewardItemData("Top2 Tag *7 days", "", "tag"), item3 = RewardItemData("Vehicle *7 days", "IMG_20260919_010054.jpg", "vehicle", videoUrl = "VID_20260919_010017.mp4"), frame = RewardItemData("Frames *7 days", "gemini_generated_video_0d259062.mp4", "green", isWinnerReward = true), familyFrame = RewardItemData("Family Frame *7 d", "", "tag"), roomTheme = RewardItemData("Room Theme *7 d", "IMG-20260914-WA0045.jpg", "tall"), onClick = onOpenModal, label = "TOP2 Family")
            Spacer(Modifier.height(24.dp))
            RewardCard(title = "TOP 3 Reward", item1 = RewardItemData("Medal *7 days", "1000196574-background (1).mp4", "black"), item2 = RewardItemData("Top3 Tag *7 days", "", "tag"), item3 = RewardItemData("Vehicle *7 days", "IMG_20260919_222412.jpg", "mixed", videoUrl = "VID_20260919_222502.mp4"), frame = RewardItemData("Frames *7 days", "gemini_generated_video_123c050b~2.mp4", "green", isWinnerReward = true), familyFrame = RewardItemData("Family Frame *7 d", "", "tag"), roomTheme = RewardItemData("Room Theme *7 d", "IMG-20260914-WA0046.jpg", "tall"), onClick = onOpenModal, label = "TOP3 Family")
            Spacer(Modifier.height(16.dp))
            RewardCard(title = "TOP 4 TO 10\nReward", item1 = RewardItemData("Medal *3 days", "", "tag"), item2 = RewardItemData("Frames *3 days", "gemini_generated_video_0d259062.mp4", "green", isWinnerReward = true), item3 = RewardItemData("Vehicle *3 days", "IMG_20260919_222041.jpg", "mixed", videoUrl = "VID_20260919_222549.mp4"), frame = null, familyFrame = null, roomTheme = null, onClick = onOpenModal, label = "TOP4-10 Family")
            Spacer(Modifier.height(60.dp))
        }
    }
}

@Composable
private fun RewardCard(title: String, item1: RewardItemData, item2: RewardItemData, item3: RewardItemData, frame: RewardItemData?, familyFrame: RewardItemData?, roomTheme: RewardItemData?, onClick: (String, String, Boolean, String) -> Unit, label: String) {
    Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        Box(Modifier.fillMaxWidth().padding(top = 20.dp).background(familyCardBg, RoundedCornerShape(12.dp)).border(2.dp, familyGold, RoundedCornerShape(12.dp)).padding(top = 40.dp, bottom = 24.dp)) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    RewardItemBox(item1, onClick, label)
                    RewardItemBox(item2, onClick, label)
                    RewardItemBox(item3, onClick, label)
                }
                if (frame != null && familyFrame != null) {
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth().offset(y = (-8).dp), horizontalArrangement = Arrangement.Center) {
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { RewardItemBox(frame, onClick, label) }
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { RewardItemBox(familyFrame, onClick, label) }
                    }
                }
                if (roomTheme != null) {
                    Spacer(Modifier.height(16.dp))
                    RewardItemBox(roomTheme, onClick, label)
                }
            }
        }
        Box(Modifier.align(Alignment.TopCenter).offset(y = (-5).dp), contentAlignment = Alignment.Center) {
            AsyncImage(model = familyAsset("file_00000000b9048207a6cb463144ef26f4.png"), contentDescription = null, modifier = Modifier.width(220.dp).height(50.dp), contentScale = ContentScale.Fit)
            Text(title, color = Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.offset(y = (-2).dp))
        }
    }
}

@Composable
private fun RewardItemBox(item: RewardItemData, onClick: (String, String, Boolean, String) -> Unit, label: String) {
    val clickUrl = item.videoUrl.ifBlank { item.thumbnail }
    val isTall = item.type == "tall"
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(if (isTall) 140.dp else 90.dp).clickable { if (clickUrl.isNotBlank()) onClick(clickUrl, label, item.isWinnerReward, item.type) }) {
        Box(Modifier.fillMaxWidth().aspectRatio(if (isTall) 0.45f else 1f).clip(RoundedCornerShape(12.dp)).background(Brush.verticalGradient(listOf(Color(0xFF8C3A19), Color(0xFF5C1A06))), RoundedCornerShape(12.dp)).border(2.dp, familyGold, RoundedCornerShape(12.dp)).padding(4.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp))) {
                if (item.thumbnail.isNotBlank()) {
                    if (item.thumbnail.substringBefore("?").substringAfterLast(".").lowercase() in setOf("mp4", "webm", "m4v")) {
                        FamilyRewardVideo(url = familyAsset(item.thumbnail), modifier = Modifier.fillMaxSize(), rounded = false, autoplay = true)
                    } else {
                        AsyncImage(model = familyAsset(item.thumbnail), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = if (isTall) ContentScale.Fit else ContentScale.Crop)
                    }
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color.Black.copy(alpha = 0.7f), 0.20f to Color.Transparent, 0.80f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.7f))))
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(item.title, color = Color(0xFFFDE68A), fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun FamilyRewardVideo(url: String, modifier: Modifier = Modifier, rounded: Boolean = true, autoplay: Boolean = false) {
    val context = LocalContext.current
    val player = remember(url) { ExoPlayer.Builder(context).build().apply { setMediaItem(MediaItem.fromUri(url)); repeatMode = Player.REPEAT_MODE_ONE; volume = 0f; playWhenReady = autoplay; prepare(); if (autoplay) play() } }
    var videoVisible by remember(url) { mutableStateOf(false) }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() { videoVisible = true }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) { videoVisible = false }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener); player.release() }
    }
    Box(modifier.clip(if (rounded) RoundedCornerShape(8.dp) else RoundedCornerShape(0.dp)).background(Color.Transparent)) {
        AndroidView(modifier = Modifier.fillMaxSize(), factory = { ctx -> PlayerView(ctx).apply { useController = false; resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM; setShutterBackgroundColor(android.graphics.Color.TRANSPARENT); layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT); this.player = player } }, update = { it.player = player; if (autoplay && !player.isPlaying) player.play() })
        androidx.compose.animation.AnimatedVisibility(visible = !videoVisible, enter = androidx.compose.animation.fadeIn(), exit = androidx.compose.animation.fadeOut()) { Box(Modifier.fillMaxSize().background(Color(0xFF210D08))) }
    }
}

@Composable
private fun FamilyRewardModal(url: String, title: String, isWinnerReward: Boolean, type: String, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(if (isWinnerReward) Color.Black.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.35f)).clickable(onClick = onClose), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val isVideo = url.substringBefore("?").substringAfterLast(".").lowercase() in setOf("mp4", "webm", "m4v")
                val contentModifier = if (isWinnerReward) Modifier.size(320.dp).zIndex(1f) else Modifier.fillMaxWidth().fillMaxHeight(0.75f).zIndex(1f)
                Box(contentModifier) {
                    if (isVideo) { FamilyRewardVideo(url = familyAsset(url), modifier = Modifier.fillMaxSize(), rounded = false, autoplay = true) } else { AsyncImage(model = familyAsset(url), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit) }
                    if (!isWinnerReward) { Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color.Black.copy(alpha = 0.65f), 0.15f to Color.Transparent, 0.70f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.65f)))) }
                }
            }
        }
    }
}

// --------------------------------------------------------
// CREATE FAMILY 
// --------------------------------------------------------
@Composable
internal fun FamilyCreate(onBack:()->Unit, name:String, onName:(String)->Unit, announcement:String, onAnnouncement:(String)->Unit, image:String, onImage:()->Unit, busy:Boolean, error:String?, onCreate:()->Unit, editMode:Boolean=false) {
    Box(Modifier.fillMaxSize().background(Color(0xFFF9F9F9))) {
        
        Box(Modifier.fillMaxWidth().wrapContentHeight()) {
            AsyncImage(
                model = familyAsset("IMG_20261115_230814-bhai-iska-jo-background-image-hai-bss-wo-dee-bss-d.jpg"),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.FillWidth
            )
            Box(Modifier.matchParentSize().background(
                Brush.verticalGradient(
                    0.0f to Color.Transparent,
                    0.90f to Color.Transparent,
                    1.0f to Color(0xFFF9F9F9)
                )
            ))
        }

        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(top = 16.dp, start = 8.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = Color.Black, modifier = Modifier.size(28.dp)) }
                Text(if (editMode) "Edit my family" else "Create my family", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f).padding(end = 48.dp), textAlign = TextAlign.Center)
            }

            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 20.dp)) {
                
                Box(Modifier.align(Alignment.CenterHorizontally).padding(top = 24.dp).size(100.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFFF3F4F6)).clickable(onClick = onImage), contentAlignment = Alignment.Center) {
                    if (image.isNotBlank()) {
                        AsyncImage(model = image, contentDescription = "Family image", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Icon(Icons.Default.PhotoCamera, contentDescription = "Upload", tint = Color.LightGray, modifier = Modifier.size(36.dp))
                    }
                }

                Text("Family name", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(start = 20.dp, top = 32.dp, bottom = 8.dp))
                OutlinedTextField(
                    value = name, onValueChange = onName,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Color(0xFFF3F4F6),
                        focusedContainerColor = Color(0xFFF3F4F6),
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = Color.Transparent
                    ),
                    placeholder = { Text("Family Name", color = Color.Gray.copy(alpha = 0.5f)) },
                    trailingIcon = { Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.LightGray, modifier = Modifier.size(20.dp)) }
                )

                Text("Family Announcement", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 8.dp))
                Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(150.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFF3F4F6))) {
                    TextField(
                        value = announcement, onValueChange = onAnnouncement,
                        modifier = Modifier.fillMaxSize(),
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent
                        ),
                        placeholder = { Text("Family Announcement", color = Color.Gray.copy(alpha = 0.5f)) }
                    )
                    Text("${announcement.length}/500", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp))
                }

                if (!editMode) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Required Level", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("5", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                if (error != null) {
                    Text(error, color = Color(0xFFDC2626), fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp))
                }
            }

            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Button(
                    onClick = onCreate, enabled = !busy,
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(54.dp)
                ) {
                    Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xFFFFD700), Color(0xFFD2691E))), RoundedCornerShape(50)), contentAlignment = Alignment.Center) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(if (busy) (if (editMode) "Saving..." else "Creating family...") else (if (editMode) "Save Changes" else "Create with 30000000 Coins "), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            if (!busy && !editMode) {
                                AsyncImage(
                                    model = familyAsset("file_00000000e56882119c217d508b6733dc.png"),
                                    contentDescription = "Coins",
                                    modifier = Modifier.size(18.dp),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                    }
                }
                
            }
        }
    }
}

// DARK THEME FAMILY RULES
@Composable
private fun FamilyRules(onClose:()->Unit) {
    Dialog(onDismissRequest=onClose,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.6f)).clickable(onClick = onClose),contentAlignment=Alignment.BottomCenter) {
            Column(Modifier.fillMaxWidth().fillMaxHeight(.8f).clip(RoundedCornerShape(topStart=24.dp,topEnd=24.dp)).background(Brush.verticalGradient(listOf(Color(0xFF3B0C06), Color(0xFF1A0D06)))).border(2.dp, familyGold.copy(alpha = 0.5f), RoundedCornerShape(topStart=24.dp,topEnd=24.dp)).padding(horizontal=24.dp,vertical=20.dp).clickable(enabled = false, onClick = {})) {
                Box(Modifier.width(48.dp).height(6.dp).background(familyGold.copy(alpha = 0.4f), RoundedCornerShape(50)).align(Alignment.CenterHorizontally))
                Spacer(Modifier.height(16.dp))
                Text("Family rules",color=familyGold,fontSize=18.sp,fontWeight=FontWeight.Bold,modifier=Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom=12.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    familyRules.forEach { Text(it,color=Color(0xFFFDE68A).copy(alpha = 0.9f),fontSize=14.sp,lineHeight=20.sp) }
                    Spacer(Modifier.height(10.dp)); HorizontalDivider(color = familyGold.copy(alpha = 0.2f)); Spacer(Modifier.height(10.dp))
                    Text("Regards, Zyora Team",color=familyGold.copy(alpha = 0.7f),fontSize=12.sp,fontWeight=FontWeight.SemiBold, fontStyle = FontStyle.Italic, modifier=Modifier.align(Alignment.End))
                }
            }
        }
    }
}

// ORIGINAL QUESTION MARK ICON WAPAS KAR DIYA
@Composable
private fun FamilyQuestion(modifier:Modifier=Modifier) {
    Canvas(modifier) {
        drawCircle(Color.White,style=Stroke(2.dp.toPx()))
        drawContext.canvas.nativeCanvas.drawText("?",size.width/2f,size.height*.73f,android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply{color=android.graphics.Color.WHITE;textSize=size.minDimension*.72f;textAlign=android.graphics.Paint.Align.CENTER;typeface=android.graphics.Typeface.DEFAULT_BOLD})
    }
}

package com.hurry.voicechat

import android.content.Context
import android.graphics.SurfaceTexture
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.view.Surface
import android.view.TextureView
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.Calendar
import java.util.concurrent.atomic.AtomicBoolean

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

// ZAHO TO ZYORA CHANGED HERE
private val familyRules = listOf(
    "1. The top 10 families in the weekly family rankings will receive various rewards. Rewards are distributed every Monday at 12:00 AM (India time).",
    "2. There is no need to wait for official approval.",
    "3. A family can have a maximum of 50 members.",
    "4. Individuals can apply to join a family. Once the family leader approves the request, the applicant can join the family immediately.",
    "5. Each user can join only one family. Regular family members can leave the family at any time without the leader's approval.",
    "6. The family leader has the authority to remove other members from the family at any time without the removed member's consent. The family leader can also disband the family freely without official approval.",
    "7. Upon the disbanding of the family, all members will immediately leave the original family.",
    "8. Family rankings reflect the family's strength. Zyora determines family rankings based on the total spending of all family members.",
    "9. Any benefits or relationships formed within the family are the result of voluntary actions taken by the members themselves and have no connection to Zyora."
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

private data class FamilySupportTargetRow(
    val lv: String,
    val target: String,
    val totalBonus: String,
    val leaderCoins: String,
    val top1Coins: String,
    val top2Coins: String,
    val top3Coins: String
)

private val familySupportTargetRows = listOf(
    FamilySupportTargetRow("1", "96M", "6.720M", "480K", "1.920M", "1.440M", "960K"),
    FamilySupportTargetRow("2", "180M", "12.600M", "900K", "3.600M", "2.700M", "1.800M"),
    FamilySupportTargetRow("3", "360M", "28.800M", "2.160M", "8.640M", "6.480M", "4.320M"),
    FamilySupportTargetRow("4", "600M", "52.800M", "4.080M", "16.320M", "12.240M", "8.160M"),
    FamilySupportTargetRow("5", "900M", "79.200M", "6.120M", "24.480M", "18.360M", "12.240M"),
    FamilySupportTargetRow("6", "1320M", "116.160M", "8.976M", "35.904M", "26.928M", "17.952M"),
    FamilySupportTargetRow("7", "1800M", "158.400M", "12.240M", "48.960M", "36.720M", "24.480M"),
    FamilySupportTargetRow("8", "3000M", "264M", "20.400M", "81.600M", "61.200M", "40.800M"),
    FamilySupportTargetRow("9", "6000M", "528M", "40.800M", "163.200M", "122.400M", "81.600M"),
    FamilySupportTargetRow("10", "12000M", "1,056M", "81.600M", "326.400M", "244.800M", "163.200M")
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
    var rankingPeriod by remember { mutableStateOf("this_week") }
    var seconds by remember { mutableIntStateOf(0) }
    var topFamilies by remember { mutableStateOf<List<FamilyRecord>>(emptyList()) }
    var myFamilyRecord by remember { mutableStateOf<FamilyRecord?>(null) }
    LaunchedEffect(context, rankingPeriod) {
        // Restore the selected week's last known ranking immediately, then refresh from MongoDB.
        val cached = withContext(Dispatchers.IO) { FamilyProfileApi.cachedLeaderboard(context, rankingPeriod) }
        topFamilies = cached
        while (true) {
            runCatching { FamilyProfileApi.leaderboard(rankingPeriod) }.onSuccess { latest ->
                topFamilies = latest
                withContext(Dispatchers.IO) { FamilyProfileApi.saveLeaderboardCache(context, latest, rankingPeriod) }
            }
            delay(30000)
        }
    }

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
            val now = Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Kolkata"))
            val days = (Calendar.MONDAY - now.get(Calendar.DAY_OF_WEEK) + 7) % 7
            val end = (now.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, if (days == 0) 7 else days)
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
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
                                            val transactionTime = System.currentTimeMillis()
                                            val transactionDate = java.text.SimpleDateFormat(
                                                "yyyy.MM.dd HH:mm", java.util.Locale.getDefault()
                                            ).format(java.util.Date(transactionTime))
                                            NativeIndexDatabase(context).saveWalletTransaction(
                                                "family_create_${familyAuthUid}_${transactionTime}",
                                                familyAuthUid,
                                                "Create Family",
                                                -30_000_000L,
                                                transactionDate,
                                                transactionTime,
                                                "coin"
                                            )
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
                    rankingPeriod = rankingPeriod,
                    onRankingPeriod = { rankingPeriod = it },
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
private fun FamilyHome(onBack: () -> Unit, onRules: () -> Unit, seconds: Int, rankingPeriod: String, onRankingPeriod: (String) -> Unit, activeRank: Int, onRank: (Int) -> Unit, onCreate: () -> Unit, familyCreated: Boolean, familyName: String, familyImage: String, familyCreatorId: String, topFamilies: List<FamilyRecord>, myFamily: FamilyRecord?, familyCode: String, onOpenFamily: (String) -> Unit) {
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
                    FamilyQuestion(Modifier.size(20.dp).clickable(onClick = onRules))
                }

                // 1 frame Middle m 1 row, 2nd row m 2,3 frame
                Column(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    FamilyPodiumCard(family = topFamilies.getOrNull(0), frame = "IMG_20261115_135339.png", size = 175.dp, modifier = Modifier.zIndex(2f).offset(y = (-20).dp), avatarSize = 134.dp)
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
                            
                            val listFrame = when(index) {
                                0 -> "IMG_20261115_135339.png"
                                1 -> "IMG_20261115_135356.png"
                                else -> "IMG_20261115_135419.png"
                            }
                            FamilyPodiumCard(family = family, frame = listFrame, size = 65.dp, avatarSize = if (index == 0) 49.dp else 65.dp * 0.85f)
                            
                            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                                Text(family.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AsyncImage(model = familyAsset("IMG_20261116_162146.png"), contentDescription = "Members", modifier = Modifier.size(10.dp))
                                    Spacer(Modifier.width(2.dp))
                                    Text("${family.members.size.coerceAtMost(50)}/50", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                                }
                            }
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
                            
                            AsyncImage(model = family.image, contentDescription = family.name, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                            
                            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                                Text(family.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AsyncImage(model = familyAsset("IMG_20261116_162146.png"), contentDescription = "Members", modifier = Modifier.size(10.dp))
                                    Spacer(Modifier.width(2.dp))
                                    Text("${family.members.size.coerceAtMost(50)}/50", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                                }
                            }
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

@Composable
private fun FamilyPodiumCard(family: FamilyRecord?, frame: String, size: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier, avatarSize: androidx.compose.ui.unit.Dp = size * 0.85f) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Box(Modifier.align(Alignment.Center)) { 
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
            }

            Column(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                FamilyPodiumCard(family = topFamilies.getOrNull(0), frame = "IMG_20261115_135339.png", size = 175.dp, modifier = Modifier.zIndex(2f).offset(y = (-15).dp), avatarSize = 134.dp)
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

            // TOP 1
            RewardCard(
                title = "TOP 1 Reward",
                row1 = listOf(
                    RewardItemData("Frames 7 Days", "IMG_20261117_222458.png", "green", videoUrl = "VID_20261119_100449_690_bsl.mp4", isWinnerReward = true),
                    RewardItemData("Medal 7 Days", "IMG_20261117_210225.png", "medal"),
                    RewardItemData("Top1 Tag 7 Days", "IMG_20261117_193617.png", "tag")
                ),
                row2 = listOf(
                    RewardItemData("Entry 7 Days", "IMG_20260918_141104.jpg", "vehicle", videoUrl = "gemini_generated_video_e407ad86~2.mp4"),
                    RewardItemData("5 Lakh coins", "file_00000000b2d481fd8cd233482dbeb9ef.png", "coins")
                ),
                row3 = listOf(
                    RewardItemData("Room Theme 7 Days", "IMG-20260914-WA0043.jpg", "tall")
                ),
                onClick = onOpenModal,
                label = "TOP1 Family"
            )
            Spacer(Modifier.height(24.dp))
            
            // TOP 2
            RewardCard(
                title = "TOP 2 Reward",
                row1 = listOf(
                    RewardItemData("Frames 7 Days", "IMG_20261117_222526.png", "green", videoUrl = "VID_20261119_100523_352_bsl.mp4", isWinnerReward = true),
                    RewardItemData("Medal 7 Days", "IMG_20261117_210250.png", "medal"),
                    RewardItemData("Top2 Tag 7 Days", "IMG_20261117_193700.png", "tag")
                ),
                row2 = listOf(
                    RewardItemData("Room Theme 7 Days", "IMG-20260914-WA0045.jpg", "tall")
                ),
                onClick = onOpenModal,
                label = "TOP2 Family"
            )
            Spacer(Modifier.height(24.dp))
            
            // TOP 3
            RewardCard(
                title = "TOP 3 Reward",
                row1 = listOf(
                    RewardItemData("Frames 7 Days", "IMG_20261117_222701.png", "green", videoUrl = "VID_20261119_100548_604_bsl.mp4", isWinnerReward = true),
                    RewardItemData("Medal 7 Days", "IMG_20261117_210313.png", "medal"),
                    RewardItemData("Top3 Tag 7 Days", "IMG_20261117_193640.png", "tag")
                ),
                row2 = listOf(
                    RewardItemData("Room Theme 7 Days", "IMG-20260914-WA0046.jpg", "tall")
                ),
                onClick = onOpenModal,
                label = "TOP3 Family"
            )
            Spacer(Modifier.height(16.dp))
            
            // TOP 4 to 10
            RewardCard(
                title = "TOP 4-10 Reward",
                row1 = listOf(
                    RewardItemData("Frames 3 Days", "IMG_20261117_222718.png", "green", videoUrl = "VID_20261119_100632_615_bsl.mp4", isWinnerReward = true),
                    RewardItemData("Medal 3 Days", "IMG_20261117_210200.png", "medal")
                ),
                onClick = onOpenModal,
                label = "TOP4-10 Family"
            )
            Spacer(Modifier.height(20.dp))

            // IMAGE 1: FAMILY SUPPORT TARGET TABLE (TOP1-3)
            FamilySupportTargetTable()

            Spacer(Modifier.height(22.dp))

            // IMAGE 2: FAMILY SUPPORT INFO SECTION
            FamilySupportInfoSection()

            Spacer(Modifier.height(60.dp))
        }
    }
}

@Composable
private fun FamilySupportTargetTable() {
    val outerBorderColor = Color(0xFFD9B27A)
    val gridLineColor = Color(0xFF7B3331)
    val rowBgColor = Color(0xFF4A1012)
    val headerBrush = Brush.verticalGradient(listOf(Color(0xFFA9382E), Color(0xFF791F17)))
    val headerTextColor = Color(0xFFFFEED8)
    val cellTextColor = Color(0xFFF3DDD0)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(rowBgColor)
            .border(1.5.dp, outerBorderColor, RoundedCornerShape(4.dp))
    ) {
        // TABLE HEADER
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .background(headerBrush),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.weight(0.55f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                Text("Lv.", color = headerTextColor, fontSize = 10.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, maxLines = 1)
            }
            Box(Modifier.width(1.dp).fillMaxHeight().background(gridLineColor))
            Box(Modifier.weight(0.95f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                Text("Target", color = headerTextColor, fontSize = 10.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, maxLines = 1)
            }
            Box(Modifier.width(1.dp).fillMaxHeight().background(gridLineColor))
            Box(Modifier.weight(1.15f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                Text("Total\nBonus", color = headerTextColor, fontSize = 10.sp, lineHeight = 12.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
            }
            Box(Modifier.width(1.dp).fillMaxHeight().background(gridLineColor))
            Box(Modifier.weight(1.05f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                Text("Leader\nCoins", color = headerTextColor, fontSize = 10.sp, lineHeight = 12.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
            }
            Box(Modifier.width(1.dp).fillMaxHeight().background(gridLineColor))
            Column(Modifier.weight(3.45f).fillMaxHeight()) {
                // TOP1-3 GOLDEN PILL HEADER
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .padding(horizontal = 6.dp, vertical = 5.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Brush.verticalGradient(listOf(Color(0xFFFFF4C7), Color(0xFFF6CB6F)))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "TOP1-3",
                        color = Color(0xFF7B1E15),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Canvas(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 8.dp)
                            .size(width = 8.dp, height = 5.dp)
                    ) {
                        val path = Path().apply {
                            moveTo(0f, 0f)
                            lineTo(size.width, 0f)
                            lineTo(size.width / 2f, size.height)
                            close()
                        }
                        drawPath(path, color = Color(0xFF9C2217))
                    }
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(gridLineColor))
                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.weight(1.15f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                        Text("Top1\nCoins", color = headerTextColor, fontSize = 10.sp, lineHeight = 11.5.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
                    }
                    Box(Modifier.width(1.dp).fillMaxHeight().background(gridLineColor))
                    Box(Modifier.weight(1.15f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                        Text("Top2\nCoins", color = headerTextColor, fontSize = 10.sp, lineHeight = 11.5.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
                    }
                    Box(Modifier.width(1.dp).fillMaxHeight().background(gridLineColor))
                    Box(Modifier.weight(1.15f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                        Text("Top3\nCoins", color = headerTextColor, fontSize = 10.sp, lineHeight = 11.5.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
                    }
                }
            }
        }

        // TABLE ROWS (LV. 1 - 10)
        familySupportTargetRows.forEach { row ->
            Box(Modifier.fillMaxWidth().height(1.dp).background(gridLineColor))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .background(rowBgColor),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.weight(0.55f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    Text(row.lv, color = cellTextColor, fontSize = 10.sp, textAlign = TextAlign.Center, maxLines = 1, softWrap = false)
                }
                Box(Modifier.width(1.dp).fillMaxHeight().background(gridLineColor))
                Box(Modifier.weight(0.95f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    Text(row.target, color = cellTextColor, fontSize = 10.sp, textAlign = TextAlign.Center, maxLines = 1, softWrap = false)
                }
                Box(Modifier.width(1.dp).fillMaxHeight().background(gridLineColor))
                Box(Modifier.weight(1.15f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    Text(row.totalBonus, color = cellTextColor, fontSize = 9.5.sp, textAlign = TextAlign.Center, maxLines = 1, softWrap = false)
                }
                Box(Modifier.width(1.dp).fillMaxHeight().background(gridLineColor))
                Box(Modifier.weight(1.05f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    Text(row.leaderCoins, color = cellTextColor, fontSize = 9.5.sp, textAlign = TextAlign.Center, maxLines = 1, softWrap = false)
                }
                Box(Modifier.width(1.dp).fillMaxHeight().background(gridLineColor))
                Row(
                    modifier = Modifier.weight(3.45f).fillMaxHeight(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.weight(1.15f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                        Text(row.top1Coins, color = cellTextColor, fontSize = 9.5.sp, textAlign = TextAlign.Center, maxLines = 1, softWrap = false)
                    }
                    Box(Modifier.width(1.dp).fillMaxHeight().background(gridLineColor))
                    Box(Modifier.weight(1.15f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                        Text(row.top2Coins, color = cellTextColor, fontSize = 9.5.sp, textAlign = TextAlign.Center, maxLines = 1, softWrap = false)
                    }
                    Box(Modifier.width(1.dp).fillMaxHeight().background(gridLineColor))
                    Box(Modifier.weight(1.15f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                        Text(row.top3Coins, color = cellTextColor, fontSize = 9.5.sp, textAlign = TextAlign.Center, maxLines = 1, softWrap = false)
                    }
                }
            }
        }
    }
}

@Composable
private fun FamilySupportInfoSection() {
    val barColor = Color(0xFFE5A93C)
    val headingColor = Color(0xFFF3C677)
    val bodyColor = Color(0xFFEAD7C7)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // SECTION 1
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(5.dp).height(14.dp).background(barColor))
            Spacer(Modifier.width(8.dp))
            Text("What is Family Support?", color = headingColor, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "Family members can make family contribution by sending gifts. The higher the total contribution, the higher the reward level and the richer the bonus. Please refer to the reward table for specific reward rules.",
            color = bodyColor,
            fontSize = 13.5.sp,
            lineHeight = 20.sp
        )

        Spacer(Modifier.height(22.dp))

        // SECTION 2
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(5.dp).height(14.dp).background(barColor))
            Spacer(Modifier.width(8.dp))
            Text("How to get family support bonus?", color = headingColor, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "1. Gifting by family members.\n1 Coin = 1 Contribution\n2. Lucky gifts are calculated 20% of coins value.\n3. Only members who contribute more than 1.2M per week can receive the Top 20 rewards.",
            color = bodyColor,
            fontSize = 13.5.sp,
            lineHeight = 21.sp
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Tips: If you leave the family, both your family ranking and contribution will be cleared.",
            color = bodyColor,
            fontSize = 13.5.sp,
            lineHeight = 20.sp
        )

        Spacer(Modifier.height(22.dp))

        // SECTION 3
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(5.dp).height(14.dp).background(barColor))
            Spacer(Modifier.width(8.dp))
            Text("When can i get the family support bonus?", color = headingColor, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "The bonus will be sent at 00:00 local time base on the rules.",
            color = bodyColor,
            fontSize = 13.5.sp,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun RewardCard(title: String, row1: List<RewardItemData>, row2: List<RewardItemData> = emptyList(), row3: List<RewardItemData> = emptyList(), onClick: (String, String, Boolean, String) -> Unit, label: String) {
    Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        Box(Modifier.fillMaxWidth().padding(top = 20.dp).background(familyCardBg, RoundedCornerShape(12.dp)).border(2.dp, familyGold, RoundedCornerShape(12.dp)).padding(top = 40.dp, bottom = 24.dp)) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                // ROW 1
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    row1.forEach { item ->
                        RewardItemBox(item, onClick, label)
                    }
                }
                // ROW 2
                if (row2.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth().offset(y = if (row1.size == 3 && row2.size == 2) (-8).dp else 0.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                        row2.forEach { item ->
                            RewardItemBox(item, onClick, label)
                        }
                    }
                }
                // ROW 3
                if (row3.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                        row3.forEach { item ->
                            RewardItemBox(item, onClick, label)
                        }
                    }
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
private fun FamilyFramePreviewAvatar(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val avatar = remember {
        val prefs = context.getSharedPreferences("hurry_user", Context.MODE_PRIVATE)
        sequenceOf("image", "dp", "photo", "photoURL", "photoUrl", "avatar", "avatarUrl", "profileImage", "profileImageUrl", "profile_image", "imageUrl", "picture")
            .map { prefs.getString(it, "").orEmpty().trim() }
            .firstOrNull { it.isNotBlank() && it != "null" && it != "undefined" }
            .orEmpty()
    }
    Box(modifier.clip(CircleShape).background(Color(0xFF292929)), contentAlignment = Alignment.Center) {
        if (avatar.isNotBlank()) {
            AsyncImage(model = avatar, contentDescription = "Profile avatar", modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
        } else {
            Icon(Icons.Default.Person, contentDescription = "Profile avatar", tint = Color.White, modifier = Modifier.fillMaxSize(0.65f))
        }
    }
}

@Composable
private fun RewardItemBox(item: RewardItemData, onClick: (String, String, Boolean, String) -> Unit, label: String) {
    val clickUrl = item.videoUrl.ifBlank { item.thumbnail }
    val isTall = item.type == "tall"
    val isTag = item.type == "tag"
    val isMedal = item.type == "medal"
    val isCoins = item.type == "coins"
    val isVehicle = item.type == "vehicle"
    val isFrame = item.type == "green"
    val boxWidth = if (isTall) 140.dp else 90.dp
    
    // ADDED PADDING FOR FRAME TO MAKE IT SMALLER (12.dp)
    val imagePadding = if (isMedal) 10.dp else if (isFrame) 12.dp else 0.dp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally, 
        modifier = Modifier.width(boxWidth).clickable(enabled = isVehicle || isFrame) { 
            if (clickUrl.isNotBlank()) onClick(clickUrl, label, item.isWinnerReward, item.type) 
        }
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(if (isTall) 0.45f else 1f).clip(RoundedCornerShape(12.dp)).background(Brush.verticalGradient(listOf(Color(0xFF8C3A19), Color(0xFF5C1A06))), RoundedCornerShape(12.dp)).border(2.dp, familyGold, RoundedCornerShape(12.dp)).padding(4.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp))) {
                if (item.thumbnail.isNotBlank()) {
                    if (item.thumbnail.substringBefore("?").substringAfterLast(".").lowercase() in setOf("mp4", "webm", "m4v")) {
                        if (isVehicle) {
                            // Match Store vehicle preview: same decoder, green-screen handling,
                            // and exact 10% top / 15% bottom alpha fade.
                            StoreVideoPreview(
                                url = familyAsset(item.thumbnail),
                                modifier = Modifier.fillMaxSize(),
                                pause = false,
                                topFade = 0.10f,
                                bottomFade = 0.15f,
                                removeGreenScreen = true,
                                mute = false,
                                loop = true,
                                cacheVideo = false
                            )
                        } else {
                            // Family frame preview: show the profile avatar underneath
                            // and keep the transparent video frame smaller above it.
                            BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                val avatarSize = minOf(maxWidth, maxHeight) * 0.48f
                                FamilyFramePreviewAvatar(Modifier.size(avatarSize))
                                FamilyBlackKeyVideo(
                                    url = familyAsset(item.thumbnail),
                                    modifier = Modifier.fillMaxSize().padding(16.dp)
                                )
                            }
                        }
                    } else {
                        AsyncImage(
                            model = familyAsset(item.thumbnail), 
                            contentDescription = null, 
                            modifier = Modifier.fillMaxSize().padding(imagePadding).then(
                                if (isVehicle) {
                                    Modifier.graphicsLayer { alpha = 0.99f }.drawWithContent {
                                        drawContent()
                                        // 4 SIDES 15% FADE FOR IMAGE
                                        drawRect(
                                            brush = Brush.verticalGradient(
                                                0f to Color.Transparent,
                                                0.15f to Color.Black,
                                                0.85f to Color.Black,
                                                1f to Color.Transparent
                                            ),
                                            blendMode = BlendMode.DstIn
                                        )
                                        drawRect(
                                            brush = Brush.horizontalGradient(
                                                0f to Color.Transparent,
                                                0.15f to Color.Black,
                                                0.85f to Color.Black,
                                                1f to Color.Transparent
                                            ),
                                            blendMode = BlendMode.DstIn
                                        )
                                    }
                                } else Modifier
                            ), 
                            contentScale = if (isTall || isTag || isCoins) ContentScale.Fit else ContentScale.Crop
                        )
                    }
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color.Black.copy(alpha = 0.7f), 0.20f to Color.Transparent, 0.80f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.7f))))
                }
            }
        }
        if (item.title.isNotBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(item.title, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.offset(y = (-3).dp))
        }
    }
}

// BLACK BACKGROUND REMOVER VIDEO PLAYER FOR FRAME VIDEOS
@Composable
private fun FamilyBlackKeyVideo(url: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val player = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            repeatMode = Player.REPEAT_MODE_ONE
            volume = 0f
            playWhenReady = true
            prepare()
            play()
        }
    }
    val renderer = remember(url, player) {
        BlackKeyVideoRenderer(
            onVideoSurfaceReady = { surface ->
                player.setVideoSurface(surface)
                if (!player.isPlaying) player.play()
            },
            onVideoSurfaceDestroyed = {
                runCatching { player.setVideoSurface(null) }
            }
        )
    }

    DisposableEffect(player, renderer) {
        onDispose {
            runCatching { player.setVideoSurface(null) }
            runCatching { player.release() }
            renderer.release()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            TextureView(ctx).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                isOpaque = false
                surfaceTextureListener = renderer
            }
        }
    )
}

private class BlackKeyVideoRenderer(
    private val onVideoSurfaceReady: (Surface) -> Unit,
    private val onVideoSurfaceDestroyed: () -> Unit
) : TextureView.SurfaceTextureListener {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var renderThread: HandlerThread? = null
    private var renderHandler: Handler? = null

    private var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
    private var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE

    private var program = 0
    private var textureId = 0
    private var aPositionLoc = -1
    private var aTexCoordLoc = -1
    private var uTexMatrixLoc = -1
    private var uTextureLoc = -1

    private var videoSurfaceTexture: SurfaceTexture? = null
    private var videoSurface: Surface? = null

    private var viewWidth = 1
    private var viewHeight = 1
    private val released = AtomicBoolean(false)
    private val texMatrix = FloatArray(16)

    private val vertexBuffer: FloatBuffer = ByteBuffer
        .allocateDirect(QUAD_VERTICES.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .apply { put(QUAD_VERTICES); position(0) }

    private val texCoordBuffer: FloatBuffer = ByteBuffer
        .allocateDirect(QUAD_TEX_COORDS.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .apply { put(QUAD_TEX_COORDS); position(0) }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        released.set(false)
        viewWidth = width.coerceAtLeast(1)
        viewHeight = height.coerceAtLeast(1)
        val thread = HandlerThread("FamilyBlackKeyGL").apply { start() }
        renderThread = thread
        val handler = Handler(thread.looper)
        renderHandler = handler

        handler.post {
            if (released.get()) return@post
            if (!initEgl(surface) || !initGl()) {
                releaseGlResources()
                return@post
            }
            GLES20.glViewport(0, 0, viewWidth, viewHeight)
            GLES20.glClearColor(0f, 0f, 0f, 0f)
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            EGL14.eglSwapBuffers(eglDisplay, eglSurface)

            val vst = SurfaceTexture(textureId)
            videoSurfaceTexture = vst
            vst.setOnFrameAvailableListener({
                renderHandler?.post { drawFrame() }
            }, handler)
            val outSurface = Surface(vst)
            videoSurface = outSurface
            mainHandler.post {
                if (!released.get()) onVideoSurfaceReady(outSurface)
            }
        }
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        renderHandler?.post {
            viewWidth = width.coerceAtLeast(1)
            viewHeight = height.coerceAtLeast(1)
        }
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        release()
        return true
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit

    fun release() {
        if (!released.compareAndSet(false, true)) return
        mainHandler.post { onVideoSurfaceDestroyed() }
        val handler = renderHandler
        val thread = renderThread
        renderHandler = null
        renderThread = null
        if (handler != null) {
            handler.post {
                releaseGlResources()
                thread?.quitSafely()
            }
        } else {
            thread?.quitSafely()
        }
    }

    private fun initEgl(outputSurface: SurfaceTexture): Boolean {
        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (eglDisplay == EGL14.EGL_NO_DISPLAY) return false
        val version = IntArray(2)
        if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) return false

        val configAttribs = intArrayOf(
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,
            EGL14.EGL_NONE
        )
        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)
        if (!EGL14.eglChooseConfig(eglDisplay, configAttribs, 0, configs, 0, 1, numConfigs, 0) || numConfigs[0] <= 0) {
            return false
        }
        val config = configs[0] ?: return false

        val contextAttribs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
        eglContext = EGL14.eglCreateContext(eglDisplay, config, EGL14.EGL_NO_CONTEXT, contextAttribs, 0)
        if (eglContext == EGL14.EGL_NO_CONTEXT) return false

        val surfaceAttribs = intArrayOf(EGL14.EGL_NONE)
        eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, config, outputSurface, surfaceAttribs, 0)
        if (eglSurface == EGL14.EGL_NO_SURFACE) return false

        return EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)
    }

    private fun initGl(): Boolean {
        val vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER)
        val fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER)
        if (vertexShader == 0 || fragmentShader == 0) return false

        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)
        GLES20.glDeleteShader(vertexShader)
        GLES20.glDeleteShader(fragmentShader)

        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            GLES20.glDeleteProgram(program)
            program = 0
            return false
        }

        aPositionLoc = GLES20.glGetAttribLocation(program, "aPosition")
        aTexCoordLoc = GLES20.glGetAttribLocation(program, "aTexCoord")
        uTexMatrixLoc = GLES20.glGetUniformLocation(program, "uTexMatrix")
        uTextureLoc = GLES20.glGetUniformLocation(program, "sTexture")

        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        textureId = textures[0]
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        return true
    }

    private fun compileShader(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        val compiled = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == 0) {
            GLES20.glDeleteShader(shader)
            return 0
        }
        return shader
    }

    private fun drawFrame() {
        if (released.get() || eglSurface == EGL14.EGL_NO_SURFACE || program == 0) return
        val vst = videoSurfaceTexture ?: return
        runCatching {
            vst.updateTexImage()
            vst.getTransformMatrix(texMatrix)

            GLES20.glViewport(0, 0, viewWidth, viewHeight)
            GLES20.glClearColor(0f, 0f, 0f, 0f)
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

            GLES20.glUseProgram(program)
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
            GLES20.glUniform1i(uTextureLoc, 0)
            GLES20.glUniformMatrix4fv(uTexMatrixLoc, 1, false, texMatrix, 0)

            vertexBuffer.position(0)
            GLES20.glEnableVertexAttribArray(aPositionLoc)
            GLES20.glVertexAttribPointer(aPositionLoc, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)

            texCoordBuffer.position(0)
            GLES20.glEnableVertexAttribArray(aTexCoordLoc)
            GLES20.glVertexAttribPointer(aTexCoordLoc, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer)

            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

            GLES20.glDisableVertexAttribArray(aPositionLoc)
            GLES20.glDisableVertexAttribArray(aTexCoordLoc)

            EGL14.eglSwapBuffers(eglDisplay, eglSurface)
        }
    }

    private fun releaseGlResources() {
        runCatching { videoSurfaceTexture?.setOnFrameAvailableListener(null) }
        runCatching { videoSurface?.release() }
        runCatching { videoSurfaceTexture?.release() }
        videoSurface = null
        videoSurfaceTexture = null

        if (textureId != 0) {
            GLES20.glDeleteTextures(1, intArrayOf(textureId), 0)
            textureId = 0
        }
        if (program != 0) {
            GLES20.glDeleteProgram(program)
            program = 0
        }
        if (eglDisplay != EGL14.EGL_NO_DISPLAY) {
            EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
            if (eglSurface != EGL14.EGL_NO_SURFACE) {
                EGL14.eglDestroySurface(eglDisplay, eglSurface)
                eglSurface = EGL14.EGL_NO_SURFACE
            }
            if (eglContext != EGL14.EGL_NO_CONTEXT) {
                EGL14.eglDestroyContext(eglDisplay, eglContext)
                eglContext = EGL14.EGL_NO_CONTEXT
            }
            EGL14.eglReleaseThread()
            eglDisplay = EGL14.EGL_NO_DISPLAY
        }
    }

    companion object {
        private val QUAD_VERTICES = floatArrayOf(
            -1f, -1f,
             1f, -1f,
            -1f,  1f,
             1f,  1f
        )

        private val QUAD_TEX_COORDS = floatArrayOf(
            0f, 0f,
            1f, 0f,
            0f, 1f,
            1f, 1f
        )

        private const val VERTEX_SHADER = """
            attribute vec4 aPosition;
            attribute vec4 aTexCoord;
            uniform mat4 uTexMatrix;
            varying vec2 vTexCoord;
            void main() {
                gl_Position = aPosition;
                vTexCoord = (uTexMatrix * aTexCoord).xy;
            }
        """

        private const val FRAGMENT_SHADER = """
            #extension GL_OES_EGL_image_external : require
            precision mediump float;
            varying vec2 vTexCoord;
            uniform samplerExternalOES sTexture;
            void main() {
                vec4 c = texture2D(sTexture, vTexCoord);
                float peak = max(max(c.r, c.g), c.b);
                float alpha = smoothstep(0.06, 0.24, peak);
                gl_FragColor = vec4(c.rgb * alpha, alpha);
            }
        """
    }
}

// VIDEO SYSTEM UPDATED TO FIX TEXTUREVIEW TRANSPARENCY & AUDIO
@Composable
private fun FamilyRewardVideo(url: String, modifier: Modifier = Modifier, rounded: Boolean = true, autoplay: Boolean = false, hasSound: Boolean = false) {
    val context = LocalContext.current
    val player = remember(url) { 
        ExoPlayer.Builder(context).build().apply { 
            setMediaItem(MediaItem.fromUri(url))
            repeatMode = Player.REPEAT_MODE_ONE
            volume = if (hasSound) 1f else 0f 
            
            // Audio Attributes added ensure Exoplayer properly requests focus and plays sound!
            val audioAttributes = androidx.media3.common.AudioAttributes.Builder()
                .setUsage(androidx.media3.common.C.USAGE_MEDIA)
                .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MOVIE)
                .build()
            setAudioAttributes(audioAttributes, false)

            playWhenReady = autoplay
            prepare()
            if (autoplay) play() 
        } 
    }
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
        AndroidView(
            modifier = Modifier.fillMaxSize(), 
            factory = { ctx -> 
                android.view.TextureView(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    // BSS YE LINE MISSING THI FADE/TRANSPARENCY KE LIYE!
                    isOpaque = false 
                } 
            }, 
            update = { textureView -> 
                player.setVideoTextureView(textureView)
                if (autoplay && !player.isPlaying) player.play() 
            }
        )
        androidx.compose.animation.AnimatedVisibility(visible = !videoVisible, enter = androidx.compose.animation.fadeIn(), exit = androidx.compose.animation.fadeOut()) { Box(Modifier.fillMaxSize().background(Color(0xFF210D08))) }
    }
}

@Composable
private fun FamilyRewardModal(url: String, title: String, isWinnerReward: Boolean, type: String, onClose: () -> Unit) {
    val isFrameVideo = type == "green"
    val isVideo = url.substringBefore("?").substringAfterLast(".").lowercase() in setOf("mp4", "webm", "m4v")
    var frameAspectRatio by remember(url) { mutableFloatStateOf(1f) }

    LaunchedEffect(url, isFrameVideo) {
        if (isFrameVideo && isVideo) {
            withContext(Dispatchers.IO) {
                val retriever = MediaMetadataRetriever()
                runCatching {
                    retriever.setDataSource(familyAsset(url), emptyMap())
                    val w = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toFloatOrNull() ?: 0f
                    val h = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toFloatOrNull() ?: 0f
                    val rot = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
                    if (w > 0f && h > 0f) {
                        frameAspectRatio = if (rot == 90 || rot == 270) h / w else w / h
                    }
                }
                runCatching { retriever.release() }
            }
        }
    }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val contentModifier = if (isWinnerReward) {
                    if (isFrameVideo) Modifier.fillMaxWidth().aspectRatio(frameAspectRatio).zIndex(1f)
                    else Modifier.size(320.dp).zIndex(1f)
                } else Modifier.fillMaxWidth().fillMaxHeight(0.75f).zIndex(1f)

                Box(contentModifier) {
                    if (isVideo) {
                        if (isFrameVideo) {
                            // Smaller transparent frame layered over the profile avatar.
                            BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                val avatarSize = minOf(maxWidth, maxHeight) * 0.46f
                                FamilyFramePreviewAvatar(Modifier.size(avatarSize))
                                FamilyBlackKeyVideo(
                                    url = familyAsset(url),
                                    modifier = Modifier.fillMaxSize().padding(16.dp)
                                )
                            }
                        } else {
                            StoreVideoPreview(
                                url = familyAsset(url),
                                modifier = Modifier.fillMaxSize(),
                                pause = false,
                                topFade = if (type == "vehicle") 0.10f else 0f,
                                bottomFade = if (type == "vehicle") 0.15f else 0f,
                                removeGreenScreen = false,
                                mute = type != "vehicle",
                                loop = true,
                                cacheVideo = false
                            )
                        }
                    } else {
                        AsyncImage(model = familyAsset(url), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                    }
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

// FULL SCREEN FAMILY RULES (BORDERS REMOVED)
@Composable
private fun FamilyRules(onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Box(
            Modifier.fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF3B0C06), Color(0xFF1A0D06))))
                .clickable(enabled = false, onClick = {})
        ) {
            Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 20.dp)) {
                
                // BACK BUTTON & HEADING
                Row(Modifier.fillMaxWidth().statusBarsPadding().padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onClose, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                    Text(
                        "Family rules",
                        color = familyGold,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f).offset(x = (-24).dp),
                        textAlign = TextAlign.Center
                    )
                }

                // RULES TEXT
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    familyRules.forEach { 
                        Text(it, color = Color(0xFFFDE68A).copy(alpha = 0.9f), fontSize = 14.sp, lineHeight = 20.sp) 
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Regards, Zyora Team",
                        color = familyGold.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold, 
                        fontStyle = FontStyle.Italic, 
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }
    }
}

@Composable
private fun FamilyQuestion(modifier:Modifier=Modifier) {
    Canvas(modifier) {
        drawCircle(Color.White,style=Stroke(2.dp.toPx()))
        drawContext.canvas.nativeCanvas.drawText("?",size.width/2f,size.height*.73f,android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply{color=android.graphics.Color.WHITE;textSize=size.minDimension*.72f;textAlign=android.graphics.Paint.Align.CENTER;typeface=android.graphics.Typeface.DEFAULT_BOLD})
    }
}

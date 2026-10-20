package com.hurry.voicechat

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

// GITHUB LINK AUTO-ADDER (Sab Images aur Videos par ye link add hoga)
private const val FAMILY_ASSET_BASE = "https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/"
private fun familyAsset(file: String): String {
    return if (file.startsWith("http")) file else FAMILY_ASSET_BASE + file.removePrefix("/")
}

private val familyGold = Color(0xFFFFD700)
private val familyDark = Color(0xFF1A0D06)
private val familyBrown = Color(0xFF2A1610)
private val familyCardBg = Color(0xFF3B0C06)

// PERFECT GREEN SCREEN REMOVE MATRIX
private val familyRemoveGreenMatrix = ColorMatrix(floatArrayOf(
    1f, 0f, 0f, 0f, 0f,
    0f, 1f, 0f, 0f, 0f,
    0f, 0f, 1f, 0f, 0f,
    1.5f, -2.5f, 1.5f, 1f, 0f
))

// BLACK SCREEN REMOVE MATRIX
private val familyRemoveBlackMatrix = ColorMatrix(floatArrayOf(
    1f, 0f, 0f, 0f, 0f,
    0f, 1f, 0f, 0f, 0f,
    0f, 0f, 1f, 0f, 0f,
    1.5f, 1.5f, 1.5f, 0f, -0.2f
))

// GRAYSCALE + GREEN REMOVE MATRIX (For inactive tabs)
private val familyGrayscaleGreenMatrix = ColorMatrix(floatArrayOf(
    0.33f, 0.59f, 0.11f, 0f, 0f,
    0.33f, 0.59f, 0.11f, 0f, 0f,
    0.33f, 0.59f, 0.11f, 0f, 0f,
    1.5f, -2.5f, 1.5f, 1f, 0f
))

private val familyRules = listOf(
    "1. The top 10 families in the weekly family rankings will receive various rewards. Rewards are distributed every Sunday at 5:30 AM (GMT+5:30).",
    "2. There is no need to wait for official approval.",
    "3. A family can have a maximum of 100 members.",
    "4. Individuals can apply to join a family. Once the family leader approves the request, the applicant can join the family immediately.",
    "5. Each user can join only one family. Regular family members can leave the family at any time without the leader's approval.",
    "6. The family leader has the authority to remove other members from the family at any time without the removed member's consent. The family leader can also disband the family freely without official approval.",
    "7. Upon the disbanding of the family, all members will immediately leave the original family.",
    "8. Family rankings reflect the family's strength. Zaho determines family rankings based on the total spending of all family members.",
    "9. Any benefits or relationships formed within the family are the result of voluntary actions taken by the members themselves and have no connection to Zaho."
)

@Composable
fun Family(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("hurry_user", Context.MODE_PRIVATE) }
    var view by remember { mutableStateOf("main") }
    var showRules by remember { mutableStateOf(false) }
    var showApplyMode by remember { mutableStateOf(false) }
    var applyMode by remember { mutableStateOf(prefs.getString("applyMode", "free") ?: "free") }
    var familyName by remember { mutableStateOf(prefs.getString("familyName", "") ?: "") }
    var announcement by remember { mutableStateOf(prefs.getString("familyAnnouncement", "") ?: "") }
    var familyImage by remember { mutableStateOf(prefs.getString("familyImage", "") ?: "") }
    var createBusy by remember { mutableStateOf(false) }
    var createError by remember { mutableStateOf<String?>(null) }
    
    var activeModal by remember { mutableStateOf<Pair<String, String>?>(null) } 
    
    val scope = rememberCoroutineScope()
    val walletDao = remember { WalletDatabase.get(context).walletDao() }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) runCatching { context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        familyImage = uri?.toString().orEmpty()
    }
    var activeRank by remember { mutableIntStateOf(0) }
    var seconds by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            val now = Calendar.getInstance()
            val days = (Calendar.SUNDAY - now.get(Calendar.DAY_OF_WEEK) + 7) % 7
            val end = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, if (days == 0) 7 else days); set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59) }
            seconds = ((end.timeInMillis - now.timeInMillis).coerceAtLeast(0L) / 1000L).toInt()
            delay(1000)
        }
    }

    Box(Modifier.fillMaxSize().background(if (view == "create") Color.White else if (view == "top") familyBrown else familyDark)) {
        when (view) {
            "create" -> FamilyCreate(onBack = { view = "main" }, name = familyName, onName = { familyName = it }, announcement = announcement, onAnnouncement = { announcement = it }, image = familyImage, onImage = { imagePicker.launch(arrayOf("image/*")) }, mode = applyMode, onMode = { showApplyMode = true }, busy = createBusy, error = createError, onCreate = {
                if (createBusy) return@FamilyCreate
                if (familyName.isBlank()) {
                    createError = "Please enter a Family Name."
                } else {
                    createBusy = true
                    createError = null
                    scope.launch {
                        val result = withContext(Dispatchers.IO) {
                            runCatching {
                                val current = walletDao.getUserData() ?: UserDataEntity()
                                if (current.balance < 10_000_000L) {
                                    "You need 10,000,000 coins to create a family."
                                } else {
                                    walletDao.saveUserData(current.copy(balance = current.balance - 10_000_000L))
                                    val now = System.currentTimeMillis()
                                    walletDao.insertTransaction(TransactionEntity(
                                        title = "Create Family", amount = -10_000_000L,
                                        date = java.text.SimpleDateFormat("yyyy.MM.dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(now)),
                                        timestamp = now, type = "coins"
                                    ))
                                    null
                                }
                            }.getOrElse { "Could not create family. Please try again." }
                        }
                        createBusy = false
                        if (result == null) {
                            prefs.edit().putString("familyName", familyName.trim())
                                .putString("familyAnnouncement", announcement)
                                .putString("familyImage", familyImage)
                                .putString("applyMode", applyMode)
                                .putString("familyCode", prefs.getString("familyCode", "")?.ifBlank { "FAM" + System.currentTimeMillis().toString().takeLast(6) } ?: "FAM" + System.currentTimeMillis().toString().takeLast(6))
                                .putBoolean("familyCreated", true).apply()
                            view = "top"
                            Toast.makeText(context, "Family created successfully", Toast.LENGTH_SHORT).show()
                        } else createError = result
                    }
                }
            })
            "top" -> FamilyTop(onBack = { view = "main" }, onRules = { showRules = true }, activeRank = activeRank, onRank = { activeRank = it }, onOpenModal = { url, title -> activeModal = Pair(url, title) })
            else -> FamilyHome(onBack = onBack, onRules = { showRules = true }, seconds = seconds, activeRank = activeRank, onRank = { activeRank = it; if (it == 2) view = "top" }, onCreate = { view = "create" })
        }
        
        if (activeModal != null) {
            FamilyRewardModal(url = activeModal!!.first, title = activeModal!!.second, onClose = { activeModal = null })
        }

        if (showRules) FamilyRules(onClose = { showRules = false })
        if (showApplyMode) FamilyModeSheet(applyMode, { applyMode = it; prefs.edit().putString("applyMode", it).apply(); showApplyMode = false }, { showApplyMode = false })
    }
}

// --------------------------------------------------------
// MAIN HOME SCREEN (View 1)
// --------------------------------------------------------
@Composable
private fun FamilyHome(onBack: () -> Unit, onRules: () -> Unit, seconds: Int, activeRank: Int, onRank: (Int) -> Unit, onCreate: () -> Unit) {
    val scrollState = rememberScrollState()

    Box(Modifier.fillMaxSize().background(familyDark)) {
        Box(Modifier.fillMaxWidth().height(400.dp)) {
            AsyncImage(model = familyAsset("IMG_20260901_160704.png"), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, familyDark.copy(alpha=0.4f), familyDark))))
        }

        Column(Modifier.fillMaxSize().verticalScroll(scrollState)) {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 8.dp, end = 16.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = Color.White, modifier = Modifier.size(28.dp)) }
                Spacer(Modifier.weight(1f))
                FamilyQuestion(Modifier.size(28.dp).clickable(onClick = onRules))
            }

            Box(Modifier.fillMaxWidth().height(240.dp).padding(top = 10.dp)) {
                AsyncImage(model = familyAsset("1788258909655~2.jpg"), contentDescription = null, modifier = Modifier.align(Alignment.CenterStart).offset(x = (-10).dp, y = 30.dp).size(160.dp), contentScale = ContentScale.Fit, colorFilter = ColorFilter.colorMatrix(familyRemoveGreenMatrix))
                AsyncImage(model = familyAsset("1788258915366~2.jpg"), contentDescription = null, modifier = Modifier.align(Alignment.CenterEnd).offset(x = 10.dp, y = 30.dp).size(160.dp), contentScale = ContentScale.Fit, colorFilter = ColorFilter.colorMatrix(familyRemoveGreenMatrix))
                AsyncImage(model = familyAsset("IMG_20260901_161023.png"), contentDescription = null, modifier = Modifier.align(Alignment.TopCenter).offset(y = (-15).dp).size(260.dp).zIndex(2f), contentScale = ContentScale.Fit, colorFilter = ColorFilter.colorMatrix(familyRemoveGreenMatrix))
            }

            Spacer(Modifier.height(10.dp))

            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.Bottom) {
                AsyncImage(model = familyAsset("IMG_20260901_230303.jpg"), contentDescription = null, modifier = Modifier.weight(1f).height(100.dp).clickable { onRank(0) }, contentScale = ContentScale.Fit, colorFilter = if (activeRank == 0) ColorFilter.colorMatrix(familyRemoveGreenMatrix) else ColorFilter.colorMatrix(familyGrayscaleGreenMatrix))
                AsyncImage(model = familyAsset("IMG_20260901_230319.jpg"), contentDescription = null, modifier = Modifier.weight(1.2f).height(120.dp).clickable { onRank(1) }.zIndex(1f), contentScale = ContentScale.Fit, colorFilter = if (activeRank == 1) ColorFilter.colorMatrix(familyRemoveGreenMatrix) else ColorFilter.colorMatrix(familyGrayscaleGreenMatrix))
                AsyncImage(model = familyAsset("IMG_20260901_230330.jpg"), contentDescription = null, modifier = Modifier.weight(1f).height(100.dp).clickable { onRank(2) }, contentScale = ContentScale.Fit, colorFilter = if (activeRank == 2) ColorFilter.colorMatrix(familyRemoveGreenMatrix) else ColorFilter.colorMatrix(familyGrayscaleGreenMatrix))
            }

            Row(Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 10.dp).background(Brush.horizontalGradient(listOf(Color.Transparent, familyGold.copy(alpha = .12f), Color.Transparent))).border(1.dp, familyGold.copy(alpha = .18f)), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Text("Countdown", color = Color(0xFFFDF6E3), fontSize = 14.sp, modifier = Modifier.padding(end = 6.dp))
                Text("%02d".format(seconds / 86400), color = Color.White, modifier = Modifier.background(Color(0xFF1A0F02), RoundedCornerShape(4.dp)).padding(4.dp), fontWeight = FontWeight.Bold)
                Text(" Days ", color = Color(0xFFFDF6E3), fontSize = 12.sp)
                Text("%02d".format((seconds / 3600) % 24), color = Color.White, modifier = Modifier.background(Color(0xFF1A0F02), RoundedCornerShape(4.dp)).padding(4.dp), fontWeight = FontWeight.Bold)
                Text(":", color = Color.White)
                Text("%02d".format((seconds / 60) % 60), color = Color.White, modifier = Modifier.background(Color(0xFF1A0F02), RoundedCornerShape(4.dp)).padding(4.dp), fontWeight = FontWeight.Bold)
                Text(":", color = Color.White)
                Text("%02d".format(seconds % 60), color = Color.White, modifier = Modifier.background(Color(0xFF1A0F02), RoundedCornerShape(4.dp)).padding(4.dp), fontWeight = FontWeight.Bold)
            }

            AsyncImage(model = familyAsset("1788258921361~2.jpg"), contentDescription = "Top three families", modifier = Modifier.fillMaxWidth().height(150.dp), contentScale = ContentScale.FillBounds, colorFilter = ColorFilter.colorMatrix(familyRemoveGreenMatrix))
            
            for (rank in 4..50) {
                Box(Modifier.fillMaxWidth().height(64.dp), contentAlignment = Alignment.CenterStart) {
                    AsyncImage(model = familyAsset("1788259008478~2.jpg"), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds, colorFilter = ColorFilter.colorMatrix(familyRemoveGreenMatrix))
                    Text(rank.toString(), color = familyGold, fontSize = 22.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(start = 24.dp))
                }
            }
            Spacer(Modifier.height(110.dp))
        }

        // PERFECT BOTTOM OVERLAP, WIDE, AND LOWERED FIX
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().offset(y = 28.dp)) { // 28dp se aur niche laya
            AsyncImage(
                model = familyAsset("file_000000009d1081f59878648feb821b5e.png"), 
                contentDescription = null, 
                modifier = Modifier.fillMaxWidth().scale(1.15f), // Scale 1.15x se aur Wide ho gaya
                contentScale = ContentScale.FillWidth
            )
            // matchParentSize() isse Create button exactly center overlap karega
            Box(Modifier.matchParentSize(), contentAlignment = Alignment.Center) {
                AsyncImage(
                    model = familyAsset("file_0000000056508230808da89fb794a97f.png"), 
                    contentDescription = "Create Family", 
                    modifier = Modifier.fillMaxWidth(0.5f).clickable(onClick = onCreate), 
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

// --------------------------------------------------------
// TOP RANKINGS REWARDS CARDS (View 3)
// --------------------------------------------------------
@Composable
private fun FamilyTop(onBack: () -> Unit, onRules: () -> Unit, activeRank: Int, onRank: (Int) -> Unit, onOpenModal: (String, String) -> Unit) {
    val scrollState = rememberScrollState()

    Box(Modifier.fillMaxSize().background(familyBrown)) {
        Box(Modifier.fillMaxWidth().height(400.dp)) {
            AsyncImage(model = familyAsset("IMG_20260901_160704.png"), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, familyBrown.copy(alpha=0.4f), familyBrown))))
        }

        Column(Modifier.fillMaxSize().verticalScroll(scrollState)) {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(top = 8.dp, start = 8.dp, end = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = Color.White, modifier = Modifier.size(28.dp)) }
                FamilyQuestion(Modifier.size(28.dp).clickable(onClick = onRules))
            }

            Box(Modifier.fillMaxWidth().height(240.dp).padding(top = 10.dp)) {
                AsyncImage(model = familyAsset("1788258909655~2.jpg"), contentDescription = null, modifier = Modifier.align(Alignment.CenterStart).offset(x = (-10).dp, y = 30.dp).size(160.dp), contentScale = ContentScale.Fit, colorFilter = ColorFilter.colorMatrix(familyRemoveGreenMatrix))
                AsyncImage(model = familyAsset("1788258915366~2.jpg"), contentDescription = null, modifier = Modifier.align(Alignment.CenterEnd).offset(x = 10.dp, y = 30.dp).size(160.dp), contentScale = ContentScale.Fit, colorFilter = ColorFilter.colorMatrix(familyRemoveGreenMatrix))
                AsyncImage(model = familyAsset("IMG_20260901_161023.png"), contentDescription = null, modifier = Modifier.align(Alignment.TopCenter).offset(y = (-15).dp).size(260.dp).zIndex(2f), contentScale = ContentScale.Fit, colorFilter = ColorFilter.colorMatrix(familyRemoveGreenMatrix))
            }

            Spacer(Modifier.height(10.dp))

            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.Bottom) {
                AsyncImage(model = familyAsset("IMG_20260901_230303.jpg"), contentDescription = null, modifier = Modifier.weight(1f).height(100.dp).clickable { onRank(0) }, contentScale = ContentScale.Fit, colorFilter = if (activeRank == 0) ColorFilter.colorMatrix(familyRemoveGreenMatrix) else ColorFilter.colorMatrix(familyGrayscaleGreenMatrix))
                AsyncImage(model = familyAsset("IMG_20260901_230319.jpg"), contentDescription = null, modifier = Modifier.weight(1.2f).height(120.dp).clickable { onRank(1) }.zIndex(1f), contentScale = ContentScale.Fit, colorFilter = if (activeRank == 1) ColorFilter.colorMatrix(familyRemoveGreenMatrix) else ColorFilter.colorMatrix(familyGrayscaleGreenMatrix))
                AsyncImage(model = familyAsset("IMG_20260901_230330.jpg"), contentDescription = null, modifier = Modifier.weight(1f).height(100.dp).clickable { onRank(2) }, contentScale = ContentScale.Fit, colorFilter = if (activeRank == 2) ColorFilter.colorMatrix(familyRemoveGreenMatrix) else ColorFilter.colorMatrix(familyGrayscaleGreenMatrix))
            }

            Spacer(Modifier.height(40.dp))

            RewardCard(title = "TOP 1 Reward", 
                item1 = Triple("Medal *7 days", "1000196572-background (1).mp4", "black"), 
                item2 = Triple("Top1 Tag *7 days", "", "tag"), 
                item3 = Triple("Vehicle *7 days", "IMG_20260918_141104.jpg", "vehicle"), 
                frame = Triple("Frames *7 days", "gemini_generated_video_0d259062.mp4", "green"),
                familyFrame = Triple("Family Frame *7 d", "", "tag"),
                roomTheme = Triple("Room Theme *7 d", "IMG-20260914-WA0043.jpg", "tall"),
                onClick = onOpenModal, label = "TOP1 Family"
            )

            Spacer(Modifier.height(40.dp))

            RewardCard(title = "TOP 2 Reward", 
                item1 = Triple("Medal *7 days", "1000196573-background (1).mp4", "black"), 
                item2 = Triple("Top2 Tag *7 days", "", "tag"), 
                item3 = Triple("Vehicle *7 days", "IMG_20260919_010054.jpg", "vehicle"), 
                frame = Triple("Frames *7 days", "gemini_generated_video_0d259062.mp4", "green"),
                familyFrame = Triple("Family Frame *7 d", "", "tag"),
                roomTheme = Triple("Room Theme *7 d", "IMG-20260914-WA0045.jpg", "tall"),
                onClick = onOpenModal, label = "TOP2 Family"
            )

            Spacer(Modifier.height(40.dp))

            RewardCard(title = "TOP 3 Reward", 
                item1 = Triple("Medal *7 days", "1000196574-background (1).mp4", "black"), 
                item2 = Triple("Top3 Tag *7 days", "", "tag"), 
                item3 = Triple("Vehicle *7 days", "IMG_20260919_222412.jpg", "mixed"), 
                frame = Triple("Frames *7 days", "gemini_generated_video_123c050b~2.mp4", "black"),
                familyFrame = Triple("Family Frame *7 d", "", "tag"),
                roomTheme = Triple("Room Theme *7 d", "IMG-20260914-WA0046.jpg", "tall"),
                onClick = onOpenModal, label = "TOP3 Family"
            )

            Spacer(Modifier.height(40.dp))

            RewardCard(title = "TOP 4 TO 10\nReward", 
                item1 = Triple("Medal *3 days", "", "tag"), 
                item2 = Triple("Frames *3 days", "gemini_generated_video_0d259062.mp4", "green"), 
                item3 = Triple("Vehicle *3 days", "IMG_20260919_222041.jpg", "mixed"), 
                frame = null, familyFrame = null, roomTheme = null,
                onClick = onOpenModal, label = "TOP4-10 Family"
            )

            Spacer(Modifier.height(40.dp))
        }
    }
}

// REWARD CARD COMPONENT
@Composable
private fun RewardCard(title: String, item1: Triple<String, String, String>, item2: Triple<String, String, String>, item3: Triple<String, String, String>, frame: Triple<String, String, String>?, familyFrame: Triple<String, String, String>?, roomTheme: Triple<String, String, String>?, onClick: (String, String) -> Unit, label: String) {
    Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        Box(Modifier.fillMaxWidth().padding(top = 20.dp).background(familyCardBg, RoundedCornerShape(12.dp)).border(2.dp, familyGold, RoundedCornerShape(12.dp)).padding(top = 40.dp, bottom = 24.dp)) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    RewardItemBox(item1, onClick, label)
                    RewardItemBox(item2, onClick, label)
                    RewardItemBox(item3, onClick, label)
                }
                if (frame != null && familyFrame != null) {
                    Spacer(Modifier.height(20.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { RewardItemBox(frame, onClick, label) }
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { RewardItemBox(familyFrame, onClick, label) }
                    }
                }
                if (roomTheme != null) {
                    Spacer(Modifier.height(20.dp))
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

// REWARD THUMBNAIL BOX
@Composable
private fun RewardItemBox(item: Triple<String, String, String>, onClick: (String, String) -> Unit, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(if(item.third == "tall") 160.dp else 90.dp).clickable { if(item.second.isNotBlank()) onClick(item.second, label) }) {
        Box(Modifier.fillMaxWidth().aspectRatio(if(item.third == "tall") 0.8f else 1f).background(Brush.verticalGradient(listOf(Color(0xFF8C3A19), Color(0xFF5C1A06))), RoundedCornerShape(12.dp)).border(2.dp, familyGold, RoundedCornerShape(12.dp)).padding(4.dp), contentAlignment = Alignment.Center) {
            if (item.second.isNotBlank()) {
                val colorFilter = when(item.third) {
                    "black" -> ColorFilter.colorMatrix(familyRemoveBlackMatrix)
                    "green" -> ColorFilter.colorMatrix(familyRemoveGreenMatrix)
                    else -> null
                }
                AsyncImage(model = familyAsset(item.second), contentDescription = null, modifier = Modifier.fillMaxSize().scale(if (item.third == "tall") 1f else 1.3f), contentScale = ContentScale.Crop, colorFilter = colorFilter)
            }
        }
        Spacer(Modifier.height(6.dp))
        Row { repeat(5) { Icon(Icons.Default.Add, null, tint = familyGold, modifier = Modifier.size(10.dp)) } } 
        Text(item.first, color = Color(0xFFFDE68A), fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
    }
}

// VIDEO MODAL UI POPUP WITH MISSING BARS/LINES ADDED
@Composable
private fun FamilyRewardModal(url: String, title: String, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f)).clickable(onClick = onClose), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // MISSING LINES/BARS ADDED HERE
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.offset(y = 24.dp).zIndex(2f)) {
                    Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(end = 6.dp)) {
                        Box(Modifier.width(32.dp).height(2.dp).background(familyGold))
                        Spacer(Modifier.height(3.dp))
                        Box(Modifier.width(20.dp).height(2.dp).background(familyGold))
                    }
                    Text(title, color = familyGold, fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
                    Column(horizontalAlignment = Alignment.Start, modifier = Modifier.padding(start = 6.dp)) {
                        Box(Modifier.width(32.dp).height(2.dp).background(familyGold))
                        Spacer(Modifier.height(3.dp))
                        Box(Modifier.width(20.dp).height(2.dp).background(familyGold))
                    }
                }
                
                AsyncImage(
                    model = familyAsset(url),
                    contentDescription = null,
                    modifier = Modifier.size(320.dp).zIndex(1f),
                    contentScale = ContentScale.Fit,
                    colorFilter = if(url.contains("gemini") || url.contains("green")) ColorFilter.colorMatrix(familyRemoveGreenMatrix) else ColorFilter.colorMatrix(familyRemoveBlackMatrix)
                )
                
                Row(modifier = Modifier.offset(y = (-20).dp).zIndex(2f)) {
                    repeat(5) { Icon(Icons.Default.Add, null, tint = familyGold, modifier = Modifier.size(32.dp)) } 
                }
            }
        }
    }
}

// --------------------------------------------------------
// CREATE FAMILY (View 2)
// --------------------------------------------------------
@Composable
private fun FamilyCreate(onBack:()->Unit,name:String,onName:(String)->Unit,announcement:String,onAnnouncement:(String)->Unit,image:String,onImage:()->Unit,mode:String,onMode:()->Unit,busy:Boolean,error:String?,onCreate:()->Unit) {
    Column(Modifier.fillMaxSize().background(Color.White)) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(top=16.dp,start=8.dp),verticalAlignment=Alignment.CenterVertically){IconButton(onClick=onBack){Icon(Icons.Default.ArrowBack,"Back",tint=Color.Black,modifier=Modifier.size(28.dp))}}
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom=20.dp)) {
            Box(Modifier.align(Alignment.CenterHorizontally).padding(top=4.dp).size(96.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFF9FAFB)).border(2.dp,familyGold,RoundedCornerShape(8.dp)).clickable(onClick=onImage),contentAlignment=Alignment.Center) {
                if(image.isNotBlank()) AsyncImage(model=image,contentDescription="Family image",modifier=Modifier.fillMaxSize(),contentScale=ContentScale.Crop) else Icon(Icons.Default.Add,"Upload Image",tint=Color.Gray,modifier=Modifier.size(36.dp))
            }
            Text("Upload Image",color=Color.Gray,fontSize=14.sp,fontWeight=FontWeight.Bold,modifier=Modifier.align(Alignment.CenterHorizontally).padding(top=8.dp))
            Column(Modifier.padding(horizontal=20.dp).padding(top=12.dp)) {
                Text("Family name",color=Color.Black,fontWeight=FontWeight.Bold,fontSize=14.sp)
                OutlinedTextField(value=name,onValueChange=onName,modifier=Modifier.fillMaxWidth(),singleLine=true,shape=RoundedCornerShape(12.dp),colors=OutlinedTextFieldDefaults.colors(unfocusedContainerColor=Color(0xFFF3F4F6),focusedContainerColor=Color(0xFFF3F4F6)))
                Spacer(Modifier.height(12.dp)); Text("Family Announcement",color=Color.Black,fontWeight=FontWeight.Bold,fontSize=14.sp)
                OutlinedTextField(value=announcement,onValueChange=onAnnouncement,modifier=Modifier.fillMaxWidth(),singleLine=true,shape=RoundedCornerShape(12.dp),colors=OutlinedTextFieldDefaults.colors(unfocusedContainerColor=Color(0xFFF3F4F6),focusedContainerColor=Color(0xFFF3F4F6)))
                Spacer(Modifier.height(12.dp)); Text("Setting",color=Color.Gray,fontWeight=FontWeight.Bold,fontSize=14.sp)
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFFF3F4F6)).clickable(onClick=onMode).padding(16.dp),verticalAlignment=Alignment.CenterVertically){Text("Apply Mode",color=Color.Black,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f));Text(if(mode=="admin")"Apply Mode / Admin & Owner" else "Free mode",color=Color.Gray,fontSize=12.sp);Icon(Icons.Default.ChevronRight,null,tint=Color.Gray)}
                if (error != null) Text(error,color=Color(0xFFDC2626),fontSize=13.sp,fontWeight=FontWeight.Medium,modifier=Modifier.padding(top=10.dp))
                Spacer(Modifier.height(16.dp)); Text("1. Creating a family requires ≥ LV.5",color=Color.Gray,fontSize=13.sp)
                Text("2. Family info can be modified once a week",color=Color.Gray,fontSize=13.sp,modifier=Modifier.padding(top=4.dp))
                Text("3. If family with only 1 member for 7 days will be disbanded automatically",color=Color.Gray,fontSize=13.sp,modifier=Modifier.padding(top=4.dp))
            }
        }
        Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom=16.dp),contentAlignment=Alignment.Center){Button(onClick=onCreate,enabled=!busy,shape=RoundedCornerShape(50),colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF00BFA5)),modifier=Modifier.height(52.dp).widthIn(min=280.dp)){Text(if(busy) "Creating family..." else "Created with 10,000,000 coins",color=Color.White,fontWeight=FontWeight.Bold,fontSize=16.sp)}}
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

@Composable
private fun FamilyModeSheet(selected:String,onSelect:(String)->Unit,onClose:()->Unit) {
    Dialog(onDismissRequest=onClose,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.5f)).clickable(onClick=onClose),contentAlignment=Alignment.BottomCenter) {
            Column(Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(topStart=24.dp,topEnd=24.dp)).background(Color.White).padding(horizontal=24.dp,vertical=8.dp).clickable(enabled=false, onClick={})) {
                Row(Modifier.fillMaxWidth().weight(1f).clickable{onSelect("free")},verticalAlignment=Alignment.CenterVertically){Text("Free mode",color=Color.Black,fontSize=14.sp,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f));RadioButton(selected=="free",onClick={onSelect("free")})}
                HorizontalDivider(color=Color(0xFFF3F4F6))
                Row(Modifier.fillMaxWidth().weight(1f).clickable{onSelect("admin")},verticalAlignment=Alignment.CenterVertically){Text("Apply Mode / Admin & Owner",color=Color.Black,fontSize=14.sp,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f));RadioButton(selected=="admin",onClick={onSelect("admin")})}
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

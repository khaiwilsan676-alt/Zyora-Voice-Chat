package com.hurry.voicechat

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import android.content.Context
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.rememberAsyncImagePainter

private const val LEVEL_ASSET_BASE = "https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/"
private fun levelAsset(file: String) = LEVEL_ASSET_BASE + file

private data class NativeLevelTier(
    val range: String,
    val rightGraphic: String,
    val badge: String,
    val rewards: List<Pair<Int, String>>
)

private val levelTiers = listOf(
    NativeLevelTier("Lv.1 - Lv.10","IMG_20260911_230430.png","IMG_20260917_220530.png",listOf(2 to "16,000",4 to "25,000",6 to "35,000",8 to "46,000",10 to "58,000")),
    NativeLevelTier("Lv.11 - Lv.20","IMG_20260911_230448.png","IMG_20260917_220613.png",listOf(12 to "70,000",14 to "84,000",16 to "98,000",18 to "114,000",20 to "130,000")),
    NativeLevelTier("Lv.21 - Lv.30","IMG_20260911_230538.png","IMG_20260917_220641.png",listOf(22 to "146,000",24 to "164,000",26 to "280,000",28 to "430,000",30 to "676,000")),
    NativeLevelTier("Lv.31 - Lv.40","IMG_20260911_230602.png","IMG_20260917_220710.png",listOf(32 to "1,000,000",34 to "1,600,000",36 to "11,200,000",38 to "11,400,000",40 to "16,000,000")),
    NativeLevelTier("Lv.41 - Lv.50","IMG_20260911_230631.png","IMG_20260917_220733.png",listOf(42 to "22,000,000",44 to "29,000,000",46 to "37,000,000",48 to "47,000,000",50 to "58,000,000")),
    NativeLevelTier("Lv.51 - Lv.60","IMG_20260911_230722.png","IMG_20260917_220753.png",listOf(52 to "70,000,000",54 to "84,000,000",56 to "100,000,000",58 to "120,000,000",60 to "140,000,000")),
    NativeLevelTier("Lv.61 - Lv.70","IMG_20260911_230739.png","IMG_20260917_220815.png",listOf(62 to "160,000,000",64 to "180,000,000",66 to "200,000,000",68 to "230,000,000",70 to "260,000,000")),
    NativeLevelTier("Lv.71 - Lv.80","IMG_20260911_230808.png","IMG_20260917_220839.png",listOf(72 to "290,000,000",74 to "320,000,000",76 to "360,000,000",78 to "400,000,000",80 to "440,000,000")),
    NativeLevelTier("Lv.81 - Lv.90","IMG_20260911_230826.png","IMG_20260917_220900.png",listOf(82 to "480,000,000",84 to "520,000,000",86 to "570,000,000",88 to "620,000,000",90 to "680,000,000")),
    NativeLevelTier("Lv.91 - Lv.100","file_00000000b06081fabde2d7eac02ce8c2.png","IMG_20260917_220922.png",listOf(92 to "740,000,000",94 to "800,000,000",96 to "860,000,000",98 to "930,000,000",100 to "1,000,000,000"))
)

private val levelGold = Color(0xFFFCD34D)
private val levelCardBrush = Brush.horizontalGradient(listOf(Color(0xFF06080D), Color(0xFF080D17), Color(0xFF132C54)))
private val levelTextMuted = Color(0xFFB8BEC9)

@Composable
private fun LevelAsset(
    file: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    contentScale: ContentScale = ContentScale.Fit
) {
    AsyncImage(model = levelAsset(file), contentDescription = description, modifier = modifier, contentScale = contentScale)
}

@Composable
private fun LevelRewardRow(title: String, subtitle: String? = null, trailing: @Composable RowScope.() -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(levelCardBrush, RoundedCornerShape(6.dp)).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f).padding(end = 8.dp)) {
            Text(title, color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
            if (subtitle != null) Text(subtitle, color = Color(0xFF9CA3AF), fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically, content = trailing)
    }
}

@Composable
private fun LevelUpgradeDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).widthIn(max = 340.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB)),
            shadowElevation = 20.dp
        ) {
            Column(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("◆", color = Color(0xFFF59E0B), fontSize = 18.sp)
                    Text("  How to upgrade  ", color = Color(0xFF111827), fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp)
                    Text("◆", color = Color(0xFFF59E0B), fontSize = 18.sp)
                }
                Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
                    val methods = listOf(
                        Triple("Send Gifts", "1 coins=1 EXP", 0),
                        Triple("Join rooms", "2000 EXP/Day", 1),
                        Triple("Share Rooms", "2000 EXP/Day", 2),
                        Triple("Take the mic and unmute in your ov", "1 min = 500 exp, 10000 exp/day", 3),
                        Triple("Take the mic and unmute in others'", "1 min = 500 exp, 20000 exp/day", 4)
                    )
                    methods.forEachIndexed { index, item ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                                when (item.third) {
                                    0 -> Icon(Icons.Default.CardGiftcard, null, tint = Color(0xFF3B82F6), modifier = Modifier.size(40.dp))
                                    1 -> Icon(Icons.Default.Home, null, tint = Color(0xFF3B82F6), modifier = Modifier.size(40.dp))
                                    2 -> Icon(Icons.Default.Share, null, tint = Color(0xFF3B82F6), modifier = Modifier.size(40.dp))
                                    else -> Icon(Icons.Default.Mic, null, tint = Color(0xFF3B82F6), modifier = Modifier.size(40.dp))
                                }
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text(item.first, color = Color(0xFF111827), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, lineHeight = 17.sp, maxLines = 1, overflow = TextOverflow.Clip)
                                Text(item.second, color = Color(0xFF6B7280), fontSize = 13.sp, lineHeight = 16.sp)
                            }
                        }
                        if (index != methods.lastIndex) HorizontalDivider(color = Color(0xFFF3F4F6))
                    }
                }
                Box(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 16.dp)) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                    ) {
                        Text("Confirm", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun Level(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val userPrefs = remember { context.getSharedPreferences("hurry_user", Context.MODE_PRIVATE) }
    val storedName = userPrefs.getString("name", "")?.trim().orEmpty()
    val userName = storedName.takeIf { it.isNotBlank() && it.lowercase() !in listOf("guest", "user", "null", "undefined") }
    val userUid = userPrefs.getString("uid", "")?.trim().orEmpty().ifBlank {
        userPrefs.getString("phone", "")?.trim().orEmpty()
    }
    val userPhoto = userPrefs.getString("photo", "")?.trim().orEmpty()
    val profilePainter = rememberAsyncImagePainter(model = levelAsset("file_000000007044820ea729df406d1dc320.png"))
    val profileIntrinsic = profilePainter.intrinsicSize
    val profileAspect = if (profileIntrinsic.width.isFinite() && profileIntrinsic.height.isFinite() && profileIntrinsic.width > 0f && profileIntrinsic.height > 0f) profileIntrinsic.width / profileIntrinsic.height else 3.25f
    val profileFrameWidth = LocalConfiguration.current.screenWidthDp.dp
    val profileFrameHeight = profileFrameWidth / profileAspect
    val displayName = userName ?: userUid.take(8).ifBlank { "Guest" }
    var showUpgrade by remember { mutableStateOf(false) }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val activeTierIndex by remember {
        derivedStateOf {
            val first = listState.firstVisibleItemIndex
            first.coerceIn(0, levelTiers.lastIndex)
        }
    }
    val currentTier = levelTiers[activeTierIndex]

    Box(Modifier.fillMaxSize().background(Color(0xFF04060A))) {
        LevelAsset("file_00000000e02481f4bb2153e2714aca47.png", Modifier.fillMaxWidth().height(280.dp).align(Alignment.TopCenter), contentScale = ContentScale.Crop)
        Box(
            Modifier.align(Alignment.TopStart).offset(x = (-40).dp, y = (-40).dp).size(260.dp)
                .blur(90.dp).clip(CircleShape).background(Color(0x4D1D4ED8))
        )
        Box(Modifier.fillMaxWidth().height(280.dp).background(Brush.verticalGradient(listOf(Color.Transparent, Color(0x6604060A), Color(0xFF04060A)))))

        Column(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxWidth().statusBarsPadding()) {
                Box(Modifier.fillMaxWidth().height(56.dp).padding(top = 8.dp), contentAlignment = Alignment.Center) {
                    IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart).padding(start = 2.dp)) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White, modifier = Modifier.size(26.dp))
                    }
                    Text("Level", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    IconButton(onClick = { showUpgrade = true }, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 2.dp)) {
                        Icon(Icons.Default.HelpOutline, "How to upgrade", tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                }

                Box(Modifier.fillMaxWidth().height((profileFrameHeight - 48.dp).coerceAtLeast(1.dp))) {
                    Box(Modifier.width(profileFrameWidth).height(profileFrameHeight).offset(x = 0.dp, y = (-40).dp)) {
                        Image(
                            painter = profilePainter,
                            contentDescription = "Top Card Frame",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.FillBounds
                        )
                        Row(Modifier.fillMaxSize().padding(horizontal = 32.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(48.dp).clip(CircleShape).background(Color(0xFF626A75)), contentAlignment = Alignment.Center) {
                                if (userPhoto.isNotBlank()) {
                                    AsyncImage(
                                        model = userPhoto,
                                        contentDescription = "User",
                                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Text(displayName.take(1).uppercase(), color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(displayName, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Spacer(Modifier.width(8.dp))
                                    LevelAsset(levelTiers[0].badge, Modifier.height(28.dp).width(58.dp))
                                }
                                Spacer(Modifier.height(8.dp))
                                Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha=.25f))) {
                                    Box(Modifier.fillMaxWidth(.38f).fillMaxHeight().clip(RoundedCornerShape(20.dp)).background(Brush.horizontalGradient(listOf(Color(0xFFFFE072), Color(0xFFF4B63F))))) {
                                        Box(Modifier.align(Alignment.CenterEnd).offset(x = 6.dp).size(12.dp).border(2.dp, Color(0xFFF4B63F), CircleShape).clip(CircleShape).background(Color.White))
                                    }
                                }
                                Text("4.5k/20.2k remaining to reach Level 5 >", color = Color.White.copy(alpha=.9f), fontSize = 10.5.sp, modifier = Modifier.padding(top = 5.dp), maxLines = 1)
                            }
                        }
                    }
                }

                Box(Modifier.fillMaxWidth().height(38.dp).padding(horizontal = 16.dp)) {
                    LevelAsset("file_000000006688821197edc482e295d3fd.png", Modifier.align(Alignment.TopStart).offset(x = (-0).dp, y = (-14).dp).height(28.dp))
                    Row(Modifier.align(Alignment.CenterStart).offset(y = (-16).dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.ArrowUpward, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Box(Modifier.width(14.dp).height(1.5.dp).background(Color.White, RoundedCornerShape(2.dp)))
                            Spacer(Modifier.height(2.dp))
                            Box(Modifier.width(8.dp).height(1.5.dp).background(Color.White, RoundedCornerShape(2.dp)))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("Update level to ${currentTier.range}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                    LevelAsset(currentTier.rightGraphic, Modifier.align(Alignment.CenterEnd).offset(y = (-18).dp, x = 6.dp).size(124.dp))
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(36.dp)
            ) {
                itemsIndexed(levelTiers, key = { index, tier -> tier.range }) { index, tier ->
                    Column(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                        Row(Modifier.fillMaxWidth().heightIn(min = 60.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                Text("⌃", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(7.dp))
                                Text("Upgrade to level ${tier.range}", color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                            }
                            LevelAsset(tier.rightGraphic, Modifier.size(60.dp))
                        }

                        Row(Modifier.fillMaxWidth().padding(horizontal = 2.dp).padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.weight(1f).height(76.dp).background(Brush.linearGradient(listOf(Color(0xFF06080D), Color(0xFF132C54).copy(alpha = .60f))), RoundedCornerShape(8.dp))) {
                                Text("Lv.${tier.rewards[0].first}", color = Color.White.copy(alpha=.70f), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.TopStart).padding(start = 6.dp, top = 4.dp))
                                Column(Modifier.fillMaxSize().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                    LevelAsset("file_00000000b2d481fd8cd233482dbeb9ef.png", Modifier.size(32.dp))
                                    Spacer(Modifier.height(6.dp))
                                    Text(tier.rewards[0].second, color = Color.White.copy(alpha=.9f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            Column(Modifier.weight(1f).height(76.dp).background(Brush.linearGradient(listOf(Color(0xFF06080D), Color(0xFF132C54).copy(alpha = .60f))), RoundedCornerShape(8.dp)).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                Box(Modifier.background(Brush.horizontalGradient(listOf(Color(0xFF177488), Color(0xFF1EA3B3))), RoundedCornerShape(5.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                    Text("Entry Tag", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                                Text("Entry", color = Color.White.copy(alpha=.9f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 5.dp))
                            }
                            Column(Modifier.weight(1f).height(76.dp).background(Brush.linearGradient(listOf(Color(0xFF06080D), Color(0xFF132C54).copy(alpha = .60f))), RoundedCornerShape(8.dp)).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                Spacer(Modifier.size(32.dp))
                                Text("Frame", color = Color.White.copy(alpha=.9f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            LevelRewardRow("Level ${tier.range.removePrefix("Lv.").replace(" - Lv.", "-Lv.")}", "Level badge upgraded") {
                                LevelAsset(tier.badge, Modifier.height(40.dp).width(68.dp))
                            }
                            LevelRewardRow("Room Send image", if (index == 0) "Lv.5" else null) {
                                LevelAsset("file_00000000ac708211bca5d461c021b0af.png", Modifier.size(32.dp))
                            }
                            if (index >= 2) {
                                LevelRewardRow("Background Image") {
                                    Box(Modifier.size(32.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFF131F33)), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Image, contentDescription = null, tint = Color(0xFF6EE7B7), modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                            if (index >= 7) {
                                LevelRewardRow("Room Theme") {
                                    Box(Modifier.size(32.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFF131F33)), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Public, contentDescription = null, tint = Color(0xFFE879F9), modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                            tier.rewards.drop(1).forEach { reward ->
                                LevelRewardRow("Level ${reward.first} Reward") {
                                    LevelAsset("file_00000000b2d481fd8cd233482dbeb9ef.png", Modifier.size(24.dp))
                                    Spacer(Modifier.width(5.dp))
                                    Text(reward.second, color = levelGold, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showUpgrade) LevelUpgradeDialog { showUpgrade = false }
}

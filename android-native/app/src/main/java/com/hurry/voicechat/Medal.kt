package com.hurry.voicechat

import android.content.Context
import android.graphics.Color as AndroidColor
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.os.Build
import android.view.View
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil3.compose.AsyncImage

data class HurryMedal(val id: String, val name: String, val image: String)

private const val MEDAL_ASSET_BASE = "https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/"
private fun medalAsset(name: String) = MEDAL_ASSET_BASE + name

private data class NativeMedal(
    val id: String, val name: String, val video: String, val stars: Int = 4,
    val category: String = "achievement", val variant: String = "black",
    val tier: String? = null, val size: Int = 220, val giftText: String = "Event Based"
)

private val nativeMedals = listOf(
    NativeMedal("1","Rich","VID_20260924_124439.mp4",5,variant="green",tier="rich",giftText="0/5000000000000 Coins Of gifts Send"),
    NativeMedal("2","Rich","1000200509-background.mp4",4,tier="rich",giftText="0/400000000000 Coins Of gifts Send"),
    NativeMedal("3","Rich","1000200510-background.mp4",5,tier="rich",giftText="0/30000000000 Coins Of gifts Send"),
    NativeMedal("new-1","Super Gamer","1000201032-background.mp4",tier=null,giftText="0/1000000000000 Coins You won from game"),
    NativeMedal("new-2","Millionaire","1000200518-background.mp4",tier="millionaire",giftText="0/1000000 Online Recharge"),
    NativeMedal("new-3","Millionaire","1000200521-background.mp4",tier="millionaire",giftText="0/50000000 Online Recharge"),
    NativeMedal("new-4","Millionaire","1000200522-background.mp4",tier="millionaire",giftText="0/100000000 Online Recharge"),
    NativeMedal("new-5","Room Top 1","1000200514-background.mp4",tier="roomtop"),
    NativeMedal("new-6","Room Top 2","1000200515-background.mp4",tier="roomtop"),
    NativeMedal("new-7","Room Top 3","1000200516-background.mp4",tier="roomtop"),
    NativeMedal("new-8","Medal 13","1000200511-background.mp4",tier="medal111213",giftText="0/50000 Coins Of gifts Send"),
    NativeMedal("new-9","Medal 12","1000200512-background.mp4",tier="medal111213",giftText="0/50000 Coins Of gifts Send"),
    NativeMedal("new-10","Medal 11","1000200513-background.mp4",tier="medal111213",giftText="0/50000 Coins Of gifts Send"),
    NativeMedal("4","VIP2","1000200505-background.mp4",tier="vip",giftText="0/50000 Coins Of gifts Send"),
    NativeMedal("5","VIP1","1000200506-background.mp4",tier="vip",giftText="0/50000 Coins Of gifts Send"),
    NativeMedal("6","Pure Love","1000200507-background.mp4",tier="vip",giftText="0/50000 Coins Of gifts Send")
)

private val medalPurple = Color(0xFF2A1B5E)
private val medalCard = Color(0xFF1E1245)
private val medalGold = Color(0xFFFACC15)
private val medalBg = Color(0xFF02050E)

@Composable
private fun MedalVideo(
    video: String, 
    modifier: Modifier = Modifier, 
    animated: Boolean = true, 
    variant: String = "black",
    isColorless: Boolean = false
) {
    val context = LocalContext.current
    val player = remember(video) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(medalAsset(video)))
            repeatMode = Player.REPEAT_MODE_ONE
            volume = 0f
            playWhenReady = animated
            prepare()
        }
    }
    DisposableEffect(player) {
        onDispose { player.release() }
    }
    
    AndroidView(
        modifier = modifier.aspectRatio(1f),
        factory = { ctx ->
            android.view.TextureView(ctx).apply {
                isOpaque = false
                
                val baseValues = if (variant == "green") {
                    floatArrayOf(
                        1f, 0f, 0f, 0f, 0f,
                        0f, 1f, 0f, 0f, 0f,
                        0f, 0f, 1f, 0f, 0f,
                        1.5f, -2.5f, 1.5f, 1f, 0f
                    )
                } else {
                    floatArrayOf(
                        1f, 0f, 0f, 0f, 0f,
                        0f, 1f, 0f, 0f, 0f,
                        0f, 0f, 1f, 0f, 0f,
                        4f, 4f, 4f, 0f, -204f
                    )
                }

                val chromaMatrix = android.graphics.ColorMatrix(baseValues)
                val finalColorFilter = if (isColorless) {
                    val grayMatrix = android.graphics.ColorMatrix()
                    grayMatrix.setSaturation(0f)
                    grayMatrix.setConcat(grayMatrix, chromaMatrix)
                    ColorMatrixColorFilter(grayMatrix)
                } else {
                    ColorMatrixColorFilter(chromaMatrix)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    setRenderEffect(android.graphics.RenderEffect.createColorFilterEffect(finalColorFilter))
                } else {
                    setLayerType(View.LAYER_TYPE_HARDWARE, Paint().apply { this.colorFilter = finalColorFilter })
                }
            }
        },
        update = { view ->
            player.setVideoTextureView(view)
            player.playWhenReady = animated
            if (animated && player.playbackState == Player.STATE_IDLE) player.prepare()
            if (!animated && player.playbackState == Player.STATE_READY) player.pause()
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Medal(onBack: () -> Unit = {}, medals: List<HurryMedal> = emptyList()) {
    var activeTab by remember { mutableStateOf("achievement") }
    var selected by remember { mutableStateOf<NativeMedal?>(null) }
    var activeTier by remember { mutableIntStateOf(0) }
    var overlay by remember { mutableStateOf<String?>(null) }
    val tierMedals = remember(selected) {
        selected?.tier?.let { group -> nativeMedals.filter { it.tier == group } } ?: listOfNotNull(selected)
    }
    val display = tierMedals.getOrNull(activeTier) ?: selected

    Box(Modifier.fillMaxSize().background(medalBg)) {
        AsyncImage(
            model = medalAsset("file_00000000f1dc821196bf96f688c3b2f6.png"),
            contentDescription = null,
            modifier = Modifier.fillMaxWidth().fillMaxHeight(.48f).align(Alignment.TopCenter),
            contentScale = ContentScale.Crop
        )
        Box(
            Modifier.fillMaxWidth().fillMaxHeight(.48f).align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, medalBg.copy(alpha = .24f), medalBg),
                        startY = 0f,
                        endY = 1050f
                    )
                )
        )
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxWidth().statusBarsPadding().padding(top = 19.dp, start = 12.dp, end = 12.dp)) {
                Box(Modifier.fillMaxWidth().height(42.dp), contentAlignment = Alignment.Center) {
                    IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                    }
                    Text("Medal", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { overlay = "Rules" }, modifier = Modifier.align(Alignment.CenterEnd)) {
                        Icon(Icons.Default.HelpOutline, "Rules", tint = Color.White.copy(alpha = .85f))
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 14.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(40.dp).height(1.dp).background(Color(0xFFA89BBF)))
                    Text("  The Medal I Wear  ", color = Color(0xFFF2EEF9), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Box(Modifier.width(40.dp).height(1.dp).background(Color(0xFFA89BBF)))
                }
                Column {
                    repeat(2) { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            repeat(5) { col ->
                                Box(
                                    Modifier.weight(1f).aspectRatio(1f).border(1.dp, Color.White.copy(alpha=.85f), RoundedCornerShape(5.dp))
                                        .background(Color(0x99281B54), RoundedCornerShape(5.dp))
                                        .clickable { overlay = "Unequipped Medals" },
                                    contentAlignment = Alignment.Center
                                ) { Icon(Icons.Default.Add, null, tint = Color(0xFFE2D5FF), modifier = Modifier.size(22.dp)) }
                            }
                        }
                        if (row == 0) Spacer(Modifier.height(6.dp))
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 12.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Text("Obtained Medal(s): ", color = Color(0xFFE5E1EF), fontSize = 14.sp)
                    Text("3", color = medalGold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("  Check>", color = medalGold, fontSize = 14.sp, modifier = Modifier.clickable { overlay = "Obtained Medals" })
                }
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    listOf("achievement" to "Achievements", "activity" to "Activities", "gift" to "gift").forEach { (key,label) ->
                        Column(Modifier.weight(1f).clickable { activeTab = key }.padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(label, color = if(activeTab == key) Color.White else Color(0xFF8B79B5), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(7.dp))
                            Box(Modifier.width(8.dp).height(2.dp).background(if(activeTab == key) medalGold else Color.Transparent, RoundedCornerShape(2.dp)))
                        }
                    }
                }
            }
            val visibleMedals = nativeMedals.filter { it.category == activeTab }
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(visibleMedals, key = { it.id }) { medal ->
                    Column(
                        Modifier.height(190.dp).background(Color(0xFF130D26), RoundedCornerShape(5.dp))
                            .clickable { selected = medal; activeTier = (nativeMedals.filter { it.tier == medal.tier }.indexOf(medal)).coerceAtLeast(0) }
                            .padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                            MedalVideo(
                                video = medal.video, 
                                modifier = Modifier.size((medal.size.coerceAtMost(250) / 2).dp).aspectRatio(1f), 
                                animated = false, 
                                variant = medal.variant,
                                isColorless = true
                            )
                        }
                        Text(medal.name, color = Color(0xFFE4DFED), fontSize = 13.sp, maxLines = 1, textAlign = TextAlign.Center)
                    }
                }
            }
        }

        if (selected != null && display != null) {
            Column(Modifier.fillMaxSize().background(Color.Black).verticalScroll(rememberScrollState())) {
                Box(Modifier.fillMaxWidth().height(260.dp)) {
                    AsyncImage(model = medalAsset("IMG_20260924_132006.png"), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    IconButton(onClick = { selected = null; activeTier = 0 }, modifier = Modifier.statusBarsPadding().align(Alignment.TopStart).padding(top = 22.dp)) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                    }
                    IconButton(onClick = { overlay = "Rules" }, modifier = Modifier.statusBarsPadding().align(Alignment.TopEnd).padding(top = 22.dp)) {
                        Icon(Icons.Default.HelpOutline, "Rules", tint = Color.White)
                    }
                    
                    MedalVideo(
                        video = display.video, 
                        modifier = Modifier.align(Alignment.BottomCenter).size(280.dp).aspectRatio(1f), 
                        animated = true, 
                        variant = display.variant,
                        isColorless = false
                    )
                }
                Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    AsyncImage(model = medalAsset("IMG_20260924_132112.png"), contentDescription = "Medal frame", modifier = Modifier.size(288.dp))
                    Row(horizontalArrangement = Arrangement.Center) {
                        repeat(display.stars) { Icon(Icons.Default.Star, null, tint = medalGold, modifier = Modifier.size(13.dp)) }
                    }
                    Text(display.name, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(display.giftText, color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp), textAlign = TextAlign.Center)
                    if (tierMedals.size > 1) {
                        Spacer(Modifier.height(18.dp))
                        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            tierMedals.forEachIndexed { index, tier ->
                                if (index > 0) Text("  >  ", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                AsyncImage(
                                    model = medalAsset(listOf("IMG_20260924_132022.png","IMG_20260924_132038.png","IMG_20260924_132051.png")[index.coerceAtMost(2)]),
                                    contentDescription = "Tier ${index + 1}",
                                    modifier = Modifier.size(48.dp).border(if(activeTier == index) 2.dp else 0.dp, if(activeTier == index) medalGold else Color.Transparent, RoundedCornerShape(5.dp)).clickable { activeTier = index }
                                )
                            }
                        }
                        Text("Not Obtained", color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(top = 8.dp).background(Color.DarkGray.copy(alpha=.7f), RoundedCornerShape(5.dp)).padding(horizontal = 12.dp, vertical = 3.dp))
                    }
                }
            }
        }

        if (overlay != null) {
            val title = overlay!!
            val isRules = title == "Rules"
            ModalBottomSheet(
                onDismissRequest = { overlay = null },
                containerColor = medalPurple,
                contentColor = Color.White,
                scrimColor = Color.Black.copy(alpha=.6f),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp), // Curve kam kar diya
                dragHandle = null, // Top line hata di
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                // Height ko exactly 60% (60vh) par set kar diya
                Column(Modifier.fillMaxWidth().fillMaxHeight(if (isRules) .6f else .6f).padding(horizontal = 16.dp)) {
                    Text(title, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), textAlign = TextAlign.Center, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Medium)
                    if (isRules) {
                        Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
                            Text(
                                "Rules\n\n1. Important Information:\n(a) Before participating, carefully review event rules, including task completion, rewards, and prohibited activities.\n(b) Participation confirms that you have read and agreed to the rules.\n\n2. What are Medals?\n(a) Medals are rewards for users who meet criteria by completing platform tasks or events.\n(b) Medals are divided into achievement and activity types, with three levels each.\n\n3. How to earn medals?\nEach medal has unique earning criteria detailed on its page. Medals are awarded after completing required objectives.\n\n4. Medal Role:\nMedals showcase achievements and can be displayed next to your avatar. Some provide benefits as you level up.\n\n5. Medal Benefits Expiration:\n(a) Medals generally do not expire; improperly obtained medals are invalid.\n(b) Some medals are time-limited.\n(c) A maximum of 10 medals can be displayed.\n(d) They appear in your info card and room chat.\n(e) Visit Me > Medals to check status.\n\n6. Improving Medal Levels\nMedal levels depend on task and activity status. Complete tasks and activities to upgrade.\n\n7. Additional Notes:\nCheating and abuse are prohibited. Medal descriptions may be updated. Medal rewards cannot be exchanged for cash or goods; unauthorized transactions are prohibited.",
                                color = Color.White, fontSize = 13.sp, lineHeight = 19.sp
                            )
                            // Right side me Zyora Team text add kar diya hai
                            Text(
                                "Regard, Zyora Team",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                                textAlign = TextAlign.End
                            )
                        }
                    } else {
                        val list = if (title == "Obtained Medals") listOf("CP Level Badge","Millionaire I","Charm Star I","Fruit King I","Millionaire II","Fruit King II") else listOf("CP Spokesperson","Level Medal","Weird Star","SVIP Badge","Money Gun Talent","First Recharge")
                        LazyVerticalGrid(columns = GridCells.Fixed(3), contentPadding = PaddingValues(bottom = 30.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(list.size) { index ->
                                Column(Modifier.aspectRatio(1f).background(medalCard, RoundedCornerShape(12.dp)).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                    AsyncImage(model = medalAsset(listOf("IMG_20260924_132022.png","IMG_20260924_132038.png","IMG_20260924_132051.png")[index % 3]), contentDescription = list[index], modifier = Modifier.weight(1f).fillMaxWidth())
                                    Text(list[index], color = Color.White, fontSize = 10.sp, textAlign = TextAlign.Center, maxLines = 2)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

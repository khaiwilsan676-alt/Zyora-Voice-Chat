package com.hurry.voicechat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.viewinterop.AndroidView
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.TextureView
import android.media.MediaPlayer
import android.graphics.SurfaceTexture
import android.view.Surface
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import coil3.compose.AsyncImage
import java.net.URL
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private val StoreBlue = Color(0xFF1D4ED8)
private val StoreBg = Color(0xFFF5F6F8)
private val StoreCyan = Color(0xFF00B4D8)
private const val StoreAssetBase = "https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/"

private data class NativeStoreTab(val id: String, val label: String, val icon: String)
private data class NativeStoreItem(
    val id: String, val name: String, val image: String, val tab: String,
    val stars: Int, val price: Int, val duration: String, val video: String? = null,
    val ownedByDefault: Boolean = false
)

private val nativeStoreTabs = listOf(
    NativeStoreTab("Vehicle", "Vehicle", "IMG_20260913_090019.png"),
    NativeStoreTab("Avatar Frame", "Frame", "IMG_20260913_090057.png"),
    NativeStoreTab("Theme", "Theme", "IMG_20260913_090137.png"),
    NativeStoreTab("Chat Bubble", "Bubble", "IMG_20260913_090116.png"),
    NativeStoreTab("ID", "ID", "IMG_20260913_090150.png")
)

private val nativeStoreItems = listOf(
    // 🎯 Vehicles Update Kar diye as requested
    NativeStoreItem("v1", "Leopard Roar", "IMG_20261023_124625.png", "Vehicle", 5, 1000000, "5D", "VID_20260905_095157_269_bsl.mp4"),
    NativeStoreItem("v2", "Speedster", "IMG_20261023_125302.png", "Vehicle", 5, 600000, "5D", "alioss_19_2025090317293759938~2.mp4"),
    NativeStoreItem("v3", "Blaze Runner", "IMG_20261023_125318.png", "Vehicle", 5, 549999, "5D", "alioss_19_2025120517555513292~2.mp4"),
    NativeStoreItem("v4", "Star Rider", "IMG_20261023_125335.png", "Vehicle", 5, 500000, "5D", "alioss_19_2025121720314422497~2.mp4"),
    NativeStoreItem("v5", "Sky Drift", "IMG_20261023_125357.png", "Vehicle", 5, 259000, "5D", "alioss_19_2026021110563339968~2.mp4"),
    NativeStoreItem("v6", "Neon Flash", "IMG_20261023_125133.png", "Vehicle", 5, 890999, "5D", "alioss_16_2025080802251364763~2.mp4"),
    NativeStoreItem("v7", "Royal Cruiser", "IMG_20261023_125152.png", "Vehicle", 5, 425000, "5D", "alioss_16_2025092202291646319~2.mp4"),
    NativeStoreItem("v8", "Shadow Wing", "IMG_20261023_125210.png", "Vehicle", 5, 525000, "5D", "alioss_16_2025092202374953873~2.mp4"),
    NativeStoreItem("v9", "Galaxy Surfer", "IMG_20261023_125416.png", "Vehicle", 5, 359999, "5D", "alioss_17_2025041211424496804~2.mp4"),

    NativeStoreItem("a1", "Crown Wings", "IMG_20261021_192009.png", "Avatar Frame", 5, 250000, "3D", video = "VID_20260905_024534_955_bsl.mp4"),
    NativeStoreItem("a2", "Host Wings", "IMG_20261021_192044.png", "Avatar Frame", 5, 500000, "3D", video = "VID_20260905_024726_660_bsl.mp4"),
    NativeStoreItem("a3", "Mystic Wings", "file_00000000c67c81fba9294a82cd14d3ca.png", "Avatar Frame", 5, 750000, "3D", video = "VID_20260905_083446_619_bsl.mp4"),
    NativeStoreItem("a4", "Ninja Neo", "file_00000000b8048211813166b471b0cdf6.png", "Avatar Frame", 5, 700000, "3D"),
    NativeStoreItem("a5", "Gemshine", "file_00000000a77c8211a913aef88c4d9a4d.png", "Avatar Frame", 5, 249000, "3D"),
    NativeStoreItem("a6", "Love Beat", "file_00000000445482119241b60988ca201a.png", "Avatar Frame", 5, 300000, "3D"),
    NativeStoreItem("a7", "Golden Aura", "file_00000000406482119e4e67fbbe323a54.png", "Avatar Frame", 5, 199000, "3D"),
    
    NativeStoreItem("t1", "Seafood", "IMG-20260904-WA0004.jpg", "Theme", 4, 2700000, "30D"),
    NativeStoreItem("t2", "Night Sky", "IMG-20260904-WA0005.jpg", "Theme", 5, 2400000, "30D"),
    NativeStoreItem("t3", "Seafood", "IMG-20260904-WA0006.jpg", "Theme", 4, 2700000, "30D"),
    NativeStoreItem("t4", "Night Sky", "IMG-20260904-WA0007.jpg", "Theme", 5, 2400000, "30D"),
    NativeStoreItem("t5", "Seafood", "IMG-20260904-WA0040.jpg", "Theme", 4, 2700000, "30D"),
    NativeStoreItem("t6", "Night Sky", "IMG-20260904-WA0041.jpg", "Theme", 5, 2400000, "30D"),
    NativeStoreItem("c1", "1", "file_000000003d888211822aa6837fe5013c.png", "Chat Bubble", 4, 500000, "3D"),
    NativeStoreItem("c2", "2", "file_000000006044821186ff566329797142.png", "Chat Bubble", 4, 250000, "2D"),
    NativeStoreItem("c3", "3", "file_00000000c44c81f598f62ae8a45e13a7.png", "Chat Bubble", 5, 300000, "3D"),
    NativeStoreItem("c4", "4", "IMG_20260920_122831.png", "Chat Bubble", 4, 246000, "3D"),
    NativeStoreItem("c5", "5", "IMG_20260920_122743.png", "Chat Bubble", 4, 159000, "3D"),
    NativeStoreItem("c6", "6", "IMG_20260920_121752.png", "Chat Bubble", 4, 200000, "3D"),
    NativeStoreItem("i1", "ID Badge 8", "1784533036732~2.jpg", "ID", 5, 10000000, "3D", ownedByDefault = true)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorePage(onBack: () -> Unit = {}, initialView: String = "store") {
    var currentView by remember { mutableStateOf(if (initialView.equals("bag", true)) "bag" else "store") }
    var activeTab by remember { mutableStateOf("Vehicle") }
    var selectedItem by remember { mutableStateOf<NativeStoreItem?>(null) }
    var tryItem by remember { mutableStateOf<NativeStoreItem?>(null) }
    var sendItem by remember { mutableStateOf<NativeStoreItem?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var balance by remember { mutableIntStateOf(82927) }
    val ownedIds = remember { mutableStateListOf("i1") }
    val equippedIds = remember { mutableStateListOf<String>() }
    val isBag = currentView == "bag"
    val visibleItems = nativeStoreItems.filter { it.tab == activeTab && (!isBag || ownedIds.contains(it.id)) }

    Column(Modifier.fillMaxSize().background(StoreBg)) {
        Column(Modifier.fillMaxWidth().background(StoreBg).statusBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(Modifier.size(36.dp).clip(CircleShape).clickable {
                    if (isBag) currentView = "store" else onBack()
                }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = Color.Black, modifier = Modifier.size(26.dp))
                }
                Text(if (isBag) "Bag" else "Store", color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(
                    if (isBag) "Store" else "Bag", color = StoreBlue, fontSize = 15.sp,
                    fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(6.dp))
                        .clickable { currentView = if (isBag) "store" else "bag" }.padding(5.dp)
                )
            }
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                nativeStoreTabs.forEach { tab ->
                    val active = activeTab == tab.id
                    Box(
                        Modifier.width(63.dp).height(80.dp).clip(RoundedCornerShape(6.dp))
                            .background(if (active) Color.Transparent else Color(0x33000000))
                            .clickable { activeTab = tab.id },
                        contentAlignment = Alignment.Center
                    ) {
                        if (active) {
                            AsyncImage(
                                model = StoreAssetBase + "IMG_20260913_090226.png",
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }
                        Column(
                            Modifier.fillMaxSize().padding(top = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            AsyncImage(model = StoreAssetBase + tab.icon, contentDescription = tab.label, modifier = Modifier.size(40.dp))
                            Spacer(Modifier.height(3.dp))
                            Text(tab.label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                        }
                    }
                }
            }
        }

        if (activeTab == "ID") {
            Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White).padding(20.dp)) {
                    Text("Customize ID", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827))
                    Spacer(Modifier.height(16.dp))
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFFF3F4F6)).padding(14.dp), contentAlignment = Alignment.Center) {
                        Text("Start Your Customization Journey", fontSize = 14.sp, color = Color(0xFF9CA3AF), fontWeight = FontWeight.Medium)
                    }
                }
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFFF3F4F6)).clickable { }.padding(14.dp), contentAlignment = Alignment.Center) {
                    Text("Customize", color = Color(0xFF9CA3AF), fontWeight = FontWeight.SemiBold)
                }
            }
        } else if (visibleItems.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (isBag) AsyncImage(model = StoreAssetBase + "file_0000000047308211a02722299d1fda2e.png", contentDescription = "No data", modifier = Modifier.size(120.dp))
                    Text(if (isBag) "No data" else "No items found", color = Color(0xFF9CA3AF), fontSize = 14.sp)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(visibleItems, key = { it.id }) { item ->
                    val theme = item.tab == "Theme"
                    val isVehicle = item.tab == "Vehicle"
                    val isFrame = item.tab == "Avatar Frame"
                    val owned = ownedIds.contains(item.id)
                    val equipped = equippedIds.contains(item.id)
                    
                    Box(
                        Modifier.fillMaxWidth().then(if (theme) Modifier.height(200.dp) else Modifier.wrapContentHeight())
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color.White)
                    ) {
                        if (theme) {
                            AsyncImage(
                                model = StoreAssetBase + item.image,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Column(
                            Modifier.fillMaxWidth().padding(5.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    "Try",
                                    color = if (theme) Color.White else StoreBlue,
                                    fontSize = 10.sp,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .then(if (theme) Modifier.background(Color(0x33FFFFFF)).border(1.dp, Color.White, CircleShape) else Modifier.border(1.dp, StoreBlue, CircleShape))
                                        .clickable { tryItem = item }
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("◷", color = if (theme) Color.White else Color.Gray, fontSize = 12.sp)
                                    Spacer(Modifier.width(3.dp))
                                    Text(item.duration, color = if (theme) Color.White else Color.Gray, fontSize = 11.sp)
                                }
                            }
                            if (!theme) {
                                val mediaModifier = Modifier.fillMaxWidth().height(if (isVehicle || isFrame) 120.dp else 80.dp)
                                if (item.image.endsWith(".mp4", ignoreCase = true)) {
                                    // 🎯 Video hai to fadeEdges pass kiya (only for vehicles 10% fade)
                                    StoreVideoPreview(StoreAssetBase + item.image, mediaModifier, pause = true, fadeEdges = isVehicle)
                                } else {
                                    // 🎯 Green remove wala StoreChromaImage hata diya Vehicle ke liye, ab simple load hoga
                                    AsyncImage(model = StoreAssetBase + item.image, contentDescription = item.name, modifier = mediaModifier)
                                }
                            } else {
                                Spacer(Modifier.weight(1f).fillMaxWidth().height(80.dp))
                            }
                            Text("★".repeat(item.stars) + "☆".repeat(5 - item.stars), color = Color(0xFFFFC928), fontSize = 16.sp, lineHeight = 18.sp)
                            Row(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                AsyncImage(model = StoreAssetBase + "file_00000000e56882119c217d508b6733dc.png", contentDescription = "Coins", modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("%,d".format(item.price), color = if (theme) Color.White else Color(0xFF111827), fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                            }
                            Row(
                                Modifier.fillMaxWidth().height(30.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, StoreBlue, CircleShape)
                                    .background(Color.White)
                            ) {
                                Box(
                                    Modifier.weight(1f).fillMaxHeight()
                                        .clickable { sendItem = item },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Send", color = StoreBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Box(
                                    Modifier.weight(1f).fillMaxHeight()
                                        .background(StoreBlue)
                                        .clickable {
                                            if (owned) {
                                                if (equipped) equippedIds.remove(item.id) else {
                                                    equippedIds.removeAll { id -> nativeStoreItems.find { it.id == id }?.tab == item.tab }
                                                    equippedIds.add(item.id)
                                                }
                                            } else {
                                                selectedItem = item
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(if (owned) if (equipped) "Equipped" else "Equip" else "Buy", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedItem != null) {
        val item = selectedItem!!
        ModalBottomSheet(onDismissRequest = { selectedItem = null }, containerColor = Color.White, dragHandle = {
            BottomSheetDefaults.DragHandle(color = Color(0xFFD1D5DB))
        }) {
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Purchase", color = Color(0xFF111827), fontSize = 17.sp, fontWeight = FontWeight.Bold)
                val previewSource = item.video ?: item.image
                if (previewSource.endsWith(".mp4", ignoreCase = true)) {
                    StoreVideoPreview(StoreAssetBase + previewSource, Modifier.size(180.dp), pause = false, fadeEdges = item.tab == "Vehicle")
                } else {
                    AsyncImage(model = StoreAssetBase + previewSource, contentDescription = item.name, modifier = Modifier.size(180.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("%,d".format(item.price), color = Color(0xFF111827), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(6.dp))
                    AsyncImage(model = StoreAssetBase + "file_00000000e56882119c217d508b6733dc.png", contentDescription = "Coins", modifier = Modifier.size(24.dp))
                }
                Text(item.name, Modifier.fillMaxWidth().padding(top = 8.dp), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827))
                Row(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    Text("Balance: ", fontSize = 13.sp, color = Color(0xFF4B5563), fontWeight = FontWeight.SemiBold)
                    AsyncImage(model = StoreAssetBase + "file_00000000e56882119c217d508b6733dc.png", contentDescription = "Coins", modifier = Modifier.size(16.dp))
                    Text("%,d".format(balance), fontSize = 13.sp, color = Color(0xFF4B5563), fontWeight = FontWeight.SemiBold)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f).clip(CircleShape).background(Color(0xFFFFF7ED)).padding(vertical = 9.dp), contentAlignment = Alignment.Center) {
                        Text("✓  3days", color = Color(0xFFF97316), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Box(Modifier.weight(1f).clip(CircleShape).background(Color(0xFFF9FAFB)).padding(vertical = 9.dp), contentAlignment = Alignment.Center) {
                        Text("7days", color = Color(0xFF9CA3AF), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 22.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { sendItem = item; selectedItem = null }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0F2FE), contentColor = StoreBlue)) { Text("Send", fontWeight = FontWeight.Bold) }
                    Button(onClick = {
                        if (ownedIds.contains(item.id)) {
                            if (equippedIds.contains(item.id)) equippedIds.remove(item.id) else {
                                equippedIds.removeAll { id -> nativeStoreItems.find { it.id == id }?.tab == item.tab }
                                equippedIds.add(item.id)
                            }
                        } else if (balance >= item.price) {
                            balance -= item.price
                            ownedIds.add(item.id)
                        }
                        selectedItem = null
                    }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = StoreCyan, contentColor = Color.White)) {
                        Text(if (ownedIds.contains(item.id)) if (equippedIds.contains(item.id)) "Using" else "Use" else "Buy", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (tryItem != null) {
        val item = tryItem!!
        Dialog(
            onDismissRequest = { tryItem = null },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = if (item.tab == "Theme") 0.7f else if (item.tab == "Chat Bubble") 0.6f else 0.5f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { tryItem = null },
                contentAlignment = Alignment.Center
            ) {
                when (item.tab) {
                    "Theme" -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(enabled = false) {}) {
                            Box(Modifier.width(230.dp).height(480.dp).border(4.dp, Color.Yellow, RoundedCornerShape(24.dp)).clip(RoundedCornerShape(24.dp)).background(Color.Transparent)) {
                                AsyncImage(model = StoreAssetBase + item.image, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            }
                            Row(Modifier.padding(top = 16.dp)) {
                                Text("★".repeat(item.stars) + "☆".repeat(5 - item.stars), color = Color.Yellow, fontSize = 20.sp)
                            }
                            Text(item.name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                    "Avatar Frame" -> {
                        Box(
                            Modifier.width(300.dp).height(300.dp).background(Color.White, RoundedCornerShape(16.dp)).clickable(enabled = false) {},
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Spacer(Modifier.height(40.dp))
                                Text(item.name, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                                    Text("◷", color = Color.Gray, fontSize = 13.sp)
                                    Spacer(Modifier.width(4.dp))
                                    Text(item.duration, color = Color.Gray, fontSize = 13.sp)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)) {
                                    AsyncImage(model = StoreAssetBase + "file_00000000e56882119c217d508b6733dc.png", contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("%,d".format(item.price), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                }
                                val isOwned = ownedIds.contains(item.id)
                                val isEquipped = equippedIds.contains(item.id)
                                Button(
                                    onClick = {
                                        if (isOwned) {
                                            if (isEquipped) equippedIds.remove(item.id) else {
                                                equippedIds.removeAll { id -> nativeStoreItems.find { it.id == id }?.tab == item.tab }
                                                equippedIds.add(item.id)
                                            }
                                        } else if (balance >= item.price) {
                                            balance -= item.price
                                            ownedIds.add(item.id)
                                        }
                                        tryItem = null
                                    },
                                    modifier = Modifier.fillMaxWidth().height(42.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = StoreBlue)
                                ) {
                                    Text(if (isOwned) if (isEquipped) "Equipped" else "Equip" else "Buy", fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = { tryItem = null },
                                    modifier = Modifier.fillMaxWidth().height(42.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray),
                                    border = BorderStroke(1.dp, Color.LightGray)
                                ) {
                                    Text("Cancel", fontWeight = FontWeight.Bold)
                                }
                            }
                            Box(Modifier.align(Alignment.TopCenter).offset(y = (-55).dp).size(110.dp), contentAlignment = Alignment.Center) {
                                Box(Modifier.size(80.dp).clip(CircleShape).background(Color.Gray), contentAlignment = Alignment.Center) {
                                    Text("U", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                                }
                                val source = item.video ?: item.image
                                if (source.endsWith(".mp4", true)) {
                                    StoreVideoPreview(StoreAssetBase + source, Modifier.fillMaxSize(), pause = false)
                                } else {
                                    AsyncImage(model = StoreAssetBase + source, contentDescription = null, modifier = Modifier.fillMaxSize())
                                }
                            }
                        }
                    }
                    "Chat Bubble" -> {
                        Column(
                            Modifier.width(260.dp).background(Color.White, RoundedCornerShape(22.dp)).padding(top = 20.dp, bottom = 16.dp, start = 16.dp, end = 16.dp).clickable(enabled = false) {},
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                                Box(Modifier.size(16.dp).border(5.dp, Color(0xFFE5E7EB), CircleShape).background(Color.White))
                                Spacer(Modifier.width(8.dp))
                                Text("3D", color = Color(0xFF9CA3AF), fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            }
                            Box(Modifier.size(120.dp), contentAlignment = Alignment.Center) {
                                AsyncImage(model = StoreAssetBase + item.image, contentDescription = null, modifier = Modifier.fillMaxSize())
                                Text(
                                    "Zyora",
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    style = androidx.compose.ui.text.TextStyle(
                                        shadow = Shadow(color = Color(0xE6FFD700), blurRadius = 12f)
                                    )
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp)) {
                                AsyncImage(model = StoreAssetBase + "file_00000000e56882119c217d508b6733dc.png", contentDescription = null, modifier = Modifier.size(28.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("%,d".format(item.price), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp, bottom = 12.dp).alpha(0.6f)) {
                                AsyncImage(model = StoreAssetBase + "file_00000000e56882119c217d508b6733dc.png", contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("%,d".format(item.price * 2), fontSize = 14.sp, color = Color.Gray, textDecoration = TextDecoration.LineThrough)
                            }

                            Button(
                                onClick = {
                                    if (!ownedIds.contains(item.id) && balance >= item.price) {
                                        balance -= item.price
                                        ownedIds.add(item.id)
                                    }
                                    tryItem = null
                                },
                                modifier = Modifier.fillMaxWidth().height(42.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C88C))
                            ) {
                                Text("Buy", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = { tryItem = null },
                                modifier = Modifier.fillMaxWidth().height(42.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF2F2F2), contentColor = Color(0xFF8A8A8A))
                            ) {
                                Text("Cancel", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    else -> { // Vehicle Try Dialog
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(enabled = false) {}) {
                            Box(Modifier.fillMaxWidth().height(360.dp), contentAlignment = Alignment.Center) {
                                val source = item.video ?: item.image
                                if (source.endsWith(".mp4", true)) {
                                    // 🎯 Video Try dialog me bhi fadeEdges apply
                                    StoreVideoPreview(StoreAssetBase + source, Modifier.fillMaxSize(), pause = false, fadeEdges = true)
                                } else {
                                    AsyncImage(model = StoreAssetBase + source, contentDescription = item.name, modifier = Modifier.fillMaxSize())
                                }
                            }
                            Row(Modifier.padding(top = 16.dp)) {
                                Text("★".repeat(item.stars) + "☆".repeat(5 - item.stars), color = Color.Yellow, fontSize = 20.sp)
                            }
                            Text(item.name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                }
            }
        }
    }

    if (sendItem != null) {
        ModalBottomSheet(onDismissRequest = { sendItem = null; searchQuery = "" }, containerColor = Color.White) {
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp)) {
                Text("Send to friends", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827))
                Text("Contact", modifier = Modifier.padding(top = 14.dp, bottom = 8.dp), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1F2937))
                Row(Modifier.fillMaxWidth().clip(CircleShape).background(Color(0xFFF3F4F6)).padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("⌕", fontSize = 20.sp, color = Color.Gray)
                    Spacer(Modifier.width(8.dp))
                    BasicTextField(value = searchQuery, onValueChange = { searchQuery = it }, singleLine = true, modifier = Modifier.fillMaxWidth(), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = Color(0xFF1F2937)), decorationBox = { inner ->
                        if (searchQuery.isEmpty()) Text("Search friends...", fontSize = 14.sp, color = Color(0xFF9CA3AF))
                        inner()
                    })
                }
                listOf("add please", "99❤️OWNER KABIR☆khan").filter { it.contains(searchQuery, true) }.forEachIndexed { index, name ->
                    Row(Modifier.fillMaxWidth().clickable { sendItem = null; searchQuery = "" }.padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(model = StoreAssetBase + if (index == 0) "IMG_20260913_090019.png" else "IMG_20260913_090057.png", contentDescription = name, modifier = Modifier.size(44.dp).clip(CircleShape))
                        Text(name, Modifier.padding(start = 12.dp).weight(1f), fontSize = 14.sp, color = Color(0xFF1F2937), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("Send", color = StoreBlue, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { sendItem = null; searchQuery = "" }.padding(8.dp))
                    }
                }
                Spacer(Modifier.height(18.dp))
            }
        }
    }
}

// Ye function aise hi chhor raha hu taaki future ke liye bacha rahe, par ab image ke liye directly AsyncImage chalega.
@Composable
private fun StoreChromaImage(
    url: String,
    description: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit
) {
    var bitmap by remember(url) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(url) {
        bitmap = withContext(Dispatchers.IO) {
            runCatching {
                val original = URL(url).openStream().use { BitmapFactory.decodeStream(it) }
                if (original == null) null else {
                    val pixels = IntArray(original.width * original.height)
                    original.getPixels(pixels, 0, original.width, 0, 0, original.width, original.height)
                    for (i in pixels.indices) {
                        val color = pixels[i]
                        val a = (color ushr 24) and 0xFF
                        val r = (color ushr 16) and 0xFF
                        val g = (color ushr 8) and 0xFF
                        val b = color and 0xFF
                        
                        val maxRB = max(r, b)
                        val greenness = g - maxRB
                        
                        val edge0 = 10.2f  
                        val edge1 = 38.25f 
                        val t = ((greenness.toFloat() - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
                        val blend = t * t * (3f - 2f * t)
                        
                        val despilledG = min(g, maxRB + 13)

                        val finalAlpha = (a * (1f - blend)).roundToInt().coerceIn(0, 255)
                        pixels[i] = (finalAlpha shl 24) or (r shl 16) or (despilledG shl 8) or b
                    }
                    Bitmap.createBitmap(pixels, original.width, original.height, Bitmap.Config.ARGB_8888)
                }
            }.getOrNull()
        }
    }
    val imageBitmap = bitmap
    if (imageBitmap != null) {
        Image(bitmap = imageBitmap.asImageBitmap(), contentDescription = description, modifier = modifier, contentScale = contentScale)
    } else {
        Box(modifier, contentAlignment = Alignment.Center) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = StoreBlue)
        }
    }
}

// 🎯 FADE EDGES LOGIC ADDED HERE! (Top aur bottom 10% fade blend)
@Composable
private fun StoreVideoPreview(url: String, modifier: Modifier = Modifier, pause: Boolean = false, fadeEdges: Boolean = false) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var isVideoReady by remember { mutableStateOf(false) } 

    val chromaMatrix = remember {
        ColorMatrix(floatArrayOf(
            1f, 0f, 0f, 0f, 0f,     
            0f, 1f, 0f, 0f, 0f,     
            0f, 0f, 1f, 0f, 0f,     
            2.5f, -4f, 2.5f, 1f, 0f 
        ))
    }

    // Fade logic condition
    val fadeModifier = if (fadeEdges) {
        Modifier
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                drawRect(
                    brush = Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        0.1f to Color.Black,
                        0.9f to Color.Black,
                        1.0f to Color.Transparent
                    ),
                    blendMode = BlendMode.DstIn
                )
            }
    } else Modifier

    Box(
        modifier = modifier
            .then(fadeModifier)
            .alpha(if (isVideoReady) 1f else 0f)
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
                colorFilter = ColorFilter.colorMatrix(chromaMatrix)
            },
        contentAlignment = Alignment.Center
    ) {
        key(url) {
            AndroidView(
                factory = { ctx ->
                    val textureView = TextureView(ctx)
                    textureView.isOpaque = false // Transparent ke liye jaruri
                    
                    val mediaPlayer = MediaPlayer()
                    textureView.tag = mediaPlayer
                    
                    textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                            mediaPlayer.setSurface(Surface(surface))
                            try {
                                mediaPlayer.setDataSource(ctx, Uri.parse(url))
                                mediaPlayer.isLooping = true
                                mediaPlayer.setOnInfoListener { _, what, _ ->
                                    if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                                        isVideoReady = true
                                        true
                                    } else false
                                }
                                mediaPlayer.prepareAsync()
                                mediaPlayer.setOnPreparedListener {
                                    if (!pause) it.start()
                                    else {
                                        it.seekTo(100)
                                        isVideoReady = true
                                    }
                                }
                            } catch (e: Exception) { e.printStackTrace() }
                        }
                        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
                        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                            mediaPlayer.release()
                            return true
                        }
                        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
                    }
                    textureView
                },
                onRelease = { view ->
                    val mp = view.tag as? MediaPlayer
                    mp?.release()
                }
            )
        }
    }
}

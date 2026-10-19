package com.hurry.voicechat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.VideoView
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import coil3.compose.AsyncImage
import java.net.URL
import kotlin.math.max
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
    NativeStoreItem("v1", "Leopard Roar", "IMG_20260905_222412.jpg", "Vehicle", 5, 1000000, "5D", "VID_20260905_095157_269_bsl.mp4"),
    NativeStoreItem("a1", "Crown Wings", "VID_20260905_024534_955_bsl.mp4", "Avatar Frame", 5, 250000, "3D"),
    NativeStoreItem("a2", "Host Wings", "VID_20260905_024726_660_bsl.mp4", "Avatar Frame", 5, 500000, "3D"),
    NativeStoreItem("a3", "Mystic Wings", "VID_20260905_083446_619_bsl.mp4", "Avatar Frame", 5, 750000, "3D"),
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
                            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .20f)))
                        }
                        Column(
                            Modifier.fillMaxWidth().padding(5.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    "Try",
                                    color = if (theme) Color.White else StoreBlue,
                                    fontSize = 11.sp,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .then(if (theme) Modifier.background(Color(0x33FFFFFF)).border(1.dp, Color.White, CircleShape) else Modifier.border(1.dp, StoreBlue, CircleShape))
                                        .clickable { tryItem = item }
                                        .padding(horizontal = 10.dp, vertical = 3.dp)
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("◷", color = if (theme) Color.White else Color.Gray, fontSize = 12.sp)
                                    Spacer(Modifier.width(3.dp))
                                    Text(item.duration, color = if (theme) Color.White else Color.Gray, fontSize = 11.sp)
                                }
                            }
                            if (!theme) {
                                val mediaModifier = Modifier.fillMaxWidth().height(if (item.tab == "Vehicle" || item.tab == "Avatar Frame") 120.dp else 80.dp)
                                if (item.image.endsWith(".mp4", ignoreCase = true)) {
                                    StoreVideoPreview(StoreAssetBase + item.image, mediaModifier)
                                } else {
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
                                            } else selectedItem = item
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(if (owned) if (equipped) "Using" else "Use" else "Buy", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (tryItem != null) {
        val item = tryItem!!
        ModalBottomSheet(onDismissRequest = { tryItem = null }, containerColor = Color.White) {
            Column(
                Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Preview", color = Color(0xFF111827), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(14.dp))
                Box(
                    Modifier.fillMaxWidth().height(if (item.tab == "Theme") 300.dp else 240.dp)
                        .clip(RoundedCornerShape(16.dp)).background(Color(0xFFF5F6F8)),
                    contentAlignment = Alignment.Center
                ) {
                    val source = item.video ?: item.image
                    if (source.endsWith(".mp4", ignoreCase = true)) {
                        StoreVideoPreview(StoreAssetBase + source, Modifier.fillMaxSize())
                    } else {
                        StoreChromaImage(
                            url = StoreAssetBase + source,
                            description = item.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = if (item.tab == "Theme") ContentScale.Crop else ContentScale.Fit
                        )
                    }
                }
                Text(item.name, modifier = Modifier.padding(top = 12.dp), color = Color(0xFF111827), fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("★".repeat(item.stars), modifier = Modifier.padding(top = 4.dp), color = Color(0xFFF59E0B), fontSize = 17.sp)
                Row(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { tryItem = null }, modifier = Modifier.weight(1f)) { Text("Close") }
                    Button(
                        onClick = { selectedItem = item; tryItem = null },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = StoreCyan, contentColor = Color.White)
                    ) { Text("Buy", fontWeight = FontWeight.Bold) }
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
                    StoreVideoPreview(StoreAssetBase + previewSource, Modifier.size(180.dp))
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
                        val base = max(r, b)
                        val greenExcess = g - base
                        if (g > 70 && greenExcess > 18) {
                            val strength = ((greenExcess - 18f) / 72f).coerceIn(0f, 1f)
                            val smooth = strength * strength * (3f - 2f * strength)
                            val alpha = (a * (1f - smooth)).roundToInt().coerceIn(0, 255)
                            val cleanGreen = minOf(g, base + 12)
                            pixels[i] = (alpha shl 24) or (r shl 16) or (cleanGreen shl 8) or b
                        }
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

@Composable
private fun StoreVideoPreview(url: String, modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    AndroidView(
        modifier = modifier,
        factory = {
            val videoView = VideoView(context)
            videoView.tag = url
            videoView.setVideoURI(Uri.parse(url))
            videoView.setOnPreparedListener { player ->
                player.isLooping = true
                videoView.start()
            }
            videoView.setOnErrorListener { _, _, _ -> true }
            videoView
        },
        update = { view ->
            if (view.tag != url) {
                view.tag = url
                view.setVideoURI(Uri.parse(url))
                view.setOnPreparedListener { player ->
                    player.isLooping = true
                    view.start()
                }
            }
        }
    )
}

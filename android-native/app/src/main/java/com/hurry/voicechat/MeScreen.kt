package com.hurry.voicechat

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage

private const val RAW_ME = "https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/"

private data class MeMenu(val label: String, val asset: String)

@Composable
fun MeScreen(onOpen: (String) -> Unit = {}, onPublicProfile: (RoomProfileUser) -> Unit = {}, languageCode: String = "en") {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    // Native login stores the authenticated Firebase profile in hurry_auth.
    // Read the same preferences so the signed-in account ID appears on the Me page.
    val prefs = remember { context.getSharedPreferences("hurry_auth", Context.MODE_PRIVATE) }
    val uid = prefs.getString("uid", "") ?: ""
    val email = prefs.getString("email", "") ?: ""
    val name = prefs.getString("name", "")?.takeIf { it.isNotBlank() && it.lowercase() !in listOf("guest","user","null","undefined") }
        ?: email.substringBefore("@").takeIf { it.isNotBlank() }
        ?: "Zyora User"
    val photo = prefs.getString("photo", null)?.takeIf { it.isNotBlank() }
        ?: prefs.getString("image", "") ?: ""
    val savedAccount = prefs.getString("accountNumber", null)?.takeIf { it.isNotBlank() }
        ?: prefs.getString("accountId", "") ?: ""
    val account = savedAccount.ifBlank { nativeMeAccountNumber(uid) }
    val phone = prefs.getString("phone", "") ?: ""

    val topItems = listOf(
        MeMenu("Invite Friends", "IMG_20260915_225333.png"),
        MeMenu("Family", "IMG_20260915_225349.png"),
        MeMenu("Level", "IMG_20260915_225404.png"),
        MeMenu("Medal", "IMG_20260915_225426.png"),
        MeMenu("Store", "IMG_20260915_225447.png"),
        MeMenu("Bag", "IMG_20260915_225506.png"),
        MeMenu("Seller Center", "IMG_20260915_225536.png")
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF1F1F1),),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Column(
                Modifier.fillMaxWidth().background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF2563EB), Color(0xFFEFF6FF), Color(0xFFF1F1F1)),
                        startY = 0f, endY = 620f
                    )
                ).padding(top = (WindowInsets.statusBars.asPaddingValues().calculateTopPadding() - 16.dp).coerceAtLeast(0.dp) + 36.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        if (photo.isNotBlank()) {
                            AsyncImage(photo, null, Modifier.size(80.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                        } else {
                            Box(Modifier.size(80.dp).clip(CircleShape).background(Color(0xFF666666)), contentAlignment = Alignment.Center) {
                                Text(name.take(1).uppercase(), color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Normal)
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(name, fontSize = 24.sp, fontWeight = FontWeight.Normal, color = Color(0xFF111827))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("ID: $account", fontSize = 12.sp, fontWeight = FontWeight.Normal, color = Color(0xFF374151))
                                IconButton(
                                    onClick = { clipboard.setText(AnnotatedString(account)) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.ContentCopy,
                                        contentDescription = "Copy ID",
                                        tint = Color(0xFF374151),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            if (phone.isNotBlank()) Text(phone, fontSize = 12.sp, fontWeight = FontWeight.Normal, color = Color(0xFF4B5563))
                        }
                    }
                    Text("›", fontSize = 32.sp, color = Color(0xFF374151), modifier = Modifier.padding(top = 10.dp).clickable { onPublicProfile(RoomProfileUser(id = uid, uid = uid, accountId = account.ifBlank { uid }, name = name, image = photo, country = "India")) })
                }

                Spacer(Modifier.height(18.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Stat("0", nativeText(languageCode, "friends"))
                    DividerV()
                    Stat("1", nativeText(languageCode, "followers"))
                    DividerV()
                    Stat("0", nativeText(languageCode, "following"))
                    DividerV()
                    Stat("1", nativeText(languageCode, "visitors"))
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.weight(1f).height(56.dp).clip(RoundedCornerShape(8.dp)).clickable { onOpen("Coins") }) {
                    FeatureImage("file_00000000f26c81f88083bc494a0f229b.png", Modifier.fillMaxSize())
                }
                Box(Modifier.weight(1f).height(56.dp).clip(RoundedCornerShape(8.dp)).clickable { onOpen("Diamonds") }) {
                    FeatureImage("file_00000000fe848207abf557a118ff8a5b.png", Modifier.fillMaxSize())
                }
            }
        }

        item {
            AsyncImage(
                RAW_ME + "file_00000000a25081fbb57574619596eed8.png", null,
                Modifier.fillMaxWidth().padding(horizontal = 12.dp).offset(y = (-40).dp).padding(bottom = 24.dp).heightIn(min = 76.dp).clip(RoundedCornerShape(8.dp)).clickable { onOpen("SVIP") },
                contentScale = ContentScale.Crop
            )
        }

        item {
            MenuCard(topItems, onOpen, Modifier.offset(y = (-76).dp))
        }

        item {
            val bottom = listOf("Language Setting", "Settings", "Customer Service", "Help & Feedback")
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 2.dp).offset(y = (-55).dp).clip(RoundedCornerShape(6.dp)).background(Color.White)) {
                bottom.forEach { label ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onOpen(label) }.padding(horizontal = 16.dp, vertical = 15.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MeMenuIcon(
                            type = when (label) {
                                "Language Setting" -> MeIconType.LANGUAGE
                                "Settings" -> MeIconType.SETTINGS
                                "Customer Service" -> MeIconType.SUPPORT
                                else -> MeIconType.HELP
                            }
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(nativeText(languageCode, when (label) { "Invite Friends" -> "invite"; "Family" -> "family"; "Level" -> "level"; "Medal" -> "medal"; "Store" -> "store"; "Bag" -> "bag"; "Seller Center" -> "seller"; "Language Setting" -> "language"; "Settings" -> "settings"; "Customer Service" -> "support"; "Help & Feedback" -> "help"; else -> label }), Modifier.weight(1f), fontSize = 15.sp, fontWeight = FontWeight.Normal, color = Color(0xFF171717))
                        Text("›", fontSize = 25.sp, color = Color(0xFFAAAAAA))
                    }
                }
            }
        }
    }
}


private fun nativeMeAccountNumber(uid: String): String {
    if (uid.isBlank()) return ""
    val special = when (uid) {
        "HUSxSvQnabgU029dWYt1TUV04hd2" -> "100002"
        "ADqW31RGBMaosOzy0HiqexKSD7h1" -> "100003"
        "100002" -> "100002"
        "100003" -> "100003"
        else -> null
    }
    if (special != null) return special
    if (uid in setOf("500001", "500002", "500003", "500004", "500005", "700001", "700002", "700003")) return uid
    return (10000000L + kotlin.math.abs(uid.hashCode().toLong()) % 90000000L).toString()
}

@Composable private fun Stat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Normal, color = Color(0xFF111827))
        Text(label, fontSize = 11.sp, color = Color(0xFF4B5563))
    }
}
enum class MeIconType { LANGUAGE, SETTINGS, SUPPORT, HELP }

@Composable
private fun MeMenuIcon(type: MeIconType) {
    androidx.compose.foundation.Canvas(Modifier.size(if (type == MeIconType.SUPPORT) 20.dp else 24.dp)) {
        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
        val c = Color(0xFF1E1E1E)
        when (type) {
            MeIconType.LANGUAGE -> {
                drawCircle(color = c, radius = size.minDimension * 0.42f, center = androidx.compose.ui.geometry.Offset(size.width/2f, size.height/2f), style = stroke)
                drawLine(color = c, start = androidx.compose.ui.geometry.Offset(size.width*0.08f, size.height/2f), end = androidx.compose.ui.geometry.Offset(size.width*0.92f, size.height/2f), strokeWidth = stroke.width)
                drawOval(c, topLeft = androidx.compose.ui.geometry.Offset(size.width*0.33f, size.height*0.08f), size = androidx.compose.ui.geometry.Size(size.width*0.34f, size.height*0.84f), style = stroke)
            }
            MeIconType.SETTINGS -> {
                val p = androidx.compose.ui.graphics.Path().apply { moveTo(size.width*.50f, size.height*.08f); lineTo(size.width*.84f, size.height*.28f); lineTo(size.width*.84f, size.height*.72f); lineTo(size.width*.50f, size.height*.92f); lineTo(size.width*.16f, size.height*.72f); lineTo(size.width*.16f, size.height*.28f); close() }
                drawPath(path = p, color = c, style = stroke)
                drawCircle(color = c, radius = size.minDimension*.125f, center = androidx.compose.ui.geometry.Offset(size.width/2f,size.height/2f), style = stroke)
            }
            MeIconType.SUPPORT -> {
                val head = androidx.compose.ui.graphics.Path().apply { moveTo(size.width*.23f,size.height*.46f); lineTo(size.width*.23f,size.height*.35f); cubicTo(size.width*.23f,size.height*.13f,size.width*.40f,size.height*.05f,size.width*.50f,size.height*.05f); cubicTo(size.width*.60f,size.height*.05f,size.width*.77f,size.height*.13f,size.width*.77f,size.height*.35f); lineTo(size.width*.77f,size.height*.63f); cubicTo(size.width*.77f,size.height*.86f,size.width*.62f,size.height*.98f,size.width*.50f,size.height*.98f) }
                drawPath(path = head, color = c, style = stroke)
                drawLine(color = c, start = androidx.compose.ui.geometry.Offset(size.width*.23f,size.height*.43f), end = androidx.compose.ui.geometry.Offset(size.width*.23f,size.height*.64f), strokeWidth=stroke.width)
                drawLine(color = c, start = androidx.compose.ui.geometry.Offset(size.width*.77f,size.height*.43f), end = androidx.compose.ui.geometry.Offset(size.width*.77f,size.height*.64f), strokeWidth=stroke.width)
                val left = androidx.compose.ui.graphics.Path().apply { moveTo(size.width*.23f,size.height*.43f); lineTo(size.width*.14f,size.height*.43f); cubicTo(size.width*.08f,size.height*.43f,size.width*.08f,size.height*.64f,size.width*.14f,size.height*.64f); lineTo(size.width*.23f,size.height*.64f); close() }
                drawPath(path = left, color = c, style = stroke)
                val right = androidx.compose.ui.graphics.Path().apply { moveTo(size.width*.77f,size.height*.43f); lineTo(size.width*.86f,size.height*.43f); cubicTo(size.width*.92f,size.height*.43f,size.width*.92f,size.height*.64f,size.width*.86f,size.height*.64f); lineTo(size.width*.77f,size.height*.64f); close() }
                drawPath(path = right, color = c, style = stroke)
                val smile = androidx.compose.ui.graphics.Path().apply { moveTo(size.width*.38f,size.height*.58f); cubicTo(size.width*.38f,size.height*.69f,size.width*.62f,size.height*.69f,size.width*.62f,size.height*.58f) }
                drawPath(path = smile, color = c, style = stroke)
            }
            MeIconType.HELP -> {
                drawCircle(color = c, radius = size.minDimension*.42f, center = androidx.compose.ui.geometry.Offset(size.width/2f,size.height/2f), style = stroke)
                val q = androidx.compose.ui.graphics.Path().apply { moveTo(size.width*.38f,size.height*.38f); cubicTo(size.width*.40f,size.height*.28f,size.width*.60f,size.height*.28f,size.width*.62f,size.height*.40f); cubicTo(size.width*.64f,size.height*.53f,size.width*.50f,size.height*.55f,size.width*.50f,size.height*.68f) }
                drawPath(path = q, color = c, style = stroke)
                drawCircle(color = c, radius =size.minDimension*.04f,center=androidx.compose.ui.geometry.Offset(size.width/2f,size.height*.78f))
            }
        }
    }
}
@Composable private fun DividerV() { Box(Modifier.height(34.dp).width(1.dp).background(Color(0xFFD1D5DB))) }

@Composable private fun FeatureImage(asset: String, modifier: Modifier) {
    AsyncImage(RAW_ME + asset, null, modifier.clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
}

@Composable private fun MenuCard(items: List<MeMenu>, onOpen: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.padding(horizontal = 16.dp, vertical = 0.dp).clip(RoundedCornerShape(6.dp)).background(Color.White)) {
        items.forEach { item ->
            Row(Modifier.fillMaxWidth().clickable { onOpen(item.label) }.padding(horizontal = 16.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(RAW_ME + item.asset, null, Modifier.size(if (item.label == "Seller Center") 40.dp else 32.dp), contentScale = ContentScale.Crop)
                Spacer(Modifier.width(16.dp))
                Text(item.label, Modifier.weight(1f), fontSize = 15.sp, fontWeight = FontWeight.Normal, color = Color(0xFF171717))
                Text("›", fontSize = 25.sp, color = Color(0xFFAAAAAA))
            }
        }
    }
}

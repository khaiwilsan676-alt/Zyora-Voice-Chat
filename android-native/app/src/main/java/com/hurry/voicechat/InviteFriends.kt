package com.hurry.voicechat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

private const val INVITE_ASSET_BASE = "https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/"
private fun inviteAsset(file: String) = INVITE_ASSET_BASE + file
private enum class InviteKey { NONE, GREEN, WHITE }

@Composable
private fun InviteArt(file: String, modifier: Modifier = Modifier, key: InviteKey = InviteKey.NONE, scale: ContentScale = ContentScale.Fit, alignment: Alignment = Alignment.Center) {
    var bitmap by remember(file, key) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(file, key) {
        bitmap = withContext(Dispatchers.IO) {
            runCatching {
                val source = URL(inviteAsset(file)).openStream().use { BitmapFactory.decodeStream(it) }
                if (source == null || key == InviteKey.NONE) source
                else {
                    val output = source.copy(Bitmap.Config.ARGB_8888, true)
                    val pixels = IntArray(output.width * output.height)
                    output.getPixels(pixels, 0, output.width, 0, 0, output.width, output.height)
                    for (i in pixels.indices) {
                        val p = pixels[i]
                        val a = (p ushr 24) and 0xff
                        val r = (p ushr 16) and 0xff
                        val g = (p ushr 8) and 0xff
                        val b = p and 0xff
                        
                        if (key == InviteKey.GREEN) {
                            val maxRB = maxOf(r, b)
                            val greenness = g - maxRB
                            
                            if (greenness > 5 && g > 30) {
                                pixels[i] = 0x00000000
                            } else if (greenness > -15 && g > 20) {
                                val factor = (greenness + 15) / 20f
                                val na = (a * (1f - factor)).toInt().coerceIn(0, 255)
                                val cleanG = minOf(g, maxRB)
                                pixels[i] = (na shl 24) or (r shl 16) or (cleanG shl 8) or b
                            }
                        } else {
                            if (r > 210 && g > 210 && b > 210) {
                                val avg = (r + g + b) / 3f
                                if (avg > 240f) pixels[i] = p and 0x00ffffff
                                else {
                                    val factor = (avg - 210f) / 30f
                                    val na = (a * (1f - factor)).toInt().coerceIn(0, 255)
                                    pixels[i] = (na shl 24) or (r shl 16) or (g shl 8) or b
                                }
                            }
                        }
                    }
                    output.setPixels(pixels, 0, output.width, 0, 0, output.width, output.height)
                    output
                }
            }.getOrNull()
        }
    }
    val image = bitmap
    if (image != null) {
        Image(
            bitmap = image.asImageBitmap(),
            contentDescription = file,
            modifier = modifier,
            contentScale = scale,
            alignment = alignment
        )
    } else {
        Box(modifier)
    }
}

private val inviteYellow = Color(0xFFFFD52A)
private val invitePaleYellow = Color(0xFFFFE76A)
private val inviteRed = Color(0xFF4D0515)
private val invitePopupPurple = Color(0xFF750A75)

@Composable
fun InviteFriends(onBack: (() -> Unit)? = null, onClose: (() -> Unit)? = null) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val vh = screenHeight / 100f
    val userPrefs = remember { context.getSharedPreferences("hurry_auth", Context.MODE_PRIVATE) }
    val code = userPrefs.getString("uid", null)?.takeIf { it.isNotBlank() } ?: "WELCOME123"
    val inviteLink = "https://yourapp.com/invite/$code"
    
    var isSheetOpen by remember { mutableStateOf(false) }
    var copied by remember { mutableStateOf(false) }
    
    var inviteRules by remember { mutableStateOf(false) }
    var giftRules by remember { mutableStateOf(false) }
    var rechargeRules by remember { mutableStateOf(false) }
    var helpRules by remember { mutableStateOf(false) }

    fun share(platform: String) {
        val shareText = "Join me on this amazing app! Use my invite code: $code or click here: $inviteLink"
        try {
            when (platform) {
                "whatsapp" -> {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/?text=" + Uri.encode(shareText)))
                    context.startActivity(intent)
                }
                "facebook" -> {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/sharer/sharer.php?u=" + Uri.encode(inviteLink)))
                    context.startActivity(intent)
                }
                "copy" -> {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Invite link", inviteLink))
                    copied = true
                    Toast.makeText(context, "Link copied!", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TITLE, "Invite Code")
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    context.startActivity(Intent.createChooser(intent, null))
                }
            }
        } catch (_: Exception) {
            Toast.makeText(context, "Unable to open sharing app", Toast.LENGTH_SHORT).show()
        }
    }

    Box(Modifier.fillMaxSize().background(inviteRed)) {
        
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        ) {
            Box(Modifier.fillMaxWidth().height(screenHeight * .5f)) {
                InviteArt(
                    "file_000000007084821183efae0ef07553af.png",
                    Modifier.fillMaxSize(),
                    key = InviteKey.NONE,
                    scale = ContentScale.Crop,
                    alignment = Alignment.TopCenter
                )
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            0.95f to Color.Transparent, 
                            1.0f to inviteRed
                        )
                    )
                )

                Row(
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(start = 8.dp, end = 8.dp, top = 8.dp)
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = { onBack?.invoke() ?: onClose?.invoke() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                    IconButton(onClick = { helpRules = true }) {
                        Icon(Icons.Default.HelpOutline, contentDescription = "Help", tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                }
            }

            Column(
                Modifier.fillMaxWidth().widthIn(max = 448.dp).align(Alignment.CenterHorizontally).padding(horizontal = 0.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(Modifier.fillMaxWidth()) {
                    InviteArt("1788074201753~2.jpg", Modifier.fillMaxWidth().wrapContentHeight(), key = InviteKey.GREEN)
                    Row(
                        Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(start = 30.dp, end = 30.dp, bottom = 15.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Friend's Invite", color = inviteYellow, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("0", color = inviteYellow, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Box(Modifier.fillMaxWidth()) {
                    InviteArt("1788100666322~2.jpg", Modifier.fillMaxWidth().wrapContentHeight(), key = InviteKey.GREEN)
                    Column(Modifier.fillMaxWidth().padding(horizontal = 35.dp)) {
                        Row(
                            Modifier.fillMaxWidth().padding(top = 100.dp).height(24.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Invite Friends", color = inviteYellow, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            InviteReward("250000") { inviteRules = true }
                        }
                        Row(
                            Modifier.fillMaxWidth().padding(top = 51.dp).height(32.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Friends Recharge", color = inviteYellow, fontSize = 14.sp, fontWeight = FontWeight.Bold, lineHeight = 16.sp)
                                Text(">500000 Coins", color = invitePaleYellow, fontSize = 10.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.sp)
                            }
                            InviteReward("250000") { rechargeRules = true }
                        }
                        Row(
                            Modifier.fillMaxWidth().padding(top = 48.dp).height(38.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Friends Send", color = inviteYellow, fontSize = 14.sp, fontWeight = FontWeight.Bold, lineHeight = 16.sp)
                                Text("Gift", color = inviteYellow, fontSize = 14.sp, fontWeight = FontWeight.Bold, lineHeight = 16.sp)
                            }
                            InviteReward("6%") { giftRules = true }
                        }
                    }
                }

                Box(Modifier.fillMaxWidth().wrapContentHeight()) {
                    InviteArt("1788102034828~2.jpg", Modifier.fillMaxWidth().wrapContentHeight(), key = InviteKey.GREEN)

                    // Number of Invitation — bottom: 14vh (left: 85px)
                    Column(
                        Modifier.align(Alignment.BottomStart).padding(start = 85.dp, bottom = vh * 14f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("0", color = inviteYellow, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text("Number\nInvitation", color = inviteYellow, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, lineHeight = 12.sp, textAlign = TextAlign.Center)
                    }

                    // Total Coins Rewards — bottom: 15vh (right: 80px)
                    Column(
                        Modifier.align(Alignment.BottomEnd).padding(end = 80.dp, bottom = vh * 15f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        InviteArt("file_00000000e56882119c217d508b6733dc.png", Modifier.size(40.dp), key = InviteKey.WHITE)
                        Text("0", color = inviteYellow, fontSize = 24.sp, fontWeight = FontWeight.Bold, lineHeight = 25.sp)
                        Text("Total Coins\nRewards", color = inviteYellow, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, lineHeight = 12.sp, textAlign = TextAlign.Center)
                    }

                    // Claim block — bottom: 35vh (center)
                    Column(
                        Modifier.align(Alignment.BottomCenter).padding(bottom = vh * 35f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        InviteArt("file_00000000e56882119c217d508b6733dc.png", Modifier.size(50.dp), key = InviteKey.WHITE)
                        Text("0", color = inviteYellow, fontSize = 30.sp, fontWeight = FontWeight.Bold, lineHeight = 32.sp)
                        Box(
                            Modifier.padding(top = 12.dp).clip(CircleShape)
                                .background(Brush.verticalGradient(listOf(Color(0xFFE62E2E), Color(0xFFB80D0D), Color(0xFF7A0000))))
                                .border(1.5.dp, inviteYellow, CircleShape)
                                .padding(horizontal = 40.dp, vertical = 6.dp)
                        ) {
                            Text("Claim", color = inviteYellow, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
                
                Spacer(Modifier.height(100.dp))
            }
        }

        InviteArt(
            "1788074191602~2.jpg",
            Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 8.dp, bottom = 20.dp) 
                .widthIn(max = 360.dp)
                .fillMaxWidth(.88f)
                .wrapContentHeight()
                .clickable { isSheetOpen = true },
            key = InviteKey.GREEN
        )

        if (isSheetOpen) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)).clickable { isSheetOpen = false }) {
                Column(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                        .background(Color.White)
                        .padding(horizontal = 24.dp, vertical = 24.dp)
                        .clickable(enabled = false) {}
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        InviteShareItem("WhatsApp", Color(0xFF25D366), "whatsapp") { share("whatsapp") }
                        InviteShareItem("Facebook", Color(0xFF1877F2), "facebook") { share("facebook") }
                        InviteShareItem("Copy Link", Color(0xFF0088CC), "link") { share("copy") }
                        InviteShareItem("More", Color(0xFFF97316), "more") { share("more") }
                    }
                    if (copied) Text("✓ Link copied!", modifier = Modifier.fillMaxWidth().padding(top = 12.dp), textAlign = TextAlign.Center, color = Color(0xFF2563EB), fontSize = 11.sp)
                }
            }
        }
    }

    if (inviteRules) InviteRulesDialog(
        text = "If invite friend download & enter the Invite code in the input Given. Then a Inverter will Receive a 250,000Coins rewards hand to hand in there coins wallet",
        centered = true
    ) { inviteRules = false }
    
    if (giftRules) InviteRulesDialog(
        text = "If Invited friend Will send Gift to any one So a Inviter user will receive 6% of the Sended gift Value.",
        centered = true
    ) { giftRules = false }
    
    if (rechargeRules) InviteRulesDialog(
        text = "If the Invited friend Recharge more then >500000 so a inviter User will receive 250000 coins In there Coins wallet",
        centered = true
    ) { rechargeRules = false }
    
    if (helpRules) InviteRulesDialog(
        text = "1. Invite a friend to register and receive instant coins. You will get 100,000 coins when they log in. If your friend logs into the Zyora app the next day or within 3 days, they will receive an additional 150,000 coins.\n\n2. If your invited friend recharges 500,000 coins within 90 days, you will receive 6% of the coins from each recharge. Additionally, if your friend sends gift coins, you will receive 6% of the coins from the gifts they send.\n\n3. The Zyora app must not already be downloaded or registered on your friend's phone, and they must not be part of another account's network. The invitation will be considered successful—and rewards granted—only when all these conditions are met.\n\n4. Invite your friends; you will earn free coins when they recharge or send gifts.",
        signature = "Best regards,\nThe Zyora Team",
        centered = true,
        smallText = true
    ) { helpRules = false }
}

@Composable
private fun InviteReward(value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text("Get", color = inviteYellow, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        InviteArt("file_00000000e56882119c217d508b6733dc.png", Modifier.size(20.dp), key = InviteKey.WHITE)
        Text(value, color = inviteYellow, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Box(Modifier.padding(start = 1.dp).size(16.dp).clip(CircleShape).background(inviteYellow), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = inviteRed, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun InviteShareItem(label: String, color: Color, glyph: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.clickable(onClick = onClick)) {
        Box(Modifier.size(48.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
            when (glyph) {
                "whatsapp" -> InviteWhatsAppMark(Modifier.size(26.dp))
                "facebook" -> InviteBrandGlyph("M15.12 5.32H17V2.14C16.68 2.1 15.58 2 14.3 2c-2.68 0-4.52 1.64-4.52 4.66V9.3H6.75v3.55h3.03V22h3.73v-9.15h3.1l.49-3.55h-3.59V7.01c0-1.03.28-1.69 1.61-1.69z", Modifier.size(22.dp))
                "link" -> Icon(Icons.Default.Link, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                "more" -> Icon(Icons.Default.MoreHoriz, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }
        Text(label, color = Color(0xFF1F2937), fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun InviteWhatsAppMark(modifier: Modifier = Modifier) {
    val handset = remember {
        PathParser().parsePathString(
            "M17.5 14.33c-.31-.15-1.81-.89-2.09-.99-.28-.1-.48-.15-.68.15-.2.3-.79.99-.97 1.2-.18.2-.36.22-.67.07-.31-.15-1.29-.48-2.46-1.52-.91-.81-1.52-1.81-1.7-2.12-.18-.3-.02-.47.13-.62.14-.14.31-.36.46-.54.15-.18.2-.31.3-.52.1-.2.05-.38-.02-.52-.08-.15-.68-1.64-.93-2.25-.24-.6-.49-.52-.67-.53-.18-.01-.38-.01-.59-.01-.2 0-.54.08-.82.38-.28.3-1.06 1.04-1.06 2.53 0 1.49 1.09 2.93 1.24 3.13.15.2 2.14 3.26 5.17 4.57.72.31 1.28.5 1.72.64.72.23 1.38.2 1.9.12.58-.09 1.81-.74 2.06-1.46.25-.72.25-1.34.18-1.46-.07-.12-.27-.2-.58-.35z"
        ).toPath()
    }
    androidx.compose.foundation.Canvas(modifier) {
        drawContext.transform.scale(size.width / 24f, size.height / 24f, pivot = androidx.compose.ui.geometry.Offset.Zero)
        val bgPath = PathParser().parsePathString("M12.01 2C6.48 2 2 6.48 2 12c0 1.95.56 3.77 1.5 5.25L2 22l4.89-1.47c1.44.89 3.16 1.42 5.11 1.42 5.53 0 10.01-4.48 10.01-10s-4.48-10-10-10z").toPath()
        drawPath(bgPath, color = Color.White)
        drawPath(handset, color = Color(0xFF25D366))
        drawContext.transform.scale(24f / size.width, 24f / size.height, pivot = androidx.compose.ui.geometry.Offset.Zero)
    }
}

@Composable
private fun InviteBrandGlyph(pathData: String, modifier: Modifier = Modifier) {
    val path = remember(pathData) { PathParser().parsePathString(pathData).toPath() }
    androidx.compose.foundation.Canvas(modifier) {
        drawContext.transform.scale(size.width / 24f, size.height / 24f, pivot = androidx.compose.ui.geometry.Offset.Zero)
        drawPath(path, color = Color.White)
        drawContext.transform.scale(24f / size.width, 24f / size.height, pivot = androidx.compose.ui.geometry.Offset.Zero)
    }
}

@Composable
private fun InviteRulesDialog(
    text: String, 
    signature: String? = null,
    centered: Boolean, 
    smallText: Boolean = false, 
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxWidth().padding(horizontal = 32.dp), contentAlignment = Alignment.Center) {
            Column(
                Modifier.fillMaxWidth().widthIn(max = 300.dp).clip(RoundedCornerShape(16.dp))
                    .background(invitePopupPurple).border(3.dp, Color(0xFFFACC15), RoundedCornerShape(16.dp))
                    .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 24.dp)
            ) {
                Text("Rules", modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), color = inviteYellow, fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, textAlign = TextAlign.Center)
                
                Text(
                    text,
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    fontSize = if (smallText) 11.sp else 12.sp,
                    lineHeight = if (smallText) 16.sp else 18.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = if (centered) TextAlign.Center else TextAlign.Start
                )
                
                if (signature != null) {
                    Text(
                        signature,
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        color = Color.White,
                        fontSize = if (smallText) 11.sp else 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}

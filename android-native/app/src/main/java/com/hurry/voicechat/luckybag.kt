package com.hurry.voicechat

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage

private val LuckyGold = Color(0xFF8B5E3C)
private val LuckyCream = Color(0xFFF5D9A8)
private val LuckyPink = Color(0xFFFE3C68)

@Composable
fun LuckyBag(senderId: String, senderName: String, senderImage: String, roomId: String, roomName: String, onClose: () -> Unit = {}, onSent: (String, String, Long, Int, String) -> Unit = { _, _, _, _, _ -> }) {
    var selectedCoins by remember { mutableIntStateOf(10000) }
    var recipients by remember { mutableIntStateOf(5) }
    var showClaim by remember { mutableStateOf(false) }
    var showRules by remember { mutableStateOf(false) }
    var sending by remember { mutableStateOf(false) }
    var sendError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    BackHandler(enabled = true) {
        when {
            showRules -> showRules = false
            showClaim -> showClaim = false
            else -> onClose()
        }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        // Main Background dark overlay
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.40f)).clickable { onClose() })
        
        // ==================== MAIN LUCKY BAG SENDER PANEL ====================
        Box(
            Modifier.fillMaxWidth().widthIn(max = 440.dp).heightIn(min = 400.dp, max = 520.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(Modifier.fillMaxWidth()) {
                
                // Base Image
                AsyncImage(
                    model = "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/file_000000008aec8230a4f2d1854cb19882.png",
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.FillWidth 
                )
                
                // DOC icon (Claim Details) - Top Start
                IconButton(
                    onClick = { showClaim = true },
                    modifier = Modifier.align(Alignment.TopStart).padding(start = 16.dp, top = 32.dp).size(28.dp)
                ) {
                    Icon(Icons.Default.Description, contentDescription = "Claim details", tint = Color.White, modifier = Modifier.fillMaxSize(0.9f))
                }
                
                // ? Help icon (Rules) - Top End
                IconButton(
                    onClick = { showRules = true },
                    modifier = Modifier.align(Alignment.TopEnd).padding(end = 16.dp, top = 26.dp).size(30.dp)
                ) {
                    Icon(Icons.Default.HelpOutline, contentDescription = "Rules", tint = Color.White, modifier = Modifier.fillMaxSize())
                }
                
                // Options and Send Button
                Column(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 18.dp)
                ) {
                    Text("Gold Quantity", color = LuckyGold, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
                    val coinOptions = listOf(10000, 100000, 200000, 500000, 1000000, 1500000, 2000000)
                    coinOptions.chunked(4).forEach { row ->
                        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            row.forEach { amount ->
                                LuckyOption(
                                    label = amount.toString(),
                                    selected = selectedCoins == amount,
                                    modifier = Modifier.weight(1f),
                                    onClick = { selectedCoins = amount }
                                )
                            }
                            repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                    Text("Number of people", color = LuckyGold, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(5, 10, 30, 50).forEach { count ->
                            LuckyOption(count.toString(), recipients == count, Modifier.weight(1f)) { recipients = count }
                        }
                    }
                    Button(
                        enabled = !sending,
                        onClick = {
                            if (sending) return@Button
                            sending = true
                            sendError = null
                            scope.launch {
                                val result = withContext(Dispatchers.IO) {
                                    runCatching {
                                        val existingWalletBalance = WalletDatabase.get(context).walletDao().getUserData()?.balance ?: 0L
                                        val payload = JSONObject()
                                            .put("senderId", senderId)
                                            .put("initialBalance", existingWalletBalance)
                                            .put("senderName", senderName)
                                            .put("senderImage", senderImage)
                                            .put("roomId", roomId)
                                            .put("roomName", roomName)
                                            .put("amount", selectedCoins)
                                            .put("recipients", recipients)
                                        val connection = (URL("https://hurry-voice-chat-lz75.onrender.com/api/lucky-bag/send").openConnection() as HttpURLConnection).apply {
                                            requestMethod = "POST"
                                            connectTimeout = 12000
                                            readTimeout = 15000
                                            doOutput = true
                                            setRequestProperty("Content-Type", "application/json")
                                        }
                                        connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
                                        val status = connection.responseCode
                                        val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                                        val response = stream.bufferedReader().use { it.readText() }
                                        connection.disconnect()
                                        val json = try {
                                            JSONObject(response)
                                        } catch (_: Exception) {
                                            val preview = response.replace(Regex("\\s+"), " ").take(180)
                                            throw IllegalStateException(
                                                if (response.trimStart().startsWith("<!DOCTYPE", ignoreCase = true) ||
                                                    response.trimStart().startsWith("<html", ignoreCase = true)) {
                                                    "Lucky Bag server returned an HTML page (HTTP $status), not JSON. The backend deployment or API URL is unavailable. Please redeploy/check the backend."
                                                } else {
                                                    "Lucky Bag server returned an invalid response (HTTP $status): $preview"
                                                }
                                            )
                                        }
                                        if (status !in 200..299 || !json.optBoolean("success")) {
                                            throw IllegalStateException(json.optString("error", "Lucky Bag send failed"))
                                        }
                                        json
                                    }
                                }
                                sending = false
                                result.onSuccess { json ->
                                    onSent(senderName, senderImage, selectedCoins.toLong(), recipients, json.optString("bagId"))
                                    showClaim = false
                                    onClose()
                                }.onFailure { error ->
                                    sendError = error.message ?: "Lucky Bag send failed"
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(0.90f).align(Alignment.CenterHorizontally).padding(top = 18.dp).height(48.dp),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = LuckyPink)
                    ) {
                        Text(if (sending) "Sending..." else "Send", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                    sendError?.let {
                        Text(it, color = Color.Red, fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 6.dp))
                    }
                }
            }
        }

        // ==================== RULES BLACK BOTTOM SHEET ====================
        if (showRules) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .40f)).clickable { showRules = false }, contentAlignment = Alignment.BottomCenter) {
                Column(
                    Modifier.fillMaxWidth().widthIn(max = 440.dp).clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)).background(Color.Black).clickable(enabled = false) {}
                ) {
                    Box(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 12.dp), contentAlignment = Alignment.Center) {
                        IconButton(onClick = { showRules = false }, modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp)) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        Text("Lucky Packet", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp)) {
                        Text("- Rule -", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(18.dp))
                        listOf(
                            "Click and select the Amount to be send in the lucky packet",
                            "Select the Number of the user who can open",
                            "If no user grab lucky packet coins will be return in the wallet of the packet sender"
                        ).forEachIndexed { index, rule ->
                            Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("${index + 1}.", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                Text(rule, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, lineHeight = 21.sp)
                            }
                        }
                    }
                }
            }
        }

        // ==================== FULL SCREEN DOC CLAIM SCREEN ====================
        if (showClaim) {
            Box(Modifier.fillMaxSize().background(Color.White).clickable(enabled = false) {}) {
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxWidth()) {
                        
                        // Top Header Curve (Red & Gold Canvas matching SVG)
                        Canvas(modifier = Modifier.fillMaxWidth().aspectRatio(690f / 295f)) {
                            val w = size.width
                            val h = size.height
                            
                            // Red Header Curve
                            val redBrush = Brush.verticalGradient(listOf(Color(0xFFF12D4D), Color(0xFFF52E4D)))
                            val redPath = Path().apply {
                                moveTo(0f, 0f)
                                lineTo(w, 0f)
                                lineTo(w, h * (164f/295f))
                                cubicTo(w * (649f/690f), h * (225f/295f), w * (577f/690f), h * (256f/295f), w * (470f/690f), h * (266f/295f))
                                cubicTo(w * (418f/690f), h * (271f/295f), w * (379f/690f), h * (273f/295f), w * (345f/690f), h * (273f/295f))
                                cubicTo(w * (235f/690f), h * (273f/295f), w * (100f/690f), h * (251f/295f), 0f, h * (164f/295f))
                                close()
                            }
                            drawPath(redPath, redBrush)
                            
                            // Golden Curved Border under the red header
                            val goldBrush = Brush.verticalGradient(listOf(Color(0xFFF8CFA5), Color(0xFFFFE6A2), Color(0xFFFFF0BB)))
                            val goldPath = Path().apply {
                                moveTo(0f, h * (163f/295f))
                                cubicTo(w * (85f/690f), h * (253f/295f), w * (216f/690f), h * (277f/295f), w * (345f/690f), h * (281f/295f))
                                cubicTo(w * (481f/690f), h * (280f/295f), w * (617f/690f), h * (253f/295f), w, h * (163f/295f))
                                lineTo(w, h * (190f/295f))
                                cubicTo(w * (603f/690f), h * (276f/295f), w * (480f/690f), h * (293f/295f), w * (345f/690f), h * (292f/295f))
                                cubicTo(w * (204f/690f), h * (289f/295f), w * (77f/690f), h * (265f/295f), 0f, h * (190f/295f))
                                close()
                            }
                            drawPath(goldPath, goldBrush)
                        }

                        // Top Bar: Left Direction Arrow and Middle Heading
                        Box(Modifier.fillMaxWidth().padding(top = 32.dp)) {
                            // Left Direction Arrow
                            IconButton(
                                onClick = { showClaim = false }, 
                                modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp)
                            ) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                            }
                            
                            // Middle Heading
                            Text(
                                text = "Claim details",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }

                    Spacer(Modifier.height(56.dp))

                    // Grey Header separator
                    Box(Modifier.fillMaxWidth().height(8.dp).background(Color(0xFFF0F0F0)))
                    
                    // Claim status Text
                    Text(
                        text = "Unclaimed, Opened 30/30, Total 500,000/500,000 Gold Coins",
                        color = Color(0xFFD0A27D),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                    )

                    // Line separator
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFEEEEEE)))
                    
                    // Area for claimed user list... (Empty for now)

                }
            }
        }
    }
}

@Composable
private fun LuckyOption(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.height(36.dp).clip(RoundedCornerShape(50))
            .background(if (selected) LuckyPink else LuckyCream)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (selected) Color.White else LuckyGold, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 1)
    }
}

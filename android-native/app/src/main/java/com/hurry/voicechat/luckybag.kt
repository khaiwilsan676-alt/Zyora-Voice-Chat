package com.hurry.voicechat

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
fun LuckyBag(onClose: () -> Unit = {}) {
    var selectedCoins by remember { mutableIntStateOf(10000) }
    var recipients by remember { mutableIntStateOf(5) }
    var showClaim by remember { mutableStateOf(false) }
    var showRules by remember { mutableStateOf(false) }

    BackHandler(enabled = true) {
        when {
            showRules -> showRules = false
            showClaim -> showClaim = false
            else -> onClose()
        }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.40f)).clickable { onClose() })
        
        Box(
            Modifier.fillMaxWidth().widthIn(max = 440.dp).heightIn(min = 400.dp, max = 520.dp),
            contentAlignment = Alignment.BottomCenter // Isse pura content bottom mein chipak jayega
        ) {
            // Ek inner wrapper box jisme Image base rahegi aur icons/content uske upar overlap karenge
            Box(Modifier.fillMaxWidth()) {
                
                // Background Image - Bottom aligned by default inside this wrapping box
                AsyncImage(
                    model = "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/file_000000008aec8230a4f2d1854cb19882.png",
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.FillWidth 
                )
                
                // Doc icon overlapping on Top Start of the image
                IconButton(
                    onClick = { showClaim = true },
                    modifier = Modifier.align(Alignment.TopStart).padding(start = 16.dp, top = 32.dp).size(28.dp)
                ) {
                    Icon(Icons.Default.Description, contentDescription = "Claim details", tint = Color.White, modifier = Modifier.fillMaxSize(0.9f))
                }
                
                // ? Help icon overlapping on Top End of the image
                IconButton(
                    onClick = { showRules = true },
                    modifier = Modifier.align(Alignment.TopEnd).padding(end = 16.dp, top = 26.dp).size(30.dp)
                ) {
                    Icon(Icons.Default.HelpOutline, contentDescription = "Rules", tint = Color.White, modifier = Modifier.fillMaxSize())
                }
                
                // Bottom content (Gold Quantity, Users, Send Button) overlapping on Bottom Center of the image
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
                        onClick = { showClaim = true },
                        modifier = Modifier.fillMaxWidth(0.90f).align(Alignment.CenterHorizontally).padding(top = 18.dp).height(48.dp),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = LuckyPink)
                    ) {
                        Text("Send", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                }
            }
        }

        if (showRules) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .40f)).clickable { showRules = false }, contentAlignment = Alignment.BottomCenter) {
                Column(
                    Modifier.fillMaxWidth().widthIn(max = 440.dp).clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)).background(Color(0xFFFFE8CC))
                ) {
                    Box(Modifier.fillMaxWidth().background(Color(0xFFF13E58)).padding(top = 22.dp, bottom = 12.dp), contentAlignment = Alignment.Center) {
                        IconButton(onClick = { showRules = false }, modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp)) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        Text("Lucky Packet", color = Color(0xFFC47A3A), fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)).background(Color(0xFFFFE8B5)).padding(horizontal = 26.dp, vertical = 6.dp))
                    }
                    Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 32.dp)) {
                        Text("- Rule -", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = Color(0xFFC47A3A), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(18.dp))
                        listOf(
                            "Click and select the Amount to be send in the lucky packet",
                            "Select the Number of the user who can open",
                            "If no user grab lucky packet coins will be return in the wallet of the packet sender"
                        ).forEachIndexed { index, rule ->
                            Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("${index + 1}.", color = Color(0xFFC47A3A), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                Text(rule, color = Color(0xFFC47A3A), fontSize = 14.sp, fontWeight = FontWeight.Medium, lineHeight = 21.sp)
                            }
                        }
                    }
                }
            }
        }

        if (showClaim) {
            Column(Modifier.fillMaxSize().background(Color.White)) {
                Box(Modifier.fillMaxWidth().fillMaxHeight(0.42f).background(Color(0xFFE92350))) {
                    IconButton(onClick = { showClaim = false }, modifier = Modifier.align(Alignment.TopStart).padding(8.dp)) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Text("Claim details", modifier = Modifier.align(Alignment.Center).padding(top = 26.dp), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Box(Modifier.fillMaxWidth().height(34.dp).align(Alignment.BottomCenter).background(Color(0xFFFBE0C5)))
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


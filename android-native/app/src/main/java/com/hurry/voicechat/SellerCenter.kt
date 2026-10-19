package com.hurry.voicechat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import coil3.compose.AsyncImage
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SellerText = Color(0xFF1F2937)
private val SellerMuted = Color(0xFF9CA3AF)
private val SellerBlue = Color(0xFF60A5FA)
private val SellerField = Color(0xFFF7F8FA)
private val SellerLine = Color(0xFFF3F4F6)

@Composable
fun SellerCenter(onBack: () -> Unit = {}) {
    var showingDetails by remember { mutableStateOf(false) }
    var salesMethod by remember { mutableStateOf("User") }
    var targetId by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    val balance = 0L

    Column(Modifier.fillMaxSize().background(Color.White)) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SellerBackArrow { if (showingDetails) showingDetails = false else onBack() }
            Text(
                if (showingDetails) "Details" else "Coin Seller Center",
                modifier = Modifier.weight(1f).padding(end = 24.dp),
                fontSize = 17.sp, fontWeight = FontWeight.Bold, color = SellerText,
                textAlign = TextAlign.Center, letterSpacing = 0.3.sp
            )
        }
        if (showingDetails) {
            SellerDetailsView()
        } else {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 32.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
                        Box(Modifier.size(48.dp).border(1.dp, Color(0xFFE5E7EB), CircleShape).background(Color(0xFFF9FAFB), CircleShape), contentAlignment = Alignment.Center) {
                            Text("K", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = SellerBlue)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("꧁Ks༒Prad...", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = SellerText)
                            Spacer(Modifier.height(2.dp))
                            Text("ID:116943047   ▢", fontSize = 12.sp, color = SellerMuted)
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(SellerLine))
                    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("WhatsApp", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = SellerText)
                        Spacer(Modifier.weight(1f))
                        Text("+91 9837152239   ›", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = SellerText)
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Payment Method", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = SellerText)
                        Spacer(Modifier.weight(1f))
                        Text("🇮🇳   ›", fontSize = 17.sp, color = SellerText)
                    }
                }
                Box(Modifier.fillMaxWidth().height(8.dp).background(Color(0xFFF9FAFB), RoundedCornerShape(8.dp)))
                Column(Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
                    Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AsyncImage(
                                    model = "https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/file_00000000e56882119c217d508b6733dc.png",
                                    contentDescription = "Coins",
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("%,d".format(balance), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = SellerText)
                            }
                            Spacer(Modifier.height(2.dp))
                            Text("Available Balance", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = SellerMuted)
                        }
                        Row(
                            Modifier.clickable { showingDetails = true }.padding(3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.material3.Icon(
                                imageVector = Icons.Filled.Description,
                                contentDescription = "Details",
                                tint = SellerBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(3.dp))
                            Text("Details", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SellerBlue)
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(SellerLine))
                    Row(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Total Balance:", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF6B7280))
                        Spacer(Modifier.weight(1f))
                        AsyncImage(
                            model = "https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/file_00000000e56882119c217d508b6733dc.png",
                            contentDescription = "Coins",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("%,d".format(balance), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SellerText)
                    }
                    Row(Modifier.fillMaxWidth().padding(bottom = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Sales method:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SellerText)
                        Spacer(Modifier.width(24.dp))
                        listOf("User", "Seller").forEach { option ->
                            Row(
                                Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { salesMethod = option }.padding(end = 18.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    Modifier.size(16.dp).border(2.dp, if (salesMethod == option) SellerBlue else Color(0xFFD1D5DB), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (salesMethod == option) Box(Modifier.size(8.dp).background(SellerBlue, CircleShape))
                                }
                                Spacer(Modifier.width(6.dp))
                                Text(option, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = SellerText)
                            }
                        }
                    }
                    Text(if (salesMethod == "User") "User ID:" else "Seller ID:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SellerText)
                    Spacer(Modifier.height(8.dp))
                    SellerInput(value = targetId, onValueChange = { targetId = it }, hint = "Please input the id", modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), trailing = "Check")
                    Text("Amount:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SellerText)
                    Spacer(Modifier.height(8.dp))
                    SellerInput(value = amount, onValueChange = { amount = it.filter(Char::isDigit) }, hint = "Please input the number", modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp))
                    if (amount.toLongOrNull() != null && amount.toLong() > balance) {
                        Text("Insufficient balance", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFFEF4444), modifier = Modifier.padding(start = 4.dp, bottom = 10.dp))
                    }
                    Box(
                        Modifier.fillMaxWidth().height(48.dp).background(Color(0xFFBFDBFE), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Transfer", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun SellerBackArrow(onClick: () -> Unit) {
    Box(Modifier.size(32.dp).clickable { onClick() }, contentAlignment = Alignment.Center) {
        androidx.compose.material3.Icon(
            imageVector = Icons.Filled.ArrowBack,
            contentDescription = "Back",
            tint = Color.Black,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun SellerInput(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
    trailing: String? = null
) {
    Row(
        modifier.background(SellerField, RoundedCornerShape(12.dp)).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text(hint, fontSize = 14.sp, color = Color(0xFF9CA3AF))
            BasicTextField(value = value, onValueChange = onValueChange, singleLine = true, textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = SellerText), modifier = Modifier.fillMaxWidth())
        }
        if (trailing != null) {
            Spacer(Modifier.width(10.dp))
            Text(trailing, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = SellerBlue)
        }
    }
}

@Composable
private fun SellerDetailsView() {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        var searchId by remember { mutableStateOf("") }
        SellerInput(searchId, { searchId = it }, "Please input the user id", Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth().padding(vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Order Type", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = SellerText)
            Spacer(Modifier.width(8.dp))
            Text("All  ▾", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = SellerText)
        }
        val records = listOf(
            Triple("💫FAKE SMILE🦅", "09/04/2026 19:16", "-6,450,000"),
            Triple("🇮🇳Indian_Tiger🇮🇳", "09/04/2026 19:10", "-2,150,000"),
            Triple("🖤KING—⭐", "09/04/2026 19:06", "-2,150,000"),
            Triple("Asael🖤", "09/04/2026 18:52", "-2,150,000")
        )
        records.forEach { record ->
            Row(Modifier.fillMaxWidth().border(width = 0.dp, color = Color.Transparent).padding(vertical = 16.dp), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text("Transfer", Modifier.background(Color(0xFFDBEAFE), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 3.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2563EB))
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("To User", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SellerText)
                        Spacer(Modifier.width(8.dp))
                        Box(Modifier.size(32.dp).background(Color(0xFFF3F4F6), CircleShape), contentAlignment = Alignment.Center) { Text("●", color = Color(0xFF9CA3AF)) }
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(record.first, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SellerText)
                            Text("116943047", fontSize = 10.sp, color = SellerMuted)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(record.second, fontSize = 12.sp, color = SellerMuted)
                }
                Spacer(Modifier.width(6.dp))
                Row(
                    modifier = Modifier.padding(top = 62.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AsyncImage(
                        model = "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/file_00000000e56882119c217d508b6733dc.png",
                        contentDescription = "Coins",
                        modifier = Modifier.size(16.dp)
                    )
                    Text(record.third, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SellerText)
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(SellerLine))
        }
    }
}

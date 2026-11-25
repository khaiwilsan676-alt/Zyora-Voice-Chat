package com.hurry.voicechat

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val PkBackground = Color(0xFF090B12)
private val PkPanel = Color(0xFF171B27)
private val PkPink = Color(0xFFFF477E)
private val PkBlue = Color(0xFF438BFF)

@Composable
fun PKBattlePage(onClose: () -> Unit) {
    var selectedMode by remember { mutableStateOf("1 vs 1") }
    BackHandler(onBack = onClose)

    Column(
        Modifier.fillMaxSize().background(PkBackground)
            .statusBarsPadding().navigationBarsPadding()
            .padding(horizontal = 18.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().height(56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(
                "PK Battle",
                Modifier.weight(1f),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White.copy(alpha = .8f))
            }
        }

        Spacer(Modifier.height(18.dp))
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF30152C), Color(0xFF15264A), Color(0xFF171B27))))
                .padding(vertical = 28.dp, horizontal = 16.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("ROOM PK CHALLENGE", color = Color.White.copy(alpha = .7f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Spacer(Modifier.height(24.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    PkSide("YOUR ROOM", PkPink, Modifier.weight(1f))
                    Text("VS", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
                    PkSide("OPPONENT", PkBlue, Modifier.weight(1f))
                }
                Spacer(Modifier.height(20.dp))
                Text("Find another room and start a friendly battle", color = Color.White.copy(alpha = .72f), fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        }

        Spacer(Modifier.height(26.dp))
        Text("Battle mode", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf("1 vs 1", "2 vs 2", "3 vs 3").forEach { mode ->
                val active = selectedMode == mode
                Box(
                    Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                        .background(if (active) Color(0xFF342039) else PkPanel)
                        .border(1.dp, if (active) PkPink else Color.Transparent, RoundedCornerShape(12.dp))
                        .clickable { selectedMode = mode }.padding(vertical = 13.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(mode, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("How PK works", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(PkPanel).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PkInfoRow("1", "Choose a battle mode")
            PkInfoRow("2", "Invite another room to compete")
            PkInfoRow("3", "Room gifts contribute to the live score")
        }

        Spacer(Modifier.weight(1f))
        Text(
            "PK matchmaking and live scoring will appear here when enabled.",
            Modifier.fillMaxWidth().padding(bottom = 14.dp),
            color = Color.White.copy(alpha = .42f), fontSize = 11.sp, textAlign = TextAlign.Center
        )
        Button(
            onClick = { },
            enabled = false,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(15.dp),
            colors = ButtonDefaults.buttonColors(disabledContainerColor = Color(0xFF343744), disabledContentColor = Color.White.copy(alpha = .6f))
        ) {
            Text("Start PK", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun PkSide(label: String, accent: Color, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(64.dp).clip(CircleShape).background(accent.copy(alpha = .16f))
                .border(1.dp, accent.copy(alpha = .8f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Person, contentDescription = null, tint = accent, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PkInfoRow(number: String, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(25.dp).clip(CircleShape).background(Color(0xFF343044)),
            contentAlignment = Alignment.Center
        ) { Text(number, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.width(10.dp))
        Text(label, color = Color.White.copy(alpha = .82f), fontSize = 13.sp)
    }
}

package com.hurry.voicechat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight

@Composable
fun FollowPage(title: String, onBack: () -> Unit) {
    val socialPage = title in setOf("Friends", "Fans", "Following", "Visitors")
    Column(Modifier.fillMaxSize().background(Color.White).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("‹", fontSize = 36.sp, color = Color(0xFF222222), modifier = Modifier.clickable { onBack() })
            Spacer(Modifier.width(12.dp))
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.Normal, color = Color(0xFF111827))
        }
        if (socialPage) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(bottom = 48.dp)) {
                    androidx.compose.foundation.Canvas(Modifier.size(76.dp)) {
                        val ink = Color(0xFFD8DCE3)
                        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.4.dp.toPx())
                        val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height * 0.31f)
                        drawCircle(ink, radius = size.minDimension * 0.13f, center = center, style = stroke)
                        drawArc(ink, startAngle = 200f, sweepAngle = 140f, useCenter = false,
                            topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.17f, size.height * 0.36f),
                            size = androidx.compose.ui.geometry.Size(size.width * 0.66f, size.height * 0.53f), style = stroke)
                        drawCircle(ink, radius = size.minDimension * 0.09f, center = androidx.compose.ui.geometry.Offset(size.width * 0.75f, size.height * 0.40f), style = stroke)
                        drawArc(ink, startAngle = 205f, sweepAngle = 130f, useCenter = false,
                            topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.58f, size.height * 0.46f),
                            size = androidx.compose.ui.geometry.Size(size.width * 0.34f, size.height * 0.36f), style = stroke)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("No $title yet", fontSize = 14.sp, color = Color(0xFF9CA3AF))
                }
            }
        }
    }
}


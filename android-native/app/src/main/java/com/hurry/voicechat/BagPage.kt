package com.hurry.voicechat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BagPage(onBack: () -> Unit = {}) {
    Column(Modifier.fillMaxSize().background(Color.White)) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 12.dp)
        ) {
            Text("‹", fontSize = 34.sp, color = Color(0xFF222222),
                modifier = Modifier.clickableBack(onBack))
            Spacer(Modifier.width(12.dp))
            Text("Bag", fontSize = 20.sp, color = Color(0xFF111827))
        }
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            item {
                Text("My Bag", fontSize = 18.sp, color = Color(0xFF111827), modifier = Modifier.padding(vertical = 18.dp))
                Text("Your items and rewards will appear here.", fontSize = 14.sp, color = Color(0xFF6B7280))
            }
        }
    }
}

private fun Modifier.clickableBack(onBack: () -> Unit): Modifier =
    this.clickable(onClick = onBack)

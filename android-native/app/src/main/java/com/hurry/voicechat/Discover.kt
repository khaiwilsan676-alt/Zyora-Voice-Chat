package com.hurry.voicechat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DiscoverPage(
    onBack: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf("Discover") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("Following", "Discover").forEach { item ->
                Column(
                    modifier = Modifier
                        .clickable(
                            interactionSource = null,
                            indication = null
                        ) { selectedTab = item }
                        .padding(horizontal = 18.dp)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = item,
                        color = if (selectedTab == item) Color(0xFF202124) else Color(0xFF858585),
                        fontSize = 16.sp,
                        fontWeight = if (selectedTab == item) FontWeight.Bold else FontWeight.Medium
                    )
                    Spacer(Modifier.height(5.dp))
                    Box(
                        Modifier
                            .width(24.dp)
                            .height(3.dp)
                            .background(
                                if (selectedTab == item) Color(0xFF3B82F6) else Color.Transparent,
                                RoundedCornerShape(2.dp)
                            )
                    )
                }
            }
        }
    }
}

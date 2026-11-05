package com.hurry.voicechat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage // Ensure you have coil dependency for loading image paths

// Fake User Data Model
data class VisitorData(
    val name: String,
    val gender: String, // "M" for Male, "F" for Female
    val date: String,
    val views: Int,
    val avatarUrl: String
)

@Composable
fun VisitorsPage(onBack: () -> Unit) {
    // Tab State: 0 for "My Visitors", 1 for "Who I have visited"
    var selectedTab by remember { mutableStateOf(1) }

    // Fake Data matching your screenshot
    val fakeVisitors = listOf(
        VisitorData("Ghost", "M", "04/11 18:53", 87, "https://i.pravatar.cc/150?u=1"),
        VisitorData("HUMSAFAR", "M", "04/11 18:09", 4, "https://i.pravatar.cc/150?u=2"),
        VisitorData("MEET !!", "F", "04/11 18:09", 2, "https://i.pravatar.cc/150?u=3"),
        VisitorData("palak", "F", "04/11 14:47", 7, "https://i.pravatar.cc/150?u=4"),
        VisitorData("Current", "M", "04/11 14:46", 74, "https://i.pravatar.cc/150?u=5"),
        VisitorData("जाटनी", "F", "04/11 01:06", 2, "https://i.pravatar.cc/150?u=6")
    )

    // Top 3% Blue color mixing into White page
    val topBlueGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFE0F0FF),
            Color.White,
            Color.White
        ),
        startY = 0f,
        endY = 400f
    )

    Column(
        Modifier
            .fillMaxSize()
            .background(topBlueGradient)
            .statusBarsPadding()
    ) {
        // Top Bar
        Row(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = Color(0xFF222222),
                modifier = Modifier.clickable { onBack() }
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = "Visitors",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF111827),
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.width(40.dp)) // To balance the back arrow
        }

        // Tabs
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TabItem(
                title = "My Visitors",
                isSelected = selectedTab == 0,
                onClick = { selectedTab = 0 }
            )
            TabItem(
                title = "Who I have visited",
                isSelected = selectedTab == 1,
                onClick = { selectedTab = 1 }
            )
        }
        Divider(color = Color(0xFFE5E7EB), thickness = 1.dp)

        // List & Banner Content
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                // Banner Logic based on Selected Tab
                val bannerImage = if (selectedTab == 0) {
                    "public/IMG_20261105_125016.png" // My Visitors Banner
                } else {
                    "public/IMG_20261105_124951.png" // Who I have visited Banner
                }

                AsyncImage(
                    model = bannerImage,
                    contentDescription = "Banner",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0D251C)) // Dark fallback color
                )
                Spacer(Modifier.height(16.dp))
            }

            // User List Logic
            items(fakeVisitors) { visitor ->
                VisitorItemRow(
                    visitor = visitor,
                    isMyVisitorsTab = selectedTab == 0
                )
            }
        }
    }
}

@Composable
fun TabItem(title: String, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) Color(0xFF111827) else Color(0xFF9CA3AF)
        )
        Spacer(Modifier.height(8.dp))
        // Underline Indicator
        if (isSelected) {
            Box(
                Modifier
                    .height(3.dp)
                    .width(40.dp)
                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                    .background(Color(0xFF222222))
            )
        } else {
            Spacer(Modifier.height(3.dp))
        }
    }
}

@Composable
fun VisitorItemRow(visitor: VisitorData, isMyVisitorsTab: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Profile Avatar
        AsyncImage(
            model = visitor.avatarUrl,
            contentDescription = "Avatar",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(Color.LightGray)
        )
        Spacer(Modifier.width(12.dp))

        // Name, Gender, and Views Info
        Column(Modifier.weight(1f)) {
            // Apply Blur if it's "My Visitors" Tab
            val blurModifier = if (isMyVisitorsTab) Modifier.blur(8.dp) else Modifier

            Row(verticalAlignment = Alignment.CenterVertically, modifier = blurModifier) {
                Text(
                    text = visitor.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(Modifier.width(6.dp))
                
                // Gender Icon Background Circle
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(if (visitor.gender == "M") Color(0xFF3B82F6) else Color(0xFFEC4899))
                ) {
                    Text(
                        text = if (visitor.gender == "M") "♂" else "♀",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(Modifier.height(4.dp))
            
            // Date and Views Count
            Text(
                text = "${visitor.date}  |  View ${visitor.views} times",
                fontSize = 12.sp,
                color = Color(0xFF9CA3AF)
            )
        }

        // Chat Icon Logic (Only show if NOT in "My Visitors" Tab)
        if (!isMyVisitorsTab) {
            AsyncImage(
                model = "public/IMG_20261105_125330.png",
                contentDescription = "Chat",
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
            )
        }
    }
}

package com.hawa.app.nativeapp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

data class ActiveUser(val accountId: String, val name: String, val image: String)

@Composable
fun ActiveUsers(isOpen: Boolean, onClose: () -> Unit, roomUsers: Array<ActiveUser>, onOpenProfile: (ActiveUser) -> Unit, onCopyUserId: (String) -> Unit) {
    if (!isOpen) return
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.30f)).clickable { onClose() })
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.30f).clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)).background(Color.White)) {
            Box(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 12.dp)) {
                Text("Active Users", Modifier.fillMaxWidth(), color = Color(0xFF1F2937), fontWeight = FontWeight.Bold)
            }
            Box(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp, vertical = 12.dp)) {
                if (roomUsers.isNotEmpty()) {
                    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(roomUsers.toList(), key = { it.accountId }) { u ->
                            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFFF9FAFB)).padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                AsyncImage(model = u.image.ifBlank { "/default-avatar.png" }, contentDescription = u.name, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(20.dp)).clickable { onOpenProfile(u) }, contentScale = ContentScale.Crop)
                                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                    Text(u.name, color = Color(0xFF1F2937), fontWeight = FontWeight.SemiBold, maxLines = 1)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("ID: " + u.accountId, color = Color(0xFF9CA3AF))
                                        Text("⧉", Modifier.padding(start = 4.dp).clickable { onCopyUserId(u.accountId) }, color = Color(0xFF9CA3AF))
                                    }
                                }
                            }
                        }
                    }
                } else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No active users", color = Color(0xFF9CA3AF)) }
            }
        }
    }
}
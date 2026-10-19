package com.hurry.voicechat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun PublicProfile(
    user: RoomProfileUser,
    onBack: () -> Unit = {},
    onFollow: () -> Unit = {},
    onMessage: () -> Unit = {}
) {
    Column(Modifier.fillMaxSize().background(Color.White)) {
        Row(Modifier.fillMaxWidth().padding(top = 28.dp, start = 8.dp, end = 16.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("‹") }
            Text("Profile", style = MaterialTheme.typography.titleLarge)
        }
        Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(104.dp).clip(CircleShape).background(Color.LightGray))
            Spacer(Modifier.height(12.dp))
            Text(user.name, style = MaterialTheme.typography.headlineSmall)
            Text(user.flag + " " + user.country)
            Text("ID: " + user.accountId.ifBlank { user.id }, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(12.dp))
            Text(user.bio.ifBlank { "No bio" }, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onFollow) { Text(if (user.isFollowing) "Following" else "Follow") }
                OutlinedButton(onClick = onMessage) { Text("Message") }
            }
        }
    }
}
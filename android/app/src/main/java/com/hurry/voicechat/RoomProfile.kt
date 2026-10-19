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

data class RoomProfileUser(
    val id: String = "", val uid: String = "", val accountId: String = "",
    val name: String = "User", val image: String = "", val gender: String = "♂",
    val age: Int = 24, val country: String = "India", val flag: String = "🇮🇳",
    val followers: Int = 0, val bio: String = "", val isFollowing: Boolean = false,
    val isOnline: Boolean = true, val isInSeat: Boolean = false,
    val isMuted: Boolean = false, val isLocked: Boolean = false
)

@Composable
fun RoomProfile(
    user: RoomProfileUser,
    isCurrentUser: Boolean = false,
    isRoomOwner: Boolean = false,
    onClose: () -> Unit = {},
    onCopyId: () -> Unit = {},
    onLeaveSeat: () -> Unit = {},
    onMention: () -> Unit = {},
    onFollow: () -> Unit = {},
    onMessage: () -> Unit = {},
    onThirdAction: () -> Unit = {},
    onMute: () -> Unit = {},
    onLock: () -> Unit = {},
    onKickOut: () -> Unit = {},
    onReport: () -> Unit = {}
) {
    val height = when {
        !isCurrentUser && isRoomOwner -> 40
        !isCurrentUser -> 32
        user.isInSeat -> 34
        else -> 21
    }
    Box(Modifier.fillMaxSize().background(Color.Transparent)) {
        Column(
            Modifier.fillMaxWidth().height((height * 8).dp).align(Alignment.BottomCenter)
                .clip(MaterialTheme.shapes.extraLarge).background(Color.White).padding(18.dp)
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(Modifier.width(42.dp).height(4.dp).clip(CircleShape).background(Color.LightGray))
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(72.dp).clip(CircleShape).background(Color.LightGray))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(user.name, style = MaterialTheme.typography.titleLarge)
                    Text(user.flag + " " + user.country, style = MaterialTheme.typography.bodyMedium)
                    Text("ID: " + (user.accountId.ifBlank { user.id }), style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(user.bio.ifBlank { "No bio" }, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!isCurrentUser) {
                    Button(onClick = onFollow, Modifier.weight(1f)) { Text(if (user.isFollowing) "Following" else "Follow") }
                    OutlinedButton(onClick = onMessage, Modifier.weight(1f)) { Text("Message") }
                } else if (user.isInSeat) {
                    Button(onClick = onLeaveSeat, Modifier.fillMaxWidth()) { Text("Leave Seat") }
                }
            }
            if (isRoomOwner && !isCurrentUser) {
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onMute, Modifier.weight(1f)) { Text(if (user.isMuted) "Unmute" else "Mute") }
                    OutlinedButton(onClick = onKickOut, Modifier.weight(1f)) { Text("Kick Out") }
                }
            }
            TextButton(onClick = onClose, Modifier.align(Alignment.End)) { Text("Close") }
        }
    }
}
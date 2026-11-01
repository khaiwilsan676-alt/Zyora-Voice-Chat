package com.hurry.voicechat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

private val ChatNavy = Color(0xFF172536)
private val ChatBlue = Color(0xFF2878E8)
private val ChatCanvas = Color(0xFFF5F7FA)
private val ChatText = Color(0xFF182230)

data class NativeChatMessage(
    val id: String,
    val text: String,
    val sender: String,
    val timestamp: Long = System.currentTimeMillis(),
    val type: String = "message",
    val imageUrl: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    currentUser: HurryUser,
    targetUser: HurryUser,
    onClose: () -> Unit,
    onJoinRoom: ((String) -> Unit)? = null,
    sharedRoomData: String? = null
) {
    var input by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf(listOf<NativeChatMessage>()) }
    var menuExpanded by remember { mutableStateOf(false) }
    var blocked by remember { mutableStateOf(false) }
    var showReport by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var reply by remember { mutableStateOf<NativeChatMessage?>(null) }

    Column(Modifier.fillMaxSize().background(ChatCanvas)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = ChatNavy,
            shadowElevation = 4.dp
        ) {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().heightIn(min = 64.dp)
                    .padding(start = 4.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Surface(
                    modifier = Modifier.size(42.dp),
                    shape = CircleShape,
                    color = Color(0xFF344A63)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            targetUser.name.trim().firstOrNull()?.uppercase() ?: "?",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Column(
                    Modifier.weight(1f).padding(start = 11.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        targetUser.name,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Text(
                        if (blocked) "Blocked" else "Chat",
                        color = Color(0xFFB9C8D8),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More options", tint = Color.White)
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(Color.White)
                ) {
                    DropdownMenuItem(
                        text = { Text(if (blocked) "Unblock" else "Block") },
                        leadingIcon = { Icon(Icons.Default.Block, contentDescription = null) },
                        onClick = { blocked = !blocked; menuExpanded = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Report") },
                        leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null) },
                        onClick = { menuExpanded = false; showReport = true }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Clear chat") },
                        leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null) },
                        onClick = { menuExpanded = false; showClearConfirm = true }
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp),
            contentPadding = PaddingValues(top = 14.dp, bottom = 12.dp)
        ) {
            items(messages, key = { it.id }) { message ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = if (message.sender == "me") Arrangement.End else Arrangement.Start
                ) {
                    Surface(
                        color = if (message.sender == "me") Color(0xFFDCEBFF) else Color.White,
                        shape = RoundedCornerShape(
                            topStart = 18.dp, topEnd = 18.dp,
                            bottomStart = if (message.sender == "me") 18.dp else 5.dp,
                            bottomEnd = if (message.sender == "me") 5.dp else 18.dp
                        ),
                        shadowElevation = 1.dp,
                        modifier = Modifier.widthIn(max = 300.dp)
                    ) {
                        Column(Modifier.padding(horizontal = 13.dp, vertical = 9.dp)) {
                            if (message.type == "image") {
                                Text("[Image]", color = ChatText)
                            }
                            Text(message.text, color = ChatText)
                            Text(
                                SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp)),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF7C8795),
                                modifier = Modifier.align(Alignment.End).padding(top = 3.dp)
                            )
                        }
                    }
                }
            }
        }

        if (reply != null) {
            Row(
                Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Replying to ${reply!!.text}", Modifier.weight(1f), maxLines = 1, color = ChatText)
                TextButton(onClick = { reply = null }) { Text("Cancel") }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth().imePadding().navigationBarsPadding(),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                TextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f).defaultMinSize(minHeight = 50.dp),
                    placeholder = { Text("Message", color = Color(0xFF8993A0)) },
                    singleLine = true,
                    shape = RoundedCornerShape(26.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF0F2F5),
                        unfocusedContainerColor = Color(0xFFF0F2F5),
                        disabledContainerColor = Color(0xFFF0F2F5),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        cursorColor = ChatBlue,
                        focusedTextColor = ChatText,
                        unfocusedTextColor = ChatText
                    )
                )
                FilledIconButton(
                    onClick = {
                        if (input.isNotBlank() && !blocked) {
                            messages = messages + NativeChatMessage(
                                UUID.randomUUID().toString(), input.trim(), "me"
                            )
                            input = ""
                        }
                    },
                    enabled = !blocked && input.isNotBlank(),
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = ChatBlue,
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFFB8C5D4),
                        disabledContentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send message")
                }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear chat?") },
            text = { Text("This clears the messages shown in this chat on this screen.") },
            confirmButton = {
                TextButton(onClick = { messages = emptyList(); showClearConfirm = false }) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showReport) {
        AlertDialog(
            onDismissRequest = { showReport = false },
            title = { Text("Report user") },
            text = { Text("Report options are not connected to the server in this native screen yet.") },
            confirmButton = {
                TextButton(onClick = { showReport = false }) { Text("OK") }
            }
        )
    }
}

package com.hurry.voicechat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun UserReport(
    targetName: String,
    targetImage: String,
    targetId: String,
    onDismiss: () -> Unit,
    idBadgeImage: String? = null
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        var reportCategory by remember { mutableStateOf("") }
        var reportDescription by remember { mutableStateOf("") }
        var reportSubmitted by remember { mutableStateOf(false) }
        
        // Aapka purana logic aur items same hain
        val reportReasons = listOf(
            "Harassment",
            "Hate speech",
            "Violence",
            "Nudity",
            "Spam or scam",
            "Fake account",
            "Other"
        )

        // Top 3% Blue gradient fading into Grey
        val gradientBrush = Brush.verticalGradient(
            colors = listOf(Color(0xFFE0EFFF), Color(0xFFF7F8FA)),
            startY = 0f,
            endY = 300f 
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF7F8FA))
        ) {
            // Gradient Background at Top
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(gradientBrush)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
            ) {
                // Top Bar (Image jaisa same)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF202633))
                    }
                    Text(
                        "Report",
                        modifier = Modifier.weight(1f).padding(end = 48.dp), // end padding to center text
                        fontSize = 19.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF202633),
                        textAlign = TextAlign.Center
                    )
                }

                // Success Screen (Aapka purana logic un-touched)
                if (reportSubmitted) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(28.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier.size(76.dp).clip(CircleShape).background(Color(0xFFE7F8F0)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF16A36A), modifier = Modifier.size(38.dp))
                        }
                        Text(
                            "Thanks for letting us know",
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF202633),
                            modifier = Modifier.padding(top = 20.dp)
                        )
                        Text(
                            "Your report helps us keep Zyora safe for everyone.",
                            color = Color(0xFF77808E),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth().padding(top = 28.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF237BEE))
                        ) { Text("Done", color = Color.White, fontWeight = FontWeight.SemiBold) }
                    }
                } else {
                    // Form UI (Image ke design ke according set kiya hua)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 18.dp)
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))

                        // Grid layout for buttons (2 in a row)
                        reportReasons.chunked(2).forEach { rowItems ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                rowItems.forEach { reason ->
                                    val isSelected = reportCategory == reason
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) Color(0xFFEAF2FF) else Color.White)
                                            .border(
                                                width = 1.dp,
                                                color = if (isSelected) Color(0xFF237BEE) else Color.Transparent,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable { reportCategory = reason }
                                            .padding(vertical = 14.dp, horizontal = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = reason,
                                            color = if (isSelected) Color(0xFF237BEE) else Color(0xFF303846),
                                            fontSize = 14.sp,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                // Empty box if the row has only 1 item (odd count)
                                if (rowItems.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }

                        // Description label
                        Text(
                            text = "Description",
                            fontSize = 14.sp,
                            color = Color(0xFF303846),
                            modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                        )

                        // Input Field like Image
                        OutlinedTextField(
                            value = reportDescription,
                            onValueChange = { if (it.length <= 500) reportDescription = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp),
                            placeholder = { Text("Please enter", color = Color(0xFF9AA3AF), fontSize = 14.sp) },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            )
                        )
                    }

                    // Bottom Submit Button (Blue instead of Green)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 24.dp)
                            .navigationBarsPadding()
                    ) {
                        Button(
                            onClick = { if (reportCategory.isNotBlank()) reportSubmitted = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            enabled = reportCategory.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF237BEE), // Blue color
                                disabledContainerColor = Color(0xFF91B8EF) // Light blue when disabled
                            )
                        ) {
                            Text(
                                "Submit",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDangerActionMenu(
    expanded: Boolean,
    isBlocked: Boolean,
    onDismiss: () -> Unit,
    onBlockedChange: (Boolean) -> Unit,
    onReport: () -> Unit
) {
    if (!expanded) return

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.Transparent,
        contentColor = Color.White,
        scrimColor = Color.Black.copy(alpha = 0.5f),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1C1C1E))
            ) {
                androidx.compose.material3.Text(
                    "Report",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onDismiss()
                            onReport()
                        }
                        .padding(vertical = 18.dp),
                    color = Color.White,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
                Divider(color = Color.DarkGray.copy(alpha = 0.5f), thickness = 0.5.dp)
                androidx.compose.material3.Text(
                    if (isBlocked) "Unblock" else "Block",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onBlockedChange(!isBlocked)
                            onDismiss()
                        }
                        .padding(vertical = 18.dp),
                    color = Color.White,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                shape = RoundedCornerShape(16.dp)
            ) {
                androidx.compose.material3.Text(
                    "Cancel",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

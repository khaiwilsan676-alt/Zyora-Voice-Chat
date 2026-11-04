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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage

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
        val reportReasons = listOf(
            "Harassment or bullying",
            "Hate speech",
            "Violence or dangerous content",
            "Nudity or sexual content",
            "Spam or scam",
            "Fake account",
            "Other"
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF7F8FA))
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF202633))
                }
                Text(
                    "Report",
                    modifier = Modifier.weight(1f),
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF202633),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.width(48.dp))
            }

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
                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)
                ) {
                    Text(
                        "Why are you reporting this account?",
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF202633),
                        modifier = Modifier.padding(top = 22.dp)
                    )
                    Text(
                        "Choose the reason that best describes the problem. Your report is private.",
                        fontSize = 13.sp,
                        color = Color(0xFF7A8492),
                        modifier = Modifier.padding(top = 7.dp, bottom = 18.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White).padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = targetImage,
                            contentDescription = "Reported account",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(48.dp).clip(CircleShape)
                        )
                        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(
                                targetName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF202633),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!idBadgeImage.isNullOrBlank()) {
                                AsyncImage(
                                    model = idBadgeImage,
                                    contentDescription = "Purple ID Badge",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.height(20.dp).widthIn(max = 30.dp)
                                )
                                Text(targetId, fontSize = 12.sp, color = Color(0xFF8992A0), modifier = Modifier.padding(top = 3.dp))
                            } else {
                                Text("ID: $targetId", fontSize = 12.sp, color = Color(0xFF8992A0), modifier = Modifier.padding(top = 3.dp))
                            }
                        }
                        Box(
                            modifier = Modifier.size(22.dp).background(Color(0xFFF05133), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("!", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                    Text(
                        "SELECT A REASON",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF8A93A0),
                        modifier = Modifier.padding(top = 24.dp, bottom = 9.dp)
                    )
                    Column(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White)
                    ) {
                        reportReasons.forEachIndexed { index, reason ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { reportCategory = reason }.padding(horizontal = 15.dp, vertical = 15.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(reason, modifier = Modifier.weight(1f), fontSize = 14.sp, color = Color(0xFF303846))
                                Box(
                                    modifier = Modifier.size(20.dp).border(
                                        width = if (reportCategory == reason) 6.dp else 1.5.dp,
                                        color = if (reportCategory == reason) Color(0xFF237BEE) else Color(0xFFB9C1CC),
                                        shape = CircleShape
                                    )
                                )
                            }
                            if (index != reportReasons.lastIndex) {
                                HorizontalDivider(color = Color(0xFFF0F2F5), thickness = 1.dp, modifier = Modifier.padding(start = 15.dp))
                            }
                        }
                    }
                    Text(
                        "ADDITIONAL DETAILS (OPTIONAL)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF8A93A0),
                        modifier = Modifier.padding(top = 22.dp, bottom = 9.dp)
                    )
                    OutlinedTextField(
                        value = reportDescription,
                        onValueChange = { if (it.length <= 500) reportDescription = it },
                        modifier = Modifier.fillMaxWidth().height(124.dp),
                        placeholder = { Text("Tell us a little more...", color = Color(0xFF9AA3AF), fontSize = 13.sp) },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF237BEE),
                            unfocusedBorderColor = Color(0xFFE1E5EB),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )
                    Text(
                        "${reportDescription.length}/500",
                        modifier = Modifier.fillMaxWidth().padding(top = 5.dp, bottom = 20.dp),
                        textAlign = TextAlign.End,
                        fontSize = 11.sp,
                        color = Color(0xFF9AA3AF)
                    )
                }
                Column(
                    modifier = Modifier.fillMaxWidth().background(Color.White).navigationBarsPadding().padding(horizontal = 18.dp, vertical = 14.dp)
                ) {
                    Button(
                        onClick = { if (reportCategory.isNotBlank()) reportSubmitted = true },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        enabled = reportCategory.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF237BEE),
                            disabledContainerColor = Color(0xFFD5DCE6)
                        )
                    ) {
                        Text("Submit report", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Text(
                        "We review reports to help keep the community safe.",
                        modifier = Modifier.fillMaxWidth().padding(top = 9.dp),
                        textAlign = TextAlign.Center,
                        fontSize = 11.sp,
                        color = Color(0xFF929BA8)
                    )
                }
            }
        }
    }
}

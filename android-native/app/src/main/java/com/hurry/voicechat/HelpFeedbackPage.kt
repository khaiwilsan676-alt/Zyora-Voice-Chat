package com.hurry.voicechat

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val FeedbackBlue = Color(0xFF2563EB)
private val FeedbackGray = Color(0xFF6B7280)
private val FeedbackBorder = Color(0xFFE5E7EB)

@Composable
fun HelpFeedbackPage(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("hurry_feedback", Context.MODE_PRIVATE) }
    var selectedType by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var evidenceUri by remember { mutableStateOf<Uri?>(null) }
    var evidenceName by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var success by remember { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }

    val evidencePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            evidenceUri = uri
            evidenceName = uri.lastPathSegment?.substringAfterLast('/')?.take(28) ?: "Evidence selected"
            try { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
        }
    }

    Column(Modifier.fillMaxSize().background(Color(0xFFF9FAFB))) {
        // ---------- Top bar with proper back arrow ----------
        Row(
            Modifier
                .fillMaxWidth()
                .background(Color.White)
                .statusBarsPadding()
                .height(58.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable {
                        selectedType = ""
                        description = ""
                        contact = ""
                        evidenceUri = null
                        evidenceName = ""
                        error = ""
                        success = false
                        onBack()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF374151),
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                "Help & Feedback",
                Modifier.padding(start = 8.dp),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF111827)
            )
        }

        if (success) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFF0FDF4))
                    .border(1.dp, Color(0xFFBBF7D0), RoundedCornerShape(6.dp))
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("✓", fontSize = 38.sp, color = Color(0xFF16A34A))
                Text(
                    "Thank You!",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF15803D),
                    modifier = Modifier.padding(top = 12.dp)
                )
                Text(
                    "Your feedback has been submitted successfully.",
                    fontSize = 14.sp,
                    color = Color(0xFF16A34A),
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ---------- Issue type ----------
                Column {
                    Text(
                        "Type of Issue",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1F2937),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    listOf("App Bug", "Suggestion", "Recharge", "Others").chunked(2).forEach { line ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            line.forEach { type ->
                                val selected = selectedType == type
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .height(54.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (selected) Color(0xFFEFF6FF) else Color.White)
                                        .border(
                                            if (selected) 2.dp else 1.dp,
                                            if (selected) Color(0xFF3B82F6) else FeedbackBorder,
                                            RoundedCornerShape(6.dp)
                                        )
                                        .clickable { selectedType = type; error = "" }
                                        .padding(horizontal = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        type,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (selected) Color(0xFF1D4ED8) else Color(0xFF374151)
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                }

                // ---------- Description ----------
                Column {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Problem description",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1F2937)
                        )
                        Text("${description.length}/300", fontSize = 12.sp, color = Color(0xFF9CA3AF))
                    }
                    Spacer(Modifier.height(8.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(132.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFF9FAFB))
                            .border(1.dp, FeedbackBorder, RoundedCornerShape(6.dp))
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        if (description.isEmpty())
                            Text("Problem description", fontSize = 14.sp, color = Color(0xFF9CA3AF))
                        BasicTextField(
                            value = description,
                            onValueChange = { if (it.length <= 300) description = it },
                            modifier = Modifier.fillMaxSize(),
                            textStyle = TextStyle(fontSize = 14.sp, color = Color(0xFF111827)),
                            maxLines = 5
                        )
                    }
                }

                // ---------- Evidence ----------
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Image Evidence",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1F2937)
                        )
                        Text(" (Optional)", fontSize = 14.sp, color = Color(0xFF9CA3AF))
                    }
                    Spacer(Modifier.height(8.dp))
                    if (evidenceUri == null) {
                        Box(
                            Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFF3F4F6))
                                .border(1.dp, FeedbackBorder, RoundedCornerShape(6.dp))
                                .clickable { evidencePicker.launch(arrayOf("image/*", "video/*")) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("+", fontSize = 28.sp, color = Color(0xFF9CA3AF))
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFE5E7EB)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    if ((context.contentResolver.getType(evidenceUri!!) ?: "").startsWith("video/")) "▶" else "IMG",
                                    fontSize = 13.sp,
                                    color = FeedbackGray
                                )
                            }
                            Text(
                                evidenceName,
                                Modifier.weight(1f).padding(start = 10.dp),
                                fontSize = 12.sp,
                                color = FeedbackGray,
                                maxLines = 2
                            )
                            Text(
                                "×",
                                Modifier.clip(CircleShape).clickable { evidenceUri = null; evidenceName = "" }.padding(8.dp),
                                fontSize = 20.sp,
                                color = Color(0xFFEF4444)
                            )
                        }
                    }
                }

                // ---------- Contact ----------
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Contact Information",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1F2937)
                        )
                        Text(" (Optional)", fontSize = 14.sp, color = Color(0xFF9CA3AF))
                    }
                    Spacer(Modifier.height(8.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(Color(0xFFF9FAFB))
                            .border(1.dp, FeedbackBorder, RoundedCornerShape(28.dp))
                            .padding(horizontal = 20.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (contact.isEmpty())
                            Text("Phone or Email", fontSize = 14.sp, color = Color(0xFF9CA3AF))
                        BasicTextField(
                            value = contact,
                            onValueChange = { contact = it },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(fontSize = 14.sp, color = Color(0xFF111827)),
                            singleLine = true
                        )
                    }
                }

                // ---------- Error ----------
                if (error.isNotEmpty()) {
                    Text(
                        error,
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFEF2F2))
                            .border(1.dp, Color(0xFFFECACA), RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        fontSize = 12.sp,
                        color = Color(0xFFDC2626)
                    )
                }

                // ---------- Submit ----------
                Column(
                    Modifier.padding(top = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(if (submitting) Color(0xFF93C5FD) else FeedbackBlue)
                            .clickable(enabled = !submitting) {
                                when {
                                    selectedType.isBlank() -> error = "Please select a type of issue"
                                    description.isBlank() -> error = "Please describe your problem"
                                    contact.isBlank() -> error = "Please enter your contact information"
                                    else -> {
                                        submitting = true
                                        val obj = org.json.JSONObject()
                                            .put("type", selectedType)
                                            .put("description", description.trim())
                                            .put("contactInfo", contact.trim())
                                            .put("evidenceUri", evidenceUri?.toString() ?: "")
                                            .put("timestamp", System.currentTimeMillis())
                                            .put("status", "pending")
                                        val old = prefs.getString("pending_feedback", "[]") ?: "[]"
                                        val array = try { org.json.JSONArray(old) } catch (_: Exception) { org.json.JSONArray() }
                                        array.put(obj)
                                        prefs.edit().putString("pending_feedback", array.toString()).apply()
                                        submitting = false
                                        success = true
                                        selectedType = ""
                                        description = ""
                                        contact = ""
                                        evidenceUri = null
                                        evidenceName = ""
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (submitting) "Submitting..." else "Submit",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                    Text(
                        "Thank you! Feel free to give us any feedback.",
                        Modifier.padding(top = 12.dp),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1F2937)
                    )
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

package com.hurry.voicechat

import android.app.DatePickerDialog
import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Calendar

private const val NEW_USER_API = "https://zyora-server.onrender.com"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Newuserafterlogin(onBack: () -> Unit, onComplete: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("hurry_auth", Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()
    var nickname by remember { mutableStateOf(prefs.getString("name", "").orEmpty()) }
    var birthday by remember { mutableStateOf(prefs.getString("birthday", "").orEmpty()) }
    var country by remember { mutableStateOf(prefs.getString("countryName", "India").orEmpty().ifBlank { "India" }) }
    var gender by remember { mutableStateOf(prefs.getString("gender", "").orEmpty()) }
    var avatar by remember { mutableStateOf(prefs.getString("image", "").orEmpty()) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var countryMenu by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    val countries = listOf("India", "United States", "United Kingdom", "Pakistan", "Bangladesh", "Nepal", "Sri Lanka", "Indonesia", "Philippines", "Thailand", "Vietnam", "Japan", "South Korea", "Saudi Arabia", "United Arab Emirates", "Canada", "Australia", "Other")
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) { selectedUri = uri; avatar = uri.toString() }
    }

    fun chooseBirthday() {
        val now = Calendar.getInstance()
        DatePickerDialog(context, { _, year, month, day ->
            birthday = "%04d-%02d-%02d".format(year, month + 1, day)
        }, now.get(Calendar.YEAR) - 18, now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show()
    }

    Column(Modifier.fillMaxSize().background(Color(0xFFF4F6FA)).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF1F2937)) }
            Spacer(Modifier.width(8.dp))
            Text("Welcome To Zyora", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Color(0xFF202B3C))
        }
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp).padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Avatar", color = Color(0xFF263449), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Box(Modifier.size(76.dp).clip(CircleShape).background(Color(0xFFDCE5F2)).clickable { imagePicker.launch("image/*") }, contentAlignment = Alignment.Center) {
                    if (avatar.isNotBlank() && avatar != "/default-avatar.png") {
                        AsyncImage(model = selectedUri ?: avatar, contentDescription = "Choose profile photo", modifier = Modifier.fillMaxSize().clip(CircleShape))
                    } else {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Choose profile photo", tint = Color(0xFF4773AE), modifier = Modifier.size(28.dp))
                    }
                }
            }
            ProfileInputRow("Nickname") {
                BasicTextField(value = nickname, onValueChange = { nickname = it.take(30) }, singleLine = true, modifier = Modifier.fillMaxWidth(), textStyle = androidx.compose.ui.text.TextStyle(color = Color(0xFF263449), fontSize = 15.sp), decorationBox = { inner -> if (nickname.isBlank()) Text("Enter nickname", color = Color(0xFF9AA4B2), fontSize = 15.sp); inner() })
            }
            ProfileInputRow("Birthday") {
                Row(Modifier.fillMaxWidth().clickable { chooseBirthday() }, horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(birthday.ifBlank { "Choose birthday" }, color = if (birthday.isBlank()) Color(0xFF9AA4B2) else Color(0xFF263449), fontSize = 15.sp)
                    Text("›", color = Color(0xFF718096), fontSize = 24.sp)
                }
            }
            ProfileInputRow("Country") {
                Box {
                    Row(Modifier.fillMaxWidth().clickable { countryMenu = true }, horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(country, color = Color(0xFF263449), fontSize = 15.sp)
                        Text("⌄", color = Color(0xFF718096), fontSize = 20.sp)
                    }
                    DropdownMenu(expanded = countryMenu, onDismissRequest = { countryMenu = false }, modifier = Modifier.heightIn(max = 320.dp).background(Color.White)) {
                        countries.forEach { item -> DropdownMenuItem(text = { Text(item) }, onClick = { country = item; countryMenu = false }) }
                    }
                }
            }
            ProfileInputRow("Gender") {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf("Male", "Female").forEach { item ->
                        val chosen = gender == item
                        Surface(modifier = Modifier.weight(1f).height(44.dp).clickable { gender = item }, shape = RoundedCornerShape(10.dp), color = if (chosen) Color(0xFFE3EEFF) else Color(0xFFF5F7FA), border = androidx.compose.foundation.BorderStroke(1.dp, if (chosen) Color(0xFF4C83D9) else Color(0xFFE2E7EF)) {
                            Box(contentAlignment = Alignment.Center) { Text(item, color = if (chosen) Color(0xFF2865B8) else Color(0xFF4B5563), fontWeight = if (chosen) FontWeight.SemiBold else FontWeight.Normal) }
                        }
                    }
                }
            }
            if (error.isNotBlank()) Text(error, color = Color(0xFFDC2626), fontSize = 13.sp)
            Spacer(Modifier.weight(1f))
            Button(onClick = {
                if (nickname.isBlank()) { error = "Please enter your nickname"; return@Button }
                if (birthday.isBlank()) { error = "Please choose your birthday"; return@Button }
                if (gender.isBlank()) { error = "Please choose Male or Female"; return@Button }
                scope.launch {
                    saving = true; error = ""
                    try {
                        var finalAvatar = avatar
                        val uid = prefs.getString("uid", "").orEmpty()
                        if (selectedUri != null) {
                            val dataUri = withContext(Dispatchers.IO) {
                                val bytes = context.contentResolver.openInputStream(selectedUri!!)?.use { it.readBytes() } ?: throw IllegalStateException("Could not read selected photo")
                                "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
                            }
                            finalAvatar = ProfilePersistence.uploadDpToGitHub(dataUri, prefs.getString("accountId", uid).orEmpty()) ?: dataUri
                        }
                        val countryInfo = mapOf(
                            "India" to ("🇮🇳" to "IN"), "United States" to ("🇺🇸" to "US"),
                            "United Kingdom" to ("🇬🇧" to "GB"), "Pakistan" to ("🇵🇰" to "PK"),
                            "Bangladesh" to ("🇧🇩" to "BD"), "Nepal" to ("🇳🇵" to "NP"),
                            "Sri Lanka" to ("🇱🇰" to "LK"), "Indonesia" to ("🇮🇩" to "ID"),
                            "Philippines" to ("🇵🇭" to "PH"), "Thailand" to ("🇹🇭" to "TH"),
                            "Vietnam" to ("🇻🇳" to "VN"), "Japan" to ("🇯🇵" to "JP"),
                            "South Korea" to ("🇰🇷" to "KR"), "Saudi Arabia" to ("🇸🇦" to "SA"),
                            "United Arab Emirates" to ("🇦🇪" to "AE"), "Canada" to ("🇨🇦" to "CA"),
                            "Australia" to ("🇦🇺" to "AU"), "Other" to ("🌐" to "OTHER")
                        )
                        val flagAndCode = countryInfo[country] ?: ("🌐" to "OTHER")
                        val birthDate = java.time.LocalDate.parse(birthday)
                        val age = java.time.Period.between(birthDate, java.time.LocalDate.now()).years.coerceAtLeast(0)
                        val payload = JSONObject()
                            .put("id", uid).put("uid", uid).put("appLongId", uid)
                            .put("accountId", prefs.getString("accountId", uid).orEmpty())
                            .put("accountNumber", prefs.getString("accountId", uid).orEmpty())
                            .put("name", nickname.trim()).put("displayName", nickname.trim())
                            .put("email", prefs.getString("email", "").orEmpty())
                            .put("image", finalAvatar).put("avatar", finalAvatar).put("photo", finalAvatar)
                            .put("country", flagAndCode.first).put("countryName", country).put("countryCode", flagAndCode.second)
                            .put("birthday", birthday).put("age", age).put("gender", gender)
                        withContext(Dispatchers.IO) {
                            val conn = (URL("$NEW_USER_API/api/users").openConnection() as HttpURLConnection).apply {
                                requestMethod = "PUT"; connectTimeout = 15000; readTimeout = 15000; doOutput = true
                                setRequestProperty("Content-Type", "application/json")
                            }
                            try {
                                conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
                                val code = conn.responseCode
                                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                                val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                                if (code !in 200..299) throw IllegalStateException("Save failed (HTTP $code): $response")
                            } finally { conn.disconnect() }
                        }
                        prefs.edit().putString("name", nickname.trim()).putString("image", finalAvatar).putString("photo", finalAvatar).putString("country", flagAndCode.first).putString("countryName", country).putString("birthday", birthday).putString("gender", gender).putInt("age", age).putBoolean("needsProfileSetup", false).putBoolean("profileSetupComplete", true).apply()
                        onComplete()
                    } catch (t: Throwable) { error = t.message ?: "Could not save profile. Please try again." }
                    finally { saving = false }
                }
            }, enabled = !saving, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3979D6))) {
                if (saving) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp) else Text("Continue", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ProfileInputRow(label: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White).padding(horizontal = 15.dp, vertical = 13.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(label, color = Color(0xFF7B8798), fontSize = 13.sp, fontWeight = FontWeight.Medium)
        content()
    }
}

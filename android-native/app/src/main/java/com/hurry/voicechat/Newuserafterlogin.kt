package com.hurry.voicechat

import android.app.DatePickerDialog
import android.content.Context
import android.net.Uri
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
import java.util.Calendar

private const val NEW_USER_API = "https://zyora-server.onrender.com"

data class CountryData(val name: String, val flag: String, val dialCode: String, val code: String)

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
    
    val countries = listOf(
        CountryData("India", "🇮🇳", "+91", "IN"),
        CountryData("Singapore", "🇸🇬", "+65", "SG"),
        CountryData("United States", "🇺🇸", "+1", "US"),
        CountryData("United Kingdom", "🇬🇧", "+44", "GB"),
        CountryData("Pakistan", "🇵🇰", "+92", "PK"),
        CountryData("Bangladesh", "🇧🇩", "+880", "BD"),
        CountryData("Nepal", "🇳🇵", "+977", "NP"),
        CountryData("Sri Lanka", "🇱🇰", "+94", "LK"),
        CountryData("Indonesia", "🇮🇩", "+62", "ID"),
        CountryData("Philippines", "🇵🇭", "+63", "PH"),
        CountryData("Thailand", "🇹🇭", "+66", "TH"),
        CountryData("Vietnam", "🇻🇳", "+84", "VN"),
        CountryData("Malaysia", "🇲🇾", "+60", "MY"),
        CountryData("Japan", "🇯🇵", "+81", "JP"),
        CountryData("South Korea", "🇰🇷", "+82", "KR"),
        CountryData("Saudi Arabia", "🇸🇦", "+966", "SA"),
        CountryData("United Arab Emirates", "🇦🇪", "+971", "AE"),
        CountryData("Canada", "🇨🇦", "+1", "CA"),
        CountryData("Australia", "🇦🇺", "+61", "AU"),
        CountryData("Germany", "🇩🇪", "+49", "DE"),
        CountryData("France", "🇫🇷", "+33", "FR"),
        CountryData("Other", "🌐", "+00", "OTHER")
    )
    
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) { selectedUri = uri; avatar = uri.toString() }
    }

    fun chooseBirthday() {
        val now = Calendar.getInstance()
        DatePickerDialog(context, { _, year, month, day ->
            birthday = "%04d-%02d-%02d".format(year, month + 1, day)
        }, now.get(Calendar.YEAR) - 18, now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show()
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top Bar
        Row(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { 
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Black) 
            }
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp), 
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Title and Subtitle
            Column {
                Text("Hi, welcome to Zyora", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Improve information to match accurate friends~", fontSize = 14.sp, color = Color(0xFF555555))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Avatar Section
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Profile photo", color = Color(0xFFBBBBBB), fontSize = 15.sp)
                Box(
                    Modifier
                        .size(64.dp)
                        .clickable { imagePicker.launch("image/*") }
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEEEEEE)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (avatar.isNotBlank() && avatar != "/default-avatar.png") {
                            AsyncImage(
                                model = selectedUri ?: avatar,
                                contentDescription = "Profile photo",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        } else {
                            // Agar DP nahi hai toh App Logo Colorless mode me show karega
                            androidx.compose.foundation.Image(
                                painter = painterResource(id = R.drawable.logo),
                                contentDescription = "Default Logo",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0088FF)), // Camera button blue color
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = android.R.drawable.ic_menu_camera),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            // Nickname Section
            Column {
                Text("Nickname", color = Color(0xFFBBBBBB), fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                BasicTextField(
                    value = nickname,
                    onValueChange = { nickname = it.take(30) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Medium),
                    decorationBox = { inner -> 
                        Box(Modifier.fillMaxWidth()) {
                            if (nickname.isBlank()) Text("", color = Color.Transparent)
                            inner()
                        }
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Divider(color = if (nickname.isBlank()) Color(0xFFFF69B4) else Color(0xFFEEEEEE), thickness = 1.dp)
                if (nickname.isBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Please enter a nickname", color = Color(0xFFFF69B4), fontSize = 12.sp)
                }
            }

            // Birthday Section
            Column {
                Text("Birthday", color = Color(0xFFBBBBBB), fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { chooseBirthday() },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = birthday.ifBlank { "YYYY-MM-DD" },
                        color = if (birthday.isBlank()) Color(0xFFBBBBBB) else Color.Black,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Divider(color = Color(0xFFEEEEEE), thickness = 1.dp)
            }

            // Country Section 
            Column {
                Text("Country", color = Color(0xFFBBBBBB), fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                
                ExposedDropdownMenuBox(
                    expanded = countryMenu,
                    onExpandedChange = { countryMenu = it }
                ) {
                    val currentCountry = countries.find { it.name == country } ?: countries.first()
                    
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                            .clickable { countryMenu = true },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(currentCountry.flag, fontSize = 20.sp)
                            Spacer(Modifier.width(8.dp))
                            Text(currentCountry.name, color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }
                        Text(currentCountry.dialCode, color = Color(0xFFBBBBBB), fontSize = 14.sp)
                    }
                    
                    ExposedDropdownMenu(
                        expanded = countryMenu,
                        onDismissRequest = { countryMenu = false },
                        modifier = Modifier
                            .background(Color.White)
                            .heightIn(max = 350.dp)
                    ) {
                        countries.forEach { item ->
                            DropdownMenuItem(
                                text = { 
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(text = item.flag, fontSize = 20.sp)
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(text = item.name, color = Color.Black, fontSize = 15.sp)
                                        }
                                        Text(text = item.dialCode, color = Color.Gray, fontSize = 13.sp)
                                    }
                                },
                                onClick = { 
                                    country = item.name 
                                    countryMenu = false 
                                },
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Divider(color = Color(0xFFEEEEEE), thickness = 1.dp)
            }

            // Gender Section
            Column {
                Text("Gender", color = Color(0xFFBBBBBB), fontSize = 14.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val isMale = gender == "Male"
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clickable { 
                                gender = "Male"
                                // Agar nickname khali hai ya kachra hai toh default Male name set karega
                                if (nickname.isBlank() || nickname == "jsjsjs" || nickname == "null") {
                                    nickname = listOf("Simpson", "Edward", "Liho").random()
                                }
                            },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isMale) Color(0xFF4AC4FA) else Color(0xFFE0E0E0)
                    ) {
                        Row(
                            Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text("♂ Male", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            // Image hata di gayi hai
                        }
                    }

                    val isFemale = gender == "Female"
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clickable { 
                                gender = "Female"
                                // Agar nickname khali hai ya kachra hai toh default Female name set karega
                                if (nickname.isBlank() || nickname == "jsjsjs" || nickname == "null") {
                                    nickname = listOf("Eman", "Barbie", "Ali").random()
                                }
                            },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isFemale) Color(0xFFFF85C2) else Color(0xFFE0E0E0)
                    ) {
                        Row(
                            Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text("♀ Female", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            // Image hata di gayi hai
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("*Gender will not be changed after you select", color = Color(0xFFFF69B4), fontSize = 12.sp)
            }

            if (error.isNotBlank()) {
                Text(error, color = Color(0xFFDC2626), fontSize = 13.sp)
            }

            Spacer(Modifier.weight(1f))

            // Submit Button
            Button(
                onClick = {
                    if (nickname.isBlank()) { error = "Please enter your nickname"; return@Button }
                    if (birthday.isBlank()) { error = "Please choose your birthday"; return@Button }
                    if (gender.isBlank()) { error = "Please choose Male or Female"; return@Button }
                    scope.launch {
                        saving = true; error = ""
                        try {
                            var finalAvatar = avatar
                            val uid = prefs.getString("uid", "").orEmpty().ifBlank { prefs.getString("accountId", "").orEmpty() }
                            if (selectedUri != null) {
                                finalAvatar = withContext(Dispatchers.IO) {
                                    ProfilePersistence.imageDataUri(context, selectedUri!!)
                                        ?: throw IllegalStateException("Could not process the selected photo. Please choose another image.")
                                }
                            }
                            
                            val selectedCountryData = countries.find { it.name == country } ?: countries.last()
                            
                            val birthParts = birthday.split("-").map { it.toInt() }
                            val birthCalendar = Calendar.getInstance().apply {
                                set(birthParts[0], birthParts[1] - 1, birthParts[2], 0, 0, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            val todayCalendar = Calendar.getInstance()
                            var age = todayCalendar.get(Calendar.YEAR) - birthCalendar.get(Calendar.YEAR)
                            if (todayCalendar.get(Calendar.MONTH) < birthCalendar.get(Calendar.MONTH) ||
                                (todayCalendar.get(Calendar.MONTH) == birthCalendar.get(Calendar.MONTH) &&
                                    todayCalendar.get(Calendar.DAY_OF_MONTH) < birthCalendar.get(Calendar.DAY_OF_MONTH))) age--
                            age = age.coerceAtLeast(0)
                            
                            val payload = JSONObject()
                                .put("id", uid).put("uid", uid).put("appLongId", uid)
                                .put("accountId", prefs.getString("accountId", uid).orEmpty())
                                .put("accountNumber", prefs.getString("accountId", uid).orEmpty())
                                .put("name", nickname.trim()).put("displayName", nickname.trim())
                                .put("email", prefs.getString("email", "").orEmpty())
                                .put("image", finalAvatar).put("avatar", finalAvatar).put("photo", finalAvatar)
                                .put("country", selectedCountryData.flag).put("countryName", selectedCountryData.name).put("countryCode", selectedCountryData.code)
                                .put("birthday", birthday).put("age", age).put("gender", gender)
                            
                            withContext(Dispatchers.IO) {
                                val conn = (URL("$NEW_USER_API/api/users").openConnection() as HttpURLConnection).apply {
                                    requestMethod = "PUT"; connectTimeout = 30000; readTimeout = 45000; doOutput = true
                                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                                }
                                try {
                                    conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
                                    val code = conn.responseCode
                                    val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                                    val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                                    if (code !in 200..299) {
                                        val serverMessage = runCatching { JSONObject(response).optString("error") }.getOrDefault("")
                                        val safeMessage = serverMessage.takeIf { it.isNotBlank() }
                                            ?: if (response.trimStart().startsWith("<")) "Zyora server returned an HTML error (HTTP $code). Please try again in a moment."
                                            else "Could not save your profile (HTTP $code). Please try again."
                                        throw IllegalStateException(safeMessage)
                                    }
                                    val saveResult = runCatching { JSONObject(response) }.getOrNull()
                                    if (saveResult?.optBoolean("success", false) != true || saveResult.optJSONObject("user") == null) {
                                        throw IllegalStateException("Server did not confirm that your profile was saved to MongoDB. Please retry.")
                                    }
                                } finally { conn.disconnect() }
                            }
                            prefs.edit()
                                .putString("name", nickname.trim())
                                .putString("image", finalAvatar)
                                .putString("photo", finalAvatar)
                                .putString("country", selectedCountryData.flag)
                                .putString("countryName", selectedCountryData.name)
                                .putString("birthday", birthday)
                                .putString("gender", gender)
                                .putInt("age", age)
                                .putBoolean("needsProfileSetup", false)
                                .putBoolean("profileSetupComplete", true)
                                .apply()
                            onComplete()
                        } catch (t: Throwable) { error = t.message ?: "Could not save profile. Please try again." }
                        finally { saving = false }
                    }
                },
                enabled = !saving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(horizontal = 32.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0088FF)) // Submit button blue color
            ) {
                if (saving) {
                    CircularProgressIndicator(Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Submit", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 16.sp) // White text on blue background
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

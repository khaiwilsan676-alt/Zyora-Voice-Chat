package com.hurry.voicechat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext

private data class LanguageOption(val code: String, val label: String, val native: String)

private val languageOptions = listOf(
    LanguageOption("en", "English", "English"),
    LanguageOption("hi", "Hindi", "हिन्दी"),
    LanguageOption("bn", "Bengali", "বাংলা"),
    LanguageOption("ar", "Arabic", "العربية"),
    LanguageOption("ur", "Urdu", "اردو"),
    LanguageOption("tr", "Turkish", "Türkçe"),
    LanguageOption("es", "Spanish", "Español"),
    LanguageOption("fr", "French", "Français"),
    LanguageOption("de", "German", "Deutsch"),
    LanguageOption("pt", "Portuguese", "Português"),
    LanguageOption("ru", "Russian", "Русский"),
    LanguageOption("id", "Indonesian", "Bahasa Indonesia"),
    LanguageOption("ja", "Japanese", "日本語"),
    LanguageOption("ko", "Korean", "한국어"),
    LanguageOption("zh", "Chinese", "中文")
)

@Composable
fun LanguagePage(
    onBack: () -> Unit = {},
    selectedLanguage: String = "English",
    onLanguageSelected: (String) -> Unit = {}
) {
    val initial = languageOptions.firstOrNull {
        it.code.equals(selectedLanguage, ignoreCase = true) ||
            it.label.equals(selectedLanguage, ignoreCase = true) ||
            it.native == selectedLanguage
    }?.code ?: "en"
    val context = LocalContext.current
    val languagePrefs = remember { context.getSharedPreferences("zyora_settings", android.content.Context.MODE_PRIVATE) }
    var pendingCode by remember { mutableStateOf(languagePrefs.getString("appLanguage", initial) ?: initial) }

    Column(Modifier.fillMaxSize().background(Color.White)) {
        Row(
            Modifier.fillMaxWidth()
                .statusBarsPadding()
                .background(Color.White)
                .border(width = 0.5.dp, color = Color(0xFFF3F4F6))
                .padding(horizontal = 12.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(40.dp).clickable { onBack() }.padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Canvas(Modifier.size(24.dp)) {
                    val stroke = 2.2.dp.toPx()
                    drawLine(Color(0xFF111827), androidx.compose.ui.geometry.Offset(size.width * 0.88f, size.height / 2), androidx.compose.ui.geometry.Offset(size.width * 0.15f, size.height / 2), strokeWidth = stroke, cap = StrokeCap.Round)
                    drawLine(Color(0xFF111827), androidx.compose.ui.geometry.Offset(size.width * 0.15f, size.height / 2), androidx.compose.ui.geometry.Offset(size.width * 0.48f, size.height * 0.18f), strokeWidth = stroke, cap = StrokeCap.Round)
                    drawLine(Color(0xFF111827), androidx.compose.ui.geometry.Offset(size.width * 0.15f, size.height / 2), androidx.compose.ui.geometry.Offset(size.width * 0.48f, size.height * 0.82f), strokeWidth = stroke, cap = StrokeCap.Round)
                }
            }
            Text(
                nativeText(initial, "language"),
                modifier = Modifier.weight(1f),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111827),
                textAlign = TextAlign.Center
            )
            Box(
                Modifier.defaultMinSize(minWidth = 56.dp, minHeight = 36.dp)
                    .background(Color.Transparent, RoundedCornerShape(8.dp))
                    .clickable {
                        languagePrefs.edit().putString("appLanguage", pendingCode).apply()
                        onLanguageSelected(pendingCode)
                        onBack()
                    }
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(nativeText(initial, "save"), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2563EB))
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
        ) {
            items(languageOptions, key = { it.code }) { language ->
                Row(
                    Modifier.fillMaxWidth()
                        .clickable { pendingCode = language.code }
                        .padding(horizontal = 8.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(language.native, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF111827))
                        Spacer(Modifier.height(2.dp))
                        Text(language.label, fontSize = 12.sp, color = Color(0xFF6B7280))
                    }
                    Box(
                        Modifier.size(24.dp).border(
                            width = 2.dp,
                            color = Color(0xFFD1D5DB),
                            shape = CircleShape
                        ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (pendingCode == language.code) {
                            Box(Modifier.size(12.dp).background(Color(0xFF3B82F6), CircleShape))
                        }
                    }
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFF3F4F6)))
            }
        }
    }
}

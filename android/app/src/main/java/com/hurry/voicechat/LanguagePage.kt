package com.hurry.voicechat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun LanguagePage(onBack:()->Unit={}, selectedLanguage:String="English", onLanguageSelected:(String)->Unit={}) {
    var selected by remember { mutableStateOf(selectedLanguage) }
    val languages=listOf("English","Hindi","Punjabi","Bengali","Tamil","Telugu")
    Column(Modifier.fillMaxSize().background(Color.White)) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().height(58.dp).padding(horizontal=8.dp),verticalAlignment=Alignment.CenterVertically) {
            Text("‹",style=MaterialTheme.typography.headlineLarge,modifier=Modifier.clickable(onClick=onBack).padding(horizontal=10.dp))
            Text("Language",style=MaterialTheme.typography.titleLarge)
        }
        LazyColumn(Modifier.fillMaxSize()) { items(languages.size) { i ->
            val language=languages[i]
            Row(Modifier.fillMaxWidth().clickable{selected=language;onLanguageSelected(language)}.padding(horizontal=20.dp,vertical=17.dp),verticalAlignment=Alignment.CenterVertically) {
                Text(language,Modifier.weight(1f)); if(selected==language) Text("✓",color=Color(0xFF2979FF))
            }
        }}
    }
}

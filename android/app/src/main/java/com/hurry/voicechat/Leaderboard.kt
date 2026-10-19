package com.hurry.voicechat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun Leaderboard(onBack:()->Unit={}, initialTab:String="honour") {
    var tab by remember { mutableStateOf(initialTab) }; var period by remember { mutableStateOf("daily") }
    val tabs=listOf("honour" to "Honour","charm" to "Charm","room" to "Room")
    val periods=listOf("daily" to "Daily","weekly" to "Weekly","monthly" to "Monthly")
    Box(Modifier.fillMaxSize().background(Color(0xFF1A0204))) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().height(56.dp),verticalAlignment=Alignment.CenterVertically) {
                Text("‹",color=Color.White,style=MaterialTheme.typography.headlineLarge,modifier=Modifier.clickable(onClick=onBack).padding(12.dp))
                Row(Modifier.weight(1f).height(42.dp).background(Color(0xFF110A07),RoundedCornerShape(24.dp)).padding(2.dp)) {
                    tabs.forEach { (id,label)-> Box(Modifier.weight(1f).fillMaxHeight().background(if(tab==id) Color(0xFFB8782B) else Color.Transparent,RoundedCornerShape(22.dp)).clickable{tab=id},contentAlignment=Alignment.Center){Text(label,color=Color.White)} }
                }; Spacer(Modifier.width(12.dp))
            }
            Row(Modifier.padding(start=14.dp,top=6.dp,bottom=8.dp)) { periods.forEach { (id,label)-> Text(label,color=if(period==id) Color.White else Color.White.copy(.5f),modifier=Modifier.clickable{period=id}.padding(horizontal=10.dp,vertical=8.dp)) } }
            LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                items(50) { i -> Row(Modifier.fillMaxWidth().background(Color(0xFF240A0D),RoundedCornerShape(10.dp)).padding(12.dp),verticalAlignment=Alignment.CenterVertically) {
                    Text("#"+(i+4),color=Color.White,modifier=Modifier.width(45.dp)); Box(Modifier.size(44.dp).background(Color(0xFF402126),RoundedCornerShape(22.dp))); Spacer(Modifier.width(10.dp)); Text("Hurry User "+(i+4),color=Color.White,modifier=Modifier.weight(1f)); Text(""+(50-i).coerceAtLeast(1),color=Color(0xFFFFD56A))
                }}
            }
        }
    }
}

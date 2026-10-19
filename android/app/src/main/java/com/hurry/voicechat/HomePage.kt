package com.hurry.voicechat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage

@Composable
fun HomePage(rooms: List<HurryRoom> = emptyList(), onOpenRoom: (HurryRoom)->Unit = {}, onOpenMessage:()->Unit = {}, onOpenMe:()->Unit = {}, onSearch:()->Unit = {}) {
    var tab by remember { mutableStateOf("Popular") }
    val visible = rooms
    Scaffold(containerColor=Color(0xFFF7F9FC), bottomBar={
        NavigationBar(containerColor=Color.White, tonalElevation=0.dp) {
            NavigationBarItem(true, {}, icon={Text("⌂")}, label={Text("Home")})
            NavigationBarItem(false, onOpenMessage, icon={Text("◌")}, label={Text("Message")})
            NavigationBarItem(false, onOpenMe, icon={Text("●")}, label={Text("Me")})
        }
    }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            Row(Modifier.fillMaxWidth().background(Color.White).padding(top=10.dp,start=14.dp,end=14.dp,bottom=8.dp), verticalAlignment=Alignment.CenterVertically) {
                Text("Zaho", style=MaterialTheme.typography.titleLarge, modifier=Modifier.weight(1f))
                Text("⌕", style=MaterialTheme.typography.headlineSmall, modifier=Modifier.clickable(onClick=onSearch).padding(8.dp))
            }
            Row(Modifier.fillMaxWidth().background(Color.White).padding(horizontal=14.dp,vertical=6.dp), horizontalArrangement=Arrangement.spacedBy(22.dp)) {
                listOf("Popular","Mine").forEach { name ->
                    Text(name, color=if(tab==name) Color.Black else Color(0xFF8A8F98), style=MaterialTheme.typography.titleMedium, modifier=Modifier.clickable{tab=name}.padding(vertical=7.dp))
                }
            }
            LazyColumn(Modifier.fillMaxSize(), contentPadding=PaddingValues(12.dp), verticalArrangement=Arrangement.spacedBy(10.dp)) {
                item { Box(Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFFEAF0F7)), contentAlignment=Alignment.Center) { Text("Banner") } }
                items(visible) { room -> Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White).clickable{onOpenRoom(room)}.padding(10.dp), verticalAlignment=Alignment.CenterVertically) {
                    AsyncImage(model=room.image, contentDescription=room.name, modifier=Modifier.size(72.dp).clip(RoundedCornerShape(10.dp)), contentScale=ContentScale.Crop)
                    Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(room.name, maxLines=1); Text(room.country, color=Color.Gray); if(room.announcement.isNotBlank()) Text(room.announcement,color=Color.Gray,maxLines=1) }
                    if(room.locked) Text("🔒")
                }}
            }
        }
    }
}

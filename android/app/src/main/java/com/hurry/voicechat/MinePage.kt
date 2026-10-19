package com.hurry.voicechat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

data class MineRoom(val accountId:String,val name:String,val country:String="🇮🇳",val image:String,val isLocked:Boolean=false)

@Composable
fun MinePage(isRoomCreated:Boolean,myRoom:MineRoom?=null,userPhoto:String="",userName:String="",followingRooms:List<MineRoom>=emptyList(),recentRooms:List<MineRoom>=emptyList(),onCardClick:()->Unit={},onUserCardClick:(MineRoom)->Unit={}) {
 var tab by remember{mutableStateOf(0)}
 Column(Modifier.fillMaxWidth().padding(horizontal=12.dp).offset(y=(-8).dp)){
  Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(Brush.linearGradient(listOf(Color(0xFF667EEA),Color(0xFF764BA2)))).clickable{onCardClick()}.padding(24.dp),horizontalArrangement=Arrangement.spacedBy(16.dp)){
   AsyncImage(model=if(isRoomCreated)myRoom?.image?:userPhoto else userPhoto,contentDescription=null,modifier=Modifier.size(56.dp))
   Column{Text(if(isRoomCreated)myRoom?.name?:"Room" else "Embark Your Hurry Journey!",color=Color.White,style=MaterialTheme.typography.titleLarge);Text(if(isRoomCreated)"Tap to enter your room" else "Tap to create your room",color=Color.White.copy(.8f))}
  }
  Row(horizontalArrangement=Arrangement.spacedBy(14.dp),modifier=Modifier.padding(vertical=8.dp)){Text("Following",color=if(tab==0)Color.Black else Color.Gray,modifier=Modifier.clickable{tab=0});Text("Recent",color=if(tab==1)Color.Black else Color.Gray,modifier=Modifier.clickable{tab=1})}
  val rooms=if(tab==0)followingRooms else recentRooms
  if(rooms.isEmpty())Box(Modifier.fillMaxWidth().padding(48.dp)){Text("No data",color=Color.Gray)}
  else LazyVerticalGrid(columns=GridCells.Fixed(2),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){items(rooms,key={it.accountId}){r->Box(Modifier.height(180.dp).clip(RoundedCornerShape(16.dp)).clickable{onUserCardClick(r)}){AsyncImage(model=r.image,contentDescription=r.name,modifier=Modifier.fillMaxSize());Text(r.name,color=Color.White,modifier=Modifier.align(Alignment.BottomStart).fillMaxWidth().background(Color.Black.copy(.45f)).padding(10.dp))}}}
 }
}
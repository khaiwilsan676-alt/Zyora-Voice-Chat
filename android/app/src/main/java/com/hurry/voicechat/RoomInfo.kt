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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage

data class RoomInfoOwner(val id:String="",val uid:String="",val accountId:String="",val name:String,val image:String)
data class RoomInfoData(val roomName:String,val roomDp:String,val roomAnnouncement:String,val roomId:String)
data class RoomInfoFollower(val accountId:String,val name:String,val image:String)

@Composable
fun RoomInfo(isOpen:Boolean,onClose:()->Unit,isRoomOwner:Boolean,roomOwner:RoomInfoOwner,roomData:RoomInfoData,roomFollowers:List<RoomInfoFollower>,onOpenProfile:(RoomInfoFollower)->Unit,onCopyId:(String)->Unit,copied:Boolean=false){
 if(!isOpen)return
 Box(Modifier.fillMaxSize(),contentAlignment=Alignment.BottomCenter){
  Box(Modifier.fillMaxSize().background(Color.Black.copy(.30f)).clickable(onClick=onClose))
  var tab by remember{mutableStateOf(0)}
  Column(Modifier.fillMaxWidth().fillMaxHeight(.50f).clip(RoundedCornerShape(topStart=24.dp,topEnd=24.dp)).background(Color.White)){
   Text("Room Information",Modifier.fillMaxWidth().padding(24.dp),18.sp,color=Color(0xFF222222))
   TabRow(selectedTabIndex=tab){Tab(tab==0,{tab=0},{Text("Profile")});Tab(tab==1,{tab=1},{Text("Members")})}
   if(tab==0)Column(Modifier.fillMaxWidth().padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
    Row(verticalAlignment=Alignment.CenterVertically){
     AsyncImage(roomData.roomDp,"Room",Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)),contentScale=ContentScale.Crop)
     Column(Modifier.padding(start=12.dp)){Text(if(roomData.roomName.isBlank())"Room" else roomData.roomName,style=MaterialTheme.typography.titleMedium);Row(verticalAlignment=Alignment.CenterVertically){Text("ID: "+roomOwner.accountId,fontSize=12.sp,color=Color.Gray);Text(" ⧉",Modifier.clickable{onCopyId(roomOwner.accountId)},fontSize=14.sp,color=Color.Gray);if(copied)Text(" Copied!",fontSize=11.sp,color=Color.Green)}}}
    Text("Host",fontSize=12.sp,color=Color.Gray);Text(if(roomOwner.name.isBlank())"Unknown" else roomOwner.name)
    Text("Announcement:",fontSize=12.sp,color=Color.Gray);Text(if(roomData.roomAnnouncement.isBlank())"—" else roomData.roomAnnouncement,fontSize=14.sp)
   }else LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
    item{MemberRow(roomOwner.name,roomOwner.image,roomOwner.accountId,true){onOpenProfile(RoomInfoFollower(roomOwner.accountId,roomOwner.name,roomOwner.image))}}
    items(roomFollowers){f->MemberRow(f.name,f.image,f.accountId,false){onOpenProfile(f)}}
    if(roomFollowers.isEmpty())item{Text("No followers yet",Modifier.fillMaxWidth().padding(24.dp),textAlign=androidx.compose.ui.text.style.TextAlign.Center,color=Color.Gray)}
   }
  }
 }
}
@Composable private fun MemberRow(name:String,image:String,id:String,owner:Boolean,onClick:()->Unit){
 Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0xFFF7F7F7)).clickable(onClick=onClick).padding(12.dp),verticalAlignment=Alignment.CenterVertically){
  AsyncImage(if(image.isBlank())"/default-avatar.png" else image,name,Modifier.size(36.dp).clip(androidx.compose.foundation.shape.CircleShape),contentScale=ContentScale.Crop)
  Text(name,Modifier.weight(1f).padding(start=12.dp),fontSize=14.sp)
  if(owner)Text("HOST",fontSize=10.sp,color=Color(0xFF3B82F6))
 }
}

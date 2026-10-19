package com.hurry.voicechat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage

data class ActiveUserNative(val accountId:String,val name:String,val image:String)

@Composable
fun ActiveUsers(
 isOpen:Boolean,
 onClose:()->Unit,
 roomUsers:List<ActiveUserNative>,
 currentUser:ActiveUserNative,
 roomOwnerId:String,
 onOpenProfile:(ActiveUserNative)->Unit,
 onCopyUserId:(String)->Unit
){
 if(!isOpen)return
 Box(Modifier.fillMaxSize(),contentAlignment=Alignment.BottomCenter){
  Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.30f)).clickable(onClick=onClose))
  Column(
   Modifier.fillMaxWidth().fillMaxHeight(.40f)
    .clip(RoundedCornerShape(topStart=15.dp,topEnd=15.dp))
    .background(Color(0xEB000000))
  ){
   Text("Active Users",modifier=Modifier.fillMaxWidth().padding(start=16.dp,end=16.dp,top=16.dp,bottom=8.dp),textAlign=TextAlign.Center,fontSize=16.sp,fontWeight=FontWeight.Bold,color=Color.White)
   LazyColumn(
    Modifier.weight(1f).fillMaxWidth(),
    contentPadding=PaddingValues(start=12.dp,end=12.dp,top=8.dp,bottom=16.dp),
    verticalArrangement=Arrangement.spacedBy(12.dp)
   ){
    item(key="__current_user__"){
     Row(Modifier.fillMaxWidth().padding(horizontal=4.dp),verticalAlignment=Alignment.CenterVertically){
      AsyncImage(model=currentUser.image.ifBlank{"/default-avatar.png"},contentDescription=currentUser.name,modifier=Modifier.size(32.dp).clip(CircleShape).clickable{onOpenProfile(currentUser)},contentScale=ContentScale.Crop)
      Row(Modifier.weight(1f).padding(start=8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)){
       Text(currentUser.name.ifBlank{"Owner"},fontSize=12.sp,fontWeight=FontWeight.SemiBold,color=Color.White,maxLines=1)
       androidx.compose.material3.Icon(
        imageVector=Icons.Filled.Home,
        contentDescription="Room owner",
        modifier=Modifier.size(14.dp),
        tint=Color.White
       )
      }
     }
    }
    items(roomUsers.filter{it.accountId != roomOwnerId && it.accountId != currentUser.accountId},key={it.accountId}){user->
     Row(Modifier.fillMaxWidth().padding(horizontal=4.dp),verticalAlignment=Alignment.CenterVertically){
      AsyncImage(model=user.image.ifBlank{"/default-avatar.png"},contentDescription=user.name,modifier=Modifier.size(32.dp).clip(CircleShape).clickable{onOpenProfile(user)},contentScale=ContentScale.Crop)
      Column(Modifier.weight(1f).padding(start=8.dp)){
       Text(user.name,fontSize=12.sp,fontWeight=FontWeight.SemiBold,color=Color.White,maxLines=1)
       Text("ID: ${user.accountId}",fontSize=10.sp,color=Color.White.copy(alpha=.60f),maxLines=1)
      }
     }
    }
   }
  }
 }
}

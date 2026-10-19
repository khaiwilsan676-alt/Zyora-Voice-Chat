package com.hurry.voicechat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage

data class ActiveUserNative(val accountId:String,val name:String,val image:String)

@Composable
fun ActiveUsers(isOpen:Boolean,onClose:()->Unit,roomUsers:List<ActiveUserNative>,onOpenProfile:(ActiveUserNative)->Unit,onCopyUserId:(String)->Unit){
 if(!isOpen)return
 Box(Modifier.fillMaxSize(),contentAlignment=Alignment.BottomCenter){
  Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.30f)).clickable(onClick=onClose))
  Column(Modifier.fillMaxWidth().fillMaxHeight(.30f).clip(RoundedCornerShape(topStart=24.dp,topEnd=24.dp)).background(Color.White)){
   Text("Active Users",Modifier.fillMaxWidth().padding(horizontal=24.dp,vertical=22.dp),18.sp,FontWeight.Bold,color=Color(0xFF333333))
   HorizontalDivider(color=Color(0xFFE5E7EB))
   if(roomUsers.isEmpty()) Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text("No active users",14.sp,color=Color(0xFF9CA3AF))}
   else LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(12.dp,12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
    items(roomUsers,key={it.accountId}){user->
     Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0xFFF9FAFB)).padding(12.dp,10.dp),verticalAlignment=Alignment.CenterVertically){
      AsyncImage(model=if(user.image.isBlank())"/default-avatar.png" else user.image,contentDescription=user.name,modifier=Modifier.size(40.dp).clip(CircleShape).clickable{onOpenProfile(user)},contentScale=ContentScale.Crop)
      Column(Modifier.weight(1f).padding(start=12.dp)){
       Text(user.name,14.sp,FontWeight.SemiBold,color=Color(0xFF333333),maxLines=1)
       Row(verticalAlignment=Alignment.CenterVertically){
        Text("ID: "+user.accountId,12.sp,color=Color(0xFF9CA3AF))
        Text("  ⧉",Modifier.clickable{onCopyUserId(user.accountId)},14.sp,color=Color(0xFF9CA3AF))
       }
      }
     }
    }
   }
  }
 }
}

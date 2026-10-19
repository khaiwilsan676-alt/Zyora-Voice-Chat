package com.hurry.voicechat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage

data class NativeSeat(val number:Int,val isOccupied:Boolean,val isLocked:Boolean=false,val user:NativeSeatUser?=null,val isMuted:Boolean=false,val isSpeaking:Boolean=false)
data class NativeSeatUser(val name:String,val image:String,val accountId:String)

@Composable
fun SeatItem(seatNumber:Int,seatData:NativeSeat?,onClick:()->Unit,onAvatarClick:()->Unit={},accountId:String="",roomOwnerId:String=""){
 val locked=seatData?.isLocked==true;val occupied=seatData?.isOccupied==true;val user=seatData?.user;val speaking=seatData?.isSpeaking==true&&!seatData.isMuted;val owner=occupied&&user?.accountId==roomOwnerId
 Column(horizontalAlignment=Alignment.CenterHorizontally,modifier=Modifier.clickable(onClick=onClick)){
  Box(Modifier.size(60.dp),contentAlignment=Alignment.Center){
   if(speaking)Box(Modifier.size(68.dp).background(Color(0x553B82F6),CircleShape))
   Box(Modifier.size(60.dp).clip(CircleShape).background(Color(0x527D8FA8)),contentAlignment=Alignment.Center){
    when{
     locked->Text("🔒",fontSize=25.sp)
     occupied&&user!=null->AsyncImage(if(user.image.isBlank())"/default-avatar.png" else user.image,user.name,Modifier.fillMaxSize().clip(CircleShape).clickable(onClick=onAvatarClick),contentScale=ContentScale.Crop)
     else->Text("♟",fontSize=28.sp,color=Color(0xFF94A7BE))
    }
   }
   if(seatData?.isMuted==true)Box(Modifier.align(Alignment.BottomEnd).size(20.dp).background(Color.Red,CircleShape),contentAlignment=Alignment.Center){Text("×",Color.White,fontSize=12.sp)}
  }
  Row(verticalAlignment=Alignment.CenterVertically){
   if(owner)Text("⌂ ",fontSize=10.sp,color=Color(0xFF3B82F6))
   Text(if(locked)"No $seatNumber" else if(occupied&&user!=null)user.name else "No $seatNumber",fontSize=10.sp,color=Color.White.copy(.80f),maxLines=1)
  }
 }
}

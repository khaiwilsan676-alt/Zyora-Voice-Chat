package com.hurry.voicechat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class SeatActionData(val isLocked:Boolean=false,val isOccupied:Boolean=false,val isMuted:Boolean=false)
@Composable
fun SeatActions(isOpen:Boolean,onClose:()->Unit,seatNumber:Int?,seatData:SeatActionData?=null,isMySeat:Boolean,isTakenByOther:Boolean,onTakeSeat:()->Unit,onLeaveSeat:()->Unit,onToggleMute:()->Unit,onToggleLock:()->Unit,onInvite:()->Unit){
 if(!isOpen||seatNumber==null)return
 Box(Modifier.fillMaxSize(),contentAlignment=Alignment.BottomCenter){
  Box(Modifier.fillMaxSize().background(Color.Black.copy(.30f)).clickable(onClick=onClose))
  Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart=24.dp,topEnd=24.dp)).background(Color.White).padding(24.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){
   if(!isTakenByOther&&!isMySeat)Action("Take Mic",!(seatData?.isLocked==true&&!seatData.isOccupied),onTakeSeat)
   if(isMySeat)Action("Leave Seat",true,onLeaveSeat)
   if(isMySeat)Action(if(seatData?.isMuted==true)"Unmute" else "Mute",true,onToggleMute)
   Action(if(seatData?.isLocked==true)"Unlock Mic" else "Lock Mic",true,onToggleLock)
   Action("Invite",true,onInvite)
  }
 }
}
@Composable private fun Action(label:String,enabled:Boolean,onClick:()->Unit){Text(label,Modifier.fillMaxWidth().clickable(enabled=enabled,onClick=onClick).padding(vertical=11.dp),fontSize=16.sp,color=if(enabled)Color.Black else Color.Gray)}

package com.hurry.voicechat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage

data class SignInReward(val day:Int,val reward:String,val image:String)
private val signInRewards=listOf(
 SignInReward(1,"+5000","file_00000000e56882119c217d508b6733dc.png"),
 SignInReward(2,"+5000","file_00000000e56882119c217d508b6733dc.png"),
 SignInReward(3,"×2 Days","/file_00000000d808821186c1b7b612eea3fc.png"),
 SignInReward(4,"+10,000","file_00000000e56882119c217d508b6733dc.png"),
 SignInReward(5,"+10,000","file_00000000e56882119c217d508b6733dc.png"),
 SignInReward(6,"×2 Days","file_00000000e56882119c217d508b6733dc.png"),
 SignInReward(7,"+15,000","file_00000000e56882119c217d508b6733dc.png")
)
@Composable
fun DailyCheckInModal(isOpen:Boolean,onClose:()->Unit,currentDay:Int,onSignIn:()->Unit,claimedToday:Boolean=false){
 if(!isOpen)return
 Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.60f)).clickable(onClick=onClose).padding(top=60.dp,start=16.dp,end=16.dp),contentAlignment=Alignment.Center){
  Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Color.White).border(2.dp,Color(0xFF3B82F6),RoundedCornerShape(24.dp)).padding(top=50.dp,start=24.dp,end=24.dp,bottom=20.dp)){
   LazyVerticalGrid(columns=GridCells.Fixed(4),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.heightIn(max=360.dp)){items(signInRewards.take(4)){RewardCard(it,currentDay)}}
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){RewardCard(signInRewards[4],currentDay,Modifier.weight(1f).height(80.dp));RewardCard(signInRewards[5],currentDay,Modifier.weight(1f).height(80.dp))}
   Spacer(Modifier.height(8.dp));RewardCard(signInRewards[6],currentDay,Modifier.fillMaxWidth().height(100.dp),true);Spacer(Modifier.height(20.dp))
   Button(onClick=onSignIn,enabled=!claimedToday&&currentDay<=7,modifier=Modifier.fillMaxWidth().height(52.dp),shape=RoundedCornerShape(12.dp),colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF3B82F6),disabledContainerColor=Color(0xFF9CA3AF))){Text(if(claimedToday)"Claimed Today — Come Back Tomorrow" else if(currentDay>7)"All Rewards Claimed!" else "Sign In",fontSize=16.sp)}
  }
  Box(Modifier.align(Alignment.BottomCenter).padding(bottom=8.dp).size(48.dp).background(Color.White.copy(alpha=.20f),RoundedCornerShape(50.dp)).clickable(onClick=onClose),contentAlignment=Alignment.Center){Text("×",Color.White,24.sp)}
 }
}
@Composable private fun RewardCard(item:SignInReward,currentDay:Int,modifier:Modifier=Modifier,big:Boolean=false){
 val borderColor=when{item.day<currentDay->Color(0xFF4ADE80);item.day==currentDay->Color(0xFF3B82F6);else->Color(0xFFE5E7EB)}
 Box(modifier.aspectRatio(if(big)3f else 1f).clip(RoundedCornerShape(8.dp)).border(2.dp,borderColor,RoundedCornerShape(8.dp)).background(Color.White),contentAlignment=Alignment.Center){
  Text(item.day.toString(),Modifier.align(Alignment.TopStart).background(Color(0xFF3B82F6),RoundedCornerShape(topStart=8.dp,bottomEnd=8.dp)).padding(horizontal=7.dp,vertical=3.dp),Color.White,10.sp)
  Column(horizontalAlignment=Alignment.CenterHorizontally){AsyncImage(model=item.image,contentDescription="reward",modifier=Modifier.size(if(big)48.dp else 40.dp));Text(item.reward,10.sp,color=Color(0xFF374151))}
  if(item.day<currentDay)Text("✓",Modifier.align(Alignment.TopEnd).padding(3.dp),color=Color(0xFF22C55E),fontSize=14.sp)
 }
}

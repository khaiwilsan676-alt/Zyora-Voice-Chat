package com.hurry.voicechat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ExitMenu(isOpen:Boolean,onClose:()->Unit,onKeep:()->Unit,onExit:()->Unit){
 if(!isOpen)return
 Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.40f))){
  Column(Modifier.align(Alignment.Center),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(32.dp)){
   Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)){
    Box(Modifier.size(80.dp).background(Color(0xFF3B82F6),CircleShape).clickable(onClick=onKeep),contentAlignment=Alignment.Center){Text("−",Color.White,34.sp,FontWeight.Bold)}
    Text("Keep",Color.White,16.sp,FontWeight.SemiBold)
   }
   Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)){
    Box(Modifier.size(80.dp).background(Color(0xFF3B82F6),CircleShape).clickable(onClick=onExit),contentAlignment=Alignment.Center){Text("→",Color.White,34.sp)}
    Text("Exit",Color.White.copy(alpha=.70f),14.sp,FontWeight.Medium)
   }
  }
  Box(Modifier.align(Alignment.BottomCenter).padding(bottom=32.dp).size(40.dp).background(Color.White.copy(alpha=.10f),CircleShape).clickable(onClick=onClose),contentAlignment=Alignment.Center){Text("×",Color.White,24.sp)}
 }
}

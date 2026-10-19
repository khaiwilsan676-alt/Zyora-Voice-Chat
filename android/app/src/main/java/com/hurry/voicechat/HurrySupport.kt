package com.hurry.voicechat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
@Composable fun HurrySupport(onBack:()->Unit){
 Column(Modifier.fillMaxSize().background(Color.White)){
  Row(Modifier.fillMaxWidth().padding(top=30.dp,start=8.dp,end=8.dp,bottom=12.dp)){IconButton(onClick=onBack){Icon(Icons.Default.ArrowBack,"Back")};Text("Zaho Support",style=MaterialTheme.typography.titleLarge,modifier=Modifier.padding(top=12.dp))}
  Column(Modifier.fillMaxSize().padding(16.dp)){Text("How can we help?",style=MaterialTheme.typography.headlineSmall);Spacer(Modifier.height(12.dp));Text("For account, wallet, room, gift and other Zaho app support, contact the Zaho Team.",color=Color.Gray);Spacer(Modifier.height(20.dp));Text("Support",style=MaterialTheme.typography.titleMedium);Spacer(Modifier.height(6.dp));Text("Please provide your account information and a clear description of the issue.",color=Color.Gray)}
 }
}
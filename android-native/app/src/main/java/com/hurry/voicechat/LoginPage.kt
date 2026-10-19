package com.hurry.voicechat
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

@Composable
fun LoginPage(onLogin:(String)->Unit={}) {
 var phone by remember { mutableStateOf("") }
 Box(Modifier.fillMaxSize().padding(24.dp),contentAlignment=Alignment.Center) {
  Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(16.dp)) {
   AsyncImage(model="file:///android_asset/logo.png",contentDescription="Hurry",modifier=Modifier.size(92.dp))
   Text("Hurry",style=MaterialTheme.typography.headlineLarge)
   OutlinedTextField(phone,{phone=it},singleLine=true,label={Text("Phone")},shape=RoundedCornerShape(14.dp))
   Button({onLogin(phone)},enabled=phone.isNotBlank(),modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(14.dp)){Text("Continue")}
  }
 }
}
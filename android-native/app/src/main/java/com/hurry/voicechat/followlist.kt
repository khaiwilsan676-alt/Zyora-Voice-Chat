package com.hurry.voicechat
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
@Composable fun FollowList(onBack:()->Unit={},type:String="friends"){Column(Modifier.fillMaxSize().padding(top=28.dp)){Text(type,modifier=Modifier.padding(16.dp));repeat(10){Text("User "+(it+1),modifier=Modifier.padding(horizontal=16.dp,vertical=8.dp))};TextButton(onClick=onBack){Text("Back")}}}
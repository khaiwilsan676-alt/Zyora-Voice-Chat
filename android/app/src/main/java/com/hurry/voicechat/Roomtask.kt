package com.hurry.voicechat
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
@Composable fun Roomtask(onBack:()->Unit={}){Column(Modifier.fillMaxSize().padding(top=28.dp).padding(16.dp),horizontalAlignment=Alignment.CenterHorizontally){Text("Room Task",style=MaterialTheme.typography.titleLarge);Spacer(Modifier.height(20.dp));Text("Complete your room tasks.");Spacer(Modifier.weight(1f));Button(onClick=onBack){Text("Back")}}}
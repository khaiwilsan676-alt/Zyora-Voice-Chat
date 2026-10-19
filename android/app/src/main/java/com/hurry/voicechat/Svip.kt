package com.hurry.voicechat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
@Composable fun Svip(onBack:()->Unit={}){Column(Modifier.fillMaxSize().background(Color.Black),horizontalAlignment=Alignment.CenterHorizontally){Row(Modifier.fillMaxWidth().padding(top=28.dp)){TextButton(onClick=onBack){Text("‹",color=Color.White)}};Spacer(Modifier.weight(1f));Text("SVIP COMING SOON",color=Color.White,style=MaterialTheme.typography.headlineSmall);Spacer(Modifier.weight(1f))}}
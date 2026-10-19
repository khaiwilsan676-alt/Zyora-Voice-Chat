package com.hurry.voicechat
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
@Composable fun Wildparty(onClose:()->Unit={},onMinimize:()->Unit={}){Column(Modifier.fillMaxSize().padding(16.dp)){Text("Wild Party",style=MaterialTheme.typography.titleLarge);Spacer(Modifier.weight(1f));Button(onClick=onClose,modifier=Modifier.fillMaxWidth()){Text("Close")}}}
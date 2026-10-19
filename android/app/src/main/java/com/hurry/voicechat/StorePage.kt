package com.hurry.voicechat
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
@Composable fun StorePage(onBack:()->Unit={}){Column(Modifier.fillMaxSize().padding(top=28.dp).padding(16.dp)){Row(Modifier.fillMaxWidth()){TextButton(onClick=onBack){Text("‹")};Text("Store",style=MaterialTheme.typography.titleLarge)};Spacer(Modifier.height(20.dp));Text("Store",style=MaterialTheme.typography.headlineSmall)}}
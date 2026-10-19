package com.hurry.voicechat
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
@Composable fun OwnerBan(){var id by remember{mutableStateOf("")};Column(Modifier.fillMaxSize().padding(16.dp)){Text("Owner Ban",style=MaterialTheme.typography.titleLarge);OutlinedTextField(id,{id=it},label={Text("User ID")},modifier=Modifier.fillMaxWidth());Button(onClick={},modifier=Modifier.fillMaxWidth()){Text("Search")}}}
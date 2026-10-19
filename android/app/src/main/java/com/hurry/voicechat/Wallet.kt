package com.hurry.voicechat
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
@Composable fun Wallet(onBack:()->Unit={}){var coins by remember{mutableStateOf(0)};var diamonds by remember{mutableStateOf(0)};Column(Modifier.fillMaxSize().padding(top=28.dp).padding(16.dp)){Row(Modifier.fillMaxWidth()){TextButton(onClick=onBack){Text("‹")};Text("Wallet",style=MaterialTheme.typography.titleLarge)};Spacer(Modifier.height(20.dp));Text("₹ 0");Text("Coins: "+coins);Text("Diamonds: "+diamonds)}}
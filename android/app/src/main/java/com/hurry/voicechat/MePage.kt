package com.hurry.voicechat
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
@Composable fun MePage(onBack:()->Unit={},onWallet:()->Unit={},onSetting:()->Unit={}){Column(Modifier.fillMaxSize().padding(top=40.dp)){Text("Me",style=MaterialTheme.typography.headlineSmall,modifier=Modifier.padding(16.dp));Button(onClick=onWallet,modifier=Modifier.fillMaxWidth().padding(16.dp)){Text("Wallet")};TextButton(onClick=onSetting,modifier=Modifier.fillMaxWidth()){Text("Setting")}}}
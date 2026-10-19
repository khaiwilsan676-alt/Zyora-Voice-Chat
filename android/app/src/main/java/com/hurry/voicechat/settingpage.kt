package com.hurry.voicechat
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
@Composable fun SettingPage(onBack:()->Unit={},onLogout:()->Unit={}) {
 var notifications by remember{mutableStateOf(true)}; var block by remember{mutableStateOf(false)}; var about by remember{mutableStateOf(false)}
 if(block){Column(Modifier.fillMaxSize().statusBarsPadding().padding(16.dp)){TextButton({block=false}){Text("‹")};Text("Blocklist",style=MaterialTheme.typography.titleLarge);Spacer(Modifier.height(80.dp));Text("No data")};return}
 if(about){Column(Modifier.fillMaxSize().statusBarsPadding().padding(16.dp)){TextButton({about=false}){Text("‹")};Text("About Us",style=MaterialTheme.typography.titleLarge);Spacer(Modifier.height(24.dp));Text("Zaho is a real-time group voice chat application for multiple players.");Text("We have Free online voice chat, online parties and gorgeous gifts.")};return}
 Column(Modifier.fillMaxSize().statusBarsPadding()){Row(Modifier.padding(8.dp)){TextButton(onBack){Text("‹")};Spacer(Modifier.weight(1f));Text("Settings",style=MaterialTheme.typography.titleLarge);Spacer(Modifier.weight(1f))};HorizontalDivider();ListItem(headlineContent={Text("Message Notifications")},trailingContent={Switch(notifications,{notifications=!notifications})});ListItem(headlineContent={Text("Blocklist")},trailingContent={TextButton({block=true}){Text("›")}});ListItem(headlineContent={Text("About")},trailingContent={TextButton({about=true}){Text("›")}});Spacer(Modifier.height(40.dp));Button(onLogout,Modifier.fillMaxWidth().padding(horizontal=24.dp)){Text("Logout")}}
}
package com.hurry.voicechat
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
@Composable fun SearchPage(onBack:()->Unit={},onSearch:(String)->Unit={}){var q by remember{mutableStateOf("")};Column(Modifier.fillMaxSize().padding(top=28.dp).padding(16.dp)){Row(Modifier.fillMaxWidth()){TextButton(onClick=onBack){Text("‹")};Text("Search",style=MaterialTheme.typography.titleLarge)};OutlinedTextField(q,{q=it},Modifier.fillMaxWidth(),singleLine=true,placeholder={Text("Search")});Button({onSearch(q)},Modifier.fillMaxWidth().padding(top=12.dp)){Text("Search")}}}
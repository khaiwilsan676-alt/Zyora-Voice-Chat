package com.hurry.voicechat
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
@Composable fun LuckyBag(onClose:()->Unit={}){var coins by remember{mutableStateOf(10000)};var people by remember{mutableStateOf(5)};Column(Modifier.fillMaxWidth().padding(16.dp)){Text("Lucky Bag",style=MaterialTheme.typography.titleLarge);Text("Gold Quantity");Row{listOf(10000,100000,200000,500000,1000000,1500000,2000000).forEach{Button({coins=it},Modifier.padding(2.dp)){Text(it.toString())}}};Text("Number of people");Row{listOf(5,10,30,50).forEach{Button({people=it},Modifier.padding(2.dp)){Text(it.toString())}}};Button(onClick=onClose){Text("Close")}}}
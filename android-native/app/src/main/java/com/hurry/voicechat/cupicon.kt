package com.hurry.voicechat
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
@Composable fun CupIcon(onBack:()->Unit={},count:Int=0){Column(Modifier.fillMaxWidth().padding(16.dp)){Text("Cup");Text(count.toString());TextButton(onClick=onBack){Text("Back")}}}
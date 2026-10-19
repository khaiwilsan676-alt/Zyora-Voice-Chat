package com.hurry.voicechat
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
data class RoomSettingsDataNative(val roomName:String="",val announcement:String="",val micMode:Int=15,val theme:String="",val isLocked:Boolean=false,val roomPassword:String="")
@Composable fun RoomSettingPage(initial:RoomSettingsDataNative=RoomSettingsDataNative(),onBack:()->Unit={},onSave:(RoomSettingsDataNative)->Unit={}){var s by remember{mutableStateOf(initial)};Column(Modifier.fillMaxSize().padding(top=28.dp).padding(16.dp)){Text("Room Settings",style=MaterialTheme.typography.titleLarge);OutlinedTextField(s.roomName,{s=s.copy(roomName=it)},Modifier.fillMaxWidth(),label={Text("Room Name")});OutlinedTextField(s.announcement,{s=s.copy(announcement=it)},Modifier.fillMaxWidth(),label={Text("Announcement")});Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("Room Lock");Switch(s.isLocked,{s=s.copy(isLocked=it)})};Button({onSave(s)},Modifier.fillMaxWidth()){Text("Save")};TextButton(onClick=onBack,modifier=Modifier.fillMaxWidth()){Text("Back")}}}
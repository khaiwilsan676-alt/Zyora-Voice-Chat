package com.hurry.voicechat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
data class NativeMusicTrack(val id:String,val name:String,val url:String)
@Composable fun Fourgride(onClose:()->Unit,onClearChat:()->Unit,publicMsgOff:Boolean,onTogglePublicMsg:()->Unit,speaker:Boolean,onToggleSpeaker:()->Unit,onMusicPlay:((NativeMusicTrack)->Unit)?=null,onOpenStore:((String)->Unit)?=null){
 var entryEffect by remember{mutableStateOf(false)}
 var giftEffect by remember{mutableStateOf(false)}
 var musicSheet by remember{mutableStateOf(false)}
 var luckyBag by remember{mutableStateOf(false)}
 Column(Modifier.fillMaxWidth().background(Color.White).padding(bottom=18.dp)){
  Row(Modifier.fillMaxWidth().padding(14.dp)){Text("Room settings",style=MaterialTheme.typography.titleLarge,Modifier.weight(1f));IconButton(onClick=onClose){Icon(Icons.Default.Close,"Close")}}
  LazyColumn{
   item{SettingSwitch("Public messages",!publicMsgOff,onTogglePublicMsg)}
   item{SettingSwitch("Speaker",speaker,onToggleSpeaker)}
   item{SettingSwitch("Entry effect",entryEffect){entryEffect=!entryEffect}}
   item{SettingSwitch("Gift effect",giftEffect){giftEffect=!giftEffect}}
   item{ListItem(headlineContent={Text("Music")},modifier=Modifier.clickable{musicSheet=true})}
   item{ListItem(headlineContent={Text("Lucky Bag")},modifier=Modifier.clickable{luckyBag=true})}
   item{ListItem(headlineContent={Text("Clear chat")},modifier=Modifier.clickable{onClearChat()})}
  }
 }
 if(musicSheet)AlertDialog(onDismissRequest={musicSheet=false},confirmButton={TextButton({musicSheet=false}){Text("Close")}},title={Text("Music")},text={Text("Music library")})
 if(luckyBag)AlertDialog(onDismissRequest={luckyBag=false},confirmButton={TextButton({luckyBag=false}){Text("Close")}},title={Text("Lucky Bag")},text={Text("Lucky Bag")})
}
@Composable private fun SettingSwitch(title:String,checked:Boolean,onChange:()->Unit)=ListItem(headlineContent={Text(title)},trailingContent={Switch(checked,onCheckedChange={onChange()})})
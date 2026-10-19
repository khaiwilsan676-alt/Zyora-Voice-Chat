package com.hurry.voicechat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*
data class NativeChatMessage(val id:String,val text:String,val sender:String,val timestamp:Long=System.currentTimeMillis(),val type:String="message",val imageUrl:String?=null)
@Composable fun ChatScreen(currentUser:HurryUser,targetUser:HurryUser,onClose:()->Unit,onJoinRoom:((String)->Unit)?=null,sharedRoomData:String?=null){
 var input by remember{mutableStateOf("")}
 var messages by remember{mutableStateOf(listOf<NativeChatMessage>())}
 var options by remember{mutableStateOf(false)}
 var blocked by remember{mutableStateOf(false)}
 var reply by remember{mutableStateOf<NativeChatMessage?>(null)}
 Column(Modifier.fillMaxSize().background(Color.White)){
  Row(Modifier.fillMaxWidth().padding(top=27.dp,start=8.dp,end=8.dp,bottom=8.dp),verticalAlignment=Alignment.CenterVertically){
   IconButton(onClick=onClose){Icon(Icons.Default.ArrowBack,"Back")}
   Column(Modifier.weight(1f)){Text(targetUser.name,style=MaterialTheme.typography.titleMedium);Text(if(blocked)"Blocked" else "Online",style=MaterialTheme.typography.labelSmall,color=Color.Gray)}
   IconButton(onClick={options=true}){Icon(Icons.Default.MoreVert,"More")}
  }
  LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(horizontal=12.dp)){items(messages){m->Row(Modifier.fillMaxWidth().padding(vertical=4.dp),horizontalArrangement=if(m.sender=="me")Arrangement.End else Arrangement.Start){Surface(color=if(m.sender=="me")Color(0xFFE9F3FF) else Color(0xFFF1F1F1),shape=MaterialTheme.shapes.medium){Column(Modifier.padding(horizontal=12.dp,vertical=8.dp)){if(m.type=="image")Text("[Image]");Text(m.text);Text(SimpleDateFormat("HH:mm",Locale.getDefault()).format(Date(m.timestamp)),style=MaterialTheme.typography.labelSmall,color=Color.Gray)}}}}}
  if(reply!=null)Row(Modifier.fillMaxWidth().padding(horizontal=12.dp)){Text("Replying to "+reply!!.text,Modifier.weight(1f));TextButton({reply=null}){Text("Cancel")}}
  Row(Modifier.fillMaxWidth().padding(8.dp),verticalAlignment=Alignment.CenterVertically){OutlinedTextField(value=input,onValueChange={input=it},modifier=Modifier.weight(1f),placeholder={Text("Message")},singleLine=true);IconButton(onClick={if(input.isNotBlank()){messages=messages+NativeChatMessage(UUID.randomUUID().toString(),input,"me");input=""}}){Icon(Icons.Default.Send,"Send")}}
 }
 if(options)AlertDialog(onDismissRequest={options=false},title={Text("Options")},text={Text(if(blocked)"User is blocked" else "Block this user")},confirmButton={TextButton({blocked=!blocked;options=false}){Text(if(blocked)"Block" else "Unblock")}})
}
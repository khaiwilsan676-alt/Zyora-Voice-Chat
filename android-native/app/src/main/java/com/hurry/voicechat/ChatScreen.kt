package com.hurry.voicechat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
  Surface(
   modifier=Modifier.fillMaxWidth().imePadding().navigationBarsPadding(),
   color=Color.White,
   shadowElevation=8.dp
  ){
   Row(
    modifier=Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=9.dp),
    verticalAlignment=Alignment.CenterVertically,
    horizontalArrangement=Arrangement.spacedBy(9.dp)
   ){
    TextField(
     value=input,
     onValueChange={input=it},
     modifier=Modifier.weight(1f).defaultMinSize(minHeight=50.dp),
     placeholder={Text("Message",color=Color(0xFF858B95))},
     singleLine=true,
     shape=RoundedCornerShape(28.dp),
     colors=TextFieldDefaults.colors(
      focusedContainerColor=Color(0xFFF1F3F6),
      unfocusedContainerColor=Color(0xFFF1F3F6),
      disabledContainerColor=Color(0xFFF1F3F6),
      focusedIndicatorColor=Color.Transparent,
      unfocusedIndicatorColor=Color.Transparent,
      disabledIndicatorColor=Color.Transparent,
      cursorColor=Color(0xFF2878E8)
     )
    )
    FilledIconButton(
     onClick={if(input.isNotBlank()){messages=messages+NativeChatMessage(UUID.randomUUID().toString(),input,"me");input=""}},
     modifier=Modifier.size(48.dp),
     shape=CircleShape,
     colors=IconButtonDefaults.filledIconButtonColors(containerColor=Color(0xFF2878E8),contentColor=Color.White)
    ){
     Icon(Icons.Default.Send,contentDescription="Send message")
    }
   }
  }
 }
 if(options)AlertDialog(onDismissRequest={options=false},title={Text("Options")},text={Text(if(blocked)"User is blocked" else "Block this user")},confirmButton={TextButton({blocked=!blocked;options=false}){Text(if(blocked)"Block" else "Unblock")}})
}
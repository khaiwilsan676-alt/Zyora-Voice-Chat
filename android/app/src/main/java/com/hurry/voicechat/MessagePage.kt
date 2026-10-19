package com.hurry.voicechat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

data class MessageChat(val id:String,val name:String,val photo:String,val lastMessage:String="",val unread:Int=0)
@Composable
fun MessagePage(chats:List<MessageChat>=emptyList(),onChatOpen:(MessageChat)->Unit={}) {
 Column(Modifier.fillMaxSize()){
  Text("Message",style=MaterialTheme.typography.headlineSmall,modifier=Modifier.padding(start=16.dp,top=11.dp,bottom=12.dp))
  LazyColumn{items(chats,key={it.id}){c->Row(Modifier.fillMaxWidth().clickable{onChatOpen(c)}.padding(horizontal=12.dp,vertical=10.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){AsyncImage(model=c.photo,contentDescription=c.name,modifier=Modifier.size(56.dp));Column(Modifier.weight(1f)){Text(c.name);Text(c.lastMessage,maxLines=1,color=MaterialTheme.colorScheme.onSurfaceVariant)};if(c.unread>0)Badge{Text(c.unread.toString())}}}}
 }
}
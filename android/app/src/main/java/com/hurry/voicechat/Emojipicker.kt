package com.hurry.voicechat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage

data class EmojiSticker(val id:String,val name:String,val src:String)
@Composable
fun EmojiPicker(onClose:()->Unit={},onSelectEmoji:(EmojiSticker)->Unit={}){
 var activeTab by remember{mutableStateOf("emojis")}
 val stickers=listOf(
  EmojiSticker("laugh","Laugh","/512.gif"),EmojiSticker("sad","Sad","/512 (6).gif"),EmojiSticker("love","Sleep","/512 (3).gif"),EmojiSticker("thinking","Thinking","/512 (2).gif"),
  EmojiSticker("party","Party","/512 (16).gif"),EmojiSticker("loving","Loving","/512 (15).gif"),EmojiSticker("smart","Smart","/512 (13).gif"),EmojiSticker("irritating","Irritating","/512 (12).gif"),
  EmojiSticker("rolling","Rolling","/512 (10).gif"),EmojiSticker("unamused","Unamused","/512 (11).gif"),EmojiSticker("pleading","Pleading","/512 (4).gif"),EmojiSticker("hug","Hug","/512 (8).gif"),
  EmojiSticker("kiss","Kiss-R","/512 (14).gif"),EmojiSticker("Angery"," Angery","/512 (1).gif")
 )
 Box(Modifier.fillMaxSize(),contentAlignment=Alignment.BottomCenter){
  Box(Modifier.fillMaxSize().clickable(onClick=onClose))
  Column(Modifier.fillMaxWidth().fillMaxHeight(.42f).background(Color(0xFF121212)).padding(horizontal=16.dp,vertical=12.dp)){
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.Center){Tab("Emojis",activeTab=="emojis"){activeTab="emojis"};Spacer(Modifier.width(32.dp));Tab("Premium",activeTab=="premium"){activeTab="premium"}}
   if(activeTab=="emojis")LazyVerticalGrid(columns=GridCells.Fixed(4),contentPadding=PaddingValues(vertical=12.dp),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){items(stickers){s->Column(Modifier.fillMaxWidth().clickable{onSelectEmoji(s);onClose()},horizontalAlignment=Alignment.CenterHorizontally){AsyncImage(model=s.src,contentDescription=s.name,modifier=Modifier.size(48.dp),contentScale=ContentScale.Fit);Text(s.name,11.sp,color=Color.White.copy(alpha=.90f))}}}
   else Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text("Premium Emojis Coming Soon",color=Color.White.copy(alpha=.70f),fontSize=12.sp)}
  }
 }
}
@Composable private fun Tab(label:String,selected:Boolean,onClick:()->Unit){Column(horizontalAlignment=Alignment.CenterHorizontally,modifier=Modifier.clickable(onClick=onClick)){Text(label,color=if(selected)Color.White else Color.White.copy(alpha=.60f),fontSize=14.sp);if(selected)Box(Modifier.padding(top=4.dp).height(2.dp).width(42.dp).background(Color.White,RoundedCornerShape(2.dp)))}}

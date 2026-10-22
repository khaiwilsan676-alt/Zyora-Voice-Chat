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
  EmojiSticker("laugh","Laugh","https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/512.gif"),
  EmojiSticker("sad","Sad","https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/512%20(6).gif"),
  EmojiSticker("love","Sleep","https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/512%20(3).gif"),
  EmojiSticker("thinking","Thinking","https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/512%20(2).gif"),
  EmojiSticker("party","Party","https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/512%20(16).gif"),
  EmojiSticker("loving","Loving","https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/512%20(15).gif"),
  EmojiSticker("smart","Smart","https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/512%20(13).gif"),
  EmojiSticker("irritating","Irritating","https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/512%20(12).gif"),
  EmojiSticker("rolling","Rolling","https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/512%20(10).gif"),
  EmojiSticker("unamused","Unamused","https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/512%20(11).gif"),
  EmojiSticker("pleading","Pleading","https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/512%20(4).gif"),
  EmojiSticker("hug","Hug","https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/512%20(8).gif"),
  EmojiSticker("kiss","Kiss-R","https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/512%20(14).gif"),
  EmojiSticker("Angery"," Angery","https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/512%20(1).gif")
 )
 Box(Modifier.fillMaxSize(),contentAlignment=Alignment.BottomCenter){
  Box(Modifier.fillMaxSize().clickable(onClick=onClose))
  
  // Yaha background sidha Black kar diya hai, purani image hata di
  Box(Modifier.fillMaxWidth().fillMaxHeight(.42f).background(Color.Black)){
   
   // Top Border overlap image 
   AsyncImage(
    model="https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/file_0000000022f481fab7ddec2efb9449f9.png",
    contentDescription="Top Border",
    modifier=Modifier
        .fillMaxWidth()
        .height(30.dp) // Border ki height yaha se adjust kar lena
        .align(Alignment.TopCenter), 
    contentScale=ContentScale.FillWidth
   )

   Column(Modifier.fillMaxSize().padding(horizontal=16.dp,vertical=12.dp)){
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.Center,verticalAlignment=Alignment.CenterVertically){
    Tab("Emojis",activeTab=="emojis"){activeTab="emojis"}
    Spacer(Modifier.width(32.dp))
    Tab("Premium",activeTab=="premium"){activeTab="premium"}
   }
   if(activeTab=="emojis") LazyVerticalGrid(
    columns=GridCells.Fixed(4),
    modifier=Modifier.weight(1f),
    contentPadding=PaddingValues(vertical=12.dp),
    horizontalArrangement=Arrangement.spacedBy(8.dp),
    verticalArrangement=Arrangement.spacedBy(16.dp)
   ){
    items(stickers){s->
     Column(
      Modifier.fillMaxWidth().clickable{
          onSelectEmoji(s)
          onClose()
      },
      horizontalAlignment=Alignment.CenterHorizontally
     ){
      AsyncImage(model=s.src,contentDescription=s.name,modifier=Modifier.size(48.dp),contentScale=ContentScale.Fit)
      Text(s.name.trim(),fontSize=11.sp,color=Color.White.copy(alpha=.90f),maxLines=1)
     }
    }
   } else Box(Modifier.weight(1f).fillMaxWidth(),contentAlignment=Alignment.Center){
    Text("Premium Emojis Coming Soon",color=Color.White.copy(alpha=.70f),fontSize=12.sp)
   }
   }
  }
 }
}

@Composable private fun Tab(label:String,selected:Boolean,onClick:()->Unit){Column(horizontalAlignment=Alignment.CenterHorizontally,modifier=Modifier.clickable(onClick=onClick)){Text(label,color=if(selected)Color.White else Color.White.copy(alpha=.60f),fontSize=14.sp);if(selected)Box(Modifier.padding(top=4.dp).height(2.dp).width(42.dp).background(Color.White,RoundedCornerShape(2.dp)))}}

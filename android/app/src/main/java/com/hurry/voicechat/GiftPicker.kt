package com.hurry.voicechat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
data class NativeGift(val id:Int,val name:String,val coins:Int,val image:String="",val lucky:Boolean=false)
@Composable fun GiftPicker(open:Boolean=true,onClose:()->Unit,seats:List<Any> = emptyList(),onSend:((Int)->Unit)?=null,roomId:String?=null,currentUserAccountId:String?=null,currentUserName:String="User",currentUserImage:String="/default-avatar.png",roomUsers:List<Any> = emptyList()){
 if(!open)return
 var tab by remember{mutableStateOf("Hot")}; var multiplier by remember{mutableStateOf(1)}; var selected by remember{mutableStateOf<Int?>(null)}
 val gifts=if(tab=="Lucky") listOf(NativeGift(101,"Tiara",3000,lucky=true),NativeGift(102,"Lucky Clover",1499,lucky=true),NativeGift(103,"Hi",999,lucky=true),NativeGift(104,"Rose",3999,lucky=true),NativeGift(105,"Kiss",1600,lucky=true),NativeGift(106,"Balloon",4000,lucky=true),NativeGift(107,"Dragon",7000,lucky=true),NativeGift(108,"Nine Hands",10999,lucky=true),NativeGift(109,"Coffin",8999,lucky=true),NativeGift(110,"Sword",9999,lucky=true),NativeGift(111,"Love lock",5000,lucky=true),NativeGift(112,"Lantern",6999,lucky=true),NativeGift(113,"Ring",5999,lucky=true),NativeGift(114,"Dancing Girl",12000,lucky=true),NativeGift(115,"Whale",7899,lucky=true),NativeGift(116,"Star",9800,lucky=true),NativeGift(117,"Fire Bird",13000,lucky=true)) else listOf(NativeGift(1,"Teddy",70000),NativeGift(2,"Autumn's Embrace",54900),NativeGift(3,"Arab King",500000))
 Column(Modifier.fillMaxWidth().background(Color.White).padding(12.dp)){
  Row(verticalAlignment=Alignment.CenterVertically){Text("Gift",style=MaterialTheme.typography.titleLarge,Modifier.weight(1f));TextButton(onClick=onClose){Text("Close")}}
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly){listOf("Hot","Lucky","Luxury","Event").forEach{t->TextButton({tab=t}){Text(t,color=if(tab==t)Color(0xFFE91E63) else Color.Gray)}}}
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End){listOf(1,10,299,599,999).forEach{v->TextButton({multiplier=v}){Text(v.toString()+"×")}}}
  LazyVerticalGrid(columns=GridCells.Fixed(4),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.heightIn(max=360.dp)){items(gifts){g->Card(Modifier.fillMaxWidth().aspectRatio(1f).clickable{selected=g.id},shape=RoundedCornerShape(12.dp)){Column(Modifier.fillMaxSize(),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){Text(g.name,style=MaterialTheme.typography.labelSmall);Text(g.coins.toString());if(selected==g.id)Text("✓")}}}}
  Button(onClick={selected?.let{id->gifts.firstOrNull{it.id==id}?.let{onSend?.invoke(it.coins*multiplier)}}},Modifier.fillMaxWidth()){Text("Send")}
 }
}
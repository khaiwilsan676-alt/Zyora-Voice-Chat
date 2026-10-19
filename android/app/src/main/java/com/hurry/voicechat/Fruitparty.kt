package com.hurry.voicechat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
data class FruitGridItemNative(val id:Int,val name:String,val multiplier:Int)
@Composable fun Fruitparty(onClose:()->Unit,onMinimize:(()->Unit)?=null,isMinimized:Boolean=false){
 val items=listOf(FruitGridItemNative(1,"Lemon",5),FruitGridItemNative(5,"Grapes",10),FruitGridItemNative(3,"Orange",5),FruitGridItemNative(8,"Cherry",45),FruitGridItemNative(2,"Apple",25),FruitGridItemNative(7,"Mango",5),FruitGridItemNative(4,"Strawberry",15),FruitGridItemNative(6,"Pear",5))
 var balance by remember{mutableStateOf(82927)}
 var bets by remember{mutableStateOf(mapOf<Int,Int>())}
 var countdown by remember{mutableStateOf(30)}
 var history by remember{mutableStateOf(false)}
 var rules by remember{mutableStateOf(false)}
 LaunchedEffect(Unit){while(true){delay(1000);countdown=if(countdown<=1)30 else countdown-1}}
 Column(Modifier.fillMaxSize().background(Color.Black)){
  Row(Modifier.fillMaxWidth().padding(top=18.dp,start=12.dp,end=8.dp),verticalAlignment=Alignment.CenterVertically){
   IconButton(onClick=onClose){Icon(Icons.Default.Close,"Close",tint=Color.White)}
   Text("Fruit Party",color=Color.White,style=MaterialTheme.typography.titleLarge,modifier=Modifier.weight(1f))
   Text("Coins "+balance,color=Color.White)
   IconButton(onClick={history=true}){Icon(Icons.Default.History,"History",tint=Color.White)}
  }
  Text("Round • "+countdown+" s",color=Color.White,modifier=Modifier.align(Alignment.CenterHorizontally).padding(8.dp))
  LazyVerticalGrid(columns=GridCells.Fixed(3),contentPadding=PaddingValues(12.dp),verticalArrangement=Arrangement.spacedBy(10.dp),horizontalArrangement=Arrangement.spacedBy(10.dp),modifier=Modifier.weight(1f)){
   items(items){item->Card(Modifier.fillMaxWidth().aspectRatio(1f).clickable{val old=bets[item.id]?:0;if(balance>=100){bets=bets+(item.id to old+100);balance-=100}},shape=RoundedCornerShape(14.dp)){Column(Modifier.fillMaxSize(),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){Text(item.name);Text("x"+item.multiplier);if((bets[item.id]?:0)>0)Text("Bet "+(bets[item.id]?:0))}}}
  }
  Row(Modifier.fillMaxWidth().padding(12.dp),horizontalArrangement=Arrangement.SpaceEvenly){TextButton({rules=true}){Text("Rules",color=Color.White)};TextButton({onMinimize?.invoke()}){Text("Minimize",color=Color.White)}}
 }
 if(history)AlertDialog(onDismissRequest={history=false},confirmButton={TextButton({history=false}){Text("Close")}},title={Text("My Records")},text={Text("Played bets are stored for this game session.")})
 if(rules)AlertDialog(onDismissRequest={rules=false},confirmButton={TextButton({rules=false}){Text("Close")}},title={Text("Rules")},text={Text("Place bets on a fruit. Award is bet multiplied by the fruit multiplier.")})
}
package com.hurry.voicechat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign

@Composable
fun Level(onBack:()->Unit={}) {
    var showHelp by remember { mutableStateOf(false) }; val tiers=(1..10).toList()
    Box(Modifier.fillMaxSize().background(Color(0xFF04060A))) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().height(58.dp),verticalAlignment=Alignment.CenterVertically) {
                Text("‹",color=Color.White,style=MaterialTheme.typography.headlineLarge,modifier=Modifier.clickable(onClick=onBack).padding(12.dp))
                Text("Level",color=Color.White,style=MaterialTheme.typography.titleLarge,modifier=Modifier.weight(1f),textAlign=TextAlign.Center)
                Text("?",color=Color.White,style=MaterialTheme.typography.titleLarge,modifier=Modifier.clickable{showHelp=true}.padding(16.dp))
            }
            Column(Modifier.padding(horizontal=16.dp)) {
                Row(Modifier.fillMaxWidth().background(Color(0xFF0D1626),RoundedCornerShape(12.dp)).padding(14.dp),verticalAlignment=Alignment.CenterVertically) {
                    Box(Modifier.size(52.dp).background(Color(0xFF5B6470),RoundedCornerShape(26.dp))); Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)){Text("Guest",color=Color.White,style=MaterialTheme.typography.titleMedium);Spacer(Modifier.height(8.dp));LinearProgressIndicator(progress={.38f},modifier=Modifier.fillMaxWidth().height(6.dp));Text("4.5k/20.2k remaining to reach Level 5 >",color=Color.LightGray,style=MaterialTheme.typography.labelSmall)}
                }
            }
            LazyColumn(Modifier.fillMaxSize().padding(horizontal=16.dp),contentPadding=PaddingValues(top=12.dp,bottom=24.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
                items(tiers.size){i->val tier=tiers[i];Column{
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text("⌃  Upgrade to level Lv."+tier,color=Color.White,modifier=Modifier.weight(1f));Text("Lv."+tier,color=Color(0xFFFFD56A))}
                    Spacer(Modifier.height(7.dp));Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){repeat(3){card->Box(Modifier.weight(1f).height(76.dp).background(Color(0xFF0B1421),RoundedCornerShape(8.dp)),contentAlignment=Alignment.Center){Text(if(card==0)"Coins" else if(card==1)"Entry" else "Frame",color=Color.White)}}}
                    Spacer(Modifier.height(8.dp));listOf("Level "+tier,"Room Send image").forEach{label->Row(Modifier.fillMaxWidth().background(Color(0xFF0A111C),RoundedCornerShape(7.dp)).padding(horizontal=14.dp,vertical=13.dp),verticalAlignment=Alignment.CenterVertically){Text(label,color=Color.White,modifier=Modifier.weight(1f));Text("›",color=Color.Gray)}}
                }}
            }
        }
        if(showHelp) AlertDialog(onDismissRequest={showHelp=false},confirmButton={TextButton(onClick={showHelp=false}){Text("OK")}},title={Text("Level")},text={Text("Upgrade your level to unlock level rewards.")})
    }
}

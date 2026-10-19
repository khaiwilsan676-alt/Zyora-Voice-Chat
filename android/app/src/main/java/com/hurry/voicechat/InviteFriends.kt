package com.hurry.voicechat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
@Composable fun InviteFriends(onBack:(()->Unit)?=null,onClose:(()->Unit)?=null){
 var rules by remember{mutableStateOf(false)}
 var copied by remember{mutableStateOf(false)}
 val code="WELCOME123"
 val link="https://yourapp.com/invite/"+code
 Column(Modifier.fillMaxSize().background(Color(0xFF4D0515))){
  Row(Modifier.fillMaxWidth().padding(top=24.dp,start=8.dp,end=8.dp)){IconButton(onClick={onBack?.invoke()?:onClose?.invoke()}){Icon(Icons.Default.ArrowBack,"Back",tint=Color.White)};Text("Invite Friends",color=Color.White,style=MaterialTheme.typography.titleLarge,modifier=Modifier.padding(top=12.dp))}
  Column(Modifier.padding(20.dp)){Text("Friend's Invite",color=Color.Yellow,style=MaterialTheme.typography.headlineSmall);Spacer(Modifier.height(24.dp));Text("Invite Friends",color=Color.Yellow);Text("Get 250000 coins",color=Color.Yellow);Spacer(Modifier.height(18.dp));Text("Friends Recharge",color=Color.Yellow);Text(">500000 Coins → Get 250000 coins",color=Color.Yellow);Spacer(Modifier.height(18.dp));Text("Friends Send Gift",color=Color.Yellow);Text("Get 6%",color=Color.Yellow);Spacer(Modifier.height(24.dp));Text("Invite Code",color=Color.White);Text(code,color=Color.Yellow,style=MaterialTheme.typography.headlineMedium);Text(link,color=Color.White);Spacer(Modifier.height(20.dp));Button(onClick={copied=true}){Text(if(copied)"Copied" else "Copy Link")};TextButton(onClick={rules=true}){Text("Rules",color=Color.Yellow)}}
 }
 if(rules)AlertDialog(onDismissRequest={rules=false},confirmButton={TextButton({rules=false}){Text("OK")}},title={Text("Rules")},text={Text("Invite a friend to register and receive coins. Eligible recharge and gift activity can provide inviter rewards according to the Hurry invite rules.")})
}
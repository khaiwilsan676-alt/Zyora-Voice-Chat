package com.hurry.voicechat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
@Composable fun SellerCenter(onBack:()->Unit={}) {
 var view by remember{mutableStateOf(false)}; var method by remember{mutableStateOf("User")}; var id by remember{mutableStateOf("")}; var amount by remember{mutableStateOf("")}
 Column(Modifier.fillMaxSize().background(Color.White).statusBarsPadding().padding(16.dp)){
  Row(Modifier.fillMaxWidth()){TextButton(onClick=onBack){Text("‹")};Text("Coin Seller Center",style=MaterialTheme.typography.titleMedium);Spacer(Modifier.weight(1f));TextButton({view=true}){Text("Details")}}
  Text("꧁Ks༒Prad...",style=MaterialTheme.typography.titleMedium);Text("ID:116943047",style=MaterialTheme.typography.bodySmall)
  HorizontalDivider(Modifier.padding(vertical=12.dp));Text("WhatsApp                         +91 9837152239");Text("Payment Method                         🇮🇳")
  Spacer(Modifier.height(20.dp));Text("Available Balance",style=MaterialTheme.typography.bodySmall);Text("0",style=MaterialTheme.typography.headlineMedium)
  Row{Text("Sales method:");Spacer(Modifier.width(18.dp));FilterChip(method=="User",{method="User"},{Text("User")});Spacer(Modifier.width(8.dp));FilterChip(method=="Seller",{method="Seller"},{Text("Seller")})}
  OutlinedTextField(id,{id=it},Modifier.fillMaxWidth(),label={Text("$method ID")});OutlinedTextField(amount,{amount=it},Modifier.fillMaxWidth(),label={Text("Amount")})
  Button({}){Text("Transfer")}
  if(view) AlertDialog(onDismissRequest={view=false},confirmButton={Button({view=false}){Text("OK")}},title={Text("Details")},text={Text("Transfer records")})
 }
}
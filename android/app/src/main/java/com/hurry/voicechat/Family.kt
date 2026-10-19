package com.hurry.voicechat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
data class FamilyMemberNative(val id:String,val name:String,val relation:String,val avatar:String="",val isAdmin:Boolean=false)
@Composable fun Family(onBack:()->Unit){
 var showRules by remember{mutableStateOf(false)}
 var members by remember{mutableStateOf(listOf<FamilyMemberNative>())}
 Column(Modifier.fillMaxSize().background(Color.White)){
  Row(Modifier.fillMaxWidth().padding(top=18.dp,start=14.dp,end=14.dp,bottom=12.dp),verticalAlignment=Alignment.CenterVertically){
   IconButton(onClick=onBack){Icon(Icons.Default.ArrowBack,"Back")}
   Text("Family",style=MaterialTheme.typography.titleLarge,modifier=Modifier.weight(1f))
   IconButton(onClick={members=members+FamilyMemberNative("new","New member","Member")}){Icon(Icons.Default.Add,"Add")}
  }
  LazyColumn(Modifier.fillMaxSize().padding(horizontal=14.dp)){
   item{Card(Modifier.fillMaxWidth().clickable{showRules=true},shape=RoundedCornerShape(18.dp)){Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("Family rules",style=MaterialTheme.typography.titleMedium);Text("Learn about family rewards and membership",color=Color.Gray)};Icon(Icons.Default.ChevronRight,"Open")}};Spacer(Modifier.height(16.dp))}
   item{Text("Rewards",style=MaterialTheme.typography.titleMedium);Spacer(Modifier.height(8.dp))}
   item{Row(Modifier.fillMaxWidth()){listOf("Weekly ranking","Family activity","Member reward").forEach{title->Column(Modifier.weight(1f).padding(4.dp),horizontalAlignment=Alignment.CenterHorizontally){Box(Modifier.fillMaxWidth().aspectRatio(1f).background(Color(0xFF6B230A),RoundedCornerShape(14.dp)));Row{repeat(5){Icon(Icons.Default.Star,null,Modifier.size(10.dp),tint=Color(0xFFFFD700))}};Text(title,style=MaterialTheme.typography.labelSmall)}}}}
   item{Spacer(Modifier.height(20.dp));Text("Members",style=MaterialTheme.typography.titleMedium)}
   items(members.size){i->ListItem(headlineContent={Text(members[i].name)},supportingContent={Text(members[i].relation)})}
  }
 }
 if(showRules)AlertDialog(onDismissRequest={showRules=false},confirmButton={TextButton({showRules=false}){Text("OK")}},title={Text("Family rules")},text={Text("Top 10 families in weekly rankings receive rewards. A family can have up to 100 members. Each user can join one family. Leaders can manage members and disband the family.")})
}
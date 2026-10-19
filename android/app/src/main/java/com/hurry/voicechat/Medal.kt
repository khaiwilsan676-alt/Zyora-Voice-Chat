package com.hurry.voicechat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

data class HurryMedal(val id:String,val name:String,val image:String)
@Composable
fun Medal(onBack:()->Unit={},medals:List<HurryMedal>=emptyList()){
 Column(Modifier.fillMaxSize().background(Color(0xFF050814))){
  Row(Modifier.padding(16.dp),horizontalArrangement=Arrangement.spacedBy(16.dp),verticalAlignment=Alignment.CenterVertically){Text("‹",color=Color.White,style=MaterialTheme.typography.headlineMedium);Text("Medal",color=Color.White,style=MaterialTheme.typography.titleLarge)}
  LazyVerticalGrid(columns=GridCells.Fixed(3),contentPadding=PaddingValues(12.dp),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){items(medals,key={it.id}){m->Column(Modifier.background(Color(0xFF1E1245),RoundedCornerShape(12.dp)).padding(8.dp),horizontalAlignment=Alignment.CenterHorizontally){AsyncImage(model=m.image,contentDescription=m.name,modifier=Modifier.size(78.dp));Text(m.name,color=Color.White,maxLines=1)}}}}
 }
}
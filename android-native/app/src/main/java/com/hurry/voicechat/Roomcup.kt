package com.hurry.voicechat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private data class CupEntry(val id:String,val name:String,val image:String,val coins:Long,val rank:Int)

@Composable
fun RoomCupSheet(roomId:String,userId:String,userName:String,userImage:String,onClose:()->Unit,onOnline:()->Unit) {
    var period by remember { mutableStateOf("daily") }
    var entries by remember { mutableStateOf(emptyList<CupEntry>()) }
    var mine by remember { mutableStateOf<CupEntry?>(null) }
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(roomId,period) {
        loading=true
        val data=withContext(Dispatchers.IO) { runCatching {
            val r=java.net.URLEncoder.encode(roomId,"UTF-8")
            val u=java.net.URLEncoder.encode(userId,"UTF-8")
            val c=URL("https://zyora-server.onrender.com/api/rooms/$r/contributions?period=$period&userId=$u").openConnection() as HttpURLConnection
            c.connectTimeout=8000; c.readTimeout=10000
            try {
                if(c.responseCode !in 200..299) null else {
                    val j=JSONObject(c.inputStream.bufferedReader().use{it.readText()})
                    fun parse(o:JSONObject)=CupEntry(o.optString("userId"),o.optString("name","User"),o.optString("image"),o.optLong("coins"),o.optInt("rank"))
                    val a=j.optJSONArray("leaderboard")
                    (0 until (a?.length()?:0)).mapNotNull{a?.optJSONObject(it)?.let(::parse)} to j.optJSONObject("currentUser")?.let(::parse)
                }
            } finally { c.disconnect() }
        }.getOrNull() }
        entries=data?.first.orEmpty(); mine=data?.second; loading=false
    }
    val clock=remember{SimpleDateFormat("HH:mm",Locale.getDefault()).apply{timeZone=TimeZone.getTimeZone("GMT+05:30")}}
    var now by remember{mutableStateOf(clock.format(Date()))}
    LaunchedEffect(Unit){while(true){now=clock.format(Date());kotlinx.coroutines.delay(30000)}}
    Box(Modifier.fillMaxSize().background(Color.Black.copy(.38f)).clickable{onClose()},contentAlignment=Alignment.BottomCenter) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.70f).clip(RoundedCornerShape(topStart=15.dp,topEnd=15.dp)).background(Color(0xFF050505)).clickable(enabled=false){},horizontalAlignment=Alignment.CenterHorizontally) {
            Spacer(Modifier.height(13.dp))
            Row(horizontalArrangement=Arrangement.spacedBy(34.dp),verticalAlignment=Alignment.CenterVertically) {
                CupTab("Contribution",true){}
                CupTab("Online User",false,onOnline)
            }
            Spacer(Modifier.height(9.dp))
            Box(Modifier.width(30.dp).height(3.dp).clip(CircleShape).background(Color.White))
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal=13.dp),verticalAlignment=Alignment.CenterVertically) {
                Row(Modifier.weight(1f).clip(CircleShape).background(Color(0xFF242424)).padding(3.dp)) {
                    CupPeriod("Daily",period=="daily",Modifier.weight(1f)){period="daily"}
                    CupPeriod("Weekly",period=="weekly",Modifier.weight(1f)){period="weekly"}
                }
                Spacer(Modifier.width(8.dp))
                Text("$now (GMT+5:30)",color=Color.LightGray,fontSize=10.sp)
            }
            Spacer(Modifier.height(12.dp))
            if(loading) Box(Modifier.weight(1f).fillMaxWidth(),contentAlignment=Alignment.Center){Text("Loading...",color=Color.Gray,fontSize=12.sp)}
            else if(entries.isEmpty()) Box(Modifier.weight(1f).fillMaxWidth(),contentAlignment=Alignment.Center) {
                Column(horizontalAlignment=Alignment.CenterHorizontally) {
                    AsyncImage(model="https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/file_0000000047308211a02722299d1fda2e.png",contentDescription="No data",modifier=Modifier.size(112.dp),contentScale=ContentScale.Fit)
                    Text("No data",color=Color.Gray,fontSize=12.sp)
                }
            } else LazyColumn(Modifier.weight(1f).fillMaxWidth(),contentPadding=PaddingValues(horizontal=14.dp,vertical=4.dp),verticalArrangement=Arrangement.spacedBy(13.dp)) {
                itemsIndexed(entries.take(20),key={_,e->e.id}){index,e->
                    Row(Modifier.fillMaxWidth().height(46.dp),verticalAlignment=Alignment.CenterVertically) {
                        when(index){
                            0->AsyncImage(model="https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/IMG_20261117_190736.jpg",contentDescription="Rank 1",modifier=Modifier.size(25.dp),contentScale=ContentScale.Fit)
                            1->AsyncImage(model="https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/IMG_20261117_190720.jpg",contentDescription="Rank 2",modifier=Modifier.size(25.dp),contentScale=ContentScale.Fit)
                            2->AsyncImage(model="https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/IMG_20261117_190658.jpg",contentDescription="Rank 3",modifier=Modifier.size(25.dp),contentScale=ContentScale.Fit)
                            else->Text("${index+1}",Modifier.width(25.dp),color=Color.White,fontSize=14.sp,textAlign=TextAlign.Center)
                        }
                        AsyncImage(model=e.image.ifBlank{"https://zyora-server.onrender.com/default-avatar.png"},contentDescription=e.name,modifier=Modifier.padding(start=9.dp).size(38.dp).clip(CircleShape),contentScale=ContentScale.Crop)
                        Column(Modifier.weight(1f).padding(start=9.dp),verticalArrangement=Arrangement.Center) {
                            Text(e.name,color=Color.White,fontSize=12.sp,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis)
                            Row(verticalAlignment=Alignment.CenterVertically) {
                                AsyncImage(model="https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/IMG_20261028_004355.png",contentDescription="Level",modifier=Modifier.width(36.dp).height(16.dp),contentScale=ContentScale.FillBounds)
                                Text("Lv.1",color=Color.White,fontSize=8.sp,fontWeight=FontWeight.Bold,modifier=Modifier.offset(x=(-31).dp))
                            }
                        }
                        Text(e.coins.toString(),color=Color.White,fontSize=12.sp,fontWeight=FontWeight.SemiBold)
                        AsyncImage(model="https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/1788258883971~2.jpg",contentDescription="Coins",modifier=Modifier.padding(start=4.dp).size(17.dp),contentScale=ContentScale.Fit)
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(start=12.dp,end=14.dp,top=8.dp,bottom=10.dp).clip(RoundedCornerShape(9.dp)).background(Color(0xFF263B54)).padding(horizontal=10.dp,vertical=9.dp),verticalAlignment=Alignment.CenterVertically) {
                AsyncImage(model=userImage.ifBlank{"https://zyora-server.onrender.com/default-avatar.png"},contentDescription=userName,modifier=Modifier.size(35.dp).clip(CircleShape),contentScale=ContentScale.Crop)
                Column(Modifier.weight(1f).padding(start=8.dp),verticalArrangement=Arrangement.spacedBy(2.dp)) {
                    Text(userName.ifBlank{"User"},color=Color.White,fontSize=11.sp,fontWeight=FontWeight.SemiBold,maxLines=1)
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        AsyncImage(model="https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/IMG_20261028_004355.png",contentDescription="Level",modifier=Modifier.width(36.dp).height(16.dp),contentScale=ContentScale.FillBounds)
                        Text("Lv.1",color=Color.White,fontSize=8.sp,fontWeight=FontWeight.Bold,modifier=Modifier.offset(x=(-31).dp))
                    }
                }
                Text(mine?.coins?.toString()?: "0",color=Color.White,fontSize=12.sp,fontWeight=FontWeight.Bold)
                AsyncImage(model="https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/1788258883971~2.jpg",contentDescription="Coins",modifier=Modifier.padding(start=4.dp).size(17.dp),contentScale=ContentScale.Fit)
                Text(if((mine?.rank?:0)>0)"#${mine?.rank}" else "#--",color=Color.White,fontSize=12.sp,modifier=Modifier.padding(start=10.dp))
            }
        }
    }
}
@Composable private fun CupTab(label:String,selected:Boolean,onClick:()->Unit){Text(label,Modifier.clickable{onClick()},color=if(selected)Color.White else Color.Gray,fontSize=14.sp,fontWeight=if(selected)FontWeight.SemiBold else FontWeight.Normal)}
@Composable private fun CupPeriod(label:String,selected:Boolean,modifier:Modifier,onClick:()->Unit){Box(modifier.height(29.dp).clip(CircleShape).background(if(selected)Color.White else Color.Transparent).clickable{onClick()},contentAlignment=Alignment.Center){Text(label,color=if(selected)Color.Black else Color.Gray,fontSize=11.sp,fontWeight=if(selected)FontWeight.SemiBold)}}

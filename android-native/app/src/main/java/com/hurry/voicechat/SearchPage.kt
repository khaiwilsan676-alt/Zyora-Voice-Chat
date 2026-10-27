package com.hurry.voicechat

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

private data class SearchUserNative(val id:String,val accountId:String,val name:String,val country:String,val image:String,val bio:String,val isRoom:Boolean=false,val locked:Boolean=false)

// 3 Blue Tracks aur Number ka Custom Component
@Composable
fun RoomTrackAnimation() {
    var number by remember { mutableIntStateOf((50..300).random()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay((1500..3500).random().toLong())
            val diff = (-7..10).random()
            if (number + diff > 0) number += diff
        }
    }
    val infiniteTransition = rememberInfiniteTransition(label = "audio_track")
    val h1 by infiniteTransition.animateFloat(initialValue = 6f, targetValue = 18f, animationSpec = infiniteRepeatable(animation = tween(400, easing = LinearEasing), repeatMode = RepeatMode.Reverse), label = "h1")
    val h2 by infiniteTransition.animateFloat(initialValue = 10f, targetValue = 22f, animationSpec = infiniteRepeatable(animation = tween(550, easing = LinearEasing), repeatMode = RepeatMode.Reverse), label = "h2")
    val h3 by infiniteTransition.animateFloat(initialValue = 4f, targetValue = 16f, animationSpec = infiniteRepeatable(animation = tween(350, easing = LinearEasing), repeatMode = RepeatMode.Reverse), label = "h3")
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = number.toString(), fontSize = 13.sp, color = Color(0xFF6B7280), fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.Bottom, modifier = Modifier.height(22.dp)) {
            Box(Modifier.width(3.dp).height(h1.dp).clip(CircleShape).background(Color(0xFF3B82F6)))
            Box(Modifier.width(3.dp).height(h2.dp).clip(CircleShape).background(Color(0xFF3B82F6)))
            Box(Modifier.width(3.dp).height(h3.dp).clip(CircleShape).background(Color(0xFF3B82F6)))
        }
    }
}

@Composable
fun SearchPage(onBack:()->Unit={},onOpenProfile:(RoomProfileUser)->Unit={},onOpenRoom:(HurryRoom)->Unit={}) {
 var query by remember { mutableStateOf("") }
 var tab by remember { mutableStateOf("User") }
 var results by remember { mutableStateOf<List<SearchUserNative>>(emptyList()) }
 var searching by remember { mutableStateOf(false) }
 var hasSearched by remember { mutableStateOf(false) }
 val searchCache = remember { mutableStateMapOf<String, List<SearchUserNative>>() }
 var requestId by remember { mutableIntStateOf(0) }
 suspend fun performSearch(qInput:String, requestToken:Int) {
  val q=qInput.trim(); if(q.isBlank()) { results=emptyList(); hasSearched=false; searching=false; return }
  val cacheKey = "$tab:$q"
  searchCache[cacheKey]?.let { cached ->
   if (requestToken == requestId) { results=cached; hasSearched=true; searching=false }
   return
  }
  if (requestToken == requestId) { searching=true; hasSearched=true }
  val fetched=withContext(Dispatchers.IO) {
   runCatching {
    val encoded=URLEncoder.encode(q,"UTF-8")
    val endpoint = if (tab == "Room") "https://zyora-voice-chat.onrender.com/api/rooms" else "https://zyora-voice-chat.onrender.com/api/users?search=$encoded&accountId=$encoded&q=$encoded"
    val conn=URL(endpoint).openConnection() as HttpURLConnection
    conn.connectTimeout=3500; conn.readTimeout=3500
    try {
     val status=conn.responseCode
     if (status == HttpURLConnection.HTTP_NOT_FOUND) return@runCatching emptyList<SearchUserNative>()
     val stream=if(status in 200..299) conn.inputStream else conn.errorStream
     val body=stream?.bufferedReader()?.use{it.readText()}.orEmpty()
     if(status !in 200..299) throw java.io.IOException("User search failed: HTTP $status")
     val root=body.trim()
     val arr=if(root.startsWith("[")) JSONArray(root) else {
      val o=JSONObject(root)
      val data=o.optJSONObject("data")
      if (tab == "Room") o.optJSONArray("rooms") ?: o.optJSONArray("results") ?: JSONArray()
      else o.optJSONArray("users") ?: o.optJSONArray("results") ?: data?.optJSONArray("users") ?: JSONArray().apply {
        (o.optJSONObject("user") ?: data?.optJSONObject("user"))?.let{put(it)}
       }
     }
     val list= mutableListOf<SearchUserNative>(); val seen=mutableSetOf<String>()
     for(i in 0 until arr.length()) {
      val u=arr.optJSONObject(i) ?: continue
      val id=u.optString("id").takeUnless{it=="null"}.orEmpty()
       .ifBlank{u.optString("uid").takeUnless{it=="null"}.orEmpty()}
       .ifBlank{u.optString("appLongId").takeUnless{it=="null"}.orEmpty()}
       .ifBlank{u.optString("_id").takeUnless{it=="null"}.orEmpty()}
      val acc=u.optString("accountId").takeUnless{it=="null"}.orEmpty()
       .ifBlank{u.optString("accountNumber").takeUnless{it=="null"}.orEmpty()}
       .ifBlank{u.optString("displayUserNumber").takeUnless{it=="null"}.orEmpty()}
       .ifBlank{u.optString("Account Number").takeUnless{it=="null"}.orEmpty()}
       .ifBlank{id}
      if (tab == "Room") {
       val roomId = u.optString("accountId").takeUnless{it=="null"}.orEmpty().ifBlank{u.optString("roomId").ifBlank{id}}
       val roomName = u.optString("name").takeUnless{it=="null"}.orEmpty().ifBlank{u.optString("roomName").ifBlank{"Room"}}
       val roomImage = u.optString("dp").takeUnless{it=="null"}.orEmpty().ifBlank{u.optString("image").ifBlank{u.optString("roomDp").ifBlank{u.optString("Room dp","")}}}
       if (roomId.isBlank() || !seen.add(roomId)) continue
       if (!roomId.contains(q, true) && !roomName.contains(q, true)) continue
       list.add(SearchUserNative(roomId,roomId,roomName,u.optString("country","🇮🇳"),roomImage,"",true,u.optBoolean("isLocked",false)))
       continue
      }
      if(acc.isBlank() || !seen.add(acc)) continue
      list.add(SearchUserNative(id.ifBlank{acc},acc,
       u.optString("name").takeUnless{it=="null"}.orEmpty().ifBlank{u.optString("displayName").ifBlank{u.optString("userName","User")}},
       u.optString("country","🇮🇳"),
       u.optString("image").takeUnless{it=="null"}.orEmpty().ifBlank{u.optString("photo").ifBlank{u.optString("photoURL").ifBlank{u.optString("avatar","")}}},
       u.optString("bio","")))
     }
     list
    } finally { conn.disconnect() }
   }.getOrDefault(emptyList())
  }
  if (requestToken == requestId) {
   searchCache[cacheKey] = fetched
   results = fetched
   searching = false
  }
 }
 LaunchedEffect(query,tab) {
  val token = ++requestId
  val q = query.trim()
  if (q.isBlank()) { results=emptyList(); hasSearched=false; searching=false; return@LaunchedEffect }
  searchCache["$tab:$q"]?.let { results=it; hasSearched=true; searching=false; return@LaunchedEffect }
  delay(180)
  performSearch(q, token)
 }
 Column(Modifier.fillMaxSize().background(Color(0xFFF0F2F5))) {
  Row(Modifier.fillMaxWidth().background(Color(0xFFF0F2F5)).statusBarsPadding().padding(start=14.dp,end=14.dp,top=7.dp,bottom=6.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
   BasicTextField(
    value = query,
    onValueChange = { value -> query = value.filter { it.isDigit() } },
    modifier = Modifier.weight(1f).height(44.dp).clip(CircleShape).background(Color(0xFFE5E7EB)),
    singleLine = true,
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1F2937)),
    cursorBrush = SolidColor(Color(0xFF2563EB)),
    decorationBox = { innerTextField ->
     Row(modifier = Modifier.fillMaxSize().padding(start = 12.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
      Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF6B7280), modifier = Modifier.size(19.dp))
      Spacer(Modifier.width(8.dp))
      Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
       if (query.isEmpty()) Text("Search by ID...", fontSize = 13.sp, color = Color(0xFF9CA3AF))
       innerTextField()
      }
     }
    }
   )
   Text("Cancel",modifier=Modifier.clickable{onBack()}.padding(vertical=8.dp),fontSize=14.sp,fontWeight=FontWeight.Medium,color=Color(0xFF374151))
  }
  Row(Modifier.fillMaxWidth().background(Color(0xFFF0F2F5)).padding(start=18.dp, end=16.dp),horizontalArrangement=Arrangement.spacedBy(22.dp)) {
   listOf("User","Room").forEach { label ->
    Column(Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { tab = label }.padding(top=8.dp,bottom=4.dp),horizontalAlignment=Alignment.Start) {
     Text(label,fontSize=14.sp,fontWeight=if(tab==label) FontWeight.SemiBold else FontWeight.Normal,color=if(tab==label)Color.Black else Color(0xFF6B7280))
     Spacer(Modifier.height(3.dp)); Box(Modifier.width(if(tab==label)18.dp else 0.dp).height(2.dp).clip(RoundedCornerShape(2.dp)).background(if(tab==label)Color.Black else Color.Transparent))
    }
   }
  }
  Box(Modifier.fillMaxSize().padding(horizontal=16.dp)) {
   when {
    searching -> Column(Modifier.fillMaxWidth().padding(top=76.dp),horizontalAlignment=Alignment.CenterHorizontally){Icon(Icons.Default.Search,contentDescription="Searching",tint=Color(0xFF6B7280),modifier=Modifier.size(30.dp));Spacer(Modifier.height(12.dp));Text("Searching...",fontSize=12.sp,fontWeight=FontWeight.SemiBold,color=Color(0xFF9CA3AF))}
    hasSearched && results.isEmpty() -> Column(Modifier.fillMaxWidth().padding(top=76.dp),horizontalAlignment=Alignment.CenterHorizontally){Icon(Icons.Default.Search,null,tint=Color(0x669CA3AF),modifier=Modifier.size(48.dp));Spacer(Modifier.height(8.dp));Text("No result found",fontSize=14.sp,fontWeight=FontWeight.SemiBold,color=Color(0xFF6B7280));Text("Try a different ID",fontSize=12.sp,color=Color(0xFF9CA3AF),modifier=Modifier.padding(top=4.dp))}
    !hasSearched -> Column(Modifier.fillMaxWidth().padding(top=76.dp),horizontalAlignment=Alignment.CenterHorizontally){Icon(Icons.Default.Search,null,tint=Color(0x669CA3AF),modifier=Modifier.size(48.dp));Spacer(Modifier.height(12.dp));Text("Search ${if(tab=="User")"Users" else "Rooms"}",fontSize=14.sp,fontWeight=FontWeight.SemiBold,color=Color(0xFF6B7280));Text("Enter an ID number to find people",fontSize=12.sp,color=Color(0xFF9CA3AF),modifier=Modifier.padding(top=4.dp))}
    else -> LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp),contentPadding = PaddingValues(top = 16.dp, bottom = 20.dp)) {
     items(results,key={it.accountId}) { u ->
      Row(Modifier.fillMaxWidth().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
        if (tab == "Room") onOpenRoom(HurryRoom(id=u.id,name=u.name,image=u.image,country=u.country,accountId=u.accountId,locked=u.locked))
        else onOpenProfile(RoomProfileUser(id=u.id,uid=u.id,accountId=u.accountId,name=u.name,image=u.image,country=u.country,flag="",bio=u.bio))
       }.padding(vertical = 8.dp),verticalAlignment=Alignment.CenterVertically) {
       AsyncImage(model=u.image.ifBlank{"https://zyora-voice-chat.vercel.app/default-avatar.png"},contentDescription=u.name,modifier=Modifier.size(48.dp).clip(if(tab=="Room") RoundedCornerShape(4.dp) else CircleShape).background(Color(0xFFE5E7EB)),contentScale=ContentScale.Crop)
       Spacer(Modifier.width(12.dp))
       if (tab == "Room") {
        Text(text=u.name,fontSize=15.sp,fontWeight=FontWeight.SemiBold,color=Color(0xFF111827),maxLines=1,overflow=TextOverflow.Ellipsis,modifier=Modifier.weight(1f))
       } else {
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.Center) {
         Text(text=u.name,fontSize=15.sp,fontWeight=FontWeight.SemiBold,color=Color(0xFF111827),maxLines=1,overflow=TextOverflow.Ellipsis)
         Spacer(Modifier.height(3.dp))
         Row(verticalAlignment=Alignment.CenterVertically) {
          Text(text=countryFlag(u.country),fontSize=12.sp)
          Spacer(Modifier.width(5.dp))
          AsyncImage(model="https://zyora-voice-chat.vercel.app/IMG_20261028_004355.png",contentDescription="Lv.1",modifier=Modifier.height(18.dp).width(31.dp),contentScale=ContentScale.Fit)
         }
        }
       }
      }
     }
    }
   }
  }
 }
}

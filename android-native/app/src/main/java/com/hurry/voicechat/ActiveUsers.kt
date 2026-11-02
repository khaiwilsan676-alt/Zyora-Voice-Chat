package com.hurry.voicechat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import org.json.JSONObject
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage

data class ActiveUserNative(val accountId:String,val name:String,val image:String)

@Composable
fun ActiveUsers(
 isOpen:Boolean,
 onClose:()->Unit,
 roomUsers:List<ActiveUserNative>,
 currentUser:ActiveUserNative,
 roomOwnerId:String,
 onOpenProfile:(ActiveUserNative)->Unit,
 onCopyUserId:(String)->Unit
){
 if(!isOpen)return
 val context = LocalContext.current
 val database = remember(context) { NativeIndexDatabase(context) }
 var countryByUser by remember { mutableStateOf<Map<String,String>>(emptyMap()) }
 LaunchedEffect(roomUsers, currentUser) {
  val profiles = (listOf(currentUser) + roomUsers).mapNotNull { user ->
   if (user.accountId.isBlank()) return@mapNotNull null
   val cached = database.loadProfile(user.accountId) ?: JSONObject()
   if (user.name.isNotBlank()) cached.put("name", user.name)
   if (user.image.isNotBlank()) cached.put("image", user.image)
   val remote = runCatching {
    ProfilePersistence.load(TargetUser(uid=user.accountId,id=user.accountId,accountId=user.accountId,name=user.name,image=user.image))
   }.getOrNull()
   if (remote != null) {
    listOf("country","countryName","location").forEach { key ->
     val value = remote.optString(key,"").trim()
     if (value.isNotBlank() && value!="null") cached.put(key,value)
    }
   }
   database.saveProfile(user.accountId, cached)
   user.accountId to sequenceOf("country","countryName","location").map { cached.optString(it,"").trim() }.firstOrNull { it.isNotBlank() && it!="null" }.orEmpty()
  }.toMap()
  countryByUser = profiles
 }
 Box(Modifier.fillMaxSize(),contentAlignment=Alignment.BottomCenter){
  Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.30f)).clickable(onClick=onClose))
  Column(
   Modifier.fillMaxWidth().fillMaxHeight(.40f)
    .clip(RoundedCornerShape(topStart=15.dp,topEnd=15.dp))
    .background(Color(0xEB000000))
  ){
   Text("Active Users",modifier=Modifier.fillMaxWidth().padding(start=16.dp,end=16.dp,top=16.dp,bottom=8.dp),textAlign=TextAlign.Center,fontSize=16.sp,fontWeight=FontWeight.Bold,color=Color.White)
   val visibleUsers = (roomUsers + currentUser)
    .filter { it.accountId.isNotBlank() }
    .distinctBy { it.accountId }
    .sortedWith(compareBy<ActiveUserNative> { if (it.accountId == roomOwnerId) 0 else 1 }.thenBy { it.name.lowercase() })
   LazyColumn(
    Modifier.weight(1f).fillMaxWidth(),
    contentPadding=PaddingValues(start=12.dp,end=12.dp,top=8.dp,bottom=16.dp),
    verticalArrangement=Arrangement.spacedBy(12.dp)
   ){
    items(visibleUsers,key={it.accountId}){ user ->
     Row(
      Modifier.fillMaxWidth().clickable { onOpenProfile(user) }.padding(horizontal=4.dp),
      verticalAlignment=Alignment.CenterVertically
     ){
      AsyncImage(
       model=user.image.ifBlank{"https://zyora-voice-chat.vercel.app/default-avatar.png"},
       contentDescription=user.name,
       modifier=Modifier.size(38.dp).clip(CircleShape),
       contentScale=ContentScale.Crop
      )
      Column(Modifier.weight(1f).padding(start=9.dp)){
       Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)){
        Text(user.name.ifBlank{"User"},fontSize=13.sp,fontWeight=FontWeight.SemiBold,color=Color.White,maxLines=1)
        if(user.accountId==roomOwnerId){
         AsyncImage(model="https://zyora-voice-chat.vercel.app/IMG_20261027_235537.png",contentDescription="Owner",contentScale=ContentScale.Fit,
          modifier=Modifier.height(19.dp).width(42.dp))
        }
       }
       Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)){
        val country = countryByUser[user.accountId].orEmpty()
        if(country.isNotBlank()) Text(activeCountryFlag(country),fontSize=14.sp)
        Box(Modifier.height(16.dp).width(36.dp),contentAlignment=Alignment.CenterEnd){
         AsyncImage(model="https://zyora-voice-chat.vercel.app/IMG_20261028_004355.png",contentDescription="Level",
          contentScale=ContentScale.FillBounds,modifier=Modifier.fillMaxSize())
         Text("Lv.1",color=Color.White,fontSize=8.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(end=5.dp))
        }
       }
      }
     }
    }
    if(visibleUsers.isEmpty()){
     item { Text("No active users",modifier=Modifier.fillMaxWidth().padding(18.dp),textAlign=TextAlign.Center,fontSize=12.sp,color=Color.White.copy(alpha=.65f)) }
    }
   }
  }
 }
}

private fun activeCountryFlag(country: String): String {
 val code = when(country.trim().lowercase()) {
  "india","indian" -> "IN"
  "united states","usa","us" -> "US"
  "united kingdom","uk","britain" -> "GB"
  "pakistan" -> "PK"
  "bangladesh" -> "BD"
  "nepal" -> "NP"
  "sri lanka" -> "LK"
  "uae","united arab emirates" -> "AE"
  "saudi arabia" -> "SA"
  "canada" -> "CA"
  "australia" -> "AU"
  else -> country.trim().uppercase().takeIf { it.length==2 } ?: return ""
 }
 return code.map { String(Character.toChars(0x1F1E6 + it.code - 65)) }.joinToString("")
}

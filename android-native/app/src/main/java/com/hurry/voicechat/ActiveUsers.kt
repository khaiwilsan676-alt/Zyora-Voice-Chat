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
 roomOwnerId:String,
 onOpenProfile:(ActiveUserNative)->Unit,
 onCopyUserId:(String)->Unit
){
 if(!isOpen)return
 val context = LocalContext.current
 val database = remember(context) { NativeIndexDatabase(context) }
 var countryByUser by remember { mutableStateOf<Map<String,String>>(emptyMap()) }
 LaunchedEffect(roomUsers) {
  val profiles = roomUsers.mapNotNull { user ->
   if (user.accountId.isBlank()) return@mapNotNull null
   val cached = database.loadProfile(user.accountId) ?: JSONObject()
   if (user.name.isNotBlank()) cached.put("name", user.name)
   if (user.image.isNotBlank()) cached.put("image", user.image)
   val remote = runCatching {
    ProfilePersistence.load(TargetUser(uid=user.accountId,id=user.accountId,accountId=user.accountId,name=user.name,image=user.image))
   }.getOrNull()
   if (remote != null) {
    val profileObjects = listOfNotNull(remote, remote.optJSONObject("profile"), remote.optJSONObject("publicProfile"), remote.optJSONObject("user"), remote.optJSONObject("data"))
    val country = profileObjects.asSequence().flatMap { obj ->
     sequenceOf("flag","countryName","country","Country","countryCode","country_code","location","country_name","nationality").map { key -> obj.optString(key,"").trim() }
    }.firstOrNull { it.isNotBlank() && it!="null" }.orEmpty()
    if (country.isNotBlank()) cached.put("country", country)
   }
   val cachedObjects = listOfNotNull(cached, cached.optJSONObject("profile"), cached.optJSONObject("publicProfile"), cached.optJSONObject("user"), cached.optJSONObject("data"))
   val country = cachedObjects.asSequence().flatMap { obj ->
    sequenceOf("countryName","country","Country","countryCode","country_code","location","country_name","nationality").map { key -> obj.optString(key,"").trim() }
   }.firstOrNull { it.isNotBlank() && it!="null" }.orEmpty()
   if (country.isNotBlank()) cached.put("country", country)
   database.saveProfile(user.accountId, cached)
   user.accountId to country
  }.toMap()
  countryByUser = profiles
 }
 Box(Modifier.fillMaxSize(),contentAlignment=Alignment.BottomCenter){
  Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.30f)).clickable(onClick=onClose))
  Column(
   Modifier.fillMaxWidth().fillMaxHeight(.48f)
    .clip(RoundedCornerShape(topStart=15.dp,topEnd=15.dp))
    .background(Color(0xEB000000))
  ){
   Text("Active Users",modifier=Modifier.fillMaxWidth().padding(start=16.dp,end=16.dp,top=16.dp,bottom=8.dp),textAlign=TextAlign.Center,fontSize=16.sp,fontWeight=FontWeight.Bold,color=Color.White)
   val visibleUsers = roomUsers
    .filter { it.accountId.isNotBlank() }
    .distinctBy { it.accountId }
    .sortedWith(compareBy<ActiveUserNative> { if (it.accountId == roomOwnerId) 0 else 1 }.thenBy { it.name.lowercase() })
   LazyColumn(
    Modifier.weight(1f).fillMaxWidth(),
    contentPadding=PaddingValues(start=12.dp,end=12.dp,top=4.dp,bottom=12.dp),
    verticalArrangement=Arrangement.spacedBy(8.dp)
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
        Text(user.name.ifBlank{"User"},fontSize=13.sp,fontWeight=FontWeight.SemiBold,color=Color.White,maxLines=1,softWrap=false,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        if(user.accountId==roomOwnerId){
         AsyncImage(model="https://zyora-voice-chat.vercel.app/IMG_20261027_235537.png",contentDescription="Owner",contentScale=ContentScale.Fit,
          modifier=Modifier.height(19.dp).width(42.dp))
        }
       }
       Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)){
        val country = countryByUser[user.accountId].orEmpty()
        // Keep the flag slot reserved so Lv.1 never shifts when a profile has no country.
        Box(Modifier.width(18.dp).height(18.dp),contentAlignment=Alignment.Center) {
         if(country.isNotBlank()) Text(roomProfileCountryFlag(country),fontSize=14.sp)
        }
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

fun roomProfileCountryFlag(country: String): String {
 val raw = country.trim()
 if (raw.isBlank()) return ""
 val regionalIndicators = raw.codePoints().toArray().filter { it in 0x1F1E6..0x1F1FF }
 if (regionalIndicators.size >= 2) {
  return String(Character.toChars(regionalIndicators[0])) + String(Character.toChars(regionalIndicators[1]))
 }
 val normalized = raw.substringBefore("(").trim().lowercase()
 val aliases = mapOf(
  "india" to "IN", "indian" to "IN",
  "united states" to "US", "united states of america" to "US", "usa" to "US", "us" to "US",
  "united kingdom" to "GB", "uk" to "GB", "britain" to "GB", "england" to "GB",
  "pakistan" to "PK", "bangladesh" to "BD", "nepal" to "NP", "sri lanka" to "LK",
  "uae" to "AE", "united arab emirates" to "AE", "saudi arabia" to "SA",
  "canada" to "CA", "australia" to "AU", "turkey" to "TR", "türkiye" to "TR",
  "other" to ""
 )
 val iso = java.util.Locale.getISOCountries().toSet()
 val code = aliases[normalized]
  ?: raw.uppercase().takeIf { it.length == 2 && it in iso }
  ?: java.util.Locale.getISOCountries().firstOrNull { java.util.Locale("", it).displayCountry.equals(normalized, ignoreCase = true) }
  ?: return ""
 if (code.isBlank()) return ""
 return code.map { String(Character.toChars(0x1F1E6 + it.code - 65)) }.joinToString("")
}

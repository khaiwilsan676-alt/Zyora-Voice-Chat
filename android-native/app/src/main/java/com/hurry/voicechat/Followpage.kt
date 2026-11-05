package com.hurry.voicechat
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val VISITOR_API = "https://zyora-server.onrender.com"
private data class VisitorData(val id:String,val name:String,val gender:String,val avatar:String,val lastViewedAt:Long,val viewCount:Int)

private suspend fun loadProfileVisitors(context:Context,userId:String,type:String):List<VisitorData> = withContext(Dispatchers.IO) {
    if(userId.isBlank()) return@withContext emptyList()
    val encoded=java.net.URLEncoder.encode(userId,"UTF-8")
    val connection=(URL("$VISITOR_API/api/profile-visitors?userId=$encoded&type=$type").openConnection() as HttpURLConnection).apply { requestMethod="GET";connectTimeout=12000;readTimeout=12000 }
    try {
        val status=connection.responseCode
        val body=(if(status in 200..299) connection.inputStream else connection.errorStream)?.bufferedReader()?.use{it.readText()}.orEmpty()
        if(status !in 200..299) return@withContext emptyList()
        val root=JSONObject(body)
        val rows=root.optJSONArray("visitors") ?: JSONArray()
        val result=(0 until rows.length()).mapNotNull { i -> rows.optJSONObject(i)?.let { v ->
            VisitorData(v.optString("id"),v.optString("name","User"),v.optString("gender"),v.optString("avatar"),v.optLong("lastViewedAt"),v.optInt("viewCount",1).coerceAtLeast(1))
        }}.take(20)
        NativeIndexDatabase(context).saveProfile("visitor_history:"+userId+":"+type,root)
        result
    } finally { connection.disconnect() }
}
private fun visitorDate(t:Long)=if(t<=0L) "—" else SimpleDateFormat("dd/MM/yyyy • hh:mm a",Locale.getDefault()).format(Date(t))

@Composable fun FollowPage(type:String,onBack:()->Unit) {
    if(type.equals("Visitors",true)) VisitorsPage(onBack) else FollowList(onBack=onBack,type=type)
}

@Composable fun VisitorsPage(onBack:()->Unit) {
    val context=LocalContext.current
    val prefs=remember(context){context.getSharedPreferences("hurry_auth",Context.MODE_PRIVATE)}
    val myId=remember(prefs){prefs.getString("accountId","").orEmpty().ifBlank{prefs.getString("uid","").orEmpty()}.ifBlank{prefs.getString("id","").orEmpty()}}
    var tab by remember { mutableIntStateOf(0) }
    var rows by remember { mutableStateOf<List<VisitorData>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    val scope=rememberCoroutineScope()
    LaunchedEffect(myId,tab) {
        val type=if(tab==0) "received" else "sent"
        val cached=NativeIndexDatabase(context).loadProfile("visitor_history:"+myId+":"+type)
        val cachedRows=cached?.optJSONArray("visitors")
        if(cachedRows!=null) rows=(0 until cachedRows.length()).mapNotNull { i -> cachedRows.optJSONObject(i)?.let { v ->
            VisitorData(v.optString("id"),v.optString("name","User"),v.optString("gender"),v.optString("avatar"),v.optLong("lastViewedAt"),v.optInt("viewCount",1).coerceAtLeast(1))
        }}.take(20)
        loading=rows.isEmpty()
        rows=loadProfileVisitors(context,myId,type)
        loading=false
    }
    Column(Modifier.fillMaxSize().background(Color(0xFF101116))) {
        Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal=16.dp),verticalAlignment=Alignment.CenterVertically) {
            Icon(Icons.Default.ArrowBack,"Back",tint=Color.White,modifier=Modifier.clickable{onBack()})
            Text("Visitors",color=Color.White,fontSize=19.sp,fontWeight=FontWeight.SemiBold,modifier=Modifier.weight(1f),textAlign=androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(Modifier.width(24.dp))
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly) {
            VisitorTab("My Visitors",tab==0){tab=0}
            VisitorTab("Who I have visited",tab==1){tab=1}
        }
        HorizontalDivider(color=Color(0xFF30313A))
        when {
            loading -> Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){CircularProgressIndicator(color=Color(0xFF4B9BFF))}
            rows.isEmpty() -> Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text("No visitors yet",color=Color(0xFF8B8D98),fontSize=14.sp)}
            else -> LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(horizontal=16.dp,vertical=10.dp)){items(rows,key={it.id}){VisitorItemRow(it,tab==0)}}
        }
    }
}
@Composable private fun VisitorTab(title:String,selected:Boolean,onClick:()->Unit) {
    Column(Modifier.clickable(onClick=onClick).padding(horizontal=12.dp,vertical=10.dp),horizontalAlignment=Alignment.CenterHorizontally) {
        Text(title,color=if(selected) Color.White else Color(0xFF888B96),fontSize=14.sp,fontWeight=if(selected) FontWeight.SemiBold else FontWeight.Normal)
        Spacer(Modifier.height(8.dp))
        Box(Modifier.width(38.dp).height(3.dp).clip(RoundedCornerShape(4.dp)).background(if(selected) Color(0xFF4B9BFF) else Color.Transparent))
    }
}
@Composable private fun VisitorItemRow(v:VisitorData,myVisitors:Boolean) {
    Row(Modifier.fillMaxWidth().padding(vertical=10.dp),verticalAlignment=Alignment.CenterVertically) {
        AsyncImage(model=v.avatar.takeIf{it.isNotBlank()},contentDescription="Avatar",contentScale=ContentScale.Crop,modifier=Modifier.size(52.dp).clip(CircleShape).background(Color(0xFF30313A)))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Text(v.name,color=Color.White,fontSize=15.sp,fontWeight=FontWeight.SemiBold)
                if(v.gender.isNotBlank()){Spacer(Modifier.width(6.dp));val male=v.gender.equals("male",true)||v.gender.equals("m",true);Box(Modifier.size(17.dp).clip(CircleShape).background(if(male) Color(0xFF347FE5) else Color(0xFFE34D91)),contentAlignment=Alignment.Center){Text(if(male)"♂" else "♀",color=Color.White,fontSize=10.sp)}}
            }
            Spacer(Modifier.height(4.dp))
            Text(if(myVisitors) "${v.viewCount} views • Last viewed ${visitorDate(v.lastViewedAt)}" else "Viewed ${visitorDate(v.lastViewedAt)}",color=Color(0xFF9699A5),fontSize=11.sp)
        }
    }
    HorizontalDivider(color=Color(0xFF242630))
}
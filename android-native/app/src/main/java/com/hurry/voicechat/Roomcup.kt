package com.hurry.voicechat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import androidx.compose.ui.platform.LocalContext
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

private fun formatCupCoins(value: Long): String {
    val amount = value.coerceAtLeast(0L).toDouble()
    return when {
        amount >= 1_000_000_000.0 -> String.format(Locale.US, "%.1fB", amount / 1_000_000_000.0).removeSuffix(".0B")
        amount >= 1_000_000.0 -> String.format(Locale.US, "%.1fM", amount / 1_000_000.0).removeSuffix(".0M")
        amount >= 1_000.0 -> String.format(Locale.US, "%.1fK", amount / 1_000.0).removeSuffix(".0K")
        else -> value.coerceAtLeast(0L).toString()
    }
}

/** Daily room cup total shown in the room's top cup pill. */
@Composable
fun RoomDailyCupTotal(roomId: String) {
    var total by remember(roomId) { mutableStateOf(0L) }
    var refresh by remember(roomId) { mutableIntStateOf(0) }
    LaunchedEffect(roomId) {
        while (true) {
            kotlinx.coroutines.delay(10_000)
            refresh++
        }
    }
    LaunchedEffect(roomId, refresh) {
        val latest = withContext(Dispatchers.IO) {
            runCatching {
                val encodedRoom = java.net.URLEncoder.encode(roomId, "UTF-8")
                val connection = URL("https://zyora-server.onrender.com/api/rooms/$encodedRoom/contributions?period=daily").openConnection() as HttpURLConnection
                connection.connectTimeout = 8_000
                connection.readTimeout = 10_000
                try {
                    if (connection.responseCode !in 200..299) 0L
                    else {
                        val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                        // Server total includes every sender, not just the Top 20 leaderboard.
                        json.optLong("totalRoomCoins", 0L).coerceAtLeast(0L)
                    }
                } finally {
                    connection.disconnect()
                }
            }.getOrDefault(0L)
        }
        total = latest
    }
    Text(
        text = formatCupCoins(total),
        color = Color(0xFFEEF3A3),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.1.sp
    )
}

@Composable
fun RoomCupSheet(roomId:String,userId:String,userName:String,userImage:String,onClose:()->Unit,initialOnline:Boolean=false,roomUsers:List<ActiveUserNative> = emptyList(),roomOwnerId:String="",roomAdminIds:List<String> = emptyList(),onOpenProfile:(ActiveUserNative)->Unit={},onCopyUserId:(String)->Unit={}) {
    val context = LocalContext.current
    var selectedOnline by remember(initialOnline) { mutableStateOf(initialOnline) }
    
    var period by remember { mutableStateOf("daily") }
    var entries by remember { mutableStateOf(emptyList<CupEntry>()) }
    var mine by remember { mutableStateOf<CupEntry?>(null) }
    var loading by remember { mutableStateOf(true) }
    var refreshTick by remember { mutableIntStateOf(0) }
    
    // Swipe state handle karne ke liye
    var swipeOffset by remember { mutableFloatStateOf(0f) }
    
    LaunchedEffect(roomId,period) { while (true) { kotlinx.coroutines.delay(10000); refreshTick++ } }
    
    LaunchedEffect(roomId,period,refreshTick) {
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
        Column(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(.62f)
                .clip(RoundedCornerShape(topStart=15.dp,topEnd=15.dp))
                .background(Color(0xFF050505))
                .clickable(enabled=false){}
                // Swipe gesture yaha lagaya hai
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (swipeOffset < -40) { // Right to Left Drag
                                if (!selectedOnline) {
                                    if (period == "daily") {
                                        period = "weekly"
                                    } else if (period == "weekly") {
                                        selectedOnline = true
                                    }
                                }
                            } else if (swipeOffset > 40) { // Left to Right Drag (Wapas aane ke liye)
                                if (selectedOnline) {
                                    selectedOnline = false
                                    period = "weekly"
                                } else if (period == "weekly") {
                                    period = "daily"
                                }
                            }
                            swipeOffset = 0f
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            swipeOffset += dragAmount
                        }
                    )
                },
            horizontalAlignment=Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(13.dp))
            Row(horizontalArrangement=Arrangement.spacedBy(34.dp),verticalAlignment=Alignment.CenterVertically) {
                CupTab("Contribution", !selectedOnline) { selectedOnline = false }
                CupTab("Online Users", selectedOnline) { selectedOnline = true }
            }
            Spacer(Modifier.height(16.dp))
            
            if (selectedOnline) {
                val visibleUsers = remember(roomUsers,roomOwnerId) { roomUsers.filter { it.accountId.isNotBlank() }.distinctBy { it.accountId }.sortedWith(compareBy<ActiveUserNative> { if(it.accountId==roomOwnerId) 0 else 1 }.thenBy { it.name.lowercase() }) }
                var genderByUser by remember(roomId) { mutableStateOf<Map<String,String>>(emptyMap()) }
                LaunchedEffect(roomId, visibleUsers) {
                    genderByUser = withContext(Dispatchers.IO) {
                        val db = NativeIndexDatabase(context)
                        visibleUsers.mapNotNull { user ->
                            runCatching { db.loadProfile(user.accountId)?.optString("gender", "").orEmpty()
                                .takeIf { it.isNotBlank() && it != "null" }?.let { user.accountId to it }
                            }.getOrNull()
                        }.toMap()
                    }
                }
                LazyColumn(Modifier.weight(1f).fillMaxWidth(),contentPadding=PaddingValues(horizontal=12.dp,vertical=8.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    itemsIndexed(visibleUsers,key={_,u->u.accountId}) { _,u ->
                        Row(Modifier.fillMaxWidth().clickable { onOpenProfile(u) }.padding(horizontal=4.dp),verticalAlignment=Alignment.CenterVertically) {
                            AsyncImage(model=u.image.ifBlank{"https://zyora-server.onrender.com/default-avatar.png"},contentDescription=u.name,modifier=Modifier.size(38.dp).clip(CircleShape),contentScale=ContentScale.Crop)
                            Column(Modifier.weight(1f).padding(start=9.dp)) {
                                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(3.dp)) {
                                    Text(u.name.ifBlank{"User"},color=Color.White,fontSize=13.sp,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis)
                                    if(u.accountId==roomOwnerId) AsyncImage(model="https://zyora-voice-chat.vercel.app/IMG_20261027_235537.png",contentDescription="Owner",modifier=Modifier.width(36.dp).height(16.dp),contentScale=ContentScale.Fit)
                                    else if(u.accountId in roomAdminIds) AsyncImage(model=ROOM_ADMIN_TAG,contentDescription="Room Admin",modifier=Modifier.width(36.dp).height(16.dp),contentScale=ContentScale.Fit)
                                }
                                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                                    val gender = genderByUser[u.accountId].orEmpty()
                                    if(gender.isNotBlank()) {
                                        val isFemale = gender.equals("female", true) || gender.equals("f", true) || gender == "♀"
                                        Box(Modifier.size(18.dp).clip(CircleShape).background(if(isFemale) Color(0xFFEC4899) else Color(0xFF3B82F6)),contentAlignment=Alignment.Center) {
                                            CustomGenderIcon(isFemale=isFemale,modifier=Modifier.size(8.dp))
                                        }
                                    }
                                    Box(Modifier.height(16.dp).width(36.dp),contentAlignment=Alignment.CenterEnd) {
                                        AsyncImage(model="https://zyora-voice-chat.vercel.app/IMG_20261028_004355.png",contentDescription="Level",modifier=Modifier.fillMaxSize(),contentScale=ContentScale.FillBounds)
                                        // 1dp Left (x=4.dp se x=3.dp)
                                        Text("Lv.1",color=Color.White,fontSize=8.sp,fontWeight=FontWeight.Bold,modifier=Modifier.offset(x=3.dp,y=(-3).dp).padding(end=5.dp))
                                    }
                                }
                            }
                        }
                    }
                    if(visibleUsers.isEmpty()) item { Text("No active users",Modifier.fillMaxWidth().padding(18.dp),color=Color.White.copy(alpha=.65f),fontSize=12.sp,textAlign=TextAlign.Center) }
                }
            } else {
                Row(Modifier.fillMaxWidth().padding(horizontal=13.dp),verticalAlignment=Alignment.CenterVertically) {
                    Row(Modifier.width(150.dp).height(34.dp).clip(CircleShape).background(Color(0xFF242424)).padding(3.dp)) {
                        CupPeriod("Daily", period == "daily", Modifier.weight(1f)) { period = "daily" }
                        CupPeriod("Weekly", period == "weekly", Modifier.weight(1f)) { period = "weekly" }
                    }
                    Spacer(Modifier.weight(1f))
                    Text("$now (GMT+5:30)",color=Color.LightGray,fontSize=10.sp)
                }
                Spacer(Modifier.height(12.dp))
                
                if(loading) Box(Modifier.weight(1f).fillMaxWidth(),contentAlignment=Alignment.Center){Text("Loading...",color=Color.Gray,fontSize=12.sp)}
                else if(entries.isEmpty()) Box(Modifier.weight(1f).fillMaxWidth(),contentAlignment=Alignment.Center) {
                    Column(horizontalAlignment=Alignment.CenterHorizontally) {
                        AsyncImage(model="https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/file_0000000047308211a02722299d1fda2e.png",contentDescription="No data",modifier=Modifier.size(75.dp),contentScale=ContentScale.Fit)
                        Spacer(Modifier.height(8.dp))
                        Text("No data.",color=Color.Gray,fontSize=12.sp)
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
                                    // 1dp Right (x=-23.dp se x=-22.dp)
                                    Text("Lv.1",color=Color.White,fontSize=8.sp,fontWeight=FontWeight.Bold,modifier=Modifier.offset(x=(-22).dp))
                                }
                            }
                            Text(formatCupCoins(e.coins),color=Color.White,fontSize=12.sp,fontWeight=FontWeight.SemiBold)
                            AsyncImage(model="https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/file_00000000e56882119c217d508b6733dc.png",contentDescription="Coins",modifier=Modifier.padding(start=4.dp).size(17.dp),contentScale=ContentScale.Fit)
                        }
                    }
                }
                
                // Ye dekh 50% (0.50f) Blue karke uske baad Black mix kar diya
                Row(Modifier.fillMaxWidth().background(Brush.verticalGradient(0.0f to Color(0xFF1E88E5), 0.50f to Color.Black, 1.0f to Color.Black)).padding(horizontal=16.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically) {
                    
                    // BOTTOM RANK LOGIC - Image for 1, 2, 3
                    val myRank = mine?.rank ?: 0
                    if (myRank in 1..3) {
                        when (myRank) {
                            1 -> AsyncImage(model="https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/IMG_20261117_190736.jpg",contentDescription="Rank 1",modifier=Modifier.width(28.dp).padding(end=3.dp).height(25.dp),contentScale=ContentScale.Fit)
                            2 -> AsyncImage(model="https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/IMG_20261117_190720.jpg",contentDescription="Rank 2",modifier=Modifier.width(28.dp).padding(end=3.dp).height(25.dp),contentScale=ContentScale.Fit)
                            3 -> AsyncImage(model="https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/IMG_20261117_190658.jpg",contentDescription="Rank 3",modifier=Modifier.width(28.dp).padding(end=3.dp).height(25.dp),contentScale=ContentScale.Fit)
                        }
                    } else {
                        Text(if(myRank>0) "$myRank" else "-",color=Color.White,fontSize=16.sp,fontWeight=FontWeight.Bold,modifier=Modifier.width(28.dp).padding(end=12.dp))
                    }
                    
                    AsyncImage(model=userImage.ifBlank{"https://zyora-server.onrender.com/default-avatar.png"},contentDescription=userName,modifier=Modifier.size(42.dp).clip(CircleShape),contentScale=ContentScale.Crop)
                    
                    Column(Modifier.weight(1f).padding(start=10.dp),verticalArrangement=Arrangement.spacedBy(2.dp)) {
                        Text(userName.ifBlank{"User"},color=Color.White,fontSize=14.sp,fontWeight=FontWeight.SemiBold,maxLines=1)
                        Row(verticalAlignment=Alignment.CenterVertically) {
                            AsyncImage(model="https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/IMG_20261028_004355.png",contentDescription="Level",modifier=Modifier.width(36.dp).height(16.dp),contentScale=ContentScale.FillBounds)
                            // 1dp Right (x=-23.dp se x=-22.dp)
                            Text("Lv.1",color=Color.White,fontSize=8.sp,fontWeight=FontWeight.Bold,modifier=Modifier.offset(x=(-22).dp))
                        }
                    }
                    
                    Text(formatCupCoins(mine?.coins ?: 0L),color=Color.White,fontSize=14.sp,fontWeight=FontWeight.Bold)
                    AsyncImage(model="https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/file_00000000e56882119c217d508b6733dc.png",contentDescription="Coins",modifier=Modifier.padding(start=6.dp).size(18.dp),contentScale=ContentScale.Fit)
                }
            }
        }
    }
}

@Composable private fun CupTab(label:String,selected:Boolean,onClick:()->Unit){
    Column(Modifier.clickable{onClick()},horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(7.dp)) {
        Text(label,color=if(selected)Color.White else Color.Gray,fontSize=14.sp,fontWeight=if(selected)FontWeight.SemiBold else FontWeight.Normal)
        Box(Modifier.width(30.dp).height(3.dp).clip(CircleShape).background(if(selected)Color.White else Color.Transparent))
    }
}

@Composable private fun CupPeriod(label:String,selected:Boolean,modifier:Modifier,onClick:()->Unit){
    Box(modifier.fillMaxHeight().clip(CircleShape).background(if(selected)Color.White else Color.Transparent).clickable{onClick()},contentAlignment=Alignment.Center) {
        Text(label,color=if(selected)Color.Black else Color.Gray,fontSize=12.sp,fontWeight=FontWeight.SemiBold)
    }
}

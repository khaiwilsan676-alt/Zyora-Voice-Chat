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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
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
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter

private const val VISITOR_API = "https://zyora-server.onrender.com"
private data class VisitorData(val id:String,val name:String,val gender:String,val avatar:String,val lastViewedAt:Long,val viewCount:Int)

// Ye helper function duplicate users ko merge karke unka View Count badha dega
private fun processVisitorRows(rows: JSONArray): List<VisitorData> {
    val rawList = (0 until rows.length()).mapNotNull { i -> 
        rows.optJSONObject(i)?.let { v ->
            VisitorData(
                v.optString("id"),
                v.optString("name","User"),
                v.optString("gender"),
                v.optString("avatar"),
                v.optLong("lastViewedAt"),
                v.optInt("viewCount",1).coerceAtLeast(1)
            )
        }
    }
    // ID ke hisaab se group kiya aur view count ko sum (plus) kar diya
    return rawList.groupBy { it.id }.map { (_, list) ->
        val latest = list.maxByOrNull { it.lastViewedAt } ?: list.first()
        val totalViews = list.sumOf { it.viewCount }
        latest.copy(viewCount = totalViews)
    }.sortedByDescending { it.lastViewedAt }.take(20)
}

private suspend fun loadProfileVisitors(context:Context,userId:String,type:String):List<VisitorData> = withContext(Dispatchers.IO) {
    if(userId.isBlank()) return@withContext emptyList()
    
    fun cachedVisitors(): List<VisitorData> {
        val cached = runCatching { NativeIndexDatabase(context).loadProfile("visitor_history:" + userId + ":" + type) }.getOrNull()
        val cachedRows = cached?.optJSONArray("visitors") ?: return emptyList()
        return processVisitorRows(cachedRows) // Cache me bhi same deduplication logic
    }
    
    runCatching {
        val encoded=java.net.URLEncoder.encode(userId,"UTF-8")
        val connection=(URL("$VISITOR_API/api/profile-visitors?userId=$encoded&type=$type").openConnection() as HttpURLConnection).apply { requestMethod="GET";connectTimeout=12000;readTimeout=12000 }
        try {
            val status=connection.responseCode
            val body=(if(status in 200..299) connection.inputStream else connection.errorStream)?.bufferedReader()?.use{it.readText()}.orEmpty()
            if(status !in 200..299) return@runCatching cachedVisitors()
            
            val root=JSONObject(body)
            val rows=root.optJSONArray("visitors") ?: JSONArray()
            val result = processVisitorRows(rows) // Network API data pe deduplication
            
            runCatching { NativeIndexDatabase(context).saveProfile("visitor_history:"+userId+":"+type,root) }
            result
        } finally { connection.disconnect() }
    }.getOrElse { cachedVisitors() }
}

private fun visitorDate(t:Long)=if(t<=0L) "—" else SimpleDateFormat("dd/MM",Locale.getDefault()).format(Date(t))

val topBlueGradient = Brush.verticalGradient(
    0.0f to Color(0xFF0066FF),
    0.20f to Color(0xFFE5E7EB),
    1.0f to Color(0xFFE5E7EB)
)

@Composable 
fun FollowPage(type:String, onBack:()->Unit) {
    if(type.equals("Visitors",true)) {
        VisitorsPage(onBack)
    } else {
        OtherSocialPages(type = type, onBack = onBack)
    }
}

// -------------------------------------------------------------
// FANS, FRIENDS, FOLLOWING UI
// -------------------------------------------------------------
@Composable
fun OtherSocialPages(type: String, onBack: () -> Unit) {
    val socialPage = type in setOf("Friends", "Fans", "Following")
    
    Column(
        Modifier
            .fillMaxSize()
            .background(topBlueGradient)
            .statusBarsPadding()
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = Color(0xFF222222),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .clickable { onBack() }
            )
            
            Text(
                text = type, 
                fontSize = 20.sp, 
                fontWeight = FontWeight.Normal, 
                color = Color(0xFF111827),
                modifier = Modifier.align(Alignment.Center)
            )
        }

        if (socialPage) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally, 
                    modifier = Modifier.padding(bottom = 48.dp)
                ) {
                    val canvasModifier = Modifier.size(76.dp)
                    
                    when (type) {
                        "Friends" -> FriendsCanvas(canvasModifier)
                        "Fans" -> FansCanvas(canvasModifier)
                        "Following" -> FollowingCanvas(canvasModifier)
                    }

                    Spacer(Modifier.height(12.dp))
                    Text("No $type yet", fontSize = 14.sp, color = Color(0xFF9CA3AF))
                }
            }
        }
    }
}

@Composable
fun FriendsCanvas(modifier: Modifier) {
    androidx.compose.foundation.Canvas(modifier) {
        val ink = Color(0xFFB0B5BD)
        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.4.dp.toPx())
        drawCircle(ink, radius = size.minDimension * 0.12f, center = androidx.compose.ui.geometry.Offset(size.width * 0.35f, size.height * 0.35f), style = stroke)
        drawArc(ink, startAngle = 180f, sweepAngle = 180f, useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.15f, size.height * 0.55f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.4f, size.height * 0.4f), style = stroke)
        drawCircle(ink, radius = size.minDimension * 0.12f, center = androidx.compose.ui.geometry.Offset(size.width * 0.65f, size.height * 0.35f), style = stroke)
        drawArc(ink, startAngle = 180f, sweepAngle = 180f, useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.45f, size.height * 0.55f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.4f, size.height * 0.4f), style = stroke)
    }
}

@Composable
fun FansCanvas(modifier: Modifier) {
    androidx.compose.foundation.Canvas(modifier) {
        val ink = Color(0xFFB0B5BD)
        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.4.dp.toPx())
        drawCircle(ink, radius = size.minDimension * 0.13f, center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height * 0.31f), style = stroke)
        drawArc(ink, startAngle = 200f, sweepAngle = 140f, useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.17f, size.height * 0.36f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.66f, size.height * 0.53f), style = stroke)
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(size.width * 0.75f, size.height * 0.20f)
            lineTo(size.width * 0.80f, size.height * 0.10f)
            lineTo(size.width * 0.85f, size.height * 0.20f)
            lineTo(size.width * 0.95f, size.height * 0.25f)
            lineTo(size.width * 0.85f, size.height * 0.30f)
            lineTo(size.width * 0.80f, size.height * 0.40f)
            lineTo(size.width * 0.75f, size.height * 0.30f)
            lineTo(size.width * 0.65f, size.height * 0.25f)
            close()
        }
        drawPath(path, ink, style = stroke)
    }
}

@Composable
fun FollowingCanvas(modifier: Modifier) {
    androidx.compose.foundation.Canvas(modifier) {
        val ink = Color(0xFFB0B5BD)
        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.4.dp.toPx())
        drawCircle(ink, radius = size.minDimension * 0.13f, center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height * 0.31f), style = stroke)
        drawArc(ink, startAngle = 200f, sweepAngle = 140f, useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.17f, size.height * 0.36f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.66f, size.height * 0.53f), style = stroke)
        drawLine(ink, start = androidx.compose.ui.geometry.Offset(size.width * 0.75f, size.height * 0.25f), end = androidx.compose.ui.geometry.Offset(size.width * 0.95f, size.height * 0.25f), strokeWidth = 2.4.dp.toPx())
        drawLine(ink, start = androidx.compose.ui.geometry.Offset(size.width * 0.85f, size.height * 0.15f), end = androidx.compose.ui.geometry.Offset(size.width * 0.85f, size.height * 0.35f), strokeWidth = 2.4.dp.toPx())
    }
}

// -------------------------------------------------------------
// VISITORS UI (API LOGIC)
// -------------------------------------------------------------
@Composable 
fun VisitorsPage(onBack:()->Unit) {
    val context=LocalContext.current
    val prefs=remember(context){context.getSharedPreferences("hurry_auth",Context.MODE_PRIVATE)}
    val myId=remember(prefs){prefs.getString("accountId","").orEmpty().ifBlank{prefs.getString("uid","").orEmpty()}.ifBlank{prefs.getString("id","").orEmpty()}}
    
    var tab by remember { mutableIntStateOf(0) }
    var rows by remember { mutableStateOf<List<VisitorData>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    val scope=rememberCoroutineScope()
    
    val baseUrl = "https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/"

    LaunchedEffect(myId,tab) {
        val type=if(tab==0) "received" else "sent"
        val cached=NativeIndexDatabase(context).loadProfile("visitor_history:"+myId+":"+type)
        val cachedRows=cached?.optJSONArray("visitors")
        if(cachedRows!=null) {
            rows = processVisitorRows(cachedRows) // Cache UI pe bhi duplicate issue resolve kardiya hai 
        }
        loading=rows.isEmpty()
        rows=loadProfileVisitors(context,myId,type)
        loading=false
    }
    
    DisposableEffect(myId,tab) {
        if(myId.isBlank()) return@DisposableEffect onDispose { }
        val live=IO.socket(VISITOR_API,IO.Options().apply { transports=arrayOf("websocket"); reconnection=true })
        val listener=Emitter.Listener {
            scope.launch { rows=loadProfileVisitors(context,myId,if(tab==0) "received" else "sent") }
        }
        live.on("profile_visitor_updated",listener)
        live.on(Socket.EVENT_CONNECT) { live.emit("register",JSONObject().put("userId",myId).put("accountId",myId)) }
        live.connect()
        onDispose { live.off("profile_visitor_updated",listener); live.disconnect(); live.close() }
    }
    
    Column(
        Modifier
            .fillMaxSize()
            .background(topBlueGradient)
            .statusBarsPadding()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = Color(0xFF222222),
                modifier = Modifier.clickable { onBack() }
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = "Visitors",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF111827),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.width(40.dp))
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            VisitorTab("My Visitors", tab == 0) { tab = 0 }
            VisitorTab("Who I have visited", tab == 1) { tab = 1 }
        }
        HorizontalDivider(color = Color(0xFFD1D5DB), thickness = 1.dp)

        val bannerImage = if (tab == 0) {
            "${baseUrl}public/IMG_20261106_122418.png"
        } else {
            "${baseUrl}public/IMG_20261106_122332.png"
        }
        
        AsyncImage(
            model = bannerImage,
            contentDescription = "Banner",
            contentScale = ContentScale.Fit, 
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp) 
                .height(60.dp)
        )

        when {
            loading -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color(0xFF0066FF)) }
            rows.isEmpty() -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { Text("No visitors yet", color = Color(0xFF9CA3AF), fontSize = 14.sp) }
            else -> LazyColumn(
                Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
            ) {
                items(rows) { visitor ->
                    VisitorItemRow(v = visitor, isMyVisitorsTab = tab == 0, baseUrl = baseUrl)
                }
            }
        }
    }
}

@Composable 
private fun VisitorTab(title:String, selected:Boolean, onClick:()->Unit) {
    Column(
        Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            color = if (selected) Color(0xFF111827) else Color(0xFF9CA3AF),
            fontSize = 16.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .width(40.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                .background(if (selected) Color(0xFF222222) else Color.Transparent)
        )
    }
}

@Composable 
private fun VisitorItemRow(v:VisitorData, isMyVisitorsTab:Boolean, baseUrl:String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = v.avatar.takeIf { it.isNotBlank() },
            contentDescription = "Avatar",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(Color.LightGray)
        )
        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            val blurModifier = if (isMyVisitorsTab) Modifier.blur(8.dp) else Modifier

            Row(verticalAlignment = Alignment.CenterVertically, modifier = blurModifier) {
                // Name Text modified: weight de diya + maxlines 1 kardia taki ye lamba hoke gender icon ko niche na dhakele
                Text(
                    text = v.name,
                    color = Color.Black,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false) 
                )
                
                if(v.gender.isNotBlank()) {
                    Spacer(Modifier.width(6.dp))
                    val male = v.gender.equals("male", true) || v.gender.equals("m", true)
                    
                    Box(
                        contentAlignment = Alignment.Center, 
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(if (male) Color(0xFF3B82F6) else Color(0xFFEC4899))
                    ) {
                        // Gender Text modified: PlatformTextStyle me includeFontPadding false kardia taki box ke ekdam center me rahe
                        Text(
                            text = if (male) "♂" else "♀",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            style = androidx.compose.ui.text.TextStyle(
                                platformStyle = androidx.compose.ui.text.PlatformTextStyle(
                                    includeFontPadding = false
                                )
                            )
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(4.dp))
            
            Text(
                text = "${visitorDate(v.lastViewedAt)}  |  View ${v.viewCount} times",
                color = Color(0xFF9CA3AF),
                fontSize = 12.sp
            )
        }

        if (!isMyVisitorsTab) {
            AsyncImage(
                model = "${baseUrl}public/IMG_20261105_125330.png",
                contentDescription = "Chat",
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
            )
        }
    }
}

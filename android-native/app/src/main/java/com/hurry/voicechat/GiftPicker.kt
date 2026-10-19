package com.hurry.voicechat

import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay

private const val GIFT_ASSET_ROOT = "https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/"
data class NativeGift(val id:Int,val name:String,val coins:Int,val image:String="",val video:String?=null,val lucky:Boolean=false,val noMask:Boolean=false,val sideFade:Boolean=false)

private val hotNativeGifts = listOf(
    NativeGift(1,"Teddy",70000,"IMG_20260922_142150.jpg","VID_20260921_011932.mp4",noMask=false),
    NativeGift(2,"Autumn's Embrace",54900,"IMG_20260922_182259.png","gemini_generated_video_89e836bd~2.mp4",noMask=true),
    NativeGift(3,"Arab King",500000,"image_d9df9625~2.jpg","gemini_generated_video_17e19680~2.mp4",noMask=false,sideFade=true)
)
private val luckyNativeGifts = listOf(
    NativeGift(101,"Tiara",3000,"IMG_20260927_213855.png",lucky=true,noMask=true),
    NativeGift(102,"Lucky Clover",1499,"IMG_20260927_213917.png",lucky=true,noMask=true),
    NativeGift(103,"Hi",999,"IMG_20260927_213946.png",lucky=true,noMask=true),
    NativeGift(104,"Rose",3999,"IMG_20260927_214121.png",lucky=true,noMask=true),
    NativeGift(105,"Kiss",1600,"IMG_20260927_214139.png",lucky=true,noMask=true),
    NativeGift(106,"Balloon",4000,"IMG_20260927_214220.png",lucky=true,noMask=true),
    NativeGift(107,"Dragon",7000,"IMG_20260927_221521.png",lucky=true,noMask=true),
    NativeGift(108,"Nine Hands",10999,"IMG_20260927_221544.png",lucky=true,noMask=true),
    NativeGift(109,"Coffin",8999,"IMG_20260927_221559.png",lucky=true,noMask=true),
    NativeGift(110,"Sword",9999,"IMG_20260927_221615.png",lucky=true,noMask=true),
    NativeGift(111,"Love lock",5000,"IMG_20260927_221637.png",lucky=true,noMask=true),
    NativeGift(112,"Lantern",6999,"IMG_20260927_221654.png",lucky=true,noMask=true),
    NativeGift(113,"Ring",5999,"IMG_20260927_221707.png",lucky=true,noMask=true),
    NativeGift(114,"Dancing Girl",12000,"IMG_20260927_221722.png",lucky=true,noMask=true),
    NativeGift(115,"Whale",7899,"IMG_20260927_221742.png",lucky=true,noMask=true),
    NativeGift(116,"Star",9800,"file_0000000066f482118f772ed6fab4ad1f.png",lucky=true,noMask=true),
    NativeGift(117,"Fire Bird",13000,"file_00000000fe088211b7be0110e2d3f878.png",lucky=true,noMask=true)
)

@Composable
fun GiftPicker(
    open:Boolean=true,
    onClose:()->Unit,
    seats:List<Any> = emptyList(),
    onSend:((Int)->Unit)?=null,
    roomId:String?=null,
    currentUserAccountId:String?=null,
    currentUserName:String="User",
    currentUserImage:String="/default-avatar.png",
    roomUsers:List<Any> = emptyList()
) {
    if (!open) return
    val context = LocalContext.current
    var tab by remember { mutableStateOf("Hot") }
    var multiplier by remember { mutableIntStateOf(1) }
    var showMultipliers by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Int?>(null) }
    var recipientMode by remember { mutableStateOf("mic") }
    var selectedTargets by remember { mutableStateOf(setOf<String>()) }
    var balance by remember { mutableIntStateOf(0) }
    var playingVideo by remember { mutableStateOf<String?>(null) }
    var insufficient by remember { mutableStateOf(false) }
    val ink = Color.White
    val gifts = when(tab) { "Hot" -> hotNativeGifts; "Lucky" -> luckyNativeGifts; else -> emptyList() }
    val selectedGift = gifts.firstOrNull { it.id == selected }
    val visibleSeats = seats.mapNotNull { it as? NativeRoomSeat }.filter { it.occupied }
    LaunchedEffect(Unit) {
        while (true) {
            balance = context.getSharedPreferences("hurry_wallet", android.content.Context.MODE_PRIVATE).getInt("balance", 0)
            delay(1000)
        }
    }
    if (playingVideo != null) {
        Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment=Alignment.Center) {
            AndroidView(
                factory = { ctx -> VideoView(ctx).apply {
                    setVideoURI(Uri.parse(GIFT_ASSET_ROOT + playingVideo))
                    setOnPreparedListener { it.isLooping = true; start() }
                } },
                modifier = Modifier.fillMaxSize()
            )
            IconButton(onClick={playingVideo=null;onClose()},modifier=Modifier.align(Alignment.TopEnd).padding(16.dp)) { Icon(Icons.Default.Close,null,tint=Color.White) }
        }
        return
    }

    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.56f)).clickable { onClose() }, contentAlignment=Alignment.BottomCenter) {
        Column(
            Modifier.fillMaxWidth().fillMaxHeight(.50f).clip(RoundedCornerShape(topStart=5.dp,topEnd=5.dp))
                .background(Color(0xEB080D11)).clickable(enabled=false){}.padding(start=16.dp,end=16.dp,top=10.dp,bottom=8.dp)
        ) {
            Box(Modifier.fillMaxWidth()) {
                Row(Modifier.align(Alignment.TopEnd).padding(top=0.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    RecipientIcon(active=recipientMode=="mic",label="All",onClick={
                        recipientMode="mic"
                        selectedTargets=visibleSeats.mapNotNull{it.accountId.takeIf(String::isNotBlank)}.toSet()
                    },house=false)
                    RecipientIcon(active=recipientMode=="room",label="All",onClick={recipientMode="room";selectedTargets=emptySet()},house=true)
                }
                Row(Modifier.fillMaxWidth().padding(end=100.dp).horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalAlignment=Alignment.CenterVertically) {
                    visibleSeats.forEach { seat ->
                        val active = seat.accountId in selectedTargets
                        AsyncImage(model=seat.userImage.ifBlank{GIFT_ASSET_ROOT+"default-avatar.png"},contentDescription=seat.userName,modifier=Modifier.size(44.dp).clip(CircleShape).border(if(active)2.5.dp else 0.dp,if(active)Color(0xFF3B82F6) else Color.Transparent,CircleShape).clickable {
                            recipientMode="single"
                            selectedTargets=if(active) selectedTargets-seat.accountId else selectedTargets+seat.accountId
                        },contentScale=ContentScale.Crop)
                    }
                }
            }
            Spacer(Modifier.height(7.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha=.10f)))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top=7.dp,bottom=5.dp),horizontalArrangement=Arrangement.spacedBy(18.dp)) {
                listOf("Hot","Lucky","Luxury","Event").forEach { name ->
                    Text(name,fontSize=14.sp,fontWeight=if(tab==name)FontWeight.Bold else FontWeight.Medium,color=if(tab==name)Color.White else Color(0xFF9AA0A6),modifier=Modifier.clickable { tab=name;selected=null })
                }
            }
            LazyVerticalGrid(columns=GridCells.Fixed(4),modifier=Modifier.weight(1f).fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(3.dp),verticalArrangement=Arrangement.spacedBy(2.dp)) {
                items(gifts,key={it.id}) { gift ->
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).border(if(selected==gift.id)1.5.dp else 1.dp,if(selected==gift.id)Color(0xFF3B82F6) else Color.Transparent,RoundedCornerShape(8.dp)).clickable { selected=gift.id }.padding(horizontal=2.dp,vertical=5.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
                        Box(Modifier.size(64.dp),contentAlignment=Alignment.Center) {
                            AsyncImage(model=GIFT_ASSET_ROOT+gift.image,contentDescription=gift.name,modifier=Modifier.fillMaxSize().clip(if(gift.sideFade)RoundedCornerShape(12.dp) else RoundedCornerShape(0.dp)),contentScale=if(gift.noMask)ContentScale.Fit else ContentScale.Crop)
                            if(gift.lucky) AsyncImage(model=GIFT_ASSET_ROOT+"IMG_20260928_172454.png",contentDescription=null,modifier=Modifier.align(Alignment.TopStart).size(16.dp))
                            if(tab=="Hot" && gift.id==3) AsyncImage(model=GIFT_ASSET_ROOT+"IMG_20260928_172516.png",contentDescription=null,modifier=Modifier.align(Alignment.TopStart).size(16.dp))
                        }
                        Text(gift.name,fontSize=10.sp,color=Color.White,maxLines=1,overflow=TextOverflow.Ellipsis)
                        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.Center) {
                            AsyncImage(model=GIFT_ASSET_ROOT+"file_00000000e56882119c217d508b6733dc.png",contentDescription="Coins",modifier=Modifier.size(10.dp).clip(CircleShape))
                            Text("%,d".format(gift.coins),fontSize=9.sp,color=Color(0xFFB6BBC2),modifier=Modifier.padding(start=2.dp))
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top=7.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
                Row(Modifier.clip(RoundedCornerShape(5.dp)).clickable { }.padding(vertical=6.dp),verticalAlignment=Alignment.CenterVertically) {
                    AsyncImage(model=GIFT_ASSET_ROOT+"file_00000000e56882119c217d508b6733dc.png",contentDescription="Balance",modifier=Modifier.size(20.dp).clip(CircleShape))
                    Text("%,d".format(balance),fontSize=11.sp,fontWeight=FontWeight.Bold,color=Color.White,modifier=Modifier.padding(start=5.dp))
                    Icon(Icons.Default.ChevronRight,null,tint=Color.White,modifier=Modifier.size(16.dp))
                }
                Row(Modifier.clip(CircleShape).border(1.dp,Color(0xFF3B82F6),CircleShape),verticalAlignment=Alignment.CenterVertically) {
                    Box {
                        Row(Modifier.clickable { showMultipliers=!showMultipliers }.padding(horizontal=10.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically) {
                            Text("${multiplier}×",fontSize=12.sp,fontWeight=FontWeight.Bold,color=Color.White)
                            Icon(Icons.Default.KeyboardArrowUp,null,tint=Color.White,modifier=Modifier.size(13.dp))
                        }
                        DropdownMenu(expanded=showMultipliers,onDismissRequest={showMultipliers=false}) {
                            listOf(1,10,299,599,999).forEach { value -> DropdownMenuItem(text={Text("${value}×")},onClick={multiplier=value;showMultipliers=false}) }
                        }
                    }
                    Button(onClick={
                        val cost=(selectedGift?.coins ?: 0)*multiplier
                        if(selectedGift==null)return@Button
                        if(cost>balance){insufficient=true;return@Button}
                        val targets=when(recipientMode) {
                            "room" -> roomUsers.mapNotNull{it as? ActiveUserNative}.map{it.accountId}
                            "mic" -> visibleSeats.map{it.accountId}
                            else -> selectedTargets.toList()
                        }.filter(String::isNotBlank).distinct()
                        onSend?.invoke(cost)
                        if(selectedGift.video!=null && tab!="Lucky") playingVideo=selectedGift.video
                        else onClose()
                    },enabled=selectedGift!=null,colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF2563EB),disabledContainerColor=Color(0xFF2563EB).copy(alpha=.5f)),contentPadding=PaddingValues(horizontal=22.dp,vertical=6.dp),shape=RoundedCornerShape(topStart=0.dp,bottomStart=0.dp,topEnd=50.dp,bottomEnd=50.dp)) {
                        Text("Send",fontSize=12.sp,fontWeight=FontWeight.Bold,color=Color.White)
                    }
                }
            }
            if(insufficient) Text("Insufficient Balance",Modifier.align(Alignment.CenterHorizontally).padding(top=3.dp),color=Color.White,fontSize=12.sp)
        }
    }
}

@Composable
private fun RecipientIcon(active:Boolean,label:String,onClick:()->Unit,house:Boolean) {
    Box(Modifier.size(38.dp).clip(CircleShape).background(if(active)Color(0x333B82F6) else Color(0xFF282D32)).border(2.dp,if(active)Color(0xFF3B82F6) else Color(0xFF6B7280),CircleShape).clickable(onClick=onClick),contentAlignment=Alignment.Center) {
        if(house) Icon(Icons.Default.Home,null,tint=if(active)Color(0xFF3B82F6) else Color(0xFF9CA3AF),modifier=Modifier.size(20.dp))
        else Icon(Icons.Default.Mic,null,tint=if(active)Color(0xFF3B82F6) else Color(0xFF9CA3AF),modifier=Modifier.size(18.dp))
        Text(label,Modifier.align(Alignment.BottomEnd).offset(x=4.dp,y=4.dp).clip(RoundedCornerShape(50)).background(if(active)Color(0xFF3B82F6) else Color(0xFF9CA3AF)).padding(horizontal=4.dp),fontSize=8.sp,color=Color.White,fontWeight=FontWeight.Bold)
    }
}
